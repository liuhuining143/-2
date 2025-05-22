package com.example.debug;

import java.util.*;



public class TokenCodeTable {
    // Token 编码范围定义
    private static final int KEYWORD_START = 1;
    private static final int KEYWORD_END = 999;
    private static final int NUMBER_START = 1001;
    private static final int NUMBER_END = 1999;
    private static final int OPERATOR_START = 2001;
    private static final int OPERATOR_END = 2999;
    private static final int IDENTIFIER_CODE = 3001;
    private static final int SEPARATOR_START = 4001;
    private static final int SEPARATOR_END = 4999;
    private static final int COMMENT_START = 5001;
    private static final int COMMENT_END = 5999;
    private static final Map<Integer, String> KIND_TO_TYPE = new HashMap<>();

    private static final Map<String, Integer> TOKEN_CODE_MAP = new HashMap<>();
    private static final Map<String, Integer> KEYWORDS = new HashMap<>();
    private static final Map<String, Integer> SYMBOLS = new HashMap<>();
   /* private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
            "public", "private", "protected", "class", "interface", "enum",
            "extends", "implements", "package", "import", "void", "int",
            "long", "short", "byte", "char", "float", "double", "boolean",
            "if", "else", "while", "for", "do", "switch", "case", "default",
            "break", "continue", "return", "try", "catch", "finally", "throw",
            "throws", "new", "this", "super", "instanceof", "static", "final",
            "abstract", "synchronized", "transient", "volatile", "native",
            "strictfp", "assert", "const", "goto", "true", "false", "null"
    ));*/



    static {



        // 关键字映射
        KEYWORDS.put("public", 1001);
        KEYWORDS.put("private", 1002);
        KEYWORDS.put("void", 1003);
        KEYWORDS.put("class", 1004);
        KEYWORDS.put("static", 1005);
        KEYWORDS.put("int", 1006);
        // 添加其他Java关键字...

        // 符号映射
        SYMBOLS.put("(", 2001);
        SYMBOLS.put(")", 2002);
        SYMBOLS.put("{", 2003);
        SYMBOLS.put("}", 2004);


        // 1. 关键字（编码范围：1-999）
        addKeywords(
                "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
                "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float",
                "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native",
                "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp",
                "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void", "volatile",
                "while", "var", "record", "sealed", "non-sealed", "yield"
        );

        // 2. 数字（编码范围：1001-1999）
        TOKEN_CODE_MAP.put("integer", 1001);     // 十进制整数
        TOKEN_CODE_MAP.put("float", 1002);       // 浮点数
        TOKEN_CODE_MAP.put("binary", 1003);      // 二进制 (0b1010)
        TOKEN_CODE_MAP.put("octal", 1004);       // 八进制 (0123)
        TOKEN_CODE_MAP.put("hex", 1005);         // 十六进制 (0x1AF)
        TOKEN_CODE_MAP.put("scientific", 1006);  // 科学计数法 (1.23e5)

        // 3. 运算符（编码范围：2001-2999）
        addOperators(
                "+", "-", "*", "/", "%", "=", "==", "!=", ">", "<", ">=", "<=", "&&", "||", "!",
                "&", "|", "^", "~", "<<", ">>", ">>>", "++", "--", "+=", "-=", "*=", "/=", "%=",
                "&=", "|=", "^=", "<<=", ">>=", ">>>=", "->", "::", "?", ":", "instanceof", "."
        );

        // 4. 分隔符（编码范围：4001-4999）
        addSeparators(
                "(", ")", "{", "}", "[", "]", ";", ",", ".", "@", "...", "::"
        );

        // 5. 注释（编码范围：5001-5999）
        TOKEN_CODE_MAP.put("//", 5001);       // 单行注释
        TOKEN_CODE_MAP.put("/*", 5002);       // 多行注释开始
        TOKEN_CODE_MAP.put("*/", 5003);       // 多行注释结束
        TOKEN_CODE_MAP.put("/**", 5004);      // Javadoc 注释开始
    }

    // 辅助方法：批量添加关键字
    private static void addKeywords(String... keywords) {
        int code = KEYWORD_START;
        for (String keyword : keywords) {
            TOKEN_CODE_MAP.put(keyword, code++);
        }
    }

    // 辅助方法：批量添加运算符
    private static void addOperators(String... operators) {
        int code = OPERATOR_START;
        for (String op : operators) {
            TOKEN_CODE_MAP.put(op, code++);
        }
    }

    // 辅助方法：批量添加分隔符
    private static void addSeparators(String... separators) {
        int code = SEPARATOR_START;
        for (String sep : separators) {
            TOKEN_CODE_MAP.put(sep, code++);
        }
    }

    /**
     * 获取 Token 的编码
     */


    /**
     * 判断是否为关键字
     */
    public static boolean isKeyword(String word) {
        return KEYWORDS.containsKey(word) || TOKEN_CODE_MAP.containsKey(word);
    }

    public static int getTokenCode(String word) {
        // 优先级：先查关键字，再查符号，最后默认
        if (KEYWORDS.containsKey(word)) {
            return KEYWORDS.get(word);
        } else if (SYMBOLS.containsKey(word)) {
            return SYMBOLS.get(word);
        }
        return -1; // 或分配默认编码
    }

    /**
     * 判断是否为数字类型
     */
    public static boolean isNumber(String tokenType) {
        int code = getTokenCode(tokenType);
        return code >= NUMBER_START && code <= NUMBER_END;
    }

    /**
     * 判断是否为运算符
     */
    public static boolean isOperator(String token) {
        int code = getTokenCode(token);
        return code >= OPERATOR_START && code <= OPERATOR_END;
    }

    /**
     * 判断是否为标识符（动态判断，非预定义）
     */
    public static boolean isIdentifier(String token) {
        return !isKeyword(token) && token.matches("[a-zA-Z_$][a-zA-Z0-9_$]*");
    }

    /**
     * 判断是否为分隔符
     */
    public static boolean isDelimiter(String token) {
        int code = getTokenCode(token);
        return code >= SEPARATOR_START && code <= SEPARATOR_END;
    }

    /**
     * 判断是否为注释符号
     */
    public static boolean isComment(String token) {
        int code = getTokenCode(token);
        return code >= COMMENT_START && code <= COMMENT_END;
    }

    /**
     * 判断是否为有效预定义 Token
     */
    public static boolean isValidToken(String token) {
        return TOKEN_CODE_MAP.containsKey(token);
    }

    public static String getTokenType(int kind) {
        String type = KIND_TO_TYPE.get(kind);
        return type != null ? type : "UNKNOWN_" + kind;  // 避免返回 null
    }


}