package org.cloudburstmc.server.entity.hostile;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.data.CreeperData;
import org.cloudburstmc.api.entity.hostile.Creeper;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.registry.CloudEntityRegistry;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.POWERED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trip tests for the creeper data path: every key today's load/save handle,
 * plus an unknown key, survive with the same NBT types.
 *
 * <p>Today's load fires a power event through the server, which the stub fixture has none
 * of, so load-side tests run through a minimal subclass that records the powered flag
 * without firing. The save path is the production one on both classes.
 */
class CreeperPersistenceTest {

    private static final class LoadableCreeper extends EntityCreeper {
        LoadableCreeper(EntityType<Creeper> type, Location location) {
            super(type, location);
        }

        @Override
        public void setPowered(boolean powered) {
            this.data.setFlag(POWERED, powered);
        }
    }

    private static LoadableCreeper loadable() {
        PersistenceFixtures.ensureInitialized();
        return new LoadableCreeper(EntityTypes.CREEPER,
                Location.from(Vector3f.ZERO, InterfaceProxy.create(Level.class)));
    }

    private static NbtMap save(CloudEntity entity) {
        NbtMapBuilder builder = NbtMap.builder();
        entity.saveAdditionalData(builder);
        return builder.build();
    }

    @Test
    void roundTripPreservesKeysAndTypes() {
        assertEquals(CreeperData.class, CloudEntityRegistry.get().getDataClass(EntityTypes.CREEPER));

        LoadableCreeper creeper = loadable();
        creeper.loadAdditionalData(NbtMap.builder()
                .putBoolean("powered", true)
                .putInt("Mystery", 7)
                .build());

        assertTrue(creeper.isPowered());

        NbtMap saved = save(creeper);

        assertEquals(NbtType.BYTE, NbtType.byClass(saved.get("powered").getClass()), "powered must be a byte");
        assertEquals(true, saved.getBoolean("powered"));
        assertEquals(7, saved.getInt("Mystery"));
    }

    @Test
    void defaultInstanceSavesTodayDefaults() {
        EntityCreeper creeper = (EntityCreeper) PersistenceFixtures.entity(EntityTypes.CREEPER);

        NbtMap saved = save(creeper);

        // Today's save always writes powered false for a fresh creeper.
        assertEquals(NbtType.BYTE, NbtType.byClass(saved.get("powered").getClass()), "powered must be a byte");
        assertFalse(saved.getBoolean("powered"));
    }

    @Test
    void absentKeyLeavesStateUnchanged() {
        LoadableCreeper creeper = loadable();
        creeper.setPowered(true);

        creeper.loadAdditionalData(NbtMap.EMPTY);

        assertTrue(creeper.isPowered());
    }
}
