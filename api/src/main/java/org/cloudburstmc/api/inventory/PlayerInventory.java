package org.cloudburstmc.api.inventory;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;

import java.util.List;

public interface PlayerInventory extends CreatureInventory {

    @Override
    Player getHolder();

    void setHeldItemIndex(int index, boolean send);

    void sendCreativeContents();

    @NonNull
    CraftingGrid getCraftingGrid();

    @NonNull
    List<? extends ItemStack> getHotbar();

    @NonNull
    ItemStack getHotbarSlot(int slot);

    boolean setHotbarSlot(int slot, @NonNull ItemStack item);

    @NonNull
    ItemStack getCursorItem();

    boolean setCursorItem(@NonNull ItemStack item);

    void clearCursor();
}
