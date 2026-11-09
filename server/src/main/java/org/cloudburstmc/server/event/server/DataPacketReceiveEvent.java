package org.cloudburstmc.server.event.server;

import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.event.server.ServerEvent;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;

/**
 * Fires before a decoded gameplay packet is handled. A reference-counted payload is borrowed for
 * the duration of the event.
 */
public class DataPacketReceiveEvent extends ServerEvent implements Cancellable {

    private final BedrockPacket packet;
    private final Player player;

    public DataPacketReceiveEvent(Player player, BedrockPacket packet) {
        this.packet = packet;
        this.player = player;
    }

    public BedrockPacket getPacket() {
        return packet;
    }

    public Player getPlayer() {
        return player;
    }
}
