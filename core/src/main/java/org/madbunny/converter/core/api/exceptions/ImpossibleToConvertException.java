package org.madbunny.converter.core.api.exceptions;

public class ImpossibleToConvertException extends Exception {
    public ImpossibleToConvertException(String from, String to) {
        super("Impossible to convert expressions");
        this.from = from;
        this.to = to;
    }

    public final String from;
    public final String to;
}
