package org.cloudburstmc.api.event.block;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.potion.PotionEffect;

import java.util.Objects;

/**
 * Called before a beacon applies one of its powers to a player.
 */
public class BeaconEffectEvent extends BlockEvent implements Cancellable {

    private final Player player;
    private final boolean primary;
    private PotionEffect effect;

    public BeaconEffectEvent(Block beacon, PotionEffect effect, Player player, boolean primary) {
        super(beacon);
        this.effect = Objects.requireNonNull(effect, "effect");
        this.player = player;
        this.primary = primary;
    }

    /**
     * Returns the player receiving the effect.
     */
    public Player getPlayer() {
        return this.player;
    }

    /**
     * Returns whether this is the beacon's primary power.
     */
    public boolean isPrimary() {
        return this.primary;
    }

    /**
     * Returns the effect to apply.
     */
    public PotionEffect getEffect() {
        return this.effect;
    }

    /**
     * Replaces the effect applied to this player.
     */
    public void setEffect(PotionEffect effect) {
        this.effect = Objects.requireNonNull(effect, "effect");
    }
}
