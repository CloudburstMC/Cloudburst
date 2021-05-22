package org.cloudburstmc.server.player.manager;

import com.nukkitx.protocol.bedrock.data.inventory.ContainerSlotType;
import com.nukkitx.protocol.bedrock.data.inventory.ItemStackRequest;
import com.nukkitx.protocol.bedrock.data.inventory.StackRequestSlotInfoData;
import com.nukkitx.protocol.bedrock.data.inventory.stackrequestactions.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.server.inventory.*;
import org.cloudburstmc.server.inventory.transaction.CraftingTransaction;
import org.cloudburstmc.server.inventory.transaction.InventoryTransaction;
import org.cloudburstmc.server.inventory.transaction.ItemStackTransaction;
import org.cloudburstmc.server.inventory.transaction.action.DropItemStackAction;
import org.cloudburstmc.server.inventory.transaction.action.PlaceItemStackAction;
import org.cloudburstmc.server.inventory.transaction.action.SwapItemStackAction;
import org.cloudburstmc.server.inventory.transaction.action.TakeItemStackAction;
import org.cloudburstmc.server.item.CloudItemStack;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import java.util.Arrays;

import static com.nukkitx.protocol.bedrock.data.inventory.stackrequestactions.StackRequestActionType.*;

@Log4j2
@Getter
public class PlayerInventoryManager {
    private final CloudPlayer player;
    private final CloudPlayerInventory mainInv;
    private final PlayerCursorInventory cursor;
    private final CloudEnderChestInventory enderChest;
    private final CloudCraftingGrid craftingGrid;
    @Setter
    private InventoryTransaction transaction;
    @Setter
    private BlockEntity viewingBlock;

    public PlayerInventoryManager(CloudPlayer player) {
        this.player = player;
        this.mainInv = new CloudPlayerInventory(player);
        this.cursor = new PlayerCursorInventory(player);
        this.enderChest = new CloudEnderChestInventory(player);
        this.craftingGrid = new CloudCraftingGrid(player);
        transaction = null;
        viewingBlock = null;
    }


    public void handle(ItemStackRequest request) {
        if (isCraftingRequest(request)) {
            if (this.transaction == null || !(this.transaction instanceof CraftingTransaction)) {
                log.warn("Received crafting item stack request when crafting transaction is null!");
                return;// new ItemStackResponsePacket.Response(ItemStackResponsePacket.ResponseStatus.ERROR, request.getRequestId(), containers);
            } else {
                // TODO - create crafting transaction with CraftEventPacket and Execute the crafting request here
                return;// new ItemStackResponsePacket.Response(ItemStackResponsePacket.ResponseStatus.OK, request.getRequestId(), containers);
            }
        }

        if (this.transaction == null) {
            this.transaction = new ItemStackTransaction(player);
        }

        for (StackRequestActionData data : request.getActions()) {
            StackRequestSlotInfoData source;
            StackRequestSlotInfoData target;
            switch (data.getType()) {
                case TAKE:
                    source = ((TakeStackRequestActionData) data).getSource();
                    target = ((TakeStackRequestActionData) data).getDestination();

                    this.transaction.addAction(new TakeItemStackAction(
                            request.getRequestId(),
                            CloudItemRegistry.get().getItemByNetId(source.getStackNetworkId()),
                            source.getSlot(),
                            CloudItemRegistry.get().getItemByNetId(target.getStackNetworkId()),
                            target.getSlot()
                    ));
                    this.transaction.addInventory(getInventoryByType(source.getContainer()));
                    this.transaction.addInventory(getInventoryByType(target.getContainer()));
                    continue;
                case PLACE:
                    source = ((PlaceStackRequestActionData) data).getSource();
                    target = ((PlaceStackRequestActionData) data).getDestination();

                    this.transaction.addAction(new PlaceItemStackAction(
                            request.getRequestId(),
                            CloudItemRegistry.get().getItemByNetId(source.getStackNetworkId()),
                            source.getSlot(),
                            CloudItemRegistry.get().getItemByNetId(target.getStackNetworkId()),
                            target.getSlot()
                    ));
                    this.transaction.addInventory(getInventoryByType(source.getContainer()));
                    this.transaction.addInventory(getInventoryByType(target.getContainer()));
                    continue;
                case SWAP:
                    source = ((SwapStackRequestActionData) data).getSource();
                    target = ((SwapStackRequestActionData) data).getDestination();

                    this.transaction.addAction(new SwapItemStackAction(
                            request.getRequestId(),
                            CloudItemRegistry.get().getItemByNetId(source.getStackNetworkId()),
                            source.getSlot(),
                            CloudItemRegistry.get().getItemByNetId(target.getStackNetworkId()),
                            target.getSlot()
                    ));
                    this.transaction.addInventory(getInventoryByType(source.getContainer()));
                    this.transaction.addInventory(getInventoryByType(target.getContainer()));
                    continue;
                case DROP:
                    source = ((DropStackRequestActionData) data).getSource();

                    this.transaction.addAction(new DropItemStackAction(
                            request.getRequestId(),
                            CloudItemRegistry.get().getItemByNetId(source.getStackNetworkId()),
                            source.getSlot(),
                            CloudItemRegistry.get().AIR,
                            -1
                    ));

                    this.transaction.addInventory(getInventoryByType(source.getContainer()));
                    continue;
                case DESTROY:
                case CONSUME:
                    break;
                case CREATE:
                    break;
                case BEACON_PAYMENT:
                    break;
                case MINE_BLOCK:
                    break;

            }


        }
    }

    private boolean isCraftingRequest(ItemStackRequest request) {
        return Arrays.stream(request.getActions()).anyMatch(req -> (!player.isCreative() && (req.getType() == CRAFT_CREATIVE
                || req.getType() == CRAFT_NON_IMPLEMENTED_DEPRECATED
                || req.getType() == CRAFT_RECIPE
                || req.getType() == CRAFT_RECIPE_AUTO
                || req.getType() == CRAFT_RECIPE_OPTIONAL
                || req.getType() == CRAFT_RESULTS_DEPRECATED))
        );
    }

    private boolean checkItem(CloudItemStack item, CloudItemStack netItem) {
        return item.getStackNetworkId() == netItem.getStackNetworkId() && item.equals(netItem, true, true);
    }

    private BaseInventory getInventoryByType(ContainerSlotType type) {
        return switch (type) {
            case HOTBAR, HOTBAR_AND_INVENTORY, INVENTORY, OFFHAND -> mainInv;
            case CRAFTING_INPUT, CRAFTING_OUTPUT, CREATIVE_OUTPUT -> craftingGrid;
            case CURSOR -> cursor;
            default -> null;
        };
    }
}
