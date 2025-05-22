package com.example.debug.AST;

import java.util.List;

public class IfStatement extends ASTNode{
    private final ASTNode condition;
    private final BlockStatement thenBlock;
    private final BlockStatement elseBlock;

    public IfStatement(int line, ASTNode condition,
                       BlockStatement thenBlock, BlockStatement elseBlock) {
        super(line);
        this.condition = condition;
        this.thenBlock = thenBlock;
        this.elseBlock = elseBlock;
    }

    @Override
    public String toString(int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(" ".repeat(indent)).append("条件语句: 如果 (").append(condition).append(")\n")
                .append(thenBlock.toString(indent + 2));
        if (elseBlock != null) {
            sb.append("\n").append(" ".repeat(indent)).append("否则\n")
                    .append(elseBlock.toString(indent + 2));
        }
        return sb.toString();
    }
    @Override
    public int getChildCount() {
        return elseBlock != null ? 3 : 2;
    }

    @Override
    public ASTNode getChild(int index) {
        switch (index) {
            case 0:
                return condition;
            case 1:
                return thenBlock;
            case 2:
                if (elseBlock != null) {
                    return elseBlock;
                }
            default:
                throw new IndexOutOfBoundsException("Index: " + index + ", ChildCount: " + getChildCount());
        }
    }

    public ASTNode getCondition() {
        return condition;
    }

    public ASTNode getThenBranch() {
        return thenBlock;
    }

    public ASTNode getElseBranch() {
        return elseBlock;
    }
}

// for 循环
class ForStatement extends ASTNode {
    private final ASTNode init;
    private final ASTNode condition;
    private final ASTNode update;
    private final BlockStatement body;

    public ForStatement(int line, ASTNode init, ASTNode condition,
                        ASTNode update, BlockStatement body) {
        super(line);
        this.init = init;
        this.condition = condition;
        this.update = update;
        this.body = body;
    }

    @Override
    public String toString(int indent) {
        return " ".repeat(indent) + "for (" + init + "; " + condition + "; " + update + ")\n"
                + body.toString(indent + 2);
    }
    @Override
    public int getChildCount() {
        return 4;
    }

    @Override
    public ASTNode getChild(int index) {
        switch (index) {
            case 0:
                return init;
            case 1:
                return condition;
            case 2:
                return update;
            case 3:
                return body;
            default:
                throw new IndexOutOfBoundsException("Index: " + index + ", ChildCount: " + getChildCount());
        }
    }

    public ASTNode getInit() {
        return init;
    }

    public BlockStatement getBody() {
        return body;
    }

    public ASTNode getUpdate() {
        return update;
    }

    public ASTNode getCondition() {
        return condition;
    }
}

class CollectionInitializer extends ASTNode {
    private final String type;
    private final List<ASTNode> elements;

    public CollectionInitializer(int line, String type, List<ASTNode> elements) {
        super(line);
        this.type = type;
        this.elements = elements;
    }

    @Override
    public String toString(int indent) {
        return " ".repeat(indent) + "new " + type + "() { " + String.join((CharSequence) ", ", (CharSequence) elements) + " }";
    }
    @Override
    public int getChildCount() {
        return elements.size();
    }

    @Override
    public ASTNode getChild(int index) {
        return elements.get(index);
    }
}
