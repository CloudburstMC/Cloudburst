package org.cloudburstmc.server.blockentity.impl;

import com.nukkitx.math.vector.Vector3i;
import org.cloudburstmc.server.block.BlockIds;
import org.cloudburstmc.server.blockentity.BlockEntityType;
import org.cloudburstmc.server.blockentity.EnchantingTable;
import org.cloudburstmc.server.world.chunk.Chunk;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class EnchantingTableBlockEntity extends BaseBlockEntity implements EnchantingTable {

    public EnchantingTableBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    public boolean isValid() {
        return getBlockState().getType() == BlockIds.ENCHANTING_TABLE;
    }

}
