package org.cloudburstmc.api.data;

import org.cloudburstmc.api.util.Identifier;

import java.util.function.Function;

public final class BehaviorKey<F, E> implements DataKey<F, E> {

    private final Identifier id;
    private final Class<F> functionType;
    private final Class<E> executorType;

    BehaviorKey(Identifier id, Class<F> functionType, Class<E> executorType) {
        this.id = id;
        this.functionType = functionType;
        this.executorType = executorType;
    }

    @Override
    public Identifier getId() {
        return id;
    }

    @Override
    public Class<F> getType() {
        return functionType;
    }

    @Override
    public Class<E> getMutableType() {
        return executorType;
    }

    public Class<E> getExecutorType() {
        return executorType;
    }

    @Override
    public Function<E, F> getImmutableFunction() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Function<F, E> getMutableFunction() {
        throw new UnsupportedOperationException();
    }
}
