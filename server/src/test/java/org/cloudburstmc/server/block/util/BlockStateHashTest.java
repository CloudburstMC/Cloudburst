package org.cloudburstmc.server.block.util;

import org.cloudburstmc.nbt.NbtMap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BlockStateHashTest {

    @Test
    void usesReservedHashForUnknownBlock() {
        assertEquals(-2, BlockStateHash.compute("minecraft:unknown", NbtMap.EMPTY));
    }
}
