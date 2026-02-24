package org.cloudburstmc.api.blockentity;

import org.cloudburstmc.api.inventory.view.BlockStorageView;

/**
 * A chest block entity with 27 storage slots. Two adjacent chests can be paired to form a double chest.
 */
public interface Chest extends BlockEntity, BlockStorageView {

    boolean isFindable();

    void setFindable(boolean findable);

    boolean isPaired();

    Chest getPair();

    boolean pairWith(Chest chest);

    boolean unpair();
}
