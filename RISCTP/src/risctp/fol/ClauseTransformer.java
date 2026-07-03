// ---------------------------------------------------------------------------
// ClauseTransformer.java
// The transformation of first-order formulas into clausal form.
// $Id: ClauseTransformer.java,v 1.6 2023/03/08 16:41:15 schreine Exp $
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

import risctp.syntax.*;
import risctp.syntax.AST.*;
import risctp.syntax.AST.Exp.*;
import risctp.types.Symbol.*;

// --------------------------------------------------------------------------
// A transformer of formulas into clausal form in analogy to functions 
// "cnf, "simpcnf", "purecnf", "dnf", "simpdnf", "purednf" on pages 58-62 in
// [John Harrison, "Handbook of Practical Logic and Automated Reasoning"]
//--------------------------------------------------------------------------
public class ClauseTransformer extends ASTVisitor.Base<Void>
{
  // a set of clauses
  public static class ClauseSet
  {
    private List<TypedVar> tvars; // the free variables of all clauses in this set
    private List<Clause> clauses; // the clauses in this set
    public ClauseSet(List<TypedVar> tvars, List<Clause> clauses) 
    { this.tvars = tvars; this.clauses = clauses; }
    public List<TypedVar> getTypedVars() { return tvars; }
    public List<Clause> getClauses() { return clauses; }
  }
 
  /****************************************************************************
   * Transform formula into clausal form.
   * @param formula the formula.
   * @param names the set of names that must not be used for Skolem functions;
   *        the names of the generated Skolem functions are added to this set.
   * @param skolem a set to which the symbols of the generated Skolem functions 
   *        are added.
   * @param goal true if the formula is a "goal"
   * @return a logically equivalent set of clauses.
   ***************************************************************************/
  public static ClauseSet transform(Exp formula, Set<String> names,
    Set<FunctionSymbol> skolem, boolean goal)
  {
    Exp formula0 = SNFTransformer.transform(formula, names, skolem);
    List<TypedVar> tvars = new ArrayList<TypedVar>();
    Exp formula1 = specialize(formula0, tvars);
    List<List<Exp>> cnf = cnf(formula1);
    List<Clause> clauses = new ArrayList<Clause>();
    for (List<Exp> literals : cnf)
    {
      Set<VariableSymbol> free = new LinkedHashSet<VariableSymbol>();
      FreeVars visitor = new FreeVars(free);
      for (Exp literal : literals)
        literal.accept(visitor);
      List<TypedVar> tvars0 = typedVars(free, tvars);
      clauses.add(new Clause(tvars0, literals, goal));
    }
    return new ClauseSet(tvars, clauses);
  }

  /****************************************************************************
   * Split formula in prenex normal form into its quantifier prefix and matrix.
   * @param formula the formula.
   * @param tvars a list to which all quantified variables are added.
   * @return the formula with the quantified variables stripped from it.
   ***************************************************************************/
  private static Exp specialize(Exp formula, List<TypedVar> tvars)
  {
    Exp matrix = formula;
    while (matrix instanceof Forall)
    {
      Forall matrix0 = (Forall)matrix;
      tvars.add(matrix0.tvar);
      matrix = matrix0.exp;
    }
    return matrix;
  }
  
  /****************************************************************************
   * Create sublist of typed variables whose symbols occur in given set.
   * @param symbols a set of variable symbols.
   * @param tvars a list of typed variables.
   * @return the sublist of 'tvars' whose symbols occur in 'symbols'
   ***************************************************************************/
  private static List<TypedVar> typedVars(Set<VariableSymbol> symbols, 
    List<TypedVar> tvars)
  {
    List<TypedVar> result = new ArrayList<TypedVar>();
    for (TypedVar tvar : tvars)
    {
      VariableSymbol symbol = (VariableSymbol)tvar.id.getSymbol();
      if (symbols.contains(symbol)) result.add(tvar);
    }
    return result;
  }

  /****************************************************************************
   * Compute the conjunctive normal form of a formula in negation normal form.
   * @param exp a formula in negation normal form.
   * @return a logically equivalent formula in conjunctive normal form.
   ***************************************************************************/
  private static List<List<Exp>> cnf(Exp exp)
  {
    ClauseTransformer transformer = new ClauseTransformer();
    exp.accept(transformer);
    return transformer.cnf;
  }
  
  // the conjunctive normal form computed by this transformer
  private List<List<Exp>> cnf = new ArrayList<List<Exp>>();

  // the corresponding string representations 
  private List<Set<String>> scnf = new ArrayList<Set<String>>();

  /***************************************************************************
   * Add clause to cnf, avoiding redundancies.
   * @param clause the clause
   ***************************************************************************/
  private void addClause(List<Exp> clause)
  {
    // compute simplified version of clause and its string representation
    List<Exp> clause0 = new ArrayList<Exp>();
    Set<String> sclause0 = new HashSet<String>();
    
    // remove duplicate literals and collect
    // positive and negative occurrences of atoms
    Set<String> pos = new HashSet<String>();
    Set<String> neg = new HashSet<String>();
    for (Exp literal : clause)
    {
      String str = literal.toString();
      if (sclause0.contains(str)) continue;
      clause0.add(literal);
      sclause0.add(str);
      if (literal instanceof Not)
      {
        Not literal0 = (Not)literal;
        neg.add(literal0.exp.toString());
      }
      else
        pos.add(str);
    }
    
    // check for complimentary literals
    for (String literal : pos)
    {
      if (neg.contains(literal)) return;
    }
    
    // check whether new clause is subsumed by old clauses
    for (Set<String> sclause : scnf)
    {
      if (sclause0.containsAll(sclause)) return;
    }
    
    // remove old clauses that are subsumed by new clause
    List<List<Exp>> cnf1 = cnf; 
    List<Set<String>> scnf1 = scnf; 
    cnf = new ArrayList<List<Exp>>();
    scnf = new ArrayList<Set<String>>();
    int n = cnf1.size();
    for (int i = 0; i < n; i++)
    {
      List<Exp> clause1 = cnf1.get(i);
      Set<String> sclause1 = scnf1.get(i);
      if (sclause1.containsAll(sclause0)) continue;
      cnf.add(clause1);
      scnf.add(sclause1);
    }
    
    // add clause to conjunctive form
    cnf.add(clause0);
    scnf.add(sclause0);
  }
  
  // --------------------------------------------------------------------------
  //
  // the visitors of the cloner below
  //
  // --------------------------------------------------------------------------
  
  // positive truth constants
  public Void visit(True exp)
  {
    // a true clause can be discarded
    return null;
  }
  
  // negative truth constants
  public Void visit(False exp)
  {
    addClause(new ArrayList<Exp>());
    return null;
  }
  
  // a positive literal
  public Void visit(Apply exp)
  {
    List<Exp> literals = new ArrayList<Exp>();
    literals.add(exp);
    addClause(literals);
    return null;
  }
  
  // a negative literal
  public Void visit(Not exp)
  {
    List<Exp> literals = new ArrayList<Exp>();
    literals.add(exp);
    addClause(literals);
    return null;
  }
  
  // And formulas are handled by implementation of visit() in super class
  
  // the core of the transformation
  public Void visit(Or exp)
  {
    List<List<Exp>> cnf1 = cnf(exp.exp1);
    List<List<Exp>> cnf2 = cnf(exp.exp2);
    for (List<Exp> clause1 : cnf1)
    {
      for (List<Exp> clause2 : cnf2)
      {
        List<Exp> clause = new ArrayList<Exp>(clause1);
        clause.addAll(clause2);
        addClause(clause);
      }
    }
    return null;
  }
}
// ----------------------------------------------------------------------------
// end of file
// ----------------------------------------------------------------------------