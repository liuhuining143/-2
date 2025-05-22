package com.example.debug.AST;

public abstract class ASTNode {
    private final int line;

    public ASTNode(int line) {
        this.line = line;
    }

    public int getLine() {
        return line;
    }

    public abstract String toString(int indent);
    public abstract int getChildCount();
    public abstract ASTNode getChild(int index);
}