package org.cloudburstmc.api.item.component;

@FunctionalInterface
public interface DamageChanceHandler {

    float execute(int unbreakingLevel);
}
