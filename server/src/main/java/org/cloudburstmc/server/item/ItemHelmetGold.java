package org.cloudburstmc.server.item;

import org.cloudburstmc.server.utils.Identifier;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class ItemHelmetGold extends ItemArmor {

    public ItemHelmetGold(Identifier id) {
        super(id);
    }

    @Override
    public int getTier() {
        return TIER_GOLD;
    }

    @Override
    public boolean isHelmet() {
        return true;
    }

    @Override
    public int getArmorPoints() {
        return 2;
    }

    @Override
    public int getMaxDurability() {
        return 78;
    }
}
