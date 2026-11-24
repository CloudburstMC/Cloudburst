package org.cloudburstmc.server.entity;

import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.KnockbackCause;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.server.entity.misc.EntityAreaEffectCloud;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EntityConstructionTest {

    @Test
    void baseStateIsAvailableBeforeDeferredInitialization() {
        CloudEntity entity = new CloudEntity(EntityTypes.PIG,
                Location.from(Vector3f.ZERO, InterfaceProxy.create(Level.class))) {
            @Override
            protected void initEntity() {
                fail("Construction must not invoke subclass initialization");
            }
        };

        assertEquals(400, entity.getAirTicks());
        assertNull(entity.getOwner());
        assertFalse(entity.isCustomNameAlwaysVisible());
        entity.setAirTicks(200);
        assertEquals(200, entity.getAirTicks());

        Vector3f knockback = Vector3f.from(0.5f, 0.4f, 0);
        entity.applyKnockback(knockback, KnockbackCause.DAMAGE, null);
        assertEquals(knockback, entity.getMotion());
    }

    @Test
    void freshLivingEntityStartsAtItsSpeciesMaximum() {
        CloudEntity wither = (CloudEntity) PersistenceFixtures.entity(EntityTypes.WITHER);

        wither.initializeHealth();

        assertEquals(300, wither.getMaxHealth());
        assertEquals(300f, wither.getHealth());
    }

    @Test
    void savedHealthOverridesTheInitialHealth() {
        CloudEntity wither = (CloudEntity) PersistenceFixtures.entity(EntityTypes.WITHER);
        wither.initializeHealth();

        wither.loadAdditionalData(NbtMap.builder().putFloat("Health", 115f).build());

        assertEquals(115f, wither.getHealth());
        wither.setMaxHealth(400);
        assertEquals(115f, wither.getHealth());
    }

    @Test
    void areaCloudGeometryIsAvailableBeforeWorldInitialization() {
        EntityAreaEffectCloud cloud = new EntityAreaEffectCloud(EntityTypes.AREA_EFFECT_CLOUD,
                Location.from(Vector3f.ZERO, InterfaceProxy.create(Level.class)));

        assertEquals(3.0f, cloud.getRadius());
        assertEquals(6.0f, cloud.getWidth());
        assertEquals(6.0f, cloud.getLength());
        assertEquals(10, cloud.getWaitTime());
        assertNotNull(cloud.getParticle());

        cloud.setRadius(2.0f);
        cloud.recalculateBoundingBox();
        assertEquals(4.0f, cloud.getBoundingBox().getMaxX() - cloud.getBoundingBox().getMinX());
    }
}
