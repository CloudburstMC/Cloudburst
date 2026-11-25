package org.cloudburstmc.api.blockentity;

/**
 * A block entity with an animated lid.
 */
public interface Lidded {

    /**
     * Returns whether the lid is open.
     */
    boolean isLidOpen();

    /**
     * Opens the lid until explicitly closed, or closes it even if the inventory is being viewed.
     *
     * @param open whether to open the lid
     */
    void setLidOpen(boolean open);
}
