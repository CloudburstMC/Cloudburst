package org.cloudburstmc.server.item.behavior;

import org.cloudburstmc.server.utils.Identifier;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class ItemPickaxeStone extends ItemTool {

    public ItemPickaxeStone(Identifier id) {
        super(id);
    }

    @Override
    public int getMaxDurability() {
        return DURABILITY_STONE;
    }

    @Override
    public boolean isPickaxe() {
        return true;
    }

    @Override
    public int getTier() {
        return TIER_STONE;
    }

    @Override
    public int getAttackDamage() {
        return 3;
    }
}
