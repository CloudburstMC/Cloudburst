package org.cloudburstmc.api.event.player;

import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.player.Player;

public final class PlayerAnimationEvent extends PlayerEvent implements Cancellable {

    private final Type animationType;

    public PlayerAnimationEvent(Player player) {
        this(player, Type.SWING_ARM);
    }

    public PlayerAnimationEvent(Player player, Type animation) {
        super(player);
        this.animationType = animation;
    }

    public Type getAnimationType() {
        return this.animationType;
    }

    public enum Type {
        NO_ACTION,
        SWING_ARM,
        WAKE_UP,
        CRITICAL_HIT,
        MAGIC_CRITICAL_HIT,
        ROW_RIGHT,
        ROW_LEFT,
    }
}
