package org.cloudburstmc.server.level.generator.standard.misc.filter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.level.generator.standard.misc.ConstantBlock;
import org.cloudburstmc.server.registry.BlockRegistry;

import java.util.Arrays;

/**
 * Implementation of {@link BlockFilter} which checks if the block is contained in a list of IDs.
 *
 * @author DaPorkchop_
 */
@JsonDeserialize
public final class AnyOfBlockFilter implements BlockFilter {
    final int[] runtimeIds;

    @JsonCreator
    public AnyOfBlockFilter(ConstantBlock[] blocks) {
        this.runtimeIds = Arrays.stream(blocks)
                .mapToInt(ConstantBlock::runtimeId)
                .distinct()
                .sorted()
                .toArray();
    }

    @JsonCreator
    public AnyOfBlockFilter(String value) {
        this.runtimeIds = Arrays.stream(value.split(","))
                .map(ConstantBlock::new)
                .mapToInt(ConstantBlock::runtimeId)
                .distinct()
                .sorted()
                .toArray();
    }

    @Override
    public boolean test(BlockState blockState) {
        return this.test(BlockRegistry.get().getRuntimeId(blockState));
    }

    @Override
    public boolean test(int runtimeId) {
        return Arrays.binarySearch(this.runtimeIds, runtimeId) >= 0;
    }
}
