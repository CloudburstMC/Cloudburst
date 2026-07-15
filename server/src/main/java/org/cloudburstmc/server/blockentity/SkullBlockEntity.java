package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.Skull;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;

public class SkullBlockEntity extends BaseBlockEntity implements Skull {
    private float rotation;
    private int skullType;
    private boolean mouthMoving;
    private int mouthTickCount;

    public SkullBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);

        tag.listenForByte("SkullType", this::setSkullType);
        tag.listenForFloat("Rotation", this::setRotation);
        tag.listenForInt("MouthTickCount", this::setMouthTickCount);
        tag.listenForBoolean("MouthMoving", this::setMouthMoving);
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);

        tag.putByte("SkullType", (byte) this.getSkullType());
        tag.putFloat("Rotation", this.getRotation());
        tag.putInt("MouthTickCount", this.getMouthTickCount());
        tag.putBoolean("MouthMoving", this.isMouthMoving());
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
