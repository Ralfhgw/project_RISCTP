from typing import List, Tuple, Literal, Optional, Final, TypeAlias, Union

from exp import Exp
from scanner import scan

# Define the argument type for the Exp class
ExpArg: TypeAlias = Union[str, 'Exp']

# Define the return type for auxiliary functions: (new_position, expression)
ParseResult: TypeAlias = Tuple[int, Optional['Exp']]

def _report_error(tokens: List[str], p: int) -> None:
    """Prints the error sublist and raises an exception."""
    sublist: List[str] = tokens[p:]
    error_msg: str = f"Parsing error at position {p}. Remaining tokens: {sublist}"
    print(error_msg)
    raise ValueError(error_msg)

# --- Term Parser ---

def _aux_parse_term(tokens: List[str], p: int) -> ParseResult:
    if p >= len(tokens) or tokens[p] not in ("var", "fun", "const"):
        return -1, None
    
    tag: str = "fun" if tokens[p] == "const" else tokens[p]
    # Expecting: tag [ "name" ... ]
    if p + 2 >= len(tokens) or tokens[p+1] != "[" or not tokens[p+2].startswith('"'):
        return -1, None
    
    name: str = tokens[p+2].strip('"')
    args: List[ExpArg] = [name]
    curr: int = p + 3

    while curr < len(tokens) and tokens[curr] == ",":
        curr += 1
        next_p, sub_term = _aux_parse_term(tokens, curr)
        if next_p == -1 or sub_term is None:
            return -1, None
        args.append(sub_term)
        curr = next_p

    if curr < len(tokens) and tokens[curr] == "]":
        return curr + 1, Exp(tag=tag, args=args) # type: ignore
    
    return -1, None

def parseTerm(tokens: List[str]) -> Exp:
    pos, res = _aux_parse_term(tokens, 0)
    if pos != len(tokens) or res is None:
        _report_error(tokens, max(0, pos))
    return res

# --- Formula Parser ---

def _aux_parse_formula(tokens: List[str], p: int) -> ParseResult:
    if p >= len(tokens):
        return -1, None
    
    tag: str = tokens[p]
    
    # 1. Atomic: true, false
    if tag in ("true", "false"):
        if p + 2 < len(tokens) and tokens[p+1] == "[" and tokens[p+2] == "]":
            return p + 3, Exp(tag=tag, args=[]) # type: ignore
        return -1, None

    # 2. Predicate: pred["str", term, ...]
    if tag == "pred":
        if p + 2 < len(tokens) and tokens[p+1] == "[" and tokens[p+2].startswith('"'):
            args: List[ExpArg] = [tokens[p+2].strip('"')]
            curr = p + 3
            while curr < len(tokens) and tokens[curr] == ",":
                curr += 1
                next_p, term = _aux_parse_term(tokens, curr)
                if next_p == -1 or term is None: return -1, None
                args.append(term)
                curr = next_p
            if curr < len(tokens) and tokens[curr] == "]":
                return curr + 1, Exp(tag="pred", args=args) # type: ignore

    # 3. Unary: not[formula]
    if tag == "not":
        if p + 1 < len(tokens) and tokens[p+1] == "[":
            next_p, f = _aux_parse_formula(tokens, p + 2)
            if next_p != -1 and f and next_p < len(tokens) and tokens[next_p] == "]":
                return next_p + 1, Exp(tag="not", args=[f]) # type: ignore

    # 4. Binary: and, or, imp, iff [f1, f2]
    if tag in ("and", "or", "imp", "iff"):
        if p + 1 < len(tokens) and tokens[p+1] == "[":
            p1, f1 = _aux_parse_formula(tokens, p + 2)
            if p1 != -1 and f1 and p1 < len(tokens) and tokens[p1] == ",":
                p2, f2 = _aux_parse_formula(tokens, p1 + 1)
                if p2 != -1 and f2 and p2 < len(tokens) and tokens[p2] == "]":
                    return p2 + 1, Exp(tag=tag, args=[f1, f2]) # type: ignore

    # 5. Quantifiers: forall, exists ["str", "str", formula]
    if tag in ("forall", "exists"):
        try:
            if tokens[p+1] == "[" and tokens[p+2].startswith('"') and tokens[p+3] == "," \
               and tokens[p+4].startswith('"') and tokens[p+5] == ",":
                v1, v2 = tokens[p+2].strip('"'), tokens[p+4].strip('"')
                next_p, f = _aux_parse_formula(tokens, p + 6)
                if next_p != -1 and f and next_p < len(tokens) and tokens[next_p] == "]":
                    return next_p + 1, Exp(tag=tag, args=[v1, v2, f]) # type: ignore
        except IndexError: pass

    return -1, None

def parseFormula(tokens: List[str]) -> Exp:
    pos, res = _aux_parse_formula(tokens, 0)
    if pos != len(tokens) or res is None:
        _report_error(tokens, max(0, pos))
    return res

# --- Problem Parser ---

def _aux_parse_decl(tokens: List[str], p: int) -> ParseResult:
    if p >= len(tokens): return -1, None
    tag: str = tokens[p]
    
    # typedecl, fundecl, preddecl (sequences of "id")
    if tag in ("typedecl", "fundecl", "preddecl"):
        if p + 2 < len(tokens) and tokens[p+1] == "[" and tokens[p+2].startswith('"'):
            args: List[ExpArg] = [tokens[p+2].strip('"')]
            curr = p + 3
            while curr < len(tokens) and tokens[curr] == ",":
                if curr + 1 < len(tokens) and tokens[curr+1].startswith('"'):
                    args.append(tokens[curr+1].strip('"'))
                    curr += 2
                else: return -1, None
            if curr < len(tokens) and tokens[curr] == "]":
                return curr + 1, Exp(tag=tag, args=args) # type: ignore

    # axiom, goal ["id", formula]
    if tag in ("axiom", "goal"):
        if p + 3 < len(tokens) and tokens[p+1] == "[" and tokens[p+2].startswith('"') and tokens[p+3] == ",":
            id_val: str = tokens[p+2].strip('"')
            next_p, f = _aux_parse_formula(tokens, p + 4)
            if next_p != -1 and f and next_p < len(tokens) and tokens[next_p] == "]":
                return next_p + 1, Exp(tag=tag, args=[id_val, f]) # type: ignore

    return -1, None

def _aux_parse_problem(tokens: List[str], p: int) -> ParseResult:
    if p >= len(tokens) or tokens[p] != "problem":
        return -1, None
    if p + 2 >= len(tokens) or tokens[p+1] != "[" or not tokens[p+2].startswith('"'):
        return -1, None
    
    args: List[ExpArg] = [tokens[p+2].strip('"')]
    curr: int = p + 3
    
    while curr < len(tokens) and tokens[curr] == ",":
        curr += 1
        next_p, decl = _aux_parse_decl(tokens, curr)
        if next_p == -1 or decl is None: return -1, None
        args.append(decl)
        curr = next_p
        
    if curr < len(tokens) and tokens[curr] == "]":
        return curr + 1, Exp(tag="problem", args=args) # type: ignore
    return -1, None

def parseProblem(tokens: List[str]) -> Exp:
    pos, res = _aux_parse_problem(tokens, 0)
    if pos != len(tokens) or res is None:
        _report_error(tokens, max(0, pos))
    return res
    
# --- Example Usage ---
if __name__ == "__main__":
    text = """
problem["SupermanExistence",
typedecl["Entity"],
preddecl["superman", "Entity"],
preddecl["able_to_prevent_evil", "Entity"],
preddecl["willing_to_prevent_evil", "Entity"],
preddecl["prevents_evil", "Entity"],
preddecl["impotent", "Entity"],
preddecl["malevolent", "Entity"],
axiom["A1",
forall["x", "Entity",
imp[and[pred["superman", var["x"]], and[pred["able_to_prevent_evil", var["x"]], pred["willing_to_prevent_evil", var["x"]]]],
pred["prevents_evil", var["x"]]]]],
axiom["A2",
forall["x", "Entity",
imp[and[pred["superman", var["x"]], not[pred["able_to_prevent_evil", var["x"]]]],
pred["impotent", var["x"]]]]],
axiom["A3",
forall["x", "Entity",
imp[and[pred["superman", var["x"]], not[pred["willing_to_prevent_evil", var["x"]]]],
pred["malevolent", var["x"]]]]],
axiom["A4",
forall["x", "Entity",
imp[pred["superman", var["x"]],
not[pred["prevents_evil", var["x"]]]]]],
axiom["A5",
forall["x", "Entity",
imp[pred["superman", var["x"]],
and[not[pred["impotent", var["x"]]], not[pred["malevolent", var["x"]]]]]]],
goal["SupermanDoesNotExist",
not[exists["x", "Entity", pred["superman", var["x"]]]]]
]
 """
    try:
        print(text)
        tokens = scan(text)
        # print(tokens)
        formula = parseProblem(tokens)
        print(formula)
    except ValueError as e:
        pass
      