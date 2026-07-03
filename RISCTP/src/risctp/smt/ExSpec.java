// ---------------------------------------------------------------------------
// SMTLIB.java
// Translation of RISCTP language to ExSpec (https://www.cs.cas.cz/~ratschan/exspec/)
// $Id: ExSpec.java,v 1.7 2024/07/04 11:33:39 schreine Exp $
//
// Author: Wolfgang Schreiner <Wolfgang.Schreiner@risc.jku.at>
// Copyright (C) 2022-, Research Institute for Symbolic Computation (RISC)
// Johannes Kepler University, Linz, Austria, https://www.risc.jku.at
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General public final License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General public final License for more details.
//
// You should have received a copy of the GNU General public final License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.
// ----------------------------------------------------------------------------
package risctp.smt;

import java.io.*;
import java.util.*;

import risctp.problem.*;
import risctp.syntax.*;
import risctp.syntax.AST.*;
import risctp.syntax.AST.Decl.*;
import risctp.syntax.AST.Exp.*;
import risctp.syntax.AST.Pattern.ConstrPattern;
import risctp.syntax.AST.Pattern.DefaultPattern;
import risctp.types.*;
import risctp.types.Symbol.*;

public class ExSpec extends ASTVisitor.Base<Void>
{
  // the proof problem 
  private final ProofProblem problem;
  
  // the output medium
  private final PrintWriter out;
  
  // the translations of special names
  private Map<String,String> names;
  
  // the declarations to be added to the formula to be checked
  private List<String> vars;

  // the axioms to be added to the formula to be checked
  private List<String> axioms;
  
  // are we in the context of a term?
  private boolean inTerm;
  
  // the content produced so far
  private StringBuilder builder;
  
  /***************************************************************************
   * Create a visitor that prints the ExSpec syntax of (part of) a proof problem.
   * @param problem the proof problem.
   * @param out the output medium.
   **************************************************************************/
  public ExSpec(ProofProblem problem, PrintWriter out)
  {
    this.problem = problem;
    this.out = out;
    this.names = new HashMap<String,String>();
    this.vars = new ArrayList<String>();
    this.axioms = new ArrayList<String>();
    this.inTerm = false;
    builder = new StringBuilder();
  }
  
  // get the content produced so far
  public String getText() { return builder.toString(); }
  
  // thrown if ExSpec does not support a RISCTP concept
  public static final class Unsupported extends RuntimeException
  {
    public static final long serialVersionUID = 18092023L;
    public Unsupported(String msg) { super(msg); }
  }
  
  /***************************************************************************
   * Throws an Unsupported runtime exception.
   * @param msg the message carried by the exception.
   * @return null
   **************************************************************************/
  private static String unsupported(String msg)
  {
    throw new Unsupported(msg);
  }
  
  // --------------------------------------------------------------------------
  //
  // auxiliaries
  //
  // --------------------------------------------------------------------------
  
  /****************************************************************************
   * Print string to SMTLIB output medium.
   * @param string the string.
   ***************************************************************************/
  private void print(String string)
  {
    out.print(string);
    builder.append(string);
  }
  
  /****************************************************************************
   * Print string with new line to SMTLIB output medium.
   * @param string the string.
   ***************************************************************************/
  private void println(String string)
  {
    out.println(string);
    builder.append(string);
    builder.append('\n');
  }
  
  // ExSpec keywords
  private final List<String> KEYWORDS = 
    Arrays.asList(new String[]{ "integer", "not", "forall", "exists", 
    // Haskell reserved identifiers: exspec applies lexer for Haskell language definition
    "case", "class", "data", "default", "deriving", "do", "else" ,
    "if", "import", "in", "infix", "infixl", "infixr", "instance" ,
    "let", "module", "newtype", "of", "then", "type", "where", "_" 
    });
  
  /****************************************************************************
   * Return true if variable name is understood by ExSpec.
   * @param name the name.
   * @return true if it is a variable name in the ExSpec language.
   ***************************************************************************/
  private boolean isExSpecName(String name)
  {
    if (KEYWORDS.indexOf(name) >= 0) return false;
    int n = name.length();
    if (n == 0) return false;
    char ch = name.charAt(0);
    if (!Character.isLetter(ch)) return false;
    for (int i = 0; i < n; i++)
    {
      ch = name.charAt(i);
      if (!Character.isLetterOrDigit(ch)) return false;
    }
    return true;
  }
  
  /****************************************************************************
   * Get ExSpec name corresponding to identifier.
   * @param id the identifier.
   * @return a version of the identifier understood by ExSpec.
   ***************************************************************************/
  private String getExSpecName(Id id)
  {
    String name = id.toString();
    if (AST.isNat(name)) return name;
    if (name.endsWith("_RISCTP"))
      unsupported("name '" + name + "' must not end with '_RISCTP'");
    if (isExSpecName(name)) return name;
    String name0 = names.get(name);
    if (name0 != null) return name0;
    int n = names.size();
    name0 = name + "_" + n + "_RISCTP";
    names.put(name, name0);
    return name0;
  }
  
  /****************************************************************************
   * Check whether type is Int.
   * @param tsymbol the symbol of the type.
   * @return true if the type is Int.
   ***************************************************************************/
  private boolean isIntType(TypeSymbol tsymbol)
  {
    TypeSymbol tsymbol0 = tsymbol.root;
    return tsymbol0 == problem.intSymbol;
  }
  
  /****************************************************************************
   * Check whether type is Map[Int,Int].
   * @param tsymbol the symbol of the type.
   * @return true if the type is Int.
   ***************************************************************************/
  private boolean isMapType(TypeSymbol tsymbol)
  {
    TypeSymbol tsymbol0 = tsymbol.root;
    Type type0 = tsymbol0.type;
    if (!type0.id.toString().equals(AST.MAPNAME)) return false;
    TypeSymbol tsymbol1 = (TypeSymbol)type0.types[0].id.getSymbol();
    if (!isIntType(tsymbol1)) return false;
    TypeSymbol tsymbol2 = (TypeSymbol)type0.types[1].id.getSymbol();
    if (!isIntType(tsymbol2)) return false;
    return true;
  }
  
  /****************************************************************************
   * Get the name of an ExSpecType.
   * @param tsymbol the symbol of the type.
   * @return the name (throws exception, if type is not a vali ExSpec type).
   ***************************************************************************/
  private String exSpecType(TypeSymbol tsymbol)
  {
    if (isIntType(tsymbol)) return "ℤ";
    if (isMapType(tsymbol)) return "ℤ→ℤ";
    unsupported("unsupported type " + tsymbol.type);
    return null;
  }
  
  /****************************************************************************
   * Get the ExSpec type of an identifier.
   * @param id an identifier denoting a variable, constant, or function.
   * @return the ExSpec version of its type.
   ***************************************************************************/
  private String getExSpecType(Id id)
  {
    Symbol symbol = id.getSymbol();
    if (symbol instanceof VariableSymbol)
    {
      VariableSymbol symbol0 = (VariableSymbol)symbol;
      return exSpecType(symbol0.tsymbol);
    }
    else if (symbol instanceof FunctionSymbol)
    {
      FunctionSymbol symbol0 = (FunctionSymbol)symbol;
      int n = symbol0.tsymbols.size();
      if (n == 0)
        return exSpecType(symbol0.tsymbol);
      if (n > 1)
        unsupported("function types of arity " + n + " are not supported");
      TypeSymbol tsymbol = symbol0.tsymbol;
      if (!isIntType(tsymbol))
        unsupported("function result type " + tsymbol.type + " is not supported");
      TypeSymbol tsymbol0 = symbol0.tsymbols.get(0);
      if (!isIntType(tsymbol0))
        unsupported("function argument type " + tsymbol0.type + " is not supported");
      return "ℤ→ℤ";
    }
    // cannot happen
    return null;
  }
  
  // unary minus is overloaded
  private static final String UNARYMINUSNAME = AST.MINUSNAME + "§0";

  /****************************************************************************
   * Returns name of an infix function.
   * @param id the function identifier.
   * @return the infix name (null if not infix).
   ***************************************************************************/
  private String getInfixName(Id id)
  {
    Symbol symbol = id.getSymbol();
    id = symbol.id;
    FunctionSymbol fun = (FunctionSymbol)symbol;
    if (problem.equalities.values().contains(fun)) return "=";
    if (problem.inequalities.values().contains(fun)) return "≠";
    if (problem.intFuns.contains(fun))
    {
      String name = AST.unquote(id.toString());
      switch (name)
      {
      case AST.MINUSNAME: return "-";
      case UNARYMINUSNAME: return "-";
      case AST.PLUSNAME: return "+";
      case AST.MULTNAME: unsupported("multiplication is not supported");
      case AST.DIVNAME: unsupported("division is not supported");
      case AST.MODNAME: unsupported("remainder is not supported");
      case AST.LESSNAME: return "<";
      case AST.LESSEQUALNAME: return "≤";
      case AST.GREATERNAME: return ">";
      case AST.GREATEREQUALNAME: return ">=";
      }
    }
    if (problem.mapSelectors.values().contains(fun)) return "()";
    if (problem.mapStores.values().contains(fun)) unsupported("map update is not supported");
    return null;
  }
  
  /****************************************************************************
   * Get string representation of an abstract syntax tree.
   * @param tree the abstract syntax tree.
   * @return its string representation.
   ***************************************************************************/
  private String getString(AST tree)
  {
    StringWriter swriter = new StringWriter();
    ExSpec translator = new ExSpec(problem, new PrintWriter(swriter, true));
    tree.accept(translator);
    return swriter.toString();
  }
  
  // --------------------------------------------------------------------------
  //
  // the visitor methods
  //
  // --------------------------------------------------------------------------
  
  public Void visit(Problem problem)
  {
    super.visit(problem);
    return null;
  }
  
  public Void visit(TypeDecl decl)
  {
    return null;
  }
  
  public Void visit(DataType decl)
  {
    return null;
  }
  
  public Void visit(Function decl)
  {
    FunctionSymbol fun = (FunctionSymbol)decl.id.getSymbol();
    if (problem.equalities.values().contains(fun)) return null;
    if (problem.inequalities.values().contains(fun)) return null;
    if (problem.intFuns.contains(fun)) return null;
    if (problem.mapSelectors.values().contains(fun)) return null;
    if (problem.mapStores.values().contains(fun)) return null;
    if (problem.constructors.contains(fun)) return null;
    if (problem.selectors.contains(fun)) return null;
    if (problem.testers.contains(fun)) return null;
    Id id = decl.id;
    String name = getExSpecName(id);
    String type = getExSpecType(id);
    vars.add(name + ":" + type);
    if (decl.exp == null) return null;
    String exp0 = getString(decl.exp);
    int n = decl.tvars.length;
    if (n == 0)
    {
      axioms.add(name + " = " + exp0);
      return null;
    }
    String axiom = "∀";
    String names0 = "";
    for (int i = 0; i < n; i++)
    {
      Id id0 = decl.tvars[i].id;
      String name0 = getExSpecName(id0);
      String type0 = getExSpecType(id0);
      if (i > 0) { axiom += ","; names0 += ","; }
      axiom += name0 + ":" + type0;
      names0 += name0;
    }
    axiom += ". " + name + "(" + names0 + ") = " + exp0;
    axioms.add(axiom);
    return null;
  }

  public Void visit(Axiom decl)
  {
    FormulaSymbol formula = (FormulaSymbol)decl.id.getSymbol();
    if (problem.intAxioms.contains(formula)) return null;
    if (problem.mapAxioms.contains(formula)) return null;
    if (problem.dataTypeAxioms.contains(formula)) return null;
    String formula0 = getString(decl.formula);
    axioms.add(formula0);
    return null;
  } 
  
  public Void visit(Theorem decl)
  {
    int n = vars.size();
    for (int i = 0; i < n; i++)
    {
      if (i > 0) print(",");
      print(vars.get(i));
    }
    println(". ");
    n = axioms.size();
    if (n > 0)
    {
      if (n > 1) print("[");
      for (int i = 0; i < n; i++)
      {
        if (i > 0 ) print(" ∧ ");
        print(axioms.get(i));
      }
      if (n > 1) print("]");
      print(" ⇒ ");
    }
    print("¬[");
    decl.formula.accept(this);
    println("]"); // terminating newline required by ExSpec
    return null;
  }
  
  public Void visit(Var var)
  {
    var.id.accept(this);
    return null;
  }
  
  private void term(Exp exp)
  {
    boolean inTerm0 = inTerm;
    inTerm = true;
    exp.accept(this);
    inTerm = inTerm0;
  }
  
  public Void visit(Apply exp)
  {
    Id id = exp.id;
    if (exp.exps.length == 0)
    {
      id.accept(this);
      return null;
    }
    String OPEN = inTerm ? "(" : "[";
    String CLOSE = inTerm ? ")" : "]";
    String infix = getInfixName(exp.id);
    if (infix != null)
    {
      if (exp.exps.length == 1)
      {
        print(OPEN + infix);
        term(exp.exps[0]);
        print(CLOSE);
        return null;
      }
      if (exp.exps.length == 2)
      {
        if (infix.equals("()"))
        {
          term(exp.exps[0]);
          print("(");
          term(exp.exps[1]);
          print(")");
        }
        else
        {
          print(OPEN); 
          term(exp.exps[0]);
          print(" " + infix + " ");
          term(exp.exps[1]);
          print(CLOSE);
        }
        return null;
      }
    }
    id.accept(this);
    if (exp.exps.length == 0) return null;
    print(OPEN);
    int i = 0;
    for (Exp exp0 : exp.exps)
    {
      if (i > 0) print(", ");
      term(exp0);
      i++;
    }
    print(CLOSE);
    return null;
  }
  
  public Void visit(If exp)
  {
    if (inTerm) unsupported("conditional terms are not supported");
    print("[[");
    exp.exp1.accept(this);
    print(" ⇒");
    exp.exp2.accept(this);
    print("] ∧ [¬");
    exp.exp1.accept(this);
    print("] ⇒");
    exp.exp3.accept(this);
    print("]");
    return null;
  }
  
  public Void visit(Let exp)
  {
    unsupported("let binders are not supported");
    return null;
  }
  
  public Void visit(Match exp)
  {
    unsupported("match expressions are not supported");
    return null;
  }
  
  public Void visit(Choose exp)
  {
    unsupported("choose expressions are not supported");
    return null;
  }
  
  public Void visit(False exp)
  {
    if (inTerm) unsupported("formulas within terms are not supported");
    print("[0 = 1]");
    return null;
  }
  
  public Void visit(True exp)
  {
    if (inTerm) unsupported("formulas within terms are not supported");
    print("[0 = 0]");
    return null;
  }
  
  public Void visit(Not exp)
  {
    if (inTerm) unsupported("formulas within terms are not supported");
    print("[¬ ");
    exp.exp.accept(this);
    print("]");
    return null;
  }
  
  public Void visit(And exp)
  {
    if (inTerm) unsupported("formulas within terms are not supported");
    print("[");
    exp.exp1.accept(this);
    print(" ∧ ");
    exp.exp2.accept(this);
    print("]");
    return null;
  }
  
  public Void visit(Or exp)
  {
    if (inTerm) unsupported("formulas within terms are not supported");
    print("[");
    exp.exp1.accept(this);
    print(" ∨ ");
    exp.exp2.accept(this);
    print("]");
    return null;
  }
  
  public Void visit(Imp exp)
  {
    if (inTerm) unsupported("formulas within terms are not supported");
    print("[");
    exp.exp1.accept(this);
    print(" ⇒ ");
    exp.exp2.accept(this);
    print("]");
    return null;
  }
  
  public Void visit(Equiv exp)
  {
    if (inTerm) unsupported("formulas within terms are not supported");
    print("[[");
    exp.exp1.accept(this);
    print(" ⇒ ");
    exp.exp2.accept(this);
    print("] ∧ [");
    exp.exp2.accept(this);
    print(" ⇒ ");
    exp.exp1.accept(this);
    print("]]");
    return null;
  }
  
  public Void visit(Forall exp)
  {
    if (inTerm) unsupported("formulas within terms are not supported");
    print("[∀");
    exp.tvar.accept(this);
    print(". ");
    exp.exp.accept(this);
    print("]");
    return null;
  }
  
  public Void visit(Exists exp)
  {
    if (inTerm) unsupported("formulas within terms are not supported");
    print("[∃");
    exp.tvar.accept(this);
    print(". ");
    exp.exp.accept(this);
    print("]");
    return null;
  }
  
  public Void visit(Id id)
  {
    String name = getExSpecName(id);
    print(name); 
    return null;
  }

  public Void visit(Type type)
  {
    return null;
  }
  
  public Void visit(TypedVar tvar)
  {
    print("");
    print(getExSpecName(tvar.id));
    print(":");
    print(getExSpecType(tvar.id));
    return null;
  }
  
  public Void visit(LetBinder binder)
  {
    return null;
  }
  
  public Void visit(MatchBinder binder)
  {
    return null;
  }
  
  public Void visit(DefaultPattern pattern)
  {
    return null;
  }
  
  public Void visit(ConstrPattern pattern)
  {
    return null;
  }
  
  public Void visit(DataTypeItem item)
  {
    return null;
  }
  
  public Void visit(DataTypeConstr constr)
  {
    return null;
  }
}
//----------------------------------------------------------------------------
//end of file
//----------------------------------------------------------------------------