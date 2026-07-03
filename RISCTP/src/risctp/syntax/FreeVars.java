// ---------------------------------------------------------------------------
// FreeVars.java
// Compute the free variables of an expression.
// $Id: FreeVars.java,v 1.5 2023/03/02 07:37:44 schreine Exp $
//
// Author: Wolfgang Schreiner <Wolfgang.Schreiner@risc.jku.at>
// Copyright (C) 2022-, Research Institute for Symbolic Computation (RISC)
// Johannes Kepler University, Linz, Austria, https://www.risc.jku.at
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.
// ----------------------------------------------------------------------------
package risctp.syntax;

import java.util.*;

import risctp.syntax.AST.*;
import risctp.syntax.AST.Exp.*;
import risctp.syntax.AST.Pattern.*;
import risctp.types.*;
import risctp.types.Symbol.*;

public class FreeVars extends ASTVisitor.Base<Void>
{
  /****************************************************************************
   * Compute the free variables of an expression.
   * @param exp the expression.
   * @return the set of its free variables.
   ***************************************************************************/
  public static Set<VariableSymbol> compute(Exp exp)
  {
    Set<VariableSymbol> result = new LinkedHashSet<VariableSymbol>();
    FreeVars free = new FreeVars(result);
    exp.accept(free);
    return result;
  }

  // the set of free variables collected so far
  private Set<VariableSymbol> result; 

  /****************************************************************************
   * Create visitor that adds the free variables to given result set.
   * @param result the result set.
   ***************************************************************************/
  public FreeVars(Set<VariableSymbol> result) { this.result = result; }

  private static VariableSymbol getSymbol(Id id) 
  {
    return (VariableSymbol)id.getSymbol();
  }
  
  public Void visit(Var exp)
  {
    result.add(getSymbol(exp.id));
    return null;
  }
  
  public Void visit(Apply exp)
  {
    Symbol symbol = exp.id.getSymbol();
    if (!(symbol instanceof VariableSymbol)) return super.visit(exp);
    result.add((VariableSymbol)symbol);
    return null;
  }
  
  public Void visit(Forall exp)
  {
    Set<VariableSymbol> result0 = compute(exp.exp);
    result0.remove(getSymbol(exp.tvar.id));
    result.addAll(result0);
    return null;
  }
  
  public Void visit(Exists exp)
  {
    Set<VariableSymbol> result0 = compute(exp.exp);
    result0.remove(getSymbol(exp.tvar.id));
    result.addAll(result0);
    return null;
  }
  
  public Void visit(Choose exp)
  {
    Set<VariableSymbol> result0 = compute(exp.exp);
    result0.remove(getSymbol(exp.tvar.id));
    result.addAll(result0);
    return null;
  }
  
  public Void visit(Let exp)
  {
    Set<VariableSymbol> result0 = compute(exp.exp);
    for (LetBinder binder : exp.binders)
      result0.remove(getSymbol(binder.id));
    for (LetBinder binder : exp.binders)
      result0.addAll(compute(binder.exp));
    result.addAll(result0);
    return null;
  }
  
  public Void visit(Match exp)
  {
    Set<VariableSymbol> result0 = compute(exp.exp);
    for (MatchBinder binder : exp.binders)
    {
      Set<VariableSymbol> result1 = compute(binder.exp);
      Pattern pattern = binder.pattern;
      if (pattern instanceof ConstrPattern)
      {
        ConstrPattern pattern0 = (ConstrPattern)pattern;
        for (TypedVar tvar : pattern0.tvars)
          result1.remove(getSymbol(tvar.id));
      }
      result0.addAll(result1);
    }
    result.addAll(result0);
    return null;
  }
}
// ----------------------------------------------------------------------------
// end of file
// ----------------------------------------------------------------------------