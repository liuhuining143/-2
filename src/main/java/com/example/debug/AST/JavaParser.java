package com.example.debug.AST;

import com.example.debug.ErrorEntry;
import com.example.debug.Symbol.Quadruple;
import com.example.debug.Symbol.SymbolType;
import com.example.debug.Token;


import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

public class JavaParser {
    private final List<Token> tokens;
    private int currentPos;
    private final List<ErrorEntry> errors = new ArrayList<>();

    private static final String TYPE_KEYWORD = "关键字";
    private static final String TYPE_IDENTIFIER = "标识符";
    private Stack<String> scope=new Stack<>();
    private static final Map<String, SymbolType> symbols = new HashMap<>();
    private final List<Quadruple> quadruples = new ArrayList<>();
    private int quadCounter = 0;
    // 调试模式开关
    private static final boolean DEBUG_MODE = true; // 可配置为false关闭调试输出
    private int labelCounter = 0;
    private int globalTempCounter = 0; // 全局唯一递增计数器
    private final Deque<String> scopeStack = new ArrayDeque<>(); // 存储当前作用域名
    private boolean isMethod=true;


    public JavaParser(List<Token> tokens) {
        this.tokens = tokens;
        this.currentPos = 0;
    }

    // 解析入口
    public ASTNode parse() {
        System.out.println("开始语法分析...");
        List<ASTNode> nodes = new ArrayList<>();
        while (!isAtEnd()) {
            Token token = peek();
            if (isKeyword(token, "package")) {
                System.out.println("匹配到 package 关键字，开始解析包声明");
                nodes.add(parsePackage());
            } else if (checkValue("import") && isKeyword(token, "import")) {
                System.out.println("匹配到 import 关键字，开始解析导入声明");
                nodes.add(parseImport());
            } else if (isKeyword(token, "public")||isKeyword(token, "private")) {
                System.out.println("开始解析修饰符");
                if(isKeyword(tokens.get(currentPos+1), "class")){
                    System.out.println("匹配到 public/private 关键字，开始解析类定义");
                    advance();
                    nodes.add(parseClass());
                }
                else {
                    System.out.println("开始解析方法定义");
                    nodes.add(parseMethod());
                }
            } else if (isKeyword(token, "class")) {
                System.out.println("匹配到 class 关键字，开始解析类定义");
                nodes.add(parseClass());
            } else if (isKeyword(token, "void")||isKeyword(token, "int")||isKeyword(token, "long")||isKeyword(token, "short")||isKeyword(token, "byte")||isKeyword(token, "char")||isKeyword(token, "float")||isKeyword(token, "double")||isKeyword(token, "boolean")||isKeyword(token, "String")) {
                System.out.println("匹配到 返回值 关键字，开始解析方法定义");
                nodes.add(parseMethod());
            } else  if (token.getType().equals("单行注释") || token.getType().equals("多行注释")) {
                System.out.println("跳过注释: " + token.getValue());
                advance();
                continue;
            } else {
                System.out.println("跳过无法识别的 Token: " + token.getValue());
                advance();
            }
        }
        System.out.println("语法分析完成");
        return new ProgramNode(nodes);
    }

    // 解析包声明：package com.example;
    private PackageDeclaration parsePackage() {
        System.out.println("解析包声明...");
        consumeValue("package", "Expect 'package'");
        StringBuilder packageName = new StringBuilder();

        // 解析包名（com.example.test）
        while (true) {
            if (checkType(TYPE_IDENTIFIER)) {
                packageName.append(consume(TYPE_IDENTIFIER, "Expect identifier").getValue());
                if (checkValue(".")) {
                    packageName.append(consumeValue(".", "Expect '.'").getValue());
                } else {
                    break;
                }
            } else {
                errors.add(new ErrorEntry(peek().getLine(), "语法错误", "Unexpected token in package name: " + peek().getValue()));
                throw new ParseError();
            }
        }

        consumeValue(";", "Expect ';' after package name");
        System.out.println("包声明解析完成: " + packageName);
        return new PackageDeclaration(previous().getLine(), packageName.toString());
    }

    // 解析导入声明
    private ImportDeclaration parseImport() {
        consumeValue("import", "Expect 'import'");
        StringBuilder importName = new StringBuilder();
        while (checkType(TYPE_IDENTIFIER) ){
            importName.append(consume(TYPE_IDENTIFIER, "Expect identifier").getValue());
            if (checkValue(".")) {
                consumeValue(".", "Expect '.'");
                importName.append(".");
            } else if (checkValue(".*")) {
                consumeValue(".*", "Expect '.*'");
                importName.append(".*");
            }
        }
        consumeValue(";", "Expect ';' after import");
        return new ImportDeclaration(previous().getLine(), importName.toString());
    }

    // 解析类定义：class MyClass { ... }
    private ClassDeclaration parseClass() {
        SymbolType symbol = new SymbolType();
        System.out.println("解析类定义...");
        consumes("class", "Expect 'class'");
        String className = consume("标识符", "Expect class name").getValue();
        scope.push("Class "+className);
        symbol.setName(className);

            Stack<String> scopeCopy = new Stack<>();
            scopeCopy.addAll(this.scope);
            symbol.setScope(scopeCopy);

        symbol.setType("类");
        symbol.setLine(previous().getLine());
        symbols.put(className,symbol);

        String scopePath = String.join(".", scope);
        emitQuadruple("CLASS_DEF", className, scopePath, null);


        consumes("{", "Expect '{' after class name");

        List<FieldDeclaration> fields = new ArrayList<>();
        List<MethodDeclaration> methods = new ArrayList<>();

        while (!matchs("}") && !isAtEnd()) {
            List<String> modifiers = parseModifiers();

            // 增强的成员类型判断逻辑
            if (isMethodDeclaration(modifiers)&&isMethod) {
                methods.add(parseMethod());
            } else if (isFieldDeclaration(modifiers)) {
                fields.add(parseField(modifiers));
            } else {
                // 处理可能的内嵌类或初始化块
                if (checkValue("static") && checkNextToken("{")) {
                    System.out.println("静态代码块");
                } else {
                    errors.add(new ErrorEntry(peek().getLine(),
                            "语法错误",
                            "非法的类成员: " + peek().getValue()));
                    synchronizeToMemberEnd();
                }
            }
        }


        System.out.println("类定义解析完成: " + className);
        scope.pop();
        return new ClassDeclaration(previous().getLine(), className, fields, methods);
    }



    // 解析方法定义：public static void method(int param) { ... }
    private MethodDeclaration parseMethod() {
        System.out.println("解析方法定义...");
        isMethod=false;

        try {
            // 1. 解析修饰符（public/static/final等）
            List<String> modifiers = parseModifiers();
            System.out.println("方法修饰符: " + modifiers);

            // 2. 解析返回类型（支持泛型、数组、void）
            String returnType = consumeType();
            System.out.println("方法返回类型: " + returnType);

            // 3. 解析方法名
            String methodName = consume("标识符", "Expect method name").getValue();
            scopeStack.push("method_" + methodName);
            scope.push("Method "+methodName);
            SymbolType symbol = new SymbolType();
            symbol.setName(methodName);
            Stack<String> scopeCopy = new Stack<>();
            scopeCopy.addAll(this.scope);
            symbol.setScope(scopeCopy);
            symbol.setType("方法");
            symbol.setLine(previous().getLine());
            symbol.setReturnType(returnType);

            System.out.println("方法名称: " + methodName);

            // 4. 解析参数列表（支持复杂类型）
            consumes("(", "Expect '(' after method name");
            List<Parameter> parameters= new ArrayList<>();
            if(!checkValue(")")){parameters = parseParameters();}
            consumes(")", "Expect ')' after parameters");
            System.out.println("参数列表: " + parameters.size() + " 个参数");
            symbol.setParameters(parameters);
            symbols.put(methodName,symbol);

            // 5. 解析throws声明（简单跳过）
            if (matchs("throws")) {
                System.out.println("跳过throws声明");
                while (!matchs("{") && !isAtEnd() && !checkValue("{")) {
                    advance(); // 跳过异常列表
                }
            }

            // 方法入口四元式（增强参数信息）
            String paramTypes = parameters.stream()
                    .map(p -> p.getType())
                    .collect(Collectors.joining(","));
            emitQuadruple("METHOD_BEGIN", methodName, returnType, paramTypes);

            // 参数符号记录与四元式
            for (Parameter param : parameters) {
                SymbolType paramSymbol = new SymbolType();
                paramSymbol.setType("param");
                paramSymbol.setDataType(param.getType());
                symbols.put(param.getName(), paramSymbol);
                emitQuadruple("PARAM", param.getName(), param.getType(), null);
            }

            BlockStatement body = null;
            if (matchs("{")) {
                body = parseBlock();

                // 生成方法体代码
                if (body != null) {
                    // 进入方法作用域
                    scopeStack.push(methodName);


                    // 生成语句代码
                    for (ASTNode stmt : body.getStatements()) {
                        generateStatementCode(stmt);
                    }


                    scopeStack.pop();
                }
                isMethod=true;
                System.out.println("方法体解析完成");
            } else {
                consumes(";", "Expect ';' for abstract method");
                System.out.println("抽象方法（无方法体）");
            }
            scope.pop();
            System.out.println("方法定义解析完成: " + methodName);
            int localVarCount = 0;


            emitQuadruple("METHOD_END", methodName, String.valueOf(localVarCount), null);

            return new MethodDeclaration(
                    previous().getLine(),
                    modifiers, // 包含所有修饰符
                    returnType,
                    methodName,
                    parameters,
                    body
            );
        } catch (ParseError e) {
            System.out.println("方法解析失败! 当前位置: line=" + peek().getLine()
                    + ", token=" + peek().getValue());
            synchronizeToMethodEnd(); // 错误恢复：跳转到方法结束
            throw e;
        }
    }




    // 解析参数列表：int a, String b, int[] c, List<String> d
    private List<Parameter> parseParameters() {
        System.out.println("解析参数列表...");
        List<Parameter> params = new ArrayList<>();
        if (matchs(")")) return params; // 无参数
        do {

            String type = consumeType();
            String name = consume("标识符", "Expect parameter name").getValue();
            params.add(new Parameter(type, name));
        } while (matchs(","));
        System.out.println("参数列表解析完成，参数数量: " + params.size());
        return params;
    }

    // 解析语句块：{ ... }
    private BlockStatement parseBlock() {
        int line = previous().getLine();
        List<ASTNode> statements = new ArrayList<>();
        int braceDepth = 1;

        // 进入新的块作用域
        int initialScopeDepth = scope.size();
        String scopeName = "block_" + line;
        scopeStack.push(scopeName); // 进入新作用域
        System.out.println("进入块作用域: " + scopeName);

        try {
            while (braceDepth > 0 && !isAtEnd()) {
                if (matchs("{")) {
                    braceDepth++;
                } else if (matchs("}")) {
                    braceDepth--;
                    if (braceDepth == 0) {
                        break;
                    }
                } else {
                    try {
                        ASTNode stmt = parseStatement();
                        if (stmt != null) {
                            statements.add(stmt);
                        }
                    } catch (ParseError e) {
                        synchronizeToStatementEnd();
                    }
                }
            }
        } finally {
            // 防御性弹出：仅弹出当前块的作用域
            if (scope.size() > initialScopeDepth && !scope.isEmpty()) {
                scope.pop();
            }
        }

        if (braceDepth != 0) {
            errors.add(new ErrorEntry(line, "语法错误", "未闭合的代码块"));
        }

        return new BlockStatement(line, statements);
    }
    private void synchronizeToStatementEnd() {
        System.out.println("错误恢复：跳转到语句结束");
        while (!isAtEnd()) {
            if (matchs(";") || matchs("}") || matchs("{")||matchs("public")
                    || checkNextToken("if", "for", "while", "return")) {
                break;
            }
            advance();
        }
    }

    private void synchronizeToMethodEnd() {
        int braceDepth = 1;
        while (!isAtEnd() && braceDepth > 0) {
            if (matchs("{")) braceDepth++;
            else if (matchs("}")) braceDepth--;
            else advance();
        }

    }

    // 解析语句（if、for、赋值等）
    private ASTNode parseStatement() {
        if (matchs("}")) return null; // 处理代码块结束

        while (peek().getType().equals("单行注释") || peek().getType().equals("多行注释")) {
            advance(); // 跳过注释 Token
        }

        // 优先级4：局部变量声明
        if (isLocalVariableDeclaration()) {
            return parseVariableDeclaration();
        }

        // 优先级1：控制流语句
        if (matchs("if")){ System.out.println("准备解析 if 语句，当前 token: " + peek().getValue());
        return parseIf();}
        if (matchs("else")) {
            errors.add(new ErrorEntry(peek().getLine(),
                    "语法错误",
                    "未匹配的else语句"));
            throw new ParseError();
        }

        // 优先级2：循环语句
        if (matchs("for")) return parseFor();
        if (matchs("while")) return parseWhile();

        // 优先级3：返回/抛出语句
        if (matchs("return")) return parseReturn();
        if (matchs("throw")) return parseThrow();
        if (matchs("switch")) return parseSwitch();
        if (matchs("try")) return parseTryCatch();
        if(matchs("do")) return parseDoWhile();
        if(matchs("break")||matchs("continue")) return parseFlowControl();

      //  if(matchs("//"))currentPos++ ;



        // 最后处理表达式语句
        return parseExpressionStatement();
    }


    // 解析 if 语句：if (condition) { ... } else { ... }
    private IfStatement parseIf() {
        System.out.println("开始解析 if 语句，当前 token: " + peek().getValue());
        int line = consumes("(", "Expect '(' after 'if'").getLine();
        ASTNode condition = parseExpression();
        consumes(")", "Expect ')' after condition");

        System.out.println("条件解析完成");
        BlockStatement thenBlock = parseBlock();

        // 处理else逻辑
        BlockStatement elseBlock = null;
        if (matchs("else")) {
            if (checkValue("if")) {
                // 处理else if结构
                elseBlock = new BlockStatement(peek().getLine(),
                        Collections.singletonList(parseIf()));
            } else {
                elseBlock = parseBlock();
            }
        }

        System.out.println("if语句解析完成");
        return new IfStatement(line, condition, thenBlock, elseBlock);
    }


    private void synchronizeToMemberEnd() {
        while (!isAtEnd()) {
            // 类成员的结束边界
            if (matchs(";", "}", "public", "private", "protected")) break;

            // 方法特征检测
            if (checkType(TYPE_KEYWORD) && checkNextToken("class", "interface")) break;

            advance();
        }
    }



    // 解析 for 循环：for (init; condition; update) { ... }
    private ForStatement parseFor() {

        System.out.println("解析 for 循环...");
        int line = consumes("(", "Expect '(' after 'for'").getLine();


        // 解析初始化部分
        ASTNode init = null;
        if (!checkValue(";")) {
            if (isTypeKeyword(peek().getValue())) {
                // 如果是类型关键字，说明是局部变量声明
                init = parseVariableDeclaration(); // 已经消费了分号
            } else {
                // 否则认为是一个普通表达式
                init = parseExpression();
                consumes(";", "Expect ';' after init"); // 这里才消费分号
            }
        } else {
            // 空初始化，直接消费 ';'
            advance();
        }

        // 解析条件表达式
        ASTNode condition = checkValue(";") ?
                new LiteralExpr(peek().getLine(), "true") : parseExpression();
        consumes(";", "Expect ';' after condition");

        // 解析更新表达式
        ASTNode update = checkValue(")") ?
                null : parseExpression();
        consumes(")", "Expect ')' after update");


        BlockStatement body = parseBlock();
        System.out.println("for 循环解析完成");

        return new ForStatement(line, init, condition, update, body);
    }



    // 解析集合初始化：List<String> list = new ArrayList<>();
    private CollectionInitializer parseCollection() {
        System.out.println("解析集合初始化...");
        int line = previous().getLine();
        String type = consume("标识符", "Expect collection type").getValue();
        consumes("(", "Expect '(' after new");
        List<ASTNode> elements = new ArrayList<>();
        while (!matchs(")") && !isAtEnd()) {
            elements.add(parseExpression());
            if (!matchs(",")) break;
        }
        consumes(")", "Expect ')' after elements");
        System.out.println("集合初始化解析完成: " + type);
        return new CollectionInitializer(line, type, elements);
    }


    // 解析变量声明语句：int x = 10;
    private ASTNode parseVariableDeclaration() {
        System.out.println("解析变量声明语句...");
        // 使用 consumeType 方法解析变量类型
        String type = consumeType();
        String name = consume("标识符", "Expect variable name").getValue();
        ASTNode initializer = null;
        if (matchs("=")) {
            if (checkValue("{")) { // 数组初始化
                advance(); // 关键！消费 '{' 进入数组初始化
                initializer = parseArrayInitializer(name); // 传入当前变量名
            } else {
                initializer = parseExpression();
            }
        }
        consumes(";", "Expect ';' after variable declaration");
        System.out.println("变量声明语句解析完成");
        return new VariableDeclaration(previous().getLine(), type, name, initializer);
    }

    // 判断是否为类型关键字
    private boolean isTypeKeyword(String value) {
        return Set.of("int", "long", "short", "byte", "char", "float", "double", "boolean", "String").contains(value);
    }



    private Token consumes(String value, String errorMessage) {
        if (checkValue(value)) {
            System.out.println("消费 Token: " + peek().getValue());
            return advance();
        }
        System.out.println("语法错误: " + errorMessage+peek().getValue());
        errors.add(new ErrorEntry(peek().getLine(), "语法错误", errorMessage));
        throw new ParseError();
    }

    private boolean match(String... types) {
        for (String type : types) {
            if (check(type)) {
                System.out.println("匹配到 Token: " + type);
                advance();
                return true;
            }
        }
        return false;
    }


    private boolean matchs(String... values) {
        for (String value : values ) {
            if (!isAtEnd() && peek().getValue().equals(value)) {
                System.out.println("匹配到 Token: " + value);
                advance();
                return true;
            }
        }
        return false;
    }

    private boolean check(String type) {
        return !isAtEnd() && peek().getType().equals(type);
    }

    private Token advance() {
        if (!isAtEnd()) currentPos++;
        return previous();
    }

    private Token peek() {
        return tokens.get(currentPos);
    }

    private Token previous() {
        return tokens.get(currentPos - 1);
    }

    private boolean isAtEnd() {
        return currentPos >= tokens.size();
    }




    private ASTNode parseExpressionStatement() {
        System.out.println("解析表达式语句...");
        ASTNode expr = parseExpression();
        if (expr != null) {
            generateExpressionCode(expr);
        }
        if (!matchs(";")) {
            errors.add(new ErrorEntry(peek().getLine(), "语法错误", "缺少分号"));
            synchronizeToStatementEnd();
        }
        System.out.println("表达式语句解析完成");
        return expr;
    }



    public List<ErrorEntry> getErrors() {
        return errors;
    }

    /**
     * 解析带包名的限定符名称 (如 java.util.List)
     * @return 完整限定名 (java.util.List)
     */
    private String parseQualifiedName() {
        StringBuilder qualifiedName = new StringBuilder();

        // 消费第一个标识符
        qualifiedName.append(consume(TYPE_IDENTIFIER, "Expect identifier").getValue());

        // 循环处理后续的点分隔部分
        while (matchs(".")) {
            qualifiedName.append(".");
            qualifiedName.append(consume(TYPE_IDENTIFIER, "Expect identifier after '.'").getValue());
        }

        return qualifiedName.toString();
    }


    // 辅助方法：判断是否为关键字
    private boolean isKeyword(Token token, String keyword) {
        return token.getValue().equals(keyword) && token.getType().equals(TYPE_KEYWORD);
    }

    // 辅助方法：消费指定值的 Token
    private Token consumeValue(String value, String errorMessage) {
        if (checkValue(value)) {
            System.out.println("消费 Token 值: " + value);
            return advance();
        }
        errors.add(new ErrorEntry(peek().getLine(), "语法错误", errorMessage + ", found: " + peek().getValue()));
        throw new ParseError();
    }

    // 辅助方法：消费指定类型的 Token
    private Token consume(String type, String errorMessage) {
        if (checkType(type)) {
            System.out.println("消费 Token 类型: " + type);
            return advance();
        }
        errors.add(new ErrorEntry(peek().getLine(), "语法错误", errorMessage + ", found type: " + peek().getType()));
        throw new ParseError();
    }


    // 辅助方法：检查当前 Token 类型
    private boolean checkType(String type) {
        return !isAtEnd() && peek().getType().equals(type);
    }

    // 辅助方法：检查当前 Token 值
    private boolean checkValue(String value) {
        return !isAtEnd() && peek().getValue().equals(value);
    }







//     解析字段声明
    private FieldDeclaration parseField(List<String> modifiers) {
        System.out.println("解析字段声明...");
        StringBuilder type = new StringBuilder();
        String name = null;
        Token token = peek();
        if ("this".equals(token.getValue())) {
            type.append(advance().getValue()); // 消费 this 关键字
            // 检查是否有后续的 . 和标识符（如 this.InnerClass）
            if (matchs(".")) {
                type.append(".");
                if (checkType("标识符")) {

                    type.append(consume("标识符", "Expect identifier after 'this.'").getValue());
                } else {
                    errors.add(new ErrorEntry(peek().getLine(), "语法错误", "Expect identifier after 'this.'"));
                    throw new ParseError();
                }
            }
        }else {
            // 1. 解析字段类型（支持泛型/数组）
            type = new StringBuilder(consumeType());
            System.out.println("字段类型: " + type);
            // 2. 解析字段名
            name = consume("标识符", "Expect field name").getValue();
            System.out.println("字段名称: " + name);
        }
        // 3. 处理初始化表达式（可选）
        ASTNode initializer = null;
        if (matchs("=")) {
            System.out.println("解析初始化表达式...");
            initializer = parseExpression();
        }
        SymbolType symbol = new SymbolType();
        symbol.setName(name);
        symbol.setType("变量");
        symbol.setLine(previous().getLine());
        Stack<String> scopeCopy = new Stack<>();
        scopeCopy.addAll(this.scope);
        symbol.setScope(scopeCopy);
        symbol.setModifier(type.toString());

        // 生成字段声明四元式
        String modStr = String.join(",", modifiers);
        emitQuadruple("FIELD_DECL", name, type.toString(), modStr);


        if (initializer != null) {
            if (initializer instanceof LiteralExpr) {
                symbol.setValue(((LiteralExpr) initializer).getValue());
            }
        }
        symbols.put(name, symbol);
        consumes(";", "Expect ';' after field declaration");
        return new FieldDeclaration(previous().getLine(), modifiers, type.toString(), name, initializer);
    }
    private String consumeType() {
        StringBuilder type = new StringBuilder();
        // 解析基础类型
        if (isPrimitiveType(peek())) {
            type.append(advance().getValue());
        } else {
            type.append(parseQualifiedName());
        }

        // 处理泛型
        if (matchs("<")) {
            type.append("<");
            do {
                type.append(consumeType());
            } while (matchs(","));
            consumes(">", "Expect '>'");
            type.append(">");
        }

        // 处理数组
        while (matchs("[")) {
            consumes("]", "Expect ']'");
            type.append("[]");
        }
        return type.toString();
    }

    // 判断基础类型关键字
    private boolean isPrimitiveType(Token token) {
        return token.getType().equals(TYPE_KEYWORD) &&
                Set.of("void", "int", "boolean", "char", "byte",
                        "short", "long", "float", "double").contains(token.getValue());
    }

    private boolean isModifier(Token token) {
        if (token == null) return false;
        String value = token.getValue();
        return token.getType().equals("关键字") &&
                Set.of("public", "private", "protected", "static", "final").contains(value);
    }

    private List<String> parseModifiers() {
        List<String> modifiers = new ArrayList<>();
        while (isModifier(peek())) {
            System.out.println("解析修饰符: " + peek().getValue());
            modifiers.add(advance().getValue());
        }
        return modifiers;
    }

    private boolean checkNextToken(String... expectedValues) {
        if (currentPos + 1 >= tokens.size()) return false;
        Token nextToken = tokens.get(currentPos + 1);
        return Arrays.asList(expectedValues).contains(nextToken.getValue());
    }




    // 解析 while 循环：while (condition) { ... }
    private WhileStatement parseWhile() {
        System.out.println("解析 while 循环..."+peek().getValue());
        int line = peek().getLine();
        consumes("(", "Expect '(' after 'while'");
        ASTNode condition = parseExpression();
        String condResult = generateExpressionCode(condition);
        consumes(")", "Expect ')' after condition");

        // 生成标签
        String loopStartLabel = genLabel("WHILE_START");
        String loopBodyLabel = genLabel("WHILE_BODY");
        String loopEndLabel = genLabel("WHILE_END");

        // 跳转到条件判断处
        emitQuadruple("JMP", null, null, loopStartLabel);

        // 插入循环体标签
        emitQuadruple("LABEL", loopBodyLabel, null, null);

        BlockStatement body = parseBlock();

        // 跳转到条件判断处
        emitQuadruple("JMP", null, null, loopStartLabel);

        // 插入条件判断标签
        emitQuadruple("LABEL", loopStartLabel, null, null);

        // 如果条件为假，跳转到循环结束处
        emitQuadruple("JZ", condResult, null, loopEndLabel);

        // 插入循环结束标签
        emitQuadruple("LABEL", loopEndLabel, null, null);

        System.out.println("while 循环解析完成");
        return new WhileStatement(line, condition, body);
    }




    private ReturnStatement parseReturn() {
        int line = previous().getLine();
        ASTNode value = null;
        if (!checkValue(";")) {
            value = parseExpression();
        }
        consumes(";", "Expect ';' after return");
        return new ReturnStatement(line, value);
    }

    // Do-While循环
    private DoWhileStatement parseDoWhile() {
        System.out.println("解析 do-while 循环...");
        int line = peek().getLine();

        // 插入循环体开始标签
        String loopBodyLabel = genLabel("DO_WHILE_BODY");
        emitQuadruple("LABEL", loopBodyLabel, null, null);

        BlockStatement body = parseBlock();

        consumes("while", "Expect 'while' after do-while body");
        consumes("(", "Expect '(' after while");
        ASTNode condition = parseExpression();
        String condResult = generateExpressionCode(condition);
        consumes(")", "Expect ')' after condition");

        // 生成循环结束标签
        String loopEndLabel = genLabel("DO_WHILE_END");

        // 如果条件为假，跳转到循环结束处
        emitQuadruple("JZ", condResult, null, loopEndLabel);

        // 跳转到循环体开始处
        emitQuadruple("JMP", null, null, loopBodyLabel);

        // 插入循环结束标签
        emitQuadruple("LABEL", loopEndLabel, null, null);

        consumes(";", "Expect ';' after do-while");
        System.out.println("do-while 循环解析完成");
        return new DoWhileStatement(line, body, condition);
    }

    // Switch语句
    private SwitchStatement parseSwitch() {
        System.out.println("解析 switch 语句...");
        int line =peek().getLine();
        consumes("(", "Expect '(' after switch");
        ASTNode expr = parseExpression();
        String exprResult = generateExpressionCode(expr);
        consumes(")", "Expect ')' after expression");
        consumes("{", "Expect '{' in switch");

        List<CaseStatement> cases = new ArrayList<>();
        List<String> caseLabels = new ArrayList<>();
        String defaultLabel = null;
        String endLabel = genLabel("SWITCH_END");

        while (!matchs("}") && !isAtEnd()) {
            if (matchs("case")) {
                ASTNode caseExpr = parseExpression();
                String caseExprResult = generateExpressionCode(caseExpr);
                String caseLabel = genLabel("CASE");
                caseLabels.add(caseLabel);
                consumes(":", "Expect ':' after case");
                // 如果表达式结果等于 case 值，跳转到 case 标签
                emitQuadruple("JE", exprResult, caseExprResult, caseLabel);
                cases.add(new CaseStatement(caseExpr, parseCaseBody()));
            } else if (matchs("default")) {
                defaultLabel = genLabel("DEFAULT");
                consumes(":", "Expect ':' after default");
                cases.add(new DefaultCaseStatement(parseCaseBody()));
            } else {
                throw error(peek(), "Unexpected token in switch");
            }
        }

        // 无条件跳转到结束标签
        emitQuadruple("JMP", null, null, endLabel);

        // 插入 case 标签和代码
        for (int i = 0; i < cases.size(); i++) {
            if (cases.get(i) instanceof DefaultCaseStatement) {
                emitQuadruple("LABEL", defaultLabel, null, null);
            } else {
                emitQuadruple("LABEL", caseLabels.get(i), null, null);
            }
            // 每个 case 结束后跳转到结束标签
            emitQuadruple("JMP", null, null, endLabel);
        }

        // 插入结束标签
        emitQuadruple("LABEL", endLabel, null, null);

        consumes("}", "Expect '}' after switch");
        System.out.println("switch 语句解析完成");
        return new SwitchStatement(line, expr, cases);
    }

    // Try-Catch语句
    private TryCatchStatement parseTryCatch() {
        int line = peek().getLine();
        BlockStatement tryBlock = parseBlock();

        List<CatchClause> catches = new ArrayList<>();
        BlockStatement finallyBlock = null;
        System.out.println("解析catch   "+peek().getValue());
        while (matchs("catch")) {
            consumes("(", "Expect '(' after catch");
            String type = consumeType();
            String varName = consume("标识符", "Expect variable name").getValue();
            consumes(")", "Expect ')'");

            // 进入catch参数作用域
            scope.push("catch:" + varName);
            // 记录catch参数到符号表
            SymbolType catchParamSymbol = new SymbolType();
            catchParamSymbol.setName(varName);
            catchParamSymbol.setType("parameter");
            catchParamSymbol.setDataType(type);
            catchParamSymbol.setLine(previous().getLine());
            Stack<String> scopeCopy = new Stack<>();
            scopeCopy.addAll(scope);
            catchParamSymbol.setScope(scopeCopy);
            symbols.put(varName, catchParamSymbol);

            BlockStatement catchBody = parseBlock();
            scope.pop(); // 退出catch参数作用域

            catches.add(new CatchClause(type, varName, catchBody));
        }

        if (matchs("finally")) {
            finallyBlock = parseBlock();
        }

        return new TryCatchStatement(line, tryBlock, catches, finallyBlock);
    }

    // Throw语句
    private ThrowStatement parseThrow() {
        int line = peek().getLine();
        ASTNode expr = parseExpression();
        consumes(";", "Expect ';' after throw");
        return new ThrowStatement(line, expr);
    }



    // Break/Continue
    private FlowControlStatement parseFlowControl() {
        Token token = previous();
        String label = null;
        if (checkType("标识符")) {
            label = advance().getValue();
        }
        consumes(";", "Expect ';' after " + token.getValue());
        return new FlowControlStatement(
                token.getLine(),
                token.getValue(),
                label
        );
    }

    private List<ASTNode> parseCaseBody() {
        List<ASTNode> body = new ArrayList<>();
        while (!checkValue("case") && !checkValue("default") && !checkValue("}")) {
            body.add(parseStatement());
        }
        return body;
    }
    private ParseError error(Token token, String message) {
        errors.add(new ErrorEntry(token.getLine(), "语法错误", message));
        return new ParseError();
    }

    public Map<String, SymbolType> getSymbols() {
        return symbols;
    }

    private void emitQuadruple(String op, String arg1, String arg2, String result) {
        quadruples.add(new Quadruple(op, arg1, arg2, result));
        System.out.printf("[Q] %s %s %s %s\n", op, arg1, arg2, result);

    }




    // 新增方法：精确判断方法声明
    private boolean isMethodDeclaration(List<String> modifiers) {
        // 方法必须满足以下条件：
        // 1. 包含类型声明或void
        // 2. 后面紧跟形参列表
        int lookahead = currentPos;
        try {
            // 跳过可能存在的注解
            while (tokens.get(lookahead).getValue().equals("@")) {
                lookahead += 2; // 跳过注解名称
            }

            // 检查返回类型部分
            if (!isValidType(tokens.get(lookahead))) return false;
            lookahead++;

            // 检查方法名
            if (!tokens.get(lookahead).getType().equals(TYPE_IDENTIFIER)) return false;
            lookahead++;

            // 必须紧跟形参列表
            return tokens.get(lookahead).getValue().equals("(");
        } catch (IndexOutOfBoundsException e) {
            return false;
        }
    }

    private String genTempVar() {
        String scopePrefix = scopeStack.isEmpty() ? "" : scopeStack.peek();
        int count = ++globalTempCounter;
        return "t_" + scopePrefix + "_" + count;
    }





    private String genLabel(String prefix) {
        return prefix + "_" + (++labelCounter);
    }

    private String generateExpressionCode(ASTNode expr) {
        if (expr == null) {
            throw new IllegalArgumentException("表达式节点不能为null");
        }

        //=== 1. 处理二元运算 ===//
        if (expr instanceof BinaryExpr) {
            BinaryExpr be = (BinaryExpr) expr;
            String op = be.getOperator().getValue();

            // 新增：处理比较运算符
            if ("<".equals(op)) {
                String left = generateExpressionCode(be.getLeft());
                String right = generateExpressionCode(be.getRight());
                String temp = genTempVar();
                emitQuadruple("LT", left, right, temp); // Less Than
                String boolTemp = genTempVar();
                emitQuadruple("TO_BOOL", temp, null, boolTemp);
                return boolTemp;
            }

            if (">".equals(op)) {
                String left = generateExpressionCode(be.getLeft());
                String right = generateExpressionCode(be.getRight());
                String temp = genTempVar();
                emitQuadruple("GT", left, right, temp); // Greater Than
                String boolTemp = genTempVar();
                emitQuadruple("TO_BOOL", temp, null, boolTemp);
                return boolTemp;
            }

            if ("<=".equals(op)) {
                String left = generateExpressionCode(be.getLeft());
                String right = generateExpressionCode(be.getRight());
                String temp = genTempVar();
                emitQuadruple("LE", left, right, temp); // Less or Equal
                String boolTemp = genTempVar();
                emitQuadruple("TO_BOOL", temp, null, boolTemp);
                return boolTemp;
            }

            if (">=".equals(op)) {
                String left = generateExpressionCode(be.getLeft());
                String right = generateExpressionCode(be.getRight());
                String temp = genTempVar();
                emitQuadruple("GE", left, right, temp); // Greater or Equal
                String boolTemp = genTempVar();
                emitQuadruple("TO_BOOL", temp, null, boolTemp);
                return boolTemp;
            }

            if ("==".equals(op)) {
                String left = generateExpressionCode(be.getLeft());
                String right = generateExpressionCode(be.getRight());
                String temp = genTempVar();
                emitQuadruple("EQ", left, right, temp); // Equal
                String boolTemp = genTempVar();
                emitQuadruple("TO_BOOL", temp, null, boolTemp);
                return boolTemp;
            }

            if ("!=".equals(op)) {
                String left = generateExpressionCode(be.getLeft());
                String right = generateExpressionCode(be.getRight());
                String temp = genTempVar();
                emitQuadruple("NE", left, right, temp); // Not Equal
                String boolTemp = genTempVar();
                emitQuadruple("TO_BOOL", temp, null, boolTemp);
                return boolTemp;
            }


            // 处理数组访问（如 arr[i]）
            if ("[]".equals(op)) {
                String arrayRef = generateExpressionCode(be.getLeft());
                String index = generateExpressionCode(be.getRight());
                String temp = genTempVar();
                emitQuadruple("ARRAY_LOAD", arrayRef, index, temp);
                return temp;
            }

            // 处理逻辑运算符 ||、&& 的短路特性
            if (op.equals("||") || op.equals("&&")) {
                return handleShortCircuitOp(be, op);
            }

            // 普通二元运算（算术、比较等）
            String left = generateExpressionCode(be.getLeft());
            String right = generateExpressionCode(be.getRight());
            String temp = genTempVar();
            emitQuadruple(op, left, right, temp);
            return temp;
        }

        //=== 2. 处理字面量 ===//
        if (expr instanceof LiteralExpr) {
            LiteralExpr le = (LiteralExpr) expr;
            printAST(le,0);
            String temp = genTempVar();
            String type = determineLiteralType(le.getValue());
            emitQuadruple("LOAD_" + type.toUpperCase(), le.getValue(), null, temp);
            return temp;
        }

        //=== 3. 处理变量/字段访问 ===//
        if (expr instanceof IdentifierExpr) {
            IdentifierExpr ie = (IdentifierExpr) expr;
            String varName = ie.getIdentifier();
            validateSymbolExists(varName);
            String temp = genTempVar();
            emitQuadruple("LOAD", varName, null, temp);
            return temp;
        }

        //=== 4. 处理方法调用 ===//
        if (expr instanceof MethodCallExpr mce) {
            String caller = generateExpressionCode(mce.getCallee());

            List<String> args = new ArrayList<>();
            for (ASTNode arg : mce.getArguments()) {
                if (arg == null) {
                    System.out.println("No No No No No No No No No No No");
                    throw new ParseError(); // 防御 null 参数
                }
                args.add(generateExpressionCode(arg));
            }

            String temp = genTempVar();
            emitQuadruple("CALL", caller, String.join(",", args), temp);
            printAST(mce, 0);
            return temp;
        }


        //=== 5. 处理一元运算 ===//
        if (expr instanceof UnaryExpr) {
            UnaryExpr ue = (UnaryExpr) expr;
            String operand = generateExpressionCode(ue.getRight());
            String temp = genTempVar();
            emitQuadruple(ue.getOperator().getValue(), operand, null, temp);
            return temp;
        }

        //=== 6. 条件表达式 a ? b : c ===//
        if (expr instanceof ConditionalExpr) {
            return generateConditionalExpr((ConditionalExpr) expr);
        }

        //=== 7. 括号表达式 ===//
        if (expr instanceof ParenthesizedExpr) {
            return generateParenthesizedExpr((ParenthesizedExpr) expr);
        }

        //=== 8. 数组访问表达式 ===//
        if (expr instanceof ArrayAccessExpr) {
            return generateArrayAccess((ArrayAccessExpr) expr);
        }

        //=== 9. 后缀表达式（如 i++） ===//
        if (expr instanceof PostfixExpr) {
            return generatePostfixExpr((PostfixExpr) expr);
        }

        //=== 10. 数组初始化器 ===//
        if (expr instanceof ArrayInitializerExpr) {
            return generateArrayInitializer((ArrayInitializerExpr) expr);
        }

        //=== 11. 新建数组表达式 ===//
        if (expr instanceof NewArrayExpr) {
            return generateNewArray((NewArrayExpr) expr);
        }

        //=== 12. 创建新对象表达式 ===//
        if (expr instanceof NewObjectExpr) {
            return generateNewObject((NewObjectExpr) expr);
        }

        //=== 13. 成员访问表达式 ===//
        if (expr instanceof MemberAccessExpr) {
            return generateMemberAccess((MemberAccessExpr) expr);
        }

        throw new ParseError();
    }

    //=== 辅助方法：处理短路逻辑 ===//
    private String handleShortCircuitOp(BinaryExpr be, String op) {
        String left = generateExpressionCode(be.getLeft());
        String temp = genTempVar();
        String shortCircuitLabel = genLabel(op.equals("||") ? "OR_TRUE" : "AND_FALSE");
        String endLabel = genLabel("LOGIC_END");

        // 生成条件跳转
        if (op.equals("||")) {
            emitQuadruple("JNZ", left, null, shortCircuitLabel); // 左为真则跳转
        } else {
            emitQuadruple("JZ", left, null, shortCircuitLabel);  // 左为假则跳转
        }

        // 计算右表达式
        String right = generateExpressionCode(be.getRight());
        emitQuadruple("MOV", right, null, temp);
        emitQuadruple("JMP", null, null, endLabel);

        // 短路分支
        emitQuadruple("LABEL", shortCircuitLabel, null, null);
        emitQuadruple("MOV", op.equals("||") ? "1" : "0", null, temp);
        emitQuadruple("LABEL", endLabel, null, null);
        return temp;
    }


    /**
     * 处理位运算符（& 和 |）的非短路逻辑
     */
    private String handleBitwiseOp(BinaryExpr be, String op) {
        String left = generateExpressionCode(be.getLeft());
        String right = generateExpressionCode(be.getRight());
        String temp = genTempVar();
        emitQuadruple(op.equals("&") ? "BIT_AND" : "BIT_OR", left, right, temp);
        return temp;
    }

    /**
     * 判断变量是否为数组类型
     */
    private boolean isArrayType(String varName) {
        SymbolType symbol = symbols.get(varName);
        return symbol != null && symbol.getDataType().endsWith("[]");
    }



    private String determineLiteralType(String value) {
        if (value.matches("\\d+")) return "INT";
        if (value.matches("\\d+\\.\\d+")) return "FLOAT";
        if (value.startsWith("\"") && value.endsWith("\"")) return "STRING";
        if ("true".equals(value) || "false".equals(value)) return "BOOL";
        if ("null".equals(value)) return "NULL";
        throw new ParseError();
    }


    private void validateSymbolExists(String name) {
        if (!symbols.containsKey(name)) {
            throw new ParseError();
        }
    }

    // 判断字段声明（新增方法）
    private boolean isFieldDeclaration(List<String> modifiers) {
        try {
            int lookahead = currentPos;

            // 跳过注解
            while (tokens.get(lookahead).getValue().equals("@")) {
                lookahead += 2;
            }

            // 必须包含有效类型
            if (!isValidType(tokens.get(lookahead))) return false;

            // 类型后必须跟标识符
            if (!tokens.get(lookahead+1).getType().equals(TYPE_IDENTIFIER)) return false;

            // 字段特征：后面是分号或等号
            return Arrays.asList(";", "=").contains(tokens.get(lookahead+2).getValue());
        } catch (IndexOutOfBoundsException e) {
            return false;
        }
    }



    private boolean isLocalVariableDeclaration() {
        try {
            int lookahead = currentPos;

            // 检查类型部分
            if (!isValidType(tokens.get(lookahead))) return false;
            lookahead++;

            // 跳过可能的数组维度（如 [][][]）
            while (lookahead < tokens.size() &&
                    tokens.get(lookahead).getValue().equals("[") &&
                    lookahead + 1 < tokens.size() &&
                    tokens.get(lookahead + 1).getValue().equals("]")) {
                lookahead += 2; // 跳过 [ ]
            }

            // 变量名必须是标识符
            if (!tokens.get(lookahead).getType().equals(TYPE_IDENTIFIER)) return false;
            lookahead++;

            // 判断是否是赋值或结束
            return lookahead < tokens.size() &&
                    Arrays.asList("=", ";").contains(tokens.get(lookahead).getValue());

        } catch (IndexOutOfBoundsException e) {
            return false;
        }
    }



    // 类型有效性校验（新增方法）
    private boolean isValidTypeStart(Token token) {
        return token.getType().equals(TYPE_KEYWORD) ||
                token.getType().equals(TYPE_IDENTIFIER) ||
                token.getValue().equals("void");
    }

    private boolean isValidType(Token token) {
        // 支持的类型结构包括：
        // 1. 基础类型（int, String等）
        // 2. 数组类型（int[]）
        // 3. 泛型类型（List<String>）
        // 4. 嵌套类型（Map.Entry）
        int lookahead = currentPos;
        try {
            while (true) {
                Token t = tokens.get(lookahead);

                if (t.getValue().equals("<")) {
                    int bracketDepth = 1;
                    while (bracketDepth > 0) {
                        lookahead++;
                        Token inner = tokens.get(lookahead);
                        if (inner.getValue().equals("<")) bracketDepth++;
                        if (inner.getValue().equals(">")) bracketDepth--;
                    }
                }

                if (t.getValue().equals("[")) {
                    if (!tokens.get(lookahead+1).getValue().equals("]")) return false;
                    lookahead++;
                }

                if (t.getValue().equals(".")) {
                    if (!tokens.get(lookahead+1).getType().equals(TYPE_IDENTIFIER)) return false;
                    lookahead++;
                }

                if (!isValidTypeStart(t) &&
                        !t.getValue().matches("[<>.\\[\\]]")) {
                    return false;
                }

                lookahead++;

                // 类型声明结束条件
                if (tokens.get(lookahead).getType().equals(TYPE_IDENTIFIER) ||
                        tokens.get(lookahead).getValue().equals(";") ||
                        tokens.get(lookahead).getValue().equals("=")) {
                    break;
                }
            }
            return true;
        } catch (IndexOutOfBoundsException e) {
            return false;
        }
    }


    public List<Quadruple> getQuadruples() {
        return quadruples;
    }

    /**
     * 生成语句的四元式代码
     * @param stmt 语句AST节点
     */
    private void generateStatementCode(ASTNode stmt) {
        System.out.println("正在解析方法体");
        if (stmt == null) return;

        // 调试输出
        if (DEBUG_MODE) {
            System.out.printf("[Line %d] 生成语句代码: %s\n",
                    stmt.getLine(), stmt.getClass().getSimpleName());
        }

        // 根据语句类型分发处理
        if (stmt instanceof VariableDeclaration) {
            generateVarDeclCode((VariableDeclaration) stmt);
        }
        else if (stmt instanceof IfStatement) {
            generateIfCode((IfStatement) stmt);
        }
        else if (stmt instanceof ReturnStatement) {
            generateReturnCode((ReturnStatement) stmt);
        }
        else if (stmt instanceof BlockStatement) {
            generateBlockCode((BlockStatement) stmt);
        }
        else if (stmt instanceof ForStatement) {
            generateForCode((ForStatement) stmt);
        }
        else if (stmt instanceof ExpressionStatement) {
            ExpressionStatement es = (ExpressionStatement) stmt;
            generateExpressionCode(es.getExpression()); // 显式调用生成
        }
        else if (stmt instanceof MethodCallExpr) {
            // 新增对 MethodCallExpr 的支持
            MethodCallExpr mce = (MethodCallExpr) stmt;
            generateExpressionCode(mce); // 复用表达式生成逻辑即可
        }
        else if (stmt instanceof WhileStatement) {
            generateWhileCode((WhileStatement) stmt);
        }
        else if (stmt instanceof DoWhileStatement) {
            generateDoWhileCode((DoWhileStatement) stmt);
        }
        else if (stmt instanceof SwitchStatement) {
            generateSwitchCode((SwitchStatement) stmt);
        }
        else if (stmt instanceof TryCatchStatement) {
            generateTryCatchCode((TryCatchStatement) stmt);
        }
        else if (stmt instanceof ThrowStatement) {
            generateThrowCode((ThrowStatement) stmt);
        }
        else if (stmt instanceof FlowControlStatement) {
            generateFlowControlCode((FlowControlStatement) stmt);
        }
        else if (stmt instanceof CaseStatement || stmt instanceof DefaultCaseStatement) {
            generateCaseCode((CaseStatement) stmt);
        }
        else if (stmt instanceof ArrayInitializerExpr) {
            generateArrayInitializer((ArrayInitializerExpr) stmt);
        }

        // 其他语句类型...
        else {
            System.out.println("未知语句类型: " + stmt.getClass().getSimpleName());
            throw new ParseError();
        }
    }

    private void generateVarDeclCode(VariableDeclaration decl) {
        String varName = decl.getVariableName();
        SymbolType symbol = new SymbolType();
        symbol.setName(varName);
        symbol.setType("local");
        symbol.setDataType(decl.getVarType());
        symbols.put(varName, symbol);

        // 处理初始化表达式
        if (decl.getInitializer() != null) {
            String initValue = generateExpressionCode(decl.getInitializer());
            emitQuadruple("STORE", initValue, null, varName);
        }
    }

    private void generateIfCode(IfStatement ifStmt) {
        printAST(ifStmt, 0);
        // 生成条件表达式代码
        String condResult = generateExpressionCode(ifStmt.getCondition());

        // 生成标签（带作用域前缀保证唯一性）
        String currentScope = String.join("_", scopeStack);
        String elseLabel = genLabel(currentScope + "_ELSE");
        String endLabel = genLabel(currentScope + "_IF_END");

        // 条件跳转四元式
        emitQuadruple("JZ", condResult, null, elseLabel);

        // 生成then分支代码
        generateStatementCode(ifStmt.getThenBranch());
        emitQuadruple("JMP", null, null, endLabel);

        // 生成else分支标签和代码
        emitQuadruple("LABEL", elseLabel, null, null);
        if (ifStmt.getElseBranch() != null) {
            generateStatementCode(ifStmt.getElseBranch());
        }

        // 生成结束标签
        emitQuadruple("LABEL", endLabel, null, null);
    }

    private void generateBlockCode(BlockStatement block) {
        // 进入新的块作用域

        String scopeName = "block_" + block.getLine();
        scopeStack.push(scopeName);


        // 生成块内代码
        for (ASTNode stmt : block.getStatements()) {
            generateStatementCode(stmt);
        }

        // 退出作用域，弹出计数器

        scopeStack.pop();

    }


    private void generateReturnCode(ReturnStatement retStmt) {
        if (retStmt.getValue() != null) {
            String retValue = generateExpressionCode(retStmt.getValue());
            emitQuadruple("RETURN", retValue, null, null);
        } else {
            emitQuadruple("RETURN", null, null, null);
        }
    }


    private void generateForCode(ForStatement forStmt) {

        scopeStack.push("for_" + forStmt.getLine());

        try{
        // 生成初始化代码
        if (forStmt.getInit() != null) {
            generateStatementCode(forStmt.getInit());
        }

        // 生成标签（带作用域前缀）
        String currentScope = String.join("_", scopeStack);
        String loopStartLabel = genLabel(currentScope + "_LOOP_START");
        String loopBodyLabel = genLabel(currentScope + "_LOOP_BODY");
        String loopEndLabel = genLabel(currentScope + "_LOOP_END");

        // 跳转到条件判断
        emitQuadruple("JMP", null, null, loopStartLabel);

        // 生成循环体标签
        emitQuadruple("LABEL", loopBodyLabel, null, null);

        // 生成循环体代码
        generateStatementCode(forStmt.getBody());

        // 生成更新表达式代码
        if (forStmt.getUpdate() != null) {
            generateExpressionCode(forStmt.getUpdate());
        }

        // 跳回条件判断
        emitQuadruple("JMP", null, null, loopStartLabel);

        // 条件判断标签
        emitQuadruple("LABEL", loopStartLabel, null, null);

        // 生成条件判断代码
        String condResult = generateExpressionCode(forStmt.getCondition());
        emitQuadruple("JZ", condResult, null, loopEndLabel);

        // 跳转到循环体
        emitQuadruple("JMP", null, null, loopBodyLabel);

        // 生成循环结束标签
        emitQuadruple("LABEL", loopEndLabel, null, null);
        }
        finally {

            scopeStack.pop();
        }
    }

    public void printAST(ASTNode node, int indent) {
        if (node == null) return;

        // 打印当前节点信息
        System.out.println(node.toString(indent));

        // 递归打印子节点
        for (int i = 0; i < node.getChildCount(); i++) {
            try {
                ASTNode child = node.getChild(i);
                printAST(child, indent + 2); // 缩进+2
            } catch (IndexOutOfBoundsException e) {
                System.err.println(" ".repeat(indent) + "Error: Invalid child index " + i);
            }
        }
    }


    private String generateConditionalExpr(ConditionalExpr expr) {
        String cond = generateExpressionCode(expr.getCondition());
        String trueLabel = genLabel("COND_TRUE");
        String falseLabel = genLabel("COND_FALSE");
        String endLabel = genLabel("COND_END");
        String temp = genTempVar();

        // 条件跳转
        emitQuadruple("JNZ", cond, null, trueLabel);
        emitQuadruple("JMP", null, null, falseLabel);

        // true分支
        emitQuadruple("LABEL", trueLabel, null, null);
        String trueVal = generateExpressionCode(expr.getThenBranch());
        emitQuadruple("MOV", trueVal, null, temp);
        emitQuadruple("JMP", null, null, endLabel);

        // false分支
        emitQuadruple("LABEL", falseLabel, null, null);
        String falseVal = generateExpressionCode(expr.getElseBranch());
        emitQuadruple("MOV", falseVal, null, temp);

        // 结束标签
        emitQuadruple("LABEL", endLabel, null, null);
        return temp;
    }


    private String generateParenthesizedExpr(ParenthesizedExpr expr) {
        // 直接返回内部表达式的计算结果
        return generateExpressionCode(expr.getExpression());
    }


    private String generateArrayAccess(ArrayAccessExpr expr) {
            String arrayRef = generateExpressionCode(expr.getArray());
        System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
            String index = generateExpressionCode(expr.getIndex());
            String temp = genTempVar();
            emitQuadruple("ARRAY_LOAD", arrayRef, index, temp);

            return temp;

    }



    private String generatePostfixExpr(PostfixExpr expr) {
        String operand = generateExpressionCode(expr.getOperand());
        String temp = genTempVar();
        String op = expr.getOperator().getValue();

        // 保存原始值
        emitQuadruple("MOV", operand, null, temp);

        // 生成修改指令
        switch(op) {
            case "++":
                emitQuadruple("ADD", operand, "1", operand);
                break;
            case "--":
                emitQuadruple("SUB", operand, "1", operand);
                break;
        }
        return temp; // 返回原始值
    }



    private String generateArrayInitializer(ArrayInitializerExpr expr) {
        String arrayTemp = genTempVar();
        int size = expr.getElements().size();
        System.out.println("[DEBUG] 正在生成数组初始化器: " + expr.getVariableName() + " 类型: " + expr.getElementType());

        // 创建数组
        emitQuadruple("NEW_ARRAY", expr.getElementType(), String.valueOf(size), arrayTemp);

        // 初始化元素
        for(int i=0; i<size; i++) {
            String element = generateExpressionCode(expr.getElements().get(i));
            emitQuadruple("ARRAY_STORE", arrayTemp, String.valueOf(i), element);
        }
        System.out.println("[DEBUG] 数组初始化完成: " );

        return arrayTemp;
    }


    private String generateNewArray(NewArrayExpr expr) {
        String size = generateExpressionCode(expr.getSize());
        String temp = genTempVar();
        String type = generateExpressionCode(expr.getType());

        // 根据类型创建数组
        emitQuadruple("NEW_ARRAY", type, size, temp);
        return temp;
    }



    private String generateNewObject(NewObjectExpr expr) {
        String temp = genTempVar();
        StringBuilder argsBuilder = new StringBuilder();

        // 处理参数
        for(ASTNode arg : expr.getArguments()) {
            argsBuilder.append(generateExpressionCode(arg)).append(",");
        }
        if(argsBuilder.length() > 0) {
            argsBuilder.setLength(argsBuilder.length()-1);
        }

        // 生成对象创建指令
        emitQuadruple("NEW_OBJ", expr.getType().toString(), argsBuilder.toString(), temp);
        return temp;
    }



    private String generateMemberAccess(MemberAccessExpr expr) {
        String objectRef = generateExpressionCode(expr.getObject());
        String fieldName = ((IdentifierExpr)expr.getMember()).getIdentifier();
        String temp = genTempVar();

        // 生成字段访问指令
        emitQuadruple("FIELD_LOAD", objectRef, fieldName, temp);
        return temp;
    }


    /**
     * 生成 while 循环的四元式代码
     */
    private void generateWhileCode(WhileStatement stmt) {
        String loopStartLabel = genLabel("WHILE_START");
        String loopBodyLabel = genLabel("WHILE_BODY");
        String loopEndLabel = genLabel("WHILE_END");

        // 跳转到条件判断标签
        emitQuadruple("JMP", null, null, loopStartLabel);

        // 插入循环体标签
        emitQuadruple("LABEL", loopBodyLabel, null, null);

        // 生成循环体代码
        generateStatementCode(stmt.getBody());

        // 跳转回条件判断
        emitQuadruple("JMP", null, null, loopStartLabel);

        // 插入条件判断标签
        emitQuadruple("LABEL", loopStartLabel, null, null);

        // 生成条件表达式代码
        String condResult = generateExpressionCode(stmt.getCondition());

        // 条件不满足时跳转出循环
        emitQuadruple("JZ", condResult, null, loopEndLabel);

        // 插入循环结束标签
        emitQuadruple("LABEL", loopEndLabel, null, null);
    }


    /**
     * 生成 do-while 循环的四元式代码
     */
    private void generateDoWhileCode(DoWhileStatement stmt) {
        String loopBodyLabel = genLabel("DO_WHILE_BODY");
        String loopEndLabel = genLabel("DO_WHILE_END");

        // 插入循环体开始标签
        emitQuadruple("LABEL", loopBodyLabel, null, null);

        // 生成循环体代码
        generateStatementCode(stmt.getBody());

        // 生成条件表达式代码
        String condResult = generateExpressionCode(stmt.getCondition());

        // 如果条件为真则继续循环
        emitQuadruple("JNZ", condResult, null, loopBodyLabel);

        // 插入循环结束标签
        emitQuadruple("LABEL", loopEndLabel, null, null);
    }


    /**
     * 生成 try-catch-finally 的四元式代码（简化版）
     */
    private void generateTryCatchCode(TryCatchStatement stmt) {
        String catchLabel = genLabel("CATCH");
        String finallyLabel = genLabel("FINALLY");
        String endLabel = genLabel("TRY_END");

        // 设置异常捕获点
        emitQuadruple("SET_CATCH", catchLabel, null, null);

        // 生成 try 块
        generateStatementCode(stmt.getTryBlock());

        // 清除异常捕获点
        emitQuadruple("CLR_CATCH", null, null, null);

        // 跳过 catch
        emitQuadruple("JMP", null, null, endLabel);

        // 插入 catch 标签
        emitQuadruple("LABEL", catchLabel, null, null);
        for (CatchClause clause : stmt.getCatches()) {
            generateCatchClause(clause);
        }

        // 插入 finally 标签
        if (stmt.getFinallyBlock() != null) {
            emitQuadruple("LABEL", finallyLabel, null, null);
            generateStatementCode(stmt.getFinallyBlock());
        }

        // 插入结束标签
        emitQuadruple("LABEL", endLabel, null, null);
    }

    private void generateCatchClause(CatchClause clause) {
        // 进入 catch 参数作用域
        scope.push("catch:" + clause.getVarName());

        SymbolType catchParamSymbol = new SymbolType();
        catchParamSymbol.setName(clause.getVarName());
        catchParamSymbol.setType("parameter");
        catchParamSymbol.setDataType(clause.getType());
        catchParamSymbol.setLine(currentPos);
        symbols.put(clause.getVarName(), catchParamSymbol);

        // 生成 catch 体代码
        generateStatementCode(clause.getBody());

        // 弹出作用域
        scope.pop();
    }


    /**
     * 生成 throw 表达式的四元式代码
     */
    private void generateThrowCode(ThrowStatement stmt) {
        String exceptionRef = generateExpressionCode(stmt.getException());
        emitQuadruple("THROW", exceptionRef, null, null);
    }


    /**
     * 生成 break/continue 控制流语句的四元式代码
     */
    private void generateFlowControlCode(FlowControlStatement stmt) {
        String targetLabel = findTargetLabel(stmt.getLabel(), stmt.getValue());
        emitQuadruple(stmt.getValue().equals("break") ? "BREAK" : "CONTINUE",
                targetLabel, null, null);
    }

    // 简化版查找目标标签的方法（需要结合当前作用域栈）
    private String findTargetLabel(String label, String controlType) {
        return label != null ? label : genLabel(controlType.toUpperCase());
    }



    /**
     * 生成 case/default 分支的四元式代码
     */
    private void generateCaseCode(CaseStatement stmt) {
        // 直接生成 case 主体代码
        for (ASTNode bodyStmt : stmt.getBody()) {
            generateStatementCode(bodyStmt);
        }
    }


    private void generateSwitchCode(SwitchStatement stmt) {
        // 生成switch表达式的值
        String exprResult = generateExpressionCode(stmt.getExpression());

        // 生成结束标签
        String endLabel = genLabel("SWITCH_END");

        // 存储case的标签和对应的表达式结果
        List<String> caseLabels = new ArrayList<>();
        String defaultLabel = null;

        // 遍历所有cases，生成比较和跳转指令
        for (CaseStatement caseStmt : stmt.getCases()) {
            if (caseStmt instanceof DefaultCaseStatement) {
                // 默认情况，生成对应的标签
                defaultLabel = genLabel("DEFAULT");
                continue;
            }

            // 普通case，生成比较和跳转
            String caseLabel = genLabel("CASE");
            caseLabels.add(caseLabel);

            // 生成case条件的值
            String caseExprResult = generateExpressionCode(caseStmt.getExpression());

            // 生成条件跳转：如果exprResult等于caseExprResult，跳转到caseLabel
            emitQuadruple("JE", exprResult, caseExprResult, caseLabel);
        }

        // 生成默认情况的跳转（如果有的话）
        if (defaultLabel != null) {
            // 没有匹配的case时跳转到default
            emitQuadruple("JMP", null, null, defaultLabel);
        } else {
            // 没有default时直接跳转到结束
            emitQuadruple("JMP", null, null, endLabel);
        }

        // 处理各个case的代码块
        int caseIndex = 0;
        for (CaseStatement caseStmt : stmt.getCases()) {
            String currentLabel;
            if (caseStmt instanceof DefaultCaseStatement) {
                currentLabel = defaultLabel;
            } else {
                currentLabel = caseLabels.get(caseIndex);
                caseIndex++;
            }

            // 生成当前case的标签
            emitQuadruple("LABEL", currentLabel, null, null);

            // 生成case内的语句
            for (ASTNode bodyStmt : caseStmt.getBody()) {
                generateStatementCode(bodyStmt);
            }

            // 生成跳转到结束标签（每个case执行完后跳出switch）
            emitQuadruple("JMP", null, null, endLabel);
        }

        // 生成结束标签
        emitQuadruple("LABEL", endLabel, null, null);
    }


















































    /**
     * 解析表达式（增强版）
     * 支持所有Java表达式类型，包括：
     * - 基本字面量（数字、字符串、布尔值）
     * - 标识符和成员访问（如 System.out.println）
     * - 数组访问（如 arr[i]）
     * - 方法调用（如 method()）
     * - 各种运算符（算术、比较、逻辑、位运算等）
     */
    private ASTNode parseExpression() {
        System.out.println("解析表达式...");
        ASTNode left = parseAssignment();

        if (peek().getType().equals(")") || peek().getType().equals(";")) {
            System.out.println("表达式结束: " + previous().getValue());
        }

        return left;
    }


    /**
     * 解析赋值表达式
     * 支持简单赋值(=)和复合赋值(+=, -=, *=, /=, %=)
     */
    private ASTNode parseAssignment() {

        ASTNode expr = parseConditional();

        if (matchs("=", "+=", "-=", "*=", "/=", "%=")) {
            Token operator = previous();
            ASTNode right = parseAssignment(); // 右结合

            // 特别处理数组初始化的情况
            if (checkValue("{")) {
                List<ASTNode> elements = new ArrayList<>();
                advance(); // 消费 "{"

                if (!checkValue("}")) {
                    do {
                        elements.add(parseExpression());
                    } while (matchs(","));
                }

                advance(); // 消费 "}"

                // 确保 expr 是 IdentifierExpr
                if (expr instanceof IdentifierExpr) {
                    return new ArrayInitializerExpr(expr.getLine(),
                            ((IdentifierExpr) expr).getIdentifier(),
                            elements);
                } else {
                    System.out.println("数组初始化表达式的左值必须是标识符");
                    throw new ParseError();
                }
            }

            return new BinaryExpr(expr.getLine(), expr, operator, right);
        }

        return expr;
    }


    /**
     * 解析条件表达式 a ? b : c
     * 实现右结合特性
     */
    private ASTNode parseConditional() {
        ASTNode condition = parseLogicalOr();

        if (matchs("?")) {
            ASTNode thenBranch = parseExpression();
            consume(":", "Expect ':' in conditional expression");
            ASTNode elseBranch = parseAssignment(); // 条件表达式的右侧是赋值表达式

            return new ConditionalExpr(condition.getLine(), condition, thenBranch, elseBranch);
        }

        return condition;
    }



    /**
     * 解析逻辑或表达式 a || b
     * 实现短路求值特性
     */
    private ASTNode parseLogicalOr() {
        ASTNode left = parseLogicalAnd();

        while (matchs("||")) {
            Token operator = previous();
            ASTNode right = parseLogicalAnd();
            left = new BinaryExpr(left.getLine(), left, operator, right);
        }

        return left;
    }


    /**
     * 解析逻辑与表达式 a && b
     * 实现短路求值特性
     */
    private ASTNode parseLogicalAnd() {
        ASTNode left = parseBitwiseOr();

        while (matchs("&&")) {
            Token operator = previous();
            ASTNode right = parseBitwiseOr();
            left = new BinaryExpr(left.getLine(), left, operator, right);
        }

        return left;
    }


    /**
     * 解析位或表达式 a | b
     */
    private ASTNode parseBitwiseOr() {
        ASTNode left = parseBitwiseXor();

        while (matchs("|")) {
            Token operator = previous();
            ASTNode right = parseBitwiseXor();
            left = new BinaryExpr(left.getLine(), left, operator, right);
        }

        return left;
    }



    /**
     * 解析位异或表达式 a ^ b
     */
    private ASTNode parseBitwiseXor() {
        ASTNode left = parseBitwiseAnd();

        while (matchs("^")) {
            Token operator = previous();
            ASTNode right = parseBitwiseAnd();
            left = new BinaryExpr(left.getLine(), left, operator, right);
        }

        return left;
    }



    /**
     * 解析位与表达式 a & b
     */
    private ASTNode parseBitwiseAnd() {
        ASTNode left = parseEquality();

        while (matchs("&")) {
            Token operator = previous();
            ASTNode right = parseEquality();
            left = new BinaryExpr(left.getLine(), left, operator, right);
        }

        return left;
    }



    /**
     * 解析相等性判断表达式 a == b 或 a != b
     */
    private ASTNode parseEquality() {
        ASTNode left = parseRelational();

        while (matchs("==", "!=")) {
            Token operator = previous();
            ASTNode right = parseRelational();
            left = new BinaryExpr(left.getLine(), left, operator, right);
        }

        return left;
    }



    /**
     * 解析关系表达式 a < b, a > b, a <= b, a >= b
     */
    private ASTNode parseRelational() {
        ASTNode left = parseShift();

        while (matchs("<", ">", "<=", ">=")) {
            Token operator = previous();
            ASTNode right = parseShift();
            left = new BinaryExpr(left.getLine(), left, operator, right);
        }

        return left;
    }



    /**
     * 解析位移表达式 a << b, a >> b, a >>> b
     */
    private ASTNode parseShift() {
        ASTNode left = parseAdditive();

        while (matchs("<<", ">>", ">>>")) {
            Token operator = previous();
            ASTNode right = parseAdditive();
            left = new BinaryExpr(left.getLine(), left, operator, right);
        }

        return left;
    }


    /**
     * 解析加减法表达式 a + b, a - b
     */
    private ASTNode parseAdditive() {
        ASTNode left = parseMultiplicative();

        while (matchs("+", "-")) {
            Token operator = previous();
            ASTNode right = parseMultiplicative();
            left = new BinaryExpr(left.getLine(), left, operator, right);
        }

        return left;
    }



    /**
     * 解析乘除法表达式 a * b, a / b, a % b
     */
    private ASTNode parseMultiplicative() {
        ASTNode left = parseUnary();

        while (matchs("*", "/", "%")) {
            Token operator = previous();
            ASTNode right = parseUnary();
            left = new BinaryExpr(left.getLine(), left, operator, right);
        }

        return left;
    }



    /**
     * 解析一元运算符表达式
     * 支持 ++x, --x, +x, -x, !x, ~x
     */
    private ASTNode parseUnary() {
        if (matchs("+", "-", "!", "~")) {
            Token operator = previous();
            ASTNode right = parseUnary();
            return new UnaryExpr(operator.getLine(), operator, right);
        }

        return parsePostfix();
    }



    /**
     * 解析后缀表达式
     * 支持 x++, x--
     */
    private ASTNode parsePostfix() {
        ASTNode expr = parsePrimary();

        if (matchs("++", "--")) {
            Token operator = previous();
            // 创建后缀表达式节点
            return new PostfixExpr(expr.getLine(), expr, operator);
        }

        return expr;
    }



    /**
     * 解析基本表达式（标识符、字面量、括号表达式、方法调用、数组访问等）
     */
    private ASTNode parsePrimary() {
        System.out.println("fuck "+peek().getValue());
        if (matchs("true", "false", "null")) {
            // 处理布尔字面量和null
            Token token = previous();
            return new LiteralExpr(token.getLine(), token.getValue());
        }

        if (match("整数", "浮点数", "小数", "字符串字面量")) {
            // 处理数值和字符串字面量
            Token token = previous();
            return new LiteralExpr(token.getLine(), token.getValue());
        }

        if (matchs("new")) {
            return parseNewExpression(); // 处理new表达式
        }

        if (matchs("(")) {
            ASTNode expr = parseExpression();
            consumes(")", "Expect ')' after expression");
            return new ParenthesizedExpr(expr.getLine(), expr); // 括号表达式
        }

        if (match("标识符")) {
            String identifier = previous().getValue();
            ASTNode expr = new IdentifierExpr(previous().getLine(), identifier);

            // 处理链式成员访问（如 System.out.println）
            while (matchs(".")) {
                Token dot = previous();
                Token member = consume("标识符", "Expect member name after '.'");
                expr = new MemberAccessExpr(expr, dot, new IdentifierExpr(member.getLine(), member.getValue()));
            }

            // 处理数组访问（如arr[i]）
            while (matchs("[")) {
                ASTNode index = parseExpression();
                consumes("]", "Expect ']' after array index");
                expr = new ArrayAccessExpr(expr.getLine(), expr, index);
            }



            // 处理方法调用
            if (matchs("(")) {
                List<ASTNode> arguments = parseArguments();
                return new MethodCallExpr(
                        previous().getLine(), // 使用标识符的行号
                        expr,                 // 包含完整的调用路径
                        arguments
                );
            }


            return expr;
        }

        // 处理数组初始化式（如直接出现在表达式中的 {1,2,3}）
        if (matchs("{")) {
            List<ASTNode> elements = new ArrayList<>();
            do {
                elements.add(parseExpression());
            } while (matchs(","));
            consumes("}", "Expect '}' after array elements");
            return new ArrayInitializerExpr(previous().getLine(),"data", elements);
        }


        throw new ParseError();
    }




    /**
     * 解析new表达式，包括对象创建和数组分配
     */
    private ASTNode parseNewExpression() {
        Token newToken = previous();

        if (matchs("[")) {
            // 处理数组创建表达式 new int[10]
            ASTNode type = parseType();
            ASTNode size = parseArraySize();
            return new NewArrayExpr(newToken.getLine(), type, size);
        } else {
            // 处理对象创建表达式 new Object()
            ASTNode type = parseType();
            consumes("(", "Expect '(' after type");
            List<ASTNode> arguments = parseArguments();
            return new NewObjectExpr(newToken.getLine(), type, arguments);
        }
    }



    /**
     * 解析类型表达式（用于new表达式）
     */
    private ASTNode parseType() {
        if (matchs("标识符")) {
            String typeName = previous().getValue();
            ASTNode typeExpr = new IdentifierExpr(previous().getLine(), typeName);

            // 处理带包名的类型（如 java.util.List）
            while (matchs(".")) {
                Token dot = previous();
                Token member = consume("标识符", "Expect type name after '.'");
                typeExpr = new MemberAccessExpr(typeExpr, dot, new IdentifierExpr(member.getLine(), member.getValue()));
            }

            return typeExpr;
        }

        throw new ParseError();
    }




    /**
     * 解析数组大小表达式
     */
    private ASTNode parseArraySize() {
        if (matchs("]")) {
            // 空数组维度 new int[][]
            return null;
        } else {
            ASTNode sizeExpr = parseExpression();
            consumes("]", "Expect ']' after array size");
            return sizeExpr;
        }
    }



    /**
     * 获取运算符优先级（数值越大优先级越高）
     * 完全遵循Java语言规范
     */
    private int getOperatorPrecedence(String operator) {
        switch (operator) {
            // 0. 最低优先级占位
            case ",": return 0;

            // 1. 赋值类（右结合）
            case "=": case "+=": case "-=": case "*=": case "/=": case "%=":
            case "&=": case "|=": case "^=": case "<<=": case ">>=": case ">>>=": return 1;

            // 2. 条件运算符（右结合）
            case "?:": return 2;

            // 3. 逻辑或
            case "||": return 3;

            // 4. 逻辑与
            case "&&": return 4;

            // 5. 位或
            case "|": return 5;

            // 6. 位异或
            case "^": return 6;

            // 7. 位与
            case "&": return 7;

            // 8. 相等判断
            case "==": case "!=": return 8;

            // 9. 大小比较
            case "<": case ">": case "<=": case ">=":
            case "instanceof": return 9;

            // 10. 位移
            case "<<": case ">>": case ">>>": return 10;

            // 11. 加减
            case "+": case "-": return 11;

            // 12. 乘除模
            case "*": case "/": case "%": return 12;

            // 13. 一元运算符（最高优先级）
            case "++": case "--":  case "!": case "~":
            case "new": case "(type)": return 13;

            default:
                System.out.println(" operator: " + operator); return -1; // 无效运算符
        }
    }


    static class ConditionalExpr extends ASTNode {
        private final ASTNode condition;
        private final ASTNode thenBranch;
        private final ASTNode elseBranch;

        public ConditionalExpr(int line, ASTNode condition, ASTNode thenBranch, ASTNode elseBranch) {
            super(line);
            this.condition = condition;
            this.thenBranch = thenBranch;
            this.elseBranch = elseBranch;
        }

        @Override
        public String toString(int indent) {
            StringBuilder sb = new StringBuilder(" ".repeat(indent));
            sb.append("ConditionalExpr(\n");
            sb.append(condition.toString(indent + 2)).append(",\n");
            sb.append(thenBranch.toString(indent + 2)).append(",\n");
            sb.append(elseBranch.toString(indent + 2)).append("\n");
            sb.append(" ".repeat(indent)).append(")");
            return sb.toString();
        }

        @Override
        public int getChildCount() {
            return 3;
        }

        @Override
        public ASTNode getChild(int index) {
            switch(index) {
                case 0: return condition;
                case 1: return thenBranch;
                case 2: return elseBranch;
                default: throw new IndexOutOfBoundsException("ConditionalExpr has only 3 children. Index: " + index);
            }
        }

        public ASTNode getCondition() {
            return condition;
        }

        public ASTNode getThenBranch() {
            return thenBranch;
        }

        public ASTNode getElseBranch() {
            return elseBranch;
        }
    }



    static class MemberAccessExpr extends ASTNode {
        private final ASTNode object;
        private final Token dot;
        private final ASTNode member;

        public MemberAccessExpr(ASTNode object, Token dot, ASTNode member) {
            super(object.getLine());
            this.object = object;
            this.dot = dot;
            this.member = member;
        }

        @Override
        public String toString(int indent) {
            return " ".repeat(indent) + "MemberAccessExpr(" +
                    object.toString(0) + "." +
                    member.toString(0) + ")";
        }

        @Override
        public int getChildCount() {
            return 2;
        }

        @Override
        public ASTNode getChild(int index) {
            if (index == 0) return object;
            if (index == 1) return member;
            throw new IndexOutOfBoundsException("MemberAccessExpr has only 2 children. Index: " + index);
        }

        public Object getMember() {
            return member;
        }
        public Token getDot() {
            return dot;
        }

        public ASTNode getObject() {
            return object;
        }
    }




    static class ParenthesizedExpr extends ASTNode {
        private final ASTNode expression;

        public ParenthesizedExpr(int line, ASTNode expression) {
            super(line);
            this.expression = expression;
        }

        @Override
        public String toString(int indent) {
            return " ".repeat(indent) + "ParenthesizedExpr(" +
                    expression.toString(0) + ")";
        }

        @Override
        public int getChildCount() {
            return 1;
        }

        @Override
        public ASTNode getChild(int index) {
            if (index == 0) return expression;
            throw new IndexOutOfBoundsException("ParenthesizedExpr has only 1 child. Index: " + index);
        }

        public ASTNode getExpression() {
            return expression;
        }
    }



    static class PostfixExpr extends ASTNode {
        private final ASTNode operand;
        private final Token operator;

        public PostfixExpr(int line, ASTNode operand, Token operator) {
            super(line);
            this.operand = operand;
            this.operator = operator;
        }

        @Override
        public String toString(int indent) {
            return " ".repeat(indent) + "PostfixExpr(" +
                    operand.toString(0) + ", " +
                    operator.getValue() + ")";
        }

        @Override
        public int getChildCount() {
            return 1;
        }

        @Override
        public ASTNode getChild(int index) {
            if (index == 0) return operand;
            throw new IndexOutOfBoundsException("PostfixExpr has only 1 child. Index: " + index);
        }

        public ASTNode getOperand() {
            return operand;
        }
        public Token getOperator() {
            return operator;
        }
    }



    /**
     * 数组访问表达式节点类
     * 表示数组访问操作，如 arr[i]
     */
    static class ArrayAccessExpr extends ASTNode {
        private final ASTNode array;
        private final ASTNode index;

        /**
         * 创建一个新的数组访问表达式节点
         * @param line 行号
         * @param array 数组对象
         * @param index 索引表达式
         */
        public ArrayAccessExpr(int line, ASTNode array, ASTNode index) {
            super(line);
            this.array = array;
            this.index = index;
        }

        @Override
        public String toString(int indent) {
            return " ".repeat(indent) + "ArrayAccessExpr(" +
                    array.toString(0) + "[" +
                    index.toString(0) + "])";
        }

        @Override
        public int getChildCount() {
            return 2;
        }

        @Override
        public ASTNode getChild(int index) {
            if (index == 0) {
                return array;
            } else if (index == 1) {
                return this.index;
            }
            throw new IndexOutOfBoundsException("ArrayAccessExpr has only 2 children. Index: " + index);
        }

        public ASTNode getArray() {
            return array;
        }

        public ASTNode getIndex() {
            return index;
        }
    }




    static class ArrayInitializerExpr extends ASTNode {
        private final String variableName;
        private final List<ASTNode> elements;

        public ArrayInitializerExpr(int line, String variableName, List<ASTNode> elements) {
            super(line);
            this.variableName = variableName;
            this.elements = elements != null ? elements : new ArrayList<>();
        }

        @Override
        public String toString(int indent) {
            StringBuilder sb = new StringBuilder(" ".repeat(indent));
            sb.append("ArrayInitializerExpr(").append(variableName).append(", [\n");

            for (int i = 0; i < elements.size(); i++) {
                sb.append(elements.get(i).toString(indent + 4));
                if (i < elements.size() - 1) sb.append(",");
                sb.append("\n");
            }

            sb.append(" ".repeat(indent)).append("])");
            return sb.toString();
        }

        @Override
        public int getChildCount() {
            return elements.size();
        }

        @Override
        public ASTNode getChild(int index) {
            return elements.get(index);
        }

        public List<ASTNode> getElements() {
            return elements;
        }


        public String getElementType() {
            SymbolType symbol = symbols.get(variableName);
            if (symbol == null || symbol.getDataType() == null) {
                throw new IllegalStateException("无法确定数组变量 " + variableName + " 的类型");
            }

            String dataType = symbol.getDataType();
            if (dataType.endsWith("[]")) {
                return dataType.substring(0, dataType.length() - 2);
            }

            return dataType;
        }

        public String getVariableName() {
            return variableName;
        }
    }



    static class NewArrayExpr extends ASTNode {
        private final ASTNode type;
        private final ASTNode size;

        public NewArrayExpr(int line, ASTNode type, ASTNode size) {
            super(line);
            this.type = type;
            this.size = size;
        }

        @Override
        public String toString(int indent) {
            return " ".repeat(indent) + "NewArrayExpr(" +
                    type.toString(0) + ", " +
                    (size != null ? size.toString(0) : "[]") + ")";
        }

        @Override
        public int getChildCount() {
            return size != null ? 2 : 1;
        }

        @Override
        public ASTNode getChild(int index) {
            if (index == 0) return type;
            if (index == 1 && size != null) return size;
            throw new IndexOutOfBoundsException("Invalid child index for NewArrayExpr");
        }

        public ASTNode getSize() {
            return size;
        }
        public ASTNode getType() {
            return type;
        }
    }



    static class NewObjectExpr extends ASTNode {
        private final ASTNode type;
        private final List<ASTNode> arguments;

        public NewObjectExpr(int line, ASTNode type, List<ASTNode> arguments) {
            super(line);
            this.type = type;
            this.arguments = arguments;
        }

        @Override
        public String toString(int indent) {
            StringBuilder sb = new StringBuilder(" ".repeat(indent));
            sb.append("NewObjectExpr(").append(type.toString(0)).append("(\n");

            for (int i = 0; i < arguments.size(); i++) {
                sb.append(arguments.get(i).toString(indent + 4));
                if (i < arguments.size() - 1) sb.append(",");
                sb.append("\n");
            }

            sb.append(" ".repeat(indent)).append("))");
            return sb.toString();
        }

        @Override
        public int getChildCount() {
            return 1 + arguments.size();
        }

        @Override
        public ASTNode getChild(int index) {
            if (index == 0) return type;
            return arguments.get(index - 1);
        }

        public ASTNode getType() {
            return type;
        }

        public List<ASTNode> getArguments() {
            return arguments;
        }
    }



    /**
     * 标识符表达式节点类
     * 表示变量名、方法名、类型名等标识符
     */
    static class IdentifierExpr extends ASTNode {
        private final String identifier;

        /**
         * 创建一个新的标识符表达式节点
         * @param line 行号
         * @param identifier 标识符名称
         */
        public IdentifierExpr(int line, String identifier) {
            super(line);
            this.identifier = identifier;
        }

        @Override
        public String toString(int indent) {
            return " ".repeat(indent) + "IdentifierExpr(" + identifier + ")";
        }

        @Override
        public int getChildCount() {
            return 0;
        }

        @Override
        public ASTNode getChild(int index) {
            throw new IndexOutOfBoundsException("IdentifierExpr has no children. Index: " + index);
        }

        public String getIdentifier() {
            return identifier;
        }
    }



    /**
     * 方法调用表达式节点类
     * 表示对象或类的方法调用，如 obj.method() 或 Class.method()
     */
    static class MethodCallExpr extends ASTNode {
        private final ASTNode callee;  // 调用目标（可以是标识符或成员访问表达式）
        private final List<ASTNode> arguments;

        /**
         * 创建一个新的方法调用表达式节点
         * @param line 行号
         * @param callee 调用目标
         * @param arguments 参数列表
         */
        public MethodCallExpr(int line, ASTNode callee, List<ASTNode> arguments) {
            super(line);
            this.callee = callee;
            this.arguments = arguments;
        }

        @Override
        public String toString(int indent) {
            StringBuilder sb = new StringBuilder(" ".repeat(indent));
            sb.append("MethodCallExpr(")
                    .append(callee.toString(0))  // 显示调用目标
                    .append("(");
            for (int i = 0; i < arguments.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(arguments.get(i).toString(0));
            }
            sb.append("))");
            return sb.toString();
        }

        @Override
        public int getChildCount() {
            return 1 + arguments.size(); // callee + arguments
        }

        @Override
        public ASTNode getChild(int index) {
            if (index == 0) return callee;
            return arguments.get(index - 1);
        }

        public ASTNode getCallee() {
            return callee;
        }

        public List<ASTNode> getArguments() {
            return arguments;
        }
    }



    /**
     * 一元表达式节点类
     * 支持前缀形式的一元运算符，如 !、~、+、- 等
     */
    static class UnaryExpr extends ASTNode {
        private final Token operator;
        private final ASTNode right;

        /**
         * 创建一个新的一元表达式节点
         * @param line 行号
         * @param operator 运算符标记
         * @param right 右操作数
         */
        public UnaryExpr(int line, Token operator, ASTNode right) {
            super(line);
            this.operator = operator;
            this.right = right;
        }

        @Override
        public String toString(int indent) {
            return " ".repeat(indent) + "UnaryExpr(" +
                    operator.getValue() + ", " +
                    right.toString(0) + ")";
        }

        @Override
        public int getChildCount() {
            return 1;
        }

        @Override
        public ASTNode getChild(int index) {
            if (index == 0) {
                return right;
            }
            throw new IndexOutOfBoundsException("UnaryExpr has only 1 child. Index: " + index);
        }

        public ASTNode getRight() {
            return right;
        }

        public Token getOperator() {
            return operator;
        }
    }



    /**
     * 解析方法调用或new表达式的参数列表
     * 支持各种类型的参数传递
     */
    private List<ASTNode> parseArguments() {
        List<ASTNode> args = new ArrayList<>();

        if (!checkValue(")")) {
            do {
                // 解析单个参数表达式
                ASTNode arg = parseExpression();
                args.add(arg);
            } while (matchs(","));
        }

        consumes(")", "Expect ')' after argument list");
        return args;
    }

    public class ExpressionStatement extends ASTNode {
        private final ASTNode expression;

        public ExpressionStatement(int line, ASTNode expression) {
            super(line);
            this.expression = expression;
        }

        public ASTNode getExpression() {
            return expression;
        }

        @Override
        public int getChildCount() {
            return 1;
        }

        @Override
        public ASTNode getChild(int index) {
            if (index == 0) return expression;
            throw new IndexOutOfBoundsException();
        }

        @Override
        public String toString(int indent) {
            return " ".repeat(indent) + "ExpressionStatement(\n" +
                    getExpression().toString(indent + 2) + "\n" +
                    " ".repeat(indent) + ")";
        }
    }


    private ArrayInitializerExpr parseArrayInitializer(String variableName) {
        int line = previous().getLine();
        List<ASTNode> elements = new ArrayList<>();

        while (!checkValue("}") && !isAtEnd()) {
            elements.add(parseExpression());
            if (!matchs(",")) break;
        }
        consumes("}", "Expect '}' after array elements");

        return new ArrayInitializerExpr(line,variableName, elements);
    }





}