package org.cloudburstmc.server.item.behavior;

import org.cloudburstmc.server.utils.Identifier;

public class ItemPickaxeNetherite extends ItemTool {

    public ItemPickaxeNetherite(Identifier id) {
        super(id);
    }

    @Override
    public int getMaxDurability() {
        return DURABILITY_NETHERITE;
    }

    @Override
    public boolean isPickaxe() {
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
