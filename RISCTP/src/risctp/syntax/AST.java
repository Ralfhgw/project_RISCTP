// ---------------------------------------------------------------------------
// AST.java
// Abstract syntax trees.
// $Id: AST.java,v 1.43 2024/12/11 12:52:11 schreine Exp $
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
package risctp.syntax;

import risctp.types.*;

import java.util.*;

public abstract class AST
{
  // every abstract syntax tree can be visited
  abstract public<T> T accept(ASTVisitor<T> v);

  // predefined names
  public static final String BOOLNAME = "Bool";
  public static final String INTNAME = "Int";
  public static final String NATNAME = "Nat";
  public static final String NONZERONAME = "NonZero§";
  public static final String MAPNAME = "Map";
  public static final String TUPLENAME = "Tuple";
  public static final String PLUSNAME = "+";
  public static final String MINUSNAME = "-";
  public static final String MULTNAME = "⋅";
  public static final String DIVNAME = "/";
  public static final String MODNAME = "%";
  public static final String EQUALNAME = "=";
  public static final String NOTEQUALNAME = "≠";
  public static final String LESSNAME = "<";
  public static final String LESSEQUALNAME = "≤";
  public static final String GREATERNAME = ">";
  public static final String GREATEREQUALNAME = "≥";
  public static final String TUPLECONSTRUCTNAME = "⟨⟩";
  public static final String MAPSELECTNAME = "[]";
  public static final String TUPLESELECTNAME = ".";
  public static final String MAPSTORENAME = "[]=";
  public static final String TUPLESTORENAME = ".=";
  public static final String MAPCONSTRUCTNAME = "map"; // [.,.]
  public static final String DATATYPEISNAME = "is";
  public static final String VALUENAME = "value";
  public static final String HEIGHTNAME = "height";
  public static final String UNDEFNAME = "undef";

  // internal version of an identifier (cannot appear as user identifier)
  public static String internal(String string) { return '§' + string; }

  // identifying natural numbers
  private static final java.util.regex.Pattern natPattern = 
      java.util.regex.Pattern.compile("[0-9][0-9]*");
  public static boolean isNat(String string)
  {
    return natPattern.matcher(string).matches();
  }

  // string quoting and unquoting
  private static final java.util.regex.Pattern simplePattern = 
      java.util.regex.Pattern.compile("([_a-zA-Z][_a-zA-Z0-9]*)|([0-9][0-9]*)");
  public static boolean isSimple(String string)
  {
    return simplePattern.matcher(string).matches();
  }
  public static String quote(String string)
  {
    if (isSimple(string)) return string;
    return '\'' + string + '\'';
  }
  public static String unquote(String string)
  {
    if (string.charAt(0) != '\'') return string;
    return string.substring(1, string.length()-1);
  }

  // the string representation of a sequence of abstract syntax trees
  public static String toString(Object[] asts, String sep)
  {
    StringBuilder builder = new StringBuilder();
    int i = 0;
    int n = asts.length;
    for (Object ast : asts)
    {
      builder.append(ast);
      i++; 
      if (i < n) builder.append(sep);
    }
    return builder.toString();
  }

  /****************************************************************************
   * Create id of datatype tester predicate.
   * @param id the id of the corresponding constructor function.
   * @return the id of the tester predicate.
   ***************************************************************************/
  public static Id testerId(Id id)
  {
    List<String> strings = new ArrayList<String>();
    strings.add(AST.DATATYPEISNAME);
    strings.addAll(Arrays.asList(id.getStrings()));
    return Id.create(strings);
  }

  // --------------------------------------------------------------------------
  //
  // Proving Problems.
  //
  // --------------------------------------------------------------------------
  public static final class Problem extends AST
  {
    public final Decl[] decls;
    private Problem(Decl[] decls) { this.decls = decls; }
    public static Problem create(List<Decl> decls)
    { return new Problem(decls.toArray(new Decl[decls.size()])); }
    public String toString() { return toString(decls, "\n"); }
    public<T> T accept(ASTVisitor<T> v) { return v.visit(this); }
  }

  // --------------------------------------------------------------------------
  //
  // Declarations
  //
  // --------------------------------------------------------------------------
  public static abstract class Decl extends AST
  {
    private Decl() { }
    public abstract<T> T accept(ASTVisitor<T> v);
    public final static class TypeDecl extends Decl
    {
      public final Id id; 
      public final Type type; // may be null
      public final Exp exp;   // may be null
      private TypeDecl(Id id, Type type, Exp exp)
      { this.id = id; this.type = type; this.exp = exp; }
      public static TypeDecl create(Id id, Type type, Exp exp)
      { return new TypeDecl(id, type, exp); }
      public String toString()
      {
        StringBuilder builder = new StringBuilder();
        builder.append("type " + id);
        if (type != null) builder.append(" = " + type);
        if (exp != null) builder.append(" with " + exp);
        builder.append(";");
        return builder.toString();
      }
      public<T> T accept(ASTVisitor<T> v) { return v.visit(this); }
    }
    public final static class DataType extends Decl
    {
      public final DataTypeItem[] items;
      private DataType(DataTypeItem[] items) { this.items = items; }
      public static DataType create(List<DataTypeItem> items)
      { return new DataType(items.toArray(new DataTypeItem[items.size()])); }
      public String toString() 
      { return "datatype " + toString(items, " and ") + ";"; }
      public<T> T accept(ASTVisitor<T> v) { return v.visit(this); }
    }
    public final static class Function extends Decl
    {
      public final Id id; public final TypedVar[] tvars; public final Type type; 
      public final Exp exp; // may be null
      private Function(Id id, TypedVar[] tvars, Type type, Exp exp)
      { this.id = id; this.tvars = tvars; this.type = type; this.exp = exp; }
      public static Function create(Id id, List<TypedVar> tvars, Type type, Exp exp)
      { return new Function(id, tvars.toArray(new TypedVar[tvars.size()]), type, exp); }
      public String toString() 
      { 
        String tname = type.toString();
        String keyword; String rtype; String separator;
        if (tname.equals(BOOLNAME))
        {
          keyword = "pred";
          rtype = "";
          separator = "⇔";
        }
        else if (tvars.length == 0)
        {
          keyword = "const";
          rtype = ":" + tname;
          separator = "=";
        }
        else
        {
          keyword = "fun";
          rtype = ":"+ tname;
          separator = "=";
        }
        String param;
        if (tvars.length == 0)
          param = "";
        else
          param = "(" + toString(tvars, ",") + ")";
        String body;
        if (exp == null) 
          body = "";
        else
          body = " " + separator + " " + exp;
        return keyword + " " + id + param + rtype + body + ";" ;
      }
      public<T> T accept(ASTVisitor<T> v) { return v.visit(this); }
    }
    public final static class Axiom extends Decl
    {
      public final Id id; public final Exp formula;
      private Axiom(Id id, Exp formula) { this.id = id; this.formula = formula; }
      public static Axiom create(Id id, Exp formula) { return new Axiom(id, formula); }
      private Id fid = null; private Type[] ftypes = null;
      public void setFunction(Id fid, Type[] ftypes) { this.fid = fid; this.ftypes = ftypes; }
      public Id getFunId() { return fid; }
      public Type[] getFunTypes() { return ftypes; }
      public String toString() { return "axiom" + 
          (fid == null ? "" : "[" + fid + (ftypes == null || ftypes.length == 0 ? "" : 
             "(" + toString(ftypes, ",") + ")") + "]") + " " +
          id + " ⇔ " + formula + ";"; }
      public<T> T accept(ASTVisitor<T> v) { return v.visit(this); }
    }
    public final static class Theorem extends Decl
    {
      public final Id id; public final Exp formula;
      private Theorem(Id id, Exp formula) { this.id = id; this.formula = formula; }
      public static Theorem create(Id id, Exp formula) { return new Theorem(id, formula); }
      public String toString() { return "theorem " + id + " ⇔ " + formula + ";"; }
      public<T> T accept(ASTVisitor<T> v) { return v.visit(this); }
    }
  }

  // --------------------------------------------------------------------------
  //
  // Expressions
  //
  // --------------------------------------------------------------------------
  public static abstract class Exp extends AST
  {
    private Exp() { }
    public abstract<T> T accept(ASTVisitor<T> v);
    private static final String exp(Exp exp) 
    { 
      if (exp instanceof Var)
        return exp.toString();
      else if (exp instanceof Apply)
      {
        Apply exp0 = (Apply)exp;
        int len = exp0.exps.length;
        if (len == 0) return exp.toString();
        String uname = unquote(exp0.id.toString());
        switch (uname)
        {
        case MINUSNAME:    
          if (len <= 2) return "(" + exp + ")"; 
          break;
        case PLUSNAME: case MULTNAME: case DIVNAME: case MODNAME:
        case EQUALNAME: case NOTEQUALNAME: 
        case LESSNAME: case LESSEQUALNAME:
        case GREATERNAME: case GREATEREQUALNAME: 
          if (len == 2) return "(" + exp + ")"; 
          break;
        case MAPSTORENAME: 
          if (len == 3) return "(" + exp + ")"; 
          break;     
        }
        String[] strings = exp0.id.getStrings();
        switch(strings[0])
        {
        case TUPLESTORENAME: 
          return "(" + exp + ")"; 
        }
        return exp.toString(); 
      }
      return "(" + exp + ")"; 
    }
    // combinations of two formulas (of which one or both may be null)
    public static Exp and(Exp exp1, Exp exp2)
    {
      if (exp1 == null) return exp2;
      if (exp2 == null) return exp1;
      return And.create(exp1, exp2);
    }
    public static Exp or(Exp exp1, Exp exp2)
    {
      if (exp1 == null) return exp2;
      if (exp2 == null) return exp1;
      return Or.create(exp1, exp2);
    }
    public static Exp imp(Exp exp1, Exp exp2)
    {
      if (exp1 == null) return exp2;
      if (exp2 == null) return Not.create(exp1);
      return Imp.create(exp1, exp2);
    }
    public static final class Var extends Exp
    {
      public final Id id;
      private Var(Id id) { this.id = id; }
      public static Var create(Id id) { return new Var(id); }
      public String toString() { return id.toString(); }
      public<T> T accept(ASTVisitor<T> v) { return v.visit(this); }
    }
    public static final class Apply extends Exp
    {
      public final Id id; public final Exp[] exps;
      private Apply(Id id, Exp[] exps) 
      { this.id = id; this.exps = exps; }
      public static Apply create(Id id, List<Exp> exps) 
      { return new Apply(id, exps.toArray(new Exp[exps.size()])); }
      private boolean notGoal = false; // prevent atom to become a goal
      public void setNotGoal(boolean value) { notGoal = value; }
      public boolean isNotGoal() { return notGoal; }
      public String toString() 
      { 
        if (exps.length == 0) return id.toString();
        if (!notGoal)
        {
          String string = unquote(id.toString());
          switch (string)
          {
          case MINUSNAME:        
            if (exps.length == 1) return "-" + exp(exps[0]); 
            if (exps.length == 2) return exp(exps[0]) + "-" + exp(exps[1]);
            break;
          case PLUSNAME:         
            if (exps.length == 2) return exp(exps[0]) + "+" + exp(exps[1]);
            break;
          case MULTNAME:         
            if (exps.length == 2) return exp(exps[0]) + "⋅" + exp(exps[1]);
            break;
          case DIVNAME:          
            if (exps.length == 2) return exp(exps[0]) + "/" + exp(exps[1]);
            break;
          case MODNAME:          
            if (exps.length == 2) return exp(exps[0]) + "%" + exp(exps[1]);
            break;
          case EQUALNAME:        
            if (exps.length == 2) return exp(exps[0]) + " = " + exp(exps[1]);
            break;
          case NOTEQUALNAME:     
            if (exps.length == 2) return exp(exps[0]) + " ≠ " + exp(exps[1]);
            break;
          case LESSNAME:         
            if (exps.length == 2) return exp(exps[0]) + " < " + exp(exps[1]);
            break;
          case LESSEQUALNAME:    
            if (exps.length == 2) return exp(exps[0]) + " ≤ " + exp(exps[1]);
            break;
          case GREATERNAME:      
            if (exps.length == 2) return exp(exps[0]) + " > " + exp(exps[1]);
            break;
          case GREATEREQUALNAME: 
            if (exps.length == 2) return exp(exps[0]) + " ≥ " + exp(exps[1]);
            break;
          case TUPLECONSTRUCTNAME: 
            return "⟨" + toString(exps, ",") + "⟩";
          case MAPSELECTNAME:      
            if (exps.length == 2) return exp(exps[0]) + "[" + exps[1] + "]";
            break;
          case MAPSTORENAME:      
            if (exps.length == 3) return exp(exps[0]) + " with [" + exps[1] + "] = " + exp(exps[2]);
            break;
          }
        }
        String[] strings = id.getStrings();
        switch(strings[0])
        {
        case MAPCONSTRUCTNAME: 
          if (strings.length == 3)
            return "map[" + strings[1] + "," + strings[2] + "](" + exps[0] + ")";
          break;
        case TUPLESELECTNAME:
          if (strings.length == 2)
            return exp(exps[0]) + "." + strings[1];
          break;
        case TUPLESTORENAME:      
          if (strings.length == 2)
            return exp(exps[0]) + " with ." + strings[1] + " = " + exp(exps[1]);
          break;
        }
        return id + "(" + toString(exps, ",") + ")" + (notGoal ? "{*}" : ""); 
      }
      public<T> T accept(ASTVisitor<T> v) { return v.visit(this); }
    }
    public static final class If extends Exp
    {
      public final Exp exp1; public final Exp exp2; public final Exp exp3;
      private If(Exp exp1, Exp exp2, Exp exp3) 
      { this.exp1 = exp1; this.exp2 = exp2; this.exp3 = exp3; }
      public static If create(Exp exp1, Exp exp2, Exp exp3)
      { return new If(exp1, exp2, exp3); }
      public String toString() 
      { return "if " + exp1 + " then " + exp2 + " else " + exp(exp3); }
      public<T> T accept(ASTVisitor<T> v) { return v.visit(this); }
    }
    public static final class Let extends Exp
    {
      public final LetBinder[] binders; public final Exp exp;
      private Let(LetBinder[] binder, Exp exp) { this.binders = binder; this.exp = exp; }
      public static Let create(List<LetBinder> binder, Exp exp) 
      { 
        return new Let(binder.toArray(new LetBinder[binder.size()]), exp);
      }
      public String toString() { return "let " + toString(binders, ", ") + " in " + exp(exp); }
      public<T> T accept(ASTVisitor<T> v) { return v.visit(this); }
    }
    public static final class Match extends Exp
    {
      public final Exp exp; public final MatchBinder[] binders;
      private Match(Exp exp, MatchBinder[] binders) 
      { this.exp = exp; this.binders = binders; }
      public static Match create(Exp exp, List<MatchBinder> binders)
      { 
        return new Match(exp, binders.toArray(new MatchBinder[binders.size()])); 
      }
      public String toString() 
      { return "match " + exp + " with " + toString(binders, " | "); }
      public<T> T accept(ASTVisitor<T> v) { return v.visit(this); }
    }
    public static final class Choose extends Exp
    {
      // SAT: condition is satisfied by at least one value
      // UNI: condition is satisfied by at most one value (i.e., makes it unique)
      // DEF: condition is satisfied by exactly one value (i.e., defines it)
      public enum Kind { ANY, SAT, UNI, DEF }
      public final Kind kind; public final TypedVar tvar; public final Exp exp;
      private Choose(Kind kind, TypedVar tvar, Exp exp) 
      { this.kind = kind; this.tvar = tvar; this.exp = exp; }
      public static Choose create(Kind kind, TypedVar tvar, Exp exp) 
      { return new Choose(kind, tvar, exp); }
      public String toString() 
      { return "choose" + 
          (kind == Kind.SAT ? "[sat]" : kind == Kind.UNI ? "[uni]" : 
            kind == Kind.DEF ? "[def]" : "")
          + " " + tvar + " with " + exp(exp); }
      public<T> T accept(ASTVisitor<T> v) { return v.visit(this); }
    }
    public static final class False extends Exp
    {
      private False() { }
      public static False create() { return new False(); }
      public String toString() { return "⊥"; }
      public<T> T accept(ASTVisitor<T> v) { return v.visit(this); }
    }
    public static final class True extends Exp
    {
      private True() { }
      public static True create() { return new True(); }
      public String toString() { return "⊤"; }
      public<T> T accept(ASTVisitor<T> v) { return v.visit(this); }
    }
    public static final class Not extends Exp
    {
      public final Exp exp;
      private Not(Exp exp) { this.exp = exp; }
      public static Not create(Exp exp) { return new Not(exp); }
      public String toString() { return "¬" + exp(exp); }
      public<T> T accept(ASTVisitor<T> v) { return v.visit(this); }
    }
    public static final class And extends Exp
    {
      public final Exp exp1; public final Exp exp2;
      private And(Exp exp1, Exp exp2) 
      { this.exp1 = exp1; this.exp2 = exp2; }
      public static And create(Exp exp1, Exp exp2) 
      { return new And(exp1, exp2); }
      public String toString() { return exp(exp1) + " ∧ " + exp(exp2); }
      public<T> T accept(ASTVisitor<T> v) { return v.visit(this); }
    }
    public static final class Or extends Exp
    {
      public final Exp exp1; public final Exp exp2;
      private Or(Exp exp1, Exp exp2) 
      { this.exp1 = exp1; this.exp2 = exp2; }
      public static Or create(Exp exp1, Exp exp2) 
      { return new Or(exp1, exp2); }
      public String toString() { return exp(exp1) + " ∨ " + exp(exp2); }
      public<T> T accept(ASTVisitor<T> v) { return v.visit(this); }
    }
    public static final class Imp extends Exp
    {
      public final Exp exp1; public final Exp exp2;
      private Imp(Exp exp1, Exp exp2) 
      { this.exp1 = exp1; this.exp2 = exp2; }
      public static Imp create(Exp exp1, Exp exp2) 
      { return new Imp(exp1, exp2); }
      public String toString() { return exp(exp1) + " ⇒ " + exp(exp2); }
      public<T> T accept(ASTVisitor<T> v) { return v.visit(this); }
    }
    public static final class Equiv extends Exp
    {
      public final Exp exp1; public final Exp exp2;
      private Equiv(Exp exp1, Exp exp2) 
      { this.exp1 = exp1; this.exp2 = exp2; }
      public static Equiv create(Exp exp1, Exp exp2) 
      { return new Equiv(exp1, exp2); }
      public String toString() { return exp(exp1) + " ⇔ " + exp(exp2); }
      public<T> T accept(ASTVisitor<T> v) { return v.visit(this); }
    } 
    public static final class Forall extends Exp
    {
      public final TypedVar tvar; public final Exp exp;
      private Forall(TypedVar tvar, Exp exp) { this.tvar = tvar; this.exp = exp; }
      public static Forall create(TypedVar tvar, Exp exp) { return new Forall(tvar, exp); }
      public String toString() 
      { 
        List<TypedVar> tvars = new ArrayList<TypedVar>();
        Exp exp0 = typedVars(tvars);
        StringBuilder builder = new StringBuilder();
        builder.append("∀" + tvars.get(0));
        int n = tvars.size();
        for (int i = 1; i < n; i++) builder.append("," + tvars.get(i));
        builder.append(". " + exp(exp0));
        return builder.toString();
      }
      private Exp typedVars(List<TypedVar> tvars)
      {
        tvars.add(tvar);
        if (!(exp instanceof Forall)) return exp;
        Forall exp0 = (Forall)exp;
        return exp0.typedVars(tvars);
      }
      public<T> T accept(ASTVisitor<T> v) { return v.visit(this); }
    }
    public static final class Exists extends Exp
    {
      public final TypedVar tvar; public final Exp exp;
      private Exists(TypedVar tvar, Exp exp) { this.tvar = tvar; this.exp = exp; }
      public static Exists create(TypedVar tvar, Exp exp) { return new Exists(tvar, exp); }
      public String toString() 
      { 
        List<TypedVar> tvars = new ArrayList<TypedVar>();
        Exp exp0 = typedVars(tvars);
        StringBuilder builder = new StringBuilder();
        builder.append("∃" + tvars.get(0));
        int n = tvars.size();
        for (int i = 1; i < n; i++) builder.append("," + tvars.get(i));
        builder.append(". " + exp(exp0));
        return builder.toString();
      }
      private Exp typedVars(List<TypedVar> tvars)
      {
        tvars.add(tvar);
        if (!(exp instanceof Exists)) return exp;
        Exists exp0 = (Exists)exp;
        return exp0.typedVars(tvars);
      }
      public<T> T accept(ASTVisitor<T> v) { return v.visit(this); }
    }
  }

  // --------------------------------------------------------------------------
  //
  // Identifiers
  //
  // --------------------------------------------------------------------------
  public static final class Id extends AST
  {
    // string is interned, may apply reference equality for comparison
    private String string; private Symbol symbol; 
    private Id(String string) { this.string = string; }
    private Id(String[] strings) { setString(strings); }
    public void setString(String[] strings)
    { 
      this.symbol = null; 
      StringBuilder builder = new StringBuilder();
      boolean composed = false;;
      for (String string : strings)
      {
        string = unquote(string);
        if (composed) builder.append("::");
        builder.append(string);
        composed = true;
      }
      string = quote(builder.toString()).intern();
    }
    public String[] getStrings()
    {
      return unquote(string).split("::");
    }
    public boolean equals(Id id) { return string == id.string; }
    public void setSymbol(Symbol symbol) { this.symbol = symbol; }
    public Symbol getSymbol() { return symbol; }
    public static Id create(String string) 
    { return new Id(string.intern()); }
    public static Id create(Id id)
    { return new Id(id.string); }
    public static Id create(String[] strings) 
    { return new Id(strings); }
    public static Id create(List<String> strings) 
    { return new Id(strings.toArray(new String[strings.size()])); }
    public String toString() { return string; }
    public<T> T accept(ASTVisitor<T> v) { return v.visit(this); }
  }

  // --------------------------------------------------------------------------
  //
  // Types
  //
  // --------------------------------------------------------------------------
  public static final class Type extends AST
  {
    public final Id id; public final Type[] types;
    private Type(Id id, Type[] types) { this.id = id; this.types = types; }
    public static Type create(Id id) { return new Type(id, new Type[0]); }
    public static Type create(Id id, List<Type> types)
    { return new Type(id, types.toArray(new Type[types.size()])); }
    public String toString() 
    { 
      if (types.length == 0) return id.toString();
      return id + "[" + toString(types, ",") + "]"; 
    }
    public<T> T accept(ASTVisitor<T> v) { return v.visit(this); }
  }

  // --------------------------------------------------------------------------
  //
  // Typed Variables
  //
  // --------------------------------------------------------------------------
  public static final class TypedVar extends AST
  {
    public final Id id; public final Type type;
    private TypedVar(Id id, Type type) { this.id = id; this.type = type; }
    public static TypedVar create(Id id, Type type) { return new TypedVar(id, type); }
    public String toString() { return id + ":" + type; }
    public<T> T accept(ASTVisitor<T> v) { return v.visit(this); }
  }

  // --------------------------------------------------------------------------
  //
  // Let Binders
  //
  // --------------------------------------------------------------------------
  public static final class LetBinder extends AST
  {
    public final Id id; public final Exp exp;
    private LetBinder(Id id, Exp exp) { this.id = id; this.exp = exp; }
    public static LetBinder create(Id id, Exp exp) { return new LetBinder(id, exp); }
    public String toString() { return id + " = " + exp; }
    public<T> T accept(ASTVisitor<T> v) { return v.visit(this); }
  }

  // --------------------------------------------------------------------------
  //
  // Match Binders
  //
  // --------------------------------------------------------------------------
  public static final class MatchBinder extends AST
  {
    public final Pattern pattern; public final Exp exp;
    private MatchBinder(Pattern pattern, Exp exp)
    { this.pattern = pattern; this.exp = exp;}
    public static MatchBinder create(Pattern pattern, Exp exp)
    { return new MatchBinder(pattern, exp); }
    public String toString() { return pattern + " -> " + exp; }
    public<T> T accept(ASTVisitor<T> v) { return v.visit(this); }
  }

  // --------------------------------------------------------------------------
  //
  // Patterns
  //
  // --------------------------------------------------------------------------
  public static abstract class Pattern extends AST
  {
    private Pattern() { }
    public abstract<T> T accept(ASTVisitor<T> v);
    public static final class DefaultPattern extends Pattern
    {
      private DefaultPattern() { }
      public static DefaultPattern create() { return new DefaultPattern(); }
      public String toString() { return "_"; }
      public<T> T accept(ASTVisitor<T> v) { return v.visit(this); }
    }
    public static final class ConstrPattern extends Pattern
    {
      public final Id id; public final TypedVar[] tvars;
      private ConstrPattern(Id id, TypedVar[] tvars) 
      { this.id = id; this.tvars = tvars; }
      public static ConstrPattern create(Id id, List<TypedVar> tvars) 
      { return new ConstrPattern(id, tvars.toArray(new TypedVar[tvars.size()])); }
      public String toString() 
      { 
        if (tvars.length == 0) return id.toString(); 
        return id.toString() + "(" + toString(tvars, ",") + ")";
      }
      public<T> T accept(ASTVisitor<T> v) { return v.visit(this); }
    }
  }

  // --------------------------------------------------------------------------
  //
  // Datatype Items
  //
  // --------------------------------------------------------------------------
  public static final class DataTypeItem extends AST
  {
    public final Id id; public final DataTypeConstr[] constrs;
    public final Exp exp; // may be null
    private DataTypeItem(Id id, DataTypeConstr[] constrs, Exp exp) 
    { this.id = id; this.constrs = constrs; this.exp = exp; }
    public static DataTypeItem create(Id id, List<DataTypeConstr> constrs, Exp exp)
    { return new DataTypeItem(id, constrs.toArray(new DataTypeConstr[constrs.size()]), exp); }
    public String toString() 
    { 
      String withClause = exp == null ? "" : " with " + exp;
      return id + " = " + toString(constrs, " | ") + withClause; 
    }
    public<T> T accept(ASTVisitor<T> v) { return v.visit(this); }
  }

  // --------------------------------------------------------------------------
  //
  // Datatype Constructor
  //
  // --------------------------------------------------------------------------
  public static final class DataTypeConstr extends AST
  {
    public final Id id; public final TypedVar[] tvars;
    private DataTypeConstr(Id id, TypedVar[] tvars) 
    { this.id = id; this.tvars = tvars; }
    public static DataTypeConstr create(Id id, List<TypedVar> tvars) 
    { return new DataTypeConstr(id, tvars.toArray(new TypedVar[tvars.size()])); }
    public String toString() 
    { 
      if (tvars.length == 0) return id.toString(); 
      return id.toString() + "(" + toString(tvars, ",") + ")";
    }
    public<T> T accept(ASTVisitor<T> v) { return v.visit(this); }
  }
}
// ----------------------------------------------------------------------------
// end of file
// ----------------------------------------------------------------------------
