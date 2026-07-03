// ---------------------------------------------------------------------------
// Datatypes.java
// Replace "match" expressions.
// $Id: Datatypes.java,v 1.8 2023/10/25 12:18:17 schreine Exp $
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
import risctp.syntax.AST.Exp.*;
import risctp.syntax.AST.Pattern.*;
import risctp.types.Symbol.*;

public class Datatypes extends ASTCloner
{
  /****************************************************************************
   * Replace in proof problem "match" expressions.
   * @param problem the proof problem.
   * @return an equivalent problem where the replacements have been performed.
   ***************************************************************************/
  public static ProofProblem process(ProofProblem problem)
  {
    Datatypes cloner = new Datatypes(problem);
    return cloner.result;
  }
  
  private ProofProblem problem;
  private ProofProblem result;
  
  private Datatypes(ProofProblem problem)
  {
    super(false);
    this.problem = problem;
    this.result = new ProofProblem(problem);
    this.result.decls = new ArrayList<Decl>();
    for (Decl decl : problem.decls)
    {
      result.decls.add((Decl)decl.accept(this));
      // preserve datatype declarations for application of SMT in MESON
      /*
      if (!(decl instanceof DataType))
      {
        result.decls.add((Decl)decl.accept(this));
        continue;
      }
      DataType decl0 = (DataType)decl;
      for (DataTypeItem item : decl0.items)
      {
        result.decls.add(TypeDecl.create(item.id, null, null));
      }
      */
    }
  }
  
  public Exp visit(Match exp)
  {
    exp = (Match)super.visit(exp);
    Exp exp0 = null;
    MatchBinder[] binders = exp.binders;
    int n = binders.length;
    for (int i = n-1; i >= 0; i--)
      exp0 = translate(exp.exp, binders[i], exp0);
    return exp0;
  }
  
  private Exp translate(Exp exp, MatchBinder binder, Exp exp0)
  {
    if (binder.pattern instanceof DefaultPattern)
      return binder.exp;
    ConstrPattern pattern = (ConstrPattern)binder.pattern;
    FunctionSymbol constructor = (FunctionSymbol)pattern.id.getSymbol();
    FunctionSymbol tester = problem.testerMap.get(constructor);
    List<FunctionSymbol> selectors = problem.selectorMap.get(constructor);
    List<Exp> args = new ArrayList<Exp>(1);
    args.add(exp);
    int n = selectors.size();
    Exp result = binder.exp;
    if (n > 0)
    {
      List<LetBinder> binders = new ArrayList<LetBinder>(n);
      for (int i = 0; i < n; i++)
      {
        binders.add(LetBinder.create(pattern.tvars[i].id, 
            Apply.create(selectors.get(i).id, args)));
      }
      result = Let.create(binders, result);
    }
    if (exp0 == null) return result;
    // prevent condition to become a proof goal in MESON
    Apply condition = Apply.create(tester.id, args);
    condition.setNotGoal(true);
    return If.create(condition, result, exp0);
  }
}
// ----------------------------------------------------------------------------
// end of file
// ----------------------------------------------------------------------------
