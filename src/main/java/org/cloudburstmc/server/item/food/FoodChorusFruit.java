package org.cloudburstmc.server.item.food;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.BlockCategory;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.event.player.PlayerTeleportEvent;
import org.cloudburstmc.server.item.behavior.ItemIds;
import org.cloudburstmc.server.world.World;
import org.cloudburstmc.server.world.Sound;
import org.cloudburstmc.server.player.Player;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by Leonidius20 on 20.08.18.
 */
public class FoodChorusFruit extends FoodNormal {

    public FoodChorusFruit() {
        super(4, 2.4F);
        addRelative(ItemIds.CHORUS_FRUIT);
    }

    @Override
    protected boolean onEatenBy(Player player) {
        super.onEatenBy(player);
        // Teleportation
        int minX = player.getPosition().getFloorX() - 8;
        int minY = player.getPosition().getFloorY() - 8;
        int minZ = player.getPosition().getFloorZ() - 8;
        int maxX = minX + 16;
        int maxY = minY + 16;
        int maxZ = minZ + 16;

        World world = player.getWorld();
        if (world == null) return false;

        for (int attempts = 0; attempts < 128; attempts++) {
            int x = ThreadLocalRandom.current().nextInt(minX, maxX);
            int y = ThreadLocalRandom.current().nextInt(minY, maxY);
            int z = ThreadLocalRandom.current().nextInt(minZ, maxZ);

            if (y < 0) continue;

            while (y >= 0 && !world.getBlockAt(x, y + 1, z).inCategory(BlockCategory.SOLID)) {
                y--;
            }
            y++; // Back up to non solid

            BlockState blockStateUp = world.getBlockAt(x, y + 1, z);
            BlockState blockStateUp2 = world.getBlockAt(x, y + 2, z);

            if (blockStateUp.inCategory(BlockCategory.SOLID) || blockStateUp.inCategory(BlockCategory.LIQUID) ||
                    blockStateUp2.inCategory(BlockCategory.SOLID) || blockStateUp2.inCategory(BlockCategory.LIQUID)) {
                continue;
            }

            // Sounds are broadcast at both source and destination
            world.addSound(player.getPosition(), Sound.MOB_ENDERMEN_PORTAL);
            player.teleport(Vector3f.from(x + 0.5, y + 1, z + 0.5), PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT);
            world.addSound(player.getPosition(), Sound.MOB_ENDERMEN_PORTAL);

            break;
        }

        return true;
    }

}