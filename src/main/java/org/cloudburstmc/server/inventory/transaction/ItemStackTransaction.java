package org.cloudburstmc.server.inventory.transaction;

import com.nukkitx.protocol.bedrock.packet.ItemStackResponsePacket;
import lombok.Getter;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.ArrayList;
import java.util.List;

public class ItemStackTransaction extends InventoryTransaction {
    @Getter
    private List<ItemStackResponsePacket.Response> responses = new ArrayList<>();

    public ItemStackTransaction(CloudPlayer source) {
        super(source, new ArrayList<>(), true);
    }

    @Override
    public boolean canExecute() {
        return !this.getActions().isEmpty();
    }

    @Override
    protected boolean callExecuteEvent() {
        //TODO - call event?
        return true;
    }

    public void addResponse(ItemStackResponsePacket.Response response) {
        this.responses.add(response);
    }
}
