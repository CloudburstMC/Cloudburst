package org.cloudburstmc.api.util.function;

import java.util.function.ObjDoubleConsumer;

@FunctionalInterface
public interface TriConsumer<T, U, V> {

    void accept(T t, U u, V v);
}
