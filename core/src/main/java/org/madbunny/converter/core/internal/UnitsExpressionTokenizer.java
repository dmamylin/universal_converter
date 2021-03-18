package org.madbunny.converter.core.internal;

import org.madbunny.converter.core.api.UnitsDatabase;
import org.madbunny.converter.core.api.exceptions.EmptyExpressionException;
import org.madbunny.converter.core.api.exceptions.IncorrectExpressionException;
import org.madbunny.converter.core.api.exceptions.UnknownUnitsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class UnitsExpressionTokenizer {
    static private final Logger LOG = LoggerFactory.getLogger(UnitsDatabaseOverCsvFile.class);
    private static final String TOKEN_MUL = "\\*";
    private static final String TOKEN_DIV = "/";
    private static final int MAX_DIV_TOKENS = 1;

    private final UnitsDatabase unitsDatabase;

    public UnitsExpressionTokenizer(UnitsDatabase unitsDatabase) {
        LOG.info("Creating " + this.getClass().getSimpleName());
        this.unitsDatabase = unitsDatabase;
        LOG.info("Creation of " + this.getClass().getSimpleName() + " complete");
    }

    public TokenizedUnitsExpression tokenize(String expression) throws
            EmptyExpressionException,
            IncorrectExpressionException,
            UnknownUnitsException {
        var withoutSpaces = expression.replaceAll("\\s+", "");
        var mulExpressions = withoutSpaces.split(TOKEN_DIV);
        if (mulExpressions.length == 0) {
            throw new EmptyExpressionException();
        }

        var divTokens = mulExpressions.length - 1;
        if (divTokens > MAX_DIV_TOKENS) {
            throw new IncorrectExpressionException(String.format("Too many division tokens: %d", divTokens));
        }

        var numeratorAndDenominator = new ArrayList<String[]>();
        for (var expr : mulExpressions) {
            var units = expr.split(TOKEN_MUL);
            if (units.length == 0) {
                throw new EmptyExpressionException();
            }

            for (var unit : units) {
                if (!unitsDatabase.containsUnit(unit)) {
                    throw new UnknownUnitsException(unit);
                }
            }

            numeratorAndDenominator.add(units);
        }

        return buildTokenized(numeratorAndDenominator);
    }

    private static TokenizedUnitsExpression buildTokenized(ArrayList<String[]> numeratorAndDenominator) {
        var builder = new TokenizedUnitsExpression.Builder()
                .withNumerator(numeratorAndDenominator.get(0));
        if (numeratorAndDenominator.size() > 1) {
            builder.withDenominator(numeratorAndDenominator.get(1));
        }
        return builder.build();
    }
}
