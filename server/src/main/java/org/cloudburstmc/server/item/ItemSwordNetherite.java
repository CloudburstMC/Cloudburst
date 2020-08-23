package org.cloudburstmc.server.item;

import org.cloudburstmc.server.utils.Identifier;

public class ItemSwordNetherite extends ItemTool {

    public ItemSwordNetherite(Identifier id) {
        super(id);
    }

    @Override
    public int getMaxDurability() {
        return ItemTool.DURABILITY_NETHERITE;
    }

    @Override
    public boolean isSword() {
        return true;
    }

    @Override
    public int getTier() {
        return ItemTool.TIER_NETHERITE;
    }

    @Override
    public int getAttackDamage() {
        return 9;
    }
}
