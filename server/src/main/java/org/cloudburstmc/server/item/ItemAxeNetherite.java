package org.cloudburstmc.server.item;

import org.cloudburstmc.server.item.behavior.ItemTool;
import org.cloudburstmc.server.utils.Identifier;

public class ItemAxeNetherite extends ItemTool {

    public ItemAxeNetherite(Identifier id) {
        super(id);
    }

    @Override
    public int getMaxDurability() {
        return DURABILITY_NETHERITE;
    }

    @Override
    public boolean isAxe() {
        return true;
    }

    @Override
    public int getTier() {
        return TIER_NETHERITE;
    }

    @Override
    public int getAttackDamage() {
        return 7;
    }
}
