package org.cloudburstmc.server.inventory;

import org.cloudburstmc.api.inventory.CraftingGrid;
import org.cloudburstmc.api.inventory.InventoryType;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.ArrayList;
import java.util.List;

public class CloudCraftingGrid extends BaseInventory implements CraftingGrid {
    public static final int CRAFTING_GRID_SMALL_OFFSET = 28;
    public static final int CRAFTING_GRID_LARGE_OFFSET = 32;
    public static final int CRAFTING_RESULT_OFFSET = 40;


    private CraftingGrid.Type gridType = Type.CRAFTING_GRID_SMALL;
    private static final int CRAFTING_RESULT_SLOT = 10;


    public CloudCraftingGrid(CloudPlayer holder) {
        super(holder, InventoryType.WORKBENCH); // 10 slots, can reuse 0-3 for small grid, slot 10 is result always
    }

    @Override
    public CloudPlayer getHolder() {
        return (CloudPlayer) super.getHolder();
    }

    @Override
    public Type getCraftingGridType() {
        return this.gridType;
    }

    public void setCraftingGridType(CraftingGrid.Type type) {
        this.gridType = type;
    }

    @Override
    public ItemStack getCraftingResult() {
        return super.getItem(CRAFTING_RESULT_SLOT);
    }

    @Override
    public boolean setItem(int index, ItemStack item, boolean send) {
        if (index <= 10) {
            return super.setItem(index, item, send);
        }
        if (index == 50) {
            return super.setItem(index - CRAFTING_RESULT_OFFSET, item, false);
        } else if (this.getCraftingGridType() == Type.CRAFTING_GRID_SMALL) {
            return super.setItem(index - CRAFTING_GRID_SMALL_OFFSET, item, false);
        } else {
            return super.setItem(index - CRAFTING_GRID_LARGE_OFFSET, item, false);
        }
    }

    @Override
    public void decrementCount(int slot) {
        if (slot <= 10) {
            super.decrementCount(slot);
        }
        if (slot == 50) {
            super.decrementCount(slot - CRAFTING_RESULT_OFFSET);
        } else if (this.getCraftingGridType() == Type.CRAFTING_GRID_SMALL) {
            super.decrementCount(slot - CRAFTING_GRID_SMALL_OFFSET);
        } else {
            super.decrementCount(slot - CRAFTING_GRID_LARGE_OFFSET);
        }
    }

    @Override
    public void incrementCount(int slot) {
        if (slot <= 10) {
            super.incrementCount(slot);
        }
        if (slot == 50) {
            super.incrementCount(slot - CRAFTING_RESULT_OFFSET);
        } else if (this.getCraftingGridType() == Type.CRAFTING_GRID_SMALL) {
            super.incrementCount(slot - CRAFTING_GRID_SMALL_OFFSET);
        } else {
            super.incrementCount(slot - CRAFTING_GRID_LARGE_OFFSET);
        }
    }

    @Override
    public void resetCraftingGrid() {
        List<ItemStack> drops = new ArrayList<>();
        if (!isEmpty()) {
            for (ItemStack item : this.getContents().values()) {
                if (!item.isNull()) {
                    drops.add(item);
                }
            }
        }
        if (drops.size() > 0) {
            for (ItemStack item : drops) {
                getHolder().dropItem(item); // TODO - should check base player inventory for space first?
            }
        }
        this.clearAll();
        this.setCraftingGridType(Type.CRAFTING_GRID_SMALL);
    }
}
