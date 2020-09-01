package org.cloudburstmc.server.item.data;

import lombok.Value;

@Value
public class Damageable {

    /**
     * Current item durability
     */
    int durability;

    boolean unbreakable;

}
