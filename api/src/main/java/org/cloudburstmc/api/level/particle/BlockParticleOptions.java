package org.cloudburstmc.api.level.particle;

import org.cloudburstmc.api.block.BlockState;

import java.util.Objects;

/**
 * Renders particles using the texture of a block state.
 *
 * @param type  the block-textured particle type
 * @param state the block state supplying the texture
 */
public record BlockParticleOptions(ParticleType type, BlockState state) implements ParticleOptions {

    public BlockParticleOptions {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(state, "state");
    }

    @Override
    public ParticleType getType() {
        return this.type;
    }
}
