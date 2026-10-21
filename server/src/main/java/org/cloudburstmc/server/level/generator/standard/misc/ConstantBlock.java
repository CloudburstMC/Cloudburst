package org.cloudburstmc.server.level.generator.standard.misc;

import com.fasterxml.jackson.annotation.JsonCreator;
import net.daporkchop.lib.random.PRandom;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.server.block.BlockStateParser;
import org.cloudburstmc.server.level.generator.standard.misc.filter.BlockFilter;
import org.cloudburstmc.server.level.generator.standard.misc.selector.BlockSelector;
import org.jspecify.annotations.NonNull;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.util.Objects;
import java.util.random.RandomGenerator;
import java.util.stream.Stream;

/**
 * Selects and matches one block state with a selection weight of one.
 */
@JsonDeserialize
public record ConstantBlock(BlockState state) implements BlockFilter, BlockSelector, BlockSelector.Entry {

    @JsonCreator(mode = JsonCreator.Mode.DISABLED)
    public ConstantBlock {
        Objects.requireNonNull(state, "state");
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static ConstantBlock parse(String value) {
        return new ConstantBlock(BlockStateParser.parse(value));
    }

    @Override
    public boolean test(BlockState state) {
        return this.state == state;
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public BlockState get(int index) {
        Objects.checkIndex(index, 1);
        return this.state;
    }

    @Override
    public BlockState select(@NonNull PRandom random) {
        return this.state;
    }

    @Override
    public Stream<BlockState> states() {
        return Stream.of(this.state);
    }

    @Override
    public int sizeWeighted() {
        return size();
    }

    @Override
    public BlockState getWeighted(int index) {
        return get(index);
    }

    @Override
    public BlockState selectWeighted(@NonNull RandomGenerator random) {
        return this.state;
    }

    @Override
    public Stream<Entry> entries() {
        return Stream.of(this);
    }

    @Override
    public int weight() {
        return 1;
    }
}
