from exp import Exp
from scanner import scan
from parser import parseProblem
from checker import check_problem, CheckException
from risctp import problemstr
from executor import execute, start, stop

import atexit
import tempfile, os

RISCTP_PATH='../RISCTP/etc/RISCTP'
Z3_PATH='../RISCTP/etc/z3'

INTERACTIVE_CMD = ['-web', '9999', '1']
RISCTP_SMT_CMD = [RISCTP_PATH, \
  '-method', 'smt', '-solver', 'z3', '-path', Z3_PATH]
RISCTP_MESON_CMD = [RISCTP_PATH, '-method', 'meson']
  
RISCTP_CMD = RISCTP_SMT_CMD
INTERACTIVE_URL = 'http://localhost:9999/'
_interactive_process = None
_interactive_spec_path = None
def _cleanup_interactive_session()->None:
    global _interactive_process, _interactive_spec_path
    if _interactive_process is not None:
        stop(_interactive_process)
        _interactive_process = None
    if _interactive_spec_path and os.path.exists(_interactive_spec_path):
        os.remove(_interactive_spec_path)
    _interactive_spec_path = None
atexit.register(_cleanup_interactive_session)

def prove(fol_text:str,interactive:bool=False)->tuple[bool,str]:
    """
    Given the text of a proof problem in FOL-PRE syntax, apply RISCTP 
    to the proof problem, either interactively or automatically 
    (depending on the second parameter). In the automatic mode, the
    function returns 'True' if the proof could be performed and 'False' otherwise, 
    in both cases together with the output produced by the prover.
    In the interactive mode, the result announces the local RISCTP web URL.
    """
    global _interactive_process, _interactive_spec_path
    result:str = ''
    try:
        tokens: list(str) = scan(fol_text)
        problem: Exp = parseProblem(tokens)
        check_problem(problem)
        risctp_text = problemstr(problem)
        spec_path = create_persistent_temp_file(risctp_text)
        risctp_cmd = RISCTP_CMD.copy()
        if interactive:
            risctp_cmd += INTERACTIVE_CMD
        risctp_cmd += [ spec_path ]
        if interactive:
            _cleanup_interactive_session()
            risctp_proc = start(risctp_cmd)
            if risctp_proc == None:
                os.remove(spec_path)
                return False, 'could not start RISCTP process'
            _interactive_process = risctp_proc
            _interactive_spec_path = spec_path
            return True, f'interactive RISCTP session started; open {INTERACTIVE_URL} manually'
        else:
            code, stdout = execute(risctp_cmd)
            os.remove(spec_path)
            return (code == 0), stdout
    except ValueError as e:
        result = 'error in scanning/parsing text: ' + str(e)
    except CheckException as e:
        result = 'error in type-checking problem: ' + str(e)
    except Exception as e:
        result = 'internal error: ' + str(e)
    return False, result


def create_persistent_temp_file(content:str)->str:
    # delete=False ensures the file stays on disk after .close()
    # mode='w+' allows us to write text and read if needed
    with tempfile.NamedTemporaryFile(mode='w+', delete=False, encoding='utf-8') as temp:
        temp.write(content)
        temp_path = temp.name
    return temp_path

'''
# --- Usage Example ---
file_path = create_persistent_temp_file("This is some secret text for later.")

# The file is closed now, but it still exists!
# You can pass 'file_path' to your 'execute' function or other methods.
with open(file_path, 'r') as f:
    print(f"Re-opening file: {f.read()}")

# IMPORTANT: Since delete=False, you are responsible for cleaning it up
# os.remove(file_path)
'''

if __name__ == '__main__':
    fol_text = """
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
    code, stdout = prove(fol_text,True)
    print(code)
    print(stdout)
