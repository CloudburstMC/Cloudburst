package org.cloudburstmc.server.entity.projectile;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.Projectile;
import org.cloudburstmc.api.entity.data.ProjectileData;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.blockentity.DtoStateChecks;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Data-path tests for the projectile branch. {@link EntityProjectile} is abstract and has
 * no registered type mapping to {@link ProjectileData}, so the tests drive the production
 * load and save paths through a minimal subclass and assert the mapped NBT types.
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

    private static NbtMap save(CloudEntity entity) {
        NbtMapBuilder builder = NbtMap.builder();
        entity.saveAdditionalData(builder);
        return builder.build();
    }

    @Test
    void dataRoundTripPreservesKeysAndTypes() {
        TestProjectile projectile = projectile();

        projectile.loadAdditionalData(NbtMap.builder()
                .putFloat("damage", 2.5f)
                .build());

        assertEquals(2.5f, projectile.getDamage());

        NbtMap written = save(projectile);

        assertTrue(written.get("damage") instanceof Float, "damage must be a float");
        assertEquals(2.5f, written.getFloat("damage"));
    }

    @Test
    void defaultInstanceFillsTodayDefaults() {
        TestProjectile projectile = projectile();

        NbtMap written = save(projectile);

        // Today's save always writes the unset damage as zero.
        assertTrue(written.get("damage") instanceof Float, "damage must be a float");
        assertEquals(0f, written.getFloat("damage"));
    }

    @Test
    void absentKeyLeavesStateUnchanged() {
        TestProjectile projectile = projectile();
        projectile.setDamage(3f);

        // A tag without keys keeps the current values, as today's load did.
        projectile.loadAdditionalData(NbtMap.EMPTY);

        assertEquals(3f, projectile.getDamage());
    }

    @Test
    void projectileKeepsNoDuplicateState() {
        DtoStateChecks.assertEntityKeepsNoDuplicateState(
                EntityProjectile.class, ProjectileData.class);
        Set<String> fields = new HashSet<>();
        for (Field field : EntityProjectile.class.getDeclaredFields()) {
            fields.add(field.getName());
        }
        for (String removed : Set.of("damage")) {
            assertFalse(fields.contains(removed),
                    "EntityProjectile keeps " + removed + "; state must live in ProjectileData only");
        }
    }
}
