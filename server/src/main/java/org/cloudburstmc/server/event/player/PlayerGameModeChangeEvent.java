package org.cloudburstmc.server.event.player;

import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.server.player.GameMode;
import org.cloudburstmc.server.player.Player;

public class PlayerGameModeChangeEvent extends PlayerEvent implements Cancellable {

    protected final GameMode gamemode;

    public PlayerGameModeChangeEvent(Player player, GameMode newGameMode) {
        super(player);
        this.gamemode = newGameMode;
    }

    public GameMode getNewGamemode() {
        return gamemode;
    }
}
