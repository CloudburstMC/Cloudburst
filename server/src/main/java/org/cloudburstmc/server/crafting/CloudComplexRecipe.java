package org.cloudburstmc.server.crafting;

import org.cloudburstmc.api.block.BlockIds;
import org.cloudburstmc.api.crafting.ComplexRecipe;
import org.cloudburstmc.api.crafting.RecipeType;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Identifier;

import java.util.UUID;

public final class CloudComplexRecipe implements ComplexRecipe {

    private final Identifier id;
    private final UUID uuid;

    public CloudComplexRecipe(Identifier id, UUID uuid) {
        this.id = id;
        this.uuid = uuid;
    }

    public UUID uuid() {
        return uuid;
    }

    @Override
    public Identifier getId() {
        return id;
    }

    @Override
    public ItemStack getResult() {
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeType getType() {
        return RecipeType.COMPLEX;
    }

    @Override
    public Identifier getBlock() {
        return BlockIds.AIR;
    }
}
