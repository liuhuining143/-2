package com.example.debug.AST;

import java.util.List;

public class PackageDeclaration extends ASTNode{
    private final String packageName;

    public PackageDeclaration(int line, String packageName) {
        super(line);
        this.packageName = packageName;
    }

    @Override
    public String toString(int indent) {
        return " ".repeat(indent) + "Package: " + packageName;
    }

    @Override
    public int getChildCount() {
        // 包声明没有子节点
        return 0;
    }

    @Override
    public ASTNode getChild(int index) {
        throw new IndexOutOfBoundsException("PackageDeclaration has no children. Index: " + index);
    }
}



