package org.cloudburstmc.api.event.entity;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.RegainReason;
import org.cloudburstmc.api.event.Cancellable;

import static java.util.Objects.requireNonNull;

/**
 * Fired before an entity regains health. Cancellation prevents the healing.
 */
public class EntityRegainHealthEvent extends EntityEvent implements Cancellable {

    private final RegainReason regainReason;
    private float amount;

    /**
     * @param entity       the entity being healed
     * @param amount       the finite, non-negative health to restore
     * @param regainReason the cause of healing
     */
    public EntityRegainHealthEvent(Entity entity, float amount, RegainReason regainReason) {
        this.entity = requireNonNull(entity, "entity");
        this.regainReason = requireNonNull(regainReason, "regainReason");
        this.setAmount(amount);
    }

    /**
     * @return the cause of healing
     */
    public RegainReason getRegainReason() {
        return this.regainReason;
    }

    /**
     * @return the proposed health increase
     */
    public float getAmount() {
        return this.amount;
    }

    /**
     * Changes the health increase. Zero prevents healing without cancelling the event.
     *
     * @param amount the finite, non-negative health to restore
     * @throws IllegalArgumentException if the amount is negative or not finite
     */
    public void setAmount(float amount) {
        if (!Float.isFinite(amount) || amount < 0) {
            throw new IllegalArgumentException("Healing must be finite and non-negative");
        }

        this.amount = amount;
    }
}
