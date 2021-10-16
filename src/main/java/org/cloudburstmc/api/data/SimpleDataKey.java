package org.cloudburstmc.api.data;

import org.cloudburstmc.api.util.Identifier;

import java.util.function.Function;

public final class SimpleDataKey<T> implements DataKey<T, T> {

    private final Identifier id;
    private final Class<T> type;

    SimpleDataKey(Identifier id, Class<T> type) {
        this.id = id;
        this.type = type;
    }

    @Override
    public Identifier getId() {
        return id;
    }

    @Override
    public Class<T> getType() {
        return type;
    }

    @Override
    public Class<T> getMutableType() {
        return type;
    }

    @Override
    public Function<T, T> getImmutableFunction() {
        return Function.identity();
    }

    @Override
    public Function<T, T> getMutableFunction() {
        return Function.identity();
    }
}
