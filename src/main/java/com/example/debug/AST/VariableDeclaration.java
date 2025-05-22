package com.example.debug.AST;

public class VariableDeclaration extends ASTNode{
    private final String type;
    private final String name;
    private final ASTNode initializer;

    public VariableDeclaration(int line, String type, String name, ASTNode initializer) {
        super(line);
        this.type = type;
        this.name = name;
        this.initializer = initializer;
    }

    @Override
    public String toString(int indent) {
        StringBuilder sb = new StringBuilder(" ".repeat(indent));
        sb.append("变量声明: ").append(type).append(" ").append(name);
        if (initializer != null) {
            sb.append(" = ").append(initializer.toString(0));
        }
        return sb.toString();
    }

    @Override
    public int getChildCount() {
        return initializer != null ? 1 : 0;
    }

    @Override
    public ASTNode getChild(int index) {
        if (index == 0 && initializer != null) {
            return initializer;
        }
        throw new IndexOutOfBoundsException("变量声明有 " + getChildCount() + " 个子节点。索引: " + index);
    }

    public String getVariableName() {
        return name;
    }

    public String getVarType() {
        return type;
    }

    public ASTNode getInitializer() {
        return initializer;
    }
}
