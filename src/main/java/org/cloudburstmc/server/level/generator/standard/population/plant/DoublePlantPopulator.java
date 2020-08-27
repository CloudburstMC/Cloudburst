package org.cloudburstmc.server.level.generator.standard.population.plant;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import net.daporkchop.lib.random.PRandom;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockStates;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.level.ChunkManager;
import org.cloudburstmc.server.level.chunk.IChunk;
import org.cloudburstmc.server.level.generator.standard.StandardGenerator;
import org.cloudburstmc.server.level.generator.standard.misc.filter.BlockFilter;
import org.cloudburstmc.server.utils.Identifier;
import org.cloudburstmc.server.utils.data.DoublePlantType;

import java.util.Objects;

/**
 * Places patches of double plants in the world.
 *
 * @author DaPorkchop_
 */
@JsonDeserialize
public class DoublePlantPopulator extends AbstractPlantPopulator {
    public static final Identifier ID = Identifier.fromString("cloudburst:double_plant");

    @JsonProperty
    @JsonAlias("type")
    protected DoublePlantType[] types;

    @Override
    protected void init0(long levelSeed, long localSeed, StandardGenerator generator) {
        super.init0(levelSeed, localSeed, generator);

        Objects.requireNonNull(this.types, "type must be set!");
    }

    @Override
    protected void placeCluster(PRandom random, ChunkManager level, int x, int y, int z) {
        final BlockFilter on = this.on;
        final BlockFilter replace = this.replace;

        DoublePlantType type = this.types[random.nextInt(this.types.length)];
        final BlockState state = BlockStates.DOUBLE_PLANT.withTrait(BlockTraits.DOUBLE_PLANT_TYPE, type);
        final BlockState bottom = state.withTrait(BlockTraits.IS_UPPER_BLOCK, false);
        final BlockState top = state.withTrait(BlockTraits.IS_UPPER_BLOCK, true);

        for (int i = this.patchSize - 1; i >= 0; i--) {
            int blockY = y + random.nextInt(4) - random.nextInt(4);
            if (blockY < 0 || blockY >= 254) {
                continue;
            }
            int blockX = x + random.nextInt(8) - random.nextInt(8);
            int blockZ = z + random.nextInt(8) - random.nextInt(8);

            IChunk chunk = level.getChunk(blockX >> 4, blockZ >> 4);
            if (on.test(chunk.getBlock(blockX & 0xF, blockY, blockZ & 0xF, 0))
                    && replace.test(chunk.getBlock(blockX & 0xF, blockY + 1, blockZ & 0xF, 0))
                    && replace.test(chunk.getBlock(blockX & 0xF, blockY + 2, blockZ & 0xF, 0))) {
                chunk.setBlock(blockX & 0xF, blockY + 1, blockZ & 0xF, 0, bottom);
                chunk.setBlock(blockX & 0xF, blockY + 2, blockZ & 0xF, 0, top);
            }
        }
    }

    @Override
    public Identifier getId() {
        return ID;
    }
}
