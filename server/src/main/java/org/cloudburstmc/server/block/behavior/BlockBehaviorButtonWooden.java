package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.utils.Identifier;

/**
 * Created by CreeperFace on 27. 11. 2016.
 */
public class BlockBehaviorButtonWooden extends BlockBehaviorButton {

    public BlockBehaviorButtonWooden(Identifier id) {
        super(id);
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_AXE;
    }
}
