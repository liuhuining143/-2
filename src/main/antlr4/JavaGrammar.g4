grammar JavaGrammar;

// 起始规则
compilationUnit
    : packageDeclaration? importDeclaration* typeDeclaration* EOF
    ;

packageDeclaration
    : 'package' qualifiedName ';'
    ;

importDeclaration
    : 'import' 'static'? qualifiedName ('.' '*')? ';'
    ;

typeDeclaration
    : classDeclaration
    | interfaceDeclaration
    ;

classDeclaration
    : 'class' IDENTIFIER ('extends' typeType)? ('implements' typeList)? classBody
    ;

interfaceDeclaration
    : 'interface' IDENTIFIER ('extends' typeList)? interfaceBody
    ;

classBody
    : '{' classBodyDeclaration* '}'
    ;

interfaceBody
    : '{' interfaceBodyDeclaration* '}'
    ;

classBodyDeclaration
    : ';'
    | memberDeclaration
    ;

memberDeclaration
    : methodDeclaration
    | fieldDeclaration
    ;
// 定义 interfaceBodyDeclaration 规则
interfaceBodyDeclaration
    : methodDeclaration
    | fieldDeclaration
    | ';' // 允许空声明
    ;
methodDeclaration
    : typeTypeOrVoid IDENTIFIER '(' formalParameterList? ')' methodBody
    ;

fieldDeclaration
    : typeType variableDeclarators ';'
    ;

formalParameterList
    : formalParameter (',' formalParameter)*
    ;

formalParameter
    : typeType variableDeclaratorId
    ;

variableDeclarators
    : variableDeclarator (',' variableDeclarator)*
    ;

variableDeclarator
    : variableDeclaratorId ('=' variableInitializer)?
    ;

variableDeclaratorId
    : IDENTIFIER ('[' ']')*
    ;

variableInitializer
    : arrayInitializer
    | expression
    ;

arrayInitializer
    : '{' (variableInitializer (',' variableInitializer)* ','?)? '}'
    ;

typeTypeOrVoid
    : 'void'
    | typeType
    ;

typeType
    : primitiveType ('[' ']')*
    | referenceType
    ;

primitiveType
    : 'boolean'
    | 'char'
    | 'byte'
    | 'short'
    | 'int'
    | 'long'
    | 'float'
    | 'double'
    ;

referenceType
    : qualifiedName ('[' ']')*
    ;

qualifiedName
    : IDENTIFIER ('.' IDENTIFIER)*
    ;

typeList
    : typeType (',' typeType)*
    ;

methodBody
    : block
    | ';'
    ;

block
    : '{' blockStatement* '}'
    ;

blockStatement
    : localVariableDeclarationStatement
    | statement
    ;

localVariableDeclarationStatement
    : typeType variableDeclarators ';'
    ;

statement
    : block
    | 'if' '(' expression ')' statement ('else' statement)?
    | 'while' '(' expression ')' statement
    | 'for' '(' forControl ')' statement
    | 'return' expression? ';'
    | expression ';'
    | ';'
    ;

forControl
    : enhancedForControl
    | forInit? ';' expression? ';' forUpdate?
    ;

enhancedForControl
    : typeType variableDeclaratorId ':' expression
    ;

forInit
    : typeType variableDeclarators
    | expressionList
    ;

forUpdate
    : expressionList
    ;

expressionList
    : expression (',' expression)*
    ;

expression
    : primary
    | expression '.' IDENTIFIER
    | expression '[' expression ']'
    | expression '(' expressionList? ')'
    | '(' typeType ')' expression
    | expression ('++' | '--')
    | ('+' | '-' | '++' | '--') expression
    | ('~' | '!') expression
    | expression ('*' | '/' | '%') expression
    | expression ('+' | '-') expression
    | expression ('<' '<' | '>' '>' '>' | '>' '>') expression
    | expression ('<' | '>' | '<=' | '>=') expression
    | expression ('==' | '!=') expression
    | expression '&' expression
    | expression '^' expression
    | expression '|' expression
    | expression '&&' expression
    | expression '||' expression
    | expression '?' expression ':' expression
    | expression '=' expression
    ;

primary
    : '(' expression ')'
    | literal
    | IDENTIFIER
    ;

literal
    : INTEGER_LITERAL
    | FLOAT_LITERAL
    | BOOLEAN_LITERAL
    | CHAR_LITERAL
    | STRING_LITERAL
    | 'null'
    ;

// 词法规则
IDENTIFIER
    : [a-zA-Z_] [a-zA-Z_0-9]*
    ;

INTEGER_LITERAL
    : DECIMAL_LITERAL
    | HEX_LITERAL
    | OCTAL_LITERAL
    | BINARY_LITERAL
    ;

DECIMAL_LITERAL
    : '0'
    | [1-9] [0-9]*
    ;

HEX_LITERAL
    : '0' [xX] [0-9a-fA-F]+
    ;

OCTAL_LITERAL
    : '0' [0-7]+
    ;

BINARY_LITERAL
    : '0' [bB] [0-1]+
    ;

FLOAT_LITERAL
    : DECIMAL_FLOAT_LITERAL
    | HEX_FLOAT_LITERAL
    ;

DECIMAL_FLOAT_LITERAL
    : [0-9]* '.' [0-9]+ ([eE] [+-]? [0-9]+)?
    | [0-9]+ '.' ([eE] [+-]? [0-9]+)?
    | [0-9]+ [eE] [+-]? [0-9]+
    ;

HEX_FLOAT_LITERAL
    : '0' [xX] [0-9a-fA-F]* '.' [0-9a-fA-F]+ [pP] [+-]? [0-9]+
    | '0' [xX] [0-9a-fA-F]+ '.' [pP] [+-]? [0-9]+
    | '0' [xX] [0-9a-fA-F]+ [pP] [+-]? [0-9]+
    ;

BOOLEAN_LITERAL
    : 'true'
    | 'false'
    ;

CHAR_LITERAL
    : '\'' (~['\\\r\n] | EscapeSequence) '\''
    ;

STRING_LITERAL
    : '"' (~["\\\r\n] | EscapeSequence)* '"'
    ;

EscapeSequence
    : '\\' [btnfr"'\\]
    | '\\' ([0-3]? [0-7])? [0-7]
    ;

// 忽略空白字符和注释
WS
    : [ \t\r\n]+ -> skip
    ;

COMMENT
    : '/*' .*? '*/' -> skip
    ;

LINE_COMMENT
    : '//' ~[\r\n]* -> skip
    ;