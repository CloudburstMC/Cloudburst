package org.cloudburstmc.server.utils.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.server.block.BlockIds;
import org.cloudburstmc.server.utils.Identifier;

@RequiredArgsConstructor
@Getter
public enum NetherBrickType {
    DEFAULT(BlockIds.NETHER_BRICK),
    RED(BlockIds.RED_NETHER_BRICK),
    CHISELED(BlockIds.CHISELED_NETHER_BRICKS),
    CRACKED(BlockIds.CRACKED_NETHER_BRICKS);

    private final Identifier id;
}
