package org.cloudburstmc.server.testutil;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.server.entity.CloudMob;
import org.cloudburstmc.server.registry.CloudEntityRegistry;

public final class TestEntities {

    public static final EntityType<TestCreatureEntity> LEGACY_CREATURE_TYPE =
            EntityType.from("test:legacy_creature", TestCreatureEntity.class);

    public static final EntityType<CountingMob> COUNTING_MOB_TYPE =
            EntityType.from("test:counting_mob", CountingMob.class);

    private TestEntities() {
    }

    public static void registerAll(CloudEntityRegistry registry) {
        registry.register(LEGACY_CREATURE_TYPE, TestCreatureEntity::new, 1000, false);
        registry.register(COUNTING_MOB_TYPE, CountingMob::new, 1001, false);
    }

    public static class TestCreatureEntity extends CloudMob {

        public TestCreatureEntity(EntityType<?> type, Location location) {
            super(type, location);
        }
    }

    public static class CountingMob extends CloudMob {

        public int absorptionApplications;

        public CountingMob(EntityType<?> type, Location location) {
            super(type, location);
        }

        @Override
        public void setAbsorption(float absorption) {
            absorptionApplications++;
            super.setAbsorption(absorption);
        }
    }
}
