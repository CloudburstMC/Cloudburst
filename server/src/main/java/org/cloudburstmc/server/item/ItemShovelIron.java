package org.cloudburstmc.server.item;

import org.cloudburstmc.server.utils.Identifier;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class ItemShovelIron extends ItemTool {

    public ItemShovelIron(Identifier identifier) {
        super(identifier);
    }

    @Override
    public int getMaxDurability() {
        return DURABILITY_IRON;
    }

    @Override
    public boolean isShovel() {
        return true;
    }

    @Override
    public int getTier() {
        return TIER_IRON;
    }

    @Override
    public int getAttackDamage() {
        return 4;
    }
}
