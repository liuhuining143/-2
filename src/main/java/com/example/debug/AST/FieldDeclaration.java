package com.example.debug.AST;

import java.util.List;

public class FieldDeclaration extends ASTNode{
    private final List<String> modifiers;
    private final String type;
    private final String name;
    private final ASTNode initializer;

    public FieldDeclaration(int line, List<String> modifiers, String type, String name, ASTNode initializer) {
        super(line);
        this.modifiers = modifiers;
        this.type = type;
        this.name = name;
        this.initializer = initializer;
    }

    @Override
    public String toString(int indent) {
        StringBuilder sb = new StringBuilder(" ".repeat(indent));
        sb.append("字段声明: ");
        if (!modifiers.isEmpty()) {
            sb.append(String.join(" ", modifiers)).append(" ");
        }
        sb.append(type).append(" 类型的字段 ").append(name);
        if (initializer != null) {
            sb.append(" 初始值为 ").append(initializer.toString(0));
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
        throw new IndexOutOfBoundsException("字段声明在索引 " + index + " 处没有子节点");
    }
}
