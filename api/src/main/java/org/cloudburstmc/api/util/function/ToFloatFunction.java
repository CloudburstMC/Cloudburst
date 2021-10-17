package org.cloudburstmc.api.util.function;

@FunctionalInterface
public interface ToFloatFunction<T> {

    float apply(T t);
}
