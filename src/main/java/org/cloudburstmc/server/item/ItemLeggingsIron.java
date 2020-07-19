package org.cloudburstmc.server.item;

import org.cloudburstmc.server.utils.Identifier;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class ItemLeggingsIron extends ItemArmor {

    public ItemLeggingsIron(Identifier id) {
        super(id);
    }

    @Override
    public int getTier() {
        return TIER_IRON;
    }

    @Override
    public boolean isLeggings() {
        return true;
    }

    @Override
    public int getArmorPoints() {
        return 5;
    }

    @Override
    public int getMaxDurability() {
        return 226;
    }
}
