package org.cloudburstmc.api.event.player;

import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Skin;

/**
 * author: KCodeYT
 * Nukkit Project
 */
public class PlayerChangeSkinEvent extends PlayerEvent implements Cancellable {

    private final Skin skin;

    public PlayerChangeSkinEvent(Player player, Skin skin) {
        super(player);
        this.skin = skin;
    }

    public Skin getSkin() {
        return this.skin;
    }

}
