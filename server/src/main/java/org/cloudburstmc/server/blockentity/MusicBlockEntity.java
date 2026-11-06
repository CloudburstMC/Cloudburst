package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.Noteblock;
import org.cloudburstmc.api.blockentity.data.BlockEntityData;
import org.cloudburstmc.api.blockentity.data.MusicData;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.math.vector.Vector3i;

public class MusicBlockEntity extends BaseBlockEntity implements Noteblock {

    public MusicBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
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

    public byte getNote() {
        return data().getNote();
    }

    public void setNote(int note) {
        data().setNote((byte) note);
    }

    public void changeNote() {
        this.setNote((getNote() + 1) % 25);
    }

    public boolean isPowered() {
        return data().isPowered();
    }

    public void setPowered(boolean powered) {
        data().setPowered(powered);
    }
}
