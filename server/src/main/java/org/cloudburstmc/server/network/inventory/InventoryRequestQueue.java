package org.cloudburstmc.server.network.inventory;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequest;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequestSlotData;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.*;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponse;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponseStatus;
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.LegacySetItemSlotData;
import org.cloudburstmc.protocol.bedrock.packet.ItemStackRequestPacket;
import org.cloudburstmc.protocol.bedrock.packet.ItemStackResponsePacket;
import org.cloudburstmc.server.container.Container;
import org.cloudburstmc.server.container.screen.CloudEnchantingContainerScreen;
import org.cloudburstmc.server.container.screen.CloudInventoryScreen;
import org.cloudburstmc.server.container.view.CloudSlotGroupBase;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.*;

/**
 * Queues inventory requests and sends their acknowledgements before deferred slot updates.
 */
@Log4j2
public class InventoryRequestQueue {

    private final CloudPlayer player;
    private final InventoryResponses acknowledgements;

    private final Queue<ItemStackRequest> requests = new ArrayDeque<>();
    private final Deque<ArmorUseAcknowledgement> pendingArmorUses = new ArrayDeque<>();
    private final LinkedHashSet<InventorySlotUpdate> slotsToResend = new LinkedHashSet<>();

    @Getter
    private boolean processingRequests;

    public InventoryRequestQueue(CloudPlayer player) {
        this.player = player;
        this.acknowledgements = new InventoryResponses(player);
    }

    public void handlePacket(ItemStackRequestPacket packet) {
        for (ItemStackRequest request : packet.getRequests()) {
            this.requests.offer(request);
        }
        this.processQueue();
    }

    public void handleSingleRequest(ItemStackRequest request) {
        this.requests.offer(request);
        this.processQueue();
    }

    public void resendSlotAfterResponse(CloudInventoryScreen screen, ContainerSlotType container, int slot) {
        if (screen.resolveSlotGroup(container) instanceof CloudSlotGroupBase group) {
            int inventorySlot = screen.resolveInventorySlot(container, slot);
            this.slotsToResend.addLast(new InventorySlotUpdate(group.getContainer(), group.getContainerSlot(inventorySlot)));
        }
    }

    public boolean deferInventorySlotUpdate(Container inventory, int slot) {
        if (!this.processingRequests) {
            return false;
        }

        this.slotsToResend.add(new InventorySlotUpdate(inventory, slot));
        return true;
    }

    public boolean deferInventoryContentsUpdate(Container inventory) {
        if (!this.processingRequests) {
            return false;
        }

        for (int slot = 0; slot < inventory.size(); slot++) {
            this.slotsToResend.add(new InventorySlotUpdate(inventory, slot));
        }

        return true;
    }

    public void acknowledgeLegacyTransaction(int requestId, List<LegacySetItemSlotData> legacySlots) {
        if (requestId == 0 || legacySlots.isEmpty()) {
            return;
        }

        ItemStackResponse response = this.acknowledgements.acknowledgeLegacyCurrentState(requestId, legacySlots);
        ItemStackResponsePacket packet = new ItemStackResponsePacket();
        packet.getEntries().add(response);
        this.player.sendPacket(packet);
    }

    public void recordServerAuthoritativeArmorUse(int armorSlot) {
        this.pendingArmorUses.addLast(ArmorUseAcknowledgement.forSlot(armorSlot));
        while (this.pendingArmorUses.size() > 8) {
            this.pendingArmorUses.removeFirst();
        }
    }

    private void processQueue() {
        if (this.processingRequests) {
            return;
        }

        List<ItemStackResponse> responses = new ArrayList<>();
        this.processingRequests = true;
        try {
            ItemStackRequest request;
            while ((request = this.requests.poll()) != null) {
                this.handleRequest(request, responses);
            }

            if (!responses.isEmpty()) {
                ItemStackResponsePacket packet = new ItemStackResponsePacket();
                packet.getEntries().addAll(responses);
                this.player.sendPacket(packet);
            }
        } finally {
            this.processingRequests = false;
        }

        List<InventorySlotUpdate> updates = List.copyOf(this.slotsToResend);
        this.slotsToResend.clear();
        for (InventorySlotUpdate update : updates) {
            this.player.onInventorySlotChange(update.container(), update.slot());
        }

        if (this.player.getInventoryManager().getScreen() instanceof CloudEnchantingContainerScreen enchanting) {
            enchanting.sendOffers();
        }
    }

    private void handleRequest(ItemStackRequest request, List<ItemStackResponse> responses) {
        if (!NetIds.isValid(request.getRequestId())) {
            return;
        }

        CloudInventoryScreen screen = this.player.getInventoryManager().getScreen();
        try {
            ItemStackResponse acknowledgement = this.acknowledgeArmorUse(request, screen);
            responses.add(acknowledgement != null ? acknowledgement : new InventoryRequest(this.player, screen, request).execute());
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            log.debug("Rejected inventory request {} from {}", request.getRequestId(), this.player.getName(), e);
            responses.add(new ItemStackResponse(ItemStackResponseStatus.ERROR, request.getRequestId(), List.of()));
        }
    }

    private ItemStackResponse acknowledgeArmorUse(ItemStackRequest request, CloudInventoryScreen screen) {
        while (!this.pendingArmorUses.isEmpty() && this.pendingArmorUses.getFirst().isExpired()) {
            this.pendingArmorUses.removeFirst();
        }

        Iterator<ArmorUseAcknowledgement> iterator = this.pendingArmorUses.iterator();
        while (iterator.hasNext()) {
            ArmorUseAcknowledgement update = iterator.next();
            if (update.matches(request)) {
                iterator.remove();
                return this.acknowledgements.acknowledgeCurrentState(request, screen);
            }
        }

        return null;
    }

    private record InventorySlotUpdate(Container container, int slot) {
    }

    private record ArmorUseAcknowledgement(int armorSlot, long deadline) {
        private static final long RESPONSE_WINDOW_NANOS = 2_000_000_000L;

        private static ArmorUseAcknowledgement forSlot(int armorSlot) {
            return new ArmorUseAcknowledgement(armorSlot, System.nanoTime() + RESPONSE_WINDOW_NANOS);
        }

        private boolean isExpired() {
            return System.nanoTime() - this.deadline >= 0;
        }

        private boolean matches(ItemStackRequest request) {
            for (ItemStackRequestAction action : request.getActions()) {
                if (matches(action)) {
                    return true;
                }
            }
            return false;
        }

        private boolean matches(ItemStackRequestAction action) {
            return switch (action) {
                case TransferItemStackRequestAction transfer -> matches(transfer.getSource()) || matches(transfer.getDestination());
                case SwapAction swap -> matches(swap.getSource()) || matches(swap.getDestination());
                case DropAction drop -> matches(drop.getSource());
                case DestroyAction destroy -> matches(destroy.getSource());
                case ConsumeAction consume -> matches(consume.getSource());
                default -> false;
            };
        }

        private boolean matches(ItemStackRequestSlotData slotData) {
            return slotData.getSlot() == this.armorSlot && slotData.getContainerName() != null
                    && slotData.getContainerName().getContainer() == ContainerSlotType.ARMOR;
        }
    }
}
