package org.cloudburstmc.server.level.generator.standard.misc.filter;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.server.block.BlockStateParser;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.util.Arrays;

/**
 * Matches states selected by named expressions with optional property wildcards.
 */
@JsonDeserialize
public final class AnyOfBlockFilter extends ReferenceOpenHashSet<BlockState> implements BlockFilter {

    @JsonCreator
    public AnyOfBlockFilter(String[] values) {
        Arrays.stream(values)
                .flatMap(BlockStateParser::parseMatching)
                .forEach(this::add);
    }

    @Override
    public boolean test(BlockState blockState) {
        return super.contains(blockState);
    }
}
