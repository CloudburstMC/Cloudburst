package org.cloudburstmc.server.item.behavior;

import org.cloudburstmc.server.utils.Identifier;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class ItemChestplateChain extends ItemArmor {

    public ItemChestplateChain(Identifier id) {
        super(id);
    }

    @Override
    public int getTier() {
        return TIER_CHAIN;
    }

    @Override
    public boolean isChestplate() {
        return true;
    }

    @Override
    public int getArmorPoints() {
        return 5;
    }

    @Override
    public int getMaxDurability() {
        return 241;
    }
}
