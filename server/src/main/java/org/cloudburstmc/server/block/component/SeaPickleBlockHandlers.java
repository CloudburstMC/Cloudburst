package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.block.component.*;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.util.CollisionContext;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.block.util.BlockSupport;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class SeaPickleBlockHandlers {

    public static final ReplaceBlockHandler CAN_BE_REPLACED = (block, replacement, player, face, click) ->
            replacement.getType() == block.getState().getType() && block.getState().ensureTrait(BlockTraits.CLUSTER_COUNT) < 3;

    public static final PlacementStateHandler RESOLVE_PLACEMENT_STATE = (state, block, player, face, click) ->
            block.getState().getType() == state.getType()
                    ? block.getState().incrementTrait(BlockTraits.CLUSTER_COUNT)
                    : state.withTrait(BlockTraits.IS_DEAD, !block.getLiquid().isSource() || !block.getLiquid().getType().isSameFamily(LiquidTypes.WATER));

    public static final SurviveBlockHandler CAN_SURVIVE = block -> {
        Block below = block.getSide(Direction.DOWN);
        return !below.getCollisionShape(CollisionContext.empty()).getFaceShape(Direction.UP).isEmpty()
                || BlockSupport.isFaceSturdy(block.getLevel(), below.getPosition(), Direction.UP);
    };

    public static final BlockLootHandler GET_LOOT = (block, context) -> List.of(ItemStack.from(ItemTypes.SEA_PICKLE, block.getState().ensureTrait(BlockTraits.CLUSTER_COUNT) + 1));

    public static final FertilizeBlockHandler FERTILIZE = (block, player, random) -> {
        if (block.getState().ensureTrait(BlockTraits.IS_DEAD)
                || !block.getLiquid().isSource()
                || !block.getLiquid().getType().isSameFamily(LiquidTypes.WATER)
                || !block.getRelativeState(0, -1, 0).is(BlockTags.CORAL_BLOCKS)) {
            return FertilizationResult.none();
        }

        List<BlockChange> changes = new ArrayList<>();
        Vector3i origin = block.getPosition();

        for (int x = -2; x <= 2; x++) {
            int radius = 2 - Math.abs(x);

            for (int z = -radius; z <= radius; z++) {
                for (int y = -1; y <= 0; y++) {
                    Vector3i position = origin.add(x, y, z);
                    if (position.equals(origin) || random.nextInt(6) != 0) {
                        continue;
                    }

                    Block target = block.getLevel().getLoadedBlock(position);
                    if (target == null || !target.getState().getType().isLiquid()
                            || !target.getLiquid().getType().isSameFamily(LiquidTypes.WATER)
                            || !target.getRelativeState(0, -1, 0).is(BlockTags.CORAL_BLOCKS)) {
                        continue;
                    }

                    BlockState grown = BlockTypes.SEA_PICKLE.getDefaultState()
                            .withTrait(BlockTraits.CLUSTER_COUNT, random.nextInt(4))
                            .withTrait(BlockTraits.IS_DEAD, false);
                    changes.add(new BlockChange(target, grown));
                }
            }
        }

        changes.add(new BlockChange(block, block.getState().withTrait(BlockTraits.CLUSTER_COUNT, 3)));
        return new FertilizationResult(changes, List.of());
    };
}
