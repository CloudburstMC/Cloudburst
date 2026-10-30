package org.cloudburstmc.server.testutil;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.server.entity.EntityCreature;
import org.cloudburstmc.server.registry.CloudEntityRegistry;

/**
 * Test-only entity types used by persistence round-trip tests. The ids are
 * intentionally in the {@code test} namespace; they stay registered in the shared
 * singleton for the JVM's lifetime, so all registration happens in exactly one place
 * ({@link PersistenceFixtures}).
 */
public final class TestEntities {

    public static final EntityType<TestCreatureEntity> LEGACY_CREATURE_TYPE =
            EntityType.from("test:legacy_creature", TestCreatureEntity.class);

    public static final EntityType<CountingCreatureEntity> COUNTING_CREATURE_TYPE =
            EntityType.from("test:counting_creature", CountingCreatureEntity.class);

    private TestEntities() {
    }

    public static void registerAll(CloudEntityRegistry registry) {
        // The old four-argument overload defaults to the base data class; the
        // runtime class's minimum resolves the effective class to CreatureData.
        registry.register(LEGACY_CREATURE_TYPE, TestCreatureEntity::new, 1000, false);
        registry.register(COUNTING_CREATURE_TYPE, CountingCreatureEntity::new, 1001, false);
    }

    public static class TestCreatureEntity extends EntityCreature {

        public TestCreatureEntity(EntityType<?> type, Location location) {
            super(type, location);
        }
    }

    /**
     * Counts runtime {@code setAbsorption} calls so tests can observe how many
     * times a load applies its active effects. Absorption is set only from the
     * effect's {@code onApplied} path on a fresh fixture, so one load of one
     * absorption effect must count exactly one call.
     */
    public static class CountingCreatureEntity extends EntityCreature {

        public int absorptionApplications;

        public CountingCreatureEntity(EntityType<?> type, Location location) {
            super(type, location);
        }

        @Override
        public void setAbsorption(float absorption) {
            absorptionApplications++;
            super.setAbsorption(absorption);
        }
    }
}
