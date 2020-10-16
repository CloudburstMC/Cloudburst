package org.cloudburstmc.server.blockentity.impl;

import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.blockentity.BlockEntityType;
import org.cloudburstmc.server.blockentity.Skull;
import org.cloudburstmc.server.level.chunk.Chunk;

/**
 * Created by Snake1999 on 2016/2/3.
 * Package cn.nukkit.blockentity in project Nukkit.
 */
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

    @Override
    public boolean isValid() {
        return getBlockState().getType() == BlockTypes.SKULL;
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