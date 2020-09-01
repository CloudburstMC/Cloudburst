package org.cloudburstmc.server.item.behavior;

import org.cloudburstmc.server.utils.Identifier;

public class ItemHoeNetherite extends ItemTool {

    public ItemHoeNetherite(Identifier id) {
        super(id);
    }

    @Override
    public int getMaxDurability() {
        return ItemTool.DURABILITY_NETHERITE;
    }

    @Override
    public boolean isHoe() {
        return true;
    }

    @Override
    public int getTier() {
        return ItemTool.TIER_NETHERITE;
    }
}
