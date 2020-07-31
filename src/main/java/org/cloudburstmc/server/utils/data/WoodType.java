package org.cloudburstmc.server.utils.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.server.utils.BlockColor;

@RequiredArgsConstructor
@Getter
public enum WoodType {
    OAK(BlockColor.WOOD_BLOCK_COLOR),
    BIRCH(BlockColor.SAND_BLOCK_COLOR),
    SPRUCE(BlockColor.SPRUCE_BLOCK_COLOR),
    JUNGLE(BlockColor.DIRT_BLOCK_COLOR),
    ACACIA(BlockColor.ORANGE_BLOCK_COLOR),
    DARK_OAK(BlockColor.BROWN_BLOCK_COLOR);

    private final BlockColor color;
}
