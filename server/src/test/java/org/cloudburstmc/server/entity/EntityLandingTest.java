package org.cloudburstmc.server.entity;

import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EntityLandingTest {

    @Test
    void resolvesShortAndFullHeightLandingSurfaces() {
        CloudEntity entity = entity(Vector3f.from(0.5f, 64.9375f, 0.5f));
        assertEquals(Vector3i.from(0, 64, 0), entity.getLandingBlockPosition());
        entity.position = Vector3f.from(0.5f, 65, 0.5f);
        assertEquals(Vector3i.from(0, 64, 0), entity.getLandingBlockPosition());
        entity.position = Vector3f.from(-0.5f, -1.0625f, -0.5f);
        assertEquals(Vector3i.from(-1, -2, -1), entity.getLandingBlockPosition());
    }

    @Test
    void prefersTheActualSupportWhenLandingOffCenter() {
        CloudEntity entity = entity(Vector3f.from(0.05f, 64.9375f, 0.5f));
        entity.supportingBlockPosition = Optional.of(Vector3i.from(-1, 64, 0));
        assertEquals(Vector3i.from(-1, 64, 0), entity.getLandingBlockPosition());
    }

    @Test
    void tracksAndClearsFallsBelowZero() {
        CloudEntity entity = entity(Vector3f.from(0, -20, 0));
        entity.position = Vector3f.from(0, -18.75f, 0);
        entity.updateFallState(false);
        entity.position = Vector3f.from(0, -19.5f, 0);
        entity.updateFallState(false);
        assertEquals(0.75f, entity.fallDistance);
        entity.resetFallDistance();
        assertEquals(0, entity.fallDistance);
        assertEquals(-19.5f, entity.highestPosition);
        entity.position = Vector3f.from(0, -20, 0);
        entity.updateFallState(true);
        assertEquals(0, entity.fallDistance);
        assertEquals(-20, entity.highestPosition);
    }

    private static CloudEntity entity(Vector3f position) {
        CloudEntity entity = new CloudEntity(EntityTypes.PIG,
                Location.from(position, InterfaceProxy.create(Level.class))) {
        };
        entity.position = position;
        entity.resetFallDistance();
        return entity;
    }
}
