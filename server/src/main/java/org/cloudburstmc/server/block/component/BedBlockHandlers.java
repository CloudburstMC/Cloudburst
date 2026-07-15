package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.block.component.*;
import org.cloudburstmc.api.blockentity.Bed;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.api.item.ItemKeys;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemStackBuilder;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.DyeColor;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.block.util.PlacementSupport;
import org.cloudburstmc.server.entity.vehicle.DismountHelper;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.level.particle.DestroyBlockParticle;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudBlockEntityRegistry;
import org.cloudburstmc.server.registry.CloudBlockRegistry;

import java.util.List;

@UtilityClass
public class BedBlockHandlers {

    private static final List<RelativeOffset> SURROUND_OFFSETS = List.of(
            new RelativeOffset(1, 0),
            new RelativeOffset(1, -1),
            new RelativeOffset(1, -2),
            new RelativeOffset(0, -2),
            new RelativeOffset(-1, -2),
            new RelativeOffset(-1, -1),
            new RelativeOffset(-1, 0),
            new RelativeOffset(-1, 1),
            new RelativeOffset(0, 1),
            new RelativeOffset(1, 1)
    );

    private static final List<RelativeOffset> ABOVE_OFFSETS = List.of(
            new RelativeOffset(0, 0),
            new RelativeOffset(0, -1)
    );

    /**
     * Places both the foot (at {@code blockPosition}) and head (one block ahead
     * in the player's facing direction).
     */
    public static final PlaceBlockHandler PLACE = (blockState, player, blockPosition, face, clickPosition) -> {
        if (!(player instanceof CloudPlayer cloudPlayer)) {
            return false;
        }

        Direction facing = Direction.fromYaw(player.getYaw());

        Vector3i footPos = blockPosition;
        Vector3i headPos = Vector3i.from(
                footPos.getX() + facing.getStepX(),
                footPos.getY(),
                footPos.getZ() + facing.getStepZ()
        );

        CloudLevel level = (CloudLevel) player.getLevel();
        if (level.isOutsideBuildHeight(headPos.getY())) {
            return false;
        }

        BlockState headExisting = level.getBlockState(headPos.getX(), headPos.getY(), headPos.getZ());
        if (!CloudBlockRegistry.REGISTRY.getComponent(headExisting.getType(), BlockComponents.REPLACEABLE).get()) {
            return false;
        }

        if (!PlacementSupport.hasFloorSupport(level, footPos)
                || !PlacementSupport.hasFloorSupport(level, headPos)) {
            return false;
        }

        BlockState footState = blockState
                .withTrait(BlockTraits.DIRECTION, facing)
                .withTrait(BlockTraits.IS_HEAD_PIECE, false)
                .withTrait(BlockTraits.IS_OCCUPIED, false);

        BlockState headState = blockState
                .withTrait(BlockTraits.DIRECTION, facing)
                .withTrait(BlockTraits.IS_HEAD_PIECE, true)
                .withTrait(BlockTraits.IS_OCCUPIED, false);

        if (!level.setBlockState(footPos.getX(), footPos.getY(), footPos.getZ(), 0, footState, true, true)) {
            return false;
        }

        if (!level.setBlockState(headPos.getX(), headPos.getY(), headPos.getZ(), 0, headState, true, true)) {
            level.setBlockState(footPos.getX(), footPos.getY(), footPos.getZ(), 0,
                    level.getBlockState(footPos.getX(), footPos.getY(), footPos.getZ()), true, true);
            return false;
        }

        int damage = 0;
        Integer rawDamage = cloudPlayer.getInventory().getSelectedItem().get(ItemKeys.DAMAGE);
        if (rawDamage != null) {
            damage = rawDamage;
        }
        DyeColor color = DyeColor.getByWoolData(damage);

        spawnBedEntity(level, footPos, color);
        spawnBedEntity(level, headPos, color);

        return true;
    };

    /**
     * Right-click handler for the bed block.
     *
     * <p>Resolves whichever piece (foot or head) was clicked to the head block
     * position and delegates to {@link CloudPlayer#sleepOn(Vector3i)}.</p>
     */
    public static final UseBlockHandler BED = (block, player, direction, item) -> {
        if (!(player instanceof CloudPlayer cloudPlayer)) {
            return false;
        }

        Vector3i headPos = resolveHeadPos(block);
        return cloudPlayer.sleepOn(headPos);
    };

    /**
     * Destroys whichever half (foot or head) was broken and simultaneously
     * removes the partner half, leaving no orphaned block state behind.
     */
    public static final PlayerBlockHandler ON_DESTROY = (block, player) -> {
        Vector3i partnerPos = resolvePartnerPos(block);
        BlockType bedType = block.getState().getType();
        CloudLevel level = (CloudLevel) block.getLevel();

        block.set(BlockStates.AIR);

        BlockState partnerState = level.getBlockState(partnerPos.getX(), partnerPos.getY(), partnerPos.getZ());
        if (partnerState.getType() == bedType) {
            level.addParticle(new DestroyBlockParticle(partnerPos.toFloat().add(0.5f, 0.5f, 0.5f), partnerState));
            level.setBlockState(partnerPos.getX(), partnerPos.getY(), partnerPos.getZ(), 0, BlockStates.AIR, false, true);
        }
    };

    /**
     * Returns one bed item whose damage encodes the bed's color. Always drops exactly one item regardless
     * of whether the foot or head was broken.
     */
    public static final ResourceBlockHandler GET_RESOURCE = (block, random, bonusLevel) -> {
        DyeColor color = DyeColor.WHITE;
        CloudLevel level = (CloudLevel) block.getLevel();
        BlockEntity be = level.getBlockEntity(block.getPosition());
        if (be instanceof Bed bed) {
            color = bed.getColor();
        } else {
            Vector3i partnerPos = resolvePartnerPos(block);
            BlockEntity partnerBe = level.getBlockEntity(partnerPos);
            if (partnerBe instanceof Bed partnerBed) {
                color = partnerBed.getColor();
            }
        }

        BlockState defaultState = block.getState().getType().getDefaultState();
        ItemStackBuilder builder;
        try {
            builder = ItemStack.from(defaultState).toBuilder().amount(1);
        } catch (IllegalArgumentException e) {
            return ItemStack.EMPTY;
        }

        int woolData = color.getWoolData();
        if (woolData != 0) {
            builder.data(ItemKeys.DAMAGE, woolData);
        }

        return builder.build();
    };

    /**
     * Returns the bed item for pick-block.
     */
    public static final PickBlockHandler GET_PICK_BLOCK = (block) -> {
        DyeColor color = DyeColor.WHITE;
        CloudLevel level = (CloudLevel) block.getLevel();
        BlockEntity be = level.getBlockEntity(block.getPosition());
        if (be instanceof Bed bed) {
            color = bed.getColor();
        } else {
            Vector3i partnerPos = resolvePartnerPos(block);
            BlockEntity partnerBe = level.getBlockEntity(partnerPos);
            if (partnerBe instanceof Bed partnerBed) {
                color = partnerBed.getColor();
            }
        }

        BlockState defaultState = block.getState().getType().getDefaultState();
        ItemStackBuilder builder;
        try {
            builder = ItemStack.from(defaultState).toBuilder().amount(1);
        } catch (IllegalArgumentException e) {
            return ItemStack.EMPTY;
        }

        int woolData = color.getWoolData();
        if (woolData != 0) {
            builder.data(ItemKeys.DAMAGE, woolData);
        }

        return builder.build();
    };

    public static @Nullable Vector3f findStandUpPosition(CloudLevel level, Vector3i position, Direction forward, float yaw) {
        Direction right = forward.rotateClockwise();
        Direction side = right.isFacing(yaw) ? right.getOpposite() : right;
        if (level.getBlockState(position.sub(0, 1, 0)).getType() == BlockTypes.BED) {
            return findBunkBedStandUpPosition(level, position, forward, side);
        }

        Vector3f safePosition = findBedStandUpPosition(level, position, forward, side, true);
        return safePosition != null ? safePosition : findBedStandUpPosition(level, position, forward, side, false);
    }

    private static @Nullable Vector3f findBunkBedStandUpPosition(CloudLevel level, Vector3i position, Direction forward, Direction side) {
        Vector3f safePosition = findStandUpPositionAtOffset(level, position, forward, side, SURROUND_OFFSETS, true);
        if (safePosition != null) {
            return safePosition;
        }

        Vector3i below = position.sub(0, 1, 0);
        safePosition = findStandUpPositionAtOffset(level, below, forward, side, SURROUND_OFFSETS, true);
        if (safePosition != null) {
            return safePosition;
        }

        safePosition = findStandUpPositionAtOffset(level, position, forward, side, ABOVE_OFFSETS, true);
        if (safePosition != null) {
            return safePosition;
        }

        safePosition = findStandUpPositionAtOffset(level, position, forward, side, SURROUND_OFFSETS, false);
        if (safePosition != null) {
            return safePosition;
        }

        safePosition = findStandUpPositionAtOffset(level, below, forward, side, SURROUND_OFFSETS, false);
        return safePosition != null ? safePosition
                : findStandUpPositionAtOffset(level, position, forward, side, ABOVE_OFFSETS, false);
    }

    private static @Nullable Vector3f findBedStandUpPosition(CloudLevel level, Vector3i position, Direction forward, Direction side, boolean avoidDanger) {
        Vector3f safePosition = findStandUpPositionAtOffset(level, position, forward, side, SURROUND_OFFSETS, avoidDanger);
        return safePosition != null ? safePosition
                : findStandUpPositionAtOffset(level, position, forward, side, ABOVE_OFFSETS, avoidDanger);
    }

    private static @Nullable Vector3f findStandUpPositionAtOffset(CloudLevel level, Vector3i position, Direction forward, Direction side, List<RelativeOffset> offsets, boolean avoidDanger) {
        for (RelativeOffset offset : offsets) {
            int x = side.getStepX() * offset.sideSteps() + forward.getStepX() * offset.forwardSteps();
            int z = side.getStepZ() * offset.sideSteps() + forward.getStepZ() * offset.forwardSteps();
            Vector3f safePosition = DismountHelper.findSafeDismountLocation(level, position.add(x, 0, z), avoidDanger);
            if (safePosition != null) {
                return safePosition;
            }
        }

        return null;
    }

    private static void spawnBedEntity(CloudLevel level, Vector3i pos, DyeColor color) {
        CloudChunk chunk = level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk == null) {
            return;
        }

        Bed entity = CloudBlockEntityRegistry.get().newEntity(BlockEntityTypes.BED, chunk, pos);
        entity.setColor(color);
        entity.spawnToAll();
    }

    private static Vector3i resolveHeadPos(Block block) {
        BlockState state = block.getState();
        if (state.ensureTrait(BlockTraits.IS_HEAD_PIECE)) {
            return block.getPosition();
        }

        Direction facing = state.ensureTrait(BlockTraits.DIRECTION);
        Vector3i foot = block.getPosition();
        return Vector3i.from(
                foot.getX() + facing.getStepX(),
                foot.getY(),
                foot.getZ() + facing.getStepZ()
        );
    }

    private static Vector3i resolvePartnerPos(Block block) {
        BlockState state = block.getState();
        Direction facing = state.ensureTrait(BlockTraits.DIRECTION);
        boolean isHead = state.ensureTrait(BlockTraits.IS_HEAD_PIECE);
        Vector3i pos = block.getPosition();
        int stepX = facing.getStepX();
        int stepZ = facing.getStepZ();

        if (isHead) {
            return Vector3i.from(pos.getX() - stepX, pos.getY(), pos.getZ() - stepZ);
        } else {
            return Vector3i.from(pos.getX() + stepX, pos.getY(), pos.getZ() + stepZ);
        }
    }

    private record RelativeOffset(int sideSteps, int forwardSteps) {
    }
}
