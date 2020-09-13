package org.cloudburstmc.server.item.data;

import lombok.Value;

import javax.annotation.concurrent.Immutable;

@Value(staticConstructor = "of")
@Immutable
public class Damageable {

    /**
     * Current item durability
     */
    int durability;

    boolean unbreakable;

}
