package org.cloudburstmc.server.block.component;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.block.component.*;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.event.entity.EntityChangeBlockEvent;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.sound.SoundType;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.CardinalDirection;
import org.cloudburstmc.server.block.CloudBlock;
import org.cloudburstmc.server.entity.misc.EntityFallingBlock;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.registry.CloudEntityRegistry;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FallingBlockHandlers {

    public static final FallingBlockDropHandler BLOCK_ITEM_DROP = state -> state.getType().asItem()
            .map(itemType -> ItemStack.builder()
                    .itemType(itemType)
                    .setData(ItemDataComponents.BLOCK_STATE, state)
                    .amount(1)
                    .build())
            .orElse(ItemStack.EMPTY);

    public static final BooleanBlockHandler IS_FREE_TO_FALL = block -> canReplace(block.getSide(Direction.DOWN));

    public static final TickBlockHandler ON_TICK = (block, random) -> {
        if (block.requireComponent(BlockComponents.IS_FREE_TO_FALL).execute(block)) {
            block.requireComponent(BlockComponents.START_FALLING).execute(block);
        }
    };

    public static final FallingBlockPlacementHandler RESOLVE_PLACEMENT = (state, target) -> {
        int y = target.getPosition().getY();
        if (y < target.getLevel().getMinHeight() || y >= target.getLevel().getMaxHeight()
                || !canReplace(target) || isUnstableOnLanding(state, target)) {
            return List.of();
        }

        return List.of(new BlockChange(target, state));
    };

    public static final FallingLandBlockHandler ANVIL_LAND = (entity, target, fallDistance) -> {
        int effectiveDistance = (int) Math.ceil(fallDistance - 1);
        if (Math.floor(effectiveDistance * entity.getDamagePerBlock()) <= 0
                || ((CloudLevel) entity.getLevel()).getRandom().nextFloat() >= 0.05f + effectiveDistance * 0.05f) {
            return;
        }

        CardinalDirection direction = entity.getBlockState().ensureTrait(BlockTraits.CARDINAL_DIRECTION);
        if (entity.getBlockState().getType() == BlockTypes.ANVIL) {
            entity.setBlockState(BlockTypes.CHIPPED_ANVIL.getDefaultState().withTrait(BlockTraits.CARDINAL_DIRECTION, direction));
        } else if (entity.getBlockState().getType() == BlockTypes.CHIPPED_ANVIL) {
            entity.setBlockState(BlockTypes.DAMAGED_ANVIL.getDefaultState().withTrait(BlockTraits.CARDINAL_DIRECTION, direction));
        } else {
            entity.setDropCancelled(true);
        }
    };

    public static ComplexBlockHandler scheduleFall(int delay) {
        return block -> block.getLevel().scheduleUpdate(block.getPosition(), delay);
    }

    public static ComplexBlockHandler startFalling(@Nullable SoundType landingSound, @Nullable SoundType breakSound, float damagePerBlock, int maximumDamage) {
        return block -> {
            EntityFallingBlock fallingBlock = (EntityFallingBlock) CloudEntityRegistry.get().newEntity(
                    EntityTypes.FALLING_BLOCK,
                    Location.from(block.getPosition().toFloat().add(0.5f, 0, 0.5f), block.getLevel())
            );

            fallingBlock.setBlockState(block.getState());
            fallingBlock.setLandingSound(landingSound);
            fallingBlock.setBreakSound(breakSound);
            fallingBlock.setDamagePerBlock(damagePerBlock);
            fallingBlock.setMaximumDamage(maximumDamage);

            EntityChangeBlockEvent event = new EntityChangeBlockEvent(fallingBlock, block, BlockStates.AIR);
            block.getLevel().getServer().getEventManager().fire(event);
            if (event.isCancelled()) {
                fallingBlock.close();
                return;
            }

            if (!fallingBlock.startFalling(block, event.getBlockState())) {
                fallingBlock.close();
            }
        };
    }

    public static boolean canReplace(Block block) {
        return block.requireComponent(BlockComponents.CAN_BE_REPLACED_BY_FALLING_BLOCK).execute(block.getState());
    }

    public static boolean isUnstableOnLanding(BlockState state, Block target) {
        Block proposed = new CloudBlock(
                target.getLevel(),
                target.getPosition(),
                new BlockState[]{state, target.getSecondaryState()}
        );

        return !proposed.requireComponent(BlockComponents.CAN_SURVIVE).execute(proposed)
                || proposed.requireComponent(BlockComponents.IS_FREE_TO_FALL).execute(proposed);
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
