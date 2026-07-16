package org.cloudburstmc.api.block;

import lombok.experimental.UtilityClass;

/**
 * Server access to the block-layer representation of liquid states.
 */
@UtilityClass
public class LiquidStateAccess {

    public static BlockState blockState(LiquidState liquid) {
        return liquid.blockState();
    }
}
