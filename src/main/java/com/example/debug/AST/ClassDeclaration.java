package com.example.debug.AST;

import java.util.List;

public class ClassDeclaration extends ASTNode {
    private final String className;
    private final List<FieldDeclaration> fields;  // 字段列表
    private final List<MethodDeclaration> methods; // 方法列表

    public ClassDeclaration(
            int line,
            String className,
            List<FieldDeclaration> fields,
            List<MethodDeclaration> methods
    ) {
        super(line);
        this.className = className;
        this.fields = fields;
        this.methods = methods;
    }

    // Getter 方法
    public String getClassName() { return className; }
    public List<FieldDeclaration> getFields() { return fields; }
    public List<MethodDeclaration> getMethods() { return methods; }

    @Override
    public String toString(int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(" ".repeat(indent))
                .append("类声明: 类名 ").append(className).append("\n");

        // 添加字段
        for (FieldDeclaration field : fields) {
            sb.append(" ".repeat(indent + 2))
                    .append("字段: ").append(field.toString(0)).append("\n");
        }

        // 添加方法
        for (MethodDeclaration method : methods) {
            sb.append(" ".repeat(indent + 2))
                    .append("方法: ").append(method.toString(0)).append("\n");
        }

        return sb.toString();
    }

    @Override
    public int getChildCount() {
        return fields.size() + methods.size();
    }

    @Override
    public ASTNode getChild(int index) {
        // 字段在前，方法在后
        if (index < fields.size()) {
            return fields.get(index);
        } else if (index - fields.size() < methods.size()) {
            return methods.get(index - fields.size());
        }
        throw new IndexOutOfBoundsException("Index: " + index);
    }
}
