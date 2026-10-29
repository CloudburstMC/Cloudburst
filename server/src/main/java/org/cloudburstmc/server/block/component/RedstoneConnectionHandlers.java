package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.component.RedstoneConnectionHandler;

@UtilityClass
public class RedstoneConnectionHandlers {

    public static final RedstoneConnectionHandler ALL_SIDES = (block, direction) -> direction != null;

    public static final RedstoneConnectionHandler REPEATER = (block, direction) -> direction != null
            && block.getState().ensureTrait(BlockTraits.CARDINAL_DIRECTION).toDirection().getAxis() == direction.getAxis();

    public static final RedstoneConnectionHandler OBSERVER = (block, direction) -> direction != null
            && block.getState().ensureTrait(BlockTraits.FACING_DIRECTION) == direction;

    public static final RedstoneConnectionHandler WIRE = (block, direction) -> true;
}
