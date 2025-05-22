package com.example.debug.AST;

import java.util.Arrays;
import java.util.List;

public class MethodDeclaration extends ASTNode{
    private final String methodName;
    private final String returnType;
    private final List<Parameter> parameters;
    private final BlockStatement body;
    private List<String> modifiers = List.of();  // 新增字段

    public MethodDeclaration(
            int line,
            List<String> modifiers,
            String methodName,
            String returnType,
            List<Parameter> parameters,
            BlockStatement body
    ) {
        super(line);
        this.modifiers = modifiers;
        this.methodName = methodName;
        this.returnType = returnType;
        this.parameters = parameters;
        this.body = body;
    }

    public MethodDeclaration(int line, String methodName, String returnType,
                             List<Parameter> parameters, BlockStatement body, List<String> modifiers) {
        super(line);
        this.methodName = methodName;
        this.returnType = returnType;
        this.parameters = parameters;
        this.body = body;
    }



    @Override
    public String toString(int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(" ".repeat(indent)).append("方法声明: ");
        // 输出修饰符
        if (!modifiers.isEmpty()) {
            sb.append(String.join(" ", modifiers)).append(" ");
        }
        sb.append(returnType).append(" 类型返回值的方法 ").append(methodName).append("(");
        for (int i = 0; i < parameters.size(); i++) {
            sb.append(parameters.get(i));
            if (i < parameters.size() - 1) sb.append(", ");
        }
        sb.append(")");
        sb.append("\n").append(body.toString(indent + 2));
        return sb.toString();
    }

    @Override
    public int getChildCount() {
        // 子节点包括所有参数和方法体
        return parameters.size() + 1;
    }

    @Override
    public ASTNode getChild(int index) {
        if (index < parameters.size()) {
            // 前几个子节点是参数
            return new ASTNode(0) {
                @Override
                public String toString(int indent) {
                    return " ".repeat(indent) + parameters.get(index).toString();
                }

                @Override
                public int getChildCount() {
                    // 参数节点没有子节点
                    return 0;
                }

                @Override
                public ASTNode getChild(int childIndex) {
                    throw new IndexOutOfBoundsException("参数节点没有子节点。索引: " + childIndex);
                }
            };
        } else if (index == parameters.size()) {
            // 最后一个子节点是方法体
            return body;
        }
        throw new IndexOutOfBoundsException("索引: " + index + ", 子节点数量: " + getChildCount());
    }
}


class LiteralExpr extends ASTNode {
    private final String value;

    public LiteralExpr(int line, String value) {
        super(line);
        this.value = value;
    }

    @Override
    public String toString(int indent) {
        return " ".repeat(indent) + "字面量表达式: " + value;
    }

    @Override
    public int getChildCount() {
        // 字面量表达式没有子节点
        return 0;
    }

    @Override
    public ASTNode getChild(int index) {
        throw new IndexOutOfBoundsException("字面量表达式没有子节点。索引: " + index);
    }

    public String getValue() {
        return value;
    }
}

