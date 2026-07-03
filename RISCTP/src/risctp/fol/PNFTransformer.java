// ---------------------------------------------------------------------------
// PNFTransformer.java
// The transformation of first-order formulas into prenex normal form.
// $Id: PNFTransformer.java,v 1.8 2023/09/12 08:42:02 schreine Exp $
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
// A cloner that transforms formulas into prenex normal form in analogy to the
// functions "pnf", "prenex", "pullquants", and "pullq" on pages 143 and 144 of
// [John Harrison, "Handbook of Practical Logic and Automated Reasoning"]
//--------------------------------------------------------------------------
public class PNFTransformer extends ASTCloner
{
  /****************************************************************************
   * Transform formula into prenex normal form.
   * @param formula the formula.
   * @return a logically equivalent prenex normal form.
   ***************************************************************************/
  public static Exp transform(Exp formula)
  {
    Exp formula0 = Simplifier.transform(formula);
    Exp formula1 = NNFTransformer.transform(formula0);
    Set<String> vars = Variables.getNames(formula);
    PNFTransformer transformer = new PNFTransformer(vars);
    return (Exp)formula1.accept(transformer);
  }
  
  // collect names of all (also non-free) variables referenced in formula
  // in order to avoid the repeated calculation of free variables
  // for all subformulas performed in "pullquants"/"pullq" of [Harrison]
  private static class Variables extends ASTVisitor.Base<Void>
  {
    public static Set<String> getNames(Exp formula)
    {
      Variables visitor = new Variables();
      formula.accept(visitor);
      return visitor.names;
    }
    private Set<String> names = new HashSet<String>();
    public Void visit(Var var) 
    { 
      names.add(var.toString()); 
      return null;
    }
  }
  
  // the names of all variables that are referenced in the formula.
  private Set<String> vars;
  
  /****************************************************************************
   * Create cloner for transforming a formula into prenex normal form.
   * @param vars the names of all variables that are used in the formula.
   ***************************************************************************/
  private PNFTransformer(Set<String> vars)
  {
    // ids remain shared (and thus preserve symbols)
    super(false);
    this.vars = vars;
  }
  
  // auxiliary to apply cloner recursively
  private Exp pnf(Exp exp) { return (Exp)exp.accept(this); }
   
  // literals remain unchanged
  public Exp visit(Apply exp) { return exp; }
  public Exp visit(Not exp) { return exp; }
  
  // logical connectives as a type
  private enum Connective 
  { 
    And, Or;
    private Exp create(Exp exp1, Exp exp2)
    {
      switch (this)
      {
      case And: return Exp.And.create(exp1, exp2);
      case Or: return Exp.Or.create(exp1, exp2);
      default: return null;
      }
    }
  }
  
  // quantifiers as a type
  private enum Quantifier 
  { 
    Forall, Exists;
    private Exp create(TypedVar tvar, Exp exp)
    {
      switch (this)
      {
      case Forall: return Exp.Forall.create(tvar, exp);
      case Exists: return Exp.Exists.create(tvar, exp);
      default: return null;
      }
    }
  }
  
  // quantifiers are pulled out of And/OR formulas
  public Exp visit(And exp)
  {
    return pull(Connective.And, pnf(exp.exp1), pnf(exp.exp2));
  }
  public Exp visit(Or exp)
  {
    return pull(Connective.Or, pnf(exp.exp1), pnf(exp.exp2));
  }
  
  /****************************************************************************
   * Construct formula composed from subformulas by application of logical 
   * connective but with quantifiers pulled to the outermost level.
   * @param connective the logical connective.
   * @param exp1 the first subformula.
   * @param exp2 the second subformula.
   * @return the constructed formula.
   ***************************************************************************/
  private Exp pull(Connective connective, Exp exp1, Exp exp2)
  {
    if (exp1 instanceof Forall)
    {
      Forall exp10 = (Forall)exp1;
      TypedVar tvar1 = exp10.tvar;
      TypedVar tvar2 = null;
      exp1 = exp10.exp;
      if (exp2 instanceof Forall)
      {
        Forall exp20 = (Forall)exp2;
        tvar2 = exp20.tvar;
        exp2 = exp20.exp;
      }
      return pull(connective, Quantifier.Forall, tvar1, tvar2, exp1, exp2);
    }
    else if (exp1 instanceof Exists)
    {
      Exists exp10 = (Exists)exp1;
      TypedVar tvar1 = exp10.tvar;
      TypedVar tvar2 = null;
      exp1 = exp10.exp;
      if (exp2 instanceof Exists)
      {
        Exists exp20 = (Exists)exp2;
        tvar2 = exp20.tvar;
        exp2 = exp20.exp;
      }
      return pull(connective, Quantifier.Exists, tvar1, tvar2, exp1, exp2);
    }
    else if (exp2 instanceof Forall)
    {
      Forall exp20 = (Forall)exp2;
      TypedVar tvar2 = exp20.tvar;
      exp2 = exp20.exp;
      return pull(connective, Quantifier.Forall, null, tvar2, exp1, exp2);
    }
    else if (exp2 instanceof Exists)
    {
      Exists exp20 = (Exists)exp2;
      TypedVar tvar2 = exp20.tvar;
      exp2 = exp20.exp;
      return pull(connective, Quantifier.Exists, null, tvar2, exp1, exp2);
    }
    return connective.create(exp1, exp2);
  }
  
  /****************************************************************************
   * Construct formula composed from subformulas (potentially quantified
   * with the same quantifier) by application of logical connective but 
   * with the quantifier pulled to the outermost level.
   * @param connective the logical connective.
   * @param quantifier the quantifier.
   * @param tvar1 the quantified variable of the first subformula (may be null)
   * @param tvar2 the quantified variable of the second subformula (may be null)
   * @param exp1 the first subformula (if tvar1 is not null, the formula body).
   * @param exp2 the second subformula (if tvar2 is not null, the formula body).
   * @return the constructed formula.
   ***************************************************************************/
  private Exp pull(Connective connective, Quantifier quantifier,
    TypedVar tvar1, TypedVar tvar2, Exp exp1, Exp exp2)
  {
    TypedVar tvar10 = null;
    TypedVar tvar20 = null; 
    if (tvar1 != null)
    {
      tvar10 = variant(tvar1);
      exp1 = substitute(tvar1, tvar10, exp1);
    }
    if (tvar2 != null)
    {
      tvar20 = variant(tvar2);
      exp2 = substitute(tvar2, tvar20, exp2);
    }
    Exp exp = pull(connective, exp1, exp2);
    if (tvar20 != null) exp = quantifier.create(tvar20, exp);
    if (tvar10 != null && tvar10 != tvar20) exp = quantifier.create(tvar10, exp); 
    return exp;
  }
  
  /****************************************************************************
   * Create a variant of a typed variable.
   * @param tvar the typed variable.
   * @return a variant of the typed variable whose name is different from
   * the name of any variable in the formula.
   ***************************************************************************/
  private TypedVar variant(TypedVar tvar)
  {
    Id id = tvar.id;
    VariableSymbol symbol = (VariableSymbol)id.getSymbol();
    String name0 = Names.unused(id.toString(), vars);
    // add name of variant to set of variable names
    vars.add(name0);
    Id id0 = Id.create(name0);
    VariableSymbol symbol0 = new VariableSymbol(id0, symbol.tsymbol);
    id0.setSymbol(symbol0);
    return TypedVar.create(id0, tvar.type);
  }
  
  /****************************************************************************
   * Substitute a variable in an expression by another variable.
   * @param tvar the typed version of the variable to be substituted.
   * @param tvar0 the typed version of the substituting variable.
   * @param exp the expression.
   * @return a clone of the expression with every occurrence of tvar
   * substituted by tvar0.
   ***************************************************************************/
  private Exp substitute(TypedVar tvar, TypedVar tvar0, Exp exp)
  {
    VariableSymbol symbol = (VariableSymbol)tvar.id.getSymbol();
    Exp var = Var.create(tvar0.id);
    return new Substitution(symbol, var).apply(exp);
  }
}
// ----------------------------------------------------------------------------
// end of file
// ----------------------------------------------------------------------------