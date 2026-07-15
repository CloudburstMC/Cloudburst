package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.blockentity.Bed;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.util.data.DyeColor;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;

public class BedBlockEntity extends BaseBlockEntity implements Bed {

    public DyeColor color = DyeColor.WHITE;

    public BedBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
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

        tag.putByte("color", (byte) this.getColor().getWoolData());
    }

    @Override
    public DyeColor getColor() {
        return color;
    }

    private void setColor(int color) {
        this.color = DyeColor.getByWoolData(color);
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
