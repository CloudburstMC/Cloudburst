package org.cloudburstmc.server.inventory;

import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Identifier;

import javax.annotation.concurrent.Immutable;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
@Immutable
public interface Recipe {

    Identifier getId();

    ItemStack getResult();

    RecipeType getType();

    Identifier getBlock();
}
