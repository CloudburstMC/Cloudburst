package org.cloudburstmc.server.blockentity.impl;

import com.nukkitx.math.vector.Vector3i;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.blockentity.BlockEntityType;
import org.cloudburstmc.server.blockentity.Smoker;
import org.cloudburstmc.server.inventory.InventoryType;
import org.cloudburstmc.server.level.chunk.Chunk;
import org.cloudburstmc.server.utils.Identifier;

public class SmokerBlockEntity extends FurnaceBlockEntity implements Smoker {

    public SmokerBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position, InventoryType.SMOKER);
    }

    @Override
    public boolean isValid() {
        Identifier id = getBlockState().getType();
        return id == BlockTypes.SMOKER || id == BlockTypes.LIT_SMOKER;
    }

    @Override
    public float getBurnRate() {
        return 2.0f;
    }

    @Override
    protected void extinguishFurnace() {
        this.getLevel().setBlock(this.getPosition(), BlockState.get(BlockTypes.SMOKER).copyTrait(BlockTraits.FACING_DIRECTION, getBlockState()), true);
    }

    @Override
    protected void lightFurnace() {
        this.getLevel().setBlock(this.getPosition(), BlockState.get(BlockTypes.LIT_SMOKER).copyTrait(BlockTraits.FACING_DIRECTION, getBlockState()), true);
    }
}
