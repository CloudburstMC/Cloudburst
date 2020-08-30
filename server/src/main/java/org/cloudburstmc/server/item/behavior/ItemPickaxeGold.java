package org.cloudburstmc.server.item.behavior;

import org.cloudburstmc.server.utils.Identifier;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class ItemPickaxeGold extends ItemTool {

    public ItemPickaxeGold(Identifier id) {
        super(id);
    }

    @Override
    public int getMaxDurability() {
        return DURABILITY_GOLD;
    }

    @Override
    public boolean isPickaxe() {
        return true;
    }

    @Override
    public int getTier() {
        return TIER_GOLD;
    }

    @Override
    public int getAttackDamage() {
        return 2;
    }
}
