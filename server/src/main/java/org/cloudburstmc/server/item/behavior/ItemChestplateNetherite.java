package org.cloudburstmc.server.item.behavior;

import org.cloudburstmc.server.utils.Identifier;

public class ItemChestplateNetherite extends ItemArmor {

    public ItemChestplateNetherite(Identifier id) {
        super(id);
    }

    @Override
    public int getTier() {
        return TIER_NETHERITE;
    }

    @Override
    public boolean isChestplate() {
        return true;
    }

    @Override
    public int getArmorPoints() {
        return 8;
    }

    @Override
    public int getMaxDurability() {
        return 592;
    }

    @Override
    public int getToughness() {
        return 4;
    }
}
