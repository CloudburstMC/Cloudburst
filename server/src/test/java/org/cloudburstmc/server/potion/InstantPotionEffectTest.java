package org.cloudburstmc.server.potion;

import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.RegainReason;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.event.entity.PotionEffectCause;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.potion.EffectTypes;
import org.cloudburstmc.api.potion.PotionEffect;
import org.cloudburstmc.api.potion.PotionType;
import org.cloudburstmc.api.potion.PotionTypes;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.server.entity.EntityLiving;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InstantPotionEffectTest {

    @Test
    void roundsHealingAndDamageToWholeAmounts() {
        assertEquals(4, apply(PotionTypes.HEALING, 1, false).healed);
        assertEquals(6, apply(PotionTypes.HARMING, 1, false).damaged);
        assertEquals(1, apply(PotionTypes.HEALING, 0.2, false).healed);
        assertEquals(0, apply(PotionTypes.HARMING, 0, false).damaged);
    }

    @Test
    void invertsHealingAndHarmingForUndeadTargets() {
        assertEquals(6, apply(PotionTypes.HEALING, 1, true).damaged);
        assertEquals(4, apply(PotionTypes.HARMING, 1, true).healed);
    }

    @Test
    void largeAmplifiersStayBoundedInsteadOfWrappingTheShift() {
        PotionType potion = PotionType.of(Identifier.parse("test:strong_healing"),
                new PotionEffect(EffectTypes.INSTANT_HEALTH, 1, Integer.MAX_VALUE));

        assertEquals((float) Integer.MAX_VALUE, apply(potion, 1, false).healed);
    }

    @Test
    void rejectsInvalidIntensity() {
        assertThrows(IllegalArgumentException.class, () -> apply(PotionTypes.HEALING, Double.NaN, false));
        assertThrows(IllegalArgumentException.class, () -> apply(PotionTypes.HARMING, -1, false));
    }

    private static RecordingTarget apply(PotionType potion, double intensity, boolean undead) {
        RecordingTarget target = new RecordingTarget(undead);
        new CloudPotion(potion).apply(target, intensity, 1,
                DamageSource.of(DamageTypes.MAGIC), null, PotionEffectCause.POTION_SPLASH);
        return target;
    }

    private static class RecordingTarget extends EntityLiving {
        private final boolean undead;
        private float healed;
        private float damaged;

        private RecordingTarget(boolean undead) {
            super(EntityTypes.ZOMBIE, Location.from(Vector3f.ZERO, InterfaceProxy.create(Level.class)));
            this.undead = undead;
        }

        @Override
        public boolean isUndead() {
            return this.undead;
        }

        @Override
        public void heal(float amount, RegainReason reason) {
            this.healed += amount;
        }

        @Override
        public boolean damage(float amount, DamageSource source) {
            this.damaged += amount;
            return true;
        }
    }
}
