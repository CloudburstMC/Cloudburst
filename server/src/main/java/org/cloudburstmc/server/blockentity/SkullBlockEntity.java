package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.Skull;
import org.cloudburstmc.api.blockentity.data.BlockEntityData;
import org.cloudburstmc.api.blockentity.data.SkullData;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.math.vector.Vector3i;

public class SkullBlockEntity extends BaseBlockEntity implements Skull {
    private float rotation;
    private int skullType;
    private boolean mouthMoving;
    private int mouthTickCount;

    public SkullBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    protected Class<? extends BlockEntityData> minimumDataClass() {
        return SkullData.class;
    }

    @Override
    protected void applyData(BlockEntityData data) {
        super.applyData(data);
        SkullData skull = (SkullData) data;
        this.skullType = skull.getSkullType();
        this.rotation = skull.getRotation();
        this.mouthTickCount = skull.getMouthTickCount();
        this.mouthMoving = skull.isMouthMoving();
    }

    @Override
    protected void fillData(BlockEntityData data) {
        super.fillData(data);
        SkullData skull = (SkullData) data;
        skull.setSkullType((byte) this.skullType);
        skull.setRotation(this.rotation);
        skull.setMouthTickCount(this.mouthTickCount);
        skull.setMouthMoving(this.mouthMoving);
    }

    public float getRotation() {
        return this.rotation;
    }

    public void setRotation(float rotation) {
        this.rotation = rotation;
    }

    public int getMouthTickCount() {
        return mouthTickCount;
    }

    public void setMouthTickCount(int mouthTickCount) {
        this.mouthTickCount = mouthTickCount;
    }

    public boolean isMouthMoving() {
        return mouthMoving;
    }

    public void setMouthMoving(boolean mouthMoving) {
        this.mouthMoving = mouthMoving;
    }

    public int getSkullType() {
        return skullType;
    }

    public void setSkullType(int skullType) {
        this.skullType = skullType;
    }

    @Override
    public boolean isSpawnable() {
        return true;
    }
}
