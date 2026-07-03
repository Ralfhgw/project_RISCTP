from exp import Exp, ExpArg

# ----------------------------------------------------------------------------
# conversion of expressions to RISCTP syntax
# ----------------------------------------------------------------------------

BUILTIN_TYPES: list[str] = [ 'Int', 'ℤ', 'Nat', 'ℕ']
UNARY_FUNOPS: list[str] = [ '-' ]
BINARY_FUNOPS: list[str] = [ '+', '-', '*', '/' ]
UNARY_PREDOPS: list[str] = [ ]
BINARY_PREDOPS: list[str] = [ '=', '≠', '<', '<=', '≤', '>', '>=', '≥' ] 

UNDERSCORE: int = ord('_')
DIGIT_0: int = ord('0')
DIGIT_9: int = ord('9')
LOWER_A: int = ord('a')
LOWER_Z: int = ord('z')
UPPER_A: int = ord('A')
UPPER_Z: int = ord('Z')

def isdigit(code: int)->bool:
    return (DIGIT_0 <= code and code <= DIGIT_9)
def isletter(code: int)->bool:
    return (LOWER_A <= code and code <= LOWER_Z) or \
           (UPPER_A <= code and code <= UPPER_Z)
      
def idstr(name: str)->str:
    if plain(name):
      return name;
    return "'" + "".join([ clean(ch) for ch in name ]) + "'"
def plain(name: str)->bool:
    assert len(name) >= 1
    first: int = ord(name[0])
    if first != UNDERSCORE and not isletter(first):
      return False
    for ch in name[1:]:
      code: int = ord(ch)
      if (not isletter(code)) and (not isdigit(code)):
        return False
    return True
def clean(ch: str)->str:
    assert len(ch) == 1
    if ch in [ "'", "\\", "§" ]:
      return '"' + str(ord(ch)) + '"'
    return ch

def varstr(name: str)->str:
    return idstr('_' + name)
def parens(text: str)->str:
    return '(' + text + ')'
    
def problemstr(exp: Exp)->str:
    '''
    Return string representation of proof problem in RISCTP syntax
    '''
    assert exp.tag == 'problem'
    args: list[ExpArg] = exp.args
    n: int = len(args)
    assert n >= 1
    result : str = '// === problem ' + args[0] + " ===\n"
    for decl in args[1:]:
      result += declstr(decl)
    result += "// === end of problem ===\n"
    return result
   
def declstr(exp: Exp)->str:
    '''
    Return string representation of declaration in RISCTP syntax
    '''
    args: list[ExpArg] = exp.args
    n: int = len(args)
    assert n >= 1
    name: str = args[0]
    match exp.tag:
        case 'typedecl':
            result: str = '// ' if name in BUILTIN_TYPES else ''
            result += 'type ' + idstr(name) + ';\n'
            return result
        case 'fundecl':
            assert n >= 2;
            result: str = '// ' \
                if name in UNARY_FUNOPS or name in BINARY_FUNOPS else ''
            if n == 2:
                result += 'const ' + idstr(name) + ':' + idstr(args[1]) + ';\n'
            else:
                result += 'fun ' + idstr(name) + '(' +  sigstr(args[1:]) \
                        + '):' + idstr(args[n-1]) + ';\n'
            return result
        case 'preddecl':
            result: str = '// ' \
                if name in UNARY_PREDOPS or name in BINARY_PREDOPS else ''
            if n == 1:
                result += 'pred ' + idstr(name) + ';\n'
            else:
                result += 'pred ' + idstr(name) + '(' + sigstr(args[1:]) + ');\n'
            return result
        case 'axiom':
            assert n == 2
            return 'axiom ' + idstr(name) + ' ⇔ ' + formulastr(args[1]) + ';\n';
        case 'goal':
            assert n == 2
            return 'theorem ' + idstr(name) + ' ⇔ ' + formulastr(args[1]) + ';\n';    
        case _:
            assert False, "unknown kind of declaration"

def sigstr(args: list[ExpArg])->str:
    n: int = len(args)
    result: str = ''
    for i, arg in enumerate(args):
        astr: str = arg
        result += 'x' + str(i) + ':' + idstr(astr)
        if i+1 < n:
            result += ','
    return result

def formulastr(exp: Exp)->str:
    '''
    Return string representation of formula in RISCTP syntax
    '''
    args: list[ExpArg] = exp.args
    n: int = len(args)
    match exp.tag:
        case 'true':
            return 'true'
        case 'false':
            return 'false'
        case 'pred':
            return atomicstr(exp)
        case 'not':
            assert n == 1
            return parens('¬' + formulastr(args[0]))
        case 'and':
            assert n == 2
            return parens(formulastr(args[0]) + '∧' + formulastr(args[1]))
        case 'or':
            assert n == 2
            return parens(formulastr(args[0]) + '∨' + formulastr(args[1]))
        case 'imp':
            assert n == 2
            return parens(formulastr(args[0]) + '⇒' + formulastr(args[1]))    
        case 'iff':
            assert n == 2
            return parens(formulastr(args[0]) + '⇔' + formulastr(args[1]))   
        case 'forall':
            assert n == 3
            vstr: str = varstr(args[0])
            tstr: str = idstr(args[1])
            fstr: str = formulastr(args[2])
            return parens('∀' + vstr + ':' + tstr + '.' + fstr)   
        case 'exists':
            assert n == 3
            vstr: str = varstr(args[0])
            tstr: str = idstr(args[1])
            fstr: str = formulastr(args[2])
            return parens('∃' + vstr + ':' + tstr + '.' + fstr)  
        case _:
            assert False, 'unknown kind of formula'

def atomicstr(exp: Exp)->str:
    '''
    Return string representation of atomic formula in RISCTP syntax
    '''
    args: list[ExpArg] = exp.args
    n: int = len(args)
    assert n >= 1
    name: str = args[0]
    assert exp.tag == 'pred'
    if n == 1:
        return name
    if n == 2 and name in UNARY_PREDOPS:
        arg1: str = termstr(args[1])
        return parens(name + arg1)
    if n == 3 and name in BINARY_PREDOPS:
        arg1: str = termstr(args[1])
        arg2: str = termstr(args[2])
        return parens(arg1 + name + arg2)
    result: str = name + '(';
    for i, arg in enumerate(args[1:]):
      result += termstr(arg)
      if i+2 < n:
        result += ','
    result += ')';
    return result
    
def termstr(exp: Exp)->str:
    '''
    Return string representation of term in RISCTP syntax
    '''
    args: list[ExpArg] = exp.args
    n: int = len(args)
    assert n >= 1
    name: str = args[0]
    if exp.tag == 'var':
      assert n == 1
      return varstr(name)
    assert exp.tag == 'fun'
    if n == 1:
        return idstr(name)
    if n == 2 and name in UNARY_FUNOPS:
        arg1: str = termstr(args[1])
        return parens(name + arg1)
    if n == 3 and name in BINARY_FUNOPS:
        arg1: str = termstr(args[1])
        arg2: str = termstr(args[2])
        return parens(arg1 + name + arg2)
    result: str = name + '(';
    for i, arg in enumerate(args[1:]):
      result += termstr(arg)
      if i+2 < n:
        result += ','
    result += ')';
    return result
    
if __name__ == '__main__':
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
    print(problemstr(problem))
