package org.cloudburstmc.server.entity.passive;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.entity.Shearable;
import org.cloudburstmc.api.event.player.PlayerShearEntityEvent;
import org.cloudburstmc.api.inventory.view.PlayerInventoryView;
import org.cloudburstmc.api.item.ItemBehaviors;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

@UtilityClass
public class PlayerShearing {

    public static boolean tryShear(Shearable entity, Player player, ItemStack tool, Supplier<List<ItemStack>> createDrops, Consumer<List<ItemStack>> applyShearing) {
        if (tool.getType() != ItemTypes.SHEARS || !entity.readyForShearing()) {
            return false;
        }

        Level level = entity.getLevel();
        PlayerInventoryView inventory = player.getInventory();
        int slot = inventory.getSelectedSlot();
        if (!tool.equals(inventory.getItem(slot))) {
            return false;
        }

        PlayerShearEntityEvent event = new PlayerShearEntityEvent(player, entity, tool, createDrops.get());
        level.getServer().getEventManager().fire(event);
        if (event.isCancelled() || !entity.readyForShearing()
                || entity.getLevel() != level || player.getLevel() != level
                || inventory.getSelectedSlot() != slot || !tool.equals(inventory.getItem(slot))) {
            return true;
        }

        applyShearing.accept(event.getDrops());
        if (!player.isCreative() && tool.equals(inventory.getItem(slot))) {
            ItemStack damaged = CloudItemRegistry.get().requireComponent(tool.getType(), ItemBehaviors.ON_DAMAGE).execute(tool, 1, player);
            if (tool.equals(inventory.getItem(slot))) {
                inventory.setItem(slot, damaged);
            }
        }

        return true;
    }
}
