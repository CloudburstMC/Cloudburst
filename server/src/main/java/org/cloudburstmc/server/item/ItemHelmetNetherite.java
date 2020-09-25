package org.cloudburstmc.server.item;

import org.cloudburstmc.server.utils.Identifier;

public class ItemHelmetNetherite extends ItemArmor {

    public ItemHelmetNetherite(Identifier id) {
        super(id);
    }

    @Override
    public int getTier() {
        return ItemArmor.TIER_NETHERITE;
    }

    @Override
    public boolean isHelmet() {
        return true;
    }

    @Override
    public int getArmorPoints() {
        return 3;
    }

    @Override
    public int getMaxDurability() {
        return 407;
    }

    @Override
    public int getToughness() {
        return 3;
    }
}
