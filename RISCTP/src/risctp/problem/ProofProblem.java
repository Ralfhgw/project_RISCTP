// ---------------------------------------------------------------------------
// ProofProblem.java
// A problem processed for proving.
// $Id: ProofProblem.java,v 1.27 2024/06/03 08:39:45 schreine Exp $
//
// Author: Wolfgang Schreiner <Wolfgang.Schreiner@risc.jku.at>
// Copyright (C) 2022-, Research Institute for Symbolic Computation (RISC)
// Johannes Kepler University, Linz, Austria, https://www.risc.jku.at
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General public License for more details.
//
// You should have received a copy of the GNU General public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.
// ----------------------------------------------------------------------------
package risctp.problem;

import java.util.*;

import risctp.syntax.AST.*;
import risctp.types.*;
import risctp.types.Symbol.*;

public class ProofProblem
{
  // the list of all declarations
  public List<Decl> decls;
  
  // the list of functions declared with defining terms
  public Set<FunctionSymbol> defFuns;
  
  // the map of defined functions to their defining axioms
  public Map<FunctionSymbol,FormulaSymbol> defAxioms;
  
  // map of types to their "undef" values and equalities/inequalities
  public Map<TypeSymbol,FunctionSymbol> undefs;
  public Map<TypeSymbol,FunctionSymbol> equalities;
  public Map<TypeSymbol,FunctionSymbol> inequalities;
  
  // maps of choose axioms to axiomatized function
  public Map<FormulaSymbol,FunctionSymbol> chooseMap;

  // all generated type checking conditions
  public Set<FormulaSymbol> typeTheorems;

  // map of declaration symbols to set of type checking conditions
  public Map<Symbol,List<FormulaSymbol>> typeTheoremMap;
  
  // the builtin/predefined types
  public TypeSymbol boolSymbol;
  public TypeSymbol intSymbol;
  public TypeSymbol natSymbol; 
  public TypeSymbol nonZeroSymbol;

  // the boolean value symbols
  public FunctionSymbol trueValueSymbol;
  public FunctionSymbol falseValueSymbol;
  
  // collection of integer builtin functions (except equalities and inequalities)
  public Set<FunctionSymbol> intFuns;
  
  // collection of integer axioms
  public Set<FormulaSymbol> intAxioms;
  
  // map types and axioms, maps to their constructors, selectors, store operations
  public Set<TypeSymbol> mapTypes;
  public Set<FormulaSymbol> mapAxioms;
  public Map<TypeSymbol,FunctionSymbol> mapConstructors;
  public Map<TypeSymbol,FunctionSymbol> mapSelectors;
  public Map<TypeSymbol,FunctionSymbol> mapStores;
 
  // map of data type symbols to their declarations
  public Map<TypeSymbol,DataTypeItem> dataMap;
  
  // map of data types to their "height" functions and the axioms characterizing them
  public Map<TypeSymbol,FunctionSymbol> heightMap;
  public Set<FormulaSymbol> heightAxioms;
  
  // collections of data types and corresponding constructors, selectors, testers, axioms
  public Set<TypeSymbol> dataTypes;
  public Set<FunctionSymbol> constructors;
  public Set<FunctionSymbol> selectors;
  public Set<FunctionSymbol> testers;
  public Set<FormulaSymbol> dataTypeAxioms;
  
  // map of constructor to corresponding tester and selectors
  public Map<FunctionSymbol,FunctionSymbol> testerMap;
  public Map<FunctionSymbol,List<FunctionSymbol>> selectorMap;
  
  // set of generated datatype declarations
  public Set<TypeSymbol> generatedDataTypes;
  
  // formulas (maybe axioms) that originated from theorems before decomposition
  public Set<FormulaSymbol> originallyTheorems;
  
  // axioms that are implied by other axioms
  public Set<FormulaSymbol> impliedAxioms;
  
  public ProofProblem(TypeSymbol boolSymbol0, TypeSymbol intSymbol0)
  {
    decls = new ArrayList<Decl>();
    defFuns = new HashSet<FunctionSymbol>();
    defAxioms = new HashMap<FunctionSymbol,FormulaSymbol>();
    undefs = new HashMap<TypeSymbol,FunctionSymbol>();
    equalities = new HashMap<TypeSymbol,FunctionSymbol>();
    inequalities = new HashMap<TypeSymbol,FunctionSymbol>();
    chooseMap = new HashMap<FormulaSymbol,FunctionSymbol>();
    typeTheorems = new HashSet<FormulaSymbol>();
    typeTheoremMap = new HashMap<Symbol,List<FormulaSymbol>>();
    boolSymbol = boolSymbol0;
    intSymbol = intSymbol0;
    natSymbol = intSymbol;     // to be redefined 
    nonZeroSymbol = intSymbol; // to be redefined 
    trueValueSymbol = null; // to be defined
    falseValueSymbol = null; // to be defined
    intFuns = new HashSet<FunctionSymbol>();
    intAxioms = new HashSet<FormulaSymbol>();
    mapTypes = new HashSet<TypeSymbol>();
    mapAxioms = new HashSet<FormulaSymbol>();
    mapConstructors = new HashMap<TypeSymbol,FunctionSymbol>();
    mapSelectors = new HashMap<TypeSymbol,FunctionSymbol>();
    mapStores = new HashMap<TypeSymbol,FunctionSymbol>();
    dataMap = new HashMap<TypeSymbol,DataTypeItem>();
    heightMap = new HashMap<TypeSymbol,FunctionSymbol>();
    heightAxioms = new HashSet<FormulaSymbol>();
    dataTypes = new HashSet<TypeSymbol>();
    constructors = new HashSet<FunctionSymbol>();
    selectors = new HashSet<FunctionSymbol>();
    testers = new HashSet<FunctionSymbol>();
    dataTypeAxioms = new HashSet<FormulaSymbol>();
    testerMap = new HashMap<FunctionSymbol,FunctionSymbol>();
    selectorMap = new HashMap<FunctionSymbol,List<FunctionSymbol>>();
    generatedDataTypes = new HashSet<TypeSymbol>();
    originallyTheorems = new HashSet<FormulaSymbol>();
    impliedAxioms = new HashSet<FormulaSymbol>();
  }
  
  public ProofProblem(ProofProblem problem)
  {
    this.name = problem.name;
    this.decls = problem.decls;
    this.defFuns = problem.defFuns;
    this.defAxioms = problem.defAxioms;
    this.undefs = problem.undefs;
    this.generatedDataTypes = problem.generatedDataTypes;
    this.equalities = problem.equalities;
    this.inequalities = problem.inequalities;
    this.chooseMap = problem.chooseMap;
    this.typeTheorems = problem.typeTheorems;
    this.typeTheoremMap = problem.typeTheoremMap;
    this.boolSymbol = problem.boolSymbol;
    this.intSymbol = problem.intSymbol;
    this.natSymbol = problem.natSymbol; 
    this.nonZeroSymbol = problem.nonZeroSymbol; 
    this.trueValueSymbol = problem.trueValueSymbol;
    this.falseValueSymbol = problem.falseValueSymbol;
    this.intFuns = problem.intFuns;
    this.intAxioms = problem.intAxioms;
    this.mapTypes = problem.mapTypes;
    this.mapAxioms = problem.mapAxioms;
    this.mapConstructors = problem.mapConstructors;
    this.mapSelectors = problem.mapSelectors;
    this.mapStores = problem.mapStores;
    this.dataMap = problem.dataMap;
    this.heightMap = problem.heightMap;
    this.heightAxioms = problem.heightAxioms;
    this.dataTypes = problem.dataTypes;
    this.constructors = problem.constructors;
    this.selectors = problem.selectors;
    this.testers = problem.testers;
    this.dataTypeAxioms = problem.dataTypeAxioms;
    this.testerMap = problem.testerMap;
    this.selectorMap = problem.selectorMap;
    this.originallyTheorems = problem.originallyTheorems;
    this.impliedAxioms = problem.impliedAxioms;
  }
  
  // its name (may be null)
  public String name;
  public void setName(String name) { this.name = name; }
  public String getName() { return this.name; }
}
// ----------------------------------------------------------------------------
// end of file
// ----------------------------------------------------------------------------
