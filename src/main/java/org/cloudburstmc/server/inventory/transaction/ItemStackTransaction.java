package org.cloudburstmc.server.inventory.transaction;

import com.nukkitx.protocol.bedrock.data.inventory.ContainerSlotType;
import com.nukkitx.protocol.bedrock.packet.ItemStackResponsePacket;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.server.inventory.transaction.action.InventoryAction;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Log4j2
public class ItemStackTransaction extends InventoryTransaction {
    @Getter
    @Setter
    private ItemStackResponsePacket.ResponseStatus responseStatus = ItemStackResponsePacket.ResponseStatus.OK;
    private final Object2ReferenceMap<ContainerSlotType, List<ItemStackResponsePacket.ItemEntry>> containers = new Object2ReferenceOpenHashMap<>();

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

    public void addContaiers(Collection<ItemStackResponsePacket.ContainerEntry> containers) {
        for (ItemStackResponsePacket.ContainerEntry entry : containers) {
            List<ItemStackResponsePacket.ItemEntry> list = this.containers.computeIfAbsent(entry.getContainer(), x -> new ArrayList<>());
            list.addAll(entry.getItems());
        }

    }

    public List<ItemStackResponsePacket.ContainerEntry> getContainerEntries() {
        List<ItemStackResponsePacket.ContainerEntry> result = new ArrayList<>();
        for (ContainerSlotType container : containers.keySet()) {
            result.add(new ItemStackResponsePacket.ContainerEntry(container, containers.get(container)));
        }
        return result;
    }
}
