package org.madbunny.converter.core.api;

import java.math.BigDecimal;

public class UnitsRelation {
    public final String from;

    public final String to;

    // Amount of 'to' unit in one piece of 'from' unit
    public final BigDecimal amount;

    public UnitsRelation(String from, String to, BigDecimal amount) {
        this.from = from;
        this.to = to;
        this.amount = amount;
    }
}
