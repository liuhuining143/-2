package com.example.debug.Symbol;

public class Quadruple {
    private String op; // 操作符
    private String arg1; // 操作数 1
    private String arg2; // 操作数 2
    private String result; // 结果

    public Quadruple(String op, String arg1, String arg2, String result) {
        this.op = op;
        this.arg1 = arg1;
        this.arg2 = arg2;
        this.result = result;
    }

    public String getOp() {
        return op;
    }

    public String getArg1() {
        return arg1;
    }

    public String getArg2() {
        return arg2;
    }

    public String getResult() {
        return result;
    }

    @Override
    public String toString() {
        return "(" + op + ", " + arg1 + ", " + arg2 + ", " + result + ")";
    }
}
