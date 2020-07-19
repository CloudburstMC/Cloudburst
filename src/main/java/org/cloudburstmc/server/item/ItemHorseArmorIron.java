package org.cloudburstmc.server.item;

import org.cloudburstmc.server.utils.Identifier;

public class ItemHorseArmorIron extends Item {

    public ItemHorseArmorIron(Identifier id) {
        super(id);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }
}
