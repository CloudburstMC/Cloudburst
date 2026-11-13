package org.cloudburstmc.server.testutil;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.server.entity.EntityCreature;
import org.cloudburstmc.server.registry.CloudEntityRegistry;

public final class TestEntities {

    public static final EntityType<TestCreatureEntity> LEGACY_CREATURE_TYPE =
            EntityType.from("test:legacy_creature", TestCreatureEntity.class);

    public static final EntityType<CountingCreatureEntity> COUNTING_CREATURE_TYPE =
            EntityType.from("test:counting_creature", CountingCreatureEntity.class);

    private TestEntities() {
    }

    public static void registerAll(CloudEntityRegistry registry) {
        registry.register(LEGACY_CREATURE_TYPE, TestCreatureEntity::new, 1000, false);
        registry.register(COUNTING_CREATURE_TYPE, CountingCreatureEntity::new, 1001, false);
    }

    public static class TestCreatureEntity extends EntityCreature {

        public TestCreatureEntity(EntityType<?> type, Location location) {
            super(type, location);
        }
    }

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
