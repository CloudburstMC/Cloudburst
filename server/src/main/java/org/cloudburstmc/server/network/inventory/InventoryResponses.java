package org.cloudburstmc.server.network.inventory;

import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerId;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.protocol.bedrock.data.inventory.FullContainerName;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequest;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequestSlotData;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.*;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponse;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponseContainer;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponseSlot;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponseStatus;
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.LegacySetItemSlotData;
import org.cloudburstmc.server.container.screen.CloudInventoryScreen;
import org.cloudburstmc.server.container.screen.ResultContainerScreen;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.cloudburstmc.server.network.inventory.InventoryRequest.container;

/**
 * Builds acknowledgements from server inventory state.
 */
@Log4j2
public class InventoryResponses {

    private static final int LEGACY_ARMOR_CONTAINER_ID = 6;
    private static final int BEDROCK_HOTBAR_START_SLOT = 36;
    private static final int BEDROCK_HOTBAR_END_SLOT = 44;
    private static final int HOTBAR_SIZE = 9;

    private final CloudPlayer player;

    public InventoryResponses(CloudPlayer player) {
        this.player = player;
    }

    public ItemStackResponse acknowledgeCurrentState(ItemStackRequest request, CloudInventoryScreen screen) {
        Map<ContainerSlotType, Map<Integer, ItemStack>> slots = new LinkedHashMap<>();

        for (ItemStackRequestAction action : request.getActions()) {
            trackCurrentStateSlots(slots, screen, action);
        }

        slots.computeIfAbsent(ContainerSlotType.CURSOR, key -> new LinkedHashMap<>()).put(0, ItemStack.EMPTY);

        return okResponse(request.getRequestId(), slots);
    }

    public ItemStackResponse acknowledgeLegacyCurrentState(int requestId, List<LegacySetItemSlotData> legacySlots) {
        Map<ContainerSlotType, Map<Integer, ItemStack>> slots = new LinkedHashMap<>();

        for (LegacySetItemSlotData legacySlot : legacySlots) {
            for (byte rawSlot : legacySlot.getSlots()) {
                trackLegacyCurrentStateSlot(slots, legacySlot.getContainerId(), Byte.toUnsignedInt(rawSlot));
            }
        }

        slots.computeIfAbsent(ContainerSlotType.CURSOR, key -> new LinkedHashMap<>()).put(0, ItemStack.EMPTY);

        return okResponse(requestId, slots);
    }

    public static void putCurrentStateSlot(Map<ContainerSlotType, Map<Integer, ItemStack>> slots, ContainerSlotType containerType, int slot, ItemStack item) {
        slots.computeIfAbsent(containerType, key -> new LinkedHashMap<>()).put(slot, item);
    }

    public static ItemStackResponse okResponse(int requestId, Map<ContainerSlotType, Map<Integer, ItemStack>> slots) {
        List<ItemStackResponseContainer> containers = new ArrayList<>();
        for (Map.Entry<ContainerSlotType, Map<Integer, ItemStack>> entry : slots.entrySet()) {
            ContainerSlotType containerType = entry.getKey();

            List<ItemStackResponseSlot> responseSlots = new ArrayList<>();
            for (Map.Entry<Integer, ItemStack> slotEntry : entry.getValue().entrySet()) {
                responseSlots.add(makeResponseSlot(slotEntry.getKey(), slotEntry.getValue()));
            }

            containers.add(new ItemStackResponseContainer(
                    containerType,
                    responseSlots,
                    new FullContainerName(containerType, null)
            ));
        }

        return new ItemStackResponse(ItemStackResponseStatus.OK, requestId, containers);
    }

    private static ItemStackResponseSlot makeResponseSlot(int slot, ItemStack item) {
        if (item.isEmpty()) {
            return new ItemStackResponseSlot(slot, slot, 0, 0, "", 0, "");
        }

        int netId = ItemStackNetworkIds.get(item);
        String customName = item.get(ItemDataComponents.CUSTOM_NAME);

        return new ItemStackResponseSlot(
                slot,
                slot,
                item.getCount(),
                netId,
                customName == null ? "" : customName,
                item.getDamage(),
                ""
        );
    }

    private void trackCurrentStateSlots(Map<ContainerSlotType, Map<Integer, ItemStack>> slots, CloudInventoryScreen screen, ItemStackRequestAction action) {
        switch (action) {
            case TransferItemStackRequestAction transfer -> {
                trackCurrentStateSlot(slots, screen, transfer.getSource());
                trackCurrentStateSlot(slots, screen, transfer.getDestination());
            }
            case SwapAction swap -> {
                trackCurrentStateSlot(slots, screen, swap.getSource());
                trackCurrentStateSlot(slots, screen, swap.getDestination());
            }
            case DropAction drop -> trackCurrentStateSlot(slots, screen, drop.getSource());
            case DestroyAction destroy -> trackCurrentStateSlot(slots, screen, destroy.getSource());
            case ConsumeAction consume -> trackCurrentStateSlot(slots, screen, consume.getSource());
            case CraftRecipeOptionalAction ignored when screen instanceof ResultContainerScreen resultScreen ->
                    resultScreen.trackResultSlots((type, slot) -> putCurrentStateSlot(slots, type, slot, screen.getSlot(type, slot)));
            default -> {
            }
        }
    }

    private void trackCurrentStateSlot(Map<ContainerSlotType, Map<Integer, ItemStack>> slots, CloudInventoryScreen screen, ItemStackRequestSlotData slotData) {
        ContainerSlotType containerType = container(slotData);
        int slot = slotData.getSlot();

        if (containerType == ContainerSlotType.ARMOR) {
            putCurrentStateSlot(slots, ContainerSlotType.ARMOR, slot, this.player.getArmor().getItem(slot));
            return;
        }

        if (containerType == ContainerSlotType.CURSOR) {
            putCurrentStateSlot(slots, ContainerSlotType.CURSOR, 0, ItemStack.EMPTY);
            return;
        }

        if (isInventoryContainer(containerType)) {
            int inventorySlot = normalizeInventorySlot(slot);
            if (inventorySlot < 0 || inventorySlot >= this.player.getInventory().size()) {
                log.debug("Ignoring out-of-bounds player inventory slot {} while acknowledging request for {}", slot, player.getName());
                return;
            }

            ContainerSlotType responseContainer = responseContainerForInventorySlot(inventorySlot);
            putCurrentStateSlot(slots, responseContainer, inventorySlot, this.player.getInventory().getItem(inventorySlot));
            return;
        }

        putCurrentStateSlot(slots, containerType, slot, screen.getSlot(containerType, slot));
    }

    private void trackLegacyCurrentStateSlot(Map<ContainerSlotType, Map<Integer, ItemStack>> slots, int containerId, int slot) {
        if (containerId == ContainerId.INVENTORY) {
            trackLegacyPlayerInventorySlot(slots, slot);
            return;
        }

        if (containerId == ContainerId.ARMOR || containerId == LEGACY_ARMOR_CONTAINER_ID) {
            trackLegacyArmorSlot(slots, slot);
            return;
        }

        if (containerId == ContainerId.OFFHAND) {
            putCurrentStateSlot(slots, ContainerSlotType.OFFHAND, slot, this.player.getOffhand().getOffhandItem());
            return;
        }

        ContainerSlotType containerType = containerSlotTypeByOrdinal(containerId);
        if (isInventoryContainer(containerType)) {
            trackLegacyProtocolInventorySlot(slots, containerType, slot);
            return;
        }

        if (containerType == ContainerSlotType.ARMOR) {
            trackLegacyArmorSlot(slots, slot);
            return;
        }

        if (containerType == ContainerSlotType.OFFHAND) {
            putCurrentStateSlot(slots, ContainerSlotType.OFFHAND, slot, this.player.getOffhand().getOffhandItem());
        }
    }

    private void trackLegacyPlayerInventorySlot(Map<ContainerSlotType, Map<Integer, ItemStack>> slots, int slot) {
        int inventorySlot = normalizeInventorySlot(slot);
        if (inventorySlot < 0 || inventorySlot >= this.player.getInventory().size()) {
            return;
        }

        ContainerSlotType responseContainer = responseContainerForInventorySlot(inventorySlot);
        putCurrentStateSlot(slots, responseContainer, inventorySlot, this.player.getInventory().getItem(inventorySlot));
    }

    private void trackLegacyProtocolInventorySlot(Map<ContainerSlotType, Map<Integer, ItemStack>> slots, ContainerSlotType containerType, int slot) {
        int inventorySlot = normalizeInventorySlot(slot);
        if (inventorySlot < 0 || inventorySlot >= this.player.getInventory().size()) {
            return;
        }

        putCurrentStateSlot(slots, containerType, slot, this.player.getInventory().getItem(inventorySlot));
    }

    private void trackLegacyArmorSlot(Map<ContainerSlotType, Map<Integer, ItemStack>> slots, int slot) {
        if (slot < 0 || slot >= 4) {
            return;
        }

        putCurrentStateSlot(slots, ContainerSlotType.ARMOR, slot, this.player.getArmor().getItem(slot));
    }

    private ContainerSlotType containerSlotTypeByOrdinal(int containerId) {
        ContainerSlotType[] containerTypes = ContainerSlotType.values();
        if (containerId < 0 || containerId >= containerTypes.length) {
            return ContainerSlotType.UNKNOWN;
        }
        return containerTypes[containerId];
    }

    private boolean isInventoryContainer(ContainerSlotType containerType) {
        return containerType == ContainerSlotType.HOTBAR
                || containerType == ContainerSlotType.HOTBAR_AND_INVENTORY
                || containerType == ContainerSlotType.INVENTORY;
    }

    private ContainerSlotType responseContainerForInventorySlot(int inventorySlot) {
        return inventorySlot < HOTBAR_SIZE ? ContainerSlotType.HOTBAR : ContainerSlotType.INVENTORY;
    }

    private int normalizeInventorySlot(int slot) {
        if (slot >= BEDROCK_HOTBAR_START_SLOT && slot <= BEDROCK_HOTBAR_END_SLOT) {
            return slot - BEDROCK_HOTBAR_START_SLOT;
        }
        return slot;
    }
}
