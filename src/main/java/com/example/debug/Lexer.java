package com.example.debug;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Lexer {
    private String address;
    private boolean inMultiLineComment = false;
    private int readline;
    private List<Token> tokens=new ArrayList<>();
    private List<ErrorEntry> errors;

    public Lexer() {
        this.tokens = new ArrayList<>();
        this.errors = new ArrayList<>();
        this.readline = 1;
    }

    public List<Token> getTokens() {
        return tokens;
    }

    public List<Token> analyze(String code) throws LexerException {
        tokens.clear();
        errors.clear();
        readline = 1;

        String[] lines = code.split("\n");
        for (String line : lines) {
            process(line);
            readline++;
        }

        // 如果有错误，抛出异常
        if (!errors.isEmpty()) {
            throw new LexerException("发现错误", tokens, errors); // 携带 Tokens 和 Errors
        }
        return tokens;
    }

    public Lexer(String address) {
        this.address = address;
        this.readline = 1;
    }

    public void openFile() throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(address))) {
            String line;
            while ((line = br.readLine()) != null) {
                readline++;
                process(line);
            }
        }catch (IOException e) {
            System.err.println("Error reading file: " + address + ", " + e.getMessage());
            throw e;
        }
    }

    private void process(String line) {
        int pos = 0;
        while (pos < line.length()) {
            char c = line.charAt(pos);

            if (inMultiLineComment) {
                boolean foundEnd = false;
                while (pos < line.length()) {
                    if (pos + 1 < line.length() && line.charAt(pos) == '*' && line.charAt(pos + 1) == '/') {
                        tokens.add(new Token("多行注释", "*/", readline, TokenCodeTable.getTokenCode("*/")));
                        pos += 2; 
                        inMultiLineComment = false;
                        foundEnd = true;
                        break;
                    }
                    pos++;
                }
                if (!foundEnd) {
                    pos = line.length();
                }
                continue;
            }

            // 1. 处理字符串字面量
            if (c == '"') {
                pos = processStringLiteral(line, pos);
                continue;
            } else if (c == '\'') {
                pos = processCharLiteral(line, pos);
                continue;

            }
            if (Character.isJavaIdentifierStart(c)){
                pos=processIdentifier(line,pos);
            }
            else if (Character.isDigit(c)){
                pos=processNumber(line,pos);
            }
            else if (c=='+'||c=='-'||c=='*'||c=='%'||c=='='||c=='<'||c=='>'||c=='!'||c=='&'||c=='|'||c=='^'||c=='~'||c=='?'||c==':'){
               pos=processComment(line,pos);
            }
            else if (c=='('||c==')'||c=='{'||c=='}'||c=='['||c==']'||c==','||c==';'||c=='.'||c=='@'||c=='#'||c=='$'){
                String word=String.valueOf(c);
                String type="分隔符";
                int code=TokenCodeTable.getTokenCode(word);
                tokens.add(new Token(type,word,readline,code));
                pos++;
            }
            else if (c=='/'){
                if (pos+1<line.length()&&line.charAt(pos+1)=='/'){
                    String word="//";
                    String type="单行注释";
                    int code=TokenCodeTable.getTokenCode(word);
                    tokens.add(new Token(type,word,readline,code));
                    pos=line.length();
                }
                else if (pos+1<line.length()&&line.charAt(pos+1)=='*'){
                    String word = "/*";
                    String type = "多行注释";
                    int code = TokenCodeTable.getTokenCode(word);
                    tokens.add(new Token(type, word, readline, code));
                    pos += 2;
                    inMultiLineComment = true;
                }
                else if (pos+1<line.length()&&line.charAt(pos+1)=='='){
                    String word="/=";
                    String type="运算符";
                    int code=TokenCodeTable.getTokenCode(word);
                    tokens.add(new Token(type,word,readline,code));
                    pos+=2;
                }
                else {
                    String word="/";
                    String type="运算符";
                    int code=TokenCodeTable.getTokenCode(word);
                    tokens.add(new Token(type,word,readline,code));
                    pos++;
                }
            }
            else if (Character.isWhitespace(c)){
                pos++;
            }
            else {
                errors.add(new ErrorEntry(readline, "词法错误", "非法字符: " + c));
                pos++;
            }
        }
    }

    private int processIdentifier(String line, int pos) {
        int end = pos;
        while (end < line.length() && Character.isJavaIdentifierPart(line.charAt(end))) {
            end++;
        }
        String word = line.substring(pos, end);

        // 新增关键字判断
        String type = TokenCodeTable.isKeyword(word) ? "关键字" : "标识符";

        tokens.add(new Token(type, word, readline, TokenCodeTable.getTokenCode(word)));
        return end;
    }

    private int processStringLiteral(String line, int startPos) {
        int pos = startPos + 1;
        StringBuilder stringLiteral = new StringBuilder("\"");

        while (pos < line.length()) {
            char c = line.charAt(pos);
            stringLiteral.append(c);

            if (c == '"') {
                // 检查是否是转义的 "
                if (pos > 0 && line.charAt(pos - 1) != '\\') {
                    tokens.add(new Token("字符串字面量", stringLiteral.toString(), readline, TokenCodeTable.getTokenCode("字符串字面量")));
                    return pos + 1;
                }
            }
            pos++;
        }

        // 如果未找到结束的 "
        errors.add(new ErrorEntry(readline, "词法错误", "未闭合的字符串字面量"));
        return pos;
    }


    private int processNumber(String line, int pos) {
        int startPos = pos;
        StringBuilder number = new StringBuilder();
        boolean isHex = false, isOctal = false, isDecimal = false, isScientific = false;
        int radix = 10;

        // 1. 检测进制前缀
        if (pos < line.length() && line.charAt(pos) == '0') {
            number.append('0');
            pos++;
            if (pos < line.length()) {
                char nextChar = Character.toLowerCase(line.charAt(pos));
                if (nextChar == 'x'||nextChar == 'X') { // 十六进制
                    isHex = true;
                    radix = 16;
                    number.append(line.charAt(pos));
                    pos++;
                } else if (Character.digit(nextChar, 8) != -1) { // 八进制
                    isOctal = true;
                    radix = 8;
                } else if (nextChar == '.' || nextChar == 'e' || nextChar == 'E') {
                    isDecimal = true; // 可能是小数或科学计数法
                } else if (Character.isDigit(nextChar)) {
                    isOctal = true;
                    radix = 8;
                }
            }
        }

        // 2. 处理整数部分
        boolean hasIntegerPart = false;
        while (pos < line.length()) {
            char c = line.charAt(pos);
            int digit = Character.digit(c, radix);

            // 处理十六进制字母
            if (isHex && (c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F')) {
                digit = 10 + (Character.toLowerCase(c) - 'a');
            }

            if (digit == -1) break;
            number.append(c);
            pos++;
            hasIntegerPart = true;
        }

        // 处理小数部分
        if (pos < line.length() && line.charAt(pos) == '.' && !isHex && !isOctal) {
            isDecimal = true;
            number.append('.');
            pos++;
            boolean hasFractionPart = false;
            while (pos < line.length()) {
                char c = line.charAt(pos);
                if (Character.digit(c, 10) == -1) break;
                number.append(c);
                pos++;
                hasFractionPart = true;
            }
            if (!hasFractionPart) {
                errors.add(new ErrorEntry(readline, "词法错误", "小数部分缺失"));
            }
        }

        // 处理科学计数法
        if (pos < line.length() && (line.charAt(pos) == 'e' || line.charAt(pos) == 'E') && !isHex && !isOctal) {
            isScientific = true;
            number.append(line.charAt(pos));
            pos++;
            boolean hasExponentSign = false;
            if (pos < line.length() && (line.charAt(pos) == '+' || line.charAt(pos) == '-')) {
                number.append(line.charAt(pos));
                pos++;
                hasExponentSign = true;
            }
            boolean hasExponentPart = false;
            while (pos < line.length()) {
                char c = line.charAt(pos);
                if (Character.digit(c, 10) == -1) break;
                number.append(c);
                pos++;
                hasExponentPart = true;
            }
            if (!hasExponentPart) {
                errors.add(new ErrorEntry(readline, "词法错误", "科学计数法指数缺失"));
            }
        }

        // 5. 验证并生成 Token
        String numStr = number.toString();
        String type = "整数";
        if (isDecimal) type = "小数";
        if (isScientific) type = "科学计数法";
        if (isHex) type = "十六进制数";
        if (isOctal) type = "八进制数";

        try {
            if (isHex) {
                Long.parseLong(numStr.substring(2), 16);
            } else if (isOctal) {
                Long.parseLong(numStr, 8);
            } else if (isDecimal || isScientific) {
                Double.parseDouble(numStr);
            } else {
                Long.parseLong(numStr);
            }
            tokens.add(new Token(type, numStr, readline, TokenCodeTable.getTokenCode(type)));
        } catch (NumberFormatException e) {
            errors.add(new ErrorEntry(readline, "词法错误", "无效数字格式: " + numStr));
        }

        return pos;
    }
    private int processComment(String line,int pos){      //处理运算符
        int end=pos;
        while (end<line.length()&&(line.charAt(end)=='+'||line.charAt(end)=='-'||line.charAt(end)=='*'||line.charAt(end)=='%'||line.charAt(end)=='='||line.charAt(end)=='<'||line.charAt(end)=='>'||line.charAt(end)=='!'||line.charAt(end)=='&'||line.charAt(end)=='|'||line.charAt(end)=='^'||line.charAt(end)=='~'||line.charAt(end)=='?'||line.charAt(end)==':')){
            end++;
        }
        String word=line.substring(pos,end);
        String type="运算符";
        int code=TokenCodeTable.getTokenCode(word);
        tokens.add(new Token(type,word,readline,code));
        return end;
    }

    public int processCharLiteral(String line,int pos){
        int end=pos+3;
        if(line.charAt(pos+2)=='\''){
            String c="'";
            String c2=line.charAt(pos+1)+"";
            String type="字符字面量";
            int code=TokenCodeTable.getTokenCode(c);
            tokens.add(new Token(type,c,readline,code));
            tokens.add(new Token("字符",c2,readline,code));
            tokens.add(new Token(type,c,readline,code));
            return end;
        }
        else{
            errors.add(new ErrorEntry(readline,"词法错误","字符‘字面量缺失"));
            return pos+1;
        }
    }

    public List<ErrorEntry> getErrors() {
        return errors;
    }


}
