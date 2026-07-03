// ---------------------------------------------------------------------------
// Operations.java
// Reduction of the number of builtin operations.
// $Id: Operations.java,v 1.1 2024/02/01 13:43:55 schreine Exp $
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
import risctp.types.Symbol.*;

// --------------------------------------------------------------------------
// A cloner that reduces the number of builtin operations.
//--------------------------------------------------------------------------
public class Operations extends ASTCloner
{
  // the relevant theory symbols
  private FunctionSymbol lessPred;
  private FunctionSymbol lessEqualPred;
  private FunctionSymbol greaterPred;
  private FunctionSymbol greaterEqualPred;
  
  // for replacement of inequalities by equalities
  private Collection<FunctionSymbol> inequalities;
  private Map<TypeSymbol,FunctionSymbol> equalities;
  
  // the problem after simplification
  private ProofProblem result;
  
  /****************************************************************************
   * Transform the proof problem.
   * @param problem the proof problem.
   * @return the transformed version.
   ***************************************************************************/
  public static ProofProblem process(ProofProblem problem)
  {
    Operations cloner = new Operations(problem);
    return cloner.result;
  }
  
  private Operations(ProofProblem problem)
  {
    // ids remain shared (and thus preserve symbols)
    super(false);
    // gather relevant theory symbols
    for (FunctionSymbol fun : problem.intFuns)
    {
      String name = fun.id.toString();
      switch (name)
      {
      case AST.LESSNAME: lessPred = fun; break;
      case AST.LESSEQUALNAME: lessEqualPred = fun; break;
      case AST.GREATERNAME: greaterPred = fun; break;
      case AST.GREATEREQUALNAME: greaterEqualPred = fun; break;
      }
    }
    inequalities = problem.inequalities.values();
    equalities = problem.equalities;
    
    // perform transformation
    this.result = new ProofProblem(problem);
    this.result.decls = new ArrayList<Decl>();
    for (Decl decl : problem.decls)
      result.decls.add((Decl)decl.accept(this));
  }

  public Exp visit(Apply exp)
  {
    FunctionSymbol fun = (FunctionSymbol)exp.id.getSymbol();
    if (fun == greaterPred)
    {
      // x > y <=> y < x
      List<Exp> args = new ArrayList<Exp>(2);
      args.add((Exp)exp.exps[1].accept(this));
      args.add((Exp)exp.exps[0].accept(this));
      return Apply.create(lessPred.id, args);
    }
    if (fun == greaterEqualPred)
    {
      // x >= y <=> y <= x
      List<Exp> args = new ArrayList<Exp>(2);
      args.add((Exp)exp.exps[1].accept(this));
      args.add((Exp)exp.exps[0].accept(this));
      return Apply.create(lessEqualPred.id, args);
    }
    /*
    if (fun == lessEqualPred)
    {
      // x <= y <=> !(y < x)
      List<Exp> args = new ArrayList<Exp>(2);
      args.add((Exp)exp.exps[1].accept(this));
      args.add((Exp)exp.exps[0].accept(this));
      return Not.create(Apply.create(lessFun.id, args));
    }
    */
    if (inequalities.contains(fun))
    {
      // also applied for non-integer types
      // x ~= y <=> !(x == y)
      TypeSymbol tsymbol = fun.tsymbols.get(0);
      FunctionSymbol equality = equalities.get(tsymbol);
      List<Exp> args = new ArrayList<Exp>(2);
      args.add((Exp)exp.exps[0].accept(this));
      args.add((Exp)exp.exps[1].accept(this));
      return Not.create(Apply.create(equality.id, args));
    }
    return super.visit(exp);
  }
}
// ----------------------------------------------------------------------------
// end of file
// ----------------------------------------------------------------------------