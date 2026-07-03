// ---------------------------------------------------------------------------
// ASTCloner.java
// A base class for cloning abstract syntax trees.
// $Id: ASTCloner.java,v 1.9 2024/06/06 17:23:56 schreine Exp $
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

import java.util.ArrayList;
import java.util.List;

import risctp.syntax.AST.*;
import risctp.syntax.AST.Decl.*;
import risctp.syntax.AST.Exp.*;
import risctp.syntax.AST.Pattern.*;

public class ASTCloner extends ASTVisitor.Base<AST>
{
  // indicate whether ids are also to be cloned (or, alternatively, shared)
  private boolean cloneIds;
  public ASTCloner(boolean cloneIds) { this.cloneIds = cloneIds; }
  
  public Problem visit(Problem problem)
  {
    List<Decl> decls0 = new ArrayList<Decl>(problem.decls.length);
    for (Decl decl0 : problem.decls) decls0.add((Decl)decl0.accept(this));
    return Problem.create(decls0);
  }
  public TypeDecl visit(TypeDecl decl)
  {
    Id id0 = (Id)decl.id.accept(this);
    Type type0 = decl.type == null ? null : (Type)decl.type.accept(this);
    Exp exp0 = decl.exp == null ? null : (Exp)decl.exp.accept(this);
    return TypeDecl.create(id0, type0, exp0);
  }
  public DataType visit(DataType decl)
  {
    List<DataTypeItem> items0 = new ArrayList<DataTypeItem>(decl.items.length);
    for (DataTypeItem item0 : decl.items) items0.add((DataTypeItem)item0.accept(this));
    return DataType.create(items0);
  }
  public Function visit(Function decl)
  {
    Id id0 = (Id)decl.id.accept(this);
    List<TypedVar> tvars0 = new ArrayList<TypedVar>(decl.tvars.length);
    for (TypedVar tvar0 : decl.tvars) tvars0.add((TypedVar)tvar0.accept(this));
    Type type0 = (Type)decl.type.accept(this);
    Exp exp0 = decl.exp == null ? null : (Exp)decl.exp.accept(this);
    return Function.create(id0, tvars0, type0, exp0);
  }
  public Axiom visit(Axiom decl)
  {
    Id id0 = (Id)decl.id.accept(this);
    Exp formula0 = (Exp)decl.formula.accept(this);
    Axiom axiom = Axiom.create(id0, formula0);
    Id fid = decl.getFunId();
    if (fid != null)
    {
      Id fid0 = (Id)fid.accept(this);
      Type[] ftypes = decl.getFunTypes();
      Type[] ftypes0 = null;
      if (ftypes != null)
      {
        int n = ftypes.length;
        ftypes0 = new Type[n];
        for (int i = 0; i < n; i++)
          ftypes0[i] = (Type)ftypes[i].accept(this);
      }
      axiom.setFunction(fid0, ftypes0);
    }
    return axiom;
  }
  public Theorem visit(Theorem decl)
  {
    Id id0 = (Id)decl.id.accept(this);
    Exp formula0 = (Exp)decl.formula.accept(this);
    return Theorem.create(id0, formula0);
  }
  public Exp visit(Var exp)
  {
    Id id0 = (Id)exp.id.accept(this);
    return Var.create(id0);
  }
  public Exp visit(Apply exp)
  {
    Id id0 = (Id)exp.id.accept(this);
    List<Exp> exps0 = new ArrayList<Exp>(exp.exps.length);
    for (Exp exp0 : exp.exps) exps0.add((Exp)exp0.accept(this));
    Apply apply = Apply.create(id0, exps0);
    apply.setNotGoal(exp.isNotGoal());
    return apply;
  }
  public Exp visit(If exp)
  {
    Exp exp1 = (Exp)exp.exp1.accept(this);
    Exp exp2 = (Exp)exp.exp2.accept(this);
    Exp exp3 = (Exp)exp.exp3.accept(this);
    return If.create(exp1, exp2, exp3);
  }
  public Exp visit(Let exp)
  {
    List<LetBinder> binders0 = new ArrayList<LetBinder>(exp.binders.length);
    for (LetBinder binder0 : exp.binders) binders0.add((LetBinder)binder0.accept(this));
    Exp exp0 = (Exp)exp.exp.accept(this);
    return Let.create(binders0, exp0);
  }
  public Exp visit(Match exp)
  {
    Exp exp0 = (Exp)exp.exp.accept(this);
    List<MatchBinder> binders0 = new ArrayList<MatchBinder>(exp.binders.length);
    for (MatchBinder binder0 : exp.binders) binders0.add((MatchBinder)binder0.accept(this));
    return Match.create(exp0, binders0);
  }
  public Exp visit(Choose exp)
  {
    TypedVar tvar0 = (TypedVar)exp.tvar.accept(this);
    Exp exp0 = (Exp)exp.exp.accept(this);
    return Choose.create(exp.kind, tvar0, exp0);
  }
  public Exp visit(False exp)
  {
    return False.create();
  }
  public Exp visit(True exp)
  {
    return True.create();
  }
  public Exp visit(Not exp)
  {
    Exp exp0 = (Exp)exp.exp.accept(this);
    return Not.create(exp0);
  }
  public Exp visit(And exp)
  {
    Exp exp1 = (Exp)exp.exp1.accept(this);
    Exp exp2 = (Exp)exp.exp2.accept(this);
    return And.create(exp1, exp2);
  }
  public Exp visit(Or exp)
  {
    Exp exp1 = (Exp)exp.exp1.accept(this);
    Exp exp2 = (Exp)exp.exp2.accept(this);
    return Or.create(exp1, exp2);
  }
  public Exp visit(Imp exp)
  {
    Exp exp1 = (Exp)exp.exp1.accept(this);
    Exp exp2 = (Exp)exp.exp2.accept(this);
    return Imp.create(exp1, exp2);
  }
  public Exp visit(Equiv exp)
  {
    Exp exp1 = (Exp)exp.exp1.accept(this);
    Exp exp2 = (Exp)exp.exp2.accept(this);
    return Equiv.create(exp1, exp2);
  }
  public Exp visit(Forall exp)
  {
    TypedVar tvar0 = (TypedVar)exp.tvar.accept(this);
    Exp exp0 = (Exp)exp.exp.accept(this);
    return Forall.create(tvar0, exp0);
  }
  public Exp visit(Exists exp)
  {
    TypedVar tvar0 = (TypedVar)exp.tvar.accept(this);
    Exp exp0 = (Exp)exp.exp.accept(this);
    return Exists.create(tvar0, exp0);
  }
  public Id visit(Id id)
  {
    if (!cloneIds) return id;
    return Id.create(id.toString());
  }
  public Type visit(Type type)
  {
    Id id0 = (Id)type.id.accept(this);
    List<Type> types0 = new ArrayList<Type>(type.types.length);
    for (Type type0 : type.types) types0.add((Type)type0.accept(this));
    return Type.create(id0, types0);
  }
  public TypedVar visit(TypedVar tvar)
  {
    Id id0 = (Id)tvar.id.accept(this);
    Type type0 = (Type)tvar.type.accept(this);
    return TypedVar.create(id0, type0);
  }
  public LetBinder visit(LetBinder binder)
  {
    Id id0 = (Id)binder.id.accept(this);
    Exp exp0 = (Exp)binder.exp.accept(this);
    return LetBinder.create(id0, exp0);
  }
  public MatchBinder visit(MatchBinder binder)
  {
    Pattern pattern0 = (Pattern)binder.pattern.accept(this);
    Exp exp0 = (Exp)binder.exp.accept(this);
    return MatchBinder.create(pattern0, exp0);
  }
  public DefaultPattern visit(DefaultPattern pattern)
  {
    return DefaultPattern.create();
  }
  public ConstrPattern visit(ConstrPattern pattern)
  {
    Id id0 = (Id)pattern.id.accept(this);
    List<TypedVar> tvars0 = new ArrayList<TypedVar>(pattern.tvars.length);
    for (TypedVar tvar0 : pattern.tvars) tvars0.add((TypedVar)tvar0.accept(this));
    return ConstrPattern.create(id0, tvars0);
  }
  public DataTypeItem visit(DataTypeItem item)
  {
    Id id0 = (Id)item.id.accept(this);
    List<DataTypeConstr> constrs0 = new ArrayList<DataTypeConstr>(item.constrs.length);
    for (DataTypeConstr constr0 : item.constrs) constrs0.add((DataTypeConstr)constr0.accept(this));
    Exp exp0 = item.exp == null ? null : (Exp)item.exp.accept(this);
    return DataTypeItem.create(id0, constrs0, exp0);
  }
  public DataTypeConstr visit(DataTypeConstr constr)
  {
    Id id0 = (Id)constr.id.accept(this);
    List<TypedVar> tvars0 = new ArrayList<TypedVar>(constr.tvars.length);
    for (TypedVar tvar0 : constr.tvars) tvars0.add((TypedVar)tvar0.accept(this));
    return DataTypeConstr.create(id0, tvars0);
  }
}
