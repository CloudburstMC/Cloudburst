package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.Smoker;
import org.cloudburstmc.api.inventory.InventoryType;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.level.chunk.CloudChunk;

public class SmokerBlockEntity extends FurnaceBlockEntity implements Smoker {

    public SmokerBlockEntity(BlockEntityType<?> type, CloudChunk chunk, Vector3i position) {
        super(type, chunk, position, InventoryType.SMOKER);
    }

    @Override
    public boolean isValid() {
        return getBlockState().getType() == BlockTypes.SMOKER;
    }

    @Override
    public float getBurnRate() {
        return 2.0f;
    }

    @Override
    protected void extinguishFurnace() {
        this.getLevel().setBlock(this.getPosition(), getBlockState().withTrait(BlockTraits.IS_EXTINGUISHED, true), true);
    }

    @Override
    protected void lightFurnace() {
        this.getLevel().setBlock(this.getPosition(), getBlockState().withTrait(BlockTraits.IS_EXTINGUISHED, false), true);
    }
}
