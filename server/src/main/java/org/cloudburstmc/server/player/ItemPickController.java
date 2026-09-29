package org.cloudburstmc.server.player;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.inventory.view.PlayerInventoryView;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;

import java.util.Map;
import java.util.Objects;

@UtilityClass
public class ItemPickController {

    public static int getTargetSlot(PlayerInventoryView inventory, int sourceSlot) {
        return sourceSlot >= 0 && sourceSlot < inventory.getHotbarSize() ? sourceSlot : suitableHotbarSlot(inventory);
    }

    public static void pick(PlayerInventoryView inventory, ItemStack item, int source, int target, boolean creative) {
        Objects.requireNonNull(inventory, "inventory");
        Objects.requireNonNull(item, "item");
        if (source < -1 || source >= inventory.size() || target < 0 || target >= inventory.getHotbarSize()) {
            throw new IllegalArgumentException("Pick slots are outside the inventory");
        }

        if (item.isEmpty()) {
            return;
        }

        if (source < 0 && !creative) {
            return;
        }

        if (source >= 0 && inventory.getItem(source).isEmpty()) {
            return;
        }

        if (source == target) {
            inventory.setSelectedSlot(target);
            return;
        }

        ItemStack previous = inventory.getItem(target);
        if (source >= 0) {
            inventory.setItem(target, inventory.getItem(source));
            inventory.setItem(source, previous);
        } else {
            if (!previous.isEmpty()) {
                int freeSlot = inventory.firstEmpty();
                if (freeSlot >= 0) {
                    inventory.setItem(freeSlot, previous);
                }
            }

            inventory.setItem(target, item);
        }

        inventory.setSelectedSlot(target);
    }

    private static int suitableHotbarSlot(PlayerInventoryView inventory) {
        int selected = inventory.getSelectedSlot();
        int size = inventory.getHotbarSize();

        for (int offset = 0; offset < size; offset++) {
            int slot = (selected + offset) % size;
            if (inventory.getItem(slot).isEmpty()) {
                return slot;
            }
        }

        for (int offset = 0; offset < size; offset++) {
            int slot = (selected + offset) % size;
            if (inventory.getItem(slot).getOrDefault(ItemDataComponents.ENCHANTMENTS, Map.of()).isEmpty()) {
                return slot;
            }
        }

        return selected;
    }
}
