package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.Skull;
import org.cloudburstmc.api.blockentity.data.BlockEntityData;
import org.cloudburstmc.api.blockentity.data.SkullData;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.math.vector.Vector3i;

public class SkullBlockEntity extends BaseBlockEntity implements Skull {

    public SkullBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    protected Class<? extends BlockEntityData> minimumDataClass() {
        return SkullData.class;
    }

    @Override
    protected SkullData data() {
        return (SkullData) super.data();
    }

    public float getRotation() {
        return data().getRotation();
    }

    public void setRotation(float rotation) {
        data().setRotation(rotation);
    }

    public int getMouthTickCount() {
        return data().getMouthTickCount();
    }

    public void setMouthTickCount(int mouthTickCount) {
        data().setMouthTickCount(mouthTickCount);
    }

    public boolean isMouthMoving() {
        return data().isMouthMoving();
    }

    public void setMouthMoving(boolean mouthMoving) {
        data().setMouthMoving(mouthMoving);
    }

    public int getSkullType() {
        return data().getSkullType();
    }

    public void setSkullType(int skullType) {
        data().setSkullType((byte) skullType);
    }

    @Override
    public boolean isSpawnable() {
        return true;
    }
}
