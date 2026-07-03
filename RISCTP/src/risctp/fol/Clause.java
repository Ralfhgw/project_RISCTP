// ---------------------------------------------------------------------------
// Clause.java
// A clause, i.e., a quantified combination of literals.
// $Id: Clause.java,v 1.9 2024/07/08 08:17:02 schreine Exp $
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

import risctp.syntax.AST.*;
import risctp.syntax.AST.Exp.*;

public class Clause
{
  private String name;          // the name of the clause (may be null)
  private List<TypedVar> tvars; // its free variables
  private List<Exp> literals;   // its literals
  private boolean goal;         // true if the clause is a "goal"
  
  // construct a named clause
  public Clause(String name, List<TypedVar> tvars, List<Exp> literals, boolean goal) 
  { this.name = name; this.tvars = tvars; this.literals = literals; this.goal = goal; }
  
  // construct an unnamed clause
  public Clause(List<TypedVar> tvars, List<Exp> literals, boolean goal) 
  { this(null, tvars, literals, goal); }
  
  // get the elements of the clause (name may be null)
  public String getName() { return name; }
  public List<TypedVar> getTypedVars() { return tvars; }
  public List<Exp> getLiterals() { return literals; }
  public boolean isGoal() { return goal; }
  
  // true if clause can be removed from formula without changing its semantics
  private boolean implied = false;
  public void setImplied(boolean value) { implied = value; }
  public boolean isImplied() { return implied; }

  // true if clause stems from a theory axiom
  private boolean theory = false;
  public void setTheory(boolean value) { theory = value; }
  public boolean isTheory() { return theory; }
  
  // string representation of the quantifier prefix of the clause formula
  // NONE: without quantifier
  // VARS: only names of variables
  // TYPES: names and types of variables 
  public enum PrefixFormat { NONE, NAMES, TYPES }
  
  // string representation of the matrix of the clause formula
  // SET: a set {l1,l2,...} of possibly negated atoms
  // DISJUNCTION: a a disjunction l1 \/ l2 \/ ... of possibly negated literals
  // IMPLICATION: an implication l1 /\ ... => r1 \/ ... of positive literals
  public enum MatrixFormat { SET, DISJUNCTION, IMPLICATION }
  
  /**************************************************************************
   * Get a string representation of the clause formula in the denoted format.
   * @param negated true if the clause is to be considered as *negated*.
   * @param prefix the prefix format.
   * @param matrix the matrix format.
   *************************************************************************/
  public String getString(boolean negated,
    PrefixFormat prefix, MatrixFormat matrix)
  {
    StringBuilder builder = new StringBuilder();
    printPrefix(negated, prefix, builder);
    printMatrix(negated, matrix, builder);
    return builder.toString();
  }
  
  /**************************************************************************
   * Print quantifier prefix of the clause formula.
   * @param negated true if the clause is to be considered as *negated*.
   * @param prefix the prefix format.
   * @param builder the builder to which to print.
   *************************************************************************/
  private void printPrefix(boolean negated,
    PrefixFormat prefix, StringBuilder builder)
  {
    if (prefix == PrefixFormat.NONE) return;
    List<TypedVar> tvars = getTypedVars();
    int n = tvars.size();
    if (n == 0) return;
    if (negated)
      builder.append("∃");
    else
      builder.append("∀");
    for (int i = 0; i < n; i++)
    {
      TypedVar tvar = tvars.get(i);
      if (prefix == PrefixFormat.NAMES)
        builder.append(tvar.id.toString());
      else
        builder.append(tvar.toString());
      if (i < n-1) builder.append(",");
    }
    builder.append(". ");
  }
  
  /**************************************************************************
   * Print matrix of the clause formula.
   * @param negated true if the formula is to be considered as *negated*.
   * @param matrix the matrix format.
   * @param builder the builder to which to print.
   *************************************************************************/
  private void printMatrix(boolean negated,
    MatrixFormat matrix, StringBuilder builder)
  {
    List<Exp> literals = getLiterals();
    int n = literals.size();
    if (negated && matrix == MatrixFormat.IMPLICATION)
      matrix = MatrixFormat.DISJUNCTION;
    switch (matrix)
    {
    case SET:
    {
      builder.append("{");
      for (int i = 0; i < n; i++)
      {
        Exp literal = cneg(negated, literals.get(i));
        builder.append(literal.toString());
        if (i < n-1) builder.append(", ");
      }
      builder.append("}");
      break;
    }
    case DISJUNCTION:
    {
      for (int i = 0; i < n; i++)
      {
        Exp literal = cneg(negated, literals.get(i));
        builder.append(literal.toString());
        if (i < n-1) 
        {
          if (negated)
            builder.append(" ∧ ");
          else
            builder.append(" ∨ ");
        }
      }
      break;
    }
    case IMPLICATION:
    {
      List<Exp> pos = new ArrayList<Exp>();
      List<Exp> neg = new ArrayList<Exp>();
      for (Exp literal : literals)
      {
        if (literal instanceof Not)
        {
          Not literal0 = (Not)literal;
          neg.add(literal0.exp);
        }
        else
          pos.add(literal);
      }
      int negn = neg.size();
      int posn = pos.size();
      if (negn == 0)
      {
        /*if (posn == 0)*/ builder.append("⊤");
      }
      else
      {
        if (negn > 1) builder.append("(");
        for (int i = 0; i < negn; i++)
        {
          Exp literal = neg.get(i);
          builder.append(literal.toString());
          if (i < negn-1) builder.append(" ∧ ");
        }
        if (negn > 1) builder.append(")");
      }
      /*if (negn > 0 || posn == 0)*/ builder.append(" ⇒ ");
      if (posn == 0)
        builder.append("⊥");
      else
      {
        if (posn > 1) builder.append("(");
        for (int i = 0; i < posn; i++)
        {
          Exp literal = pos.get(i);
          builder.append(literal.toString());
          if (i < posn-1) builder.append(" ∨ ");
        }
        if (posn > 1) builder.append(")");
      }
      break;
    }
    default:
    {
    }
    }
    if (implied) builder.append(" {implied}");
  }

  /***************************************************************************
   * Return conditionally negated form of literal.
   * @param negated if true the literal is negated, otherwise left unchanged.
   * @param literal the literal.
   * @return the conditionally negated literal (without double negation).
   **************************************************************************/
  private static Exp cneg(boolean negated, Exp literal)
  {
    if (!negated) return literal;
    if (literal instanceof Not)
    {
      Not literal0 = (Not)literal;
      return literal0.exp;
    }
    return Not.create(literal);
  }
}
// ----------------------------------------------------------------------------
// end of file
// ----------------------------------------------------------------------------
