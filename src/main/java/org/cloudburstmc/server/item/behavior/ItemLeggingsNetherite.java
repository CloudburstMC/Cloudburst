package org.cloudburstmc.server.item.behavior;

import org.cloudburstmc.server.utils.Identifier;

public class ItemLeggingsNetherite extends ItemArmor {

    public ItemLeggingsNetherite(Identifier id) {
        super(id);
    }

    @Override
    public boolean isLeggings() {
        return true;
    }

    @Override
    public int getTier() {
        return ItemArmor.TIER_NETHERITE;
    }

    @Override
    public int getArmorPoints() {
        return 6;
    }

    @Override
    public int getMaxDurability() {
        return 555;
    }

    @Override
    public int getToughness() {
        return 3;
    }
}
