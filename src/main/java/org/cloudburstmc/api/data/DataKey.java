package org.cloudburstmc.api.data;

import org.cloudburstmc.api.util.Identifier;

import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;

public sealed interface DataKey<T, M> permits BehaviorKey, ListDataKey, SimpleDataKey {

    @SuppressWarnings("unchecked")
    static <T> SimpleDataKey<T> simple(Identifier id, Class<? super T> type) {
        checkNotNull(id, "id");
        checkNotNull(type, "type");
        return new SimpleDataKey<>(id, (Class<T>) type);
    }

    static <T> ListDataKey<T> list(Identifier id, Class<T> type) {
        checkNotNull(id, "id");
        checkNotNull(type, "type");
        return new ListDataKey<>(id, type);
    }

    static <T> BehaviorKey<T, T> behavior(Identifier id, Class<T> type) {
        return behavior(id, type, type);
    }

    @SuppressWarnings("unchecked")
    static <T, R> BehaviorKey<T, R> behavior(Identifier id, Class<? super T> functionType, Class<? super R> returnType) {
        checkNotNull(id, "id");
        checkNotNull(functionType, "functionType");
        checkNotNull(returnType, "returnType");
        return new BehaviorKey<>(id, (Class<T>) functionType, (Class<R>) returnType);
    }

    Identifier getId();

    Class<T> getType();

    Class<M> getMutableType();

    Function<M, T> getImmutableFunction();

    Function<T, M> getMutableFunction();
}
