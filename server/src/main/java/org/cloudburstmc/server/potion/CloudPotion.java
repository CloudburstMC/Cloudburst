package org.cloudburstmc.server.potion;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.RegainReason;
import org.cloudburstmc.api.entity.damage.DamageSource;
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
        requireNonNull(entity, "entity");
        requireNonNull(damageSource, "damageSource");
        requireNonNull(cause, "cause");

        if (!Double.isFinite(intensity) || intensity < 0 || !Float.isFinite(durationScale) || durationScale < 0) {
            throw new IllegalArgumentException("Potion intensity and duration scale must be finite and non-negative");
        }

        if (!(entity instanceof EntityLiving) || intensity == 0) {
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

        if (effect.getType() == EffectTypes.INSTANT_HEALTH || effect.getType() == EffectTypes.INSTANT_DAMAGE) {
            this.applyInstantEffect(entity, effect, intensity, damageSource);
        } else if (effect.getType() == EffectTypes.SATURATION) {
            if (entity instanceof CloudPlayer player) {
                int nutrition = effect.getAmplifier() + 1;
                player.getFoodData().eat(nutrition, nutrition * 2.0f);
            }
        } else {
            int baseDuration = effect.isInfinite() ? PotionEffect.INFINITE_DURATION : effect.getDuration() == 0 ? 0 : Math.max(1, (int) Math.floor(effect.getDuration() * durationScale));
            int duration = effect.isInfinite() ? PotionEffect.INFINITE_DURATION : (int) (intensity * baseDuration + 0.5);
            if (effect.isInfinite() || duration > (cause == PotionEffectCause.ARROW ? 0 : 20)) {
                ((CloudEntity) entity).addPotionEffect(effect.withDuration(duration), source, cause);
            }
        }
    }

    private void applyInstantEffect(Entity entity, PotionEffect effect, double intensity, DamageSource damageSource) {
        boolean heals = (effect.getType() == EffectTypes.INSTANT_HEALTH) != entity.isUndead();
        int amount = (int) (Math.scalb(intensity * (heals ? 4 : 6), effect.getAmplifier()) + 0.5);
        if (amount == 0) {
            return;
        }

        if (heals) {
            entity.heal(amount, RegainReason.MAGIC);
        } else {
            entity.damage(amount, damageSource);
        }
    }
}
