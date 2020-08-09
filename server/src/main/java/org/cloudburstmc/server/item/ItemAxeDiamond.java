package org.cloudburstmc.server.item;

import org.cloudburstmc.server.utils.Identifier;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class ItemAxeDiamond extends ItemTool {

    public ItemAxeDiamond(Identifier id) {
        super(id);
    }

    @Override
    public int getMaxDurability() {
        return DURABILITY_DIAMOND;
    }

    @Override
    public boolean isAxe() {
        return true;
    }

    @Override
    public int getTier() {
        return TIER_DIAMOND;
    }

    @Override
    public int getAttackDamage() {
        return 6;
    }
}
