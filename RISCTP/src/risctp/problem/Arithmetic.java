// ---------------------------------------------------------------------------
// Arithmetic.java
// Arithmetic simplification of first-order formulas.
// $Id: Arithmetic.java,v 1.19 2024/07/15 15:13:31 schreine Exp $
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
import risctp.syntax.AST.Exp.*;
import risctp.types.Symbol.*;

// --------------------------------------------------------------------------
// A cloner that simplifies formulas with respect to arithmetic properties.
//--------------------------------------------------------------------------
public class Arithmetic extends ASTCloner
{
  // the integer type
  private TypeSymbol intSymbol;
  
  // the relevant integer symbols
  private FunctionSymbol equalPred;
  private FunctionSymbol notEqualPred;
  private FunctionSymbol lessPred;
  private FunctionSymbol lessEqualPred;
  private FunctionSymbol greaterPred;
  private FunctionSymbol greaterEqualPred;
  private FunctionSymbol plusFun;
  private FunctionSymbol minusFun;
  private FunctionSymbol negationFun;
  private FunctionSymbol multFun;
  
  // the integer zero
  private FunctionSymbol zeroConst;

  /****************************************************************************
   * Perform arithmetic simplification of proof problem.
   * @param problem the proof problem.
   * @return an equivalent problem where the simplification has been performed.
   ***************************************************************************/
  public static ProofProblem process(ProofProblem problem)
  {
    Arithmetic cloner = new Arithmetic(problem);
    return cloner.transform(problem);
  }
  
  /****************************************************************************
   * Create cloner for a proof problem.
   * @param problem the proof problem.
   ***************************************************************************/
  public Arithmetic(ProofProblem problem)
  {
    // ids remain shared (and thus preserve symbols)
    super(false);

    // gather relevant theory symbols
    intSymbol = problem.intSymbol;
    equalPred = problem.equalities.get(problem.intSymbol);
    notEqualPred = problem.inequalities.get(problem.intSymbol);
    for (FunctionSymbol fun : problem.intFuns)
    {
      String name = fun.id.toString();
      switch (name)
      {
      case "0": zeroConst = fun; break;
      case AST.LESSNAME: lessPred = fun; break;
      case AST.LESSEQUALNAME: lessEqualPred = fun; break;
      case AST.GREATERNAME: greaterPred = fun; break;
      case AST.GREATEREQUALNAME: greaterEqualPred = fun; break;
      case AST.PLUSNAME: plusFun = fun; break;
      case AST.MINUSNAME: minusFun = fun; break;
      case "'"+AST.MINUSNAME+"§0'": negationFun = fun; break;
      case AST.MULTNAME: multFun = fun; break;
      }
    }
  }
  
  /****************************************************************************
   * Transform a proof problem.
   * @param problem the problem.
   * @return the result of the transformation.
   **************************************************************************/
  private ProofProblem transform(ProofProblem problem)
  {
    // perform transformation
    ProofProblem result = new ProofProblem(problem);
    result.decls = new ArrayList<Decl>();
    for (Decl decl : problem.decls)
    {
      // only rewrite theorems 
      if (decl instanceof Decl.Theorem) 
        decl = (Decl)decl.accept(this);
      result.decls.add(decl);
    }
    return result;
  }

  // transform an atomic formula
  public Exp visit(Apply exp)
  {
    FunctionSymbol fun = (FunctionSymbol)exp.id.getSymbol();
    Exp exp0 = super.visit(exp);
    if (isBinaryIntegerRelation(fun)) 
      exp0 = simplifyBinaryIntegerRelation(exp);
    return exp0;
  }

  // true if symbol denotes a binary integer relation
  public boolean isBinaryIntegerRelation(FunctionSymbol fun)
  {
    if (fun == equalPred) return true;
    if (fun == notEqualPred) return true;
    if (fun == lessPred) return true;
    if (fun == lessEqualPred) return true;
    if (fun == greaterPred) return true;
    if (fun == greaterEqualPred) return true;
    return false;
  }
  
  // rewrite a-b ~ c to a ~ b+c and a+b ~ c+b to a ~ c
  private Exp simplifyBinaryIntegerRelation(Apply exp)
  {
    FunctionSymbol fun = (FunctionSymbol)exp.id.getSymbol();
    Exp term1 = exp.exps[0];
    Exp term2 = exp.exps[1];
    List<Exp> pterms1 = new ArrayList<Exp>();
    List<Exp> pterms2 = new ArrayList<Exp>();
    List<Exp> nterms1 = new ArrayList<Exp>();
    List<Exp> nterms2 = new ArrayList<Exp>();
    separateSummands(term1, pterms1, nterms1);
    separateSummands(term2, pterms2, nterms2);
    pterms1.addAll(nterms2);
    pterms2.addAll(nterms1);
    // removeDuplicateSummands(pterms1, pterms2);
    simplifySummands(pterms1, pterms2);
    Exp sum1 = constructSum(pterms1);
    Exp sum2 = constructSum(pterms2);
    Integer number1 = number(sum1);
    Integer number2 = number(sum2);
    if (number1 != null && number2 != null)
    {
      Boolean result = 
          (fun == equalPred) ? (number1 == number2) :
            (fun == notEqualPred) ? (number1 != number2) :
              (fun == lessPred) ? (number1 < number2) :
                (fun == lessEqualPred) ? (number1 <= number2) :
                  (fun == greaterPred) ? (number1 > number2) :
                    (fun == greaterEqualPred) ? (number1 >= number2) :
                      null;
      if (result != null) return result ? True.create() : False.create();
    }
    if (fun == equalPred)
    {
      Exp[] exps0 = new Exp[2];
      boolean okay0 = splitVarNum(pterms1, exps0);
      if (okay0)
      {
        sum1 = exps0[0];
        sum2 = Apply.create(minusFun.id, Arrays.asList(new Exp[] {sum2, exps0[1]}));
      }
      else
      {
        okay0 = splitVarNum(pterms2, exps0);
        if (okay0)
        {
          sum2 = exps0[0];
          sum1 = Apply.create(minusFun.id, Arrays.asList(new Exp[] {sum1, exps0[1]}));
        }
      }
    }
    List<Exp> args = new ArrayList<Exp>();
    args.add(sum1);
    args.add(sum2);
    Apply result = Apply.create(exp.id, args);
    result.setNotGoal(exp.isNotGoal());
    return result;
  }
  
  // split "[x,n]" or "[n,x]" into "x" and "n" and signal success
  private boolean splitVarNum(List<Exp> exps, Exp[] exps0)
  {
    if (exps.size() != 2) return false;
    Exp exp1 = exps.get(0);
    Exp exp2 = exps.get(1);
    if (!(exp1 instanceof Apply) || !(exp2 instanceof Apply)) return false;
    Apply apply1 = (Apply)exp1;
    Apply apply2 = (Apply)exp2;
    boolean nat1  = AST.isNat(apply1.id.toString());
    boolean nat2 = AST.isNat(apply2.id.toString());
    if (!nat1 && nat2) { exps0[0] = exp1; exps0[1] = exp2; return true; } 
    if (nat1 && !nat2) { exps0[0] = exp2; exps0[1] = exp1; return true; }
    return false;
  }
  
  // construct a sum from a list of terms
  private Exp constructSum(List<Exp> terms)
  {
    Exp sum = null;
    for (Exp term : terms)
    {
      if (term instanceof Apply)
      {
        Apply apply = (Apply)term;
        if (apply.id.toString().equals("0")) continue;
      }
      if (sum == null) { sum = term; continue; }
      List<Exp> args = new ArrayList<Exp>();
      args.add(sum);
      args.add(term);
      sum = Apply.create(plusFun.id, args);
    }
    if (sum == null) sum = Apply.create(zeroConst.id, new ArrayList<Exp>());
    return sum;
  }
  
  // separate the term into positive and negative summands
  private void separateSummands(Exp term, List<Exp> pos, List<Exp> neg)
  {
    if (!(term instanceof Apply)) { pos.add(term); return;}
    Apply apply = (Apply)term;
    FunctionSymbol fun = (FunctionSymbol)apply.id.getSymbol();
    Exp[] terms = apply.exps;
    if (fun == plusFun)
    {
      separateSummands(terms[0], pos, neg);
      separateSummands(terms[1], pos, neg);
      return;
    }
    if (fun == minusFun)
    {
      separateSummands(terms[0], pos, neg);
      separateSummands(terms[1], neg, pos);
      return;
    }
    if (fun == negationFun)
    {
      separateSummands(terms[0], neg, pos);
      return;
    }
    pos.add(term);
  }
  
  // remove identical summands from both sides
  /*
  private void removeDuplicateSummands(List<Exp> pos, List<Exp> neg)
  {
    List<String> nstrings = new ArrayList<String>();
    for (Exp n : neg) nstrings.add(n.toString());
    List<Exp> pos0 = new ArrayList<Exp>(pos);
    pos.clear();
    for (Exp p : pos0)
    {
      String pstring = p.toString();
      int j = nstrings.indexOf(pstring);
      if (j == -1) { pos.add(p); continue; }
      nstrings.remove(j);
      neg.remove(j);
    }
  }
  */

  // simplify c*s+...~d*s+... to (c-d)*s+...~... for constants c >= d
  private void simplifySummands(List<Exp> pos, List<Exp> neg)
  {
    List<Integer> pconsts = new ArrayList<Integer>();
    List<Integer> nconsts = new ArrayList<Integer>();
    List<Exp> pterms = new ArrayList<Exp>();
    List<Exp> nterms = new ArrayList<Exp>();
    List<String> pstrings = new ArrayList<String>();
    List<String> nstrings = new ArrayList<String>();
    splitFactors(pos, pconsts, pterms, pstrings);
    splitFactors(neg, nconsts, nterms, nstrings);
    pos.clear();
    neg.clear();
    int plen = pconsts.size();
    for (int i = 0; i < plen; i++)
    {
      int pconst = pconsts.get(i);
      Exp pterm = pterms.get(i);
      String pstring = pstrings.get(i);
      int j = nstrings.indexOf(pstring);
      if (j == -1) 
      { 
        pos.add(productExp(pconst, pterm));
        continue;
      }
      int nconst = nconsts.get(j);
      if (pconst > nconst)
        pos.add(productExp(pconst-nconst, pterm));
      else if (pconst < nconst)
        neg.add(productExp(nconst-pconst, pterm));
      nconsts.remove(j);
      nterms.remove(j);
      nstrings.remove(j);
    }
    int nlen = nconsts.size();
    for (int j = 0; j < nlen; j++)
    {    
      int nconst = nconsts.get(j);
      Exp nterm = nterms.get(j);
      neg.add(productExp(nconst, nterm));
    }
  }

  // create a product expresssion const0*term (term may be null)
  private Exp productExp(int const0, Exp term)
  {
    if (const0 == 1 && term != null) return term;
    Exp num = numberExp(const0);
    if (const0 == 0 || term == null) return num;
    List<Exp> args = new ArrayList<Exp>();
    args.add(num); 
    args.add(term);
    return Apply.create(multFun.id, args);
  }
  
  // create a numeric expression
  private Exp numberExp(int num)
  {
    Id id = Id.create(Integer.toString(num));
    FunctionSymbol symbol = new FunctionSymbol(id, new ArrayList<TypeSymbol>(), intSymbol);
    id.setSymbol(symbol);
    return Apply.create(id, new ArrayList<Exp>());
  }
  
  // split expressions into constant factors * terms (terms may be null)
  // with representation "strings" of the terms
  private void splitFactors(List<Exp> exps, 
    List<Integer> consts, List<Exp> terms, List<String> strings)
  {
    for (Exp exp : exps)
    {
      if (!(exp instanceof Apply))
      {
        add(1, exp, consts, terms, strings);
        continue;
      }
      Apply apply = (Apply)exp;
      FunctionSymbol fun = (FunctionSymbol)apply.id.getSymbol();
      if (fun != multFun)
      {
        add(1, exp, consts, terms, strings);
        continue;
      }
      Exp exp1 = apply.exps[0];
      Exp exp2 = apply.exps[1];
      Integer num1 = number(exp1);
      Integer num2 = number(exp2);
      if (num1 != null && num2 != null)
        add(num1*num2, null, consts, terms, strings);
      else if (num1 != null)
        add(num1, exp2, consts, terms, strings);
      else if (num2 != null)
        add(num2, exp1, consts, terms, strings);
      else
        add(1, exp, consts, terms, strings);
    }
  }
  
  // add const0*term (where term may be null) to consts,terms,strings
  private static void add(int const0, Exp term,
    List<Integer> consts, List<Exp> terms, List<String> strings)
  {
    if (const0 == 0) return;
    String string = term == null ? "" : term.toString();
    // do not create larger constants, these may not be axiomatized
    /* 
    int i = strings.indexOf(string);
    if (i != -1)
    {
      int const1 = consts.get(i);
      consts.set(i, const1+const0);
      return;
    }
    */
    consts.add(const0);
    terms.add(term);
    strings.add(string);
  }
  
  // return numeric value of "exp" (null if exp does not denote a number)
  private static Integer number(Exp exp)
  {
    if (!(exp instanceof Apply)) return null;
    Apply apply = (Apply)exp;
    FunctionSymbol fun = (FunctionSymbol)apply.id.getSymbol();
    String name = fun.id.toString();
    if (!AST.isNat(name)) return null;
    try
    {
      return Integer.parseInt(name);
    }
    catch(NumberFormatException e)
    {
      // should not occur
      return null;
    }
  }
}
// ----------------------------------------------------------------------------
// end of file
// ----------------------------------------------------------------------------