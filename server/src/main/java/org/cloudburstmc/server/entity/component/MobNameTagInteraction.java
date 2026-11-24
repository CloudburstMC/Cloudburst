package org.cloudburstmc.server.entity.component;

import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.entity.EntityComponents;
import org.cloudburstmc.api.inventory.view.PlayerInventoryView;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.registry.CloudEntityRegistry;

@UtilityClass
public class MobNameTagInteraction {

    public static boolean tryApply(CloudEntity entity, Player player, ItemStack item) {
        if (item.isEmpty() || item.getType() != ItemTypes.NAME_TAG || !entity.isAlive()) {
            return false;
        }

        String name = item.get(ItemDataComponents.CUSTOM_NAME);
        if (name == null || name.isEmpty()) {
            return false;
        }

        PlayerInventoryView inventory = player.getInventory();
        ItemStack selected = inventory.getSelectedItem();
        if (!selected.equals(item)) {
            return false;
        }

        if (!CloudEntityRegistry.get().requireComponent(entity.getType(), EntityComponents.CAN_BE_NAMED).execute(entity)) {
            return false;
        }

        entity.customName(Component.text(name));
        entity.setCustomNameVisible(true);
        if (!player.isCreative()) {
            inventory.setSelectedItem(selected.decreaseCount());
        }

        return true;
    }
}
