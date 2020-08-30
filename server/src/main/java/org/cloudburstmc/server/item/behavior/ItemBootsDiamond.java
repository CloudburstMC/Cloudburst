package org.cloudburstmc.server.item.behavior;

import org.cloudburstmc.server.utils.Identifier;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class ItemBootsDiamond extends ItemArmor {

    public ItemBootsDiamond(Identifier id) {
        super(id);
    }

    @Override
    public int getTier() {
        return TIER_DIAMOND;
    }

    @Override
    public boolean isBoots() {
        return true;
    }

    @Override
    public int getArmorPoints() {
        return 3;
    }

    @Override
    public int getMaxDurability() {
        return 430;
    }

    @Override
    public int getToughness() {
        return 2;
    }
}
