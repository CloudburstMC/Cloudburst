package org.cloudburstmc.server.item;

import org.cloudburstmc.server.utils.Identifier;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class ItemShears extends ItemTool {

    public ItemShears(Identifier id) {
        super(id);
    }

    @Override
    public int getMaxDurability() {
        return DURABILITY_SHEARS;
    }

    @Override
    public boolean isShears() {
        return true;
    }
}
