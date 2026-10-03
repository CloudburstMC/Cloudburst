package org.cloudburstmc.api.event.player;

import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.event.entity.EntityDeathEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;

import java.util.List;

/**
 * Fired before a player dies. Cancellation restores the configured revival health.
 * A non-null death message is broadcast when the death-message game rule permits it.
 */
public class PlayerDeathEvent extends EntityDeathEvent {

    @Nullable
    private Component deathMessage;
    private boolean keepInventory = false;
    private boolean keepExperience = false;

    /**
     * @param player            the dying player
     * @param damageSource      the source responsible for death
     * @param drops             the proposed item drops
     * @param deathMessage      the message to broadcast, or {@code null} to suppress it
     * @param droppedExperience the non-negative experience to drop
     */
    public PlayerDeathEvent(Player player, DamageSource damageSource, List<ItemStack> drops, @Nullable Component deathMessage, int droppedExperience) {
        super(player, damageSource, drops, droppedExperience);
        this.deathMessage = deathMessage;
    }

    /**
     * @return the dying player
     */
    @Override
    public Player getEntity() {
        return (Player) super.getEntity();
    }

    /**
     * Returns the proposed death message, or {@code null} if suppressed.
     *
     * @return the death message component, or {@code null}
     */
    @Nullable
    public Component getDeathMessage() {
        return deathMessage;
    }

    /**
     * Sets the death message to broadcast. Pass {@code null} to suppress the message entirely.
     * This does not override the death-message game rule.
     *
     * @param deathMessage the new death message, or {@code null}
     */
    public void setDeathMessage(@Nullable Component deathMessage) {
        this.deathMessage = deathMessage;
    }

    /**
     * @return whether inventory contents are retained instead of dropping the event's items
     */
    public boolean getKeepInventory() {
        return keepInventory;
    }

    /**
     * @param keepInventory whether inventory contents should be retained and item drops suppressed
     */
    public void setKeepInventory(boolean keepInventory) {
        this.keepInventory = keepInventory;
    }

    /**
     * @return whether current experience is retained and experience drops suppressed
     */
    public boolean getKeepExperience() {
        return keepExperience;
    }

    /**
     * @param keepExperience whether current experience should be retained and experience drops suppressed
     */
    public void setKeepExperience(boolean keepExperience) {
        this.keepExperience = keepExperience;
    }
}
