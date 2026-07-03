// ---------------------------------------------------------------------------
// Instances.java
// Generate ground instances of rules.
// $Id: InstancesRes.java,v 1.2 2026/04/14 10:46:24 schreine Exp $
//
// Author: Wolfgang Schreiner <Wolfgang.Schreiner@risc.jku.at>
// Copyright (C) 2023-, Research Institute for Symbolic Computation (RISC)
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
package risctp.res;

import java.util.*;

import risctp.types.*;
import risctp.types.Symbol.*;
import risctp.fol.*;
import risctp.problem.*;

public class InstancesRes
{
  // maximum number of instances per rule, maximum total number of instances
  private static final int MAX_RULE = 2000;
  private static final int MAX_TOTAL = 10000;

  /****************************************************************************
   * Generate a list of ground instances of rules using ground terms that
   * appear in some given rules.
   * @param problem the proof problem from which the rules originate.
   * @param rules the rules to be instantiated.
   * @param sources the rules that serve as sources of ground terms.
   * @return ground instances of the rules.
   ***************************************************************************/
  public static List<PureEquation> generate(ProofProblem problem,
    List<PureEquation> PureEquations, List<PureEquation> sources)
  {
    // determine all ground terms
    GroundTermAnalyzer analyzer = new GroundTermAnalyzer(problem);
    analyzer.analyze(sources);
    List<Object> groundTerms = analyzer.groundTerms();

    // classify the ground terms according to their types
    TermClassifier classifier = new TermClassifier();
    classifier.classify(groundTerms, true);
    Map<TypeSymbol, List<Object>> typeMap = classifier.typeMap();

    // generate rule instances from the type map
    Generator generator = new Generator(typeMap);
    generator.generate(PureEquations);
    return generator.rules();
  }

  // -------------------------------------------------------------------------
  //
  // A rule analyzer that determines ground terms.
  //
  // -------------------------------------------------------------------------
  private static class GroundTermAnalyzer
  {
    // the ground terms resulting from the analysis
    private List<Object> groundTerms;

    // the textual representations of the ground terms
    private Set<String> strings;

    // get all collected ground terms
    public List<Object> groundTerms() { return groundTerms; }

    // the original proof problem
    private ProofProblem problem;

    // problem symbols for creating derived ground terms t+1, t-1
    private FunctionSymbol one;
    private FunctionSymbol plus;
    private FunctionSymbol minus;

    /**************************************************************************
     * Construct the analyzer.
     * @param problem the proof problem from which the terms stem.
     *************************************************************************/
    public GroundTermAnalyzer(ProofProblem problem)
    {
      this.problem = problem;
      groundTerms = new ArrayList<Object>();
      strings = new HashSet<String>();
      for (FunctionSymbol fun : problem.intFuns)
      {
        String name = fun.id.toString();
        switch (name)
        {
        case "1": one = fun; break;
        case "+": plus = fun; break;
        case "-": minus = fun; break;
        }
      }
    }

    /**************************************************************************
     * Analyze the given rules.
     * @param rule the rules to be analyzed.
     * Adds to "groundTerms" all ground (sub)terms that appear in the rules.
     *************************************************************************/
    public void analyze(List<PureEquation> rules)
    {
      for (PureEquation rule : rules) analyze(rule);
    }

    /**************************************************************************
     * Analyze the given rule.
     * @param rule the rule to be analyzed.
     * Adds to "groundTerms" all ground (sub)terms that appear in the rule.
     *************************************************************************/
    public void analyze(PureEquation rule)
    {
      for (Object lit : rule.lits) analyzeTerm(lit);
    }

    /**************************************************************************
     * Analyze the given term.
     * @param term the term to be analyzed.
     * @return true if the term is ground; adds to "groundTerms" all the
     * subterms of the term (including the term itself) that are ground.
     *************************************************************************/
    public boolean analyzeTerm(Object term)
    {
      Symbol symbol = Term.symbol(term);
      if (symbol instanceof VariableSymbol) return false;
      boolean isGround = true;
      int n = Term.argnumber(term);
      for (int i = 0; i < n; i++)
      {
        Object arg = Term.argument(term, i);
        boolean isGround0 = analyzeTerm(arg);
        isGround = isGround && isGround0;
      }
      if (!isGround) return false;
      add(term);
      return true;
    }

    /**************************************************************************
     * Add ground term to "groundTerms" if it does not yet appear there.
     * @param term the ground term to be added.
     *************************************************************************/
    private void add(Object term)
    {
      // basic operation
      String string = Term.toString(term);
      if (strings.contains(string)) return;
      groundTerms.add(term);
      strings.add(string);

      // for integer constant c, add some derivation (c+1, c-1)
      if (Term.argnumber(term) != 0) return;
      FunctionSymbol fun = (FunctionSymbol)Term.symbol(term);
      if (fun.tsymbol.root != problem.intSymbol) return;
      if (one == null || plus == null || minus == null) return;
      Object term1 = Term.term(plus, new Object[] { term, new Object[] { one } });
      Object term2 = Term.term(minus, new Object[] { term, new Object[] { one } });
      String name1 = Term.toString(term1);
      String name2 = Term.toString(term2);
      if (!strings.contains(name1)) { groundTerms.add(term1); strings.add(name1); }
      if (!strings.contains(name2)) { groundTerms.add(term2); strings.add(name2); }
    }
  }

  // --------------------------------------------------------------------------
  //
  // A classifier of terms according to their types.
  //
  // --------------------------------------------------------------------------
  private static class TermClassifier
  {
    // a map of types to the ground terms of that types
    private Map<TypeSymbol,List<Object>> typeMap;

    // get the classification result
    public Map<TypeSymbol,List<Object>> typeMap() { return typeMap; }

    /*************************************************************************
     * Construct a classifier.
     ************************************************************************/
    private TermClassifier()
    {
      typeMap = new HashMap<TypeSymbol,List<Object>>();
    }

    /**************************************************************************
     * Classify the terms according to their type by populating "termMap".
     * @param terms the terms to be classified.
     * @param subtyping true if subtyping is to be considered
     *************************************************************************/
    private void classify(List<Object> terms, boolean subtyping)
    {
      // the basic classification
      for (Object term : terms)
      {
        FunctionSymbol symbol = (FunctionSymbol)Term.symbol(term);
        TypeSymbol tsymbol = symbol.tsymbol;
        // without considering subtyping, we classify according to the root type
        if (!subtyping) tsymbol = tsymbol.root;
        List<Object> terms0 =
            typeMap.computeIfAbsent(tsymbol, (key)->new ArrayList<Object>());
        terms0.add(term);
      }

      // without considering subtyping, we are done
      if (!subtyping) return;

      // create a copy of the map where supertypes also have the terms
      // that the subtypes had in the original map and the subtypes
      // also have the terms that the supertypes had in the original map
      // (however, we keep the terms distinct that were originally associated
      // to different subtypes)
      Map<TypeSymbol,List<Object>> typeMap0 = new HashMap<TypeSymbol,List<Object>>();
      for (TypeSymbol tsymbol : typeMap.keySet())
      {
        List<Object> terms0 = new ArrayList<Object>(typeMap.get(tsymbol));
        typeMap0.put(tsymbol, terms0);
        while (tsymbol != tsymbol.base)
        {
          tsymbol = tsymbol.base;
          List<Object> terms1 = typeMap.get(tsymbol);
          if (terms1 == null) terms1 = new ArrayList<Object>();
          List<Object> terms2 = typeMap0.computeIfAbsent(tsymbol,
              (key)->new ArrayList<Object>());
          terms2.addAll(terms1);
          terms0.addAll(terms2);
          terms2.addAll(terms0);
        }
      }

      // replace the original map by the copy
      typeMap = typeMap0;
    }
  }

  // --------------------------------------------------------------------------
  //
  // An generator of rule instances.
  //
  // --------------------------------------------------------------------------
  private static class Generator
  {
    // the map of types to lists of ground terms
    private Map<TypeSymbol,List<Object>> typeMap;

    // the (partial) substitution
    private List<Object> terms;

    // the instances of the current rule
    private List<PureEquation> result0;

    // the instances of all rules
    private List<PureEquation> result;
    public List<PureEquation> rules() { return result; }

    /**************************************************************************
     * Create an instance generator.
     * @param typeMap a map of types to lists of ground terms.
     *************************************************************************/
    public Generator(Map<TypeSymbol,List<Object>> typeMap)
    {
      this.typeMap = typeMap;
      this.terms = new ArrayList<Object>();
      this.result0 = new ArrayList<PureEquation>();
      this.result = new ArrayList<PureEquation>();
    }

    /**************************************************************************
     * Generate ground instances of a list of rules.
     * @param rules the rules.
     * @return (some) instances of non-ground rules.
     *************************************************************************/
    public List<PureEquation> generate(List<PureEquation> rules)
    {
      // add ground rules
      // instantiate the other rules and add the instances to the result
      for (PureEquation rule : rules)
      {
        if (result.size() >= MAX_TOTAL) break;
        if (rule.vars.length == 0)
        {
          result.add(rule);
          continue;
        }
        generate(rule);
        result.addAll(result0);
        result0.clear();
      }
      return result;
    }

    /**************************************************************************
     * Generate instances of a rule and add them to "result0".
     * @param rule the rule to be instantiated.
     *************************************************************************/
    private void generate(PureEquation rule)
    {
      if (result0.size() >= MAX_RULE) return;
      int n = terms.size();
      if (n == rule.vars.length)
      {
        instantiate(rule);
        return;
      }
      VariableSymbol vsymbol = rule.vars[n];
      TypeSymbol tsymbol = vsymbol.tsymbol;
      List<Object> terms0 = typeMap.get(tsymbol);
      if (terms0 == null) return;
      for (Object term0 : terms0)
      {
        terms.add(term0);
        generate(rule);
        terms.remove(n);
      }
    }

    /**************************************************************************
     * Generate the instance of the rule described by the substitution "terms".
     * @param rule the rule to be instantiated.
     *************************************************************************/
    private void instantiate(PureEquation rule)
    {
      Subst subst = new Subst();
      int n = rule.vars.length;
      for (int i = 0; i < n; i++) subst.put(rule.vars[i], terms.get(i));
      int n0 = rule.lits.length;
      Object[] lits0 = new Object[n0];
      for (int i = 0; i < n0; i++) lits0[i] = subst.apply(rule.lits[i]);
      PureEquation rule0 = new PureEquation(rule.name + "." + result0.size(), rule.number,
          new VariableSymbol[0], lits0, rule.negs, rule.select, rule.score, rule.age,
          rule.generatedby, rule.parents, rule.marked);
      result0.add(rule0);
    }
  }
}


// ----------------------------------------------------------------------------
// end of file
// ----------------------------------------------------------------------------