package org.madbunny.converter.core.api;

import org.madbunny.converter.core.internal.UnitsConverterDummy;
import org.madbunny.converter.core.internal.UnitsConverterOverDb;

public abstract class UnitsConverterFactory {
    public static UnitsConverter createDummyConverter() {
        return new UnitsConverterDummy();
    }

    public static UnitsConverter createOverDb(UnitsDatabase unitsDatabase) {
        return new UnitsConverterOverDb(unitsDatabase);
    }
}
