// ---------------------------------------------------------------------------
// Definitions.java
// Replace definitions of constants/functions/predicates by axioms.
// $Id: Definitions.java,v 1.8 2023/12/14 16:18:18 schreine Exp $
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
import risctp.syntax.AST.Decl.Function;
import risctp.syntax.AST.Exp.*;
import risctp.types.Symbol.*;

public class Definitions
{
  /****************************************************************************
   * Replace definitions by axioms.
   * @param problem a proof problem.
   * @return an equivalent problem without definitions.
   ***************************************************************************/
  public static ProofProblem process(ProofProblem problem)
  {
    ProofProblem result = new ProofProblem(problem);
    result.decls = new ArrayList<Decl>();
    for (Decl decl : problem.decls)
    {
      if (!(decl instanceof Function))
      {
        result.decls.add(decl);
        continue;
      }
      Function decl0 = (Function)decl;
      if (decl0.exp == null)
      {
        result.decls.add(decl);
        continue;
      }
      // declaration without defining expression
      Id id = decl0.id;
      TypedVar[] tvars = decl0.tvars;
      Type type = decl0.type;
      Function decl1 = Function.create(id, Arrays.asList(tvars), type, null);
      result.decls.add(decl1);
      // axiom
      List<Exp> args = new ArrayList<Exp>(tvars.length);
      for (TypedVar tvar : tvars)
        args.add(Var.create(tvar.id));
      Exp apply = Apply.create(id, args);
      TypeSymbol tsymbol = (TypeSymbol)type.id.getSymbol();
      boolean isFormula = tsymbol.root == result.boolSymbol;
      Exp formula;
      if (isFormula)
        formula = Equiv.create(apply, decl0.exp);
      else
      {
        List<Exp> exps = new ArrayList<Exp>(2);
        exps.add(apply);
        exps.add(decl0.exp);
        FunctionSymbol eq = result.equalities.get(tsymbol.root);
        formula = Apply.create(eq.id, exps);
      }
      int n = tvars.length;
      if (n > 0)
      {
        for (int i = n-1; i >= 0; i--)
          formula = Forall.create(tvars[i], formula);
      }
      String aname = "def" + AST.internal(Integer.toString(result.decls.size())); 
      Id aid = Id.create(aname);
      Axiom axiom = Axiom.create(aid, formula);
      FormulaSymbol asymbol = new FormulaSymbol(aid, true);
      aid.setSymbol(asymbol);
      result.decls.add(axiom);
      // register in result defining axiom for function
      FunctionSymbol fsymbol = (FunctionSymbol)id.getSymbol();
      result.defAxioms.put(fsymbol, asymbol);
    }
    return result;
  }
}
// ----------------------------------------------------------------------------
// end of file
// ----------------------------------------------------------------------------