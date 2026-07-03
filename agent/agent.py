import sys
import argparse
import os
import requests
import re

from typing import TypedDict, Annotated, Sequence


def _detect_suspicious_goal_collapse(fol_text: str) -> str | None:
    goal_match = re.search(r'goal\[\s*"([^"]+)"\s*,', fol_text)
    if not goal_match:
        return None

    def _find_matching_bracket(text: str, open_index: int) -> int:
        depth = 0
        for i in range(open_index, len(text)):
            if text[i] == '[':
                depth += 1
            elif text[i] == ']':
                depth -= 1
                if depth == 0:
                    return i
        return -1

    goal_open = fol_text.find('[', goal_match.start())
    goal_end = _find_matching_bracket(fol_text, goal_open)
    if goal_end == -1:
        return None

    body_start = goal_match.end()
    goal_body = fol_text[body_start:goal_end].strip()
    if goal_body.startswith(('forall[', 'exists[')):
        return None

    placeholder_match = re.fullmatch(
        r'pred\[\s*"([^"]+)"\s*,\s*fun\[\s*"([A-Za-z])"\s*\]\s*\]',
        goal_body,
    )
    if not placeholder_match:
        return None

    const_name = placeholder_match.group(2)
    if const_name != 'c':
        return None

    problem_without_goal = fol_text[:goal_match.start()]
    if re.search(rf'fun\[\s*"{re.escape(const_name)}"\s*\]', problem_without_goal):
        return None

    if not re.search(rf'fundecl\[\s*"{re.escape(const_name)}"\s*,', fol_text):
        return None

    return (
        "Verdacht auf fehlerhafte Ziel-Uebersetzung: Das Ziel wurde auf die kuenstliche "
        f'Konstante fun["{const_name}"] reduziert ({goal_body}), obwohl diese Konstante '
        "ausserhalb des Ziels nicht verwendet wird. Das Modell hat wahrscheinlich ein "
        "allgemeines Ziel unzulaessig in einen Einzelfall umgeformt."
    )


def _normalize_fol_problem(fol_text: str) -> str:
    def _find_matching_bracket(text: str, open_index: int) -> int:
        depth = 0
        for i in range(open_index, len(text)):
            if text[i] == '[':
                depth += 1
            elif text[i] == ']':
                depth -= 1
                if depth == 0:
                    return i
        return -1

    fol_text = fol_text.strip()
    fol_text = re.sub(r'[}\s]+$', '', fol_text)
    fol_text = fol_text.replace('const[', 'fun[')

    default_type_match = re.search(r'typedecl\[\s*"([^"]+)"\s*\]', fol_text)
    default_type = default_type_match.group(1) if default_type_match else 'Entity'

    declared_predicates = re.findall(r'preddecl\[\s*"([^"]+)"', fol_text)
    declared_functions = re.findall(r'fundecl\[\s*"([^"]+)"', fol_text)

    def _rewrite_declared_calls(text: str, names: list[str], wrapper: str) -> str:
        for name in sorted(names, key=len, reverse=True):
            if not re.fullmatch(r'[A-Za-z_][A-Za-z0-9_]*', name):
                continue
            pattern = rf'(?<!["\w]){re.escape(name)}\s*\['
            replacement = f'{wrapper}["{name}", '
            text = re.sub(pattern, replacement, text)
        return text

    fol_text = _rewrite_declared_calls(fol_text, declared_predicates, 'pred')
    fol_text = _rewrite_declared_calls(fol_text, declared_functions, 'fun')

    quantified_vars = set(re.findall(r'(?:forall|exists)\[\s*"([^"]+)"\s*,', fol_text))
    for var_name in sorted(quantified_vars, key=len, reverse=True):
        pattern = rf',\s*fundecl\[\s*"{re.escape(var_name)}"\s*,\s*"[^"]+"\s*\]'
        fol_text = re.sub(pattern, '', fol_text)

    declared_constants = set(
        re.findall(r'fundecl\[\s*"([^"]+)"\s*,\s*"([^"]+)"\s*\]', fol_text)
    )
    inferred_constants = []
    for name in re.findall(r'fun\[\s*"([^"]+)"\s*\]', fol_text):
        entry = (name, default_type)
        if name not in quantified_vars and entry not in declared_constants and entry not in inferred_constants:
            inferred_constants.append(entry)

    declared_predicate_names = set(re.findall(r'preddecl\[\s*"([^"]+)"\s*,', fol_text))
    inferred_predicates = []
    for name in re.findall(r'pred\[\s*"([^"]+)"\s*,', fol_text):
        if name not in declared_predicate_names and name not in inferred_predicates:
            inferred_predicates.append(name)

    inferred_decls = []
    inferred_decls.extend(
        f'\n  preddecl["{name}", "{default_type}"],' for name in inferred_predicates
    )
    inferred_decls.extend(
        f'\n  fundecl["{name}", "{typ}"],' for name, typ in inferred_constants
    )

    if inferred_decls:
        decls = ''.join(inferred_decls)
        type_pattern = rf'(typedecl\[\s*"{re.escape(default_type)}"\s*\]\s*,)'
        fol_text, count = re.subn(
            type_pattern,
            r'\1' + decls,
            fol_text,
            count=1,
        )
        if count == 0:
            fol_text = re.sub(
                r'(problem\[\s*"[^"]+"\s*,)',
                r'\1' + decls,
                fol_text,
                count=1,
            )

    goal_match = re.search(r'goal\[\s*"([^"]+)"\s*,', fol_text)
    if goal_match:
        goal_open = fol_text.find('[', goal_match.start())
        goal_end = _find_matching_bracket(fol_text, goal_open)
        if goal_end != -1:
            body_start = goal_match.end()
            goal_body = fol_text[body_start:goal_end].strip()
            used_vars = set(re.findall(r'var\[\s*"([^"]+)"\s*\]', goal_body))
            bound_vars = set(re.findall(r'(?:forall|exists)\[\s*"([^"]+)"\s*,', goal_body))
            free_vars = sorted(v for v in used_vars if v not in bound_vars)
            if free_vars and not goal_body.startswith(('forall[', 'exists[')):
                for var_name in reversed(free_vars):
                    goal_body = f'forall["{var_name}", "{default_type}", {goal_body}]'
                fol_text = fol_text[:body_start] + goal_body + fol_text[goal_end:]

    return fol_text


class _InfixFormulaParser:
    def __init__(self, text: str):
        self.text = text.strip()
        self.tokens = self._tokenize(self.text)
        self.index = 0

    def _tokenize(self, text: str) -> list[str]:
        token_re = re.compile(
            r'\s*(->|\(|\)|,|forall\b|exists\b|and\b|or\b|not\b|[A-Za-z_][A-Za-z0-9_]*|.)'
        )
        tokens = []
        for match in token_re.finditer(text):
            token = match.group(1)
            if token.isspace():
                continue
            if token == '.':
                raise ValueError(f"Unerwartetes Zeichen '.' in Formel: {text}")
            if len(token) == 1 and not re.fullmatch(r'[A-Za-z_(),]', token) and token != '-' and token != '>':
                raise ValueError(f"Unerwartetes Zeichen '{token}' in Formel: {text}")
            tokens.append(token)
        return tokens

    def _peek(self) -> str | None:
        return self.tokens[self.index] if self.index < len(self.tokens) else None

    def _consume(self, expected: str | None = None) -> str:
        token = self._peek()
        if token is None:
            raise ValueError(f"Unerwartetes Ende der Formel: {self.text}")
        if expected is not None and token != expected:
            raise ValueError(f"Erwartet '{expected}', aber '{token}' gefunden in: {self.text}")
        self.index += 1
        return token

    def _consume_identifier(self) -> str:
        token = self._peek()
        if token is None or not re.fullmatch(r'[A-Za-z_][A-Za-z0-9_]*', token):
            raise ValueError(f"Bezeichner erwartet, aber '{token}' gefunden in: {self.text}")
        if token in {"forall", "exists", "and", "or", "not"}:
            raise ValueError(f"Bezeichner erwartet, aber Schluesselwort '{token}' gefunden in: {self.text}")
        self.index += 1
        return token

    def parse(self):
        node = self._parse_implication()
        if self._peek() is not None:
            raise ValueError(f"Unerwarteter Rest '{self._peek()}' in Formel: {self.text}")
        return node

    def _parse_implication(self):
        left = self._parse_disjunction()
        if self._peek() == '->':
            self._consume('->')
            right = self._parse_implication()
            return ('imp', left, right)
        return left

    def _parse_disjunction(self):
        node = self._parse_conjunction()
        while self._peek() == 'or':
            self._consume('or')
            node = ('or', node, self._parse_conjunction())
        return node

    def _parse_conjunction(self):
        node = self._parse_unary()
        while self._peek() == 'and':
            self._consume('and')
            node = ('and', node, self._parse_unary())
        return node

    def _parse_unary(self):
        token = self._peek()
        if token == 'not':
            self._consume('not')
            return ('not', self._parse_unary())
        if token in {'forall', 'exists'}:
            quantifier = self._consume()
            variable = self._consume_identifier()
            body = self._parse_implication()
            return (quantifier, variable, body)
        if token == '(':
            self._consume('(')
            node = self._parse_implication()
            self._consume(')')
            return node
        return self._parse_atom()

    def _parse_atom(self):
        name = self._consume_identifier()
        self._consume('(')
        argument = self._consume_identifier()
        self._consume(')')
        return ('pred', name, argument)


def _split_formula_lines(block: str) -> list[str]:
    formulas = []
    current = []
    balance = 0
    for raw_line in block.splitlines():
        line = raw_line.strip()
        if not line or line.startswith('//') or line.startswith('%'):
            continue
        current.append(line)
        balance += line.count('(') - line.count(')')
        if balance <= 0:
            formula = ' '.join(current).strip()
            if formula:
                formulas.append(formula)
            current = []
            balance = 0
    if current:
        formula = ' '.join(current).strip()
        if formula:
            formulas.append(formula)
    return formulas


def _extract_axioms_and_goal(query: str) -> tuple[list[str], str] | None:
    match = re.search(r'Axiome:\s*(.*?)\s*Ziel:\s*(.*)', query, flags=re.IGNORECASE | re.DOTALL)
    if not match:
        return None
    axioms_block = match.group(1)
    goal_block = match.group(2)
    goal_block = re.split(r'\n\s*Antworte\b', goal_block, maxsplit=1, flags=re.IGNORECASE)[0]
    axioms = _split_formula_lines(axioms_block)
    goals = _split_formula_lines(goal_block)
    if not axioms or not goals:
        raise ValueError("Axiome oder Ziel konnten nicht als Formelblock gelesen werden.")
    if len(goals) != 1:
        raise ValueError("Der direkte Formelpfad erwartet genau ein Ziel.")
    return axioms, goals[0]


def _canonicalize_axioms_goal_block(text: str) -> str:
    extracted = _extract_axioms_and_goal(text)
    if extracted is None:
        raise ValueError("Kein lesbarer Axiome/Ziel-Block gefunden.")
    axioms, goal = extracted
    return "Axiome:\n" + "\n".join(axioms) + "\n\nZiel:\n" + goal


def _collect_predicate_names(node, names: list[str]) -> None:
    kind = node[0]
    if kind == 'pred':
        if node[1] not in names:
            names.append(node[1])
        return
    if kind in {'and', 'or', 'imp'}:
        _collect_predicate_names(node[1], names)
        _collect_predicate_names(node[2], names)
        return
    if kind == 'not':
        _collect_predicate_names(node[1], names)
        return
    if kind in {'forall', 'exists'}:
        _collect_predicate_names(node[2], names)
        return
    raise ValueError(f"Unbekannter Knotentyp: {kind}")



def _collect_symbols(node, bound: set[str], predicates: list[str], constants: list[str]) -> None:
    kind = node[0]
    if kind == 'pred':
        name, argument = node[1], node[2]
        if name not in predicates:
            predicates.append(name)
        if argument not in bound and argument not in constants:
            constants.append(argument)
        return
    if kind in {'and', 'or', 'imp'}:
        _collect_symbols(node[1], bound, predicates, constants)
        _collect_symbols(node[2], bound, predicates, constants)
        return
    if kind == 'not':
        _collect_symbols(node[1], bound, predicates, constants)
        return
    if kind in {'forall', 'exists'}:
        new_bound = set(bound)
        new_bound.add(node[1])
        _collect_symbols(node[2], new_bound, predicates, constants)
        return
    raise ValueError(f"Unbekannter Knotentyp: {kind}")


def _render_direct_formula(node, bound: set[str]) -> str:
    kind = node[0]
    if kind == 'pred':
        name, argument = node[1], node[2]
        term = f'var["{argument}"]' if argument in bound else f'fun["{argument}"]'
        return f'pred["{name}", {term}]'
    if kind == 'not':
        return f'not[{_render_direct_formula(node[1], bound)}]'
    if kind in {'and', 'or', 'imp'}:
        return f'{kind}[{_render_direct_formula(node[1], bound)}, {_render_direct_formula(node[2], bound)}]'
    if kind in {'forall', 'exists'}:
        new_bound = set(bound)
        new_bound.add(node[1])
        return f'{kind}["{node[1]}", "Entity", {_render_direct_formula(node[2], new_bound)}]'
    raise ValueError(f"Unbekannter Knotentyp: {kind}")


def _build_direct_fol_problem(query: str) -> str | None:
    extracted = _extract_axioms_and_goal(query)
    if extracted is None:
        return None

    axiom_formulas, goal_formula = extracted
    parsed_axioms = [_InfixFormulaParser(formula).parse() for formula in axiom_formulas]
    parsed_goal = _InfixFormulaParser(goal_formula).parse()

    predicates: list[str] = []
    constants: list[str] = []
    for node in [*parsed_axioms, parsed_goal]:
        _collect_symbols(node, set(), predicates, constants)

    lines = ['problem["DirektPruefung",', '  typedecl["Entity"],']
    for name in predicates:
        lines.append(f'  preddecl["{name}", "Entity"],')
    for name in constants:
        lines.append(f'  fundecl["{name}", "Entity"],')
    for index, node in enumerate(parsed_axioms, start=1):
        rendered = _render_direct_formula(node, set())
        lines.append(f'  axiom["A{index}", {rendered}],')
    lines.append(f'  goal["G", {_render_direct_formula(parsed_goal, set())}]')
    lines.append(']')
    return "\n".join(lines)


def _query_wants_verbose_details(query: str) -> bool:
    return bool(re.search(r'\b(details?|protokoll|debug|solver|ausgabe)\b', query, flags=re.IGNORECASE))


def _query_wants_yes_no_answer(query: str) -> bool:
    return bool(re.search(r'Antworte\s+am\s+Ende\s+nur\s+mit\s*:\s*', query, flags=re.IGNORECASE | re.DOTALL))


def _looks_like_proof_request(query: str) -> bool:
    return bool(re.search(
        r'\b(proof|prove|beweis|pruefe|prÃ¼fe|folgt|does it follow|is it true|gueltig|gÃ¼ltig|widerlege|verify)\b',
        query,
        flags=re.IGNORECASE,
    ))


def _extract_message_text(content) -> str:
    if isinstance(content, str):
        return content
    if isinstance(content, list):
        parts = []
        for item in content:
            if isinstance(item, dict) and item.get("type") == "text":
                parts.append(item.get("text", ""))
        return "\n".join(parts)
    return str(content)

def _extract_formal_block(text: str) -> str:
    pattern = re.compile(
        r'(Axiome:\s*.*?\s*Ziel:\s*.*?)(?=(?:\n\s*Axiome:)|(?:\n\s*Wenn das so korrekt ist\b)|\Z)',
        flags=re.IGNORECASE | re.DOTALL,
    )
    matches = [m.group(1).strip() for m in pattern.finditer(text)]
    if _prove_debug_enabled():
        print(f"=== FORMAL BLOCK CANDIDATES: {len(matches)} ===")
        for i, block in enumerate(matches, start=1):
            print(f"=== FORMAL BLOCK CANDIDATE {i} BEGIN ===")
            print(block)
            print(f"=== FORMAL BLOCK CANDIDATE {i} END ===")
    if not matches:
        raise ValueError("Kein sauberer Axiome/Ziel-Block in der LLM-Antwort gefunden.")

    candidates = []
    for block in matches:
        if not re.search(r'\bAxiome:', block, flags=re.IGNORECASE):
            continue
        if not re.search(r'\bZiel:', block, flags=re.IGNORECASE):
            continue
        candidates.append(block)

    if not candidates:
        raise ValueError("Axiome:-Block fehlt in der LLM-Antwort.")

    return candidates[-1]

def _translate_natural_language_to_formal_blocks(llm, query: str) -> str:
    translation_prompt = """
Du uebersetzt eine natuerlichsprachliche Logikfrage in ein strenges Zwischenformat.
Antworte ausschliesslich im folgenden Format und fuege nichts anderes hinzu:

Axiome:
<eine Formel pro Zeile>

Ziel:
<genau eine Formel>

Regeln:
- Verwende nur die Operatoren forall, exists, not, and, or, ->.
- Verwende nur Praedikate der Form Name(x) oder Name(Konstante).
- Bewahre die Bedeutung exakt. Erfinde keine neuen Tatsachen.
- Verwende keine FOL-PRE-Syntax, kein problem[...], keine Erklaerung.
- Erfinde keine zusammengesetzten Praedikate wie BlackAnimal(x), WhiteSwan(x), MortalHuman(x), wenn der Nutzer diese Namen nicht ausdruecklich selbst verwendet.
- Bei Eigenschaft + generischem Nomen gilt: benutze die Eigenschaft als Praedikat. Beispiel: 'black animal' wird Black(x), nicht BlackAnimal(x).
- Wenn die Aussage von der Form 'if A and B, then C' ist, schreibe A und B als getrennte Axiome, Ziel ist C.
- Wenn 'there are no X that are Y' vorkommt, verwende not exists x (X(x) and Y(x)).
- Wenn 'every X is Y' vorkommt, verwende forall x (X(x) -> Y(x)).

Beispiel:
Aussage:
Is it true that, if every swan is white and there are no black swans, then every black animal is not a swan?

Korrekte Antwort:
Axiome:
forall x (Swan(x) -> White(x))
not exists x (Swan(x) and Black(x))

Ziel:
forall x (Black(x) -> not Swan(x))
"""
    response = llm.invoke([
        SystemMessage(content=translation_prompt),
        HumanMessage(content=query),
    ])
    translated = _extract_message_text(response.content).strip()
    _debug_print_proof_text("FORMAL BRIDGE RAW OUTPUT", translated)
    extracted = _extract_formal_block(translated)
    canonical = _canonicalize_axioms_goal_block(extracted)
    _debug_print_proof_text("FORMAL BRIDGE EXTRACTED BLOCK", canonical)
    return canonical


def _run_query_through_formal_bridge(llm, query: str) -> str | None:
    if not _looks_like_proof_request(query):
        return None
    translated = _translate_natural_language_to_formal_blocks(llm, query)
    direct_problem = _build_direct_fol_problem(translated)
    if direct_problem is None:
        raise ValueError("Das LLM-Zwischenformat enthielt keine lesbaren Axiome/Ziel-Bloecke.")
    verbose = _query_wants_verbose_details(query)
    result = _run_proof_text(direct_problem, verbose=verbose)
    if _query_wants_yes_no_answer(query):
        yes_no = "Ja" if result.startswith("SUCCESS") else "Nein"
        if verbose:
            return f"{result}\n{yes_no}"
        return yes_no
    return result

def _translate_query_to_formal_blocks_only(llm, query: str) -> str | None:
    if not _looks_like_proof_request(query):
        return None
    return _translate_natural_language_to_formal_blocks(llm, query)


def _revise_formal_blocks(llm, original_query: str, current_blocks: str, user_feedback: str) -> str:
    try:
        canonical_feedback = _canonicalize_axioms_goal_block(user_feedback)
    except ValueError:
        canonical_feedback = None

    if canonical_feedback is not None:
        _debug_print_proof_text("FORMAL BRIDGE REVISION USER BLOCK", canonical_feedback)
        return canonical_feedback

    revision_prompt = """
Du bekommst:
1. die urspruengliche natuerlichsprachliche Anfrage,
2. die aktuelle formale Uebersetzung,
3. die Rueckmeldung des Nutzers zur Korrektur.

Erzeuge daraus eine verbesserte formale Uebersetzung.
Antworte ausschliesslich im folgenden Format und fuege nichts anderes hinzu:

Axiome:
<eine Formel pro Zeile>

Ziel:
<genau eine Formel>

Regeln:
- Verwende nur die Operatoren forall, exists, not, and, or, ->.
- Verwende keine FOL-PRE-Syntax.
- Bewahre die Bedeutung der urspruenglichen Anfrage exakt.
- Beruecksichtige die Nutzerkorrektur strikt.
- Erfinde keine neuen Axiome oder Hilfspraedikate.
"""
    revision_query = (
        "Urspruengliche Anfrage:\n"
        f"{original_query}\n\n"
        "Aktuelle formale Uebersetzung:\n"
        f"{current_blocks}\n\n"
        "Nutzerkorrektur:\n"
        f"{user_feedback}"
    )
    response = llm.invoke([
        SystemMessage(content=revision_prompt),
        HumanMessage(content=revision_query),
    ])
    translated = _extract_message_text(response.content).strip()
    _debug_print_proof_text("FORMAL BRIDGE REVISION RAW OUTPUT", translated)
    extracted = _extract_formal_block(translated)
    canonical = _canonicalize_axioms_goal_block(extracted)
    _debug_print_proof_text("FORMAL BRIDGE REVISION EXTRACTED BLOCK", canonical)
    return canonical


def _disjointness_signature(node):
    kind = node[0]
    if kind == 'forall':
        var_name = node[1]
        body = node[2]
        if (
            isinstance(body, tuple) and len(body) == 3 and body[0] == 'imp'
            and isinstance(body[1], tuple) and len(body[1]) == 3 and body[1][0] == 'pred'
            and isinstance(body[2], tuple) and len(body[2]) == 2 and body[2][0] == 'not'
            and isinstance(body[2][1], tuple) and len(body[2][1]) == 3 and body[2][1][0] == 'pred'
            and body[1][2] == var_name and body[2][1][2] == var_name
        ):
            return ("disjoint", tuple(sorted((body[1][1], body[2][1][1]))))
    if kind == 'not':
        inner = node[1]
        if isinstance(inner, tuple) and len(inner) == 3 and inner[0] == 'exists':
            var_name = inner[1]
            body = inner[2]
            if (
                isinstance(body, tuple) and len(body) == 3 and body[0] == 'and'
                and isinstance(body[1], tuple) and len(body[1]) == 3 and body[1][0] == 'pred'
                and isinstance(body[2], tuple) and len(body[2]) == 3 and body[2][0] == 'pred'
                and body[1][2] == var_name and body[2][2] == var_name
            ):
                return ("disjoint", tuple(sorted((body[1][1], body[2][1]))))
    return None


def _detect_suspicious_axiom_goal_overlap(formal_blocks: str) -> str | None:
    try:
        extracted = _extract_axioms_and_goal(formal_blocks)
    except ValueError:
        return None
    if extracted is None:
        return None
    axioms, goal = extracted
    try:
        goal_ast = _InfixFormulaParser(goal).parse()
    except ValueError:
        return None
    goal_sig = _disjointness_signature(goal_ast)
    goal_norm = re.sub(r'\s+', '', goal)
    for index, axiom in enumerate(axioms, start=1):
        axiom_norm = re.sub(r'\s+', '', axiom)
        if axiom_norm == goal_norm:
            return (
                f"WARNUNG: Axiom A{index} ist textgleich mit dem Ziel. "
                "Bitte pruefe, ob die LLM das Ziel unzulaessig als Zusatzannahme eingefuehrt hat."
            )
        try:
            axiom_ast = _InfixFormulaParser(axiom).parse()
        except ValueError:
            continue
        axiom_sig = _disjointness_signature(axiom_ast)
        if goal_sig is not None and axiom_sig == goal_sig:
            return (
                f"WARNUNG: Axiom A{index} ist logisch gleichwertig zum Ziel ({axiom}). "
                "Bitte pruefe, ob die LLM eine zusaetzliche Annahme eingefuehrt hat, "
                "die das Ziel bereits vorwegnimmt."
            )
    return None


def _describe_simple_implication(node) -> str | None:
    if (
        isinstance(node, tuple)
        and len(node) == 3
        and node[0] == 'forall'
        and isinstance(node[2], tuple)
        and len(node[2]) == 3
        and node[2][0] == 'imp'
        and isinstance(node[2][1], tuple)
        and len(node[2][1]) == 3
        and node[2][1][0] == 'pred'
        and isinstance(node[2][2], tuple)
        and len(node[2][2]) == 3
        and node[2][2][0] == 'pred'
    ):
        return f"Aus den Axiomen folgt nur, dass jedes `{node[2][1][1]}` auch `{node[2][2][1]}` ist."
    return None


def _goal_disjointness_parts(goal_ast) -> tuple[str, str, str] | None:
    if (
        isinstance(goal_ast, tuple)
        and len(goal_ast) == 3
        and goal_ast[0] == 'forall'
        and isinstance(goal_ast[2], tuple)
        and len(goal_ast[2]) == 3
        and goal_ast[2][0] == 'imp'
        and isinstance(goal_ast[2][2], tuple)
        and len(goal_ast[2][2]) == 2
        and goal_ast[2][2][0] == 'not'
        and isinstance(goal_ast[2][2][1], tuple)
        and len(goal_ast[2][2][1]) == 3
        and goal_ast[2][2][1][0] == 'pred'
    ):
        antecedent = goal_ast[2][1]
        if isinstance(antecedent, tuple) and len(antecedent) == 3 and antecedent[0] == 'pred':
            return goal_ast[1], antecedent[1], goal_ast[2][2][1][1]
        if (
            isinstance(antecedent, tuple)
            and len(antecedent) == 3
            and antecedent[0] == 'and'
            and isinstance(antecedent[1], tuple)
            and len(antecedent[1]) == 3
            and antecedent[1][0] == 'pred'
        ):
            return goal_ast[1], antecedent[1][1], goal_ast[2][2][1][1]
    return None


def _suggest_missing_assumption(goal_ast) -> str | None:
    disjointness = _goal_disjointness_parts(goal_ast)
    if disjointness is None:
        return None
    var_name, left, right = disjointness
    return (
        "Zum Beweis waere zum Beispiel eine Zusatzannahme noetig wie "
        f"`not exists {var_name} ({left}({var_name}) and {right}({var_name}))` "
        "oder eine andere Regel, die genau diese Unvertraeglichkeit ausdrueckt."
    )


def _explain_failure_from_formal_blocks(formal_blocks: str) -> str | None:
    try:
        extracted = _extract_axioms_and_goal(formal_blocks)
    except ValueError:
        return None
    if extracted is None:
        return None

    axioms, goal = extracted
    try:
        goal_ast = _InfixFormulaParser(goal).parse()
    except ValueError:
        return None

    axiom_asts = []
    for axiom in axioms:
        try:
            axiom_asts.append(_InfixFormulaParser(axiom).parse())
        except ValueError:
            continue


    parts: list[str] = []
    simple_axiom_descriptions = []
    for axiom_ast in axiom_asts:
        description = _describe_simple_implication(axiom_ast)
        if description:
            simple_axiom_descriptions.append(description)

    if simple_axiom_descriptions:
        parts.append(" ".join(simple_axiom_descriptions))

    suggestion = _suggest_missing_assumption(goal_ast)
    if suggestion:
        parts.append(
            "Daraus folgt aber noch nicht, dass die im Ziel genannten Eigenschaften einander ausschliessen."
        )
        parts.append(suggestion)
    else:
        goal_predicates: list[str] = []
        _collect_predicate_names(goal_ast, goal_predicates)

        axiom_predicates: list[str] = []
        for axiom_ast in axiom_asts:
            _collect_predicate_names(axiom_ast, axiom_predicates)

        missing = [name for name in goal_predicates if name not in axiom_predicates]
        if missing:
            missing_text = ", ".join(f"`{name}`" for name in missing)
            parts.append(
                "Im Ziel kommen zusaetzliche Praedikate vor, die in den Axiomen nicht ausreichend angebunden sind: "
                f"{missing_text}."
            )


    if not parts:
        parts.append(
            "Das Ziel folgt aus den vorhandenen Axiomen nicht zwingend. "
            "Es fehlt mindestens eine zusaetzliche Regel, die die Praedikate des Ziels mit den Axiomen verbindet."
        )

    return " ".join(parts)


def _prove_debug_enabled() -> bool:
    return os.getenv("PROVE_DEBUG", "").strip().lower() in {"1", "true", "yes", "on"}


def _debug_print_proof_text(label: str, text: str) -> None:
    if not _prove_debug_enabled():
        return
    print(f"=== {label} BEGIN ===")
    print(text)
    print(f"=== {label} END ===")
    print(f"=== {label} REPR BEGIN ===")
    print(repr(text))
    print(f"=== {label} REPR END ===")


def _run_proof_text(fol_text: str, interactive: bool=False, verbose: bool=False) -> str:
    _debug_print_proof_text("PROVE RAW INPUT", fol_text)
    lines = fol_text.splitlines()
    clean_lines = []
    for line in lines:
        stripped = line.strip()
        if not stripped or stripped.startswith('%') or stripped.startswith('//'):
            continue
        clean_lines.append(line)
    fol_text = "\n".join(clean_lines).strip()
    _debug_print_proof_text("PROVE CLEANED INPUT", fol_text)
    fol_text = _normalize_fol_problem(fol_text)
    _debug_print_proof_text("PROVE NORMALIZED INPUT", fol_text)

    suspicious_goal = _detect_suspicious_goal_collapse(fol_text)
    if suspicious_goal:
        if verbose:
            return f"FAILURE\n{suspicious_goal}"
        return "FAILURE"

    success, output = risctp_prover.prove(fol_text, interactive)
    status = "SUCCESS" if success else "FAILURE"
    if verbose:
        return f"{status}\n{output}"
    return status


from langchain_core.messages import BaseMessage, HumanMessage, SystemMessage
from langchain_core.tools import tool
from langchain_openai import ChatOpenAI
from langchain_google_genai import ChatGoogleGenerativeAI
from langchain_anthropic import ChatAnthropic
from langchain.agents import create_agent
from langgraph.checkpoint.memory import MemorySaver

import prover as risctp_prover

@tool
def download(url: str) -> str:
    """Download resource from the denoted URL and return its content."""
    print("download", url)
    if "FOL-PRE.txt" in url:
        local_path = "FOL-PRE.txt"
        if os.path.exists(local_path):
            try:
                with open(local_path, "r", encoding="utf-8") as f:
                    return f.read()
            except Exception as e:
                return f"Fehler beim Lesen der lokalen Datei: {e}"
        else:
            return "Fehler: Die lokale Datei FOL-PRE.txt wurde auf dem Server nicht gefunden."
    try:
        with requests.get(url, timeout=10) as r:
            r.raise_for_status()
            return r.text
    except requests.exceptions.RequestException as e:
        return f"An error occurred during download: {e}"

@tool(return_direct=True)
def prove(fol_text: str, interactive: bool=False, verbose: bool=False) -> str:
    """
    Uebergibt ein FOL-PRE-Problem an den Python-Prover-Adapter.
    Standardmaessig wird nur SUCCESS oder FAILURE zurueckgegeben.
    Mit verbose=True wird zusaetzlich das Solver-Protokoll ausgegeben.
    """
    print("prove", "interactive" if interactive else "automatic")
    return _run_proof_text(fol_text, interactive=interactive, verbose=verbose)


SYSTEM_PROMPT=\
'''
Du bist ein praeziser Logik-Assistent fuer das RISCTP-System.
Antworte dem Nutzer immer auf Deutsch. Nutze sauberes Markdown zur Strukturierung.

DEINE AUFGABE:
Du hilfst bei der Uebersetzung natuerlicher Sprache in formale Logik und bei der Pruefung,
ob ein Ziel aus gegebenen Axiomen folgt.

VERFUEGBARE TOOLS:

1. Tool "download(url: str) -> str"
   Verwende dieses Tool, wenn du fuer deine Antwort zusaetzlichen Referenztext brauchst,
   insbesondere fuer die Datei FOL-PRE.txt oder fuer eine vom Nutzer genannte Ressource.
   Verwende es nicht, wenn du die Antwort bereits aus dem aktuellen Kontext geben kannst.

2. Tool "prove(fol_text: str, interactive: bool=False, verbose: bool=False) -> str"
   Verwende dieses Tool immer dann, wenn der Nutzer eine logische Aussage pruefen,
   beweisen, widerlegen oder formal verifizieren lassen will.
   Verwende dieses Tool auch dann, wenn der Nutzer nur eine Ja/Nein-Antwort verlangt.
   Rate in solchen Faellen niemals frei aus dem Sprachmodell heraus.
   Verwende verbose=False als Standard.
   Verwende verbose=True nur dann, wenn der Nutzer ausdruecklich das Solver-Protokoll,
   Details, Debug-Ausgaben oder die genaue Rueckmeldung des Solvers sehen will.

WAS IM SICHTBAREN CHAT STEHEN SOLL:
- Praesentiere Axiome und Ziel in gut lesbarer Form.
- Verwende im sichtbaren Chat keine haessliche FOL-PRE-Praefixsyntax, wenn sie nicht ausdruecklich verlangt wird.
- Wenn der Nutzer keine direkte Pruefung verlangt, darfst du zuerst die Uebersetzung erklaeren und bestaetigen lassen.

WANN "prove" AUFGERUFEN WERDEN MUSS:
- Wenn der Nutzer ausdruecklich nach einem Beweis, einer Pruefung oder einer logischen Folgerung fragt.
- Wenn der Nutzer Formulierungen verwendet wie "folgt", "beweisbar", "gueltig", "pruefe", "verify", "does it follow".
- Wenn der Nutzer nur "Ja/Nein" als Antwort will.
- Wenn der Nutzer nach einer Bestaetigung wie "Ja" oder "Beweis starten" den Beweis ausloest.

WIE "prove" AUFGERUFEN WERDEN MUSS:
Wenn du das Tool "prove" aufrufst, musst du das Problem zwingend in der offiziellen,
eckigen FOL-PRE-Praefixsyntax mit problem[...] formatieren.
Freie Schreibweisen im Tool-Argument sind verboten.

Muster fuer das unsichtbare Tool-Argument (fol_text):
problem["ProblemName",
  typedecl["Entity"],
  fundecl["c", "Entity"],
  preddecl["p1", "Entity"],
  preddecl["p2", "Entity"],
  axiom["A1", forall["x", "Entity", imp[pred["p1", var["x"]], not[pred["p2", var["x"]]]]]],
  goal["G", pred["p2", fun["c"]]]
]

WICHTIGE FORMATREGELN FUER FOL-PRE:
- Verwende fuer Konstanten immer fun["Name"], niemals const["Name"].
- Wenn eine Konstante wie fun["Socrates"] vorkommt, muss sie auch als fundecl["Socrates", "Entity"] deklariert sein.
- Das Tool-Argument muss mit problem[...] beginnen und ein vollstaendiges Problem enthalten.

WENN DER NUTZER AXIOME ODER ZIELE BEREITS ALS LOGISCHE FORMELN ANGIBT:
- Bewahre die logische Struktur exakt. Uebersetze nur mechanisch nach FOL-PRE.
- Erfinde keine neuen Konstanten, Zeugen oder Hilfspraedikate.
- Ersetze ein universell quantifiziertes Ziel niemals durch eine einzelne Konstante wie fun["c"].
- Jede verwendete Praedikatsbezeichnung braucht eine preddecl["Name", "Entity"].
- Das Problem braucht genau eine typedecl["Entity"].
- Eine Formel der Form (A and B) -> C muss zu imp[and[A, B], C] werden.
- Eine Formel der Form A -> (B and C) muss zu imp[A, and[B, C]] werden.
- Eine Formel der Form A -> (B or C) muss zu imp[A, or[B, C]] werden.
- Vertausche diese drei Formen niemals miteinander.

BEISPIEL FUER EINE KORREKTE MECHANISCHE UEBERSETZUNG:
Nutzereingabe:
forall x ((LuftfeuchtigkeitHoch(x) and FensterGeschlossen(x)) -> EntfeuchterAn(x))

Korrekte Uebersetzung im Tool:
axiom["A1", forall["x", "Entity", imp[
  and[
    pred["LuftfeuchtigkeitHoch", var["x"]],
    pred["FensterGeschlossen", var["x"]]
  ],
  pred["EntfeuchterAn", var["x"]]
]]]

Wenn der Nutzer "Zeige Details", "mit Protokoll", "Debug", "Solver-Ausgabe" oder aehnliches verlangt,
sollst du prove mit verbose=True aufrufen. Sonst verwende verbose=False.

EMPFOHLENER ABLAUF:
1. Verstehe die Nutzeranfrage.
2. Wenn noetig, nutze download fuer Referenzmaterial.
3. Wenn eine formale Pruefung verlangt ist, formatiere das Problem korrekt und rufe prove auf.
4. Gib das Ergebnis ehrlich wieder und erfinde keinen Beweis.
5. Interpretiere SUCCESS als erfolgreich bewiesen und FAILURE als nicht erfolgreich bewiesen.
6. Wenn der Nutzer nur eine knappe Antwort will, formuliere diese auf Basis von SUCCESS oder FAILURE.
7. Wenn prove mit verbose=True einen Parsing-, Typ-, Solver- oder Prover-Fehler liefert, darfst du prove nicht erneut mit einer improvisierten Variante aufrufen.
8. Gib in diesem Fall den Fehler stattdessen direkt und unveraendert an den Nutzer weiter.
'''

def _decorate_failure_result(result: str, formal_blocks: str, original_query: str | None=None) -> str:
    if not result.startswith("FAILURE"):
        return result
    if original_query and _query_wants_yes_no_answer(original_query):
        return result
    explanation = _explain_failure_from_formal_blocks(formal_blocks)
    if not explanation:
        return result
    return f"{result}\n\n{explanation}"


class MultiModelAgent:
    def __init__(self, model_type: str):
        self.tools = [download, prove]
        self.memory = MemorySaver()
        self.config = {"configurable": {"thread_id": "default_user"}}
        self.counter = 0
        self.model_type = model_type
        self.pending_formal_blocks: str | None = None
        self.pending_original_query: str | None = None

        if model_type == "gpt":
            self.llm = ChatOpenAI(model="gpt-4o-mini", temperature=0)
        elif model_type == "gemini":
            self.llm = ChatGoogleGenerativeAI(model="gemini-2.5-pro", temperature=0)
        elif model_type == "claude":
            self.llm = ChatAnthropic(model="claude-sonnet-4-6", temperature=0)
        elif model_type == "ollama":
            try:
                from langchain_ollama import ChatOllama
            except ImportError as e:
                raise ImportError(
                    "MODEL_TYPE=ollama verlangt das Paket 'langchain-ollama'. "
                    "Installiere es in der aktiven venv, zum Beispiel mit "
                    "'pip install langchain-ollama'."
                ) from e
            self.llm = ChatOllama(
                model=os.getenv("OLLAMA_MODEL", "llama3.1:8b"),
                base_url=os.getenv("OLLAMA_BASE_URL", "http://192.168.2.31:11434"),
                temperature=0,
            )
        else:
            raise ValueError("Unsupported model type. Use 'gpt', 'gemini', 'claude' or 'ollama'.")

        self.agent_executor = create_agent(
            self.llm,
            self.tools,
            checkpointer=self.memory
        )

    def agent(self, query: str) -> str:
        self.counter = self.counter+1
        if _prove_debug_enabled():
            print(f"=== ACTIVE LLM: {self.model_type} ===")
            llm_model = getattr(self.llm, "model", None) or getattr(self.llm, "model_name", None)
            if llm_model:
                print(f"=== ACTIVE LLM MODEL: {llm_model} ===")
        normalized = query.strip().lower()
        approval_tokens = {"ja", "yes", "beweis starten", "starte den beweis", "start proof"}
        direct_mode_hint = (
            "Wenn der Nutzer nur mit Ja/Nein beantwortet werden will oder keine Rueckfrage wuenscht, "
            "musst du das Tool prove verwenden und darfst nicht frei raten."
        )

        if self.pending_formal_blocks is not None:
            if normalized in approval_tokens:
                try:
                    direct_problem = _build_direct_fol_problem(self.pending_formal_blocks)
                    if direct_problem is None:
                        return "FAILURE\nDie zwischengespeicherte Uebersetzung enthaelt keine lesbaren Axiome/Ziel-Bloecke."
                    _debug_print_proof_text("FORMAL BRIDGE DIRECT PROBLEM", direct_problem)
                    verbose = _query_wants_verbose_details(self.pending_original_query or "")
                    result = _run_proof_text(direct_problem, verbose=verbose)
                    result = _decorate_failure_result(
                        result,
                        self.pending_formal_blocks,
                        self.pending_original_query or "",
                    )
                    if _query_wants_yes_no_answer(self.pending_original_query or ""):
                        yes_no = "Ja" if result.startswith("SUCCESS") else "Nein"
                        if verbose:
                            result = f"{result}\n{yes_no}"
                        else:
                            result = yes_no
                    self.pending_formal_blocks = None
                    self.pending_original_query = None
                    return result
                except ValueError as e:
                    self.pending_formal_blocks = None
                    self.pending_original_query = None
                    return f"FAILURE\nDie bestaetigte Uebersetzung konnte nicht verarbeitet werden: {e}"
            try:
                revised_blocks = _revise_formal_blocks(
                    self.llm,
                    self.pending_original_query or "",
                    self.pending_formal_blocks,
                    query,
                )
            except ValueError as e:
                self.pending_formal_blocks = None
                self.pending_original_query = None
                return (
                    "FAILURE\n"
                    "Die Korrektur der formalen Uebersetzung konnte nicht stabil verarbeitet werden: "
                    f"{e}"
                )
            self.pending_formal_blocks = revised_blocks
            warning = _detect_suspicious_axiom_goal_overlap(revised_blocks)
            warning_block = f"{warning}\n\n" if warning else ""
            return (
                "Hier ist die ueberarbeitete formale Uebersetzung:\n\n"
                f"```text\n{revised_blocks}\n```\n\n"
                f"{warning_block}"
                "Wenn das jetzt korrekt ist, antworte nur mit:\n"
                "`Ja`\n\n"
                "Dann fuehre ich den Beweis mit genau dieser Uebersetzung aus."
            )

        try:
            direct_problem = _build_direct_fol_problem(query)
        except ValueError as e:
            return f"FAILURE\nDirekter Formelpfad konnte die Eingabe nicht lesen: {e}"

        if direct_problem is not None:
            verbose = _query_wants_verbose_details(query)
            result = _run_proof_text(direct_problem, verbose=verbose)
            result = _decorate_failure_result(result, query, query)
            if _query_wants_yes_no_answer(query):
                yes_no = "Ja" if result.startswith("SUCCESS") else "Nein"
                if verbose:
                    return f"{result}\n{yes_no}"
                return yes_no
            return result

        try:
            translated_blocks = _translate_query_to_formal_blocks_only(self.llm, query)
        except ValueError as e:
            translated_blocks = (
                "FAILURE\n"
                "Natuerlichsprachlicher Formelpfad konnte die Anfrage nicht stabil "
                f"von der LLM-Ausgabe in einen beweisbaren Formelblock ueberfuehren: {e}"
            )

        if translated_blocks is not None:
            if translated_blocks.startswith("FAILURE\n"):
                return translated_blocks
            self.pending_formal_blocks = translated_blocks
            self.pending_original_query = query
            warning = _detect_suspicious_axiom_goal_overlap(translated_blocks)
            warning_block = f"{warning}\n\n" if warning else ""
            return (
                "Hier ist die formale Uebersetzung der Anfrage:\n\n"
                f"```text\n{translated_blocks}\n```\n\n"
                f"{warning_block}"
                "Wenn das so korrekt ist, antworte nur mit:\n"
                "`Ja`\n\n"
                "Dann fuehre ich den Beweis mit genau dieser Uebersetzung aus."
            )

        if self.counter == 1:
            messages = [
                SystemMessage(content=SYSTEM_PROMPT),
                SystemMessage(content=direct_mode_hint),
                HumanMessage(content=query),
            ]
        elif normalized in approval_tokens:
            messages = [
                SystemMessage(content="Die Bestaetigung wurde erteilt. Fuehre jetzt den Tool-Aufruf prove aus, falls der letzte Schritt die Rueckfrage nach der Logik war."),
                HumanMessage(content=query),
            ]
        else:
            messages = [
                SystemMessage(content=direct_mode_hint),
                HumanMessage(content=query),
            ]
        input_message = {"messages": messages}
        response = self.agent_executor.invoke(input_message, config=self.config)
        return response["messages"][-1].content

    def reset(self):
        self.counter = 0
        self.pending_formal_blocks = None
        self.pending_original_query = None
        import uuid
        self.config["configurable"]["thread_id"] = str(uuid.uuid4())
        print("(Conversation history has been reset.)")

if __name__ == "__main__":
    pass




