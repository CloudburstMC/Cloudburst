package org.cloudburstmc.api.inventory.view;

import org.cloudburstmc.api.blockentity.Lectern;

/**
 * A lectern view with access to the block and its lectern state.
 */
public interface BlockLecternView extends LecternView, BlockSlotGroup<Lectern> {
}
