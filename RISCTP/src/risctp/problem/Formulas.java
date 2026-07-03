// ---------------------------------------------------------------------------
// Formulas.java
// Separate in expressions terms from formulas.
// $Id: Formulas.java,v 1.11 2024/06/06 17:23:56 schreine Exp $
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
import java.util.function.*;

import risctp.*;
import risctp.syntax.AST.*;
import risctp.syntax.AST.Exp.*;
import risctp.syntax.AST.Decl.*;
import risctp.types.Symbol.*;

public class Formulas 
{
  /****************************************************************************
   * Separate in expressions terms from formulas.
   * @param problem a proof problem.
   * @return an equivalent problem with formulas not appearing as terms.
   ***************************************************************************/
  public static ProofProblem process(ProofProblem problem)
  {
    Formulas cloner = new Formulas(problem);
    return cloner.process();
  }
    
  // the proof problem to be processed/returned
  private ProofProblem problem;
  private ProofProblem result;
  
  // the equality on booleans
  private FunctionSymbol boolEqual;
  
  private Formulas(ProofProblem problem)
  {
    this.problem = problem;
    result = new ProofProblem(problem);
    result.decls = new ArrayList<Decl>(result.decls.size());
    boolEqual = problem.equalities.get(problem.boolSymbol);
  }
   
  /****************************************************************************
   * Process proof problem assuming that the only occurrences of expressions
   * are in axioms and theorems.
   * @return the processed proof problem.
   ***************************************************************************/
  private ProofProblem process()
  {
    for (Decl decl : problem.decls)
    {
      if (decl instanceof Axiom)
      {
        Axiom decl0 = (Axiom)decl;
        Exp formula = processFormula(decl0.formula);
        Axiom decl1 = Axiom.create(decl0.id, formula);
        Id fid = decl0.getFunId();
        Type[] ftypes = decl0.getFunTypes();
        decl1.setFunction(fid, ftypes);
        result.decls.add(decl1);
        continue;
      }
      if (decl instanceof Theorem)
      {
        Theorem decl0 = (Theorem)decl;
        Exp formula = processFormula(decl0.formula);
        Theorem decl1 = Theorem.create(decl0.id, formula);
        result.decls.add(decl1);
        continue;
      }
      result.decls.add(decl);
    }
    return result;
  }

  // --------------------------------------------------------------------------
  //
  // formulas
  //
  // --------------------------------------------------------------------------
  
  private Exp processFormula(Exp exp) 
  { 
    // we assume Choose, Let, Match have been already eliminated
    if (exp instanceof True || exp instanceof False) 
      return exp;
    if (exp instanceof Var)
    {
      FunctionSymbol pred = problem.testerMap.get(problem.trueValueSymbol);
      List<Exp> args = new ArrayList<Exp>(1);
      args.add(exp);
      return Apply.create(pred.id, args);
    }
    if (exp instanceof If) 
    {
      // if elimination is simple on the formula level
      If exp0 = (If)exp;
      Exp exp1 = processFormula(exp0.exp1);
      Exp exp2 = processFormula(exp0.exp2);
      Exp exp3 = processFormula(exp0.exp3);
      return And.create(
          Imp.create(exp1, exp2), 
          Imp.create(Not.create(exp1), exp3));
    }
    // predicate applications
    if (exp instanceof Apply) 
    {
      Apply exp0 = (Apply)exp;
      FunctionSymbol pred = (FunctionSymbol)exp0.id.getSymbol();
      if (pred != boolEqual) return processFormula(exp0);
      return processFormula(exp0.exps[0], exp0.exps[1], 
          (Exp exp1,Exp exp2)->Equiv.create(exp1, exp2));
    }
    // unary formulas
    if (exp instanceof Not) 
    {
      Not exp0 = (Not)exp;
      return processFormula(exp0.exp, (Exp exp1)->Not.create(exp1));  
    }
    if (exp instanceof Forall) 
    {
      Forall exp0 = (Forall)exp;
      return processFormula(exp0.exp, (Exp exp1)->Forall.create(exp0.tvar, exp1));  
    }
    if (exp instanceof Exists) 
    {
      Exists exp0 = (Exists)exp;
      return processFormula(exp0.exp, (Exp exp1)->Exists.create(exp0.tvar, exp1));  
    }
    // binary formulas
    if (exp instanceof And) 
    {
      And exp0 = (And)exp;
      return processFormula(exp0.exp1, exp0.exp2, (Exp exp1,Exp exp2)->And.create(exp1, exp2));
    }
    if (exp instanceof Or)       
    {
      Or exp0 = (Or)exp;
      return processFormula(exp0.exp1, exp0.exp2, (Exp exp1,Exp exp2)->Or.create(exp1, exp2));
    }
    if (exp instanceof Imp) 
    {
      Imp exp0 = (Imp)exp;
      return processFormula(exp0.exp1, exp0.exp2, (Exp exp1,Exp exp2)->Imp.create(exp1, exp2));
    }
    if (exp instanceof Equiv) 
    {
      Equiv exp0 = (Equiv)exp;
      return processFormula(exp0.exp1, exp0.exp2, (Exp exp1,Exp exp2)->Equiv.create(exp1, exp2));
    }
    Main.getMain().error("unknown expression " + exp, true);
    return null; 
  }
  
  private Exp processFormula(Exp exp, UnaryOperator<Exp> op)
  {
    Exp result = processFormula(exp);
    return op.apply(result);
  }
  
  private Exp processFormula(Exp exp1, Exp exp2, BinaryOperator<Exp> op)
  {
    Exp result1 = processFormula(exp1);
    Exp result2 = processFormula(exp2);
    return op.apply(result1, result2);
  }
  
  private Exp processFormula(Apply exp)
  {
    List<CondExp> result = processTerm(exp);
    return flattenFormula(result);
  }
  
  private Exp flattenFormula(List<CondExp> cexps)
  {
    Exp result = null;
    for (CondExp cexp : cexps)
      result = Exp.and(result, Exp.imp(cexp.cond, cexp.exp));
    return result;
  }
  
  // --------------------------------------------------------------------------
  //
  // terms
  //
  // --------------------------------------------------------------------------
  
  // a conditional expression, a pair of a condition and an expression
  private static class CondExp
  {
    public final Exp cond; // may be null (no condition)
    public final Exp exp;
    public CondExp(Exp cond, Exp value)
    {
      this.cond = cond;
      this.exp = value;
    }
  }
  
  private List<CondExp> processTerm(Exp exp)
  {
    // we assume Choose, Let, Match have been already eliminated
    if (exp instanceof Var)
    {
      List<CondExp> result = new ArrayList<CondExp>(1);
      result.add(new CondExp(null, exp));
      return result;
    }
    else if (exp instanceof Apply)
    {
      Apply exp0 = (Apply)exp;
      FunctionSymbol symbol = (FunctionSymbol)exp0.id.getSymbol();
      if (symbol == problem.trueValueSymbol || symbol ==  problem.falseValueSymbol) 
      {
        List<CondExp> result = new ArrayList<CondExp>(1);
        result.add(new CondExp(null, exp));
        return result;
      }
      if (symbol.tsymbol.root != problem.boolSymbol)
      {
        return processTerm((Apply)exp);
      }
    }
    else if (exp instanceof If)
      return processTerm((If)exp);
    
    // formula appears as term
    Exp exp0 = processFormula(exp);
    List<Exp> args = new ArrayList<Exp>(0);
    return processTerm(If.create(exp0, 
        Apply.create(problem.trueValueSymbol.id, args), 
        Apply.create(problem.falseValueSymbol.id, args)));
  }
  
  private List<CondExp> processTerm(If exp)
  {
    Exp exp1 = processFormula(exp.exp1);
    List<CondExp> cexps2 = processTerm(exp.exp2);
    List<CondExp> cexps3 = processTerm(exp.exp3);
    List<CondExp> result = new ArrayList<CondExp>();
    for (CondExp cexp2 : cexps2)
    {
      Exp cond = Exp.and(exp1, cexp2.cond);
      result.add(new CondExp(cond, cexp2.exp));
    }
    exp1 = Not.create(exp1);
    for (CondExp cexp3 : cexps3)
    {
      Exp cond = Exp.and(exp1, cexp3.cond);
      result.add(new CondExp(cond, cexp3.exp));
    }
    return result;
  }
  
  private List<CondExp> processTerm(Apply exp)
  {
    Id id = exp.id;
    boolean notGoal = exp.isNotGoal();
    Apply atom = Apply.create(id, new ArrayList<Exp>(0));
    atom.setNotGoal(notGoal);
    List<CondExp> result = new ArrayList<CondExp>();
    result.add(new CondExp(null, atom));
    for (Exp exp0 : exp.exps)
    {
      List<CondExp> cexps0 = processTerm(exp0);
      result = combine(result, cexps0, (CondExp cexp1, CondExp cexp2)->
      {
        Apply exp1 = (Apply)cexp1.exp;
        List<Exp> args = new ArrayList<Exp>(Arrays.asList(exp1.exps));
        args.add(cexp2.exp);
        Exp cond = Exp.and(cexp1.cond, cexp2.cond);
        Apply atom0 = Apply.create(id, args);
        atom0.setNotGoal(notGoal);
        return new CondExp(cond, atom0);
      });
    }
    return result;
  }
  
  // --------------------------------------------------------------------------
  //
  // auxiliaries
  //
  // --------------------------------------------------------------------------
  
  /****************************************************************************
   * Combine every element of list1 with every element of list2.
   * @param list1 the first list.
   * @param list2 the second list.
   * @param op the operation for combining values from the lists.
   * @return the combined list.
   ***************************************************************************/
  private static <T1,T2,R> List<R> combine(List<T1> list1, 
    List<T2> list2, BiFunction<T1,T2,R> op)
  {
    List<R> result = new ArrayList<R>(list1.size()*list2.size());
    for (T1 val1 : list1)
    {
      for (T2 val2 : list2)
      {
        R val = op.apply(val1, val2);
        result.add(val);
      }
    }
    return result;
  }
}
// ----------------------------------------------------------------------------
// end of file
// ----------------------------------------------------------------------------
