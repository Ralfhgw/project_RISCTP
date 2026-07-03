// ---------------------------------------------------------------------------
// Overloading.java
// Give overladed functions unique names.
// $Id: Overloading.java,v 1.20 2022/07/05 15:03:57 schreine Exp $
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
import risctp.types.*;
import risctp.types.Symbol.*;

public class Overloading extends ASTCloner
{
  /****************************************************************************
   * Give overloaded functions a unique name.
   * @param problem a proof problem.
   * @return an equivalent problem without overloaded functions (with the
   * equality predicate as the only exception).
   ***************************************************************************/
  public static ProofProblem process(ProofProblem problem)
  {
    Overloading cloner = new Overloading(problem);
    return cloner.process();
  }
  
  // the proof problem to be processed
  private ProofProblem problem;
  
  // the map of already used constant/function/predicate names to counters
  private Map<String,Integer> used;
  
  // the translation of overloaded function symbols to new function symbols
  private Map<FunctionSymbol,FunctionSymbol> translation;

  // the generated proof problem
  private ProofProblem result;

  private Overloading(ProofProblem problem)
  {
    super(true); // option does not matter, visit(Id) is overwritten below
    this.problem = problem;
    this.used = new HashMap<String,Integer>();
    this.translation = new HashMap<FunctionSymbol,FunctionSymbol>();
    result = new ProofProblem(problem);
    result.decls = new ArrayList<Decl>();
    result.defFuns = new HashSet<FunctionSymbol>();
    result.defAxioms = new HashMap<FunctionSymbol,FormulaSymbol>();
    result.intFuns = new HashSet<FunctionSymbol>();
    result.dataMap = new HashMap<TypeSymbol,DataTypeItem>();
    result.heightMap = new HashMap<TypeSymbol,FunctionSymbol>();
    result.constructors = new HashSet<FunctionSymbol>();
    result.selectors = new HashSet<FunctionSymbol>();
    result.testers = new HashSet<FunctionSymbol>();
    result.testerMap =  new HashMap<FunctionSymbol,FunctionSymbol>();
    result.selectorMap = new HashMap<FunctionSymbol,List<FunctionSymbol>>();
    result.equalities = new HashMap<TypeSymbol,FunctionSymbol>();
    result.inequalities = new HashMap<TypeSymbol,FunctionSymbol>();
    result.chooseMap = new HashMap<FormulaSymbol,FunctionSymbol>();
    result.typeTheoremMap = new HashMap<Symbol,List<FormulaSymbol>>();
    result.mapConstructors = new HashMap<TypeSymbol,FunctionSymbol>();
    result.mapSelectors = new HashMap<TypeSymbol,FunctionSymbol>();
    result.mapStores = new HashMap<TypeSymbol,FunctionSymbol>();
  }
  
  /****************************************************************************
   * Process the current proof problem.
   * @return the proof problem after resolving overloading.
   ***************************************************************************/
  private ProofProblem process()
  {
    for (Decl decl : problem.decls)
      result.decls.add((Decl)decl.accept(this));
    translate();
    return result;
  }
  
  /****************************************************************************
   * Translate function sets/maps.
   ***************************************************************************/
  private void translate()
  {
    translateFuns(problem.defFuns, result.defFuns);
    translateFunctionMap(problem.defAxioms, result.defAxioms);
    translateFuns(problem.intFuns, result.intFuns);
    translateTypeMap(problem.heightMap, result.heightMap);
    translateFuns(problem.constructors, result.constructors);
    translateFuns(problem.selectors, result.selectors);
    translateFuns(problem.testers, result.testers);
    translateFunMap(problem.testerMap, result.testerMap);
    translateFunListMap(problem.selectorMap, result.selectorMap);
    translateTypeMap(problem.equalities, result.equalities);
    translateTypeMap(problem.inequalities, result.inequalities);
    translateFormulaMap(problem.chooseMap, result.chooseMap);
    translateFormulaListMap(problem.typeTheoremMap, result.typeTheoremMap);
    translateTypeMap(problem.mapConstructors, result.mapConstructors);
    translateTypeMap(problem.mapSelectors, result.mapSelectors);
    translateTypeMap(problem.mapStores, result.mapStores);
  }
  
  /****************************************************************************
   * Translate a set of function symbols.
   * @param set the set.
   * @param result the translated version of the set (to be filled).
   ***************************************************************************/
  private void translateFuns(Collection<FunctionSymbol> set, Collection<FunctionSymbol> result)
  {
    for (FunctionSymbol elem : set)
    {
      FunctionSymbol elem0 = translation.get(elem);
      if (elem0 == null) elem0 = elem;
      result.add(elem0);
    }
  }
  
  /****************************************************************************
   * Translate a map of function symbols.
   * @param map the map.
   * @param result the translated version of the map (to be filled).
   ***************************************************************************/
  private void translateFunMap(Map<FunctionSymbol,FunctionSymbol> map,
    Map<FunctionSymbol,FunctionSymbol> result)
  {
    for (Map.Entry<FunctionSymbol,FunctionSymbol> entry : map.entrySet())
    {
      FunctionSymbol key = entry.getKey();
      FunctionSymbol value = entry.getValue();
      FunctionSymbol key0 = translation.get(key);
      if (key0 == null) key0 = key;
      FunctionSymbol value0 = translation.get(value);
      if (value0 == null) value0 = value;
      result.put(key0, value0);
    }
  }
  
  /****************************************************************************
   * Translate a map of function symbols to lists of such symbols.
   * @param map the map.
   * @param result the translated version of the map (to be filled).
   ***************************************************************************/
  private void translateFunListMap(Map<FunctionSymbol,List<FunctionSymbol>> map,
    Map<FunctionSymbol,List<FunctionSymbol>> result)
  {
    for (Map.Entry<FunctionSymbol,List<FunctionSymbol>> entry : map.entrySet())
    {
      FunctionSymbol key = entry.getKey();
      List<FunctionSymbol> value = entry.getValue();
      FunctionSymbol key0 = translation.get(key);
      if (key0 == null) key0 = key;
      List<FunctionSymbol> value0 = new ArrayList<FunctionSymbol>(value.size());
      result.put(key0, value0);
      translateFuns(value, value0);
    }
  }

  /****************************************************************************
   * Translate a map of symbols to lists of formula symbols.
   * @param map the map.
   * @param result the translated version of the map (to be filled).
   ***************************************************************************/
  private void translateFormulaListMap(Map<Symbol,List<FormulaSymbol>> map,
    Map<Symbol,List<FormulaSymbol>> result)
  {
    for (Map.Entry<Symbol,List<FormulaSymbol>> entry : map.entrySet())
    {
      Symbol key = entry.getKey();
      List<FormulaSymbol> value = entry.getValue();
      FunctionSymbol key0 = translation.get(key);
      if (key0 != null) key = key0;
      result.put(key, value);
    }
  }
  
  /****************************************************************************
   * Translate a map of type to function symbols.
   * @param map the map.
   * @param result the translated version of the map (to be filled).
   ***************************************************************************/
  private void translateTypeMap(Map<TypeSymbol,FunctionSymbol> map,
    Map<TypeSymbol,FunctionSymbol> result)
  {
    for (Map.Entry<TypeSymbol,FunctionSymbol> entry : map.entrySet())
    {
      TypeSymbol key = entry.getKey();
      FunctionSymbol value = entry.getValue();
      FunctionSymbol value0 = translation.get(value);
      if (value0 == null) value0 = value;
      result.put(key, value0);
    }
  }
  
  /****************************************************************************
   * Translate a map of formula to function symbols.
   * @param map the map.
   * @param result the translated version of the map (to be filled).
   ***************************************************************************/
  private void translateFormulaMap(Map<FormulaSymbol,FunctionSymbol> map,
    Map<FormulaSymbol,FunctionSymbol> result)
  {
    for (Map.Entry<FormulaSymbol,FunctionSymbol> entry : map.entrySet())
    {
      FormulaSymbol key = entry.getKey();
      FunctionSymbol value = entry.getValue();
      FunctionSymbol value0 = translation.get(value);
      if (value0 == null) value0 = value;
      result.put(key, value0);
    }
  }
  
  /****************************************************************************
   * Translate a map of function symbols to formula symbols.
   * @param map the map.
   * @param result the translated version of the map (to be filled).
   ***************************************************************************/
  private void translateFunctionMap(Map<FunctionSymbol,FormulaSymbol> map,
    Map<FunctionSymbol,FormulaSymbol> result)
  {
    for (Map.Entry<FunctionSymbol,FormulaSymbol> entry : map.entrySet())
    {
      FunctionSymbol key = entry.getKey();
      FormulaSymbol value = entry.getValue();
      FunctionSymbol key0 = translation.get(key);
      if (key0 == null) key0 = key;
      result.put(key0, value);
    }
  }
  
  /****************************************************************************
   * Introduce new symbol for overloaded function.
   * @param id the id of a function (which may or may not be overloaded)
   ***************************************************************************/
  private void renameOverloaded(Id id)
  {
    FunctionSymbol symbol = (FunctionSymbol)id.getSymbol();
    id = symbol.id;
    String name = id.toString();
    Integer counter = used.get(name);
    if (counter == null)
    {
      used.put(id.toString(), 0);
      return;
    }
    used.put(id.toString(), counter+1);
    String[] names = id.getStrings();
    int n = names.length;
    names[n-1] = names[n-1] + AST.internal(counter.toString());
    Id id0 = Id.create(names);
    FunctionSymbol symbol0 = new FunctionSymbol(id0, symbol.tsymbols, symbol.tsymbol);
    id0.setSymbol(symbol0);
    translation.put(symbol, symbol0);
  }

  // --------------------------------------------------------------------------
  //
  // the visitors that perform the overloading resolution.
  //
  // --------------------------------------------------------------------------
  
  public Function visit(Function decl)
  {
    FunctionSymbol symbol = (FunctionSymbol)decl.id.getSymbol();
    // already existing translation of datatype operations must be preserved
    if (!problem.constructors.contains(symbol) &&
        !problem.selectors.contains(symbol) &&
        !problem.testers.contains(symbol))
      renameOverloaded(decl.id);
    return super.visit(decl);
  }
  
  public DataTypeItem visit(DataTypeItem item)
  {
    DataTypeItem item0 = super.visit(item);
    TypeSymbol symbol0 = (TypeSymbol)item0.id.getSymbol();
    result.dataMap.put(symbol0, item0);
    return item0;
  }
  
  public DataTypeConstr visit(DataTypeConstr constr)
  {
    renameOverloaded(constr.id);
    FunctionSymbol symbol = (FunctionSymbol)constr.id.getSymbol();
    for (TypedVar tvar : constr.tvars)
      renameOverloaded(tvar.id);
    renameOverloaded(problem.testerMap.get(symbol).id);
    return super.visit(constr);
  }

  public Id visit(Id id)
  {
    // by default, ids are not replicated, thus symbols are preserved
    Symbol symbol = id.getSymbol();
    if (!(symbol instanceof FunctionSymbol)) return id;
    FunctionSymbol symbol0 = (FunctionSymbol)symbol;
    FunctionSymbol symbol1 = translation.get(symbol0);
    if (symbol1 == null) return id;
    return symbol1.id;
  }
}
//----------------------------------------------------------------------------
// end of file
//----------------------------------------------------------------------------