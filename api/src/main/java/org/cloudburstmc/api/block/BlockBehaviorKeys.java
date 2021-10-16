package org.cloudburstmc.api.block;

import org.cloudburstmc.api.block.behavior.BlockStateBehavior;
import org.cloudburstmc.api.data.DataKey;
import org.cloudburstmc.api.data.SimpleDataKey;
import org.cloudburstmc.api.util.AxisAlignedBB;
import org.cloudburstmc.api.util.Identifier;

public class BlockBehaviorKeys {

    public static final SimpleDataKey<BlockStateBehavior<String>> GET_DESCRIPTION_ID = DataKey.simple(Identifier.fromString("get_description_id"), BlockStateBehavior.class);

    public static final SimpleDataKey<BlockStateBehavior<AxisAlignedBB>> GET_BOUNDING_BOX = DataKey.simple(Identifier.fromString("get_bounding_box"), BlockStateBehavior.class);
}
