package org.cloudburstmc.server.item.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.misc.FireworksRocket;
import org.cloudburstmc.api.item.ItemKeys;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.component.UseHandler;
import org.cloudburstmc.api.item.component.UseOnHandler;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.entity.misc.EntityFireworksRocket;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudEntityRegistry;

import java.util.Objects;

@UtilityClass
public class FireworkRocketItemHandlers {

    private static final double BLOCK_USE_OFFSET = 0.15;

    public static final UseHandler USE = (item, entity) -> {
        if (!(entity instanceof CloudPlayer player) || !player.isGliding()) {
            return item;
        }

        spawnRocket(player, player.getLocation().getPosition(), item, true);
        return consume(item, player);
    };

    public static final UseOnHandler USE_ON = (item, entity, blockPosition, face, clickPosition) -> {
        if (!(entity instanceof CloudPlayer player)) {
            return item;
        }

        if (player.isGliding()) {
            return USE.execute(item, player);
        }

        Vector3f spawnPosition = resolveBlockUseSpawnPosition(blockPosition, face, clickPosition);
        spawnRocket(player, spawnPosition, item, false);

        return consume(item, player);
    };

    private static Vector3f resolveBlockUseSpawnPosition(Vector3i blockPosition, Direction face, Vector3f clickPosition) {
        Objects.requireNonNull(blockPosition, "blockPosition");
        Objects.requireNonNull(face, "face");
        Objects.requireNonNull(clickPosition, "clickPosition");

        double x = blockPosition.getX() + clickPosition.getX();
        double y = blockPosition.getY() + clickPosition.getY();
        double z = blockPosition.getZ() + clickPosition.getZ();

        if (face.getStepX() != 0) {
            x = blockPosition.getX() + (face.getStepX() > 0 ? 1 : 0);
        }

        if (face.getStepY() != 0) {
            y = blockPosition.getY() + (face.getStepY() > 0 ? 1 : 0);
        }

        if (face.getStepZ() != 0) {
            z = blockPosition.getZ() + (face.getStepZ() > 0 ? 1 : 0);
        }

        return Vector3f.from(
                x + face.getStepX() * BLOCK_USE_OFFSET,
                y + face.getStepY() * BLOCK_USE_OFFSET,
                z + face.getStepZ() * BLOCK_USE_OFFSET
        );
    }

    private static void spawnRocket(CloudPlayer player, Vector3f position, ItemStack item, boolean boostPlayer) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(item, "item");

        CloudLevel level = player.getLevel();
        Location location = Location.from(position, level);
        FireworksRocket entity = CloudEntityRegistry.get().newEntity(EntityTypes.FIREWORKS_ROCKET, location);
        if (!(entity instanceof EntityFireworksRocket rocket)) {
            throw new IllegalStateException("Fireworks rocket registry returned " + entity.getClass().getName());
        }

        rocket.setFireworkData(item.get(ItemKeys.FIREWORK_DATA));
        if (boostPlayer) {
            rocket.setBoostedPlayer(player);
        }

        rocket.spawnToAll();
        rocket.spawnTo(player);
    }

    private static ItemStack consume(ItemStack item, CloudPlayer player) {
        if (player.isCreative()) {
            return item;
        }

        return item.decreaseCount();
    }
}
