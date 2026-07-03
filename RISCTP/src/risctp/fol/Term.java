// ---------------------------------------------------------------------------
// Term.java
// A low-overhead representation of terms and atomic formulas.
// $Id: Term.java,v 1.20 2024/03/25 14:24:02 schreine Exp $
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

import risctp.syntax.*;
import risctp.syntax.AST.*;
import risctp.syntax.AST.Exp.*;
import risctp.types.*;
import risctp.types.Symbol.*;

import java.util.*;

// ----------------------------------------------------------------------------
// A term is internally represented by a value of type Object which
// - either is a VariableSymbol
// - or is an Object[] array whose first element is a FunctionSymbol
//   and whose remaining elements are representations of the argument terms.
// However, in order to abstract from the internal representation (and thus
// provide for portability with respect to changes of this representation),
// only use the public static functions below for building terms and
// accessing their contents!
// ----------------------------------------------------------------------------
public final class Term
{
  //  construct term from variable
  public static Object term(VariableSymbol symbol) { return symbol; }
  
  // construct term from function symbol and argument terms
  public static Object term(FunctionSymbol symbol, Object[] terms) 
  { 
    int n = terms.length;
    Object[] result = new Object[n+1];
    result[0] = symbol;
    for (int i = 0; i < n; i++) result[i+1] = terms[i];
    return result;
  }
  
  // get symbol of term
  // symbols are unique, i.e., they can be compard by == (pointer equality)
  public static Symbol symbol(Object term) 
  { 
    if (term instanceof VariableSymbol) return (VariableSymbol)term;
    Object[] term0 = (Object[])term;
    return (FunctionSymbol)term0[0];
  }
  
  // get number of arguments of function application
  public static int argnumber(Object term) 
  { 
    Object[] term0 = (Object[])term;
    return term0.length-1; 
  }
  
  // get argument i from function application with 0 <= i < argnumber
  public static Object argument(Object term, int i) 
  { 
    Object[] term0 = (Object[])term;
    return term0[i+1]; 
  }
  
  // construct term from an expression 
  // (in which only variables and function applications occur)
  public static Object term(Exp exp)
  {
    if (exp instanceof Var)
    {
      Var exp0 = (Var)exp;
      VariableSymbol symbol = (VariableSymbol)exp0.id.getSymbol();
      if (symbol == null) throw new RuntimeException("null symbol in variable " + exp0.id);
      return term(symbol);
    }
    if (exp instanceof Apply)
    {
      Apply exp0 = (Apply)exp;
      FunctionSymbol symbol = (FunctionSymbol)exp0.id.getSymbol();
      if (symbol == null) throw new RuntimeException("null symbol in function " + exp0.id);
      Exp[] exps = exp0.exps;
      int n = exps.length;
      Object[] terms = new Object[n];
      for (int i = 0; i < n; i++)
        terms[i] = term(exps[i]);
     return term(symbol, terms);
    }
    new RuntimeException("unsupported expression " + exp);
    return null;
  }
  
  // the string representation of the term
  public static String toString(Object term)
  {
    StringBuilder builder = new StringBuilder();
    toString(term, builder);
    return builder.toString();
  }
  
  // add string representation of the term term to the builder
  private static void toString(Object term, StringBuilder builder)
  {
    Symbol symbol = symbol(term);
    String name = symbol.id.toString();
    builder.append(name);
    if (symbol instanceof VariableSymbol) return;
    int n = argnumber(term);
    if (n == 0) 
    {
      // ensure constant name differs from variable name (only for visual clarity)
      if (!AST.isNat(name) && !name.contains("§")) builder.append("§");
      return;
    }
    builder.append('(');
    for (int i = 0; i < n; i++)
    {
      Object arg = argument(term, i);
      toString(arg, builder);
      if (i+1 < n) builder.append(',');
    }
    builder.append(')');
  }
  
  /****************************************************************************
   * Check whether a (function/variable) symbol occurs in a term.
   * @param term the term.
   * @param symbol the symbol.
   * @return true if and only if the symbol occurs in the term.
   ***************************************************************************/
  public static boolean contains(Object term, Symbol symbol)
  {
    Symbol symbol0 = symbol(term);
    if (symbol == symbol0) return true;
    if (symbol0 instanceof VariableSymbol) return false;
    int n = argnumber(term);
    for (int i = 0; i < n; i++)
    {
      Object arg = argument(term, i);
      if (contains(arg, symbol)) return true;
    }
    return false;
  }
  
  /****************************************************************************
   * Check whether two terms are syntactically equal.
   * @param term1 the first term.
   * @param term2 the second term.
   * @return true if and only if the two terms are syntactically equal.
   ***************************************************************************/
  public static boolean equal(Object term1, Object term2)
  {
    Symbol symbol1 = symbol(term1);
    Symbol symbol2 = symbol(term2);
    if (symbol1 != symbol2) return false;
    if (symbol1 instanceof VariableSymbol) return true;
    int n = argnumber(term1);
    for (int i = 0; i < n; i++)
    {
      Object arg1 = argument(term1, i);
      Object arg2 = argument(term2, i);
      if (!equal(arg1, arg2)) return false;
    }
    return true;
  }
  
  /****************************************************************************
   * Add the variables of a term to a collection.
   * @param term the term.
   * @param vars the collection to which the variables are added.
   ***************************************************************************/
  public static void vars(Object term, Collection<VariableSymbol> vars)
  {
    Symbol symbol = symbol(term);
    if (symbol instanceof VariableSymbol)
    {
      VariableSymbol symbol0 = (VariableSymbol)symbol;
      vars.add(symbol0);
      return;
    }
    int n = argnumber(term);
    for (int i = 0; i < n; i++)
    {
      Object arg = argument(term, i);
      vars(arg, vars);
    }
  }
  
  /****************************************************************************
   * Does term not contain any variables?
   * @param term the term.
   * @return true if and only if the term does not contain any variables.
   ***************************************************************************/
  public static boolean ground(Object term)
  {
    Symbol symbol = symbol(term);
    if (symbol instanceof VariableSymbol) return false;
    int n = argnumber(term);
    for (int i = 0; i < n; i++)
    {
      Object arg = argument(term, i);
      if (!ground(arg)) return false;
    }
    return true;
  }
}
// ----------------------------------------------------------------------------
// end of file
// ----------------------------------------------------------------------------