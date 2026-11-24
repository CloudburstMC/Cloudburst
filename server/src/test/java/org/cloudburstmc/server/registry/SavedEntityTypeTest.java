package org.cloudburstmc.server.registry;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.entity.UnknownEntity;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SavedEntityTypeTest {

    @Test
    void savedRegisteredIdUsesTheRegisteredType() {
        PersistenceFixtures.ensureInitialized();
        CloudEntityRegistry registry = CloudEntityRegistry.get();

        assertSame(EntityTypes.ZOMBIE, registry.resolveSavedType(EntityTypes.ZOMBIE.getId()));
        assertSame(EntityTypes.CHEST_MINECART, registry.resolveSavedType(EntityTypes.CHEST_MINECART.getId()));
    }

    @Test
    void savedPlaceholderDoesNotBecomeARegisteredType() {
        PersistenceFixtures.ensureInitialized();
        CloudEntityRegistry registry = CloudEntityRegistry.get();
        Identifier id = Identifier.parse("test:unregistered_saved_entity");

        EntityType<?> placeholder = registry.resolveSavedType(id);

        assertSame(UnknownEntity.class, placeholder.getEntityClass());
        assertSame(placeholder, registry.resolveSavedType(id));
        assertTrue(registry.get(id).isEmpty());
        assertFalse(registry.values().contains(placeholder));
    }
}
