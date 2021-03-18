package org.madbunny.converter.core.api;

import org.madbunny.converter.core.api.exceptions.DatabaseCreationException;
import org.madbunny.converter.core.internal.UnitsDatabaseOverCsvFile;

public abstract class UnitsDatabaseFactory {
    public static UnitsDatabase createFromCsvFile(String fileName) throws DatabaseCreationException {
        return new UnitsDatabaseOverCsvFile(fileName);
    }
}
