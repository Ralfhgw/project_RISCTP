// TypeChecker.java
// The type checker.
// $Id: TypeChecker.java,v 1.142 2024/06/20 14:14:50 schreine Exp $
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
package risctp.types;

import java.io.*;
import java.util.*;
import java.util.function.*;

import risctp.*;
import risctp.syntax.*;
import risctp.syntax.AST.*;
import risctp.syntax.AST.Decl.*;
import risctp.syntax.AST.Decl.Function;
import risctp.syntax.AST.Exp.*;
import risctp.types.Symbol.*;
import risctp.problem.*;

public class TypeChecker
{
  // the path to the integer axioms files
  private final String INTAXIOMSPATH = "risctp/fol/intAxioms.txt";
  private final String NONLINEARAXIOMSPATH = "risctp/fol/nonLinearAxioms.txt";
  
  // the output medium
  PrintWriter writer;
  
  // the number of type checking errors
  private int errors;
 
  // the type table
  public TypeTable typeTable;

  // the generated proof problem
  private ProofProblem result;
 
  // the current environment 
  private final Environment env;

  // the type theorem generator to be applied in current expression context
  private UnaryOperator<Exp> typeTheoremFun;

  // the current declaration being processed
  private Decl currentDecl;

  // the commands to be executed after processing the declaration
  private List<Runnable> postDecl;
  
  // the canonical versions of compound types
  private Set<TypeSymbol> compoundTypes;

  // if true, then generate type checking theorems
  private boolean generateTypeTheorems;
  
  // if true, then generate "height" function for data types
  private boolean generateHeight;

  // if true axioms for integers, nonlinear arithmetic, maps, data types are to be generated
  private boolean intAxioms;
  private boolean nonLinearAxioms;
  private boolean mapAxioms;
  private boolean dataAxioms;
  
  /****************************************************************************
   * Create a type checker that encapsulates the derived information.
   * @param intAxioms true if linear arithmetic axioms are to be generated.
   * @param nonLinearAxioms true if also non-linear axioms are to be generated.
   * @param mapAxioms true if map axioms are to be generated.
   * @param dataAxioms true if data type axioms are to be generated.
   * After type checking, various transformations need to be performed:
   * - Reduction to SMT-LIB:
   *   - Replace subtypes by explicit type constraints.
   *   - Replace "choose" expressions by axiomatized functions.
   *   - Replace overloaded functions by uniquely named ones.
   * - Already implemented preparations for reduction to first-order logic:
   *   - Translate algebraic data types to types, operations, axioms.
   *   - Translate tuple types to algebraic types.
   *   - Translate map types to types, operations, axioms.
   * - Still required reductions to first-order logic with equality:
   *   - Replace constants annotated with variable symbols by variables.
   *   - Replace function definitions by axioms.
   *   - Replace "match" and "let" and "if-then-else" expressions.
   *   - Separate formulas (atomic predicates) from terms (function applications).
   * Known limitation:
   * - 'height' function for checkResult.datatypes is only declared but not axiomatized (due
   *   to the application-dependent interpretation of the height of maps:
   *   Map[T,Bool] may be interpreted as an T-indexed array of Bool (height 0) 
   *   but also as a set of T-values (height is maximum height of T-index mapped
   *   to 'true); thus we leave the axiomatization to the user/translation
   ***************************************************************************/
  public TypeChecker(boolean intAxioms, boolean nonLinearAxioms,
    boolean mapAxioms, boolean dataAxioms) 
  { 
    this.intAxioms = intAxioms;
    this.nonLinearAxioms = nonLinearAxioms;
    this.mapAxioms = mapAxioms;
    this.dataAxioms = dataAxioms;
    writer = Main.getMain().getOutput();
    errors = 0;
    typeTable = new TypeTable();
    result = new ProofProblem(typeTable.getBool(), typeTable.getInt());
    env = new Environment(); 
    typeTheoremFun = (Exp exp)->exp;
    currentDecl = null;
    postDecl = new ArrayList<Runnable>();
    compoundTypes = new HashSet<TypeSymbol>();
    generateTypeTheorems = true;
    generateHeight = false;
    initEnv();
    generateHeight = true;
  }
  public TypeChecker()
  {
    this(false, false, false, false);
  }
  
  /****************************************************************************
   * initialize the environment with predefined entities
   ***************************************************************************/
  private void initEnv()
  {
    boolType();
    typeDecl(result.intSymbol);
    binaryFunction(AST.LESSNAME, result.intSymbol, result.intSymbol, result.boolSymbol);
    binaryFunction(AST.LESSEQUALNAME, result.intSymbol, result.intSymbol, result.boolSymbol);
    binaryFunction(AST.GREATERNAME, result.intSymbol, result.intSymbol, result.boolSymbol);
    binaryFunction(AST.GREATEREQUALNAME, result.intSymbol, result.intSymbol, result.boolSymbol);
    natType();
    nonZeroType();
    binaryFunction(AST.PLUSNAME, result.intSymbol, result.intSymbol, result.intSymbol);
    binaryFunction(AST.MINUSNAME, result.intSymbol, result.intSymbol, result.intSymbol);
    unaryFunction(AST.MINUSNAME, result.intSymbol, result.intSymbol); // overloaded
    binaryFunction(AST.MULTNAME, result.intSymbol, result.intSymbol, result.intSymbol);
    binaryFunction(AST.DIVNAME, result.intSymbol, result.nonZeroSymbol, result.intSymbol);
    binaryFunction(AST.MODNAME, result.intSymbol, result.nonZeroSymbol, result.intSymbol);
    axiomatizationCompleted = false;
    completeAxiomatization();
  }
  
  // if false, completeAxiomatization() needs to be invoked before next theorem
  boolean axiomatizationCompleted = true;
  
  /****************************************************************************
   * Complete the axiomatization (axioms added now have higher priority
   * in MESON type checking)
   ***************************************************************************/
  private void completeAxiomatization()
  {
    if (axiomatizationCompleted) return;
    axiomatizationCompleted = true;
    if (nonLinearAxioms) readNonLinearAxioms();
    if (intAxioms || nonLinearAxioms) readIntAxioms(); // declareIntAxioms();
  }

  /****************************************************************************
   * Return the proof problem generated by type-checking.
   * @return the result (null, if an error occurred).
   ***************************************************************************/
  public ProofProblem getResult()
  {
    if (errors == 0) return result; 
    writer.println("ERROR: " + errors + " type errors.");
    return null;
  }
  
  /****************************************************************************
   * Create Boolean type and its values.
   ***************************************************************************/
  private void boolType()
  {
    Id typeId = result.boolSymbol.id;
    Id trueId = Id.create(AST.internal("true"));
    Id falseId = Id.create(AST.internal("false"));
    List<DataTypeConstr> constrs = new ArrayList<DataTypeConstr>(2);
    constrs.add(DataTypeConstr.create(trueId, new ArrayList<TypedVar>(0)));
    constrs.add(DataTypeConstr.create(falseId, new ArrayList<TypedVar>(0)));
    List<DataTypeItem> items = new ArrayList<DataTypeItem>(1);
    items.add(DataTypeItem.create(typeId, constrs, null));
    DataType decl = DataType.create(items);
    checkDecl(decl);
    result.trueValueSymbol = (FunctionSymbol)trueId.getSymbol(); 
    result.falseValueSymbol = (FunctionSymbol)falseId.getSymbol(); 
  }
  
  /****************************************************************************
   * Create declaration for builtin type.
   * @param tsymbol the symbol for the type.
   ***************************************************************************/
  private void typeDecl(TypeSymbol tsymbol)
  {
    TypeDecl decl = TypeDecl.create(tsymbol.id, null, null);
    checkDecl(decl);
  }
  
  /****************************************************************************
   * introduce the "natural number" type
   ***************************************************************************/
  private void natType()
  {
    Id id = Id.create(AST.NATNAME);
    List<Exp> args = new ArrayList<Exp>(2);
    args.add(Var.create(Id.create(AST.VALUENAME)));
    Apply zero = Apply.create(Id.create("0"), new ArrayList<Exp>(0));
    args.add(zero);
    TypeDecl decl = TypeDecl.create(id, Type.create(result.intSymbol.id),
        Apply.create(Id.create(AST.GREATEREQUALNAME), args));
    checkDecl(decl);
    result.natSymbol = (TypeSymbol)id.getSymbol();
    result.intFuns.add((FunctionSymbol)zero.id.getSymbol());
  }
  
  /****************************************************************************
   * introduce the "nonzero number" type
   ***************************************************************************/
  private void nonZeroType()
  {
    Id id = Id.create(AST.NONZERONAME);
    List<Exp> args = new ArrayList<Exp>(2);
    args.add(Var.create(Id.create(AST.VALUENAME)));
    args.add(Apply.create(Id.create("0"), new ArrayList<Exp>(0)));
    TypeDecl decl = TypeDecl.create(id, Type.create(result.intSymbol.id), 
        Not.create(Apply.create(Id.create(AST.EQUALNAME), args)));
    checkDecl(decl);
    result.nonZeroSymbol = (TypeSymbol)id.getSymbol();
  }
  
  // auxiliaries for defining axioms
  private static Var var(String name) 
  { return Var.create(Id.create(name)); }
  private static List<Exp> args(Exp... args) 
  { return new ArrayList<Exp>(Arrays.asList(args)); }
  private static Apply apply(String name, Exp... args) 
  { return Apply.create(Id.create(name), args(args)); }
  private TypedVar intVar(String name)
  { return TypedVar.create(Id.create(name), Type.create(Id.create(AST.INTNAME))); }
  private Forall forallInt(String name, Exp formula)
  { return Forall.create(intVar(name), formula); }
  
  /****************************************************************************
   * Generate an axiom on the integers.
   * @param name the name of the axiom.
   * @param formula its formula.
   ***************************************************************************/
  private void intAxiom(String name, Exp formula)
  { 
    Id id = Id.create(AST.internal(name));
    Axiom axiom = Axiom.create(id, formula);
    checkDecl(axiom); 
    FormulaSymbol fsymbol = (FormulaSymbol)id.getSymbol();
    result.intAxioms.add(fsymbol);
  }
  
  /****************************************************************************
   * Read integer axioms from resource.
   ***************************************************************************/
  private void readIntAxioms()
  {
    InputStream stream = risctp.Main.class.getResourceAsStream("/" + INTAXIOMSPATH);
    if (stream == null)
    {
      writer.println("ERROR: cannot read file " + INTAXIOMSPATH);
      return;
    }
    Problem problem = Main.getMain().parseStream(stream);
    for (Decl decl : problem.decls)
    {
      if (!(decl instanceof Axiom)) continue;
      Axiom axiom = (Axiom)decl;
      intAxiom(axiom.id.toString(), axiom.formula);
    }
  }
  
  /****************************************************************************
   * Read non-linear arithmetic axioms from resource.
   ***************************************************************************/
  private void readNonLinearAxioms()
  {
    InputStream stream = risctp.Main.class.getResourceAsStream("/" + NONLINEARAXIOMSPATH);
    if (stream == null)
    {
      writer.println("ERROR: cannot read file " + NONLINEARAXIOMSPATH);
      return;
    }
    Problem problem = Main.getMain().parseStream(stream);
    for (Decl decl : problem.decls)
    {
      if (!(decl instanceof Axiom)) continue;
      Axiom axiom = (Axiom)decl;
      intAxiom(axiom.id.toString(), axiom.formula);
    }
  }
  
  /****************************************************************************
   * Generate axiomatization of the integers (heuristical, i.e., incomplete).
   ***************************************************************************/
  public void declareIntAxioms()
  {
    // the equality axioms used by Vampire according to [Langenreither, 2024]
    // x+y = y+x
    intAxiom("comm+", forallInt("x", forallInt("y",
        apply(AST.EQUALNAME,  
            apply(AST.PLUSNAME, var("x"), var("y")),
            apply(AST.PLUSNAME, var("y"), var("x"))))));
    // x+(y+z) = (x+y)+z
    intAxiom("assoc+", forallInt("x", forallInt("y", forallInt("z",
        apply(AST.EQUALNAME,  
            apply(AST.PLUSNAME, var("x"), apply(AST.PLUSNAME, var("y"), var("z"))),
            apply(AST.PLUSNAME, apply(AST.PLUSNAME, var("x"), var("y")), var("z")))))));
    // x+0 = x
    intAxiom("neut+", forallInt("x",
        apply(AST.EQUALNAME,
            apply(AST.PLUSNAME, var("x"), apply("0")),
            var("x"))));
    // x+(-x) = 0
    intAxiom("inv+", forallInt("x",
        apply(AST.EQUALNAME,
            apply(AST.PLUSNAME, var("x"), apply(AST.MINUSNAME, var("x"))),
            apply("0"))));
    // x-y = x+(-y)
    intAxiom("def-", forallInt("x", forallInt("y",
        apply(AST.EQUALNAME,  
            apply(AST.MINUSNAME, var("x"), var("y")),
            apply(AST.PLUSNAME, var("x"), apply(AST.MINUSNAME, var("y")))))));
    // x*y = y*x
    intAxiom("comm*", forallInt("x", forallInt("y",
        apply(AST.EQUALNAME,  
            apply(AST.MULTNAME, var("x"), var("y")),
            apply(AST.MULTNAME, var("y"), var("x"))))));
    // x*(y*z) = (x*y)*z
    intAxiom("assoc*", forallInt("x", forallInt("y", forallInt("z",
        apply(AST.EQUALNAME,  
            apply(AST.MULTNAME, var("x"), apply(AST.MULTNAME, var("y"), var("z"))),
            apply(AST.MULTNAME, apply(AST.MULTNAME, var("x"), var("y")), var("z")))))));
    // x*1 = x
    intAxiom("neut*", forallInt("x",
        apply(AST.EQUALNAME,
            apply(AST.MULTNAME, var("x"), apply("1")),
            var("x"))));
    // x*0 = 0
    intAxiom("absorb*", forallInt("x",
        apply(AST.EQUALNAME,
            apply(AST.MULTNAME, var("x"), apply("0")),
            apply("0"))));
    // -(-x) = x
    intAxiom("inv-", forallInt("x",
        apply(AST.EQUALNAME,
            apply(AST.MINUSNAME, apply(AST.MINUSNAME, var("x"))),
            var("x"))));
    // -(x+y) = (-x)+(-y)
    intAxiom("distrib-", forallInt("x", forallInt("y",
        apply(AST.EQUALNAME,  
            apply(AST.MINUSNAME, apply(AST.PLUSNAME, var("x"), var("y"))),
            apply(AST.PLUSNAME, apply(AST.MINUSNAME, var("x")), apply(AST.MINUSNAME, var("y")))))));
    // x*(y+z) = (x*y)+(x*z)
    intAxiom("distrib*", forallInt("x", forallInt("y", forallInt("z",
        apply(AST.EQUALNAME,  
            apply(AST.MULTNAME, var("x"), apply(AST.PLUSNAME, var("y"), var("z"))),
            apply(AST.PLUSNAME, apply(AST.MULTNAME, var("x"), var("y")), apply(AST.MULTNAME, var("x"), var("z"))))))));

    // we give inequalities with 3 variables low priority
    // an inequality axiom missing in Vampire
    // (x < y) /\ (0 < z) => (x*z < y*z)
    intAxiom("preserve<*", forallInt("x", forallInt("y", forallInt("z",
        Imp.create(
            And.create(
                apply(AST.LESSNAME, var("x"), var("y")),
                apply(AST.LESSNAME, apply("0"), var("z"))),
            apply(AST.LESSNAME, apply(AST.MULTNAME, var("x"), var("z")), apply(AST.MULTNAME, var("y"), var("z"))))))));  
    // inequality axiom used by Vampire:
    // (x < y) => (x+z < y+z)
    intAxiom("preserve<+", forallInt("x", forallInt("y", forallInt("z",
        Imp.create(
            apply(AST.LESSNAME, var("x"), var("y")),
            apply(AST.LESSNAME, apply(AST.PLUSNAME, var("x"), var("z")), apply(AST.PLUSNAME, var("y"), var("z"))))))));
    // (x < y) /\ (y < z) => (x < z) [ wrong: ~(x < z) in Vampire paper ]
    intAxiom("trans<", forallInt("x", forallInt("y", forallInt("z",
        Imp.create(
            And.create(
                apply(AST.LESSNAME, var("x"), var("y")),
                apply(AST.LESSNAME, var("y"), var("z"))),
            apply(AST.LESSNAME, var("x"), var("z")))))));  
    // additional inequalities to simplify proofs involving <=
    // (x <= y) /\ (y <= z) => (x <= z)
    intAxiom("trans<=", forallInt("x", forallInt("y", forallInt("z",
        Imp.create(
            And.create(
                apply(AST.LESSEQUALNAME, var("x"), var("y")),
                apply(AST.LESSEQUALNAME, var("y"), var("z"))),
            apply(AST.LESSEQUALNAME, var("x"), var("z")))))));
    // (x <= y) /\ (y < z) => (x < z) 
    intAxiom("trans1<=", forallInt("x", forallInt("y", forallInt("z",
        Imp.create(
            And.create(
                apply(AST.LESSEQUALNAME, var("x"), var("y")),
                apply(AST.LESSNAME, var("y"), var("z"))),
            apply(AST.LESSNAME, var("x"), var("z")))))));
    // (x < y) /\ (y <= z) => (x < z) 
    intAxiom("trans2<=", forallInt("x", forallInt("y", forallInt("z",
        Imp.create(
            And.create(
                apply(AST.LESSNAME, var("x"), var("y")),
                apply(AST.LESSEQUALNAME, var("y"), var("z"))),
            apply(AST.LESSNAME, var("x"), var("z")))))));
    
    // trichotomy used by Vampire
    // (x < y) \/ (y < x) \/ (x = y)
    {
      // make equality not target of MESON proof
      Apply eq = apply(AST.EQUALNAME, var("x"), var("y"));
      eq.setNotGoal(true);
      intAxiom("trichotomy", forallInt("x", forallInt("y",
          Or.create(
              apply(AST.LESSNAME, var("x"), var("y")),
              Or.create(
                  apply(AST.LESSNAME, var("y"), var("x")),
                  eq)))));
    }
    
    // actually equivalence, we just use one direction
    // (x < y) \/ (y < x) => ~(x = y)
    intAxiom("notequal<", forallInt("x", forallInt("y",
        Imp.create(
            Or.create(
                apply(AST.LESSNAME, var("x"), var("y")),
                apply(AST.LESSNAME, var("y"), var("x"))),
            Not.create(apply(AST.EQUALNAME, var("x"), var("y")))))));

    // reduction of != to =
    // x != y <=> ~(x = y)
    {
      // make equality not target of MESON proof
      Apply eq = apply(AST.EQUALNAME, var("y"), var("x"));
      // eq.setNotGoal(true);
      intAxiom("neqdef", forallInt("x", forallInt("y",
          Equiv.create(
              apply(AST.NOTEQUALNAME, var("x"), var("y")),
              Not.create(eq)))));
    }
    
    // we assume >,>= have been replaced by< (see risct.problem.Arithmetic)

    // additional inequalities to simplify proofs
    // (x = y => ~(x < y))
    intAxiom("irrefl2<", forallInt("x", forallInt("y",
        Imp.create(
            apply(AST.EQUALNAME, var("x"), var("y")),
            Not.create(apply(AST.LESSNAME, var("x"), var("y")))))));
    // (x = y => x <= y)
    intAxiom("refl<=", forallInt("x", forallInt("y",
        Imp.create(
            apply(AST.EQUALNAME, var("x"), var("y")),
            apply(AST.LESSEQUALNAME, var("x"), var("y"))))));
    // (x <= y) <=> !(y < x)
    intAxiom("def<=", forallInt("x", forallInt("y",
        Equiv.create(
            apply(AST.LESSEQUALNAME, var("x"), var("y")),
            Not.create(apply(AST.LESSNAME, var("y"), var("x")))))));

    // inequality used by Vampire
    // (x < y) <=> ~(y < x+1)
    intAxiom("equiv<", forallInt("x", forallInt("y",
        Equiv.create(
            apply(AST.LESSNAME, var("x"), var("y")),
            Not.create(apply(AST.LESSNAME, var("y"), apply(AST.PLUSNAME, var("x"), apply("1"))))))));

    // some more helpful equalities and inequalities
    // (x < y) <=> (x+1 <= y)
    intAxiom("plus1<=", forallInt("x", forallInt("y",
        Equiv.create(
            apply(AST.LESSNAME, var("x"), var("y")),
            apply(AST.LESSEQUALNAME, apply(AST.PLUSNAME, var("x"), apply("1")), var("y"))))));
    // (x < y) <=> (x <= y-1)
    intAxiom("minus1<=", forallInt("x", forallInt("y",
        Equiv.create(
            apply(AST.LESSNAME, var("x"), var("y")),
            apply(AST.LESSEQUALNAME, var("x"), apply(AST.MINUSNAME, var("y"), apply("1")))))));
    
    // basic numerics
    // x-1 < x
    intAxiom("minus1<", forallInt("x",
        apply(AST.LESSNAME, apply(AST.MINUSNAME, var("x"), apply("1")), var("x"))));  
    // x < x+1
    intAxiom("plus1<", forallInt("x",
        apply(AST.LESSNAME, var("x"), apply(AST.PLUSNAME, var("x"), apply("1")))));
    // 0 < 1
    intAxiom("0<1", apply(AST.LESSNAME, apply("0"), apply("1")));
    
    // an inequality used in Vampire
    // ~(x < x)
    intAxiom("irrefl<", forallInt("x",
        Not.create(
            apply(AST.LESSNAME, var("x"), var("x")))));
  }
  
  /****************************************************************************
   * Process declaration.
   * @param decl the declaration to be processed.
   ***************************************************************************/
  private void checkDecl(Decl decl)
  {
    // set new declaration context
    Decl oldDecl = currentDecl;
    currentDecl = decl;
    
    // check declaration
    check(decl);
    
    // add declaration to result list
    result.decls.add(decl);
    
    // add defined function to symbol list
    if (decl instanceof Function)
    {
      Function decl0 = (Function)decl;
      if (decl0.exp != null)
      {
        FunctionSymbol fsymbol = (FunctionSymbol)decl0.id.getSymbol();
        result.defFuns.add(fsymbol);
      }
    }
    
    // process post-commands, new list necessary for recursive processing
    List<Runnable> ps = postDecl;
    postDecl = new ArrayList<Runnable>();
    for (Runnable p : ps) p.run();
  
    // restore declaration context
    currentDecl = oldDecl;
  }
  
  /****************************************************************************
   * create binary function with given name and types.
   * @param name the function name.
   * @param tsymbol0 the type of the argument.
   * @param tsymbol the type of the result.
   **************************************************************************/
  private void unaryFunction(String name, TypeSymbol tsymbol0, TypeSymbol tsymbol)
  {
    Id id = Id.create(name);
    List<TypedVar> tvars = new ArrayList<TypedVar>(1);
    tvars.add(TypedVar.create(Id.create("x"), tsymbol0.type));
    Function fun = Function.create(id, tvars, tsymbol.type, null);
    checkDecl(fun);
    FunctionSymbol fsymbol = (FunctionSymbol)id.getSymbol();
    result.intFuns.add(fsymbol);
  }
  
  /****************************************************************************
   * create binary function with given name and types.
   * @param name the function name.
   * @param tsymbol1 the type of the first argument.
   * @param tsymbol2 the type of the second argument.
   * @param tsymbol the type of the result.
   **************************************************************************/
  private void binaryFunction(String name, 
    TypeSymbol tsymbol1, TypeSymbol tsymbol2, TypeSymbol tsymbol)
  {
    Id id = Id.create(name);
    List<TypedVar> tvars = new ArrayList<TypedVar>(2);
    tvars.add(TypedVar.create(Id.create("x1"), tsymbol1.type));
    tvars.add(TypedVar.create(Id.create("x2"), tsymbol2.type));
    Function fun = Function.create(id, tvars, tsymbol.type, null);
    checkDecl(fun);
    FunctionSymbol fsymbol = (FunctionSymbol)id.getSymbol();
    result.intFuns.add(fsymbol);
  }
  
  /****************************************************************************
   * generates equality and inequality functions on a newly generated type.
   * @param tsymbol the type.
   * @param trailing true if the declarations are to be added to trailingDecls.
   ***************************************************************************/
  private void typeOperations(TypeSymbol tsymbol, boolean trailing)
  {
    Runnable addeq = ()->
    {
      Id undefId = Id.create(AST.unquote(tsymbol.id.toString()) + AST.internal(AST.UNDEFNAME));
      // make maps to bool (aka sets) false for values outside the domain
      Exp exp = tsymbol.root == result.boolSymbol ? False.create() : null;
      Function undef = Function.create(undefId, 
          new ArrayList<TypedVar>(0), tsymbol.root.type, exp);
      Id id1 = Id.create("x");
      Id id2 = Id.create("y");
      List<TypedVar> tvars = new ArrayList<TypedVar>(2);
      tvars.add(TypedVar.create(id1, tsymbol.type));
      tvars.add(TypedVar.create(id2, tsymbol.type));
      Id equalId = Id.create(AST.EQUALNAME);
      Function equality = Function.create(equalId, 
          tvars, result.boolSymbol.type, null);
      Id notequalId = Id.create(AST.NOTEQUALNAME);
      List<Exp> exps = new ArrayList<Exp>(2);
      exps.add(Var.create(id1));
      exps.add(Var.create(id2));
      // for integers inequality is separately axiomatized
      Function inequality = Function.create(notequalId, 
          tvars, result.boolSymbol.type, 
          intAxioms && tsymbol.root == result.intSymbol? null : 
            Not.create(Apply.create(equalId, exps)));
      checkDecl(undef);
      checkDecl(equality);
      checkDecl(inequality);
      result.undefs.put(tsymbol, (FunctionSymbol)undefId.getSymbol());
      result.equalities.put(tsymbol, (FunctionSymbol)equalId.getSymbol());
      result.inequalities.put(tsymbol, (FunctionSymbol)notequalId.getSymbol());
    };
    if (trailing)
      postDecl.add(addeq);
    else
      addeq.run();
    /*
    List<TypeSymbol> tsymbols2 = new ArrayList<TypeSymbol>(2);
    tsymbols2.add(tsymbol.root);
    tsymbols2.add(tsymbol.root);
    {
      Id equalId = Id.create(AST.EQUALNAME);
      FunctionSymbol equalSymbol = getFunction(equalId, tsymbols2);
      if (equalSymbol == null)
      {
        equalSymbol = new FunctionSymbol(equalId, tsymbols2, result.boolSymbol);
        equalId.setSymbol(equalSymbol);
        env.putFunction(equalSymbol);
      }
    }
    {
      Id notEqualId = Id.create(AST.NOTEQUALNAME);
      FunctionSymbol notEqualSymbol = getFunction(notEqualId, tsymbols2);
      if (notEqualSymbol == null)
      {
        notEqualSymbol = new FunctionSymbol(notEqualId, tsymbols2, result.boolSymbol);
        notEqualId.setSymbol(notEqualSymbol);
        env.putFunction(notEqualSymbol);
      }
    }
    return tsymbol;
    */
  }  
  
  /***************************************************************************
   * The exception raised by the type checker on an error.
   **************************************************************************/
  private static final class TypeError extends RuntimeException
  {
    private static final long serialVersionUID = 28091967L;
    // public final AST tree;

    /*************************************************************************
     * Create a type error with the denoted message originating from 
     * type-checking the denoted phrase.
     * @param tree the abstract syntax tree of the phrase
     * @param msg the message
     ************************************************************************/
    private TypeError(AST tree, String msg) 
    { 
      super(msg); 
      // this.tree = tree;
    }
    public static TypeError raise(AST tree, String msg)
    {
      throw new TypeError(tree, msg);
    }
  }
  
  // -------------------------------------------------------------------------
  //
  // Type-checking auxiliaries
  //
  // -------------------------------------------------------------------------
  
  /***************************************************************************
   * Get string "id(types)" from identifier and list of type symbols.
   * @param id the identifier
   * @param tsymbols the list of type symbols.
   * @return the string.
   **************************************************************************/
  public static String toString(Id id, List<TypeSymbol> tsymbols)
  {
    List<Type> texps = new ArrayList<Type>(tsymbols.size());
    for (TypeSymbol tsymbol : tsymbols) texps.add(tsymbol.root.type);
    Type[] texps0 = texps.toArray(new Type[texps.size()]);
    return id + "(" + AST.toString(texps0, ",") + ")";
  }
  
  /****************************************************************************
   * Has list of variable symbols a symbol with a specific id?
   * @param symbols the symbol list.
   * @param id the id.
   * @return true if the list has a symbol with the id, false otherwise.
   ***************************************************************************/
  private static boolean has(List<VariableSymbol> symbols, Id id)
  {
    for (VariableSymbol symbol : symbols)
    {
      if (id.equals(symbol.id)) return true;
    }
    return false;
  }

  /****************************************************************************
   * Get function with denoted name and parameter types.
   * @param id the name of the function.
   * @param tsymbols the types of its parameters.
   * @return the function (null, if the denoted function does not exist)
   ***************************************************************************/
  public FunctionSymbol getFunction(Id id, List<TypeSymbol> tsymbols)
  {
    // special functions, must be tested before general case, because
    // we have overlapping definitions of operations for compound types
    String[] strings = id.getStrings();
    switch (strings[0])
    {
    case AST.MAPSELECTNAME:
    {
      if (strings.length == 1 && tsymbols.size() == 2)
      {
        // we look up for function with matching *root* map type
        TypeSymbol tsymbol = tsymbols.get(0).root;
        FunctionSymbol fun = result.mapSelectors.get(tsymbol);
        if (fun == null) return null;
        if (fun.tsymbols.get(1).root!= tsymbols.get(1).root) return null;
        return fun;
      }
      break;
    }
    case AST.MAPSTORENAME:
    {
      if (strings.length == 1 && tsymbols.size() == 3)
      {
        // we look up for function with matching *root* map type
        TypeSymbol tsymbol = tsymbols.get(0).root;
        FunctionSymbol fun = result.mapStores.get(tsymbol);
        if (fun == null) return null;
        if (fun.tsymbols.get(1).root!= tsymbols.get(1).root) return null;
        return fun;
      }
      break;
    }
    case AST.MAPCONSTRUCTNAME:
      if (strings.length == 3 && tsymbols.size() == 1)
      {
        Id keyId = Id.create(strings[1]);
        TypeSymbol ksymbol = env.getType(keyId);
        if (ksymbol == null) return null;
        Id valueId = Id.create(strings[2]);
        TypeSymbol vsymbol = env.getType(valueId);
        if (vsymbol == null) return null;
        TypeSymbol esymbol = tsymbols.get(0);
        if (vsymbol.root != esymbol.root) return null;
        List<TypeSymbol> tsymbols0 = new ArrayList<TypeSymbol>(2);
        tsymbols0.add(ksymbol);
        tsymbols0.add(vsymbol);
        TypeSymbol tsymbol = compoundType(Id.create(AST.MAPNAME), tsymbols0);
        // we look up for function with matching *root* map type
        FunctionSymbol fsymbol = result.mapConstructors.get(tsymbol.root);
        id.setSymbol(fsymbol);
        return fsymbol;
      }
      break;
    case AST.TUPLECONSTRUCTNAME:
      // generate type with corresponding constructor
      compoundType(Id.create(AST.TUPLENAME), tsymbols);
      break;
    }
    // default case, we look up for a function with matching *root* types
    return env.getFunction(id, tsymbols);
  }

  /****************************************************************************
   * Apply function to single argument.
   * @param fun the function.
   * @param arg the argument.
   * @return the application expression.
   ***************************************************************************/
  public static Exp applyFun(FunctionSymbol fun, Exp arg)
  {
    List<Exp> exps = new ArrayList<Exp>();
    exps.add(arg);
    return Apply.create(fun.id, exps);
  }
  
  /****************************************************************************
   * Execute command in current variable scope and leave scope afterwards.
   * @param command the command to be executed.
   **************************************************************************/
  private void inVariableScope(Runnable command)
  {
    try
    {
      command.run();
    }
    catch(TypeError e)
    {
      env.exitVariableScope();
      throw e;
    }
    env.exitVariableScope();
  }
  
  /****************************************************************************
   * Execute command in context of a condition.
   * @param context a formula that is true when the command is executed.
   * @param command the command.
   ***************************************************************************/
  private void impliesContext(Exp context, Runnable command)
  {
    UnaryOperator<Exp> typeTheoremFun0 = typeTheoremFun;
    typeTheoremFun = (Exp formula)-> 
      typeTheoremFun0.apply(Imp.create(context,formula));
    try
    {
      command.run();
    }
    catch(TypeError e)
    {
      typeTheoremFun = typeTheoremFun0;
      throw e;
    }
    typeTheoremFun = typeTheoremFun0;
  }
  
  /****************************************************************************
   * Execute command in context of universally quantified variables
   * @param tvars the universally quantified variables.
   * @param command the command.
   ***************************************************************************/
  private void forallContext(TypedVar[] tvars, Runnable command)
  {
    UnaryOperator<Exp> typeTheoremFun0 = typeTheoremFun;
    int n = tvars.length;
    typeTheoremFun = (Exp formula)-> 
    {
      for (int i = n-1; i >= 0; i--)
        formula = Forall.create(tvars[i], formula);
      return typeTheoremFun0.apply(formula);
    };
    try
    {
      command.run();
    }
    catch(TypeError e)
    {
      typeTheoremFun = typeTheoremFun0;
      throw e;
    }
    typeTheoremFun = typeTheoremFun0;
  }
  
  /****************************************************************************
   * Execute command in context of let binders.
   * @param binders the binders.
   * @param command the command.
   ***************************************************************************/
  private void letContext(LetBinder[] binders, Runnable command)
  {
    UnaryOperator<Exp> typeTheoremFun0 = typeTheoremFun;
    typeTheoremFun = (Exp formula)-> 
      typeTheoremFun0.apply(Let.create(Arrays.asList(binders), formula));
    try
    {
      command.run();
    }
    catch(TypeError e)
    {
      typeTheoremFun = typeTheoremFun0;
      throw e;
    }
    typeTheoremFun = typeTheoremFun0;
  }
  /****************************************************************************
   * Get an expression defining a natural number.
   * @param number the number.
   * @return the expression (may be null).
   ***************************************************************************/
  private Exp natExp(int number)
  {
    // zero and one remain undefined, characterized by axioms
    if (number < 2) return null;
    
    // for simplicity, we might also define bigger numbers by powers of ten
    if (number > 9) return null;
    
    // define number as (number-1)+1
    int pred = number-1;
    createNat(pred);
    createNat(1);
    return apply(AST.PLUSNAME, apply(Integer.toString(pred)), apply("1"));
  }
  
  /****************************************************************************
   * Ensure that there is a definition of number.
   * @param number a number greater equal 2.
   ***************************************************************************/
  private void createNat(int number)
  {
    String name = Integer.toString(number);
    for (FunctionSymbol fun : result.intFuns)
    {
      if (name.equals(fun.id.toString())) return;
    }
    Id id = Id.create(name);
    // use type "Int" rather than "Nat" to avoid type-checking axiom 
    // (which is difficult to prove without SMT)
    Function fun =
        Function.create(id, new ArrayList<TypedVar>(), result.intSymbol.type, 
            natExp(number));
    checkDecl(fun);
    FunctionSymbol fsymbol = (FunctionSymbol)id.getSymbol();
    result.intFuns.add(fsymbol);
  }
  
  // -------------------------------------------------------------------------
  //
  // Problems and declarations
  //
  // -------------------------------------------------------------------------
   
  /***************************************************************************
   * Type-check problem specification
   * of the problem (potentially with additional declarations added).
   * @param problem the problem specification.
   * @return the result of the check (null, in case of errors)
   ***************************************************************************/
  public ProofProblem check(Problem problem)
  {
    for (Decl d : problem.decls)
      checkDeclaration(d);
    return getResult();
  }
  
  /****************************************************************************
   * Check declaration (catching any output errors that may have occurred.
   * @param d the declaration.
   ***************************************************************************/
  public void checkDeclaration(Decl d)
  {      
    try
    {
      checkDecl(d);
    }
    catch (TypeError e)
    {
      errors++;
      writer.println("Type error " + errors + ": " + e.getMessage() + ".");
      writer.println("  " + d);
    }
  }
  
  /****************************************************************************
   * Declarations
   ***************************************************************************/
  private void check(Decl decl)
  {
    if (decl instanceof TypeDecl)
      check((TypeDecl)decl);
    else if (decl instanceof DataType)
      check((DataType)decl);
    else if (decl instanceof Function)
      check((Function)decl);
    else if (decl instanceof Axiom)
      check((Axiom)decl);
    else if (decl instanceof Theorem)
      check((Theorem)decl);
    else
      TypeError.raise(decl, "unknown kind of declaration");
  }
  private void check(TypeDecl decl)
  {
    TypeSymbol tsymbol0 = null;
    Exp exp0 = null;
    boolean sat = decl.exp == null;
    if (decl.type != null) 
    {       
      tsymbol0 = check(decl.type);
      FunctionSymbol pred0 = tsymbol0.getPred();
      sat = sat && tsymbol0.isSat();
      if (pred0 != null) exp0 = applyFun(pred0, Var.create(Id.create(AST.VALUENAME)));
      exp0 = Exp.and(exp0, decl.exp);
    }
    Id id = decl.id;
    // type symbol may be already set from map type declaration
    TypeSymbol tsymbol = (TypeSymbol)id.getSymbol();
    if (tsymbol == null)
    {
      tsymbol = env.getType(id);
      if (tsymbol != null)
        TypeError.raise(id, "type " + id + " was already declared");
      Type type = Type.create(id);
      tsymbol = new TypeSymbol(id, type, tsymbol0);
    }
    if (sat) tsymbol.setSat(true);
    createTypePred(tsymbol, exp0);
    if (decl.type == null) typeOperations(tsymbol, true);
    id.setSymbol(tsymbol);
    env.putType(tsymbol);
  }
  private void createTypePred(TypeSymbol tsymbol, Exp exp)
  {
    if (exp == null) return;
    List<TypedVar> tvars = new ArrayList<TypedVar>();
    // use as type of "value" the root type of tsymbol
    tvars.add(TypedVar.create(Id.create(AST.VALUENAME), tsymbol.root.type));
    Type boolType = Type.create(Id.create(AST.BOOLNAME), new ArrayList<Type>());
    List<String> strings = new ArrayList<String>();
    strings.addAll(Arrays.asList(tsymbol.id.getStrings()));
    strings.add("type");
    Id id0 = Id.create(strings);
    Function decl = Function.create(id0, tvars, boolType, exp);
    // use as type of "value" the root type of tsymbol
    Id param = Id.create(AST.VALUENAME);
    TypedVar tvar = TypedVar.create(param, tsymbol.root.type);
    List<Exp> args = new ArrayList<Exp>();
    args.add(Var.create(param));
    postDecl.add(()->
    {
      checkDecl(decl);
      FunctionSymbol pred = (FunctionSymbol)id0.getSymbol();
      tsymbol.setPred(pred);
      if (!tsymbol.isSat())
        addTypeCheckingTheorem(Exists.create(tvar, Apply.create(id0, args)));
    });
  }
  private void check(DataType decl)
  {
    List<TypeSymbol> tsymbols = new ArrayList<TypeSymbol>();
    for (DataTypeItem item : decl.items)
      tsymbols.add(declare(item));
    if (generateHeight)
    {
      for (TypeSymbol tsymbol : tsymbols)
        declareHeightFun(tsymbol);
    }
    for (DataTypeItem item : decl.items)
      createTypePred(item);
    for (TypeSymbol tsymbol : tsymbols)
      typeOperations(tsymbol, true);
    for (DataTypeItem item : decl.items)
      check(item);
    if (dataAxioms)
    {
      for (DataTypeItem item : decl.items)
        addDataAxioms(item);
      // axiomatization to be provided by user/translation
      // for (DataTypeItem item : decl.items)
      //   axiomatizeHeightFun(item, tsymbols);
    }
  }
  private TypeSymbol declare(DataTypeItem item)
  {
    Id id = item.id;
    TypeSymbol tsymbol = env.getType(id);
    if (tsymbol != null)
      TypeError.raise(item, "type " + id + " was already declared");
    // type symbol may be already set from tuple type declaration
    tsymbol = (TypeSymbol)id.getSymbol();
    if (tsymbol == null)
    {
      Type type = Type.create(id);
      tsymbol = new TypeSymbol(id, type, null);
      id.setSymbol(tsymbol);
    }
    env.putType(tsymbol);
    result.dataTypes.add(tsymbol);
    result.dataMap.put(tsymbol, item);
    return tsymbol;
  }
  private void createTypePred(DataTypeItem item)
  {
    TypeSymbol tsymbol = (TypeSymbol)item.id.getSymbol();
    createTypePred(tsymbol, item.exp);
  }
  private void check(DataTypeItem item)
  {
    postDecl.add(()->
    {
      TypeSymbol tsymbol = (TypeSymbol)item.id.getSymbol();
      for (DataTypeConstr constr : item.constrs)
        check(tsymbol, constr);
    });
  }
  private void check(TypeSymbol tsymbol, DataTypeConstr constr)
  {
    Id id = constr.id;
    int n = constr.tvars.length;
    // the list of selector function symbols
    List<FunctionSymbol> fsymbols = new ArrayList<FunctionSymbol>(n);
    {
      // type-checked the selector variables
      List<VariableSymbol> vsymbols = check(constr.tvars);
      // replace in selector ids the variable symbols by function symbols
      for (VariableSymbol vsymbol : vsymbols)
      {
        Id id0 = vsymbol.id;
        List<TypeSymbol> tsymbols = new ArrayList<TypeSymbol>();
        tsymbols.add(tsymbol);
        FunctionSymbol fsymbol = new FunctionSymbol(id0,
            tsymbols, vsymbol.tsymbol);
        id0.setSymbol(fsymbol);
        fsymbols.add(fsymbol);
      }
    }
    
    // create fresh typed variable list for selectors
    List<TypedVar> tvars = new ArrayList<TypedVar>(n);
    for (FunctionSymbol fsymbol : fsymbols)
    {
      TypeSymbol tsymbol1 = fsymbol.tsymbol;
      tvars.add(TypedVar.create(Id.create(fsymbol.id),tsymbol1.type)); 
    }
    
    // create constructor function
    Function cfun = Function.create(id, tvars, tsymbol.type, null);
    checkDecl(cfun);
    FunctionSymbol csymbol = (FunctionSymbol)id.getSymbol();
    result.constructors.add(csymbol);
    result.selectorMap.put(csymbol, fsymbols);
    
    // create injectivity axiom for constructor
    if (dataAxioms && n != 0) 
    {
      List<TypedVar> tvars0 = new ArrayList<TypedVar>(2*n);
      List<Exp> exps1 = new ArrayList<Exp>(n);
      List<Exp> exps2 = new ArrayList<Exp>(n);
      for (TypedVar tvar : tvars)
      {
        String name = AST.unquote(tvar.id.toString());
        Id id1 = Id.create(name + AST.internal("1"));
        Id id2 = Id.create(name + AST.internal("2"));
        tvars0.add(TypedVar.create(id1, tvar.type));
        tvars0.add(TypedVar.create(id2, tvar.type));
        exps1.add(Var.create(id1));
        exps2.add(Var.create(id2));
      }
      Exp formula = null;
      Iterator<Exp> iterator1 = exps1.iterator();
      Iterator<Exp> iterator2 = exps2.iterator();
      for (int i = 0; i < n; i++)
      {
        List<Exp> args = new ArrayList<Exp>(2);
        args.add(iterator1.next());
        args.add(iterator2.next());
        // make resulting equality not target of MESON proof
        Apply apply = Apply.create(Id.create(AST.EQUALNAME), args);
        apply.setNotGoal(true);
        formula = Exp.and(formula, apply);
      }
      List<Exp> args = new ArrayList<Exp>(2);
      args.add(Apply.create(id, exps1));
      args.add(Apply.create(id, exps2));
      formula = Imp.create(Apply.create(Id.create(AST.EQUALNAME), args), formula);
      ListIterator<TypedVar> iterator = tvars0.listIterator(2*n);
      while (iterator.hasPrevious()) formula = Forall.create(iterator.previous(), formula);
      Id aid = Id.create("inject" + AST.internal(Integer.toString(result.decls.size())));
      Axiom axiom = Axiom.create(aid, formula);
      checkDecl(axiom);
      result.dataTypeAxioms.add((FormulaSymbol)aid.getSymbol());
    }
    
    // create selector functions
    for (FunctionSymbol fsymbol : fsymbols)
    {
      // selector declaration
      Id id0 = fsymbol.id;
      List<TypedVar> tvars0 = new ArrayList<TypedVar>(1);
      tvars0.add(TypedVar.create(Id.create("x"),tsymbol.type));
      Function fun = Function.create(id0, tvars0, fsymbol.tsymbol.type, null);
      checkDecl(fun);
      result.selectors.add(fsymbol);
      // selector axiom
      if (dataAxioms)
      {
        List<Exp> args0 = new ArrayList<Exp>(n);
        for (FunctionSymbol fsymbol0 : fsymbols)
          args0.add(Var.create(Id.create(fsymbol0.id)));
        List<Exp> args1 = new ArrayList<Exp>(n);
        args1.add(Apply.create(id, args0));
        List<Exp> args = new ArrayList<Exp>(2);
        args.add(Apply.create(id0, args1));
        args.add(Var.create(Id.create(fsymbol.id)));
        Exp formula = Apply.create(Id.create(AST.EQUALNAME), args);
        ListIterator<TypedVar> iterator = tvars.listIterator(n);
        while (iterator.hasPrevious())
        {
          TypedVar tvar = iterator.previous();
          TypedVar tvar0 = TypedVar.create(Id.create(tvar.id), tvar.type);
          formula = Forall.create(tvar0, formula);
        }
        Id aid = Id.create("select" + AST.internal(Integer.toString(result.decls.size())));
        Axiom axiom = Axiom.create(aid,formula);
        checkDecl(axiom);
        result.dataTypeAxioms.add((FormulaSymbol)aid.getSymbol());
      }
    }
    
    // create tester function
    {
      Id param = Id.create(AST.internal("x"));
      Id id0 = AST.testerId(id);
      TypedVar tvar = TypedVar.create(param, tsymbol.type);
      List<TypedVar> tvars0 = new ArrayList<TypedVar>(1);
      tvars0.add(tvar);
      // do not define but axiomatize later in addTesterAxiom1/2
      Function fun = Function.create(id0, tvars0, result.boolSymbol.type, null);
      checkDecl(fun);
      FunctionSymbol fsymbol = (FunctionSymbol)id0.getSymbol();
      result.testers.add(fsymbol);
      result.testerMap.put(csymbol, fsymbol);
    } 
  }
  private void addTesterAxiom2(DataTypeConstr constr)
  {
    Id param = Id.create(AST.internal("x"));
    Id aid = Id.create("test" + AST.internal(Integer.toString(result.decls.size())));
    Exp formula = testerFormula(param, constr);
    Apply app = testerApplication(constr, Var.create(param));
    // make assumption not goal of MESON proof
    app.setNotGoal(true);
    formula = Imp.create(app, formula);
    FunctionSymbol csymbol = (FunctionSymbol)constr.id.getSymbol();
    TypedVar tvar = TypedVar.create(param, csymbol.tsymbol.type);
    formula = Forall.create(tvar, formula);
    Axiom axiom = Axiom.create(aid, formula);
    checkDecl(axiom);
    result.dataTypeAxioms.add((FormulaSymbol)aid.getSymbol());
  }
  private void addTesterAxiom1(DataTypeConstr constr)
  {
    int n = constr.tvars.length;
    List<TypedVar> tvars1 = new ArrayList<TypedVar>(n);
    Exp capp = constructorApplication(constr, tvars1);
    Exp tapp = testerApplication(constr, capp);
    Exp aformula = tapp;
    ListIterator<TypedVar> iterator = tvars1.listIterator(n);
    while (iterator.hasPrevious())
    {
      TypedVar tvar = iterator.previous();
      aformula = Forall.create(tvar, aformula);
    }
    Id aid = Id.create("test" + AST.internal(Integer.toString(result.decls.size())));
    Axiom axiom = Axiom.create(aid, aformula);
    checkDecl(axiom);
    result.dataTypeAxioms.add((FormulaSymbol)aid.getSymbol());
  }
  private Apply testerApplication(DataTypeConstr constr, Exp arg)
  {
    FunctionSymbol csymbol = (FunctionSymbol)constr.id.getSymbol();
    FunctionSymbol tsymbol = result.testerMap.get(csymbol);
    List<Exp> args = new ArrayList<Exp>(1);
    args.add(arg);
    return Apply.create(tsymbol.id, args);
  }
  private Exp testerFormula(Id param, DataTypeConstr constr)
  {
    int n = constr.tvars.length;
    List<TypedVar> tvars = new ArrayList<TypedVar>(n);
    Exp app = constructorApplication(constr, tvars);
    List<Exp> args1 = new ArrayList<Exp>(n);
    // constructor application first to simplify MESON proofs
    args1.add(app);
    args1.add(Var.create(param));
    Exp formula = Apply.create(Id.create(AST.EQUALNAME), args1);
    ListIterator<TypedVar> iterator = tvars.listIterator(n);
    while (iterator.hasPrevious())
    {
      TypedVar tvar = iterator.previous();
      formula = Exists.create(tvar, formula);
    }
    return formula;
  }
  private Exp constructorApplication(DataTypeConstr constr, List<TypedVar> tvars)
  {
    Id id = constr.id;
    int n = constr.tvars.length;
    List<FunctionSymbol> fsymbols = new ArrayList<FunctionSymbol>(n);
    for (TypedVar tvar : constr.tvars)
    {
      FunctionSymbol fsymbol = (FunctionSymbol)tvar.id.getSymbol();
      fsymbols.add(fsymbol);
      TypeSymbol tsymbol1 = fsymbol.tsymbol;
      tvars.add(TypedVar.create(Id.create(fsymbol.id),tsymbol1.type)); 
    }
    List<Exp> args0 = new ArrayList<Exp>(n);
    for (FunctionSymbol fsymbol0 : fsymbols)
      args0.add(Var.create(Id.create(fsymbol0.id)));
    return Apply.create(id, args0);
  }
  private void declareHeightFun(TypeSymbol tsymbol)
  {
    List<TypedVar> params = new ArrayList<TypedVar>(1);
    params.add(TypedVar.create(Id.create("x"), Type.create(tsymbol.id)));
    Id heightId = Id.create(AST.HEIGHTNAME);
    Function heightFun = Function.create(heightId, params, 
        result.natSymbol.type, null);
    postDecl.add(()->{
      checkDecl(heightFun);
      FunctionSymbol hsymbol = (FunctionSymbol)heightId.getSymbol();
      result.heightMap.put(tsymbol, hsymbol);
    });
  }
  private void addDataAxioms(DataTypeItem item)
  {
    postDecl.add(()->
    {
      Id param = Id.create(AST.internal("x"));
      Exp formula = null;
      /*
      for (DataTypeConstr constr1 : item.constrs)
      {
        Exp conjunct = testerFormula(param, constr1);
        for (DataTypeConstr constr2 : item.constrs)
        {
          if (constr1 == constr2) continue;
          conjunct = Exp.and(conjunct, Not.create(testerFormula(param, constr2)));
        }
        formula = Exp.or(formula, conjunct);
      }
      */
      // more direct formulation simplifies MESON proof
      for (DataTypeConstr constr : item.constrs)
        formula = Exp.or(formula, testerApplication(constr, Var.create(param)));
      int i1 = 1;
      for (DataTypeConstr constr1 : item.constrs)
      {
        Exp tester1 = testerApplication(constr1, Var.create(param));
        int i2 = 1;
        for (DataTypeConstr constr2 : item.constrs)
        {
          if (i2 <= i1) { i2++; continue; }
          Exp tester2 = testerApplication(constr2, Var.create(param));
          formula = Exp.and(formula, Not.create(And.create(tester1, tester2)));
          i2++;
        }
        i1++;
      }
      TypeSymbol tsymbol = (TypeSymbol)item.id.getSymbol();
      formula = Forall.create(TypedVar.create(param, tsymbol.type), formula);
      Id aid = Id.create("datatype" + AST.internal(Integer.toString(result.decls.size())));
      Axiom axiom = Axiom.create(aid, formula);
      checkDecl(axiom);
      result.dataTypeAxioms.add((FormulaSymbol)aid.getSymbol());
      // add supplementary tester axiom to simplify MESON proof
      for (DataTypeConstr constr : item.constrs)
      {
        addTesterAxiom2(constr);
        addTesterAxiom1(constr);
      }
    });
  }
  
  /*
  private void axiomatizeHeightFun(DataTypeItem item, List<TypeSymbol> tsymbols)
  {
    Id tid = item.id;
    TypeSymbol tsymbol = (TypeSymbol)tid.getSymbol();
    FunctionSymbol hsymbol = checkResult.heightMap.get(tsymbol);
    List<MatchBinder> binders = new ArrayList<MatchBinder>();
    for (DataTypeConstr constr : item.constrs)
    {
      List<Exp> exps = new ArrayList<Exp>();
      Pattern pattern = heightTerms(tsymbols, constr, exps);
      Exp exp;
      if (exps.isEmpty())
        exp = Apply.create(Id.create("0"), new ArrayList<Exp>(0));
      else
      {
        List<Exp> args = new ArrayList<Exp>(2);
        args.add(Apply.create(Id.create("1"), new ArrayList<Exp>(0)));
        args.add(applyMax(exps));
        exp = Apply.create(Id.create(AST.PLUSNAME), args);
      }
      binders.add(MatchBinder.create(pattern, exp));
    }
    Id var = Id.create("__x");
    List<Exp> heightArgs = new ArrayList<Exp>(1);
    heightArgs.add(Var.create(var));
    Apply apply = Apply.create(hsymbol.id, heightArgs);
    Match match = Match.create(Var.create(var), binders);
    List<Exp> eqArgs = new ArrayList<Exp>(2);
    eqArgs.add(apply);
    eqArgs.add(match);
    Exp axiom = Forall.create(TypedVar.create(var, Type.create(tid)),
            Apply.create(Id.create(AST.EQUALNAME), eqArgs));
    Id axiomId = Id.create(new String[]{ "__" + AST.HEIGHTNAME, 
        Integer.toString(checkResult.decls.size()) });
    Decl decl = Axiom.create(axiomId, axiom);
    check(decl);
    checkResult.decls.add(decl); 
  }
  private Pattern heightTerms(List<TypeSymbol> tsymbols, 
    DataTypeConstr constr, List<Exp> exps)
  {
    List<TypedVar> tvars = new ArrayList<TypedVar>(constr.tvars.length);
    for (TypedVar tvar : constr.tvars)
    {
      FunctionSymbol fsymbol = (FunctionSymbol)tvar.id.getSymbol();
      Id id = Id.create(tvar.id.getStrings());
      VariableSymbol vsymbol = new VariableSymbol(id, fsymbol.tsymbol);
      id.setSymbol(vsymbol);
      tvars.add(TypedVar.create(id, tvar.type));
    }
    Pattern pattern = ConstrPattern.create(constr.id, tvars);
    for (TypedVar tvar : tvars)
    {
      Id id = tvar.id;
      VariableSymbol vsymbol = (VariableSymbol)id.getSymbol();
      TypeSymbol tsymbol = vsymbol.tsymbol;
      heightTerms(tsymbols, Var.create(id), tsymbol, exps);
    }
    return pattern;
  }
  */
  /****************************************************************************
   * Create applications of "height" function for the current expression.
   * @param tsymbols the data types in the current data type declaration.
   * @param exp the current expression.
   * @param tsymbol its type.
   * @param exps the list to which the applications are to be added.
   ***************************************************************************/
  /*
  private void heightTerms(List<TypeSymbol> tsymbols, 
    Exp exp, TypeSymbol tsymbol, List<Exp> exps)
  {
    Type type = tsymbol.root.type;
    int n = type.types.length;
    if (n == 0)
    {
      for (TypeSymbol tsymbol0 : tsymbols)
      {
        if (tsymbol == tsymbol0)
        {
          FunctionSymbol heightFun = checkResult.heightMap.get(tsymbol);
          List<Exp> args = new ArrayList<Exp>(1);
          args.add(exp);
          Exp exp0 = Apply.create(heightFun.id, args);
          exps.add(exp0);
          return;
        }
      }
      return;
    }
    Id tid = type.id;
    switch (tid.toString())
    {
    case AST.MAPNAME:
    {
      TypeSymbol keyType = (TypeSymbol)type.types[0].id.getSymbol();
      TypeSymbol valueType = (TypeSymbol)type.types[1].id.getSymbol();
      Set<String> free = FreeVars.compute(exp);
      Id keyId = Id.create(Names.unused("key", free));
      Id resultId = Id.create(Names.unused("result", free));
      List<Exp> args = new ArrayList<Exp>(2);
      args.add(exp);
      args.add(Var.create(keyId));
      Exp exp0 = Apply.create(Id.create(AST.MAPSELECTNAME), args);
      List<Exp> exps0 = new ArrayList<Exp>();
      heightTerms(tsymbols, exp0, valueType, exps0);
      if (exps0.size() == 0) return;
      Exp max0 = applyMax(exps0);
      List<Exp> args0 = new ArrayList<Exp>(2);
      args0.add(Var.create(resultId));
      args0.add(max0);
      Exp exists = Exists.create(TypedVar.create(keyId, keyType.type),
          Apply.create(Id.create(AST.EQUALNAME), args0));
      Exp forall = Forall.create(TypedVar.create(keyId, keyType.type),
          Apply.create(Id.create(AST.GREATEREQUALNAME), args0));
      Exp result =
          Choose.create(TypedVar.create(resultId, checkResult.natSymbol.type),
              And.create(exists, forall));
      exps.add(result);
      break;
    }
    case AST.TUPLENAME:
    {
      for (int i = 0; i < n; i++)
      {
        List<Exp> args = new ArrayList<Exp>(1);
        args.add(exp);
        Id id = Id.create(new String[] { AST.TUPLESELECTNAME, Integer.toString(i+1) });
        Exp exp0 = Apply.create(id, args);
        TypeSymbol tsymbol0 = (TypeSymbol)type.types[i].id.getSymbol();
        heightTerms(tsymbols, exp0, tsymbol0, exps);
      }
      break;
    }
    default:
        TypeError.raise(exp, "unknown compound type " + type);
    }
  }
  private Exp applyMax(List<Exp> exps)
  {
    int n = exps.size();
    if (n == 1) return exps.get(0);
    Set<String> free = new HashSet<String>();
    for (Exp exp : exps) free.addAll(FreeVars.compute(exp));
    List<String> vars = Names.unused(n, "max", free);
    Exp exp = Var.create(Id.create(vars.get(n-1)));
    for (int i = n-1; i >= 1; i--)
    {
      Exp exp0 = Var.create(Id.create(vars.get(i-1)));
      Exp exp1 = exps.get(i);
      List<Exp> args = new ArrayList<Exp>(2);
      args.add(exp1);
      args.add(exp0);
      Exp exp2 = If.create(
          Apply.create(Id.create(AST.GREATERNAME), args),
          exp1, exp0);
      List<LetBinder> binders = new ArrayList<LetBinder>(1);
      binders.add(LetBinder.create(Id.create(vars.get(i)), exp2));
      exp = Let.create(binders, exp);
    }
    List<LetBinder> binders = new ArrayList<LetBinder>(1);
    binders.add(LetBinder.create(Id.create(vars.get(0)), exps.get(0)));
    return Let.create(binders, exp);
  }
  */
  private void check(Function decl)
  {
    Id id = decl.id;
    List<VariableSymbol> vsymbols = check(decl.tvars);
    TypeSymbol tsymbol = check(decl.type);
    List<TypeSymbol> tsymbols = new ArrayList<TypeSymbol>(vsymbols.size());
    for (VariableSymbol vsymbol : vsymbols) tsymbols.add(vsymbol.tsymbol);
    // function symbol may be already set from datatype declaration
    FunctionSymbol fsymbol = (FunctionSymbol)id.getSymbol();
    if (fsymbol == null)
    {
      fsymbol = getFunction(id, tsymbols);
      if (fsymbol != null)
      {
        if (tsymbols.size() == 0)
          TypeError.raise(decl, "constant " + id.toString() + " was already declared");
        else
          TypeError.raise(decl, "function " + toString(id, tsymbols) + " was already declared");
      }
      fsymbol = new FunctionSymbol(id, tsymbols, tsymbol);
      id.setSymbol(fsymbol);
    }
    env.putFunction(fsymbol);
    if (decl.exp != null)
    {
      env.enterVariableScope();
      env.putVariables(vsymbols);
      TypeSymbol[] t = new TypeSymbol[1];
      inVariableScope(
          ()->forallContext(decl.tvars,
              ()-> t[0] = check(decl.exp)));
      TypeSymbol tsymbol0 = t[0];
      if (tsymbol0.root != tsymbol.root)
        TypeError.raise(decl.exp, "expression '" + decl.exp + 
            "' must have type " + tsymbol.root.type + 
            " but has type " + tsymbol0.root.type);
      FunctionSymbol pred = tsymbol.getPred();
      if (pred != null && !tsymbol0.isSubType(tsymbol))
      {
        List<Exp> exps = new ArrayList<Exp>();
        exps.add(decl.exp);
        Exp formula = Apply.create(pred.id, exps);
        int n = decl.tvars.length;
        for (int i = n-1; i >= 0; i--)
          formula = Forall.create(decl.tvars[i], formula);
        addTypeCheckingTheorem(formula);
      }
    }
  }
  private void addTypeCheckingTheorem(Exp formula)
  {
    if (!generateTypeTheorems) return;
    Id id0 = Symbol.getSymbol(currentDecl).id;
    Id id = Id.create("typecheck(" + id0.toString() + ")" + 
    AST.internal(Integer.toString(result.typeTheorems.size())));
    // clone to avoid sharing with original definition
    formula = (Exp)formula.accept(new ASTCloner(true));
    Theorem theorem = Theorem.create(id, formula);
    generateTypeTheorems = false;
    checkDecl(theorem);
    generateTypeTheorems = true;
    FormulaSymbol symbol = (FormulaSymbol)id.getSymbol();
    result.typeTheorems.add(symbol);
    List<FormulaSymbol> typeTheorems0 = result.typeTheoremMap.get(Symbol.getSymbol(currentDecl));
    if (typeTheorems0 == null)
    {
      typeTheorems0 = new ArrayList<FormulaSymbol>();
      result.typeTheoremMap.put(Symbol.getSymbol(currentDecl), typeTheorems0);
    }
    typeTheorems0.add(symbol);
  }
  private void check(Axiom decl)
  {
    Id id = decl.id;
    FormulaSymbol fsymbol = env.getAxiom(id);
    if (fsymbol != null)
      TypeError.raise(decl, "axiom " + id + " was already declared");
    fsymbol = new FormulaSymbol(id, true);
    id.setSymbol(fsymbol);
    env.putAxiom(fsymbol);
    Id fid = decl.getFunId();
    Type[] ftypes = decl.getFunTypes();
    if (fid != null && ftypes != null)
    {
      List<TypeSymbol> tsymbols = new ArrayList<TypeSymbol>();
      for (Type ftype : ftypes)
        tsymbols.add(check(ftype));
      FunctionSymbol fun = env.getFunction(fid, tsymbols);
      if (fun == null)
      {
        if (tsymbols.size() == 0)
          TypeError.raise(decl, "unknown constant '" + fid.toString() + "'");
        else
          TypeError.raise(decl, "unknown function " + toString(fid, tsymbols));
      }
      fid.setSymbol(fun);
    }
    checkBool(decl.formula);
  }
  private void check(Theorem decl)
  {
    completeAxiomatization();
    Id id = decl.id;
    FormulaSymbol fsymbol = env.getTheorem(id);
    if (fsymbol != null)
      TypeError.raise(decl, "theorem " + id + " was already declared");
    fsymbol = new FormulaSymbol(id, false);
    id.setSymbol(fsymbol);
    env.putTheorem(fsymbol);
    checkBool(decl.formula);
  }
  
  private List<VariableSymbol> check(TypedVar[] tvars)
  {
    List<VariableSymbol> vsymbols = new ArrayList<VariableSymbol>(tvars.length);
    for (TypedVar tvar : tvars)
    {
      if (has(vsymbols, tvar.id))
        TypeError.raise(tvar, "variable with same name already declared in variable list");
      vsymbols.add(check(tvar));
    }
    return vsymbols;
  }
  private VariableSymbol check(TypedVar tvar)
  {
    Id id = tvar.id;
    TypeSymbol tsymbol = check(tvar.type);
    VariableSymbol vsymbol = new VariableSymbol(id, tsymbol);
    id.setSymbol(vsymbol);
    return vsymbol;
  }
  
  // -------------------------------------------------------------------------
  //
  // Types and Expressions
  //
  // -------------------------------------------------------------------------
  
  /****************************************************************************
   * Check type expression and return its value as a type symbol.
   * @param texp the type expression.
   * @param type the type symbol.
   ***************************************************************************/
  public TypeSymbol check(Type texp)
  {
    Id id = texp.id;
    int n = texp.types.length;
    if (n == 0)
    {
      TypeSymbol tsymbol = env.getType(id);
      if (tsymbol == null)
        TypeError.raise(texp, "unknown type " + id);
      id.setSymbol(tsymbol);
      return tsymbol;
    }
    List<TypeSymbol> tsymbols = new ArrayList<TypeSymbol>(n);
    for (Type type : texp.types) tsymbols.add(check(type));
    return compoundType(id, tsymbols);
  }
  
  /****************************************************************************
   * Generate compound type.
   * @param id the name of the type constructor.
   * @param tsymbols the list of argument types.
   * @return the symbol of the compound type.
   ***************************************************************************/
  private TypeSymbol compoundType(Id id, List<TypeSymbol> tsymbols)
  {
    TypeSymbol tsymbol = typeTable.getTypeSymbol(id, tsymbols);
    id.setSymbol(tsymbol);
    if (compoundTypes.contains(tsymbol)) return tsymbol;
    compoundTypes.add(tsymbol);
    int n = tsymbols.size();
    switch (id.toString())
    {
    case AST.MAPNAME: 
      if (n != 2)
        TypeError.raise(id, "type constructor must be applied to 2 arguments but " 
            + n + " arguments are given");
      mapTypeOperations(tsymbol, id, tsymbols);
      break;
    case AST.TUPLENAME: 
      tupleTypeOperations(tsymbol, id, tsymbols);
      break;
    default:
      TypeError.raise(id, "unknown type constructor");
      break;
    }
    return tsymbol;
  }
  
  /***************************************************************************
   * Generate map type operations.
   * @param mapSymbol the symbol to be used for the type.
   * @param id the name of the type constructor.
   * @param tsymbols the argument type symbols.
   * @return the result type symbol.
   **************************************************************************/
  private void mapTypeOperations(TypeSymbol mapSymbol, Id id, List<TypeSymbol> tsymbols)
  { 
    // declare type
    TypeSymbol keySymbol = tsymbols.get(0);
    TypeSymbol valueSymbol = tsymbols.get(1);
    FunctionSymbol keyPred = keySymbol.getPred();
    FunctionSymbol valuePred = valueSymbol.getPred();
    Exp formula1 = null;
    if (valuePred != null)
    {
      Id param = Id.create("x");
      List<Exp> args = new ArrayList<Exp>(2);
      args.add(Var.create(Id.create(AST.VALUENAME)));
      args.add(Var.create(param));
      formula1 = applyFun(valuePred, Apply.create(Id.create(AST.MAPSELECTNAME), args));
      if (keyPred != null)
        formula1 = Imp.create(applyFun(keyPred, Var.create(param)), formula1);
      formula1 = Forall.create(TypedVar.create(param, keySymbol.root.type), formula1);
    }
    Exp formula2 = null;
    if (keyPred != null)
    {
      Id param = Id.create("x");
      List<Exp> args1 = new ArrayList<Exp>(2);
      args1.add(Var.create(Id.create(AST.VALUENAME)));
      args1.add(Var.create(param));
      Exp arg1 = Apply.create(Id.create(AST.MAPSELECTNAME), args1);
      Exp arg2 = Apply.create(result.undefs.get(valueSymbol.root).id, new ArrayList<Exp>(0));
      List<Exp> args = new ArrayList<Exp>(2);
      args.add(arg1);
      args.add(arg2);
      formula2 = Apply.create(Id.create(AST.EQUALNAME), args);
      formula2 = Imp.create(Not.create(applyFun(keyPred, Var.create(param))), formula2);
      formula2 = Forall.create(TypedVar.create(param, keySymbol.root.type), formula2);
    }
    TypeDecl decl;      
    if (mapSymbol.root == mapSymbol)
    {
      decl = TypeDecl.create(mapSymbol.id, null, null);
    }
    else
    {
      // formula2 is satisfiable, prevent corresponding type checking condition
      if (formula1 == null) mapSymbol.setSat(true);
      List<Type> types = new ArrayList<Type>(2);
      types.add(keySymbol.root.type);
      types.add(valueSymbol.root.type);
      decl = TypeDecl.create(mapSymbol.id, 
          Type.create(Id.create(id), types), Exp.and(formula1, formula2));
    }
    checkDecl(decl);

    // generate operations only for root type
    if (mapSymbol != mapSymbol.root) return;
    keySymbol = keySymbol.root;
    valueSymbol = valueSymbol.root;
    
    // register map type and generate unique name suffix
    int n = result.mapTypes.size();
    result.mapTypes.add(mapSymbol);
    String suffix = n == 0 ? "" : AST.internal(Integer.toString(n));
    
    // generate map selection function
    {
      Id id0 = Id.create(AST.MAPSELECTNAME + suffix); 
      List<TypedVar> params = new ArrayList<TypedVar>(2);
      params.add(TypedVar.create(Id.create("m"), mapSymbol.type));
      params.add(TypedVar.create(Id.create("k"), keySymbol.type));
      Function fun = Function.create(id0, params, valueSymbol.type, null);
      checkDecl(fun);
      result.mapSelectors.put(mapSymbol, (FunctionSymbol)fun.id.getSymbol());
      // generate extensionality axiom
      if (mapAxioms)
      {
        Id map1 = Id.create("m1");
        Id map2 = Id.create("m2");
        Id key = Id.create("k");
        List<Exp> args1 = new ArrayList<Exp>(2);
        args1.add(Var.create(map1));
        args1.add(Var.create(key));
        List<Exp> args2 = new ArrayList<Exp>(2);
        args2.add(Var.create(map2));
        args2.add(Var.create(key));
        List<Exp> args = new ArrayList<Exp>(2);
        args.add(Apply.create(id0, args1));
        args.add(Apply.create(id0, args2));
        Exp formula = Apply.create(Id.create(AST.EQUALNAME), args);
        formula = Forall.create(TypedVar.create(key, keySymbol.type), formula);
        args = new ArrayList<Exp>(2);
        args.add(Var.create(map1));
        args.add(Var.create(map2));
        formula = Imp.create(formula, Apply.create(Id.create(AST.EQUALNAME), args));
        formula = Forall.create(TypedVar.create(map2, mapSymbol.type), formula);
        formula = Forall.create(TypedVar.create(map1, mapSymbol.type), formula);
        Id aid = Id.create("ext" + AST.internal(Integer.toString(result.decls.size())));
        Axiom axiom = Axiom.create(aid,formula);
        checkDecl(axiom);
        result.mapAxioms.add((FormulaSymbol)aid.getSymbol());
      }
    }

    // generate map construction function
    {   
      Id id0 = Id.create(AST.MAPCONSTRUCTNAME + suffix);  
      Id mapId = Id.create("m");
      Id keyId = Id.create("k");
      Id valueId = Id.create("v");
      List<TypedVar> params = new ArrayList<TypedVar>(1);
      params.add(TypedVar.create(valueId, valueSymbol.type));
      List<Exp> args = new ArrayList<Exp>(2);
      List<Exp> args0 = new ArrayList<Exp>(2);
      args0.add(Var.create(mapId));
      args0.add(Var.create(keyId));
      args.add(Apply.create(Id.create(AST.MAPSELECTNAME), args0));
      args.add(Var.create(valueId));
      Exp exp = Choose.create(Choose.Kind.DEF, TypedVar.create(mapId, mapSymbol.type),
          Forall.create(TypedVar.create(keyId, keySymbol.type),
              Apply.create(Id.create(AST.EQUALNAME), args)));
      Function fun = Function.create(id0, params, mapSymbol.type, exp);
      checkDecl(fun);
      result.mapConstructors.put(mapSymbol, (FunctionSymbol)fun.id.getSymbol());
    }

    // generate map store function
    {
      Id id0 = Id.create(AST.MAPSTORENAME + suffix);
      Id mapId = Id.create("m");
      Id keyId = Id.create("k");
      Id valueId = Id.create("v");
      List<TypedVar> params = new ArrayList<TypedVar>(3);
      params.add(TypedVar.create(mapId, mapSymbol.type));
      params.add(TypedVar.create(keyId, keySymbol.type));
      params.add(TypedVar.create(valueId, valueSymbol.type));
      Function fun = Function.create(id0, params, mapSymbol.type, null);
      checkDecl(fun);
      result.mapStores.put(mapSymbol, (FunctionSymbol)fun.id.getSymbol());
    }
    /*
    if (mapAxioms)
    {
      Id id0 = Id.create(AST.MAPSTORENAME + suffix);
      Id mapId = Id.create("m");
      Id keyId = Id.create("k");
      Id valueId = Id.create("v");
      List<Exp> args0 = new ArrayList<Exp>(2);
      args0.add(Var.create(mapId));
      args0.add(Var.create(keyId));
      args0.add(Var.create(valueId));
      Exp exp0 = Apply.create(id0, args0);
      List<Exp> args1 = new ArrayList<Exp>(2);
      args1.add(exp0);
      args1.add(Var.create(keyId));
      Exp exp1 = Apply.create(Id.create(AST.MAPSELECTNAME), args1);
      List<Exp> args = new ArrayList<Exp>(2);
      args.add(exp1);
      args.add(Var.create(valueId));
      Exp formula = Apply.create(Id.create(AST.EQUALNAME), args);
      formula = Forall.create(TypedVar.create(valueId, valueSymbol.type), formula);
      formula = Forall.create(TypedVar.create(keyId, keySymbol.type), formula);
      formula = Forall.create(TypedVar.create(mapId, mapSymbol.type), formula);
      Id aid = Id.create("meqstore" + AST.internal(Integer.toString(result.decls.size())));
      Axiom axiom = Axiom.create(aid, formula);
      checkDecl(axiom);
      result.mapAxioms.add((FormulaSymbol)aid.getSymbol());
    }
    */
    if (mapAxioms)
    {
      Id id0 = Id.create(AST.MAPSTORENAME + suffix);
      Id mapId = Id.create("m");
      Id keyId = Id.create("k");
      Id key0Id = Id.create("k0");
      Id valueId = Id.create("v");
      List<Exp> args0 = new ArrayList<Exp>(2);
      args0.add(Var.create(mapId));
      args0.add(Var.create(keyId));
      args0.add(Var.create(valueId));
      List<Exp> args1 = new ArrayList<Exp>(2);
      args1.add(Apply.create(id0, args0));
      args1.add(Var.create(key0Id));
      List<Exp> args3 = new ArrayList<Exp>(2);
      args3.add(Apply.create(Id.create(AST.MAPSELECTNAME), args1));
      args3.add(Var.create(valueId));
      List<Exp> args4 = new ArrayList<Exp>(2);
      args4.add(Var.create(keyId));
      args4.add(Var.create(key0Id));
      Apply apply = Apply.create(Id.create(AST.EQUALNAME), args4);
      apply.setNotGoal(true);
      Exp formula = Imp.create(apply,
          Apply.create(Id.create(AST.EQUALNAME), args3));
      formula = Forall.create(TypedVar.create(valueId, valueSymbol.type), formula);
      formula = Forall.create(TypedVar.create(key0Id, keySymbol.type), formula);
      formula = Forall.create(TypedVar.create(keyId, keySymbol.type), formula);
      formula = Forall.create(TypedVar.create(mapId, mapSymbol.type), formula);
      Id aid = Id.create("meqstore" + AST.internal(Integer.toString(result.decls.size())));
      Axiom axiom = Axiom.create(aid, formula);
      checkDecl(axiom);
      result.mapAxioms.add((FormulaSymbol)aid.getSymbol());
    }
    if (mapAxioms)
    {
      Id id0 = Id.create(AST.MAPSTORENAME + suffix);
      Id mapId = Id.create("m");
      Id keyId = Id.create("k");
      Id key0Id = Id.create("k0");
      Id valueId = Id.create("v");
      List<Exp> args0 = new ArrayList<Exp>(2);
      args0.add(Var.create(mapId));
      args0.add(Var.create(keyId));
      args0.add(Var.create(valueId));
      List<Exp> args1 = new ArrayList<Exp>(2);
      args1.add(Apply.create(id0, args0));
      args1.add(Var.create(key0Id));
      List<Exp> args2 = new ArrayList<Exp>(2);
      args2.add(Var.create(mapId));
      args2.add(Var.create(key0Id));
      List<Exp> args3 = new ArrayList<Exp>(2);
      args3.add(Apply.create(Id.create(AST.MAPSELECTNAME), args1));
      args3.add(Apply.create(Id.create(AST.MAPSELECTNAME), args2));
      List<Exp> args4 = new ArrayList<Exp>(2);
      args4.add(Var.create(keyId));
      args4.add(Var.create(key0Id));
      Apply apply = Apply.create(Id.create(AST.EQUALNAME), args4);
      apply.setNotGoal(true);
      Exp formula = Imp.create(Not.create(apply),
          Apply.create(Id.create(AST.EQUALNAME), args3));
      formula = Forall.create(TypedVar.create(valueId, valueSymbol.type), formula);
      formula = Forall.create(TypedVar.create(key0Id, keySymbol.type), formula);
      formula = Forall.create(TypedVar.create(keyId, keySymbol.type), formula);
      formula = Forall.create(TypedVar.create(mapId, mapSymbol.type), formula);
      Id aid = Id.create("mneqstore" + AST.internal(Integer.toString(result.decls.size())));
      Axiom axiom = Axiom.create(aid, formula);
      checkDecl(axiom);
      result.mapAxioms.add((FormulaSymbol)aid.getSymbol());
    }
  }
  
  /***************************************************************************
   * Generate tuple type operations.
   * @param tupleSymbol the symbol to be used for the new type.
   * @param id the name of the type constructor.
   * @param tsymbols the argument type symbols.
   * @return the result type symbol.
   **************************************************************************/
  private void tupleTypeOperations(TypeSymbol tupleSymbol,  
    Id id, List<TypeSymbol> tsymbols)
  {
    // generate tuple subtype
    if (tupleSymbol.root != tupleSymbol)
    {
      int n = tsymbols.size();
      List<Type> types = new ArrayList<Type>(n);
      Exp formula = null;
      int i = 0;
      for (TypeSymbol tsymbol : tsymbols)
      {
        i++;
        types.add(tsymbol.root.type);
        FunctionSymbol pred = tsymbol.getPred();
        if (pred == null) continue;
        Id id0 = Id.create(new String[] { AST.TUPLESELECTNAME, Integer.toString(i) });
        List<Exp> args = new ArrayList<Exp>(1);
        args.add(Var.create(Id.create(AST.VALUENAME)));
        formula = Exp.and(formula, applyFun(pred, Apply.create(id0, args)));
      }
      TypeDecl decl = TypeDecl.create(tupleSymbol.id, 
          Type.create(Id.create(id), types), formula);
      checkDecl(decl);
      return;
    }
    
    // create datatype with corresponding constructor and selector operations
    List<TypedVar> tvars = new ArrayList<TypedVar>(tsymbols.size());
    int i = 1;
    for (TypeSymbol tsymbol : tsymbols)
    {
      Id id0 = Id.create(new String[] { AST.TUPLESELECTNAME, Integer.toString(i) });
      tvars.add(TypedVar.create(id0, tsymbol.type));
      i++;
    }
    Id cid = Id.create(AST.TUPLECONSTRUCTNAME);
    List<DataTypeConstr> constrs = new ArrayList<DataTypeConstr>();
    constrs.add(DataTypeConstr.create(cid, tvars));
    Exp exp = null;
    i = 0;
    for (TypeSymbol tsymbol : tsymbols)
    {
      i++;
      FunctionSymbol pred = tsymbol.getPred();
      if (pred == null) continue;
      Id id0 = Id.create(new String[] { AST.TUPLESELECTNAME, Integer.toString(i) });
      List<Exp> exps = new ArrayList<Exp>();
      exps.add(Var.create(Id.create(AST.VALUENAME)));
      exp = Exp.and(exp, applyFun(pred, Apply.create(id0, exps)));
    }
    DataTypeItem item = DataTypeItem.create(tupleSymbol.id, constrs, null);
    List<DataTypeItem> items = new ArrayList<DataTypeItem>();
    items.add(item);
    DataType decl = DataType.create(items);
    checkDecl(decl);
    result.generatedDataTypes.add(tupleSymbol);

    // create store operations
    i = 1;
    int n = tsymbols.size();
    for (TypeSymbol tsymbol : tsymbols)
    {
      Id id0 = Id.create(new String[] { AST.TUPLESTORENAME, Integer.toString(i) });
      Id tid = Id.create("t");
      Id xid = Id.create("x");
      List<TypedVar> tvars0 = new ArrayList<TypedVar>(2);
      tvars0.add(TypedVar.create(tid, tupleSymbol.type));
      tvars0.add(TypedVar.create(xid, tsymbol.type));
      List<Exp> exps0 = new ArrayList<Exp>();
      for (int j = 1; j <= n; j++)
      {
        if (j == i) { exps0.add(Var.create(xid)); continue; }
        List<Exp> exps1 = new ArrayList<Exp>(1);
        exps1.add(Var.create(tid));
        Id id1 = Id.create(new String[] { AST.TUPLESELECTNAME, Integer.toString(j) });
        exps0.add(Apply.create(id1, exps1));
      }
      Exp exp0 = Apply.create(cid, exps0);
      Function fun = Function.create(id0, tvars0, tupleSymbol.type, exp0);
      checkDecl(fun);
      i++;
      // simplify proofs by explicit tuple store axiom
      if (dataAxioms)
      {
        Id tid0 = Id.create("t");
        Id xid0 = Id.create("x");
        List<Exp> args0 = new ArrayList<Exp>(2);
        args0.add(Var.create(tid0));
        args0.add(Var.create(xid0));
        List<Exp> args1 = new ArrayList<Exp>(2);
        args1.add(Apply.create(id0, args0));
        List<Exp> args = new ArrayList<Exp>(2);
        Id selid = Id.create(new String[] { AST.TUPLESELECTNAME, Integer.toString(i-1) });
        args.add(Apply.create(selid, args1));
        args.add(Var.create(xid0));
        Exp formula = Apply.create(Id.create(AST.EQUALNAME), args);
        formula = Forall.create(TypedVar.create(xid0, tsymbol.type), formula);
        formula = Forall.create(TypedVar.create(tid0, tupleSymbol.type), formula);
        Id aid = Id.create("tstore" + AST.internal(Integer.toString(result.decls.size())));
        Axiom axiom = Axiom.create(aid, formula);
        checkDecl(axiom);
        result.dataTypeAxioms.add((FormulaSymbol)aid.getSymbol());
      }
    }
  }
  
  /****************************************************************************
   * Check whether expression is boolean.
   * @param exp the expression.
   * @return its type (raises a type error, if it is not boolean).
   ***************************************************************************/
  private void checkBool(Exp exp)
  {
    TypeSymbol tsymbol = check(exp);
    if (tsymbol.root != result.boolSymbol.root)
      TypeError.raise(exp, 
          "expression '" + exp + "' must have type " + result.boolSymbol.root.type + 
          " but has type " + tsymbol.root.type);
  }
  
  /****************************************************************************
   * Check expression and return its type symbol.
   * @param exp the expression.
   * @param type its type symbol.
   ***************************************************************************/
  private TypeSymbol check(Exp exp)
  {
    if (exp instanceof Var)
      return check((Var)exp);
    if (exp instanceof Apply)
      return check((Apply)exp);
    if (exp instanceof If)
      return check((If)exp);
    if (exp instanceof Let)
      return check((Let)exp);
    if (exp instanceof Match)
      return check((Match)exp);
    if (exp instanceof Choose)
      return check((Choose)exp);
    if (exp instanceof False)
      return check((False)exp);
    if (exp instanceof True)
      return check((True)exp);
    if (exp instanceof Not)
      return check((Not)exp);
    if (exp instanceof And)
      return check((And)exp);   
    if (exp instanceof Or)
      return check((Or)exp);
    if (exp instanceof Imp)
      return check((Imp)exp);
    if (exp instanceof Equiv)
      return check((Equiv)exp);
    if (exp instanceof Forall)
      return check((Forall)exp); 
    if (exp instanceof Exists)
      return check((Exists)exp);
    TypeError.raise(exp, "unknown kind of expression");
    return null;
  }
  private TypeSymbol check(Var exp)
  {
    // variables are actually generated after type-checking
    Id id = exp.id;
    VariableSymbol vsymbol = env.getVariable(id);
    if (vsymbol == null) 
      TypeError.raise(exp, "unknown variable " + exp);
    id.setSymbol(vsymbol);
    return vsymbol.tsymbol;
  }
  private TypeSymbol check(Apply exp)
  {
    Id id = exp.id;
    int n = exp.exps.length;
    if (n == 0)
    {
      String name = id.toString();
      if (AST.isNat(name)) 
      {  
        for (FunctionSymbol fsymbol : result.intFuns)
        {
          if (name.equals(fsymbol.id.toString())) 
          {
            id.setSymbol(fsymbol);
            return fsymbol.tsymbol;
          }
        }
        // use type "Int" rather than "Nat" to avoid type-checking axiom 
        // (which is difficult to prove without SMT)
        Function fun =
            Function.create(id, new ArrayList<TypedVar>(), result.intSymbol.type, 
                natExp(Integer.parseInt(name)));
        checkDecl(fun);
        FunctionSymbol fsymbol = (FunctionSymbol)id.getSymbol();
        result.intFuns.add(fsymbol);
        return result.natSymbol;
      }
      VariableSymbol vsymbol = env.getVariable(id);
      if (vsymbol != null)
      {
       id.setSymbol(vsymbol);
       return vsymbol.tsymbol;
      }
    }
    List<TypeSymbol> tsymbols = new ArrayList<TypeSymbol>(n);
    for (Exp exp0 : exp.exps)
      tsymbols.add(check(exp0));
    FunctionSymbol fsymbol = getFunction(id, tsymbols);
    if (fsymbol == null)
    {
      if (n == 0)
        TypeError.raise(exp, "unknown value '" + id.toString() + "'");
      else
        TypeError.raise(exp, "unknown function " + toString(id, tsymbols) 
        + " called as '" + exp + "'");
    }
    id.setSymbol(fsymbol);
    // generate type-checking condition
    List<TypeSymbol> mapSymbols = mapSymbols(id, tsymbols);
    List<TypeSymbol> paramSymbols = mapSymbols != null ? mapSymbols : fsymbol.tsymbols;
    Iterator<TypeSymbol> paramTypeIterator = paramSymbols.iterator();
    Iterator<TypeSymbol> argTypeIterator = tsymbols.iterator();
    Iterator<Exp> argExpIterator = Arrays.asList(exp.exps).iterator();
    Exp formula = null;
    while (paramTypeIterator.hasNext())
    {
      TypeSymbol paramSymbol = paramTypeIterator.next();
      TypeSymbol argSymbol = argTypeIterator.next();
      Exp argExp = argExpIterator.next();
      if (argSymbol.isSubType(paramSymbol)) continue;
      FunctionSymbol paramPred = paramSymbol.getPred();
      if (paramPred == null) continue;
      formula = Exp.and(formula, applyFun(paramPred, argExp));
    }
    if (formula != null) addTypeCheckingTheorem(typeTheoremFun.apply(formula));
    return fsymbol.tsymbol;
  }
  private List<TypeSymbol> mapSymbols(Id id, List<TypeSymbol> tsymbols)
  {
    if (tsymbols.size() == 0) return null;
    TypeSymbol mapSymbol = tsymbols.get(0);
    if (!mapSymbol.root.type.id.toString().equals(AST.MAPNAME)) return null;
    while (!mapSymbol.type.id.toString().equals(AST.MAPNAME))
      mapSymbol = mapSymbol.base;
    TypeSymbol keySymbol = (TypeSymbol)mapSymbol.type.types[0].id.getSymbol();
    TypeSymbol valueSymbol = (TypeSymbol)mapSymbol.type.types[1].id.getSymbol();
    String[] strings = id.getStrings();
    switch (strings[0])
    {
    case AST.MAPSELECTNAME:
      return Arrays.asList(new TypeSymbol[] { mapSymbol, keySymbol });
    case AST.MAPSTORENAME:
      return Arrays.asList(new TypeSymbol[] { mapSymbol, keySymbol, valueSymbol });
    case AST.MAPCONSTRUCTNAME:
      return Arrays.asList(new TypeSymbol[] { valueSymbol });
    }
    return null;
  }
  private TypeSymbol check(If exp)
  {
    checkBool(exp.exp1);
    TypeSymbol tsymbol2 = check(exp.exp2);
    TypeSymbol tsymbol3 = check(exp.exp3);
    if (tsymbol2.root != tsymbol3.root)
      TypeError.raise(exp, "conditional branches have different types " +
          tsymbol2.root.type + " and " + tsymbol3.root.type);
    if (tsymbol2 == tsymbol3) return tsymbol2;
    // branch types are not identical: return identical core type
    return tsymbol2.root;
  }
  private TypeSymbol check(Let exp)
  {
    List<VariableSymbol> vsymbols = new ArrayList<VariableSymbol>(exp.binders.length);
    for (LetBinder binder : exp.binders) 
    {  
      if (has(vsymbols, binder.id))
        TypeError.raise(binder, "variable with same name already declared in variable list");
      vsymbols.add(check(binder));
    }
    env.enterVariableScope();
    env.putVariables(vsymbols);
    TypeSymbol[] t = new TypeSymbol[1];
    inVariableScope(
        ()-> letContext(exp.binders,
            ()-> t[0] = check(exp.exp)));
    TypeSymbol tsymbol = t[0];
    return tsymbol;
  }
  private VariableSymbol check(LetBinder binder)
  {
    Id id = binder.id;
    TypeSymbol tsymbol = check(binder.exp);
    VariableSymbol vsymbol = new VariableSymbol(id, tsymbol);
    id.setSymbol(vsymbol);
    return vsymbol;
  }
  private TypeSymbol check(Match exp)
  {
    TypeSymbol tsymbol = check(exp.exp);
    DataTypeItem item = result.dataMap.get(tsymbol);
    if (item == null)
      TypeError.raise(exp,
          "no datatype declaration for expression type " + tsymbol.type);
    Set<FunctionSymbol> csymbols = new HashSet<FunctionSymbol>(item.constrs.length);
    for (DataTypeConstr constr : item.constrs)
    {
      FunctionSymbol csymbol = (FunctionSymbol)constr.id.getSymbol();
      csymbols.add(csymbol);
    }
    Set<FunctionSymbol> csymbols0 = new HashSet<FunctionSymbol>(item.constrs.length);
    List<TypeSymbol> tsymbols = new ArrayList<TypeSymbol>(exp.binders.length);
    for (MatchBinder binder : exp.binders)
      tsymbols.add(check(tsymbol, csymbols, binder, csymbols0));
    if (csymbols0.size() < csymbols.size())
    {
      csymbols.removeAll(csymbols0);
      String message = "";
      Iterator<FunctionSymbol> iter = csymbols.iterator();
      while (true)
      {
        FunctionSymbol csymbol = iter.next();
        message = message + csymbol.id;
        if (!iter.hasNext()) break;
        message = message + ", ";
      }
      TypeError.raise(exp, "no patterns for some constructors (" + message + ")");
    }
    TypeSymbol tsymbol1 = null;
    int i = 0;
    for (TypeSymbol tsymbol2 : tsymbols)
    {
      if (tsymbol1 == null) 
        tsymbol1 = tsymbol2;
      else if (tsymbol1 != tsymbol2)
      {
        if (tsymbol1.root != tsymbol2.root)
          TypeError.raise(exp.binders[i], 
              "match branch has type " +
                  tsymbol2.root.type + " different from type " + 
                  tsymbol1.root.type + " of previous branches");
        // fail back to core type
        tsymbol1 = tsymbol1.root;
      }
      i++;
    }
    return tsymbol1;
  }
  private TypeSymbol check(TypeSymbol tsymbol, Set<FunctionSymbol> csymbols, 
    MatchBinder binder, Set<FunctionSymbol> csymbols0)
  {
    if (binder.pattern instanceof Pattern.DefaultPattern)
    {
      csymbols0.addAll(csymbols);
      return check(binder.exp);
    }
    if (binder.pattern instanceof Pattern.ConstrPattern)
    {
      Pattern.ConstrPattern pattern0 = (Pattern.ConstrPattern)binder.pattern;
      Id id = pattern0.id;
      FunctionSymbol csymbol = null;
      for (FunctionSymbol csymbol0 : csymbols)
      {
        if (id.equals(csymbol0.id)) { csymbol = csymbol0; break; }
      }
      if (csymbol == null)
        TypeError.raise(pattern0, "unknown constructor for datatype " + tsymbol.id);
      csymbols0.add(csymbol);
      id.setSymbol(csymbol);
      int n = csymbol.tsymbols.size();
      if (n != pattern0.tvars.length)
        TypeError.raise(binder.pattern, "pattern must have " + n + " parameters"); 
      List<VariableSymbol> vsymbols = check(pattern0.tvars);
      for (int i = 0; i < n; i++)
      {
        TypeSymbol tsymbol1 = csymbol.tsymbols.get(i);
        TypeSymbol tsymbol2 = vsymbols.get(i).tsymbol;
        if (tsymbol1 != tsymbol2)
          TypeError.raise(pattern0, "parameter " + vsymbols.get(i).id + 
              " of pattern must have type " + tsymbol1.root.type + 
              " but has type " + tsymbol2.root.type);
      }
      env.enterVariableScope();
      env.putVariables(vsymbols);
      TypeSymbol[] t = new TypeSymbol[1];
      inVariableScope(
          ()-> forallContext(pattern0.tvars,
            ()-> t[0] = check(binder.exp)));
      TypeSymbol tsymbol0 = t[0];
      return tsymbol0;
    }
    TypeError.raise(binder, "unknown kind of match-binder");
    return null;
  }
  private TypeSymbol check(Choose exp)
  {
    VariableSymbol vsymbol = check(exp.tvar);
    env.enterVariableScope();
    env.putVariable(vsymbol);
    inVariableScope(
        ()->forallContext(new TypedVar[] { exp.tvar },
            ()->checkBool(exp.exp)));
    // for a "choose" expression, create a type theorem for the existence of the chosen value
    // (for a "choose[exp]" expression, in "Choosing" a more general theorem 
    // is generated that does not depend on the context)
    if (exp.kind == Choose.Kind.ANY) addTypeCheckingTheorem(typeTheoremFun.apply(Exists.create(exp.tvar, exp.exp)));
    return vsymbol.tsymbol;
  }  
  private TypeSymbol check(False exp)
  {
    return result.boolSymbol;
  }
  private TypeSymbol check(True exp)
  {
    return result.boolSymbol;
  }
  private TypeSymbol check(Not exp)
  {
    checkBool(exp.exp);
    return result.boolSymbol;
  }
  private TypeSymbol check(And exp)
  {
    checkBool(exp.exp1);
    impliesContext(exp.exp1, ()->checkBool(exp.exp2));
    return result.boolSymbol;
  }
  private TypeSymbol check(Or exp)
  {
    checkBool(exp.exp1);
    impliesContext(Not.create(exp.exp1), ()->checkBool(exp.exp2));
    return result.boolSymbol;
  }
  private TypeSymbol check(Imp exp)
  {
    checkBool(exp.exp1);
    impliesContext(exp.exp1, ()->checkBool(exp.exp2));
    return result.boolSymbol;
  }
  private TypeSymbol check(Equiv exp)
  {
    checkBool(exp.exp1);
    checkBool(exp.exp2);
    return result.boolSymbol;
  }
  private TypeSymbol check(Forall exp)
  {
    VariableSymbol vsymbol = check(exp.tvar);
    env.enterVariableScope();
    env.putVariable(vsymbol);
    inVariableScope(
        ()->forallContext(new TypedVar[] { exp.tvar },
            ()->checkBool(exp.exp)));
    return result.boolSymbol;
  }
  private TypeSymbol check(Exists exp)
  {
    VariableSymbol vsymbol = check(exp.tvar);
    env.enterVariableScope();
    env.putVariable(vsymbol);
    inVariableScope(
        ()->forallContext(new TypedVar[] { exp.tvar },
            ()->checkBool(exp.exp)));
    return result.boolSymbol;
  }
}
// ----------------------------------------------------------------------------
// end of file
// ----------------------------------------------------------------------------