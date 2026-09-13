package org.cloudburstmc.server.item.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.projectile.EnderPearl;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.item.component.UseHandler;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.concurrent.ThreadLocalRandom;

@UtilityClass
public class EnderPearlItemHandlers {

    private static final int COOLDOWN_TICKS = 20;
    private static final float POWER = 1.5f;
    private static final double INACCURACY = 0.0172275;

    public static final UseHandler USE = (item, entity) -> {
        if (!(entity instanceof CloudPlayer player) || player.hasItemCooldown(ItemTypes.ENDER_PEARL)) {
            return item;
        }

        EnderPearl pearl = player.launchProjectile(EntityTypes.ENDER_PEARL, launchMotion(player));
        if (pearl.isClosed()) {
            return item;
        }

        player.setItemCooldown(ItemTypes.ENDER_PEARL, COOLDOWN_TICKS);
        player.getLevel().addLevelSoundEvent(player.getPosition(), SoundEvent.THROW, -1,
                Identifier.parse("minecraft:player"), false, false);
        return player.isCreative() ? item : item.decreaseCount();
    };

    private static Vector3f launchMotion(CloudPlayer player) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Vector3f direction = player.getDirectionVector();
        Vector3f projectileMotion = Vector3f.from(
                direction.getX() + triangle(random) * INACCURACY,
                direction.getY() + triangle(random) * INACCURACY,
                direction.getZ() + triangle(random) * INACCURACY
        ).mul(POWER);
        Vector3f playerMotion = player.getKnownMovement();
        return projectileMotion.add(
                playerMotion.getX(), player.isOnGround() ? 0 : playerMotion.getY(), playerMotion.getZ());
    }

    private static double triangle(ThreadLocalRandom random) {
        return random.nextDouble() - random.nextDouble();
    }
}
