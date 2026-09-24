package org.cloudburstmc.server.potion;

import lombok.Getter;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.event.entity.EntityRegainHealthEvent;
import org.cloudburstmc.api.potion.EffectType;
import org.cloudburstmc.api.potion.EffectTypes;
import org.cloudburstmc.api.potion.PotionEffect;
import org.cloudburstmc.protocol.bedrock.packet.MobEffectPacket;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.network.NetworkUtils;
import org.cloudburstmc.server.player.CloudPlayer;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;
import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.INVISIBLE;

/**
 * Mutable state for a potion effect currently active on an entity.
 */
public class ActivePotionEffect {
    @Getter
    private final EffectType type;
    @Getter
    private final int amplifier;
    @Getter
    private final boolean ambient;
    private final boolean particles;
    @Getter
    private int duration;

    private ActivePotionEffect(PotionEffect effect) {
        this.type = effect.getType();
        this.duration = effect.getDuration();
        this.amplifier = effect.getAmplifier();
        this.ambient = effect.isAmbient();
        this.particles = effect.hasParticles();
    }

    public static ActivePotionEffect from(PotionEffect effect) {
        return new ActivePotionEffect(requireNonNull(effect, "effect"));
    }

    public boolean hasParticles() {
        return this.particles;
    }

    public boolean isInfinite() {
        return this.duration == PotionEffect.INFINITE_DURATION;
    }

    public PotionEffect snapshot() {
        return new PotionEffect(this.type, this.duration, this.amplifier, this.ambient, this.particles);
    }

    public void decreaseDuration(int ticks) {
        checkArgument(ticks >= 0, "ticks cannot be negative");
        if (!this.isInfinite()) {
            this.duration = Math.max(0, this.duration - ticks);
        }
    }

    public boolean shouldApplyTick(int currentTick) {
        int timingValue = this.isInfinite() ? currentTick : this.duration;
        int interval;

        if (this.type == EffectTypes.POISON) {
            interval = 25 >> this.amplifier;
        } else if (this.type == EffectTypes.WITHER) {
            interval = 40 >> this.amplifier;
        } else if (this.type == EffectTypes.REGENERATION) {
            interval = 50 >> this.amplifier;
        } else {
            return false;
        }

        return interval == 0 || timingValue % interval == 0;
    }

    public void applyTick(Entity entity) {
        if (this.type == EffectTypes.POISON) {
            if (entity.getHealth() > 1) {
                entity.damage(1, DamageSource.of(DamageTypes.MAGIC));
            }
        } else if (this.type == EffectTypes.WITHER) {
            entity.damage(1, DamageSource.of(DamageTypes.WITHER));
        } else if (this.type == EffectTypes.REGENERATION && entity.getHealth() < entity.getMaxHealth()) {
            entity.heal(new EntityRegainHealthEvent(entity, 1, EntityRegainHealthEvent.CAUSE_MAGIC));
        }
    }

    public void onApplied(Entity entity, @Nullable ActivePotionEffect previousEffect) {
        if (entity instanceof CloudPlayer player) {
            MobEffectPacket.Event event = previousEffect == null ? MobEffectPacket.Event.ADD : MobEffectPacket.Event.MODIFY;
            player.sendPacket(NetworkUtils.effectToNetwork(this.snapshot(), player.getRuntimeId(), event, player.getServer().getTick()));

            if (this.type == EffectTypes.SPEED) {
                if (previousEffect != null) {
                    player.setMovementSpeed(player.getMovementSpeed() / (1 + 0.2f * (previousEffect.amplifier + 1)), false);
                }

                player.setMovementSpeed(player.getMovementSpeed() * (1 + 0.2f * (this.amplifier + 1)));
            } else if (this.type == EffectTypes.SLOWNESS) {
                if (previousEffect != null) {
                    player.setMovementSpeed(player.getMovementSpeed() / (1 - 0.15f * (previousEffect.amplifier + 1)), false);
                }

                player.setMovementSpeed(player.getMovementSpeed() * (1 - 0.15f * (this.amplifier + 1)));
            }
        }

        if (this.type == EffectTypes.INVISIBILITY) {
            ((CloudEntity) entity).getData().setFlag(INVISIBLE, true);
            entity.setNameTagVisible(false);
        } else if (this.type == EffectTypes.ABSORPTION) {
            int absorption = (this.amplifier + 1) * 4;
            if (absorption > entity.getAbsorption()) {
                entity.setAbsorption(absorption);
            }
        }
    }

    public void onRemoved(Entity entity) {
        if (entity instanceof CloudPlayer player) {
            player.sendPacket(NetworkUtils.effectToNetwork(this.snapshot(), player.getRuntimeId(),
                    MobEffectPacket.Event.REMOVE, player.getServer().getTick()));

            if (this.type == EffectTypes.SPEED) {
                player.setMovementSpeed(player.getMovementSpeed() / (1 + 0.2f * (this.amplifier + 1)));
            } else if (this.type == EffectTypes.SLOWNESS) {
                player.setMovementSpeed(player.getMovementSpeed() / (1 - 0.15f * (this.amplifier + 1)));
            }
        }

        if (this.type == EffectTypes.INVISIBILITY) {
            ((CloudEntity) entity).getData().setFlag(INVISIBLE, false);
            entity.setNameTagVisible(true);
        } else if (this.type == EffectTypes.ABSORPTION) {
            entity.setAbsorption(0);
        }
    }
}
