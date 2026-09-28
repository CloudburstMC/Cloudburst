package org.cloudburstmc.server.item.component;

public record SpearProfile(
        int stabCooldownTicks,
        int delayTicks,
        float damageMultiplier,
        int dismountTicks,
        float dismountSpeed,
        int knockbackTicks,
        float knockbackSpeed,
        int damageTicks,
        float damageSpeed
) {

    public SpearProfile {
        if (stabCooldownTicks < 0 || delayTicks < 0 || dismountTicks < 0 || knockbackTicks < 0
                || damageTicks < 0 || invalid(damageMultiplier) || invalid(dismountSpeed)
                || invalid(knockbackSpeed) || invalid(damageSpeed)) {
            throw new IllegalArgumentException("Spear values must be finite and nonnegative");
        }
    }

    private static boolean invalid(float value) {
        return !Float.isFinite(value) || value < 0;
    }
}
