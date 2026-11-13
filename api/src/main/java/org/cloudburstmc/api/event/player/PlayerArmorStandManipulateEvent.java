package org.cloudburstmc.api.event.player;

import org.cloudburstmc.api.entity.misc.ArmorStand;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.item.EquipmentSlot;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;

/**
 * Called before a player places, removes, or swaps an item on an armor stand.
 */
public class PlayerArmorStandManipulateEvent extends PlayerEvent implements Cancellable {

    private final ArmorStand armorStand;
    private final ItemStack playerItem;
    private final ItemStack armorStandItem;
    private final EquipmentSlot slot;

    public PlayerArmorStandManipulateEvent(Player player, ArmorStand armorStand, ItemStack playerItem, ItemStack armorStandItem, EquipmentSlot slot) {
        super(player);
        this.armorStand = armorStand;
        this.playerItem = playerItem;
        this.armorStandItem = armorStandItem;
        this.slot = slot;
    }

    /**
     * Returns the stand being manipulated.
     */
    public ArmorStand getArmorStand() {
        return this.armorStand;
    }

    /**
     * Returns the player's held stack before the interaction, which may be empty.
     */
    public ItemStack getPlayerItem() {
        return this.playerItem;
    }

    /**
     * Returns the stand's stack before the interaction, which may be empty.
     */
    public ItemStack getArmorStandItem() {
        return this.armorStandItem;
    }

    /**
     * Returns the slot on the stand being changed.
     */
    public EquipmentSlot getSlot() {
        return this.slot;
    }
}
