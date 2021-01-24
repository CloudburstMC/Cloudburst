package org.cloudburstmc.server.blockentity.impl;

import com.nukkitx.math.vector.Vector3i;
import org.cloudburstmc.server.block.BlockIds;
import org.cloudburstmc.server.blockentity.BlockEntityType;
import org.cloudburstmc.server.blockentity.EnderChest;
import org.cloudburstmc.server.world.chunk.Chunk;

public class EnderChestBlockEntity extends ChestBlockEntity implements EnderChest {

    public EnderChestBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    public boolean isValid() {
        return this.getBlockState().getType() == BlockIds.ENDER_CHEST;
    }

    @Override
    public boolean isSpawnable() {
        return true;
    }
}
