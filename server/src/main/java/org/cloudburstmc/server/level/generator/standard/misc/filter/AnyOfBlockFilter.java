package org.cloudburstmc.server.level.generator.standard.misc.filter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.level.generator.standard.StandardGeneratorUtils;
import org.cloudburstmc.server.level.generator.standard.misc.ConstantBlock;
import org.cloudburstmc.server.registry.BlockRegistry;

import java.util.Arrays;
import java.util.Collections;

/**
 * Implementation of {@link BlockFilter} which checks if the block is contained in a list of IDs.
 *
 * @author DaPorkchop_
 */
@JsonDeserialize
public final class AnyOfBlockFilter extends ReferenceOpenHashSet<BlockState> implements BlockFilter {
    public AnyOfBlockFilter(BlockState[] states) {
        Collections.addAll(this, states);
    }

    @JsonCreator
    public AnyOfBlockFilter(AnyOfBlockFilter[] filters) {
        for (AnyOfBlockFilter filter : filters)  {
            this.addAll(filter);
        }
    }

    @JsonCreator
    public AnyOfBlockFilter(String value) {
        this(Arrays.stream(value.split(","))
                .flatMap(StandardGeneratorUtils::parseStateWildcard)
                .toArray(BlockState[]::new));
    }

    @Override
    public boolean test(BlockState blockState) {
        return super.contains(BlockRegistry.get().getRuntimeId(blockState));
    }
}
