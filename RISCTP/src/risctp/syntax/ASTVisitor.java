// ---------------------------------------------------------------------------
// ASTVisitor.java
// Visitors to abstract syntax trees.
// $Id: ASTVisitor.java,v 1.6 2024/06/06 17:23:56 schreine Exp $
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

import risctp.syntax.AST.*;
import risctp.syntax.AST.Decl.*;
import risctp.syntax.AST.Exp.*;
import risctp.syntax.AST.Pattern.*;

// a visitor for abstract syntax trees returning values of type T
public interface ASTVisitor<T>
{
  // proving problems
  public T visit(Problem ast);

  // declarations
  public T visit(TypeDecl ast);  
  public T visit(DataType ast);
  public T visit(Function ast);
  public T visit(Axiom ast);
  public T visit(Theorem ast);

  // expressions
  public T visit(Var ast);
  public T visit(Apply ast);
  public T visit(If ast);
  public T visit(Let ast);
  public T visit(Match ast);
  public T visit(Choose ast);
  public T visit(False ast);
  public T visit(True ast);
  public T visit(Not ast);
  public T visit(And ast);
  public T visit(Or ast);
  public T visit(Imp ast);
  public T visit(Equiv ast);
  public T visit(Forall ast);
  public T visit(Exists ast);

  // miscellaneous
  public T visit(Id ast);
  public T visit(Type ast);
  public T visit(TypedVar ast);
  public T visit(LetBinder ast);
  public T visit(MatchBinder ast);
  public T visit(DefaultPattern ast);
  public T visit(ConstrPattern ast);
  public T visit(DataTypeItem ast);
  public T visit(DataTypeConstr ast);

  // a base class for implementing visitors
  public static class Base<T> implements ASTVisitor<T>
  { 
    public T visit(Problem problem)
    {
      for (Decl decl : problem.decls)
        decl.accept(this);
      return null;
    }
    public T visit(TypeDecl decl)
    {
      decl.id.accept(this);
      if (decl.type != null) decl.type.accept(this);
      if (decl.exp != null) decl.exp.accept(this);
      return null;
    }
    public T visit(DataType decl)
    {
      for (DataTypeItem item : decl.items)
        item.accept(this);
      return null;
    }
    public T visit(Function decl)
    {
      decl.id.accept(this);
      for (TypedVar tvar : decl.tvars)
        tvar.accept(this);
      decl.type.accept(this);
      if (decl.exp != null) decl.exp.accept(this);
      return null;
    }
    public T visit(Axiom decl)
    {
      decl.id.accept(this);
      Id fid = decl.getFunId();
      if (fid != null) fid.accept(this);
      Type[] ftypes = decl.getFunTypes();
      if (ftypes != null)
      {
        for (Type ftype : ftypes) ftype.accept(this);
      }
      decl.formula.accept(this);
      return null;
    }
    public T visit(Theorem decl)
    {
      decl.id.accept(this);
      decl.formula.accept(this);
      return null;
    }
    public T visit(Var exp)
    {
      exp.id.accept(this);
      return null;
    }
    public T visit(Apply exp)
    {
      exp.id.accept(this);
      for (Exp exp0 : exp.exps)
        exp0.accept(this);
      return null;
    }
    public T visit(If exp)
    {
      exp.exp1.accept(this);
      exp.exp2.accept(this);
      exp.exp3.accept(this);
      return null;
    }
    public T visit(Let exp)
    {
      for (LetBinder binder : exp.binders)
        binder.accept(this);
      exp.exp.accept(this);
      return null;
    }
    public T visit(Match exp)
    {
      exp.exp.accept(this);
      for (MatchBinder binder : exp.binders)
        binder.accept(this);
      return null;
    }
    public T visit(Choose exp)
    {
      exp.tvar.accept(this);
      exp.exp.accept(this);
      return null;
    }
    public T visit(False exp)
    {
      return null;
    }
    public T visit(True exp)
    {
      return null;
    }
    public T visit(Not exp)
    {
      exp.exp.accept(this);
      return null;
    }
    public T visit(And exp)
    {
      exp.exp1.accept(this);
      exp.exp2.accept(this);
      return null;
    }
    public T visit(Or exp)
    {
      exp.exp1.accept(this);
      exp.exp2.accept(this);
      return null;
    }
    public T visit(Imp exp)
    {
      exp.exp1.accept(this);
      exp.exp2.accept(this);
      return null;
    }
    public T visit(Equiv exp)
    {
      exp.exp1.accept(this);
      exp.exp2.accept(this);
      return null;
    }
    public T visit(Forall exp)
    {
      exp.tvar.accept(this);
      exp.exp.accept(this);
      return null;
    }
    public T visit(Exists exp)
    {
      exp.tvar.accept(this);
      exp.exp.accept(this);
      return null;
    }
    public T visit(Id id)
    {
      return null;
    }
    public T visit(Type type)
    {
      type.id.accept(this);
      for (Type type0 : type.types)
        type0.accept(this);
      return null;
    }
    public T visit(TypedVar tvar)
    {
      tvar.id.accept(this);
      tvar.type.accept(this);
      return null;
    }
    public T visit(LetBinder binder)
    {
      binder.id.accept(this);
      binder.exp.accept(this);
      return null;
    }
    public T visit(MatchBinder binder)
    {
      binder.pattern.accept(this);
      binder.exp.accept(this);
      return null;
    }
    public T visit(DefaultPattern pattern)
    {
      return null;
    }
    public T visit(ConstrPattern pattern)
    {
      pattern.id.accept(this);
      for (TypedVar tvar : pattern.tvars)
        tvar.accept(this);
      return null;
    }
    public T visit(DataTypeItem item)
    {
      item.id.accept(this);
      for (DataTypeConstr constr : item.constrs)
        constr.accept(this);
      if (item.exp != null) item.exp.accept(this);
      return null;
    }
    public T visit(DataTypeConstr constr)
    {
      constr.id.accept(this);
      for (TypedVar tvar : constr.tvars)
        tvar.accept(this);
      return null;
    }
  }
}
// ----------------------------------------------------------------------------
// end of file
// ----------------------------------------------------------------------------
