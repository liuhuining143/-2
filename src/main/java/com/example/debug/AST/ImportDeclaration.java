package com.example.debug.AST;

public class ImportDeclaration extends ASTNode{
    private final String importPath; // 存储完整导入路径（如 "java.util.List" 或 "com.example.*"）

    public ImportDeclaration(int line, String importPath) {
        super(line);
        this.importPath = importPath;
    }

    // 获取导入路径（用于后续语义分析）
    public String getImportPath() {
        return importPath;
    }

    @Override
    public String toString(int indent) {
        return " ".repeat(indent) + "import " + importPath + ";"; // 格式化输出示例："import java.util.List;"
    }

    @Override
    public int getChildCount() {
        return 0; // 导入声明是叶子节点，没有子节点
    }

    @Override
    public ASTNode getChild(int index) {
        throw new IndexOutOfBoundsException("ImportDeclaration has no children");
    }
}
