package org.cloudburstmc.server.item;

import org.cloudburstmc.server.utils.Identifier;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class ItemBook extends Item {

    public ItemBook(Identifier id) {
        super(id);
    }

    @Override
    public int getEnchantAbility() {
        return 1;
    }
}
