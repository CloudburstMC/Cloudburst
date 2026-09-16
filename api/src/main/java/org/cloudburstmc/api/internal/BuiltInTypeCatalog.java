package org.cloudburstmc.api.internal;

import org.cloudburstmc.api.util.Identifier;

import java.util.*;
import java.util.function.Function;

/**
 * Collects built-in API constants as they are declared.
 *
 * @param <T> constant type
 */
public final class BuiltInTypeCatalog<T> {
    private final Function<? super T, Identifier> identifierFunction;
    private final Map<Identifier, T> valuesByIdentifier = new HashMap<>();
    private final List<T> values = new ArrayList<>();
    private final List<T> valueView = Collections.unmodifiableList(this.values);

    private BuiltInTypeCatalog(Function<? super T, Identifier> identifierFunction) {
        this.identifierFunction = Objects.requireNonNull(identifierFunction, "identifierFunction");
    }

    public static <T> BuiltInTypeCatalog<T> create(Function<? super T, Identifier> identifierFunction) {
        return new BuiltInTypeCatalog<>(identifierFunction);
    }

    public <V extends T> V register(V value) {
        Objects.requireNonNull(value, "value");
        Identifier identifier = Objects.requireNonNull(this.identifierFunction.apply(value), "identifier");

        T existing = this.valuesByIdentifier.putIfAbsent(identifier, value);
        if (existing != null) {
            throw new IllegalStateException("Duplicate built-in type identifier " + identifier);
        }

        this.values.add(value);
        return value;
    }

    public Optional<T> get(Identifier identifier) {
        return Optional.ofNullable(this.valuesByIdentifier.get(Objects.requireNonNull(identifier, "identifier")));
    }

    public List<T> values() {
        return this.valueView;
    }
}
