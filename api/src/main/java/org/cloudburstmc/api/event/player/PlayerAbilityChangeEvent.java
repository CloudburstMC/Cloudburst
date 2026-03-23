package org.cloudburstmc.api.event.player;

import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.player.Ability;
import org.cloudburstmc.api.player.Player;

/**
 * Fired when a boolean ability flag on a player is about to change.
 *
 * <p>Plugins may redirect the outcome to a different value by calling
 * {@link #setNewValue(boolean)}.</p>
 */
public final class PlayerAbilityChangeEvent extends PlayerEvent implements Cancellable {

    private final Ability ability;
    private final boolean oldValue;
    private boolean newValue;

    public PlayerAbilityChangeEvent(Player player, Ability ability, boolean oldValue, boolean newValue) {
        super(player);
        this.ability = ability;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    /**
     * The ability flag that is changing.
     */
    public Ability getAbility() {
        return ability;
    }

    /**
     * The value of the ability flag before this change is applied.
     */
    public boolean getOldValue() {
        return oldValue;
    }

    /**
     * The value that will be applied if this event is not cancelled.
     */
    public boolean getNewValue() {
        return newValue;
    }

    /**
     * Overrides the value that will be applied.
     *
     * @param newValue the value to apply to the ability flag
     */
    public void setNewValue(boolean newValue) {
        this.newValue = newValue;
    }
}
