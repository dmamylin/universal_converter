package org.madbunny.converter.core.internal;

import com.opencsv.CSVReader;
import org.madbunny.converter.core.api.UnitsDatabase;
import org.madbunny.converter.core.api.UnitsRelation;
import org.madbunny.converter.core.api.exceptions.DatabaseCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class UnitsDatabaseOverCsvFile implements UnitsDatabase {
    static private final Logger LOG = LoggerFactory.getLogger(UnitsDatabaseOverCsvFile.class);
    private static final int SIGNIFICANT_DIGITS = 100;
    private static final MathContext MATH_CONTEXT = new MathContext(SIGNIFICANT_DIGITS, RoundingMode.CEILING);

    private final Set<String> units = new HashSet<>();
    private final RelationsStorage storage = new RelationsStorage();

    private static class LineParsingContext {
        public final String fileName;
        public final int lineNumber;
        public final String[] line;

        public LineParsingContext(String fileName, int lineNumber, String[] line) {
            this.fileName = fileName;
            this.lineNumber = lineNumber;
            this.line = line;
        }

        public String getLineRef() {
            return String.format("line: \"%s\" at %s:%d", String.join(",", line), fileName, lineNumber);
        }
    }

    private static class RelationsStorage {
        private final Map<String, Map<String, BigDecimal>> storage = new HashMap<>();

        void insert(UnitsRelation relation) throws DatabaseCreationException {
            var toMap = storage.get(relation.from);
            if (toMap == null) {
                var newToMap = new HashMap<String, BigDecimal>();
                newToMap.put(relation.to, relation.amount);
                storage.put(relation.from, newToMap);
                return;
            }

            // Check if there is a conflicting entry in the db: with same from and to, but different amount
            var existingAmount = toMap.get(relation.to);
            if (existingAmount != null) {
                if (existingAmount.equals(relation.amount)) {
                    return;
                } else {
                    throw new DatabaseCreationException(String.format("Conflicting entries in the db: %s != %s",
                            existingAmount.toPlainString(),
                            relation.amount.toPlainString()
                    ));
                }
            }

            /* Check if there is a conflicting backward entry in the db: where otherFrom=to and otherTo=from, but
             * otherAmount != 1.0/amount */
            var backwardToMap = storage.get(relation.to);
            if (backwardToMap != null) {
                var backwardAmount = backwardToMap.get(relation.from);
                if (backwardAmount != null) {
                    var invertedAmount = BigDecimal.ONE.divide(relation.amount, MATH_CONTEXT);
                    if (!backwardAmount.equals(invertedAmount)) {
                        throw new DatabaseCreationException(String.format("Conflicting entries in the db: %s != %s",
                                backwardAmount.toPlainString(),
                                invertedAmount.toPlainString()
                        ));
                    }
                }
            }

            toMap.put(relation.to, relation.amount);
        }

        public void traverse(Consumer<UnitsRelation> visitor) {
            storage.forEach((from, toMap) -> {
                toMap.forEach((to, amount) -> {
                    visitor.accept(new UnitsRelation(from, to, amount));
                });
            });
        }
    }

    public UnitsDatabaseOverCsvFile(String csvFileName) throws DatabaseCreationException {
        LOG.info("Creating " + this.getClass().getSimpleName() + " from file: " + csvFileName);
        try (var reader = new CSVReader(new FileReader(csvFileName))) {
            String[] line;
            int lineNumber = 1;
            while ((line = reader.readNext()) != null) {
                var ctx = new LineParsingContext(csvFileName, lineNumber, line);
                var unitsRelation = parseLine(ctx);
                var areSameUnits = unitsRelation.from.equals(unitsRelation.to);
                if (areSameUnits && !unitsRelation.amount.equals(BigDecimal.ONE)) {
                    onError(ctx, String.format("Conversion between the same units is not 1 but: \"%s\"",
                            unitsRelation.amount.toPlainString()));
                }

                units.add(unitsRelation.from);
                units.add(unitsRelation.to);
                if (!areSameUnits) {
                    storage.insert(unitsRelation);
                }
                lineNumber++;
            }
        } catch (Exception e) {
            LOG.error("Creation of " + this.getClass().getSimpleName() + " failed");
            throw new DatabaseCreationException(e.getMessage());
        }
        LOG.info("Creation of " + this.getClass().getSimpleName() + " complete");
    }

    @Override
    public void traverseDirectRelations(Consumer<UnitsRelation> visitor) {
        storage.traverse(visitor);
    }

    @Override
    public boolean containsUnit(String unit) {
        return units.contains(unit);
    }

    private static UnitsRelation parseLine(LineParsingContext ctx) throws DatabaseCreationException {
        if (ctx.line.length != 3) {
            onError(ctx, ctx.line.length < 3 ? "Not enough values to unpack" : "Too many value to unpack");
        }

        var from = parseNonEmptyString(ctx, ctx.line[0], false);
        var to = parseNonEmptyString(ctx, ctx.line[1], false);
        var amount = parseAmount(ctx, ctx.line[2]);
        return new UnitsRelation(from, to, amount);
    }

    private static String parseNonEmptyString(LineParsingContext ctx, String value, boolean keepSpaces) throws DatabaseCreationException {
        if (!keepSpaces) {
            value = value.replaceAll("\\s+", value);
        }

        if (value.length() == 0) {
            onError(ctx, "Empty value");
        }
        return value;
    }

    private static BigDecimal parseAmount(LineParsingContext ctx, String value) throws DatabaseCreationException {
        value = parseNonEmptyString(ctx, value, true).strip();
        BigDecimal result = null;
        try {
            result = new BigDecimal(value);
        } catch (NumberFormatException exception) {
            onError(ctx, String.format("Incorrect number: \"%s\"", value));
        }

        if (result.equals(BigDecimal.ZERO)) {
            onError(ctx, "Amount of units could not be zero");
        }
        return result;
    }

    private static void onError(LineParsingContext ctx, String message) throws DatabaseCreationException {
        throw new DatabaseCreationException(message + ", " + ctx.getLineRef());
    }
}
