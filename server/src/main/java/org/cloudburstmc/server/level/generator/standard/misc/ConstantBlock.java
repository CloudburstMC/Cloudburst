package org.cloudburstmc.server.level.generator.standard.misc;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.lib.random.PRandom;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.util.BlockUtils;
import org.cloudburstmc.server.level.generator.standard.misc.filter.BlockFilter;
import org.cloudburstmc.server.level.generator.standard.misc.selector.BlockSelector;
import org.cloudburstmc.server.registry.BlockRegistry;
import org.cloudburstmc.server.utils.Identifier;

import java.util.stream.Stream;

/**
 * Represents a constant block configuration option.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@JsonDeserialize
public final class ConstantBlock implements BlockFilter, BlockSelector, BlockSelector.Entry {
    @NonNull
    private final BlockState state;

    @JsonCreator
    public ConstantBlock(
            @JsonProperty(value = "id", required = true) Identifier id,
            @JsonProperty(value = "meta") @JsonAlias({
                    "damage",
                    "metadata"
            }) int meta) {
        this(BlockRegistry.get().getBlock(id, meta));
    }

    @JsonCreator
    public ConstantBlock(String value) {
        this(BlockUtils.parseState(value));
    }

    public BlockState state() {
        return this.state;
    }

    //BlockFilter

    @Override
    public boolean test(BlockState state) {
        return this.state == state;
    }

    //BlockSelector

    @Override
    public int size() {
        return 1;
    }

    @Override
    public BlockState get(int index) {
        Preconditions.checkElementIndex(index, 1);
        return this.state;
    }

    @Override
    public BlockState select(PRandom random) {
        return this.state;
    }

    @Override
    public Stream<BlockState> states() {
        return Stream.of(this.state);
    }

    @Override
    public int sizeWeighted() {
        return 1;
    }

    @Override
    public BlockState getWeighted(int index) {
        Preconditions.checkElementIndex(index, 1);
        return this.state;
    }

    @Override
    public BlockState selectWeighted(PRandom random) {
        return this.state;
    }

    @Override
    public Stream<Entry> entries() {
        return Stream.of(this);
    }

    //BlockSelector.Entry

    @Override
    public int weight() {
        return 1;
    }
}
