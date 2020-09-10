package org.cloudburstmc.server.inventory;

import com.nukkitx.protocol.bedrock.data.inventory.CraftingData;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.utils.Identifier;

import javax.annotation.concurrent.Immutable;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
@Immutable
public interface Recipe {

    ItemStack getResult();

    void registerToCraftingManager(CraftingManager manager);

    RecipeType getType();

    CraftingData toNetwork(int networkId);

    Identifier getBlock();
}
