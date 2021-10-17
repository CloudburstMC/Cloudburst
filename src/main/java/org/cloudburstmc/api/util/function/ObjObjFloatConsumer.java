package org.cloudburstmc.api.util.function;

@FunctionalInterface
public interface ObjObjFloatConsumer<T, U> {

    void accept(T t, U u, float value);
}
