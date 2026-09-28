package org.cloudburstmc.api.item.component;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Defines directional attack protection and durability costs while an item is raised.
 *
 * @param delayTicks           ticks before a raised item can block
 * @param horizontalAngle      maximum angle in degrees between the facing direction and damage origin
 * @param damageThreshold      minimum blocked damage that costs durability
 * @param damageBase           fixed durability cost when the threshold is reached
 * @param damageFactor         additional durability cost per point of blocked damage
 * @param disableCooldownScale multiplier for an attacker's blocking-disable duration
 */
public record AttackBlockingComponent(int delayTicks, float horizontalAngle, float damageThreshold, float damageBase, float damageFactor, float disableCooldownScale) {

    public AttackBlockingComponent {
        checkArgument(delayTicks >= 0, "delayTicks must be non-negative");
        checkArgument(Float.isFinite(horizontalAngle) && horizontalAngle > 0 && horizontalAngle <= 180, "horizontalAngle must be between zero and 180 degrees");
        checkArgument(Float.isFinite(damageThreshold) && damageThreshold >= 0, "damageThreshold must be finite and non-negative");
        checkArgument(Float.isFinite(damageBase) && damageBase >= 0, "damageBase must be finite and non-negative");
        checkArgument(Float.isFinite(damageFactor) && damageFactor >= 0, "damageFactor must be finite and non-negative");
        checkArgument(Float.isFinite(disableCooldownScale) && disableCooldownScale >= 0, "disableCooldownScale must be finite and non-negative");
    }

    /**
     * Tests the horizontal angle to the damage origin.
     *
     * @param facingDot dot product of the normalized horizontal facing and source directions
     * @return whether the origin is within the blocking angle
     */
    public boolean blocksDirection(float facingDot) {
        return Float.isFinite(facingDot) && Math.acos(Math.clamp(facingDot, -1, 1)) < Math.toRadians(this.horizontalAngle);
    }

    /**
     * Calculates durability lost to a successful block, rounded down.
     *
     * @param blockedDamage damage prevented by the item
     * @return non-negative durability cost
     */
    public int durabilityDamage(float blockedDamage) {
        checkArgument(Float.isFinite(blockedDamage) && blockedDamage >= 0, "blockedDamage must be finite and non-negative");
        return blockedDamage < this.damageThreshold ? 0 : (int) Math.floor(this.damageBase + this.damageFactor * blockedDamage);
    }
}
