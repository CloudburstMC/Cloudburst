package org.cloudburstmc.server.block.behavior;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.behavior.ResourceCountBlockBehavior;

@UtilityClass
public class AirBlockBehaviors {

    public static final ResourceCountBlockBehavior GET_RESOURCE_COUNT = (behavior, block, random, bonusLevel) -> 0;
}
