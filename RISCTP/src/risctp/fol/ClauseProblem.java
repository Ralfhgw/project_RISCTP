// ---------------------------------------------------------------------------
// ClauseProblem.java
// The clausal form of a proof problem.
// $Id: ClauseProblem.java,v 1.17 2024/07/08 08:17:02 schreine Exp $
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
package risctp.fol;

import java.util.*;
import java.io.*;

import risctp.problem.*;
import risctp.syntax.AST.*;
import risctp.syntax.AST.Decl.*;
import risctp.syntax.AST.Exp.*;
import risctp.types.Symbol.*;
import risctp.fol.ClauseTransformer.*;

public class ClauseProblem
{
  /****************************************************************************
   * Transform a proof problem into clause problems.
   * @param problem the proof problem.
   * @return the clause problems
   ***************************************************************************/
  public static List<ClauseProblem> transform(ProofProblem problem)
  {
    List<ClauseProblem> result = new ArrayList<ClauseProblem>();
    for (Decl decl : problem.decls)
    {
      if (!(decl instanceof Theorem)) continue;
      Theorem theorem = (Theorem) decl;
      ClauseProblem cproblem = new ClauseProblem(problem, theorem);
      result.add(cproblem);
    }
    if (result.isEmpty())
    {
      // there is no theorem, so the axioms must be inconsisten
      ClauseProblem cproblem = new ClauseProblem(problem, null);
      result.add(cproblem);
    }
    return result;
  }
  
  private ProofProblem problem; // the original problem
  private List<Decl> decls;     // the declarations of all symbols in clauses
  private List<Clause> clauses; // the list of clauses derived from the problem

  /****************************************************************************
   * Transform a proof problem into a clause problem.
   * @param problem the proof problem.
   * @param theorem the theorem in the problem to be transformed (may be null)
   ***************************************************************************/
  private ClauseProblem(ProofProblem problem, Theorem theorem)
  {
    this.problem = problem;
    this.decls = new ArrayList<Decl>();
    this.clauses = new ArrayList<Clause>();
    transform(theorem);
  }
  
  // get the elements of the clause problem
  public ProofProblem getProofProblem() { return problem; }
  public List<Decl> getDeclarations() { return decls; }
  public List<Clause> getClauses() { return clauses; }
  
  /****************************************************************************
   * Print the proof problem.
   * @param out the output medium.
   * @param decl if true, also the declarations are printed
   * @param goal if true, the "goal" clauses are printed separately in negated form.
   * @param prefix the format for the quantifier prefix of clause formulas.
   * @param matrix the format for the matrix of clause formulas.
   ***************************************************************************/
  public void print(PrintWriter out, boolean decl, boolean goal,
    Clause.PrefixFormat prefix, Clause.MatrixFormat matrix)
  {
    if (decl)
    {
      for (Decl decl0 : decls) 
        out.println(decl0.toString());
      out.println();
    }
    int counter = 1;
    for (Clause clause : clauses)
    {
      if (goal && clause.isGoal()) continue;
      out.print(counter + ":");
      String name = clause.getName();
      if (name != null) out.print("[" + name + "]");
      out.print(" ");
      out.println(clause.getString(false, prefix, matrix));
      counter++;
    }
    if (goal)
    {
      out.println("----------------------------------------");
      counter = 1;
      for (Clause clause : clauses)
      {
        if (!clause.isGoal()) continue;
        out.print(counter + ":");
        String name = clause.getName();
        if (name != null) out.print("[" + name + "]");
        out.print(" ");
        out.println(clause.getString(false, prefix, matrix));
        counter++;
      }
    }
  }
  
  /****************************************************************************
   * Transform the proof problem into a list of clauses.
   * @param theorem the theorem to be proved (may be null)
   ***************************************************************************/
  private void transform(Theorem theorem)
  {
    // the set of those names that must not be used for Skolem function
    // the names of the generated Skolem functions are added to this set
    Set<String> names = getFunctionNames(problem.decls);
    for (Decl decl : problem.decls)
    {
      if (decl instanceof Axiom)
      {
        // transform axiom formula into clauses
        Axiom decl0 = (Axiom)decl;
        Id id = decl0.id;
        Exp formula = decl0.formula;
        FormulaSymbol symbol = (FormulaSymbol)id.getSymbol();
        boolean goal = problem.originallyTheorems.contains(symbol);
        boolean implied = problem.impliedAxioms.contains(symbol);
        boolean theory = 
            problem.intAxioms.contains(symbol) ||
            problem.mapAxioms.contains(symbol) ||
            problem.dataTypeAxioms.contains(symbol);
        transform(id, formula, names, goal, implied, theory);
        continue;
      }
      if (decl instanceof Theorem)
      {
        // transform *negated* theorem formula into clauses
        Theorem decl0 = (Theorem)decl;
        if (decl0 != theorem) continue;
        Id id = decl0.id;
        Exp formula = decl0.formula;
        transform(id, Not.create(formula), names, true, false, false);
        continue;
      }
      // register any other declaration
      decls.add(decl);
    }
  }
  
  /****************************************************************************
   * Transform named formula into clauses.
   * @param id the name of the formula.
   * @param formula its expression.
   * @param goal true if the clause is a "goal".
   * @param implied true if the clause is implied by others.
   * @param theory true if the clause stems from a theory axiom.
   * @param names the set of names that must not be used for Skolem functions;
   *        the names of the generated Skolem functions are added to this set.
   ***************************************************************************/
  private void transform(Id id, Exp formula, Set<String> names, 
    boolean goal, boolean implied, boolean theory)
  {
    // transform the formula into a set of clauses
    Set<FunctionSymbol> skolem = new LinkedHashSet<FunctionSymbol>();
    ClauseSet cset = ClauseTransformer.transform(formula, names, skolem, goal);
    
    // add declarations of generated Skolem functions
    for (FunctionSymbol fun : skolem)
    {
      Id fid = fun.id;
      List<TypedVar> tvars = new ArrayList<TypedVar>();
      int counter = 0;
      for (TypeSymbol tsymbol : fun.tsymbols)
      {
        Id pid = Id.create("x_" + counter);
        tvars.add(TypedVar.create(pid, tsymbol.type));
        counter++;
      }
      Type type = fun.tsymbol.type;
      decls.add(Function.create(fid, tvars, type, null));
    }
    
    // add clauses
    String name = id.toString();
    List<Clause> clauses0 = cset.getClauses();
    boolean single = clauses0.size() == 1;
    int counter = 0;
    for (Clause clause : clauses0)
    {
      String name0 = single ? name : name + "." + counter;
      Clause clause0 = new Clause(name0, 
          clause.getTypedVars(), clause.getLiterals(), goal);
      clause0.setImplied(implied);
      clause0.setTheory(theory);
      clauses.add(clause0);
      counter++;
    }
  }

  /****************************************************************************
   * Get names of all declared functions.
   * @param decls a list of declarations.
   * @return the names of all functions declared in the list.
   ***************************************************************************/
  private static Set<String> getFunctionNames(List<Decl> decls)
  {
    Set<String> names = new HashSet<String>();
    for (Decl decl : decls)
    {
      if (!(decl instanceof Function)) continue;
      Function decl0 = (Function)decl;
      names.add(decl0.id.toString());
    }
    return names;
  }
}
// ----------------------------------------------------------------------------
// end of file
// ----------------------------------------------------------------------------