package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.server.registry.CloudBlockEntityRegistry;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Every registered block-entity type survives a save followed by a load without
 * throwing. This catches a DTO class registered to the wrong runtime class through
 * the cast in {@code applyData}.
 */
class AllBlockEntitiesRoundTripTest {

    @Test
    void allRegisteredBlockEntityTypesRoundTrip() {
        int index = 0;
        for (BlockEntityType<?> type : CloudBlockEntityRegistry.get().values()) {
            Vector3i position = Vector3i.from(700 + index * 2, 64, -500);
            index++;
            BlockEntity entity = PersistenceFixtures.blockEntity(type, position);
            NbtMap saved = assertDoesNotThrow(((BaseBlockEntity) entity)::getServerTag,
                    "save of " + type);
            assertDoesNotThrow(() -> ((BaseBlockEntity) entity).loadAdditionalData(saved),
                    "load of " + type);
            assertDoesNotThrow(((BaseBlockEntity) entity)::getServerTag,
                    "re-save of " + type);
            assertDoesNotThrow(((BaseBlockEntity) entity)::getClientTag,
                    "client tag of " + type);
        }
    }
}
