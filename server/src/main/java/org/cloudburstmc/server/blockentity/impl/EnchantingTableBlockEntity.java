package org.cloudburstmc.server.blockentity.impl;

import com.nukkitx.math.vector.Vector3i;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.blockentity.BlockEntityType;
import org.cloudburstmc.server.blockentity.EnchantingTable;
import org.cloudburstmc.server.level.chunk.Chunk;

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
        return getBlockState().getType() == BlockTypes.ENCHANTING_TABLE;
    }

}
