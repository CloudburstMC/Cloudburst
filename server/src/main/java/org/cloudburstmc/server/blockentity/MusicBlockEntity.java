package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.Noteblock;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;

public class MusicBlockEntity extends BaseBlockEntity implements Noteblock {

    private byte note;
    private boolean powered;

    public MusicBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);

        tag.listenForByte("note", this::setNote);
        tag.listenForBoolean("powered", this::setPowered);
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);

        tag.putByte("note", this.getNote());
        tag.putBoolean("powered", this.isPowered());
    }

    @Override
    public boolean isValid() {
        return this.getBlockState().getType() == BlockTypes.NOTEBLOCK;
    }

    public byte getNote() {
        return note;
    }

    public void setNote(int note) {
        this.note = (byte) note;
    }

    public void changeNote() {
        this.setNote((note + 1) % 25);
    }

    public boolean isPowered() {
        return this.powered;
    }

    public void setPowered(boolean powered) {
        this.powered = powered;
    }
}
