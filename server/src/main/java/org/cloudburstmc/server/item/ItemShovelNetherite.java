package org.cloudburstmc.server.item;

import org.cloudburstmc.server.utils.Identifier;

public class ItemShovelNetherite extends ItemTool {

    public ItemShovelNetherite(Identifier id) {
        super(id);
    }

    @Override
    public int getMaxDurability() {
        return DURABILITY_NETHERITE;
    }

    @Override
    public boolean isShovel() {
        return true;
    }

    @Override
    public int getTier() {
        return TIER_NETHERITE;
    }

    @Override
    public int getAttackDamage() {
        return 6;
    }
}
