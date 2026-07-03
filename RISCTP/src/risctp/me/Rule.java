// ---------------------------------------------------------------------------
// Rule.java
// A clausal rule.
// $Id: Rule.java,v 1.11 2024/07/08 08:17:02 schreine Exp $
//
// Author: Wolfgang Schreiner <Wolfgang.Schreiner@risc.jku.at>
// Copyright (C) 2023-, Research Institute for Symbolic Computation (RISC)
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
package risctp.me;

import java.io.*;
import java.util.*;
import java.util.function.*;

import risctp.fol.*;
import risctp.syntax.AST.*;
import risctp.syntax.AST.Exp.*;
import risctp.types.Symbol.*;

public final class Rule
{
  // its name
  public final String name;

  // the quantified variables
  public final VariableSymbol[] vars;

  // the literals
  public final Object[] lits;

  // their polarities
  public final boolean[] negs;

  // set if the literal must not become a goal
  public final boolean[] notGoals;
  
  // create a new rule from its elements
  public Rule(String name, VariableSymbol[] vars, Object[] lits, 
    boolean[] negs, boolean[] notGoals)
  {
    this.name = name;
    this.vars = vars;
    this.lits = lits;
    this.negs = negs;
    this.notGoals = notGoals;
  }

  // true if rule need not be considered as a starting point of proof search
  private boolean implied = false;
  public void setImplied(boolean value) { implied = value; }
  public boolean isImplied() { return implied; }
  
  // true if rule need not be emitted to SMT solver
  private boolean theory = false;
  public void setTheory(boolean value) { theory = value; }
  public boolean isTheory() { return theory; }

  /**************************************************************************
   * Print core content of rule.
   * @param out the output medium.
   * @param negate true if the negation of the rule is to be printed.
   * @param quant true if the quantifier prefix is to be printed.
   * @param pred a predicate that states whether a literal is to be printed.
   * @param consumer a consumer that accepts the literal and prints it.
   *************************************************************************/
  public void print(PrintWriter out, boolean negate, boolean quant,
    BiPredicate<Object,Boolean> predicate, BiConsumer<Object,Boolean> consumer)
  {
    int varn = vars.length;
    if (quant && varn > 0)
    {
      if (negate) out.print("∃"); else out.print("∀");
      for (int i = 0; i < varn; i++)
      {
        VariableSymbol var = vars[i];
        out.print(var.id.toString());
        out.print(":");
        out.print(var.tsymbol.type.toString());
        if (i+1 < varn) out.print(",");
      }
      out.print(". ");
    }
    List<Object> plits = new ArrayList<Object>();
    List<Object> nlits = new ArrayList<Object>();
    int n = lits.length;
    for (int i = 0; i < n; i++)
    {
      Object lit = lits[i];
      boolean neg = negs[i];
      if (!predicate.test(lit, neg)) continue;
      if (neg) nlits.add(lit); else plits.add(lit);
    }
    int nlitsn = nlits.size();
    int plitsn = plits.size();
    if (nlitsn == 0)
    {
      /*if (plitsn == 0)*/ out.print("⊤");
    }
    else
    {
      for (int i = 0; i < nlitsn; i++)
      {
        consumer.accept(nlits.get(i), false);
        if (i+1 < nlitsn) out.print(" ∧ ");
      }
    }
    if (negate) 
    { 
      if (plitsn > 0) out.print(" ∧ "); 
    }
    else 
    {
      /*if (nlitsn > 0 || plitsn == 0)*/ out.print(" ⇒ ");
    }
    if (plitsn == 0)
    {
      if (!negate) out.print("⊥");
    }
    else
    {
      for (int i = 0; i < plitsn; i++)
      {
        if (negate) out.print("¬");
        consumer.accept(plits.get(i), true);
        if (i+1 < plitsn) 
        {
          if (negate) out.print(" ∧ "); else out.print(" ∨ ");
        }
      }
    }
    if (isImplied()) { out.print(" {implied}"); }
  }

  // transform a clause to a rule
  public Rule(Clause clause)
  {
    // initialize the name
    name = clause.getName();

    // set the theory status
    theory = clause.isTheory();
    
    // initialize the quantified variables
    List<TypedVar> tvars = clause.getTypedVars();
    int n = tvars.size();
    vars = new VariableSymbol[n];
    for (int i = 0; i < n; i++)
    {
      TypedVar tvar = tvars.get(i);
      vars[i] = (VariableSymbol)tvar.id.getSymbol();
    }

    // initialize the literals and their polarities
    List<Exp> literals = clause.getLiterals();
    List<Object> lits0 = new ArrayList<Object>();
    List<Boolean> negs0 = new ArrayList<Boolean>();
    List<Boolean> notGoals0 = new ArrayList<Boolean>();
    for (Exp literal : literals)
    {
      if (literal instanceof Not)
      {
        Not literal0 = (Not)literal;
        Apply atom = (Apply)literal0.exp;
        Object lit = Term.term(atom);
        lits0.add(lit);
        negs0.add(true);
        notGoals0.add(atom.isNotGoal());
      }
      else
      {
        Apply atom = (Apply)literal;
        Object lit = Term.term(atom);
        lits0.add(lit);
        negs0.add(false);
        notGoals0.add(atom.isNotGoal());
      }
    }
    int litn = lits0.size();
    lits = lits0.toArray(new Object[litn]);
    negs = new boolean[litn];
    for (int i = 0; i < litn; i++) negs[i] = negs0.get(i);
    notGoals = new boolean[litn];
    for (int i = 0; i < litn; i++) notGoals[i] = notGoals0.get(i);
  }
}
// ----------------------------------------------------------------------------
// end of file
// ----------------------------------------------------------------------------

