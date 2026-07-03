// ---------------------------------------------------------------------------
// Substitution.java
// Perform variable substitutions in expressions.
// $VariableSymbol: Substitution.java,v 1.1 2022/05/16 15:27:40 schreine Exp schreine $
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
package risctp.syntax;

import java.util.*;
import java.util.function.*;

import risctp.syntax.AST.*;
import risctp.syntax.AST.Exp.*;
import risctp.types.Symbol.*;

public class Substitution extends HashMap<VariableSymbol,Exp>
{
  private static final long serialVersionUID = 20230302L;

  /****************************************************************************
   * Create an empty substitution.
   ***************************************************************************/
  public Substitution() { }
  
  /****************************************************************************
   * Create a substitution with a single variable mapped to an expression.
   * @param var the variable.
   * @param exp the expression.
   ***************************************************************************/
  public Substitution(VariableSymbol var, Exp exp)
  {
    put(var, exp);
  }
  
  /****************************************************************************
   * Create a substitution from an existing mapping of variables to expressions.
   * @param map the mapping.
   ***************************************************************************/
  public Substitution(Map<VariableSymbol,Exp> map)
  {
    super(map);
  }

  /****************************************************************************
   * Apply this substitution to an expression.
   * @param exp the expression.
   * @return a clone of 'exp' with every variable in the support of this
   * substitution replaced by the expression to which it is mapped. 
   * Please note that among all quantified expressions only Forall and Exist 
   * formulas are considered.
   ***************************************************************************/
  public Exp apply(Exp exp)
  {
    Cloner cloner = new Cloner();
    return (Exp)exp.accept(cloner);
  }
  
  // the cloner for performing substitutions
  private class Cloner extends ASTCloner
  {
    public Cloner()
    {
      // ids are not cloned and thus preserve symbols
      super(false);
    }
    
    public Exp visit(Var var)
    {
      VariableSymbol symbol = (VariableSymbol)var.id.getSymbol();
      Exp exp = Substitution.this.get(symbol);
      if (exp != null) return exp;
      return var;
    }

    public Exp visit(Forall formula)
    {
      return visitQuantified(formula.tvar, formula.exp, 
          (TypedVar tvar, Exp body)->Forall.create(tvar, body));
    }

    public Exp visit(Exists formula)
    {
      return visitQuantified(formula.tvar, formula.exp, 
          (TypedVar tvar, Exp body)->Exists.create(tvar, body));
    }

    /****************************************************************************
     * Substitute in a quantified formula with given typed variable and body
     * and formula constructor.
     * @param tvar the typed variable.
     * @param body the formula body.
     * @param create the formula constructor.
     * @return a version of the formula with the current substitution performed.
     ***************************************************************************/
    private Exp visitQuantified(TypedVar tvar, Exp body, 
      BiFunction<TypedVar,Exp,Exp> create)
    {
      VariableSymbol var = (VariableSymbol)tvar.id.getSymbol();
      Set<VariableSymbol> free = FreeVars.compute(body);
      free.remove(var);
      Substitution subst = new Substitution();
      Set<VariableSymbol> vars0 = new HashSet<VariableSymbol>();
      for (VariableSymbol key : free)
      {
        Exp exp = Substitution.this.get(key);
        if (exp == null) continue;
        subst.put(key, exp);
        vars0.addAll(FreeVars.compute(exp));
      }
      if (containsName(vars0, var.id.toString())) 
      { 
        VariableSymbol var0 = rename(var, vars0);
        subst.put(var, Var.create(var0.id));
        var = var0;
      }
      Exp body0 = subst.apply(body);
      return create.apply(TypedVar.create(var.id, tvar.type), body0);
    }

    /****************************************************************************
     * Return a variant of a variable symbol whose name is different from those
     * of a given set of variables.
     * @param var the variable symbol to be renamed.
     * @param vars the given set of variables.
     * @return a variable symbol whose type is that of the given symbol and whose
     * name is a variant of the symbol's name that does not conflict with the 
     * names of the given variable symbols.
     ***************************************************************************/
    private VariableSymbol rename(VariableSymbol var, Set<VariableSymbol> vars)
    {
      String base = var.id.toString();
      int n = base.length();
      while (true)
      {
        // remove number suffix from base name
        if (n == 1) break;
        int ch = base.charAt(n-1);
        if (!Character.isDigit(ch)) break;
        n--;
        base = base.substring(0, n);
      }
      int counter = 0;
      while (true)
      {
        String name = base + counter;
        if (!containsName(vars, name))
        {
          Id id0 = Id.create(name);
          VariableSymbol var0 = new VariableSymbol(id0, var.tsymbol);
          id0.setSymbol(var0);
          return var0;
        }
        counter++;
      }
    }

    /****************************************************************************
     * Determine whether any variable symbol in given set has given name.
     * @param vars a set of variable symbols.
     * @param name a name.
     * @return true if some element of the set has the name.
     ***************************************************************************/
    private boolean containsName(Set<VariableSymbol> vars, String name)
    {
      for (VariableSymbol var : vars)
      {
        if (name.equals(var.id.toString())) return true;
      }
      return false;
    }

  }
}
// ----------------------------------------------------------------------------
// end of file
// ----------------------------------------------------------------------------
