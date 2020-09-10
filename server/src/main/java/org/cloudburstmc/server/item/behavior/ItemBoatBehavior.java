package org.cloudburstmc.server.item.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.behavior.BlockBehaviorWater;
import org.cloudburstmc.server.entity.EntityTypes;
import org.cloudburstmc.server.entity.vehicle.Boat;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.level.Location;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.EntityRegistry;
import org.cloudburstmc.server.utils.data.TreeSpecies;

/**
 * Created by yescallop on 2016/2/13.
 */
public class ItemBoatBehavior extends CloudItemBehavior {

    @Override
    public boolean canBeActivated() {
        return true;
    }

    @Override
    public boolean onActivate(ItemStack item, Player player, Block block, Block target, Direction face, Vector3f clickPos, Level level) {
        if (face != Direction.UP) return false;
        Vector3f spawnPos = Vector3f.from(block.getX() + 0.5,
                block.getY() - (target instanceof BlockBehaviorWater ? 0.0625 : 0), block.getZ());
        Boat boat = EntityRegistry.get().newEntity(EntityTypes.BOAT, Location.from(spawnPos, level));
        boat.setRotation((player.getYaw() + 90f) % 360, 0);
        boat.setWoodType(item.ensureMetadata(TreeSpecies.class));

        if (player.isSurvival()) {
            player.getInventory().decrementHandCount();
        }

        boat.spawnToAll();
        return true;
    }

    @Override
    public int getMaxStackSize(ItemStack item) {
        return 1;
    }
}
