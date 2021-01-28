package org.cloudburstmc.api.inventory;

import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Identifier;

public interface Recipe {
    Identifier getId();

    ItemStack getResult();

    RecipeType getType();

    Identifier getBlock();
}
