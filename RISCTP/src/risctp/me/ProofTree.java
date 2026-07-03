// ---------------------------------------------------------------------------
// ProofTree.java
// A model elimination proof tree.
// $Id: ProofTree.java,v 1.95 2024/11/27 14:51:54 schreine Exp $
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

import risctp.fol.*;
import risctp.types.Symbol.*;
import risctp.me.ModelElimination.Literal;
import risctp.me.ModelElimination.Context;

public class ProofTree
{
  // the maximum number of nodes (0 if no limit) in tree and the current number,
  private static int MAX_NODE_NUMBER = 0;
  private int nodeNumber;
  
  // the name
  private String name;
  
  // the output medium (may be null)
  private PrintWriter out;
  
  // is also proof search tree to be published?
  private boolean search;
  
  // if not null, method to be invoked on newly created/updated/finalized proof node
  private Consumer<Node> newNode;
  private Consumer<Node> updateNode;
  private Consumer<Node> finalizeNode;
  
  // are tree nodes to be printed respectively annotated with text?
  private boolean print;
  private boolean annotate;
  
  // the current proof complexity measure
  private ModelElimination.Measure measure = null;
  public void setMeasure(ModelElimination.Measure measure) { this.measure = measure; }
  
  // the list of root nodes
  private List<Node> roots;
  
  // a stack of proof nodes (and its alternative for the pruned tree)
  private Stack<Node> nodeStack;
  private Stack<Node> prunedStack;

  /***************************************************************************
   * Create a new proof tree.
   * @param name the name of the tree
   * @param out the output medium (null, if no output is to be produced).
   * @param search true if also the proof search tree is to be published.
   * @param print true if the tree nodes are to be printed.
   * @param annotate true if the tree nodes are to be annotated with text.
   * @param newNode if not null, method to be invoked on every new node of this tree.
   * @param updateNode if not null, method to be invoked on every updated node.
   * @param finalizeNode if not null, method to be invoked on every finalized node.
   * The other parameters denote the relevant prover state.
   **************************************************************************/
  public ProofTree(String name, PrintWriter out, boolean search,
    boolean print, boolean annotate,
    Consumer<Node> newNode, Consumer<Node> updateNode, Consumer<Node> finalizeNode)
  {
    this.name = name;
    this.out = out;
    this.search = search;
    this.print = print;
    this.annotate = annotate;
    this.newNode = newNode;
    this.updateNode = updateNode;
    this.finalizeNode = finalizeNode;
    this.roots = new ArrayList<Node>();
    this.nodeStack = new Stack<ProofTree.Node>();
    this.prunedStack = new Stack<ProofTree.Node>();
    this.nodeNumber = 0;
  }

  // get all roots of a proof tree (or the expected single root)
  public List<Node> getRoots() { return roots; }
  public Node getRoot() { return roots.get(0); }
  
  // push/pop a node to/from the stack
  public void push(Node node) { nodeStack.push(node); prunedStack.push(node);}
  public void pop() { nodeStack.pop(); prunedStack.pop(); }

  // alternatives for pruned stack
  public void push(Node node, Node node2)
  {
    nodeStack.push(node);
    prunedStack.push(node2);
  }

  // pop up to certain level of tree
  public void pop(Node node)
  {
    while (!nodeStack.isEmpty() && nodeStack.top() != node) nodeStack.pop();
    while (!nodeStack.isEmpty() && prunedStack.top() != node) prunedStack.pop();
  }

  // deal with iterative deepening iteration numbers
  private boolean isIterationNode() { return nodeStack.size() == 3; }
  
  /****************************************************************************
   * Print outline of this tree.
   ***************************************************************************/
  public void printOutline()
  {
    for (Node root : roots)
      root.printOutline(out, 0);
  }
  
  /****************************************************************************
   * Print complete text of this tree.
   ***************************************************************************/
  public void printText()
  {
    for (Node root : roots)
      root.printText(out);
  }
  
  /****************************************************************************
   * Create new node.
   * @param name the name of the node.
   * @param type the type of the node.
   * @return the node (a dummy node, if the maximum number of nodes is exceeded).
   ***************************************************************************/
  private Node createNode(String name, Type type)
  {
    if (MAX_NODE_NUMBER > 0 && nodeNumber > MAX_NODE_NUMBER) 
    {
      throw new ModelElimination.AbortedException("Maximum size " + 
          MAX_NODE_NUMBER + " of the proof search tree has been exceeded\n" +
          "(you may retry the proof without creating the search proof tree).", false);
    }
    nodeNumber++;
    return new Node(name, type);
  }

  // the type of a proof node
  // LEAF: a leaf node with a fixed closed status
  // AND: a node that gets closed if every child is closed
  // OR: a node that gets closed if some child is closed
  private enum Type { LEAF, AND, OR }

  //--------------------------------------------------------------------------
  //
  // a node of the proof tree
  //
  // --------------------------------------------------------------------------
  public class Node
  { 
    // the parent of this node (null, if the node is a root)
    private Node parent;

    // the children of this node (only for type AND and OR)
    private List<Node> children;

    // the children of this node in the pruned tree (may be different from children)
    private List<Node> prunedChildren;
    
    // the name of the node
    private String name;

    // the type of this node
    private Type type;

    // is the node finalized?
    private boolean finalized;
    
    // is the node closed?
    private boolean closed;

    // the text associated to this node
    private String text;

    // create a node whose parent is the top node on the stack
    private Node(String name, Type type)
    {    
      // record this node appropriately
      Node parent = nodeStack.top();
      Node prunedParent = prunedStack.top();
      if (parent == null) 
        roots.add(this);
      else
      {
        // prune tree immediately to save space
        if (parent.type == Type.OR) 
        {
          parent.children.clear();
          if (!search) prunedParent.prunedChildren.clear();
        }
        parent.children.add(this);
        prunedParent.prunedChildren.add(this);
      }
      
      // set the remaining node state
      this.name = name; 
      this.type = type; 
      this.finalized = false;
      this.closed = false;
      this.text = "";
      this.parent = parent;
      this.children = type == Type.LEAF ? null : new ArrayList<Node>();
      this.prunedChildren = type == Type.LEAF ? null : new ArrayList<Node>();
      
      // invoked handler, if any
      if (search && newNode != null) newNode.accept(this);
    }

    // get the elements of this node
    public String getName() { return name; }
    public Type getType() { return type; }
    public boolean isClosed() { return closed; }
    public Node getParent() { return parent; }
    public List<Node> getChildren() { return children; }

    // get text representation of the index sequence of this node
    private String key = null;
    public String getKey()
    {
      if (key != null) return key;
      if (parent == null) { key = ""; return key; }
      String base = parent.getKey();
      int number = 1;
      for (Node child : parent.getChildren())
      {
        if (child == this) break;
        number++;
      }
      key = base + (base.equals("") ? "" : ".") + number;
      return key;
    }

    // get the text representation of this node (cleared for a finalized node)
    public String getText() 
    {
      String result = text;
      if (finalized) text = null;
      return result;
    }
    
    /****************************************************************************
     * Write text into this node.
     * @param add true if text is to be added (otherwise, it is replaced).
     * @param printer the consumer invoked on the output medium to produce the text.
     ***************************************************************************/
    private void setText(boolean add, Consumer<PrintWriter> printer)
    {
      StringWriter writer = new StringWriter();
      printer.accept(new PrintWriter(writer, true));
      String string = writer.toString();
      if (annotate)
      {
        if (add) text += string; else text = string;
      }
      if (print && out != null) 
      {
        out.print(string);
        out.println("----------------------------------------------------------------------");
      }
      if (search && updateNode != null) updateNode.accept(this);
    }
    
    /**************************************************************************
     * Finalize a proof node with the given substitution.
     * @param closed true if the proof node is closed.
     * @param subst the closing substitution (may be null)
     *************************************************************************/
    public void finalize(boolean closed, Subst subst) 
    {
      this.closed = closed;
      String message;
      if (parent == null)
      {
        if (closed)
        {
          if (subst == null)
            message = "SUCCESS: the proof has been completed.";
          else
            message = "SUCCESS: the proof has been completed with the following substitution:";
        }
        else
          message = "FAILURE: the proof has NOT been completed.";
      }
      else if (closed)
      {
        if (subst == null)
          message = "SUCCESS: goal " + name + " has been proved.";
        else
          message = "SUCCESS: goal " + name + " has been proved with the following substitution:";
      }
      else
        message = "FAILURE: goal " + name + " has NOT been proved.";
      setText(true, (PrintWriter out)->
      {
        out.println("------------------------------------------------------------------------------------------");
        out.println(message);
        if (closed && subst != null)
        {
          out.println();
          Set<VariableSymbol> vars = subst.keySet();
          for (VariableSymbol var : vars)
          {
            Object term = subst.get(var);
            if (term == null) continue;
            // resolve all variables in term
            term = subst.apply(term);
            out.println("  " + var.id + " → " + Term.toString(term));
          }
        }
      });
      if (search && finalizeNode != null) finalizeNode.accept(this);
    }

    /**************************************************************************
     * Print outline of subtree below this node with the given indentation level
     * @param out the output medium.
     * @param level the indentation level.
     *************************************************************************/
    public void printOutline(PrintWriter out, int level)
    {
      indent(out, level);
      out.print("[" + getKey() + "] ");
      out.println(name);
      if (children != null)
      {
        int n = children.size();
        for (int i = 0; i < n; i++)
        {
          Node child = children.get(i);
          child.printOutline(out, level+1);
        }
      }
    }

    /**************************************************************************
     * Indent output at the given indentation level.
     * @param out the output medium.
     * @param level the indentation level.
     *************************************************************************/
    private void indent(PrintWriter out, int level)
    {
      for (int i = 0; i < level; i++) out.print("  ");
    }

    /**************************************************************************
     * Print complete text of subtree rooted in this node.
     * @param out the output medium.
     * @param level the indentation level.
     *************************************************************************/
    public void printText(PrintWriter out)
    {
      out.println("----------------------------------------------------------------------");
      out.print(text);
      if (children != null)
      {
        int n = children.size();
        for (int i = 0; i < n; i++)
        {
          Node child = children.get(i);
          child.printText(out);
        }
      }
      out.println("----------------------------------------------------------------------");
    }
  }

  // --------------------------------------------------------------------------
  //
  // operations that construct proof nodes
  //
  // --------------------------------------------------------------------------
 
  /****************************************************************************
   * Create the root of the proof tree.
   * @param rules all rules.
   * @param axioms the rules that stem from the axioms.
   * @param goals the rules that stem from the theorem.
   * @param smt the smt reasoning level.
   * @return the created root node.
   ***************************************************************************/
  public Node createRoot(Rule[] rules, Rule[] axioms, Rule[] goals, int smt)
  {
    Node node = createNode(name, Type.OR);
    node.setText(false, (PrintWriter out) -> 
    {
      out.println("Proof [" + name + "]");
      out.println();
      int an = axioms.length;
      if (an > 0)
      {
        out.println("We are given the following axioms:");
        out.println();
        // print axioms in original (unreverted) order
        for (int i = an-1; i >= 0; i--)
        {
          Rule axiom = axioms[i];
          out.print("[" + axiom.name + "] ");
          axiom.print(out, false, true, (Object lit, Boolean neg)-> true, (Object lit, Boolean neg)->
          {
            out.print(Term.toString(lit));
          });
          out.println();
        }
        out.println();
      }
      int gn = goals.length;
      if (gn == 1)
      {
        out.println("The following \"goal\" represents the theorem to be proved:");
        out.println();
        Rule goal = goals[0];
        out.print("[" + goal.name + "] ");
        goal.print(out, true, true, (Object lit, Boolean neg)-> true, (Object lit, Boolean neg)->
        {
          out.print(Term.toString(lit));
        });
        out.println();
        out.println();
      }
      else
      {
        out.println("The following \"negated goals\" represent the negation of the theorem to be proved:");
        out.println();
        // print goals in original (unreverted) order
        for (int i = gn-1; i >= 0; i--)
        {
          Rule goal = goals[i];
          out.print("[" + goal.name + "] ");
          goal.print(out, false, true, (Object lit, Boolean neg)-> true, (Object lit, Boolean neg)->
          {
            out.print(Term.toString(lit));
          });
          out.println();
        }
        out.println();
      }
      out.println("To prove the theorem, we apply the proof strategy MESON (model elimination, subgoal oriented)");
      if (an == 0)
      {
        if (gn == 1)
        {
          out.println("to prove the (not negated) goal.");
        }
        else
        {
          out.println("to derive from the negated goals a contradiction. For this,");
          out.println("we prove some (not negated) goal from the \"knowledge\" represented by the other formulas.");
          out.println("We start the proof with the last goal; if this does not succeed, we also try the previous ones.");
        }
      }
      else
      {
        if (gn == 1)
        {
          out.println("to prove from the axioms the (not negated) goal.");
        }
        else
        {
          out.println("to derive from the axioms and negated goals a contradiction. For this,");
          out.println("we prove some (not negated) goal from the \"knowledge\" represented by the other formulas.");
          out.println("We start the proof with the last goal; if this does not succeed, we also try the previous ones.");
        }
      }
      if (smt != ModelElimination.SMT_OFF)
      {
        out.println();
        out.println("According to the 'SMT' setting, we aid MESON by application of an SMT solver:");
        switch (smt)
        {
        case ModelElimination.SMT_MIN:
          out.println("in every proof situation, we first apply MESON before the SMT solver;");
          out.println("if the MESON proof fails, we apply the SMT solver to the whole proof problem.");
          break;
        case ModelElimination.SMT_MED:
          out.println("in every proof situation, we first apply the SMT solver before MESON;");
          out.println("if the MESON proof fails, we apply the SMT solver to the whole proof problem.");
          break;
        case ModelElimination.SMT_MAX:
          out.println("we apply to the whole proof problem the SMT solver before MESON;");
          out.println("also in MESON, we apply in every proof situation the SMT solver first.");
          break;
        }
      }
    });
    return node;
  }

  /****************************************************************************
   * Try proof of goal with multiple proof bounds.
   * @param goal the goal to be proved.
   * @param the other parameters denote the proof bounds.
   * @return the created proof node.
   ***************************************************************************/
  public Node createNodeBounds(Rule goal, boolean INCREMENT_DEPTH, 
    int MAX_DEPTH_BEGIN, int MAX_DEPTH_END, int MAX_DEPTH_INC,
    int MAX_SIZE_BEGIN, int MAX_SIZE_END, int MAX_SIZE_INC)
  {
    Node node = createNode(goal.name, Type.OR);
    node.setText(false, (PrintWriter out) -> 
    {
      out.println("Goal: " + node.getName());
      out.println();
      out.print("Goal: ");
      printGoal(out, Arrays.asList(goal.vars), null, goal.lits, goal.negs);
      out.println();
      long timeout = ModelElimination.getTimeout();
      if (timeout != 0)
        out.print("The proof search is aborted after " + timeout + " ms and ");
      else
        out.print("The proof search proceeds without time bound and ");
      if (MAX_DEPTH_BEGIN != -1 && MAX_SIZE_BEGIN != -1)
      {
        out.println("is limited by both proof depth (" 
            + MAX_DEPTH_BEGIN + ") and size (" + MAX_SIZE_BEGIN + ").");
      }
      else if (MAX_DEPTH_BEGIN != -1)
      {
        out.println("is limited by proof depth (" 
            + MAX_DEPTH_BEGIN + ").");
      }
      else if (MAX_SIZE_BEGIN != -1)
      {
        out.println("is limited by proof size (" 
            + MAX_SIZE_BEGIN + ").");
      }
      else
      {
        out.println("is not limited by proof depth or size.");
      }
      if (INCREMENT_DEPTH)
      {
        if (MAX_DEPTH_BEGIN >= 0 && MAX_DEPTH_INC > 0 && MAX_DEPTH_END > MAX_DEPTH_BEGIN)
          out.println("If the proof search fails, we repeat it with increasing depth limits (increment: "
              + MAX_DEPTH_INC + ", "
              + (MAX_DEPTH_END == -1 ? "no maximum" : "maximum: " + MAX_DEPTH_END ) + ").");
      }
      else
      {
        if (MAX_SIZE_BEGIN >= 0 && MAX_SIZE_INC > 0 && MAX_SIZE_END > MAX_SIZE_BEGIN)
          out.println("If the proof search fails, we repeat it with increasing size limits (increment: "
              + MAX_SIZE_INC + ", " 
              + (MAX_DEPTH_END == -1 ? "no maximum" : "maximum: " + MAX_DEPTH_END ) + ").");
      }
    });
    return node;
  }
  
  /****************************************************************************
   * Try proof of a goal.
   * @param goal the goal to be proved.
   * @return the created proof node.
   ***************************************************************************/
  public Node createNodeGoal(Rule goal)
  {
    Node node = createNode(goal.name, Type.AND);
    node.setText(false, (PrintWriter out) -> 
    {
      out.println("Goal: " + node.getName());
      out.println();
      out.print("Formula: ");
      printGoal(out, Arrays.asList(goal.vars), null, goal.lits, goal.negs);
      out.println();
      out.println("Our goal is to prove this formula.");
    });
    return node;
  }
  
  /****************************************************************************
   * Prove a sequence of literals.
   * @param name the name of to be used for the proof node.
   * @param context the current proof context.
   * @param lits the literals to be proved.
   * @param negs the corresponding polarities.
   * @param eqs the positions of equations "var=term" yielding new substitution.
   * @param subst the resulting substitution.
   * @param lits0 the literals without the negations.
   * @param negs0 the corresponding polarities.
   * @param iterationNumber the current iteration number.
   * @return the created proof node.
   ***************************************************************************/
  public Node createNodeProveLits(String name, Context context, 
    Object[] lits, boolean[] negs, 
    Set<Integer> eqs, Subst subst, Object[] lits0, boolean[] negs0,
    int iterationNumber)
  {
    String itertext = "";
    if (isIterationNode()) // hack to print top-level iteration number
    {
      itertext = " (iteration " + (iterationNumber) + ")";
    }
    int en = eqs.size();
    // preserve (only) those leaves that result from variable substitutions
    Object[] lits1 = en == 0 ? lits : lits0;
    boolean[] negs1 = en == 0 ? negs : negs0;
    Type type = en > 0 && lits1.length == 0 ? Type.LEAF : Type.AND;
    Node node = createNode(name + itertext, type);
    node.setText(false, (PrintWriter out) -> 
    {
      printHeader(out, node, context);
      Set<VariableSymbol> vars = new LinkedHashSet<VariableSymbol>();
      out.print("Goal: ");
      printGoal(out, null, context.subst, lits, negs);
      for (Object lit : lits) 
      {
        Object lit0 = context.subst.apply(lit);
        Term.vars(lit0, vars);
      }
      int varn = vars.size();
      printVars(out, vars);
      out.println();
      if (en > 0)
      {
        if (en == 1) out.println("The equality"); else out.print("The equalities");
        out.println();
        for (int eq : eqs)
        {
          Object lit = context.subst.apply(lits[eq]);
          out.println("  " + litString(lit, !negs[eq]));
        }
        out.println();
        Set<VariableSymbol> evars = new LinkedHashSet<VariableSymbol>(subst.keySet());
        evars.removeAll(context.subst.keySet());
        if (evars.size() == 0)
        {
          if (en == 1)
            out.println("of identical terms is obviously true and is thus removed from the goal.");
          else
            out.println("of identical terms are obviously true and are thus removed from the goal.");
        }
        else
        {
          out.println("can be proved by the variable substitution");
          out.println();
          for (VariableSymbol evar : evars)
          {
            Object term = subst.get(evar);
            out.println("  " + evar.id + " → " + Term.toString(term));
          }
          out.println();
          if (en == 1) out.print("and is "); else out.print("and are ");
          out.println("thus (applying the substitution) removed from the goal.");
        }
        out.println();
        vars.removeAll(evars);
        varn = vars.size();
      }
      if (lits1.length == 0)
      {
        out.println("The goal is proved.");
        return;
      }
      if (varn == 0)
      {
        out.println("To prove the goal, we prove each subgoal:");
      }
      else
      {
        out.println("To prove the goal, we determine variable values that satisfy each subgoal:");
      }
      out.println();
      int litn = lits1.length;
      for (int i = 0; i < litn; i++)
      {
        Object lit0 = subst.apply(lits1[i]);
        out.println("  " + litString(lit0, !negs1[i]));
      }
    });
    return node;
  }

  /****************************************************************************
   * Literal was proved by application of an SMT solver.
   * @param context the current proof context.
   * @param lit the literal to be proved (as a term).
   * @param neg true if the literal is negated.
   * @param result the message returned by the solver.
   * @param input the input to the solver.
   * @param rules the rules from which the literal could be proved.
   * @return the created proof node.
   ***************************************************************************/ 
  public Node createNodeSMTSolver(Context context,
    Object lit, boolean neg, String result, String input, List<Rule> rules)
  {
    Object lit0 = context.subst.apply(lit);
    String name0 = litString(lit0, !neg);
    Node node = createNode(name0, Type.LEAF);
    node.setText(false, (PrintWriter out) -> 
    {
      printHeader(out, node, context);
      out.println("Goal: " + name0);
      printContext(out, context, lit0);
      out.println();
      out.println("The goal has been proved by the SMT solver: the solver states by the output");
      out.println();
      out.print("  " + result);
      out.println();
      if (rules.isEmpty())
      {
        out.println("the unsatisfiability of the negated goal.");
        return;
      }
      out.println("the unsatisfiability of the negated goal in conjunction with this knowledge:");
      out.println();
      for (Rule rule : rules)
      {
        out.print("[" + rule.name + "] ");
        if (rule.lits.length == 1)
        {
          out.println(litString(rule.lits[0], rule.negs[0]));
          continue;
        }
        rule.print(out, false, false,
            (Object litx, Boolean negx)-> true,
            (Object litx, Boolean negx)->
            {
              out.print(Term.toString(litx));
            });
        out.println();
      }
    });
    node.finalize(true, context.subst);
    return node;
  }

  /****************************************************************************
   * Proof problem was closed by application of an SMT solver.
   * @param name the name of the problem.
   * @param result1 the message returned by the solver on the rules.
   * @param rules the rules used in the proof.
   * @param result2 the message returned by the prover on the instances.
   * @param instances ground instances of rules that were used in the proof.
   * @return the created proof node.
   ***************************************************************************/ 
  public Node createNodeSMTSolver(String name,
    String result1, List<Rule> rules, String result2, List<Rule> instances)
  {
    Node node = createNode(name, Type.LEAF);
    node.setText(false, (PrintWriter out) -> 
    {
      out.println("Proof problem: " + name);
      out.println();
      boolean unsat1 = result1 != null && result1.startsWith("unsat");
      if (unsat1)
      {
        out.println("The problem has been closed by the SMT solver: the solver states by the output");
        out.println();
        out.print("  " + result1);
        out.println();
        out.println("the unsatisfiability of these clauses that arise from the negation of the theorem to be proved:");
        out.println();
        printRules(out, rules);
      }
      boolean unsat2 = result2 != null && result2.startsWith("unsat"); 
      if (unsat2)
      {
        if (unsat1)
        {
          out.println();
          out.println("In more detail, the solver states the unsatisfiability of these clause instances:");
        }
        else
        {
          out.println("The problem has been closed by the SMT solver: the solver states by the output");
          out.println();
          out.print("  " + result2);
          out.println();
          out.println("the unsatisfiability of these instances of clauses that arise from the negation of the theorem to be proved::");
        }
        out.println();
        printRules(out, instances);
      }
      out.println();
      out.println("Thus the theorem is valid.");
    });
    node.finalize(true, null);
    return node;
  }
  
  /****************************************************************************
   * Print a list of rules.
   * @param rules the rules to be printed.
   ***************************************************************************/
  private static void printRules(PrintWriter out, List<Rule> rules)
  {
    for (Rule rule : rules)
    {
      out.print("[" + rule.name + "] ");
      int varn = rule.vars.length;
      if (varn > 0)
      {
        out.print("∀");
        for (int i = 0; i < varn; i++)
        {
          VariableSymbol var = rule.vars[i];
          out.print(var.id.toString());
          out.print(":");
          out.print(var.tsymbol.type.toString());
          if (i+1 < varn) out.print(",");
        }
        out.print(". ");
      }
      if (rule.lits.length == 1)
      {
        out.println(litString(rule.lits[0], rule.negs[0]));
        continue;
      }
      rule.print(out, false, false,
          (Object litx, Boolean negx)-> true,
          (Object litx, Boolean negx)->
          {
            out.print(Term.toString(litx));
          });
      out.println();
    }
  }
  
  /****************************************************************************
   * Attempt aborted to prove a literal.
   * @param context the current proof context.
   * @param lit the literal to be proved (as a term).
   * @param neg true if the literal is negated.
   * @param message the reason for the abortion.
   * @return the created proof node.
   ***************************************************************************/ 
  public Node createNodeAbortion(Context context,
    Object lit, boolean neg, String message)
  {
    Object lit0 = context.subst.apply(lit);
    String name0 = litString(lit0, !neg);
    Node node = createNode(name0, Type.LEAF);
    node.setText(false, (PrintWriter out) -> 
    {
      printHeader(out, node, context);
      out.println("Goal: " + name0);
      out.println();
      out.println(message);
    });
    node.finalize(false, null);
    return node;
  }

  /****************************************************************************
   * Attempt failed to prove a literal because of an identical assumption.
   * @param Context the current proof context.
   * @param lit the literal to be proved (as a term).
   * @param neg true if the literal is negated.
   * @param i the index of the assumption.
   * @return the created proof node.
   ***************************************************************************/ 
  public Node createNodeIdentityFailure(Context context,
    Object lit, boolean neg, int i)
  {
    int assn = context.assumptions.size();
    Object lit0 = context.subst.apply(lit);
    String name0 = litString(lit0, !neg);
    Node node = createNode(name0, Type.LEAF);
    node.setText(false, (PrintWriter out) -> 
    {
      printHeader(out, node, context);
      out.println("Goal: " + name0);
      printContext(out, context, lit0);
      out.println();
      out.println("The proof attempt is aborted because the goal contradicts assumption [" + (assn-i) + "].");
    });
    node.finalize(false, null);
    return node;
  }

  /****************************************************************************
   * Attempt succeeded to prove a literal because it denotes the equality
   * of two identical terms.
   * @param context the current proof context.
   * @param lit the literal to be proved (as a term).
   * @param neg true if the literal is negated.
   * @return the created proof node.
   ***************************************************************************/ 
  public Node createNodeEqualitySuccess(Context context,
    Object lit, boolean neg)
  {
    Object lit0 = context.subst.apply(lit);
    String name0 = litString(lit0, !neg);
    Node node = createNode(name0 + " [=]", Type.LEAF);
    node.setText(false, (PrintWriter out) -> 
    {
      printHeader(out, node, context);
      out.println("Goal: " + name0);
      printContext(out, context, lit0);
      out.println();
      out.println("The goal is valid because it denotes the equality of two identical terms.");
    });
    node.finalize(true, context.subst);
    return node;
  }
  
  /****************************************************************************
   * Attempt succeeded to prove a literal because it denotes the equality
   * of two terms that can be unified.
   * @param context the current proof context.
   * @param lit the literal to be proved (as a term).
   * @param neg true if the literal is negated.
   * @param subst the unifying substitution
   * @return the created proof node.
   ***************************************************************************/ 
  /*
  public Node createNodeEqualitySuccess(Context context,
    Object lit, boolean neg, Subst subst)
  {
    Object lit0 = context.subst.apply(lit);
    String name0 = litString(lit0, !neg);
    Node node = createNode(name0 + " [=]", Type.LEAF);
    node.setText(false, (PrintWriter out) -> 
    {
      printHeader(out, node, context);
      out.println("Goal: " + name0);
      printContext(out, context, lit0);
      out.println();
      Collection<VariableSymbol> vars = new LinkedHashSet<VariableSymbol>(subst.keySet());
      vars.removeAll(context.subst.keySet());
      if (vars.isEmpty())
      {
        out.println("The goal is valid because it denotes the equality of two identical terms.");
        return;
      }
      out.println("The goal is valid because it denotes the equality of two terms");
      out.println("that can be unified by the following variable substitution:");
      out.println();
      for (VariableSymbol var : vars)
      {
        Object term = subst.get(var);
        out.println("  " + var.id + " → " + Term.toString(term));
      }
    });
    node.finalize(true, subst);
    return node;
  }
  */
  
  /****************************************************************************
   * Attempt succeeded to prove a literal because of a complementary assumption.
   * @param context the current proof context.
   * @param lit the literal to be proved (as a term).
   * @param neg true if the literal is negated.
   * @param i the index of the assumption.
   * @param lit1 the assumption.
   * @param subst0 the substitution after the unification.
   * @return the created proof node.
   ***************************************************************************/ 
  public Node createNodeComplementSuccess(Context context,
    Object lit, boolean neg, 
    int i, Object lit1, Subst subst0)
  {
    Object lit0 = context.subst.apply(lit);
    String name0 = litString(lit0, !neg);
    int assn = context.assumptions.size();
    Node node = createNode(name0 + " [" + (assn-i) + "]", Type.LEAF);
    Set<VariableSymbol> vars = new LinkedHashSet<VariableSymbol>();
    Term.vars(lit0, vars);
    Object lit2 = context.subst.apply(lit1);
    Term.vars(lit2, vars);
    node.setText(false, (PrintWriter out) -> 
    {
      printHeader(out, node, context);
      out.println("Goal: " + name0);
      printContext(out, context, lit0);
      out.println();
      List<VariableSymbol> vars0 = new ArrayList<VariableSymbol>();
      List<Object> terms0 = new ArrayList<Object>();
      for (VariableSymbol var : vars)
      {
        Object var0 = Term.term(var);
        Object term = subst0.apply(var0);
        if (Term.equal(term,var0)) continue;
        vars0.add(var);
        terms0.add(term);
      }
      if (vars0.isEmpty())
        out.println("The goal can be proved from assumption [" + (assn-i) + "].");
      else
      {
        out.println("The goal can be proved from assumption [" + (assn-i) + 
            "] by the following substitution:");
        out.println();
        int n = vars0.size();
        for (int j = 0; j < n; j++)
        {
          VariableSymbol var = vars0.get(j);
          Object term = terms0.get(j);
          out.println("  " + var.id + " → " + Term.toString(term));
        }
      }
    });
    node.finalize(true, subst0);
    return node;
  }

  /****************************************************************************
   * Attempt to prove literal in various ways.
   * @param context the current proof context.
   * @param lit the literal to be proved (as a term).
   * @param neg true if the literal is negated.
   * @return the created proof node.
   ***************************************************************************/
  public Node createNodeProveLiteral(Context context,
    Object lit, boolean neg)
  {
    Object lit0 = context.subst.apply(lit);
    String name0 = litString(lit0, !neg);
    Node node = createNode(name0, Type.OR);
    node.setText(false, (PrintWriter out) -> 
    {
      printHeader(out, node, context);
      out.println("Goal: " + name0);
      Set<VariableSymbol> vars = new LinkedHashSet<VariableSymbol>();
      Term.vars(lit0, vars);
      printVars(out, vars);
      out.println();  
      out.println("To prove the goal, we first consider the current assumptions.");
      out.println("If this does not suffice, we consider every clause with a matching literal,");
      out.println("and ultimately every clause with an equality matching some term in the goal.");
    });
    return node;
  }

  /****************************************************************************
   * Attempt started to prove a literal via application of a rule with the given 
   * head literal under the given substitution.
   * @param context the current proof context.
   * @param subst0 the substitution after the unification.
   * @param lit the literal to be proved (as a term).
   * @param neg true if the literal is negated.
   * @param rule the rule that is to be applied.
   * @param rlit the head literal of the rule.
   * @return the created proof node.
   ***************************************************************************/
  public Node createNodeApplyRule(Context context,
    Subst subst0, Object lit, boolean neg, Rule rule, Object rlit)
  {
    Object lit0 = context.subst.apply(lit);
    String name0 = litString(lit0, !neg);
    Node node = createNode(name0 + " [" + rule.name + "]", Type.AND);
    Object lit1 = context.subst.apply(rlit);
    Set<VariableSymbol> vars = new LinkedHashSet<VariableSymbol>();
    Term.vars(lit0, vars);
    Term.vars(lit1, vars);
    node.setText(false, (PrintWriter out) -> 
    {
      printHeader(out, node, context);
      out.println("Goal: " + name0);
      printContext(out, context, lit0);
      out.println();
      out.println("To prove the goal, we assume its negation");
      out.println();
      int assn = context.assumptions.size();
      out.println("[" + (assn+1) + "] " + litString(lit0, neg));
      out.println();
      out.println("and show a contradiction. For this, consider knowledge [" 
          + rule.name + "] with the following instance:");
      out.println();
      out.print("  ");
      rule.print(out, false, true,
          (Object litx, Boolean negx)-> true,
          (Object litx, Boolean negx)->
          {
            Object lity = context.subst.apply(litx);
            out.print(Term.toString(lity));
          });
      out.println();
      out.println();
      if (vars.isEmpty())
      {
        out.println("Assumption [" + (assn+1) + "] matches the" 
            + " literal " + Term.toString(rlit) 
            + " on the " + (neg ? "right" : "left")
            + " side.");
        out.println();
        out.println("Therefore, dropping the literal, we know:");
      }
      else
      {
        out.println("Assumption [" + (assn+1) + "] matches the" 
            + " literal " + Term.toString(rlit) 
            + " on the " + (neg ? "right" : "left")
            + " side of this clause by the following substitution:");
        out.println();
        for (VariableSymbol var : vars)
        {
          Object var0 = Term.term(var);
          Object term = subst0.apply(var0);
          if (Term.equal(term,var0)) continue;
          out.println("  " + var.id + " → " + Term.toString(term));
        }
        out.println();
        out.println("Therefore, applying this substitution and dropping the literal, we know:");
      }
      out.println();
      out.print("  ");
      Set<VariableSymbol> vars0 = new LinkedHashSet<VariableSymbol>();
      for (Object litx : rule.lits)
      {
        Object lity = subst0.apply(litx);
        Term.vars(lity, vars0);
      } 
      int varn0 = vars0.size();
      if (varn0 > 0)
      {
        out.print("∀");
        int i = 0;
        for (VariableSymbol var0: vars0)
        {
          out.print(var0.id);
          out.print(":");
          out.print(var0.tsymbol.type);
          if (i+1 < varn0) out.print(",");
          i++;
        }
        out.print(". ");
      }
      rule.print(out, false, false,
          (Object litx, Boolean negx)-> litx != rlit,
          (Object litx, Boolean negx)->
          {
            Object lity = subst0.apply(litx);
            out.print(Term.toString(lity));
          });
      out.println();
      out.println();
      int nx = rule.lits.length;
      if (nx == 1)
      {
        out.println("Therefore we have a contradiction.");
        return;
      }
      int varsn0 = vars0.size();
      if (varsn0 == 0)
      {
        out.println("Therefore, to show a contradiction, we prove this subgoal:");
      }
      else
      {
        out.println("Therefore, to show a contradiction, we determine variable values that satisfy this subgoal:");
      }
      out.println();
      Object[] litsx = new Object[nx-1];
      boolean[] negsx = new boolean[nx-1];
      int ix = 0;
      for (int i = 0; i < nx; i++)
      {
        Object litx = rule.lits[i];
        if (litx == rlit) continue;
        boolean negx = rule.negs[i];
        litsx[ix] = litx;
        negsx[ix] = negx;
        ix++;
      }
      out.print("  ");
      printGoal(out, null, subst0, litsx, negsx);
    });
    return node;
  }

  /****************************************************************************
   * Attempt started to prove a literal via application of a rule with the given 
   * head literal under the given substitution.
   * @param context the current proof context.
   * @param subst0 the substitution after the unification.
   * @param lit the literal to be proved (as a term).
   * @param neg true if the literal is negated.
   * @param rule the rule that is to be applied.
   * @param rlit the head literal of the rule.
   * @param diff the differing subterms.
   * @param pred1 one predicate applicable to the differing subterms.
   * @param pred2 null or another predicate applicable to the differing subterms.
   * @return the created proof node.
   ***************************************************************************/
  public Node createNodeApplyCaseSplit(Context context,
    Subst subst0, Object lit, boolean neg, Rule rule, Object rlit, 
    Object[] diff, FunctionSymbol pred1, FunctionSymbol pred2)
  {
    Object lit0 = context.subst.apply(lit);
    String name0 = litString(lit0, !neg);
    Node node = createNode(name0 + " [" + rule.name + "]", Type.AND);
    node.setText(false, (PrintWriter out) -> 
    {
      printHeader(out, node, context);
      out.println("Goal: " + name0);
      printContext(out, context, lit0);
      out.println();
      out.println("To prove the goal, we assume its negation");
      out.println();
      int assn = context.assumptions.size();
      out.println("[" + (assn+1) + "] " + litString(lit0, neg));
      out.println();
      out.println("and show a contradiction. For this, consider knowledge [" 
          + rule.name + "] with the following instance:");
      out.println();
      out.print("  ");
      rule.print(out, false, true,
          (Object litx, Boolean negx)-> true,
          (Object litx, Boolean negx)->
          {
            Object lity = context.subst.apply(litx);
            out.print(Term.toString(lity));
          });
      out.println();
      out.println();
      out.println("Assumption [" + (assn+1) + "] matches the" 
          + " literal " + Term.toString(rlit) 
          + " on the " + (neg ? "right" : "left"));
      out.println("if we assume the equality " 
          + Term.toString(diff[0]) + " = " + Term.toString(diff[1]) + ".");
      out.println();
      out.println("Under this assumption, dropping the literal, we therefore know:");
      out.println();
      out.print("  ");
      Set<VariableSymbol> vars0 = new LinkedHashSet<VariableSymbol>();
      for (Object litx : rule.lits)
      {
        Object lity = subst0.apply(litx);
        Term.vars(lity, vars0);
      } 
      int varn0 = vars0.size();
      if (varn0 > 0)
      {
        out.print("∀");
        int i = 0;
        for (VariableSymbol var0: vars0)
        {
          out.print(var0.id);
          out.print(":");
          out.print(var0.tsymbol.type);
          if (i+1 < varn0) out.print(",");
          i++;
        }
        out.print(". ");
      }
      rule.print(out, false, false,
          (Object litx, Boolean negx)-> litx != rlit,
          (Object litx, Boolean negx)->
          {
            Object lity = subst0.apply(litx);
            out.print(Term.toString(lity));
          });
      out.println();
      out.println();
      if (pred2 == null)
        out.println("Therefore, to show a contradiction, we prove this subgoal:");
      else
      {
        out.println("Therefore, to show a contradiction, we prove this subgoal");
        out.println("(which splits the assumption into two inequalities):");
      }
      out.println();
      int nx = rule.lits.length;
      Object[] litsx = pred2 == null ? new Object[nx] : new Object[nx+1];
      boolean[] negsx = pred2 == null ? new boolean[nx] : new boolean[nx+1];
      int ix = 0;
      for (int i = 0; i < nx; i++)
      {
        Object litx = rule.lits[i];
        if (litx == rlit) continue;
        boolean negx = rule.negs[i];
        litsx[ix] = litx;
        negsx[ix] = negx;
        ix++;
      }
      litsx[nx-1] = Term.term(pred1, new Object[] { diff[1], diff[0] });
      negsx[nx-1] = true;
      if (pred2 != null)
      {
        litsx[nx] = Term.term(pred2, new Object[] { diff[0], diff[1] });
        negsx[nx] = true;
      }
      out.print("  ");
      printGoal(out, null, subst0, litsx, negsx);
    });
    return node;
  }

  /****************************************************************************
   * Attempt started to prove a literal via application of a rule with the given 
   * head literal under the given substitution.
   * @param context the current proof context.
   * @param subst0 the substitution after the unification.
   * @param lit the literal to be proved (as a term).
   * @param neg true if the literal is negated.
   * @param rule the rule that is to be applied.
   * @param rlit the head literal of the rule.
   * @param term0 the term in the literal.
   * @param term1 the matching term in the rule.
   * @param term2 the replacement term in the rule.
   * @param left true if match occurred in lhs of equality in the rule.
   * @param litr the literal to be proved after the replacement.
   * @return the created proof node.
   ***************************************************************************/
  public Node createNodeApplyEqRule(Context context,
    Subst subst0, Object lit, boolean neg, Rule rule, Object rlit, 
    Object term0, Object term1, Object term2,
    boolean left, Object litr)
  {
    Object lit0 = context.subst.apply(lit);
    String name0 = litString(lit0, !neg);
    Node node = createNode(name0 + " [" + rule.name + "]", Type.AND);
    Object lit1 = context.subst.apply(rlit);
    Set<VariableSymbol> vars = new LinkedHashSet<VariableSymbol>();
    Term.vars(lit0, vars);
    Term.vars(lit1, vars);
    node.setText(false, (PrintWriter out) -> 
    {
      printHeader(out, node, context);
      out.println("Goal: " + name0);
      printContext(out, context, lit0);
      out.println();
      out.print("Consider knowledge [" + rule.name + "] with the ");
      if (rule.lits.length > 1)
        out.println("instance");
      else
        out.println("following instance that denotes an equality:");
      out.println();
      out.print("  ");
      Set<VariableSymbol> vars0 = new LinkedHashSet<VariableSymbol>();
      for (Object litx : rule.lits)
      {
        Object lity = subst0.apply(litx);
        Term.vars(lity, vars0);
      } 
      int varn0 = vars0.size();
      if (varn0 > 0)
      {
        out.print("∀");
        int i = 0;
        for (VariableSymbol var0: vars0)
        {
          out.print(var0.id);
          out.print(":");
          out.print(var0.tsymbol.type);
          if (i+1 < varn0) out.print(",");
          i++;
        }
        out.print(". ");
      }
      rule.print(out, false, false,
          (Object litx, Boolean negx)-> true,
          (Object litx, Boolean negx)->
          {
            Object lity = subst0.apply(litx);
            out.print(Term.toString(lity));
          });
      out.println();
      out.println();
      if (rule.lits.length > 1)
      {
        out.println("that includes this equality:");
        out.println();
        out.print("  ");
        out.println(Term.toString(subst0.apply(rlit)));
        out.println();
      }
      Set<VariableSymbol> vars1 = new LinkedHashSet<VariableSymbol>();
      Term.vars(context.subst.apply(term0), vars1);
      Term.vars(subst0.apply(term2), vars1);
      List<VariableSymbol> vars2 = new ArrayList<VariableSymbol>();
      List<Object> terms2 = new ArrayList<Object>();
      for (VariableSymbol var: vars1)
      {
        Object var0 = Term.term(var);
        Object term = subst0.apply(var0);
        if (Term.equal(term,var0)) continue;
        vars2.add(var);
        terms2.add(term);
      }
      out.print("Therefore, ");
      if (!vars2.isEmpty())
      { 
        out.println("applying the substitution");
        out.println();
        int n = vars2.size();
        for (int i = 0; i < n; i++)
        {
          VariableSymbol var = vars2.get(i);
          Object term = terms2.get(i);
          out.println("  " + var.id + " → " + Term.toString(term));
        }
        out.println();
      }
      out.println("we may replace in the goal the term");
      out.println();
      out.print("  ");
      out.println(Term.toString(context.subst.apply(term0)));
      out.println();
      out.println("by the term");
      out.println();
      out.print("  ");
      out.println(Term.toString(subst0.apply(term2)));
      out.println();
      out.print("such that ");
      if (rule.lits.length > 1) out.print("(adding the prerequisites of the equality) ");
      out.println("it suffices to prove this new goal:");
      int nx = rule.lits.length;
      out.println();
      Object[] litsx = new Object[nx];
      boolean[] negsx = new boolean[nx];
      litsx[0] = litr;
      negsx[0] = neg;
      int ix = 1;
      for (int i = 0; i < nx; i++)
      {
        Object litx = rule.lits[i];
        if (litx == rlit) continue;
        boolean negx = rule.negs[i];
        litsx[ix] = litx;
        negsx[ix] = negx;
        ix++;
      }
      out.print("  ");
      printGoal(out, null, subst0, litsx, negsx);
    });
    return node;
  }

  /****************************************************************************
   * Attempt started to prove literal "f(a)=f(b)" by proving "a=b". 
   * @param context the current proof context.
   * @param lit the literal "f(a)=f(b)" (as a term).
   * @param lit0 the clause "a=b" (as a term).
   * @return the created proof node.
   ***************************************************************************/
  public Node createNodeApplySubstitution(Context context,
    Object lit, Object lit0)
  {
    Object litx = context.subst.apply(lit);
    String name = litString(litx, false);
    Node node = createNode(name + " [substitution]", Type.AND);
    node.setText(false, (PrintWriter out) -> 
    {
      printHeader(out, node, context);
      out.println("Goal: " + name);
      printContext(out, context, litx);
      out.println();
      out.println("To prove the goal, by the substitution property of equality");
      out.println("it suffices to prove the following new goal:");
      out.println();
      out.println("  " + Term.toString(context.subst.apply(lit0)));
    });
    return node;
  }

  /****************************************************************************
   * Attempt started to prove literal "p(a,...)" by proving 
   * clause "x=a /\ ... /\ p(x,...)"
   * @param context the current proof context.
   * @param lit the literal "p(a,...)" (as a term).
   * @param lits the clause "p(x,...) /\ x=a /\ ..." (as a term sequence).
   * @return the created proof node.
   ***************************************************************************/
  public Node createNodeApplyEqCongruence(Context context,
    Object lit, Object[] lits)
  {
    Object lit0 = context.subst.apply(lit);
    String name0 = litString(lit0, false);
    Node node = createNode(name0 + " [congruence]", Type.AND);
    node.setText(false, (PrintWriter out) -> 
    {
      printHeader(out, node, context);
      out.println("Goal: " + name0);
      printContext(out, context, lit0);
      out.println();
      out.println("To prove the goal, by the congruence property of equality");
      out.println("it suffices to prove the following new goals:");
      out.println();
      int n = lits.length;
      for (int i = 0; i < n; i++)
      {
        out.println("  " + Term.toString(context.subst.apply(lits[i])));
      }
    });
    return node;
  }

  // --------------------------------------------------------------------------
  //
  // Auxiliaries
  //
  // --------------------------------------------------------------------------
  
  /****************************************************************************
   * Print a goal. 
   * @param out the output medium.
   * @param vars the variables of the goal (may be null).
   * @param subst the substitution to be applied to the literals (may be null).
   * @param lits its literals.
   * @param negs their polarities.
   ***************************************************************************/
  private static void printGoal(PrintWriter out, 
    Collection<VariableSymbol> vars, Subst subst, Object[] lits, boolean[] negs)
  {
    int litn = lits.length;
    if (litn == 0)
    {
      out.println("⊤");
      return;
    }
    if (vars != null)
    {
      int varn = vars.size();
      if (varn > 0)
      {
        out.print("∃");
        int i = 0;
        for (VariableSymbol var : vars)
        {
          out.print(var.id.toString());
          out.print(":");
          out.print(var.tsymbol.type.toString());
          if (i+1 < varn) out.print(",");
          i++;
        }
        out.print(". ");
      }
    }
    for (int i = 0; i < litn; i++)
    {
      Object lit = lits[i];
      if (subst != null) lit = subst.apply(lit);
      out.print(litString(lit, !negs[i]));
      if (i+1 < litn) out.print(" ∧ ");
    }
    out.println();
  }
  
  /****************************************************************************
   * Print header for proof situation.
   * @param out the output medium.
   * @param node the proof node.
   * @param context the proof context.
   ***************************************************************************/
  private void printHeader(PrintWriter out, Node node, Context context)
  {
    out.println("Goal: " + node.getName() 
    + " (proof depth: " + context.depth 
    + ", proof size: " + measure.sizeStack.top().value + ")");
    out.println();
  }

  /****************************************************************************
   * Print the current proof state.
   * @param out the output medium.
   * @param context the current proof context.
   * @param goal the literal to be proved.
   ***************************************************************************/
  public void printContext(PrintWriter out, Context context, Object goal)
  {
    Set<VariableSymbol> vars = new LinkedHashSet<VariableSymbol>();
    Term.vars(goal, vars);
    int assn = context.assumptions.size();
    Object[] lits0 = new Object[assn];
    boolean[] negs0 = new boolean[assn];
    int i = 0;
    for (Literal ass : context.assumptions)
    {
      Object lit0 = context.subst.apply(ass.term);
      lits0[assn-i-1] = lit0;
      negs0[assn-i-1] = ass.neg;
      Term.vars(lit0, vars);
      i++;
    }
    if (assn > 0)
    {
      out.println();
      out.println("Assumptions:");
      out.println();
      for (i = 0; i < assn; i++)
      {
        out.println("[" + (i+1) + "] " + litString(lits0[i], negs0[i]));
      }
      if (vars.size() > 0) out.println();
    }
    printVars(out, vars);
  }
  
  /****************************************************************************
   * Print variables.
   * @param out the output stream
   * @param vars the variables.
   ***************************************************************************/
  private static void printVars(PrintWriter out, Set<VariableSymbol> vars)
  {
    int varn = vars.size();
    if (varn > 0)
    {
      out.print("Variables: ");
      int i = 0;
      for (VariableSymbol var : vars)
      {
        out.print(var.id.toString());
        out.print(":");
        out.print(var.tsymbol.type.toString());
        if (i+1 < varn) out.print(",");
        i = i+1;
      }
      out.println();
    }
  }
  
  /****************************************************************************
   * Get string representation of literal.
   * @param lit the literal.
   * @param neg is literal negated?
   * @return the string represenation.
   ***************************************************************************/
  private static String litString(Object lit, boolean neg)
  {
    return (neg ? "¬" : "" ) + Term.toString(lit);
  }
  
  // --------------------------------------------------------------------------
  //
  // Prune a proof tree to only represent the successful proof.
  //
  // --------------------------------------------------------------------------
  
  /****************************************************************************
   * Retains only those roots that represent closed proof trees and
   * replaces the OR-nodes of these trees by their last branches. Also
   * publishes the remaining trees via the registered handlers.
   ***************************************************************************/
  public void prune()
  {
    List<Node> roots0 = new ArrayList<Node>();
    for (Node root: roots)
    {
      if (!root.closed) 
      {
        // keep root of open proof for display
        root.children = new ArrayList<Node>();
        publish(root);
        continue;
      }
      Node root0 = prune(root);
      root.children = new ArrayList<Node>();
      if (root0 != null) 
      {
        // preserve the root node, although it is an OR-node
        root.children.add(root0);
        root0.parent = root;
        roots0.add(root);
        publish(root);
      }
    }
    roots = roots0;
  }

  /****************************************************************************
   * Remove from a subtree the OR-nodes and their superfluous branches.
   * @param node the root of the subtree.
   * @return the root of the subtree without the superfluous nodes (may be null).
   ***************************************************************************/
  private Node prune(Node node)
  {
    if (!node.closed) return null;
    List<Node> children = node.prunedChildren;
    node.children = children;
    node.prunedChildren = null;
    if (children == null) return node;
    if (node.type == Type.AND)
    {
      List<Node> children0 = new ArrayList<Node>();
      node.children = children0;
      for (Node child: children)
      {
        if (child.type == Type.AND)
        {
          for (Node child0: child.prunedChildren)
          {
            Node child1 = prune(child0);
            if (child1 == null) continue;
            children0.add(child1);
            child1.parent = node;
          }
          continue;
        }
        Node child0 = prune(child);
        if (child0 == null) continue;
        children0.add(child0);
        child0.parent = node;
      }
      return node;
    }
    int n = children.size();
    if (n == 0) return node;
    Node child = children.get(n-1);
    Node child0 = prune(child);
    return child0;
  }

  /****************************************************************************
   * Publish a subtree via the registered handlers.
   * @param node the root of the subtree.
   ***************************************************************************/
  private void publish(Node node)
  {
    if (newNode != null) newNode.accept(node);
    if (updateNode != null) updateNode.accept(node);
    if (finalizeNode != null) finalizeNode.accept(node);
    List<Node> children = node.children;
    if (children == null) return;
    for (Node child: children)
      publish(child);
  }
}
// ----------------------------------------------------------------------------
// end of file
// ----------------------------------------------------------------------------