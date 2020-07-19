package org.cloudburstmc.server.item;

import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.utils.Identifier;

public class PlaceableItem extends Item {
    private final Identifier blockId;

    private PlaceableItem(Identifier id, Identifier blockId) {
        super(id);
        this.blockId = blockId;
    }

    public static ItemFactory factory(Identifier blockId) {
        return identifier -> new PlaceableItem(identifier, blockId);
    }

    @Override
    public BlockState getBlock() {
        return BlockState.get(blockId);
    }
}
