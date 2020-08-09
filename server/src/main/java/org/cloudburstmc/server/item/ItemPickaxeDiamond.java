package org.cloudburstmc.server.item;

import org.cloudburstmc.server.utils.Identifier;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class ItemPickaxeDiamond extends ItemTool {

    public ItemPickaxeDiamond(Identifier id) {
        super(id);
    }

    @Override
    public int getMaxDurability() {
        return DURABILITY_DIAMOND;
    }

    @Override
    public boolean isPickaxe() {
        return true;
    }

    @Override
    public int getTier() {
        return TIER_DIAMOND;
    }

    @Override
    public int getAttackDamage() {
        return 5;
    }
}
