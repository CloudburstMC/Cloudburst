package org.cloudburstmc.server.entity;

import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.KnockbackCause;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;
import org.cloudburstmc.api.event.entity.EntityKnockbackEvent;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.potion.EffectTypes;
import org.cloudburstmc.api.potion.PotionEffect;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.server.event.entity.CloudEntityDamageEvent;
import org.cloudburstmc.server.potion.ActivePotionEffect;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import java.util.function.DoubleUnaryOperator;

import static org.junit.jupiter.api.Assertions.*;

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
                damage -> damage * 0.5, damage -> damage, 1, 2, damage -> damage * 0.5, 1
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
    void helmetWearUsesAcceptedDamageBeforeHelmetProtection() {
        CloudEntityDamageEvent event = new CloudEntityDamageEvent(
                new DamageTarget(), DamageSource.of(DamageTypes.FALLING_ANVIL), 20,
                damage -> damage * 0.5, damage -> damage * 5, 0.75f, 7.5f, damage -> damage, 0
        );

        assertEquals(37.5f, event.getAdjustedDamage());
        assertEquals(30, event.getFinalDamage());
        assertEquals(40, event.getHelmetDamage());

        event.setDamage(12);
        assertEquals(15, event.getFinalDamage());
        assertEquals(20, event.getHelmetDamage());
    }

    @Test
    void knockbackNormalizesLargeFiniteDirectionsWithoutOverflow() {
        DamageTarget target = new DamageTarget();
        target.onGround = true;

        target.knockBack(0.4f, Float.MAX_VALUE, Float.MAX_VALUE, KnockbackCause.ENTITY_ATTACK, null);

        assertEquals(0.4, Math.hypot(target.getMotion().getX(), target.getMotion().getZ()), 1.0e-6);
        assertEquals(0.4f, target.getMotion().getY());
    }

    @Test
    void knockbackEventPreservesAttributionAndValidatesListenerChanges() {
        DamageTarget target = new DamageTarget();
        DamageTarget attacker = new DamageTarget();
        EntityKnockbackEvent event = new EntityKnockbackEvent(target,
                KnockbackCause.SHIELD_BLOCK, attacker, Vector3f.from(0.5f, 0.4f, 0));

        assertEquals(attacker, event.getSourceEntity());
        event.setKnockback(Vector3f.ZERO);
        assertEquals(Vector3f.ZERO, event.getKnockback());
        event.setCancelled(true);
        assertTrue(event.isCancelled());
        assertThrows(IllegalArgumentException.class, () -> event.setKnockback(Vector3f.from(Float.NaN, 0, 0)));
        assertThrows(IllegalArgumentException.class, () -> event.setKnockback(Vector3f.from(0, Float.POSITIVE_INFINITY, 0)));
    }

    @Test
    void sourceAdjustmentsKeepLargeFiniteDamageBounded() {
        CloudEntityDamageEvent event = new CloudEntityDamageEvent(
                new DamageTarget(), DamageSource.of(DamageTypes.FREEZE), Float.MAX_VALUE,
                damage -> 0, damage -> damage * 5, 1, 0, damage -> damage, 0
        );

        assertEquals(Float.MAX_VALUE, event.getAdjustedDamage());
        assertEquals(Float.MAX_VALUE, event.getFinalDamage());
    }

    @Test
    void healthBoostCannotOverflowMaximumHealth() {
        DamageTarget target = new DamageTarget();
        target.effects.put(EffectTypes.HEALTH_BOOST,
                ActivePotionEffect.from(new PotionEffect(EffectTypes.HEALTH_BOOST, 100, Integer.MAX_VALUE)));

        assertEquals(Integer.MAX_VALUE, target.getMaxHealth());
        target.setMaxHealth(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, target.getMaxHealth());
    }

    @Test
    void fallDamageKeepsFractionalRoundingAndLargeJumpBoostDoesNotIncreaseIt() {
        float[] applied = {0};
        DamageTarget target = new DamageTarget() {
            @Override
            public boolean damage(float amount, DamageSource source) {
                assertEquals(DamageTypes.FALL, source.getDamageType());
                applied[0] += amount;
                return true;
            }
        };

        target.applyFallDamage(3.1f);
        assertEquals(0, applied[0]);
        target.applyFallDamage(4);
        assertEquals(1, applied[0]);
        target.effects.put(EffectTypes.JUMP_BOOST, ActivePotionEffect.from(new PotionEffect(EffectTypes.JUMP_BOOST, 100, Integer.MAX_VALUE)));
        target.applyFallDamage(10);
        assertEquals(1, applied[0]);
        assertThrows(IllegalArgumentException.class, () -> target.applyFallDamage(Float.NaN));
    }

    @Test
    void nestedDamageKeepsCancellationLocalToEachHit() {
        DamageTarget target = new DamageTarget() {
            @Override
            protected boolean applyDamage(CloudEntityDamageEvent event) {
                if (event.getDamageType() == DamageTypes.EXPLOSION) {
                    CloudDamageResult nested = this.damageWithResult(1, DamageSource.of(DamageTypes.GENERIC));
                    assertTrue(nested.cancelled());
                    return true;
                }

                event.setCancelled(true);
                return false;
            }
        };

        CloudDamageResult result = target.damageWithResult(4, DamageSource.of(DamageTypes.EXPLOSION));
        assertTrue(result.applied());
        assertFalse(result.cancelled());
    }

    @Test
    void rejectionDoesNotImplyListenerCancellation() {
        DamageTarget target = new DamageTarget() {
            @Override
            protected boolean applyDamage(CloudEntityDamageEvent event) {
                return false;
            }
        };

        CloudDamageResult result = target.damageWithResult(4, DamageSource.of(DamageTypes.EXPLOSION));
        assertFalse(result.applied());
        assertFalse(result.cancelled());
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
