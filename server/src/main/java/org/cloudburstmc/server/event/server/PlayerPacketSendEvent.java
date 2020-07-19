package org.cloudburstmc.server.event.server;

import com.nukkitx.protocol.bedrock.BedrockPacket;
import org.cloudburstmc.server.event.Cancellable;
import org.cloudburstmc.server.event.HandlerList;
import org.cloudburstmc.server.event.player.PlayerEvent;
import org.cloudburstmc.server.player.Player;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class PlayerPacketSendEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private final BedrockPacket packet;

    public PlayerPacketSendEvent(Player player, BedrockPacket packet) {
        super(player);
        this.packet = packet;
    }

    public static HandlerList getHandlers() {
        return handlers;
    }

    public BedrockPacket getPacket() {
        return packet;
    }
}