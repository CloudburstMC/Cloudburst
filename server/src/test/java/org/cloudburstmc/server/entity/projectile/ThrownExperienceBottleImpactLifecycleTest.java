package org.cloudburstmc.server.entity.projectile;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.util.BlockHitResult;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ThrownExperienceBottleImpactLifecycleTest {

    @Test
    void closedBottleIgnoresLateEntityAndBlockImpacts() {
        EntityThrownExperienceBottle bottle = new EntityThrownExperienceBottle(
                EntityTypes.THROWN_EXPERIENCE_BOTTLE,
                Location.from(Vector3f.ZERO, InterfaceProxy.create(Level.class))) {
            @Override
            public CloudLevel getLevel() {
                return fail("Closed bottle must not access the world");
            }
        };
        bottle.close();

        for (int i = 0; i < 2; i++) {
            bottle.onCollideWithEntity(InterfaceProxy.create(Entity.class));
            bottle.onBlockCollision(hit(Direction.UP));
            bottle.onBlockCollision(hit(null));
        }

        assertTrue(bottle.isClosed());
    }

    @Test
    void closesBeforeEffectsAndRejectsReentrantAndLateImpacts() {
        int[] effectBoundaryVisits = {0};
        IllegalStateException boundary = new IllegalStateException("Effect boundary reached");
        EntityThrownExperienceBottle bottle = new EntityThrownExperienceBottle(
                EntityTypes.THROWN_EXPERIENCE_BOTTLE,
                Location.from(Vector3f.ZERO, InterfaceProxy.create(Level.class))) {
            @Override
            public CloudLevel getLevel() {
                effectBoundaryVisits[0]++;
                assertTrue(this.isClosed());
                this.onCollideWithEntity(InterfaceProxy.create(Entity.class));
                this.onBlockCollision(hit(Direction.UP));
                throw boundary;
            }
        };

        assertSame(boundary, assertThrows(IllegalStateException.class,
                () -> bottle.onBlockCollision(hit(Direction.UP))));
        bottle.onCollideWithEntity(InterfaceProxy.create(Entity.class));
        bottle.onBlockCollision(hit(null));

        assertEquals(1, effectBoundaryVisits[0]);
        assertTrue(bottle.isClosed());
    }

    private static BlockHitResult hit(Direction face) {
        return new BlockHitResult(Vector3f.ZERO, InterfaceProxy.create(Block.class), face, false);
    }
}
