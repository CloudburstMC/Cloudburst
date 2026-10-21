package org.cloudburstmc.api.event.level;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockChange;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.TreeType;
import org.cloudburstmc.api.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Called before a tree grows naturally or through bone meal.
 * For bone meal, this event precedes {@code BlockFertilizeEvent}, which inherits its cancellation state.
 */
public class StructureGrowEvent extends LevelEvent implements Cancellable {

    private final Location location;
    private final TreeType species;
    private final boolean fromBonemeal;
    private final @Nullable Player player;
    private final List<BlockChange> blocks;

    /**
     * @param location     the growth origin
     * @param species      the proposed tree form
     * @param fromBonemeal whether bone meal caused growth
     * @param player       the player applying bone meal, or {@code null}
     * @param blocks       the proposed block changes
     */
    public StructureGrowEvent(Location location, TreeType species, boolean fromBonemeal, @Nullable Player player, List<BlockChange> blocks) {
        super(Objects.requireNonNull(location, "location").getLevel());
        this.location = location;
        this.species = Objects.requireNonNull(species, "species");
        this.fromBonemeal = fromBonemeal;
        this.player = player;
        this.blocks = new ArrayList<>(Objects.requireNonNull(blocks, "blocks"));
    }

    /**
     * @return the growth origin
     */
    public Location getLocation() {
        return this.location;
    }

    /**
     * @return the proposed tree form
     */
    public TreeType getSpecies() {
        return this.species;
    }

    /**
     * @return whether bone meal caused growth
     */
    public boolean isFromBonemeal() {
        return this.fromBonemeal;
    }

    /**
     * @return the player applying bone meal, or {@code null}
     */
    public @Nullable Player getPlayer() {
        return this.player;
    }

    /**
     * Returns the mutable changes to apply when growth is allowed.
     * Bone-meal growth passes listener changes on to {@code BlockFertilizeEvent}.
     *
     * @return the proposed block changes
     */
    public List<BlockChange> getBlocks() {
        return this.blocks;
    }
}
