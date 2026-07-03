// ---------------------------------------------------------------------------
// Subtypes.java
// Get rid of subtypes by adding explicit type constraints.
// $Id: Subtypes.java,v 1.16 2024/05/31 05:49:53 schreine Exp $
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
package risctp.problem;

import java.util.*;

import risctp.syntax.*;
import risctp.syntax.AST.*;
import risctp.syntax.AST.Decl.*;
import risctp.syntax.AST.Exp.*;
import risctp.types.*;
import risctp.types.Symbol.*;

public class Subtypes extends ASTCloner
{
  /****************************************************************************
   * Get rid of subtypes by adding explicit type constraints.
   * @param problem a proof problem.
   * @return an equivalent problem without subtypes.
   ***************************************************************************/
  public static ProofProblem process(ProofProblem problem)
  {
    Subtypes cloner = new Subtypes(problem);
    return cloner.process();
  }
  
  // the proof problem to be processed
  private ProofProblem problem;
 
  // the generated proof problem
  private ProofProblem result;

  private Subtypes(ProofProblem problem)
  {
    super(false); // do not clone ids to preserve their symbols
    this.problem = problem;
    this.result = new ProofProblem(problem);
    result.decls = new ArrayList<Decl>();
  }
  
  /****************************************************************************
   * Process the current proof problem.
   * @return the proof problem after adding type constraints.
   ***************************************************************************/
  private ProofProblem process()
  {
    for (Decl decl : problem.decls)
    {
      if (decl instanceof Axiom)
      {
        Axiom decl0 = (Axiom)decl;
        FormulaSymbol symbol = (FormulaSymbol)decl0.id.getSymbol();
        if (result.heightAxioms.contains(symbol))
        {
          // skip constraint from universally quantified variable of "height" axiom
          Forall formula0 = (Forall)decl0.formula;
          Exp exp0 = (Exp)formula0.exp.accept(this);
          Axiom decl1 = Axiom.create(decl0.id, Forall.create(formula0.tvar, exp0));
          result.decls.add(decl1);
          continue;
        }
      }
      Decl decl0 = (Decl)decl.accept(this);
      if (decl0 instanceof TypeDecl)
      {
        // get rid of subtype constraint in declaration (remains in symbol)
        TypeDecl decl1 = (TypeDecl)decl0;
        TypeDecl decl2 = TypeDecl.create(decl1.id, decl1.type, null);
        result.decls.add(decl2);
      }
      else if (decl0 instanceof DataType)
      {
        DataType decl1 = (DataType)decl0;
        List<DataTypeItem> items = new ArrayList<DataTypeItem>(decl1.items.length);
        for (DataTypeItem item : decl1.items)
          items.add(DataTypeItem.create(item.id, Arrays.asList(item.constrs), null));
        DataType decl2 = DataType.create(items);
        result.decls.add(decl2);
      }
      else if (decl0 instanceof Function)
      {
        Function decl1 = (Function)decl0;
        result.decls.add(decl1);
        if (decl1.exp == null)
        {
          // create type axiom instead of (later) defining axiom
          Axiom decl2 = typeAxiom(decl1);
          if (decl2 != null) result.decls.add(decl2);
        }
      }
      else
        result.decls.add(decl0);
    }
    return result;
  }
  
  /***************************************************************************
   * Create type axiom for declaration
   * @param decl the declaration
   * @return the type axiom (null, if axiom is trivial)
   **************************************************************************/
  public Axiom typeAxiom(Function decl)
  {
    FunctionSymbol fsymbol = (FunctionSymbol)decl.id.getSymbol();
    TypeSymbol tsymbol = fsymbol.tsymbol;
    FunctionSymbol pred = tsymbol.getPred();
    if (pred == null) return null;
    Id id = decl.id;
    int n = decl.tvars.length;
    List<Exp> args = new ArrayList<Exp>(n);
    Exp condition = null;
    for (TypedVar tvar : decl.tvars)
    {
      Id id0 = tvar.id;
      args.add(Var.create(id0));
      // drop parameter type predicate from "height" function
      if (problem.heightMap.containsValue(fsymbol)) continue;
      VariableSymbol vsymbol = (VariableSymbol)id0.getSymbol();
      FunctionSymbol pred0 = vsymbol.tsymbol.getPred();
      if (pred0 == null) continue;
      condition = Exp.and(condition, TypeChecker.applyFun(pred0, Var.create(id0)));
    }
    Exp formula = TypeChecker.applyFun(pred, Apply.create(id, args));
    formula = Exp.imp(condition, formula);
    for (int i = n-1; i >= 0; i--)
      formula = Forall.create(decl.tvars[i], formula);
    Id aid = Id.create(AST.unquote(id.toString())+AST.internal("type"));
    FormulaSymbol asymbol = new FormulaSymbol(aid, true);
    aid.setSymbol(asymbol);
    return Axiom.create(aid, formula);
  }
  
  public Forall visit(Forall formula)
  {
    Forall formula0 = (Forall)super.visit(formula);
    VariableSymbol vsymbol = (VariableSymbol)formula0.tvar.id.getSymbol();
    FunctionSymbol pred = vsymbol.tsymbol.getPred();
    if (pred == null) return formula0;
    return Forall.create(formula0.tvar,
        Imp.create(TypeChecker.applyFun(pred, Var.create(formula.tvar.id)), 
            formula0.exp));
  } 
  
  public Exists visit(Exists formula)
  {
    Exists formula0 = (Exists)super.visit(formula);
    VariableSymbol vsymbol = (VariableSymbol)formula0.tvar.id.getSymbol();
    FunctionSymbol pred = vsymbol.tsymbol.getPred();
    if (pred == null) return formula0;
    return Exists.create(formula0.tvar,
        And.create(TypeChecker.applyFun(pred, Var.create(formula.tvar.id)), 
            formula0.exp));
  } 
  
  public Choose visit(Choose formula)
  {
    Choose formula0 = (Choose)super.visit(formula);
    VariableSymbol vsymbol = (VariableSymbol)formula0.tvar.id.getSymbol();
    FunctionSymbol pred = vsymbol.tsymbol.getPred();
    if (pred == null) return formula0;
    return Choose.create(formula0.kind, formula0.tvar,
        And.create(TypeChecker.applyFun(pred, Var.create(formula.tvar.id)), 
            formula0.exp));
  } 
}
// ----------------------------------------------------------------------------
// end of file
// ----------------------------------------------------------------------------