package org.cloudburstmc.server.level.generator.standard.misc.selector;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.lib.random.PRandom;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.level.generator.standard.misc.ConstantBlock;
import org.cloudburstmc.server.registry.BlockRegistry;

import java.util.Arrays;

/**
 * Implementation of {@link BlockSelector} which selects a block from a pool of options.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public final class MultiBlockSelector implements BlockSelector {
    @NonNull
    protected final BlockState[] states;

    protected MultiBlockSelector(@NonNull ConstantBlock[] blocks) {
        this(Arrays.stream(blocks)
                .map(ConstantBlock::state)
                .toArray(BlockState[]::new));
    }

    @Override
    public BlockState select(@NonNull PRandom random) {
        return this.states[random.nextInt(this.states.length)];
    }
}
