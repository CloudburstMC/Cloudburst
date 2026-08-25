package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockComponents;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.component.*;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.BoundingBox;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.block.util.PlacementSupport;
import org.cloudburstmc.server.block.util.TripwireCalculator;
import org.cloudburstmc.server.level.CloudLevel;

import java.util.List;
import java.util.Set;

@UtilityClass
public class TripwireBlockHandlers {

    private static final int WIRE_RECHECK_TICKS = 10;

    public static final BlockLootHandler GET_LOOT = (block, context) ->
            List.of(ItemStack.builder().itemType(ItemTypes.STRING).amount(1).build());

    public static final PickBlockHandler GET_PICK_BLOCK = (block) ->
            ItemStack.builder().itemType(ItemTypes.STRING).amount(1).build();

    public static final NeighborBlockHandler ON_NEIGHBOUR_CHANGED = (block, neighbor) -> {
        CloudLevel level = (CloudLevel) block.getLevel();
        Vector3i pos = block.getPosition();
        BlockState state = level.getBlockState(pos.getX(), pos.getY(), pos.getZ());

        boolean nowSuspended = isSuspended(level, pos);
        if (nowSuspended != state.ensureTrait(BlockTraits.IS_SUSPENDED)) {
            level.setBlockState(pos, state.withTrait(BlockTraits.IS_SUSPENDED, nowSuspended), true, false);
        }
    };

    public static final PlayerBlockHandler ON_DESTROY = (block, player) -> {
        boolean holdingShears = player != null && player.getInventory().getSelectedItem().getType() == ItemTypes.SHEARS;

        CloudLevel level = (CloudLevel) block.getLevel();
        Vector3i pos = block.getPosition();
        BlockState oldState = block.getState();

        BlockState wireSourceState = oldState.withTrait(BlockTraits.IS_POWERED, true);
        if (holdingShears) {
            wireSourceState = wireSourceState.withTrait(BlockTraits.IS_DISARMED, true);
        }

        block.set(BlockStates.AIR);
        TripwireCalculator.notifyHooksAround(level, pos, wireSourceState);
    };

    public static final EntityInsideBlockHandler ON_ENTITY_INSIDE = (block, entity, precise) -> {
        if (entity instanceof Player && ((Player) entity).isSpectator()) {
            return;
        }

        BlockState state = block.getState();
        if (state.ensureTrait(BlockTraits.IS_POWERED)) {
            return;
        }

        CloudLevel level = (CloudLevel) block.getLevel();
        Vector3i pos = block.getPosition();

        level.setBlockState(pos, state.withTrait(BlockTraits.IS_POWERED, true), false, true);
        TripwireCalculator.notifyHooksAround(level, pos);
        level.scheduleUpdate(pos, WIRE_RECHECK_TICKS);
    };

    public static final TickBlockHandler ON_TICK = (block, random) -> {
        CloudLevel level = (CloudLevel) block.getLevel();
        Vector3i pos = block.getPosition();
        BlockState state = level.getBlockState(pos.getX(), pos.getY(), pos.getZ());

        if (!state.ensureTrait(BlockTraits.IS_POWERED)) {
            return;
        }

        if (hasEntityInside(level, pos)) {
            level.scheduleUpdate(pos, WIRE_RECHECK_TICKS);
            return;
        }

        level.setBlockState(pos, state.withTrait(BlockTraits.IS_POWERED, false), false, true);
        TripwireCalculator.notifyHooksAround(level, pos);
    };

    private boolean isSuspended(CloudLevel level, Vector3i pos) {
        return !PlacementSupport.hasFloorSupport(level, pos);
    }

    private boolean hasEntityInside(CloudLevel level, Vector3i pos) {
        BoundingBox bb = new BoundingBox(
                pos.getX() + 0.1f, pos.getY() + 0.1f, pos.getZ() + 0.1f,
                pos.getX() + 0.9f, pos.getY() + 0.9f, pos.getZ() + 0.9f
        );
        Set<Entity> entities = level.getCollidingEntities(bb);
        return entities.stream().anyMatch(e -> !(e instanceof Player) || !((Player) e).isSpectator());
    }
}
