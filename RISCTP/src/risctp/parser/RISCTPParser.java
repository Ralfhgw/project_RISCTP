// Generated from RISCTP.g4 by ANTLR 4.13.1

  package risctp.parser;

import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue"})
public class RISCTPParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.13.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, T__7=8, T__8=9, 
		T__9=10, T__10=11, T__11=12, T__12=13, T__13=14, T__14=15, T__15=16, T__16=17, 
		T__17=18, T__18=19, T__19=20, T__20=21, T__21=22, T__22=23, T__23=24, 
		T__24=25, T__25=26, T__26=27, T__27=28, T__28=29, T__29=30, T__30=31, 
		T__31=32, T__32=33, T__33=34, T__34=35, T__35=36, T__36=37, T__37=38, 
		T__38=39, T__39=40, T__40=41, T__41=42, T__42=43, T__43=44, T__44=45, 
		T__45=46, T__46=47, T__47=48, T__48=49, T__49=50, T__50=51, T__51=52, 
		T__52=53, T__53=54, T__54=55, T__55=56, T__56=57, T__57=58, T__58=59, 
		T__59=60, T__60=61, T__61=62, T__62=63, T__63=64, T__64=65, T__65=66, 
		T__66=67, T__67=68, T__68=69, T__69=70, T__70=71, T__71=72, ID=73, QID=74, 
		DEC=75, EOS=76, WHITESPACE=77, LINECOMMENT=78, COMMENT=79, ERROR=80;
	public static final int
		RULE_problem = 0, RULE_decl = 1, RULE_exp = 2, RULE_type = 3, RULE_tvar = 4, 
		RULE_lbinder = 5, RULE_mbinder = 6, RULE_pattern = 7, RULE_dtitem = 8, 
		RULE_dtconstr = 9, RULE_id = 10, RULE_pid = 11, RULE_dec = 12;
	private static String[] makeRuleNames() {
		return new String[] {
			"problem", "decl", "exp", "type", "tvar", "lbinder", "mbinder", "pattern", 
			"dtitem", "dtconstr", "id", "pid", "dec"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'type'", "'='", "'with'", "'datatype'", "'and'", "'fun'", "'('", 
			"','", "')'", "':'", "'const'", "'pred'", "'\\u21D4'", "'<=>'", "'axiom'", 
			"'['", "']'", "'theorem'", "'{*}'", "'map'", "'\\u3008'", "'\\u27E8'", 
			"'\\u2329'", "'<<'", "'\\u3009'", "'\\u27E9'", "'\\u232A'", "'>>'", "'.'", 
			"'-'", "'*'", "'\\u22C5'", "'/'", "'%'", "'+'", "'\\u2260'", "'~='", 
			"'<'", "'\\u2264'", "'<='", "'>'", "'\\u2265'", "'>='", "'\\u22A5'", 
			"'false'", "'\\u22A4'", "'true'", "'\\u00AC'", "'~'", "'\\u2227'", "'/\\'", 
			"'\\u2228'", "'\\/'", "'\\u21D2'", "'=>'", "'\\u2200'", "'forall'", "'\\u2203'", 
			"'exists'", "'if'", "'then'", "'else'", "'match'", "'|'", "'let'", "'in'", 
			"'choose'", "'sat'", "'uni'", "'def'", "'->'", "'_'", null, null, null, 
			"';'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, "ID", "QID", "DEC", "EOS", "WHITESPACE", "LINECOMMENT", "COMMENT", 
			"ERROR"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "RISCTP.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public RISCTPParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ProblemContext extends ParserRuleContext {
		public TerminalNode EOF() { return getToken(RISCTPParser.EOF, 0); }
		public List<DeclContext> decl() {
			return getRuleContexts(DeclContext.class);
		}
		public DeclContext decl(int i) {
			return getRuleContext(DeclContext.class,i);
		}
		public ProblemContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_problem; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).enterProblem(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).exitProblem(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RISCTPVisitor ) return ((RISCTPVisitor<? extends T>)visitor).visitProblem(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ProblemContext problem() throws RecognitionException {
		ProblemContext _localctx = new ProblemContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_problem);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(29);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 301138L) != 0)) {
				{
				{
				setState(26);
				decl();
				}
				}
				setState(31);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(32);
			match(EOF);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DeclContext extends ParserRuleContext {
		public DeclContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_decl; }
	 
		public DeclContext() { }
		public void copyFrom(DeclContext ctx) {
			super.copyFrom(ctx);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class FunctionContext extends DeclContext {
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public TerminalNode EOS() { return getToken(RISCTPParser.EOS, 0); }
		public ExpContext exp() {
			return getRuleContext(ExpContext.class,0);
		}
		public List<TvarContext> tvar() {
			return getRuleContexts(TvarContext.class);
		}
		public TvarContext tvar(int i) {
			return getRuleContext(TvarContext.class,i);
		}
		public FunctionContext(DeclContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).enterFunction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).exitFunction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RISCTPVisitor ) return ((RISCTPVisitor<? extends T>)visitor).visitFunction(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ConstantContext extends DeclContext {
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public TerminalNode EOS() { return getToken(RISCTPParser.EOS, 0); }
		public ExpContext exp() {
			return getRuleContext(ExpContext.class,0);
		}
		public ConstantContext(DeclContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).enterConstant(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).exitConstant(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RISCTPVisitor ) return ((RISCTPVisitor<? extends T>)visitor).visitConstant(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class TypeDeclContext extends DeclContext {
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public TerminalNode EOS() { return getToken(RISCTPParser.EOS, 0); }
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public ExpContext exp() {
			return getRuleContext(ExpContext.class,0);
		}
		public TypeDeclContext(DeclContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).enterTypeDecl(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).exitTypeDecl(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RISCTPVisitor ) return ((RISCTPVisitor<? extends T>)visitor).visitTypeDecl(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class DataTypeContext extends DeclContext {
		public List<DtitemContext> dtitem() {
			return getRuleContexts(DtitemContext.class);
		}
		public DtitemContext dtitem(int i) {
			return getRuleContext(DtitemContext.class,i);
		}
		public TerminalNode EOS() { return getToken(RISCTPParser.EOS, 0); }
		public DataTypeContext(DeclContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).enterDataType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).exitDataType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RISCTPVisitor ) return ((RISCTPVisitor<? extends T>)visitor).visitDataType(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class TheoremContext extends DeclContext {
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public ExpContext exp() {
			return getRuleContext(ExpContext.class,0);
		}
		public TerminalNode EOS() { return getToken(RISCTPParser.EOS, 0); }
		public TheoremContext(DeclContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).enterTheorem(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).exitTheorem(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RISCTPVisitor ) return ((RISCTPVisitor<? extends T>)visitor).visitTheorem(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class AxiomContext extends DeclContext {
		public IdContext fid;
		public IdContext aid;
		public ExpContext exp() {
			return getRuleContext(ExpContext.class,0);
		}
		public TerminalNode EOS() { return getToken(RISCTPParser.EOS, 0); }
		public List<IdContext> id() {
			return getRuleContexts(IdContext.class);
		}
		public IdContext id(int i) {
			return getRuleContext(IdContext.class,i);
		}
		public List<TypeContext> type() {
			return getRuleContexts(TypeContext.class);
		}
		public TypeContext type(int i) {
			return getRuleContext(TypeContext.class,i);
		}
		public AxiomContext(DeclContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).enterAxiom(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).exitAxiom(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RISCTPVisitor ) return ((RISCTPVisitor<? extends T>)visitor).visitAxiom(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class PredicateContext extends DeclContext {
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public TerminalNode EOS() { return getToken(RISCTPParser.EOS, 0); }
		public ExpContext exp() {
			return getRuleContext(ExpContext.class,0);
		}
		public List<TvarContext> tvar() {
			return getRuleContexts(TvarContext.class);
		}
		public TvarContext tvar(int i) {
			return getRuleContext(TvarContext.class,i);
		}
		public PredicateContext(DeclContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).enterPredicate(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).exitPredicate(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RISCTPVisitor ) return ((RISCTPVisitor<? extends T>)visitor).visitPredicate(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DeclContext decl() throws RecognitionException {
		DeclContext _localctx = new DeclContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_decl);
		int _la;
		try {
			setState(145);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__0:
				_localctx = new TypeDeclContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(34);
				match(T__0);
				setState(35);
				id();
				setState(42);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__1) {
					{
					setState(36);
					match(T__1);
					setState(37);
					type();
					setState(40);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==T__2) {
						{
						setState(38);
						match(T__2);
						setState(39);
						exp(0);
						}
					}

					}
				}

				setState(44);
				match(EOS);
				}
				break;
			case T__3:
				_localctx = new DataTypeContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(46);
				match(T__3);
				setState(47);
				dtitem();
				setState(52);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__4) {
					{
					{
					setState(48);
					match(T__4);
					setState(49);
					dtitem();
					}
					}
					setState(54);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(55);
				match(EOS);
				}
				break;
			case T__5:
				_localctx = new FunctionContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(57);
				match(T__5);
				setState(58);
				id();
				setState(71);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__6) {
					{
					setState(59);
					match(T__6);
					setState(68);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==ID || _la==QID) {
						{
						setState(60);
						tvar();
						setState(65);
						_errHandler.sync(this);
						_la = _input.LA(1);
						while (_la==T__7) {
							{
							{
							setState(61);
							match(T__7);
							setState(62);
							tvar();
							}
							}
							setState(67);
							_errHandler.sync(this);
							_la = _input.LA(1);
						}
						}
					}

					setState(70);
					match(T__8);
					}
				}

				setState(73);
				match(T__9);
				setState(74);
				type();
				setState(77);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__1) {
					{
					setState(75);
					match(T__1);
					setState(76);
					exp(0);
					}
				}

				setState(79);
				match(EOS);
				}
				break;
			case T__10:
				_localctx = new ConstantContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(81);
				match(T__10);
				setState(82);
				id();
				setState(83);
				match(T__9);
				setState(84);
				type();
				setState(87);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__1) {
					{
					setState(85);
					match(T__1);
					setState(86);
					exp(0);
					}
				}

				setState(89);
				match(EOS);
				}
				break;
			case T__11:
				_localctx = new PredicateContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(91);
				match(T__11);
				setState(92);
				id();
				setState(105);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__6) {
					{
					setState(93);
					match(T__6);
					setState(102);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==ID || _la==QID) {
						{
						setState(94);
						tvar();
						setState(99);
						_errHandler.sync(this);
						_la = _input.LA(1);
						while (_la==T__7) {
							{
							{
							setState(95);
							match(T__7);
							setState(96);
							tvar();
							}
							}
							setState(101);
							_errHandler.sync(this);
							_la = _input.LA(1);
						}
						}
					}

					setState(104);
					match(T__8);
					}
				}

				setState(109);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__12 || _la==T__13) {
					{
					setState(107);
					_la = _input.LA(1);
					if ( !(_la==T__12 || _la==T__13) ) {
					_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					setState(108);
					exp(0);
					}
				}

				setState(111);
				match(EOS);
				}
				break;
			case T__14:
				_localctx = new AxiomContext(_localctx);
				enterOuterAlt(_localctx, 6);
				{
				setState(113);
				match(T__14);
				setState(132);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__15) {
					{
					setState(114);
					match(T__15);
					setState(115);
					((AxiomContext)_localctx).fid = id();
					setState(128);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==T__6) {
						{
						setState(116);
						match(T__6);
						setState(125);
						_errHandler.sync(this);
						_la = _input.LA(1);
						if (_la==ID || _la==QID) {
							{
							setState(117);
							type();
							setState(122);
							_errHandler.sync(this);
							_la = _input.LA(1);
							while (_la==T__7) {
								{
								{
								setState(118);
								match(T__7);
								setState(119);
								type();
								}
								}
								setState(124);
								_errHandler.sync(this);
								_la = _input.LA(1);
							}
							}
						}

						setState(127);
						match(T__8);
						}
					}

					setState(130);
					match(T__16);
					}
				}

				setState(134);
				((AxiomContext)_localctx).aid = id();
				setState(135);
				_la = _input.LA(1);
				if ( !(_la==T__12 || _la==T__13) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(136);
				exp(0);
				setState(137);
				match(EOS);
				}
				break;
			case T__17:
				_localctx = new TheoremContext(_localctx);
				enterOuterAlt(_localctx, 7);
				{
				setState(139);
				match(T__17);
				setState(140);
				id();
				setState(141);
				_la = _input.LA(1);
				if ( !(_la==T__12 || _la==T__13) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(142);
				exp(0);
				setState(143);
				match(EOS);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ExpContext extends ParserRuleContext {
		public ExpContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_exp; }
	 
		public ExpContext() { }
		public void copyFrom(ExpContext ctx) {
			super.copyFrom(ctx);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class TupleSelectContext extends ExpContext {
		public ExpContext exp() {
			return getRuleContext(ExpContext.class,0);
		}
		public DecContext dec() {
			return getRuleContext(DecContext.class,0);
		}
		public TupleSelectContext(ExpContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).enterTupleSelect(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).exitTupleSelect(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RISCTPVisitor ) return ((RISCTPVisitor<? extends T>)visitor).visitTupleSelect(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class OrContext extends ExpContext {
		public List<ExpContext> exp() {
			return getRuleContexts(ExpContext.class);
		}
		public ExpContext exp(int i) {
			return getRuleContext(ExpContext.class,i);
		}
		public OrContext(ExpContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).enterOr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).exitOr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RISCTPVisitor ) return ((RISCTPVisitor<? extends T>)visitor).visitOr(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ApplyContext extends ExpContext {
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public List<ExpContext> exp() {
			return getRuleContexts(ExpContext.class);
		}
		public ExpContext exp(int i) {
			return getRuleContext(ExpContext.class,i);
		}
		public ApplyContext(ExpContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).enterApply(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).exitApply(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RISCTPVisitor ) return ((RISCTPVisitor<? extends T>)visitor).visitApply(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class TrueContext extends ExpContext {
		public TrueContext(ExpContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).enterTrue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).exitTrue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RISCTPVisitor ) return ((RISCTPVisitor<? extends T>)visitor).visitTrue(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class FalseContext extends ExpContext {
		public FalseContext(ExpContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).enterFalse(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).exitFalse(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RISCTPVisitor ) return ((RISCTPVisitor<? extends T>)visitor).visitFalse(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ChooseSatContext extends ExpContext {
		public TvarContext tvar() {
			return getRuleContext(TvarContext.class,0);
		}
		public ExpContext exp() {
			return getRuleContext(ExpContext.class,0);
		}
		public ChooseSatContext(ExpContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).enterChooseSat(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).exitChooseSat(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RISCTPVisitor ) return ((RISCTPVisitor<? extends T>)visitor).visitChooseSat(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class MatchContext extends ExpContext {
		public ExpContext exp() {
			return getRuleContext(ExpContext.class,0);
		}
		public List<MbinderContext> mbinder() {
			return getRuleContexts(MbinderContext.class);
		}
		public MbinderContext mbinder(int i) {
			return getRuleContext(MbinderContext.class,i);
		}
		public MatchContext(ExpContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).enterMatch(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).exitMatch(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RISCTPVisitor ) return ((RISCTPVisitor<? extends T>)visitor).visitMatch(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class GreaterEqualContext extends ExpContext {
		public List<ExpContext> exp() {
			return getRuleContexts(ExpContext.class);
		}
		public ExpContext exp(int i) {
			return getRuleContext(ExpContext.class,i);
		}
		public GreaterEqualContext(ExpContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).enterGreaterEqual(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).exitGreaterEqual(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RISCTPVisitor ) return ((RISCTPVisitor<? extends T>)visitor).visitGreaterEqual(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class DecimalContext extends ExpContext {
		public DecContext dec() {
			return getRuleContext(DecContext.class,0);
		}
		public DecimalContext(ExpContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).enterDecimal(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).exitDecimal(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RISCTPVisitor ) return ((RISCTPVisitor<? extends T>)visitor).visitDecimal(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class EqualContext extends ExpContext {
		public List<ExpContext> exp() {
			return getRuleContexts(ExpContext.class);
		}
		public ExpContext exp(int i) {
			return getRuleContext(ExpContext.class,i);
		}
		public EqualContext(ExpContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).enterEqual(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).exitEqual(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RISCTPVisitor ) return ((RISCTPVisitor<? extends T>)visitor).visitEqual(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class EquivContext extends ExpContext {
		public List<ExpContext> exp() {
			return getRuleContexts(ExpContext.class);
		}
		public ExpContext exp(int i) {
			return getRuleContext(ExpContext.class,i);
		}
		public EquivContext(ExpContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).enterEquiv(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).exitEquiv(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RISCTPVisitor ) return ((RISCTPVisitor<? extends T>)visitor).visitEquiv(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class MapStoreContext extends ExpContext {
		public List<ExpContext> exp() {
			return getRuleContexts(ExpContext.class);
		}
		public ExpContext exp(int i) {
			return getRuleContext(ExpContext.class,i);
		}
		public MapStoreContext(ExpContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).enterMapStore(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).exitMapStore(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RISCTPVisitor ) return ((RISCTPVisitor<? extends T>)visitor).visitMapStore(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ForallContext extends ExpContext {
		public List<TvarContext> tvar() {
			return getRuleContexts(TvarContext.class);
		}
		public TvarContext tvar(int i) {
			return getRuleContext(TvarContext.class,i);
		}
		public ExpContext exp() {
			return getRuleContext(ExpContext.class,0);
		}
		public ForallContext(ExpContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).enterForall(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).exitForall(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RISCTPVisitor ) return ((RISCTPVisitor<? extends T>)visitor).visitForall(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class PlusContext extends ExpContext {
		public List<ExpContext> exp() {
			return getRuleContexts(ExpContext.class);
		}
		public ExpContext exp(int i) {
			return getRuleContext(ExpContext.class,i);
		}
		public PlusContext(ExpContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).enterPlus(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).exitPlus(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RISCTPVisitor ) return ((RISCTPVisitor<? extends T>)visitor).visitPlus(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ChooseUniContext extends ExpContext {
		public TvarContext tvar() {
			return getRuleContext(TvarContext.class,0);
		}
		public ExpContext exp() {
			return getRuleContext(ExpContext.class,0);
		}
		public ChooseUniContext(ExpContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).enterChooseUni(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).exitChooseUni(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RISCTPVisitor ) return ((RISCTPVisitor<? extends T>)visitor).visitChooseUni(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class LessContext extends ExpContext {
		public List<ExpContext> exp() {
			return getRuleContexts(ExpContext.class);
		}
		public ExpContext exp(int i) {
			return getRuleContext(ExpContext.class,i);
		}
		public LessContext(ExpContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).enterLess(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).exitLess(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RISCTPVisitor ) return ((RISCTPVisitor<? extends T>)visitor).visitLess(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class MapSelectContext extends ExpContext {
		public List<ExpContext> exp() {
			return getRuleContexts(ExpContext.class);
		}
		public ExpContext exp(int i) {
			return getRuleContext(ExpContext.class,i);
		}
		public MapSelectContext(ExpContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).enterMapSelect(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).exitMapSelect(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RISCTPVisitor ) return ((RISCTPVisitor<? extends T>)visitor).visitMapSelect(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ParenthesesContext extends ExpContext {
		public ExpContext exp() {
			return getRuleContext(ExpContext.class,0);
		}
		public ParenthesesContext(ExpContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).enterParentheses(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).exitParentheses(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RISCTPVisitor ) return ((RISCTPVisitor<? extends T>)visitor).visitParentheses(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ModContext extends ExpContext {
		public List<ExpContext> exp() {
			return getRuleContexts(ExpContext.class);
		}
		public ExpContext exp(int i) {
			return getRuleContext(ExpContext.class,i);
		}
		public ModContext(ExpContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).enterMod(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).exitMod(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RISCTPVisitor ) return ((RISCTPVisitor<? extends T>)visitor).visitMod(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class NotEqualContext extends ExpContext {
		public List<ExpContext> exp() {
			return getRuleContexts(ExpContext.class);
		}
		public ExpContext exp(int i) {
			return getRuleContext(ExpContext.class,i);
		}
		public NotEqualContext(ExpContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).enterNotEqual(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).exitNotEqual(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RISCTPVisitor ) return ((RISCTPVisitor<? extends T>)visitor).visitNotEqual(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class LessEqualContext extends ExpContext {
		public List<ExpContext> exp() {
			return getRuleContexts(ExpContext.class);
		}
		public ExpContext exp(int i) {
			return getRuleContext(ExpContext.class,i);
		}
		public LessEqualContext(ExpContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).enterLessEqual(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).exitLessEqual(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RISCTPVisitor ) return ((RISCTPVisitor<? extends T>)visitor).visitLessEqual(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ChooseDefContext extends ExpContext {
		public TvarContext tvar() {
			return getRuleContext(TvarContext.class,0);
		}
		public ExpContext exp() {
			return getRuleContext(ExpContext.class,0);
		}
		public ChooseDefContext(ExpContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).enterChooseDef(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).exitChooseDef(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RISCTPVisitor ) return ((RISCTPVisitor<? extends T>)visitor).visitChooseDef(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class TupleConstructContext extends ExpContext {
		public List<ExpContext> exp() {
			return getRuleContexts(ExpContext.class);
		}
		public ExpContext exp(int i) {
			return getRuleContext(ExpContext.class,i);
		}
		public TupleConstructContext(ExpContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).enterTupleConstruct(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).exitTupleConstruct(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RISCTPVisitor ) return ((RISCTPVisitor<? extends T>)visitor).visitTupleConstruct(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ImpContext extends ExpContext {
		public List<ExpContext> exp() {
			return getRuleContexts(ExpContext.class);
		}
		public ExpContext exp(int i) {
			return getRuleContext(ExpContext.class,i);
		}
		public ImpContext(ExpContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).enterImp(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).exitImp(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RISCTPVisitor ) return ((RISCTPVisitor<? extends T>)visitor).visitImp(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class DivContext extends ExpContext {
		public List<ExpContext> exp() {
			return getRuleContexts(ExpContext.class);
		}
		public ExpContext exp(int i) {
			return getRuleContext(ExpContext.class,i);
		}
		public DivContext(ExpContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).enterDiv(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).exitDiv(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RISCTPVisitor ) return ((RISCTPVisitor<? extends T>)visitor).visitDiv(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class NegContext extends ExpContext {
		public ExpContext exp() {
			return getRuleContext(ExpContext.class,0);
		}
		public NegContext(ExpContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).enterNeg(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).exitNeg(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RISCTPVisitor ) return ((RISCTPVisitor<? extends T>)visitor).visitNeg(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class NotContext extends ExpContext {
		public ExpContext exp() {
			return getRuleContext(ExpContext.class,0);
		}
		public NotContext(ExpContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).enterNot(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).exitNot(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RISCTPVisitor ) return ((RISCTPVisitor<? extends T>)visitor).visitNot(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ApplyNotGoalContext extends ExpContext {
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public List<ExpContext> exp() {
			return getRuleContexts(ExpContext.class);
		}
		public ExpContext exp(int i) {
			return getRuleContext(ExpContext.class,i);
		}
		public ApplyNotGoalContext(ExpContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).enterApplyNotGoal(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).exitApplyNotGoal(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RISCTPVisitor ) return ((RISCTPVisitor<? extends T>)visitor).visitApplyNotGoal(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class MultContext extends ExpContext {
		public List<ExpContext> exp() {
			return getRuleContexts(ExpContext.class);
		}
		public ExpContext exp(int i) {
			return getRuleContext(ExpContext.class,i);
		}
		public MultContext(ExpContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).enterMult(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).exitMult(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RISCTPVisitor ) return ((RISCTPVisitor<? extends T>)visitor).visitMult(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ChooseContext extends ExpContext {
		public TvarContext tvar() {
			return getRuleContext(TvarContext.class,0);
		}
		public ExpContext exp() {
			return getRuleContext(ExpContext.class,0);
		}
		public ChooseContext(ExpContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).enterChoose(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).exitChoose(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RISCTPVisitor ) return ((RISCTPVisitor<? extends T>)visitor).visitChoose(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ExistsContext extends ExpContext {
		public List<TvarContext> tvar() {
			return getRuleContexts(TvarContext.class);
		}
		public TvarContext tvar(int i) {
			return getRuleContext(TvarContext.class,i);
		}
		public ExpContext exp() {
			return getRuleContext(ExpContext.class,0);
		}
		public ExistsContext(ExpContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).enterExists(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).exitExists(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RISCTPVisitor ) return ((RISCTPVisitor<? extends T>)visitor).visitExists(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class AndContext extends ExpContext {
		public List<ExpContext> exp() {
			return getRuleContexts(ExpContext.class);
		}
		public ExpContext exp(int i) {
			return getRuleContext(ExpContext.class,i);
		}
		public AndContext(ExpContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).enterAnd(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).exitAnd(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RISCTPVisitor ) return ((RISCTPVisitor<? extends T>)visitor).visitAnd(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class LetContext extends ExpContext {
		public List<LbinderContext> lbinder() {
			return getRuleContexts(LbinderContext.class);
		}
		public LbinderContext lbinder(int i) {
			return getRuleContext(LbinderContext.class,i);
		}
		public ExpContext exp() {
			return getRuleContext(ExpContext.class,0);
		}
		public LetContext(ExpContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).enterLet(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).exitLet(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RISCTPVisitor ) return ((RISCTPVisitor<? extends T>)visitor).visitLet(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class GreaterContext extends ExpContext {
		public List<ExpContext> exp() {
			return getRuleContexts(ExpContext.class);
		}
		public ExpContext exp(int i) {
			return getRuleContext(ExpContext.class,i);
		}
		public GreaterContext(ExpContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).enterGreater(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).exitGreater(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RISCTPVisitor ) return ((RISCTPVisitor<? extends T>)visitor).visitGreater(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class IfThenElseContext extends ExpContext {
		public List<ExpContext> exp() {
			return getRuleContexts(ExpContext.class);
		}
		public ExpContext exp(int i) {
			return getRuleContext(ExpContext.class,i);
		}
		public IfThenElseContext(ExpContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).enterIfThenElse(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).exitIfThenElse(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RISCTPVisitor ) return ((RISCTPVisitor<? extends T>)visitor).visitIfThenElse(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class TupleStoreContext extends ExpContext {
		public List<ExpContext> exp() {
			return getRuleContexts(ExpContext.class);
		}
		public ExpContext exp(int i) {
			return getRuleContext(ExpContext.class,i);
		}
		public DecContext dec() {
			return getRuleContext(DecContext.class,0);
		}
		public TupleStoreContext(ExpContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).enterTupleStore(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).exitTupleStore(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RISCTPVisitor ) return ((RISCTPVisitor<? extends T>)visitor).visitTupleStore(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class MapConstructContext extends ExpContext {
		public List<PidContext> pid() {
			return getRuleContexts(PidContext.class);
		}
		public PidContext pid(int i) {
			return getRuleContext(PidContext.class,i);
		}
		public ExpContext exp() {
			return getRuleContext(ExpContext.class,0);
		}
		public MapConstructContext(ExpContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).enterMapConstruct(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).exitMapConstruct(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RISCTPVisitor ) return ((RISCTPVisitor<? extends T>)visitor).visitMapConstruct(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class MinusContext extends ExpContext {
		public List<ExpContext> exp() {
			return getRuleContexts(ExpContext.class);
		}
		public ExpContext exp(int i) {
			return getRuleContext(ExpContext.class,i);
		}
		public MinusContext(ExpContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).enterMinus(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).exitMinus(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RISCTPVisitor ) return ((RISCTPVisitor<? extends T>)visitor).visitMinus(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExpContext exp() throws RecognitionException {
		return exp(0);
	}

	private ExpContext exp(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		ExpContext _localctx = new ExpContext(_ctx, _parentState);
		ExpContext _prevctx = _localctx;
		int _startState = 4;
		enterRecursionRule(_localctx, 4, RULE_exp, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(298);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,30,_ctx) ) {
			case 1:
				{
				_localctx = new DecimalContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;

				setState(148);
				dec();
				}
				break;
			case 2:
				{
				_localctx = new ApplyContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(149);
				id();
				setState(162);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,20,_ctx) ) {
				case 1:
					{
					setState(150);
					match(T__6);
					setState(159);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -6988478312851963776L) != 0) || ((((_la - 65)) & ~0x3f) == 0 && ((1L << (_la - 65)) & 1797L) != 0)) {
						{
						setState(151);
						exp(0);
						setState(156);
						_errHandler.sync(this);
						_la = _input.LA(1);
						while (_la==T__7) {
							{
							{
							setState(152);
							match(T__7);
							setState(153);
							exp(0);
							}
							}
							setState(158);
							_errHandler.sync(this);
							_la = _input.LA(1);
						}
						}
					}

					setState(161);
					match(T__8);
					}
					break;
				}
				}
				break;
			case 3:
				{
				_localctx = new ApplyNotGoalContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(164);
				id();
				setState(177);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__6) {
					{
					setState(165);
					match(T__6);
					setState(174);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -6988478312851963776L) != 0) || ((((_la - 65)) & ~0x3f) == 0 && ((1L << (_la - 65)) & 1797L) != 0)) {
						{
						setState(166);
						exp(0);
						setState(171);
						_errHandler.sync(this);
						_la = _input.LA(1);
						while (_la==T__7) {
							{
							{
							setState(167);
							match(T__7);
							setState(168);
							exp(0);
							}
							}
							setState(173);
							_errHandler.sync(this);
							_la = _input.LA(1);
						}
						}
					}

					setState(176);
					match(T__8);
					}
				}

				setState(179);
				match(T__18);
				}
				break;
			case 4:
				{
				_localctx = new MapConstructContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(181);
				match(T__19);
				setState(182);
				match(T__15);
				setState(183);
				pid();
				setState(184);
				match(T__7);
				setState(185);
				pid();
				setState(186);
				match(T__16);
				setState(187);
				match(T__6);
				setState(188);
				exp(0);
				setState(189);
				match(T__8);
				}
				break;
			case 5:
				{
				_localctx = new TupleConstructContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(191);
				_la = _input.LA(1);
				if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 31457280L) != 0)) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(192);
				exp(0);
				setState(197);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__7) {
					{
					{
					setState(193);
					match(T__7);
					setState(194);
					exp(0);
					}
					}
					setState(199);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(200);
				_la = _input.LA(1);
				if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 503316480L) != 0)) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case 6:
				{
				_localctx = new NegContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(202);
				match(T__29);
				setState(203);
				exp(29);
				}
				break;
			case 7:
				{
				_localctx = new FalseContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(204);
				_la = _input.LA(1);
				if ( !(_la==T__43 || _la==T__44) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case 8:
				{
				_localctx = new TrueContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(205);
				_la = _input.LA(1);
				if ( !(_la==T__45 || _la==T__46) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case 9:
				{
				_localctx = new NotContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(206);
				_la = _input.LA(1);
				if ( !(_la==T__47 || _la==T__48) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(207);
				exp(15);
				}
				break;
			case 10:
				{
				_localctx = new ForallContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(208);
				_la = _input.LA(1);
				if ( !(_la==T__55 || _la==T__56) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(209);
				tvar();
				setState(214);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__7) {
					{
					{
					setState(210);
					match(T__7);
					setState(211);
					tvar();
					}
					}
					setState(216);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(217);
				match(T__28);
				setState(218);
				exp(10);
				}
				break;
			case 11:
				{
				_localctx = new ExistsContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(220);
				_la = _input.LA(1);
				if ( !(_la==T__57 || _la==T__58) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(221);
				tvar();
				setState(226);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__7) {
					{
					{
					setState(222);
					match(T__7);
					setState(223);
					tvar();
					}
					}
					setState(228);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(229);
				match(T__28);
				setState(230);
				exp(9);
				}
				break;
			case 12:
				{
				_localctx = new IfThenElseContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(232);
				match(T__59);
				setState(233);
				exp(0);
				setState(234);
				match(T__60);
				setState(235);
				exp(0);
				setState(236);
				match(T__61);
				setState(237);
				exp(8);
				}
				break;
			case 13:
				{
				_localctx = new MatchContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(239);
				match(T__62);
				setState(240);
				exp(0);
				setState(241);
				match(T__2);
				setState(243);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__63) {
					{
					setState(242);
					match(T__63);
					}
				}

				setState(245);
				mbinder();
				setState(250);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,28,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(246);
						match(T__63);
						setState(247);
						mbinder();
						}
						} 
					}
					setState(252);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,28,_ctx);
				}
				}
				break;
			case 14:
				{
				_localctx = new LetContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(253);
				match(T__64);
				setState(254);
				lbinder();
				setState(259);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__7) {
					{
					{
					setState(255);
					match(T__7);
					setState(256);
					lbinder();
					}
					}
					setState(261);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(262);
				match(T__65);
				setState(263);
				exp(6);
				}
				break;
			case 15:
				{
				_localctx = new ChooseContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(265);
				match(T__66);
				setState(266);
				tvar();
				setState(267);
				match(T__2);
				setState(268);
				exp(5);
				}
				break;
			case 16:
				{
				_localctx = new ChooseSatContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(270);
				match(T__66);
				setState(271);
				match(T__15);
				setState(272);
				match(T__67);
				setState(273);
				match(T__16);
				setState(274);
				tvar();
				setState(275);
				match(T__2);
				setState(276);
				exp(4);
				}
				break;
			case 17:
				{
				_localctx = new ChooseUniContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(278);
				match(T__66);
				setState(279);
				match(T__15);
				setState(280);
				match(T__68);
				setState(281);
				match(T__16);
				setState(282);
				tvar();
				setState(283);
				match(T__2);
				setState(284);
				exp(3);
				}
				break;
			case 18:
				{
				_localctx = new ChooseDefContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(286);
				match(T__66);
				setState(287);
				match(T__15);
				setState(288);
				match(T__69);
				setState(289);
				match(T__16);
				setState(290);
				tvar();
				setState(291);
				match(T__2);
				setState(292);
				exp(2);
				}
				break;
			case 19:
				{
				_localctx = new ParenthesesContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(294);
				match(T__6);
				setState(295);
				exp(0);
				setState(296);
				match(T__8);
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(370);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,32,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(368);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,31,_ctx) ) {
					case 1:
						{
						_localctx = new MapStoreContext(new ExpContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_exp);
						setState(300);
						if (!(precpred(_ctx, 31))) throw new FailedPredicateException(this, "precpred(_ctx, 31)");
						setState(301);
						match(T__2);
						setState(302);
						match(T__15);
						setState(303);
						exp(0);
						setState(304);
						match(T__16);
						setState(305);
						match(T__1);
						setState(306);
						exp(32);
						}
						break;
					case 2:
						{
						_localctx = new TupleStoreContext(new ExpContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_exp);
						setState(308);
						if (!(precpred(_ctx, 30))) throw new FailedPredicateException(this, "precpred(_ctx, 30)");
						setState(309);
						match(T__2);
						setState(310);
						match(T__28);
						setState(311);
						dec();
						setState(312);
						match(T__1);
						setState(313);
						exp(31);
						}
						break;
					case 3:
						{
						_localctx = new MultContext(new ExpContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_exp);
						setState(315);
						if (!(precpred(_ctx, 28))) throw new FailedPredicateException(this, "precpred(_ctx, 28)");
						setState(316);
						_la = _input.LA(1);
						if ( !(_la==T__30 || _la==T__31) ) {
						_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(317);
						exp(29);
						}
						break;
					case 4:
						{
						_localctx = new DivContext(new ExpContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_exp);
						setState(318);
						if (!(precpred(_ctx, 27))) throw new FailedPredicateException(this, "precpred(_ctx, 27)");
						setState(319);
						match(T__32);
						setState(320);
						exp(28);
						}
						break;
					case 5:
						{
						_localctx = new ModContext(new ExpContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_exp);
						setState(321);
						if (!(precpred(_ctx, 26))) throw new FailedPredicateException(this, "precpred(_ctx, 26)");
						setState(322);
						match(T__33);
						setState(323);
						exp(27);
						}
						break;
					case 6:
						{
						_localctx = new MinusContext(new ExpContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_exp);
						setState(324);
						if (!(precpred(_ctx, 25))) throw new FailedPredicateException(this, "precpred(_ctx, 25)");
						setState(325);
						match(T__29);
						setState(326);
						exp(26);
						}
						break;
					case 7:
						{
						_localctx = new PlusContext(new ExpContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_exp);
						setState(327);
						if (!(precpred(_ctx, 24))) throw new FailedPredicateException(this, "precpred(_ctx, 24)");
						setState(328);
						match(T__34);
						setState(329);
						exp(25);
						}
						break;
					case 8:
						{
						_localctx = new EqualContext(new ExpContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_exp);
						setState(330);
						if (!(precpred(_ctx, 23))) throw new FailedPredicateException(this, "precpred(_ctx, 23)");
						setState(331);
						match(T__1);
						setState(332);
						exp(24);
						}
						break;
					case 9:
						{
						_localctx = new NotEqualContext(new ExpContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_exp);
						setState(333);
						if (!(precpred(_ctx, 22))) throw new FailedPredicateException(this, "precpred(_ctx, 22)");
						setState(334);
						_la = _input.LA(1);
						if ( !(_la==T__35 || _la==T__36) ) {
						_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(335);
						exp(23);
						}
						break;
					case 10:
						{
						_localctx = new LessContext(new ExpContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_exp);
						setState(336);
						if (!(precpred(_ctx, 21))) throw new FailedPredicateException(this, "precpred(_ctx, 21)");
						setState(337);
						match(T__37);
						setState(338);
						exp(22);
						}
						break;
					case 11:
						{
						_localctx = new LessEqualContext(new ExpContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_exp);
						setState(339);
						if (!(precpred(_ctx, 20))) throw new FailedPredicateException(this, "precpred(_ctx, 20)");
						setState(340);
						_la = _input.LA(1);
						if ( !(_la==T__38 || _la==T__39) ) {
						_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(341);
						exp(21);
						}
						break;
					case 12:
						{
						_localctx = new GreaterContext(new ExpContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_exp);
						setState(342);
						if (!(precpred(_ctx, 19))) throw new FailedPredicateException(this, "precpred(_ctx, 19)");
						setState(343);
						match(T__40);
						setState(344);
						exp(20);
						}
						break;
					case 13:
						{
						_localctx = new GreaterEqualContext(new ExpContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_exp);
						setState(345);
						if (!(precpred(_ctx, 18))) throw new FailedPredicateException(this, "precpred(_ctx, 18)");
						setState(346);
						_la = _input.LA(1);
						if ( !(_la==T__41 || _la==T__42) ) {
						_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(347);
						exp(19);
						}
						break;
					case 14:
						{
						_localctx = new AndContext(new ExpContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_exp);
						setState(348);
						if (!(precpred(_ctx, 14))) throw new FailedPredicateException(this, "precpred(_ctx, 14)");
						setState(349);
						_la = _input.LA(1);
						if ( !(_la==T__49 || _la==T__50) ) {
						_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(350);
						exp(15);
						}
						break;
					case 15:
						{
						_localctx = new OrContext(new ExpContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_exp);
						setState(351);
						if (!(precpred(_ctx, 13))) throw new FailedPredicateException(this, "precpred(_ctx, 13)");
						setState(352);
						_la = _input.LA(1);
						if ( !(_la==T__51 || _la==T__52) ) {
						_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(353);
						exp(14);
						}
						break;
					case 16:
						{
						_localctx = new ImpContext(new ExpContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_exp);
						setState(354);
						if (!(precpred(_ctx, 12))) throw new FailedPredicateException(this, "precpred(_ctx, 12)");
						setState(355);
						_la = _input.LA(1);
						if ( !(_la==T__53 || _la==T__54) ) {
						_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(356);
						exp(13);
						}
						break;
					case 17:
						{
						_localctx = new EquivContext(new ExpContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_exp);
						setState(357);
						if (!(precpred(_ctx, 11))) throw new FailedPredicateException(this, "precpred(_ctx, 11)");
						setState(358);
						_la = _input.LA(1);
						if ( !(_la==T__12 || _la==T__13) ) {
						_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(359);
						exp(12);
						}
						break;
					case 18:
						{
						_localctx = new MapSelectContext(new ExpContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_exp);
						setState(360);
						if (!(precpred(_ctx, 33))) throw new FailedPredicateException(this, "precpred(_ctx, 33)");
						setState(361);
						match(T__15);
						setState(362);
						exp(0);
						setState(363);
						match(T__16);
						}
						break;
					case 19:
						{
						_localctx = new TupleSelectContext(new ExpContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_exp);
						setState(365);
						if (!(precpred(_ctx, 32))) throw new FailedPredicateException(this, "precpred(_ctx, 32)");
						setState(366);
						match(T__28);
						setState(367);
						dec();
						}
						break;
					}
					} 
				}
				setState(372);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,32,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TypeContext extends ParserRuleContext {
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public List<TypeContext> type() {
			return getRuleContexts(TypeContext.class);
		}
		public TypeContext type(int i) {
			return getRuleContext(TypeContext.class,i);
		}
		public TypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_type; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).enterType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).exitType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RISCTPVisitor ) return ((RISCTPVisitor<? extends T>)visitor).visitType(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeContext type() throws RecognitionException {
		TypeContext _localctx = new TypeContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_type);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(373);
			id();
			setState(386);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__15) {
				{
				setState(374);
				match(T__15);
				setState(383);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==ID || _la==QID) {
					{
					setState(375);
					type();
					setState(380);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==T__7) {
						{
						{
						setState(376);
						match(T__7);
						setState(377);
						type();
						}
						}
						setState(382);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					}
				}

				setState(385);
				match(T__16);
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TvarContext extends ParserRuleContext {
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public TvarContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tvar; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).enterTvar(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).exitTvar(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RISCTPVisitor ) return ((RISCTPVisitor<? extends T>)visitor).visitTvar(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TvarContext tvar() throws RecognitionException {
		TvarContext _localctx = new TvarContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_tvar);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(388);
			id();
			setState(389);
			match(T__9);
			setState(390);
			type();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class LbinderContext extends ParserRuleContext {
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public ExpContext exp() {
			return getRuleContext(ExpContext.class,0);
		}
		public LbinderContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_lbinder; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).enterLbinder(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).exitLbinder(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RISCTPVisitor ) return ((RISCTPVisitor<? extends T>)visitor).visitLbinder(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LbinderContext lbinder() throws RecognitionException {
		LbinderContext _localctx = new LbinderContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_lbinder);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(392);
			id();
			setState(393);
			match(T__1);
			setState(394);
			exp(0);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class MbinderContext extends ParserRuleContext {
		public PatternContext pattern() {
			return getRuleContext(PatternContext.class,0);
		}
		public ExpContext exp() {
			return getRuleContext(ExpContext.class,0);
		}
		public MbinderContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_mbinder; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).enterMbinder(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).exitMbinder(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RISCTPVisitor ) return ((RISCTPVisitor<? extends T>)visitor).visitMbinder(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MbinderContext mbinder() throws RecognitionException {
		MbinderContext _localctx = new MbinderContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_mbinder);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(396);
			pattern();
			setState(397);
			match(T__70);
			setState(398);
			exp(0);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class PatternContext extends ParserRuleContext {
		public PatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_pattern; }
	 
		public PatternContext() { }
		public void copyFrom(PatternContext ctx) {
			super.copyFrom(ctx);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class DefaultPatternContext extends PatternContext {
		public DefaultPatternContext(PatternContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).enterDefaultPattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).exitDefaultPattern(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RISCTPVisitor ) return ((RISCTPVisitor<? extends T>)visitor).visitDefaultPattern(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ConstrPatternContext extends PatternContext {
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public List<TvarContext> tvar() {
			return getRuleContexts(TvarContext.class);
		}
		public TvarContext tvar(int i) {
			return getRuleContext(TvarContext.class,i);
		}
		public ConstrPatternContext(PatternContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).enterConstrPattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).exitConstrPattern(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RISCTPVisitor ) return ((RISCTPVisitor<? extends T>)visitor).visitConstrPattern(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PatternContext pattern() throws RecognitionException {
		PatternContext _localctx = new PatternContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_pattern);
		int _la;
		try {
			setState(416);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__71:
				_localctx = new DefaultPatternContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(400);
				match(T__71);
				}
				break;
			case ID:
			case QID:
				_localctx = new ConstrPatternContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(401);
				id();
				setState(414);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__6) {
					{
					setState(402);
					match(T__6);
					setState(411);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==ID || _la==QID) {
						{
						setState(403);
						tvar();
						setState(408);
						_errHandler.sync(this);
						_la = _input.LA(1);
						while (_la==T__7) {
							{
							{
							setState(404);
							match(T__7);
							setState(405);
							tvar();
							}
							}
							setState(410);
							_errHandler.sync(this);
							_la = _input.LA(1);
						}
						}
					}

					setState(413);
					match(T__8);
					}
				}

				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DtitemContext extends ParserRuleContext {
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public List<DtconstrContext> dtconstr() {
			return getRuleContexts(DtconstrContext.class);
		}
		public DtconstrContext dtconstr(int i) {
			return getRuleContext(DtconstrContext.class,i);
		}
		public ExpContext exp() {
			return getRuleContext(ExpContext.class,0);
		}
		public DtitemContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_dtitem; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).enterDtitem(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).exitDtitem(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RISCTPVisitor ) return ((RISCTPVisitor<? extends T>)visitor).visitDtitem(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DtitemContext dtitem() throws RecognitionException {
		DtitemContext _localctx = new DtitemContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_dtitem);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(418);
			id();
			setState(419);
			match(T__1);
			setState(420);
			dtconstr();
			setState(425);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__63) {
				{
				{
				setState(421);
				match(T__63);
				setState(422);
				dtconstr();
				}
				}
				setState(427);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(430);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__2) {
				{
				setState(428);
				match(T__2);
				setState(429);
				exp(0);
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DtconstrContext extends ParserRuleContext {
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public List<TvarContext> tvar() {
			return getRuleContexts(TvarContext.class);
		}
		public TvarContext tvar(int i) {
			return getRuleContext(TvarContext.class,i);
		}
		public DtconstrContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_dtconstr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).enterDtconstr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).exitDtconstr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RISCTPVisitor ) return ((RISCTPVisitor<? extends T>)visitor).visitDtconstr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DtconstrContext dtconstr() throws RecognitionException {
		DtconstrContext _localctx = new DtconstrContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_dtconstr);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(432);
			id();
			setState(445);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__6) {
				{
				setState(433);
				match(T__6);
				setState(442);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==ID || _la==QID) {
					{
					setState(434);
					tvar();
					setState(439);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==T__7) {
						{
						{
						setState(435);
						match(T__7);
						setState(436);
						tvar();
						}
						}
						setState(441);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					}
				}

				setState(444);
				match(T__8);
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class IdContext extends ParserRuleContext {
		public IdContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_id; }
	 
		public IdContext() { }
		public void copyFrom(IdContext ctx) {
			super.copyFrom(ctx);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class PlainIdContext extends IdContext {
		public TerminalNode ID() { return getToken(RISCTPParser.ID, 0); }
		public PlainIdContext(IdContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).enterPlainId(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).exitPlainId(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RISCTPVisitor ) return ((RISCTPVisitor<? extends T>)visitor).visitPlainId(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class QuotedIdContext extends IdContext {
		public TerminalNode QID() { return getToken(RISCTPParser.QID, 0); }
		public QuotedIdContext(IdContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).enterQuotedId(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).exitQuotedId(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RISCTPVisitor ) return ((RISCTPVisitor<? extends T>)visitor).visitQuotedId(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IdContext id() throws RecognitionException {
		IdContext _localctx = new IdContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_id);
		try {
			setState(449);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ID:
				_localctx = new PlainIdContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(447);
				match(ID);
				}
				break;
			case QID:
				_localctx = new QuotedIdContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(448);
				match(QID);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class PidContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(RISCTPParser.ID, 0); }
		public PidContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_pid; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).enterPid(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).exitPid(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RISCTPVisitor ) return ((RISCTPVisitor<? extends T>)visitor).visitPid(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PidContext pid() throws RecognitionException {
		PidContext _localctx = new PidContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_pid);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(451);
			match(ID);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DecContext extends ParserRuleContext {
		public TerminalNode DEC() { return getToken(RISCTPParser.DEC, 0); }
		public DecContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_dec; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).enterDec(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RISCTPListener ) ((RISCTPListener)listener).exitDec(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RISCTPVisitor ) return ((RISCTPVisitor<? extends T>)visitor).visitDec(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DecContext dec() throws RecognitionException {
		DecContext _localctx = new DecContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_dec);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(453);
			match(DEC);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 2:
			return exp_sempred((ExpContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean exp_sempred(ExpContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0:
			return precpred(_ctx, 31);
		case 1:
			return precpred(_ctx, 30);
		case 2:
			return precpred(_ctx, 28);
		case 3:
			return precpred(_ctx, 27);
		case 4:
			return precpred(_ctx, 26);
		case 5:
			return precpred(_ctx, 25);
		case 6:
			return precpred(_ctx, 24);
		case 7:
			return precpred(_ctx, 23);
		case 8:
			return precpred(_ctx, 22);
		case 9:
			return precpred(_ctx, 21);
		case 10:
			return precpred(_ctx, 20);
		case 11:
			return precpred(_ctx, 19);
		case 12:
			return precpred(_ctx, 18);
		case 13:
			return precpred(_ctx, 14);
		case 14:
			return precpred(_ctx, 13);
		case 15:
			return precpred(_ctx, 12);
		case 16:
			return precpred(_ctx, 11);
		case 17:
			return precpred(_ctx, 33);
		case 18:
			return precpred(_ctx, 32);
		}
		return true;
	}

	public static final String _serializedATN =
		"\u0004\u0001P\u01c8\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
		"\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004\u0002"+
		"\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007\u0002"+
		"\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002\u000b\u0007\u000b\u0002"+
		"\f\u0007\f\u0001\u0000\u0005\u0000\u001c\b\u0000\n\u0000\f\u0000\u001f"+
		"\t\u0000\u0001\u0000\u0001\u0000\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0003\u0001)\b\u0001\u0003\u0001+\b\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0005\u00013\b\u0001\n\u0001\f\u00016\t\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0005\u0001@\b\u0001\n\u0001\f\u0001C\t\u0001\u0003\u0001E\b\u0001\u0001"+
		"\u0001\u0003\u0001H\b\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0003\u0001N\b\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0003\u0001X\b"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0005\u0001b\b\u0001\n\u0001\f\u0001e\t"+
		"\u0001\u0003\u0001g\b\u0001\u0001\u0001\u0003\u0001j\b\u0001\u0001\u0001"+
		"\u0001\u0001\u0003\u0001n\b\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0005\u0001y\b\u0001\n\u0001\f\u0001|\t\u0001\u0003\u0001~\b\u0001\u0001"+
		"\u0001\u0003\u0001\u0081\b\u0001\u0001\u0001\u0001\u0001\u0003\u0001\u0085"+
		"\b\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0003"+
		"\u0001\u0092\b\u0001\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001"+
		"\u0002\u0001\u0002\u0001\u0002\u0005\u0002\u009b\b\u0002\n\u0002\f\u0002"+
		"\u009e\t\u0002\u0003\u0002\u00a0\b\u0002\u0001\u0002\u0003\u0002\u00a3"+
		"\b\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0005"+
		"\u0002\u00aa\b\u0002\n\u0002\f\u0002\u00ad\t\u0002\u0003\u0002\u00af\b"+
		"\u0002\u0001\u0002\u0003\u0002\u00b2\b\u0002\u0001\u0002\u0001\u0002\u0001"+
		"\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001"+
		"\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001"+
		"\u0002\u0001\u0002\u0005\u0002\u00c4\b\u0002\n\u0002\f\u0002\u00c7\t\u0002"+
		"\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002"+
		"\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002"+
		"\u0005\u0002\u00d5\b\u0002\n\u0002\f\u0002\u00d8\t\u0002\u0001\u0002\u0001"+
		"\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0005"+
		"\u0002\u00e1\b\u0002\n\u0002\f\u0002\u00e4\t\u0002\u0001\u0002\u0001\u0002"+
		"\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002"+
		"\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002"+
		"\u0003\u0002\u00f4\b\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0005\u0002"+
		"\u00f9\b\u0002\n\u0002\f\u0002\u00fc\t\u0002\u0001\u0002\u0001\u0002\u0001"+
		"\u0002\u0001\u0002\u0005\u0002\u0102\b\u0002\n\u0002\f\u0002\u0105\t\u0002"+
		"\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002"+
		"\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002"+
		"\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002"+
		"\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002"+
		"\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002"+
		"\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002"+
		"\u0003\u0002\u012b\b\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002"+
		"\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002"+
		"\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002"+
		"\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002"+
		"\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002"+
		"\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002"+
		"\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002"+
		"\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002"+
		"\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002"+
		"\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002"+
		"\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002"+
		"\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0005\u0002\u0171\b\u0002"+
		"\n\u0002\f\u0002\u0174\t\u0002\u0001\u0003\u0001\u0003\u0001\u0003\u0001"+
		"\u0003\u0001\u0003\u0005\u0003\u017b\b\u0003\n\u0003\f\u0003\u017e\t\u0003"+
		"\u0003\u0003\u0180\b\u0003\u0001\u0003\u0003\u0003\u0183\b\u0003\u0001"+
		"\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0005\u0001\u0005\u0001"+
		"\u0005\u0001\u0005\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001"+
		"\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0005"+
		"\u0007\u0197\b\u0007\n\u0007\f\u0007\u019a\t\u0007\u0003\u0007\u019c\b"+
		"\u0007\u0001\u0007\u0003\u0007\u019f\b\u0007\u0003\u0007\u01a1\b\u0007"+
		"\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0005\b\u01a8\b\b\n\b\f\b\u01ab"+
		"\t\b\u0001\b\u0001\b\u0003\b\u01af\b\b\u0001\t\u0001\t\u0001\t\u0001\t"+
		"\u0001\t\u0005\t\u01b6\b\t\n\t\f\t\u01b9\t\t\u0003\t\u01bb\b\t\u0001\t"+
		"\u0003\t\u01be\b\t\u0001\n\u0001\n\u0003\n\u01c2\b\n\u0001\u000b\u0001"+
		"\u000b\u0001\f\u0001\f\u0001\f\u0000\u0001\u0004\r\u0000\u0002\u0004\u0006"+
		"\b\n\f\u000e\u0010\u0012\u0014\u0016\u0018\u0000\u000f\u0001\u0000\r\u000e"+
		"\u0001\u0000\u0015\u0018\u0001\u0000\u0019\u001c\u0001\u0000,-\u0001\u0000"+
		"./\u0001\u000001\u0001\u000089\u0001\u0000:;\u0001\u0000\u001f \u0001"+
		"\u0000$%\u0001\u0000\'(\u0001\u0000*+\u0001\u000023\u0001\u000045\u0001"+
		"\u000067\u020f\u0000\u001d\u0001\u0000\u0000\u0000\u0002\u0091\u0001\u0000"+
		"\u0000\u0000\u0004\u012a\u0001\u0000\u0000\u0000\u0006\u0175\u0001\u0000"+
		"\u0000\u0000\b\u0184\u0001\u0000\u0000\u0000\n\u0188\u0001\u0000\u0000"+
		"\u0000\f\u018c\u0001\u0000\u0000\u0000\u000e\u01a0\u0001\u0000\u0000\u0000"+
		"\u0010\u01a2\u0001\u0000\u0000\u0000\u0012\u01b0\u0001\u0000\u0000\u0000"+
		"\u0014\u01c1\u0001\u0000\u0000\u0000\u0016\u01c3\u0001\u0000\u0000\u0000"+
		"\u0018\u01c5\u0001\u0000\u0000\u0000\u001a\u001c\u0003\u0002\u0001\u0000"+
		"\u001b\u001a\u0001\u0000\u0000\u0000\u001c\u001f\u0001\u0000\u0000\u0000"+
		"\u001d\u001b\u0001\u0000\u0000\u0000\u001d\u001e\u0001\u0000\u0000\u0000"+
		"\u001e \u0001\u0000\u0000\u0000\u001f\u001d\u0001\u0000\u0000\u0000 !"+
		"\u0005\u0000\u0000\u0001!\u0001\u0001\u0000\u0000\u0000\"#\u0005\u0001"+
		"\u0000\u0000#*\u0003\u0014\n\u0000$%\u0005\u0002\u0000\u0000%(\u0003\u0006"+
		"\u0003\u0000&\'\u0005\u0003\u0000\u0000\')\u0003\u0004\u0002\u0000(&\u0001"+
		"\u0000\u0000\u0000()\u0001\u0000\u0000\u0000)+\u0001\u0000\u0000\u0000"+
		"*$\u0001\u0000\u0000\u0000*+\u0001\u0000\u0000\u0000+,\u0001\u0000\u0000"+
		"\u0000,-\u0005L\u0000\u0000-\u0092\u0001\u0000\u0000\u0000./\u0005\u0004"+
		"\u0000\u0000/4\u0003\u0010\b\u000001\u0005\u0005\u0000\u000013\u0003\u0010"+
		"\b\u000020\u0001\u0000\u0000\u000036\u0001\u0000\u0000\u000042\u0001\u0000"+
		"\u0000\u000045\u0001\u0000\u0000\u000057\u0001\u0000\u0000\u000064\u0001"+
		"\u0000\u0000\u000078\u0005L\u0000\u00008\u0092\u0001\u0000\u0000\u0000"+
		"9:\u0005\u0006\u0000\u0000:G\u0003\u0014\n\u0000;D\u0005\u0007\u0000\u0000"+
		"<A\u0003\b\u0004\u0000=>\u0005\b\u0000\u0000>@\u0003\b\u0004\u0000?=\u0001"+
		"\u0000\u0000\u0000@C\u0001\u0000\u0000\u0000A?\u0001\u0000\u0000\u0000"+
		"AB\u0001\u0000\u0000\u0000BE\u0001\u0000\u0000\u0000CA\u0001\u0000\u0000"+
		"\u0000D<\u0001\u0000\u0000\u0000DE\u0001\u0000\u0000\u0000EF\u0001\u0000"+
		"\u0000\u0000FH\u0005\t\u0000\u0000G;\u0001\u0000\u0000\u0000GH\u0001\u0000"+
		"\u0000\u0000HI\u0001\u0000\u0000\u0000IJ\u0005\n\u0000\u0000JM\u0003\u0006"+
		"\u0003\u0000KL\u0005\u0002\u0000\u0000LN\u0003\u0004\u0002\u0000MK\u0001"+
		"\u0000\u0000\u0000MN\u0001\u0000\u0000\u0000NO\u0001\u0000\u0000\u0000"+
		"OP\u0005L\u0000\u0000P\u0092\u0001\u0000\u0000\u0000QR\u0005\u000b\u0000"+
		"\u0000RS\u0003\u0014\n\u0000ST\u0005\n\u0000\u0000TW\u0003\u0006\u0003"+
		"\u0000UV\u0005\u0002\u0000\u0000VX\u0003\u0004\u0002\u0000WU\u0001\u0000"+
		"\u0000\u0000WX\u0001\u0000\u0000\u0000XY\u0001\u0000\u0000\u0000YZ\u0005"+
		"L\u0000\u0000Z\u0092\u0001\u0000\u0000\u0000[\\\u0005\f\u0000\u0000\\"+
		"i\u0003\u0014\n\u0000]f\u0005\u0007\u0000\u0000^c\u0003\b\u0004\u0000"+
		"_`\u0005\b\u0000\u0000`b\u0003\b\u0004\u0000a_\u0001\u0000\u0000\u0000"+
		"be\u0001\u0000\u0000\u0000ca\u0001\u0000\u0000\u0000cd\u0001\u0000\u0000"+
		"\u0000dg\u0001\u0000\u0000\u0000ec\u0001\u0000\u0000\u0000f^\u0001\u0000"+
		"\u0000\u0000fg\u0001\u0000\u0000\u0000gh\u0001\u0000\u0000\u0000hj\u0005"+
		"\t\u0000\u0000i]\u0001\u0000\u0000\u0000ij\u0001\u0000\u0000\u0000jm\u0001"+
		"\u0000\u0000\u0000kl\u0007\u0000\u0000\u0000ln\u0003\u0004\u0002\u0000"+
		"mk\u0001\u0000\u0000\u0000mn\u0001\u0000\u0000\u0000no\u0001\u0000\u0000"+
		"\u0000op\u0005L\u0000\u0000p\u0092\u0001\u0000\u0000\u0000q\u0084\u0005"+
		"\u000f\u0000\u0000rs\u0005\u0010\u0000\u0000s\u0080\u0003\u0014\n\u0000"+
		"t}\u0005\u0007\u0000\u0000uz\u0003\u0006\u0003\u0000vw\u0005\b\u0000\u0000"+
		"wy\u0003\u0006\u0003\u0000xv\u0001\u0000\u0000\u0000y|\u0001\u0000\u0000"+
		"\u0000zx\u0001\u0000\u0000\u0000z{\u0001\u0000\u0000\u0000{~\u0001\u0000"+
		"\u0000\u0000|z\u0001\u0000\u0000\u0000}u\u0001\u0000\u0000\u0000}~\u0001"+
		"\u0000\u0000\u0000~\u007f\u0001\u0000\u0000\u0000\u007f\u0081\u0005\t"+
		"\u0000\u0000\u0080t\u0001\u0000\u0000\u0000\u0080\u0081\u0001\u0000\u0000"+
		"\u0000\u0081\u0082\u0001\u0000\u0000\u0000\u0082\u0083\u0005\u0011\u0000"+
		"\u0000\u0083\u0085\u0001\u0000\u0000\u0000\u0084r\u0001\u0000\u0000\u0000"+
		"\u0084\u0085\u0001\u0000\u0000\u0000\u0085\u0086\u0001\u0000\u0000\u0000"+
		"\u0086\u0087\u0003\u0014\n\u0000\u0087\u0088\u0007\u0000\u0000\u0000\u0088"+
		"\u0089\u0003\u0004\u0002\u0000\u0089\u008a\u0005L\u0000\u0000\u008a\u0092"+
		"\u0001\u0000\u0000\u0000\u008b\u008c\u0005\u0012\u0000\u0000\u008c\u008d"+
		"\u0003\u0014\n\u0000\u008d\u008e\u0007\u0000\u0000\u0000\u008e\u008f\u0003"+
		"\u0004\u0002\u0000\u008f\u0090\u0005L\u0000\u0000\u0090\u0092\u0001\u0000"+
		"\u0000\u0000\u0091\"\u0001\u0000\u0000\u0000\u0091.\u0001\u0000\u0000"+
		"\u0000\u00919\u0001\u0000\u0000\u0000\u0091Q\u0001\u0000\u0000\u0000\u0091"+
		"[\u0001\u0000\u0000\u0000\u0091q\u0001\u0000\u0000\u0000\u0091\u008b\u0001"+
		"\u0000\u0000\u0000\u0092\u0003\u0001\u0000\u0000\u0000\u0093\u0094\u0006"+
		"\u0002\uffff\uffff\u0000\u0094\u012b\u0003\u0018\f\u0000\u0095\u00a2\u0003"+
		"\u0014\n\u0000\u0096\u009f\u0005\u0007\u0000\u0000\u0097\u009c\u0003\u0004"+
		"\u0002\u0000\u0098\u0099\u0005\b\u0000\u0000\u0099\u009b\u0003\u0004\u0002"+
		"\u0000\u009a\u0098\u0001\u0000\u0000\u0000\u009b\u009e\u0001\u0000\u0000"+
		"\u0000\u009c\u009a\u0001\u0000\u0000\u0000\u009c\u009d\u0001\u0000\u0000"+
		"\u0000\u009d\u00a0\u0001\u0000\u0000\u0000\u009e\u009c\u0001\u0000\u0000"+
		"\u0000\u009f\u0097\u0001\u0000\u0000\u0000\u009f\u00a0\u0001\u0000\u0000"+
		"\u0000\u00a0\u00a1\u0001\u0000\u0000\u0000\u00a1\u00a3\u0005\t\u0000\u0000"+
		"\u00a2\u0096\u0001\u0000\u0000\u0000\u00a2\u00a3\u0001\u0000\u0000\u0000"+
		"\u00a3\u012b\u0001\u0000\u0000\u0000\u00a4\u00b1\u0003\u0014\n\u0000\u00a5"+
		"\u00ae\u0005\u0007\u0000\u0000\u00a6\u00ab\u0003\u0004\u0002\u0000\u00a7"+
		"\u00a8\u0005\b\u0000\u0000\u00a8\u00aa\u0003\u0004\u0002\u0000\u00a9\u00a7"+
		"\u0001\u0000\u0000\u0000\u00aa\u00ad\u0001\u0000\u0000\u0000\u00ab\u00a9"+
		"\u0001\u0000\u0000\u0000\u00ab\u00ac\u0001\u0000\u0000\u0000\u00ac\u00af"+
		"\u0001\u0000\u0000\u0000\u00ad\u00ab\u0001\u0000\u0000\u0000\u00ae\u00a6"+
		"\u0001\u0000\u0000\u0000\u00ae\u00af\u0001\u0000\u0000\u0000\u00af\u00b0"+
		"\u0001\u0000\u0000\u0000\u00b0\u00b2\u0005\t\u0000\u0000\u00b1\u00a5\u0001"+
		"\u0000\u0000\u0000\u00b1\u00b2\u0001\u0000\u0000\u0000\u00b2\u00b3\u0001"+
		"\u0000\u0000\u0000\u00b3\u00b4\u0005\u0013\u0000\u0000\u00b4\u012b\u0001"+
		"\u0000\u0000\u0000\u00b5\u00b6\u0005\u0014\u0000\u0000\u00b6\u00b7\u0005"+
		"\u0010\u0000\u0000\u00b7\u00b8\u0003\u0016\u000b\u0000\u00b8\u00b9\u0005"+
		"\b\u0000\u0000\u00b9\u00ba\u0003\u0016\u000b\u0000\u00ba\u00bb\u0005\u0011"+
		"\u0000\u0000\u00bb\u00bc\u0005\u0007\u0000\u0000\u00bc\u00bd\u0003\u0004"+
		"\u0002\u0000\u00bd\u00be\u0005\t\u0000\u0000\u00be\u012b\u0001\u0000\u0000"+
		"\u0000\u00bf\u00c0\u0007\u0001\u0000\u0000\u00c0\u00c5\u0003\u0004\u0002"+
		"\u0000\u00c1\u00c2\u0005\b\u0000\u0000\u00c2\u00c4\u0003\u0004\u0002\u0000"+
		"\u00c3\u00c1\u0001\u0000\u0000\u0000\u00c4\u00c7\u0001\u0000\u0000\u0000"+
		"\u00c5\u00c3\u0001\u0000\u0000\u0000\u00c5\u00c6\u0001\u0000\u0000\u0000"+
		"\u00c6\u00c8\u0001\u0000\u0000\u0000\u00c7\u00c5\u0001\u0000\u0000\u0000"+
		"\u00c8\u00c9\u0007\u0002\u0000\u0000\u00c9\u012b\u0001\u0000\u0000\u0000"+
		"\u00ca\u00cb\u0005\u001e\u0000\u0000\u00cb\u012b\u0003\u0004\u0002\u001d"+
		"\u00cc\u012b\u0007\u0003\u0000\u0000\u00cd\u012b\u0007\u0004\u0000\u0000"+
		"\u00ce\u00cf\u0007\u0005\u0000\u0000\u00cf\u012b\u0003\u0004\u0002\u000f"+
		"\u00d0\u00d1\u0007\u0006\u0000\u0000\u00d1\u00d6\u0003\b\u0004\u0000\u00d2"+
		"\u00d3\u0005\b\u0000\u0000\u00d3\u00d5\u0003\b\u0004\u0000\u00d4\u00d2"+
		"\u0001\u0000\u0000\u0000\u00d5\u00d8\u0001\u0000\u0000\u0000\u00d6\u00d4"+
		"\u0001\u0000\u0000\u0000\u00d6\u00d7\u0001\u0000\u0000\u0000\u00d7\u00d9"+
		"\u0001\u0000\u0000\u0000\u00d8\u00d6\u0001\u0000\u0000\u0000\u00d9\u00da"+
		"\u0005\u001d\u0000\u0000\u00da\u00db\u0003\u0004\u0002\n\u00db\u012b\u0001"+
		"\u0000\u0000\u0000\u00dc\u00dd\u0007\u0007\u0000\u0000\u00dd\u00e2\u0003"+
		"\b\u0004\u0000\u00de\u00df\u0005\b\u0000\u0000\u00df\u00e1\u0003\b\u0004"+
		"\u0000\u00e0\u00de\u0001\u0000\u0000\u0000\u00e1\u00e4\u0001\u0000\u0000"+
		"\u0000\u00e2\u00e0\u0001\u0000\u0000\u0000\u00e2\u00e3\u0001\u0000\u0000"+
		"\u0000\u00e3\u00e5\u0001\u0000\u0000\u0000\u00e4\u00e2\u0001\u0000\u0000"+
		"\u0000\u00e5\u00e6\u0005\u001d\u0000\u0000\u00e6\u00e7\u0003\u0004\u0002"+
		"\t\u00e7\u012b\u0001\u0000\u0000\u0000\u00e8\u00e9\u0005<\u0000\u0000"+
		"\u00e9\u00ea\u0003\u0004\u0002\u0000\u00ea\u00eb\u0005=\u0000\u0000\u00eb"+
		"\u00ec\u0003\u0004\u0002\u0000\u00ec\u00ed\u0005>\u0000\u0000\u00ed\u00ee"+
		"\u0003\u0004\u0002\b\u00ee\u012b\u0001\u0000\u0000\u0000\u00ef\u00f0\u0005"+
		"?\u0000\u0000\u00f0\u00f1\u0003\u0004\u0002\u0000\u00f1\u00f3\u0005\u0003"+
		"\u0000\u0000\u00f2\u00f4\u0005@\u0000\u0000\u00f3\u00f2\u0001\u0000\u0000"+
		"\u0000\u00f3\u00f4\u0001\u0000\u0000\u0000\u00f4\u00f5\u0001\u0000\u0000"+
		"\u0000\u00f5\u00fa\u0003\f\u0006\u0000\u00f6\u00f7\u0005@\u0000\u0000"+
		"\u00f7\u00f9\u0003\f\u0006\u0000\u00f8\u00f6\u0001\u0000\u0000\u0000\u00f9"+
		"\u00fc\u0001\u0000\u0000\u0000\u00fa\u00f8\u0001\u0000\u0000\u0000\u00fa"+
		"\u00fb\u0001\u0000\u0000\u0000\u00fb\u012b\u0001\u0000\u0000\u0000\u00fc"+
		"\u00fa\u0001\u0000\u0000\u0000\u00fd\u00fe\u0005A\u0000\u0000\u00fe\u0103"+
		"\u0003\n\u0005\u0000\u00ff\u0100\u0005\b\u0000\u0000\u0100\u0102\u0003"+
		"\n\u0005\u0000\u0101\u00ff\u0001\u0000\u0000\u0000\u0102\u0105\u0001\u0000"+
		"\u0000\u0000\u0103\u0101\u0001\u0000\u0000\u0000\u0103\u0104\u0001\u0000"+
		"\u0000\u0000\u0104\u0106\u0001\u0000\u0000\u0000\u0105\u0103\u0001\u0000"+
		"\u0000\u0000\u0106\u0107\u0005B\u0000\u0000\u0107\u0108\u0003\u0004\u0002"+
		"\u0006\u0108\u012b\u0001\u0000\u0000\u0000\u0109\u010a\u0005C\u0000\u0000"+
		"\u010a\u010b\u0003\b\u0004\u0000\u010b\u010c\u0005\u0003\u0000\u0000\u010c"+
		"\u010d\u0003\u0004\u0002\u0005\u010d\u012b\u0001\u0000\u0000\u0000\u010e"+
		"\u010f\u0005C\u0000\u0000\u010f\u0110\u0005\u0010\u0000\u0000\u0110\u0111"+
		"\u0005D\u0000\u0000\u0111\u0112\u0005\u0011\u0000\u0000\u0112\u0113\u0003"+
		"\b\u0004\u0000\u0113\u0114\u0005\u0003\u0000\u0000\u0114\u0115\u0003\u0004"+
		"\u0002\u0004\u0115\u012b\u0001\u0000\u0000\u0000\u0116\u0117\u0005C\u0000"+
		"\u0000\u0117\u0118\u0005\u0010\u0000\u0000\u0118\u0119\u0005E\u0000\u0000"+
		"\u0119\u011a\u0005\u0011\u0000\u0000\u011a\u011b\u0003\b\u0004\u0000\u011b"+
		"\u011c\u0005\u0003\u0000\u0000\u011c\u011d\u0003\u0004\u0002\u0003\u011d"+
		"\u012b\u0001\u0000\u0000\u0000\u011e\u011f\u0005C\u0000\u0000\u011f\u0120"+
		"\u0005\u0010\u0000\u0000\u0120\u0121\u0005F\u0000\u0000\u0121\u0122\u0005"+
		"\u0011\u0000\u0000\u0122\u0123\u0003\b\u0004\u0000\u0123\u0124\u0005\u0003"+
		"\u0000\u0000\u0124\u0125\u0003\u0004\u0002\u0002\u0125\u012b\u0001\u0000"+
		"\u0000\u0000\u0126\u0127\u0005\u0007\u0000\u0000\u0127\u0128\u0003\u0004"+
		"\u0002\u0000\u0128\u0129\u0005\t\u0000\u0000\u0129\u012b\u0001\u0000\u0000"+
		"\u0000\u012a\u0093\u0001\u0000\u0000\u0000\u012a\u0095\u0001\u0000\u0000"+
		"\u0000\u012a\u00a4\u0001\u0000\u0000\u0000\u012a\u00b5\u0001\u0000\u0000"+
		"\u0000\u012a\u00bf\u0001\u0000\u0000\u0000\u012a\u00ca\u0001\u0000\u0000"+
		"\u0000\u012a\u00cc\u0001\u0000\u0000\u0000\u012a\u00cd\u0001\u0000\u0000"+
		"\u0000\u012a\u00ce\u0001\u0000\u0000\u0000\u012a\u00d0\u0001\u0000\u0000"+
		"\u0000\u012a\u00dc\u0001\u0000\u0000\u0000\u012a\u00e8\u0001\u0000\u0000"+
		"\u0000\u012a\u00ef\u0001\u0000\u0000\u0000\u012a\u00fd\u0001\u0000\u0000"+
		"\u0000\u012a\u0109\u0001\u0000\u0000\u0000\u012a\u010e\u0001\u0000\u0000"+
		"\u0000\u012a\u0116\u0001\u0000\u0000\u0000\u012a\u011e\u0001\u0000\u0000"+
		"\u0000\u012a\u0126\u0001\u0000\u0000\u0000\u012b\u0172\u0001\u0000\u0000"+
		"\u0000\u012c\u012d\n\u001f\u0000\u0000\u012d\u012e\u0005\u0003\u0000\u0000"+
		"\u012e\u012f\u0005\u0010\u0000\u0000\u012f\u0130\u0003\u0004\u0002\u0000"+
		"\u0130\u0131\u0005\u0011\u0000\u0000\u0131\u0132\u0005\u0002\u0000\u0000"+
		"\u0132\u0133\u0003\u0004\u0002 \u0133\u0171\u0001\u0000\u0000\u0000\u0134"+
		"\u0135\n\u001e\u0000\u0000\u0135\u0136\u0005\u0003\u0000\u0000\u0136\u0137"+
		"\u0005\u001d\u0000\u0000\u0137\u0138\u0003\u0018\f\u0000\u0138\u0139\u0005"+
		"\u0002\u0000\u0000\u0139\u013a\u0003\u0004\u0002\u001f\u013a\u0171\u0001"+
		"\u0000\u0000\u0000\u013b\u013c\n\u001c\u0000\u0000\u013c\u013d\u0007\b"+
		"\u0000\u0000\u013d\u0171\u0003\u0004\u0002\u001d\u013e\u013f\n\u001b\u0000"+
		"\u0000\u013f\u0140\u0005!\u0000\u0000\u0140\u0171\u0003\u0004\u0002\u001c"+
		"\u0141\u0142\n\u001a\u0000\u0000\u0142\u0143\u0005\"\u0000\u0000\u0143"+
		"\u0171\u0003\u0004\u0002\u001b\u0144\u0145\n\u0019\u0000\u0000\u0145\u0146"+
		"\u0005\u001e\u0000\u0000\u0146\u0171\u0003\u0004\u0002\u001a\u0147\u0148"+
		"\n\u0018\u0000\u0000\u0148\u0149\u0005#\u0000\u0000\u0149\u0171\u0003"+
		"\u0004\u0002\u0019\u014a\u014b\n\u0017\u0000\u0000\u014b\u014c\u0005\u0002"+
		"\u0000\u0000\u014c\u0171\u0003\u0004\u0002\u0018\u014d\u014e\n\u0016\u0000"+
		"\u0000\u014e\u014f\u0007\t\u0000\u0000\u014f\u0171\u0003\u0004\u0002\u0017"+
		"\u0150\u0151\n\u0015\u0000\u0000\u0151\u0152\u0005&\u0000\u0000\u0152"+
		"\u0171\u0003\u0004\u0002\u0016\u0153\u0154\n\u0014\u0000\u0000\u0154\u0155"+
		"\u0007\n\u0000\u0000\u0155\u0171\u0003\u0004\u0002\u0015\u0156\u0157\n"+
		"\u0013\u0000\u0000\u0157\u0158\u0005)\u0000\u0000\u0158\u0171\u0003\u0004"+
		"\u0002\u0014\u0159\u015a\n\u0012\u0000\u0000\u015a\u015b\u0007\u000b\u0000"+
		"\u0000\u015b\u0171\u0003\u0004\u0002\u0013\u015c\u015d\n\u000e\u0000\u0000"+
		"\u015d\u015e\u0007\f\u0000\u0000\u015e\u0171\u0003\u0004\u0002\u000f\u015f"+
		"\u0160\n\r\u0000\u0000\u0160\u0161\u0007\r\u0000\u0000\u0161\u0171\u0003"+
		"\u0004\u0002\u000e\u0162\u0163\n\f\u0000\u0000\u0163\u0164\u0007\u000e"+
		"\u0000\u0000\u0164\u0171\u0003\u0004\u0002\r\u0165\u0166\n\u000b\u0000"+
		"\u0000\u0166\u0167\u0007\u0000\u0000\u0000\u0167\u0171\u0003\u0004\u0002"+
		"\f\u0168\u0169\n!\u0000\u0000\u0169\u016a\u0005\u0010\u0000\u0000\u016a"+
		"\u016b\u0003\u0004\u0002\u0000\u016b\u016c\u0005\u0011\u0000\u0000\u016c"+
		"\u0171\u0001\u0000\u0000\u0000\u016d\u016e\n \u0000\u0000\u016e\u016f"+
		"\u0005\u001d\u0000\u0000\u016f\u0171\u0003\u0018\f\u0000\u0170\u012c\u0001"+
		"\u0000\u0000\u0000\u0170\u0134\u0001\u0000\u0000\u0000\u0170\u013b\u0001"+
		"\u0000\u0000\u0000\u0170\u013e\u0001\u0000\u0000\u0000\u0170\u0141\u0001"+
		"\u0000\u0000\u0000\u0170\u0144\u0001\u0000\u0000\u0000\u0170\u0147\u0001"+
		"\u0000\u0000\u0000\u0170\u014a\u0001\u0000\u0000\u0000\u0170\u014d\u0001"+
		"\u0000\u0000\u0000\u0170\u0150\u0001\u0000\u0000\u0000\u0170\u0153\u0001"+
		"\u0000\u0000\u0000\u0170\u0156\u0001\u0000\u0000\u0000\u0170\u0159\u0001"+
		"\u0000\u0000\u0000\u0170\u015c\u0001\u0000\u0000\u0000\u0170\u015f\u0001"+
		"\u0000\u0000\u0000\u0170\u0162\u0001\u0000\u0000\u0000\u0170\u0165\u0001"+
		"\u0000\u0000\u0000\u0170\u0168\u0001\u0000\u0000\u0000\u0170\u016d\u0001"+
		"\u0000\u0000\u0000\u0171\u0174\u0001\u0000\u0000\u0000\u0172\u0170\u0001"+
		"\u0000\u0000\u0000\u0172\u0173\u0001\u0000\u0000\u0000\u0173\u0005\u0001"+
		"\u0000\u0000\u0000\u0174\u0172\u0001\u0000\u0000\u0000\u0175\u0182\u0003"+
		"\u0014\n\u0000\u0176\u017f\u0005\u0010\u0000\u0000\u0177\u017c\u0003\u0006"+
		"\u0003\u0000\u0178\u0179\u0005\b\u0000\u0000\u0179\u017b\u0003\u0006\u0003"+
		"\u0000\u017a\u0178\u0001\u0000\u0000\u0000\u017b\u017e\u0001\u0000\u0000"+
		"\u0000\u017c\u017a\u0001\u0000\u0000\u0000\u017c\u017d\u0001\u0000\u0000"+
		"\u0000\u017d\u0180\u0001\u0000\u0000\u0000\u017e\u017c\u0001\u0000\u0000"+
		"\u0000\u017f\u0177\u0001\u0000\u0000\u0000\u017f\u0180\u0001\u0000\u0000"+
		"\u0000\u0180\u0181\u0001\u0000\u0000\u0000\u0181\u0183\u0005\u0011\u0000"+
		"\u0000\u0182\u0176\u0001\u0000\u0000\u0000\u0182\u0183\u0001\u0000\u0000"+
		"\u0000\u0183\u0007\u0001\u0000\u0000\u0000\u0184\u0185\u0003\u0014\n\u0000"+
		"\u0185\u0186\u0005\n\u0000\u0000\u0186\u0187\u0003\u0006\u0003\u0000\u0187"+
		"\t\u0001\u0000\u0000\u0000\u0188\u0189\u0003\u0014\n\u0000\u0189\u018a"+
		"\u0005\u0002\u0000\u0000\u018a\u018b\u0003\u0004\u0002\u0000\u018b\u000b"+
		"\u0001\u0000\u0000\u0000\u018c\u018d\u0003\u000e\u0007\u0000\u018d\u018e"+
		"\u0005G\u0000\u0000\u018e\u018f\u0003\u0004\u0002\u0000\u018f\r\u0001"+
		"\u0000\u0000\u0000\u0190\u01a1\u0005H\u0000\u0000\u0191\u019e\u0003\u0014"+
		"\n\u0000\u0192\u019b\u0005\u0007\u0000\u0000\u0193\u0198\u0003\b\u0004"+
		"\u0000\u0194\u0195\u0005\b\u0000\u0000\u0195\u0197\u0003\b\u0004\u0000"+
		"\u0196\u0194\u0001\u0000\u0000\u0000\u0197\u019a\u0001\u0000\u0000\u0000"+
		"\u0198\u0196\u0001\u0000\u0000\u0000\u0198\u0199\u0001\u0000\u0000\u0000"+
		"\u0199\u019c\u0001\u0000\u0000\u0000\u019a\u0198\u0001\u0000\u0000\u0000"+
		"\u019b\u0193\u0001\u0000\u0000\u0000\u019b\u019c\u0001\u0000\u0000\u0000"+
		"\u019c\u019d\u0001\u0000\u0000\u0000\u019d\u019f\u0005\t\u0000\u0000\u019e"+
		"\u0192\u0001\u0000\u0000\u0000\u019e\u019f\u0001\u0000\u0000\u0000\u019f"+
		"\u01a1\u0001\u0000\u0000\u0000\u01a0\u0190\u0001\u0000\u0000\u0000\u01a0"+
		"\u0191\u0001\u0000\u0000\u0000\u01a1\u000f\u0001\u0000\u0000\u0000\u01a2"+
		"\u01a3\u0003\u0014\n\u0000\u01a3\u01a4\u0005\u0002\u0000\u0000\u01a4\u01a9"+
		"\u0003\u0012\t\u0000\u01a5\u01a6\u0005@\u0000\u0000\u01a6\u01a8\u0003"+
		"\u0012\t\u0000\u01a7\u01a5\u0001\u0000\u0000\u0000\u01a8\u01ab\u0001\u0000"+
		"\u0000\u0000\u01a9\u01a7\u0001\u0000\u0000\u0000\u01a9\u01aa\u0001\u0000"+
		"\u0000\u0000\u01aa\u01ae\u0001\u0000\u0000\u0000\u01ab\u01a9\u0001\u0000"+
		"\u0000\u0000\u01ac\u01ad\u0005\u0003\u0000\u0000\u01ad\u01af\u0003\u0004"+
		"\u0002\u0000\u01ae\u01ac\u0001\u0000\u0000\u0000\u01ae\u01af\u0001\u0000"+
		"\u0000\u0000\u01af\u0011\u0001\u0000\u0000\u0000\u01b0\u01bd\u0003\u0014"+
		"\n\u0000\u01b1\u01ba\u0005\u0007\u0000\u0000\u01b2\u01b7\u0003\b\u0004"+
		"\u0000\u01b3\u01b4\u0005\b\u0000\u0000\u01b4\u01b6\u0003\b\u0004\u0000"+
		"\u01b5\u01b3\u0001\u0000\u0000\u0000\u01b6\u01b9\u0001\u0000\u0000\u0000"+
		"\u01b7\u01b5\u0001\u0000\u0000\u0000\u01b7\u01b8\u0001\u0000\u0000\u0000"+
		"\u01b8\u01bb\u0001\u0000\u0000\u0000\u01b9\u01b7\u0001\u0000\u0000\u0000"+
		"\u01ba\u01b2\u0001\u0000\u0000\u0000\u01ba\u01bb\u0001\u0000\u0000\u0000"+
		"\u01bb\u01bc\u0001\u0000\u0000\u0000\u01bc\u01be\u0005\t\u0000\u0000\u01bd"+
		"\u01b1\u0001\u0000\u0000\u0000\u01bd\u01be\u0001\u0000\u0000\u0000\u01be"+
		"\u0013\u0001\u0000\u0000\u0000\u01bf\u01c2\u0005I\u0000\u0000\u01c0\u01c2"+
		"\u0005J\u0000\u0000\u01c1\u01bf\u0001\u0000\u0000\u0000\u01c1\u01c0\u0001"+
		"\u0000\u0000\u0000\u01c2\u0015\u0001\u0000\u0000\u0000\u01c3\u01c4\u0005"+
		"I\u0000\u0000\u01c4\u0017\u0001\u0000\u0000\u0000\u01c5\u01c6\u0005K\u0000"+
		"\u0000\u01c6\u0019\u0001\u0000\u0000\u0000.\u001d(*4ADGMWcfimz}\u0080"+
		"\u0084\u0091\u009c\u009f\u00a2\u00ab\u00ae\u00b1\u00c5\u00d6\u00e2\u00f3"+
		"\u00fa\u0103\u012a\u0170\u0172\u017c\u017f\u0182\u0198\u019b\u019e\u01a0"+
		"\u01a9\u01ae\u01b7\u01ba\u01bd\u01c1";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}