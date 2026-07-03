// ----------------------------------------------------------------------------
// SimplifyingInferences.java
//
// $Id: SimplifyingInferences.java,v 1.3 2026/04/14 12:32:59 schreine Exp $
//
// Author: Viktoria Langenreither <viki.langi@gmail.com>
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
import java.util.function.*;
import risctp.fol.*;
import risctp.syntax.AST.Exp.Not;
import risctp.types.Symbol;
import risctp.types.Symbol.FunctionSymbol;
import risctp.types.Symbol.VariableSymbol;

public class SimplifyingInferences
{
  // the PureEquation
  public PureEquation SimplInf;

  // the equality and inequality function symbol
  public FunctionSymbol eq;
  public FunctionSymbol neq;

  // the underlying substitution
  public Subst sigma;

  //ordering of the function/constant symbols used for the lexicographic path ordering
  public List<FunctionSymbol> ordering;

  // create a new SimplifyingInferences from a PureEquation
  public SimplifyingInferences(PureEquation problem, FunctionSymbol eq, FunctionSymbol neq, Subst sigma, 
    List<FunctionSymbol> ordering)
  {
    this.SimplInf = problem;
    this.eq = eq;
    this.neq = neq;
    this.sigma = sigma;
    this.ordering = ordering;
  }

  /**************************************************************************
   * Deletion of duplicated literals: 
   * If a clause contains two literals (equalities) that are equal
   * @param clause, the clause to checked
   * @return PureEquation, with one of the duplicated literals deleted
   *************************************************************************/
  public PureEquation deleteDuplicatedLiterals(PureEquation clause)
  {
    List<Object> lits = new ArrayList<Object>();
    List<Boolean> negs = new ArrayList<Boolean>();
    int i = 0;

    for (Object c : clause.lits)
    {
      if (lits.contains(c) == false)
      {
        lits.add(c);
        negs.add(clause.negs[i]);
      }
      i++;
    }

    boolean[] new_negs = new boolean[negs.size()];
    Object[] new_lits = new Object[lits.size()];
    for (int j = 0; j < negs.size(); j++) new_negs[j] = negs.get(j);
    for (int j = 0; j < lits.size(); j++) new_lits[j] = lits.get(j);

    PureEquation rule = new PureEquation(clause.name, clause.number, clause.vars, new_lits, new_negs, clause.select,
        clause.score, clause.age, clause.generatedby, clause.parents, clause.marked);
    return rule;
  }

  /**************************************************************************
   * Deletion of resolved literals: 
   * If a clause contains a literal (inequality) with the same term on both sides
   * @param clause, the clause to checked
   * @return PureEquation, with this literal deleted
   *************************************************************************/
  public PureEquation deleteResolvedLiterals(PureEquation clause)
  {
    List<Object> lits = new ArrayList<Object>();
    List<Boolean> negs = new ArrayList<Boolean>();
    int i = 0;

    for (Object c : clause.lits)
    {
      boolean n = clause.negs[i];
      if((Term.equal(Term.argument(c, 0), Term.argument(c, 1)) && n == true) == false) 
      {
        lits.add(c);
        negs.add(n);
      }
      i++;
    }

    boolean[] new_negs = new boolean[negs.size()];
    Object[] new_lits = new Object[lits.size()];
    for (int j = 0; j < negs.size(); j++) new_negs[j] = negs.get(j);
    for (int j = 0; j < lits.size(); j++) new_lits[j] = lits.get(j);

    PureEquation rule = new PureEquation(clause.name, clause.number, clause.vars, new_lits, new_negs,
        clause.select, clause.score, clause.age, clause.generatedby, clause.parents, clause.marked);
    return rule;
  }

  /**************************************************************************
   * Syntactic tautology deletion 1: 
   * @param clause, the clause to checked
   * @return returns true, if the clause contains an equation where the left
   *         and right side are syntactically the same
   *************************************************************************/
  public boolean syntacticTautologyDeletion1(PureEquation clause)
  {
    List<Object> lits = new ArrayList<>();
    for (Object c : clause.lits) lits.add(c);
    boolean[] negs = clause.negs;
    for( int i = 0; i < lits.size(); i++)
    {
      if(Term.equal(Term.argument(lits.get(i), 0), Term.argument(lits.get(i), 1))
          && (negs[i] == false)) return true;
    }	
    return false;
  }

  /**************************************************************************
   * Syntactic tautology deletion 2: 
   * @param clause, the clause to checked
   * @return returns true, if the clause contains two syntactically equal
   *         literals with opposite negations
   *************************************************************************/
  public boolean syntacticTautologyDeletion2(PureEquation clause)
  {
    List<Object> lits = new ArrayList<>();
    for (Object c : clause.lits) lits.add(c);
    boolean[] negs = clause.negs;
    int size = lits.size();

    for(int i = 0; i < size; i++)
    {
      for(int j = i + 1; j < size; j++)
      {
        if (negs[i] != negs[j])
        {
          if(Term.equal(Term.argument(lits.get(i), 0), Term.argument(lits.get(j), 0)))
          {
            if(Term.equal(Term.argument(lits.get(i), 1), Term.argument(lits.get(j), 1)))
            {
              return true;
            }
          }
        }
      }
    }
    return false;
  }

  /**************************************************************************
   * Destructive equality resolution: 
   * (This rule is only applied if the method applicabilityDestrEqRes returns
   * an integer greater 0)
   * @param clause, the clause to apply the rule to
   * @param pos, the position of the literal x != s
   * @return returns PureEquation without the literal  x != s, but applied the
   *         most general unifier of x and s to the other literals in clause
   *************************************************************************/
  public PureEquation destructiveEqualityResolution(PureEquation clause, int pos)
  {
    List<Object> lits = new ArrayList<Object>();
    Object x = clause.lits[pos];
    for (Object c : clause.lits) if (c != x) lits.add(c);

    Subst sigma = new Subst();
    // boolean unify = Subst.unify(sigma, Term.argument(lits.get(pos), 0),
    //    Term.argument(lits.get(pos), 1));
    boolean unify = Subst.unify(sigma, Term.argument(x, 0), Term.argument(x, 1));
    
    int litn = lits.size();
    Object[] lits0 = new Object[litn];
    for (int i = 0; i < litn; i++) lits0[i] = sigma.apply(lits.get(i));

    // initialize the literals and their polarities
    boolean[] negs = new boolean[litn];
    for (int i = 0; i < litn; i++)
    {
      if (lits0[i] instanceof Not) negs[i] = true;
      else negs[i] = false;
    }

    PureEquation rule = new PureEquation(clause.name, clause.number, clause.vars, lits0, negs, clause.select,
        clause.score, clause.age, clause.generatedby, clause.parents, clause.marked);
    return rule;
  }

  // check the applicability for destructive equality resolution
  // returns -1 if not applicable otherwise returns the position of the literal
  public int applicabilityDestrEqRes(PureEquation clause)
  {
    List<Object> lits = new ArrayList<>();
    for (Object l : clause.lits) lits.add(l);
    Subst sigma = new Subst();

    int litn = lits.size();
    for (int i = 0; i < litn; i++)
    {
      Object x = Term.argument(lits.get(i), 0);
      if(x instanceof VariableSymbol)
      {
        Object s = Term.argument(lits.get(i), 1);
        boolean unify = Subst.unify(sigma, x, s);
        if (unify == true) return i;
      } 
    }
    return -1;
  }

  /**************************************************************************
   * Clause subsumption:
   * @param clause1, the clause (from the set processed) which might subsume
   *        the givenclause
   * @param givenclause, the clause (in our case the given clause) which might
   *        get subsumed
   * @return returns true, if clause1 subsumes givenclause
   *************************************************************************/
  public boolean clauseSubsumption(PureEquation clause1, PureEquation givenclause)
  {
    Subst sigma = new Subst();

    List<Object> lits1 = new ArrayList<>();
    for (Object l : clause1.lits) lits1.add(l);
    List<Object> lits2 = new ArrayList<>();
    for (Object l : givenclause.lits) lits2.add(l);

    int size1 = lits1.size();
    int size2 = lits2.size();

    if (size2 < size1) return false;
    if (size1 == size2)
    {
      boolean matchable = Subst.match(sigma, lits1, lits2);
      if (matchable == false) return false;
      else return true;
    } else
    {
      List<List<Object>> allsubsets = subsets(givenclause, size1);
      for (List<Object> subset : allsubsets)
      {
        boolean matchable = Subst.match(sigma, lits1, subset);
        if (matchable == false) return false;
        else return true;
      }
    }
    return false;
  }


  // calculates all possible subsets of given length of the literals in givenclause
  // https://www.geeksforgeeks.org/dsa/finding-all-subsets-of-a-given-set-in-java/
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
   * Rewriting of positive and negative literals: handling positive and
   * negative rewriting (symmetric rules) also checks the preconditions:
   * u|p = sigma(s) and sigma(s) > sigma(t)
   * @param clause1, the clause s = t (left hand side)
   * @param givenclause, the given clause u = v V R respectively u != v V R
   *        (right hand side)
   * @return PureEquation u[p <-- sigma(t)] = v V R 
   *************************************************************************/
  public PureEquation rewritingLiterals(PureEquation clause1, PureEquation givenclause)
  {
    if (clause1.lits.length != 1) return givenclause;
    if (clause1.negs[0] == true) return givenclause;
    List<Object> lits = new ArrayList<>();
    for (Object l : givenclause.lits) lits.add(l);
    Object s = Term.argument(clause1.lits[0], 0);
    Object t = Term.argument(clause1.lits[0], 1);
    int i = 0;
    for (Object lit : lits)
    {
      Subst sigma = new Subst();
      Object u =  Term.argument(lit, 0);
      // trying to find a substitution such that u = sigma(s)
      boolean unify = Subst.match(sigma, s, u); 
      if (unify == true)
      {
        // check if sigma(s) > sigma(t) 
        if (PureEquation.lpo(sigma.apply(s), sigma.apply(t), ordering) == true) 
        { // creating the new literal sigma(t) = v respectively sigma(t) != v
          Object[] args = new Object[2];
          args[0] = sigma.apply(t);
          args[1] = Term.argument(lit, 1);
          if (givenclause.negs[i] == true) lits.set(i, Term.term(neq, args));
          else lits.set(i, Term.term(eq, args));
        } 
        i++;
        continue; 
      } //end if unify	  
      i++;
    } //end for
    int litn = lits.size();
    Object[] lits0 = new Object[litn];
    for (int j = 0; j < litn; j++) lits0[j] = lits.get(j);
    return new PureEquation(givenclause.name, givenclause.number, givenclause.vars, lits0,
        givenclause.negs,
        givenclause.select, givenclause.score, givenclause.age, givenclause.generatedby,
        givenclause.parents, givenclause.marked);
  }

  //----------------------------------------------------------------------------

  /*public PureEquation rewritingLiteralsInnerTerms_old(PureEquation clause1, PureEquation givenclause)
  {
	if (clause1.lits.length != 1) return givenclause;
	if (clause1.negs[0] == true) return givenclause;
	List<Object> lits = new ArrayList<>();
	for (Object l : givenclause.lits) lits.add(l);
	Object s = Term.argument(clause1.lits[0], 0);
	Object t = Term.argument(clause1.lits[0], 1);
	int i = 0;
	for (Object lit : lits) // checking the precondition for the term u (instead of a position in u), also computing the result
	{
	  Subst sigma = new Subst();
	  Object u =  Term.argument(lit, 0);
	  boolean unify = Subst.match(sigma, s, u); // trying to find a substitution such that u = sigma(s)
	  if (unify == true)
	  {
		if (PureEquation.lpo(sigma.apply(s), sigma.apply(t), ordering) == true) // check if sigma(s) > sigma(t)
		{ // creating the new literal sigma(t) = v respectively sigma(t) != v
		  Object[] args = new Object[2];
		  args[0] = sigma.apply(t);
		  args[1] = Term.argument(lit, 1);
		  if (givenclause.negs[i] == true) lits.set(i, Term.term(neq, args));
		  else lits.set(i, Term.term(eq, args));
		} 
		i++;
		continue; 
	  } //end if unify	  
	  for (int p = 0; p < Term.argnumber(u); p++) // checking the precondition for the positions p in term u, also computing the result
	  {
	    sigma = new Subst();
		Object up = Term.argument(u, p);
		  unify = Subst.match(sigma, s, up); // trying to find a substitution such that u|p = sigma(s)
		  if (unify == true)
		  { 
		    if (PureEquation.lpo(sigma.apply(s), sigma.apply(t), ordering) == true) // check if sigma(s) > sigma(t)
			{ // creating the new literal u[p <-- sigma(t)] = v respectively u[p <-- sigma(t)] != v
			  Object[] args = new Object[2];			  
			  Object[] u0 = (Object[]) u;
			  u0[p] = t;
			  args[0] = sigma.apply(u0);
			  args[1] = Term.argument(lit, 1);
			  if (givenclause.negs[i] == true) lits.set(i, Term.term(neq, args));
			  else lits.set(i, Term.term(eq, args));
			} 
		  }
		}
	  i++;
	} //end for
	int litn = lits.size();
	Object[] lits0 = new Object[litn];
	for (int j = 0; j < litn; j++) lits0[j] = lits.get(j);
    return new PureEquation(givenclause.name, givenclause.number, givenclause.vars, lits0, givenclause.negs,
    	givenclause.select, givenclause.score, givenclause.age, givenclause.generatedby, givenclause.parents, givenclause.marked);
  }
   */
  //----------------------------------------------------------------------------

  public PureEquation rewritingLiteralsInnerTerms(PureEquation clause1, PureEquation givenclause, Object u, Object v,
    int lit, Function<Object, Object> overwrite)
  {
    if (clause1.lits.length != 1) return givenclause;
    if (clause1.negs[0] == true) return givenclause;
    List<Object> lits = new ArrayList<>();
    for (Object l : givenclause.lits) lits.add(l);
    Object s = Term.argument(clause1.lits[0], 0);
    Object t = Term.argument(clause1.lits[0], 1);

    PureEquation returnclause = givenclause;


    if (u instanceof VariableSymbol) return returnclause;
    for (int p = 0; p < Term.argnumber(u); p++) // checking the precondition for the positions p in term u, also computing the result
    {
      final int i0 = p;
      sigma = new Subst();

      Object up = Term.argument(u, p);
      Object[] args = new Object[2];	
      boolean unify = Subst.match(sigma, s, up); // trying to find a substitution such that u|p = sigma(s)
      if (unify == true)
      { 
        if (PureEquation.lpo(sigma.apply(s), sigma.apply(t), ordering) == true) // check if sigma(s) > sigma(t)
        { // creating the new literal u[p <-- sigma(t)] = v respectively u[p <-- sigma(t)] != v		  

          Object u0 = sigma.apply(t);
          u0 = overwrite.apply(u0);
          args[0] = u0;
          args[1] = v;
          if (givenclause.negs[lit] == true) lits.set(lit, Term.term(neq, args));
          else lits.set(lit, Term.term(eq, args));
        } 
      }

      int litn = lits.size();
      Object[] lits0 = new Object[litn];
      for (int j = 0; j < litn; j++) lits0[j] = lits.get(j);
      PureEquation simplified_givenclause = new PureEquation(givenclause.name, givenclause.number, givenclause.vars, lits0, givenclause.negs,
          givenclause.select, givenclause.score, givenclause.age, givenclause.generatedby, givenclause.parents, givenclause.marked);
      Object v0 = Term.argument(givenclause.lits[lit], 1);

      int n = Term.argnumber(u);
      FunctionSymbol fun = (FunctionSymbol)Term.symbol(u);

      Function<Object,Object> overwrite0 = (Object term0)->
      {
        Object[] terms0 = new Object[n];
        for (int j = 0; j < n; j++)
        {
          if (j == i0)
            terms0[j] = term0;
          else
            terms0[j] = Term.argument(u, j);
        }
        Object term1 = Term.term(fun, terms0);
        Object term2 = overwrite.apply(term1);
        return term2;
      };

      if (args[0] == null)
      {
        Symbol sym = Term.symbol(up);
        if (sym instanceof FunctionSymbol)
        {
          if (Term.argnumber(up) == 0) return returnclause;
          else returnclause = rewritingLiteralsInnerTerms(clause1, simplified_givenclause, up, v0, lit, overwrite0);
        }

      } else {
        //Object test = args[0];
        Symbol sym = Term.symbol(args[0]);
        if (sym instanceof FunctionSymbol)
        {
          if (Term.argnumber(args[0]) == 0) return returnclause;
          else returnclause = rewritingLiteralsInnerTerms(clause1, simplified_givenclause, Term.argument(args[0], p), v0, lit, overwrite0);
        }
      }


    }
    return returnclause;
  }

  //----------------------------------------------------------------------------

  public PureEquation rewritingAllLiterals(PureEquation clause1, PureEquation givenclause)
  {  
    PureEquation simplc = givenclause;
    simplc = rewritingLiterals(clause1, givenclause);

    List<Object> lits = new ArrayList<>();
    for (Object l : givenclause.lits) lits.add(l);
    int i = 0;

    for (Object lit : lits)
    {
      Function<Object,Object> overwrite = (Object t)->t;
      simplc = rewritingLiteralsInnerTerms(clause1, givenclause, lit, Term.argument(lit, 1), i, overwrite);

      i++;
    }
    return simplc;
  }

  /**************************************************************************
   * Negative simplify-reflect: 
   * @param clause1, the clause s != t (left hand side)
   * @param givenclause, the given clause sigma(s = t) V R (right hand side)
   * @return PureEquation R 
   *************************************************************************/
  public PureEquation negativeSimplifyReflect(PureEquation clause1,
    PureEquation givenclause)
  {
    if (clause1.lits.length != 1) return givenclause;
    if (clause1.negs[0] == false) return givenclause;  
    List<Object> lits = new ArrayList<>();
    for (Object l : givenclause.lits) lits.add(l);
    List<Boolean> negs = new ArrayList<Boolean>();
    for (boolean neg : givenclause.negs) negs.add(neg);

    Object st = clause1.lits[0];
    List<Object> newlits = new ArrayList<>();
    List<Boolean> newnegs = new ArrayList<>();

    int i = 0;
    for (Object lit : lits)
    {
      Subst sigma = new Subst();
      if (givenclause.negs[i] == false)
      { // trying to find a substitution for the literal in clause 1 (s != t)
        // and a literal l in the given clause such that sigma(l) = s!=t
        boolean unify = Subst.match(sigma, st, lit);
        // only literals that cannot be matched with a substitution are added
        // to the return PureEquation
        if (unify == true)
        {
          i++;
          continue; 
        }else
        {
          newlits.add(lit);
          newnegs.add(givenclause.negs[i]);
        }
      } else
      {
        newlits.add(lit);
        newnegs.add(givenclause.negs[i]);
      }
      i++;
    } //end for
    int litn = newlits.size();
    Object[] lits0 = new Object[litn];
    for (int j = 0; j < litn; j++) lits0[j] = newlits.get(j);
    boolean[] negs0 = new boolean[litn];
    for (int j = 0; j < litn; j++) negs0[j] = newnegs.get(j);

    return new PureEquation(givenclause.name, givenclause.number, givenclause.vars,
        lits0, negs0, givenclause.select,
        givenclause.score, givenclause.age, givenclause.generatedby,
        givenclause.parents, givenclause.marked);
  }

} // ends class

// ----------------------------------------------------------------------------
// end of file
// ----------------------------------------------------------------------------
