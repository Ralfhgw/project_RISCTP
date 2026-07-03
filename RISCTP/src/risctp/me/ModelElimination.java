// ---------------------------------------------------------------------------
// ModelElimination.java
// A model elimination prover.
// $Id: ModelElimination.java,v 1.193 2024/07/12 14:11:12 schreine Exp $
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
package risctp.me;

import java.io.*;
import java.util.*;
import java.util.function.*;
import java.util.concurrent.*;

import risctp.syntax.AST;
import risctp.syntax.AST.*;
import risctp.types.Symbol;
import risctp.types.Symbol.*;
import risctp.fol.*;
import risctp.smt.*;
import risctp.problem.*;

public class ModelElimination
{
  // if true increment proof depth (otherwise increment proof size)
  private static boolean INCREMENT_DEPTH = true;

  // the parameters of a depth-bounded search 
  private static int MAX_DEPTH_BEGIN = 1;        // -1 if not to be used
  private static int MAX_DEPTH_END = 7;          // -1 for no maximum
  private static int MAX_DEPTH_INC = 1;          // 0 for no increment

  // the parameters for a size-bounded search
  private static int MAX_SIZE_BEGIN = -1;       // -1 if not to be used
  private static int MAX_SIZE_END = 70;         // -1 for no maximum
  private static int MAX_SIZE_INC = 1;          // 0 for no increment

  // give essential information to the outside world
  public static boolean getDepth() { return INCREMENT_DEPTH; }
  public static int getLimit() { return INCREMENT_DEPTH ? MAX_DEPTH_END : MAX_SIZE_END; }
  public static boolean getIterate() { return INCREMENT_DEPTH ? 
      MAX_DEPTH_BEGIN != MAX_DEPTH_END : MAX_SIZE_BEGIN != MAX_SIZE_END; }

  // the output medium
  private PrintWriter out;

  // are the proof tree nodes to be printed respectively annotated with text?
  private boolean print;
  private boolean annotate;

  // if not null, methods to be invoked on certain occasions
  private Consumer<ProofTree.Node> newNode;
  private Consumer<ProofTree.Node> updateNode;
  private Consumer<ProofTree.Node> finalizeNode;
  private BooleanSupplier isAborted;

  // set to SMT reasoning level
  private int smtReasoning;

  // the SMT reasoning levels
  public final static int SMT_OFF = 0;
  public final static int SMT_MIN = 1;
  public final static int SMT_MED = 2;
  public final static int SMT_MAX = 3;

  // set to equality reasoning level
  private int equalityReasoning;

  // set to the number of threads to use (0: sequential execution)
  private int threads;

  // set if only a single goal is to be attempted
  private boolean single;
  
  // the equality reasoning levels
  public final static int EQUALITY_OFF = 0;
  public final static int EQUALITY_LOW = 1;
  public final static int EQUALITY_MED = 2;
  public final static int EQUALITY_HIGH = 3;
  public final static int EQUALITY_MAX = 4;

  /****************************************************************************
   * Create an instance of a model elimination prover.
   * @param out its output medium.
   * @param print true if the proof tree is to be printed on construction.
   * @param annotate true if the proof tree is to be annotated with text.
   * @param smt the smt reasoning level.
   * @param eq the equality reasoning level.
   * @param threads the number of threads to use (0: sequential execution).
   * @param single true if only a single goal is to be attempted.
   * @param newNode if not null, method to be invoked on every new proof node.
   * @param updateNode if not null, method to be invoked on every updated proof node.
   * @param finalizeNode if not null, method to be invoked on every finalized node.
   * @param isAborted if not null, method to tell whether computation is aborted.
   ***************************************************************************/
  public ModelElimination(PrintWriter out, boolean print, boolean annotate,
    int smt, int eq, int threads, boolean single,
    Consumer<ProofTree.Node> newNode, 
    Consumer<ProofTree.Node> updateNode,
    Consumer<ProofTree.Node> finalizeNode,
    BooleanSupplier isAborted)
  {
    this.out = out;
    this.print = print;
    this.annotate = annotate;
    this.smtReasoning = smt;
    this.equalityReasoning = eq;
    this.threads = threads;
    this.single = single;
    this.newNode = newNode;
    this.updateNode = updateNode;
    this.finalizeNode = finalizeNode;
    this.isAborted = isAborted;
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

  // the timeout value (0 for no timeout)
  private static long TIMEOUT = 60000;

  // true if timeout is to be ignored
  private boolean ignoreTimeout = false;

  // the exception that is thrown if proof search is prematurely aborted
  public static class AbortedException extends RuntimeException 
  { 
    // make Java happy
    public static final long serialVersionUID = 20230417; 

    // computation may be retried
    public final boolean retry;

    public AbortedException(String message, boolean retry) 
    { 
      super(message); 
      this.retry = retry;
    }
  }
  
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
   * Set the maximum proof depth.
   * @param begin the iteration start value (-1 if not to be used)
   * @param end the iteration end value (-1 if none)
   * @param inc the iteration increment value (0 if none)
   ***************************************************************************/
  public static void setDepth(int begin, int end, int inc)
  {
    INCREMENT_DEPTH = true;
    MAX_SIZE_BEGIN = -1;
    MAX_DEPTH_BEGIN = begin;
    MAX_DEPTH_END = end;
    MAX_DEPTH_INC = inc;
  }

  /****************************************************************************
   * Set the maximum proof size.
   * @param begin the iteration start value (-1 if not to be used)
   * @param end the iteration end value (-1 if none)
   * @param inc the iteration increment value (0 if none)
   ***************************************************************************/
  public static void setSize(int begin, int end, int inc)
  {
    INCREMENT_DEPTH = false;
    MAX_DEPTH_BEGIN = -1;
    MAX_SIZE_BEGIN = begin;
    MAX_SIZE_END = end;
    MAX_SIZE_INC = inc;
  }

  /****************************************************************************
   * Get the prover timeout value.
   * @return the number of milliseconds after which proof is aborted 
   *         (0 if no timeout)
   ***************************************************************************/
  public static long getTimeout()
  {
    return TIMEOUT;
  }

  // set if proof search tree is to be generated
  private static boolean generateProofSearch = false;

  // set if proof tree is to be generated
  private static boolean generateProof = false;

  /****************************************************************************
   * Indicate whether proof search tree is to be generated.
   * @param doit true if proof search tree is to be generated.
   ***************************************************************************/
  public static void generateProofSearch(boolean doit) { generateProofSearch = doit; }

  /****************************************************************************
   * Indicate whether proof tree is to be generated.
   * @param doit true if proof tree is to be generated.
   ***************************************************************************/
  public static void generateProof(boolean doit) { generateProof = doit; }

  // the executor to use for parallel execution
  private ExecutorService executor;
  
  // the number of clause applications
  private int totalClauseApplications;

  // the current clause problem to be handled and the associated proof problem
  private ClauseProblem problem;
  private ProofProblem proofProblem;

  // integer predicates used in case splits
  private FunctionSymbol lessPred;
  // private FunctionSymbol lessEqualPred;

  /****************************************************************************
   * Solve a problem in clausal form.
   * @param problem the problem.
   * @return true if the solution succeeded.
   **************************************************************************/
  public boolean solve(ClauseProblem problem)
  {
    if (smtReasoning == SMT_OFF)
      out.println("=== proof method 'meson': model elimination, subgoal-oriented");
    else
    {
      out.println("=== proof method 'meson': model elimination, subgoal-oriented (with SMT support)");
      out.println("SMT solver: " + Solver.version());
    }
    if (threads >= 1 && !single)
    {
      out.print("Multithreading with " + threads + " threads is enabled, ");
      out.println("goal selection option 'Single Goal' is not selected.");
      out.println("Therefore in every subproblem all goals are attempted in parallel.");
    }

    // process proof problem
    this.problem = problem;
    this.proofProblem = problem.getProofProblem();
    for (FunctionSymbol fun : proofProblem.intFuns)
    {
      String name = fun.id.toString();
      switch (name) { case AST.LESSNAME: lessPred = fun; }
      // switch (name) { case AST.LESSEQUALNAME: lessEqualPred = fun; }
    }

    // the executor to use for parallel execution
    executor = threads < 2 ? null : Executors.newFixedThreadPool(threads);
    
    // record the total number of all clause applications for statistics
    totalClauseApplications = 0;
 
    // start the proof which may be prematurely aborted by the watchdog
    boolean okay = false;
    long time = System.currentTimeMillis();
    try
    {
      // previous execution may have been aborted by user
      checkAbort(true);
      okay = proveGoals(problem);
    }
    catch (AbortedException e)
    {
      out.print("The proof of the goal is aborted because ");
      out.println(e.getMessage() + ".");
    }
    catch (Exception e)
    {
      StringWriter swriter = new StringWriter();
      e.printStackTrace(new PrintWriter(swriter));
      String message = swriter.toString();
      System.out.println("The proof is aborted due to the following exception:");
      System.out.println(message);
      out.print("The proof is aborted due to the following exception:");
      out.println(message);
    }
    catch (StackOverflowError e)
    {
      out.println("The proof is aborted because recursion gets too deep.");
    }
    catch (OutOfMemoryError e)
    {
      System.out.println("The program is aborted due to lack of heap space.");
      System.exit(-1);
    }

    // shutdown the executor
    if (executor != null)
    {
      executor.shutdown();
      executor = null;
    }
    
    // prover is terminated
    time = System.currentTimeMillis()-time;
    if (okay)
      out.print("SUCCESS: the proof problem has been solved");
    else
      out.print("FAILURE: the proof problem has NOT been solved");
    if (totalClauseApplications < 0)
      out.println(" (" + time + " ms).");
    else
      out.println(" (" + totalClauseApplications + " clause applications, " + time + " ms).");
    out.println("===");
    return okay;
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

  /****************************************************************************
   * Prune the generated proof tree by removing superfluous branches of all 
   * OR nodes such that it only contains the successful proof (if any)
   ***************************************************************************/
  public void pruneProof()
  {
    if (proofTree == null || !generateProof) return;
    proofTree.prune();
  }

  /****************************************************************************
   * Remove the proof tree from memory (may be huge).
   ***************************************************************************/
  public void clearProof() { proofTree = null; }

  // the created proof tree (null if none)
  private ProofTree proofTree;
  public ProofTree getProofTree() { return proofTree; }

  // the sequence of all rules (earlier ones have higher priority)
  private Rule[] rules;

  // the sequence of goal rules (earlier ones are attempted first)
  private Rule[] goals;

  // the sequence of axiom rules
  private Rule[] axioms;

  // the (variable-free) rules submitted to the SMT solver
  private List<Rule> smtRules;

  // a rule and the position of a literal in the rule
  private static class RulePos
  {
    public final Rule rule;
    public final int pos;
    public final int open;
    public RulePos(Rule rule, int pos) 
    { 
      this.rule = rule; 
      this.pos = pos; 
      this.open = openVariableNumber(rule, pos);
    }      
  }

  // mappings of predicate symbols to rules together with the
  // positions where literals with these symbols occur in the rules
  // (positively or negatively)
  private Map<FunctionSymbol, List<RulePos>> pos;
  private Map<FunctionSymbol, List<RulePos>> neg;

  // an equality rule and the position of a function application in the rule
  private static class EqRulePos extends RulePos
  {
    // true if function symbol occurs on lhs of equality (rhs, else)
    public final boolean left; 
    public EqRulePos(Rule rule, int pos, boolean left) 
    { 
      super(rule, pos);
      this.left = left;
    }      
  }

  // mapping of a function symbol to equality positions
  private Map<FunctionSymbol, List<EqRulePos>> epositions;

  // mapping of a type symbol to positions of equalities "var = var"
  private Map<TypeSymbol, List<EqRulePos>> evarpositions;

  // the equality map and all the equality symbols
  private Map<TypeSymbol, FunctionSymbol> emap;
  private Collection<FunctionSymbol> esymbols;

  /****************************************************************************
   * Initialize the prover state from the given problem.
   * @param problem the problem.
   ***************************************************************************/
  private void init(ClauseProblem problem)
  {
    // the clauses of the problem
    List<Clause> clauses = problem.getClauses();
    int n = clauses.size();

    // the proof problem
    rules = new Rule[n];
    pos = new HashMap<FunctionSymbol, List<RulePos>>();
    neg = new HashMap<FunctionSymbol, List<RulePos>>();
    epositions = new HashMap<FunctionSymbol, List<EqRulePos>>();
    evarpositions = new HashMap<TypeSymbol, List<EqRulePos>>();
    emap = problem.getProofProblem().equalities;
    esymbols = new HashSet<FunctionSymbol>(emap.values());

    // the proof tree
    if (generateProofSearch || generateProof)
    {
      proofTree = new ProofTree(problem.getProofProblem().getName(),
          out, generateProofSearch, print, annotate,
          newNode, updateNode, finalizeNode);
    }
    else
      proofTree = null;

    // generate rules, goals, and rule instances
    List<Rule> goals0 = new ArrayList<Rule>();
    List<Rule> axioms0 = new ArrayList<Rule>();
    for (int i = 0; i < n; i++)
    {
      // we invert the ordering of rules because later clauses are deemed 
      // higher-level and thus should be attempted first
      Clause clause = clauses.get(n-i-1);
      Rule rule = new Rule(clause);
      rule.setImplied(clause.isImplied());
      rule.setTheory(clause.isTheory());
      rules[i] = rule;
      if (clause.isGoal()) 
        goals0.add(rule);
      else
        axioms0.add(rule);
    }
    goals = goals0.toArray(new Rule[goals0.size()]);
    axioms = axioms0.toArray(new Rule[axioms0.size()]);

    // register the SMT rules (in the original order)
    smtRules = new ArrayList<Rule>();
    for (Rule rule : rules)
    {
      // SMT rules are the variable-free rules that do not-characterize a theory 
      if (rule.vars.length != 0) continue;
      if (rule.isTheory()) continue;
      smtRules.add(rule);
    }
    Collections.reverse(smtRules);

    // register rules for fast lookup from predicate/function symbols
    registerRules(rules, problem);
  }
  
  /****************************************************************************
   * Register rules for fast lookup from predicate symbols and
   * function symbols (for equality reasoning), respectively.
   * @param rules the rules.
   * @param problem the clause problem from which the rules stem.
   ***************************************************************************/
  private void registerRules(Rule[] rules, ClauseProblem problem)
  {
    // create a lexicographic path ordering of terms according to the
    // order in which the function symbols are introduced in the problem
    TermLPO order = new TermLPO();
    ProofProblem problem0 = problem.getProofProblem();
    for (Decl decl : problem0.decls)
    {
      if (!(decl instanceof Decl.Function)) continue;
      Decl.Function decl0 = (Decl.Function)decl;
      FunctionSymbol fun = (FunctionSymbol)(decl0.id.getSymbol());
      order.defineOrder(fun);
      if (problem0.selectors.contains(fun)) order.makeSelector(fun);
    }

    // register the rules for fast lookup from predicate symbols
    // and function symbols (for equality reasoning), respectively
    for (Rule rule : rules)
    {
      Object[] lits = rule.lits;
      boolean[] negs = rule.negs;
      boolean[] notGoals = rule.notGoals;
      int litn = lits.length;
      for (int i = 0; i < litn; i++)
      {
        Object lit = lits[i];
        FunctionSymbol pred = (FunctionSymbol)Term.symbol(lit);

        // for literal p(...) register mapping for p in neg/pos
        // unless the literal is a "non-goal"
        if (!notGoals[i] || (equalityReasoning >= EQUALITY_MED && esymbols.contains(pred)))
        {
          Map<FunctionSymbol,List<RulePos>> map = negs[i] ? neg : pos;
          List<RulePos> rules0 = map.computeIfAbsent(pred, (p)->new ArrayList<RulePos>());
          rules0.add(new RulePos(rule, i));
        }

        // we may consider equalities in a more special way
        if (equalityReasoning == EQUALITY_OFF) continue;

        // we are only interested in positive equalities
        if (negs[i]) continue;

        // for equality f(...)=g(...) register mappings for f and g
        // if a side is a variable of type T, register mapping for T instead
        if (!esymbols.contains(pred)) continue;
        Object arg1 = Term.argument(lit, 0);
        Object arg2 = Term.argument(lit, 1);
        Symbol sym1 = Term.symbol(arg1);
        Symbol sym2 = Term.symbol(arg2);
        if (sym1 instanceof FunctionSymbol)
        {
          // avoid reduction from less term to bigger one
          if (equalityReasoning == EQUALITY_MAX || !order.less(arg1, arg2))
          {
            FunctionSymbol fsym1 = (FunctionSymbol)sym1;
            List<EqRulePos> erules1 = epositions.computeIfAbsent(fsym1, 
                (f)->new ArrayList<EqRulePos>());
            erules1.add(new EqRulePos(rule, i, true));
          }
        }
        else if (equalityReasoning >= EQUALITY_MED)
        {
          // rewrite variable but
          // avoid reduction from smaller term to bigger one
          if (equalityReasoning == EQUALITY_MAX || !order.less(arg1, arg2))
          {
            VariableSymbol vsym = (VariableSymbol)sym1;
            TypeSymbol tsym = vsym.tsymbol.root;
            List<EqRulePos> erules = evarpositions.computeIfAbsent(tsym, 
                (t)->new ArrayList<EqRulePos>());
            erules.add(new EqRulePos(rule, i, true));
          }
        }
        if (sym2 instanceof FunctionSymbol)
        {
          // avoid reduction from smaller term to bigger one
          if (equalityReasoning == EQUALITY_MAX || !order.less(arg2, arg1))
          {
            FunctionSymbol fsym2 = (FunctionSymbol)sym2;
            List<EqRulePos> erules2 = epositions.computeIfAbsent(fsym2, 
                (f)->new ArrayList<EqRulePos>());
            erules2.add(new EqRulePos(rule, i, false));
          }
        }
        else if (equalityReasoning >= EQUALITY_MED)
        {
          // rewrite variable but 
          // avoid reduction from smaller term to bigger one
          if (equalityReasoning == EQUALITY_MAX || !order.less(arg2, arg1))
          {
            VariableSymbol vsym = (VariableSymbol)sym2;
            TypeSymbol tsym = vsym.tsymbol.root;
            List<EqRulePos> erules = evarpositions.computeIfAbsent(tsym, 
                (t)->new ArrayList<EqRulePos>());
            erules.add(new EqRulePos(rule, i, false));
          }
        }
      }
    }

    // order rule positions according to likelihood of successful application
    for (List<RulePos> positions : pos.values()) sortPositions(positions);
    for (List<RulePos> positions : neg.values()) sortPositions(positions);
    for (List<EqRulePos> positions : epositions.values()) sortEqPositions(positions);
    for (List<EqRulePos> positions : evarpositions.values()) sortEqPositions(positions);
  }

  /****************************************************************************
   * Get the number of "open" variables of a rule with respect to a position, 
   * i.e., the number of the free variables of the rule that do not occur in
   * the literal at the position. 
   * @param rule a rule.
   * @param pos a position in the rule.
   * @return the number of open variables.
   ***************************************************************************/
  private static int openVariableNumber(Rule rule, int pos)
  {
    VariableSymbol[] vars = rule.vars;
    Set<VariableSymbol> vars0 = new HashSet<VariableSymbol>();
    Term.vars(rule.lits[pos], vars0);
    return vars.length-vars0.size();
  }

  /****************************************************************************
   * Order rule positions such that rules with lower number of freely chosen
   * variables occur earlier (and thus are attempted earlier).
   * @param positions the list of rule positions to be sorted.
   ***************************************************************************/
  private static void sortPositions(List<RulePos> positions)
  {
    positions.sort((pos1, pos2)->
    {
      int n1 = pos1.open;
      int n2 = pos2.open;
      return n1 < n2 ? -1 : n1 > n2 ? +1 : 0;
    });
  }

  /****************************************************************************
   * Order equality rule positions such that rules with lower number of 
   * freely chosen variables occur earlier (and thus are attempted earlier).
   * @param positions the list of rule positions to be sorted.
   ***************************************************************************/
  private static void sortEqPositions(List<EqRulePos> positions)
  {
    positions.sort((pos1, pos2)->
    {
      int n1 = pos1.open;
      int n2 = pos2.open;
      return n1 < n2 ? -1 : n1 > n2 ? +1 : 0;
    });
  }

  // --------------------------------------------------------------------------
  //
  // auxiliaries of the proof procedure
  //
  // --------------------------------------------------------------------------

  /****************************************************************************
   * Determine whether two terms are equal except for a single position.
   * @param term1 the first term to be compared.
   * @param term2 the second term to be compared.
   * @return an array of length two with two subterms that occur at
   * the same position of term1 and term2, respectively, such that term1 and
   * term2 are equal if the subterms are equal (the result is null, if 
   * such subterms do not exist).
   ***************************************************************************/
  private Object[] equalExceptSubterm(Object term1, Object term2)
  {
    Object[] result = new Object[] { null, null };
    equalExceptSubterm(term1, term2, result);
    if (result[0] == null) return null;
    return result;
  }

  /****************************************************************************
   * Auxiliary procedure for equalExceptSubterm(term1, term2)
   * @param term1 the first term to be compared.
   * @param term2 the second term to be compared.
   * @param result an array of length 2 whose elements are either both null
   * (then no suitable difference pair could be found yet) or both not null 
   * (then the array contains such a pair). If the procedure now encounters 
   * another difference, then:
   * - if the array contents are not null, the array contents are set 
   *   to the differing terms
   * - otherwise, the first element is set to null and the second one
   *   to a non-null value (which indicates that another difference was found)
   ***************************************************************************/
  private void equalExceptSubterm(Object term1, Object term2, Object[] result)
  {
    Symbol symbol1 = Term.symbol(term1);
    Symbol symbol2 = Term.symbol(term2);
    if (symbol1 != symbol2) 
    {
      if (result[0] != null) { result[0] = null; return; }
      result[0] = term1;
      result[1] = term2;
      return;
    }
    if (symbol1 instanceof VariableSymbol) return;
    int n = Term.argnumber(term1);
    for (int i = 0; i < n; i++)
    {
      Object arg1 = Term.argument(term1, i);
      Object arg2 = Term.argument(term2, i);
      equalExceptSubterm(arg1, arg2, result);
      if (result[0] == null && result[1] != null) return;
    }
  }

  // --------------------------------------------------------------------------
  //
  // the proof procedure
  //
  // --------------------------------------------------------------------------

  /****************************************************************************
   * Prove some goal of the problem.
   * @param problem the problem to be proved.
   * @return true if the proof succeeded.
   ***************************************************************************/
  private boolean proveGoals(ClauseProblem problem)
  {
    // initialize the prover state
    init(problem);

    // the root of the proof tree
    ProofTree.Node orNode = null;
    if (proofTree != null)
    {
      orNode = proofTree.createRoot(rules, axioms, goals, smtReasoning);
      proofTree.push(orNode);
    }
    
    // try to solve proof problem by SMT solving
    if (smtReasoning == SMT_MAX)
    {
      // pick no goal but show unsatisfiability of clauses
      ProofTask task = new ProofTask(out, proofTree, null);
      task.call();
      if (task.proved)
      {
        if (proofTree != null)
        {
          orNode.finalize(true, null);
        }
        return true;
      }
      AbortedException e = task.abortedException();
      if (e != null)
      {
        if (!e.retry) throw e;
        String message = "The proof is aborted because " + e.getMessage() + ".";
        out.println(message);
      }
    }

    // solve multiple goals in parallel
    if (executor != null && goals.length > 1 && !single) 
      return proveGoalsParallel(orNode, problem);

    // Assuming that the conjunction of non-goal clauses is satisfiable,
    // it suffices to attempt the proof from a goal clause.
    boolean proved = false;
    int n = goals.length;
    for (int i = 0; i < n; i++)
    {     
      Rule goal = goals[i];
      if (goal.isImplied())
      {
        out.println("Clause " + (n-i) + 
            " is implied and thus not considered as a proof goal.");
        continue;
      }

      // perform a proof task
      // proof aborted by timeout may be attempted with another goal
      ProofTree p = proofTree;
      out.print("Goal " + (n-i) + ":[" + goal.name + "] ");
      goal.print(out, true, true,
          (Object litx, Boolean negx)-> true,
          (Object litx, Boolean negx)->
          {
            out.print(Term.toString(litx));
          });
      out.println();
      ProofTask task = new ProofTask(out, proofTree, goal);
      task.call();
      proved = task.proved;
      totalClauseApplications += task.clauseApplications();
      AbortedException e = task.abortedException();
      if (e != null)
      {
        if (!e.retry) throw e;
        String message = "The proof is aborted because " + e.getMessage() + ".";
        out.println(message);
        proofTree = p;
        if (proofTree != null) proofTree.pop(orNode);
      }
      
      // proof was successful or only a single goal is attempted
      if (proved || single) break;
    }
 
    // try to solve proof problem by SMT solving
    if (!proved && SMT_MIN < smtReasoning && smtReasoning < SMT_MAX)
    {
      // pick no goal but show unsatisfiability of clauses
      ProofTask task = new ProofTask(out, proofTree, null);
      task.call();
      proved = task.proved;
      if (proved) totalClauseApplications = -1;
    }
    
    // indicate the success of the proof
    if (proofTree != null)
    {
      orNode.finalize(proved, null);
      proofTree.pop();
    }
    return proved;
  }

  /****************************************************************************
   * Prove some goal of the problem, attempting all goals in parallel.
   * @param orNode the node into which to link the proof tree.
   * @param problem the problem to be proved.
   * @return true if the proof succeeded
   ***************************************************************************/
  private boolean proveGoalsParallel(ProofTree.Node orNode, ClauseProblem problem)
  {
    // we assume that init(problem) has been already executed
    int n = goals.length;
    int implied = 0;
    for (int i = 0; i < n; i++)
      if (goals[i].isImplied()) implied++;
    if (implied == 0)
      out.println("We try to prove any of the " + n + " goals by " 
          + threads + " parallel threads...");
    else
      out.println("We try to prove any of the " + (n-implied) + 
          " goals arising from non-implied clauses by " 
          + threads + " parallel threads...");

    // attempt all goal tasks in parallel (without generating a proof)
    List<ProofTask> tasks = new ArrayList<ProofTask>();
    List<Future<ProofTask>> futures = new ArrayList<Future<ProofTask>>();
    for (Rule goal : goals)
    {     
      if (goal.isImplied()) continue;
      ProofTask task = new ProofTask(null, null, goal);
      tasks.add(task);
      futures.add(executor.submit(task));
    }
   
    // wait for terminated tasks and print their outputs
    // until one task is successful
    ProofTask result = null;
    while (!futures.isEmpty())
    {
      try { Thread.sleep(10); } catch (InterruptedException e) { }
      List<Future<ProofTask>> futures0 = new ArrayList<Future<ProofTask>>();
      for (Future<ProofTask> future : futures)
      {
        if (!future.isDone())
        {
          futures0.add(future);
          continue;
        }
        try
        {
          ProofTask task = future.get();
          totalClauseApplications += task.clauseApplications();
          printTask(task);
          if (result == null && task.proved)
            result = task;
        }
        catch(ExecutionException e) { }
        catch(InterruptedException e) { }
      }
      futures = futures0;
      if (result != null) break;
    }

    // stop all still running tasks (not the result, needs to be resumed)
    for (ProofTask task : tasks)
    {
      if (task == result) continue;
      task.stop();
    }

    // wait for termination of all these tasks and print their output
    for (Future<ProofTask> future : futures)
    {
      try 
      { 
        ProofTask task = future.get();
        totalClauseApplications += task.clauseApplications();
        printTask(task);
      } 
      catch(ExecutionException e) { }
      catch(InterruptedException e) { }
    }

    boolean proved = result != null && result.proved;
    if (!proved)
    {
      out.println("No goal was proved.");
    }
    else
    {
      out.println("The following goal was proved:");
      for (int i = 0; i < n; i++)
      {
        Rule goal = goals[i];
        if (goal == result.goal)
        {
          out.print("Goal " + (n-i) + ":[" + goal.name + "] ");
          goal.print(out, true, true,
              (Object litx, Boolean negx)-> true,
              (Object litx, Boolean negx)->
              {
                out.print(Term.toString(litx));
              });
          out.println();
          break;
        }
      }
    }
    
    // record clause applications and generate the proof
    if (proved)
    {
      result.setOutput(out);
      result.setProofTree(proofTree);
      result.produceProof();
    }
    
    // try to solve proof problem by SMT solving
    else if (SMT_MIN < smtReasoning && smtReasoning < SMT_MAX)
    {
      // pick no goal but show unsatisfiability of clauses
      ProofTask task = new ProofTask(out, proofTree, null);
      task.call();
      proved = task.proved;
    }
    
    // indicate the success of the proof
    if (proofTree != null)
    {
      orNode.finalize(proved, null);
      proofTree.pop();
    }
    return proved;
  }

  /****************************************************************************
   * Print goal with its number.
   * @param goal the goal to be printed.
   ***************************************************************************/
  private void printGoal(Rule goal)
  {
    int n = goals.length;
    for (int i = 0; i < n; i++)
    {
      if (goal == goals[i]) 
      {
        out.print("Goal " + (n-i) + ":[" + goal.name + "] ");
        goal.print(out, true, true,
            (Object litx, Boolean negx)-> true,
            (Object litx, Boolean negx)->
            {
              out.print(Term.toString(litx));
            });
        out.println();
        return;
      }
    }
  }
  
  /****************************************************************************
   * Print task with its output.
   * @param task the task to be printed.
   ***************************************************************************/
  private void printTask(ProofTask task)
  {
    printGoal(task.goal);
    String output = task.getOutput();
    out.print(output);
    if (!output.endsWith("\n")) out.println();
    AbortedException e = task.abortedException();
    if (e != null)
    {
      if (!e.retry) { out.println(); throw e; }
      String message = "The proof is aborted because " + e.getMessage() + ".";
      out.println(message);
    }
  }
  
  // a proof complexity measure
  public static class Measure 
  { 
    // the stack of measure values (current one on top)
    public Stack<Int> sizeStack = new Stack<Int>();

    // an integer that can be updated in place
    public static class Int 
    { 
      public int value; 
      Int(int value)  { this.value = value; } 
    }
  }

  // a literal pushed on the assumptions stack
  public static class Literal
  {
    public final Object term;
    public final boolean neg;
    public Literal(Object term, boolean neg) { this.term = term; this.neg = neg; }
  }

  // an equality
  public static class Equality
  {
    public Object told; // the replaced term
    public Object tnew; // the replacing term
    public Equality(Object told, Object tnew) { this.told = told; this.tnew = tnew; }
  }

  // the current proof context
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

  // --------------------------------------------------------------------------
  //
  // the proof task
  //
  // --------------------------------------------------------------------------

  // the task to prove a particular goal
  private class ProofTask implements Callable<ProofTask>
  {
    // true if the proof was completed
    private boolean proved;
    
    // the goal to be proved (null, if no goal has been picked)
    private Rule goal;

    // the primary solver and the secondary one used for 
    // deriving the minimal unsatisfiable core (may be both null)
    private Solver solver;
    private Solver solver2;

    // current sequence of literals in the context of the SMT solver and the
    // corresponding sequence of free variable sets introduced by every literal
    private List<Literal> solverLits;
    private List<Set<VariableSymbol>> solverVars;

    // the current proof complexity measure
    public final Measure measure = new Measure();

    // the number of applications of clauses in the proof of this goal
    private int clauseApplications;
    public int clauseApplications() { return clauseApplications; }
    
    // a counter for generating fresh variable names
    private int varCounter;
    
    // a map of every rule to a not yet used instance of this rule
    private Map<Rule,Rule> instances;

    // the start time of the current execution
    private Long startTime;

    // the watchdog guarding the execution
    private Thread watchdog;

    // the exception created by the watchdog
    private AbortedException abortedException;
    public AbortedException abortedException() { return abortedException; }
    
    // the output stream for this task
    private PrintWriter out;
    
    // the underlying string writer (may be null)
    private StringWriter swriter;

    // local copy of the proof tree
    private ProofTree ptree;
    
    // update output stream and local proof tree copy
    public void setOutput(PrintWriter out)
    {
      this.out = out;
      this.swriter = null;
    }
    public void setProofTree(ProofTree ptree) { this.ptree = ptree; }
    
    /**************************************************************************
     * Create a task for solving a proof problem.
     * @param goal the goal to be proved (null, if no goal has been picked).
     *************************************************************************/
    private ProofTask(Rule goal)
    {
      this.proved = false;
      this.goal = goal;
      this.solver = null;
      this.solver2 = null;
      this.solverLits = null;
      this.solverVars = null;
      this.clauseApplications = 0;
      this.varCounter = 0;
      this.instances = new HashMap<Rule, Rule>();
      for (Rule rule : rules) newInstance(rule);
      startTime = null;
      watchdog = null;
      abortedException = null;
    }

    /**************************************************************************
     * Create a task for proving a goal.
     * @param out the output stream (null, if output is to be locally captured).
     * @param goal the goal to be proved (null, if no goal has been picked).
     *************************************************************************/
    public ProofTask(PrintWriter out, ProofTree ptree, Rule goal)
    {
      this(goal);
      if (out == null)
      {
        this.swriter = new StringWriter();
        out = new PrintWriter(swriter, true);
      }
      this.out = out;
      this.ptree = ptree;
    }
    
    /**************************************************************************
     * Stop the task.
     *************************************************************************/
    public void stop()
    {
      if (abortedException != null) return;
      abortedException = new AbortedException("the task is stopped", true);
    }
    
    /**************************************************************************
     * Get the output captured in the task.
     * @return the output (empty string if output was not locally captured).
     *************************************************************************/
    public String getOutput() 
    { 
      if (swriter == null) return "";
      return swriter.toString(); 
    }
    
    /**************************************************************************
     * Attempts to perform the proof.
     * @return this.
     *************************************************************************/
    public ProofTask call()
    {
      startSMT();
      startWatchDog();
      startTime = System.currentTimeMillis();
      try 
      { 
        if (goal == null)
        {
          proved = smtTheorem();
        }
        else
        {
          Subst subst = proveGoal(); 
          proved = subst != null;
        }
      } 
      catch (AbortedException e) 
      { 
        out.println(clauseApplications + " clause applications.");
      }
      startTime = null;
      watchdog = null;
      stopSMT();
      return this;
    }
    
    /**************************************************************************
     * Produce the proof tree for a previously generated proof.
     * @return this.
     *************************************************************************/
    public ProofTask produceProof()
    {
      // watchdog needed to react to user "abort"
      startTime = null;
      startSMT();
      startWatchDog();
      try { proveGoalWithProof(goal, -1); } catch (AbortedException e) { }
      startTime = null;
      watchdog = null;
      stopSMT();
      return this;
    }

    /**************************************************************************
     * Start a watchdog thread to stop the computation when timeout is reached.
     *************************************************************************/
    private void startWatchDog()
    {
      if (isAborted == null && TIMEOUT == 0) return;
      watchdog = new Thread(new Runnable() { 
        public void run() 
        { 
          Runtime runtime = Runtime.getRuntime();
          while (true)
          {
            try { Thread.sleep(100); } catch(InterruptedException e) { }
            if (watchdog == null) return;
            long time = System.currentTimeMillis();
            boolean aborted = isAborted != null && isAborted.getAsBoolean();
            if (aborted) abortedException = new AbortedException("the user has requested an abort", false);
            boolean timeout = TIMEOUT != 0 && !ignoreTimeout && startTime != null && time-startTime > TIMEOUT;
            if (timeout) abortedException = new AbortedException("of timeout after " + TIMEOUT + " ms", true);
            boolean lowmem = 25000000 > runtime.maxMemory()-runtime.totalMemory()+runtime.freeMemory();
            if (lowmem) abortedException = new AbortedException("the amount of free heap memory is low", false);
            if (abortedException != null) return;
          }
        }});
      watchdog.start();
    }
    
    /**************************************************************************
     * Initialize SMT solvers.
     *************************************************************************/
    private void startSMT()
    {
      if (smtReasoning == SMT_OFF) return;
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
      // immediately assert the SMT rules to the solver
      for (Rule rule : smtRules)
      {
        solver.assume(rule.lits, rule.negs);
      }
      solverLits = new ArrayList<Literal>();
      solverVars = new ArrayList<Set<VariableSymbol>>();
    }
    
    /**************************************************************************
     * Stop SMT solvers.
     *************************************************************************/
    private void stopSMT()
    {
      if (solver != null) { stopSolver(solver); solver = null; }
      if (solver2 != null) { stopSolver(solver2); solver2 = null; }
    }

    // the values of a bounded search
    private int maxDepth; // the current depth bound
    private int maxSize;  // the current size bound 

    // the current iteration number
    private int iterationNumber = 0;

    /****************************************************************************
     * Prove goal with increasing proof bounds ("iterative deepening").
     * @return the resulting substitution (null if the proof did not succeed)
     ***************************************************************************/
    private Subst proveGoal()
    {
      // the node of the proof tree for this goal (and any bound)
      ProofTree.Node andNode = null;
      ProofTree.Node orNode = null;
      if (ptree != null)
      {
        andNode = ptree.createNodeGoal(goal);
        ptree.push(andNode);
        orNode = ptree.createNodeBounds(goal, INCREMENT_DEPTH, 
            MAX_DEPTH_BEGIN, MAX_DEPTH_END, MAX_DEPTH_INC,
            MAX_SIZE_BEGIN, MAX_SIZE_END, MAX_SIZE_INC);
        ptree.push(orNode);
      }

      // initial values of proof bounds (-1, if none)
      maxDepth = MAX_DEPTH_BEGIN;
      maxSize = MAX_SIZE_BEGIN;

      // iterative deepening of either proof depths or proof sizes
      Subst subst0 = null;
      if (INCREMENT_DEPTH)
      {
        iterationNumber = 1;
        while (true)
        {
          out.print("Iteration " + iterationNumber + " (proof depth " + maxDepth + ")... "); 
          out.flush(); 
          subst0 = proveGoalCore(goal); 
          if (subst0 != null) break;
          if (maxDepth == -1 || MAX_DEPTH_INC <= 0) break;
          maxDepth = maxDepth+MAX_DEPTH_INC;
          if (MAX_DEPTH_END != -1 && maxDepth > MAX_DEPTH_END) break;
          iterationNumber++;
        }
      }
      else
      {
        iterationNumber = 1;
        while (true)
        {
          out.print("Iteration " + iterationNumber + " (proof size " + maxSize + ")... "); 
          out.flush(); 
          subst0 = proveGoalCore(goal); 
          if (maxSize == -1 || MAX_SIZE_INC <= 0) break;
          maxSize = maxSize+MAX_SIZE_INC;
          if (MAX_SIZE_END != -1 && maxSize > MAX_SIZE_END) break;
          iterationNumber++;
        }
      }

      // indicate success of proof
      if (ptree != null)
      {
        orNode.finalize(subst0 != null, null);
        ptree.pop();
        andNode.finalize(subst0 != null, subst0);
        ptree.pop();
      }
      return subst0;
    }

    // --------------------------------------------------------------------------
    //
    // the core of the proof procedure
    //
    // --------------------------------------------------------------------------

    /****************************************************************************
     * Prove a goal.
     * @param goal the goal.
     * @return the resulting substitution (null if proof failed)
     ***************************************************************************/
    private Subst proveGoalCore(Rule goal)
    {
      long time = System.currentTimeMillis();
      Subst subst = proveGoalWithoutProof(goal);
      time = System.currentTimeMillis()-time;
      if (subst == null) return subst;
      proveGoalWithProof(goal, time);
      return subst;
    }
    private Subst proveGoalWithoutProof(Rule goal)
    {
      // to speed up the computation, do not generate the proof tree at first
      ProofTree p = ptree;
      if (!generateProofSearch) ptree = null;
      if (ptree != null) ptree.setMeasure(measure);
      measure.sizeStack.push(new Measure.Int(0));
      clauseApplications = 0;
      Subst subst = proveLiterals(new Context(), goal.name, goal.lits, goal.negs, new Box<Boolean>(false), (Subst s)->s);
      out.println(clauseApplications + " clause applications.");
      measure.sizeStack.pop();
      ptree = p;
      return subst;
    }
    private void proveGoalWithProof(Rule goal, long time)
    {
      // only if proof was successful, redo it to generate the proof tree
      if (generateProofSearch || ptree == null) return;
      if (time >= 0)
        out.println("The proof succeded in " + time + " ms, now run it again to generate the proof tree...");
      else
        out.println("The proof succeeded, now run it again to generate the proof tree...");
      ptree.setMeasure(measure);
      measure.sizeStack.push(new Measure.Int(0));
      ignoreTimeout = true;
      clauseApplications = 0;
      proveLiterals(new Context(), goal.name, goal.lits, goal.negs, new Box<Boolean>(false), (Subst s)->s);
      ignoreTimeout = false;
      measure.sizeStack.pop();
    }

    /****************************************************************************
     * Prove a sequence of literals.
     * @param context the current proof context.
     * @param name the name of the proof node.
     * @param lits the sequence of literals.
     * @param negs the corresponding sequence of their polarities.
     * @param done set to true if all literals could be proved. 
     * @param cont the continuation to be applied to the resulting substitution
     * @return the resulting substitution (null if the proof did not succeed)
     ***************************************************************************/
    public Subst proveLiterals(Context context, 
      String name, Object[] lits, boolean[] negs, 
      Box<Boolean> done, Function<Subst,Subst> cont)
    {
      // remove from literals equations "var=term" and update substitution
      Set<Integer> eqs = new LinkedHashSet<Integer>();
      Subst subst = context.subst;
      int n = lits.length;
      for (int i = 0; i < n; i++)
      {
        Object lit = lits[i];
        boolean neg = negs[i];
        FunctionSymbol pred = (FunctionSymbol)Term.symbol(lit);
        if (!neg || !proofProblem.equalities.values().contains(pred)) continue;
        Object arg1 = Term.argument(lit, 0);
        Object arg2 = Term.argument(lit, 1);
        Symbol sym1 = Term.symbol(arg1);
        Symbol sym2 = Term.symbol(arg2);
        boolean isvar1 = sym1 instanceof VariableSymbol;
        boolean isvar2 = sym2 instanceof VariableSymbol;
        if (!isvar1 && !isvar2) continue;
        Subst subst0 = subst.unify(arg1, arg2);
        if (subst0 == null) continue;
        subst = subst0;
        eqs.add(i);
      }
      Object[] lits0 = null;
      boolean[] negs0 = null; 
      int en = eqs.size();
      if (en != 0)
      {
        int n0 = n-en;
        lits0 = new Object[n0];
        negs0 = new boolean[n0];
        int i0 = 0;
        for (int i = 0; i < n; i++)
        {
          if (eqs.contains(i)) continue;
          lits0[i0] = lits[i];
          negs0[i0] = negs[i];
          i0++;
        }
        Measure.Int size = measure.sizeStack.top();
        Measure.Int size0 = new Measure.Int(size.value+1);
        measure.sizeStack.push(size0);
      }

      // prove remaining literals under the resulting substitution
      ProofTree.Node andNode = null;
      if (ptree != null)
      {
        andNode = ptree.createNodeProveLits(name, context, lits, negs, 
            eqs, subst, lits0, negs0, iterationNumber);
        ptree.push(andNode);
      }
      if (en != 0)
      {
        context = context.changeSubst(subst);
        lits = lits0;
        negs = negs0;
      }
      Subst subst0 = proveLiterals(context, lits, negs, 0, done, cont, andNode);
      if (en != 0)
      {
        measure.sizeStack.pop();
      }
      if (ptree != null)
      {
        andNode.finalize(subst0 != null, subst0);
        ptree.pop();
      }
      return subst0;
    }

    // a box containing a value
    private static class Box<T> 
    { 
      private T value; 
      public Box(T value) { this.value = value; }
      public T get() { return value; }
      public void set(T value) { this.value = value; }
    }

    /****************************************************************************
     * Prove a subsequence of literals.
     * @param context the current proof context.
     * @param lits the full sequence of literals.
     * @param negs the corresponding sequence of their polarities.
     * @param n the index of the first literal in the subsequence.
     * @param done set to true if all literals could be proved. 
     * @param cont the continuation to be applied to produce the final substitution.
     * @param andNode the node to which this proof belongs in the pruned proof (may be null).
     * @return the resulting substitution (null if the proof did not succeed)
     ***************************************************************************/
    public Subst proveLiterals(Context context,
      Object[] lits, boolean[] negs, int n, Box<Boolean> done,
      Function<Subst,Subst> cont, ProofTree.Node andNode)
    {
      // base case of the recursion: nothing remains to be proved
      if (n == lits.length) 
      {
        done.set(true);
        return cont.apply(context.subst);
      }

      // still need to prove something
      done.set(false);

      // the first literal and whether it is ground
      Object lit = lits[n];
      boolean neg = negs[n];
      boolean ground = Term.ground(context.subst.apply(lit));
      
      // potentially abort the proof 
      boolean abort = abort(context, lit, neg);
      if (abort) return null;

      // we attempt to prove the literal in various ways
      ProofTree.Node orNode = null;
      if (ptree != null)
      {
        orNode = ptree.createNodeProveLiteral(context, lit, neg);
        ptree.push(orNode);
      }

      // apply identity rule
      Subst subst0 = applyStrategy(context, lits, negs, n, done, cont, andNode, orNode,
          (context0, lit0, neg0)->identity(context0, lit0, neg0));
      if (done(subst0, ground, done, orNode)) return subst0;
      
      // apply the assumptions
      subst0 = applyAssumptions(context, lits, negs, n, done, cont, andNode, orNode);
      if (done(subst0, ground, done, orNode)) return subst0;
      
      // apply SMT only up to level 2 to avoid huge slow down of proof search
      if (SMT_MED <= smtReasoning && context.depth <= 2)
      {
        subst0 = applyStrategy(context, lits, negs, n, done, cont, andNode, orNode,
            (context0, lit0, neg0)->smt(context0, lit0, neg0));
        if (done(subst0, ground, done, orNode)) return subst0;
      }
      
      // apply the rules and record the failed rule positions
      List<RulePos> failed = new ArrayList<RulePos>();
      subst0 = applyRules(context, lits, negs, n, done, cont, andNode, failed);
      if (done(subst0, ground, done, orNode)) return subst0;
      
      if (equalityReasoning != EQUALITY_OFF) 
      {
        // if the first literal is an equality, apply a congruence rule
        subst0 = applyEqualityCongruence(context, lits, negs, n, done, cont, andNode);
        if (done(subst0, ground, done, orNode)) return subst0;

        // apply congruence within general predicates
        subst0 = applyPredicateCongruence(context, lits, negs, n, done, cont, andNode, failed);
        if (done(subst0, ground, done, orNode)) return subst0;

        // rewrite goal by equality rules
        subst0 = applyEqRules(context, lits, negs, n, done, cont, andNode);
        if (done(subst0, ground, done, orNode)) return subst0;
      }
      
      // apply SMT only up to level 2 to avoid huge slow down of proof search
      if (smtReasoning == SMT_MIN && context.depth <= 2)
      {
        subst0 = applyStrategy(context, lits, negs, n, done, cont, andNode, orNode,
            (context0, lit0, neg0)->smt(context0, lit0, neg0));
        if (done(subst0, ground, done, orNode)) return subst0;
      }
      
      // proof has failed
      if (ptree != null) 
      {
        orNode.finalize(false, subst0);
        ptree.pop();
      }
      return subst0;
    }

    /**************************************************************************
     * Determine whether we are done and, if yes, close the proof node.
     * @param subst the current substitution.
     * @param ground the first literal is ground.
     * @param done all the other literals could be proved.
     * @param orNode the node to which the proof belongs.
     * @return true if we may return from the proof of the literal sequence.
     *************************************************************************/
    private boolean done(Subst subst, boolean ground, Box<Boolean> done, ProofTree.Node orNode) 
    {
      if (subst == null && (!ground || !done.get())) return false;
      if (ptree == null) return true;
      orNode.finalize(subst != null, subst);
      ptree.pop();
      return true;
    }
    
    // a strategy to prove a literal in a given context
    // without creating a new substitution
    private interface Strategy
    {
      // returns true if the literal could be proved
      boolean apply(Context context, Object lit, boolean neg);
    }
    
    /****************************************************************************
     * Prove a subsequence of literals, the first one by the given strategy.
     * @param context the current proof context.
     * @param lits the full sequence of literals.
     * @param negs the corresponding sequence of their polarities.
     * @param n the index of the first literal in the subsequence.
     * @param done set to true if all literals could be proved
     * @param cont the continuation to be applied to produce the final substitution.
     * @param andNode the node to which this proof belongs in the pruned proof (may be null).
     * @param orNode the node to which this proof belongs in the pruned proof (may be null).
     * @param strategy the strategy to prove the first literal.
     * @return the resulting substitution (null if the proof did not succeed)
     ***************************************************************************/  
    private Subst applyStrategy(Context context,
      Object[] lits, boolean[] negs, int n, Box<Boolean> done,
      Function<Subst,Subst> cont, ProofTree.Node andNode, ProofTree.Node orNode,
      Strategy strategy)
    {
      // increase the proof size
      Measure.Int size = measure.sizeStack.top();
      Measure.Int size0 = new Measure.Int(size.value+1);
      measure.sizeStack.push(size0);
      
      // try to prove the literal
      boolean okay = strategy.apply(context, lits[n], negs[n]);
      if (!okay)
      {
        measure.sizeStack.pop();
        return null;
      }
      
      // try to recursively prove the remaining literals
      if (ptree != null) ptree.pop();
      Subst subst1 = proveLiterals(context, lits, negs, n+1, done, cont, andNode);
      if (ptree != null) ptree.push(orNode);
      measure.sizeStack.pop();
      
      // remember the size of a successful proof
      if (subst1 != null) size.value = size0.value;
      return subst1;
    }
    
    /****************************************************************************
     * Potentially abort the proof of a literal.
     * @param context the current proof context.
     * @param lit the literal (as a term).
     * @param neg true if the literal is negated.
     * @return true if we may abort the proof of this literal
     ***************************************************************************/
    public boolean abort(Context context, Object lit, boolean neg)
    {
      // check for premature termination of the proof for external reasons
      // from which we cannot recover any more
      if (abortedException != null)
      {
        if (ptree != null) ptree.createNodeAbortion(context, lit, neg, abortedException.getMessage());
        throw abortedException;
      }

      // check for premature termination of the proof from its depth or its size
      String message;
      if (maxDepth != -1 && context.depth > maxDepth)
        message = "We give up the current proof branch because the maximum proof depth is exceeded.";
      else if (maxSize != -1 && measure.sizeStack.top().value >= maxSize)
        message = "We give up the current proof branch because the maximum proof size is exceeded.";
      else
        message = null; 
      if (message != null)
      {
        if (ptree != null) 
          ptree.createNodeAbortion(context, lit, neg, message);
        return true;
      }

      // search for equal literal on stack which indicates a search cycle
      Object lit0 = context.subst.apply(lit);
      int i = 0;
      for (Literal ass : context.assumptions)
      {
        if (neg != ass.neg) { i++; continue; } 
        Object lit1 = context.subst.apply(ass.term);
        if (Term.equal(lit0, lit1)) 
        {
          if (ptree != null) ptree.createNodeIdentityFailure(context, lit, neg, i);
          return true;
        }
        i++;
      }

      // we cannot abort the proof of this literal
      return false;
    }
      
    /****************************************************************************
     * Try to close the proof of a literal from its current context.
     * @param context the current proof context.
     * @param lit the literal (as a term).
     * @param neg true if the literal is negated.
     * @return a new substitution (null if the proof could not be closed)
     ***************************************************************************/
    public boolean identity(Context context, Object lit, boolean neg)
    {
      if (!neg) return false;
      FunctionSymbol pred = (FunctionSymbol)Term.symbol(lit);
      if (!proofProblem.equalities.values().contains(pred)) return false;
      Object term1 = Term.argument(lit, 0);
      Object term2 = Term.argument(lit, 1);
      Object term10 = context.subst.apply(term1);
      Object term20 = context.subst.apply(term2);
      if (!Term.equal(term10,term20)) return false;
      if (ptree != null) ptree.createNodeEqualitySuccess(context, lit, neg);
      return true;
    }
 
    /****************************************************************************
     * Try to close the proof of a literal from its current context.
     * @param context the current proof context.
     * @param lit the literal (as a term).
     * @param neg true if the literal is negated.
     * @return true if the proof could be closed.
     ***************************************************************************/
    public boolean smt(Context context, Object lit, boolean neg)
    {
      if (solver == null) return false;
      String result = solveSMT(context, lit, neg);
      if (result == null || !result.startsWith("unsat")) return false;
      if (ptree == null) return true;
      List<Rule> rules = rulesSMT(context, lit, neg);
      ptree.createNodeSMTSolver(context, lit, neg, result, solver.getSMTLIB(), rules);
      return true;
    }

    /*************************************************************************
     * Try to prove the theorem by showing the unsatisfiability of the clauses.
     * @return true if the proof could be closed.
     ************************************************************************/
    public boolean smtTheorem()
    {
      if (solver2 == null) return false;
      // determine the rules to be presented to the SMT solver
      List<Rule> rules0 = new ArrayList<Rule>();
      int n = rules.length;
      for (int i = n-1; i >= 0; i--)
      { 
        Rule rule = rules[i];
        if (rule.isTheory()) continue;
        rules0.add(rule);
      }
      out.println("We apply the SMT solver to the clauses...");
      List<Rule> rules1 = new ArrayList<Rule>();
      String result1 = solveSMT(rules0, rules1);
      boolean unsat1 = result1 != null && result1.startsWith("unsat");
      if (unsat1)
      {
        out.println("Theorem was proved from the clauses.");
        if (ptree == null) { return true; }  
      }
      else
      {
        out.println("Theorem was NOT proved from the clauses.");
      }
      // determine whether there are variables in the current rule set
      boolean hasVar = false;
      for (Rule rule1 : rules1)
      {
        if (rule1.vars.length > 0) { hasVar = true; break; }
      }
      String result2 = null;
      boolean unsat2 = false;
      List<Rule> instances = null;
      List<Rule> instances0 = null;
      if (hasVar && !checkAbort(false))
      {
        out.println("We apply the SMT solver to ground instances of the clauses...");
        // use for instantiation of rules1 ground terms from all rules (rules0)
        instances = Instances.generate(proofProblem, rules1, rules0);
        instances0 = new ArrayList<Rule>();
        result2 = solveSMT(instances, instances0);
        unsat2 = result2 != null && result2.startsWith("unsat");
        if (unsat2)
          out.println("Theorem was proved from the instances.");
        else
          out.println("Theorem was NOT proved from the instances.");
      }
      if (ptree != null) 
      {
        ptree.createNodeSMTSolver(problem.getProofProblem().getName(), 
            result1, rules1, result2, instances0);
      }
      return unsat1 || unsat2;
    }
    
    /****************************************************************************
     * Prove a subsequence of literals, the first one from the assumptions.
     * @param context the current proof context.
     * @param lits the full sequence of literals.
     * @param negs the corresponding sequence of their polarities.
     * @param n the index of the first literal in the subsequence.
     * @param done set to true if all literals could be proved
     * @param cont the continuation to be applied to produce the final substitution.
     * @param andNode the node to which this proof belongs in the pruned proof (may be null).
     * @return the resulting substitution (null if the proof did not succeed)
     ***************************************************************************/  
    private Subst applyAssumptions(Context context,
      Object[] lits, boolean[] negs, int n, Box<Boolean> done,
      Function<Subst,Subst> cont, ProofTree.Node andNode, ProofTree.Node orNode)
    { 
      // the first literal and its polarity and whether it has no variables
      Object lit = lits[n];
      boolean neg = negs[n];
      boolean ground = Term.ground(context.subst.apply(lit));
      
      // iterate through the complimentary assumptions and
      // attempt to unify each one with this literal 
      int i = -1;
      for (Literal ass : context.assumptions)
      {
        i++;
        if (neg == ass.neg) continue;
        
        // attempt unification
        Object lit0 = ass.term;
        Subst subst0 = context.subst.unify(lit, lit0);
        if (subst0 == null) continue;

        // unification succeeded
        Measure.Int size = measure.sizeStack.top();
        Measure.Int size0 = new Measure.Int(size.value+1);
        measure.sizeStack.push(size0);
        if (ptree != null) 
          ptree.createNodeComplementSuccess(context, lit, neg, i, lit0, subst0);
        
        // try to recursively prove the remaining literals with the updated substitution
        // (pop the or-node to avoid additional level in proof tree)
        if (ptree != null) ptree.pop();
        Subst subst1 = proveLiterals(context.changeSubst(subst0), lits, negs, n+1, done, cont, andNode);
        if (ptree != null) ptree.push(orNode);
        
        // retain the size of the successful proof
        measure.sizeStack.pop();
        if (subst1 != null)
        {
          size.value = size0.value;
          return subst1;
        }

        // there is no need to try other rules
        // if ground literal could be proved but not the continuation
        if (ground && done.get()) break;
      }
      
      // proof failed
      return null;
    }

    /****************************************************************************
     * Prove a subsequence of literals, the first one from the rules.
     * @param context the current proof context.
     * @param lits the full sequence of literals.
     * @param negs the corresponding sequence of their polarities.
     * @param n the index of the first literal in the subsequence.
     * @param done set to true if all literals could be proved
     * @param cont the continuation to be applied to produce the final substitution.
     * @param andNode the node to which this proof belongs in the pruned proof (may be null).
     * @param failed the list of failed rule positions to be updated by the procedure.
     * @return the resulting substitution (null if the proof did not succeed)
     ***************************************************************************/  
    private Subst applyRules(Context context,
      Object[] lits, boolean[] negs, int n, Box<Boolean> done,
      Function<Subst,Subst> cont, ProofTree.Node andNode,
      List<RulePos> failed)
    { 
      // the first literal and its polarity and whether it has no variables
      Object lit = lits[n];
      boolean neg = negs[n];
      boolean ground = Term.ground(context.subst.apply(lit));

      // determine the potentially matching rule positions
      FunctionSymbol pred = (FunctionSymbol)Term.symbol(lit);
      List<RulePos> rposlist = neg ? ModelElimination.this.pos.get(pred) : 
            ModelElimination.this.neg.get(pred);
      if (rposlist == null) return null;

      // iterate through the potentially matching rule positions
      // and collect the non-matching ones
      for (RulePos rpos : rposlist)
      {
        // try to use the previously recorded instance of rule
        Rule rule = rpos.rule;
        Rule rule0 = instances.get(rule);
        Object[] lits0 = rule0.lits;
        Object lit0 = lits0[rpos.pos];
        Subst subst0 = context.subst.unify(lit, lit0);
        if (subst0 == null) { failed.add(rpos); continue; }

        // rule instance can be indeed used, create and record another instance
        newInstance(rule);
        clauseApplications++;

        // increase proof size 
        Measure.Int size = measure.sizeStack.top();
        Measure.Int size0 = new Measure.Int(size.value+1);
        measure.sizeStack.push(size0);

        // we have to prove every literal
        ProofTree.Node ruleNode = null;
        if (ptree != null)
        {
          ruleNode = ptree.createNodeApplyRule(context, subst0, lit, neg, rule0, lit0);
          ptree.push(ruleNode);
        }

        // determine the assumptions of the rule
        int n1 = rule0.lits.length;
        Object[] lits1 = new Object[n1-1];
        boolean[] negs1 = new boolean[n1-1];
        int j = 0;
        for (int i = 0; i < n1; i++)
        {
          Object lit1 = lits0[i];
          if (lit1 == lit0) continue;
          lits1[j] = lit1;
          negs1[j] = rule0.negs[i];
          j++;
        }

        // try to prove the assumptions of the rule and then the remaining literals
        ProofTree.Node ruleNode0 = ruleNode;
        Context context0 = context.changeSubst(subst0).addAssumption(new Literal(lit, neg));
        Box<Boolean> done0 = new Box<Boolean>(false);
        Subst subst1 = proveLiterals(context0, rule0.name, lits1, negs1, done0, (Subst s)->
        {
          if (ptree != null) ptree.push(ruleNode0, andNode);
          Subst subst2 = proveLiterals(context.changeSubst(s), lits, negs, n+1, done, cont, andNode);
          if (ptree != null) ptree.pop();
          return subst2;
        });

        // indicate the success of the proof of every literal
        if (ptree != null) 
        {
          ruleNode.finalize(subst1 != null, subst1);
          ptree.pop();
        }

        // retain the size of the successful proof
        measure.sizeStack.pop();
        if (subst1 != null)
        {
          size.value = size0.value;
          return subst1;
        }

        // there is no need to try other rules
        // if ground literal could be proved but not the continuation
        if (ground && done.get()) break;
      }

      // proof failed
      return null;
    }

    /****************************************************************************
     * Prove a subsequence of literals, the first one by a restricted congruence.
     * @param context the current proof context.
     * @param lits the full sequence of literals.
     * @param negs the corresponding sequence of their polarities.
     * @param n the index of the first literal in the subsequence.
     * @param done set to true if all literals could be proved
     * @param cont the continuation to be applied to produce the final substitution.
     * @param andNode the node to which this proof belongs in the pruned proof (may be null).
     * @param rposlist the list of failed rule positions to be used by this rule
     * @return the resulting substitution (null if the proof did not succeed)
     ***************************************************************************/  
    private Subst applyPredicateCongruence(Context context,
      Object[] lits, boolean[] negs, int n, Box<Boolean> done,
      Function<Subst,Subst> cont, ProofTree.Node andNode,
      List<RulePos> rposlist)
    { 
      // the first literal and its polarity and whether it has no variables
      Object lit = lits[n];
      boolean neg = negs[n];
      boolean ground = Term.ground(context.subst.apply(lit));

      // iterate through the rule positions
      for (RulePos rpos : rposlist)
      {
        // try to use the previously recorded instance of rule
        Rule rule = rpos.rule;
        Rule rule0 = instances.get(rule);
        Object[] lits0 = rule0.lits;
        Object lit0 = lits0[rpos.pos];
        Object[] diff = equalExceptSubterm(lit, lit0);
        if (diff == null) { continue; }

        // determine type of difference terms
        Symbol symbol = Term.symbol(diff[0]);
        TypeSymbol tsymbol;
        if (symbol instanceof VariableSymbol)
        {
          VariableSymbol vsymbol = (VariableSymbol)symbol;
          tsymbol = vsymbol.tsymbol;
        }
        else
        {
          FunctionSymbol fsymbol = (FunctionSymbol)symbol;
          tsymbol = fsymbol.tsymbol;
        }
        boolean intSplit = tsymbol.root == proofProblem.intSymbol;
            
        // rule instance can be indeed used, create and record another instance
        newInstance(rule);
        clauseApplications++;

        // increase proof size 
        Measure.Int size = measure.sizeStack.top();
        Measure.Int size0 = new Measure.Int(size.value+1);
        measure.sizeStack.push(size0);

        // we have to prove every literal
        ProofTree.Node ruleNode = null;
        if (ptree != null)
        {
          if (intSplit)
            ruleNode = ptree.createNodeApplyCaseSplit(context, context.subst, lit, neg, rule0, lit0, 
                diff, lessPred, lessPred);
          else
          {
            FunctionSymbol equalPred = proofProblem.equalities.get(tsymbol.root);
            ruleNode = ptree.createNodeApplyCaseSplit(context, context.subst, lit, neg, rule0, lit0, 
                diff, equalPred, null);
          }
          ptree.push(ruleNode);
        }

        // we prove literal p(t) from rule q1(s) /\ ... /\ qn(s) => p(s) by proving 
        // * for integer terms: q1(s) /\ ... /\ qn(s) /\ ~(t < s) /\ ~(s < t)
        // * for terms of other types: q1(s) /\ ... /\ qn(s) /\ s = t
        // determine the assumptions of the rule
        int n1 = rule0.lits.length;
        Object[] lits1 = intSplit ? new Object[n1+1] : new Object[n1];
        boolean[] negs1 = intSplit ? new boolean[n1+1] : new boolean[n1];
        int j = 0;
        for (int i = 0; i < n1; i++)
        {
          Object lit1 = lits0[i];
          if (lit1 == lit0) continue;
          lits1[j] = lit1;
          negs1[j] = rule0.negs[i];
          j++;
        }

        // add the equality
        if (intSplit)
        {
          lits1[n1-1] = Term.term(lessPred, new Object[] { diff[1], diff[0] });
          negs1[n1-1] = false;
          lits1[n1] = Term.term(lessPred, new Object[] { diff[0], diff[1] });
          negs1[n1] = false;
        }
        else
        {
          FunctionSymbol equalPred = proofProblem.equalities.get(tsymbol.root);
          lits1[n1-1] = Term.term(equalPred, new Object[] { diff[0], diff[1] });
          negs1[n1-1] = true;
        }
        
        // try to prove the assumptions of the rule and then the remaining literals
        ProofTree.Node ruleNode0 = ruleNode;
        Context context0 = context.addAssumption(new Literal(lit, neg));
        Box<Boolean> done0 = new Box<Boolean>(false);
        Subst subst1 = proveLiterals(context0, rule0.name, lits1, negs1, done0, (Subst s)->
        {
          if (ptree != null) ptree.push(ruleNode0, andNode);
          Subst subst2 = proveLiterals(context.changeSubst(s), lits, negs, n+1, done, cont, andNode);
          if (ptree != null) ptree.pop();
          return subst2;
        });

        // indicate the success of the proof of every literal
        if (ptree != null) 
        {
          ruleNode.finalize(subst1 != null, subst1);
          ptree.pop();
        }

        // retain the size of the successful proof
        measure.sizeStack.pop();
        if (subst1 != null)
        {
          size.value = size0.value;
          return subst1;
        }

        // there is no need to try other rules
        // if ground literal could be proved but not the continuation
        if (ground && done.get()) break;
      }

      // proof failed
      return null;
    }

    /****************************************************************************
     * Prove a subsequence of literals, the first one from the equality rules.
     * @param context the current proof context.
     * @param lits the sequence of literals.
     * @param negs the corresponding sequence of their polarities.
     * @param n the index of the first literal in the subsequence.
     * @param done set to true if all literals could be proved.
     * @param andNode the node to which this proof belongs in the pruned proof (may be null).
     * @param cont the continuation to be applied to produce the final substitution.
     * @return the resulting substitution (null if the proof did not succeed)
     ***************************************************************************/  
    private Subst applyEqRules(Context context, 
      Object[] lits, boolean[] negs, int n, Box<Boolean> done,
      Function<Subst,Subst> cont, ProofTree.Node andNode)
    {
      // the first literal and its polarity
      Object lit = lits[n];
      boolean neg = negs[n];

      // determine the actual instance of the literal and whether it has no variables
      Object litx = context.subst.apply(lit);
      boolean ground = Term.ground(litx);

      // determine the potential matches of equality rules with the literal
      // (the instantiated one or the uninstantiated one for limiting the possibilities)
      List<EqMatch> matchlist = equalityReasoning >= EQUALITY_HIGH ? 
          epositions(litx) : epositions(lit);
      if (matchlist == null) return null;

      // iterate through the potential matches
      for (EqMatch match : matchlist)
      {
        // try to use the previously recorded instance of rule
        EqRulePos epos = match.pos;
        Rule rule = match.pos.rule;
        Rule rule0 = instances.get(rule);
        Object[] lits0 = rule0.lits;
        Object lit0 = lits0[epos.pos];
        Object term1 = epos.left ? Term.argument(lit0, 0) : Term.argument(lit0, 1);
        Object term2 = epos.left ? Term.argument(lit0, 1) : Term.argument(lit0, 0);
        Subst subst0 = context.subst.unify(match.term, term1);
        if (subst0 == null) continue;

        // ignore equality, if it does not change anything
        Object sterm1 = subst0.apply(term1);
        Object sterm2 = subst0.apply(term2);
        if (Term.equal(sterm1, sterm2)) continue;

        // ignore equality, if its reverse has already been applied in current branch
        Context context0 = context.addEquality(sterm1, sterm2);
        if (context0 == null) continue;

        // rule instance can be indeed used, create and record another instance
        newInstance(rule);
        clauseApplications++;

        // increase proof size 
        Measure.Int size = measure.sizeStack.top();
        Measure.Int size0 = new Measure.Int(size.value+1);
        measure.sizeStack.push(size0);

        // replace in literal the matching subterm by the other side of the equality
        Object lit1 = match.subst.apply(term2);

        // we have to prove every literal
        ProofTree.Node ruleNode = null;
        if (ptree != null)
        {
          ruleNode = ptree.createNodeApplyEqRule(context, subst0, lit, neg, rule0, lit0, 
              match.term, term1, term2, match.pos.left, lit1);
          ptree.push(ruleNode);
        }

        // prove the substituted literal and append the assumptions of the rule 
        int n1 = rule0.lits.length;
        Object[] lits1 = new Object[n1];
        boolean[] negs1 = new boolean[n1];
        lits1[0] = lit1;
        negs1[0] = neg;
        int j = 1;
        for (int i = 0; i < n1; i++)
        {
          Object lit2 = lits0[i];
          if (lit2 == lit0) continue;
          lits1[j] = lit2;
          negs1[j] = rule0.negs[i];
          j++;
        }

        // try to prove the assumptions of the rule and then the remaining literals
        ProofTree.Node ruleNode0 = ruleNode;
        Context context1 = context0.changeSubst(subst0).addAssumption(new Literal(lit, neg));
        Box<Boolean> done0 = new Box<Boolean>(false);
        Subst subst1 = proveLiterals(context1, rule0.name, lits1, negs1, done0,
            (Subst s)->
        {
          if (ptree != null) ptree.push(ruleNode0, andNode);
          Subst subst2 = proveLiterals(context.changeSubst(s), lits, negs, n+1, done, cont, andNode);
          if (ptree != null) ptree.pop();
          return subst2;
        });

        // indicate the success of the proof of every literal
        if (ptree != null) 
        {
          ruleNode.finalize(subst1 != null, subst1);
          ptree.pop();
        }

        // retain the size of the successful proof
        measure.sizeStack.pop();
        if (subst1 != null)
        {
          size.value = size0.value;
          return subst1;
        }

        // there is no need to try other rules
        // if ground literal could be proved but not the continuation,
        if (ground && done.get()) break;
      }

      // proof failed
      return null;
    }

    /****************************************************************************
     * Prove an equality f(a)=f(b) by proving a=b.
     * @param context the current proof context.
     * @param lits the sequence of literals.
     * @param negs the corresponding sequence of their polarities.
     * @param n the index of the first literal in the subsequence.
     * @param done set to true if all literals could be proved.
     * @param andNode the node to which this proof belongs in the pruned proof (may be null).
     * @param cont the continuation to be applied to produce the final substitution.
     * @return the resulting substitution (null if the proof did not succeed)
     ***************************************************************************/
    private Subst applyEqualityCongruence(Context context, 
      Object[] lits, boolean[] negs, int n, Box<Boolean> done,
      Function<Subst,Subst> cont, ProofTree.Node andNode)
    {
      // the first literal and its polarity (must be negative)
      Object lit = lits[n];
      boolean neg = negs[n];
      if (!neg) return null;
      
      // the literal after substitution
      Object litx = context.subst.apply(lit);
      
      // the predicate symbol (must be an equality)
      Symbol symbol = Term.symbol(litx);
      if (!(symbol instanceof FunctionSymbol)) return null;
      FunctionSymbol pred = (FunctionSymbol)symbol;
      if (!esymbols.contains(pred)) return null;
      
      // the argument terms (must be applications of the same function)
      Object term1 = Term.argument(litx, 0);
      Object symbol1 = Term.symbol(term1);
      if (!(symbol1 instanceof FunctionSymbol)) return null;
      Object term2 = Term.argument(litx, 1);
      Object symbol2 = Term.symbol(term2);
      if (symbol1 != symbol2) return null;

      // the subterm difference (there must be exactly one)
      Object[] subterms = equalExceptSubterm(term1, term2);
      if (subterms == null) return null;
      Object subterm1 = subterms[0];
      Object subterm2 = subterms[1];
      
      // construct new clause with literal a=b
      FunctionSymbol fun = (FunctionSymbol)symbol1;
      TypeSymbol tsymbol = fun.tsymbols.get(0);
      FunctionSymbol equality = emap.get(tsymbol.root);
      Object lit0 = Term.term(equality, new Object[] {subterm1, subterm2});
      Object[] lits0 = new Object[] { lit0 };
      boolean[] negs0 = new boolean[] { neg };
      
      // increase proof size 
      Measure.Int size = measure.sizeStack.top();
      Measure.Int size0 = new Measure.Int(size.value+1);
      measure.sizeStack.push(size0);

      // we have to prove every literal
      ProofTree.Node ruleNode = null;
      if (ptree != null)
      {
        ruleNode = ptree.createNodeApplySubstitution(context, lit, lit0);
        ptree.push(ruleNode);
      }

      // try to prove the new clause and then the remaining literals
      ProofTree.Node ruleNode0 = ruleNode;
      Box<Boolean> done0 = new Box<Boolean>(false);
      Subst subst1 = proveLiterals(context, "equality", lits0, negs0, done0,
          (Subst s)->
      {
        if (ptree != null) ptree.push(ruleNode0, andNode);
        Subst subst2 = proveLiterals(context.changeSubst(s), lits, negs, n+1, done, cont, andNode);
        if (ptree != null) ptree.pop();
        return subst2;
      });

      // indicate the success of the proof of every literal
      if (ptree != null) 
      {
        ruleNode.finalize(subst1 != null, subst1);
        ptree.pop();
      }

      // retain the size of the successful proof
      measure.sizeStack.pop();
      if (subst1 != null)
      {
        size.value = size0.value;
        return subst1;
      }

      // proof failed
      return null;
    }
    
    /****************************************************************************
     * Prove a subsequence of literals, the first one from equality congruence.
     * @param context the current proof context.
     * @param lits the sequence of literals.
     * @param negs the corresponding sequence of their polarities.
     * @param n the index of the first literal in the subsequence.
     * @param done set to true if all literals could be proved.
     * @param andNode the node to which this proof belongs in the pruned proof (may be null).
     * @param cont the continuation to be applied to produce the final substitution.
     * @return the resulting substitution (null if the proof did not succeed)
     ***************************************************************************/
    /*
  private Subst applyGeneralCongruence(Context context, 
    Object[] lits, boolean[] negs, int n, Box<Boolean> done,
    Function<Subst,Subst> cont, ProofTree.Node andNode)
  {
    // the first literal and its polarity 
    Object lit = lits[n];
    boolean neg = negs[n];

    // The predicate symbol
    Symbol symbol = Term.symbol(lit);
    if (!(symbol instanceof FunctionSymbol)) return null;
    FunctionSymbol pred = (FunctionSymbol)symbol;

    // replace literal p(a1,..an) by clause
    // x1 = a1, ..., xn = an, p(x1,...,xn) with fresh variables x1,...,xn
    Object litx = context.subst.apply(lit);
    int an = Term.argnumber(litx);
    List<Object> lits1 = new ArrayList<Object>();
    Object[] args0 = new Object[an];
    for (int i = 0; i < an; i++)
    {
      Object arg = Term.argument(litx, i);
      TypeSymbol tsymbol;
      Symbol s = Term.symbol(arg);
      VariableSymbol var = null;
      if (s instanceof VariableSymbol)
      {
        var = (VariableSymbol)s;
        tsymbol = var.tsymbol.root;
      }
      else
      {
        FunctionSymbol fun = (FunctionSymbol)s;
        tsymbol = fun.tsymbol.root;
      }
      if (var != null) { args0[i] = var; continue; }
      String name = newVarName("x");
      Id id = Id.create(name);
      var = new VariableSymbol(id, tsymbol);
      id.setSymbol(var);
      Object arg0 = Term.term(var);
      FunctionSymbol esymbol = emap.get(tsymbol);
      lits1.add(Term.term(esymbol, new Object[] { arg0, arg }));
      args0[i] = var;
    }
    int ln = lits1.size();
    lits1.add(Term.term(pred, args0));
    Object[] lits0 = lits1.toArray(new Object[ln+1]);
    boolean[] negs0 = new boolean[ln+1];
    for (int i = 0; i < ln; i++) negs0[i] = true;
    negs0[ln] = neg;

    // increase proof size 
    Measure.Int size = measure.sizeStack.top();
    Measure.Int size0 = new Measure.Int(size.value+1);
    measure.sizeStack.push(size0);

    // we have to prove every literal
    ProofTree.Node ruleNode = null;
    if (proofTree != null)
    {
      ruleNode = proofTree.createNodeApplyEqCongruence(context, lit, lits0);
      proofTree.push(ruleNode);
    }

    // try to prove the new clause and then the remaining literals
    ProofTree.Node ruleNode0 = ruleNode;
    Box<Boolean> done0 = new Box<Boolean>(false);
    Subst subst1 = proveLiterals(context, "equality", lits0, negs0, done0,
        (Subst s)->
    {
      if (proofTree != null) proofTree.push(ruleNode0, andNode);
      Subst subst2 = proveLiterals(context, lits, negs, n+1, done, cont, andNode);
      if (proofTree != null) proofTree.pop();
      return subst2;
    });

    // indicate the success of the proof of every literal
    if (proofTree != null) 
    {
      ruleNode.finalize(subst1 != null, subst1);
      proofTree.pop();
    }

    // retain the size of the successful proof
    measure.sizeStack.pop();
    if (subst1 != null)
    {
      size.value = size0.value;
      return subst1;
    }

    // proof failed
    return null;
  }
     */


    // --------------------------------------------------------------------------
    //
    // Connection to auxiliary SMT solver
    //
    // --------------------------------------------------------------------------

    /****************************************************************************
     * Check whether the SMT solver can prove the literal from the set of assumptions.
     * @param the current proof context.
     * @param lit the literal (as a term).
     * @param neg true if the literal is negated.
     * @return string that starts with "unsat" if the proof has succeeded.
     ***************************************************************************/
    private String solveSMT(Context context, Object lit, boolean neg)
    {
      assume(context.subst, context.assumptions);
      solver.openContext();
      Object lit0 = context.subst.apply(lit);
      assume(new Literal(lit0, neg));
      String result = solver.isUnsat();
      solver.closeContext();
      return result;
    }

    /****************************************************************************
     * Check whether the SMT solver can prove the unsatisfiability of 
     * a collection of closed rules.
     * @param rules the closed rules.
     * @param rules0 set to the minimal unsatisfiable core (if the proof succeeded)
     * @return string that starts with "unsat" if the proof has succeeded.
     ***************************************************************************/
    private String solveSMT(List<Rule> rules, List<Rule> rules0)
    {
      solver2.openContext();
      int i = 1;
      for (Rule rule : rules)
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

    /**************************************************************************
     * Add to rules0 the unsatisfiable core of rules.
     * @param rules the rules that were deeemed unsatisfiable by the solver.
     * @param rules0 set to the unsatisfiable core reported by the solver.
     *************************************************************************/
    private void unsatCore(List<Rule> rules, List<Rule> rules0)
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
      for (Rule rule : rules)
      {
        String name = "clause" + Integer.toString(i); i++;
        if (names.contains(name)) rules0.add(rule);
      }
    }
    
    /***************************************************************************
     * Create new solver context with list of assumptions. 
     * @param subst the current substitution.
     * @param assumptions the list of assumptions.
     **************************************************************************/
    private void assume(Subst subst, ConsList<Literal> assumptions)
    {
      int size = assumptions.size();
      int size0 = solverLits.size();
      if (size == size0+1 && assumed(subst, assumptions, size0))
      {
        // emit one new assumption
        assume(assumptions.head());
      }
      else if (size < size0 && assumed(subst, assumptions, size))
      {
        // revoke the superfluous assumptions
        unassume(size0-size);
      }
      else 
      {
        // clear the current list of assumptions and emit new ones
        unassume(size0);
        while (!assumptions.isEmpty())
        {
          assume(assumptions.head());
          assumptions = assumptions.tail();
        }
      }
    }

    /****************************************************************************
     * Add a literal to the assumptions of the solver.
     * @param lit the literal to be assumed.
     ***************************************************************************/
    private void assume(Literal lit)
    {
      Set<VariableSymbol> vars = new LinkedHashSet<VariableSymbol>();
      Term.vars(lit.term, vars);
      int n = solverVars.size();
      if (n > 0) vars.removeAll(solverVars.get(n-1));
      solverLits.add(lit);
      solverVars.add(vars);
      solver.openContext();
      for (VariableSymbol var : vars)
        solver.declareConst(var.id, var.tsymbol.type);
      solver.assume(lit.term, lit.neg);
    }

    /****************************************************************************
     * Remove assumptions from the solver.
     * @param n the number of assumptions to be removed.
     ***************************************************************************/
    private void unassume(int n)
    {
      for (int i = 0; i < n; i++) 
      { 
        solver.closeContext();
        solverLits.remove(solverLits.size()-1);
        solverVars.remove(solverVars.size()-1);
      }
    }

    /****************************************************************************
     * Return true if the first number assumptions of the prover
     * are the corresponding assumptions emitted to the SMT solver.
     * @param subst the current substitution.
     ***************************************************************************/
    private boolean assumed(Subst subst, ConsList<Literal> assumptions, int number)
    {
      for (int i = 1; i <= number; i++)
      {
        Literal alit = assumptions.head();
        Literal slit = solverLits.get(number-i);
        if (alit.neg != slit.neg) return false;
        Object aterm = subst.apply(alit.term);
        Object sterm = subst.apply(slit.term);
        if (!Term.equal(aterm, sterm)) return false;
        assumptions = assumptions.tail();
      }
      return true;
    }

    /****************************************************************************
     * Determine the basic rules of an SMT proof.
     * @param the current proof context.
     * @param lit the literal (as a term) that could be proved in the context.
     * @param neg true if the literal is negated.
     * @return the rules that (may) have been used in the proof of the literal.
     ***************************************************************************/
    private List<Rule> rulesSMT(Context context, Object lit, boolean neg)
    {
      if (solver2 != null) return coreSMT(context, lit, neg); 
      List<Rule> rules = new ArrayList<Rule>(smtRules);
      rules.remove(goal);
      for (Literal assumption : context.assumptions)
      {
        Rule rule = new Rule("", new VariableSymbol[] {} , 
            new Object[] { assumption.term }, new boolean[] { assumption.neg },
            new boolean[] { false });
        rules.add(rule);
      }
      return rules;
    }

    /****************************************************************************
     * Determine the minimal core of an SMT proof.
     * @param the current proof context.
     * @param lit the literal (as a term) that could be proved in the context.
     * @param neg true if the literal is negated.
     * @return the minimal core of rules from which the literal was proved. 
     ***************************************************************************/
    private List<Rule> coreSMT(Context context, Object lit, boolean neg)
    {
      // open temporary context in auxiliary solver
      solver2.openContext();

      // the literal to be proved
      Object lit0 = context.subst.apply(lit);
      
      // determine all assumptions on which the proof of the literal may depend
      // and declare in the auxiliary solver all free variables as constants
      List<Rule> rules = new ArrayList<Rule>(smtRules);
      rules.remove(goal);
      Set<VariableSymbol> vars = new HashSet<VariableSymbol>();
      Term.vars(lit0, vars);
      for (VariableSymbol var : vars)
        solver2.declareConst(var.id, var.tsymbol.type);
      for (Literal assumption : context.assumptions)
      {
        Set<VariableSymbol> vars0 = new HashSet<VariableSymbol>(); 
        Term.vars(assumption.term, vars0);
        vars0.removeAll(vars);
        for (VariableSymbol var : vars0)
          solver2.declareConst(var.id, var.tsymbol.type);
        vars.addAll(vars0);
        Rule rule = new Rule("", new VariableSymbol[] {} , 
            new Object[] { assumption.term }, new boolean[] { assumption.neg },
            new boolean[] { false });
        rules.add(rule);
      }
      solver2.assume(lit0, neg);
      
      // determine the assumptions actually required for the proof of the literal
      List<Rule> rules0 = new ArrayList<Rule>();
      solveSMT(rules, rules0);
      
      // close temporary context and return result
      solver2.closeContext();
      return rules0;
    }

    // --------------------------------------------------------------------------
    //
    // auxiliaries of the proof procedure
    //
    // --------------------------------------------------------------------------

    /****************************************************************************
     * Register a new instance of a rule with fresh names for the
     * quantified variables that do not appear in the proof yet.
     * @param rule the rule for which the instance is to be created.
     ***************************************************************************/
    private void newInstance(Rule rule)
    {
      VariableSymbol[] vars = rule.vars;
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
      Object[] lits = rule.lits;
      int litn = lits.length;
      Object[] lits0 = new Object[litn];
      for (int i = 0; i < litn; i++) lits0[i] = subst.apply(lits[i]);
      String name0 = rule.name;
      boolean[] negs0 = rule.negs;
      boolean[] notGoals0 = rule.notGoals;
      Rule rule0 = new Rule(name0, vars0, lits0, negs0, notGoals0); 
      instances.put(rule, rule0);
    }

    /****************************************************************************
     * Create a new variable name from a given name.
     * @param name the name of a variable.
     * @return a fresh variable name that does not appear in the proof yet.
     ***************************************************************************/
    private String newVarName(String name)
    {
      String name0 = name + "@" + varCounter;
      varCounter++;
      return name0;
    }

    // --------------------------------------------------------------------------
    //
    // Equality reasoning.
    //
    // --------------------------------------------------------------------------

    // a potential match for an equality rule in a term
    private static class EqMatch
    {
      // the position of an equality in a rule
      public final EqRulePos pos;  

      // a subterm matching the equality
      public final Object term; 

      // a function that substitutes the subterm by another term
      public final Function<Object,Object> subst;

      public EqMatch(EqRulePos pos, Object term, Function<Object,Object> subst)
      {
        this.pos = pos;
        this.term = term;
        this.subst = subst;
      }
    }

    /****************************************************************************
     * Get potential matches of equality terms with terms in a literal to be proved.
     * @param lit the term of the literal to be proved.
     * @return the potential matches.
     ***************************************************************************/
    private List<EqMatch> epositions(Object lit)
    {
      // give priority to matching of function symbols over matching of variables
      Function<Object,Object> subst = (Object t)->t;
      List<EqMatch> result1 = new ArrayList<EqMatch>();
      List<EqMatch> result2 = new ArrayList<EqMatch>();
      eposArgs(lit, subst, result1, result2);
      result1.addAll(result2);
      return result1;
    }

    /****************************************************************************
     * Get potential matches of equality terms with all proper subterms of a term.
     * @param term the term.
     * @param subst the substitution to be applied to each match.
     * @param result1 the potential matches of function symbols.
     * @param result2 the potential matches of variables.
     ***************************************************************************/
    private void eposArgs(Object term, Function<Object,Object> subst, 
      List<EqMatch> result1, List<EqMatch> result2)
    {
      FunctionSymbol fun = (FunctionSymbol)Term.symbol(term);
      int n = Term.argnumber(term);
      for (int i = 0; i < n; i++)
      {
        final int i0 = i;
        Function<Object,Object> subst0 = (Object term0)->
        {
          Object[] terms0 = new Object[n];
          for (int j = 0; j < n; j++)
          {
            if (j == i0)
              terms0[j] = term0;
            else
              terms0[j] = Term.argument(term, j);
          }
          Object term1 = Term.term(fun, terms0);
          Object term2 = subst.apply(term1);
          return term2;
        };
        Object subterm = Term.argument(term, i0);
        epos(subterm, subst0, result1, result2);
      }
    }

    /****************************************************************************
     * Get potential matches of equality terms with all subterms of a term
     * (including the term itself).
     * @param term the term.
     * @param subst the substitution to be applied to each match.
     * @param result1 the potential matches of function symbols.
     * @param result2 the potential matches of variables.
     ***************************************************************************/
    private void epos(Object term, Function<Object,Object> subst, 
      List<EqMatch> result1, List<EqMatch> result2)
    {
      // do not rewrite into variables
      Symbol sym = Term.symbol(term);
      if (sym instanceof VariableSymbol) return;

      // add equalities for outermost function symbol
      FunctionSymbol fun = (FunctionSymbol)sym;
      TypeSymbol type = fun.tsymbol.root;
      List<EqRulePos> fpositions = epositions.get(fun);
      if (fpositions != null)
      {
        for (EqRulePos pos : fpositions)
        {
          // ignore positions in the current goal
          if (pos.rule == goal) continue;
          result1.add(new EqMatch(pos, term, subst));
        }
      }

      // add recursively equalities for subterms
      eposArgs(term, subst, result1, result2);

      // add equalities for type of term
      List<EqRulePos> tpositions = evarpositions.get(type);
      if (tpositions != null)
      {
        int an = Term.argnumber(term);
        for (EqRulePos pos : tpositions)
        {
          // ignore positions in the current goal
          if (pos.rule == goal) continue;
          if (equalityReasoning < EQUALITY_HIGH)
          {
            // apply rewriting rule x1=x2 only to constants
            if (an > 0)
            {
              Object rhs = pos.rule.lits[pos.left ? 1 : 0];
              if (Term.symbol(rhs) instanceof VariableSymbol) continue;
            }
          }
          result2.add(new EqMatch(pos, term, subst));
        }
      }
    }
  }
}
// ----------------------------------------------------------------------------
// end of file
// ----------------------------------------------------------------------------
