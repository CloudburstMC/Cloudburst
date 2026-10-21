package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.block.component.*;
import org.cloudburstmc.api.event.block.BlockFadeEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.server.block.util.BlockSupport;

import java.util.concurrent.ThreadLocalRandom;

@UtilityClass
public class CoralBlockHandlers {

    public static final SurviveBlockHandler CAN_SURVIVE = block -> {
        Direction facing = block.getState().getTraits().containsKey(BlockTraits.CORAL_DIRECTION)
                ? block.getState().ensureTrait(BlockTraits.CORAL_DIRECTION) : Direction.UP;
        return BlockSupport.isFaceSturdy(block.getLevel(), facing.getOpposite().relative(block.getPosition()), facing);
    };

    public static final ComplexBlockHandler ON_STATE_CHANGED = block -> {
        if (!hasWater(block)) {
            block.getLevel().scheduleUpdate(block.getPosition(), 60 + ThreadLocalRandom.current().nextInt(40));
        }
    };

    public static final NeighborBlockHandler ON_NEIGHBOUR_CHANGED = (block, neighbor) -> {
        if (!block.requireComponent(BlockComponents.CAN_SURVIVE).execute(block)) {
            block.getLevel().breakBlock(block.getPosition(), ItemStack.EMPTY, null, true);
            return;
        }

        ON_STATE_CHANGED.execute(block);
    };

    public static PlacementStateHandler fanPlacement(BlockType wetFloor, BlockType wetWall, BlockType dryFloor, BlockType dryWall) {
        return (state, block, player, face, click) -> {
            if (face == Direction.DOWN) {
                return null;
            }

            boolean wet = block.getLiquid().isSource() && block.getLiquid().getType().isSameFamily(LiquidTypes.WATER);
            if (face.getAxis().isHorizontal()) {
                return (wet ? wetWall : dryWall).getDefaultState().withTrait(BlockTraits.CORAL_DIRECTION, face);
            }

            return (wet ? wetFloor : dryFloor).getDefaultState();
        };
    }

    public static TickBlockHandler die(BlockType deadType) {
        return (block, random) -> {
            if (hasWater(block)) {
                return;
            }

            BlockState original = block.getState();
            BlockState dead = deadType.getDefaultState().copyTraits(original);
            BlockFadeEvent event = new BlockFadeEvent(block, dead);
            block.getLevel().getServer().getEventManager().fire(event);

            if (!event.isCancelled() && block.getLevel().getBlockState(block.getPosition()) == original) {
                block.set(event.getNewState(), false, true);
            }
        };
    }

    public static boolean hasWater(Block block) {
        if (block.getLiquid().getType().isSameFamily(LiquidTypes.WATER)) {
            return true;
        }

        if (block.getState().getTraits().containsKey(BlockTraits.CORAL_FAN_DIRECTION) || block.getState().getTraits().containsKey(BlockTraits.CORAL_DIRECTION)) {
            return false;
        }

        for (Direction direction : Direction.values()) {
            if (block.getSide(direction).getLiquid().getType().isSameFamily(LiquidTypes.WATER)) {
                return true;
            }
        }

        return false;
    }
}
