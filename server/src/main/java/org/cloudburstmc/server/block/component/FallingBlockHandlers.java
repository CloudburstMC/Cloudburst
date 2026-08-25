package org.cloudburstmc.server.block.component;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.block.component.*;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.event.entity.EntityBlockChangeEvent;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.CardinalDirection;
import org.cloudburstmc.server.entity.misc.EntityFallingBlock;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.Sound;
import org.cloudburstmc.server.registry.EntityRegistry;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FallingBlockHandlers {

    private static final int FALL_DELAY = 1;

    public static final BooleanBlockHandler IS_FREE_TO_FALL = block ->
            block.getSide(Direction.DOWN).getState().isReplaceable();

    public static final NeighborBlockHandler ON_NEIGHBOUR_CHANGED = (block, neighbor) -> {
        if (neighbor.getPosition().equals(block.getSide(Direction.DOWN).getPosition())) {
            scheduleFall(block);
        }
    };

    public static final TickBlockHandler ON_TICK = (block, random) -> {
        if (block.requireComponent(BlockComponents.IS_FREE_TO_FALL).execute(block)) {
            block.requireComponent(BlockComponents.START_FALLING).execute(block);
        }
    };

    public static final FallingLandBlockHandler ANVIL_LAND = (entity, target, fallDistance) -> {
        int effectiveDistance = (int) Math.ceil(fallDistance - 1);
        if (Math.floor(effectiveDistance * entity.getDamagePerBlock()) <= 0
                || ((CloudLevel) entity.getLevel()).getRandom().nextFloat()
                >= 0.05f + effectiveDistance * 0.05f) {
            return;
        }

        CardinalDirection direction = entity.getBlockState().ensureTrait(BlockTraits.CARDINAL_DIRECTION);
        if (entity.getBlockState().getType() == BlockTypes.ANVIL) {
            entity.setBlockState(BlockTypes.CHIPPED_ANVIL.getDefaultState()
                    .withTrait(BlockTraits.CARDINAL_DIRECTION, direction));
        } else if (entity.getBlockState().getType() == BlockTypes.CHIPPED_ANVIL) {
            entity.setBlockState(BlockTypes.DAMAGED_ANVIL.getDefaultState()
                    .withTrait(BlockTraits.CARDINAL_DIRECTION, direction));
        } else {
            entity.setDropCancelled(true);
        }
    };

    public static void scheduleFall(Block block) {
        block.getLevel().scheduleUpdate(block.getPosition(), FALL_DELAY);
    }

    public static ComplexBlockHandler startFalling(Sound landingSound, Sound breakSound,
                                                   float damagePerBlock, int maximumDamage) {
        return block -> {
            EntityFallingBlock fallingBlock = (EntityFallingBlock) EntityRegistry.get().newEntity(
                    EntityTypes.FALLING_BLOCK,
                    Location.from(block.getPosition().toFloat().add(0.5f, 0, 0.5f), block.getLevel()));
            fallingBlock.setBlockState(block.getState());
            fallingBlock.setLandingSound(landingSound);
            fallingBlock.setBreakSound(breakSound);
            fallingBlock.setDamagePerBlock(damagePerBlock);
            fallingBlock.setMaximumDamage(maximumDamage);

            EntityBlockChangeEvent event = new EntityBlockChangeEvent(fallingBlock, block, BlockStates.AIR);
            block.getLevel().getServer().getEventManager().fire(event);
            if (event.isCancelled()) {
                fallingBlock.close();
                return;
            }

            if (!block.getLevel().setBlockState(block.getPosition(), event.getTo(), true, true)) {
                fallingBlock.close();
                return;
            }

            fallingBlock.spawnToAll();
        };
    }

    public static FallingLandBlockHandler solidifyConcretePowder(BlockType concreteType) {
        return (entity, target, fallDistance) -> {
            if (containsWater(target)) {
                entity.setBlockState(concreteType.getDefaultState());
                return;
            }

            for (Direction direction : Direction.values()) {
                if (direction != Direction.DOWN && containsWater(target.getSide(direction))) {
                    entity.setBlockState(concreteType.getDefaultState());
                    return;
                }
            }
        };
    }

    private static boolean containsWater(Block block) {
        return block.getLiquid().getType().isSameFamily(LiquidTypes.WATER);
    }
}
