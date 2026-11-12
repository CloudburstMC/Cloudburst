package org.cloudburstmc.api.event.player;

import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.inventory.view.BlockLoomView;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.data.BannerPattern;

import java.util.Objects;

/**
 * Called when a player selects a banner pattern at a loom.
 */
public class PlayerLoomPatternSelectEvent extends PlayerEvent implements Cancellable {

    private final BlockLoomView loomInventory;
    private BannerPattern.Type patternType;

    public PlayerLoomPatternSelectEvent(Player player, BlockLoomView loomInventory, BannerPattern.Type patternType) {
        super(player);
        this.loomInventory = Objects.requireNonNull(loomInventory, "loomInventory");
        this.patternType = Objects.requireNonNull(patternType, "patternType");
    }

    /**
     * Returns the loom inventory where the pattern was selected.
     */
    public BlockLoomView getLoomInventory() {
        return this.loomInventory;
    }

    /**
     * Returns the proposed pattern.
     */
    public BannerPattern.Type getPatternType() {
        return this.patternType;
    }

    /**
     * Replaces the proposed pattern.
     */
    public void setPatternType(BannerPattern.Type patternType) {
        this.patternType = Objects.requireNonNull(patternType, "patternType");
    }
}
