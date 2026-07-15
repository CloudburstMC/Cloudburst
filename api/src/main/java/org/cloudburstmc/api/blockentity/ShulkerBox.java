package org.cloudburstmc.api.blockentity;

import org.cloudburstmc.api.inventory.view.BlockStorageView;
import org.cloudburstmc.api.util.Direction;

/**
 * A shulker box block entity with 27 storage slots that retains its inventory when broken.
 */
public interface ShulkerBox extends BlockEntity, BlockStorageView {

    /**
     * @return the direction in which the lid opens
     */
    Direction getFacing();

    /**
     * @return the current lid animation state
     */
    ShulkerBoxAnimationState getAnimationState();

    /**
     * @return lid progress from fully closed ({@code 0}) to fully open ({@code 1})
     */
    float getOpenProgress();

    /**
     * @return previous-tick lid progress, used for render interpolation
     */
    float getPreviousOpenProgress();

    /**
     * @return the number of non-spectator viewers holding the container open
     */
    int getViewerCount();
}
