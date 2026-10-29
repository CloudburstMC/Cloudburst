package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.block.component.*;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.event.block.BlockSpreadEvent;
import org.cloudburstmc.api.event.entity.EntityChangeBlockEvent;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.api.level.sound.SoundTypes;
import org.cloudburstmc.api.player.GameMode;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.DripstoneThickness;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.block.util.BlockSupport;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.entity.projectile.EntityThrownTrident;
import org.cloudburstmc.server.level.CloudLevel;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

/**
 * Common placement, shape updates, falling and natural growth for speleothems.
 */
@UtilityClass
public class SpeleothemBlockHandlers {

    private static final float GROWTH_CHANCE = 0.011377778f;

    public static final PlacementStateHandler RESOLVE_PLACEMENT_STATE = (state, block, player, face, click) -> {
        boolean hanging = player == null ? face == Direction.DOWN : player.getPitch() < 0;
        if (!canSurvive(block, state.getType(), hanging)) {
            hanging = !hanging;
            if (!canSurvive(block, state.getType(), hanging)) {
                return null;
            }
        }

        return state.withTrait(BlockTraits.IS_HANGING, hanging).withTrait(BlockTraits.DRIPSTONE_THICKNESS,
                thickness(block, state.getType(), hanging, player == null || !player.isSneaking()));
    };

    public static final SurviveBlockHandler CAN_SURVIVE = block -> canSurvive(block, block.getState().getType(), hanging(block.getState()));

    public static final ComplexBlockHandler START_FALLING = SpeleothemBlockHandlers::fall;

    public static final NeighborBlockHandler ON_NEIGHBOUR_CHANGED = (block, neighbor) -> {
        boolean hanging = hanging(block.getState());
        if (neighbor.getX() != block.getX() || neighbor.getZ() != block.getZ() || hanging && block.getLevel().isUpdateScheduled(block.getPosition())) {
            return;
        }

        if (neighbor.getY() == block.getY() + (hanging ? 1 : -1) && !CAN_SURVIVE.execute(block)) {
            block.getLevel().scheduleUpdate(block.getPosition(), hanging ? 2 : 1);
            return;
        }

        BlockState state = block.getState();
        BlockState updated = state.withTrait(BlockTraits.DRIPSTONE_THICKNESS,
                thickness(block, state.getType(), hanging, state.ensureTrait(BlockTraits.DRIPSTONE_THICKNESS) == DripstoneThickness.MERGE));
        if (updated != state) {
            block.set(updated, false, true);
        }
    };

    public static final TickBlockHandler ON_TICK = (block, random) -> {
        if (!hanging(block.getState()) && !CAN_SURVIVE.execute(block)) {
            block.getLevel().breakBlock(block.getPosition(), ItemStack.EMPTY, null, true);
        } else {
            block.requireComponent(BlockComponents.START_FALLING).execute(block);
        }
    };

    public static final FallOnBlockHandler ON_FALL_ON = (block, entity, distance) -> {
        if (!hanging(block.getState()) && isTip(block.getState(), false)) {
            ((CloudEntity) entity).applyFallDamage(distance + 2.5f, 2, DamageSource.builder(DamageTypes.STALAGMITE).block(block).build());
        } else {
            ((CloudEntity) entity).applyFallDamage(distance);
        }
    };

    public static final EntityBlockHandler ON_PROJECTILE_HIT = (block, entity) -> {
        if (!(entity instanceof EntityThrownTrident trident) || trident.getMotion().lengthSquared() <= 0.36f
                || !block.getLevel().getGameRules().get(GameRules.PROJECTILES_CAN_BREAK_BLOCKS)) {
            return;
        }

        if (trident.getOwner() instanceof Player player) {
            if (player.getGameMode() == GameMode.ADVENTURE
                    && !trident.getTrident().getOrDefault(ItemDataComponents.CAN_DESTROY, List.of()).contains(block.getState().getType())
                    || player.isSpectator()
                    || !player.isOp() && block.getLevel() instanceof CloudLevel level && level.isInSpawnRadius(block.getPosition())) {
                return;
            }
        } else if (trident.getOwner() != null && !block.getLevel().getGameRules().get(GameRules.MOB_GRIEFING)) {
            return;
        }

        BlockState replacement = LiquidStateAccess.blockState(block.getLiquid());
        EntityChangeBlockEvent event = new EntityChangeBlockEvent(entity, block, replacement);
        block.getLevel().getServer().getEventManager().fire(event);
        if (!event.isCancelled() && block.getLevel().getBlockState(block.getPosition()) == block.getState()) {
            block.getLevel().breakBlock(block.getPosition(), ItemStack.EMPTY, null, true);
        }
    };

    public static TickBlockHandler naturalGrowth(BlockType supportType, int maxLength) {
        return (block, random) -> {
            RandomGenerator source = random == null ? ThreadLocalRandom.current() : random;
            if (source.nextFloat() >= GROWTH_CHANCE || !isWithDirection(block.getState(), true)) {
                return;
            }

            Block support = loadedVertical(block, 1);
            if (support == null || support.getState().getType() == block.getState().getType() || support.getState().getType() != supportType) {
                return;
            }

            Block tip = growingTip(block, maxLength);
            if (tip == null) {
                return;
            }

            if (source.nextInt(2) == 0) {
                growStalagmiteBelow(tip, block.getState().getType());
            } else {
                extend(tip, block.getState().getType(), true, true);
            }
        };
    }

    public static void growDownward(Block start, int maxLength) {
        Block tip = growingTip(start, maxLength);
        if (tip != null) {
            extend(tip, start.getState().getType(), true, false);
        }
    }

    public static boolean canSurvive(Block block, BlockType ownType, boolean hanging) {
        Block support = loadedVertical(block, hanging ? 1 : -1);
        return support != null && (support.getState().getType() == ownType && isWithDirection(support.getState(), hanging)
                || BlockSupport.isFaceSturdy(block.getLevel(), support.getPosition(), hanging ? Direction.DOWN : Direction.UP));
    }

    public static boolean hanging(BlockState state) {
        return state.ensureTrait(BlockTraits.IS_HANGING);
    }

    public static boolean isWithDirection(BlockState state, boolean hanging) {
        return state.is(BlockTags.SPELEOTHEMS) && hanging(state) == hanging;
    }

    public static boolean isTip(BlockState state, boolean merged) {
        if (!state.is(BlockTags.SPELEOTHEMS)) {
            return false;
        }

        DripstoneThickness thickness = state.ensureTrait(BlockTraits.DRIPSTONE_THICKNESS);
        return thickness == DripstoneThickness.TIP || merged && thickness == DripstoneThickness.MERGE;
    }

    public static @Nullable Block loadedVertical(Block block, int offsetY) {
        Vector3i position = block.getPosition().add(0, offsetY, 0);
        return position.getY() < block.getLevel().getMinHeight() || position.getY() >= block.getLevel().getMaxHeight() ? null : block.getLevel().getLoadedBlock(position);
    }

    private static DripstoneThickness thickness(Block block, BlockType ownType, boolean hanging, boolean merge) {
        Block front = loadedVertical(block, hanging ? -1 : 1);
        if (front == null) {
            return DripstoneThickness.TIP;
        }

        BlockState frontState = front.getState();
        if (frontState.getType() == ownType && isWithDirection(frontState, !hanging)) {
            return merge || frontState.ensureTrait(BlockTraits.DRIPSTONE_THICKNESS) == DripstoneThickness.MERGE ? DripstoneThickness.MERGE : DripstoneThickness.TIP;
        }

        if (!isWithDirection(frontState, hanging)) {
            return DripstoneThickness.TIP;
        }

        if (isTip(frontState, true)) {
            return DripstoneThickness.FRUSTUM;
        }

        Block behind = loadedVertical(block, hanging ? 1 : -1);
        return behind != null && isWithDirection(behind.getState(), hanging) ? DripstoneThickness.MIDDLE : DripstoneThickness.BASE;
    }

    private static @Nullable Block growingTip(Block start, int maxLength) {
        if (!isWithDirection(start.getState(), true)) {
            return null;
        }

        Block tip = start;
        for (int step = 0; step < maxLength; step++) {
            if (tip == null) {
                return null;
            }

            if (isTip(tip.getState(), false)) {
                return isWithDirection(tip.getState(), true) && tip.getLiquid().isEmpty() && canTipGrow(tip, start.getState().getType()) ? tip : null;
            }

            if (tip.getState().getType() != start.getState().getType() || !isWithDirection(tip.getState(), true)) {
                return null;
            }

            tip = loadedVertical(tip, -1);
        }

        return null;
    }

    private static boolean canTipGrow(Block tip, BlockType ownType) {
        boolean hanging = hanging(tip.getState());
        Block next = loadedVertical(tip, hanging ? -1 : 1);
        return next != null && next.getLiquid().isEmpty() && (next.getState() == BlockStates.AIR || isOwnTip(next.getState(), ownType, !hanging));
    }

    private static boolean isOwnTip(BlockState state, BlockType ownType, boolean hanging) {
        return state.getType() == ownType && isWithDirection(state, hanging) && isTip(state, false);
    }

    private static void growStalagmiteBelow(Block tip, BlockType ownType) {
        for (int distance = 1; distance <= 10; distance++) {
            Block target = loadedVertical(tip, -distance);
            if (target == null || !target.getLiquid().isEmpty()) {
                return;
            }

            if (isOwnTip(target.getState(), ownType, false) && canTipGrow(target, ownType)) {
                extend(target, ownType, false, true);
                return;
            }

            if (canSurvive(target, ownType, false)) {
                Block below = loadedVertical(target, -1);
                if (below != null && !below.getLiquid().getType().isSameFamily(LiquidTypes.WATER)) {
                    extend(below, ownType, false, true);
                    return;
                }
            }
        }
    }

    private static void extend(Block from, BlockType ownType, boolean hanging, boolean mergeTips) {
        Block target = loadedVertical(from, hanging ? -1 : 1);
        if (target == null) {
            return;
        }

        if (mergeTips && isOwnTip(target.getState(), ownType, !hanging)) {
            if (spread(from, target, growthState(ownType, !hanging, DripstoneThickness.MERGE))) {
                Block refreshed = from.refresh();
                if (refreshed.getState().getType() == ownType && isWithDirection(refreshed.getState(), hanging) && isTip(refreshed.getState(), true)) {
                    spread(target.refresh(), refreshed, growthState(ownType, hanging, DripstoneThickness.MERGE));
                }
            }
        } else if (target.getState() == BlockStates.AIR || target.getState().getType() == BlockTypes.WATER) {
            spread(from, target, growthState(ownType, hanging, DripstoneThickness.TIP));
        }
    }

    private static BlockState growthState(BlockType type, boolean hanging, DripstoneThickness thickness) {
        BlockState state = type.getDefaultState().withTrait(BlockTraits.IS_HANGING, hanging).withTrait(BlockTraits.DRIPSTONE_THICKNESS, thickness);
        return type == BlockTypes.ICICLE ? state.withTrait(BlockTraits.IS_ATTACHED, false) : state;
    }

    private static boolean spread(Block source, Block target, BlockState state) {
        BlockSpreadEvent event = new BlockSpreadEvent(target, source, state);
        target.getLevel().getServer().getEventManager().fire(event);
        return !event.isCancelled() && target.getLevel().getBlockState(target.getPosition()) == target.getState()
                && target.getLevel().setBlockState(target.getPosition(), event.getNewState());
    }

    private static void fall(Block start) {
        Block current = start;
        int length = 0;

        while (current != null && isWithDirection(current.getState(), true)) {
            boolean tip = isTip(current.getState(), true);
            float damage = tip ? Math.max(1 + length, 6) : 0;

            if (current.getState().getType() == BlockTypes.POINTED_DRIPSTONE) {
                FallingBlockHandlers.startFalling(SoundTypes.LAND_POINTED_DRIPSTONE, SoundTypes.LAND_POINTED_DRIPSTONE, damage, 40).execute(current);
            } else if (current.getState().getType() == BlockTypes.SULFUR_SPIKE) {
                FallingBlockHandlers.startFalling(SoundTypes.BLOCK_SULFUR_SPIKE_LAND, SoundTypes.BLOCK_SULFUR_SPIKE_LAND, damage, 40).execute(current);
            } else {
                FallingBlockHandlers.startFalling(null, null, damage, 40).execute(current);
            }

            if (tip) {
                return;
            }

            length++;
            current = loadedVertical(start, -length);
        }
    }
}
