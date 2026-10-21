package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.block.component.*;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.event.Event;
import org.cloudburstmc.api.event.entity.EntityChangeBlockEvent;
import org.cloudburstmc.api.event.entity.EntityInteractEvent;
import org.cloudburstmc.api.event.player.PlayerInteractEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.sound.SoundTypes;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.data.DripleafTilt;
import org.cloudburstmc.server.block.util.RedstoneSignals;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@UtilityClass
public class BigDripleafBlockHandlers {

    public static final PlacementStateHandler RESOLVE_PLACEMENT_STATE = (state, block, player, face, click) -> {
        BlockState below = block.getRelativeState(0, -1, 0);
        BlockState placement = DefaultPlacementStateHandler.INSTANCE.execute(state, block, player, face, click);
        if (below.getType() == BlockTypes.BIG_DRIPLEAF) {
            placement = placement.withTrait(BlockTraits.CARDINAL_DIRECTION, below.ensureTrait(BlockTraits.CARDINAL_DIRECTION));
        }

        return placement.withTrait(BlockTraits.DRIPLEAF_HEAD, true).withTrait(BlockTraits.DRIPLEAF_TILT, DripleafTilt.NONE);
    };

    public static final SurviveBlockHandler CAN_SURVIVE = block -> {
        Block below = block.getLevel().getLoadedBlock(block.getPosition().add(0, -1, 0));
        if (below == null) {
            return false;
        }

        boolean head = block.getState().ensureTrait(BlockTraits.DRIPLEAF_HEAD);
        boolean supported = below.getState().is(BlockTags.SUPPORTS_BIG_DRIPLEAF)
                || below.getState().getType() == BlockTypes.BIG_DRIPLEAF && (head || !below.getState().ensureTrait(BlockTraits.DRIPLEAF_HEAD));
        if (head || !supported) {
            return supported;
        }

        Block above = block.getLevel().getLoadedBlock(block.getPosition().add(0, 1, 0));
        return above != null && above.getState().getType() == BlockTypes.BIG_DRIPLEAF;
    };

    public static final NeighborBlockHandler ON_NEIGHBOUR_CHANGED = (block, neighbor) -> {
        BlockState state = block.getState();
        if (!CAN_SURVIVE.execute(block)) {
            if (state.ensureTrait(BlockTraits.DRIPLEAF_HEAD)) {
                block.getLevel().breakBlock(block.getPosition(), ItemStack.EMPTY, null, true);
            } else {
                block.getLevel().scheduleUpdate(block.getPosition(), 1);
            }
            return;
        }

        if (!state.ensureTrait(BlockTraits.DRIPLEAF_HEAD)) {
            return;
        }

        BlockState above = block.getRelativeState(0, 1, 0);
        if (above.getType() == BlockTypes.BIG_DRIPLEAF && above.ensureTrait(BlockTraits.DRIPLEAF_HEAD)) {
            block.set(stem(state));
        } else if (RedstoneSignals.hasNeighborSignal(block)) {
            setTilt(block, DripleafTilt.NONE, null);
        }
    };

    public static final ComplexBlockHandler ON_STATE_CHANGED = block -> {
        if (!block.getState().ensureTrait(BlockTraits.DRIPLEAF_HEAD)) {
            block.getLevel().cancelScheduledUpdate(block.getPosition());
            if (!CAN_SURVIVE.execute(block)) {
                block.getLevel().scheduleUpdate(block.getPosition(), 1);
            }
        }
    };

    public static final EntityInsideBlockHandler ON_ENTITY_INSIDE = (block, entity, precise) -> {
        if (!block.getState().ensureTrait(BlockTraits.DRIPLEAF_HEAD)) {
            return;
        }

        if (block.getState().ensureTrait(BlockTraits.DRIPLEAF_TILT) != DripleafTilt.NONE
                || !entity.isOnGround() || entity.getPosition().getY() <= block.getPosition().getY() + 11f / 16
                || RedstoneSignals.hasNeighborSignal(block)) {
            return;
        }

        Event interaction = entity instanceof Player player
                ? new PlayerInteractEvent(player, null, block, null, PlayerInteractEvent.Action.PHYSICAL)
                : new EntityInteractEvent(entity, block);
        block.getLevel().getServer().getEventManager().fire(interaction);
        if (!interaction.isCancelled()) {
            setTilt(block, DripleafTilt.UNSTABLE, entity);
        }
    };

    public static final EntityBlockHandler ON_PROJECTILE_HIT = (block, projectile) -> {
        if (block.getState().ensureTrait(BlockTraits.DRIPLEAF_HEAD)) {
            setTilt(block, DripleafTilt.FULL_TILT, projectile);
        }
    };

    public static final TickBlockHandler ON_TICK = (block, random) -> {
        if (!CAN_SURVIVE.execute(block)) {
            block.getLevel().breakBlock(block.getPosition(), ItemStack.EMPTY, null, true);
            return;
        }

        BlockState state = block.getState();
        if (!state.ensureTrait(BlockTraits.DRIPLEAF_HEAD)) {
            return;
        }

        DripleafTilt next = RedstoneSignals.hasNeighborSignal(block) ? DripleafTilt.NONE : switch (state.ensureTrait(BlockTraits.DRIPLEAF_TILT)) {
            case NONE, FULL_TILT -> DripleafTilt.NONE;
            case UNSTABLE -> DripleafTilt.PARTIAL_TILT;
            case PARTIAL_TILT -> DripleafTilt.FULL_TILT;
        };
        setTilt(block, next, null);
    };

    public static final FertilizeBlockHandler FERTILIZE = (block, player, random) -> {
        Block head = block;
        while (!head.getState().ensureTrait(BlockTraits.DRIPLEAF_HEAD)) {
            if (head.getPosition().getY() + 1 >= head.getLevel().getMaxHeight()) {
                return FertilizationResult.none();
            }

            Block above = head.getLevel().getLoadedBlock(head.getPosition().add(0, 1, 0));
            if (above == null || above.getState().getType() != BlockTypes.BIG_DRIPLEAF) {
                return FertilizationResult.none();
            }

            head = above;
        }

        if (head.getPosition().getY() + 1 >= head.getLevel().getMaxHeight()) {
            return FertilizationResult.none();
        }

        Block above = head.getLevel().getLoadedBlock(head.getPosition().add(0, 1, 0));
        if (above == null || blocksGrowth(above)) {
            return FertilizationResult.none();
        }

        BlockState grown = head.getState().withTrait(BlockTraits.DRIPLEAF_HEAD, true)
                .withTrait(BlockTraits.DRIPLEAF_TILT, DripleafTilt.NONE)
                .withTrait(BlockTraits.CARDINAL_DIRECTION, block.getState().ensureTrait(BlockTraits.CARDINAL_DIRECTION));
        return new FertilizationResult(List.of(new BlockChange(head, stem(grown)), new BlockChange(above, grown)), List.of());
    };

    public static final BlockLootHandler GET_LOOT = (block, context) -> List.of(ItemStack.from(ItemTypes.BIG_DRIPLEAF));
    public static final PickBlockHandler GET_PICK_BLOCK = block -> ItemStack.from(ItemTypes.BIG_DRIPLEAF);

    public static BlockState stem(BlockState state) {
        return state.withTrait(BlockTraits.DRIPLEAF_HEAD, false).withTrait(BlockTraits.DRIPLEAF_TILT, DripleafTilt.NONE);
    }

    public static boolean blocksGrowth(Block block) {
        BlockState state = block.getState();
        return state != BlockStates.AIR && state.getType() != BlockTypes.SMALL_DRIPLEAF_BLOCK
                && (!state.getType().isLiquid() || !block.getLiquid().getType().isSameFamily(LiquidTypes.WATER));
    }

    private static void setTilt(Block block, DripleafTilt tilt, @Nullable Entity entity) {
        Level level = block.getLevel();
        BlockState state = block.getState();
        DripleafTilt previous = state.ensureTrait(BlockTraits.DRIPLEAF_TILT);
        BlockState tilted = state.withTrait(BlockTraits.DRIPLEAF_TILT, tilt);
        if (entity != null) {
            EntityChangeBlockEvent event = new EntityChangeBlockEvent(entity, block, tilted);
            level.getServer().getEventManager().fire(event);
            if (event.isCancelled()) {
                return;
            }
        }

        if (level.getBlockState(block.getPosition()) != state) {
            return;
        }

        if (previous != tilt && !level.setBlockState(block.getPosition(), tilted, false, false)) {
            return;
        }

        if (tilt == DripleafTilt.FULL_TILT || tilt != DripleafTilt.UNSTABLE && previous != tilt) {
            level.playSound(block.getPosition(), tilt == DripleafTilt.NONE ? SoundTypes.TILT_UP_BIG_DRIPLEAF : SoundTypes.TILT_DOWN_BIG_DRIPLEAF,
                    1, ThreadLocalRandom.current().nextFloat(0.8f, 1.2f));
        }

        int delay = switch (tilt) {
            case NONE -> 0;
            case UNSTABLE, PARTIAL_TILT -> 10;
            case FULL_TILT -> 100;
        };

        if (delay > 0) {
            level.scheduleUpdate(block.getPosition(), delay);
        }
    }
}
