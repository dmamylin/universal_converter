package org.madbunny.converter.core.api.exceptions;

public class EmptyExpressionException extends ExpressionTokenizationException {
    public EmptyExpressionException() {
        super("An expression is empty");
    }
}
