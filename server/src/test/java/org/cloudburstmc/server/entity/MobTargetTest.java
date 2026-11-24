package org.cloudburstmc.server.entity;

import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.LivingEntity;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MobTargetTest {

    private static final Location LOCATION = Location.from(Vector3f.ZERO, InterfaceProxy.create(Level.class));

    @Test
    void acceptsLivingTargetsAndCanClearThem() {
        TestMob mob = new TestMob();
        CloudLivingEntity target = new CloudLivingEntity(EntityTypes.PIG, LOCATION) {};

        assertNull(mob.getTarget());
        mob.setTarget(target);
        assertSame(target, mob.getTarget());
        mob.setTarget(null);
        assertNull(mob.getTarget());
    }

    @Test
    void invalidAssignmentKeepsThePreviousTarget() {
        TestMob mob = new TestMob();
        TestMob target = new TestMob();
        mob.setTarget(target);

        assertThrows(IllegalArgumentException.class, () -> mob.setTarget(mob));
        assertThrows(IllegalArgumentException.class, () -> mob.setTarget(InterfaceProxy.create(LivingEntity.class)));
        assertSame(target, mob.getTarget());
    }

    @Test
    void deadTargetsAreForgottenRatherThanRestoredOnRevival() {
        TestMob mob = new TestMob();
        TestMob target = new TestMob();
        mob.setTarget(target);

        target.alive = false;
        assertNull(mob.getTarget());
        assertThrows(IllegalArgumentException.class, () -> mob.setTarget(target));

        target.alive = true;
        assertNull(mob.getTarget());
    }

    private static final class TestMob extends CloudMob {

        private boolean alive = true;

        private TestMob() {
            super(EntityTypes.ZOMBIE, LOCATION);
        }

        @Override
        public boolean isAlive() {
            return this.alive;
        }
    }
}
