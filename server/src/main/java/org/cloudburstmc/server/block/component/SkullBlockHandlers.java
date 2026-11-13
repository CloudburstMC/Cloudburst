package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.component.ComplexBlockHandler;
import org.cloudburstmc.api.block.component.NeighborBlockHandler;
import org.cloudburstmc.api.block.component.PlaceBlockHandler;
import org.cloudburstmc.api.block.component.PlacementStateHandler;
import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.server.blockentity.SkullBlockEntity;
import org.cloudburstmc.server.registry.CloudBlockEntityRegistry;

@UtilityClass
public class SkullBlockHandlers {

    public static final PlacementStateHandler RESOLVE_PLACEMENT_STATE = (state, _, player, face, _) -> {
        if (player == null) {
            return state;
        }

        if (face == Direction.DOWN) {
            return null;
        }

        return state.withTrait(BlockTraits.FACING_DIRECTION, face);
    };

    public static final PlaceBlockHandler ON_PLACE = (state, player, position, face, _) -> {
        if (player == null || !player.getLevel().setBlockState(position, state, true, true)) {
            return false;
        }

        if (face == Direction.UP && player.getLevel().getBlockEntity(position) instanceof SkullBlockEntity skull) {
            skull.setRotation(SkullBlockEntity.rotationFromYaw(player.getYaw()));
        }

        return true;
    };

    public static final ComplexBlockHandler ON_ADDED = block -> {
        if (block.getLevel().getBlockEntity(block.getPosition()) == null) {
            CloudBlockEntityRegistry.get().newEntity(BlockEntityTypes.SKULL, block);
        }
    };

    public static final ComplexBlockHandler UPDATE_POWER = block -> {
        if (block.getLevel().getBlockEntity(block.getPosition()) instanceof SkullBlockEntity skull && !skull.isClosed()) {
            skull.scheduleUpdate();
        }
    };

    public static final NeighborBlockHandler ON_NEIGHBOUR_CHANGED = (block, _) -> UPDATE_POWER.execute(block);
}
