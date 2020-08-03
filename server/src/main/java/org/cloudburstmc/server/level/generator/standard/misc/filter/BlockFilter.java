package org.cloudburstmc.server.level.generator.standard.misc.filter;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.registry.BlockRegistry;

import java.util.function.Predicate;

/**
 * Checks if a given block type is valid. Used e.g. to see if a given block may be replaced.
 *
 * @author DaPorkchop_
 */
@JsonDeserialize(using = BlockFilterDeserializer.class)
public interface BlockFilter extends Predicate<BlockState> {
    BlockFilter AIR = new BlockFilter() {
        @Override
        public boolean test(BlockState blockState) {
            return blockState.getType() == BlockTypes.AIR;
        }

        @Override
        public boolean test(int runtimeId) {
            return runtimeId == 0;
        }
    };

    BlockFilter REPLACEABLE = new BlockFilter() {
        @Override
        public boolean test(BlockState blockState) {
            return false;
        }

        @Override
        public boolean test(int runtimeId) {
            return runtimeId == 0 || this.test(BlockRegistry.get().getBlock(runtimeId));
        }
    };

    @Override
    boolean test(BlockState blockState);

    /**
     * Checks if the given runtime ID matches this filter.
     *
     * @param runtimeId the runtime ID to check
     * @return whether or not the given runtime ID matches this filter
     */
    boolean test(int runtimeId);
}
