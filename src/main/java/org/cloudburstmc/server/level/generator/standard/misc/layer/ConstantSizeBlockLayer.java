package org.cloudburstmc.server.level.generator.standard.misc.layer;

import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;
import net.daporkchop.lib.common.util.PValidation;
import net.daporkchop.lib.random.PRandom;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.registry.BlockRegistry;

/**
 * A {@link BlockLayer} with a constant size.
 *
 * @author DaPorkchop_
 */
@Accessors(fluent = true)
public final class ConstantSizeBlockLayer implements BlockLayer {
    @Getter
    private final int runtimeId;
    private final int size;

    public ConstantSizeBlockLayer(@NonNull BlockState blockState, int size) {
        this.runtimeId = BlockRegistry.get().getRuntimeId(blockState);
        this.size = PValidation.ensurePositive(size);
    }

    public ConstantSizeBlockLayer(int runtimeId, int size) {
        BlockRegistry.get().getBlock(runtimeId); //ensure runtimeId is valid
        this.runtimeId = runtimeId;
        this.size = PValidation.ensurePositive(size);
    }

    @Override
    public int size(PRandom random) {
        return this.size;
    }
}
