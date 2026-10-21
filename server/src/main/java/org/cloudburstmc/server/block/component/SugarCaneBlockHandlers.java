package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.block.component.*;
import org.cloudburstmc.api.event.block.BlockGrowEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class SugarCaneBlockHandlers {

    private static final int MAX_HEIGHT = 3;
    private static final int MAX_AGE = 15;

    public static final SurviveBlockHandler CAN_SURVIVE = block -> {
        Level level = block.getLevel();
        Vector3i belowPosition = block.getPosition().add(0, -1, 0);
        Block below = level.getLoadedBlock(belowPosition);
        if (below == null) {
            return false;
        }

        if (below.getState().getType() == BlockTypes.REEDS) {
            return true;
        }

        if (!below.getState().is(BlockTags.SUPPORTS_SUGAR_CANE)) {
            return false;
        }

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            Block adjacent = level.getLoadedBlock(direction.relative(belowPosition));
            if (adjacent != null && (adjacent.getLiquid().getType().isSameFamily(LiquidTypes.WATER)
                    || adjacent.getState().is(BlockTags.SUPPORTS_SUGAR_CANE_ADJACENTLY))) {
                return true;
            }
        }

        return false;
    };

    public static final NeighborBlockHandler ON_NEIGHBOUR_CHANGED = (block, neighbor) -> {
        if (!CAN_SURVIVE.execute(block)) {
            block.getLevel().scheduleUpdate(block.getPosition(), 1);
        }
    };

    public static final TickBlockHandler ON_TICK = (block, random) -> {
        if (!CAN_SURVIVE.execute(block)) {
            block.getLevel().breakBlock(block.getPosition(), ItemStack.EMPTY, null, true);
        }
    };

    public static final TickBlockHandler ON_RANDOM_TICK = (block, random) -> growNaturally(block);

    public static final FertilizeBlockHandler FERTILIZE = (block, player, random) -> {
        Level level = block.getLevel();
        Block base = block;
        while (base.getPosition().getY() > level.getMinHeight()) {
            Block below = level.getLoadedBlock(base.getPosition().add(0, -1, 0));
            if (below == null) {
                return FertilizationResult.none();
            }

            if (below.getState().getType() != BlockTypes.REEDS) {
                break;
            }

            base = below;
        }

        if (!CAN_SURVIVE.execute(base)) {
            return FertilizationResult.none();
        }

        List<BlockChange> changes = new ArrayList<>(MAX_HEIGHT - 1);
        for (int height = 1; height < MAX_HEIGHT; height++) {
            Vector3i position = base.getPosition().add(0, height, 0);
            if (position.getY() >= level.getMaxHeight()) {
                break;
            }

            Block target = level.getLoadedBlock(position);
            if (target == null) {
                break;
            }

            if (target.getState().getType() == BlockTypes.REEDS) {
                continue;
            }

            if (isGrowthObstructed(target)) {
                break;
            }

            changes.add(new BlockChange(target, BlockTypes.REEDS.getDefaultState()));
        }

        return changes.isEmpty() ? FertilizationResult.none() : new FertilizationResult(changes, List.of());
    };

    public static final BlockLootHandler GET_LOOT = (block, context) -> List.of(ItemStack.from(ItemTypes.SUGAR_CANE));

    public static final PickBlockHandler GET_PICK_BLOCK = block -> ItemStack.from(ItemTypes.SUGAR_CANE);

    private static void growNaturally(Block block) {
        Level level = block.getLevel();
        if (block.getPosition().getY() + 1 >= level.getMaxHeight()) {
            return;
        }

        Block above = level.getLoadedBlock(block.getPosition().add(0, 1, 0));
        if (above == null || isGrowthObstructed(above)) {
            return;
        }

        int height = 1;
        while (height < MAX_HEIGHT) {
            Block below = level.getLoadedBlock(block.getPosition().add(0, -height, 0));
            if (below == null) {
                return;
            }

            if (below.getState().getType() != BlockTypes.REEDS) {
                break;
            }

            height++;
        }

        if (height >= MAX_HEIGHT) {
            return;
        }

        BlockState original = block.getState();
        int age = original.ensureTrait(BlockTraits.AGE);
        if (age < MAX_AGE) {
            block.set(original.withTrait(BlockTraits.AGE, age + 1), false, false);
            return;
        }

        BlockGrowEvent event = new BlockGrowEvent(above, BlockTypes.REEDS.getDefaultState());
        level.getServer().getEventManager().fire(event);
        if (!event.isCancelled() && level.getBlockState(block.getPosition()) == original
                && level.getBlockState(above.getPosition()) == above.getState()) {
            above.set(event.getNewState());
        }

        if (level.getBlockState(block.getPosition()) == original) {
            block.set(original.withTrait(BlockTraits.AGE, 0), false, false);
        }
    }

    private static boolean isGrowthObstructed(Block block) {
        return block.getState().getType() != BlockTypes.AIR || !block.getLiquid().isEmpty();
    }
}
