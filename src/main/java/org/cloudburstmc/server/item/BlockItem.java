package org.cloudburstmc.server.item;

import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.block.util.BlockStateMetaMappings;
import org.cloudburstmc.server.utils.Identifier;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class BlockItem extends Item {

    private final BlockState blockState;

    public BlockItem(BlockState blockState) {
        super(blockState.getType());
        this.blockState = blockState;
    }

    public BlockState getBlock() {
        return blockState;
    }

    @Override
    public void setMeta(int meta) {
        if ((meta & 0xffff) == 0xffff || BlockStateMetaMappings.hasMeta(getId(), meta)) {
            super.setMeta(meta);
        } else {
            super.setMeta(0);
        }
    }

    @Override
    public int getMaxStackSize() {
        //Shulker boxes don't stack!
        Identifier id = this.getId();
        if (id == BlockTypes.SHULKER_BOX || id == BlockTypes.UNDYED_SHULKER_BOX) {
            return 1;
        }

        return super.getMaxStackSize();
    }
}
