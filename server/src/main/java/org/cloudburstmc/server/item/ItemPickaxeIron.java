package org.cloudburstmc.server.item;

import org.cloudburstmc.server.utils.Identifier;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class ItemPickaxeIron extends ItemTool {

    public ItemPickaxeIron(Identifier id) {
        super(id);
    }

    @Override
    public int getMaxDurability() {
        return DURABILITY_IRON;
    }

    @Override
    public boolean isPickaxe() {
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
