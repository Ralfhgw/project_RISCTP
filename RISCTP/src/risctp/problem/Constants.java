// ---------------------------------------------------------------------------
// Constants.java
// Inline constant definitions.
// $Id: Constants.java,v 1.4 2023/12/14 14:57:21 schreine Exp $
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
import risctp.syntax.AST.Decl.Function;
import risctp.types.*;
import risctp.types.Symbol.*;

public class Constants extends ASTCloner
{
  // a map of constant symbols to their defining equations
  private Map<FunctionSymbol,Exp> cdefs = new HashMap<FunctionSymbol,Exp>();
  
  // a true cloner (without cloning identifiers)
  private ASTCloner cloner = new ASTCloner(false);
  
  /****************************************************************************
   * Inline constant definitions.
   * @param problem a proof problem.
   * @return an equivalent problem without constants.
   ***************************************************************************/
  public static ProofProblem process(ProofProblem problem)
  {
    Constants transformer = new Constants();
    ProofProblem result = new ProofProblem(problem);
    result.decls = new ArrayList<Decl>();
    for (Decl decl : problem.decls)
    {
      Decl decl0 = (Decl)decl.accept(transformer);
      if (decl0 instanceof Function)
      {
        Function decl1 = (Function)decl0;
        // we could/should replace the names of constants without definitions
        // by fresh names that cannot be captured by quantification
        // however, this is only a 'cosmetical' problem because
        // the prover distinguishes between variables and constants anyway
        if (decl1.tvars.length == 0 && decl1.exp != null)
        {
          FunctionSymbol fsymbol = (FunctionSymbol)decl1.id.getSymbol();
          transformer.cdefs.put(fsymbol, decl1.exp);
          continue;
        }
      }
      result.decls.add(decl0);
    }
    return result;
  }

  // do not clone identifiers
  public Constants() { super(false); }
  public Exp visit(Apply apply)
  {
    if (apply.exps.length != 0) return super.visit(apply);
    Symbol symbol = apply.id.getSymbol();
    Exp exp = cdefs.get(symbol);
    if (exp == null) return apply;
    return (Exp)exp.accept(cloner);
  }
}
//----------------------------------------------------------------------------
//end of file
//----------------------------------------------------------------------------