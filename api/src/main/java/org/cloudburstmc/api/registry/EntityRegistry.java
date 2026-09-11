package org.cloudburstmc.api.registry;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityFactory;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.component.ComponentBuilder;
import org.cloudburstmc.api.util.component.ComponentMap;

/**
 * Registry for entity types, factories, and entity components.
 */
public interface EntityRegistry extends ComponentRegistry<EntityType<?>>, KeyedRegistry<EntityType<?>> {

    /**
     * Registers an entity factory using the default registration settings.
     *
     * @param type    entity type
     * @param factory factory used to create entities of the type
     * @param <T>     entity implementation type
     * @throws RegistryException if registration is closed or the type cannot be registered
     */
    default <T extends Entity> void register(EntityType<T> type, EntityFactory<T> factory) throws RegistryException {
        this.register(type, factory, 1000, false);
    }

    /**
     * Registers an entity factory.
     *
     * @param type        entity type
     * @param factory     factory used to create entities of the type
     * @param priority    priority used when more than one factory is registered for the same type
     * @param hasSpawnEgg whether the type has a spawn egg entry
     * @param <T>         entity implementation type
     * @throws RegistryException if registration is closed or the type cannot be registered
     */
    <T extends Entity> void register(EntityType<T> type, EntityFactory<T> factory, int priority, boolean hasSpawnEgg) throws RegistryException;

    /**
     * Creates a new entity instance.
     *
     * @param type     entity type
     * @param location spawn location
     * @param <T>      entity implementation type
     * @return new entity instance
     * @throws RegistryException if the type has no registered factory
     */
    <T extends Entity> T create(EntityType<T> type, Location location) throws RegistryException;

    /**
     * Returns the resolved component map for an entity type.
     *
     * @param type entity type
     * @return resolved component map, or {@code null} when the type is unknown
     */
    @Override
    ComponentMap getComponents(EntityType<?> type);

    /**
     * Returns a builder for configuring a registered entity type during initialization.
     *
     * @param type registered entity type
     * @return component builder for the type
     * @throws RegistryException if the type is unknown or registration has closed
     */
    ComponentBuilder configure(EntityType<?> type) throws RegistryException;

    @Override
    default Identifier getId(EntityType<?> value) {
        return value.getId();
    }
}
