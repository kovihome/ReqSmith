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

application : 'application' qualifiedId sourceRef? applicationDefinitionClosure? ;

module : 'module' qualifiedId sourceRef? applicationDefinitionClosure? ;

actor : 'actor' qualifiedId sourceRef? typelessDefinitionClosure? ;

action : 'action' qualifiedId actionDefinitionClosure? ;

class : 'class' qualifiedId (KWATOMIC | (KWENUMERATION enumDefinitionClosure) | (parent? sourceRef? simpleDefinitionClosure?)) ;

entity : 'entity' qualifiedId parent? sourceRef? definitionClosure? ;

view : 'view' qualifiedId parent? sourceRef? viewDefinitionClosure? ;

feature : 'feature' qualifiedId sourceRef? definitionClosure? ;

style : 'style' qualifiedId sourceRef? styleDefinitionClosure? ;

// building blocks of the top elements

sourceRef : 'from' qualifiedId ;

parent : 'is' qualifiedId ;

// general definition closure rules
definitionClosure : closureStart featureRef*? property*? closureEnd;

// simple definition closure rules
simpleDefinitionClosure : closureStart property*? closureEnd;

// enum definition closure rules
enumDefinitionClosure : closureStart enumList closureEnd;

// TODO: azt is meg kellene engedni, hogy a lista elemei külön sorokban szerepeljenek
enumList : (ID ',')*? ID ;

// action definition closure rules
actionDefinitionClosure : closureStart actionCall*? closureEnd ;

actionCall : ID paramList? ;

paramList : (paramValue ',')*? paramValue ;

paramValue : INT | StringLiteral | SemanticVersionNumber | qualifiedId ;

// typeless definition closure rules
typelessDefinitionClosure : closureStart featureRef*? typelessProperty*? closureEnd ;

typelessProperty : simpleTypelessProperty | compoundTypelessProperty ;

// NEW: application definition closure rules
applicationDefinitionClosure : closureStart featureRef*? applicationProperty*? closureEnd ;

applicationProperty : simpleApplicationProperty | compoundTypelessProperty ;

simpleApplicationProperty: qualifiedId (':' applicationPropertyValue)? ;

// NEW: view definition closure
viewDefinitionClosure : closureStart featureRef*? viewProperty*? closureEnd ;

viewProperty :  simpleTypelessProperty |  compoundViewProperty ;

compoundViewProperty : qualifiedId closureStart viewProperty*? closureEnd ;

// NEW style definition closure
styleDefinitionClosure : closureStart featureRef*? styleProperty*? closureEnd ;

styleProperty : simpleTypelessProperty | compoundTypelessProperty | layoutStyleProperty ;

layoutStyleProperty : qualifiedId closureStart compoundTypelessProperty*? closureEnd ;


featureRef : '@' qualifiedId ((closureStart property*? closureEnd) | (':' qualifiedId))? ;

simpleTypelessProperty : qualifiedId (':' propertyValue)? ;

compoundTypelessProperty : qualifiedId closureStart simpleTypelessProperty*? closureEnd ;

property : qualifiedId (propertyClosure | (':' optionality? propertyValue))? ;

propertyClosure : closureStart propertyAttribute*? closureEnd ;

propertyAttribute : (simpleId ':' propertyValue) | optionality | qualifiedId ;

propertyValue : INT | StringLiteral | SemanticVersionNumber | propertyType | qualifiedId ;

applicationPropertyValue: INT | StringLiteral | SemanticVersionNumber | qualifiedId ;

optionality : KWOPTIONAL | KWMANDATORY ;



// ID definitions

simpleId : ID ;

qualifiedId : (simpleId DOT)* simpleId ;

// others
closureStart : LCURLY ; // NL* ;
closureEnd : RCURLY ; // NL* ;

propertyType : KWLISTOF? ID ; // | 'string' | 'numeric' ;
StringLiteral : '"' StringElement* '"' | '\'' StringElement* '\'' ;
SemanticVersionNumber : MAJOR '.' MINOR '.' PATCH ( '-' PRERELEASE )? ( '+' BUILD )? ;

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

AND : 'and' ;
OR : 'or' ;
NOT : 'not' ;
EQ : '=' ;
COMMA : ',' ;
SEMI : ';' ;
LPAREN : '(' ;
RPAREN : ')' ;
LCURLY : '{' ;
RCURLY : '}' ;
DOT : '.' ;
// keywords
KWATOMIC : 'atomic' ;
KWENUMERATION : 'enumeration' ;
KWOPTIONAL : 'optional' ;
KWMANDATORY : 'mandatory' ;
KWLISTOF : 'listOf' ;
//APPLICATION_ : 'application' ;
//MODULE_ : 'module' ;
//ACTOR_ : 'actor' ;
//ENTITY_ : 'entity' ;
//EXTENSION_ : 'extension' ;

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

