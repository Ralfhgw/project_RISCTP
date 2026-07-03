// ---------------------------------------------------------------------------
// BoundVars.java
// Compute the bound variables of an expression.
// $Id: BoundVars.java,v 1.1 2024/04/22 15:15:49 schreine Exp $
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
import risctp.types.Symbol.*;

public class BoundVars extends ASTVisitor.Base<Void>
{
  /****************************************************************************
   * Compute the bound variables of an expression.
   * @param exp the expression.
   * @return the set of its bound variables.
   ***************************************************************************/
  public static Set<VariableSymbol> compute(Exp exp)
  {
    Set<VariableSymbol> result = new LinkedHashSet<VariableSymbol>();
    BoundVars bound = new BoundVars(result);
    exp.accept(bound);
    return result;
  }

  // the set of bound variables collected so far
  private Set<VariableSymbol> result; 

  /****************************************************************************
   * Create visitor that adds the free variables to given result set.
   * @param result the result set.
   ***************************************************************************/
  public BoundVars(Set<VariableSymbol> result) { this.result = result; }

  private static VariableSymbol getSymbol(Id id) 
  {
    return (VariableSymbol)id.getSymbol();
  }
  
  public Void visit(Forall exp)
  {
    super.visit(exp);
    result.add(getSymbol(exp.tvar.id));
    return null;
  }
  
  public Void visit(Exists exp)
  {
    super.visit(exp);
    result.add(getSymbol(exp.tvar.id));
    return null;
  }
  
  public Void visit(Choose exp)
  {
    super.visit(exp);
    result.add(getSymbol(exp.tvar.id));
    return null;
  }
  
  public Void visit(Let exp)
  {
    super.visit(exp);
    for (LetBinder binder : exp.binders)
      result.add(getSymbol(binder.id));
    return null;
  }
  
  public Void visit(Match exp)
  {
    super.visit(exp);
    for (MatchBinder binder : exp.binders)
    {
      Pattern pattern = binder.pattern;
      if (pattern instanceof ConstrPattern)
      {
        ConstrPattern pattern0 = (ConstrPattern)pattern;
        for (TypedVar tvar : pattern0.tvars)
          result.add(getSymbol(tvar.id));
      }
    }
    return null;
  }
}
// ----------------------------------------------------------------------------
// end of file
// ----------------------------------------------------------------------------