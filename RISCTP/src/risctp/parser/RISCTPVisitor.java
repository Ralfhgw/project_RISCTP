// Generated from RISCTP.g4 by ANTLR 4.13.1

  package risctp.parser;

import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link RISCTPParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface RISCTPVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link RISCTPParser#problem}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProblem(RISCTPParser.ProblemContext ctx);
	/**
	 * Visit a parse tree produced by the {@code TypeDecl}
	 * labeled alternative in {@link RISCTPParser#decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeDecl(RISCTPParser.TypeDeclContext ctx);
	/**
	 * Visit a parse tree produced by the {@code DataType}
	 * labeled alternative in {@link RISCTPParser#decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDataType(RISCTPParser.DataTypeContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Function}
	 * labeled alternative in {@link RISCTPParser#decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunction(RISCTPParser.FunctionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Constant}
	 * labeled alternative in {@link RISCTPParser#decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstant(RISCTPParser.ConstantContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Predicate}
	 * labeled alternative in {@link RISCTPParser#decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPredicate(RISCTPParser.PredicateContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Axiom}
	 * labeled alternative in {@link RISCTPParser#decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAxiom(RISCTPParser.AxiomContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Theorem}
	 * labeled alternative in {@link RISCTPParser#decl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTheorem(RISCTPParser.TheoremContext ctx);
	/**
	 * Visit a parse tree produced by the {@code TupleSelect}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTupleSelect(RISCTPParser.TupleSelectContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Or}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOr(RISCTPParser.OrContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Apply}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitApply(RISCTPParser.ApplyContext ctx);
	/**
	 * Visit a parse tree produced by the {@code True}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTrue(RISCTPParser.TrueContext ctx);
	/**
	 * Visit a parse tree produced by the {@code False}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFalse(RISCTPParser.FalseContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ChooseSat}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitChooseSat(RISCTPParser.ChooseSatContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Match}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMatch(RISCTPParser.MatchContext ctx);
	/**
	 * Visit a parse tree produced by the {@code GreaterEqual}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGreaterEqual(RISCTPParser.GreaterEqualContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Decimal}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDecimal(RISCTPParser.DecimalContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Equal}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEqual(RISCTPParser.EqualContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Equiv}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEquiv(RISCTPParser.EquivContext ctx);
	/**
	 * Visit a parse tree produced by the {@code MapStore}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMapStore(RISCTPParser.MapStoreContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Forall}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForall(RISCTPParser.ForallContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Plus}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPlus(RISCTPParser.PlusContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ChooseUni}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitChooseUni(RISCTPParser.ChooseUniContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Less}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLess(RISCTPParser.LessContext ctx);
	/**
	 * Visit a parse tree produced by the {@code MapSelect}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMapSelect(RISCTPParser.MapSelectContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Parentheses}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParentheses(RISCTPParser.ParenthesesContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Mod}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMod(RISCTPParser.ModContext ctx);
	/**
	 * Visit a parse tree produced by the {@code NotEqual}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNotEqual(RISCTPParser.NotEqualContext ctx);
	/**
	 * Visit a parse tree produced by the {@code LessEqual}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLessEqual(RISCTPParser.LessEqualContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ChooseDef}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitChooseDef(RISCTPParser.ChooseDefContext ctx);
	/**
	 * Visit a parse tree produced by the {@code TupleConstruct}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTupleConstruct(RISCTPParser.TupleConstructContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Imp}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImp(RISCTPParser.ImpContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Div}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDiv(RISCTPParser.DivContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Neg}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNeg(RISCTPParser.NegContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Not}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNot(RISCTPParser.NotContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ApplyNotGoal}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitApplyNotGoal(RISCTPParser.ApplyNotGoalContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Mult}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMult(RISCTPParser.MultContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Choose}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitChoose(RISCTPParser.ChooseContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Exists}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExists(RISCTPParser.ExistsContext ctx);
	/**
	 * Visit a parse tree produced by the {@code And}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnd(RISCTPParser.AndContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Let}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLet(RISCTPParser.LetContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Greater}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGreater(RISCTPParser.GreaterContext ctx);
	/**
	 * Visit a parse tree produced by the {@code IfThenElse}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIfThenElse(RISCTPParser.IfThenElseContext ctx);
	/**
	 * Visit a parse tree produced by the {@code TupleStore}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTupleStore(RISCTPParser.TupleStoreContext ctx);
	/**
	 * Visit a parse tree produced by the {@code MapConstruct}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMapConstruct(RISCTPParser.MapConstructContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Minus}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMinus(RISCTPParser.MinusContext ctx);
	/**
	 * Visit a parse tree produced by {@link RISCTPParser#type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType(RISCTPParser.TypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link RISCTPParser#tvar}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTvar(RISCTPParser.TvarContext ctx);
	/**
	 * Visit a parse tree produced by {@link RISCTPParser#lbinder}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLbinder(RISCTPParser.LbinderContext ctx);
	/**
	 * Visit a parse tree produced by {@link RISCTPParser#mbinder}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMbinder(RISCTPParser.MbinderContext ctx);
	/**
	 * Visit a parse tree produced by the {@code DefaultPattern}
	 * labeled alternative in {@link RISCTPParser#pattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDefaultPattern(RISCTPParser.DefaultPatternContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ConstrPattern}
	 * labeled alternative in {@link RISCTPParser#pattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstrPattern(RISCTPParser.ConstrPatternContext ctx);
	/**
	 * Visit a parse tree produced by {@link RISCTPParser#dtitem}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDtitem(RISCTPParser.DtitemContext ctx);
	/**
	 * Visit a parse tree produced by {@link RISCTPParser#dtconstr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDtconstr(RISCTPParser.DtconstrContext ctx);
	/**
	 * Visit a parse tree produced by the {@code PlainId}
	 * labeled alternative in {@link RISCTPParser#id}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPlainId(RISCTPParser.PlainIdContext ctx);
	/**
	 * Visit a parse tree produced by the {@code QuotedId}
	 * labeled alternative in {@link RISCTPParser#id}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuotedId(RISCTPParser.QuotedIdContext ctx);
	/**
	 * Visit a parse tree produced by {@link RISCTPParser#pid}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPid(RISCTPParser.PidContext ctx);
	/**
	 * Visit a parse tree produced by {@link RISCTPParser#dec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDec(RISCTPParser.DecContext ctx);
}