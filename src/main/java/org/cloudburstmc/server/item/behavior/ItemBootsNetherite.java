package org.cloudburstmc.server.item.behavior;

import org.cloudburstmc.server.utils.Identifier;

public class ItemBootsNetherite extends ItemArmor {

    public ItemBootsNetherite(Identifier id) {
        super(id);
    }

    @Override
    public int getTier() {
        return TIER_NETHERITE;
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
        return 481;
    }

    @Override
    public int getToughness() {
        return 3;
    }
}
