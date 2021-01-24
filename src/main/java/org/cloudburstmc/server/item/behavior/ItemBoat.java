package org.cloudburstmc.server.item.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.behavior.BlockBehaviorWater;
import org.cloudburstmc.server.entity.EntityTypes;
import org.cloudburstmc.server.entity.vehicle.Boat;
import org.cloudburstmc.server.world.World;
import org.cloudburstmc.server.world.Location;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.EntityRegistry;
import org.cloudburstmc.server.utils.Identifier;

/**
 * Created by yescallop on 2016/2/13.
 */
public class ItemBoat extends Item {

    public ItemBoat(Identifier id) {
        super(id);
    }

    @Override
    public boolean canBeActivated() {
        return true;
    }

    @Override
    public boolean onActivate(World world, Player player, Block block, Block target, Direction face, Vector3f clickPos) {
        if (face != Direction.UP) return false;
        Vector3f spawnPos = Vector3f.from(block.getX() + 0.5,
                block.getY() - (target instanceof BlockBehaviorWater ? 0.0625 : 0), block.getZ());
        Boat boat = EntityRegistry.get().newEntity(EntityTypes.BOAT, Location.from(spawnPos, world));
        boat.setRotation((player.getYaw() + 90f) % 360, 0);
        boat.setWoodType(this.getMeta());

        if (player.isSurvival()) {
            Item item = player.getInventory().getItemInHand();
            item.setCount(item.getCount() - 1);
            player.getInventory().setItemInHand(item);
        }

        boat.spawnToAll();
        return true;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }
}
