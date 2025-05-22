package com.example.debug;

public class Token {
    public int kind;     // Token 类型编码
    public String image=null; // 匹配的原始字符串
    public Token next;
    public Token specialToken;
    private String type;
    private String value;
    private int line;
    private int code;
    public int beginLine;
    public int endLine;
    public int beginColumn;
    public int endColumn;

    public Token() {
    }

    public Token(String type, String value, int line, int code) {
        this.type = type;
        this.value = value;
        this.line = line;
        this.code = code;
    }

    public Token(String type, String value, int line){
        this.type = type;
        this.value = value;
        this.line = line;
    }




    // Getters
    public String getType() { return type; }
    public int getCode() { return code; }
    /**
     * 设置
     * @param type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * 获取
     * @return value
     */
    public String getValue() {
        return value;
    }

    /**
     * 设置
     * @param value
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * 获取
     * @return line
     */
    public int getLine() {
        return line;
    }

    /**
     * 设置
     * @param line
     */
    public void setLine(int line) {
        this.line = line;
    }

    public String toString() {
        return "Token{type = " + type + ", value = " + value + ", line = " + line + "}";
    }

   /* public char[] getWord() {
        return image.toCharArray();
    }*/
}
