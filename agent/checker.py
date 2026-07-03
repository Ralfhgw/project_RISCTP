from exp import Exp, ExpArg

# ----------------------------------------------------------------------------
# type-checking expressions
# ----------------------------------------------------------------------------

# a signature (function/predicate name + argument types)
Sig = tuple[str,...]

# an environment 
tenv: set[str]       # set of declared types
fenv: dict[Sig, str] # mapping of constant/function signatures to result types
penv: set[Sig]       # set of predicate signatures
venv: list[tuple[str, str]] # stack of variable/type pairs
agenv: set[str]      # set of axiom/goal formulas

class CheckException(Exception):
   def __init__(self, msg:str):
       super(CheckException, self).__init__(msg)

def check(cond: bool, msg: str, exp: (ExpArg|None)=None)->None:
    if cond:
        return
    estr: str = ': ' + (str(exp) if exp is not None else '')
    raise CheckException('ERROR (' + msg + ')' + estr)
    
def check_problem(problem: Exp)->None:
    '''
    check a problem, returns normally if everything is fine,
    otherwise raises an exception
    '''
    global tenv, fenv, penv, venv, agenv
    tenv = set()
    fenv = { }
    penv = set()
    venv = [ ]
    agenv = set()
    args: list[ExpArg] = problem.args
    n: int = len(args)
    check(n >= 1, 'illformed problem', problem)
    name = getname(args[0])
    for decl in problem.args[1:]:
        check(isinstance(decl,Exp), 'illformed declaration', decl)
        match decl.tag:
            case 'typedecl':
                check_typedecl(decl)
            case 'fundecl':
                check_fundecl(decl)
            case 'preddecl':
                check_preddecl(decl)
            case 'axiom':
                check_axiomgoal(decl)
            case 'goal':
                check_axiomgoal(decl)
            case _:
                check(False, 'unknown kind of declaration', decl)
                
def check_typedecl(decl: Exp)->None:
    global tenv
    name: str = declgetname(decl)
    check(not istype(name), 'doubly declared type', decl)
    tenv.add(name)
def check_fundecl(decl: Exp)->None:
    global fenv
    sig, rtype = funsig(decl)
    check(not isfun(sig), 'doubly declared function', decl)
    fenv[sig] = rtype 
def check_preddecl(decl: Exp)->None:
    global penv
    sig: Sig = predsig(decl)
    check(not ispred(sig), 'doubly declared predicate', decl)
    penv.add(sig) 
def check_axiomgoal(decl: Exp)->None:
    global agenv
    name: str = declgetname(decl)
    check(not isaxiomgoal(name), 'doubly declared axiom/goal', decl)
    formula: Exp = axiomgoalformula(decl)
    check_formula(formula)
    agenv.add(name)
    
def declgetname(decl: Exp)->str:
    args: list[ExpArg] = decl.args
    n: int = len(args)
    check(n >= 1, 'illformed declaration', decl)
    return getname(args[0])
def getname(exp: ExpArg)->str:
    check(isinstance(exp, str), 'not an identifier', exp)
    return exp
def funsig(decl: Exp)->tuple[Sig,str]:
    args: list[ExpArg] = decl.args
    n: int = len(args)
    check(n >= 2, 'illformed function declaration', decl)
    for arg in args:
       check(isinstance(arg, str), 'illformed function declaration', decl)
    for arg in args[1:]:
       check(istype(arg), 'undeclared type', arg)
    return tuple(args[:-1]),args[-1]
def predsig(decl: Exp)->Sig:
    args: list[ExpArg] = decl.args
    n: int = len(args)
    check(n >= 1, 'illformed predicate declaration', decl)
    for arg in args:
       check(isinstance(arg, str), 'illformed predicate declaration', decl)
    for arg in args[1:]:
       check(istype(arg), 'undeclared type', arg)
    return tuple(args)
def axiomgoalformula(decl: Exp)->Exp:
    args: list[ExpArg] = decl.args
    n: int = len(args)
    check(n == 2, 'illformed axiom/goal declaration', decl)
    check(isinstance(args[1],Exp), 'illformed axiom/goal declaration', decl)
    return args[1]

def istype(name: str)->bool:
    global tenv
    return name in tenv
def isfun(sig: Sig)->bool:
    global fenv
    return sig in fenv
def ispred(sig: Sig)->bool:
    global penv
    return sig in penv
def isaxiomgoal(name: str)->bool:
    global agenv
    return name in agenv

def check_formula(formula: Exp)->None:
    '''
    check a formula, returns normally if everything is fine,
    otherwise raises an exception
    '''
    args: List[ExpArg] = formula.args
    n: int = len(args)
    match formula.tag:
        case 'true' | 'false':
            check(n == 0, "illformed truth literal", formula)
        case 'pred':
            check_atomic(formula)
        case 'not':
            check(n == 1, "invalid application of unary connective", formula)
            check_argformula(args[0])
        case 'and' | 'or' | 'imp' | 'iff':
            check(n == 2, "invalid application of binary connective", formula)
            check_argformula(args[0])
            check_argformula(args[1])
        case 'forall' | 'exists':
            check(n == 3, "invalid quantifier application", formula)
            vname: str = getname(args[0])
            tname: str = getname(args[1])
            check(istype(tname), "unknown type", args[1])
            venv.append((vname,tname))
            check_argformula(args[2])
            venv.pop()
        case _:
            check(False, "unknown kind of formula", formula)
            
def check_argformula(exp: ExpArg)->None:
    check(isinstance(exp, Exp), 'illformed formula', exp)
    check_formula(exp)

def check_atomic(formula: Exp)->None:
    args: List[ExpArg] = formula.args
    n: int = len(args)
    check(n >= 1, "illformed atomic formula", formula)
    pname: str = getname(args[0])
    atypes: list[str] = [ check_term(a) for a in args[1:] ]
    sig: Sig = tuple([ pname ] + atypes)
    check(ispred(sig), "unknown predicate or ill-typed application", formula)

def check_term(term: ExpArg)->str:
    check(isinstance(term, Exp), "illformed term", term)
    args: List[ExpArg] = term.args
    n: int = len(args)
    match term.tag:
        case 'var':
            check(n == 1, "illformed variable occurrence", term)
            vname: str = getname(args[0])
            tname = vtype(vname)
            check(tname is not None, "undeclared variable", term)
            return tname
        case 'fun':
            check(n >= 1, "illformed function application", term)
            fname: str = getname(args[0])
            atypes: list[str] = [ check_term(a) for a in args[1:] ]
            sig: Sig = tuple([ fname ] + atypes)
            check(isfun(sig), "unknown function or ill-typed application", term)
            rtype: str = ftype(sig)
            return rtype
        case _:
            check(False, "unknown kind of term", term)    

def vtype(name: str)->(str|None):
    global venv
    for vname,tname in reversed(venv):
        if vname == name:
            return tname;
    return None

def ftype(sig: Sig)->str:
    global fenv
    return fenv[sig]

if __name__ == '__main__':
    try:
        exp0: Exp = Exp('var', [ 'x' ])
        exp1: Exp = Exp('fun', [ 'c' ])
        exp2: Exp = Exp('fun', [ 'f' , exp0, exp1])
        exp3: Exp = Exp('pred', [ 'p', exp2 ])
        exp4: Exp = Exp('pred', [ '>', Exp('fun', [ 'd' ]), Exp('fun', [ 'd' ]) ])
        exp5: Exp = Exp('and', [ exp3, exp4 ])
        exp6: Exp = Exp('forall', [ 'x', 'T', exp5 ])
        idecl: Exp = Exp('typedecl', [ 'ℤ' ])
        tdecl: Exp = Exp('typedecl', [ 'T' ])
        cdecl: Exp = Exp('fundecl', [ 'c', 'T' ]) 
        ddecl: Exp = Exp('fundecl', [ 'd', 'ℤ' ]) 
        fdecl: Exp = Exp('fundecl', [ 'f', 'T', 'T', 'T' ])
        pdecl: Exp = Exp('preddecl', [ 'p', 'T' ]) 
        gdecl: Exp = Exp('preddecl', [ '>', 'ℤ', 'ℤ' ]) 
        goal: Exp = Exp('goal', [ 'G', exp6 ])
        problem: Exp = Exp('problem', [ 'P', idecl, tdecl, cdecl, ddecl, \
        fdecl, pdecl, gdecl, goal ])
        check_problem(problem)
        print('SUCCESS')
    except CheckException as e:
        print(problem)
        print(e)
