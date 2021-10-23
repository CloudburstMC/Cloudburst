package org.cloudburstmc.api.data;

import org.cloudburstmc.api.util.Identifier;

import java.util.function.Function;

public final class BehaviorKey<T, R> implements DataKey<T, R> {

    private final Identifier id;
    private final Class<T> functionType;
    private final Class<R> executorType;

    BehaviorKey(Identifier id, Class<T> functionType, Class<R> executorType) {
        this.id = id;
        this.functionType = functionType;
        this.executorType = executorType;
    }

    @Override
    public Identifier getId() {
        return id;
    }

    @Override
    public Class<T> getType() {
        return functionType;
    }

    @Override
    public Class<R> getMutableType() {
        return executorType;
    }

    public Class<R> getExecutorType() {
        return executorType;
    }

    @Override
    public Function<R, T> getImmutableFunction() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Function<T, R> getMutableFunction() {
        throw new UnsupportedOperationException();
    }
}
