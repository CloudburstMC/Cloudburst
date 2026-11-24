package org.cloudburstmc.server.entity;

import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.server.event.entity.CloudEntityDamageEvent;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import java.util.function.DoubleUnaryOperator;

import static org.junit.jupiter.api.Assertions.*;

class LivingEntityOwnershipTest {

    private static final DamageSource ATTACK = DamageSource.of(DamageTypes.MOB_ATTACK);

    @Test
    void undeadClassificationIncludesNewAndLegacyVariants() {
        assertTrue(PersistenceFixtures.entity(EntityTypes.BOGGED).isUndead());
        assertTrue(PersistenceFixtures.entity(EntityTypes.PARCHED).isUndead());
        assertTrue(PersistenceFixtures.entity(EntityTypes.ZOGLIN).isUndead());
        assertTrue(PersistenceFixtures.entity(EntityTypes.ZOMBIE_NAUTILUS).isUndead());
        assertTrue(PersistenceFixtures.entity(EntityTypes.CAMEL_HUSK).isUndead());
        assertTrue(PersistenceFixtures.entity(EntityTypes.DEPRECATED_ZOMBIE_VILLAGER).isUndead());
        assertFalse(PersistenceFixtures.entity(EntityTypes.PIG).isUndead());
    }

    @Test
    void deadBaseTickClosesWithoutSamplingLivingEnvironment() {
        TestLiving target = new TestLiving();
        target.health = 0;

        assertFalse(target.entityBaseTick());
        assertTrue(target.isClosed());
    }

    @Test
    void closedBaseTickDoesNotRunLivingWorkEvenWithHealthRemaining() {
        TestLiving target = new TestLiving();
        target.close();

        assertFalse(target.entityBaseTick(1));
        assertEquals(20, target.getHealth());
    }

    @Test
    void listenerLoweredHitCannotPassPreparationWithoutDamageOrBlocking() {
        TestLiving target = new TestLiving();
        target.onDamageAccepted(target.createDamageEvent(ATTACK, 5));
        CloudEntityDamageEvent hit = target.createDamageEvent(ATTACK, 8);

        assertTrue(target.prepareDamage(hit));
        hit.setDamage(5);
        assertFalse(target.prepareDamage(hit));
    }

    @Test
    void blockedOnlyHitRunsBlockingEffectsWithoutReplacingCooldown() {
        TestLiving target = new TestLiving();
        target.onDamageAccepted(target.createDamageEvent(ATTACK, 5));
        target.blocks = true;
        CloudEntityDamageEvent blocked = target.createDamageEvent(ATTACK, 10);

        assertTrue(target.prepareDamage(blocked));
        target.onDamageAccepted(blocked);
        assertEquals(1, target.blockedHits);
        assertEquals(0, blocked.getDamageBeforeReductions());

        target.blocks = false;
        assertEquals(1, target.createDamageEvent(ATTACK, 6).getDamageBeforeReductions());
    }

    @Test
    void olderDifferentialHitDoesNotLowerBaselineEstablishedByNestedHit() {
        TestLiving target = new TestLiving();
        target.onDamageAccepted(target.createDamageEvent(ATTACK, 5));
        CloudEntityDamageEvent outer = target.createDamageEvent(ATTACK, 7);
        target.onDamageAccepted(target.createDamageEvent(ATTACK, 9));

        assertEquals(2, outer.getDamageBeforeReductions());
        target.onDamageAccepted(outer);
        assertEquals(1, target.createDamageEvent(ATTACK, 10).getDamageBeforeReductions());
    }

    private static final class TestLiving extends CloudLivingEntity {
        private boolean blocks;
        private int blockedHits;

        private TestLiving() {
            super(EntityTypes.ZOMBIE, Location.from(Vector3f.ZERO, InterfaceProxy.create(Level.class)));
        }

        @Override
        public boolean isInsideOfWater() {
            throw new AssertionError("Inactive entities must not sample the living environment");
        }

        @Override
        protected DoubleUnaryOperator createBlockingReduction(DamageSource source) {
            return this.blocks ? damage -> damage : damage -> 0;
        }

        @Override
        protected void onDamageBlocked(EntityDamageEvent event) {
            this.blockedHits++;
        }
    }
}
