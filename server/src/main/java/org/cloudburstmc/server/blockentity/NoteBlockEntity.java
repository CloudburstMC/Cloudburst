package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.blockentity.data.MusicData;
import org.cloudburstmc.api.blockentity.data.BlockEntityData;
import org.cloudburstmc.api.block.NoteBlockInstrument;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.NoteBlock;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.block.component.NoteBlockHandlers;

public class NoteBlockEntity extends BaseBlockEntity implements NoteBlock {

    public NoteBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    protected Class<? extends BlockEntityData> minimumDataClass() {
        return MusicData.class;
    }

    @Override
    protected MusicData data() {
        return (MusicData) super.data();
    }

    @Override
    public int getNote() {
        return this.data().getNote();
    }

    @Override
    public void setNote(int note) {
        if (note < 0 || note > 24) {
            throw new IllegalArgumentException("Note must be between 0 and 24");
        }

        if (this.data().getNote() != note) {
            this.data().setNote((byte) note);
            this.setDirty();
        }
    }

    @Override
    public boolean isPowered() {
        return this.data().isPowered();
    }

    public void setPowered(boolean powered) {
        if (this.data().isPowered() != powered) {
            this.data().setPowered(powered);
            this.setDirty();
        }
    }

    @Override
    public NoteBlockInstrument getInstrument() {
        return NoteBlockHandlers.resolveInstrument(this.getBlock());
    }

    @Override
    public boolean play() {
        return !this.isClosed() && this.isValid() && NoteBlockHandlers.play(this.getBlock(), this);
    }
}
