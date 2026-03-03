package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.component.ResourceCountBlockHandler;

@UtilityClass
public class AirBlockHandlers {

    public static final ResourceCountBlockHandler GET_RESOURCE_COUNT = (block, random, bonusLevel) -> 0;
}
