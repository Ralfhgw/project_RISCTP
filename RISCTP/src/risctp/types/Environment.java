// ---------------------------------------------------------------------------
// Environment.java
// Environments, i.e, mappings of identifiers to symbols.
// $Id: Environment.java,v 1.8 2022/04/13 08:15:02 schreine Exp $
//
// Author: Wolfgang Schreiner <Wolfgang.Schreiner@risc.jku.at>
// Copyright (C) 2022-, Research Institute for Symbolic Computation (RISC)
// Johannes Kepler University, Linz, Austria, https://www.risc.jku.at
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.
// ----------------------------------------------------------------------------
package risctp.types;

import java.util.*;

import risctp.syntax.AST.*;
import risctp.types.Symbol.*;

public class Environment
{
  // the generic table type we use
  private static class Map<S> extends LinkedHashMap<String,S>
  {
    private static final long serialVersionUID = 28091967L;
    public Map() { super(); }
  }
  
  // the separate name spaces (functions may be overloaded)
  private Map<TypeSymbol> types;
  private Map<List<FunctionSymbol>> functions;
  private Map<FormulaSymbol> axioms;
  private Map<FormulaSymbol> theorems;
  
  // the local variable context
  private LinkedList<List<VariableSymbol>> variables;
  
  /****************************************************************************
   * Create an empty environment.
   ***************************************************************************/
  public Environment()
  {
    this.types = new Map<TypeSymbol>();
    this.functions = new Map<List<FunctionSymbol>>();
    this.axioms = new Map<FormulaSymbol>();
    this.theorems = new Map<FormulaSymbol>();
    this.variables = new LinkedList<List<VariableSymbol>>();
  }   
  
  /****************************************************************************
   * Enter a new variable scope.
   ***************************************************************************/
  public void enterVariableScope()
  {
    variables.add(new ArrayList<VariableSymbol>());
  }
  
  /****************************************************************************
   * Exit from the last variable scope.
   ***************************************************************************/
  public void exitVariableScope()
  {
    variables.removeLast();
  }
  
  /****************************************************************************
   * Add variable to environment.
   ***************************************************************************/
  public void putVariable(VariableSymbol symbol)
  {
    List<VariableSymbol> vars = variables.getLast();
    vars.add(symbol);
  }
  
  /****************************************************************************
   * Add variables to environment.
   ***************************************************************************/
  public void putVariables(List<VariableSymbol> symbols)
  {
    List<VariableSymbol> vars = variables.getLast();
    for (VariableSymbol symbol : symbols)
      vars.add(symbol);
  }
  
  /****************************************************************************
   * Get variable associated to id.
   * @param id the id.
   * @return a variable symbol (or null, if id does not denote a variable)
   ***************************************************************************/
  public VariableSymbol getVariable(Id id)
  {
    Iterator<List<VariableSymbol>> it = variables.descendingIterator();
    while (it.hasNext())
    {
      List<VariableSymbol> symbols = it.next();
      for (VariableSymbol s : symbols)
      {
        if (id.equals(s.id)) return s;
      }
    }
    return null;
  }
  
  /***************************************************************************
   * Get contents of environment.
   **************************************************************************/
  public Collection<TypeSymbol> getTypes() { return types.values(); }
  public Collection<FunctionSymbol> getFunctions()
  {
    List<FunctionSymbol> result = new ArrayList<FunctionSymbol>();
    for (Collection<FunctionSymbol> fs : functions.values())
      result.addAll(fs);
    return result;
  }
  public Collection<FormulaSymbol> getAxioms() { return axioms.values(); }
  public Collection<FormulaSymbol> getTheorems() { return theorems.values(); }
  
  /**************************************************************************
   * Put type symbol into environment
   * @param symbol a type symbol (whose name must not yet occur as a type)
   *************************************************************************/
  public void putType(TypeSymbol symbol)
  {
    types.put(symbol.id.toString(), symbol);
  }
  
  /**************************************************************************
   * Get type denoted by identifier.
   * @param id the identifier.
   * @return the associated type (null, if none)
   *************************************************************************/
  public TypeSymbol getType(Id id)
  {
    return types.get(id.toString());
  }
  
  /**************************************************************************
   * Put function symbol into environment
   * @param symbol a function symbol (whose name(argument types) combination
   * must not yet occur as a function)
   *************************************************************************/
  public void putFunction(FunctionSymbol symbol)
  {
    String string = symbol.id.toString();
    List<FunctionSymbol> symbols = functions.get(string);
    if (symbols == null) 
    {
      symbols = new LinkedList<FunctionSymbol>();
      functions.put(string, symbols);
    }
    symbols.add(symbol);
  }
  
  /****************************************************************************
   * Get functions denoted by identifier.
   * @param id the identifier.
   * @return the associated functions (null, if none)
   ***************************************************************************/
  public List<FunctionSymbol> getFunctions(Id id)
  {
    return functions.get(id.toString());
  }
  
  /***************************************************************************
   * Get function denoted by identifier and argument types.
   * @param id the identifier.
   * @param tsymbols the argument types.
   * @return the associated function (null, if none)
   **************************************************************************/
  public FunctionSymbol getFunction(Id id, List<TypeSymbol> tsymbols)
  {
    List<FunctionSymbol> fsymbols = getFunctions(id);
    if (fsymbols == null) return null;
    int n = tsymbols.size();
    for (FunctionSymbol fsymbol : fsymbols)
    {
      List<TypeSymbol> tsymbols0 = fsymbol.tsymbols;
      if (tsymbols0.size() != n) continue;
      Iterator<TypeSymbol> it = tsymbols.iterator();
      Iterator<TypeSymbol> it0 = tsymbols0.iterator();
      while (true)
      {
        if (!it.hasNext() && !it0.hasNext()) return fsymbol;
        if (!it.hasNext() || !it0.hasNext()) break;
        if (it.next().root != it0.next().root) break;
      } 
    }
    return null;
  }
  
  /**************************************************************************
   * Put axiom into environment
   * @param symbol an axiom symbol (whose name must not yet occur as an axiom)
   *************************************************************************/
  public void putAxiom(FormulaSymbol symbol)
  {
    axioms.put(symbol.id.toString(), symbol);
  }
  
  /**************************************************************************
   * Get axiom denoted by identifier.
   * @param id the identifier.
   * @return the associated axiom (null, if none)
   *************************************************************************/
  public FormulaSymbol getAxiom(Id id)
  {
    return axioms.get(id.toString());
  }
  
  /**************************************************************************
   * Put theorem into environment
   * @param symbol a theorem symbol (whose name must not yet occur as a theorem)
   *************************************************************************/
  public void putTheorem(FormulaSymbol symbol)
  {
    theorems.put(symbol.id.toString(), symbol);
  }
  
  /**************************************************************************
   * Get theorem denoted by identifier.
   * @param id the identifier.
   * @return the associated theorem (null, if none)
   *************************************************************************/
  public FormulaSymbol getTheorem(Id id)
  {
    return theorems.get(id.toString());
  }
}
// ----------------------------------------------------------------------------
// end of file
// ----------------------------------------------------------------------------