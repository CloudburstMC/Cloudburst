package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.Noteblock;
import org.cloudburstmc.api.blockentity.data.BlockEntityData;
import org.cloudburstmc.api.blockentity.data.MusicData;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.math.vector.Vector3i;

public class MusicBlockEntity extends BaseBlockEntity implements Noteblock {

    private byte note;
    private boolean powered;

    public MusicBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    protected Class<? extends BlockEntityData> minimumDataClass() {
        return MusicData.class;
    }

    @Override
    protected void applyData(BlockEntityData data) {
        super.applyData(data);
        MusicData music = (MusicData) data;
        this.note = music.getNote();
        this.powered = music.isPowered();
    }

    @Override
    protected void fillData(BlockEntityData data) {
        super.fillData(data);
        MusicData music = (MusicData) data;
        music.setNote(this.note);
        music.setPowered(this.powered);
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
