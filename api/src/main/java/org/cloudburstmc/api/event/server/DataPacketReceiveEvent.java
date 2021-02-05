package org.cloudburstmc.api.event.server;

import com.nukkitx.protocol.bedrock.BedrockPacket;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.player.Player;

/**
 * author: MagicDroidX
 * Nukkit Project
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