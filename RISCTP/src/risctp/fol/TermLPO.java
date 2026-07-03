// ---------------------------------------------------------------------------
// TermLPO.java
// The lexicographic path ordering (LPO) of terms.
// $Id: TermLPO.java,v 1.5 2023/12/21 17:03:12 schreine Exp $
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

public class TermLPO
{
  // a mapping of function symbols to their order of introduction
  private Map<FunctionSymbol,Integer> funOrder;
  
  // the set of function symbols that are considered as selectors
  private Set<FunctionSymbol> selectors;
  
  /****************************************************************************
   * Create a new term ordering.
   ***************************************************************************/
  public TermLPO() 
  { 
    funOrder = new HashMap<FunctionSymbol,Integer>();
    selectors = new HashSet<FunctionSymbol>();
  }
  
  /****************************************************************************
   * Define the order of a function symbol.
   * @param fun the function symbol.
   ***************************************************************************/
  public void defineOrder(FunctionSymbol fun)
  {
    // we give all constant symbols the same minimal order -1
    int n = fun.tsymbols.size() == 0 ? -1 : funOrder.size();
    funOrder.put(fun, n);
  }
  
  /****************************************************************************
   * Declare function symbol f as a selector such that f(t) is always
   * considered less than t, for arbitrary term t.
   * @param fun the function symbol.
   ***************************************************************************/
  public void makeSelector(FunctionSymbol fun)
  {
    selectors.add(fun);
  }
  
  /****************************************************************************
   * Compare two terms according to the lexicographic path ordering (LPO).
   * @param term1 the first term.
   * @param term2 the second term.
   * @return true iff term1 is less than term2.
   ***************************************************************************/
  public boolean less(Object term1, Object term2)
  {
    Symbol sym1 = Term.symbol(term1);
    Symbol sym2 = Term.symbol(term2);
    // we tweak the ordering to obey the rule "selector(arg) < arg"
    if (selectors.contains(sym1) && Term.argnumber(term1) == 1)
    {
      Object arg = Term.argument(term1, 0);
      if (Term.equal(arg, term2)) return true;
    }
    if (selectors.contains(sym2) && Term.argnumber(term2) == 1)
    {
      Object arg = Term.argument(term2, 0);
      if (Term.equal(arg, term1)) return false;
    }
    if (sym1 instanceof VariableSymbol) return sym1 != sym2 && Term.contains(term2, sym1);
    if (sym2 instanceof VariableSymbol) return false;
    if (lessEqualSomeArg(term1, term2)) return true;
    FunctionSymbol fun1 = (FunctionSymbol)sym1;
    FunctionSymbol fun2 = (FunctionSymbol)sym2;
    Integer order1 = funOrder.get(fun1);
    Integer order2 = funOrder.get(fun2);
    // Skolem functions need not appear in table
    if (order1 == null || order2 == null) return false;
    if (order1 > order2) return false;
    if (order1 < order2) return allArgsLess(term1, term2);
    return allArgsLess(term1, term2) && lessArgTuple(term1, term2);
  }
  
  /****************************************************************************
   * Compare term1 with arguments of term2 according to LPO.
   * @param term1 the first term.
   * @param term2 the second term.
   * @return true iff term1 is less than or equal some argument of term2.
   ***************************************************************************/
  private boolean lessEqualSomeArg(Object term1, Object term2)
  {
    int n2 = Term.argnumber(term2);
    for (int i = 0; i < n2; i++)
    {
      Object arg2 = Term.argument(term2, i);
      if (Term.equal(term1, arg2)) return true;
      if (less(term1, arg2)) return true;
    }
    return false;
  }
  
  /****************************************************************************
   * Compare arguments of term1 with term2 according to LPO.
   * @param term1 the first term.
   * @param term2 the second term.
   * @return true iff all arguments of term1 are less than term2.
   ***************************************************************************/
  private boolean allArgsLess(Object term1, Object term2)
  {
    int n1 = Term.argnumber(term1);
    for (int i = 0; i < n1; i++)
    {
      Object arg1 = Term.argument(term1, i);
      if (!less(arg1, term2)) return false;
    }
    return true;
  }
  
  /****************************************************************************
   * Compare two argument tuples according to the lexicographic LPO order.
   * @param term1 the first term.
   * @param term2 the second term.
   * @return true iff the argument tuple of the first term is less.
   ***************************************************************************/
  private boolean lessArgTuple(Object term1, Object term2)
  {
    int n1 = Term.argnumber(term1);
    int n2 = Term.argnumber(term2);
    int n = n1 < n2 ? n1 : n2;
    for (int i = 0; i < n; i++)
    {
      Object arg1 = Term.argument(term1, i);
      Object arg2 = Term.argument(term2, i);
      if (!Term.equal(arg1, arg2)) return less(arg1, arg2);
    }
    return n1 < n2;
  }
}
// ----------------------------------------------------------------------------
// end of file
// ----------------------------------------------------------------------------