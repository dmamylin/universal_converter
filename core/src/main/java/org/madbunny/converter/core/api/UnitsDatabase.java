package org.madbunny.converter.core.api;

import java.util.function.Consumer;

public interface UnitsDatabase {
    void traverseDirectRelations(Consumer<UnitsRelation> visitor);
    boolean containsUnit(String unit);
}
