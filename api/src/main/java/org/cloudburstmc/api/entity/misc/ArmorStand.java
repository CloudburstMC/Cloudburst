package org.cloudburstmc.api.entity.misc;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.item.EquipmentSlot;
import org.cloudburstmc.api.item.ItemStack;

/**
 * A stand that displays armor and held items.
 */
public interface ArmorStand extends Entity {

    /**
     * Returns the stack in the slot, or an empty stack if the slot is unoccupied.
     */
    ItemStack getEquipment(EquipmentSlot slot);

    /**
     * Sets the stack in the slot. An empty stack clears the slot.
     */
    void setEquipment(EquipmentSlot slot, ItemStack item);

    /**
     * Returns the current pose index, from 0 (default) through 12.
     */
    int getPoseIndex();

    /**
     * Sets the current pose.
     *
     * @throws IllegalArgumentException if the index is outside 0 through 12
     */
    void setPoseIndex(int index);
}
