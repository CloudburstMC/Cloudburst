package org.cloudburstmc.server.potion;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.protocol.bedrock.packet.MobEffectPacket;
import lombok.NonNull;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;
import org.cloudburstmc.api.event.entity.EntityRegainHealthEvent;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.potion.Effect;
import org.cloudburstmc.api.potion.EffectType;
import org.cloudburstmc.api.potion.EffectTypes;
import org.cloudburstmc.server.entity.BaseEntity;
import org.cloudburstmc.server.network.NetworkUtils;
import org.cloudburstmc.server.player.CloudPlayer;

import static com.nukkitx.protocol.bedrock.data.entity.EntityFlag.INVISIBLE;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
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
        if (EffectTypes.POISON.equals(this.getType().getId())) {
            if ((interval = (25 >> this.getAmplifier())) > 0) {
                return (this.getDuration() % interval) == 0;
            }
            return true;
        } else if (EffectTypes.WITHER.equals(this.getType().getId())) {
            if ((interval = (50 >> this.getAmplifier())) > 0) {
                return (this.getDuration() % interval) == 0;
            }
            return true;
        } else if (EffectTypes.REGENERATION.equals(this.getType().getId())) {
            if ((interval = (40 >> this.getAmplifier())) > 0) {
                return (this.getDuration() % interval) == 0;
            }
            return true;
        }
        return false;
    }

    public void applyEffect(Entity entity) {
        if (EffectTypes.POISON.equals(this.getType().getId())) {
            if (entity.getHealth() > 1) {
                entity.attack(new EntityDamageEvent(entity, EntityDamageEvent.DamageCause.MAGIC, 1));
            }
        } else if (EffectTypes.WITHER.equals(this.getType().getId())) {
            entity.attack(new EntityDamageEvent(entity, EntityDamageEvent.DamageCause.MAGIC, 1));
        } else if (EffectTypes.REGENERATION.equals(this.getType().getId())) {
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
        if (entity instanceof CloudPlayer) {
            CloudPlayer player = (CloudPlayer) entity;

            MobEffectPacket packet = new MobEffectPacket();
            packet.setRuntimeEntityId(entity.getRuntimeId());
            packet.setEffectId(this.getNetworkId());
            packet.setAmplifier(this.getAmplifier());
            packet.setParticles(this.isVisible());
            packet.setDuration(this.getDuration());
            if (oldEffect != null) {
                packet.setEvent(MobEffectPacket.Event.MODIFY);
            } else {
                packet.setEvent(MobEffectPacket.Event.ADD);
            }

            player.sendPacket(packet);

            if (this.getType() == EffectTypes.SWIFTNESS) {
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
            ((BaseEntity)entity).getData().setFlag(INVISIBLE, true);
            entity.setNameTagVisible(false);
        }

        if (this.getType() == EffectTypes.ABSORPTION) {
            int add = (this.getAmplifier() + 1) * 4;
            if (add > entity.getAbsorption()) entity.setAbsorption(add);
        }
    }

    public void remove(Entity entity) {
        if (entity instanceof Player) {
            MobEffectPacket packet = new MobEffectPacket();
            packet.setRuntimeEntityId(entity.getRuntimeId());
            packet.setEffectId(this.getNetworkId());
            packet.setEvent(MobEffectPacket.Event.REMOVE);

            ((CloudPlayer) entity).sendPacket(packet);

            if (this.getType() == EffectTypes.SWIFTNESS) {
                ((Player) entity).setMovementSpeed(((Player) entity).getMovementSpeed() / (1 + 0.2f * (this.getAmplifier() + 1)));
            }
            if (this.getType() == EffectTypes.SLOWNESS) {
                ((Player) entity).setMovementSpeed(((Player) entity).getMovementSpeed() / (1 - 0.15f * (this.getAmplifier() + 1)));
            }
        }

        if (this.getType() == EffectTypes.INVISIBILITY) {
            ((BaseEntity)entity).getData().setFlag(INVISIBLE, false);
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
