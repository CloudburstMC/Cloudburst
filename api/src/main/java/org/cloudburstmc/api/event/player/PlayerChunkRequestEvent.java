package org.cloudburstmc.api.event.player;

import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.player.Player;

public class PlayerChunkRequestEvent extends PlayerEvent implements Cancellable {

    private final int chunkX;
    private final int chunkZ;

    public PlayerChunkRequestEvent(Player player, int chunkX, int chunkZ) {
        super(player);
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }
}
