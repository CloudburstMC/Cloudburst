package org.cloudburstmc.api.event.player;

import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.player.ExhaustionReason;
import org.cloudburstmc.api.player.Player;

import static java.util.Objects.requireNonNull;

/**
 * Fired before exhaustion from an action is applied. The proposed amount may be zero.
 * Cancelling prevents the increase.
 */
public class PlayerExhaustionEvent extends PlayerEvent implements Cancellable {

    private final ExhaustionReason reason;
    private float amount;

    /**
     * @param player the affected player
     * @param reason the cause of exhaustion
     * @param amount the proposed amount to add
     */
    public PlayerExhaustionEvent(Player player, ExhaustionReason reason, float amount) {
        super(player);
        this.reason = requireNonNull(reason, "reason");
        this.setAmount(amount);
    }

    /**
     * @return the cause of exhaustion
     */
    public ExhaustionReason getReason() {
        return this.reason;
    }

    /**
     * Returns the proposed addition, not the player's total exhaustion.
     *
     * @return the amount of exhaustion to add
     */
    public float getAmount() {
        return this.amount;
    }

    /**
     * Changes the exhaustion increase. Non-positive values prevent the increase.
     *
     * @param amount the proposed exhaustion amount
     * @throws IllegalArgumentException if the amount is not finite
     */
    public void setAmount(float amount) {
        if (!Float.isFinite(amount)) {
            throw new IllegalArgumentException("Exhaustion must be finite");
        }

        this.amount = amount;
    }
}
