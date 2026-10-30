package org.cloudburstmc.server.entity.projectile;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.Projectile;
import org.cloudburstmc.api.entity.data.ProjectileData;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.nbt.ServerNbt;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Data-path tests for the projectile branch. {@link EntityProjectile} is abstract and has
 * no registered type mapping to {@link ProjectileData}, so the tests drive
 * {@code applyData}/{@code fillData} directly through a minimal subclass and assert the
 * mapped NBT types, exactly as the production save path would write them.
 */
class ProjectilePersistenceTest {

    private static final class TestProjectile extends EntityProjectile {
        TestProjectile(EntityType<?> type, Location location) {
            super(type, location);
        }
    }

    private static TestProjectile projectile() {
        PersistenceFixtures.ensureInitialized();
        return new TestProjectile(EntityTypes.SNOWBALL,
                Location.from(Vector3f.ZERO, InterfaceProxy.create(Level.class)));
    }

    @Test
    void dataRoundTripPreservesKeysAndTypes() {
        TestProjectile projectile = projectile();

        ProjectileData loaded = new ProjectileData();
        loaded.setDamage(2.5f);
        projectile.applyData(loaded);

        assertEquals(2.5f, projectile.getDamage());

        ProjectileData filled = new ProjectileData();
        projectile.fillData(filled);
        NbtMap written = ServerNbt.MAPPER.write(filled);

        assertTrue(written.get("damage") instanceof Float, "damage must be a float");
        assertEquals(2.5f, written.getFloat("damage"));
    }

    @Test
    void defaultInstanceFillsTodayDefaults() {
        TestProjectile projectile = projectile();

        ProjectileData filled = new ProjectileData();
        projectile.fillData(filled);
        NbtMap written = ServerNbt.MAPPER.write(filled);

        // Today's save always writes the unset damage as zero.
        assertTrue(written.get("damage") instanceof Float, "damage must be a float");
        assertEquals(0f, written.getFloat("damage"));
    }

    @Test
    void absentKeyLeavesStateUnchanged() {
        TestProjectile projectile = projectile();
        projectile.setDamage(3f);

        // The DTO field stays null, as the mapper leaves it when the key is absent.
        projectile.applyData(new ProjectileData());

        assertEquals(3f, projectile.getDamage());
    }
}
