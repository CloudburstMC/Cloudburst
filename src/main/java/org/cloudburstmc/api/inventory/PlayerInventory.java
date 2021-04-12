package org.cloudburstmc.api.inventory;

import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;

import javax.annotation.Nonnull;
import java.util.List;

public interface PlayerInventory extends CreatureInventory {

    @Override
    Player getHolder();

    void setHeldItemIndex(int index, boolean send);

    void sendCreativeContents();

    @Nonnull
    CraftingGrid getCraftingGrid();

    @Nonnull
    List<? extends ItemStack> getHotbar();

    @Nonnull
    ItemStack getHotbarSlot(int slot);

    boolean setHotbarSlot(int slot, @Nonnull ItemStack item);

    @Nonnull
    ItemStack getCursorItem();

    boolean setCursorItem(@Nonnull ItemStack item);

    void clearCursor();
}
