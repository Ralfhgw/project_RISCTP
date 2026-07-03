// ---------------------------------------------------------------------------
// Pruning.java
// Remove all declarations not relevant for proving selected theorems.
// $Id: Pruning.java,v 1.21 2024/06/03 08:39:45 schreine Exp $
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

public class Pruning extends ASTVisitor.Base<Void>
{
  /****************************************************************************
   * Get rid of declarations not relevant for proven given theorems.
   * @param problem a proof problem.
   * @param theorems the set of theorems to be proven
   * @param mode: 0: only type-checking theorems are preserved.
   *              1: type-checking theorems are not preserved.
   *              2: both kinds of theorems are preserved.
   * @param fixed: set of functions whose semantics is considered as "fixed"
   *               (not constrained by axioms)
   * @return an equivalent problem without irrelevant declarations.
   ***************************************************************************/
  public static ProofProblem process(ProofProblem problem, 
    Set<FormulaSymbol> theorems, int mode, Set<FunctionSymbol> fixed)
  {
    Pruning pruner = new Pruning(problem, theorems, mode, fixed);
    pruner.process();
    return pruner.result;
  }
  
  // the proof problem to be processed
  private ProofProblem problem;
 
  // the theorems to be proved
  private Set<FormulaSymbol> theorems;
  
  // the proving mode
  private int mode;
 
  // the set of functions with "fixed" semantics (not constrained by axioms)
  private Set<FunctionSymbol> fixed;
  
  // the generated proof problem
  private ProofProblem result;
  
  private Pruning(ProofProblem problem, Set<FormulaSymbol> theorems, int mode,
    Set<FunctionSymbol> fixed)
  {
    this.problem = problem;
    this.theorems = theorems;
    this.mode = mode;
    this.fixed = fixed;
    this.result = new ProofProblem(problem);
    result.decls = new ArrayList<Decl>();
  }
  
  // the dependencies of the current symbol
  private Set<Symbol> currentSet;
  
  private void process()
  {
    // generate the reflexive dependency map
    Map<Symbol,Set<Symbol>> map = new HashMap<Symbol,Set<Symbol>>();
    for (Decl decl : problem.decls)
    {
      Symbol currentSymbol = Symbol.getSymbol(decl);
      currentSet = new HashSet<Symbol>();
      currentSet.add(currentSymbol);
      map.put(currentSymbol, currentSet);
      decl.accept(this);
    }
    
    // add dependencies to type checking theorems
    if (mode != 1)
    {
      for (Symbol symbol : result.typeTheoremMap.keySet())
      {
        Set<Symbol> symbols = map.get(symbol);
        if (symbols == null) continue;
        symbols.addAll(result.typeTheoremMap.get(symbol));
      }
    }

    // generate the transitive closure of the map
    transitiveClosure(map);
    
    // invert the dependencies from axioms to constrained entities
    for (Symbol symbol : new HashSet<Symbol>(map.keySet()))
    {
      if (!(symbol instanceof FormulaSymbol)) continue;
      FormulaSymbol symbol0 = (FormulaSymbol)symbol;
      if (!symbol0.axiom) continue;
      axiomDependencies(symbol0, map);
    }
    
    // generate the transitive closure of the map
    transitiveClosure(map);
    
    // keep all declarations (also indirectly) referenced by some theorem to be proved
    for (Decl decl : problem.decls)
    {
      Symbol symbol = Symbol.getSymbol(decl);
      if (mode == 0 && theorems.contains(symbol)) continue;
      for (FormulaSymbol theorem : theorems)
      {
        Set<Symbol> symbols = map.get(theorem);
        if (symbols.contains(symbol))
        {
          result.decls.add(decl);
          break;
        }
      }
    }
  }

  /****************************************************************************
   * Generate the transitive closure of a map.
   * @param map the map (updated to the transitive closure)
   ***************************************************************************/
  private void transitiveClosure(Map<Symbol,Set<Symbol>> map)
  {
    boolean changed;
    do
    {
      changed = false;
      for (Symbol symbol1 : new HashSet<Symbol>(map.keySet()))
      {
        Set<Symbol> symbols1 = map.get(symbol1);
        int n = symbols1.size();
        for (Symbol symbol2 : new HashSet<Symbol>(symbols1))
        {
          Set<Symbol> symbols2 = map.get(symbol2);
          if (symbols2 != null) symbols1.addAll(symbols2);
        }
        if (n != symbols1.size()) changed = true;
      }
    }
    while (changed);
  }
  
  /****************************************************************************
   * For every function symbol whose interpretation is constrained by the axiom, 
   * create a dependency from the symbol to the axiom.
   * @param axiom the axiom.
   * @param map the map to which the dependencies are added.
   ***************************************************************************/
  private void axiomDependencies(FormulaSymbol axiom, Map<Symbol,Set<Symbol>> map)
  {
    FunctionSymbol choice = problem.chooseMap.get(axiom);
    if (choice != null)
    {
      // axiom only constrains choice function
      Set<Symbol> symbols = map.get(choice);
      symbols.add(axiom);
      return;
    }
    
    // the axioms arising from function definitions
    Set<FormulaSymbol> defAxioms =  new HashSet<FormulaSymbol>(problem.defAxioms.values());
    
    // consider every function referenced by the axiom
    for (Symbol symbol : map.get(axiom))
    {
      if (!(symbol instanceof FunctionSymbol)) continue;
      FunctionSymbol fun = (FunctionSymbol)symbol;
      // the defining axiom of *this* function affects this function
      // any other defining axiom does not affect this function
      // any other axiom does not affect a function with fixed semantics
      if (axiom != problem.defAxioms.get(fun))
      {
        if (defAxioms.contains(axiom)) continue;
        if (fixed.contains(fun)) continue;
      }
      Set<Symbol> symbols = map.get(fun);
      if (symbols == null) continue;
      symbols.add(axiom);
    }
  }
  
  public Void visit(Id id)
  {
    Symbol symbol = id.getSymbol();
    if (!(symbol instanceof VariableSymbol))
      currentSet.add(symbol);
    return null;
  }
  
  public Void visit(Function decl)
  {
    decl.id.accept(this);
    for (TypedVar tvar : decl.tvars)
      tvar.accept(this);
    TypeSymbol tsymbol = (TypeSymbol)decl.type.id.getSymbol();
    // predicate type "Bool" shall not cause a dependency to value type "Bool"
    if (tsymbol.root != problem.boolSymbol)
      decl.type.accept(this);
    if (decl.exp != null) decl.exp.accept(this);
    return null;
  }
}
// ----------------------------------------------------------------------------
// end of file
// ----------------------------------------------------------------------------