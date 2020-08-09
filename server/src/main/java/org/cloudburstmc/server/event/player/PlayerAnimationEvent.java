package org.cloudburstmc.server.event.player;

import com.nukkitx.protocol.bedrock.packet.AnimatePacket;
import org.cloudburstmc.server.event.Cancellable;
import org.cloudburstmc.server.event.HandlerList;
import org.cloudburstmc.server.player.Player;

public class PlayerAnimationEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlers() {
        return handlers;
    }

    private final AnimatePacket.Action animationType;

    public PlayerAnimationEvent(Player player) {
        this(player, AnimatePacket.Action.SWING_ARM);
    }

    public PlayerAnimationEvent(Player player, AnimatePacket.Action animation) {
        super(player);
        this.animationType = animation;
    }

    public AnimatePacket.Action getAnimationType() {
        return this.animationType;
    }
}
