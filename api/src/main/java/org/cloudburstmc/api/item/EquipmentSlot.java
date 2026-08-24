package org.cloudburstmc.api.item;

/**
 * Equipment slot an item may be worn or held in.
 */
public enum EquipmentSlot {
    HEAD(0),
    CHEST(1),
    LEGS(2),
    FEET(3),
    MAIN_HAND(-1),
    OFF_HAND(-1);

    private final int armorSlot;

    EquipmentSlot(int armorSlot) {
        this.armorSlot = armorSlot;
    }

    /**
     * Returns the index used by {@link org.cloudburstmc.api.inventory.view.ArmorView}, or {@code -1}
     * when this is not an armor slot.
     *
     * @return armor slot index, or {@code -1}
     */
    public int getArmorSlot() {
        return this.armorSlot;
    }

    /**
     * Returns whether this slot is part of the armor inventory.
     *
     * @return {@code true} for head, chest, legs, and feet slots
     */
    public boolean isArmor() {
        return this.armorSlot >= 0;
    }
}
