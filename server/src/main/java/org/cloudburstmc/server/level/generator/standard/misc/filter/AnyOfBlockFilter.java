package org.cloudburstmc.server.level.generator.standard.misc.filter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.util.BlockUtils;
import org.cloudburstmc.server.registry.BlockRegistry;

import java.util.Arrays;

/**
 * Implementation of {@link BlockFilter} which checks if the block is contained in a list of IDs.
 *
 * @author DaPorkchop_
 */
@JsonDeserialize
public final class AnyOfBlockFilter extends ReferenceOpenHashSet<BlockState> implements BlockFilter {
    @JsonCreator
    public AnyOfBlockFilter(String[] values) {
        Arrays.stream(values)
                .flatMap(value -> Arrays.stream(value.split(",")))
                .flatMap(BlockUtils::parseStateWildcard)
                .forEach(this::add);
    }

    @Override
    public boolean test(BlockState blockState) {
        return super.contains(BlockRegistry.get().getRuntimeId(blockState));
    }

    @AllArgsConstructor
    @JsonDeserialize
    private static final class SingleWildcard {
        @NonNull
        protected final BlockState[] states;

        @JsonCreator
        public SingleWildcard(String value) {
            this(Arrays.stream(value.split(","))
                    .flatMap(BlockUtils::parseStateWildcard)
                    .toArray(BlockState[]::new));
        }
    }
}
