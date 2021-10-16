package org.cloudburstmc.api.data;

import com.google.common.collect.ImmutableList;
import org.cloudburstmc.api.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@SuppressWarnings({"unchecked", "rawtypes"})
public final class ListDataKey<T> implements DataKey<List<T>, List<T>> {

    private final Identifier id;
    private final Class<T> type;

    ListDataKey(Identifier id, Class<T> type) {
        this.id = id;
        this.type = type;
    }

    @Override
    public Identifier getId() {
        return id;
    }

    @Override
    public Class<List<T>> getType() {
        return (Class) ImmutableList.class;
    }

    @Override
    public Class<List<T>> getMutableType() {
        return (Class) List.class;
    }

    @Override
    public Function<List<T>, List<T>> getImmutableFunction() {
        return ImmutableList::copyOf;
    }

    @Override
    public Function<List<T>, List<T>> getMutableFunction() {
        return ArrayList::new;
    }
}
