package org.cloudburstmc.server.potion;

import lombok.NonNull;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.event.entity.EntityRegainHealthEvent;
import org.cloudburstmc.api.potion.Effect;
import org.cloudburstmc.api.potion.EffectType;
import org.cloudburstmc.api.potion.EffectTypes;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.packet.MobEffectPacket;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.network.NetworkUtils;
import org.cloudburstmc.server.player.CloudPlayer;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.INVISIBLE;

public class CloudEffect extends Effect {

    private static final String TAG_ID = "Id";
    private static final String TAG_AMPLIFIER = "Amplifier";
    private static final String TAG_DURATION = "Duration";
    private static final String TAG_DURATION_EASY = "DurationEasy";
    private static final String TAG_DURATION_NORMAL = "DurationNormal";
    private static final String TAG_DURATION_HARD = "DurationHard";
    private static final String TAG_AMBIENT = "Ambient";
    private static final String TAG_SHOW_PARTICLES = "ShowParticles";
    private static final String TAG_DISPLAY_ON_SCREEN_TEXTURE_ANIMATION = "DisplayOnScreenTextureAnimation";

    public CloudEffect(@NonNull EffectType type) {
        super(type);
    }

    public static Effect fromNBT(NbtMap tag) {
        return fromNBT(tag.getByte(TAG_ID),
                tag.getBoolean(TAG_AMBIENT),
                tag.getByte(TAG_AMPLIFIER),
                tag.getBoolean(TAG_SHOW_PARTICLES),
                tag.getInt(TAG_DURATION));
    }

    public static Effect fromNBT(byte id, boolean ambient, int amplifier, boolean visible, int duration ) {
        return new CloudEffect(NetworkUtils.effectFromLegacy(id))
                .setAmbient(ambient)
                .setAmplifier(amplifier)
                .setVisible(visible)
                .setDuration(duration);
    }

    public String getName() {
        return getType().getId().getName();
    }

    public byte getNetworkId() {
        return NetworkUtils.effectToNetwork(this.getType());
    }

    public boolean canTick() {
        int interval;
        if (this.getType() == EffectTypes.POISON) {
            if ((interval = (25 >> this.getAmplifier())) > 0) {
                return (this.getDuration() % interval) == 0;
            }
            return true;
        } else if (this.getType() == EffectTypes.WITHER) {
            if ((interval = (40 >> this.getAmplifier())) > 0) {
                return (this.getDuration() % interval) == 0;
            }
            return true;
        } else if (this.getType() == EffectTypes.REGENERATION) {
            if ((interval = (50 >> this.getAmplifier())) > 0) {
                return (this.getDuration() % interval) == 0;
            }
            return true;
        }
        return false;
    }

    public void applyEffect(Entity entity) {
        if (this.getType() == EffectTypes.POISON) {
            if (entity.getHealth() > 1) {
                entity.damage(1, DamageSource.of(DamageTypes.MAGIC));
            }
        } else if (this.getType() == EffectTypes.WITHER) {
            entity.damage(1, DamageSource.of(DamageTypes.WITHER));
        } else if (this.getType() == EffectTypes.REGENERATION) {
            if (entity.getHealth() < entity.getMaxHealth()) {
                entity.heal(new EntityRegainHealthEvent(entity, 1, EntityRegainHealthEvent.CAUSE_MAGIC));
            }
        }
    }

    public void add(Entity entity) {
        CloudEffect oldEffect = (CloudEffect) entity.getEffect(getType());
        if (oldEffect != null && (Math.abs(this.getAmplifier()) < Math.abs(oldEffect.getAmplifier()) ||
                Math.abs(this.getAmplifier()) == Math.abs(oldEffect.getAmplifier())
                        && this.getDuration() < oldEffect.getDuration())) {
            return;
        }
        if (entity instanceof CloudPlayer player) {
            MobEffectPacket.Event event = oldEffect == null ? MobEffectPacket.Event.ADD : MobEffectPacket.Event.MODIFY;
            MobEffectPacket packet = NetworkUtils.effectToNetwork(this, player.getRuntimeId(), event,
                    player.getServer().getTick());
            player.sendPacket(packet);

            if (this.getType() == EffectTypes.SPEED) {
                if (oldEffect != null) {
                    player.setMovementSpeed(player.getMovementSpeed() / (1 + 0.2f * (oldEffect.getAmplifier() + 1)), false);
                }
                player.setMovementSpeed(player.getMovementSpeed() * (1 + 0.2f * (this.getAmplifier() + 1)));
            }

            if (this.getType() == EffectTypes.SLOWNESS) {
                if (oldEffect != null) {
                    player.setMovementSpeed(player.getMovementSpeed() / (1 - 0.15f * (oldEffect.getAmplifier() + 1)), false);
                }
                player.setMovementSpeed(player.getMovementSpeed() * (1 - 0.15f * (this.getAmplifier() + 1)));
            }
        }

        if (this.getType() == EffectTypes.INVISIBILITY) {
            ((CloudEntity) entity).getData().setFlag(INVISIBLE, true);
            entity.setNameTagVisible(false);
        }

        if (this.getType() == EffectTypes.ABSORPTION) {
            int add = (this.getAmplifier() + 1) * 4;
            if (add > entity.getAbsorption()) entity.setAbsorption(add);
        }
    }

    public void remove(Entity entity) {
        if (entity instanceof CloudPlayer player) {
            MobEffectPacket packet = NetworkUtils.effectToNetwork(this, player.getRuntimeId(), MobEffectPacket.Event.REMOVE,
                    player.getServer().getTick());
            player.sendPacket(packet);

            if (this.getType() == EffectTypes.SPEED) {
                player.setMovementSpeed(player.getMovementSpeed() / (1 + 0.2f * (this.getAmplifier() + 1)));
            }
            if (this.getType() == EffectTypes.SLOWNESS) {
                player.setMovementSpeed(player.getMovementSpeed() / (1 - 0.15f * (this.getAmplifier() + 1)));
            }
        }

        if (this.getType() == EffectTypes.INVISIBILITY) {
            ((CloudEntity) entity).getData().setFlag(INVISIBLE, false);
            entity.setNameTagVisible(true);
        }

        if (this.getType() == EffectTypes.ABSORPTION) {
            entity.setAbsorption(0);
        }
    }

    public NbtMap createTag() {
        return NbtMap.builder().putByte(TAG_ID, getNetworkId())
                .putBoolean(TAG_AMBIENT, isAmbient())
                .putByte(TAG_AMPLIFIER, (byte) getAmplifier())
                .putBoolean(TAG_SHOW_PARTICLES, isVisible())
                .putInt(TAG_DURATION, getDuration())
                .build();
    }

    @Override
    public CloudEffect setDuration(int ticks) {
        super.setDuration(ticks);
        return this;
    }

    @Override
    public CloudEffect setAmplifier(int amplifier) {
        super.setAmplifier(amplifier);
        return this;
    }

    @Override
    public CloudEffect setVisible(boolean visible) {
        super.setVisible(visible);
        return this;
    }

    @Override
    public CloudEffect setAmbient(boolean ambient) {
        super.setAmbient(ambient);
        return this;
    }
}
