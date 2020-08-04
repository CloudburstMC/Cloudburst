package org.cloudburstmc.server.level.generator.standard.misc;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.lib.random.PRandom;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.level.generator.standard.StandardGeneratorUtils;
import org.cloudburstmc.server.level.generator.standard.misc.filter.BlockFilter;
import org.cloudburstmc.server.level.generator.standard.misc.selector.BlockSelector;
import org.cloudburstmc.server.registry.BlockRegistry;
import org.cloudburstmc.server.utils.Identifier;

/**
 * Represents a constant block configuration option.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@JsonDeserialize
public final class ConstantBlock implements BlockFilter, BlockSelector {
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
        this(StandardGeneratorUtils.parseState(value));
    }

    public BlockState state() {
        return this.state;
    }

    //block filter methods
    @Override
    public boolean test(BlockState state) {
        return this.state == state;
    }

    //block selector methods
    @Override
    public BlockState select(PRandom random) {
        return this.state;
    }
}
