package org.cloudburstmc.api.block.data;

import org.cloudburstmc.api.block.BlockState;

/**
 * A cluster of candles with a shared lit state.
 */
public interface Candle extends Lightable {

    /**
     * @return the number of candles
     */
    int getCandles();

    /**
     * @return the smallest supported candle count
     */
    int getMinimumCandles();

    /**
     * @return the greatest supported candle count
     */
    int getMaximumCandles();

    /**
     * @param candles the replacement count within the supported bounds
     * @return a state with the requested number of candles
     * @throws IllegalArgumentException if the count is outside the supported range
     */
    BlockState withCandles(int candles);
}
