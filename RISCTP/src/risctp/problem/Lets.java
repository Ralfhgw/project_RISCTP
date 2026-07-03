// ---------------------------------------------------------------------------
// Lets.java
// Replace let expressions by performing expression substitutions.
// $Id: Lets.java,v 1.4 2023/03/02 07:37:44 schreine Exp $
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

public class Lets extends ASTCloner
{
  /****************************************************************************
   * Replace let expressions by performing variable substitutions.
   * @param problem a proof problem.
   * @return an equivalent problem without let expressions.
   ***************************************************************************/
  public static ProofProblem process(ProofProblem problem)
  {
    Lets cloner = new Lets(problem);
    return cloner.process();
  }
    
  // the proof problem to be processed/returned
  private ProofProblem problem;
  private ProofProblem result;
  
  private Lets(ProofProblem problem)
  {
    super(false);
    this.problem = problem;
    result = new ProofProblem(problem);
    result.decls = new ArrayList<Decl>(result.decls.size());
  }
  
  private ProofProblem process()
  {
    for (Decl decl : problem.decls)
      result.decls.add((Decl)decl.accept(this));
    return result;
  }
  
  public Exp visit(Let exp)
  {
    exp = (Let)super.visit(exp);
    Substitution subst = new Substitution();
    for (LetBinder binder : exp.binders)
    {
      VariableSymbol symbol = (VariableSymbol)binder.id.getSymbol();
      subst.put(symbol, binder.exp);
    }
    return subst.apply(exp.exp);
  }
}
// ----------------------------------------------------------------------------
// end of file
// ----------------------------------------------------------------------------
