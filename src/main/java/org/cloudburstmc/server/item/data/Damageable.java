package org.cloudburstmc.server.item.data;

import lombok.Value;

@Value(staticConstructor = "of")
public class Damageable {

    /**
     * Current item durability
     */
    int durability;

    boolean unbreakable;

}
