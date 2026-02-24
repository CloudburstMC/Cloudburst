package org.cloudburstmc.api.blockentity;

import org.cloudburstmc.api.inventory.view.BlockStorageView;

/**
 * A shulker box block entity with 27 storage slots that retains its inventory when broken.
 */
public interface ShulkerBox extends BlockEntity, BlockStorageView {
}
