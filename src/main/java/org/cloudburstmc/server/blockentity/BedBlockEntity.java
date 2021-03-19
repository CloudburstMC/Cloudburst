package org.cloudburstmc.server.blockentity;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.blockentity.Bed;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.utils.data.DyeColor;

/**
 * Created by CreeperFace on 2.6.2017.
 */
public class BedBlockEntity extends BaseBlockEntity implements Bed {

    public DyeColor color = DyeColor.WHITE;

    public BedBlockEntity(BlockEntityType<?> type, CloudChunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);

        tag.listenForByte("color", this::setColor);
    }

    @Override
    protected void saveClientData(NbtMapBuilder tag) {
        super.saveClientData(tag);

        tag.putByte("color", (byte) this.getColor().getDyeData());
    }

    @Override
    public boolean isValid() {
        return this.getBlockState().getType() == BlockTypes.BED;
    }

    @Override
    public DyeColor getColor() {
        return color;
    }

    private void setColor(int color) {
        this.color = DyeColor.getByDyeData(color);
    }

    @Override
    public void setColor(DyeColor color) {
        this.color = color;
    }

    @Override
    public boolean isSpawnable() {
        return true;
    }
}
