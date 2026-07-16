package org.cloudburstmc.api.event.player;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.projectile.FishingHook;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.player.Player;

/**
 * Called when a player performs a fishing action.
 *
 * <p>Cancellation is respected when casting, becoming lured, biting, or retrieving.
 * Cancelling a retrieval leaves the hook active. {@link PlayerFishState#FAILED_ATTEMPT}
 * reports an action that has already completed.</p>
 */
public final class PlayerFishEvent extends PlayerEvent implements Cancellable {
    private final FishingHook hook;
    private final @Nullable Entity caught;
    private final PlayerFishState state;
    private int experience;

    public PlayerFishEvent(Player player, FishingHook hook, @Nullable Entity caught, PlayerFishState state) {
        super(player);
        this.hook = hook;
        this.caught = caught;
        this.state = state;
    }

    /**
     * @return the fishing hook involved
     */
    public FishingHook getHook() {
        return this.hook;
    }

    /**
     * @return the caught entity or item entity, or {@code null} when nothing was caught
     */
    public @Nullable Entity getCaught() {
        return this.caught;
    }

    /**
     * @return the fishing action represented by this event
     */
    public PlayerFishState getState() {
        return this.state;
    }

    /**
     * @return the experience awarded when {@link #getState()} is
     * {@link PlayerFishState#CAUGHT_ITEM}
     */
    public int getExperience() {
        return this.experience;
    }

    /**
     * Sets the experience awarded for a caught item. This value has no effect for
     * other fishing states.
     *
     * @param experience the non-negative experience amount
     * @throws IllegalArgumentException if {@code experience} is negative
     */
    public void setExperience(int experience) {
        if (experience < 0) {
            throw new IllegalArgumentException("experience cannot be negative");
        }

        this.experience = experience;
    }
}
