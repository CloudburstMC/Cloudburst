package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.block.NoteBlockInstrument;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.NoteBlock;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.server.block.component.NoteBlockHandlers;

public class NoteBlockEntity extends BaseBlockEntity implements NoteBlock {

    private int note;
    private boolean powered;

    public NoteBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);

        tag.listenForByte("note", value -> this.note = Math.clamp(value, 0, 24));
        tag.listenForBoolean("powered", value -> this.powered = value);
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);

        tag.putByte("note", (byte) this.note);
        tag.putBoolean("powered", this.isPowered());
    }

    @Override
    public int getNote() {
        return this.note;
    }

    @Override
    public void setNote(int note) {
        if (note < 0 || note > 24) {
            throw new IllegalArgumentException("Note must be between 0 and 24");
        }

        if (this.note != note) {
            this.note = note;
            this.setDirty();
        }
    }

    @Override
    public boolean isPowered() {
        return this.powered;
    }

    public void setPowered(boolean powered) {
        if (this.powered != powered) {
            this.powered = powered;
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
