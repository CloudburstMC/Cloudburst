package org.cloudburstmc.server.entity.hostile;

import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.data.EnderDragonData;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.server.registry.CloudEntityRegistry;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trip tests for the ender dragon data path: every key today's load/save handle,
 * plus an unknown key, survive with the same NBT types.
 *
 * <p>Today's init and experience fallback need a live level, which the stub fixture has
 * none of, so the tests build the entity directly and always store the experience key,
 * exercising the production load and save paths otherwise.
 */
class EnderDragonPersistenceTest {

    private static EntityEnderDragon dragon() {
        PersistenceFixtures.ensureInitialized();
        return new EntityEnderDragon(EntityTypes.ENDER_DRAGON,
                Location.from(Vector3f.ZERO, InterfaceProxy.create(Level.class)));
    }

    private static NbtMap save(EntityEnderDragon dragon) {
        NbtMapBuilder builder = NbtMap.builder();
        dragon.saveAdditionalData(builder);
        return builder.build();
    }

    @Test
    void roundTripPreservesKeysAndTypes() {
        assertEquals(EnderDragonData.class, CloudEntityRegistry.get().getDataClass(EntityTypes.ENDER_DRAGON));

        EntityEnderDragon dragon = dragon();
        dragon.loadAdditionalData(NbtMap.builder()
                .putInt("DragonDeathTime", 5)
                .putInt("DragonDeathExperience", 500)
                .putInt("Mystery", 7)
                .build());

        assertTrue(dragon.isInDeathSequence());

        NbtMap saved = save(dragon);

        assertTrue(saved.get("DragonDeathTime") instanceof Integer, "DragonDeathTime must be an int");
        assertEquals(5, saved.getInt("DragonDeathTime"));
        assertTrue(saved.get("DragonDeathExperience") instanceof Integer, "DragonDeathExperience must be an int");
        assertEquals(500, saved.getInt("DragonDeathExperience"));
        assertEquals(7, saved.getInt("Mystery"));
    }

    @Test
    void defaultInstanceSavesTodayDefaults() {
        EntityEnderDragon dragon = dragon();

        NbtMap saved = save(dragon);

        // Today's save always writes both counters as zero for a fresh dragon.
        assertTrue(saved.get("DragonDeathTime") instanceof Integer, "DragonDeathTime must be an int");
        assertEquals(0, saved.getInt("DragonDeathTime"));
        assertTrue(saved.get("DragonDeathExperience") instanceof Integer, "DragonDeathExperience must be an int");
        assertEquals(0, saved.getInt("DragonDeathExperience"));
    }

    @Test
    void absentDeathTimeLeavesStateUnchanged() {
        EntityEnderDragon dragon = dragon();
        dragon.loadAdditionalData(NbtMap.builder()
                .putInt("DragonDeathTime", 10)
                .putInt("DragonDeathExperience", 500)
                .build());

        dragon.loadAdditionalData(NbtMap.builder()
                .putInt("DragonDeathExperience", 400)
                .build());

        NbtMap saved = save(dragon);

        assertEquals(10, saved.getInt("DragonDeathTime"));
        assertEquals(400, saved.getInt("DragonDeathExperience"));
        assertTrue(dragon.isInDeathSequence());
    }
}
