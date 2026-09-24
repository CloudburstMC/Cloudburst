package org.cloudburstmc.server.item.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.Projectile;
import org.cloudburstmc.api.item.component.UseHandler;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.concurrent.ThreadLocalRandom;

@UtilityClass
public class ThrowableItemHandlers {

    public static <T extends Projectile> UseHandler throwProjectile(EntityType<T> type, float speed, float pitchOffset) {
        return (item, entity) -> {
            if (!(entity instanceof CloudPlayer player)) {
                return item;
            }

            T projectile = player.launchProjectile(type, launchMotion(player, speed, pitchOffset));
            if (projectile == null) {
                return item;
            }

            playThrowSound(player, SoundEvent.THROW);
            return player.isCreative() ? item : item.decreaseCount();
        };
    }

    public static void playThrowSound(CloudPlayer player, SoundEvent sound) {
        player.getLevel().addLevelSoundEvent(player.getPosition(), sound, -1,
                Identifier.parse("minecraft:player"), false, false);
    }

    public static Vector3f launchMotion(CloudPlayer player, float speed, float pitchOffset) {
        double pitch = Math.toRadians(player.getPitch());
        double yaw = Math.toRadians(player.getYaw());
        float spread = 0.0172275f;

        ThreadLocalRandom random = ThreadLocalRandom.current();
        Vector3f direction = Vector3f.from(
                -Math.cos(pitch) * Math.sin(yaw),
                -Math.sin(Math.toRadians(player.getPitch() + pitchOffset)),
                Math.cos(pitch) * Math.cos(yaw)
        ).normalize();

        Vector3f motion = direction.add(
                (random.nextFloat() - random.nextFloat()) * spread,
                (random.nextFloat() - random.nextFloat()) * spread,
                (random.nextFloat() - random.nextFloat()) * spread
        ).mul(speed);

        Vector3f movement = player.getKnownMovement();
        return motion.add(movement.getX(), player.isOnGround() ? 0 : movement.getY(), movement.getZ());
    }
}
