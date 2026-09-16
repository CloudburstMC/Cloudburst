package org.cloudburstmc.server.item;

import org.cloudburstmc.api.item.EquipmentSlot;
import org.cloudburstmc.api.item.ItemTagKey;
import org.cloudburstmc.server.level.Sound;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

/**
 * Defines the shared properties of one vanilla armor material.
 */
public record ArmorMaterial(int durabilityMultiplier, int bootsDefense, int leggingsDefense, int chestplateDefense,
                            int helmetDefense, float toughness, float knockbackResistance, ItemTagKey repairTag,
                            Sound equipSound) {

    public ArmorMaterial {
        checkArgument(durabilityMultiplier > 0, "durabilityMultiplier must be positive");
        checkArgument(bootsDefense >= 0 && leggingsDefense >= 0 && chestplateDefense >= 0 && helmetDefense >= 0, "defense must be non-negative");
        checkArgument(Float.isFinite(toughness) && toughness >= 0, "toughness must be finite and non-negative");
        checkArgument(Float.isFinite(knockbackResistance) && knockbackResistance >= 0 && knockbackResistance <= 1, "knockbackResistance must be between zero and one");
        requireNonNull(repairTag, "repairTag");
        requireNonNull(equipSound, "equipSound");
    }

    /**
     * Returns the maximum durability for an armor slot.
     *
     * @param slot the armor slot
     * @return the maximum durability
     */
    public int durability(EquipmentSlot slot) {
        int baseDurability = switch (slot) {
            case HEAD -> 11;
            case CHEST -> 16;
            case LEGS -> 15;
            case FEET -> 13;
            case MAIN_HAND, OFF_HAND -> throw new IllegalArgumentException("Not an armor slot: " + slot);
        };

        return baseDurability * this.durabilityMultiplier;
    }

    /**
     * Returns the defense provided in an armor slot.
     *
     * @param slot the armor slot
     * @return the defense points
     */
    public int defense(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> this.helmetDefense;
            case CHEST -> this.chestplateDefense;
            case LEGS -> this.leggingsDefense;
            case FEET -> this.bootsDefense;
            case MAIN_HAND, OFF_HAND -> throw new IllegalArgumentException("Not an armor slot: " + slot);
        };
    }
}
