/*
 * ReqSmith - Build application from requirements
 * Copyright (c) 2023. Kovi <kovihome86@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

grammar ReqMParser;
// options { tokenVocab=ReqMLexer; }

// **** top elements ****

reqm : topStat*? EOF ;

topStat : application | module | actor | entity | view | feature | class | style | action ;

application : KWAPPLICATION qualifiedId sourceRef? applicationDefinitionClosure? ;

module : KWMODULE qualifiedId sourceRef? applicationDefinitionClosure? ;

actor : KWACTOR qualifiedId sourceRef? typelessDefinitionClosure? ;

action : KWACTION qualifiedId actionDefinitionClosure? ;

class : 'class' qualifiedId (KWATOMIC | (KWENUMERATION enumDefinitionClosure) | (parent? sourceRef? simpleDefinitionClosure?)) ;

entity : KWENTITY qualifiedId parent? sourceRef? definitionClosure? ;

view : KWVIEW qualifiedId parent? sourceRef? viewDefinitionClosure? ;

feature : KWFEATURE qualifiedId sourceRef? definitionClosure? ;

style : 'style' qualifiedId sourceRef? styleDefinitionClosure? ;

// building blocks of the top elements

sourceRef : 'from' qualifiedId ;

parent : 'is' qualifiedId ;

// general definition closure rules
definitionClosure : closureStart featureRef*? property*? closureEnd;

// simple definition closure rules
simpleDefinitionClosure : closureStart property*? closureEnd;

// enum definition closure rules
enumDefinitionClosure : closureStart idList closureEnd;

// TODO: azt is meg kellene engedni, hogy a lista elemei külön sorokban szerepeljenek
idList : (ID ',')*? ID ;

// action definition closure rules
actionDefinitionClosure : closureStart actionCall*? closureEnd ;

actionCall : ID paramList? ;

paramList : (paramValue ',')*? paramValue ;

paramValue : INT | StringLiteral | SemanticVersionNumber | qualifiedId ;

// typeless definition closure rules
typelessDefinitionClosure : closureStart featureRef*? typelessProperty*? closureEnd ;

typelessProperty : simpleTypelessProperty | compoundTypelessProperty ;

// application definition closure rules
applicationDefinitionClosure : closureStart featureRef*? applicationProperty*? closureEnd ;

applicationProperty : simpleApplicationProperty | compoundTypelessProperty | applicationTwoLevelProperty ;

simpleApplicationProperty: simpleId (':' applicationPropertyValue)? ;

applicationTwoLevelProperty : simpleId closureStart compoundTypelessProperty*? closureEnd ;

// view definition closure
viewDefinitionClosure : closureStart featureRef*? viewProperty*? closureEnd ;

viewProperty :  simpleTypelessProperty |  compoundViewProperty ;

compoundViewProperty : simpleId closureStart viewProperty*? closureEnd ;

// style definition closure
styleDefinitionClosure : closureStart featureRef*? styleProperty*? closureEnd ;

styleProperty : simpleTypelessProperty | compoundTypelessProperty | layoutStyleProperty ;

layoutStyleProperty : simpleId closureStart compoundTypelessProperty*? closureEnd ;


featureRef : '@' qualifiedId ((closureStart property*? closureEnd) | (':' qualifiedId))? ;

//simpleTypelessProperty : qualifiedId (':' (propertyValue | idList))? ;
simpleTypelessProperty : qualifiedId | (simpleId ':' (propertyValue | idList)) ;

compoundTypelessProperty : simpleId closureStart simpleTypelessProperty*? closureEnd ;

property : simpleId (propertyClosure | (':' optionality? propertyValue))? ;

propertyClosure : closureStart propertyAttribute*? closureEnd ;

propertyAttribute : (simpleId ':' propertyValue) | optionality | qualifiedId ;

propertyValue : INT | StringLiteral | SemanticVersionNumber | propertyType | qualifiedId ;

applicationPropertyValue: INT | StringLiteral | SemanticVersionNumber | qualifiedId ;

optionality : KWOPTIONAL | KWMANDATORY ;

// ID definitions
simpleId : KWACTION | KWSTYLE | ID ;
qualifiedId : (simpleId DOT)* simpleId ;

// NEW: expressions
//booleanExpr : orExpr ;
//orExpr : andExpr (OR andExpr)* ;
//andExpr : equalityExpr (AND equalityExpr)* ;
//equalityExpr : unaryExpr ((EQ | NEQ) unaryExpr)* ;
//unaryExpr : NOT unaryExpr | primary ;
//primary : ID | BOOLEAN | LPAREN booleanExpr RPAREN ;

// others
closureStart : LCURLY ; // NL* ;
closureEnd : RCURLY ; // NL* ;

propertyType : KWLISTOF? ID ; // | 'string' | 'numeric' ;
StringLiteral : '"' StringElement* '"' | '\'' StringElement* '\'' ;
SemanticVersionNumber : MAJOR '.' MINOR '.' PATCH ( '-' PRERELEASE )? ( '+' BUILD )? ;

// keywords
KWAPPLICATION: 'application' ;
KWMODULE: 'module' ;
KWACTOR: 'actor' ;
KWENTITY: 'entity' ;
KWACTION: 'action' ;
// KWTYPE: 'type' ;
KWVIEW: 'view' ;
KWSTYLE: 'style' ;
KWFEATURE: 'feature' ;

KWATOMIC : 'atomic' ;
KWENUMERATION : 'enumeration' ;
KWOPTIONAL : 'optional' ;
KWMANDATORY : 'mandatory' ;
KWLISTOF : 'listOf' ;

// **** Fragments ****
fragment StringElement : '\u0020' | '\u0021' | '\u0023' .. '\u007F' ; // | CharEscapeSeq
fragment DIGIT : [0-9] ;
fragment MAJOR : DIGIT+ ;
fragment MINOR : DIGIT+ ;
fragment PATCH : DIGIT+ ;
fragment PRERELEASE : [0-9A-Za-z-]+ ;
fragment BUILD : [0-9A-Za-z-]+ ;
fragment WhiteSpaceNL : '\u0020' | '\u0009' | '\u000D' | '\u000A' ;
// fragment WhiteSpace : '\u0020' | '\u0009' ;
// fragment ANYTEXT : ~[\r\n]*;    // [a-zA-Z_0-9 \t{}()@,.]* ;

// **** Terminal elements ****

//BOOLEAN: 'true' | 'false';
AND : 'and' ;
OR : 'or' ;
NOT : 'not' ;
EQ : '=' ; // or '==' ;
NEQ: '!=';
COMMA : ',' ;
SEMI : ';' ;
LPAREN : '(' ;
RPAREN : ')' ;
LCURLY : '{' ;
RCURLY : '}' ;
DOT : '.' ;
INT : [0-9]+ ;
ID : [a-zA-Z_][a-zA-Z_0-9]* ;
TEXT : [a-zA-Z_0-9@,.] ;

// skipped parts
// Whitespace and comments
// NEWLINE : '\r'? '\n';
// WS: [ \t\n\r\f]+ -> skip ;
WS :  WhiteSpaceNL+ -> skip ;
COMMENT : '/*' (COMMENT | .)*? '*/' -> skip ;
LINE_COMMENT : '//' (~[\r\n])* -> skip ;

