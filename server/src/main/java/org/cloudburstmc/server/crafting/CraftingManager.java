package org.cloudburstmc.server.crafting;

import com.google.inject.Singleton;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudRecipeRegistry;

@Log4j2
@Singleton
public class CraftingManager {

    public void sendRecipesTo(CloudPlayer player) {
        player.sendPacket(CloudRecipeRegistry.get().getTrimData());
        player.sendPacket(CloudRecipeRegistry.get().getNetworkData());
        player.sendPacket(CloudRecipeRegistry.get().getUnlockedRecipesPacket());
    }

    private ItemStack[][] cloneItemMap(ItemStack[][] map) {
        ItemStack[][] newMap = new ItemStack[map.length][];
        for (int i = 0; i < newMap.length; i++) {
            ItemStack[] old = map[i];
            ItemStack[] n = new ItemStack[old.length];
            System.arraycopy(old, 0, n, 0, n.length);
            newMap[i] = n;
        }
        return newMap;
    }

}
