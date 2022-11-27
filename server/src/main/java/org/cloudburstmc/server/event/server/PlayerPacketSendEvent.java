package org.cloudburstmc.server.event.server;

import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.event.player.PlayerEvent;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class PlayerPacketSendEvent extends PlayerEvent implements Cancellable {

    private final BedrockPacket packet;

    public PlayerPacketSendEvent(Player player, BedrockPacket packet) {
        super(player);
        this.packet = packet;
    }

    public BedrockPacket getPacket() {
        return packet;
    }
}