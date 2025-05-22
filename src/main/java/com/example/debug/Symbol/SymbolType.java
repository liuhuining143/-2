package com.example.debug.Symbol;

import com.example.debug.AST.Parameter;

import java.util.List;
import java.util.Stack;

public class SymbolType {
    private String name;
    private String type;
    private int line;
    private Stack<String> scope;
    private String value;
    private String Modifier;
    private String returnType;
    private List<Parameter> parameters;
    private String dataType;


    public SymbolType() {
        this.scope = new Stack<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public Stack<String> getScope() {
        return scope;
    }

    public void setScope(Stack<String> scope) {
        this.scope = scope;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getModifier() {
        return Modifier;
    }

    public void setModifier(String modifier) {
        Modifier = modifier;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<Parameter> parameters) {
        this.parameters = parameters;
    }

    @Override
    public String toString() {
        return "SymbolType{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", line=" + line +
                ", scope=" + scopeToString() +
                ", value='" + value + '\'' +
                ", modifier='" + Modifier + '\'' +
                ", returnType='" + returnType + '\'' +
                ", parameters=" + parametersToString() +
                '}';
    }

    private String scopeToString() {
        if (scope == null || scope.isEmpty()) {
            return "";
        }
        // 使用 StringBuilder 拼接作用域信息
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < scope.size(); i++) {
            if (i > 0) {
                sb.append(" -> ");
            }
            sb.append(scope.get(i));
        }
        return sb.toString();
    }

    private String parametersToString() {
        if (parameters == null || parameters.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parameters.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(parameters.get(i).toString());
        }
        return sb.toString();
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public String getDataType() {
        return dataType;
    }
}
