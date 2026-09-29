package org.cloudburstmc.server.potion;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.event.entity.EntityRegainHealthEvent;
import org.cloudburstmc.api.event.entity.PotionEffectCause;
import org.cloudburstmc.api.potion.EffectTypes;
import org.cloudburstmc.api.potion.PotionEffect;
import org.cloudburstmc.api.potion.PotionType;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.entity.EntityLiving;
import org.cloudburstmc.server.player.CloudPlayer;

import static java.util.Objects.requireNonNull;

public class CloudPotion {
    private final PotionType type;

    public CloudPotion(PotionType type) {
        this.type = requireNonNull(type, "type");
    }

    public void apply(Entity entity, double intensity, float durationScale, DamageSource damageSource, @Nullable Entity source, PotionEffectCause cause) {
        if (!(entity instanceof EntityLiving)) {
            return;
        }

        for (PotionEffect effect : this.type.getEffects()) {
            this.applyEffect(entity, effect, intensity, durationScale, damageSource, source, cause);
        }
    }

    private void applyEffect(Entity entity, PotionEffect effect, double intensity, float durationScale, DamageSource damageSource, @Nullable Entity source, PotionEffectCause cause) {
        if (entity instanceof CloudPlayer player && !player.isSurvival() && !player.isAdventure() && effect.isHarmful()) {
            return;
        }

        if (effect.getType() == EffectTypes.INSTANT_HEALTH) {
            this.applyInstantHealth(entity, effect.getAmplifier(), intensity, damageSource);
        } else if (effect.getType() == EffectTypes.INSTANT_DAMAGE) {
            this.applyInstantDamage(entity, effect.getAmplifier(), intensity, damageSource);
        } else {
            int baseDuration = effect.isInfinite() ? PotionEffect.INFINITE_DURATION : effect.getDuration() == 0 ? 0 : Math.max(1, (int) Math.floor(effect.getDuration() * durationScale));
            int duration = effect.isInfinite() ? PotionEffect.INFINITE_DURATION : (int) (intensity * baseDuration + 0.5);
            if (effect.isInfinite() || duration > (cause == PotionEffectCause.ARROW ? 0 : 20)) {
                ((CloudEntity) entity).addPotionEffect(effect.withDuration(duration), source, cause);
            }
        }
    }

    private void applyInstantHealth(Entity entity, int amplifier, double intensity, DamageSource damageSource) {
        if (entity.isUndead()) {
            entity.damage((float) (intensity * (6 << amplifier) + 0.5), damageSource);
        } else {
            entity.heal(new EntityRegainHealthEvent(entity, (float) (intensity * (4 << amplifier) + 0.5), EntityRegainHealthEvent.CAUSE_MAGIC));
        }
    }

    private void applyInstantDamage(Entity entity, int amplifier, double intensity, DamageSource damageSource) {
        if (entity.isUndead()) {
            entity.heal(new EntityRegainHealthEvent(entity, (float) (intensity * (4 << amplifier) + 0.5), EntityRegainHealthEvent.CAUSE_MAGIC));
        } else {
            entity.damage((float) (intensity * (6 << amplifier) + 0.5), damageSource);
        }
    }
}
