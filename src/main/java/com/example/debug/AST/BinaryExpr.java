package com.example.debug.AST;

import com.example.debug.Symbol.SymbolType;
import com.example.debug.Token;
import jdk.incubator.vector.VectorOperators;

class BinaryExpr extends ASTNode {
    private final ASTNode left;
    private final Token operator;
    private final ASTNode right;

    public BinaryExpr(ASTNode left, Token operator, ASTNode right) {
        super(left.getLine());
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

    public BinaryExpr(int line, ASTNode left, Token operator, ASTNode right) {
        super(line);
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

    @Override
    public String toString(int indent) {
        return " ".repeat(indent) + "BinaryExpr(" +
                left.toString(0) + ", " +
                operator.getValue() + ", " +
                right.toString(0) + ")";
    }

    @Override
    public int getChildCount() {
        return 2;
    }

    @Override
    public ASTNode getChild(int index) {
        if (index == 0) {
            return left;
        } else if (index == 1) {
            return right;
        }
        throw new IndexOutOfBoundsException("BinaryExpr has only 2 children. Index: " + index);
    }

    public ASTNode getLeft() {
        return left;
    }

    public ASTNode getRight() {
        return right;
    }


    public Token getOperator() {
        return operator;
    }
}
