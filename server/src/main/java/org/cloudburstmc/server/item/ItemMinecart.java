package org.cloudburstmc.server.item;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.behavior.BlockBehaviorRail;
import org.cloudburstmc.server.entity.EntityTypes;
import org.cloudburstmc.server.entity.vehicle.Minecart;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.level.Location;
import org.cloudburstmc.server.math.BlockFace;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.EntityRegistry;
import org.cloudburstmc.server.utils.Identifier;
import org.cloudburstmc.server.utils.Rail;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class ItemMinecart extends Item {

    public ItemMinecart(Identifier id) {
        super(id);
    }

    @Override
    public boolean canBeActivated() {
        return true;
    }

    @Override
    public boolean onActivate(Level level, Player player, Block block, Block target, BlockFace face, Vector3f clickPos) {
        if (Rail.isRailBlock(target)) {
            Rail.Orientation type = ((BlockBehaviorRail) target).getOrientation();
            double adjacent = 0.0D;
            if (type.isAscending()) {
                adjacent = 0.5D;
            }
            Vector3f pos = target.getPosition().toFloat().add(0.5, 0.0625 + adjacent, 0.5);
            Minecart minecart = EntityRegistry.get().newEntity(EntityTypes.MINECART, Location.from(pos, level));
            minecart.spawnToAll();

            if (player.isSurvival()) {
                Item item = player.getInventory().getItemInHand();
                item.decrementCount();
                player.getInventory().setItemInHand(item);
            }

            return true;
        }
        return false;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }
}
