package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.block.component.*;
import org.cloudburstmc.api.blockentity.Bell;
import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.AttachmentType;
import org.cloudburstmc.server.block.util.RedstoneSignals;
import org.cloudburstmc.server.registry.CloudBlockEntityRegistry;

@UtilityClass
public class BellBlockHandlers {

    public static final PlacementStateHandler RESOLVE_PLACEMENT_STATE = (state, block, player, face, _) -> {
        Direction facing = player == null ? facing(state) : player.getHorizontalDirection().getOpposite();
        AttachmentType attachment;
        if (face == Direction.UP) {
            attachment = AttachmentType.STANDING;
        } else if (face == Direction.DOWN) {
            attachment = AttachmentType.HANGING;
        } else if (hasSupport(block, face.getOpposite())) {
            facing = face;
            attachment = hasSupport(block, face) ? AttachmentType.MULTIPLE : AttachmentType.SIDE;
        } else {
            attachment = hasSupport(block, Direction.DOWN) ? AttachmentType.STANDING : AttachmentType.HANGING;
        }

        return state.withTrait(BlockTraits.ATTACHMENT, attachment)
                .withTrait(BlockTraits.DIRECTION, facing)
                .withTrait(BlockTraits.IS_TOGGLED, false);
    };

    public static final SurviveBlockHandler CAN_SURVIVE = block -> {
        BlockState state = block.getState();
        Direction facing = facing(state);
        return switch (attachment(state)) {
            case STANDING -> hasSupport(block, Direction.DOWN);
            case HANGING -> hasSupport(block, Direction.UP);
            case SIDE -> hasSupport(block, facing.getOpposite());
            case MULTIPLE -> hasSupport(block, facing.getOpposite()) && hasSupport(block, facing);
        };
    };

    public static final ComplexBlockHandler UPDATE_POWER = block -> {
        BlockState state = block.getState();
        boolean powered = RedstoneSignals.hasNeighborSignal(block);
        if (state.ensureTrait(BlockTraits.IS_TOGGLED) == powered) {
            return;
        }

        if (block.getLevel().setBlockState(block.getPosition(), state.withTrait(BlockTraits.IS_TOGGLED, powered), true, true) && powered) {
            ring(block, null, null);
        }
    };

    public static final ComplexBlockHandler ON_ADDED = block -> {
        if (block.getLevel().getBlockEntity(block.getPosition()) == null) {
            CloudBlockEntityRegistry.get().newEntity(BlockEntityTypes.BELL, block);
        }
        UPDATE_POWER.execute(block);
    };

    public static final NeighborBlockHandler ON_NEIGHBOUR_CHANGED = (block, _) -> {
        BlockState state = block.getState();
        AttachmentType attachment = attachment(state);
        if (attachment == AttachmentType.SIDE || attachment == AttachmentType.MULTIPLE) {
            Direction facing = facing(state);
            boolean rear = hasSupport(block, facing.getOpposite());
            boolean front = hasSupport(block, facing);
            if (!rear && !front) {
                breakBell(block);
                return;
            }

            BlockState updated = state.withTrait(BlockTraits.ATTACHMENT, rear && front ? AttachmentType.MULTIPLE : AttachmentType.SIDE)
                    .withTrait(BlockTraits.DIRECTION, rear ? facing : facing.getOpposite());
            if (updated != state) {
                block.getLevel().setBlockState(block.getPosition(), updated, true, true);
            }
        } else if (!CAN_SURVIVE.execute(block)) {
            breakBell(block);
            return;
        }

        Block current = block.getLevel().getBlock(block.getPosition());
        if (current.getState().getType() == BlockTypes.BELL) {
            UPDATE_POWER.execute(current);
        }
    };

    public static final UseBlockHandler USE = (block, player, face, _) -> {
        if (player == null || player.isSpectator() || !acceptsStrike(block.getState(), face)) {
            return false;
        }

        ring(block, player, face);
        return true;
    };

    public static final ProjectileHitBlockHandler ON_PROJECTILE_HIT = (hit, projectile) -> {
        Block block = hit.block();
        Direction strike = hit.face();
        if (strike != null && hit.position().getY() - block.getY() <= 0.8124f && acceptsStrike(block.getState(), strike)) {
            Entity owner = projectile.getOwner();
            ring(block, owner == null ? projectile : owner, strike);
        }
    };

    private static boolean acceptsStrike(BlockState state, Direction strike) {
        if (!strike.getAxis().isHorizontal()) {
            return false;
        }

        boolean aligned = facing(state).getAxis() == strike.getAxis();
        return switch (attachment(state)) {
            case STANDING -> aligned;
            case SIDE, MULTIPLE -> !aligned;
            case HANGING -> true;
        };
    }

    private static void ring(Block block, @Nullable Entity entity, @Nullable Direction strike) {
        if (block.getLevel().getBlockEntity(block.getPosition()) instanceof Bell bell) {
            bell.ring(entity, strike);
        }
    }

    private static void breakBell(Block block) {
        block.getLevel().breakBlock(block.getPosition(), ItemStack.EMPTY, null, true);
    }

    private static boolean hasSupport(Block block, Direction side) {
        SupportType support = side == Direction.UP ? SupportType.CENTER : SupportType.FULL;
        return block.getSide(side).isFaceSturdy(side.getOpposite(), support);
    }

    private static Direction facing(BlockState state) {
        return state.ensureTrait(BlockTraits.DIRECTION);
    }

    private static AttachmentType attachment(BlockState state) {
        return state.ensureTrait(BlockTraits.ATTACHMENT);
    }
}
