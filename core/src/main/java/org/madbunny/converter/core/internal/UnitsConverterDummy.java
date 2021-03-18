package org.madbunny.converter.core.internal;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.madbunny.converter.core.api.UnitsConverter;
import org.madbunny.converter.core.api.exceptions.ImpossibleToConvertException;
import org.madbunny.converter.core.api.exceptions.UnknownUnitsException;

public class UnitsConverterDummy implements UnitsConverter {
    private static final Set<String> distanceUnits = new HashSet<>(Arrays.asList("mm", "cm", "m", "km"));
    private static final Set<String> massUnits = new HashSet<>(Arrays.asList("g", "mg", "kg"));
    private static final Set<String> knownUnits = new HashSet<>(){{
        addAll(distanceUnits);
        addAll(massUnits);
    }};

    @Override
    public BigDecimal convert(String from, String to) throws UnknownUnitsException, ImpossibleToConvertException {
        checkUnits(from, to);
        checkPossibilityToConvert(from, to);
        return new BigDecimal("0.01234567890123456789012");
    }

    private static void checkUnits(String... units) throws UnknownUnitsException {
        var badUnits = new ArrayList<String>();
        for (var unit : units) {
            if (!knownUnits.contains(unit)) {
                badUnits.add(unit);
            }
        }

        if (!badUnits.isEmpty()) {
            var array = badUnits.toArray(new String[0]);
            throw new UnknownUnitsException(array);
        }
    }

    private static void checkPossibilityToConvert(String from, String to) throws ImpossibleToConvertException {
        boolean areDifferent = (isDistance(from) && isMass(to)) || (isMass(from) && isDistance(to));
        if (areDifferent) {
            throw new ImpossibleToConvertException(from, to);
        }
    }

    private static boolean isDistance(String unit) {
        return distanceUnits.contains(unit);
    }

    private static boolean isMass(String unit) {
        return massUnits.contains(unit);
    }
}
