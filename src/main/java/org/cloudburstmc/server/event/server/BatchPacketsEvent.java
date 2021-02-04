package org.cloudburstmc.server.event.server;

import com.nukkitx.protocol.bedrock.BedrockPacket;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.server.player.Player;

public class BatchPacketsEvent extends ServerEvent implements Cancellable {

    private Player[] players;
    private BedrockPacket[] packets;

    private boolean forceSync;

    public BatchPacketsEvent(Player[] players, BedrockPacket[] packets, boolean forceSync) {
        this.players = players;
        this.packets = packets;
        this.forceSync = forceSync;
    }

    public Player[] getPlayers() {
        return players;
    }

    public BedrockPacket[] getPackets() {
        return packets;
    }

    public boolean isForceSync() {
        return forceSync;
    }
}
