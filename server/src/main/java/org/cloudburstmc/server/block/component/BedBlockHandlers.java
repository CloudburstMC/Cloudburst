package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
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
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.level.particle.DestroyBlockParticle;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.BlockEntityRegistry;
import org.cloudburstmc.server.registry.CloudBlockRegistry;

@UtilityClass
public class BedBlockHandlers {

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

        Block footBelow = level.getBlock(footPos.getX(), footPos.getY() - 1, footPos.getZ());
        Block headBelow = level.getBlock(headPos.getX(), headPos.getY() - 1, headPos.getZ());
        if (!CloudBlockRegistry.REGISTRY.getComponent(footBelow.getState().getType(), BlockComponents.SOLID).get()
                || !CloudBlockRegistry.REGISTRY.getComponent(headBelow.getState().getType(), BlockComponents.SOLID).get()) {
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

    private static void spawnBedEntity(CloudLevel level, Vector3i pos, DyeColor color) {
        CloudChunk chunk = level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk == null) {
            return;
        }

        Bed entity = BlockEntityRegistry.get().newEntity(BlockEntityTypes.BED, chunk, pos);
        entity.setColor(color);
        entity.spawnToAll();
    }

    /**
     * Returns the head-block position for the given bed block.
     */
    private static Vector3i resolveHeadPos(Block block) {
        boolean isHead;
        try {
            isHead = block.getState().ensureTrait(BlockTraits.IS_HEAD_PIECE);
        } catch (Exception e) {
            isHead = true;
        }

        if (isHead) {
            return block.getPosition();
        }

        Direction facing;
        try {
            facing = block.getState().ensureTrait(BlockTraits.DIRECTION);
        } catch (Exception e) {
            facing = Direction.NORTH;
        }

        Vector3i foot = block.getPosition();
        return Vector3i.from(
                foot.getX() + facing.getStepX(),
                foot.getY(),
                foot.getZ() + facing.getStepZ()
        );
    }

    /**
     * Returns the position of the partner half of the bed relative to
     * {@code block}.
     */
    private static Vector3i resolvePartnerPos(Block block) {
        Direction facing;
        try {
            facing = block.getState().ensureTrait(BlockTraits.DIRECTION);
        } catch (Exception e) {
            facing = Direction.NORTH;
        }

        boolean isHead;
        try {
            isHead = block.getState().ensureTrait(BlockTraits.IS_HEAD_PIECE);
        } catch (Exception e) {
            isHead = false;
        }

        Vector3i pos = block.getPosition();
        int stepX = facing.getStepX();
        int stepZ = facing.getStepZ();

        if (isHead) {
            return Vector3i.from(pos.getX() - stepX, pos.getY(), pos.getZ() - stepZ);
        } else {
            return Vector3i.from(pos.getX() + stepX, pos.getY(), pos.getZ() + stepZ);
        }
    }
}
