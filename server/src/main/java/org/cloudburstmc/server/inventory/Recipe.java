package org.cloudburstmc.server.inventory;

import com.nukkitx.protocol.bedrock.data.inventory.CraftingData;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.utils.Identifier;

import javax.annotation.concurrent.Immutable;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
@Immutable
public interface Recipe {

    Item getResult();

    void registerToCraftingManager(CraftingManager manager);

    RecipeType getType();

    CraftingData toNetwork();

    Identifier getBlock();
}
