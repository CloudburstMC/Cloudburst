package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.blockentity.ShulkerBoxBlockEntity;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.registry.CloudBlockEntityRegistry;
import org.cloudburstmc.server.registry.CloudBlockRegistry;

public final class ShulkerBoxPlaceHandler extends DefaultBlockPlaceHandler {

    public ShulkerBoxPlaceHandler(CloudBlockRegistry registry) {
        super(registry);
    }

    @Override
    public boolean execute(BlockState blockState, Player player, Vector3i blockPosition, Direction face, Vector3f clickPosition) {
        if (!super.execute(blockState, player, blockPosition, face, clickPosition)) {
            return false;
        }

        CloudLevel level = (CloudLevel) player.getLevel();
        ShulkerBoxBlockEntity blockEntity;
        if (level.getBlockEntity(blockPosition) instanceof ShulkerBoxBlockEntity existing) {
            blockEntity = existing;
        } else {
            blockEntity = (ShulkerBoxBlockEntity) CloudBlockEntityRegistry.get()
                    .newEntity(BlockEntityTypes.SHULKER_BOX, level.getBlock(blockPosition));
        }

        blockEntity.setFacing(face);
        blockEntity.spawnToAll();
        return true;
    }
}
