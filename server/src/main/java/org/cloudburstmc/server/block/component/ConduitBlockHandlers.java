package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.component.ComplexBlockHandler;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.server.registry.CloudBlockEntityRegistry;

@UtilityClass
public class ConduitBlockHandlers {

    public static final ComplexBlockHandler ON_STATE_CHANGED = block -> {
        BlockEntity entity = block.getLevel().getBlockEntity(block.getPosition());
        if (entity == null) {
            entity = CloudBlockEntityRegistry.get().newEntity(BlockEntityTypes.CONDUIT, block);
        }

        entity.scheduleUpdate();
    };
}
