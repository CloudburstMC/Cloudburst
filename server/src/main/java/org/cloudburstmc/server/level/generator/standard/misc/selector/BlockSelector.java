package org.cloudburstmc.server.level.generator.standard.misc.selector;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.NonNull;
import net.daporkchop.lib.random.PRandom;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.server.level.generator.standard.misc.ConstantBlock;

import java.util.random.RandomGenerator;
import java.util.stream.Stream;

/**
 * Allows selecting a block from a pool of options.
 *
 * @author DaPorkchop_
 * @see ConstantBlock
 */
@JsonDeserialize(using = BlockSelectorDeserializer.class)
public interface BlockSelector {
    /**
     * @return the total number of {@link BlockState}s that this block selector can choose from
     */
    int size();

    /**
     * Gets the {@link BlockState} at the given index.
     *
     * @param index the index of the {@link BlockState} to get
     * @return the {@link BlockState} at the given index
     * @throws IndexOutOfBoundsException if the given index is outside of the range [0-{@link #size()})
     */
    BlockState get(int index);

    /**
     * Selects a random {@link BlockState}.
     *
     * @param random the instance of {@link PRandom} to use for generating random numbers
     * @return the selected {@link BlockState}
     */
    BlockState select(@NonNull PRandom random);

    /**
     * @return all of the {@link BlockState}s that this instance can select from
     */
    Stream<BlockState> states();

    /**
     * @return the total number of {@link BlockState}s that this block selector can choose from, taking selection weights into account
     */
    int sizeWeighted();

    /**
     * Gets the {@link BlockState} at the given index, taking selection weights into account.
     *
     * @param index the index of the {@link BlockState} to get
     * @return the {@link BlockState} at the given index
     * @throws IndexOutOfBoundsException if the given index is outside of the range [0-{@link #sizeWeighted()})
     */
    BlockState getWeighted(int index);

    /**
     * Selects a random block, taking selection weights into account.
     *
     * @param random the instance of {@link PRandom} to use for generating random numbers
     * @return the selected {@link BlockState}
     */
    BlockState selectWeighted(@NonNull RandomGenerator random);

    /**
     * @return all of the {@link BlockState}s that this instance can select from, taking selection weights into account
     */
    Stream<Entry> entries();

    /**
     * Represents a single weighted {@link BlockState} in a {@link BlockSelector}.
     *
     * @author DaPorkchop_
     */
    interface Entry {
        /**
         * @return the {@link BlockState} used by this entry
         */
        BlockState state();

        /**
         * @return this entry's random selection weight
         */
        int weight();
    }
}
