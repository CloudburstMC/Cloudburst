package org.cloudburstmc.api.util.data;

import lombok.RequiredArgsConstructor;
import org.cloudburstmc.api.util.Identifier;

import static org.cloudburstmc.api.block.BlockIds.*;

@RequiredArgsConstructor
public enum CopperType {
    RAW(COPPER_BLOCK, CUT_COPPER, CUT_COPPER_SLAB, CUT_COPPER_STAIRS),
    EXPOSED(EXPOSED_COPPER, EXPOSED_CUT_COPPER, EXPOSED_CUT_COPPER_SLAB, EXPOSED_CUT_COPPER_STAIRS),
    OXIDIZED(OXIDIZED_COPPER, OXIDIZED_CUT_COPPER, OXIDIZED_CUT_COPPER_SLAB, OXIDIZED_CUT_COPPER_STAIRS),
    WAXED(WAXED_COPPER, WAXED_CUT_COPPER, WAXED_CUT_COPPER_SLAB, WAXED_CUT_COPPER_STAIRS),
    WAXED_EXPOSED(WAXED_EXPOSED_COPPER, WAXED_EXPOSED_CUT_COPPER, WAXED_EXPOSED_CUT_COPPER_SLAB, WAXED_EXPOSED_CUT_COPPER_STAIRS),
    WAXED_OXIDIZED(WAXED_OXIDIZED_COPPER, WAXED_OXIDIZED_CUT_COPPER, WAXED_OXIDIZED_CUT_COPPER_SLAB, WAXED_OXIDIZED_CUT_COPPER_STAIRS),
    WAXED_WEATHERED(WAXED_WEATHERED_COPPER, WAXED_WEATHERED_CUT_COPPER, WAXED_WEATHERED_CUT_COPPER_SLAB, WAXED_WEATHERED_CUT_COPPER_STAIRS),
    WEATHERED(WEATHERED_COPPER, WEATHERED_CUT_COPPER, WEATHERED_CUT_COPPER_SLAB, WEATHERED_CUT_COPPER_STAIRS);

    private final Identifier blockId;
    private final Identifier cutId;
    private final Identifier slabId;
    private final Identifier stairsId;

    public Identifier getBlockId() {
        return blockId;
    }

    public Identifier getCutId() {
        return cutId;
    }

    public Identifier getSlabId() {
        return slabId;
    }

    public Identifier getStairsId() {
        return stairsId;
    }

}
