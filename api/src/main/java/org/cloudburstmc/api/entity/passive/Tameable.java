package org.cloudburstmc.api.entity.passive;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.Mob;
import org.cloudburstmc.api.player.Player;

import java.util.UUID;

/**
 * A mob that can be tamed and owned by a player.
 */
public interface Tameable extends Mob {

    /**
     * Returns the owning player's UUID, even when the player is offline.
     */
    @Nullable
    UUID getOwnerUniqueId();

    /**
     * Tames this mob to the given player UUID, or untames it if the UUID is null.
     */
    void setOwnerUniqueId(@Nullable UUID ownerUniqueId);

    /**
     * Returns the owning player if online.
     */
    @Override
    @Nullable
    Player getOwner();

    /**
     * Tames this mob to the given player, or untames it if the owner is null.
     *
     * @throws IllegalArgumentException if the owner is not a player
     */
    @Override
    void setOwner(@Nullable Entity owner);

    /**
     * Returns whether this mob is tamed.
     */
    boolean isTamed();

    /**
     * Changes whether this mob is tamed. Untaming it clears its owner.
     */
    void setTamed(boolean tamed);

    /**
     * Returns whether this mob is sitting.
     */
    boolean isSitting();

    /**
     * Changes whether this mob is sitting.
     */
    void setSitting(boolean sitting);
}
