package org.cloudburstmc.server.item;


import com.nukkitx.math.vector.Vector3f;
import lombok.val;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.Identifier;

import static org.cloudburstmc.server.block.BlockIds.FLOWING_WATER;
import static org.cloudburstmc.server.block.BlockIds.WATER;
import static org.cloudburstmc.server.item.ItemIds.POTION;

public class ItemGlassBottle extends Item {

    public ItemGlassBottle(Identifier id) {
        super(id);
    }

    @Override
    public boolean canBeActivated() {
        return true;
    }

    @Override
    public boolean onActivate(Level level, Player player, Block block, Block target, Direction face, Vector3f clickPos) {
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
                    player.getLevel().dropItem(player.getPosition().add(0, 1.3, 0), potion, player.getDirectionVector().mul(0.4));
                }
            }
        }
        return false;
    }
}
