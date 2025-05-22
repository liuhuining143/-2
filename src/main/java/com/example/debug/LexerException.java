package com.example.debug;

import java.util.List;

public class LexerException extends Exception {
    private final List<Token> tokens;
    private final List<ErrorEntry> errors;

    public LexerException(String message, List<Token> tokens, List<ErrorEntry> errors) {
        super(message);
        this.tokens = tokens;
        this.errors = errors;
    }

    public List<Token> getTokens() {
        return tokens;
    }

    public List<ErrorEntry> getErrors() {
        return errors;
    }
}
