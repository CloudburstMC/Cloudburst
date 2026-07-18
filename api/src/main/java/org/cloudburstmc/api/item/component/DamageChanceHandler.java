package org.cloudburstmc.api.item.component;

/**
 * Determines the chance that an item consumes durability.
 */
@FunctionalInterface
public interface DamageChanceHandler {

    /**
     * @param unbreakingLevel the item's unbreaking level
     * @return the durability consumption chance
     */
    float execute(int unbreakingLevel);
}
