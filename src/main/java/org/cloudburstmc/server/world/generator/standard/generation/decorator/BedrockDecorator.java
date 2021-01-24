package org.cloudburstmc.server.world.generator.standard.generation.decorator;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import net.daporkchop.lib.random.PRandom;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.world.chunk.IChunk;
import org.cloudburstmc.server.world.generator.standard.misc.AbstractGenerationPass;
import org.cloudburstmc.server.world.generator.standard.misc.ConstantBlock;
import org.cloudburstmc.server.world.generator.standard.misc.IntRange;
import org.cloudburstmc.server.utils.Identifier;

/**
 * Places a given block type using a vanilla bedrock pattern.
 *
 * @author DaPorkchop_
 */
@JsonDeserialize
public class BedrockDecorator extends AbstractGenerationPass implements Decorator {
    public static final Identifier ID = Identifier.fromString("cloudburst:bedrock");

    @JsonProperty(required = true)
    private ConstantBlock block;
    @JsonProperty(required = true)
    private IntRange base;

    @JsonProperty
    private IntRange fade = IntRange.EMPTY_RANGE;
    @JsonProperty
    private boolean reverseFade = false;

    @Override
    public void decorate(PRandom random, IChunk chunk, int x, int z) {
        final BlockState state = this.block.state();

        for (int y = this.base.min, max = this.base.max; y < max; y++) {
            chunk.setBlock(x, y, z, 0, state);
        }

        if (!this.fade.empty()) {
            if (this.reverseFade) {
                for (int y = this.fade.min, i = 1, size = this.fade.size() + 1; i < size; y++, i++) {
                    if (random.nextInt(size) < i) {
                        chunk.setBlock(x, y, z, 0, state);
                    }
                }
            } else {
                for (int y = this.fade.min, i = this.fade.size(), size = i + 1; i > 0; y++, i--) {
                    if (random.nextInt(size) < i) {
                        chunk.setBlock(x, y, z, 0, state);
                    }
                }
            }
        }
    }

    @Override
    public Identifier getId() {
        return ID;
    }
}
