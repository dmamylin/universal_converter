package org.madbunny.converter.core.internal;

import java.util.Optional;

public class TokenizedUnitsExpression {
    public static class Builder {
        private String[] numerator;
        private String[] denominator;

        Builder withNumerator(String[] units) {
            numerator = units;
            return this;
        }

        Builder withDenominator(String[] units) {
            denominator = units;
            return this;
        }

        TokenizedUnitsExpression build() {
            return new TokenizedUnitsExpression(numerator, denominator);
        }
    }

    private final String[] numerator;
    private final String[] denominator;

    private TokenizedUnitsExpression(String[] numerator, String[] denominator) {
        this.numerator = numerator;
        this.denominator = denominator;
    }

    public String[] getNumerator() {
        return numerator;
    }

    public Optional<String[]> getDenominator() {
        return denominator == null || denominator.length == 0 ? Optional.empty() : Optional.of(denominator);
    }

    public String toString() {
        var result = String.join("*", numerator);
        var maybeDen = getDenominator();
        if (maybeDen.isPresent()) {
            var den = maybeDen.get();
            result += "/" + String.join("*", denominator);
        }
        return result;
    }
}
