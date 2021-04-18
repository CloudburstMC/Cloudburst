package org.cloudburstmc.api.item.data;

import lombok.Value;

@Value(staticConstructor = "of")
public class Damageable {

    /**
     * Current item durability
     */
    int durability;

    boolean unbreakable;

    public Damageable damage() {
        return damage(1);
    }

    public Damageable damage(int amount) {
        return new Damageable(Math.max(0, this.durability + amount), unbreakable);
    }

    public Damageable repair() {
        return repair(1);
    }

    public Damageable repair(int amount) {
        return new Damageable(Math.max(0, this.durability - amount), unbreakable);
    }
}
