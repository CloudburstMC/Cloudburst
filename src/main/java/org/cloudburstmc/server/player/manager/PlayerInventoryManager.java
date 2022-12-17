package org.cloudburstmc.server.player.manager;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequest;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequestSlotData;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.*;
import org.cloudburstmc.server.inventory.*;
import org.cloudburstmc.server.inventory.transaction.CraftItemStackTransaction;
import org.cloudburstmc.server.inventory.transaction.ItemStackTransaction;
import org.cloudburstmc.server.inventory.transaction.action.CraftCreativeAction;
import org.cloudburstmc.server.inventory.transaction.action.CraftRecipeAction;
import org.cloudburstmc.server.inventory.transaction.action.*;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.Arrays;
import java.util.Map;
import java.util.StringJoiner;

import static org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.ItemStackRequestActionType.*;

@Log4j2
@Getter
public class PlayerInventoryManager {
    private final CloudPlayer player;
    private final CloudPlayerInventory playerInventory;
    private final PlayerCursorInventory cursor;
    private final CloudEnderChestInventory enderChest;
    private final CloudCraftingGrid craftingGrid;
    @Setter
    private ItemStackTransaction transaction;
    @Setter
    private BlockEntity viewingBlock;

    public PlayerInventoryManager(CloudPlayer player) {
        this.player = player;
        this.playerInventory = new CloudPlayerInventory(player);
        this.cursor = new PlayerCursorInventory(player);
        this.enderChest = new CloudEnderChestInventory(player);
        this.craftingGrid = new CloudCraftingGrid(player);
        transaction = null;
        viewingBlock = null;
    }


    public void handle(ItemStackRequest request) {
        if (isCraftingRequest(request)) {
            if (this.transaction == null || !(this.transaction instanceof CraftItemStackTransaction)) {
                log.warn("Received crafting item stack request when crafting transaction is null!");
                return;
            }
        }

        if (this.transaction == null) {
            this.transaction = new ItemStackTransaction(player);
        }

        for (ItemStackRequestAction action : request.getActions()) {
            ItemStackRequestSlotData source;
            ItemStackRequestSlotData target;
            switch (action.getType()) {
                case TAKE:
                    source = ((TakeAction) action).getSource();
                    target = ((TakeAction) action).getDestination();

                    this.transaction.addAction(new MoveItemStackAction(
                            request.getRequestId(),
                            ((TakeAction) action).getCount(),
                            source,
                            target
                    ));
                    continue;
                case PLACE:
                    source = ((PlaceAction) action).getSource();
                    target = ((PlaceAction) action).getDestination();

                    this.transaction.addAction(new MoveItemStackAction(
                            request.getRequestId(),
                            ((PlaceAction) action).getCount(),
                            source,
                            target
                    ));
                    continue;
                case SWAP:
                    source = ((SwapAction) action).getSource();
                    target = ((SwapAction) action).getDestination();

                    this.transaction.addAction(new SwapItemStackAction(
                            request.getRequestId(),
                            source,
                            target
                    ));
                    continue;
                case DROP:
                    source = ((DropAction) action).getSource();

                    this.transaction.addAction(new DropItemStackAction(
                            request.getRequestId(),
                            ((DropAction) action).getCount(),
                            ((DropAction) action).isRandomly(),
                            source,
                            null
                    ));
                    continue;
                case DESTROY:
                    source = ((DestroyAction) action).getSource();

                    this.transaction.addAction(
                            new ConsumeItemAction(
                                    request.getRequestId(),
                                    ((DestroyAction) action).getCount(),
                                    source
                            )
                    );
                    continue;
                case CONSUME:
                    source = ((ConsumeAction) action).getSource();

                    this.transaction.addAction(
                            new ConsumeItemAction(
                                    request.getRequestId(),
                                    ((ConsumeAction) action).getCount(),
                                    source
                            )
                    );
                    continue;
                case CREATE:
                case BEACON_PAYMENT:
                case MINE_BLOCK:
                    continue;
                case CRAFT_RECIPE:
                    this.transaction.addAction(new CraftRecipeAction(request.getRequestId(),
                            ((org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.CraftRecipeAction) action).getRecipeNetworkId()));
                    continue;
                case CRAFT_RESULTS_DEPRECATED:
                    // Don't use depreciated actions!
                    continue;
                case CRAFT_CREATIVE:
                    this.transaction.addAction(new CraftCreativeAction(request.getRequestId(),
                            ((org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.CraftCreativeAction) action).getCreativeItemNetworkId()));
                    continue;
                default:
                    log.warn("Received unknown ItemStackRequestAction type: {}", action.getType());
                    continue;
            }
        }
    }

    private boolean isCraftingRequest(ItemStackRequest request) {
        return Arrays.stream(request.getActions()).anyMatch(req -> (!player.isCreative() && (req.getType() == CRAFT_CREATIVE
                || req.getType() == CRAFT_NON_IMPLEMENTED_DEPRECATED
                || req.getType() == CRAFT_RECIPE
                || req.getType() == CRAFT_RECIPE_AUTO
                || req.getType() == CRAFT_RECIPE_OPTIONAL
                || req.getType() == CRAFT_RESULTS_DEPRECATED)
        ));
    }

    public BaseInventory getInventoryByType(ContainerSlotType type) {
        return switch (type) {
            case HOTBAR, HOTBAR_AND_INVENTORY, INVENTORY, OFFHAND -> playerInventory;
            case CRAFTING_INPUT, CRAFTING_OUTPUT, CREATIVE_OUTPUT -> craftingGrid;
            case CURSOR -> cursor;
            default -> null;
        };
    }

    public CraftItemStackTransaction getCraftingTransaction() {
        if (this.transaction != null && this.transaction instanceof CraftItemStackTransaction)
            return (CraftItemStackTransaction) this.transaction;
        return null;
    }

    @Override
    public String toString() {
        StringJoiner str = new StringJoiner("\n");
        str.add("Player Inventories [" + player.getName() + ":");
        str.add("Main Inventory: ");
        for (Map.Entry<Integer, ItemStack> entry : playerInventory.getContents().entrySet()) {
            str.add("  " + entry.getKey() + ": " + entry.getValue());
        }
        str.add("===============");
        str.add("Crafting Inventory:");
        for (Map.Entry<Integer, ItemStack> entry : craftingGrid.getContents().entrySet()) {
            str.add("  " + entry.getKey() + ": " + entry.getValue());
        }
        str.add("===============");
        str.add("Cursor Inventory:");
        for (Map.Entry<Integer, ItemStack> entry : cursor.getContents().entrySet()) {
            str.add("  " + entry.getKey() + ": " + entry.getValue());
        }
        str.add("===============");
        str.add("EnderChest Inventory:");
        for (Map.Entry<Integer, ItemStack> entry : enderChest.getContents().entrySet()) {
            str.add("  " + entry.getKey() + ": " + entry.getValue());
        }

        return str.toString();
    }
}
