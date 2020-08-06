package org.cloudburstmc.server.blockentity.impl;

import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.blockentity.BlockEntityType;
import org.cloudburstmc.server.blockentity.Noteblock;
import org.cloudburstmc.server.level.chunk.Chunk;

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
