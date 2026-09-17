package org.cloudburstmc.api.item;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.util.Identifier;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Identifies a typed value stored on an {@link ItemStack}.
 *
 * <p>A component type also defines how values are copied before they enter an immutable item stack.
 * Factory methods register canonical custom component types and reject identifiers in the reserved
 * {@code minecraft} namespace. Whether a component is persisted is determined by the server's item
 * serialization configuration.</p>
 *
 * @param <T> component value type
 */
public class ItemDataComponentType<T> {

    private static final Map<Identifier, ItemDataComponentType<?>> TYPES = new LinkedHashMap<>();

    private final Identifier id;
    private final Class<?> valueType;
    private final List<Class<?>> typeParameters;
    private final UnaryOperator<T> immutableCopy;
    private final @Nullable T emptyValue;

    private ItemDataComponentType(Identifier id, Class<?> valueType, List<Class<?>> typeParameters, UnaryOperator<T> immutableCopy, @Nullable T emptyValue) {
        this.id = checkNotNull(id, "id");
        this.valueType = checkNotNull(valueType, "valueType");
        this.typeParameters = List.copyOf(checkNotNull(typeParameters, "typeParameters"));
        this.immutableCopy = checkNotNull(immutableCopy, "immutableCopy");
        this.emptyValue = emptyValue;
    }

    /**
     * Creates a component type whose values are already immutable.
     *
     * @param id        component identifier
     * @param valueType component value class
     * @param <T>       component value type
     * @return component type
     */
    public static <T> ItemDataComponentType<T> value(Identifier id, Class<? super T> valueType) {
        checkCustomId(id);
        return copied(id, valueType, UnaryOperator.identity());
    }

    /**
     * Creates a boolean-valued component type for which {@code false} is equivalent to absence.
     *
     * @param id component identifier
     * @return component type
     */
    public static ItemDataComponentType<Boolean> booleanValue(Identifier id) {
        checkCustomId(id);
        return register(id, Boolean.class, List.of(), UnaryOperator.identity(), false);
    }

    /**
     * Creates a component type with an immutable-copy operation.
     *
     * @param id            component identifier
     * @param valueType     component value class
     * @param immutableCopy operation that returns an immutable value
     * @param <T>           component value type
     * @return component type
     */
    public static <T> ItemDataComponentType<T> copied(Identifier id, Class<? super T> valueType, UnaryOperator<T> immutableCopy) {
        checkCustomId(id);
        return copied0(id, valueType, immutableCopy);
    }

    /**
     * Creates a list-valued component type. Stored lists are immutable snapshots.
     *
     * @param id          component identifier
     * @param elementType list element class
     * @param <E>         list element type
     * @return component type
     */
    public static <E> ItemDataComponentType<List<E>> list(Identifier id, Class<E> elementType) {
        checkCustomId(id);
        return list0(id, elementType);
    }

    /**
     * Creates a map-valued component type. Stored maps are immutable snapshots.
     *
     * @param id        component identifier
     * @param keyType   map key class
     * @param valueType map value class
     * @param <K>       map key type
     * @param <V>       map value type
     * @return component type
     */
    public static <K, V> ItemDataComponentType<Map<K, V>> map(Identifier id, Class<K> keyType, Class<V> valueType) {
        checkCustomId(id);
        return registerMap(id, keyType, valueType);
    }

    /**
     * Returns a registered component type.
     *
     * @param id component identifier
     * @return matching component type, if registered
     */
    public static synchronized Optional<ItemDataComponentType<?>> get(Identifier id) {
        return Optional.ofNullable(TYPES.get(checkNotNull(id, "id")));
    }

    /**
     * Returns all registered component types.
     *
     * @return component types in registration order
     */
    public static synchronized List<ItemDataComponentType<?>> values() {
        return List.copyOf(TYPES.values());
    }

    /**
     * Returns the component identifier.
     *
     * @return component identifier
     */
    public Identifier getId() {
        return id;
    }

    /**
     * Returns an immutable snapshot suitable for storage on an item stack.
     *
     * @param value component value
     * @return immutable value
     */
    public T copyValue(T value) {
        return immutableCopy.apply(checkNotNull(value, "value"));
    }

    /**
     * Returns the value considered equivalent to an absent component when stacks are compared for merging.
     *
     * @return equivalent empty value, or {@code null} when absence has no value equivalent
     */
    public @Nullable T getEmptyValue() {
        return emptyValue;
    }

    @Override
    public String toString() {
        return id.toString();
    }

    static <T> ItemDataComponentType<T> builtInValue(Identifier id, Class<? super T> valueType) {
        return copied0(id, valueType, UnaryOperator.identity());
    }

    static ItemDataComponentType<Boolean> builtInBooleanValue(Identifier id) {
        return register(id, Boolean.class, List.of(), UnaryOperator.identity(), false);
    }

    static <E> ItemDataComponentType<List<E>> builtInList(Identifier id, Class<E> elementType) {
        return list0(id, elementType);
    }

    private static <E> ItemDataComponentType<List<E>> list0(Identifier id, Class<E> elementType) {
        checkNotNull(elementType, "elementType");
        return register(id, List.class, List.of(elementType), ImmutableList::copyOf, ImmutableList.of());
    }

    static <K, V> ItemDataComponentType<Map<K, V>> registerMap(Identifier id, Class<K> keyType, Class<V> valueType) {
        checkNotNull(keyType, "keyType");
        checkNotNull(valueType, "valueType");
        return register(id, Map.class, List.of(keyType, valueType), ImmutableMap::copyOf, ImmutableMap.of());
    }

    private static <T> ItemDataComponentType<T> copied0(Identifier id, Class<? super T> valueType, UnaryOperator<T> immutableCopy) {
        return register(id, valueType, List.of(), immutableCopy, null);
    }

    private static void checkCustomId(Identifier id) {
        checkNotNull(id, "id");
        if ("minecraft".equals(id.getNamespace())) {
            throw new IllegalArgumentException("The minecraft namespace is reserved for built-in item data components");
        }
    }

    @SuppressWarnings("unchecked")
    private static synchronized <T> ItemDataComponentType<T> register(
            Identifier id, Class<?> valueType, List<Class<?>> typeParameters,
            UnaryOperator<T> immutableCopy, @Nullable T emptyValue) {
        checkNotNull(id, "id");
        checkNotNull(valueType, "valueType");
        checkNotNull(typeParameters, "typeParameters");
        checkNotNull(immutableCopy, "immutableCopy");

        ItemDataComponentType<?> existing = TYPES.get(id);
        if (existing != null) {
            if (!existing.valueType.equals(valueType) || !existing.typeParameters.equals(typeParameters)) {
                throw new IllegalArgumentException("Item data component '" + id + "' is already registered with value type " + existing.valueType.getTypeName());
            }

            return (ItemDataComponentType<T>) existing;
        }

        ItemDataComponentType<T> type = new ItemDataComponentType<>(id, valueType, typeParameters, immutableCopy, emptyValue);
        TYPES.put(id, type);
        return type;
    }
}
