package org.cloudburstmc.server.potion;

import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.potion.EffectTypes;
import org.cloudburstmc.api.potion.PotionEffect;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.server.entity.EntityLiving;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FreezingEffectTest {

    @Test
    void freezingAppliesEveryTick() {
        ActivePotionEffect effect = ActivePotionEffect.from(new PotionEffect(EffectTypes.FREEZING, 900, 0));

        for (int tick = 0; tick < 900; tick++) {
            assertTrue(effect.shouldApplyTick(tick));
            effect.decreaseDuration(1);
        }

        assertFalse(effect.shouldApplyTick(900));
    }

    @Test
    void infiniteFreezingKeepsApplying() {
        ActivePotionEffect effect = ActivePotionEffect.from(new PotionEffect(EffectTypes.FREEZING, PotionEffect.INFINITE_DURATION, 0));
        effect.decreaseDuration(1000);

        assertTrue(effect.shouldApplyTick(1000));
    }

    @Test
    void freezingAddsOneTickRegardlessOfAmplifier() {
        FreezingTarget target = new FreezingTarget();
        ActivePotionEffect effect = ActivePotionEffect.from(new PotionEffect(EffectTypes.FREEZING, 900, 4));

        effect.applyTick(target);

        assertEquals(1, target.getFreezeTicks());
    }

    @Test
    void freezingStopsAtTheMaximum() {
        FreezingTarget target = new FreezingTarget();
        target.freezeForTicks(Integer.MAX_VALUE);

        assertEquals(target.getMaxFreezeTicks(), target.getFreezeTicks());
    }

    @Test
    void freezingRespectsImmunity() {
        FreezingTarget target = new FreezingTarget();
        target.canFreeze = false;

        ActivePotionEffect.from(new PotionEffect(EffectTypes.FREEZING, 900, 0)).applyTick(target);

        assertEquals(0, target.getFreezeTicks());
    }

    @Test
    void freezingRespectsLockedFreezeTicks() {
        FreezingTarget target = new FreezingTarget();
        target.setFreezeTicks(20);
        target.lockFreezeTicks(true);

        ActivePotionEffect.from(new PotionEffect(EffectTypes.FREEZING, 900, 0)).applyTick(target);

        assertEquals(20, target.getFreezeTicks());
    }

    private static class FreezingTarget extends EntityLiving {

        private boolean canFreeze = true;

        private FreezingTarget() {
            super(EntityTypes.ZOMBIE, Location.from(Vector3f.ZERO, InterfaceProxy.create(Level.class)));
        }

        @Override
        protected boolean canFreeze() {
            return this.canFreeze;
        }
    }
}
