package org.cloudburstmc.server.level.generator.standard.misc.filter;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.cloudburstmc.api.block.BlockBehaviors;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.server.registry.CloudBlockRegistry;

import java.util.function.Predicate;

/**
 * Checks if a given block type is valid. Used e.g. to see if a given block may be replaced.
 *
 * @author DaPorkchop_
 */
@JsonDeserialize(using = BlockFilterDeserializer.class)
public interface BlockFilter extends Predicate<BlockState> {
    BlockFilter AIR = state -> state == BlockStates.AIR;

    BlockFilter REPLACEABLE = state -> CloudBlockRegistry.REGISTRY.getBehavior(state.getType(), BlockBehaviors.IS_REPLACEABLE);

    @Override
    boolean test(BlockState state);
}
