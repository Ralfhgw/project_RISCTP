// ---------------------------------------------------------------------------
// Simplifier.java
// The simplification of first-order formulas.
// $Id: Simplifier.java,v 1.5 2023/03/02 07:43:28 schreine Exp $
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
// A cloner that simplifies formulas in analogy to the functions 
// "simplify", "simplify1", and "psimplify" on pages 140 and 50 of
// [John Harrison, "Handbook of Practical Logic and Automated Reasoning"]
//--------------------------------------------------------------------------
public class Simplifier extends ASTCloner
{
  /****************************************************************************
   * Simplify formula.
   * @param formula the formula
   * @return its simplified (but logically equivalent) form.
   ***************************************************************************/
  public static Exp transform(Exp formula)
  {
    Simplifier simplifier = new Simplifier();
    return (Exp)formula.accept(simplifier);
  }
  
  private Simplifier()
  {
    // ids remain shared (and thus preserve symbols)
    super(false);
  }

  public Exp visit(Apply exp)
  {
    // avoid cloning of atomic formulas
    return exp;
  }
  
  public Exp visit(Not exp)
  {
    Not exp0 = (Not)super.visit(exp);
    if (exp0.exp instanceof False) return True.create();
    if (exp0.exp instanceof True) return False.create();
    if (exp0.exp instanceof Not)
    {
      Not exp1 = (Not)exp0.exp;
      return exp1.exp;
    }
    return exp0;
  }
  
  public Exp visit(And exp)
  {
    And exp0 = (And)super.visit(exp);
    if (exp0.exp1 instanceof False) return exp0.exp1;
    if (exp0.exp1 instanceof True) return exp0.exp2;
    if (exp0.exp2 instanceof False) return exp0.exp2;
    if (exp0.exp2 instanceof True) return exp0.exp1;
    return exp0;
  }
  
  public Exp visit(Or exp)
  {
    Or exp0 = (Or)super.visit(exp);
    if (exp0.exp1 instanceof False) return exp0.exp2;
    if (exp0.exp1 instanceof True) return exp0.exp1;
    if (exp0.exp2 instanceof False) return exp0.exp1;
    if (exp0.exp2 instanceof True) return exp0.exp2;
    return exp0;
  }
  
  public Exp visit(Imp exp)
  {
    Imp exp0 = (Imp)super.visit(exp);
    if (exp0.exp1 instanceof False) return True.create();
    if (exp0.exp1 instanceof True) return exp0.exp2;
    if (exp0.exp2 instanceof False) return Not.create(exp0.exp1);
    if (exp0.exp2 instanceof True) return exp0.exp2;
    return exp0;
  }
  
  public Exp visit(Equiv exp)
  {
    Equiv exp0 = (Equiv)super.visit(exp);
    if (exp0.exp1 instanceof False) return Not.create(exp0.exp2);
    if (exp0.exp1 instanceof True) return exp0.exp2;
    if (exp0.exp2 instanceof False) return Not.create(exp0.exp1);
    if (exp0.exp2 instanceof True) return exp0.exp1;
    return exp0;
  }
  
  public Exp visit(Forall exp)
  {
    Forall exp0 = (Forall)super.visit(exp);
    Set<VariableSymbol> fvars = FreeVars.compute(exp0.exp);
    VariableSymbol var = (VariableSymbol)exp0.tvar.id.getSymbol();
    if (!fvars.contains(var)) return exp0.exp;
    return exp0;
  }
  
  public Exp visit(Exists exp)
  {
    Exists exp0 = (Exists)super.visit(exp);
    Set<VariableSymbol> fvars = FreeVars.compute(exp0.exp);
    VariableSymbol var = (VariableSymbol)exp0.tvar.id.getSymbol();
    if (!fvars.contains(var)) return exp0.exp;
    return exp0;
  }
}
// ----------------------------------------------------------------------------
// end of file
// ----------------------------------------------------------------------------