package org.cloudburstmc.server.event.entity;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;

import java.util.function.DoubleUnaryOperator;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

/**
 * Evaluates a hit against captured defenses without applying gameplay side effects.
 */
public class CloudEntityDamageEvent extends EntityDamageEvent {

    private final DoubleUnaryOperator blocking;
    private final float cooldownDamage;
    private final DoubleUnaryOperator reduction;
    private final float absorption;

    public CloudEntityDamageEvent(Entity entity, DamageSource source, float amount, float absorption) {
        this(entity, source, amount, _ -> 0, 0, damage -> damage, absorption);
    }

    public CloudEntityDamageEvent(Entity entity, DamageSource source, float amount, DoubleUnaryOperator blocking, float cooldownDamage, DoubleUnaryOperator reduction, float absorption) {
        super(entity, source, amount);
        checkArgument(Float.isFinite(cooldownDamage) && cooldownDamage >= 0, "cooldownDamage must be finite and non-negative");
        checkArgument(Float.isFinite(absorption) && absorption >= 0, "absorption must be finite and non-negative");
        this.blocking = requireNonNull(blocking, "blocking");
        this.cooldownDamage = cooldownDamage;
        this.reduction = requireNonNull(reduction, "reduction");
        this.absorption = absorption;
    }

    @Override
    public float getBlockedDamage() {
        return Math.clamp((float) this.blocking.applyAsDouble(this.getDamage()), 0, this.getDamage());
    }

    /**
     * Returns incoming damage after item blocking and before the hurt cooldown.
     */
    public float getUnblockedDamage() {
        return this.getDamage() - this.getBlockedDamage();
    }

    /**
     * Returns damage after item blocking and the hurt cooldown, before armor and effects.
     */
    public float getDamageBeforeReductions() {
        return Math.max(0, this.getUnblockedDamage() - this.cooldownDamage);
    }

    @Override
    public float getAbsorbedDamage() {
        return Math.min(this.absorption, this.getDamageAfterReductions());
    }

    @Override
    public float getFinalDamage() {
        return Math.max(0, this.getDamageAfterReductions() - this.absorption);
    }

    private float getDamageAfterReductions() {
        return (float) this.reduction.applyAsDouble(this.getDamageBeforeReductions());
    }
}
