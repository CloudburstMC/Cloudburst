package org.cloudburstmc.api.event.player;

import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.entity.Living;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.event.entity.EntityDeathEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;

/**
 * Fired when a player dies. Cancelling this event prevents the death from being processed.
 * The death message is broadcast to all online players if non-null.
 */
public final class PlayerDeathEvent extends EntityDeathEvent implements Cancellable {

    @Nullable
    private Component deathMessage;
    private boolean keepInventory = false;
    private boolean keepExperience = false;
    private int experience;

    public PlayerDeathEvent(Player player, ItemStack[] drops, @Nullable Component deathMessage, int experience) {
        super((Living) player, drops);
        this.deathMessage = deathMessage;
        this.experience = experience;
    }

    @Override
    public Player getEntity() {
        return (Player) super.getEntity();
    }

    /**
     * Returns the death message that will be broadcast, or {@code null} if suppressed.
     *
     * @return the death message component, or {@code null}
     */
    @Nullable
    public Component getDeathMessage() {
        return deathMessage;
    }

    /**
     * Sets the death message to broadcast. Pass {@code null} to suppress the message entirely.
     *
     * @param deathMessage the new death message, or {@code null}
     */
    public void setDeathMessage(@Nullable Component deathMessage) {
        this.deathMessage = deathMessage;
    }

    public boolean getKeepInventory() {
        return keepInventory;
    }

    public void setKeepInventory(boolean keepInventory) {
        this.keepInventory = keepInventory;
    }

    public boolean getKeepExperience() {
        return keepExperience;
    }

    public void setKeepExperience(boolean keepExperience) {
        this.keepExperience = keepExperience;
    }

    public int getExperience() {
        return experience;
    }

    public void setExperience(int experience) {
        this.experience = experience;
    }
}
