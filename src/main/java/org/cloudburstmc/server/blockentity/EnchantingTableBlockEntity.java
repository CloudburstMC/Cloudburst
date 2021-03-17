package org.cloudburstmc.server.blockentity;

import com.nukkitx.math.vector.Vector3i;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.EnchantingTable;
import org.cloudburstmc.api.level.chunk.Chunk;

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
