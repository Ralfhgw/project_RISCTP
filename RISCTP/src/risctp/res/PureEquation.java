// ---------------------------------------------------------------------------
// PureEquation.java
// A clausal PureEquation.
// $Id: PureEquation.java,v 1.2 2026/04/14 10:46:24 schreine Exp $
//
// Author: Viktoria Langenreither <viktoria.langenreither@gmail.com>
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

import java.io.*;
import java.util.*;
import java.util.function.*;
import risctp.fol.*;
import risctp.problem.ProofProblem;
import risctp.syntax.AST.*;
import risctp.syntax.AST.Exp.*;
import risctp.types.Symbol;
import risctp.types.Symbol.*;

public final class PureEquation
{
  // its name
  public final String name;

  // its number
  public final int number;

  // the quantified variables
  public final VariableSymbol[] vars;

  // the literals
  public final Object[] lits;

  // their polarities
  // this array contains true, if a literals is negated (i. e. "yes this literal is negated")
  public final boolean[] negs;

  // the positions of the selected literals
  public List<Integer> select;

  // score of PureEquation - needed for organizing/sorting the unprocessed queue U
  public int score;
  public int age;

  // string of the method by which this clause was generated
  public final String generatedby;

  // list of the clauses representing the parents of this clause
  public final List<PureEquation> parents;

  // true clauses that are needed for the proof
  public List<Integer> marked;

  // a counter for enumerating clauses
  public static int clauseCounter = 1;

  // create a new PureEquation from its elements
  public PureEquation(String name, int number, VariableSymbol[] vars, Object[] lits, boolean[] negs,
    List<Integer> select, int score, int age, String generatedby, List<PureEquation> parents, List<Integer> marked)
  {
    this.name = name;
    this.number = number;
    this.vars = vars;
    this.lits = lits;
    this.negs = negs;
    this.select = select;
    this.score = score;
    this.age = age;
    this.generatedby = generatedby;
    this.parents = parents;
    this.marked = marked;
  }

  // transform a clause to a PureEquation
  public PureEquation(Clause clause, ClauseProblem clauseproblem)
  {
    // initialize the name
    name = clause.getName();

    number = clauseCounter;
    clauseCounter = clauseCounter + 1;

    // initialize the quantified variables
    List<TypedVar> tvars = clause.getTypedVars();
    int n = tvars.size();
    vars = new VariableSymbol[n];

    for (int i = 0; i < n; i++)
    {
      TypedVar tvar = tvars.get(i);
      vars[i] = (VariableSymbol)tvar.id.getSymbol();
    }

    // initialize the literals and their polarities
    List<Exp> literals = clause.getLiterals();
    List<Object> lits0 = new ArrayList<Object>();
    List<Boolean> negs0 = new ArrayList<Boolean>();
    for (Exp literal : literals)
    {
      if (literal instanceof Not)
      {
        Not literal0 = (Not)literal;
        Object lit = Term.term(literal0.exp);
        lits0.add(lit);
        negs0.add(true);
      }
      else
      {
        Object lit = Term.term(literal);
        lits0.add(lit);
        negs0.add(false);
      }
    }

    // every literal in a clause has to be an equality
    ProofProblem prob = clauseproblem.getProofProblem();

    // get the (in)equality symbol
    FunctionSymbol eq = prob.equalities.get(prob.boolSymbol);
    FunctionSymbol neq = prob.inequalities.get(prob.boolSymbol);

    Collection<FunctionSymbol> eqs = prob.equalities.values();
    Collection<FunctionSymbol> neqs = prob.inequalities.values();

    // make every literal to an equlity 
    for (int i = 0; i < lits0.size(); i++)
    {
      FunctionSymbol symb = (FunctionSymbol) (Term.symbol(lits0.get(i)));

      // nothing to do if the literal is already an (in)equality
      if (eqs.contains(symb) || neqs.contains(symb)) {}
      // transforming every term to an equality by adding equals true or not equals true
      else
      {
        Object[] args = new Object[2];
        args[0] = lits0.get(i);
        if (negs0.get(i) == false)
        {
          args[1] = Term.term(prob.trueValueSymbol, new Object[0]);
          lits0.set(i, Term.term(eq, args));  
        }
        else
        {
          args[1] = Term.term(prob.trueValueSymbol, new Object[0]); 
          lits0.set(i, Term.term(neq, args)); 
        }  	
      }
    }

    int litn = lits0.size();
    Object[] new_lits = new Object[litn];
    for (int i = 0; i < litn; i++) new_lits[i] = lits0.get(i);
    lits = new_lits;
    negs = new boolean[litn];
    for (int i = 0; i < litn; i++) negs[i] = negs0.get(i);

    select = selectionFunction(lits, negs);

    score = -1;
    age = -1;

    generatedby = "initial";
    parents = new ArrayList<>();
    marked = new ArrayList<>();
  }

  //true if rule need not be considered as a starting point of proof search
  private boolean implied = false;
  public void setImplied(boolean value) { implied = value; }
  public boolean isImplied() { return implied; }

  // true if rule need not be emitted to SMT solver
  private boolean theory = false;
  public void setTheory(boolean value) { theory = value; }
  public boolean isTheory() { return theory; }

  /**************************************************************************
   * Print core content of PureEquation.
   * @param out the output medium.
   * @param negate true if the negation of the PureEquation is to be printed.
   * @param quant true if the quantifier prefix is to be printed.
   * @param pred a predicate that states whether a literal is to be printed.
   * @param consumer a consumer that accepts the literal and prints it.
   *************************************************************************/
  public void print(PrintWriter out, boolean negate, boolean quant,
    BiPredicate<Object,Boolean> predicate, BiConsumer<Object,Boolean> consumer)
  {
    int varn = vars.length;
    if (quant && varn > 0)
    {
      if (negate) out.print("∃"); else out.print("∀");
      for (int i = 0; i < varn; i++)
      {
        VariableSymbol var = vars[i];
        out.print(var.id.toString());
        out.print(":");
        out.print(var.tsymbol.type.toString());
        if (i+1 < varn) out.print(",");
      }
      out.print(". ");
    }
    List<Object> plits = new ArrayList<Object>();
    List<Object> nlits = new ArrayList<Object>();
    int n = lits.length;
    for (int i = 0; i < n; i++)
    {
      Object lit = lits[i];
      boolean neg = negs[i];
      if (!predicate.test(lit, neg)) continue;
      if (neg) nlits.add(lit); else plits.add(lit);
    }
    int nlitsn = nlits.size();
    if (nlitsn == 0)
      out.print("⊤");
    else
    {
      for (int i = 0; i < nlitsn; i++)
      {
        consumer.accept(nlits.get(i), false);
        if (i+1 < nlitsn) out.print(" ∧ ");
      }
    }
    int plitsn = plits.size();
    if (negate) 
    { 
      if (plitsn > 0) out.print(" ∧ "); 
    }
    else out.print(" ⇒ ");
    if (plitsn == 0)
    {
      if (!negate) out.print("⊥");
    }
    else
    {
      for (int i = 0; i < plitsn; i++)
      {
        if (negate) out.print("¬");
        consumer.accept(plits.get(i), true);
        if (i+1 < plitsn) 
        {
          if (negate) out.print(" ∧ "); else out.print(" ∨ ");
        }
      }
    }
  }

  /**************************************************************************
   * Print string representation of PureEquation.
   * @param clause the pureEquation to be printed
   * @return string representation of the PureEquation
   *************************************************************************/
  public String printPureEquation(PureEquation clause)
  { 
    int i = 0;
    String out = "";
    for (Object lit : clause.lits)
    {
      if (clause.negs[i] == false) out = out + "=(";
      else out = out + "≠(";
      out = out + Term.toString(Term.argument(lit, 0)) + ", ";
      out = out + Term.toString(Term.argument(lit, 1)) + ")";
      i++;
      if (i < clause.lits.length) out = out + " ∨ ";
    }
    out = out.replace("§", "");
    return out;
  }

  /**************************************************************************
   * Selects literals in a clause. If there are literals selected, the prover
   * will only use the selected literals to generate new clauses with. This is 
   * a heuristic to fasten the proof.
   * @param lits - array of the literals of the PureEquation to select literals from
   * @param negs - the corresponding array of negs
   * @return list of integer positions of the selected literals
   *************************************************************************/
  public List<Integer> selectionFunction(Object[] lits, boolean[] negs)
  {
    return selectionFunctionEmpty();
  }

  // no literals are selected. The superposition calculus runs "normally" (on maximal literals).
  // Empty list is returned.
  public List<Integer> selectionFunctionEmpty()
  {
    return new ArrayList<>();
  }

  /**************************************************************************
   * Counts the symbols of a PureEquation possibly with a (distinct) weight
   * for function symbols (and constants) and for variable symbols
   * @param clause - the PureEquation
   * @param weightfuncsymb - the weight for the function symbols (and constants)
   * @param weightvarsymb - the weight for the variable symbols
   * @return number of symbols
   *************************************************************************/
  public static int symbolCounting(PureEquation clause, int weightfuncsymb, int weightvarsymb)
  {
    int symbolcount = 0;
    Object[] lits = clause.lits;
    for(int i = 0; i < lits.length; i++) symbolcount += symbolCountingTerm(lits[i], weightfuncsymb, weightvarsymb);
    return symbolcount;
  }

  // counts the symbols in one term (literal)
  public static int symbolCountingTerm(Object lit, int weightfuncsymb, int weightvarsymb)
  {
    int symbolcount = 0;

    if (lit instanceof VariableSymbol) return (1 * weightvarsymb);
    else
    {
      symbolcount += (1 * weightfuncsymb);
      Object[] term0 = (Object[])lit;
      for(int j = 1; j < term0.length; j++)
      {
        if(term0[j] instanceof VariableSymbol) symbolcount += (1 * weightvarsymb);
        else symbolcount += symbolCountingTerm(term0[j], weightfuncsymb, weightvarsymb);
      }  
    }
    return symbolcount;
  }                                 

  /**************************************************************************
   * Computes subsets of a specified length i. e. finds subterms of a given 
   * PureEquation 
   * @param givenclause - the PureEquation
   * @param length - the desired length of the subsets
   * @return set of subsets (terms/literals) of the specified length
   *************************************************************************/
  //https://www.geeksforgeeks.org/dsa/finding-all-subsets-of-a-given-set-in-java/
  public List<List<Object>> subsets(PureEquation givenclause, int length)
  {
    List<List<Object>> allsubsets = new ArrayList<>();

    List<Object> lits = new ArrayList<>();
    for (Object l : givenclause.lits) lits.add(l);

    int n = lits.size();

    for (int i = 0; i < (1<<n); i++)
    {
      List<Object> subset = new ArrayList<>(); 

      for (int j = 0; j < n; j++)
      {
        if ((i & (1 << j)) > 0) subset.add(lits.get(j));
      }
      if (subset.size() == length) allsubsets.add(subset);
    }  
    return allsubsets;
  }

  /**************************************************************************
   * To check if a term/literals is greater than another according to the 
   * lexicographic path ordering (LPO). Needed to check, if a literal is the
   * greatest literal in a clause (according to lpo). Checks if lit1 > lit2
   * @param lit1 - literal1 to be checked
   * @param lit2 - literal2 to be checked
   * @param ordering - list of function symbols in a specified order
   * @return true if lit1 > lit2
   *************************************************************************/
  public static boolean lpo(Object lit1, Object lit2, List<FunctionSymbol> ordering)
  {
    // number of arguments the literals have
    int len1;
    int len2;
    if (lit1 instanceof VariableSymbol) len1 = 0;
    else len1 = Term.argnumber(lit1);
    if (lit2 instanceof VariableSymbol) len2 = 0;
    else len2 = Term.argnumber(lit2);

    if (properSubterm(lit1, lit2) == true) return true;

    // Assume lit1 is represented by f(t1, ...tn). Than 
    // f(t1, ...tn) > lit2 i tn) if ti > lit2 for some i
    for (int i = 0; i < len1; i++)
    {
      if (lpo(Term.argument(lit1, i), lit2, ordering) == true) return true;
    }

    // Assume lit1 is represented by f(t1, ...tn) ad lit2 by f(u1, ..., um). Than 
    // f(t1, ... tn) > f(u1, ..., um) if ti > ui for some i and tj = uj for all j < i
    Symbol f = Term.symbol(lit1);
    Symbol g = Term.symbol(lit2);
    int len0;
    if (f == g)
    {
      if (len1 > len2) len0 = len2;
      else len0 = len1;
      for (int i = 0; i < len0; i++)
      {
        // additionally require f(t1, ..., tn) > ui for every i
        if (lpo(lit1, Term.argument(lit2, i), ordering) == true)
        {
          if ((Term.argument(lit1, i) == Term.argument(lit2, i)) == false)
          {
            if (lpo(Term.argument(lit1, i), Term.argument(lit2, i), ordering) == true) return true;
            else break;
          } 
        } else break;  
      } 
    }

    // Assume lit1 is represented by f(t1, ...tn) ad lit2 by g(u1, ..., um). Than 
    // f(t1, ... tn) > g(u1, ..., um) if f > g for some odering of function/constant symbols
    boolean test = true;
    if(len1 > len2) // arity of lit1 is greater than of lit2 if len1 > len2. The ordering of function symbols says,
      //that functionsymbols with greater arity are greater
    {
      for (int i = 0; i < len2; i++)
      {
        // additionally require f(t1, ..., tn) > ui for every i
        if (lpo(lit1, Term.argument(lit2, i), ordering) == false) test = false;
      }
      if (test == true) return true;
    } else
    {
      if (len1 == len2) //if the arity of the function symbol choose randomly which is bigger
      {
        int posf = ordering.indexOf(f);
        int posg = ordering.indexOf(g);
        if (posf < posg)
        {
          for (int i = 0; i < len2; i++)
          {
            // additionally require f(t1, ..., tn) > ui for every i
            if (lpo(lit1, Term.argument(lit2, i), ordering) == false) test = false;
          }
          if (test == true) return true;
        }

      }
    }
    return false;
  }

  // checks recursively if lit2 is a proper subterm of lit1
  public static boolean properSubterm(Object lit1, Object lit2)
  {
    if (lit1 instanceof VariableSymbol)
    {
      if (Term.equal(lit1, lit2) == true) return true;
    } else
    {
      for (int i = 0; i < Term.argnumber(lit1); i++)
      {
        Object subterm = Term.argument(lit1, i);
        if (Term.equal(subterm, lit2) == true) return true;
        else return properSubterm(subterm, lit2);
      }
    }

    return false;
  }

} // ends class

// ----------------------------------------------------------------------------
// end of file
// ----------------------------------------------------------------------------

