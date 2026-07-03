// Generated from RISCTP.g4 by ANTLR 4.13.1

  package risctp.parser;

import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link RISCTPParser}.
 */
public interface RISCTPListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link RISCTPParser#problem}.
	 * @param ctx the parse tree
	 */
	void enterProblem(RISCTPParser.ProblemContext ctx);
	/**
	 * Exit a parse tree produced by {@link RISCTPParser#problem}.
	 * @param ctx the parse tree
	 */
	void exitProblem(RISCTPParser.ProblemContext ctx);
	/**
	 * Enter a parse tree produced by the {@code TypeDecl}
	 * labeled alternative in {@link RISCTPParser#decl}.
	 * @param ctx the parse tree
	 */
	void enterTypeDecl(RISCTPParser.TypeDeclContext ctx);
	/**
	 * Exit a parse tree produced by the {@code TypeDecl}
	 * labeled alternative in {@link RISCTPParser#decl}.
	 * @param ctx the parse tree
	 */
	void exitTypeDecl(RISCTPParser.TypeDeclContext ctx);
	/**
	 * Enter a parse tree produced by the {@code DataType}
	 * labeled alternative in {@link RISCTPParser#decl}.
	 * @param ctx the parse tree
	 */
	void enterDataType(RISCTPParser.DataTypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code DataType}
	 * labeled alternative in {@link RISCTPParser#decl}.
	 * @param ctx the parse tree
	 */
	void exitDataType(RISCTPParser.DataTypeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Function}
	 * labeled alternative in {@link RISCTPParser#decl}.
	 * @param ctx the parse tree
	 */
	void enterFunction(RISCTPParser.FunctionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Function}
	 * labeled alternative in {@link RISCTPParser#decl}.
	 * @param ctx the parse tree
	 */
	void exitFunction(RISCTPParser.FunctionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Constant}
	 * labeled alternative in {@link RISCTPParser#decl}.
	 * @param ctx the parse tree
	 */
	void enterConstant(RISCTPParser.ConstantContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Constant}
	 * labeled alternative in {@link RISCTPParser#decl}.
	 * @param ctx the parse tree
	 */
	void exitConstant(RISCTPParser.ConstantContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Predicate}
	 * labeled alternative in {@link RISCTPParser#decl}.
	 * @param ctx the parse tree
	 */
	void enterPredicate(RISCTPParser.PredicateContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Predicate}
	 * labeled alternative in {@link RISCTPParser#decl}.
	 * @param ctx the parse tree
	 */
	void exitPredicate(RISCTPParser.PredicateContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Axiom}
	 * labeled alternative in {@link RISCTPParser#decl}.
	 * @param ctx the parse tree
	 */
	void enterAxiom(RISCTPParser.AxiomContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Axiom}
	 * labeled alternative in {@link RISCTPParser#decl}.
	 * @param ctx the parse tree
	 */
	void exitAxiom(RISCTPParser.AxiomContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Theorem}
	 * labeled alternative in {@link RISCTPParser#decl}.
	 * @param ctx the parse tree
	 */
	void enterTheorem(RISCTPParser.TheoremContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Theorem}
	 * labeled alternative in {@link RISCTPParser#decl}.
	 * @param ctx the parse tree
	 */
	void exitTheorem(RISCTPParser.TheoremContext ctx);
	/**
	 * Enter a parse tree produced by the {@code TupleSelect}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void enterTupleSelect(RISCTPParser.TupleSelectContext ctx);
	/**
	 * Exit a parse tree produced by the {@code TupleSelect}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void exitTupleSelect(RISCTPParser.TupleSelectContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Or}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void enterOr(RISCTPParser.OrContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Or}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void exitOr(RISCTPParser.OrContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Apply}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void enterApply(RISCTPParser.ApplyContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Apply}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void exitApply(RISCTPParser.ApplyContext ctx);
	/**
	 * Enter a parse tree produced by the {@code True}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void enterTrue(RISCTPParser.TrueContext ctx);
	/**
	 * Exit a parse tree produced by the {@code True}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void exitTrue(RISCTPParser.TrueContext ctx);
	/**
	 * Enter a parse tree produced by the {@code False}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void enterFalse(RISCTPParser.FalseContext ctx);
	/**
	 * Exit a parse tree produced by the {@code False}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void exitFalse(RISCTPParser.FalseContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ChooseSat}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void enterChooseSat(RISCTPParser.ChooseSatContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ChooseSat}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void exitChooseSat(RISCTPParser.ChooseSatContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Match}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void enterMatch(RISCTPParser.MatchContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Match}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void exitMatch(RISCTPParser.MatchContext ctx);
	/**
	 * Enter a parse tree produced by the {@code GreaterEqual}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void enterGreaterEqual(RISCTPParser.GreaterEqualContext ctx);
	/**
	 * Exit a parse tree produced by the {@code GreaterEqual}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void exitGreaterEqual(RISCTPParser.GreaterEqualContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Decimal}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void enterDecimal(RISCTPParser.DecimalContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Decimal}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void exitDecimal(RISCTPParser.DecimalContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Equal}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void enterEqual(RISCTPParser.EqualContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Equal}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void exitEqual(RISCTPParser.EqualContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Equiv}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void enterEquiv(RISCTPParser.EquivContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Equiv}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void exitEquiv(RISCTPParser.EquivContext ctx);
	/**
	 * Enter a parse tree produced by the {@code MapStore}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void enterMapStore(RISCTPParser.MapStoreContext ctx);
	/**
	 * Exit a parse tree produced by the {@code MapStore}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void exitMapStore(RISCTPParser.MapStoreContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Forall}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void enterForall(RISCTPParser.ForallContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Forall}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void exitForall(RISCTPParser.ForallContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Plus}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void enterPlus(RISCTPParser.PlusContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Plus}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void exitPlus(RISCTPParser.PlusContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ChooseUni}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void enterChooseUni(RISCTPParser.ChooseUniContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ChooseUni}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void exitChooseUni(RISCTPParser.ChooseUniContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Less}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void enterLess(RISCTPParser.LessContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Less}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void exitLess(RISCTPParser.LessContext ctx);
	/**
	 * Enter a parse tree produced by the {@code MapSelect}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void enterMapSelect(RISCTPParser.MapSelectContext ctx);
	/**
	 * Exit a parse tree produced by the {@code MapSelect}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void exitMapSelect(RISCTPParser.MapSelectContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Parentheses}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void enterParentheses(RISCTPParser.ParenthesesContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Parentheses}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void exitParentheses(RISCTPParser.ParenthesesContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Mod}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void enterMod(RISCTPParser.ModContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Mod}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void exitMod(RISCTPParser.ModContext ctx);
	/**
	 * Enter a parse tree produced by the {@code NotEqual}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void enterNotEqual(RISCTPParser.NotEqualContext ctx);
	/**
	 * Exit a parse tree produced by the {@code NotEqual}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void exitNotEqual(RISCTPParser.NotEqualContext ctx);
	/**
	 * Enter a parse tree produced by the {@code LessEqual}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void enterLessEqual(RISCTPParser.LessEqualContext ctx);
	/**
	 * Exit a parse tree produced by the {@code LessEqual}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void exitLessEqual(RISCTPParser.LessEqualContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ChooseDef}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void enterChooseDef(RISCTPParser.ChooseDefContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ChooseDef}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void exitChooseDef(RISCTPParser.ChooseDefContext ctx);
	/**
	 * Enter a parse tree produced by the {@code TupleConstruct}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void enterTupleConstruct(RISCTPParser.TupleConstructContext ctx);
	/**
	 * Exit a parse tree produced by the {@code TupleConstruct}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void exitTupleConstruct(RISCTPParser.TupleConstructContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Imp}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void enterImp(RISCTPParser.ImpContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Imp}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void exitImp(RISCTPParser.ImpContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Div}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void enterDiv(RISCTPParser.DivContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Div}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void exitDiv(RISCTPParser.DivContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Neg}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void enterNeg(RISCTPParser.NegContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Neg}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void exitNeg(RISCTPParser.NegContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Not}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void enterNot(RISCTPParser.NotContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Not}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void exitNot(RISCTPParser.NotContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ApplyNotGoal}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void enterApplyNotGoal(RISCTPParser.ApplyNotGoalContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ApplyNotGoal}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void exitApplyNotGoal(RISCTPParser.ApplyNotGoalContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Mult}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void enterMult(RISCTPParser.MultContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Mult}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void exitMult(RISCTPParser.MultContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Choose}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void enterChoose(RISCTPParser.ChooseContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Choose}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void exitChoose(RISCTPParser.ChooseContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Exists}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void enterExists(RISCTPParser.ExistsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Exists}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void exitExists(RISCTPParser.ExistsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code And}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void enterAnd(RISCTPParser.AndContext ctx);
	/**
	 * Exit a parse tree produced by the {@code And}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void exitAnd(RISCTPParser.AndContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Let}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void enterLet(RISCTPParser.LetContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Let}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void exitLet(RISCTPParser.LetContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Greater}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void enterGreater(RISCTPParser.GreaterContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Greater}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void exitGreater(RISCTPParser.GreaterContext ctx);
	/**
	 * Enter a parse tree produced by the {@code IfThenElse}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void enterIfThenElse(RISCTPParser.IfThenElseContext ctx);
	/**
	 * Exit a parse tree produced by the {@code IfThenElse}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void exitIfThenElse(RISCTPParser.IfThenElseContext ctx);
	/**
	 * Enter a parse tree produced by the {@code TupleStore}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void enterTupleStore(RISCTPParser.TupleStoreContext ctx);
	/**
	 * Exit a parse tree produced by the {@code TupleStore}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void exitTupleStore(RISCTPParser.TupleStoreContext ctx);
	/**
	 * Enter a parse tree produced by the {@code MapConstruct}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void enterMapConstruct(RISCTPParser.MapConstructContext ctx);
	/**
	 * Exit a parse tree produced by the {@code MapConstruct}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void exitMapConstruct(RISCTPParser.MapConstructContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Minus}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void enterMinus(RISCTPParser.MinusContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Minus}
	 * labeled alternative in {@link RISCTPParser#exp}.
	 * @param ctx the parse tree
	 */
	void exitMinus(RISCTPParser.MinusContext ctx);
	/**
	 * Enter a parse tree produced by {@link RISCTPParser#type}.
	 * @param ctx the parse tree
	 */
	void enterType(RISCTPParser.TypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link RISCTPParser#type}.
	 * @param ctx the parse tree
	 */
	void exitType(RISCTPParser.TypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link RISCTPParser#tvar}.
	 * @param ctx the parse tree
	 */
	void enterTvar(RISCTPParser.TvarContext ctx);
	/**
	 * Exit a parse tree produced by {@link RISCTPParser#tvar}.
	 * @param ctx the parse tree
	 */
	void exitTvar(RISCTPParser.TvarContext ctx);
	/**
	 * Enter a parse tree produced by {@link RISCTPParser#lbinder}.
	 * @param ctx the parse tree
	 */
	void enterLbinder(RISCTPParser.LbinderContext ctx);
	/**
	 * Exit a parse tree produced by {@link RISCTPParser#lbinder}.
	 * @param ctx the parse tree
	 */
	void exitLbinder(RISCTPParser.LbinderContext ctx);
	/**
	 * Enter a parse tree produced by {@link RISCTPParser#mbinder}.
	 * @param ctx the parse tree
	 */
	void enterMbinder(RISCTPParser.MbinderContext ctx);
	/**
	 * Exit a parse tree produced by {@link RISCTPParser#mbinder}.
	 * @param ctx the parse tree
	 */
	void exitMbinder(RISCTPParser.MbinderContext ctx);
	/**
	 * Enter a parse tree produced by the {@code DefaultPattern}
	 * labeled alternative in {@link RISCTPParser#pattern}.
	 * @param ctx the parse tree
	 */
	void enterDefaultPattern(RISCTPParser.DefaultPatternContext ctx);
	/**
	 * Exit a parse tree produced by the {@code DefaultPattern}
	 * labeled alternative in {@link RISCTPParser#pattern}.
	 * @param ctx the parse tree
	 */
	void exitDefaultPattern(RISCTPParser.DefaultPatternContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ConstrPattern}
	 * labeled alternative in {@link RISCTPParser#pattern}.
	 * @param ctx the parse tree
	 */
	void enterConstrPattern(RISCTPParser.ConstrPatternContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ConstrPattern}
	 * labeled alternative in {@link RISCTPParser#pattern}.
	 * @param ctx the parse tree
	 */
	void exitConstrPattern(RISCTPParser.ConstrPatternContext ctx);
	/**
	 * Enter a parse tree produced by {@link RISCTPParser#dtitem}.
	 * @param ctx the parse tree
	 */
	void enterDtitem(RISCTPParser.DtitemContext ctx);
	/**
	 * Exit a parse tree produced by {@link RISCTPParser#dtitem}.
	 * @param ctx the parse tree
	 */
	void exitDtitem(RISCTPParser.DtitemContext ctx);
	/**
	 * Enter a parse tree produced by {@link RISCTPParser#dtconstr}.
	 * @param ctx the parse tree
	 */
	void enterDtconstr(RISCTPParser.DtconstrContext ctx);
	/**
	 * Exit a parse tree produced by {@link RISCTPParser#dtconstr}.
	 * @param ctx the parse tree
	 */
	void exitDtconstr(RISCTPParser.DtconstrContext ctx);
	/**
	 * Enter a parse tree produced by the {@code PlainId}
	 * labeled alternative in {@link RISCTPParser#id}.
	 * @param ctx the parse tree
	 */
	void enterPlainId(RISCTPParser.PlainIdContext ctx);
	/**
	 * Exit a parse tree produced by the {@code PlainId}
	 * labeled alternative in {@link RISCTPParser#id}.
	 * @param ctx the parse tree
	 */
	void exitPlainId(RISCTPParser.PlainIdContext ctx);
	/**
	 * Enter a parse tree produced by the {@code QuotedId}
	 * labeled alternative in {@link RISCTPParser#id}.
	 * @param ctx the parse tree
	 */
	void enterQuotedId(RISCTPParser.QuotedIdContext ctx);
	/**
	 * Exit a parse tree produced by the {@code QuotedId}
	 * labeled alternative in {@link RISCTPParser#id}.
	 * @param ctx the parse tree
	 */
	void exitQuotedId(RISCTPParser.QuotedIdContext ctx);
	/**
	 * Enter a parse tree produced by {@link RISCTPParser#pid}.
	 * @param ctx the parse tree
	 */
	void enterPid(RISCTPParser.PidContext ctx);
	/**
	 * Exit a parse tree produced by {@link RISCTPParser#pid}.
	 * @param ctx the parse tree
	 */
	void exitPid(RISCTPParser.PidContext ctx);
	/**
	 * Enter a parse tree produced by {@link RISCTPParser#dec}.
	 * @param ctx the parse tree
	 */
	void enterDec(RISCTPParser.DecContext ctx);
	/**
	 * Exit a parse tree produced by {@link RISCTPParser#dec}.
	 * @param ctx the parse tree
	 */
	void exitDec(RISCTPParser.DecContext ctx);
}