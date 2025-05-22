package com.example.debug.AST;



import java.util.List;

public class BlockStatement extends ASTNode{
    private final List<ASTNode> statements;

    public BlockStatement(int line, List<ASTNode> statements) {
        super(line);
        this.statements = statements;
    }

    @Override
    public String toString(int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(" ".repeat(indent)).append("{\n");
        for (ASTNode stmt : statements) {
            sb.append(stmt.toString(indent + 2)).append("\n");
        }
        sb.append(" ".repeat(indent)).append("}");
        return sb.toString();
    }

    @Override
    public int getChildCount() {
        return statements.size();
    }

    @Override
    public ASTNode getChild(int index) {
        return statements.get(index);
    }

    public List<ASTNode> getStatements() {
        return statements;
    }
}


// While循环节点
class WhileStatement extends ASTNode {
    private final ASTNode condition;
    private final BlockStatement body;

    public WhileStatement(int line, ASTNode condition, BlockStatement body) {
        super(line);
        this.condition = condition;
        this.body = body;
    }

    @Override public String toString(int indent) {
        return pad(indent) + "WhileStatement(\n"
                + condition.toString(indent+2) + "\n"
                + body.toString(indent+2) + ")";
    }

    @Override public int getChildCount() { return 2; }
    @Override public ASTNode getChild(int index) {
        return switch (index) {
            case 0 -> condition;
            case 1 -> body;
            default -> throw new IndexOutOfBoundsException();
        };
    }
    protected String pad(int indent) {
        return " ".repeat(indent * 2); // 每个缩进级别2个空格
    }

    public ASTNode getBody() {
        return body;
    }

    public ASTNode getCondition() {
        return condition;
    }
}

// Do-While节点
class DoWhileStatement extends ASTNode {
    private final BlockStatement body;
    private final ASTNode condition;

    public DoWhileStatement(int line, BlockStatement body, ASTNode condition) {
        super(line);
        this.body = body;
        this.condition = condition;
    }
    protected String pad(int indent) {
        return " ".repeat(indent * 2); // 每个缩进级别2个空格
    }

    @Override public String toString(int indent) {
        return pad(indent) + "DoWhileStatement(\n"
                + body.toString(indent+2) + "\n"
                + condition.toString(indent+2) + ")";
    }

    @Override public int getChildCount() { return 2; }
    @Override public ASTNode getChild(int index) {
        return switch (index) {
            case 0 -> body;
            case 1 -> condition;
            default -> throw new IndexOutOfBoundsException();
        };
    }

    public ASTNode getBody() {
        return body;
    }

    public ASTNode getCondition() {
        return condition;
    }
}

// Switch节点
class SwitchStatement extends ASTNode {
    private final ASTNode expression;
    private final List<CaseStatement> cases;

    public SwitchStatement(int line, ASTNode expr, List<CaseStatement> cases) {
        super(line);
        this.expression = expr;
        this.cases = cases;
    }
    protected String pad(int indent) {
        return " ".repeat(indent * 2); // 每个缩进级别2个空格
    }

    @Override public String toString(int indent) {
        StringBuilder sb = new StringBuilder(pad(indent) + "SwitchStatement(\n");
        sb.append(expression.toString(indent+2)).append("\n");
        for (CaseStatement cs : cases) {
            sb.append(cs.toString(indent+2)).append("\n");
        }
        return sb.append(pad(indent) + ")").toString();
    }

    @Override public int getChildCount() { return 1 + cases.size(); }
    @Override public ASTNode getChild(int index) {
        if (index == 0) return expression;
        return cases.get(index-1);
    }


    public ASTNode getExpression() {
        return expression;
    }

    public CaseStatement[] getCases() {
        return cases.toArray(new CaseStatement[0]);
    }
}

// Try-Catch节点
class TryCatchStatement extends ASTNode {
    private final BlockStatement tryBlock;
    private final List<CatchClause> catches;
    private final BlockStatement finallyBlock;

    public TryCatchStatement(int line, BlockStatement tryBlock,
                             List<CatchClause> catches, BlockStatement finallyBlock) {
        super(line);
        this.tryBlock = tryBlock;
        this.catches = catches;
        this.finallyBlock = finallyBlock;
    }
    protected String pad(int indent) {
        return " ".repeat(indent * 2); // 每个缩进级别2个空格
    }

    @Override public String toString(int indent) {
        StringBuilder sb = new StringBuilder(pad(indent) + "TryCatchStatement(\n");
        sb.append(tryBlock.toString(indent+2)).append("\n");
        for (CatchClause cc : catches) {
            sb.append(cc.toString(indent+2)).append("\n");
        }
        if (finallyBlock != null) {
            sb.append(finallyBlock.toString(indent+2)).append("\n");
        }
        return sb.append(pad(indent) + ")").toString();
    }

    @Override public int getChildCount() {
        return 1 + catches.size() + (finallyBlock != null ? 1 : 0);
    }

    @Override public ASTNode getChild(int index) {
        if (index == 0) return tryBlock;
        if (index <= catches.size()) return catches.get(index-1);
        return finallyBlock;
    }

    public ASTNode getTryBlock() {
        return tryBlock;
    }

    public CatchClause[] getCatches() {
        return catches.toArray(new CatchClause[0]);
    }

    public ASTNode getFinallyBlock() {
        return finallyBlock;
    }
}

// Return节点
class ReturnStatement extends ASTNode {
    private final ASTNode value;

    public ReturnStatement(int line, ASTNode value) {
        super(line);
        this.value = value;
    }
    protected String pad(int indent) {
        return " ".repeat(indent * 2); // 每个缩进级别2个空格
    }

    @Override public String toString(int indent) {
        return pad(indent) + "ReturnStatement("
                + (value != null ? value.toString(0) : "") + ")";
    }

    @Override public int getChildCount() { return value != null ? 1 : 0; }
    @Override public ASTNode getChild(int index) {
        if (value == null) throw new IndexOutOfBoundsException();
        return value;
    }

    public ASTNode getValue() {
        return value;
    }
}

// Throw节点
class ThrowStatement extends ASTNode {
    private final ASTNode expression;

    public ThrowStatement(int line, ASTNode expr) {
        super(line);
        this.expression = expr;
    }
    protected String pad(int indent) {
        return " ".repeat(indent * 2); // 每个缩进级别2个空格
    }

    @Override public String toString(int indent) {
        return pad(indent) + "ThrowStatement(\n"
                + expression.toString(indent+2) + ")";
    }

    @Override public int getChildCount() { return 1; }
    @Override public ASTNode getChild(int index) { return expression; }

    public ASTNode getException() {
        return expression;
    }
}

// 辅助类（需要在外部定义）
class CatchClause extends ASTNode {
    private final String exceptionType;
    private final String varName;
    private final BlockStatement body;

    public CatchClause(String type, String var, BlockStatement body) {
        super(body.getLine());
        this.exceptionType = type;
        this.varName = var;
        this.body = body;
    }
    protected String pad(int indent) {
        return " ".repeat(indent * 2); // 每个缩进级别2个空格
    }

    @Override public String toString(int indent) {
        return pad(indent) + "Catch(" + exceptionType + " " + varName + ")\n"
                + body.toString(indent+2);
    }

    @Override public int getChildCount() { return 1; }
    @Override public ASTNode getChild(int index) { return body; }

    public String getVarName() {
        return varName;
    }

    public String getType() {
        return exceptionType;
    }

    public ASTNode getBody() {
        return body;
    }
}


// Switch语句中的case分支
class CaseStatement extends ASTNode {
    private final ASTNode caseExpr;
    protected final List<ASTNode> body;

    public CaseStatement(ASTNode caseExpr, List<ASTNode> body) {
        super(caseExpr.getLine());
        this.caseExpr = caseExpr;
        this.body = body;
    }
    public List<ASTNode> getBody() {
        return body;
    }


    protected String pad(int indent) {
        return " ".repeat(indent * 2); // 每个缩进级别2个空格
    }

    @Override
    public String toString(int indent) {
        StringBuilder sb = new StringBuilder(pad(indent) + "CaseStatement(\n");
        sb.append(pad(indent+2)).append("Condition:\n")
                .append(caseExpr.toString(indent+4)).append("\n")
                .append(pad(indent+2)).append("Body:\n");
        for (ASTNode stmt : body) {
            sb.append(stmt.toString(indent+4)).append("\n");
        }
        return sb.append(pad(indent) + ")").toString();
    }

    @Override
    public int getChildCount() {
        return 1 + body.size();
    }

    @Override
    public ASTNode getChild(int index) {
        if (index == 0) return caseExpr;
        return body.get(index-1);
    }

    public ASTNode getExpression() {
        return caseExpr;
    }
}

// default分支
class DefaultCaseStatement extends CaseStatement {
    public DefaultCaseStatement(List<ASTNode> body) {
        super(null, body);
    }

    @Override
    public String toString(int indent) {
        StringBuilder sb = new StringBuilder(pad(indent) + "DefaultCase(\n");
        for (ASTNode stmt : getBody()) { // 现在可以正确访问
            sb.append(stmt.toString(indent+2)).append("\n");
        }
        return sb.append(pad(indent) + ")").toString();
    }

    @Override
    public int getChildCount() {
        return getBody().size(); // 直接使用父类方法
    }

    @Override
    public ASTNode getChild(int index) {
        return getBody().get(index); // 直接访问body内容
    }
}

class FlowControlStatement extends ASTNode {
    private final String type; // "break" 或 "continue"
    private final String label;

    public FlowControlStatement(int line, String type, String label) {
        super(line);
        this.type = type;
        this.label = label;
    }

    protected String pad(int indent) {
        return " ".repeat(indent * 2); // 每个缩进级别2个空格
    }

    @Override
    public String toString(int indent) {
        String str = pad(indent) + type + "Statement";
        if (label != null) str += "(label=" + label + ")";
        return str;
    }

    @Override
    public int getChildCount() { return 0; }

    @Override
    public ASTNode getChild(int index) {
        throw new IndexOutOfBoundsException();
    }

    public String getLabel() {
        return label;
    }

    public String getValue() {
        return type;
    }
}
