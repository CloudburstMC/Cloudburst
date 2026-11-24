package org.cloudburstmc.server.entity.misc;

import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.LivingEntity;
import org.cloudburstmc.api.entity.RegainReason;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.potion.EffectType;
import org.cloudburstmc.api.potion.EffectTypes;
import org.cloudburstmc.api.potion.PotionEffect;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.server.entity.CloudLivingEntity;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class AreaEffectCloudInstantEffectTest {

    @Test
    void preservesHalfStrengthAndUndeadInversion() throws ReflectiveOperationException {
        for (boolean undead : new boolean[]{false, true}) {
            for (int amplifier : new int[]{0, 1}) {
                RecordingTarget healing = apply(EffectTypes.INSTANT_HEALTH, amplifier, undead);
                RecordingTarget harming = apply(EffectTypes.INSTANT_DAMAGE, amplifier, undead);

                assertEquals(undead ? 0 : 2 << amplifier, healing.healed);
                assertEquals(undead ? 3 << amplifier : 0, healing.damaged);
                assertEquals(undead ? 2 << amplifier : 0, harming.healed);
                assertEquals(undead ? 0 : 3 << amplifier, harming.damaged);
            }
        }
    }

    @Test
    void preservesLargeAmountsBeforeSaturation() throws ReflectiveOperationException {
        assertEquals(536870912f, apply(EffectTypes.INSTANT_HEALTH, 28, false).healed);
        assertEquals(805306368f, apply(EffectTypes.INSTANT_DAMAGE, 28, false).damaged);
        assertEquals(1073741824f, apply(EffectTypes.INSTANT_HEALTH, 29, false).healed);
        assertEquals(1610612736f, apply(EffectTypes.INSTANT_DAMAGE, 29, false).damaged);
    }

    @Test
    void saturatesInsteadOfWrappingAtLargeAmplifiers() throws ReflectiveOperationException {
        for (int amplifier : new int[]{30, 31, 32, Integer.MAX_VALUE}) {
            for (boolean undead : new boolean[]{false, true}) {
                RecordingTarget healing = apply(EffectTypes.INSTANT_HEALTH, amplifier, undead);
                RecordingTarget harming = apply(EffectTypes.INSTANT_DAMAGE, amplifier, undead);

                assertEquals(undead ? 0 : (float) Integer.MAX_VALUE, healing.healed);
                assertEquals(undead ? (float) Integer.MAX_VALUE : 0, healing.damaged);
                assertEquals(undead ? (float) Integer.MAX_VALUE : 0, harming.healed);
                assertEquals(undead ? 0 : (float) Integer.MAX_VALUE, harming.damaged);
            }
        }
    }

    @Test
    void retainsMagicHealingReasonAndIndirectDamageAttribution() throws ReflectiveOperationException {
        RecordingTarget healing = apply(EffectTypes.INSTANT_HEALTH, 0, false);
        assertEquals(RegainReason.MAGIC, healing.healReason);

        EntityAreaEffectCloud cloud = newCloud();
        RecordingTarget owner = new RecordingTarget(false);
        RecordingTarget target = new RecordingTarget(false);
        cloud.setOwner(owner);
        apply(cloud, target, EffectTypes.INSTANT_DAMAGE, 0);

        assertNotNull(target.damageSource);
        assertSame(DamageTypes.INDIRECT_MAGIC, target.damageSource.getDamageType());
        assertSame(cloud, target.damageSource.getDirectEntity());
        assertSame(owner, target.damageSource.getCausingEntity());
    }

    private static RecordingTarget apply(EffectType type, int amplifier, boolean undead)
            throws ReflectiveOperationException {
        RecordingTarget target = new RecordingTarget(undead);
        EntityAreaEffectCloud cloud = newCloud();
        apply(cloud, target, type, amplifier);
        if (target.damageSource != null) {
            assertSame(cloud, target.damageSource.getDirectEntity());
        }
        return target;
    }

    private static void apply(EntityAreaEffectCloud cloud, RecordingTarget target, EffectType type, int amplifier)
            throws ReflectiveOperationException {
        Method method = EntityAreaEffectCloud.class.getDeclaredMethod("applyInstantEffect", LivingEntity.class, PotionEffect.class);
        method.setAccessible(true);
        method.invoke(cloud, target, new PotionEffect(type, 1, amplifier));
    }

    private static EntityAreaEffectCloud newCloud() {
        return new EntityAreaEffectCloud(EntityTypes.AREA_EFFECT_CLOUD,
                Location.from(Vector3f.ZERO, InterfaceProxy.create(Level.class)));
    }

    private static class RecordingTarget extends CloudLivingEntity {
        private final boolean undead;
        private float healed;
        private float damaged;
        private RegainReason healReason;
        private DamageSource damageSource;

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
            this.healReason = reason;
        }

        @Override
        public boolean damage(float amount, DamageSource source) {
            this.damaged += amount;
            this.damageSource = source;
            return true;
        }
    }
}
