package org.cloudburstmc.server.item.behavior;

import org.cloudburstmc.server.utils.Identifier;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class ItemBootsChain extends ItemArmor {

    public ItemBootsChain(Identifier id) {
        super(id);
    }

    @Override
    public int getTier() {
        return TIER_CHAIN;
    }

    @Override
    public boolean isBoots() {
        return true;
    }

    @Override
    public int getArmorPoints() {
        return 1;
    }

    @Override
    public int getMaxDurability() {
        return 196;
    }
}
