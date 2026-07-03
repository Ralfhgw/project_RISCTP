// ---------------------------------------------------------------------------
// Solver.java
// Interface to SMT-LIB Solver.
// $Id: Solver.java,v 1.43 2024/07/12 11:13:22 schreine Exp $
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
package risctp.smt;

import java.io.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.concurrent.*;

import risctp.*;
import risctp.syntax.AST.*;
import risctp.syntax.AST.Decl.*;
import risctp.problem.*;
import risctp.types.Symbol.*;

public final class Solver
{
  // --------------------------------------------------------------------------
  //
  // Configuration of solver
  //
  // --------------------------------------------------------------------------

  // path to solver executable
  public static final String PATH = "z3";
  private static String path = PATH;
  public static void setPath(String path) { Solver.path = path; }

  // timeout for solver process (ms, values <= 0 denote no timeout)
  public static final int TIMEOUT = 15000;
  private static int timeout = TIMEOUT;
  public static void setTimeout(int timeout) { Solver.timeout = timeout; }

  // tags of supported solvers 
  public enum SolverType { Z3, CVC5, VAMPIRE, EXSPEC }

  // chosen solver
  public static final SolverType SOLVER = SolverType.Z3;
  private static SolverType solver = SOLVER;
  public static SolverType getSolverType() { return solver; }
  
  // choice of default solver
  static { configure(null); }

  // arguments for calling to determine version and to solve problem
  private static String[] versionArgs;
  private static String[] solverArgs;

  // true if the solver supports incremental mode for internal proofs
  private static boolean solverIncremental;
  public static boolean incremental() { return solverIncremental; }
  
  /****************************************************************************
   * Configure solver with current choice of options.
   * @param solver the type of the solver (if null, solver type remains unchanged,
   * must be called if configuration options have changed)
   ***************************************************************************/
  public static void configure(SolverType solver)
  {
    versionString = null;
    if (solver == null)
      solver = Solver.solver;
    else
      Solver.solver = solver;
    switch (solver)
    {
    case Z3:
    {
      versionArgs = new String[]{ "-version" };
      if (timeout <= 0)
        solverArgs = new String[]{ "-smt2" , "-in" };
      else
        solverArgs = new String[]{ "-smt2" , "-in", "-t:" + timeout };
      solverIncremental = true;
      break;
    }
    case CVC5:
    {
      versionArgs = new String[]{ "--version" };
      if (timeout <= 0)
        solverArgs = new String[]{ "--lang" , "smt2" , "--incremental" };
      else
        solverArgs = new String[]{ "--lang" , "smt2" , "--incremental", "--tlimit-per=" + timeout };
      solverIncremental = true;
      break;
    }
    case VAMPIRE:
    {
      versionArgs = new String[]{ "--version" };
      if (timeout <= 0)
        solverArgs = new String[]{ "--input_syntax" , "smtlib2" , "-om" , "smtcomp" };
      else
        solverArgs = new String[]{ "--input_syntax" , "smtlib2" , "-om" , "smtcomp" , "-t" , Integer.toString(timeout/1000) };
      solverIncremental = true;
      break;
    }
    case EXSPEC:
    {
      versionArgs = new String[]{ "-h" };
      if (timeout <= 0)
        solverArgs = new String[]{ };
      else
        solverArgs = new String[]{ };
      solverIncremental = false;
      break;
    }
    }
  }

  // the process to which the interface is connected and its I/O streams
  private Process process;  
  private PrintWriter writer;
  private BufferedReader reader;

  // see the description of create()
  private boolean quiet;
  private boolean ints;
  private boolean arrays;
  private PrintWriter terminal;
  private Consumer<FormulaSymbol> before;
  private Consumer<FormulaSymbol> success;
  private Consumer<FormulaSymbol> failure;
  private Solver(Process process, boolean quiet, boolean ints, boolean arrays,
    PrintWriter terminal, Consumer<FormulaSymbol> before,
    Consumer<FormulaSymbol> success, Consumer<FormulaSymbol> failure) 
  { 
    this.quiet = quiet;
    this.process = process; 
    this.writer = new PrintWriter(process.getOutputStream(), true);
    this.reader = new BufferedReader(new InputStreamReader(process.getInputStream(), 
        Main.CHAR_SET));
    this.ints = ints;
    this.arrays = arrays;
    this.terminal = terminal;
    this.before = before;
    this.success = success;
    this.failure = failure;
  }

  /****************************************************************************
   * Return solver version information.
   * @result the version string (null, if an error occurred)
   ***************************************************************************/
  private static String versionString = null;
  public static String version()
  {
    if (versionString != null) return versionString;
    try
    {
      List<String> alist = new ArrayList<String>();
      alist.add(path);
      alist.addAll(Arrays.asList(versionArgs));
      ProcessBuilder builder = new ProcessBuilder(alist);
      builder.redirectErrorStream(true);
      Process process = builder.start();
      BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), 
          Main.CHAR_SET));
      process.getOutputStream().close();
      String result = reader.readLine();
      process.destroyForcibly();
      if (result == null) return result;
      versionString = firstLine(result);
      return versionString;
    }
    catch(IOException e)
    {
      Main.getMain().getOutput().println(e.getMessage());
      return null;
    }
  }

  /****************************************************************************
   * Return first line of string.
   * @param string a string that possibly consists of multiple lines.
   * @return the first line of the string.
   ***************************************************************************/
  private static String firstLine(String string)
  {
    int end = string.indexOf('\n');
    if (end == -1) return string;
    return string.substring(0, end);
  }
   
  /****************************************************************************
   * Create instance of the solver interface.
   * @param quiet true if output is to be minimized.
   * @param ints true if the theory of integers is supported.
   * @param arrays true if the theory of arrays is supported.
   * @param terminal the output to be used for progress messages.
   * @param before if not null, a function on a theorem symbol 
   *        before the proof of the theorem is started.
   * @param success if not null, a function that is invoked 
   *        when the theorem could, be successfully proved.
   * @param failure if not null, a function that is invoked 
   *        when the proof of the theorem has failed.
   * @return the instance (null, if executable cannot be invoked)
   ***************************************************************************/
  public static Solver create(boolean quiet, boolean ints, boolean arrays,
    PrintWriter terminal, Consumer<FormulaSymbol> before,
    Consumer<FormulaSymbol> success, Consumer<FormulaSymbol> failure)
  {
    try
    {
      List<String> alist = new ArrayList<String>();
      alist.add(path);
      alist.addAll(Arrays.asList(solverArgs));
      ProcessBuilder builder = new ProcessBuilder(alist);
      builder.redirectErrorStream(true);
      Process process = builder.start();
      return new Solver(process,
          quiet, ints, arrays, terminal, before, success, failure);
    }
    catch(IOException e)
    {
      terminal.println(e.getMessage());
      return null;
    }
  }

  /****************************************************************************
   * Wait for termination of process.
   ***************************************************************************/
  public void terminate()
  {
    try
    {
      process.destroyForcibly();
      boolean okay = process.waitFor(1000, TimeUnit.MILLISECONDS);
      if (!okay) terminal.println("process could not be destroyed...");
    }
    catch(InterruptedException e)
    {
      terminal.println("interrupted while waiting for process termination...");
    }
  }

  /****************************************************************************
   * Receive reply from interface.
   ***************************************************************************/
  private String receive()
  {
    try
    {
      // solvers wait for full input and can only handle one query
      if (solver == SolverType.VAMPIRE || solver == SolverType.EXSPEC) 
        try { process.getOutputStream().close(); } catch(IOException e) { }
      while (!reader.ready() && process.isAlive())
        Thread.sleep(1);
      StringBuilder builder = new StringBuilder();
      while (reader.ready())
      {
        int ch = reader.read();
        if (ch == -1) break;
        builder.append((char)ch);
      }
      String result = builder.toString();
      // Vampire may issue warnings
      if (solver == SolverType.VAMPIRE) result = vampireAnswer(result);
      return result;
    }
    catch(InterruptedException e)
    {
      terminal.println("Interrupted while waiting for input...");
      return null;
    }
    catch(IOException e)
    {
      terminal.println(e.getMessage());
      return null;
    }
  }

  /****************************************************************************
   * Process answer such that initial Vampire warning is dropped.
   * @param answer the answer of the sat solver.
   * @return the processed answer.
   ***************************************************************************/
  private String vampireAnswer(String answer)
  {
    if (!answer.startsWith("% Warning")) return answer;
    String line = firstLine(answer);
    return answer.substring(line.length());
  }
  
  /****************************************************************************
   * Print message unless in "quiet" mode.
   * @param message the message to be printed.
   ***************************************************************************/
  private void println(String message)
  {
    if (quiet) return;
    terminal.println(message);
  }
  private void print(String message)
  {
    if (quiet) return;
    terminal.print(message);
  }

  // --------------------------------------------------------------------------
  //
  // SMTLIB solvers
  //
  // --------------------------------------------------------------------------
  
  // the translator to be used in a problem solution
  private SMTLIB translator;
  
  /****************************************************************************
   * Initialize the solver for a specific proof problem.
   * @param problem the proof problem.
   ***************************************************************************/
  private void initSolver(ProofProblem problem)
  {
    translator = new SMTLIB(problem, false, ints, arrays, writer);
    translator.printPrefix();
  }
  
  /****************************************************************************
   * Initialize a prover with a given problem without attempting the proofs.
   ***************************************************************************/
  public void init(ProofProblem problem)
  {
    initSolver(problem);
  }
  
  /****************************************************************************
   * Add a declaration to the solver context.
   * @param decl the declaration.
   ***************************************************************************/
  public void add(Decl decl)
  {
    decl.accept(translator);
  }
  
  /****************************************************************************
   * Prove a given problem.
   * @param problem the problem.
   * @return true iff all theorems could be proved.
   ***************************************************************************/
  public boolean solve(ProofProblem problem)
  {
    if (solver == SolverType.EXSPEC) return solveExSpec(problem);
    initSolver(problem);
    boolean okay = processProblem(problem);
    exitSolver();
    println("=== SMT-LIB solver session");
    print(getSMTLIB());
    return okay;
  }

  /****************************************************************************
   * Indicate to the solver its termination.
   ***************************************************************************/
  public void exitSolver()
  {
    translator.printSuffix();
  }
  
  /****************************************************************************
   * Open a new proof context.
   ***************************************************************************/
  public void openContext()
  {
    translator.openContext();
  }
  
  /****************************************************************************
   * Declare constant in the current context.
   * @param id the name of the constant.
   * @param type its type.
   ***************************************************************************/
  public void declareConst(Id id, Type type)
  {
    translator.declareConst(id, type);
  }
  
  /****************************************************************************
   * Assume universally quantified clause in current context.
   * @param name the name of the clause.
   * @param vars the variables of the clause.
   * @param lits the literal terms of the clause.
   * @param negs the negation status of the literals.
   ***************************************************************************/
  public void assume(String name, VariableSymbol[] vars, Object[] lits, boolean[] negs)
  {
    translator.assume(name, vars, lits, negs);
  }

  /****************************************************************************
   * Assume variable-free clause in current context.
   * @param lits the literal terms of the clause.
   * @param negs the negation status of the literals.
   ***************************************************************************/
  public void assume(Object[] lits, boolean[] negs)
  {
    translator.assume(lits, negs);
  }
  
  /****************************************************************************
   * Assume literal in current context.
   * @param lit the literal term.
   * @param neg true if the literal is negative.
   ***************************************************************************/
  public void assume(Object lit, boolean neg)
  {
    translator.assume(lit, neg);
  }
  
  /****************************************************************************
   * Check satisfiability of current proof context.
   * @return the result string (may be null); starts with "unsat" if the
   * context is unsatisfiable.
   ***************************************************************************/
  public String isUnsat()
  {
    translator.checkSat();
    String result = receive();
    return result;
  }
  
  /****************************************************************************
   * Get the minimal unsatisfiable core.
   * @return the result string (may be null); has form "(name name ... name)"
   * if the minimal unsatisfiable core could be derived.
   ***************************************************************************/
  public String getUnsatCore()
  {
    translator.getUnsatCore();
    String result = receive();
    return result;
  }
  
  /****************************************************************************
   * Close a proof context.
   ***************************************************************************/
  public void closeContext()
  {
    translator.closeContext();
  }
  
  /****************************************************************************
   * Get SMTLIB translation produced so far.
   * @return the translation.
   ***************************************************************************/
  public String getSMTLIB()
  {
    return translator.getString();
  }
  
  /****************************************************************************
   * Process a given problem.
   * @param problem the problem.
   * @return true iff all theorems could be proved.
   ***************************************************************************/
  private boolean processProblem(ProofProblem problem)
  {
    boolean okay = true;
    List<String> constants = new ArrayList<String>();
    for (Decl decl : problem.decls)
    { 
      if (decl instanceof TypeDecl)
      {
        decl.accept(translator);
      }
      else if (decl instanceof DataType)
      {
        decl.accept(translator);
      }
      else if (decl instanceof Function)
      {
        decl.accept(translator);
        Function decl0 = (Function)decl;
        if (decl0.tvars.length == 0) 
        {
          // record constants for potential counter models
          Id id = decl0.id;
          FunctionSymbol fsymbol = (FunctionSymbol)id.getSymbol();
          TypeSymbol tsymbol = fsymbol.tsymbol.root;
          String name = id.toString();
          if (tsymbol.id.toString().equals(risctp.syntax.AST.INTNAME))
          {
            boolean number = false;
            try { Integer.parseInt(name); number = true; } catch(Exception e) { }
            if (number) continue;
          }
          constants.add(name);
        }
      }
      else if (decl instanceof Axiom)
      {
        decl.accept(translator);
      }
      else if (decl instanceof Theorem)
      {
        Theorem decl0 = (Theorem)decl;
        Boolean okay0 = checkTheorem(decl0, constants);
        // no more proof attempt is to be performed
        if (okay0 == null) return false;
        if (!okay0) okay = false;
        // Vampire can only handle one query
        if (solver == SolverType.VAMPIRE) break;
      }
      else
        println("ERROR: internal error (unknown declaration).");
    }
    return okay;
  }
  
  /****************************************************************************
   * Check a theorem.
   * @param theorem the theorem.
   * @param constants the constants in the current theory.
   * @return true if the theorem could be successfully checked,
   *         false if not, null if no more proof can be attempted.
   ***************************************************************************/
  private Boolean checkTheorem(Theorem theorem, List<String> constants)
  {
    // report query
    FormulaSymbol formula = (FormulaSymbol)theorem.id.getSymbol();
    println("Proving theorem " + formula.id.toString() + "...");
    if (before != null) before.accept(formula);
    
    // open context and perform query (do not yet close to extract model)
    openContext();
    translator.checkTheorem(theorem);

    // receive answer
    long time = System.currentTimeMillis();
    String result = receive();
    time = System.currentTimeMillis()-time;
    
    // abort case
    if (result == null)
    {
      println("FAILURE: SMT solver output terminated (" + time + " ms).\n");
      println("No more proofs are attempted.");
      if (failure != null) failure.accept(formula);
      closeContext();
      return null;
    }

    // success case
    if (result.startsWith("unsat"))
    {
      println("SUCCESS: theorem was proved (" + time + " ms).");
      if (success != null) success.accept(formula);
      closeContext();
      return true;
    }

    // failure case
    println("FAILURE: theorem was not proved (" + time + " ms).");
    println(theorem.toString());
    print(result);
    if (result.startsWith("sat") && !constants.isEmpty())
    {
      translator.getModel(constants);
      String model = receive();
      if (model != null) print(model);
    };
    if (failure != null) failure.accept(formula);
    closeContext();
    return false;
  }
  
  // --------------------------------------------------------------------------
  //
  // ExSpec
  //
  // --------------------------------------------------------------------------
  
  /****************************************************************************
   * Prove a given problem with ExSpec.
   * @param problem the problem.
   * @return true iff all theorems could be proved.
   ***************************************************************************/
  private boolean solveExSpec(ProofProblem problem)
  {
    Decl declaration = null;
    try
    {
      ExSpec translator = new ExSpec(problem, writer);
      for (Decl decl : problem.decls)
      {
        declaration = decl;
        if (decl instanceof Theorem)
        {
          Theorem decl0 = (Theorem)decl;
          return checkTheoremExSpec(decl0, translator);
        }
        decl.accept(translator);
      }
    }
    catch(ExSpec.Unsupported e)
    {
      println("ExSpec is not able to solve the problem for the following reason:");
      println("  " + e.getMessage());
      int mode = Main.getMain().getMode();
      if (mode != 1)
        println("Better use proof mode \"Without Type-Checking Theorems\" (command line option \"-mode 1\").");
      println("The problematic declaration is as follows:");
      println("  " + declaration.toString());
      println("The full proof problem is as follows:");
      for (Decl decl : problem.decls)
        println("  " + decl.toString());
      return false;
    }
    return false;
  }
  
  /****************************************************************************
   * Check a theorem.
   * @param theorem the theorem.
   * @return true if the theorem could be successfully checked,
   ***************************************************************************/
  private boolean checkTheoremExSpec(Theorem theorem, ExSpec translator)
  {
    // report query
    FormulaSymbol formula = (FormulaSymbol)theorem.id.getSymbol();
    println("Proving theorem " + formula.id.toString() + "...");
    if (before != null) before.accept(formula);
    
    // perform query
    theorem.accept(translator);

    // receive answer
    long time = System.currentTimeMillis();
    String result = receive();
    time = System.currentTimeMillis()-time;
  
    // abort case
    if (result == null)
    {
      println("FAILURE: SMT solver output terminated (" + time + " ms).\n");
      println("No more proofs are attempted.");
      if (failure != null) failure.accept(formula);
      return false;
    }

    // separate printed input from actual result
    String lines[] = result.split("\n", 2);
    boolean okay = lines.length == 2;
    if (okay)
    {
      String translation = lines[0];
      String answer = lines[1];
      okay = answer.startsWith("satisfiable") || answer.startsWith("unsatisfiable");
      if (okay)
      {
        println("Translation to ExSpec:");
        println(translation);
        result = answer;
      }
    }
    
    // success case
    if (okay && result.startsWith("unsatisfiable"))
    {
      println("SUCCESS: theorem was proved (" + time + " ms).");
      if (success != null) success.accept(formula);
      return true;
    }

    // failure case
    println("FAILURE: theorem was not proved (" + time + " ms).");
    if (!okay)
    {
      println("Input to ExSpec:");
      print(translator.getText());  
    }
    println("Output of ExSpec:");
    print(result);
    return false;
  }
}
// ----------------------------------------------------------------------------
// end of file
// ----------------------------------------------------------------------------