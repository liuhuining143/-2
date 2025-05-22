package com.example.debug;

import com.example.debug.AST.ASTNode;

import java.util.List;

class ParseResult {
    final ASTNode astRoot;
    final List<ErrorEntry> errors;

    ParseResult(ASTNode astRoot, List<ErrorEntry> errors) {
        this.astRoot = astRoot;
        this.errors = errors;
    }
}