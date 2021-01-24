package org.cloudburstmc.server.blockentity.impl;

import com.nukkitx.math.vector.Vector3i;
import org.cloudburstmc.server.block.BlockIds;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.blockentity.BlockEntityType;
import org.cloudburstmc.server.blockentity.Smoker;
import org.cloudburstmc.server.inventory.InventoryType;
import org.cloudburstmc.server.world.chunk.Chunk;
import org.cloudburstmc.server.utils.Identifier;

public class SmokerBlockEntity extends FurnaceBlockEntity implements Smoker {

    public SmokerBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position, InventoryType.SMOKER);
    }

    @Override
    public boolean isValid() {
        Identifier id = getBlockState().getType();
        return id == BlockIds.SMOKER || id == BlockIds.LIT_SMOKER;
    }

    @Override
    public float getBurnRate() {
        return 2.0f;
    }

    @Override
    protected void extinguishFurnace() {
        this.getWorld().setBlock(this.getPosition(), BlockState.get(BlockIds.SMOKER).copyTrait(BlockTraits.FACING_DIRECTION, getBlockState()), true);
    }

    @Override
    protected void lightFurnace() {
        this.getWorld().setBlock(this.getPosition(), BlockState.get(BlockIds.LIT_SMOKER).copyTrait(BlockTraits.FACING_DIRECTION, getBlockState()), true);
    }
}
