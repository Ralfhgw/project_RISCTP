// ---------------------------------------------------------------------------
// SMTLIB.java
// Translation of RISCTP language to SMT-LIB syntax.
// $Id: SMTLIB.java,v 1.30 2024/07/12 11:13:22 schreine Exp $
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
import risctp.fol.*;

public final class SMTLIB extends ASTVisitor.Base<Void>
{
  private final ProofProblem problem;
  private final boolean printAll;
  private final boolean ints;
  private final boolean arrays;
  private final PrintWriter writer;
  private final StringBuilder builder;
  
  /***************************************************************************
   * Create a visitor that prints the SMT-LIB syntax of (part of) a proof problem.
   * @param problem the proof problem
   * @param printAll if true print all declarations 
   *        (also those not needed/allowed by SMT-LIB)
   * @param ints true if the theory of ints is supported.
   * @param arrays true if the theory of arrays is supported.
   * @param writer the stream to print to.
   **************************************************************************/
  public SMTLIB(ProofProblem problem, boolean printAll,
    boolean ints, boolean arrays, PrintWriter writer)
  {
    this.problem = problem;
    this.printAll = printAll;
    this.ints = ints;
    this.arrays = arrays;
    this.writer = writer;
    this.builder = new StringBuilder();
  }
 
  /****************************************************************************
   * Print string to SMTLIB output medium.
   * @param string the string.
   ***************************************************************************/
  private void print(String string)
  {
    writer.print(string);
    builder.append(string);
    // System.out.print(string);
  }
  
  /****************************************************************************
   * Print string with new line to SMTLIB output medium.
   * @param string the string.
   ***************************************************************************/
  private void println(String string)
  {
    writer.println(string);
    builder.append(string);
    builder.append('\n');
    // System.out.println(string);
  }
  
  /****************************************************************************
   * Get the SMTLIB output produced so far.
   * @return the output.
   ***************************************************************************/
  public String getString()
  {
    return builder.toString();
  }
  
  /****************************************************************************
   * Print the complete proof problem.
   ***************************************************************************/
  public void print()
  {
    Problem.create(problem.decls).accept(this);
  }
  
  /****************************************************************************
   * Print prefix of an SMT-LIB script.
   ***************************************************************************/
  public void printPrefix()
  {
    println("(set-logic ALL)");
    println("(set-option :produce-unsat-cores true)");
  }
  
  /****************************************************************************
   * Print suffix of an SMT-LIB script.
   ***************************************************************************/
  public void printSuffix()
  {
    println("(exit)");
  }
  
  /****************************************************************************
   * Assume a formula.
   * @param formula the formula.
   ***************************************************************************/
  private void assume(Exp formula)
  {
    print("(assert ");
    formula.accept(this);
    println(")");
  }
  
  // set of words reserved in SMT-LIB
  private final static Set<String> reserved = new HashSet<String>();
  static
  {
    // reserved by SMT-LIB language
    reserved.add("BINARY"); 
    reserved.add("DECIMAL");
    reserved.add("HEXADECIMAL");
    reserved.add("NUMERAL");
    reserved.add("STRING");
    reserved.add("_");
    reserved.add("!");
    reserved.add("as");
    reserved.add("let");
    reserved.add("exists");
    reserved.add("forall");
    reserved.add("match");
    reserved.add("par");
    
    // reserved by SMT-LIB theory "Core"
    reserved.add("Bool"); 
    reserved.add("true");
    reserved.add("false");
    reserved.add("not");
    reserved.add("=>");
    reserved.add("and");
    reserved.add("or");
    reserved.add("xor");
    reserved.add("=");
    reserved.add("distinct");
    reserved.add("ite");
    
    // reserved by SMT-LIB theory "Ints"
    reserved.add("Int");
    reserved.add("-");
    reserved.add("+");
    reserved.add("*");
    reserved.add("div");
    reserved.add("mod");
    reserved.add("abs");
    reserved.add("<=");
    reserved.add("<");
    reserved.add(">=");
    reserved.add(">");
    
    // reserved by SMT-LIB theory "ArraysEx"
    reserved.add("Array");
    reserved.add("select");
    reserved.add("store");
    
    // reserved by Z3
    reserved.add("Seq");
    reserved.add("RegEx");
    reserved.add("FP");
  }
  
  /****************************************************************************
   * Get translation of a word to an allowed SMT-LIB word.
   * @param word the word.
   * @return a word that is allowed in SMT-LIB but does not occur in RISCTP
   ***************************************************************************/
  private static String translate(String word)
  {
    if (reserved.contains(word)) return "|§" + word + "§|";
    return word;
  }
 
  // --------------------------------------------------------------------------
  //
  // the visitor methods
  //
  // --------------------------------------------------------------------------
  
  public Void visit(Problem problem)
  {
    printPrefix();
    super.visit(problem);
    printSuffix();
    return null;
  }
  
  public Void visit(TypeDecl decl)
  {
    if (!printAll)
    {
      TypeSymbol type = (TypeSymbol)decl.id.getSymbol();
      if (type == problem.boolSymbol) return null;
      if (type == problem.intSymbol) return null;
      if (arrays && problem.mapTypes.contains(type)) return null;
    }
    if (decl.type == null)
    {
      print("(declare-sort ");
      decl.id.accept(this);
      println(" 0)");
      return null;
    }
    print("(define-sort ");
    decl.id.accept(this);
    print(" () ");
    decl.type.accept(this);
    println(")");
    return null;
  }
  
  public Void visit(DataType decl)
  {
    if (decl.items.length == 1)
    {
      DataTypeItem item = decl.items[0];
      if (item.id.getSymbol() != problem.boolSymbol)
      {
        print("(declare-datatype ");
        item.id.accept(this);
        print(" (");
        item.accept(this);
        println(" ))");
      }
      return null;
    }
    print("(declare-datatypes (par");
    for (DataTypeItem item : decl.items)
    {
      print(" ");
      item.id.accept(this);
    }
    print(")");
    for (DataTypeItem item : decl.items)
    {
      print(" (");
      item.accept(this);
      print(")");
    }
    println(")");
    return null;
  }
  
  public Void visit(Function decl)
  {
    FunctionSymbol fun = (FunctionSymbol)decl.id.getSymbol();
    if (!printAll)
    {
      if (problem.equalities.values().contains(fun)) return null;
      if (problem.inequalities.values().contains(fun)) return null;
      if (ints && problem.intFuns.contains(fun)) return null;
      if (arrays)
      {
        if (problem.mapSelectors.values().contains(fun)) return null;
        if (problem.mapStores.values().contains(fun)) return null;
      }
      if (problem.constructors.contains(fun)) return null;
      if (problem.selectors.contains(fun)) return null;
      // these have have indexed names in SMT-LIB and are treated specially below
      // if (problem.testers.contains(fun)) return null;
    }
    if (problem.testers.contains(fun))
    {
      print("(define-fun ");
      decl.id.accept(this);
      print(" (");
      TypedVar tvar = decl.tvars[0];
      print(" ");
      tvar.accept(this);
      print(" ");
      print(") ");
      decl.type.accept(this);
      print("( (_ is ");
      String name = decl.id.toString();
      // name has form 'is::tag' (including the '), have to extract tag
      int begin = 5;
      int end = name.length()-1;
      name = name.substring(begin, end);
      if (!AST.isSimple(name)) name = "|" + AST.unquote(name) + "|";
      print(name);
      print(") ");
      tvar.id.accept(this);
      println(" ))");
      return null;
    }
    if (decl.tvars.length == 0 && decl.exp == null)
    {
      declareConst(decl.id, decl.type);
      return null;
    }
    if (decl.exp == null)
    {
      print("(declare-fun ");
      decl.id.accept(this);
      print(" (");
      for (TypedVar tvar : decl.tvars)
      {
        print(" ");
        tvar.type.accept(this);
        print(" ");
      }
      print(") ");
      decl.type.accept(this);
      println(")");
      return null;
    }
    print("(define-fun ");
    decl.id.accept(this);
    print(" (");
    for (TypedVar tvar : decl.tvars)
    {
      print(" ");
      tvar.accept(this);
      print(" ");
    }
    print(") ");
    decl.type.accept(this);
    print(" ");
    decl.exp.accept(this);
    println(")");
    return null;
  }
  
  public void declareConst(Id id, Type type)
  {
    print("(declare-const ");
    id.accept(this);
    print(" ");
    type.accept(this);
    println(")");
  }
  
  public Void visit(Axiom decl)
  {
    if (!printAll)
    {
      FormulaSymbol formula = (FormulaSymbol)decl.id.getSymbol();
      if (ints && problem.intAxioms.contains(formula)) return null;
      if (arrays && problem.mapAxioms.contains(formula)) return null;
      if (problem.dataTypeAxioms.contains(formula)) return null;
    }
    assume(decl.formula);
    return null;
  } 
  
  public Void visit(Theorem decl)
  {
    openContext();
    checkTheorem(decl);
    closeContext();
    return null;
  }
  
  public void openContext()
  {
    println("(push 1)");
  }
  
  public void checkTheorem(Theorem decl)
  {
    assume(Not.create(decl.formula));
    checkSat();
  }
  
  public void checkSat()
  {
    println("(check-sat)");
  }

  public void getUnsatCore()
  {
    println("(get-unsat-core)");
  }

  public void getModel(List<String> names)
  {
    print("(get-value ( ");
    for (String name : names) 
    {
      if (!AST.isSimple(name)) name = "|" + AST.unquote(name) + "|";
      name = translate(name);
      print(name); 
      print(" ");
    }
    println("))");
  }
  
  public void closeContext()
  {
    println("(pop 1)");
  }
  
  public Void visit(Var var)
  {
    var.id.accept(this);
    return null;
  }
  
  public Void visit(Apply exp)
  {
    if (exp.exps.length == 0)
    {
      exp.id.accept(this);
      return null;
    }
    print("(");
    exp.id.accept(this);
    for (Exp exp0 : exp.exps)
    {
      print(" ");
      exp0.accept(this);
    }
    print(")");
    return null;
  }
  
  public Void visit(If exp)
  {
    print("(ite ");
    exp.exp1.accept(this);
    print(" ");
    exp.exp2.accept(this);
    print(" ");
    exp.exp3.accept(this);
    print(")");
    return null;
  }
  
  public Void visit(Let exp)
  {
    print("(let (");
    if (exp.binders.length > 0)
    {
      for (LetBinder binder : exp.binders)
      {
        print(" ");
        binder.accept(this);
      }
    }
    print(" ) ");
    exp.exp.accept(this);
    print(")");
    return null;
  }
  
  public Void visit(Match exp)
  {
    print("(match ");
    exp.exp.accept(this);
    print(" (");
    if (exp.binders.length > 0)
    {
      for (MatchBinder binder : exp.binders)
      {
        print(" ");
        binder.accept(this);
      }
    }
    print(" ))");
    return null;
  }
  
  public Void visit(Choose exp)
  {
    throw new RuntimeException("cannot translate " + exp + " to SMT-LIB");
  }
  
  public Void visit(False exp)
  {
    print("false");
    return null;
  }
  
  public Void visit(True exp)
  {
    print("true");
    return null;
  }
  
  public Void visit(Not exp)
  {
    print("(not ");
    exp.exp.accept(this);
    print(")");
    return null;
  }
  
  public Void visit(And exp)
  {
    print("(and ");
    exp.exp1.accept(this);
    print(" ");
    exp.exp2.accept(this);
    print(")");
    return null;
  }
  
  public Void visit(Or exp)
  {
    print("(or ");
    exp.exp1.accept(this);
    print(" ");
    exp.exp2.accept(this);
    print(")");
    return null;
  }
  
  public Void visit(Imp exp)
  {
    print("(=> ");
    exp.exp1.accept(this);
    print(" ");
    exp.exp2.accept(this);
    print(")");
    return null;
  }
  
  public Void visit(Equiv exp)
  {
    print("(= ");
    exp.exp1.accept(this);
    print(" ");
    exp.exp2.accept(this);
    print(")");
    return null;
  }
  
  public Void visit(Forall exp)
  {
    print("(forall (");
    exp.tvar.accept(this);
    print(") ");
    exp.exp.accept(this);
    print(")");
    return null;
  }
  
  public Void visit(Exists exp)
  {
    print("(exists (");
    exp.tvar.accept(this);
    print(") ");
    exp.exp.accept(this);
    print(")");
    return null;
  }
   
  private Void printVoid(String string)
  {
    print(string);
    return null;
  }
  
  // unary minus is overloaded
  private static final String UNARYMINUSNAME = AST.MINUSNAME + "§0";
  
  public Void visit(Id id)
  {
    Symbol symbol = id.getSymbol();
    id = symbol.id;
    if (symbol instanceof TypeSymbol)
    {
      TypeSymbol type = (TypeSymbol)symbol;
      if (type.equals(problem.boolSymbol)) return printVoid("Bool");
      if (type.equals(problem.intSymbol)) return printVoid("Int");
    }
    else if (symbol instanceof FunctionSymbol)
    {
      FunctionSymbol fun = (FunctionSymbol)symbol;
      if (fun == problem.trueValueSymbol) return printVoid("true");
      if (fun == problem.falseValueSymbol) return printVoid("false");
      if (problem.equalities.values().contains(fun)) return printVoid("=");
      if (problem.inequalities.values().contains(fun)) return printVoid("distinct");
      if (ints && problem.intFuns.contains(fun))
      {
        String name = AST.unquote(id.toString());
        switch (name)
        {
        case AST.MINUSNAME: return printVoid("-");
        case UNARYMINUSNAME: return printVoid("-");
        case AST.PLUSNAME: return printVoid("+");
        case AST.MULTNAME: return printVoid("*");
        case AST.DIVNAME: return printVoid("div");
        case AST.MODNAME: return printVoid("mod");
        case AST.LESSNAME: return printVoid("<");
        case AST.LESSEQUALNAME: return printVoid("<=");
        case AST.GREATERNAME: return printVoid(">");
        case AST.GREATEREQUALNAME: return printVoid(">=");
        }
      }
      if (arrays)
      {
        if (problem.mapSelectors.values().contains(fun)) return printVoid("select");
        if (problem.mapStores.values().contains(fun)) return printVoid("store");
      }
    }
    String name = id.toString();
    if (!AST.isSimple(name)) name = "|" + AST.unquote(name) + "|";
    name = translate(name);
    print(name); 
    return null;
  }

  public Void visit(Type type)
  {
    // print root type
    TypeSymbol symbol = (TypeSymbol)type.id.getSymbol();
    symbol = symbol.root;
    if (arrays && problem.mapTypes.contains(symbol))
    {
      print("(Array ");
      symbol.type.types[0].accept(this);
      print(" ");
      symbol.type.types[1].accept(this);
      print(")");
      return null;
    }
    symbol.id.accept(this);
    return null;
  }
  
  public Void visit(TypedVar tvar)
  {
    print("(");
    tvar.id.accept(this);
    print(" ");
    tvar.type.accept(this);
    print(")");
    return null;
  }
  
  public Void visit(LetBinder binder)
  {
    print("(");
    binder.id.accept(this);
    print(" ");
    binder.exp.accept(this);
    print(")");
    return null;
  }
  
  public Void visit(MatchBinder binder)
  {
    print("(");
    binder.pattern.accept(this);
    print(" ");
    binder.exp.accept(this);
    print(")");
    return null;
  }
  
  public Void visit(DefaultPattern pattern)
  {
    print("|§|");
    return null;
  }
  
  public Void visit(ConstrPattern pattern)
  {
    if (pattern.tvars.length == 0)
    {
      pattern.id.accept(this);
      return null;
    }
    print("(");
    pattern.id.accept(this);
    for (TypedVar tvar : pattern.tvars)
    {
      print(" ");
      tvar.id.accept(this);
    }
    print(")");
    return null;
  }
  
  public Void visit(DataTypeItem item)
  {
    for (DataTypeConstr constr : item.constrs) 
    {
      print(" ");
      constr.accept(this);
    }
    return null;
  }
  
  public Void visit(DataTypeConstr constr)
  {
    print("(");
    constr.id.accept(this);
    for (TypedVar tvar : constr.tvars)
    {
      print(" ");
      tvar.accept(this);
    }
    print(")");
    return null;
  }
  
  // --------------------------------------------------------------------------
  // translation of FOL clauses/literals/terms
  // --------------------------------------------------------------------------
  
  public void assume(String name, VariableSymbol[] vars, Object[] lits, boolean[] negs)
  {
    int varn = vars.length;
    print("(assert (! ");
    if (varn > 0)
    {
      print("(forall (");
      for (int i = 0; i < varn; i++)
        print(vars[i]);
      print(") ");
    }
    print("(or");
    int litn = lits.length;
    for (int i = 0; i < litn; i++)
    {
      print(" ");
      printLiteral(lits[i], negs[i]);
    }
    print(")");
    if (varn > 0) print(")");
    println(" :named " + name + "))");
  }

  private void print(VariableSymbol var)
  {
    print("(");
    var.id.accept(this);
    print(" ");
    var.tsymbol.type.accept(this);
    print(")");
  }
  
  public void assume(Object[] lits, boolean[] negs)
  {
    int n = lits.length;
    if (n == 1)
    {
      assume(lits[0], negs[0]);
      return;
    }
    print("(assert (or");
    for (int i = 0; i < n; i++)
    {
      print(" ");
      printLiteral(lits[i], negs[i]);
    }
    println("))");
  }
  
  public void assume(Object lit, boolean neg)
  {
    print("(assert ");
    printLiteral(lit, neg);
    println(")");
  }
  
  public void printLiteral(Object term, boolean neg)
  {
    if (neg) print("(not ");
    printTerm(term);
    if (neg) print(")");
  }
  
  private void printTerm(Object term)
  {
    Symbol symbol = Term.symbol(term);
    if (symbol instanceof VariableSymbol)
    {
      symbol.id.accept(this);
      return;
    }
    int n = Term.argnumber(term);
    if (n == 0)
    {
      symbol.id.accept(this);
      return;
    }
    if (problem.testers.contains(symbol))
    {
      // hack to deal with boolean testers
      String name = symbol.id.toString();
      if (name.equals("\'is::§true\'"))
      {
        printTerm(Term.argument(term, 0));
        return;
      }
      if (name.equals("\'is::§false\'"))
      {
        print("(not ");
        printTerm(Term.argument(term, 0));
        print(")");
        return;
      }
    }
    print("(");
    symbol.id.accept(this);
    for (int i = 0; i < n; i++)
    {
      Object arg = Term.argument(term, i);
      print(" ");
      printTerm(arg);
    }
    print(")");
  }
}
// ----------------------------------------------------------------------------
// end of file
// ----------------------------------------------------------------------------