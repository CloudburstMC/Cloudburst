package org.cloudburstmc.server.entity.projectile;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArrowBlockingTest {

    @Test
    void deflectsAnArrowWhoseDamageWasBlockedInsteadOfDeletingIt() {
        ImpactArrow arrow = new ImpactArrow(false);
        arrow.setMotion(Vector3f.from(0, 0, 3));
        arrow.onCollideWithEntity(InterfaceProxy.create(Entity.class));
        assertFalse(arrow.isClosed());
        assertEquals(-0.6f, arrow.getMotion().getZ(), 1.0e-6f);
        assertEquals(0.6f, arrow.getMotion().length(), 1.0e-6f);
    }

    @Test
    void piercingArrowsSurviveUntilTheirHitLimit() {
        ImpactArrow arrow = new ImpactArrow(true);
        arrow.setPierceLevel(1);
        arrow.onCollideWithEntity(InterfaceProxy.create(Entity.class));
        assertFalse(arrow.isClosed());
        arrow.onCollideWithEntity(InterfaceProxy.create(Entity.class));
        assertTrue(arrow.isClosed());
    }

    private static class ImpactArrow extends EntityArrow {
        private final boolean acceptsDamage;

        private ImpactArrow(boolean acceptsDamage) {
            super(EntityTypes.ARROW, Location.from(Vector3f.ZERO, InterfaceProxy.create(Level.class)));
            this.acceptsDamage = acceptsDamage;
        }

        @Override
        protected boolean damageEntity(Entity entity) {
            return this.acceptsDamage;
        }

        @Override
        public boolean setMotion(Vector3f motion) {
            this.motion = motion;
            return true;
        }

        @Override
        public void updateMovement() {
        }

        @Override
        public void close() {
            this.closed = true;
        }
    }
}
