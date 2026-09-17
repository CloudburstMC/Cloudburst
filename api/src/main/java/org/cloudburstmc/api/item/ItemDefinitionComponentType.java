package org.cloudburstmc.api.item;

import lombok.Value;
import org.cloudburstmc.api.util.Identifier;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Identifies a component accepted in an item definition.
 * Factory methods register canonical custom types outside the reserved {@code minecraft} namespace.
 */
@Value
public class ItemDefinitionComponentType {
    private static final Map<Identifier, ItemDefinitionComponentType> TYPES = new LinkedHashMap<>();

    Identifier id;

    private ItemDefinitionComponentType(Identifier id) {
        this.id = checkNotNull(id, "id");
    }

    /**
     * Creates an item-definition component type.
     *
     * @param id component identifier
     * @return component type
     */
    public static synchronized ItemDefinitionComponentType of(Identifier id) {
        checkNotNull(id, "id");
        if ("minecraft".equals(id.getNamespace())) {
            throw new IllegalArgumentException("The minecraft namespace is reserved for built-in item-definition components");
        }
        return register(id);
    }

    static synchronized ItemDefinitionComponentType builtIn(Identifier id) {
        checkNotNull(id, "id");
        return register(id);
    }

    /**
     * Returns a registered item-definition component type.
     *
     * @param id component identifier
     * @return matching component type, if registered
     */
    public static synchronized Optional<ItemDefinitionComponentType> get(Identifier id) {
        return Optional.ofNullable(TYPES.get(checkNotNull(id, "id")));
    }

    /**
     * Returns all registered item-definition component types.
     *
     * @return component types in registration order
     */
    public static synchronized List<ItemDefinitionComponentType> values() {
        return List.copyOf(TYPES.values());
    }

    @Override
    public String toString() {
        return id.toString();
    }

    private static ItemDefinitionComponentType register(Identifier id) {
        return TYPES.computeIfAbsent(id, ItemDefinitionComponentType::new);
    }
}
