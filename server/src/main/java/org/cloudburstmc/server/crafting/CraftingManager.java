package org.cloudburstmc.server.crafting;

import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudRecipeRegistry;

import javax.inject.Singleton;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
@Log4j2
@Singleton
public class CraftingManager {

    private CloudRecipeRegistry registry = CloudRecipeRegistry.get();

    public void sendRecipesTo(CloudPlayer player) {
        player.sendPacket(registry.getNetworkData());
    }

    private ItemStack[][] cloneItemMap(ItemStack[][] map) {
        ItemStack[][] newMap = new ItemStack[map.length][];
        for (int i = 0; i < newMap.length; i++) {
            ItemStack[] old = map[i];
            ItemStack[] n = new ItemStack[old.length];

            System.arraycopy(old, 0, n, 0, n.length);
            newMap[i] = n;
        }

        for (ItemStack[] row : newMap) {
            System.arraycopy(row, 0, row, 0, row.length);
        }
        return newMap;
    }

}
