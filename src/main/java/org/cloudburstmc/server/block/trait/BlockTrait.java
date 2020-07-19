package org.cloudburstmc.server.block.trait;

import java.util.List;
import java.util.function.Predicate;

public interface BlockTrait<E extends Comparable<E>> {

    String getName();

    List<E> getPossibleValues();

    E getDefaultValue();

    Class<E> getValueClass();

    default Predicate<E> getValidator() {
        return e -> this.getPossibleValues().contains(e);
    }

    int getIndex(Object value);
}
