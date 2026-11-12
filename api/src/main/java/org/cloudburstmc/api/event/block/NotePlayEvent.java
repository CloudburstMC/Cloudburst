package org.cloudburstmc.api.event.block;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.NoteBlockInstrument;
import org.cloudburstmc.api.event.Cancellable;

import java.util.Objects;

/**
 * Called before a note block plays. Cancelling playback or changing its sound does not change the stored tuning.
 */
public class NotePlayEvent extends BlockEvent implements Cancellable {

    private NoteBlockInstrument instrument;
    private int note;

    public NotePlayEvent(Block block, NoteBlockInstrument instrument, int note) {
        super(block);
        this.setInstrument(instrument);
        this.setNote(note);
    }

    /**
     * Returns the instrument for this playback.
     */
    public NoteBlockInstrument getInstrument() {
        return this.instrument;
    }

    /**
     * Changes the instrument for this playback.
     */
    public void setInstrument(NoteBlockInstrument instrument) {
        this.instrument = Objects.requireNonNull(instrument, "instrument");
    }

    /**
     * Returns the pitch from 0 through 24, in semitones.
     */
    public int getNote() {
        return this.note;
    }

    /**
     * Sets the pitch for this playback, from 0 through 24.
     */
    public void setNote(int note) {
        if (note < 0 || note > 24) {
            throw new IllegalArgumentException("Note must be between 0 and 24");
        }
        this.note = note;
    }
}
