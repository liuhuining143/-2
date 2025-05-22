package com.example.debug.AST;

import com.example.debug.Token;

class MemberAccessExpr extends ASTNode {
    private final ASTNode object;  // 左侧对象表达式
    private final Token dot;       // . 运算符 Token
    private final ASTNode member;  // 右侧成员表达式

    public MemberAccessExpr(ASTNode object, Token dot, ASTNode member) {
        super(dot.getLine()); // 使用 . 运算符的行号
        this.object = object;
        this.dot = dot;
        this.member = member;
    }

    @Override
    public String toString(int indent) {
        return object.toString(indent) + "." + member.toString(0);
    }

    @Override
    public int getChildCount() {
        return 2; // 包含 object 和 member
    }

    @Override
    public ASTNode getChild(int index) {
        return switch (index) {
            case 0 -> object;
            case 1 -> member;
            default -> throw new IndexOutOfBoundsException();
        };
    }

    public ASTNode getObject() {
        return object;
    }

    public JavaParser.IdentifierExpr getField() {
        return (JavaParser.IdentifierExpr) member;
    }
}
