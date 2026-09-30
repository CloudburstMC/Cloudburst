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
    private final DoubleUnaryOperator adjustment;
    private final float helmetMultiplier;
    private final float cooldownDamage;
    private final DoubleUnaryOperator reduction;
    private final float absorption;

    public CloudEntityDamageEvent(Entity entity, DamageSource source, float amount, float absorption) {
        this(entity, source, amount, _ -> 0, damage -> damage, 1, 0, damage -> damage, absorption);
    }

    public CloudEntityDamageEvent(Entity entity, DamageSource source, float amount, DoubleUnaryOperator blocking, DoubleUnaryOperator adjustment, float helmetMultiplier, float cooldownDamage, DoubleUnaryOperator reduction, float absorption) {
        super(entity, source, amount);
        checkArgument(Float.isFinite(cooldownDamage) && cooldownDamage >= 0,
                "cooldownDamage must be finite and non-negative");
        checkArgument(Float.isFinite(absorption) && absorption >= 0, "absorption must be finite and non-negative");
        checkArgument(Float.isFinite(helmetMultiplier) && helmetMultiplier > 0 && helmetMultiplier <= 1,
                "helmetMultiplier must be in (0, 1]");
        this.blocking = requireNonNull(blocking, "blocking");
        this.adjustment = requireNonNull(adjustment, "adjustment");
        this.helmetMultiplier = helmetMultiplier;
        this.cooldownDamage = cooldownDamage;
        this.reduction = requireNonNull(reduction, "reduction");
        this.absorption = absorption;
    }

    @Override
    public float getBlockedDamage() {
        return Math.clamp(evaluate(this.blocking, this.getDamage()), 0, this.getDamage());
    }

    /**
     * Returns incoming damage after item blocking and before the hurt cooldown.
     */
    public float getUnblockedDamage() {
        return this.getDamage() - this.getBlockedDamage();
    }

    /**
     * Returns unblocked damage after source-specific adjustments, before the hurt cooldown.
     */
    public float getAdjustedDamage() {
        return evaluate(damage -> this.adjustment.applyAsDouble(damage) * this.helmetMultiplier,
                this.getUnblockedDamage());
    }

    /**
     * Returns damage after blocking, source adjustments, helmet protection and the hurt cooldown,
     * before armor and effects.
     */
    public float getDamageBeforeReductions() {
        return Math.max(0, this.getAdjustedDamage() - this.cooldownDamage);
    }

    /**
     * Returns accepted damage before helmet protection for helmet durability.
     */
    public float getHelmetDamage() {
        return (float) Math.min((double) this.getDamageBeforeReductions() / this.helmetMultiplier, Float.MAX_VALUE);
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
        return evaluate(this.reduction, this.getDamageBeforeReductions());
    }

    private static float evaluate(DoubleUnaryOperator operation, float damage) {
        double result = operation.applyAsDouble(damage);
        checkArgument(Double.isFinite(result) && result >= 0, "Damage calculation must be finite and non-negative");
        return (float) Math.min(result, Float.MAX_VALUE);
    }
}
