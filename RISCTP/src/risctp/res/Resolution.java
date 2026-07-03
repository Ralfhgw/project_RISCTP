// ---------------------------------------------------------------------------
// Resolution.java
// A resolution prover.
// $Id: Resolution.java,v 1.4 2026/04/14 12:32:59 schreine Exp $
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;

import risctp.fol.*;
import risctp.me.ConsList;
import risctp.me.ModelElimination.AbortedException;
import risctp.me.ModelElimination.Equality;
import risctp.problem.ProofProblem;
import risctp.smt.Solver;
import risctp.syntax.AST.Decl;
import risctp.syntax.AST.Id;
import risctp.types.Symbol.FunctionSymbol;
import risctp.types.Symbol.VariableSymbol;

public class Resolution
{
  // the output medium
  private PrintWriter out;

  // a counter for creating fresh variables
  private int varCounter;

  // turn on/off intra-clause simplification
  private boolean intraClauseSimpl = true;

  // turn on/off clause-clause simplification
  private boolean clauseClauseSimpl = true;

  // use givenclause to simplify the set processed (before generating new clauses)
  private boolean simplifyProcessed = true;

  // maximal iteration depth
  private int infdepth = 0;

  //variable only used for printing, tracks the inference depth
  private int it = 1;

  // choose type of selection ("fifo" or "clauseweight")
  //private static String selection = "clauseweight";
  //private static String selection = "fifo";
  private String selection = "combined";

  // every third clause to be selected as the given clause is the oldest clause
  private int freqUseOldClause = 3;

  // tracking the oldest age of a clause
  private int oldestage = 0;

  // weight for variable- and functionsymbols - for the clause selection by clauseweight
  private int weightfuncsymb = 1;
  private int weightvarsymb = 1;

  // ordering of the function/constant symbols used for the lexicographic path ordering
  private List<FunctionSymbol> ordering = new ArrayList<>();

  // use (additionally) smt to solve the problem
  private boolean smt = false;

  // how often the smt-solver should be applied; use 1 for every iteration, 2 for every second iteration, ...
  private int freqUseSmt = 1;

  /****************************************************************************
   * Create an instance of a resolution prover.
   * @param out its output medium.
   * @param smt true if SMT solving is to be applied
   * @param isAborted if not null, method to tell whether computation is aborted.
   ***************************************************************************/
  public Resolution(PrintWriter out, boolean smt, BooleanSupplier isAborted)
  {
    this.out = out;
    this.smt = smt;
    this.isAborted = isAborted;
  }

  /****************************************************************************
   * Solve a problem in clausal form.
   * @param problem the problem.
   * @return true if the solution succeeded.
   **************************************************************************/
  public boolean solve(ClauseProblem problem)
  {
    try
    {
      return solveCore(problem);
    }
    catch (AbortedException e)
    {
      out.print("The proof of the goal is aborted because ");
      out.println(e.getMessage() + ".");
      return false;
    }
    catch (Throwable e)
    {
      out.println("The proof of the goal is aborted because of an internal error: ");
      e.printStackTrace(out);
      return false;
    }
  }
  
  // variable for measuring the execution time of the prover (time needed to find a proof)
  private long starttime;
  
  /****************************************************************************
   * Solve a problem in clausal form.
   * @param problem the problem.
   * @return true if the solution succeeded.
   **************************************************************************/
  public boolean solveCore(ClauseProblem problem)
  {
    // variable for measuring the execution time of the prover (time needed to find a proof)
    starttime = System.nanoTime();

    // variable only used for printing, tracks the inference depth
    it = 1;

    // counter - steadily increased with every new variable symbol found (to ensure distinct variables)
    varCounter = 0;

    // the proof problem
    ProofProblem prob = problem.getProofProblem();

    // (in)equality symbol
    FunctionSymbol eq = prob.equalities.get(prob.boolSymbol);
    FunctionSymbol neq = prob.inequalities.get(prob.boolSymbol);

    ordering.add(neq); //ordering according to arity of function symbol
    ordering.add(eq);

    // the clauses of the problem
    List<Clause> unprocessed = problem.getClauses();

    // processed and unprocessed clauses as lists
    List<PureEquation> u = new ArrayList<>();
    List<PureEquation> processed = new ArrayList<>();

    // transform all clauses to pure equations and rename all variables so all clauses have distinct ones
    // add them to the unprocessed set
    for(Clause c : unprocessed)
    {
      PureEquation transformed = new PureEquation(c, problem);
      PureEquation distinctvar = newInstance(transformed);
      u.add(distinctvar);
      ordering.addAll(findFunctionSymbols(distinctvar.lits, ordering));
    }

    // for every clause an evaluation gets calculated and stored with the clause (form list of unprocessed clauses)
    // unproc gets sorted accordingly to the result of the evaluation function
    List<PureEquation> unproc = evaluationFunction(u);
    sort(unproc, 0, unproc.size()-1);

    // setting up the problem for an external SMT solver
    if (smt) startSMT(problem);

    out.println("The inital set of clauses at the start of the prover:");
    for (PureEquation clause : unproc)
    {
      out.println(clause.number + ": " + clause.printPureEquation(clause));
    }

    // starting the main algorithm (discount loop)
    while (unproc.size() > 0)
    {
      checkAbort(true);
      checkTimeout(true);
      
      // calling the external SMT solver
      if (smt)
      {
        if ((it % freqUseSmt)== 0)
        {
          boolean smtprove = smtTheorem(problem, prob);
          out.println(smtprove);
          if (smtprove == true)
          {
            stopSMT();
            out.println("The SMT solver was able to show unsatisfiability.");
            return true; 
          }
        }
        checkAbort(true);
        checkTimeout(true);
      }

      out.println("Iteration: " + it);

      // the givenclause c gets selected, first element is selected since unproc is already sorted accordingly
      PureEquation c = selectbest(unproc);

      // givenclause gets removed from unproc
      if (selection == "combined")
      {
        for (int j = 0; j < unproc.size(); j++)
        {
          if (unproc.get(j).age == c.age) unproc.remove(j);
        }
      } else
      {
        unproc.remove(0);  
      }

      // simplify the givenclause using the already processed clauses
      c = simplify(c, processed, eq, neq);

      // check if the givenclause is redundant, only not redundant clauses get processed further
      // redundant clauses are dropped completely
      if (redundant(c, processed, eq, neq) == false)
      {		
        // if c is the empty clause ... success - the input set is unsatisfiable
        if (c.lits.length == 0)
        {
          // evaluate the time, how long the algorithm needed to find a proof
          long endtime = System.nanoTime();
          long duration = (endtime - starttime)/1000000;
          out.println("Found the empty clause in " + duration + " ms");
          out.println("Number of clauses processed until a proof for unsatisfiability was found: " + processed.size()+1);
          out.println("Inference depth: " + it);
          return true;
        } else
        {
          // list for all newly generated clauses
          List<PureEquation> t = new ArrayList<>();

          // use the givenclause to simplify the clauses in processed
          if (simplifyProcessed == true)
          {
            int i = 0;  
            for (PureEquation p : processed)
            {
              // generate a set with the givenclause but without the currently watched clause from processed
              List<PureEquation> help = new ArrayList<PureEquation>();
              help.add(c);
              for (PureEquation pe : processed)
              {
                if (pe != p) help.add(pe);
              }
              // apply the simplify method to a clause in processed and the set help
              PureEquation simpl = simplify(p, help, eq, neq);

              // if c is the empty clause ... success - the input set is unsatisfiable
              if (simpl.lits.length == 0)
              {
                // evaluate the time, how long the algorithm needed to find a proof
                long endtime = System.nanoTime();
                long duration = (endtime - starttime)/1000000;
                out.println("Found the empty clause in " + duration + " ms");
                out.println("Number of clauses processed until a proof for unsatisfiability was found: " + processed.size()+1);
                out.println("Inference depth: " + it);
                return true;
              }
              // overwrite the old clause in processed with the simplified one
              processed.set(i, simpl);
              i++;
            } // end for-loop over set processed
          } // end if simplifiedProcessed

          // add the givenclause to the set processed
          processed.add(c); 

          // generate new clauses with the givenclause and the set processed
          t.addAll(generate(c, processed, eq, neq)); 		

          // list for all newly generated clauses that are not trivial (add to smt)
          List<PureEquation> addsmt = new ArrayList<>();

          // for loop: over all newly generated clauses
          for (PureEquation p : t)
          {
            // if c is the empty clause ... success - the input set is unsatisfiable
            if (p.lits.length == 0)
            {
              // evaluate the time, how long the algorithm needed to find a proof
              long endtime = System.nanoTime();
              long duration = (endtime - starttime)/1000000;
              out.println("Found the empty clause in " + duration + " ms");
              out.println("Number of clauses processed until a proof for unsatisfiability was found: " + processed.size()+1);
              out.println("Inference depth: " + it);
              return true;
            } else
            {			
              // simplify the newly generated clause with the set processed only using efficient rules
              p = cheapSimplify(p, processed, eq, neq);

              // if c is the empty clause ... success - the input set is unsatisfiable
              if (p.lits.length == 0)
              {
                // evaluate the time, how long the algorithm needed to find a proof
                long endtime = System.nanoTime();
                long duration = (endtime - starttime)/1000000;
                out.println("Found the empty clause in " + duration + " ms");
                out.println("Number of clauses processed until a proof for unsatisfiability was found: " + processed.size()+1);
                out.println("Inference depth: " + it);
                return true;
              }

              // check if the newly generated clause is trivial, only not trivial clauses get add to the set unprocessed
              if (trivial(p,processed, eq, neq) == false)
              {
                // according to the evaluation function the new clause gets added at the correct position in the set unprocessed
                unproc = insertAtRightPos(p, unproc);

                // add the new clause to the state that is given to the external SMT sovler
                addsmt.add(p);
              }	
            } // end if else			
          } // end for	

          if (smt == true)
          {
            int len = rules.length;
            PureEquation[] newrules = new PureEquation[len + addsmt.size()];
            for (int i = 0; i < len; i++) {newrules[i] = rules[i];}
            for (PureEquation clause : addsmt)
            {
              newrules[len] = clause;
              len++;
            }
            rules = newrules;  
          }

        } // end else		
      } // end if not redundant
      it++;	
    } // end while  
    out.println("Input set is satisfiable");
    return false;
  }

  /****************************************************************************
   * Register a new instance of a rule with fresh names for the
   * quantified variables that do not appear in the proof yet.
   * @param rule the rule for which the instance is to be created.
   ***************************************************************************/
  private PureEquation newInstance(PureEquation clause)
  {
    VariableSymbol[] vars = clause.vars;
    int n = vars.length;
    VariableSymbol[] vars0 = new VariableSymbol[n];
    Subst subst = new Subst();
    for (int i = 0; i < n; i++)
    {
      VariableSymbol var = vars[i];
      Id id0 = Id.create(newVarName(var.id.toString()));
      VariableSymbol var0 = new VariableSymbol(id0, var.tsymbol);
      id0.setSymbol(var0);
      vars0[i] = var0;
      subst.put(var, var0);
    }
    Object[] lits = clause.lits;
    int litn = lits.length;
    Object[] lits0 = new Object[litn];
    for (int i = 0; i < litn; i++) lits0[i] = subst.apply(lits[i]);
    String name0 = clause.name;
    int number0 = clause.number;
    boolean[] negs0 = clause.negs;
    List<Integer> select0 = clause.select;
    int score0 = clause.score;
    int age0 = clause.age;
    String generatedby0 = clause.generatedby;
    List<PureEquation> parents0 = clause.parents;
    List<Integer> marked0 = clause.marked;
    PureEquation clause0 = new PureEquation(name0, number0, vars0, lits0, negs0, 
        select0, score0, age0, generatedby0, parents0, marked0); 
    return clause0;
  }

  /****************************************************************************
   * Create a new variable name from a given name.
   * @param name the name of a variable.
   * @return a fresh variable name that does not appear in the proof yet.
   ***************************************************************************/
  private String newVarName(String name)
  {
    String name0;
    int i = name.indexOf('@');
    if (i == -1)
    {
      name0 = name + "@" + varCounter;
    } else
    {
      String name1 = name.substring(0, name.indexOf('@'));
      name0 = name1 + "@" + varCounter;
    }
    varCounter++;
    return name0;
  }

  /****************************************************************************
   * Find all function symbols in a PureEquation
   * @param clause - the PureEquation
   * @return list of all function symbols in the PureEquation
   ***************************************************************************/
  private List<FunctionSymbol> findFunctionSymbols(Object[] clause, List<FunctionSymbol> ordering)
  {
    List<FunctionSymbol> ord = new ArrayList<>();

    for (Object lit : clause)
    {
      if (lit instanceof VariableSymbol) continue;
      else 
      {
        if (Term.symbol(lit) instanceof FunctionSymbol)
        {
          if(ord.contains(Term.symbol(lit)) == false) ord.add((FunctionSymbol) Term.symbol(lit));
        }

        if (Term.argnumber(lit) > 0)
        {
          for (int i = 0; i < Term.argnumber(lit); i++)
          {
            Object[] subterm = new Object[1];
            subterm[0] = Term.argument(lit, i);
            ord.addAll(findFunctionSymbols(subterm, ord));
          }
        }
      }
    }
    return ord;
  }

  /****************************************************************************
   * Selects the best clause (according to the evaluation function - "fifo",
   * "clauseweight" with and without weights for the variable and function symbols)
   * from the set of unprocessed clauses -> givenclause.
   * @param unprocessed - the list of unprocessed clauses
   * @return givenclause (pure equation)
   **************************************************************************/
  public PureEquation selectbest(List<PureEquation> unprocessed)
  {
    if (selection == "combined")
    {
      if ((it % freqUseOldClause)== 0)
      {
        int oldest_age = unprocessed.get(0).age;
        PureEquation oldestclause = unprocessed.get(0);
        for (PureEquation clause : unprocessed)
        {
          if (clause.age < oldest_age)
          {
            oldest_age = clause.age;
            oldestclause = clause;
          }
        }
        return oldestclause;
      } else
      {
        return unprocessed.get(0);
      }
    } else
    {
      // returns the first element of the list unprocessed - clauses are already sorted in there according to evaluation
      return unprocessed.get(0);
    }
  }

  // This function takes a list of PureEquations and returns a list of PureEquations
  // for every clause/PureEquation the score gets set according evaluation
  // (calculated by the specific evaluation function - fifo or clauseweight)
  public List<PureEquation> evaluationFunction(List<PureEquation> unprocessed)
  {
    List<PureEquation> evaluation = new ArrayList<>();
    if (selection == "fifo") evaluation = fifoClauseSelection(unprocessed);
    if (selection == "clauseweight") evaluation = clauseWeightClauseSelection(unprocessed, weightfuncsymb, weightvarsymb);
    if (selection == "combined") evaluation = combinedClauseSelection(unprocessed, weightfuncsymb, weightvarsymb);
    return evaluation;
  }

  // Returns a list of PureEquation; for every PureEquation the "score" gets set
  // This evaluation function represents the "first-in-first-out" method; Clauses get an increasing integer score
  // according to the time when they were generated; "younger" clauses get a low score
  // The score start with n, unprocessed is the set of newly generated clauses needing a score assigned
  public List<PureEquation> fifoClauseSelection(List<PureEquation> unprocessed)
  {
    List<PureEquation> fifo = new ArrayList<>();
    for (PureEquation clause : unprocessed)
    {
      fifo.add(new PureEquation(clause.name, clause.number, clause.vars, clause.lits, clause.negs, clause.select, oldestage, oldestage, 
          clause.generatedby, clause.parents, clause.marked));
      oldestage++;
    }
    return fifo;
  }

  // Returns a list of PureEquation; for every PureEquation the "score" gets set
  // This evaluation function represents the "clause weight" method
  // Clauses get an integer number as score (number of symbols - maybe weighted), smaller clauses get preferred
  public List<PureEquation> clauseWeightClauseSelection(List<PureEquation> unprocessed, int weightfuncsymb,
    int weightvarsymb)
  {
    List<PureEquation> clauseweight = new ArrayList<>();
    for (PureEquation clause : unprocessed)
    {
      clauseweight.add(new PureEquation(clause.name, clause.number, clause.vars, clause.lits, clause.negs, clause.select,
          PureEquation.symbolCounting(clause, weightfuncsymb, weightvarsymb), oldestage, 
          clause.generatedby, clause.parents, clause.marked));
      oldestage++;
    }
    return clauseweight;
  }

  // Returns a list of PureEquation;
  // for every PureEquation the "score" and the "age" gets set
  // This evaluation function represents a combination of the "clause weight" method
  // and the fifo strategy. Clauses get an integer number as score (number of symbols
  // - maybe weighted), smaller clauses get preferred additionally the age of the
  // clause gets tracked by a continuous global counter, smaller age means older clause
  public List<PureEquation> combinedClauseSelection(List<PureEquation> unprocessed,
    int weightfuncsymb, int weightvarsymb)
  {
    List<PureEquation> combined = new ArrayList<>();
    for (PureEquation clause : unprocessed)
    {
      combined.add(new PureEquation(clause.name, clause.number, clause.vars, clause.lits,
          clause.negs, clause.select,
          PureEquation.symbolCounting(clause, weightfuncsymb, weightvarsymb), oldestage, 
          clause.generatedby, clause.parents, clause.marked));
      oldestage++;
    }
    return combined;  
  }

  // using the evaluation in the Pair to insert a new clause at the correct position in the set unprocessed
  // checks the evaluation of the element in the middle of unprocessed - if it is smaller, the search for the correct
  // position starts at the beginning, if the evaluation is bigger, the search starts in the middle
  // The input list of unprocessed clauses is already sorted - the returned list is also sorted
  public List<PureEquation> insertAtRightPos(PureEquation newclause, List<PureEquation> unprocessed)
  {

    List<PureEquation> helplist = new ArrayList<>();
    if (unprocessed.isEmpty() == true)
    {
      helplist.add(newclause);
      return helplist;
    }
    helplist.add(newclause);
    List<PureEquation> newly = evaluationFunction(helplist);

    PureEquation p = newly.get(0);

    if (unprocessed.isEmpty() == true) unprocessed.add(p);
    else if (unprocessed.size() == 1)
    {
      int score = p.score;
      if (score < (unprocessed.get(0)).score) unprocessed.add(0, p);
      else unprocessed.add(p);
    } else
    {
      double n = unprocessed.size();
      double test = n / 2.0;
      int half = (int) Math.round(test);
      int score = p.score;
      if (score < (unprocessed.get(half-1)).score)
      {
        int i = 0;
        while (i < half)
        {
          if (score < (unprocessed.get(i)).score)
          {
            unprocessed.add(i, p);
            half = half +1;
            break;
          }
          i++;
        }
      } else
      {
        int i = half;
        while (i < n)
        {
          if (score < (unprocessed.get(i)).score)
          {
            unprocessed.add(i, p);
            break;
          }
          i++;
        }
        if (unprocessed.size() <= n) unprocessed.add(p);
      }
    }
    return unprocessed;
  }

  // https://www.geeksforgeeks.org/merge-sort/
  // implements the merge-sort algorithm
  // It merges two subarrays of arr
  // first subarray is from position l to m
  // second subarray is from position m+1 to r
  static void merge(List<PureEquation> arr, int l, int m, int r)
  {
    // sizes of the subarrays
    int n1 = m - l + 1;
    int n2 = r - m;

    // create temp arrays to represents the left and right subarray
    List<PureEquation> L = new ArrayList<>(); // length n1
    List<PureEquation> R = new ArrayList<>(); // length n2
    for (int i = 0; i < n1; ++i) L.add(arr.get(l + i));
    for (int j = 0; j < n2; ++j) R.add(arr.get(m + 1 + j));

    // Merge the temp arrays
    int i = 0, j = 0;
    int k = l;
    while (i < n1 && j < n2)
    {
      if ((L.get(i)).score <= (R.get(j)).score)
      {
        arr.set(k, L.get(i));
        i++;
      }
      else
      {
        arr.set(k, R.get(j));
        j++;
      }
      k++;
    }

    // Copy remaining elements of L[] if any
    while (i < n1)
    {
      arr.set(k, L.get(i));
      i++;
      k++;
    }

    // Copy remaining elements of R[] if any
    while (j < n2)
    {
      arr.set(k, R.get(j));
      j++;
      k++;
    }
  }

  // Main function that sorts arr using merge()
  static void sort(List<PureEquation> arr, int l, int r)
  {
    if (l < r)
    {
      // Find the middle point
      int m = l + (r - l) / 2;
      // Sort first and second halves
      sort(arr, l, m);
      sort(arr, m + 1, r);
      // Merge the sorted halves
      merge(arr, l, m, r);
    }
  }

  /****************************************************************************
   * Simplifies a clause (pure equation) with respect to a set of clauses.
   * Applies the rules rewriting negative literals, delete duplicated literals,
   * delete resolved literals and destructive equality resolution
   * @param c is the pure equation to be simplified
   * @param processed set of pure equations used to simplify c
   * @param eq represents the equality function symbol
   * @param neq represents the inequality function symbol
   * @return simplified pure equation 
   **************************************************************************/
  public PureEquation simplify(PureEquation c, List<PureEquation> processed, FunctionSymbol eq, FunctionSymbol neq)
  {  
    SimplifyingInferences simpl = new SimplifyingInferences(c, eq, neq, new Subst(), ordering);
    PureEquation c_old = c;
    // apply rewriting positive and negative literals
    // and negative simplify-reflect
    if (clauseClauseSimpl == true)
    {
      for (PureEquation clause : processed)
      {
        c_old = c;
        c = simpl.rewritingAllLiterals(clause, c);
        c = simpl.negativeSimplifyReflect(clause, c);

        if (c.lits.length == 0)
        {
          out.println("Clause-Clause: " + clause.number + " simplified " + c.number + " to {}");
        } else
        {
          int i = 0;
          for (Object lit : c.lits)
          {
            if (Term.equal(lit, c_old.lits[i]) == false)
            {
              out.println("Clause-Clause: " + clause.number + " simplified " + c.number + " to " + c.printPureEquation(c));
              break;
            }
            i++;
          }
        }
      }
    }
    // apply delete duplicated literals and delete resolved literals
    // test the applicability for destructive equality resolution and respectively apply this rule
    if (intraClauseSimpl == true)
    {
      c_old = c;
      c = simpl.deleteDuplicatedLiterals(c);
      c = simpl.deleteResolvedLiterals(c);

      int pos = simpl.applicabilityDestrEqRes(c);
      if(pos != -1) c = simpl.destructiveEqualityResolution(c, pos);

      if (c.lits.length == 0)
      {
        out.println("Intra-Clause: " + c.number + " simplified itself to {}");
      } else
      {
        int j = 0;
        for (Object lit : c.lits)
        {
          if (Term.equal(lit, c_old.lits[j]) == false)
          {
            out.println("Intra-Clause: " + c.number + " simplified itself to " + c.printPureEquation(c));
            break;
          }
          j++;
        }  
      }
    }
    return c;
  }

  /****************************************************************************
   * CheapSimplifies simplifies a clause (pure equation) only using efficiently
   * implemented rules. It applies the rules delete duplicated literals and
   * delete resolved literals.
   * @param c is the pure equation to be simplified
   * @param processed set of pure equations used to simplify c
   * @param eq represents the equality function symbol
   * @param neq represents the inequality function symbol
   * @return simplified pure equation  
   **************************************************************************/
  public PureEquation cheapSimplify(PureEquation c, List<PureEquation> processed, FunctionSymbol eq, FunctionSymbol neq)
  {
    if (intraClauseSimpl == true)
    {
      SimplifyingInferences simpl = new SimplifyingInferences(c, eq, neq, new Subst(), ordering);
      PureEquation c_old = c;
      c = simpl.deleteDuplicatedLiterals(c);
      c = simpl.deleteResolvedLiterals(c);

      if (c.lits.length == 0)
      {
        out.println("Intra-Clause (cheap): " + c.number + " simplified itself to {}");
      } else
      {
        int i = 0;
        for (Object lit : c.lits)
        {
          if (Term.equal(lit, c_old.lits[i]) == false)
          {
            out.println("Intra-Clause (cheap): " + c.number + " simplified itself to " + c.printPureEquation(c));
            break;
          }
          i++;
        }
      }
    }
    return c;
  }

  /****************************************************************************
   * Redundant checks, if a clause (pure equation) is redundant with respect to
   * a given set. This is done by applying clause subsumption (if possible).
   * @param c is the pure equation to be checked for redundancy
   * @param processed set of pure equations used (trivial with respect to this clauses)
   * @param eq represents the equality function symbol
   * @param neq represents the inequality function symbol
   * @return true if the clause is redundant with respect to the given set
   **************************************************************************/
  public boolean redundant(PureEquation c, List<PureEquation> processed, FunctionSymbol eq, FunctionSymbol neq)
  {
    if (clauseClauseSimpl == true)
    {
      SimplifyingInferences simpl = new SimplifyingInferences(c, eq, neq, new Subst(), ordering);
      // run over all clauses in processed and check, if they can subsume the clause c
      int size = processed.size();
      for(int i = 0; i < size; i++)
      {
        // apply the inference rule clause subsumption
        if (simpl.clauseSubsumption(processed.get(i), c))
        {
          out.println(c.number + " got subsumed");
          return true;
        }
      }
    }
    return false;
  }

  /****************************************************************************
   * Trivial checks if a clause is trivial. This methods uses the rules for
   * syntactic tautology deletion to test for triviality.
   * @param c is the pure equation to be checked
   * @param processed set of pure equations (trivial with respect to this clauses)
   * @param eq represents the equality function symbol
   * @param neq represents the inequality function symbol
   * @return true if the clause is trivial
   **************************************************************************/
  public boolean trivial(PureEquation c, List<PureEquation> processed, FunctionSymbol eq, FunctionSymbol neq)
  { 
    if (intraClauseSimpl == true)
    {
      SimplifyingInferences simpl = new SimplifyingInferences(c, eq, neq, new Subst(), ordering);
      if (simpl.syntacticTautologyDeletion1(c) == true || simpl.syntacticTautologyDeletion2(c) == true)
      {
        out.println(c.number + " is trivial");
        return true;
      }
      else return false;  
    }
    return false;
  }

  /****************************************************************************
   * Generate uses the inference rules equality resolution, equality factoring
   * as well as positive and negative superposition to compute all possible
   * new clauses between the givenclause and the already processed clauses
   * @param c is the givenclause
   * @param processed set of pure equations already processed
   * @param eq represents the equality function symbol
   * @param neq represents the inequality function symbol
   * @return a list of all newly generated pure equations
   **************************************************************************/
  public List<PureEquation> generate(PureEquation c, List<PureEquation> processed, FunctionSymbol eq, FunctionSymbol neq)
  {
    GeneratingInferences gen = new GeneratingInferences(c, eq, neq, ordering);
    List<PureEquation> t = new ArrayList<>();
    List<PureEquation> resolution = new ArrayList<>();
    List<PureEquation> factoring = new ArrayList<>();
    List<PureEquation> superposition = new ArrayList<>();

    resolution.addAll(gen.computeAllEqResolvents(c));
    for (PureEquation clause : resolution) t.add(newInstance(clause));

    int n = t.size();

    for (PureEquation newclause : t)
    {
      List<PureEquation> parents = newclause.parents;
      out.println("ER: " + parents.get(0).number + " generated " + newclause.number +
          ": " + newclause.printPureEquation(newclause));
    }

    factoring.addAll(gen.equalityFactoring(c));
    for (PureEquation clause : factoring) t.add(newInstance(clause));

    for (int i = n; i < t.size(); i++)
    {
      PureEquation newclause = t.get(i);
      List<PureEquation> parents = newclause.parents;
      out.println("EF:" + parents.get(0).number + " generated " + newclause.number +
          ": " + newclause.printPureEquation(newclause));
    }

    n = t.size();

    superposition.addAll(gen.superposition(c, processed));
    for (PureEquation clause : superposition) t.add(newInstance(clause));

    for (int i = n; i < t.size(); i++)
    {
      PureEquation newclause = t.get(i);
      List<PureEquation> parents = newclause.parents;
      String generatedBy = newclause.generatedby;
      out.println(generatedBy + ": " + parents.get(0).number + " and " + parents.get(1).number
          + " generated " + newclause.number + ": " + newclause.printPureEquation(newclause));
    }

    return t;
  }

  /****************************************************************************
   * Relevant Clauses marks all (and only those) clauses that are relevant/needed
   * to find a proof. Only used when the input set is unsatisfiable and the 
   * prover was able to find a proof. Starting by the empty clause found by the
   * prover and backtracking over the parents attribute of the pure equation.
   * @param processed is the list of processed clauses (pure equations)
   * @param emptyclause the empty clause found by the prover
   * @return List<PureEquation> with the marked attribute adjusted
   **************************************************************************/
  public List<PureEquation> relevantClauses(List<PureEquation> processed, PureEquation emptyclause)
  {
    List<PureEquation> parents = new ArrayList<>();
    for (PureEquation clause : emptyclause.parents) parents.add(clause);

    int i = 0;

    for (PureEquation parent : parents)
    {
      i = 0;
      for (PureEquation clause : processed)
      {
        if (clause == parent)
        {
          int n = emptyclause.marked.size() - 1;
          parent.marked.add(emptyclause.marked.get(n) + 1);
          infdepth = emptyclause.marked.get(n) + 1;
          processed.set(i, parent);
          processed = relevantClauses(processed, clause);
        }
        i++;
      }
    }
    return processed;
  }

  /****************************************************************************
   * Print Proof Tree
   * @param processed is the list of processed clauses (pure equations)
   * @return proof tree in string form
   **************************************************************************/
  public String printProofTree(List<PureEquation> processed)
  {
    Integer i = 1;
    String tree = "[] \r\n";

    while (i <= infdepth)
    {
      for (PureEquation clause : processed)
      {
        if (clause.marked.contains(i)) tree = tree + clause.printPureEquation(clause) + "    ";
      }
      tree = tree + "\r\n";
      i++;
    }
    return tree;
  }

  /****************************************************************************
   * SMT Solver Functions
   * written by Wolfgang Schreiner
   ***************************************************************************/

  //the primary solver and the secondary one used for
  // deriving the minimal unsatisfiable core (may be both null)
  private Solver solver;
  private Solver solver2;

  //current sequence of literals in the context of the SMT solver and the
  // corresponding sequence of free variable sets introduced by every literal
  // private List<Literal> solverLits;
  // private List<Set<VariableSymbol>> solverVars;

  //the sequence of all rules (earlier ones have higher priority)
  private PureEquation[] rules;
  //the (variable-free) rules submitted to the SMT solver
  private List<PureEquation> smtRules;

  private BooleanSupplier isAborted;

  //a literal pushed on the assumptions stack
  public static class Literal
  {
    public final Object term;
    public final boolean neg;
    public Literal(Object term, boolean neg) { this.term = term; this.neg = neg; }
  }

  //the current proof context
  public static class Context
  {
    // the current proof depth
    public final int depth;

    // the current substitutions
    public final Subst subst;

    // the stack of assumptions
    public final ConsList<Literal> assumptions;

    // the stack of applied equalities
    public final ConsList<Equality> equalities;

    public Context(int depth, Subst subst,
      ConsList<Literal> assumptions, ConsList<Equality> equalities)
    {
      this.depth = depth;
      this.subst = subst;
      this.assumptions = assumptions;
      this.equalities = equalities;
    }

    // create a new context with depth 0
    public Context()
    {
      this(0, new Subst(), new ConsList<Literal>(), new ConsList<Equality>());
    }

    // change substitution without increasing the depth
    public Context changeSubst(Subst subst)
    {
      return new Context(depth, subst, assumptions, equalities);
    }

    // add assumption increasing the depth
    public Context addAssumption(Literal assumption)
    {
      return new Context(depth+1, subst, assumptions.cons(assumption), equalities);
    }

    // add equality without increasing the depth
    // (if the reverse already occurs, null is returned)
    public Context addEquality(Object told, Object tnew)
    {
      for (Equality eq : equalities)
      {
        if (Term.equal(told, eq.tnew) && Term.equal(tnew, eq.told))
          return null;
      }
      return new Context(depth, subst, assumptions, equalities.cons(new Equality(told, tnew)));
    }
  }

  /****************************************************************************
   * Create incremental SMT solver.
   * @return the solver (null, if no solver could be created).
   ***************************************************************************/
  private Solver createSolver()
  {
    String version = Solver.version();
    if (version == null)
    {
      out.println("Could not create solver");
      out.println("===");
      return null;
    }
    if (!Solver.incremental())
    {
      out.println("SMT solver does not support incremental mode.");
      out.println("Choose another solver or switch off SMT support for proving.");
      return null;
    }
    Solver solver = Solver.create(true, true, true, out, null, null, null);
    return solver;
  }

  /***************************************************************************
   * Stop solver.
   * @param the solver to be stopped.
   **************************************************************************/
  private void stopSolver(Solver solver)
  {
    // first attempt graceful exit, then (after some delay) terminate forcefully
    try { solver.exitSolver(); } catch (Exception e) { }
    new Thread(()->
    {
      try { Thread.sleep(1000); } catch (InterruptedException e) { }
      solver.terminate();
    }).start();
  }

  /**************************************************************************
   * Initialize SMT solvers.
   *************************************************************************/
  private void startSMT(ClauseProblem problem)
  {
    solver = createSolver();
    if (solver == null) throw new RuntimeException("could not start SMT solver");
    {
      // initialize solver to produce unsatisfiable core
      solver2 = createSolver();
      if (solver2 == null) throw new RuntimeException("could not start SMT solver");
    }
    // initialize the SMT solver with the original problem and the declarations
    // of the Skolem functions generated for the clause problem
    if (solver != null)
    {
      solver.init(problem.getProofProblem());
      for (Decl decl : problem.getDeclarations())
        solver.add(decl);
    }
    if (solver2 != null)
    {
      solver2.init(problem.getProofProblem());
      for (Decl decl : problem.getDeclarations())
        solver2.add(decl);
    }

    List<Clause> clauses = problem.getClauses();
    int n = clauses.size();

    // the proof problem
    rules = new PureEquation[n];

    for (int i = 0; i < n; i++)
    {
      // we invert the ordering of rules because later clauses are deemed
      // higher-level and thus should be attempted first
      Clause clause = clauses.get(n-i-1);
      PureEquation rule = new PureEquation(clause, problem);
      rule.setImplied(clause.isImplied());
      rule.setTheory(clause.isTheory());
      rules[i] = rule;
    }

    // register the SMT rules (in the original order)
    smtRules = new ArrayList<PureEquation>();
    for (PureEquation rule : rules)
    {
      // SMT rules are the variable-free rules that do not-characterize a theory
      if (rule.vars.length != 0) continue;
      if (rule.isTheory()) continue;
      smtRules.add(rule);
    }
    Collections.reverse(smtRules);

    // immediately assert the SMT rules to the solver
    for (PureEquation rule : smtRules)
    {
      solver.assume(rule.lits, rule.negs);
    }
    // solverLits = new ArrayList<Literal>();
    // solverVars = new ArrayList<Set<VariableSymbol>>();
  }

  /**************************************************************************
   * Stop SMT solvers.
   *************************************************************************/
  private void stopSMT()
  {
    if (solver != null) { stopSolver(solver); solver = null; }
    if (solver2 != null) { stopSolver(solver2); solver2 = null; }
  }

  /****************************************************************************
   * Check whether the SMT solver can prove the unsatisfiability of
   * a collection of closed rules.
   * @param rules the closed rules.
   * @param rules0 set to the minimal unsatisfiable core (if the proof succeeded)
   * @return string that starts with "unsat" if the proof has succeeded.
   ***************************************************************************/
  private String solveSMT(List<PureEquation> rules, List<PureEquation> rules0)
  {
    solver2.openContext();
    int i = 1;
    for (PureEquation rule : rules)
    {
      String name = "clause" + Integer.toString(i); i++;
      solver2.assume(name, rule.vars, rule.lits, rule.negs);
    }
    String result = solver2.isUnsat();
    if (result != null && result.startsWith("unsat"))
      unsatCore(rules, rules0);
    solver2.closeContext();
    return result;
  }

  /*************************************************************************
   * Try to prove the theorem by showing the unsatisfiability of the clauses.
   * @return true if the proof could be closed.
   ************************************************************************/
  public boolean smtTheorem(ClauseProblem problem, ProofProblem proofProblem)
  {
    if (solver2 == null) return false;
    // determine the rules to be presented to the SMT solver
    List<PureEquation> rules0 = new ArrayList<PureEquation>();
    int n = rules.length;
    for (int i = n-1; i >= 0; i--)
    {
      PureEquation rule = rules[i];
      if (rule.isTheory()) continue;
      rules0.add(rule);
    }
    out.println("We apply the SMT solver to the clauses...");
    List<PureEquation> rules1 = new ArrayList<PureEquation>();
    String result1 = solveSMT(rules0, rules1);
    boolean unsat1 = result1 != null && result1.startsWith("unsat");
    if (unsat1)
    {
      out.println("Theorem was proved from the clauses.");
    }
    else
    {
      out.println("Theorem was NOT proved from the clauses.");
    }
    // determine whether there are variables in the current rule set
    boolean hasVar = false;
    for (PureEquation rule1 : rules1)
    {
      if (rule1.vars.length > 0) { hasVar = true; break; }
    }
    String result2 = null;
    boolean unsat2 = false;
    List<PureEquation> instances = null;
    List<PureEquation> instances0 = null;
    if (hasVar && !checkAbort(false))
    {
      out.println("We apply the SMT solver to ground instances of the clauses...");
      // use for instantiation of rules1 ground terms from all rules (rules0)
      instances = InstancesRes.generate(proofProblem, rules1, rules0);
      instances0 = new ArrayList<PureEquation>();
      result2 = solveSMT(instances, instances0);
      unsat2 = result2 != null && result2.startsWith("unsat");
      if (unsat2)
        out.println("Theorem was proved from the instances.");
      else
        out.println("Theorem was NOT proved from the instances.");
    }
    return unsat1 || unsat2;
  }

  /****************************************************************************
   * Check whether user has requested an abort.
   ***************************************************************************/
  private boolean checkAbort(boolean exception)
  {
    boolean aborted = isAborted != null && isAborted.getAsBoolean();
    if (aborted && exception)
      throw new AbortedException("the user has requested an abort", false);
    return aborted;
  }

  // the timeout value (0 for no timeout)
  private static long TIMEOUT = 60000;

  /****************************************************************************
   * Set the prover timeout value.
   * @param timeout the number of milliseconds after which proof is aborted
   *        (0 if no timeout)
   ***************************************************************************/
  public static void setTimeout(long timeout)
  {
    TIMEOUT = timeout;
  }
  /****************************************************************************
   * Check whether timeout has occurred
   ***************************************************************************/
  private boolean checkTimeout(boolean exception)
  {
    boolean aborted = TIMEOUT != 0 && TIMEOUT < (System.nanoTime()-starttime)/1000000;
    if (aborted && exception)
      throw new AbortedException("the proving time exceeds the timeout value "
          + "(" + TIMEOUT + " ms)", false);
    return aborted;
  }
  
  /**************************************************************************
   * Add to rules0 the unsatisfiable core of rules.
   * @param rules the rules that were deeemed unsatisfiable by the solver.
   * @param rules0 set to the unsatisfiable core reported by the solver.
   *************************************************************************/
  private void unsatCore(List<PureEquation> rules, List<PureEquation> rules0)
  {
    String result2 = solver2.getUnsatCore();
    if (result2 == null || result2.length() < 2)
    {
      rules0.addAll(rules);
      return;
    }
    int begin = result2.indexOf('(');
    int end = result2.lastIndexOf(')');
    if (begin >= 0 && end >= 0) result2 = result2.substring(begin+1, end);
    result2 = result2.replace('\n', ' ');
    Set<String> names = new HashSet<String>();
    for (String name : result2.split(" ")) names.add(name);
    int i = 1;
    for (PureEquation rule : rules)
    {
      String name = "clause" + Integer.toString(i); i++;
      if (names.contains(name)) rules0.add(rule);
    }
  }

} // ends class

// ----------------------------------------------------------------------------
// end of file
// ----------------------------------------------------------------------------
