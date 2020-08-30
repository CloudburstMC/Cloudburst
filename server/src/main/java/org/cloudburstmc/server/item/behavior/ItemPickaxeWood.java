package org.cloudburstmc.server.item.behavior;

import org.cloudburstmc.server.utils.Identifier;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class ItemPickaxeWood extends ItemTool {

    public ItemPickaxeWood(Identifier id) {
        super(id);
    }

    @Override
    public int getMaxDurability() {
        return DURABILITY_WOODEN;
    }

    @Override
    public boolean isPickaxe() {
        return true;
    }

    @Override
    public int getTier() {
        return TIER_WOODEN;
    }

    @Override
    public int getAttackDamage() {
        return 2;
    }
}
