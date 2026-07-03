// ---------------------------------------------------------------------------
// ProofProblemHandler.java
// A resource handler for the presentation of a proof problem.
// $Id: ProofProblemHandler.java,v 1.66 2026/04/14 10:46:24 schreine Exp $
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
package risctp.web;

import risctp.*;
import risctp.fol.Decomposer.*;
import risctp.syntax.AST.*;
import risctp.syntax.AST.Decl.*;
import risctp.syntax.AST.Exp.*;
import risctp.problem.*;
import risctp.fol.*;

import java.util.*;
import java.util.function.*;
import java.io.*;
import java.nio.*;
import java.nio.charset.*;

import org.w3c.dom.*;

public class ProofProblemHandler extends FragmentHandler 
{
  // content of root document, stored in same directory as this file
  private final static String PATH = "risctp/web/ProofProblem.html";
  
  // the root document
  private Document root;
  
  // ids of document elements referenced by the handler
  private static final String ID_OUTPUT = "output";
  private static final String ID_PROBLEM = "problem";
  private static final String ID_DISPLAY = "display";
  private static final String ID_TREELIST = "treelist";
  private static final String ID_PROBLEMS = "problems";
  private static final String ID_OPROBLEMS = "oproblems";
  private static final String ID_CPROBLEMS = "cproblems";
  
  // these may be used in newTree()
  public static final String ID_PSEARCH = "psearch";
  public static final String ID_PROOFS = "proofs";
  
  // the executor for the "run" service
  private Executor executor;

  // a map of display names to corresponding writers
  private Map<String,PrintWriter> writers;
  
  // an executor for the given handler and parameters
  public static interface Executor 
  extends BiConsumer<ProofProblemHandler, Map<String,String>>
  { }
  
  // set if execution of "run" service is to be aborted
  private boolean aborted = false;
  
  // the current instance of the main program
  private Main main = null;
  
  /**************************************************************************
   * Create handler for decomposition of proof problem.
   * @param main the current instance of the main program.
   * @param browse true if browse button is to be displayed
   * @param buttons true if the other buttons are to be displayed
   * @param executor the executor for the "run" service (may be null).
   *************************************************************************/
  public ProofProblemHandler(Main main, boolean browse, boolean buttons, Executor executor)
  {
    this.main = main;
    root = DOM.readDocument(PATH);
    if (!browse)
    {
      Element file = root.getElementById("file");
      file.setAttribute("style", "visibility:hidden");
      root.getElementById("axints").setAttribute("disabled", "true");
      root.getElementById("axnonlinear").setAttribute("disabled", "true");
      root.getElementById("axmaps").setAttribute("disabled", "true");
      root.getElementById("axdata").setAttribute("disabled", "true");
    }
    if (!buttons)
    {
      Element top2 = root.getElementById("top2");
      top2.setAttribute("style", "visibility:hidden");
      Element top3 = root.getElementById("top3");
      top3.setAttribute("style", "visibility:hidden");
      Element script = root.createElement("script");
      script.setTextContent("getNext()");
    }
    this.executor = executor;
    writers = new HashMap<String,PrintWriter>();
    writers.put(ID_OUTPUT, new PrintWriter(new ProofProblemStream(ID_OUTPUT), true));
    writers.put(ID_PROBLEM, new PrintWriter(new ProofProblemStream(ID_PROBLEM), true));
  }

  /**************************************************************************
   * Respond to request to denoted service.
   * @param params a mapping of parameter names to parameter values.
   * @param out the stream to write the response to.
   *************************************************************************/
  public void handle(String service, Map<String,String> params, PrintWriter out)
  {
    switch (service)
    {
    case "/": 
      DOM.printDocument(root, true, out);
      break;
    case "/run":
      DOM.printDocument(DOM.createDocument("html"), true, out);
      init();
      clearAll();
      aborted = false;
      if (executor != null) 
      {
        Thread thread = new Thread(() -> { executor.accept(this, params); });
        thread.start();
      }
      break;
    case "/next":
      super.handle(service, params, out);
      break;
    case "/abort":
      DOM.printDocument(DOM.createDocument("html"), true, out);
      aborted = true;
      break;
    case "/options":
      DOM.printDocument(createOptionDocument(), false, out);
      break;
    case "/file":
      String text = Main.getFileContent();
      if (text == null) text = "";
      out.print(text);
      break;
    }
  }

  /****************************************************************************
   * Create document describing the RISCTP options.
   * @return the document
   ***************************************************************************/
  private Document createOptionDocument()
  {
    Document document = DOM.createDocument("options");
    createOption(document, "mode", Integer.toString(main.getMode()));
    createOption(document, "method", Integer.toString(main.getMethod()));
    createOption(document, "timeout", Integer.toString(main.getTimeout()));
    createOption(document, "multithreaded", Boolean.toString(main.getOptionMEthreads() >= 2));
    createOption(document, "threads", Integer.toString(Integer.max(2, main.getOptionMEthreads())));
    createOption(document, "expand", Boolean.toString(main.getExpand()));
    createOption(document, "intAxioms", Boolean.toString(main.getIntAxioms()));
    createOption(document, "nonLinearAxioms", Boolean.toString(main.getNonLinearAxioms()));
    createOption(document, "mapAxioms", Boolean.toString(main.getMapAxioms()));
    createOption(document, "dataAxioms", Boolean.toString(main.getDataAxioms()));
    createOption(document, "mesonEquality", Integer.toString(main.getMesonEquality()));
    createOption(document, "mesonSMT", Integer.toString(main.getMesonSMT()));
    createOption(document, "mesonDisplay", Integer.toString(main.getMesonDisplay()));
    createOption(document, "mesonDepth", Boolean.toString(main.getMesonDepth()));
    createOption(document, "mesonLimit", Integer.toString(main.getMesonLimit()));
    createOption(document, "mesonIterate", Boolean.toString(main.getMesonIterate()));
    createOption(document, "mesonSingle", Boolean.toString(main.getMesonSingle()));
    createOption(document, "resSMT", Boolean.toString(main.getResSMT()));
    return document;
  }
  
  /****************************************************************************
   * Create an option element in the given document.
   * @param document the document.
   * @param name the name of the option.
   * @param value its value.
   ***************************************************************************/
  private void createOption(Document document, String name, String value)
  {
    Element root = document.getDocumentElement();
    Element option = document.createElement("option");
    root.appendChild(option);
    option.setAttribute("id", "option_" + name);
    option.setAttribute("value", value);
  }
  
  /****************************************************************************
   * Clear all user interface elements.
   ***************************************************************************/
  public void clearAll()
  {
    clear(ID_OUTPUT); 
    clear(ID_PROBLEM); 
    clear(ID_DISPLAY); 
    clear(ID_TREELIST);
    clear(ID_PROBLEMS);
    clear(ID_OPROBLEMS);
    clear(ID_CPROBLEMS);
    clear(ID_PSEARCH);
    clear(ID_PROOFS);
    show(ID_DISPLAY, "Click in the left pane to inspect the proof (even while the prover is still running).");
  }
  
  /***************************************************************************
   * Clear user interface element.
   * @param id the name of the element
   **************************************************************************/
  public void clear(String id)
  {
    replaceChildren(id, (Document document)->
    {
      // prevent transformer to create self-closing tags of form <tag/> 
      // these are not correctly parsed by browser (Firefox)
      Element element = document.createElement("div");
      element.setAttribute("hidden", "");
      Text text = document.createTextNode("dummy");
      element.appendChild(text);
      return element;
    });
  }
  
  /***************************************************************************
   * Add output to user interface element.
   * @param id the name of the element
   * @param string the text of the output.
   **************************************************************************/
  public void show(String id, String string)
  {
    appendChild(id, (Document document)->
    {
      Text text = document.createTextNode(string);
      return text;
    });
  }

  // get abortion status
  public boolean isAborted() { return aborted; }
  
  /****************************************************************************
   * Get writer for denoted display name (null, if none).
   ***************************************************************************/
  public PrintWriter getWriter(String display)
  {
    return writers.get(display);
  }
  
  // the stream underlying the output writer
  private class ProofProblemStream extends OutputStream
  {
    private CharsetDecoder decoder = Main.CHAR_SET.newDecoder(); 
    private String id;
    public ProofProblemStream(String id) { this.id = id; }
    private final String toString(byte[] b, int offset, int length)
    {
      try
      {
        ByteBuffer byteBuffer = ByteBuffer.wrap(b, offset, length);
        CharBuffer charBuffer = decoder.decode(byteBuffer);    
        String string = charBuffer.toString();
        return string;
      }
      catch(CharacterCodingException e)
      {
        System.out.println("error in character decoding");
        return "";
      }
    }
    public void write(byte[] b, int offset, int length)
    {
      String string = toString(b, offset, length);
      show(id, string);
    }
    public void write(byte[] b)
    {
      write(b, 0, b.length);
    }
    public void write(int b)
    {
      byte[] a = new byte[1];
      a[0] = (byte)b;
      write(a);
    }
  }

  // -------------------------------------------------------------------------
  //
  // Decomposer Proof Trees
  //
  // -------------------------------------------------------------------------
  
  // the roots of the proof trees
  private Set<ProofNode> roots = new HashSet<ProofNode>();
  
  /***************************************************************************
   * Add (root of) a proof tree.
   * @param tree the proof tree.
   **************************************************************************/
  public void appendProofTree(ProofTree tree)
  {
    appendChild(ID_TREELIST, (Document document)->
    {
      ProofNode root = tree.getRoot();
      roots.add(root);
      Element node = createProofNode(document, root, false, false, true);
      return node;
    });
  }
  
  /***************************************************************************
   * Add children of a node to the proof.
   * @param parent the parent of the children to be added.
   **************************************************************************/
  public void appendChildren(ProofNode parent)
  {
    String parentId = nodeIds.get(parent);
    String dtId = parentId + "_dt";
    String aId = parentId + "_a";
    String spanId = parentId + "_span";
    String dlId = parentId + "_dl";
    String textId = parentId + "_text";
    replaceChildren(dtId, (Document document)->
    {
      Element dt = document.createElement("span");
      Element a = document.createElement("a");
      dt.appendChild(a);
      dt.setAttribute("class", "tree");
      a.setAttribute("id", aId);
      a.setAttribute("class", "tag");
      a.setAttribute("onclick", "hide('" + aId + "','" + dlId + "');");
      if (roots.contains(parent))
        a.setTextContent("[+]");
      else
        a.setTextContent("[\u2011]");
      dt.appendChild(document.createTextNode("\u00A0"));
      Element span = document.createElement("span");
      dt.appendChild(span);
      span.setAttribute("id", spanId);
      span.setAttribute("class", "link");
      span.setAttribute("onclick", "show('" + textId + "',false);visitLink(this);");
      span.setTextContent(getName(parent));
      dt.appendChild(document.createTextNode(" (" + ruleText(parent) + ")"));
      return dt;
    });
    appendChild(dlId, (Document document)->
    {
      Element div = document.createElement("div");
      List<ProofNode> children = parent.getChildren();
      for (ProofNode child : children)
      {
        Element element = createProofNode(document, child, true, false, true);
        div.appendChild(element);
      }
      if (children.isEmpty())
      {
        Element dummy = document.createElement("pre");
        div.appendChild(dummy);
        dummy.setTextContent("dummy");
        dummy.setAttribute("hidden", "");
      }
      return div;
    });
  }
  
  /****************************************************************************
   * Return text for rule applied to parent.
   * @param parent a node to which some rule has been applied.
   * @return the rule text.
   ***************************************************************************/
  private static String ruleText(ProofNode parent)
  {
    return parent.getRuleText();
  }
  
  /****************************************************************************
   * Get name of proof node.
   * @param node the proof node.
   * @return its name.
   ***************************************************************************/
  private static String getName(ProofNode node)
  {
    /*
    Formula goal = node.getSequent().getGoal();
    if (goal == null) return "(null)";
    return goal.getName();
    */
    return node.getName();
  }
  
  // number of proof nodes added to document
  private int nodeCounter = 0;
  
  // the map of proof nodes to element ids
  private Map<ProofNode,String> nodeIds = new HashMap<ProofNode,String>();
  
  /***************************************************************************
   * Create the DOM representation of a proof node.
   * @param document the document.
   * @param node the proof node.
   * @param expanded true if the node is expanded
   * @param standalone true if the node is to be shown standalone
   * @param hide true if universally quantified axioms are hidden
   * @return the DOM element.
   **************************************************************************/
  private Element createProofNode(Document document, ProofNode node, 
    boolean expanded, boolean standalone, boolean hide)
  {
    int counter = nodeCounter;
    nodeCounter++;
    String nodeId = "node_" + counter; 
    String aId = nodeId + "_a";
    String dtId = nodeId + "_dt";
    String ddId = nodeId + "_dd";
    String spanId = nodeId + "_span";
    String textId = nodeId + "_text";
    String dlId = nodeId + "_dl";
    nodeIds.put(node, nodeId);
    Element div = document.createElement("div");
    div.setAttribute("id", nodeId);
    Element dt = document.createElement("dt");
    div.appendChild(dt);
    dt.setAttribute("id", dtId);
    dt.setAttribute("class", "tree");
    if (!standalone)
    {
      Element a = document.createElement("a");
      dt.appendChild(a);
      a.setAttribute("id", aId);
      a.setAttribute("class", "tag");
      // a.setAttribute("onclick", "hide('" + aId + "','" + dlId + "');");
      a.setTextContent("[\u00A0]");
    }
    dt.appendChild(document.createTextNode("\u00A0"));
    Element span = document.createElement("span");
    dt.appendChild(span);
    span.setAttribute("id", spanId);
    span.setAttribute("class", "link");
    span.setAttribute("onclick", "show('" + textId + "',true);visitLink(this);");
    span.setTextContent(getName(node));
    if (!standalone) dt.appendChild(document.createTextNode(" (open)"));
    Element dd = document.createElement("dd");
    div.appendChild(dd);
    dd.setAttribute("id", ddId);
    dd.setAttribute("class", "tree");
    Element text = document.createElement("text");
    dd.appendChild(text);
    text.setAttribute("id", textId);
    text.setAttribute("hidden", "");
    if (hide)
    {
      text.appendChild(document.createTextNode("(We hide universally quantified knowledge)"));
      text.appendChild(document.createElement("br"));
      text.appendChild(document.createElement("br"));
    }
    Sequent sequent = node.getSequent();
    List<Formula> formulas = sequent.getFormulas();
    Formula goal = sequent.getGoal();
    int i = 0;
    for (Formula formula : formulas)
    {
      if (formula == goal) continue;
      i++;
      if (hide && formula.getExp() instanceof Forall && formula.getNegated()) continue;
      Element line = document.createElement("span");
      text.appendChild(line);
      line.setAttribute("class", "formula");
      Element label = createLabel(document, i + ":[" + formula.getName() + "] ");
      line.appendChild(label);
      Exp exp = formula.getExp();
      boolean negated = !formula.getNegated();
      if (negated) exp = exp instanceof True ? False.create() : 
        exp instanceof False ? True.create() : Not.create(exp);
      line.appendChild(DOM.colorizedText(document, exp.toString()));
      if (formula.isImplied()) line.appendChild(document.createTextNode(" {implied}"));
      text.appendChild(document.createElement("br"));
    }
    text.appendChild(document.createElement("hr"));
    Element line = document.createElement("span");
    text.appendChild(line);
    line.setAttribute("class", "formula");
    Element label = createLabel(document,
        goal == null ? "goal: " : "goal:[" + goal.getName() + "] ");
    line.appendChild(label);
    if (goal != null)
    {
      Exp exp = goal.getExp();
      boolean negated = goal.getNegated();
      if (negated) exp = Not.create(exp);
      line.appendChild(DOM.colorizedText(document, exp.toString()));
      if (goal.isImplied()) line.appendChild(document.createTextNode(" {implied}"));
    }
    line.appendChild(document.createElement("br"));
    Element dl = document.createElement("dl");
    dd.appendChild(dl);
    dl.setAttribute("id", dlId);
    if (!expanded) dl.setAttribute("hidden", "");
    return div;
  }
  
  /***************************************************************************
   * Create a colored label element.
   * @param document the document.
   * @param key the text for the label.
   * @return the element.
   **************************************************************************/
  private Element createLabel(Document document, String key)
  {
    Element kspan = document.createElement("span");
    kspan.setAttribute("style", "color:purple");
    Text ktext = document.createTextNode(key);
    kspan.appendChild(ktext);
    return kspan;
  }
  
  // -------------------------------------------------------------------------
  //
  // Open Proof Problem Nodes
  //
  // -------------------------------------------------------------------------
  
  /***************************************************************************
   * Add an open proof node
   * @param tree the proof tree.
   **************************************************************************/
  public void appendOpenNode(ProofNode node)
  {
    appendChild(ID_OPROBLEMS, (Document document)->
    {
      Element li = document.createElement("li");
      Element dl = document.createElement("dl");
      Element element = createProofNode(document, node, true, true, false);
      li.appendChild(dl);
      dl.appendChild(element);
      return li;
    });
  }

  // -------------------------------------------------------------------------
  //
  // Proof Problems
  //
  // -------------------------------------------------------------------------
  
  // counter for proof problems
  private int problems = 0;
  
  /***************************************************************************
   * Add a proof problem.
   * @param problem the proof problem.
   **************************************************************************/
  public void appendProofProblem(ProofProblem problem)
  {
    int counter = problems;
    problems++;
    String problemId = "problem_" + counter;
    String aId = problemId + "_a";
    String preId = problemId + "_pre";
    appendChild(ID_PROBLEMS, (Document document)->
    {
      Element div = document.createElement("div");
      Element dt = document.createElement("dt");
      div.appendChild(dt);
      dt.setAttribute("class", "problem");
      Element a = document.createElement("a");
      dt.appendChild(a);
      a.setAttribute("id", aId);
      a.setAttribute("class", "link");
      String name = problem.getName();
      if (name == null) name = getTheoremName(problem);
      a.setTextContent(name);
      a.setAttribute("onclick", "showOuter('" + preId + "',true);visitLink(this);");
      Element dd = document.createElement("dd");
      div.appendChild(dd);
      dd.setAttribute("hidden", "");
      Element pre = document.createElement("pre");
      dd.appendChild(pre);
      pre.setAttribute("id", preId);
      StringBuilder builder = new StringBuilder();
      for (Decl decl : problem.decls) 
      {
        builder.append(decl.toString());
        builder.append('\n');
      }
      pre.setTextContent(builder.toString());
      return div;
    });
  }
  
  /****************************************************************************
   * Get name of theorem in problem.
   * @param problem the problem.
   * @return the name of the last theorem in the problem (usually the
   * most important one, if there are still multiple ones).
   ***************************************************************************/
  private static String getTheoremName(ProofProblem problem)
  {
    String result = "(no theorem)";
    for (Decl decl : problem.decls)
    { 
      if (decl instanceof Theorem)
      {
        Theorem theorem = (Theorem)decl;
        result = theorem.id.toString();
      }
    }
    return result;
  }

  // -------------------------------------------------------------------------
  //
  // Clause Problems
  //
  // -------------------------------------------------------------------------
  
  // counter for clause problems
  private int cproblems = 0;
  
  /***************************************************************************
   * Add a clause problem.
   * @param cproblem the clause problem.
   **************************************************************************/
  public void appendClauseProblem(ClauseProblem cproblem)
  {
    int counter = cproblems;
    cproblems++;
    String cproblemId = "cproblem_" + counter;
    String aId = cproblemId + "_a";
    String textId = cproblemId + "_text";
    appendChild(ID_CPROBLEMS, (Document document)->
    {
      Element li = document.createElement("li");
      Element dl = document.createElement("dl");
      Element dt = document.createElement("dt");
      li.appendChild(dl);
      dl.appendChild(dt);
      dt.setAttribute("class", "problem");
      Element a = document.createElement("a");
      dt.appendChild(a);
      a.setAttribute("id", aId);
      a.setAttribute("class", "link");
      ProofProblem problem = cproblem.getProofProblem();
      String name = problem.getName();
      if (name == null) name = getTheoremName(problem);
      a.setTextContent(name);
      a.setAttribute("onclick", "show('" + textId + "',true);visitLink(this);");
      Element dd = document.createElement("dd");
      dl.appendChild(dd);
      dd.setAttribute("hidden", "");
      Element text = document.createElement("text");
      dd.appendChild(text);
      text.setAttribute("id", textId);
      /*
      text.appendChild(document.createTextNode("(We hide universally quantified axioms)"));
      text.appendChild(document.createElement("br"));
      */
      List<Clause> clauses = cproblem.getClauses();
      int i = 0;
      for (Clause clause : clauses)
      {
        if (clause.isGoal()) continue;
        i++;
        // if (clause.getTypedVars().size() > 0) continue;
        Element line = document.createElement("span");
        text.appendChild(line);
        line.setAttribute("class", "formula");
        Element label = createLabel(document, i + ":[" + clause.getName() + "] ");
        line.appendChild(label);
        String formula = clause.getString(false, 
            Clause.PrefixFormat.TYPES, Clause.MatrixFormat.IMPLICATION);
        line.appendChild(DOM.colorizedText(document, formula));
        text.appendChild(document.createElement("br"));
      }
      text.appendChild(document.createElement("hr"));
      i = 1;
      for (Clause clause : clauses)
      {
        if (!clause.isGoal()) continue;
        Element line = document.createElement("span");
        text.appendChild(line);
        line.setAttribute("class", "formula");
        Element label = createLabel(document, i + ":[" + clause.getName() + "] ");
        line.appendChild(label);
        String formula = clause.getString(false, 
            Clause.PrefixFormat.TYPES, Clause.MatrixFormat.IMPLICATION);
        line.appendChild(DOM.colorizedText(document, formula));
        text.appendChild(document.createElement("br"));
        i++;
      }
      return li;
    });
  }
  
  // -------------------------------------------------------------------------
  //
  // Model Elimination Proof Trees
  //
  // -------------------------------------------------------------------------

  // number of proof nodes added to document
  private int meCounter = 0;
  
  // the id of the proof tree
  private String ID_TREE;
  
  // the root nodes
  private Set<risctp.me.ProofTree.Node> meRoots;

  // the map of proof nodes to element ids
  private Map<risctp.me.ProofTree.Node,String> meIds;
  
  /****************************************************************************
   * Indicate the creation of a new proof tree.
   * @param id the document id to which the tree is to be added.
   ***************************************************************************/
  public void newTree(String id)
  {
    ID_TREE = id;
    meRoots = new HashSet<risctp.me.ProofTree.Node>();
    meIds = new HashMap<risctp.me.ProofTree.Node,String>();
  }
  
  /****************************************************************************
   * Add a new proof node.
   * @param node the node.
   ***************************************************************************/
  public void newNode(risctp.me.ProofTree.Node node)
  {
    risctp.me.ProofTree.Node parent = node.getParent();
    String parentId;
    if (parent == null) 
    {
      meRoots.add(node);
      parentId = ID_TREE;
      appendChild(parentId, (Document document)->
      {
        Element li = document.createElement("li");
        Element dl = document.createElement("dl");
        Element elem = createNode(document, node, parent != null);
        li.appendChild(dl);
        dl.appendChild(elem);
        return li;
      });
    }
    else
    {
      parentId = meIds.get(parent);
      parentId = parentId + "_dl";
      appendChild(parentId, (Document document)->
      {
        return createNode(document, node, parent != null);
      });
    }
  }
  
  /***************************************************************************
   * Create the DOM representation of a proof node.
   * @param document the document.
   * @param node the proof node.
   * @param expanded true if the node is expanded
   * @return the DOM element.
   **************************************************************************/
  private Element createNode(Document document, 
    risctp.me.ProofTree.Node node, boolean expanded)
  {
    int counter = meCounter;
    meCounter++;
    String nodeId = ID_TREE + "meNode_" + counter; 
    String aId = nodeId + "_a";
    String dtId = nodeId + "_dt";
    String statusId = nodeId + "_status";
    String ddId = nodeId + "_dd";
    String spanId = nodeId + "_span";
    String textId = nodeId + "_text";
    String dlId = nodeId + "_dl";
    meIds.put(node, nodeId);
    Element div = document.createElement("div");
    div.setAttribute("id", nodeId);
    Element dt = document.createElement("dt");
    div.appendChild(dt);
    dt.setAttribute("id", dtId);
    dt.setAttribute("class", "tree");
    Element a = document.createElement("a");
    dt.appendChild(a);
    a.setAttribute("id", aId);
    a.setAttribute("class", "tag");
    a.setAttribute("onclick", "hide('" + aId + "','" + dlId + "');");
    if (meRoots.contains(node))
      a.setTextContent("[+]");
    else 
      a.setTextContent("[\u2011]");
    dt.appendChild(document.createTextNode("\u00A0"));
    Element span = document.createElement("span");
    dt.appendChild(span);
    span.setAttribute("id", spanId);
    span.setAttribute("class", "link");
    span.setAttribute("onclick", "show('" + textId + "',false);visitLink(this);");
    span.setTextContent(node.getName());
    Element status = document.createElement("span");
    dt.appendChild(status);
    status.setAttribute("id", statusId);
    status.appendChild(document.createTextNode(" (open)"));
    Element dd = document.createElement("dd");
    div.appendChild(dd);
    dd.setAttribute("id", ddId);
    dd.setAttribute("class", "tree");
    Element text = document.createElement("text");
    dd.appendChild(text);
    text.setAttribute("id", textId);
    text.setAttribute("hidden", "");
    Text dummy = document.createTextNode("dummy");
    text.appendChild(dummy);
    Element dl = document.createElement("dl");
    dd.appendChild(dl);
    dl.setAttribute("id", dlId);
    if (!expanded) dl.setAttribute("hidden", "");
    Element dummy2 = document.createElement("pre");
    dl.appendChild(dummy2);
    dummy2.setTextContent("dummy2");
    dummy2.setAttribute("hidden", "");
    return div;
  }
  
  /****************************************************************************
   * Update a proof node.
   * @param node the node.
   ***************************************************************************/
  public void updateNode(risctp.me.ProofTree.Node node)
  {
    String nodeId = meIds.get(node);
    String textId = nodeId + "_text";
    String preId = nodeId + "_pre";
    replaceChildren(textId, (Document document)->
    {
      Element pre = document.createElement("pre");
      pre.setAttribute("id", preId);
      // pre.setTextContent(node.getText());
      String str = node.getText();
      int eol = str.indexOf('\n');
      if (eol != -1)
      {
        String line = str.substring(0, eol);
        str = str.substring(eol+1);
        Element span = document.createElement("span");
        span.setAttribute("style", "font-family:sans-serif;font-weight:bold");
        Text text = document.createTextNode(line);
        span.appendChild(text);
        pre.appendChild(span);
        Element hr = document.createElement("hr");
        hr.setAttribute("style", "margin-left:0;margin-bottom:0;width:75%");
        pre.appendChild(hr);
      }
      Element text = DOM.colorizedText(document, str);
      pre.appendChild(text);
      return pre;
    });
  }
  
  /****************************************************************************
   * Finalize a proof node.
   * @param node the node.
   ***************************************************************************/
  public void finalizeNode(risctp.me.ProofTree.Node node)
  {
    String nodeId = meIds.get(node);
    String statusId = nodeId + "_status";
    replaceChildren(statusId, (Document document)->
    {
      Element span = document.createElement("span");
      if (node.isClosed())
      {
        span.appendChild(document.createTextNode("\u00A0(success)"));
        span.setAttribute("style", "color:darkgreen");
      }
      else
      {
        span.appendChild(document.createTextNode("\u00A0(failure)"));
        span.setAttribute("style", "color:darkorange");
      }
      return span;
    });
  }
}
// ----------------------------------------------------------------------------
// end of file
// ----------------------------------------------------------------------------