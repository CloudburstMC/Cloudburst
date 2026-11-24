package org.cloudburstmc.server.entity.projectile;

import org.cloudburstmc.api.entity.Damageable;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ProjectileDamageTest {

    @Test
    void roundsOrdinarySpeedScaledDamageUp() {
        DamageArrow arrow = new DamageArrow();
        arrow.setDamage(5);
        arrow.setVelocity(0.25f);

        assertEquals(2, arrow.getResultDamage());
    }

    @Test
    void zeroDamageAndStationaryProjectilesDealNoDamage() {
        DamageArrow arrow = new DamageArrow();
        arrow.setDamage(0);
        arrow.setVelocity(2);
        assertEquals(0, arrow.getResultDamage());

        arrow.setDamage(Float.MAX_VALUE);
        arrow.setVelocity(0);
        assertEquals(0, arrow.getResultDamage());
    }

    @Test
    void saturatesDamageAboveTheIntegerRange() {
        DamageArrow arrow = new DamageArrow();
        arrow.setVelocity(1);
        arrow.setDamage(0x1.0p31f);
        assertEquals(Integer.MAX_VALUE, arrow.getResultDamage());

        arrow.setVelocity(2);
        arrow.setDamage(Float.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, arrow.getResultDamage());
    }

    @Test
    void criticalBonusCannotOverflowSaturatedDamage() {
        DamageArrow arrow = new DamageArrow();
        arrow.setDamage(Float.MAX_VALUE);
        arrow.setVelocity(1);
        arrow.setCritical(true);

        for (int i = 0; i < 32; i++) {
            assertEquals(Integer.MAX_VALUE, arrow.getResultDamage());
        }
    }

    @Test
    void ordinaryCriticalDamageKeepsItsExistingBonusRange() {
        DamageArrow arrow = new DamageArrow();
        arrow.setDamage(3);
        arrow.setVelocity(1);
        arrow.setCritical(true);

        for (int i = 0; i < 32; i++) {
            int damage = arrow.getResultDamage();
            assertTrue(damage >= 3 && damage <= 5);
        }
    }

    @Test
    void largeFireballDealsFixedDirectDamageEvenWithoutVelocity() {
        EntityLargeFireball fireball = new EntityLargeFireball(EntityTypes.LARGE_FIREBALL,
                Location.from(Vector3f.ZERO, InterfaceProxy.create(Level.class)));
        AtomicReference<DamageSource> receivedSource = new AtomicReference<>();
        Entity target = InterfaceProxy.create(Damageable.class, (_, method, arguments) -> {
            assertEquals("damage", method.getName());
            assertEquals(6f, arguments[0]);
            receivedSource.set((DamageSource) arguments[1]);
            return false;
        });

        fireball.onCollideWithEntity(target);

        DamageSource source = receivedSource.get();
        assertSame(DamageTypes.UNATTRIBUTED_FIREBALL, source.getDamageType());
        assertSame(fireball, source.getDirectEntity());
    }

    private static final class DamageArrow extends EntityArrow {

        private DamageArrow() {
            super(EntityTypes.ARROW, Location.from(Vector3f.ZERO, InterfaceProxy.create(Level.class)));
        }

        private void setVelocity(float speed) {
            this.motion = Vector3f.from(0, 0, speed);
        }
    }
}
