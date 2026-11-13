package org.cloudburstmc.api.blockentity;

import org.cloudburstmc.api.block.NoteBlockInstrument;

/**
 * A note block's stored pitch and playback controls.
 */
public interface NoteBlock extends BlockEntity {

    /**
     * Returns the stored pitch from 0 through 24.
     */
    int getNote();

    /**
     * Sets the stored pitch from 0 through 24 without playing it.
     */
    void setNote(int note);

    /**
     * Returns the instrument selected by the surrounding blocks.
     */
    NoteBlockInstrument getInstrument();

    /**
     * Returns whether the note block currently receives redstone power.
     */
    boolean isPowered();

    /**
     * Attempts to play the stored pitch. Returns false if muted or canceled.
     */
    boolean play();
}
