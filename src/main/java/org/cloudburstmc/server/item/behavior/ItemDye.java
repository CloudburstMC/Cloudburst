package org.cloudburstmc.server.item.behavior;

import org.cloudburstmc.server.utils.Identifier;
import org.cloudburstmc.server.utils.data.DyeColor;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class ItemDye extends Item {

    public ItemDye(Identifier id) {
        super(id);
    }

    public DyeColor getDyeColor() {
        return DyeColor.getByDyeData(this.getMeta());
    }
}
