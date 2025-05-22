package com.example.debug.AST;

import java.util.List;

public class ProgramNode extends ASTNode {
    private final List<ASTNode> declarations;

    public ProgramNode(List<ASTNode> declarations) {
        super(0); // 根节点无行号
        this.declarations = declarations;
    }

    @Override
    public String toString(int indent) {
        StringBuilder sb = new StringBuilder();
        for (ASTNode node : declarations) {
            sb.append(node.toString(indent)).append("\n");
        }
        return sb.toString();
    }
    @Override
    public int getChildCount() {
        return declarations.size();
    }

    @Override
    public ASTNode getChild(int index) {
        return declarations.get(index);
    }
}