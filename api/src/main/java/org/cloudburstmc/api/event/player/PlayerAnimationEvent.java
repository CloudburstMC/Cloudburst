package org.cloudburstmc.api.event.player;

import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.player.Player;

public class PlayerAnimationEvent extends PlayerEvent implements Cancellable {

    private final PlayerAnimationType animationType;

    public PlayerAnimationEvent(Player player) {
        this(player, PlayerAnimationType.SWING_ARM);
    }

    public PlayerAnimationEvent(Player player, PlayerAnimationType animation) {
        super(player);
        this.animationType = animation;
    }

    public PlayerAnimationType getAnimationType() {
        return this.animationType;
    }
}
