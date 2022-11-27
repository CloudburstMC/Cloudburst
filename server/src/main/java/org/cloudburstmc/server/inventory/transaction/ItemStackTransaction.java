package org.cloudburstmc.server.inventory.transaction;

import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponseContainer;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponseSlot;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponseStatus;
import org.cloudburstmc.server.inventory.transaction.action.InventoryAction;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Log4j2
public class ItemStackTransaction extends InventoryTransaction {
    @Getter
    @Setter
    private ItemStackResponseStatus responseStatus = ItemStackResponseStatus.OK;
    private final Object2ReferenceMap<ContainerSlotType, List<ItemStackResponseSlot>> containers = new Object2ReferenceOpenHashMap<>();

    public ItemStackTransaction(CloudPlayer source) {
        super(source, new ArrayList<>(), true);
    }

    @Override
    public boolean canExecute() {
        if (this.getActions().isEmpty()) return false;
        for (InventoryAction action : this.getActions()) {
            if (!action.isValid(getSource())) {
                log.debug("Failed validation check on {}", action.getClass().getSimpleName());
                for (InventoryAction action2 : this.getActions()) {
                    action2.onExecuteFail(getSource());
                }
                return false;
            }
        }
        return true;
    }

    @Override
    protected boolean callExecuteEvent() {
        //TODO - call event?
        return true;
    }

    public void addContainers(Collection<ItemStackResponseContainer> containers) {
        for (ItemStackResponseContainer container : containers) {
            List<ItemStackResponseSlot> list = this.containers.computeIfAbsent(container.getContainer(), x -> new ArrayList<>());
            list.addAll(container.getItems());
        }

    }

    public List<ItemStackResponseContainer> getContainerEntries() {
        List<ItemStackResponseContainer> result = new ArrayList<>();
        for (ContainerSlotType container : containers.keySet()) {
            result.add(new ItemStackResponseContainer(container, containers.get(container)));
        }
        return result;
    }
}
