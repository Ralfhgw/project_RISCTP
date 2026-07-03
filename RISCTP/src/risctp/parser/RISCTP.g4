// ---------------------------------------------------------------------------
// RISCTP.g4
// RISC Theorem Proving Interface ANTLR4 Grammar 
// $Id: RISCTP.g4,v 1.21 2024/06/06 17:23:56 schreine Exp $
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

grammar RISCTP;

options 
{
  language=Java;
}

@header 
{
  package risctp.parser;
}

// ---------------------------------------------------------------------------
// problems and declarations
// ---------------------------------------------------------------------------

// problems
problem: ( decl )* EOF ;

// declarations
decl:
  'type' id ( '=' type ( 'with' exp )? )? EOS              #TypeDecl
| 'datatype' dtitem ( 'and' dtitem)* EOS                   #DataType
| 'fun' id ( '(' ( tvar ( ',' tvar )* )? ')' )? ':' type 
   ('=' exp )? EOS                                         #Function
| 'const' id ':' type ( '=' exp )? EOS                     #Constant
| 'pred' id ( '(' ( tvar ( ',' tvar )* )? ')' )?  
   ( ( '⇔' | '<=>' ) exp )? EOS                            #Predicate
| 'axiom' ( '[' fid=id ( '(' ( type ( ',' type )* )? ')' )? ']' )? 
   aid=id ( '⇔' | '<=>' ) exp EOS                          #Axiom
| 'theorem' id ( '⇔' | '<=>' ) exp EOS                     #Theorem
;

// ---------------------------------------------------------------------------
// expressions and types
// ---------------------------------------------------------------------------

// expressions
exp  : 
  dec                                   #Decimal
| id ( '(' ( exp ( ',' exp )* )? ')' )? #Apply
| id ( '(' ( exp ( ',' exp )* )? ')' )? '{*}' #ApplyNotGoal

// maps, tuples, variants
| 'map' '[' pid ',' pid ']' '(' exp ')'     #MapConstruct
| ( '〈' | '⟨' | '〈' | '<<' ) exp ( ',' exp )* ( '〉' | '⟩' | '〉' | '>>' )                 
                                            #TupleConstruct
| exp '[' exp ']'                           #MapSelect
| exp '.' dec                               #TupleSelect
| exp 'with' '[' exp ']' '=' exp            #MapStore
| exp 'with' '.' dec '=' exp                #TupleStore

// arithmetic
| '-' exp                #Neg
| exp ( '*' | '⋅' ) exp  #Mult
| exp '/' exp            #Div
| exp '%' exp            #Mod
| exp '-' exp            #Minus
| exp '+' exp            #Plus

// infix relations
| exp '=' exp             #Equal
| exp ( '≠' | '~=' ) exp  #NotEqual
| exp '<' exp             #Less
| exp ( '≤' | '<=' ) exp  #LessEqual
| exp '>' exp             #Greater
| exp ( '≥' | '>=' ) exp  #GreaterEqual

// formulas
| ( '⊥' | 'false' )                #False
| ( '⊤' | 'true'  )                #True
| ( '¬' | '~' ) exp                #Not
| exp ( '∧' | '/\\' ) exp          #And
| exp ( '∨' | '\\/' ) exp          #Or
| exp ( '⇒' | '=>' ) exp           #Imp
| exp ( '⇔' | '<=>' ) exp          #Equiv
| ( '∀' | 'forall' ) tvar ( ',' tvar )* '.' exp  #Forall
| ( '∃' | 'exists' ) tvar ( ',' tvar )* '.' exp  #Exists

// generic terms
| 'if' exp 'then' exp 'else' exp            #IfThenElse
| 'match' exp 'with' 
  ( '|' )? mbinder ( '|' mbinder )*         #Match
| 'let' lbinder ( ',' lbinder )* 'in' exp   #Let
| 'choose' tvar 'with' exp                  #Choose
| 'choose' '[' 'sat' ']' tvar 'with' exp    #ChooseSat
| 'choose' '[' 'uni' ']' tvar 'with' exp    #ChooseUni
| 'choose' '[' 'def' ']' tvar 'with' exp    #ChooseDef

// parenthesized expressions
| '(' exp ')'  #Parentheses
;
 
// types
type : id ( '[' ( type ( ',' type )* )? ']' )? ;

// ---------------------------------------------------------------------------
// miscellaneous
// ---------------------------------------------------------------------------

// typed variables
tvar : id ':' type ;

// let binders
lbinder : id '=' exp ;

// match binders
mbinder : pattern '->' exp ;

// patterns
pattern : 
  '_'                                      #DefaultPattern
| id ( '(' ( tvar ( ',' tvar )* )? ')' )?  #ConstrPattern
;

// datatype items
dtitem: id '=' dtconstr ( '|' dtconstr )* ( 'with' exp )? ;

// datatype constructors
dtconstr : id ( '(' ( tvar ( ',' tvar )* )? ')' )? ;

// identifiers (plain or quoted)
id:  
  ID  #PlainId
| QID #QuotedId
; 

// plain ids
pid: ID; 

// decimal literals
dec: DEC ;

// ---------------------------------------------------------------------------
// lexical rules
// ---------------------------------------------------------------------------

// reserve \ for internal identifiers
ID  : [a-zA-Z_][a-zA-Z_0-9]* ;
QID : [']~['\\§]+['] ;
DEC : [0-9]+ ;
EOS : ';' ;

WHITESPACE  : [ \t\r\n\f]+ -> skip ;
LINECOMMENT : '//' .*? '\r'? ('\n' | EOF) -> skip ;
COMMENT     : '/*' .*? '*/' -> skip ;

// matches any other character
ERROR : . ;

// ---------------------------------------------------------------------------
// end of file
// ---------------------------------------------------------------------------