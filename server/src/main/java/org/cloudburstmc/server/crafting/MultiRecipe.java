package org.cloudburstmc.server.crafting;

import lombok.Getter;
import org.cloudburstmc.api.block.BlockIds;
import org.cloudburstmc.api.crafting.RecipeType;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import java.util.List;
import java.util.UUID;

public class MultiRecipe implements CraftingRecipe {
    @Getter
    private final UUID uuid;
    private Identifier id;

    public MultiRecipe(Identifier id, UUID uuid) {
        this.id = id;
        this.uuid = uuid;
    }

    @Override
    public Identifier getId() {
        return id;
    }

    @Override
    public ItemStack getResult() {
        return CloudItemRegistry.get().AIR;
    }

    @Override
    public RecipeType getType() {
        return RecipeType.MULTI;
    }

    @Override
    public Identifier getBlock() {
        return BlockIds.AIR;
    }

    @Override
    public boolean requiresCraftingTable() {
        return false;
    }

    @Override
    public List<? extends ItemStack> getExtraResults() {
        return null;
    }

    @Override
    public List<? extends ItemStack> getAllResults() {
        return null;
    }

    @Override
    public int getPriority() {
        return -1;
    }

    @Override
    public boolean matchItems(ItemStack[][] input, ItemStack[][] output) {
        return true;
    }
}
