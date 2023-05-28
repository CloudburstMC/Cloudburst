package org.cloudburstmc.api.level.gamerule;

public interface GameRule<T extends Comparable<T>> {

    String getName();

    Class<T> getValueClass();

    T getDefaultValue();

    T parse(String value);
}
