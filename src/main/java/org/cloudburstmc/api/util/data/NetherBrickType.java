package org.cloudburstmc.api.util.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.block.BlockIds;

@RequiredArgsConstructor
@Getter
public enum NetherBrickType {
    DEFAULT(BlockIds.NETHER_BRICK),
    RED(BlockIds.RED_NETHER_BRICK),
    CHISELED(BlockIds.CHISELED_NETHER_BRICKS),
    CRACKED(BlockIds.CRACKED_NETHER_BRICKS);

    private final Identifier id;
}
