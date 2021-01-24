package org.cloudburstmc.server.item.behavior;


import com.nukkitx.math.vector.Vector3f;
import lombok.val;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.world.World;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.Identifier;

import static org.cloudburstmc.server.block.BlockIds.FLOWING_WATER;
import static org.cloudburstmc.server.block.BlockIds.WATER;
import static org.cloudburstmc.server.item.behavior.ItemIds.POTION;

public class ItemGlassBottle extends Item {

    public ItemGlassBottle(Identifier id) {
        super(id);
    }

    @Override
    public boolean canBeActivated() {
        return true;
    }

    @Override
    public boolean onActivate(World world, Player player, Block block, Block target, Direction face, Vector3f clickPos) {
        val targetType = target.getState().getType();
        if (targetType == WATER || targetType == FLOWING_WATER) {
            Item potion = Item.get(POTION);

            if (this.getCount() == 1) {
                player.getInventory().setItemInHand(potion);
            } else if (this.getCount() > 1) {
                this.decrementCount();
                player.getInventory().setItemInHand(this);
                if (player.getInventory().canAddItem(potion)) {
                    player.getInventory().addItem(potion);
                } else {
                    player.getWorld().dropItem(player.getPosition().add(0, 1.3, 0), potion, player.getDirectionVector().mul(0.4));
                }
            }
        }
        return false;
    }
}
