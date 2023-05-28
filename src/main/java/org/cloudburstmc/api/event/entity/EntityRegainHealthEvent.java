package org.cloudburstmc.api.event.entity;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.event.Cancellable;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public final class EntityRegainHealthEvent extends EntityEvent implements Cancellable {

    public static final int CAUSE_REGEN = 0;
    public static final int CAUSE_EATING = 1;
    public static final int CAUSE_MAGIC = 2;
    public static final int CAUSE_CUSTOM = 3;

    private float amount;
    private final int reason;

    public EntityRegainHealthEvent(Entity entity, float amount, int regainReason) {
        this.entity = entity;
        this.amount = amount;
        this.reason = regainReason;
    }

    public float getAmount() {
        return amount;
    }

    public void setAmount(float amount) {
        this.amount = amount;
    }

    public int getRegainReason() {
        return reason;
    }
}
