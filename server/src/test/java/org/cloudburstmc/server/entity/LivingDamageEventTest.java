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
import org.junit.jupiter.api.Test;

import java.util.function.DoubleUnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LivingDamageEventTest {

    @Test
    void changingRawDamageRecalculatesShieldDamage() {
        DamageTarget target = new DamageTarget();
        target.blockingFraction = 1;
        EntityDamageEvent event = target.createDamageEvent(DamageSource.of(DamageTypes.GENERIC), 8);

        event.setDamage(4);
        assertEquals(8, event.getOriginalDamage());
        assertEquals(4, event.getDamage());
        assertEquals(4, event.getBlockedDamage());
        assertEquals(0, event.getFinalDamage());

        event.setDamage(0);
        assertEquals(0, event.getBlockedDamage());
        assertEquals(0, event.getFinalDamage());
    }

    @Test
    void reductionsApplyOnlyToDamageRemainingAfterBlocking() {
        DamageTarget target = new DamageTarget();
        target.blockingFraction = 0.5f;
        target.reductionFactor = 0.5f;
        EntityDamageEvent event = target.createDamageEvent(DamageSource.of(DamageTypes.GENERIC), 8);

        assertEquals(4, event.getBlockedDamage());
        assertEquals(2, event.getFinalDamage());
        event.setDamage(20);
        assertEquals(10, event.getBlockedDamage());
        assertEquals(5, event.getFinalDamage());
    }

    @Test
    void defenseChangesDoNotChangeAnExistingHitsCalculation() {
        DamageTarget target = new DamageTarget();
        target.blockingFraction = 0.5f;
        target.reductionFactor = 0.5f;
        EntityDamageEvent event = target.createDamageEvent(DamageSource.of(DamageTypes.GENERIC), 8);

        target.blockingFraction = 0;
        target.reductionFactor = 1;
        assertEquals(4, event.getBlockedDamage());
        assertEquals(2, event.getFinalDamage());
        assertThrows(IllegalArgumentException.class, () -> event.setDamage(Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> event.setDamage(-1));
    }

    @Test
    void absorptionAppliesAfterBlockingAndReductions() {
        DamageTarget target = new DamageTarget();
        target.blockingFraction = 0.5f;
        target.reductionFactor = 0.5f;
        target.absorption = 3;
        EntityDamageEvent event = target.createDamageEvent(DamageSource.of(DamageTypes.GENERIC), 8);

        assertEquals(4, event.getBlockedDamage());
        assertEquals(2, event.getAbsorbedDamage());
        assertEquals(0, event.getFinalDamage());

        event.setDamage(20);
        assertEquals(10, event.getBlockedDamage());
        assertEquals(3, event.getAbsorbedDamage());
        assertEquals(2, event.getFinalDamage());
        target.absorption = 0;
        assertEquals(3, event.getAbsorbedDamage());
        assertEquals(2, event.getFinalDamage());
    }

    @Test
    void cooldownIsSubtractedAfterBlockingAndBeforeOtherReductions() {
        CloudEntityDamageEvent event = new CloudEntityDamageEvent(
                new DamageTarget(), DamageSource.of(DamageTypes.GENERIC), 10,
                damage -> damage * 0.5, 2, damage -> damage * 0.5, 1
        );

        assertEquals(5, event.getBlockedDamage());
        assertEquals(5, event.getUnblockedDamage());
        assertEquals(3, event.getDamageBeforeReductions());
        assertEquals(1, event.getAbsorbedDamage());
        assertEquals(0.5f, event.getFinalDamage());

        event.setDamage(20);
        assertEquals(10, event.getBlockedDamage());
        assertEquals(8, event.getDamageBeforeReductions());
        assertEquals(1, event.getAbsorbedDamage());
        assertEquals(3, event.getFinalDamage());

        event.setDamage(2);
        assertEquals(0, event.getDamageBeforeReductions());
        assertEquals(0, event.getAbsorbedDamage());
        assertEquals(0, event.getFinalDamage());
    }

    @Test
    public void blockedMeleeHitPushesTheAttackerAway() {
        DamageTarget defender = new DamageTarget();
        DamageTarget attacker = new DamageTarget();
        attacker.position = Vector3f.from(1, 0, 0);
        attacker.onGround = true;
        defender.blockingFraction = 1;

        defender.onDamageBlocked(defender.createDamageEvent(DamageSource.of(DamageTypes.PLAYER_ATTACK, attacker), 8));

        assertEquals(0.5f, attacker.getMotion().getX());
        assertEquals(0.4f, attacker.getMotion().getY());
        assertEquals(0, attacker.getMotion().getZ());
    }

    @Test
    public void blockedProjectileDoesNotPushItsShooter() {
        DamageTarget defender = new DamageTarget();
        DamageTarget attacker = new DamageTarget();
        attacker.position = Vector3f.from(1, 0, 0);
        defender.blockingFraction = 1;

        defender.onDamageBlocked(defender.createDamageEvent(DamageSource.of(DamageTypes.ARROW, attacker), 8));

        assertEquals(Vector3f.ZERO, attacker.getMotion());
    }

    @Test
    public void shieldRecoilRespectsAttackerKnockbackResistance() {
        DamageTarget defender = new DamageTarget();
        DamageTarget attacker = new DamageTarget();
        attacker.position = Vector3f.from(1, 0, 0);
        attacker.knockbackResistance = 1;
        defender.blockingFraction = 1;

        defender.onDamageBlocked(defender.createDamageEvent(DamageSource.of(DamageTypes.PLAYER_ATTACK, attacker), 8));

        assertEquals(Vector3f.ZERO, attacker.getMotion());
    }

    private static class DamageTarget extends EntityLiving {

        private float blockingFraction;
        private float reductionFactor = 1;
        private float absorption;
        private float knockbackResistance;

        private DamageTarget() {
            super(EntityTypes.ZOMBIE, Location.from(Vector3f.ZERO, InterfaceProxy.create(Level.class)));
        }

        @Override
        public float getAbsorption() {
            return this.absorption;
        }

        @Override
        protected float getKnockbackResistance() {
            return this.knockbackResistance;
        }

        @Override
        public boolean setMotion(Vector3f motion) {
            this.motion = motion;
            return true;
        }

        @Override
        protected DoubleUnaryOperator createBlockingReduction(DamageSource source) {
            float fraction = this.blockingFraction;
            return damage -> damage * fraction;
        }

        @Override
        protected DoubleUnaryOperator createDamageReduction(DamageSource source) {
            float factor = this.reductionFactor;
            return damage -> damage * factor;
        }
    }
}
