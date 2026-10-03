package org.cloudburstmc.api.data;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.cloudburstmc.api.util.Identifier;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A typed slot identifier in a {@link org.cloudburstmc.api.util.component.ComponentMap}.
 * <p>
 * A slot can store configuration data or a behavior handler.
 * Component types with the same identifier and value type are equal.
 *
 * @param <H> the component value type
 */
@Getter
@EqualsAndHashCode
public class ComponentType<H> {

    private final Identifier id;
    private final Class<H> type;

    private ComponentType(Identifier id, Class<H> type) {
        this.id = id;
        this.type = type;
    }

    public static <H> ComponentType<H> of(Identifier id, Class<H> type) {
        checkNotNull(id, "id");
        checkNotNull(type, "type");
        return new ComponentType<>(id, type);
    }

    public static <H> ComponentType<H> of(String id, Class<H> type) {
        return of(Identifier.parse(id), type);
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
