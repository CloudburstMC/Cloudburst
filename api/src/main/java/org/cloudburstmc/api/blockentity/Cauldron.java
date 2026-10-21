package org.cloudburstmc.api.blockentity;

/**
 * Potion and dye contents of a cauldron. Liquid type and fill level belong to its block state.
 */
public interface Cauldron extends BlockEntity {

    /**
     * @return the stored potion or dye, or plain contents when neither is present
     */
    CauldronContents getContents();

    /**
     * Replaces potion and dye contents without changing the liquid type or fill level.
     *
     * @param contents the replacement contents
     */
    void setContents(CauldronContents contents);
}
