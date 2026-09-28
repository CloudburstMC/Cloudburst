package org.cloudburstmc.api.event.player;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.player.Player;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

/**
 * Called before an attack puts a player's blocking item on cooldown.
 * Cancelling preserves blocking and prevents the cooldown.
 */
public class PlayerShieldDisableEvent extends PlayerEvent implements Cancellable {
    private final Entity damager;
    private int cooldown;

    /**
     * Creates a blocking-disable event.
     *
     * @param player   player whose blocking item is being disabled
     * @param damager  attacker responsible for disabling it
     * @param cooldown cooldown duration in ticks
     */
    public PlayerShieldDisableEvent(Player player, Entity damager, int cooldown) {
        super(requireNonNull(player, "player"));
        this.damager = requireNonNull(damager, "damager");
        this.setCooldown(cooldown);
    }

    /**
     * Returns the attacker that disabled blocking.
     *
     * @return the attacker
     */
    public Entity getDamager() {
        return this.damager;
    }

    /**
     * Returns the proposed cooldown duration.
     *
     * @return cooldown in ticks
     */
    public int getCooldown() {
        return this.cooldown;
    }

    /**
     * Sets the cooldown duration. Zero leaves the item available immediately.
     *
     * @param cooldown non-negative duration in ticks
     */
    public void setCooldown(int cooldown) {
        checkArgument(cooldown >= 0, "cooldown must be non-negative");
        this.cooldown = cooldown;
    }
}
