// ---------------------------------------------------------------------------
// TreeTranslator.java
// Translation from ANTLR 4 parse trees to abstract syntax trees.
// $Id: TreeTranslator.java,v 1.16 2024/06/06 17:23:56 schreine Exp $
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
package risctp.parser;

import java.util.*;

import risctp.syntax.AST.*;
import risctp.syntax.AST.Decl.*;
import risctp.syntax.AST.Exp.*;

import static risctp.syntax.AST.*;
import static risctp.syntax.AST.Pattern.*;

public class TreeTranslator extends RISCTPBaseVisitor<Object>
{
  public Problem visitProblem(RISCTPParser.ProblemContext c)
  {
    List<Decl> decls = new ArrayList<Decl>(c.decl().size());
    for (RISCTPParser.DeclContext d : c.decl())
    {
      decls.add((Decl)visit(d));
    }
    return Problem.create(decls);
  }
  
  public TypeDecl visitTypeDecl(RISCTPParser.TypeDeclContext c)
  {
    Id id = (Id)visit(c.id());
    Type type = c.type() == null ? null : (Type)visit(c.type());
    Exp exp = c.exp() == null ? null : (Exp)visit(c.exp());
    return TypeDecl.create(id, type, exp);
  }
  
  public DataType visitDataType(RISCTPParser.DataTypeContext c)
  {
    List<DataTypeItem> items = new ArrayList<DataTypeItem>(c.dtitem().size());
    for (RISCTPParser.DtitemContext d : c.dtitem())
    {
      items.add((DataTypeItem)visit(d));
    }
    return DataType.create(items);   
  }
  
  public Function visitFunction(RISCTPParser.FunctionContext c)
  {
    Id id = (Id)visit(c.id());
    List<TypedVar> tvars = new ArrayList<TypedVar>(c.tvar().size());
    for (RISCTPParser.TvarContext d : c.tvar())
    {
      tvars.add((TypedVar)visit(d));
    }
    Type type = (Type)visit(c.type());
    Exp exp = c.exp() == null ? null : (Exp)visit(c.exp());
    return Function.create(id, tvars, type, exp);
  }
  
  public Function visitConstant(RISCTPParser.ConstantContext c)
  {
    Id id = (Id)visit(c.id());
    List<TypedVar> tvars = new ArrayList<TypedVar>(0);
    Type type = (Type)visit(c.type());
    Exp exp = c.exp() == null ? null : (Exp)visit(c.exp());
    return Function.create(id, tvars, type, exp);
  }
  
  public Function visitPredicate(RISCTPParser.PredicateContext c)
  {
    Id id = (Id)visit(c.id());
    List<TypedVar> tvars = new ArrayList<TypedVar>(c.tvar().size());
    for (RISCTPParser.TvarContext d : c.tvar())
    {
      tvars.add((TypedVar)visit(d));
    }
    Type type = Type.create(Id.create(BOOLNAME));
    Exp exp = c.exp() == null ? null : (Exp)visit(c.exp());
    return Function.create(id, tvars, type, exp);
  }
  
  public Axiom visitAxiom(RISCTPParser.AxiomContext c)
  {
    Id aid = (Id)visit(c.aid);
    Exp exp = (Exp)visit(c.exp());
    Axiom axiom = Axiom.create(aid, exp);
    if (c.fid != null)
    {
      Id fid = (Id)visit(c.fid);
      int n = c.type().size();
      Type[] types = new Type[n];
      for (int i = 0; i < n; i++)
        types[i] = (Type)visit(c.type(i));
      axiom.setFunction(fid, types);
    }
    return axiom;
  }
  
  public Theorem visitTheorem(RISCTPParser.TheoremContext c)
  {
    Id id = (Id)visit(c.id());
    Exp exp = (Exp)visit(c.exp());
    return Theorem.create(id, exp);
  }
  
  public Apply visitDecimal(RISCTPParser.DecimalContext c)
  {
    String string = (String)visit(c.dec());
    Id id = Id.create(string);
    List<Exp> exps = new ArrayList<Exp>(0);
    return Apply.create(id, exps);
  }
  
  public Apply visitApply(RISCTPParser.ApplyContext c)
  {
    Id id = (Id)visit(c.id());
    List<Exp> exps = new ArrayList<Exp>(c.exp().size());
    for (RISCTPParser.ExpContext d : c.exp())
    {
      exps.add((Exp)visit(d));
    }
    return Apply.create(id, exps);
  }
  
  public Apply visitApplyNotGoal(RISCTPParser.ApplyNotGoalContext c)
  {
    Id id = (Id)visit(c.id());
    List<Exp> exps = new ArrayList<Exp>(c.exp().size());
    for (RISCTPParser.ExpContext d : c.exp())
    {
      exps.add((Exp)visit(d));
    }
    Apply apply = Apply.create(id, exps);
    apply.setNotGoal(true);
    return apply;
  }
  
  public Apply visitNeg(RISCTPParser.NegContext c)
  {
    Id id = Id.create(MINUSNAME);
    List<Exp> exps = new ArrayList<Exp>(1);
    exps.add((Exp)visit(c.exp()));
    return Apply.create(id, exps);
  }
  
  public Apply visitPlus(RISCTPParser.PlusContext c)
  {
    Id id = Id.create(PLUSNAME);
    List<Exp> exps = new ArrayList<Exp>(2);
    exps.add((Exp)visit(c.exp(0)));
    exps.add((Exp)visit(c.exp(1)));
    return Apply.create(id, exps);
  }
  
  public Apply visitMinus(RISCTPParser.MinusContext c)
  {
    Id id = Id.create(MINUSNAME);
    List<Exp> exps = new ArrayList<Exp>(2);
    exps.add((Exp)visit(c.exp(0)));
    exps.add((Exp)visit(c.exp(1)));
    return Apply.create(id, exps);
  }
  
  public Apply visitMult(RISCTPParser.MultContext c)
  {
    Id id = Id.create(MULTNAME);
    List<Exp> exps = new ArrayList<Exp>(2);
    exps.add((Exp)visit(c.exp(0)));
    exps.add((Exp)visit(c.exp(1)));
    return Apply.create(id, exps);
  }
  
  public Apply visitDiv(RISCTPParser.DivContext c)
  {
    Id id = Id.create(DIVNAME);
    List<Exp> exps = new ArrayList<Exp>(2);
    exps.add((Exp)visit(c.exp(0)));
    exps.add((Exp)visit(c.exp(1)));
    return Apply.create(id, exps);
  }
  
  public Apply visitMod(RISCTPParser.ModContext c)
  {
    Id id = Id.create(MODNAME);
    List<Exp> exps = new ArrayList<Exp>(2);
    exps.add((Exp)visit(c.exp(0)));
    exps.add((Exp)visit(c.exp(1)));
    return Apply.create(id, exps);
  }
  
  public Apply visitEqual(RISCTPParser.EqualContext c)
  {
    Id id = Id.create(EQUALNAME);
    List<Exp> exps = new ArrayList<Exp>(2);
    exps.add((Exp)visit(c.exp(0)));
    exps.add((Exp)visit(c.exp(1)));
    return Apply.create(id, exps);
  }
  
  public Apply visitNotEqual(RISCTPParser.NotEqualContext c)
  {
    Id id = Id.create(NOTEQUALNAME);
    List<Exp> exps = new ArrayList<Exp>(2);
    exps.add((Exp)visit(c.exp(0)));
    exps.add((Exp)visit(c.exp(1)));
    return Apply.create(id, exps);
  }
  
  public Apply visitLess(RISCTPParser.LessContext c)
  {
    Id id = Id.create(LESSNAME);
    List<Exp> exps = new ArrayList<Exp>(2);
    exps.add((Exp)visit(c.exp(0)));
    exps.add((Exp)visit(c.exp(1)));
    return Apply.create(id, exps);
  }
  
  public Apply visitLessEqual(RISCTPParser.LessEqualContext c)
  {
    Id id = Id.create(LESSEQUALNAME);
    List<Exp> exps = new ArrayList<Exp>(2);
    exps.add((Exp)visit(c.exp(0)));
    exps.add((Exp)visit(c.exp(1)));
    return Apply.create(id, exps);
  }
  
  public Apply visitGreater(RISCTPParser.GreaterContext c)
  {
    Id id = Id.create(GREATERNAME);
    List<Exp> exps = new ArrayList<Exp>(2);
    exps.add((Exp)visit(c.exp(0)));
    exps.add((Exp)visit(c.exp(1)));
    return Apply.create(id, exps);
  }
  
  public Apply visitGreaterEqual(RISCTPParser.GreaterEqualContext c)
  {
    Id id = Id.create(GREATEREQUALNAME);
    List<Exp> exps = new ArrayList<Exp>(2);
    exps.add((Exp)visit(c.exp(0)));
    exps.add((Exp)visit(c.exp(1)));
    return Apply.create(id, exps);
  }
  
  public Apply visitMapConstruct(RISCTPParser.MapConstructContext c)
  {
    String pid1 = (String)visit(c.pid(0));
    String pid2 = (String)visit(c.pid(0));
    Id id = Id.create(new String[] { MAPCONSTRUCTNAME, pid1, pid2 });
    List<Exp> exps = new ArrayList<Exp>(1);
    exps.add((Exp)visit(c.exp()));
    return Apply.create(id, exps);
  }
  
  public Apply visitTupleConstruct(RISCTPParser.TupleConstructContext c)
  {
    Id id = Id.create(TUPLECONSTRUCTNAME);
    List<Exp> exps = new ArrayList<Exp>(c.exp().size());
    for (RISCTPParser.ExpContext d : c.exp())
    {
      exps.add((Exp)visit(d));
    }
    return Apply.create(id, exps);
  }
  
  public Apply visitMapSelect(RISCTPParser.MapSelectContext c)
  {
    Id id = Id.create(MAPSELECTNAME);
    List<Exp> exps = new ArrayList<Exp>(2);
    exps.add((Exp)visit(c.exp(0)));
    exps.add((Exp)visit(c.exp(1)));
    return Apply.create(id, exps);
  }
  
  public Apply visitTupleSelect(RISCTPParser.TupleSelectContext c)
  {
    String dec = (String)visit(c.dec());
    Id id = Id.create(new String[] { TUPLESELECTNAME, dec });
    List<Exp> exps = new ArrayList<Exp>(1);
    exps.add((Exp)visit(c.exp()));
    return Apply.create(id, exps);
  }
  
  public Apply visitMapStore(RISCTPParser.MapStoreContext c)
  {
    Id id = Id.create(MAPSTORENAME);
    List<Exp> exps = new ArrayList<Exp>(3);
    exps.add((Exp)visit(c.exp(0)));
    exps.add((Exp)visit(c.exp(1)));
    exps.add((Exp)visit(c.exp(2)));
    return Apply.create(id, exps);
  }
  
  public Apply visitTupleStore(RISCTPParser.TupleStoreContext c)
  {
    String dec = (String)visit(c.dec());
    Id id = Id.create(new String[] { TUPLESTORENAME, dec });
    List<Exp> exps = new ArrayList<Exp>(2);
    exps.add((Exp)visit(c.exp(0)));
    exps.add((Exp)visit(c.exp(1)));
    return Apply.create(id, exps);
  }

  public If visitIfThenElse(RISCTPParser.IfThenElseContext c)
  {
    Exp exp0 = (Exp)visit(c.exp(0));
    Exp exp1 = (Exp)visit(c.exp(1));
    Exp exp2 = (Exp)visit(c.exp(2));
    return If.create(exp0, exp1, exp2);
  }
  
  public Match visitMatch(RISCTPParser.MatchContext c)
  {
    Exp exp = (Exp)visit(c.exp());
    List<MatchBinder> mbinders = new ArrayList<MatchBinder>(c.mbinder().size());
    for (RISCTPParser.MbinderContext d : c.mbinder())
    {
      mbinders.add((MatchBinder)visit(d));
    }
    return Match.create(exp, mbinders);
  }
  
  public Let visitLet(RISCTPParser.LetContext c)
  {
    List<LetBinder> lbinders = new ArrayList<LetBinder>(c.lbinder().size());
    for (RISCTPParser.LbinderContext d : c.lbinder())
    {
      lbinders.add((LetBinder)visit(d));
    }
    Exp exp = (Exp)visit(c.exp());
    return Let.create(lbinders, exp);
  }
  
  public Choose visitChoose(RISCTPParser.ChooseContext c)
  {
    TypedVar tvar = (TypedVar)visit(c.tvar());
    Exp exp = (Exp)visit(c.exp());
    return Choose.create(Choose.Kind.ANY, tvar, exp);
  }

  public Choose visitChooseSat(RISCTPParser.ChooseSatContext c)
  {
    TypedVar tvar = (TypedVar)visit(c.tvar());
    Exp exp = (Exp)visit(c.exp());
    return Choose.create(Choose.Kind.SAT, tvar, exp);
  }
  
  public Choose visitChooseUni(RISCTPParser.ChooseUniContext c)
  {
    TypedVar tvar = (TypedVar)visit(c.tvar());
    Exp exp = (Exp)visit(c.exp());
    return Choose.create(Choose.Kind.UNI, tvar, exp);
  }
  
  public Choose visitChooseDef(RISCTPParser.ChooseDefContext c)
  {
    TypedVar tvar = (TypedVar)visit(c.tvar());
    Exp exp = (Exp)visit(c.exp());
    return Choose.create(Choose.Kind.DEF, tvar, exp);
  }
  
  public False visitFalse(RISCTPParser.FalseContext c)
  {
    return False.create();
  }
  
  public True visitTrue(RISCTPParser.TrueContext c)
  {
    return True.create();
  }
  
  public Not visitNot(RISCTPParser.NotContext c)
  {
    Exp exp = (Exp)visit(c.exp());
    return Not.create(exp);
  }
  
  public And visitAnd(RISCTPParser.AndContext c)
  {
    Exp exp0 = (Exp)visit(c.exp(0));
    Exp exp1 = (Exp)visit(c.exp(1));
    return And.create(exp0, exp1);
  }
  
  public Or visitOr(RISCTPParser.OrContext c)
  {
    Exp exp0 = (Exp)visit(c.exp(0));
    Exp exp1 = (Exp)visit(c.exp(1));
    return Or.create(exp0, exp1);
  }
  
  public Imp visitImp(RISCTPParser.ImpContext c)
  {
    Exp exp0 = (Exp)visit(c.exp(0));
    Exp exp1 = (Exp)visit(c.exp(1));
    return Imp.create(exp0, exp1);
  }
  
  public Equiv visitEquiv(RISCTPParser.EquivContext c)
  {
    Exp exp0 = (Exp)visit(c.exp(0));
    Exp exp1 = (Exp)visit(c.exp(1));
    return Equiv.create(exp0, exp1);
  }
  
  public Forall visitForall(RISCTPParser.ForallContext c)
  {
    Exp exp = (Exp)visit(c.exp());
    int n = c.tvar().size();
    for (int i = n-1; i >= 0; i--)
    {
      TypedVar tvar = (TypedVar)visit(c.tvar(i));
      exp = Forall.create(tvar, exp);
    }
    return (Forall)exp;
  }
  
  public Exists visitExists(RISCTPParser.ExistsContext c)
  {
    Exp exp = (Exp)visit(c.exp());
    int n = c.tvar().size();
    for (int i = n-1; i >= 0; i--)
    {
      TypedVar tvar = (TypedVar)visit(c.tvar(i));
      exp = Exists.create(tvar, exp);
    }
    return (Exists)exp;
  }
  
  public Exp visitParentheses(RISCTPParser.ParenthesesContext c)
  {
    Exp exp = (Exp)visit(c.exp());
    return exp;
  }
  
  public Type visitType(RISCTPParser.TypeContext c)
  {
    Id id = (Id)visit(c.id());
    List<Type> types = new ArrayList<Type>(c.type().size());
    for (RISCTPParser.TypeContext d : c.type())
    {
      types.add((Type)visit(d));
    }
    return Type.create(id, types);
  }
  
  public TypedVar visitTvar(RISCTPParser.TvarContext c)
  {
    Id id = (Id)visit(c.id());
    Type type = (Type)visit(c.type());
    return TypedVar.create(id, type);
  }
  
  public LetBinder visitLbinder(RISCTPParser.LbinderContext c)
  {
    Id id = (Id)visit(c.id());
    Exp exp = (Exp)visit(c.exp());
    return LetBinder.create(id, exp);
  }
  
  public MatchBinder visitMbinder(RISCTPParser.MbinderContext c)
  {
    Pattern pattern = (Pattern)visit(c.pattern());
    Exp exp = (Exp)visit(c.exp());
    return MatchBinder.create(pattern, exp);
  }
  
  public DefaultPattern visitDefaultPattern(RISCTPParser.DefaultPatternContext c)
  {
    return DefaultPattern.create();
  }
  
  public ConstrPattern visitConstrPattern(RISCTPParser.ConstrPatternContext c)
  {
    Id id = (Id)visit(c.id());
    List<TypedVar> tvars = new ArrayList<TypedVar>(c.tvar().size());
    for (RISCTPParser.TvarContext d : c.tvar())
    {
      tvars.add((TypedVar)visit(d));
    }
    return ConstrPattern.create(id, tvars);
  }
  
  public DataTypeItem visitDtitem(RISCTPParser.DtitemContext c)
  {
    Id id = (Id)visit(c.id());
    List<DataTypeConstr> dtconstrs = new ArrayList<DataTypeConstr>(c.dtconstr().size());
    for (RISCTPParser.DtconstrContext d : c.dtconstr())
    {
      dtconstrs.add((DataTypeConstr)visit(d));
    }
    Exp exp = c.exp() == null ? null : (Exp)visit(c.exp());
    return DataTypeItem.create(id, dtconstrs, exp);
  }
  
  public DataTypeConstr visitDtconstr(RISCTPParser.DtconstrContext c)
  {
    Id id = (Id)visit(c.id());
    List<TypedVar> tvars = new ArrayList<TypedVar>(c.tvar().size());
    for (RISCTPParser.TvarContext d : c.tvar())
    {
      tvars.add((TypedVar)visit(d));
    }
    return DataTypeConstr.create(id, tvars);
  }
  
  public Id visitPlainId(RISCTPParser.PlainIdContext c)
  {
    return Id.create(c.getText());
  }
  
  public Id visitQuotedId(RISCTPParser.QuotedIdContext c)
  {
    return Id.create(unquote(c.getText()));
  }
  
  public String visitPid(RISCTPParser.PidContext c)
  {
    return c.getText();
  }
  
  public String visitDec(RISCTPParser.DecContext c)
  {
    return c.getText();
  }
}
//----------------------------------------------------------------------------
// end of file
//----------------------------------------------------------------------------