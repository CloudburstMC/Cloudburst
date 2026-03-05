package org.cloudburstmc.api.event.player;

import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.player.Player;

public final class PlayerToggleCrawlEvent extends PlayerEvent implements Cancellable {

    private final boolean isCrawling;

    public PlayerToggleCrawlEvent(Player player, boolean isCrawling) {
        super(player);
        this.isCrawling = isCrawling;
    }

    public boolean isCrawling() {
        return this.isCrawling;
    }
}
