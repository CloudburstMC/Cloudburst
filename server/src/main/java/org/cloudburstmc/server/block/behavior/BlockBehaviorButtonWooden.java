package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.item.ItemTool;

public class BlockBehaviorButtonWooden extends BlockBehaviorButton {

    @Override
    public int getToolType() {
        return ItemTool.TYPE_AXE;
    }
}
