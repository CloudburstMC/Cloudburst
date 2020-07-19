package org.cloudburstmc.server.item;

import org.cloudburstmc.server.utils.Identifier;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class ItemMushroomStew extends ItemEdible {

    public ItemMushroomStew(Identifier id) {
        super(id);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }
}
