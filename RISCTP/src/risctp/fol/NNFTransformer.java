// ---------------------------------------------------------------------------
// NNFTransformer.java
// The transformation of first-order formulas into negation normal form.
// $Id: NNFTransformer.java,v 1.6 2023/12/14 09:30:58 schreine Exp $
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

// --------------------------------------------------------------------------
// A cloner that transforms formulas into negation normal form 
// in analogy to the function "nnf" on page 141 of
// [John Harrison, "Handbook of Practical Logic and Automated Reasoning"]
//--------------------------------------------------------------------------
public class NNFTransformer extends ASTCloner
{
  /****************************************************************************
   * Transform formula into negation normal form.
   * @param formula the formula.
   * @return a logically equivalent negation normal form.
   ***************************************************************************/
  public static Exp transform(Exp formula)
  {
    NNFTransformer transformer = new NNFTransformer();
    return (Exp)formula.accept(transformer);
  }
  
  private NNFTransformer()
  {
    // ids remain shared (and thus preserve symbols)
    super(false);
  }
  
  // auxiliary to apply cloner recursively
  private Exp nnf(Exp exp) { return (Exp)exp.accept(this); }
   
  // the various visit methods (the cases And and Or are treated by 
  // the base class implementations)
  
  public Exp visit(Apply exp)
  {
    // avoid cloning of atomic formulas
    return exp;
  }
  
  public Exp visit(Imp exp)
  {
    return Or.create(nnf(Not.create(exp.exp1)), nnf(exp.exp2));
  }
  
  public Exp visit(Equiv exp)
  {
    // conjunctive form to simplify clause generation
    return And.create(
        Or.create(nnf(Not.create(exp.exp1)), nnf(exp.exp2)),
        Or.create(nnf(exp.exp1), nnf(Not.create(exp.exp2))));
  }
  
  public Exp visit(Not exp)
  {
    if (exp.exp instanceof Not)
    {
      Not exp0 = (Not)exp.exp;
      return nnf(exp0.exp);
    }
    else if (exp.exp instanceof And)
    {
      And exp0 = (And)exp.exp;
      return Or.create(nnf(Not.create(exp0.exp1)), nnf(Not.create(exp0.exp2)));
    }
    else if (exp.exp instanceof Or)
    {
      Or exp0 = (Or)exp.exp;
      return And.create(nnf(Not.create(exp0.exp1)), nnf(Not.create(exp0.exp2)));
    }
    else if (exp.exp instanceof Imp)
    {
      Imp exp0 = (Imp)exp.exp;
      return And.create(nnf(exp0.exp1), nnf(Not.create(exp0.exp2)));
    }
    else if (exp.exp instanceof Equiv)
    {
      Equiv exp0 = (Equiv)exp.exp;
      return Or.create(
          And.create(nnf(exp0.exp1), nnf(Not.create(exp0.exp2))),
          And.create(nnf(Not.create(exp0.exp1)), nnf(exp0.exp2)));
    }
    else if (exp.exp instanceof Forall)
    {
      Forall exp0 = (Forall)exp.exp;
      return Exists.create(exp0.tvar, nnf(Not.create(exp0.exp)));
    }
    else if (exp.exp instanceof Exists)
    {
      Exists exp0 = (Exists)exp.exp;
      return Forall.create(exp0.tvar, nnf(Not.create(exp0.exp)));
    }
    return exp;
  }
}
// ----------------------------------------------------------------------------
// end of file
// ----------------------------------------------------------------------------