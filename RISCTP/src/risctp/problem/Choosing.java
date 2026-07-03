// ---------------------------------------------------------------------------
// Choosing.java
// Get rid of choose expressions by applying axiomatized functions.
// $Id: Choosing.java,v 1.21 2025/02/25 10:05:37 schreine Exp $
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
package risctp.problem;

import java.util.*;

import risctp.syntax.*;
import risctp.syntax.AST.*;
import risctp.syntax.AST.Decl.*;
import risctp.syntax.AST.Exp.*;
import risctp.types.Symbol;
import risctp.types.Symbol.*;

public class Choosing extends ASTCloner
{
  /****************************************************************************
   * Get rid of choose expressions by applying axiomatized functions.
   * @param problem a proof problem.
   * @return an equivalent problem without choose expressions.
   ***************************************************************************/
  public static ProofProblem process(ProofProblem problem)
  {
    Choosing cloner = new Choosing(problem);
    return cloner.process();
  }
  
  // the proof problem to be processed
  private ProofProblem problem;
 
  // the generated proof problem
  private ProofProblem result;
  
  // a map of choose axiom strings to choose functions
  private Map<String,FunctionSymbol> chooseMap;
  
  private Choosing(ProofProblem problem)
  {
    super(false); // do not clone ids
    this.problem = problem;
    this.result = new ProofProblem(problem);
    result.decls = new ArrayList<Decl>();
    this.chooseMap = new HashMap<String,FunctionSymbol>();
  }
  
  /****************************************************************************
   * Process the current proof problem.
   * @return the proof problem after resolving overloading.
   ***************************************************************************/
  private ProofProblem process()
  {
    for (Decl decl : problem.decls)
    {
      Decl decl0 = (Decl)decl.accept(this);
      result.decls.add(decl0);
    }
    return result;
  }
  
  public Apply visit(Choose exp)
  {
    // process base expression
    Exp exp0 = (Exp)exp.exp.accept(this);
    exp = Choose.create(exp.kind, exp.tvar, exp0);
    
    // make variable names canonical
    exp = processChoose(exp);
    exp0 = exp.exp;

    // determine the bound variables of the expression
    Set<VariableSymbol> bound = BoundVars.compute(exp);
    Set<String> bnames = new HashSet<String>();
    for (VariableSymbol bvar : bound) bnames.add(bvar.id.toString());
    
    // determine the free variables of the expression
    Set<VariableSymbol> free = FreeVars.compute(exp);
    int n = free.size();
    
    // determine the parameter names that replace the free variables
    // in order to make the expression as canonical as possible
    int counter = 0;
    String[] names = new String[n];
    for (int i = 0; i < n; i++)
    {
      String name;
      while (true)
      {
        counter++;
        name = "_param" + counter;
        if (!bnames.contains(name)) break;
      }
      names[i] = name;
    }

    // create the list of parameters
    FreeVarSubst subst = new FreeVarSubst();
    List<TypeSymbol> types = new ArrayList<TypeSymbol>(n);
    List<TypedVar> params = new ArrayList<TypedVar>(n);
    List<Exp> args = new ArrayList<Exp>(n);
    List<Exp> args0 = new ArrayList<Exp>(n);
    int vnum = 0;
    for (VariableSymbol var : free)
    {
      Id id = var.id;
      Id id0 = Id.create(names[vnum]);
      vnum++;
      VariableSymbol vsymbol = (VariableSymbol)id.getSymbol();
      TypeSymbol tsymbol = var.tsymbol;
      VariableSymbol vsymbol0 = new VariableSymbol(id0, tsymbol);
      id0.setSymbol(vsymbol0);
      types.add(tsymbol);
      params.add(TypedVar.create(id0, tsymbol.type));
      Exp vexp = Var.create(id);
      Exp vexp0 = Var.create(id0);
      args.add(vexp);
      args0.add(vexp0);
      subst.substitute(vsymbol, vexp0);
    }
    
    // substitute the variables in the expression by the parameters
    exp0 = subst.apply(exp0);
    
    // perhaps there exists already a choose function for the axiom
    // (which defines the result value uniquely)
    String axstring = null;
    if (exp.kind == Choose.Kind.DEF)
    {
      axstring = exp0.toString();
      FunctionSymbol fun = chooseMap.get(axstring);
      if (fun != null) return Apply.create(fun.id, args);
    }
    
    // create function declaration
    VariableSymbol var = (VariableSymbol)exp.tvar.id.getSymbol();
    Id fid = Id.create("choose" + AST.internal(Integer.toString(result.decls.size())));
    FunctionSymbol fsymbol = new FunctionSymbol(fid, types, var.tsymbol);
    fid.setSymbol(fsymbol);
    Function function = Function.create(fid, params, var.tsymbol.type, null);
    result.decls.add(function);
    List<FormulaSymbol> typeTheorems = null;
    if (exp.kind != Choose.Kind.ANY) 
    {
      typeTheorems = new ArrayList<FormulaSymbol>();
      result.typeTheoremMap.put(fsymbol, typeTheorems);
    }

    // assert universal existence of chosen value by a type-checking theorem 
    if (exp.kind == Choose.Kind.SAT || exp.kind == Choose.Kind.DEF)
    {
      Exp formula = Exists.create(exp.tvar, exp0);
      for (int i = n-1; i >= 0; i--)
        formula = Forall.create(params.get(i), formula);
      Id thid = Id.create("choosesat" + AST.internal(Integer.toString(result.decls.size())));
      FormulaSymbol thsymbol = new FormulaSymbol(thid, false);
      thid.setSymbol(thsymbol);
      Theorem theorem = Theorem.create(thid, formula);
      result.decls.add(theorem);
      result.typeTheorems.add(thsymbol);
      typeTheorems.add(thsymbol);
    }
    
    // assert uniqueness of chosen value by a type-checking theorem
    if (exp.kind == Choose.Kind.UNI || exp.kind == Choose.Kind.DEF)
    {
      String name1 = exp.tvar.id + AST.internal(Integer.toString(chooseMap.size()));
      TypedVar[] tvar1 = new TypedVar[] { exp.tvar };
      Exp[] exp1 = new Exp[] { exp0 };
      rename(name1, tvar1, exp1);
      TypeSymbol tsymbol1 = (TypeSymbol)exp.tvar.type.id.getSymbol();
      FunctionSymbol equality1 = problem.equalities.get(tsymbol1.root);
      Exp formula1 = Apply.create(equality1.id, Arrays.asList(
          Apply.create(exp.tvar.id, new ArrayList<Exp>()), 
          Apply.create(tvar1[0].id, new ArrayList<Exp>())));
      formula1 = Imp.create(And.create(exp0, exp1[0]), formula1);
      formula1 = Forall.create(exp.tvar, Forall.create(tvar1[0], formula1));
      for (int i = n-1; i >= 0; i--) formula1 = Forall.create(params.get(i), formula1);
      Id thid1 = Id.create("chooseunique" + AST.internal(Integer.toString(result.decls.size())));
      FormulaSymbol thsymbol1 = new FormulaSymbol(thid1, false);
      thid1.setSymbol(thsymbol1);
      Theorem theorem1 = Theorem.create(thid1, formula1);
      ASTCloner cloner = new ASTCloner(false);
      theorem1 = (Theorem)theorem1.accept(cloner);
      result.decls.add(theorem1);
      result.typeTheorems.add(thsymbol1);
      typeTheorems.add(thsymbol1);
    }
    
    // create axiom
    List<LetBinder> binders = new ArrayList<LetBinder>(1);
    binders.add(LetBinder.create(exp.tvar.id, Apply.create(fid, args0)));
    Exp formula = Let.create(binders, exp0);
    formula = Imp.create(Exists.create(exp.tvar, exp0), formula);
    ListIterator<TypedVar> it = params.listIterator(n);
    while (it.hasPrevious()) formula = Forall.create(it.previous(), formula);
    
    Id aid = Id.create("choose" + AST.internal(Integer.toString(result.decls.size())));
    FormulaSymbol asymbol = new FormulaSymbol(aid, true);
    aid.setSymbol(asymbol);
    Axiom axiom = Axiom.create(aid, formula);
    result.decls.add(axiom);

    // register association of axiom to function
    result.chooseMap.put(asymbol, fsymbol);
    if (exp.kind == Choose.Kind.DEF) chooseMap.put(axstring, fsymbol);
    
    // create function application
    return Apply.create(fid, args);
  }

  /***************************************************************************
   * Give variable in expression canonical name.
   * @param exp a choose expression.
   * @return a logically equivalent expression.
   **************************************************************************/
  private Choose processChoose(Choose exp)
  {
    Exp body = exp.exp;
    if (body instanceof Forall) body = processForall((Forall)body);
    TypedVar[] tvar0 = new TypedVar[] { exp.tvar };
    Exp[] exp0 = new Exp[] { body };
    rename("_cvar", tvar0, exp0);
    return Choose.create(exp.kind, tvar0[0], exp0[0]);
  }
  
  /***************************************************************************
   * Give variable in expression a canonical name.
   * @param exp a forall expression.
   * @return a logically equivalent expression.
   **************************************************************************/
  private Forall processForall(Forall exp)
  {
    TypedVar[] tvar0 = new TypedVar[] { exp.tvar };
    Exp[] exp0 = new Exp[] { exp.exp };
    rename("_qvar", tvar0, exp0);
    return Forall.create(tvar0[0], exp0[0]);
  }
  
  /****************************************************************************
   * Give typed variable in expression a canonical name.
   * @param name the name.
   * @param tvar the typed variable.
   * @param exp the expression in which this variable may occur freely.
   * The contents of tvar and exp are updated.
   ***************************************************************************/
  private static void rename(String name, TypedVar[] tvar, Exp[] exp)
  {
    Set<VariableSymbol> bound = BoundVars.compute(exp[0]);
    for (VariableSymbol bvar : bound)
    {
      if (name.equals(bvar.id.toString())) return;
    }
    Id id = tvar[0].id;
    VariableSymbol vsymbol = (VariableSymbol)id.getSymbol();
    TypeSymbol tsymbol = vsymbol.tsymbol;
    Id id0 = Id.create(name);
    VariableSymbol vsymbol0 = new VariableSymbol(id0, tsymbol);
    id0.setSymbol(vsymbol0);
    FreeVarSubst subst = new FreeVarSubst();
    subst.substitute(vsymbol, Var.create(id0));
    // update the transient parameters
    tvar[0] = TypedVar.create(id0, tvar[0].type);
    exp[0] = subst.apply(exp[0]);  
  }
  
  // a class that allows to substitute in expressions free variables by terms
  private static class FreeVarSubst extends ASTCloner
  {
    // the replacement map
    private Map<VariableSymbol,Exp> map;
    
    /**************************************************************************
     * Create an empty substitution.
     *************************************************************************/
    public FreeVarSubst()
    {
      super(false);
      map = new HashMap<VariableSymbol,Exp>();
    }
    
    /*************************************************************************
     * Extend substitution by a variable/term pair
     * @param var the variable.
     * @param exp the term.
     ************************************************************************/
    public void substitute(VariableSymbol var, Exp exp)
    {
      map.put(var, exp);
    }
    
    /**************************************************************************
     * Apply the substitution to an expression.
     * @param exp the expression.
     * @return the expression after the substitution.
     *************************************************************************/
    public Exp apply(Exp exp)
    {
      return (Exp)exp.accept(this);
    }
    
    /**************************************************************************
     * The overridden visitor methods.
     *************************************************************************/
    public Exp visit(Var exp)
    {
      VariableSymbol var = (VariableSymbol)exp.id.getSymbol();
      Exp exp0 = map.get(var);
      if (exp0 != null) return exp0;
      return exp;
    }
    public Exp visit(Apply exp)
    {
      Symbol symbol = exp.id.getSymbol();
      if (!(symbol instanceof VariableSymbol)) return super.visit(exp);
      VariableSymbol var = (VariableSymbol)symbol;
      Exp exp0 = map.get(var);
      if (exp0 != null) return exp0;
      return exp;
    }
  }
}
// ----------------------------------------------------------------------------
// end of file
// ----------------------------------------------------------------------------