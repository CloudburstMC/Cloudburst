package org.cloudburstmc.server.entity;

import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.KnockbackCause;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.server.entity.misc.EntityAreaEffectCloud;
import org.cloudburstmc.server.testutil.InterfaceProxy;
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

        assertTrue(entity.justCreated);
        assertEquals(400, entity.getAirTicks());
        assertNull(entity.getOwner());
        assertFalse(entity.isNameTagAlwaysVisible());
        entity.setAirTicks(200);
        assertEquals(200, entity.getAirTicks());

        Vector3f knockback = Vector3f.from(0.5f, 0.4f, 0);
        entity.applyKnockback(knockback, KnockbackCause.DAMAGE, null);
        assertEquals(knockback, entity.getMotion());
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
