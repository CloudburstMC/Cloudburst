package org.cloudburstmc.server.level.light;

import org.cloudburstmc.api.block.BlockState;

/**
 * Block and light view over the chunks locked for one pass.
 *
 * <p>{@code isLoaded} takes block coordinates and is false when the chunk is
 * absent. Callers only write positions in loaded chunks with y in
 * {@code [minHeight, maxHeight)}; reads outside return air or 0, including
 * the block states at {@code maxHeight}, which the sky rules peek at above
 * the top layer. Writes outside are ignored.
 */
interface LightAccess {

    int minHeight();

    int maxHeight();

    boolean isLoaded(int x, int z);

    BlockState primary(int x, int y, int z);

    BlockState secondary(int x, int y, int z);

    int light(int x, int y, int z);

    void setLight(int x, int y, int z, int light);

    LightStateCache states();
}
