package org.cloudburstmc.server.player.manager;

import com.google.common.collect.Lists;
import com.nukkitx.protocol.bedrock.data.inventory.ContainerSlotType;
import com.nukkitx.protocol.bedrock.data.inventory.ItemStackRequest;
import com.nukkitx.protocol.bedrock.data.inventory.StackRequestSlotInfoData;
import com.nukkitx.protocol.bedrock.data.inventory.stackrequestactions.*;
import com.nukkitx.protocol.bedrock.packet.ItemStackResponsePacket;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.server.inventory.*;
import org.cloudburstmc.server.inventory.transaction.CraftingTransaction;
import org.cloudburstmc.server.item.CloudItemStack;
import org.cloudburstmc.server.network.NetworkUtils;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    private CraftingTransaction transaction;
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


    public ItemStackResponsePacket.Response handle(ItemStackRequest request) {
        ItemStackResponsePacket.ResponseStatus result = ItemStackResponsePacket.ResponseStatus.OK;

        List<ItemStackResponsePacket.ContainerEntry> containers = new ArrayList<>();

        if (isCraftingRequest(request)) {
            if (this.transaction == null) {
                log.warn("Received crafting item stack request when crafting transaction is null!");
                return new ItemStackResponsePacket.Response(ItemStackResponsePacket.ResponseStatus.ERROR, request.getRequestId(), containers);
            } else {


                // TODO - create crafting transaction with CraftEventPacket and Execute the crafting request here

                return new ItemStackResponsePacket.Response(ItemStackResponsePacket.ResponseStatus.OK, request.getRequestId(), containers);
            }
        }

        for (StackRequestActionData data : request.getActions()) {
            StackRequestSlotInfoData source;
            StackRequestSlotInfoData target;

            BaseInventory sourceInv;
            BaseInventory targetInv;

            switch (data.getType()) {
                case TAKE:
                    source = ((TakeStackRequestActionData) data).getSource();
                    target = ((TakeStackRequestActionData) data).getDestination();
                    sourceInv = getInventoryByType(source.getContainer());
                    targetInv = getInventoryByType(target.getContainer());

                    if (sourceInv == null || targetInv == null) {
                        result = ItemStackResponsePacket.ResponseStatus.ERROR;
                        break;
                    }
                    CloudItemStack sourceItem = CloudItemRegistry.get().getItemByNetId(source.getStackNetworkId());

                    if (!checkItem(sourceInv.getItem(source.getSlot()), sourceItem)) { // What we have doesn't match what client sent
                        result = ItemStackResponsePacket.ResponseStatus.ERROR;
                        containers.add(new ItemStackResponsePacket.ContainerEntry(source.getContainer(), Lists.newArrayList(NetworkUtils.itemStackToNetwork(source, sourceInv))));
                        containers.add(new ItemStackResponsePacket.ContainerEntry(target.getContainer(), Lists.newArrayList(NetworkUtils.itemStackToNetwork(target, targetInv))));
                        break;
                    }

                    if (!targetInv.setItem(target.getSlot(), sourceInv.getItem(source.getSlot()), false)
                            || !sourceInv.clear(source.getSlot(), false)) {
                        result = ItemStackResponsePacket.ResponseStatus.ERROR;
                    }

                    containers.add(new ItemStackResponsePacket.ContainerEntry(source.getContainer(), Lists.newArrayList(NetworkUtils.itemStackToNetwork(source, sourceInv))));
                    containers.add(new ItemStackResponsePacket.ContainerEntry(target.getContainer(), Lists.newArrayList(NetworkUtils.itemStackToNetwork(target, targetInv))));
                    break;
                case PLACE:
                    source = ((PlaceStackRequestActionData) data).getSource();
                    target = ((PlaceStackRequestActionData) data).getDestination();
                    sourceInv = getInventoryByType(source.getContainer());
                    targetInv = getInventoryByType(target.getContainer());

                    if (sourceInv == null || targetInv == null) {
                        result = ItemStackResponsePacket.ResponseStatus.ERROR;
                        break;
                    }

                    sourceItem = CloudItemRegistry.get().getItemByNetId(source.getStackNetworkId());

                    if (!checkItem(sourceInv.getItem(source.getSlot()), sourceItem)) {
                        result = ItemStackResponsePacket.ResponseStatus.ERROR;
                        containers.add(new ItemStackResponsePacket.ContainerEntry(source.getContainer(), Lists.newArrayList(NetworkUtils.itemStackToNetwork(source, sourceInv))));
                        containers.add(new ItemStackResponsePacket.ContainerEntry(target.getContainer(), Lists.newArrayList(NetworkUtils.itemStackToNetwork(target, targetInv))));
                        break;
                    }

                    if (!targetInv.setItem(target.getSlot(), sourceInv.getItem(source.getSlot()), false)
                            || !sourceInv.clear(source.getSlot(), false)) {
                        result = ItemStackResponsePacket.ResponseStatus.ERROR;
                        // no break here so the containers will still be added, but error result is set
                    }
                    //if setItems were successful, our result will still be OK
                    containers.add(new ItemStackResponsePacket.ContainerEntry(source.getContainer(), Lists.newArrayList(NetworkUtils.itemStackToNetwork(source, sourceInv))));
                    containers.add(new ItemStackResponsePacket.ContainerEntry(target.getContainer(), Lists.newArrayList(NetworkUtils.itemStackToNetwork(target, targetInv))));
                    break;
                case SWAP:
                    source = ((SwapStackRequestActionData) data).getSource();
                    target = ((SwapStackRequestActionData) data).getDestination();

                    sourceInv = getInventoryByType(source.getContainer());
                    targetInv = getInventoryByType(target.getContainer());

                    if (sourceInv == null || targetInv == null) {
                        result = ItemStackResponsePacket.ResponseStatus.ERROR;
                        break;
                    }

                    //Check Item
                    if (sourceInv.getItem(source.getSlot()).getNetworkData().getNetId() != source.getStackNetworkId()
                            || targetInv.getItem(target.getSlot()).getNetworkData().getNetId() != target.getStackNetworkId()) {
                        result = ItemStackResponsePacket.ResponseStatus.ERROR;
                        containers.add(new ItemStackResponsePacket.ContainerEntry(source.getContainer(), Lists.newArrayList(NetworkUtils.itemStackToNetwork(source, sourceInv))));
                        containers.add(new ItemStackResponsePacket.ContainerEntry(target.getContainer(), Lists.newArrayList(NetworkUtils.itemStackToNetwork(target, targetInv))));
                        break;
                    }

                    CloudItemStack swap = targetInv.getItem(target.getSlot());
                    if (!targetInv.setItem(source.getSlot(), sourceInv.getItem(source.getSlot()), false)
                            || !sourceInv.setItem(source.getSlot(), swap, false)) {
                        result = ItemStackResponsePacket.ResponseStatus.ERROR;
                    }

                    containers.add(new ItemStackResponsePacket.ContainerEntry(source.getContainer(), Lists.newArrayList(NetworkUtils.itemStackToNetwork(target, sourceInv))));
                    containers.add(new ItemStackResponsePacket.ContainerEntry(target.getContainer(), Lists.newArrayList(NetworkUtils.itemStackToNetwork(source, targetInv))));
                    break;
                case DROP:
                    source = ((DropStackRequestActionData) data).getSource();

                    sourceInv = getInventoryByType(source.getContainer());
                    if (sourceInv == null) {
                        result = ItemStackResponsePacket.ResponseStatus.ERROR;
                        break;
                    }

                    CloudItemStack toDrop = sourceInv.getItem(source.getSlot());
                    if (toDrop.isNull() || toDrop.getNetworkData().getNetId() != source.getStackNetworkId()
                            || player.getLevel().dropItem(player.getPosition(), toDrop).isClosed()) {
                        result = ItemStackResponsePacket.ResponseStatus.ERROR;
                    } else {
                        result = ItemStackResponsePacket.ResponseStatus.OK;
                    }

                    containers.add(new ItemStackResponsePacket.ContainerEntry(source.getContainer(), Lists.newArrayList(NetworkUtils.itemStackToNetwork(source, sourceInv))));
                    break;
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
        return new ItemStackResponsePacket.Response(result, request.getRequestId(), containers);
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
