package org.cloudburstmc.api.event.entity;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;

import static java.util.Objects.requireNonNull;

/**
 * Called before an item places an entity. Cancellation prevents placement and item consumption.
 */
public class EntityPlaceEvent extends EntityEvent implements Cancellable {

    private final @Nullable Player player;
    private final Block block;
    private final Direction blockFace;

    /**
     * @param entity    the entity being placed
     * @param player    the player placing it, or {@code null} for non-player placement
     * @param block     the targeted block
     * @param blockFace the targeted face
     */
    public EntityPlaceEvent(Entity entity, @Nullable Player player, Block block, Direction blockFace) {
        this.entity = requireNonNull(entity, "entity");
        this.player = player;
        this.block = requireNonNull(block, "block");
        this.blockFace = requireNonNull(blockFace, "blockFace");
    }

    /**
     * @return the placing player, or {@code null} for non-player placement
     */
    public @Nullable Player getPlayer() {
        return this.player;
    }

    /**
     * @return the targeted block
     */
    public Block getBlock() {
        return this.block;
    }

    /**
     * @return the targeted face
     */
    public Direction getBlockFace() {
        return this.blockFace;
    }
}
