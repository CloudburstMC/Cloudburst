package org.cloudburstmc.api.item.component;

@FunctionalInterface
public interface DamageChanceHandler {

    int execute(int unbreaking);
}
