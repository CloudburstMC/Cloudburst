package org.cloudburstmc.server.item.food;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockComponents;
import org.cloudburstmc.api.block.BlockTags;
import org.cloudburstmc.api.event.player.PlayerTeleportEvent;
import org.cloudburstmc.api.item.ItemIds;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.Sound;
import org.cloudburstmc.server.registry.CloudBlockRegistry;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by Leonidius20 on 20.08.18.
 */
public class FoodChorusFruit extends FoodNormal {

    public FoodChorusFruit() {
        super(4, 2.4F);
        setMetadata(ItemIds.CHORUS_FRUIT);
    }

    @Override
    public boolean onEatenBy(Player player) {
        super.onEatenBy(player);
        // Teleportation
        int minX = player.getPosition().getFloorX() - 8;
        int minY = player.getPosition().getFloorY() - 8;
        int minZ = player.getPosition().getFloorZ() - 8;
        int maxX = minX + 16;
        int maxY = minY + 16;
        int maxZ = minZ + 16;

        CloudLevel level = (CloudLevel) player.getLevel();
        if (level == null) return false;

        for (int attempts = 0; attempts < 128; attempts++) {
            int x = ThreadLocalRandom.current().nextInt(minX, maxX);
            int y = ThreadLocalRandom.current().nextInt(minY, maxY);
            int z = ThreadLocalRandom.current().nextInt(minZ, maxZ);

            if (y < 0) continue;

            while (y >= 0 && !CloudBlockRegistry.REGISTRY.getComponent(level.getBlockState(x, y + 1, z).getType(), BlockComponents.SOLID).get()) {
                y--;
            }
            y++; // Back up to non solid

            BlockState blockStateUp = level.getBlockState(x, y + 1, z);
            BlockState blockStateUp2 = level.getBlockState(x, y + 2, z);

            if (CloudBlockRegistry.REGISTRY.getComponent(blockStateUp.getType(), BlockComponents.SOLID).get() || CloudBlockRegistry.REGISTRY.getComponent(blockStateUp.getType(), BlockComponents.LIQUID).get() ||
                    CloudBlockRegistry.REGISTRY.getComponent(blockStateUp2.getType(), BlockComponents.SOLID).get() || CloudBlockRegistry.REGISTRY.getComponent(blockStateUp2.getType(), BlockComponents.LIQUID).get()) {
                continue;
            }

            // Sounds are broadcast at both source and destination
            level.addSound(player.getPosition(), Sound.MOB_ENDERMEN_PORTAL);
            player.teleport(Vector3f.from(x + 0.5, y + 1, z + 0.5), PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT);
            level.addSound(player.getPosition(), Sound.MOB_ENDERMEN_PORTAL);

            break;
        }

        return true;
    }

}