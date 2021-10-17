package org.cloudburstmc.api.util.function;

@FunctionalInterface
public interface ToFloatBiFunction<T, U> {

    float apply(T t, U u);
}
