package org.cloudburstmc.api.item.data;

public enum Coal {
    /**
     * Represents a regular coal item.
     */
    REGULAR,
    /**
     * Represents a charcoal item.
     */
    CHARCOAL;

    /**
     * Returns whether or not this item is charcoal.
     *
     * @return whether or not this item is charcoal
     */
    public final boolean isCharcoal() {
        return this == CHARCOAL;
    }

    @Override
    public final String toString() {
        return "Coal(" +
                "type=" + name() +
                ')';
    }
}
