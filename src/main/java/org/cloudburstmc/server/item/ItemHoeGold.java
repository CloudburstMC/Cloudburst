package org.cloudburstmc.server.item;

import org.cloudburstmc.server.utils.Identifier;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class ItemHoeGold extends ItemTool {

    public ItemHoeGold(Identifier id) {
        super(id);
    }

    @Override
    public int getMaxDurability() {
        return DURABILITY_GOLD;
    }

    @Override
    public boolean isHoe() {
        return true;
    }

    @Override
    public int getTier() {
        return TIER_GOLD;
    }
}
