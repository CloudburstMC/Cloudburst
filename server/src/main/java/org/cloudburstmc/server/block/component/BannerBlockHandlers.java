package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.block.component.*;
import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.CardinalDirection;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.server.blockentity.BannerBlockEntity;
import org.cloudburstmc.server.registry.CloudBlockEntityRegistry;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@UtilityClass
public class BannerBlockHandlers {

    public static final PlacementStateHandler RESOLVE_PLACEMENT_STATE =
            (state, block, player, face, _) -> resolvePlacement(state, block, player, face.getOpposite());

    public static final SurviveBlockHandler CAN_SURVIVE = block -> hasSupport(block, supportSide(block.getState()));

    public static final NeighborBlockHandler ON_NEIGHBOUR_CHANGED = (block, neighbor) -> {
        Direction support = supportSide(block.getState());
        if (neighbor.getPosition().equals(support.relative(block.getPosition())) && !CAN_SURVIVE.execute(block)) {
            block.getLevel().breakBlock(block.getPosition(), null, null, true);
        }
    };

    public static final ComplexBlockHandler ON_ADDED = block -> {
        if (block.getLevel().getBlockEntity(block.getPosition()) == null) {
            CloudBlockEntityRegistry.get().newEntity(BlockEntityTypes.BANNER, block);
        }
    };

    public static final BlockLootHandler GET_LOOT = (block, context) -> {
        if (block.getLevel().getBlockEntity(block.getPosition()) instanceof BannerBlockEntity banner) {
            return List.of(banner.asItem(true));
        }

        return DefaultBlockHandlers.GET_LOOT.execute(block, context);
    };

    public static final PickBlockHandler GET_PICK_BLOCK = block -> {
        if (block.getLevel().getBlockEntity(block.getPosition()) instanceof BannerBlockEntity banner) {
            return banner.asItem(false);
        }

        return DefaultBlockHandlers.GET_PICK_BLOCK.execute(block);
    };

    private static @Nullable BlockState resolvePlacement(BlockState state, Block block, @Nullable Player player, Direction clickedSupport) {
        if (clickedSupport != Direction.UP && hasSupport(block, clickedSupport)) {
            return stateForSupport(state, player, clickedSupport);
        }

        Direction[] supports = Direction.values();
        if (player != null) {
            Vector3f look = player.getDirectionVector();
            Arrays.sort(supports, Comparator.comparingDouble((Direction side) -> -look.dot(side.getUnitVector().toFloat())));
        }

        for (Direction support : supports) {
            if (support != Direction.UP && support != clickedSupport && hasSupport(block, support)) {
                return stateForSupport(state, player, support);
            }
        }

        return null;
    }

    private static BlockState stateForSupport(BlockState state, @Nullable Player player, Direction support) {
        if (support == Direction.DOWN) {
            BlockState standing = BlockTypes.STANDING_BANNER.getDefaultState().copyTraits(state);
            if (player == null) {
                return standing;
            }

            int rotation = Math.round((player.getYaw() + 180f) / 22.5f) & 15;
            return standing.withTrait(BlockTraits.SIGN_DIRECTION, CardinalDirection.values()[rotation]);
        }

        return BlockTypes.WALL_BANNER.getDefaultState().withTrait(BlockTraits.FACING_DIRECTION, support.getOpposite());
    }

    private static boolean hasSupport(Block block, Direction support) {
        Block supportingBlock = block.getSide(support);
        return supportingBlock.requireComponent(BlockComponents.IS_SOLID_SUPPORT).execute(supportingBlock.getState());
    }

    private static Direction supportSide(BlockState state) {
        return state.getType() == BlockTypes.WALL_BANNER ? state.ensureTrait(BlockTraits.FACING_DIRECTION).getOpposite() : Direction.DOWN;
    }
}
