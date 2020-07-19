package org.cloudburstmc.server.item;

import org.cloudburstmc.server.utils.Identifier;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class ItemSwordWood extends ItemTool {

    public ItemSwordWood(Identifier id) {
        super(id);
    }

    @Override
    public int getMaxDurability() {
        return DURABILITY_WOODEN;
    }

    @Override
    public boolean isSword() {
        return true;
    }

    @Override
    public int getTier() {
        return TIER_WOODEN;
    }

    @Override
    public int getAttackDamage() {
        return 4;
    }
}
