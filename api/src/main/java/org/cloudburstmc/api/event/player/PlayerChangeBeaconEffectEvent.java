package org.cloudburstmc.api.event.player;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.potion.EffectType;

/**
 * Called when a player confirms a beacon's powers. Cancelling keeps the previous powers and payment.
 */
public class PlayerChangeBeaconEffectEvent extends PlayerEvent implements Cancellable {

    private final Block beacon;
    private @Nullable EffectType primary;
    private @Nullable EffectType secondary;
    private boolean consumeItem = true;

    public PlayerChangeBeaconEffectEvent(Player player, @Nullable EffectType primary, @Nullable EffectType secondary, Block beacon) {
        super(player);
        this.primary = primary;
        this.secondary = secondary;
        this.beacon = beacon;
    }

    /**
     * Returns the beacon being configured.
     */
    public Block getBeacon() {
        return this.beacon;
    }

    /**
     * Returns the proposed primary power, or null.
     */
    public @Nullable EffectType getPrimary() {
        return this.primary;
    }

    /**
     * Sets the proposed primary power, or clears it with null.
     */
    public void setPrimary(@Nullable EffectType primary) {
        this.primary = primary;
    }

    /**
     * Returns the proposed secondary power, or null.
     */
    public @Nullable EffectType getSecondary() {
        return this.secondary;
    }

    /**
     * Sets the proposed secondary power, or clears it with null.
     */
    public void setSecondary(@Nullable EffectType secondary) {
        this.secondary = secondary;
    }

    /**
     * Returns whether the confirmed change consumes the payment.
     */
    public boolean willConsumeItem() {
        return this.consumeItem;
    }

    /**
     * Sets whether the confirmed change consumes the payment.
     */
    public void setConsumeItem(boolean consumeItem) {
        this.consumeItem = consumeItem;
    }
}
