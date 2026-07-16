package org.cloudburstmc.api.event.entity;

import org.cloudburstmc.api.entity.projectile.FishingHook;
import org.cloudburstmc.api.entity.projectile.FishingHookState;

/**
 * Called after a fishing hook changes physical state.
 *
 * <p>Player fishing actions are reported through {@link
 * org.cloudburstmc.api.event.player.PlayerFishEvent}.</p>
 */
public final class FishingHookStateChangeEvent extends EntityEvent {
    private final FishingHookState previousState;
    private final FishingHookState newState;

    public FishingHookStateChangeEvent(FishingHook hook, FishingHookState previousState, FishingHookState newState) {
        this.entity = hook;
        this.previousState = previousState;
        this.newState = newState;
    }

    /**
     * @return the fishing hook whose state changed
     */
    @Override
    public FishingHook getEntity() {
        return (FishingHook) super.getEntity();
    }

    /**
     * @return the hook state before the transition
     */
    public FishingHookState getPreviousState() {
        return this.previousState;
    }

    /**
     * @return the hook state after the transition
     */
    public FishingHookState getNewState() {
        return this.newState;
    }
}
