package org.cloudburstmc.server.inventory.transaction.action;

import com.nukkitx.protocol.bedrock.data.inventory.ContainerSlotType;
import com.nukkitx.protocol.bedrock.packet.ItemStackResponsePacket;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.api.crafting.CraftingGrid;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.crafting.CraftingRecipe;
import org.cloudburstmc.server.inventory.CloudCraftingGrid;
import org.cloudburstmc.server.inventory.transaction.CraftItemStackTransaction;
import org.cloudburstmc.server.inventory.transaction.InventoryTransaction;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudItemRegistry;
import org.cloudburstmc.server.registry.CloudRecipeRegistry;

import java.util.ArrayList;
import java.util.List;

@Log4j2
public class CraftRecipeAction extends ItemStackAction {
    private final int recipeId;

    public CraftRecipeAction(int reqId, int recipeId) {
        super(reqId, null, null);
        this.recipeId = recipeId;
    }

    @Override
    public boolean isValid(CloudPlayer player) {
        if (this.getTransaction() == null || !(this.getTransaction() instanceof CraftItemStackTransaction))
            return false;
        CraftItemStackTransaction transaction = ((CraftItemStackTransaction) getTransaction());
        CraftingRecipe recipe = transaction.getRecipe();
        CraftingGrid inv = transaction.getCraftingGrid();

        if (CloudRecipeRegistry.get().getRecipeNetId(recipe.getId()) != this.recipeId) {
            log.warn("Crafting recipe miss-match: {} =/= {}", this.recipeId, CloudRecipeRegistry.get().getRecipeNetId(recipe.getId()));
            return false;
        }

        int size = inv.getCraftingGridSize();

        ItemStack[][] inputs = new ItemStack[size][size];
        ItemStack[][] extraOutputs = new ItemStack[size][size];

        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                int slot = (size * r) + c;
                if (transaction.getExtraOutputs().size() <= slot || transaction.getExtraOutputs().get(slot) == null)
                    transaction.getExtraOutputs().add(slot, CloudItemRegistry.get().AIR);
                extraOutputs[r][c] = transaction.getExtraOutputs().get(slot);
                inputs[r][c] = inv.getItem(slot);
            }
        }

        ItemStack item = recipe.getResult();
        return recipe.matchItems(inputs, extraOutputs) && inv.setItem(CloudCraftingGrid.CRAFTING_RESULT_SLOT, recipe.getResult().withAmount(item.getBehavior().getMaxStackSize(item)));
    }

    @Override
    public boolean execute(CloudPlayer player) {
        return true;
    }

    @Override
    public void onAddToTransaction(InventoryTransaction transaction) {
        super.onAddToTransaction(transaction);
        transaction.addInventory(transaction.getSource().getInventoryManager().getCraftingGrid());
    }

    @Override
    public void onExecuteSuccess(CloudPlayer source) {
        List<ItemStackResponsePacket.ItemEntry> items = new ArrayList<>();
        int size = getTransaction().getSource().getCraftingInventory().getCraftingGridSize();
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                int slot = (size * r) + c;
                items.add(new ItemStackResponsePacket.ItemEntry(
                        (byte) (getTransaction().getSource().getCraftingInventory().getCraftingGridType() == CraftingGrid.Type.CRAFTING_GRID_SMALL ? slot + CloudCraftingGrid.CRAFTING_GRID_SMALL_OFFSET : slot + CloudCraftingGrid.CRAFTING_GRID_LARGE_OFFSET),
                        (byte) 0,
                        (byte) 1,
                        0,
                        "",
                        0

                ));
            }
        }

        this.getTransaction().addResponse(new ItemStackResponsePacket.Response(ItemStackResponsePacket.ResponseStatus.OK, getRequestId(), List.of(new ItemStackResponsePacket.ContainerEntry(ContainerSlotType.CRAFTING_INPUT, items))));
    }
}
