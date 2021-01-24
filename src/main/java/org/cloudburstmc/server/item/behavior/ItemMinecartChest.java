package org.cloudburstmc.server.item.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.entity.EntityTypes;
import org.cloudburstmc.server.entity.vehicle.ChestMinecart;
import org.cloudburstmc.server.world.World;
import org.cloudburstmc.server.world.Location;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.EntityRegistry;
import org.cloudburstmc.server.utils.Identifier;
import org.cloudburstmc.server.utils.Rail;
import org.cloudburstmc.server.utils.data.RailDirection;

public class ItemMinecartChest extends Item {

    public ItemMinecartChest(Identifier id) {
        super(id);
    }

    @Override
    public boolean canBeActivated() {
        return true;
    }

    @Override
    public boolean onActivate(World world, Player player, Block block, Block target, Direction face, Vector3f clickPos) {
        if (Rail.isRailBlock(target.getState())) {
            RailDirection type = target.getState().ensureTrait(BlockTraits.RAIL_DIRECTION); //TODO: check errors
            double adjacent = 0.0D;
            if (type.isAscending()) {
                adjacent = 0.5D;
            }
            Vector3f pos = target.getPosition().toFloat().add(0.5, 0.0625 + adjacent, 0.5);
            ChestMinecart minecart = EntityRegistry.get().newEntity(EntityTypes.CHEST_MINECART, Location.from(pos, world));

            if (player.isSurvival()) {
                Item item = player.getInventory().getItemInHand();
                item.setCount(item.getCount() - 1);
                player.getInventory().setItemInHand(item);
            }

            minecart.spawnToAll();
            decrementCount();
            return true;
        }
        return false;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }
}
