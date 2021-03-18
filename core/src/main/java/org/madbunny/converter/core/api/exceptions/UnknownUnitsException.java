package org.madbunny.converter.core.api.exceptions;

public class UnknownUnitsException extends Exception {
    public UnknownUnitsException(String... unknownUnits) {
        super("Unknown units");
        this.unknownUnits = unknownUnits;
    }

    public final String[] unknownUnits;
}
