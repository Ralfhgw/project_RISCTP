// ---------------------------------------------------------------------------
// Subst.java
// A substitution of variables by terms.
// $Id: Subst.java,v 1.18 2026/04/14 10:46:24 schreine Exp $
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

import risctp.types.*;
import risctp.types.Symbol.*;

// see class Term for the operations on terms represented by Object values
// see risctp.syntax.Substitution for a similar map of variables to expressions
public final class Subst extends LinkedHashMap<VariableSymbol,Object>
{
  // avoid Java warning
  private static final long serialVersionUID = 20230308L;

  // the base map for implementing "copy-on-write" behavior
  private Map<VariableSymbol,Object> map = null;

  // create an empty substitution
  public Subst() { }

  // create a substitution that maps a variable to a term
  public Subst(VariableSymbol var, Object term) { put(var, term); }

  // create a substitution from a map (or other substitution)
  // avoid costly duplication if substitution is only read
  public Subst(Map<VariableSymbol,Object> map) { this.map = map; }

  // get the value of a variable in this substitution
  public Object get(Object var)
  {
    if (map != null) return map.get(var);
    return super.get(var);
  }

  // update the value of a variable in this substitution
  public Object put(VariableSymbol var, Object term)
  {
    if (map != null) { putAll(map); map = null; }
    return super.put(var, term);
  }

  // put into this map all entries of the given map
  public void putAll(Map<? extends VariableSymbol,? extends Object> map)
  {
    while (map instanceof Subst)
    {
      Subst map0 = (Subst)map;
      if (map0.map == null) break;
      map = map0.map;
    }
    super.putAll(map);
  }

  // get the support of the substitution 
  public Set<VariableSymbol> keySet()
  {
    if (map != null) return map.keySet();
    return super.keySet();
  }

  /****************************************************************************
   * Apply this substitution to a term.
   * @param term the term.
   * @return the result of the application of this substitution to the term
   * such that the result does not contain any more a variable in the support
   * of this substitution.
   ***************************************************************************/
  public Object apply(Object term)
  {
    Symbol symbol = Term.symbol(term);
    while (symbol instanceof VariableSymbol)
    {
      Object term0 = get(symbol);
      if (term0 == null) return term;
      term = term0;
      symbol = Term.symbol(term0);
    }
    FunctionSymbol symbol0 = (FunctionSymbol)symbol;
    int n = Term.argnumber(term);
    Object[] terms0 = new Object[n];
    for (int i = 0; i < n; i++) terms0[i] = apply(Term.argument(term, i));
    return Term.term(symbol0, terms0);
  }

  /****************************************************************************
   * Unify two terms under this substitution.
   * @param term1 the first term.
   * @param term2 the second term.
   * @return a most general unifier of the two terms that extends 
   *         this substitution (null if the terms cannot be unified)
   ***************************************************************************/
  public Subst unify(Object term1, Object term2)
  {
    Subst subst = new Subst(this);
    boolean okay = unify(subst, term1, term2);
    if (okay) return subst; else return null;
  }

  /****************************************************************************
   * Unify two terms under the given substitution.
   * @param subst a substitution.
   * @param term1 the first term.
   * @param term2 the second term.
   * @return true if subst could be extended to a most general unifier of the 
   *         two terms (otherwise the content of subst is undefined).
   ***************************************************************************/
  public static boolean unify(Subst subst, Object term1, Object term2)
  {
    Symbol symbol1 = Term.symbol(term1);
    Symbol symbol2 = Term.symbol(term2);
    if (symbol1 instanceof FunctionSymbol && symbol2 instanceof FunctionSymbol)
    {
      if (symbol1 != symbol2) return false;
      int n = Term.argnumber(term1);
      for (int i = 0; i < n; i++)
      {
        boolean okay = unify(subst, Term.argument(term1, i), Term.argument(term2, i));
        if (!okay) return false;
      }
      return true;
    }
    VariableSymbol symbol0; 
    if (symbol1 instanceof VariableSymbol)
    {
      symbol0 = (VariableSymbol)symbol1;
    }
    else
    {
      symbol0 = (VariableSymbol)symbol2;
      term2 = term1;
    }
    Object term0 = subst.get(symbol0);
    if (term0 != null) return unify(subst, term0, term2);
    term0 = subst.apply(term2);
    if (Term.symbol(term0) == symbol0) return true;
    if (Term.contains(term0, symbol0)) return false;
    subst.put(symbol0, term0);
    return true;
  }

  /****************************************************************************
   * Unify two term sequences under this substitution.
   * @param terms1 the first sequence.
   * @param terms2 the second sequence.
   * @return a most general unifier of all term pairs in these sequences that
   *         extends this substitution (null, if the sequences cannot be unified)
   ***************************************************************************/
  public Subst unify(List<Object> terms1, List<Object> terms2)
  {
    Subst subst = new Subst(this);
    boolean okay = unify(subst, terms1, terms2);
    if (okay) return subst; else return null;
  }

  /****************************************************************************
   * Unify two term sequences under the given substitution.
   * @param subst the substitution.
   * @param terms1 the first sequence.
   * @param terms2 the second sequence.
   * @return true if subst could be extended to a most general unifier of the 
   *         two sequences (otherwise the content of subst is undefined).
   ***************************************************************************/
  public static boolean unify(Subst subst, List<Object> terms1, List<Object> terms2)
  {
    int n = terms1.size();
    for (int i = 0; i < n; i++)
    {
      boolean okay = unify(subst, terms1.get(i), terms2.get(i));
      if (!okay) return false;
    }
    return true;
  }

  /****************************************************************************
   * Match two terms under this substitution.
   * @param term1 the first term (the "pattern").
   * @param term2 the second term.
   * @return a substitution that extends this substitution by mappings for the
   * variables in the "pattern" term1 such that term1 and term2 become equal
   * (null if no such substitution exists)
   ***************************************************************************/
  public Subst match(Object term1, Object term2)
  {
    Subst subst = new Subst(this);
    boolean okay = match(subst, term1, term2);
    if (okay) return subst; else return null;
  }

  /****************************************************************************
   * Match two terms under the given substitution.
   * @param subst a substitution (the "pattern").
   * @param term1 the first term.
   * @param term2 the second term.
   * @return true if subst could be extended by mappings for the 
   * variables in the "pattern" term1 such that term1 and term2 
   * become equal (otherwise the content of subst is undefined).
   ***************************************************************************/
  public static boolean match(Subst subst, Object term1, Object term2)
  {
    Symbol symbol1 = Term.symbol(term1);
    Symbol symbol2 = Term.symbol(term2);
    if (symbol1 instanceof FunctionSymbol)
    {
      if (symbol1 != symbol2) return false;
      int n = Term.argnumber(term1);
      for (int i = 0; i < n; i++)
      {
        boolean okay = match(subst, Term.argument(term1, i), Term.argument(term2, i));
        if (!okay) return false;
      }
      return true;
    }
    VariableSymbol symbol0 = (VariableSymbol)symbol1;
    Object term0 = subst.get(symbol0);
    if (term0 != null) return Term.equal(term0, term2);
    // if (term0 != null) return match(subst, term0, term2);
    if (symbol0 == symbol2) return true;
    if (Term.contains(term2, symbol0)) return false;
    subst.put(symbol0, term2);
    return true;
  }

  /****************************************************************************
   * Match two term sequences under this substitution.
   * @param terms1 the first sequence (the "patterns").
   * @param terms2 the second sequence.
   * @return a substitution that extends this substitution by mappings for the
   * variables in the "patterns" terms1 such that terms1 and terms2 become equal
   * (null if no such substitution exists)
   ***************************************************************************/
  public Subst match(List<Object> terms1, List<Object> terms2)
  {
    Subst subst = new Subst(this);
    boolean okay = match(subst, terms1, terms2);
    if (okay) return subst; else return null;
  }

  /****************************************************************************
   * Match two term sequences under the given substitution.
   * @param subst the substitution.
   * @param terms1 the first sequence (the "patterns").
   * @param terms2 the second sequence.
   * @return true if subst could be extended by mappings for the 
   * variables in the "patterns" term1s such that terms1 and terms2 
   * become equal (otherwise the content of subst is undefined).
   ***************************************************************************/
  public static boolean match(Subst subst, List<Object> terms1, List<Object> terms2)
  {
    int n = terms1.size();
    for (int i = 0; i < n; i++)
    {
      boolean okay = match(subst, terms1.get(i), terms2.get(i));
      if (!okay) return false;
    }
    return true;
  }
}
// ----------------------------------------------------------------------------
// end of file
// ----------------------------------------------------------------------------