// ---------------------------------------------------------------------------
// SNFTransformer.java
// The transformation of first-order formulas into Skolem normal form.
// $Id: SNFTransformer.java,v 1.6 2024/03/25 18:08:22 schreine Exp $
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
// A cloner that transforms formulas into Skolem normal form in analogy to the
// functions "skolemize", "askolemize", "skolem", and "skolem2" on page 149 of
// [John Harrison, "Handbook of Practical Logic and Automated Reasoning"]
//--------------------------------------------------------------------------
public class SNFTransformer extends ASTCloner
{
  /****************************************************************************
   * Transform formula into Skolem normal form.
   * @param formula the formula.
   * @param names the set of names that must not be used for Skolem functions;
   *        the names of the generated Skolem functions are added to this set.
   * @param skolem a set to which the symbols of the generated Skolem functions 
   *        are added.
   * @return a logically equivalent Skolem normal form.
   ***************************************************************************/
  public static Exp transform(Exp formula, Set<String> names, 
    Set<FunctionSymbol> skolem)
  {
    return transform(formula, names, skolem, true);
  }

  /****************************************************************************
   * Skolemize a formula and optionally transform it into prenex normal form.
   * @param formula the formula.
   * @param names the set of names that must not be used for Skolem functions;
   *        the names of the generated Skolem functions are added to this set.
   * @param skolem a set to which the symbols of the generated Skolem functions 
   *        are added.
   * @param pnf if true the result is to be also in prenex normal form
   * @return a logically equivalent Skolemized formula; if 'prenex' is true,
   *         this formula is in Skolem normal form.
   ***************************************************************************/
  public static Exp transform(Exp formula, Set<String> names, 
    Set<FunctionSymbol> skolem, boolean pnf)
  {
    Exp formula0 = NNFTransformer.transform(formula);
    SNFTransformer transformer = new SNFTransformer(names, skolem);
    Exp formula1 = (Exp)formula0.accept(transformer);
    if (!pnf) return formula1;
    return PNFTransformer.transform(formula1);
  }

  // the names that must not be used for Skolem functions
  private Set<String> names;

  // the symbols of all Skolem functions generated in the Skolemization
  private Set<FunctionSymbol> skolem;

  /****************************************************************************
   * Create cloner for replacing existentially quantified variables in
   * a formula by application of Skolem functions.
   * @param names the set of names that must not be used for Skolem functions;
   *        the names of the generated Skolem functions are added to this set.
   * @param skolem a set to which the symbols of the generated Skolem functions 
   *        are added.
   ***************************************************************************/
  private SNFTransformer(Set<String> names, Set<FunctionSymbol> skolem)
  {
    // ids remain shared (and thus preserve symbols)
    super(false);
    this.names = names;
    this.skolem = skolem;
  }

  // the generated Skolem functions
  public Set<FunctionSymbol> getSkolemFuns() { return skolem; }

  // the core action of the transformation
  public Exp visit(Exists exp)
  {
    TypedVar tvar = exp.tvar;
    Id id = tvar.id;

    // free variables of formula become arguments of the Skolem function
    Set<VariableSymbol> vars = FreeVars.compute(exp);

    // create name of Skolem function and add it to set of used function names
    String name = id.toString();
    String prefix = name + "§"; // vars.isEmpty() ? "c_" + name : "f_" + name;
    String name0 = Names.unused(prefix, names);
    names.add(name0);

    // create symbol of Skolem function and add it to set of Skolem functions
    VariableSymbol vsymbol = (VariableSymbol)id.getSymbol();
    List<TypeSymbol> tsymbols = new ArrayList<TypeSymbol>();
    for (VariableSymbol var : vars) tsymbols.add(var.tsymbol);
    Id id0 = Id.create(name0);
    FunctionSymbol symbol0 = new FunctionSymbol(id0, tsymbols, vsymbol.tsymbol);
    id0.setSymbol(symbol0);
    skolem.add(symbol0);

    // create application of Skolem function
    List<Exp> args = new ArrayList<Exp>();
    for (VariableSymbol var : vars) args.add(Var.create(var.id));
    Exp app = Apply.create(id0, args);

    // replace in body reference to variable by application of Skolem function
    Exp body = new Substitution(vsymbol, app).apply(exp.exp);

    // recursively process the body
    return (Exp)body.accept(this);
  }
}
// ----------------------------------------------------------------------------
// end of file
// ----------------------------------------------------------------------------