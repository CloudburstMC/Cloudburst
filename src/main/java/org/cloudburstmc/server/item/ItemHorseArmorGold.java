package org.cloudburstmc.server.item;

import org.cloudburstmc.server.utils.Identifier;

public class ItemHorseArmorGold extends Item {

    public ItemHorseArmorGold(Identifier id) {
        super(id);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }
}
