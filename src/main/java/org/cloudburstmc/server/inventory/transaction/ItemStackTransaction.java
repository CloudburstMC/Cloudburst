package org.cloudburstmc.server.inventory.transaction;

import com.nukkitx.protocol.bedrock.packet.ItemStackResponsePacket;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.server.inventory.transaction.action.InventoryAction;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.ArrayList;
import java.util.List;

@Log4j2
public class ItemStackTransaction extends InventoryTransaction {
    @Getter
    private List<ItemStackResponsePacket.Response> responses = new ArrayList<>();

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

    public void addResponse(ItemStackResponsePacket.Response response) {
        this.responses.add(response);
    }
}
