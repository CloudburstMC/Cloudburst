package org.cloudburstmc.server.item.behavior;

import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.item.ItemFactory;
import org.cloudburstmc.server.utils.Identifier;

public class SignItem extends Item {
    private final Identifier blockId;

    private SignItem(Identifier id, Identifier blockId) {
        super(id);
        this.blockId = blockId;
    }

    public static ItemFactory factory(Identifier blockId) {
        return identifier -> new SignItem(identifier, blockId);
    }

    @Override
    public BlockState getBlock() {
        return BlockState.get(blockId);
    }

    @Override
    public int getMaxStackSize() {
        return 16;
    }
}
