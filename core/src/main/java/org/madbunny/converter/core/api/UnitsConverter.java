package org.madbunny.converter.core.api;

import org.madbunny.converter.core.api.exceptions.ExpressionTokenizationException;
import org.madbunny.converter.core.api.exceptions.ImpossibleToConvertException;
import org.madbunny.converter.core.api.exceptions.UnknownUnitsException;

import java.math.BigDecimal;

public interface UnitsConverter {
    BigDecimal convert(String from, String to) throws UnknownUnitsException, ImpossibleToConvertException, ExpressionTokenizationException;
}
