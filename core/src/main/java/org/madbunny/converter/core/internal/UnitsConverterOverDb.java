package org.madbunny.converter.core.internal;

import org.madbunny.converter.core.api.UnitsConverter;
import org.madbunny.converter.core.api.UnitsDatabase;
import org.madbunny.converter.core.api.exceptions.ExpressionTokenizationException;
import org.madbunny.converter.core.api.exceptions.ImpossibleToConvertException;
import org.madbunny.converter.core.api.exceptions.UnknownUnitsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

public class UnitsConverterOverDb implements UnitsConverter {
    private static class UnitsCounter {
        private final Map<String, Integer> counter = new HashMap<>();

        private UnitsCounter(String[] units) {
            for (var unit : units) {
                counter.merge(unit, 1, Integer::sum);
            }
        }

        public boolean tryRemove(String unit) {
            int count = counter.getOrDefault(unit, 0);
            if (count <= 0) {
                return false;
            }

            if (count == 1) {
                counter.remove(unit);
            } else {
                counter.put(unit, count - 1);
            }

            return true;
        }

        public boolean isEmpty() {
            return counter.isEmpty();
        }
    }

    static private final Logger LOG = LoggerFactory.getLogger(UnitsDatabaseOverCsvFile.class);
    private static final int SIGNIFICANT_DIGITS = 100;
    private static final MathContext MATH_CONTEXT = new MathContext(SIGNIFICANT_DIGITS, RoundingMode.CEILING);

    private final UnitsExpressionTokenizer tokenizer;
    private final WeightedGraph unitsGraph;

    public UnitsConverterOverDb(UnitsDatabase unitsDatabase) {
        LOG.info("Creating " + this.getClass().getSimpleName());
        tokenizer = new UnitsExpressionTokenizer(unitsDatabase);
        unitsGraph = buildGraph(unitsDatabase);
        LOG.info("Creation of " + this.getClass().getSimpleName() + " complete");
    }

    @Override
    public BigDecimal convert(String from, String to) throws
            UnknownUnitsException,
            ImpossibleToConvertException,
            ExpressionTokenizationException {
        var fromTokens = tokenizer.tokenize(from);
        var toTokens = tokenizer.tokenize(to);
        checkExpressions(fromTokens, toTokens);

        var numerator = doConvert(fromTokens.getNumerator(), toTokens.getNumerator());
        if (fromTokens.getDenominator().isPresent()) {
            var fromDen = fromTokens.getDenominator().get();
            var toDen = toTokens.getDenominator().get();
            var denominator = doConvert(fromDen, toDen);
            if (denominator.equals(BigDecimal.ZERO)) {
                onImpossibleToConvert(fromDen, toDen);
            }
            return numerator.divide(denominator, MATH_CONTEXT);
        }

        return numerator;
    }

    private BigDecimal doConvert(String[] from, String[] to) throws ImpossibleToConvertException {
        var counter = new UnitsCounter(to);
        var coeff = BigDecimal.ONE;
        for (var fromUnit : from) {
            var result = unitsGraph.traverseBreadthFirst(fromUnit, MATH_CONTEXT, (edge) -> {
                if (counter.tryRemove(edge.to)) {
                    return WeightedGraph.TraversalState.STOP;
                }
                return WeightedGraph.TraversalState.CONTINUE;
            });

            if (result.isEmpty()) {
                onImpossibleToConvert(from, to);
            }
            coeff = coeff.multiply(result.get(), MATH_CONTEXT);
        }

        if (!counter.isEmpty()) {
            onImpossibleToConvert(from, to);
        }

        return coeff;
    }

    private static void checkExpressions(TokenizedUnitsExpression from, TokenizedUnitsExpression to) throws
            ImpossibleToConvertException {
        compareExpressionsLengths(from.getNumerator(), to.getNumerator());

        var maybeFromDen = from.getDenominator();
        var maybeToDen = to.getDenominator();
        var areEmpty = maybeFromDen.isEmpty() && maybeToDen.isEmpty();
        var arePresent = maybeFromDen.isPresent() && maybeToDen.isPresent();
        var areOfSameStatus = areEmpty || arePresent;
        if (!areOfSameStatus) {
            var fromDen = maybeFromDen.orElse(new String[0]);
            var toDen = maybeToDen.orElse(new String[0]);
            onImpossibleToConvert(fromDen, toDen);
        } else if (arePresent) {
            compareExpressionsLengths(maybeFromDen.get(), maybeToDen.get());
        }
    }

    private static void compareExpressionsLengths(String[] from, String[] to) throws ImpossibleToConvertException {
        if (from.length != to.length) {
            onImpossibleToConvert(from, to);
        }
    }

    private static String asMultiplication(String[] units) {
        return String.join("*", units);
    }

    private static void onImpossibleToConvert(String[] from, String[] to) throws ImpossibleToConvertException {
        throw new ImpossibleToConvertException(asMultiplication(from), asMultiplication(to));
    }

    private static WeightedGraph buildGraph(UnitsDatabase unitsDatabase) {
        LOG.info("Creating " + WeightedGraph.class.getSimpleName());

        var builder = new WeightedGraph.Builder();
        unitsDatabase.traverseDirectRelations((relation) -> {
            var invAmount = BigDecimal.ONE.divide(relation.amount, MATH_CONTEXT);
            builder.withEdge(relation.from, relation.to, relation.amount);
            builder.withEdge(relation.to, relation.from, invAmount);
        });

        LOG.info("Creation of " + WeightedGraph.class.getSimpleName() + " complete");
        return builder.build();
    }
}
