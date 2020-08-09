package org.cloudburstmc.server.event.player;

import org.cloudburstmc.server.event.Cancellable;
import org.cloudburstmc.server.event.HandlerList;
import org.cloudburstmc.server.player.GameMode;
import org.cloudburstmc.server.player.Player;

public class PlayerGameModeChangeEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlers() {
        return handlers;
    }

    protected final GameMode gamemode;

    public PlayerGameModeChangeEvent(Player player, GameMode newGameMode) {
        super(player);
        this.gamemode = newGameMode;
    }

    public GameMode getNewGamemode() {
        return gamemode;
    }
}
