package org.cloudburstmc.api.event.player;

import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.server.level.Location;
import org.cloudburstmc.server.player.Player;

public class PlayerMoveEvent extends PlayerEvent implements Cancellable {

    private Location from;
    private Location to;

    private boolean resetBlocksAround;

    public PlayerMoveEvent(Player player, Location from, Location to) {
        this(player, from, to, true);
    }

    public PlayerMoveEvent(Player player, Location from, Location to, boolean resetBlocks) {
        super(player);
        this.from = from;
        this.to = to;
        this.resetBlocksAround = resetBlocks;
    }

    public Location getFrom() {
        return from;
    }

    public void setFrom(Location from) {
        this.from = from;
    }

    public Location getTo() {
        return to;
    }

    public void setTo(Location to) {
        this.to = to;
    }

    public boolean isResetBlocksAround() {
        return resetBlocksAround;
    }

    public void setResetBlocksAround(boolean value) {
        this.resetBlocksAround = value;
    }

    @Override
    public void setCancelled() {
        super.setCancelled();
    }
}
