// ---------------------------------------------------------------------------
// Main.java
// RISC Theorem Proving Interface main program.
// $Id: Main.java,v 1.188 2026/06/23 08:37:08 schreine Exp schreine $
//
// Author: Wolfgang Schreiner <Wolfgang.Schreiner@risc.jku.at>
// Copyright (C) 2022-, Research Institute for Symbolic Computation (RISC)
// Johannes Kepler University, Linz, Austria, https://www.risc.jku.at
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.
// ----------------------------------------------------------------------------
package risctp;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.function.*;
import java.util.concurrent.*;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;

import risctp.syntax.AST.*;
import risctp.syntax.AST.Decl.*;
import risctp.parser.*;
import risctp.types.*;
import risctp.types.Symbol.*;
import risctp.problem.*;
import risctp.smt.*;
import risctp.fol.*;
import risctp.me.*;
import risctp.res.*;
import risctp.web.*;

public class Main
{
  // software version
  public static final String VERSION = "1.9.1 (June 23, 2026)";

  public static final String COPYRIGHT =
  "RISC Theorem Proving Interface " + VERSION + "\n" + 
  "https://www.risc.jku.at/research/formal/software/RISCTP\n" +
  "(C) 2022-, Research Institute for Symbolic Computation (RISC)\n" +
  "This is free software distributed under the terms of the GNU GPL.\n" +
  "Execute \"RISCTP -h\" to see the available command line options.\n" +
  "-----------------------------------------------------------------";
 
  // the character set
  public static String CHAR_SET_NAME = "UTF-8";
  public static Charset CHAR_SET = Charset.forName(CHAR_SET_NAME);
 
  // the static program instance
  private static Main main = new Main();
  public static Main getMain() { return main; }
  
  /***************************************************************************
   * The command line interface.
   * @param args the command line arguments (see help())
   **************************************************************************/
  public static void main(String[] args)
  {
    main.run(args);
  }

  /****************************************************************************
   * Create new program instance.
   ***************************************************************************/
  public Main()
  {
    if (main != null) setInputOutput(main.getInput(), main.getOutput());
  }
  
  // the start time of the prover execution
  private long startTime = System.currentTimeMillis();
  
  // the current input/output interface
  private BufferedReader reader = null;
  private PrintWriter writer = null;
  
  /****************************************************************************
   * Set the input/output interface
   * @param reader the reader (if null, keep default)
   * @param writer the writer (if null, keep default)
   ***************************************************************************/
  public void setInputOutput(BufferedReader reader, PrintWriter writer)
  {
    if (reader != null) this.reader = reader;
    if (writer != null) this.writer = writer;
  }
  
  /****************************************************************************
   * Get the input interface.
   * @return the reader.
   ***************************************************************************/
  public BufferedReader getInput()
  {
    return reader;
  }
  
  /****************************************************************************
   * Get the output interface.
   * @return the writer.
   ***************************************************************************/
  public PrintWriter getOutput()
  {
    return writer;
  }
  
  /***************************************************************************
   * Print error message and potentially exit.
   * @param error the error message
   * @param exit if true then exit
   **************************************************************************/
  public void error(String error, boolean exit)
  {
    writer.println("ERROR: " + error + ".");
    if (exit) exit(false);
    // hasErrors = true;
  }
  
  /***************************************************************************
   * Print error message.
   * @param error the error message
   **************************************************************************/
  public void error(String error)
  {
    error(error, false);
  }
  
  /***************************************************************************
   * Abort from exception.
   * @param error the exception.
   **************************************************************************/
  public void abort(Exception e)
  {
    error(e.getMessage());
    e.printStackTrace(writer);
    exit(false);
  }
  
  /***************************************************************************
   * Print success/failure message.
   * @param okay true if computation was successful.
   **************************************************************************/
  private void printSuccess(boolean okay)
  {
    long time = System.currentTimeMillis()-startTime;
    if (okay)
      writer.println("SUCCESS termination (" + time + " ms).");
    else
      writer.println("FAILURE termination (" + time + " ms).");
  }
  
  /***************************************************************************
   * Terminate the program with a success/failure message.
   * @param okay true if computation was successful.
   **************************************************************************/
  public void exit(boolean okay)
  {
    printSuccess(okay);
    System.exit(okay ? 0 : -1);
  }

  // command line options
  private boolean optionHelp = false;       // print help and exit
  private boolean optionQuiet = false;      // prevent startup message
  private boolean optionWeb = false;        // use web gui
  private int     optionWebPort = -1;       // web port
  private boolean optionBrowseButton = false; // browse web button
  private boolean optionWebButtons = false; // other web buttons
  private boolean optionDecompose = false;  // decompose problem into subproblems
  private boolean optionExpand = false;     // expand predicate/function definitions
  private int     optionMEsmt = ModelElimination.SMT_OFF;     // aid MESON by applying SMT solver
  private int     optionMEeq = ModelElimination.EQUALITY_OFF; // extend MESON by equality reasoning
  private int     optionMEthreads = 0;      // number of MESON threads (0: sequential)
  private boolean optionMEsingle = false;   // MESON only attempts a single goal 
  private boolean optionResSMT = false;     // aid Resolution by applying SMT solver
  private boolean optionAxInts = false;     // emit integer arithmetic axioms
  private boolean optionAxNonLinear = false;// emit non-linear arithmetic axioms
  private boolean optionAxMaps = false;     // emit map axioms
  private boolean optionAxData = false;     // emit datatype axioms
  private boolean optionPrint = false;      // print input after parsing
  private boolean optionPrintAP = false;    // print input after all processing
  private boolean optionPrintTC = false;    // print input after type-checking
  private boolean optionPrintPR = false;    // print input after pruning problem
  private boolean optionPrintOV = false;    // print input after overloading resolution
  private boolean optionPrintCH = false;    // print input after choose removal
  private boolean optionPrintST = false;    // print input after subtype removal
  private boolean optionPrintVA = false;    // print input after variable processing
  private boolean optionPrintCD = false;    // print input after constant definition processing
  private boolean optionPrintFD = false;    // print input after function definition processing
  private boolean optionPrintDT = false;    // print input after datatype processing
  private boolean optionPrintLE = false;    // print input after let processing
  private boolean optionPrintFO = false;    // print input after formula processing
  private boolean optionPrintAS = false;    // print input after arithmetic simplification
  private boolean optionPrintAT = false;    // print only axioms and theorems
  private boolean optionPrintSMT = false;   // print SMT-LIB translation
  private int     optionPrintDec = Decomposer.VERBOSE_NONE; // print decomposition with given verbosity
  private boolean optionPrintSub = false;   // print subproblems arising from decomposition
  private boolean optionPrintCla = false;   // print clause form of problem
  private boolean optionPrintProof = false; // print proof
  private boolean optionPrintSearch = false; // print proof search
  private boolean optionAll = false;        // show all options
  
  // the problem to be processed
  private String path = null;          // path of problem file
  private Source source = null;        // processed content of file

  // set/get the problem file
  private static String fileContent = null; // raw content of file
  private static void setFileContent(String text) { fileContent = text; }
  public static String getFileContent() { return fileContent; }
  
  // the theorems to be processed
  private boolean theoremAll = true;
  private Set<String> theoremNames = new HashSet<String>();
  private Set<FormulaSymbol> theoremSymbols;
  
  // the proving mode
  private int optionMode = 2;

  // the proving method
  public enum Method { NONE, SMT, ME, RES }
  private Method method = Method.NONE;
  private boolean isFOL() { return method == Method.ME || method == Method.RES; }
  
  // the timeout (-1, if none)
  int timeout = -1;
  
  // give access to the options for GUI configuration
  public int getMode() { return optionMode; }
  public int getMethod() { return   
      switch (method) { case NONE -> 0; case SMT -> 1; case ME -> 2; case RES -> 3; }; 
  }
  public int getTimeout() { return timeout; }
  public boolean getExpand() { return optionExpand; }
  public boolean getIntAxioms() { return optionAxInts; }
  public boolean getNonLinearAxioms() { return optionAxNonLinear; }
  public boolean getMapAxioms() { return optionAxMaps; }
  public boolean getDataAxioms() { return optionAxData; }
  public int getMesonEquality() { return optionMEeq; }
  public int getMesonSMT() { return optionMEsmt; }
  public boolean getMesonDepth() { return ModelElimination.getDepth(); }
  public int getMesonLimit() { return ModelElimination.getLimit(); }
  public boolean getMesonIterate() { return ModelElimination.getIterate(); }
  public boolean getMesonSingle() { return optionMEsingle; }
  public int getOptionMEthreads() { return optionMEthreads; }
  public boolean getResSMT() { return optionResSMT; }
  
  // externally configurable timeout
  public void setTimeout(Integer val)
  {
    if (val == null) return;
    timeout = val;
    Solver.setTimeout(timeout); 
    Solver.configure(null); 
    ModelElimination.setTimeout(timeout);
    Resolution.setTimeout(timeout);
  }
  
  // MESON display option
  private int optionMesonDisplay = 2; // 0..3, 2: proof
  public int getMesonDisplay() { return  optionMesonDisplay; }
  public void setMesonDisplay(int val) { optionMesonDisplay = val; }

  // other externally configurable values
  public void addTheorem(String val) { theoremAll = false; theoremNames.add(val); }
  public void setMode(int val) { optionMode = val; }
  public void setMethod(Method val) { method = val; }
  public void setOptionQuiet(boolean val) { optionQuiet = val; }
  public void setOptionWebPort(int val) { optionWebPort = val; }
  public void setOptionBrowseButton(boolean val) { optionBrowseButton = val; }
  public void setOptionWebButtons(boolean val) { optionWebButtons = val; }
  public void setOptionDecompose(boolean val) { optionDecompose = val; }
  public void setOptionExpand(boolean val) { optionExpand = val; }
  public void setOptionAxInts(boolean val) { optionAxInts = val; }
  public void setOptionAxNonLinear(boolean val) { optionAxNonLinear = val; }
  public void setOptionAxMaps(boolean val) { optionAxMaps = val; }
  public void setOptionAxData(boolean val) { optionAxData = val; }
  public void setOptionMEsmt(int val) { optionMEsmt = val; }
  public void setOptionMEeq(int val) { optionMEeq = val; }
  public void setOptionMEthreads(int val) { optionMEthreads = val; }
  public void setOptionMEsingle(boolean val) { optionMEsingle = val; }
  public void setOptionResSMT(boolean val) { optionResSMT = val; }
  public void setOptionPrintSMT(boolean val) { optionPrintSMT = val; }
  public void setOptionPrintPR(boolean val) { optionPrintPR = val; }
  public void setOptionPrintAP(boolean val) { optionPrintAP = val; }
  public void setOptionPrintProof(boolean val) { optionPrintProof = val; }
  public void setOptionPrintAll(boolean val)
  {
    optionPrint = val;
    optionPrintTC = val;
    optionPrintPR = val;
    optionPrintOV = val;
    optionPrintCH = val;
    optionPrintST = val;
    optionPrintVA = val;
    optionPrintCD = val;
    optionPrintFD = val;
    optionPrintDT = val;
    optionPrintLE = val;
    optionPrintFO = val;
    optionPrintAS = val;
    optionPrintAT = val;
    optionPrintSMT = val;
    optionPrintSub = val;
    optionPrintCla= val;
  }
  
  /***************************************************************************
   * Print help on command line arguments and exit.
   **************************************************************************/
  public void help()
  {
    writer.println("RISCTP [ <options> ] [ <path> ]");
    writer.println("<path>: path of problem file (if none, read from stdin)");
    writer.println("<options>: the following command line options");
    writer.println("-h: print this message and exit");
    writer.println("-options: show further options");
    if (optionAll)
    {
      writer.println("<options> includes the following options:");
      writer.println("-theorem T: prove theorem T (default: all theorems)");
      writer.println("-method M: user proof method M (none, smt, meson, res; default: none)");
      writer.println("-decompose: decompose problem before application of method fol");
      writer.println("-expand: expand predicate/function definitions in decomposition");
      writer.println("-mode M: use proof mode M (default 2)");
      writer.println("   M=0: prove only type-checking theorems"); 
      writer.println("   M=1: prove without type-checking theorems");
      writer.println("   M=2: prove with type-checking theorems");
      writer.println("-solver S: use solver S (z3, cvc5, vampire, exspec; default: z3)");
      writer.println("-path P: set path to solver executable (default: \"" + Solver.PATH + "\")");
      writer.println("-timeout T: use timeout T ms (default: " + Solver.TIMEOUT + ")");
      writer.println("-axints: use integer axioms for linear arithmetic");
      writer.println("-axnonlinear: also use integer axioms for non-linear arithmetic");
      writer.println("-axmaps: use map axioms");
      writer.println("-axdata: use data type axioms");
      writer.println("-mesmt S: set level of MESON SMT support");
      writer.println("   S=0: none (default)");
      writer.println("   S=1: minimum (first apply MESON to proof situation)");
      writer.println("   S=2: medium (first apply SMT to proof situation)");
      writer.println("   S=3: maximum (first apply SMT to whole proof problem)");
      writer.println("-meeq E: set level of MESON goal literal rewriting");
      writer.println("   E=0: none (default)");
      writer.println("   E=1: low (only rewrite at outer term positions)");
      writer.println("   E=2: medium (rewrite also at inner term positions)");
      writer.println("   E=3: high (rewrite also at variables)");
      writer.println("   E=4: maximum (also do not order equalities)");
      writer.println("-medepth B E I: loop parameters for MESON proof depth D: for (D=B; D<=E; D+=I)");
      writer.println("-mesize B E I: loop parameters for MESON proof size S: for (S=B; S<=E; S+=I)");
      writer.println("-methreads T: use T threads in parallel MESON proof (default 0: sequential)");
      writer.println("-mesingle: apply MESON only to a single goal");
      writer.println("-ressmt: switch on Resolution SMT support");
      writer.println("-q: quiet execution, minimize output");
      writer.println("-web P B: use web GUI on port P with button control B");
      writer.println("   B=0: without buttons (proof is automatically started)");
      writer.println("   B=1: with buttons (proof has to be manually started)");
      writer.println("   B=2: with buttons except 'browse' (problem cannot be changed)");
      writer.println("-pall: switch on all of the following print options");
      writer.println("-p: print problem after parsing");
      writer.println("-pap: print problem after all processing");
      writer.println("-ptc: print problem after type-checking");
      writer.println("-pov: print problem after overloading resolution");
      writer.println("-pch: print problem after choice removal");
      writer.println("-pst: print problem after subtype removal");
      writer.println("-pva: print problem after variable processing");
      writer.println("-pcd: print problem after constant definition processing");
      writer.println("-pfd: print problem after function definition processing");
      writer.println("-pdt: print problem after datatype processing");
      writer.println("-ple: print problem after let processing");
      writer.println("-pfo: print problem after formula processing");
      writer.println("-pas: print problem after arithmetic simplification");
      writer.println("-ppr: print problem after pruning the problem");
      writer.println("-pat: print axioms and theorems");
      writer.println("-psmt: print SMT-LIB translation of problem");
      writer.println("-pdec V: print decomposition with verbosity V (0-3, default: 0)");
      writer.println("-psub: print subproblems after decomposition");
      writer.println("-pcla: print clause form of problems");
      writer.println("-pproof: print proof (method 'meson' only)");
      writer.println("-psearch: print proof search (method 'meson' only)");
    }
  }
  
  /***************************************************************************
   * The command line interface.
   * @param args the command line arguments (see help())
   **************************************************************************/
  public void run(String[] args)
  {
    // the default input/output interface
    reader = new BufferedReader(new InputStreamReader(System.in, CHAR_SET));
    writer = new PrintWriter(System.out, true);
    
    // process arguments
    processArguments(args);
    
    // print help and exit
    if (optionHelp) { help(); exit(false); }
    
    // print copyright message
    if (!optionQuiet) writer.println(COPYRIGHT);
    
    // start GUI server (does not return)
    if (optionWeb) 
    {  
      ProofProblem problem = null;
      // potentially read source (only from file) and process it
      if (path != null)
      {
        source = sourceFromFile(path);
        if (source == null) exit(false);
        problem = process(source);
        if (problem == null) exit(false); 
      }
      runGUI(problem);
    }
    
    // read source (potentially from stdin), process it, and prove the problem
    source = sourceFromFile(path);
    if (source == null) exit(false);
    ProofProblem problem = process(source);
    if (problem == null) exit(false);
    boolean okay = prove(problem);
    exit(okay);
  }
  
  // the handler that is invoked when "Run" button is pressed
  private ProofProblemHandler handler = null;
  
  // indicate whether problems are to be visualized
  private boolean displayProblems = true;
  
  /****************************************************************************
   * Run software in GUI mode.
   * @param problem the proof problem (may be null)
   ***************************************************************************/
  private void runGUI(ProofProblem problem)
  {
    System.out.println("RISCTP GUI can be browsed at http://localhost:" + optionWebPort + "/");
    boolean okay = runGUI(problem, null);
    if (okay)
    {
      System.out.println("Press <Enter> to terminate the server.");
      try { reader.readLine(); } catch(IOException e) { }
    }
    exit(okay);
  }

  // there is only one instance of the server shared by all instances of Main
  private static Server server = null;
  
  /****************************************************************************
   * Start the GUI server.
   * @return an error message (null, if none)
   ***************************************************************************/
  public String initGUI()
  {
    if (server != null) 
    {
      server.unregisterAll();
      return null;
    }
    server = new Server(optionWebPort);
    String error = server.run();
    if (error == null) return null;
    server = null;
    return error;
  }
  
  /****************************************************************************
   * Prove problem in GUI mode.
   * @param problem the problem (may be null)
   * @param result handler to be invoked on the result status (may be null)
   * @return true if no problem with the startup of the GUI occurred.
   ***************************************************************************/
  public boolean runGUI(ProofProblem problem, Consumer<Boolean> result)
  {
    // set options/content of file for initial display
    if (problem == null)
    {
      setMode(2);
      setOptionExpand(false);
      setOptionAxInts(true);
      setOptionAxNonLinear(false);
      setOptionAxMaps(true);
      setOptionAxData(true);
      setOptionMEeq(ModelElimination.EQUALITY_LOW);
      setOptionMEsmt(ModelElimination.SMT_OFF);
      setOptionResSMT(false);
      displayProblems = true;
      optionPrintProof = true;
      optionPrintSearch = false;
      ModelElimination.setDepth(1, 7, 1);
      ModelElimination.setTimeout(60000);
    }
    else
    {
      StringBuilder builder = new StringBuilder();
      for (Decl decl : problem.decls)
      {
        builder.append(decl);
        builder.append("\n");
      }
      setFileContent(builder.toString());
      source = null;
    }
    // define handler to be invoked when "Run" button is pressed
    handler = new ProofProblemHandler(this, optionBrowseButton, optionWebButtons,
        (ProofProblemHandler handler0, Map<String,String> params) ->
    {
      String file = params.get("file");
      String mode = params.get("mode");
      String decompose = params.get("decompose");
      // String gpsearch = params.get("gpsearch");
      // String gproofs = params.get("gproofs");
      boolean tnone = params.get("tnone").equals("true");
      boolean tsearch = params.get("tsearch").equals("true");
      boolean tproof = params.get("tproof").equals("true");
      displayProblems = !tnone;
      optionPrintSearch = tsearch;
      optionPrintProof = tsearch || tproof;
      String smt = params.get("smt");
      String me = params.get("me");
      String res = params.get("res");
      String timeout = params.get("timeout");
      String multithreaded = params.get("multithreaded");
      String threads = params.get("threads");
      String ldepth = params.get("ldepth");
      String lsize = params.get("lsize");
      String limit = params.get("limit");
      String liter = params.get("liter");
      String lsingle = params.get("lsingle");
      String expand = params.get("expand"); 
      String mesmtoff = params.get("mesmtoff");
      String mesmtmin = params.get("mesmtmin");
      String mesmtmed = params.get("mesmtmed");
      String mesmtmax = params.get("mesmtmax");
      String meeqoff = params.get("meeqoff");
      String meeqlow = params.get("meeqlow");
      String meeqmed = params.get("meeqmed");
      String meeqhigh = params.get("meeqhigh");
      String meeqmax = params.get("meeqmax");
      String ressmt = params.get("ressmt");
      String axints = params.get("axints");
      String axnonlinear = params.get("axnonlinear");
      String axmaps = params.get("axmaps");
      String axdata = params.get("axdata");
      try { optionMode = Integer.parseInt(mode); } catch(NumberFormatException e) { }
      optionDecompose = decompose.equals("true");
      optionExpand = expand.equals("true");
      if (smt.equals("true")) method = Method.SMT;
      if (me.equals("true")) method = Method.ME;
      if (res.equals("true")) method = Method.RES;
      if (mesmtoff.equals("true")) optionMEsmt = ModelElimination.SMT_OFF;
      if (mesmtmin.equals("true")) optionMEsmt = ModelElimination.SMT_MIN;
      if (mesmtmed.equals("true")) optionMEsmt = ModelElimination.SMT_MED;
      if (mesmtmax.equals("true")) optionMEsmt = ModelElimination.SMT_MAX;
      if (meeqoff.equals("true")) optionMEeq = ModelElimination.EQUALITY_OFF;
      if (meeqlow.equals("true")) optionMEeq = ModelElimination.EQUALITY_LOW;
      if (meeqmed.equals("true")) optionMEeq = ModelElimination.EQUALITY_MED;
      if (meeqhigh.equals("true")) optionMEeq = ModelElimination.EQUALITY_HIGH;
      if (meeqmax.equals("true")) optionMEeq = ModelElimination.EQUALITY_MAX;
      optionResSMT = ressmt.equals("true");
      optionAxInts = axints.equals("true");
      optionAxNonLinear = axnonlinear.equals("true");
      optionAxMaps = axmaps.equals("true");
      optionAxData = axdata.equals("true");
      Integer timeout0 = 1000*Integer.parseInt(timeout);
      if (timeout0 != null) setTimeout(timeout0);
      Integer threads0 = Integer.parseInt(threads);
      if (multithreaded.equals("true") && threads0 != null) 
        setOptionMEthreads(threads0);
      else
        setOptionMEthreads(0);
      Integer limit0 = Integer.parseInt(limit);
      int lower = liter.equals("true") ? 1 : limit0;
      if (ldepth.equals("true"))
        ModelElimination.setDepth(lower, limit0, 1);
      else if (lsize.equals("true"))
        ModelElimination.setSize(lower, limit0, 1);
      optionMEsingle = lsingle.equals("true");
      ProofProblem problem0 = problem;
      if (problem0 == null)
      {
        setFileContent(file);
        source = sourceFromText(file);
        problem0 = process(source);
      }
      if (problem0 != null) problem0 = process(problem0);
      boolean okay = problem0 != null;
      if (okay)
      {
        PrintWriter writer0 = writer;
        writer = handler0.getWriter("problem");
        for (Decl decl : problem0.decls)
          writer.println(decl);
        writer = writer0;
        List<ProofProblem> problems = decompose(problem0);
        okay = problems != null;
        if (okay)
        {
          if (method != Method.SMT && displayProblems)
          {
            for (ProofProblem subproblem : problems)
              handler.appendProofProblem(subproblem);
          }
          okay = proveMultiple(problems);
        }
      }
      handler0.signalTermination(okay);
      printSuccess(okay);
      if (result != null) result.accept(okay);
    });

    // register handler and start server to process selected sources
    String error = initGUI();
    if (error != null)
    {
      writer.println("Could not start server: " + error);
      return false;
    }
    server.register("/", handler);
    server.register("/run", handler);
    server.register("/next", handler);
    server.register("/abort", handler);
    server.register("/options", handler);
    server.register("/file", handler);

    // redirect output to GUI
    writer = handler.getWriter("output");

    // without interaction buttons, process current source immediately
    if (!optionWebButtons) 
    {
      ProofProblem problem0 = problem == null ? process(source) : problem;
      if (problem0 != null) problem0 = process(problem0);
      if (problem0 == null) exit(false);
      boolean okay = prove(problem0);
      printSuccess(okay);
    }
    return true;
  }

  /****************************************************************************
   * Process the current source file
   * @param source the source file
   * @return the resulting proof problem (null if an error occurred)
   ***************************************************************************/
  private ProofProblem process(Source source)
  {
    // set start time in case of parsing errors
    startTime = System.currentTimeMillis();
    
    // parse text
    Problem problem = parseSource(source);
    if (problem == null) return null;
    
    // print input after parsing
    if (optionPrint) 
    {
      writer.println("=== after parsing:");
      writer.println(problem);
      writer.println("===");
    }
    
    // do not emit axioms in SMT mode
    if (method == Method.SMT)
    {
      optionAxInts = false;
      optionAxNonLinear = false;
      optionAxMaps = false;
      optionAxData = false;
    }
    
    // type-check problem and generate proof problem
    TypeChecker checker = new TypeChecker(optionAxInts, optionAxNonLinear, optionAxMaps, optionAxData);
    ProofProblem problem0 = checker.check(problem);
    if (problem0 == null) return null;

    // print and return proof problem
    if (optionPrintTC) printDeclarations(problem0, 
        "after type-checking");
    return problem0;
  }
  
  /****************************************************************************
   * Process a proof problem.
   * @param problem the proof problem
   * @return the processed proof problem (null if an error occurred)
   ***************************************************************************/
  public ProofProblem process(ProofProblem problem)
  {
    // function symbols with "fixed" semantics (interpretation builtin into prover)
    Set<FunctionSymbol> fixed = new HashSet<FunctionSymbol>();
    switch (method)
    {
    case SMT:
      fixed.addAll(problem.defFuns);
      fixed.addAll(problem.undefs.values());
      fixed.addAll(problem.equalities.values());
      fixed.addAll(problem.inequalities.values());
      fixed.addAll(problem.intFuns);
      fixed.addAll(problem.constructors);
      fixed.addAll(problem.selectors);
      fixed.addAll(problem.testers);
      fixed.addAll(problem.mapConstructors.values());
      fixed.addAll(problem.mapSelectors.values());
      fixed.addAll(problem.mapStores.values());
      break;
    case ME: case RES: case NONE:
      fixed.addAll(problem.undefs.values());
      fixed.addAll(problem.equalities.values());
      fixed.addAll(problem.inequalities.values());
      break;
    }
    
    // remove subtypes
    problem = Subtypes.process(problem);
    if (problem == null) return null;
    if (optionPrintST) printDeclarations(problem, 
        "after removal of subtypes");

    // resolve overloading
    problem = Overloading.process(problem);
    if (problem == null) return null;
    if (optionPrintOV) printDeclarations(problem, 
        "after resolution of overloading");

    // remove choose expressions
    problem = Choosing.process(problem);
    if (problem == null) return null;
    if (optionPrintCH) printDeclarations(problem, 
        "after removal of choices");

    // replace constants denoting variables by actual variables 
    if (isFOL())
    {
      problem = Variables.process(problem);
      if (problem == null) return null;
      if (optionPrintVA) printDeclarations(problem, 
          "after variable processing");
    }
    
    // replace datatype declarations and match expressions
    if (isFOL())
    {
      problem = Datatypes.process(problem);
      if (problem == null) return null;
      if (optionPrintDT) printDeclarations(problem, 
          "after datatype processing");
    }
    
    // replace let expressions
    if (isFOL())
    {
      problem = Lets.process(problem);
      if (problem == null) return null;
      if (optionPrintLE) printDeclarations(problem, 
          "after let processing");
    }

    // inline constant definitions by axioms
    if (isFOL())
    {
      problem = Constants.process(problem);
      if (problem == null) return null;
      if (optionPrintCD) printDeclarations(problem, 
          "after constant definition processing");
    }
    
    // replace function definitions by axioms
    if (isFOL())
    {
      problem = Definitions.process(problem);
      if (problem == null) return null;
      if (optionPrintFD) printDeclarations(problem, 
          "after function definition processing");
    }
        
    // separate terms from formulas
    if (isFOL())
    {
      problem = Formulas.process(problem);
      if (problem == null) return null;
      if (optionPrintFO) printDeclarations(problem, 
          "after formula processing");
    }

    // reduce number of arithmetic operations and perform simplification
    if (isFOL())
    {
      problem = Operations.process(problem);
      if (problem == null) return null;
      problem = Arithmetic.process(problem);
      if (problem == null) return null;
      if (optionPrintAS) printDeclarations(problem, 
          "after arithmetic simplification");
    }

    // determine theorems and prune problem accordingly
    boolean okay = determineTheorems(problem);
    if (!okay) return null;
    problem = Pruning.process(problem, theoremSymbols, optionMode, fixed);
    if (problem == null) return null;
    if (optionPrintPR) printDeclarations(problem, 
        "after pruning unnecessary declarations");

    // print problem
    if (optionPrintAP) printDeclarations(problem,
        "after all processing");
    
    // print formulas (axioms and theorems)
    if (optionPrintAT) printFormulas(problem, 
        "axioms and theorems");
    
    // the transformed problem
    return problem;
  }
  
  /****************************************************************************
   * Decompose proof problem into subproblems.
   * @param problem the proofproblem.
   * @return the subproblems.
   ***************************************************************************/
  private List<ProofProblem> decompose(ProofProblem problem)
  {
    // decomposition only implemented for minimal FOL language
    if (!optionDecompose || !isFOL())
    {
      List<ProofProblem> problems = new ArrayList<ProofProblem>();
      problems.add(problem);
      return problems;
    }
    println("=== decomposition of proof problem into subproblems");
    Decomposer decomposer = handler == null || !displayProblems ? 
        new Decomposer(problem, writer, optionExpand, null, null, null) :
          new Decomposer(problem, writer, optionExpand,
              (Decomposer.ProofTree tree) ->
          {
            handler.appendProofTree(tree);
          },
          (Decomposer.ProofNode parent) ->
          {
            handler.appendChildren(parent);
          },
          (Decomposer.ProofNode parent) ->
          {
            handler.appendOpenNode(parent);
          });
    List<ProofProblem> problems = decomposer.decompose();
    decomposer.printProofTrees(optionPrintDec);
    int n = problems.size();
    if (n == 1)
      println("Decomposition created " + n + " subproblem.");
    else
      println("Decomposition created " + n + " subproblems.");
    if (optionPrintSub)
    {
      int counter = 1;
      for (ProofProblem subproblem : problems)
      {
        println("subproblem " + counter + ":");
        printFormulas(subproblem, "axioms and theorems");
        counter++;
      }
    }
    return problems;
  }
  
  /****************************************************************************
   * Perform proofs in type-checked proof problem.
   * @param problem the proof problem.
   * @return true if all proofs succeeded.
   ***************************************************************************/
  public boolean prove(ProofProblem problem)
  {
    problem = process(problem);
    List<ProofProblem> problems = decompose(problem);
    boolean okay = proveMultiple(problems);
    return okay;
  }
  
  /****************************************************************************
   * Perform proofs in type-checked proof problems.
   * @param problems the proof problems.
   * @return true if all proofs succeeded.
   ***************************************************************************/
  private boolean proveMultiple(List<ProofProblem> problems)
  {
    System.gc(); // make timings more predictable
    startTime = System.currentTimeMillis();
    Boolean okayParallel = proveParallel(problems, optionMEthreads);
    if (okayParallel != null) return okayParallel;
    boolean okay = true;
    int i = 1;
    boolean multiple = problems.size() > 1;
    for (ProofProblem problem : problems)
    {
      if (multiple) writer.print(i + ". ");
      boolean okay0 = proveSingle(problem);
      okay = okay && okay0;
      i++;
    }
    return okay;
  }

  /****************************************************************************
   * Perform parallel proofs in type-checked proof problems.
   * @param problems the proof problems.
   * @param threads the number of threads to use.
   * @return null, if we cannot/do not want to perform the proofs in parallel.
   *         otherwise true if all proofs succeeded.
   ***************************************************************************/
  private Boolean proveParallel(List<ProofProblem> problems, int threads)
  {
    if (threads < 2 || problems.size() < 2) return null;
    switch (method)
    {
    case SMT: 
      // actually not utilized because we do not decompose problem for SMT
      return proveParallelSMT(problems, threads);
    case ME: 
      if (!optionMEsingle) return null;
      return proveParallelMESON(problems, threads);
    default: return null;
    }
  }
  
  /****************************************************************************
   * Perform parallel SMT proofs in type-checked proof problems.
   * @param problems the proof problems.
   * @param the number of threads to use.
   * @return true if all proofs succeeded.
   ***************************************************************************/
  private boolean proveParallelSMT(List<ProofProblem> problems, int threads)
  {
    println("=== SMT solving " + problems.size() + " subproblems by " + threads + " parallel threads");
    println("SMT solver: " + Solver.version());
    ExecutorService executor = Executors.newFixedThreadPool(optionMEthreads);
    List<Future<SMTTask>> futures = new ArrayList<Future<SMTTask>>();
    for (ProofProblem problem : problems)
    {
      futures.add(executor.submit(new SMTTask(problem)));
    }
    int i = 0;
    boolean result = true;
    for (Future<SMTTask> future : futures)
    {
      if (handler != null && handler.isAborted()) { result = false; break; }
      try 
      { 
        println("=== subproblem " + (i+1) + ":");
        SMTTask task = future.get(); 
        String output = task.getOutput();
        print(output);
        if (!output.endsWith("\n")) println("");
        result = result && task.getResult();
        println("===");
      }
      catch(InterruptedException e) { return false; }
      catch(ExecutionException e) { return false; }
      i++;
    }
    println("===");
    executor.shutdown();
    return result;
  }
  
  // a task that solves a problem by SMT
  private class SMTTask implements Callable<SMTTask>
  {
    private ProofProblem problem;
    private StringWriter output;
    private Solver solver;
    private boolean result;
    public String getOutput() { return output.toString(); }
    public boolean getResult() { return result; }
    public SMTTask(ProofProblem problem)
    {
      this.problem = problem;
      this.output = new StringWriter();
      this.solver = createSolver(false, optionQuiet, new PrintWriter(output, true));
      this.result = false;
    }
    public SMTTask call()
    {
      if (solver == null) return this;
      result = solve(problem, solver, handler);
      return this;
    }
  }
  
  /****************************************************************************
   * Solve problem with SMT solver.
   * @param problem the problem.
   * @param solver the solver.
   * @param handler the proof problem handler (may be null).
   * @return true if the problem could be solved.
   ***************************************************************************/
  private static boolean solve(ProofProblem problem, 
    Solver solver, ProofProblemHandler handler)
  {
    boolean running[] = new boolean[] { true };
    if (handler != null)
    {
      new Thread(()->
      {
        while (true)
        {
          if (!running[0]) return;
          if (handler.isAborted()) { solver.terminate(); return; }
          try { Thread.sleep(1000); } catch(InterruptedException e) { }
        }
      }).start();
    }
    boolean okay = solver.solve(problem);
    running[0] = false;
    return okay;
  }
  
  /****************************************************************************
   * Perform parallel MESON proofs in type-checked proof problems.
   * @param problems the proof problems.
   * @param threads the number of threads to use.
   * @return true if all proofs succeeded.
   ***************************************************************************/
  private boolean proveParallelMESON(List<ProofProblem> problems, int threads)
  {
    print("Multithreading with " + threads + " threads is enabled, ");
    println("goal selection option 'Single Goal' is selected.");
    println("Therefore all subproblems are solved in parallel by attempting their last goals.");
    List<ClauseProblem> cproblems = new ArrayList<ClauseProblem>();
    for (ProofProblem problem : problems)
      cproblems.addAll(ClauseProblem.transform(problem));
    if (handler != null && displayProblems)
    {
      for (ClauseProblem cproblem : cproblems)
        handler.appendClauseProblem(cproblem);
    }
    println("=== MESON solving " + problems.size() + " subproblems by " + threads + " parallel threads");
    ModelElimination.generateProof(optionPrintProof);
    ModelElimination.generateProofSearch(false);
    ExecutorService executor = Executors.newFixedThreadPool(optionMEthreads);
    List<Future<MESONTask>> futures = new ArrayList<Future<MESONTask>>();
    for (ClauseProblem cproblem : cproblems)
    {
      futures.add(executor.submit(new MESONTask(cproblem)));
    }
    int i = 0;
    boolean result = true;
    for (Future<MESONTask> future : futures)
    {
      if (handler != null && handler.isAborted()) 
      { 
        println("The user has aborted the proof.");
        result = false; 
        break; 
      }
      try 
      { 
        print((i+1) + ". ");
        writer.flush();
        i++;
        MESONTask task = future.get(); 
        String output = task.getOutput();
        print(output);
        if (!output.endsWith("\n")) println("");
        writer.flush();
        result = result && task.getResult();
        if (handler == null) continue;
        handler.newTree(ProofProblemHandler.ID_PROOFS);
        ProofTree proofTree = task.getProofTree();
        if (proofTree != null) 
        {
          List<ProofTree.Node> roots = proofTree.getRoots();
          if (!roots.isEmpty()) publishTree(roots.get(0));
        }
        task.clear();
      }
      catch(InterruptedException e) { return false; }
      catch(ExecutionException e) { return false; }
    }
    println("===");
    executor.shutdown();
    return result;
  }
  
  /****************************************************************************
   * Publish the proof tree rooted in the given node.
   * @param node the root of the tree.
   ***************************************************************************/
  private void publishTree(ProofTree.Node node)
  {
    handler.newNode(node);
    handler.updateNode(node);
    List<ProofTree.Node> children = node.getChildren();
    if (children != null)
    {
      for (ProofTree.Node child : children)
        publishTree(child);
    }
    handler.finalizeNode(node);
  }

  // a task that solves a problem by MESON
  private class MESONTask implements Callable<MESONTask>
  {
    private ClauseProblem cproblem;
    private StringWriter output;
    private ModelElimination prover;
    private boolean result;
    private ProofTree proofTree;
    public String getOutput() { return output.toString(); }
    public boolean getResult() { return result; }
    public ProofTree getProofTree() { return proofTree; }
    public MESONTask(ClauseProblem cproblem)
    {
      this.cproblem = cproblem;
      this.output = new StringWriter();
      this.prover = new ModelElimination(new PrintWriter(output, true), 
          false, true, 
          optionMEsmt, optionMEeq, optionMEthreads, optionMEsingle,
          null, null, null, 
          handler == null ? null : () -> { return handler.isAborted(); });
      this.result = false;
      this.proofTree = null;
    }
    public MESONTask call()
    { 
      if (handler != null && handler.isAborted()) return this;
      // long time = System.currentTimeMillis();
      result = prover.solve(cproblem);
      // time = System.currentTimeMillis()-time;
      // output.append("Task terminated after " + time + " ms.\n");
      if (result) prover.pruneProof();
      proofTree = prover.getProofTree();
      if (!result) 
      {
        List<ProofTree.Node> roots = proofTree.getRoots();
        if (!roots.isEmpty()) roots.get(0).getChildren().clear();
      }
      return this;
    }
    public void clear()
    {
      prover.clearProof();
    }
  }
  
  /****************************************************************************
   * Perform proofs in type-checked proof problem.
   * @param problem the proof problem.
   * @return true if all proofs succeeded.
   ***************************************************************************/
  private boolean proveSingle(ProofProblem problem)
  {
    // success status of proof attempt
    boolean okay = false;
    switch (method)
    {
    case NONE:
    {
      println("=== no proof method selected");
      break;
    }
    case SMT:
    {
      // print SMT-LIB translation
      if (optionPrintSMT)
      {
        SMTLIB translator = new SMTLIB(problem, false, true, true, writer);
        writer.println("=== SMT-LIB translation");
        translator.print();
        writer.println("===");
      }
      // prove theorems with SMT-LIB solver
      println("=== SMT solving");
      Solver solver = createSolver(false, optionQuiet, writer);
      if (solver == null) return false;
      println("SMT solver: " + Solver.version());
      okay = solve(problem, solver, handler);
      println("===");
      break;
    }
    case ME: case RES:
    {
      List<ClauseProblem> cproblems = ClauseProblem.transform(problem);
      for (ClauseProblem cproblem : cproblems)
      {
        if (handler != null && displayProblems) 
          handler.appendClauseProblem(cproblem);
        if (optionPrintCla)
        {
          println("=== clause form of problem");
          cproblem.print(writer, true, true, 
              Clause.PrefixFormat.TYPES, Clause.MatrixFormat.IMPLICATION);
        }
        switch (method)
        {
        case ME:
        {
          ModelElimination.generateProof(optionPrintProof);
          ModelElimination.generateProofSearch(optionPrintSearch);
          ModelElimination prover = 
              handler == null ?
                  new ModelElimination(writer, optionPrintSearch, true, 
                      optionMEsmt, optionMEeq, optionMEthreads, optionMEsingle,
                      null, null, null, null) :
                    new ModelElimination(writer, false, true, 
                        optionMEsmt, optionMEeq, optionMEthreads, optionMEsingle,
                        (ProofTree.Node node)-> { handler.newNode(node); },
                        (ProofTree.Node node)-> { handler.updateNode(node); },
                        (ProofTree.Node node)-> { handler.finalizeNode(node); },
                        () -> { return handler.isAborted(); }
                        );  
          if (handler != null) handler.newTree(ProofProblemHandler.ID_PSEARCH);
          okay = prover.solve(cproblem);
          if (handler != null) handler.newTree(ProofProblemHandler.ID_PROOFS);
          ProofTree proofTree = prover.getProofTree();
          // does not work since we have already pruned the OR-tree (mostly)
          /*
          if (optionPrintSearch)
          {
            writer.println("Proof Search:");
            proofTree.printOutline();
            writer.println("------------------------------------------------------------");
          }
          */
          prover.pruneProof();
          if (optionPrintProof && server == null)
          {
            writer.println("Proof:");
            proofTree.printOutline();
            proofTree.printText();
            writer.println("------------------------------------------------------------");
          }
          prover.clearProof();
          break;
        }
        case RES:
        {
          Resolution prover = new Resolution(writer, optionResSMT, 
              () -> { return handler.isAborted(); });
          okay = prover.solve(cproblem);
          break;
        }
        default: { }
        }
      }
      break;
    }
    }
    return okay;
  }
  
  /****************************************************************************
   * Create SMT solver.
   * @param incremental true if the solver must support incremental mode.
   * @param quiet true if the solver shall be quiet.
   * @param writer the destination of output.
   * @return the solver (null, if no solver could be created).
   ***************************************************************************/
  private Solver createSolver(boolean incremental, boolean quiet, PrintWriter writer)
  {
    String version = Solver.version();
    if (version == null) 
    {
      println("Could not create solver");
      println("===");
      return null;
    }
    if (incremental && !Solver.incremental())
    {
      println("SMT solver does not support incremental mode.");
      println("Choose another solver or switch off SMT support for proving.");
      return null;
    }
    Solver solver = Solver.create(quiet, true, true, writer, null, null, null);
    return solver;
  }

  /****************************************************************************
   * Print message unless in "quiet" mode.
   * @param message the message to be printed.
   ***************************************************************************/
  private void print(String message)
  {
    if (optionQuiet) return;
    writer.print(message);
  }
  
  /****************************************************************************
   * Print message unless in "quiet" mode.
   * @param message the message to be printed.
   ***************************************************************************/
  private void println(String message)
  {
    if (optionQuiet) return;
    writer.println(message);
  }
  
  /***************************************************************************
   * Process command line arguments.
   **************************************************************************/
  public void processArguments(String[] args)
  {
    int n = args.length;
    int i = 0;
    while (i < n)
    {
      String arg = args[i];
      i++;
      switch (arg)
      {
      case "-theorem" :
      {
        if (i == n) 
        { 
          writer.println("option -theorem requires argument T");
          help();
          exit(false);
        }
        arg = args[i];
        i++;
        theoremAll = false; 
        theoremNames.add(arg);
        break;
      }
      case "-method" :
      {
        if (i == n) 
        { 
          writer.println("option -method requires argument M");
          help();
          exit(false);
        }
        arg = args[i];
        i++;
        switch (arg)
        {
        case "none": method = Method.NONE; break;
        case "smt": method = Method.SMT; break;
        case "meson": method = Method.ME; break;
        case "res": method = Method.RES; break;
        default:
        {
          writer.println("option -method does not support argument <" + arg + ">");
          writer.println("argument must be one of: none smt meson res");
          help();
          exit(false);
        }
        }
        break;
      }
      case "-mode" :
      {
        if (i == n) 
        { 
          writer.println("option -mode requires argument M");
          help();
          exit(false);
        }
        arg = args[i];
        i++;
        switch (arg)
        {
        case "0": optionMode = 0; break;
        case "1": optionMode = 1; break;
        case "2": optionMode = 2; break;
        default:
        {
          writer.println("option -mode does not support argument <" + arg + ">");
          writer.println("argument must be one of: 0 1 2");
          help();
          exit(false);
        }
        }
        break;
      }
      case "-solver" :
      {
        if (i == n) 
        { 
          writer.println("option -solver requires argument S");
          help();
          exit(false);
        }
        arg = args[i];
        i++;
        switch (arg)
        {
        case "z3": 
        {
          Solver.configure(Solver.SolverType.Z3); 
          break;
        }
        case "cvc5": 
        {
          Solver.configure(Solver.SolverType.CVC5); 
          break;
        }
        case "vampire": 
        {
          Solver.configure(Solver.SolverType.VAMPIRE); 
          break;
        }
        case "exspec": 
        {
          Solver.configure(Solver.SolverType.EXSPEC); 
          break;
        }
        default: 
        {
          writer.println("option -solver does not support argument <" + arg + ">");
          writer.println("argument must be one of: z3 cvc5 vampire exspec");
          help();
          exit(false);
        }
        }
        break;
      }
      case "-path" :
      {
        if (i == n) 
        { 
          writer.println("option -path requires argument P");
          help();
          exit(false);
        }
        arg = args[i];
        i++;
        Solver.setPath(arg);
        Solver.configure(null);
        break;
      }
      case "-timeout" :
      {
        if (i == n) 
        { 
          writer.println("option -timeout requires argument T");
          help();
          exit(false);
        }
        arg = args[i];
        i++;
        Integer timeout = toInteger(arg);
        if (timeout == null) 
        {
          writer.println("-timeout argument <" + arg + "> does not denote an integer");
          help();
          exit(false);
        }
        setTimeout(timeout);
        break;
      }
      case "-axints" :
      {
        optionAxInts = true;
        break;
      }
      case "-axnonlinear" :
      {
        optionAxNonLinear = true;
        break;
      }
      case "-axmaps" :
      {
        optionAxMaps = true;
        break;
      }
      case "-axdata" :
      {
        optionAxData = true;
        break;
      }
      case "-medepth" :
      {
        if (i+2 >= n) 
        { 
          writer.println("option -medepth requires arguments B E I");
          help();
          exit(false);
        }
        arg = args[i];
        i++;
        Integer B = toInteger(arg);
        if (B == null) 
        {
          writer.println("-medepth argument B=<" + arg + "> does not denote an integer");
          help();
          exit(false);
        }
        arg = args[i];
        i++;
        Integer E = toInteger(arg);
        if (E == null) 
        {
          writer.println("-medepth argument E=<" + arg + "> does not denote an integer");
          help();
          exit(false);
        }
        arg = args[i];
        i++;
        Integer I = toInteger(arg);
        if (I == null) 
        {
          writer.println("-medepth argument I=<" + arg + "> does not denote an integer");
          help();
          exit(false);
        }
        ModelElimination.setDepth(B, E, I);
        break;
      }
      case "-mesize" :
      {
        if (i+2 >= n) 
        { 
          writer.println("option -mesize requires arguments B E I");
          help();
          exit(false);
        }
        arg = args[i];
        i++;
        Integer B = toInteger(arg);
        if (B == null) 
        {
          writer.println("-mesize argument B=<" + arg + "> does not denote an integer");
          help();
          exit(false);
        }
        arg = args[i];
        i++;
        Integer E = toInteger(arg);
        if (E == null) 
        {
          writer.println("-mesize argument E=<" + arg + "> does not denote an integer");
          help();
          exit(false);
        }
        arg = args[i];
        i++;
        Integer I = toInteger(arg);
        if (I == null) 
        {
          writer.println("-mesize argument I=<" + arg + "> does not denote an integer");
          help();
          exit(false);
        }
        ModelElimination.setSize(B, E, I);
        break;
      }
      case "-mesmt" :
      {
        if (i == n) 
        { 
          writer.println("option -mesmt requires argument S");
          help();
          exit(false);
        }
        arg = args[i];
        i++;
        Integer mesmt0 = toInteger(arg);
        if (mesmt0 == null) 
        {
          writer.println("-mesmt argument <" + arg + "> does not denote an integer");
          help();
          exit(false);
        }
        if (mesmt0 < 0 || mesmt0 > 3)
        {
          writer.println("option -meeq does not support argument <" + arg + ">");
          writer.println("argument must be one of: 0 1 2 3");
        }
        setOptionMEsmt(mesmt0);
        break;
      }
      case "-meeq" :
      {
        if (i == n) 
        { 
          writer.println("option -meeq requires argument E");
          help();
          exit(false);
        }
        arg = args[i];
        i++;
        Integer meeq0 = toInteger(arg);
        if (meeq0 == null) 
        {
          writer.println("-meeq argument <" + arg + "> does not denote an integer");
          help();
          exit(false);
        }
        if (meeq0 < 0 || meeq0 > 4)
        {
          writer.println("option -meeq does not support argument <" + arg + ">");
          writer.println("argument must be one of: 0 1 2 3 4");
        }
        setOptionMEeq(meeq0);
        break;
      }
      case "-methreads" :
      {
        if (i == n) 
        { 
          writer.println("option -methreads requires argument T");
          help();
          exit(false);
        }
        arg = args[i];
        i++;
        Integer methreads = toInteger(arg);
        if (methreads == null) 
        {
          writer.println("-methreads argument <" + arg + "> does not denote an integer");
          help();
          exit(false);
        }
        if (methreads < 0)
        {
          writer.println("option -methreads does not support argument <" + arg + ">");
          writer.println("argument must be greater equal 0");
        }
        optionMEthreads = methreads;
        break;
      }
      case "mesingle": 
      {
        optionMEsingle = true;
        break;
      }
      case "ressmt": 
      {
        setOptionResSMT(true);
        break;
      }
      case "axints": 
      {
        optionAxInts = true;
        break;
      }
      case "axnonlinear": 
      {
        optionAxNonLinear = true;
        break;
      }
      case "axmaps": 
      {
        optionAxMaps = true;
        break;
      }
      case "axdata": 
      {
        optionAxData = true;
        break;
      }
      case "-h" :
      {
        optionHelp = true;
        break;
      }
      case "-q" :
      {
        optionQuiet = true;
        break;
      }
      case "-web" :
      {
        optionWeb = true;
        if (i+1 >= n) 
        { 
          writer.println("option -web requires arguments P B");
          help();
          exit(false);
        }
        arg = args[i];
        i++;
        Integer port = toInteger(arg);
        if (port == null) 
        {
          writer.println("-web argument P = <" + arg + "> does not denote an integer");
          help();
          exit(false);
        }
        if (port < 1024 || port > 65535)
        {
          writer.println("-web argument P = <" + arg + "> must be in interval [1024,65535]");
          help();
          exit(false);
        }
        optionWebPort = port;
        arg = args[i];
        i++;
        switch (arg)
        {
        case "0": optionBrowseButton = false; optionWebButtons = false; break;
        case "1": optionBrowseButton = true; optionWebButtons = true; break;
        case "2": optionBrowseButton = false; optionWebButtons = true; break;
        default:
        {
          writer.println("option -web does not support argument B = <" + arg + ">");
          writer.println("argument B must be one of: 0 1 2");
          help();
          exit(false);
        }
        }
        break;
      }
      case "-decompose" :
      {
        optionDecompose = true;
        break;
      }
      case "-expand" :
      {
        optionExpand = true;
        break;
      }
      case "-options" :
      {
        optionAll = true;
        help();
        exit(false);
        break;
      }
      case "-pall" :
      {
        setOptionPrintAll(true);
        break;
      }
      case "-p" :
      {
        optionPrint = true;
        break;
      }
      case "-pap" :
      {
        optionPrintAP = true;
        break;
      }
      case "-ptc" :
      {
        optionPrintTC = true;
        break;
      }
      case "-ppr" :
      {
        optionPrintPR = true;
        break;
      }
      case "-pov" :
      {
        optionPrintOV = true;
        break;
      }
      case "-pch" :
      {
        optionPrintCH = true;
        break;
      }
      case "-pst" :
      {
        optionPrintST = true;
        break;
      }
      case "-pva" :
      {
        optionPrintVA = true;
        break;
      }
      case "-pcd" :
      {
        optionPrintCD = true;
        break;
      }
      case "-pfd" :
      {
        optionPrintFD = true;
        break;
      }
      case "-pdt" :
      {
        optionPrintDT = true;
        break;
      }
      case "-ple" :
      {
        optionPrintLE = true;
        break;
      }
      case "-pfo" :
      {
        optionPrintFO = true;
        break;
      }
      case "-pas" :
      {
        optionPrintAS = true;
        break;
      }
      case "-pat" :
      {
        optionPrintAT = true;
        break;
      }
      case "-psmt" :
      {
        optionPrintSMT = true;
        break;
      }
      case "-pdec" :
      {
        if (i == n) 
        { 
          writer.println("option -pdec requires argument V");
          help();
          exit(false);
        }
        arg = args[i];
        i++;
        switch (arg)
        {
        case "0": optionPrintDec = Decomposer.VERBOSE_NONE; break;
        case "1": optionPrintDec = Decomposer.VERBOSE_LOW; break;
        case "2": optionPrintDec = Decomposer.VERBOSE_MEDIUM; break;
        case "3": optionPrintDec = Decomposer.VERBOSE_MAX; break;
        default:
        {
          writer.println("option -pdec does not support argument <" + arg + ">");
          writer.println("argument must be one of: 0 1 2 3");
          help();
          exit(false);
        }
        }
        break;
      }
      case "-psub" :
      {
        optionPrintSub = true;
        break;
      }
      case "-pcla" :
      {
        optionPrintCla = true;
        break;
      }
      case "-pproof" :
      {
        optionPrintProof = true;
        break;
      }
      case "-psearch" :
      {
        optionPrintSearch = true;
        break;
      }
      default:
      {
        if (arg.startsWith("-") || i < n) 
        {
          writer.println("Unknown command line option: " + arg);
          help();
          exit(false);
        }
        path = arg;
      }
      }
    }
  }
  
  /***************************************************************************
   * Converts string to integer.
   * @param arg the string
   * @return the integer (null, if none)
   **************************************************************************/
  public Integer toInteger(String arg)
  {
    try
    {
      return Integer.parseInt(arg);
    }
    catch(NumberFormatException e)
    {
      return null;
    }
  }
  
  /***************************************************************************
   * Read denoted file and return its contents.
   * @param file the file (null, if from standard input is to be read)
   * @return the lines of the file (null, if something went wrong; in
   *         this case a warning is printed).
   **************************************************************************/
  public List<String> readFile(File file)
  {
    if (file == null)
    {
      writer.println("Reading standard input...");
    }
    else
    {
      writer.println("Reading file " + file.getAbsolutePath() + "...");
    }
    try(BufferedReader fileReader = file == null ? reader : 
        new BufferedReader(new FileReader(file, CHAR_SET));)
    { 
      List<String> result = new ArrayList<String>();
      while (true)
      {
        String line = fileReader.readLine();
        if (line == null) return result;
        result.add(line);
      }
    }
    catch(IOException e)
    {
      error("could not read file " + e.getMessage(), false);
      return null;
    }
  }
  
  /***************************************************************************
   * Parse stream and returns its abstract syntax tree.
   * @param stream the stream with the input text.
   * @return the abstract syntax tree (null, if something went wrong)
   **************************************************************************/
  public Problem parseStream(InputStream stream)
  {
    try
    {
      CharStream input = CharStreams.fromStream(stream, CHAR_SET);
      RISCTPLexer lexer = new RISCTPLexer(input);
      TokenStream tokens = new CommonTokenStream(lexer);
      RISCTPParser parser = new RISCTPParser(tokens);
      parser.setErrorHandler(new ErrorStrategy(10000));
      RISCTPParser.ProblemContext context = parser.problem();
      TreeTranslator translator = new TreeTranslator();
      return (Problem)translator.visit(context);
    }
    catch(ParseCancellationException e)
    {
      error("syntax errors", false);
      return null;
    }
    catch(Exception e)
    {
      StringWriter w = new StringWriter();
      e.printStackTrace(new PrintWriter(w));
      error("parser error (" + w.toString() +")", false);
      return null;
    }
  }
  
  /***************************************************************************
   * Parse source and returns its abstract syntax tree.
   * @param source the text of the problem.
   * @return the abstract syntax tree (null, if something went wrong)
   **************************************************************************/
  private Problem parseSource(Source source)
  {
    ErrorListener listener = null;
    try
    {
      CodePointCharStream input = CharStreams.fromString(source.text);
      RISCTPLexer lexer = new RISCTPLexer(input);
      TokenStream tokens = new CommonTokenStream(lexer);
      RISCTPParser parser = new RISCTPParser(tokens);
      parser.setErrorHandler(new ErrorStrategy(10000));
      listener = new ErrorListener(source);
      parser.removeErrorListeners();
      parser.addErrorListener(listener);
      RISCTPParser.ProblemContext context = parser.problem();
      int errors = listener.getErrors();
      if (errors > 0)
      {
        error(errors + " syntax errors", false);
        return null;
      }
      TreeTranslator translator = new TreeTranslator();
      return (Problem)translator.visit(context);
    }
    catch(ParseCancellationException e)
    {
      if (listener == null)
        error("syntax errors", false);
      else
        error(listener.getErrors() + " syntax errors", false);
      return null;
    }
    catch(Exception e)
    {
      StringWriter w = new StringWriter();
      e.printStackTrace(new PrintWriter(w));
      error("parser error (" + w.toString() +")", false);
      return null;
    }
  }
  
  // the source code to be processed
  private class Source
  {
    public final List<String> lines; // the sequence of lines
    public final String text;        // its complete content
    public Source(List<String> lines, String text)
    {
      this.lines = lines;
      this.text = text;
    }
  }
  
  // get source denoted by path (stdin, if path is null)
  // returns null, if source could not be read
  private Source sourceFromFile(String path)
  {
    File file = path == null ? null : new File(path); 
    List<String> lines = readFile(file);
    if (lines == null) { return null; }
    StringBuilder builder = new StringBuilder();
    String eol = System.lineSeparator();
    for (String line : lines) builder.append(line).append(eol);
    String text = builder.toString();
    return new Source(lines, text);
  }

  // get source denoted by text
  private Source sourceFromText(String text)
  {
    String[] lines = text.split(System.lineSeparator());
    return new Source(new ArrayList<String>(Arrays.asList(lines)), text);
  }
  
  // prevent infinite error recovery loop (e.g. "for i:T=...")
  private class ErrorStrategy extends DefaultErrorStrategy
  {
    private int attempts;
    public ErrorStrategy(int attempts)
    {
      this.attempts = attempts;
    }
    public void sync(Parser recognizer) 
    { 
      attempts--;
      if (attempts < 0) throw new ParseCancellationException("");
    }
  }
  
  // the handling of parse errors
  private class ErrorListener extends BaseErrorListener
  {
    private Source source = null;
    private int errors = 0;
    private ErrorListener(Source source)
    {
      this.source = source;
      errors = 0;
    }
    public int getErrors() { return errors; }
    public void syntaxError(Recognizer<?,?> recognizer,
      Object offendingSymbol, int line, int charPositionInLine,
      String msg, RecognitionException e)
    {
      errors++;
      try { Thread.sleep(1); } catch(Exception ex) { }
      if (line >= 1 && line <= source.lines.size())
      {
        writer.println(source.lines.get(line-1));
        for (int i = 0; i < charPositionInLine; i++)
          writer.print(' ');
        writer.println('^');
      }
      writer.println(msg);
    }
  }
  
  /****************************************************************************
   * Print declarations generated from type-checking.
   * @param result the type-checking result.
   * @param header the header to be used for printing
   ***************************************************************************/
  private void printDeclarations(ProofProblem result, String header)
  {
    writer.println("=== " + header + ":");
    for (Decl decl : result.decls)
    {
      writer.println(decl);
    }
    writer.println("===");
  }
  
  /****************************************************************************
   * Print formulas (axioms and theorems).
   * @param result the type-checking result.
   * @param header the header to be used for printing
   ***************************************************************************/
  private void printFormulas(ProofProblem result, String header)
  {
    writer.println("=== " + header + ":");
    for (Decl decl : result.decls)
    {
      if (decl instanceof Axiom || decl instanceof Theorem)
        writer.println(decl);
    }
    writer.println("===");
  }
  
  /****************************************************************************
   * Determine symbols of all theorems to be proved.
   * @param the given proof problem.
   * @return true if all theorems could be determined.
   ***************************************************************************/
  private boolean determineTheorems(ProofProblem result)
  {
    theoremSymbols = new HashSet<FormulaSymbol>();
    Set<String> theoremNames0 = new HashSet<String>(theoremNames);
    for (Decl decl : result.decls)
    {
      if (decl instanceof Theorem)
      {
        Theorem theorem = (Theorem)decl;
        FormulaSymbol symbol = (FormulaSymbol)theorem.id.getSymbol();
        if (result.typeTheorems.contains(symbol)) continue;
        String name = symbol.id.toString();
        if (theoremAll || theoremNames0.contains(name))
        {
          theoremSymbols.add(symbol);
          if (!theoremAll) theoremNames0.remove(name);
        }
      }
    }
    if (theoremAll || theoremNames0.isEmpty()) return true;
    StringBuilder builder = new StringBuilder();
    builder.append("unknown theorems: ");
    for (String name : theoremNames0)
    {
      builder.append(name);
      builder.append(" ");
    }
    writer.println(builder.toString());
    return false;
  }
}
// ----------------------------------------------------------------------------
// end of file
// ----------------------------------------------------------------------------
