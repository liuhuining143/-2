package com.example.debug.AST;

public class Parameter {
    private final String type;
    private final String name;

    public Parameter(String type, String name) {
        this.type = type;
        this.name = name;
    }

    @Override
    public String toString() {
        return type + " 类型的参数 " + name;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

}
