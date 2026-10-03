package org.cloudburstmc.api.item.component;

/**
 * Nutrition restored when an item is eaten.
 * Saturation is the number of points restored, not a saturation modifier.
 *
 * @param nutrition    food points restored
 * @param saturation   saturation points restored
 * @param canAlwaysEat whether the item can be eaten at full hunger
 */
public record FoodComponent(int nutrition, float saturation, boolean canAlwaysEat) {

    public FoodComponent {
        if (nutrition < 0 || !Float.isFinite(saturation) || saturation < 0.0f) {
            throw new IllegalArgumentException("Nutrition and saturation must be non-negative");
        }
    }
}
