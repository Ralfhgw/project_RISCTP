import re
from typing import List, Final

def scan(text: str) -> List[str]:
    """
    Scans the input text and decomposes it into a list of tokens:
    '[', ']', ',', tags (ASCII letters), and identifiers ("quoted strings").
    """
    # Define regex patterns for each token type using named groups
    # Tag: one or more ASCII letters
    # Identifier: double quote, any non-quote chars, closing double quote
    token_specification: Final[List[tuple[str, str]]] = [
        ('BRACKET_OPEN',  r'\['),
        ('BRACKET_CLOSE', r'\]'),
        ('COMMA',         r','),
        ('TAG',           r'[a-zA-Z]+'),
        ('IDENTIFIER',    r'"[^"]*"'),
        ('SKIP',          r'[ \t\n\r]+'),  # Whitespace to be ignored
        ('MISMATCH',      r'.'),           # Any other character (error)
    ]
    
    # Combine patterns into a single regex
    tok_regex: str = '|'.join(f'(?P<{name}>{pattern})' for name, pattern in token_specification)
    line_num: int = 1
    line_start: int = 0
    tokens: List[str] = []

    for mo in re.finditer(tok_regex, text):
        kind: str | None = mo.lastgroup
        value: str = mo.group()
        column: int = mo.start() - line_start + 1

        if kind == 'SKIP':
            # Update line count if the skipped whitespace contains newlines
            if '\n' in value:
                line_num += value.count('\n')
                line_start = mo.end() - (value[::-1].find('\n'))
            continue
        elif kind == 'MISMATCH':
            # Find the full text of the current line for the error message
            lines = text.splitlines()
            current_line = lines[line_num - 1] if line_num <= len(lines) else ""
            
            error_msg = (
                f"\n[Error] Illegal character '{value}' at line {line_num}, column {column}\n"
                f"Line {line_num}: {current_line}\n"
                f"{' ' * (len(str(line_num)) + 8 + column - 1)}^"
            )
            print(error_msg)
            raise ValueError(f"Scanning error at line {line_num}: unexpected character {value!r}")
        else:
            # For all valid tokens, append the literal string found
            tokens.append(value)
            
            # Handle newlines that might be inside identifiers (if allowed by your spec)
            if '\n' in value:
                line_num += value.count('\n')
                line_start = mo.end() - (value[::-1].find('\n'))

    return tokens

# --- Example Usage ---
if __name__ == "__main__":
    sample_text = """
    forall["x","A",imp[pred["p",var["x"]],
      exists["y","B",pred["q",var["x"],var["y"]]]]]
    """
    try:
        result = scan(sample_text)
        print(f"Tokens found: {result}")
    except ValueError as e:
        pass