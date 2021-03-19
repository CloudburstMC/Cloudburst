package org.cloudburstmc.server.item.behavior;


import com.nukkitx.math.vector.Vector3f;
import lombok.val;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.server.inventory.PlayerInventory;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import static org.cloudburstmc.api.block.BlockTypes.FLOWING_WATER;
import static org.cloudburstmc.api.block.BlockTypes.WATER;
import static org.cloudburstmc.server.item.ItemTypes.POTION;

public class ItemGlassBottleBehavior extends CloudItemBehavior {

    @Override
    public boolean canBeActivated() {
        return true;
    }

    @Override
    public ItemStack onActivate(ItemStack item, Player player, Block block, Block target, Direction face, Vector3f clickPos, Level level) {
        val targetType = target.getState().getType();
        if (targetType == WATER || targetType == FLOWING_WATER) {
            ItemStack potion = CloudItemRegistry.get().getItem(POTION);

            if (item.getAmount() == 1) {
                ((PlayerInventory) player.getInventory()).setItemInHand(potion);
            } else if (item.getAmount() > 1) {
                ((PlayerInventory) player.getInventory()).decrementHandCount();
                if (player.getInventory().canAddItem(potion)) {
                    player.getInventory().addItem(potion);
                } else {
                    player.getLevel().dropItem(player.getPosition().add(0, 1.3, 0), potion, player.getDirectionVector().mul(0.4));
                }
            }
        }

        return null;
    }
}
