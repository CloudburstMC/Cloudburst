package org.cloudburstmc.server.block;

import org.cloudburstmc.api.item.ToolType;
import org.cloudburstmc.api.util.AxisAlignedBB;
import org.cloudburstmc.api.util.SimpleAxisAlignedBB;

public record BlockAttributes(
        boolean fallable,
        float friction,
        float hardness,
        float resistance,
        boolean solid,
        boolean pushesOutItems,
        float translucency,
        int burnChance,
        int burnAbility,
        int filterLight,
        boolean experimental,
        boolean flammable,
        boolean replaceable,
        int emitLight,
        boolean diggable,
        boolean powerSource,
        boolean breakFalling,
        boolean blockWater,
        boolean canBeSilkTouched,
        boolean blockSolid,
        boolean blockMotion,
        boolean comparatorSignal,
        boolean pushUpFalling,
        boolean waterlogFlowing,
        boolean waterlogSolid,
        AxisAlignedBB boundingBox,
        ToolType targetTool
) {

    public static final BlockAttributes DEFAULT = new BlockAttributes(
            false,
            0.6f,
            1.5f,
            30,
            true,
            true,
            0,
            0,
            0,
            15,
            false,
            false,
            false,
            0,
            true,
            false,
            false,
            true,
            true,
            true,
            true,
            false,
            false,
            true,
            false,
            new SimpleAxisAlignedBB(0, 0, 0, 1, 1, 1),
            null
    );

}
