package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.block.component.*;
import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.api.blockentity.ItemFrame;
import org.cloudburstmc.api.event.player.ItemFrameChangeAction;
import org.cloudburstmc.api.event.player.PlayerItemFrameChangeEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.api.level.sound.SoundTypes;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.server.block.util.BlockSupport;
import org.cloudburstmc.server.registry.CloudBlockEntityRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@UtilityClass
public class ItemFrameBlockHandlers {

    private static final PlaceBlockHandler DEFAULT_PLACE = new DefaultBlockPlaceHandler();

    public static final PlacementStateHandler RESOLVE_PLACEMENT_STATE = (state, block, player, face, click) ->
            state.withTrait(BlockTraits.FACING_DIRECTION, face).withTrait(BlockTraits.HAS_MAP, false);

    public static final SurviveBlockHandler CAN_SURVIVE = block -> {
        if (block.getLevel().getBlockEntity(block.getPosition()) instanceof ItemFrame frame && frame.isFixed()) {
            return true;
        }

        Direction facing = block.getState().ensureTrait(BlockTraits.FACING_DIRECTION);
        return BlockSupport.isFaceSturdy(block.getLevel(), facing.getOpposite().relative(block.getPosition()), facing);
    };

    public static final PlaceBlockHandler ON_PLACE = (state, player, position, face, click) -> {
        if (!DEFAULT_PLACE.execute(state, player, position, face, click)) {
            return false;
        }

        CloudBlockEntityRegistry.get().newEntity(state.getType() == BlockTypes.GLOW_FRAME
                ? BlockEntityTypes.GLOW_ITEM_FRAME : BlockEntityTypes.ITEM_FRAME, player.getLevel().getBlock(position));
        player.getLevel().playSound(position, SoundTypes.BLOCK_ITEMFRAME_PLACE, 1, 1);
        return true;
    };

    public static final UseBlockHandler USE = (block, player, direction, held) -> {
        if (!(block.getLevel().getBlockEntity(block.getPosition()) instanceof ItemFrame frame) || frame.isFixed()) {
            return false;
        }

        ItemStack displayed = frame.getItem();
        boolean placing = displayed.isEmpty();
        if (placing && held.isEmpty()) {
            return false;
        }

        PlayerItemFrameChangeEvent event = new PlayerItemFrameChangeEvent(player, frame,
                placing ? held.withCount(1) : displayed,
                placing ? ItemFrameChangeAction.PLACE : ItemFrameChangeAction.ROTATE);
        player.getServer().getEventManager().fire(event);
        if (event.isCancelled() || frame.isFixed() || !frame.getItem().equals(displayed) || !player.getInventory().getSelectedItem().equals(held)) {
            return true;
        }

        frame.setItem(event.getItemStack());
        if (placing) {
            if (!player.isCreative()) {
                player.getInventory().setSelectedItem(held.decreaseCount());
            }
        } else if (!frame.getItem().isEmpty()) {
            frame.setItemRotation((frame.getItemRotation() + 1) & 7);
        }

        block.getLevel().playSound(block.getPosition(), placing ? SoundTypes.BLOCK_ITEMFRAME_ADD_ITEM : SoundTypes.BLOCK_ITEMFRAME_ROTATE_ITEM, 1, 1);
        return true;
    };

    public static final AttackBlockHandler ATTACK = (block, player, direction) -> {
        if (!(block.getLevel().getBlockEntity(block.getPosition()) instanceof ItemFrame frame)) {
            return false;
        }

        if (frame.isFixed()) {
            return !player.isCreative();
        }

        if (frame.getItem().isEmpty()) {
            return false;
        }

        removeItem(block, frame, player);
        return true;
    };

    public static final NeighborBlockHandler ON_NEIGHBOUR_CHANGED = (block, neighbor) -> {
        if (!CAN_SURVIVE.execute(block)) {
            block.getLevel().breakBlock(block.getPosition(), ItemStack.EMPTY, null, true);
        }
    };

    public static final BlockExplosionHandler ON_EXPLOSION_HIT = (block, cause, source) ->
            !(block.getLevel().getBlockEntity(block.getPosition()) instanceof ItemFrame frame) || !frame.isFixed();

    public static final EntityBlockHandler ON_PROJECTILE_HIT = (block, projectile) -> {
        if (!(block.getLevel().getBlockEntity(block.getPosition()) instanceof ItemFrame frame) || frame.isFixed()) {
            return;
        }

        if (frame.getItem().isEmpty()) {
            block.getLevel().breakBlock(block.getPosition());
        } else {
            removeItem(block, frame, projectile.getOwner() instanceof Player player ? player : null);
        }
    };

    public static final BlockLootHandler GET_LOOT = (block, context) -> {
        if (!block.getLevel().getGameRules().get(GameRules.DO_ENTITY_DROPS)) {
            return List.of();
        }

        List<ItemStack> drops = new ArrayList<>(DefaultBlockHandlers.GET_LOOT.execute(block, context));
        if (block.getLevel().getBlockEntity(block.getPosition()) instanceof ItemFrame frame && shouldDrop(block, frame)) {
            drops.add(frame.getItem());
        }

        return drops;
    };

    public static final ComplexBlockHandler ON_REMOVE = block -> {
        if (block.getLevel().getBlockEntity(block.getPosition()) instanceof ItemFrame frame) {
            if (shouldDrop(block, frame)) {
                block.getLevel().dropItem(block.getPosition(), frame.getItem());
            }

            frame.setItem(ItemStack.EMPTY);
        }
    };

    public static final BlockDestroyHandler ON_DESTROY = (block, cause) -> {
        block.getLevel().playSound(block.getPosition(), SoundTypes.BLOCK_ITEMFRAME_BREAK, 1, 1);
        block.set(BlockStates.AIR);
    };

    private static void removeItem(Block block, ItemFrame frame, @Nullable Player player) {
        ItemStack displayed = frame.getItem();
        ItemStack drop = displayed;
        if (player != null) {
            PlayerItemFrameChangeEvent event = new PlayerItemFrameChangeEvent(player, frame, displayed, ItemFrameChangeAction.REMOVE);
            player.getServer().getEventManager().fire(event);
            if (event.isCancelled() || frame.isFixed() || !frame.getItem().equals(displayed)) {
                return;
            }

            drop = event.getItemStack();
        }

        if ((player == null || !player.isCreative()) && !drop.isEmpty() && shouldDrop(block, frame)) {
            block.getLevel().dropItem(block.getPosition(), drop);
        }

        frame.setItem(ItemStack.EMPTY);
        block.getLevel().playSound(block.getPosition(), SoundTypes.BLOCK_ITEMFRAME_REMOVE_ITEM, 1, 1);
    }

    private static boolean shouldDrop(Block block, ItemFrame frame) {
        return !frame.getItem().isEmpty() && block.getLevel().getGameRules().get(GameRules.DO_ENTITY_DROPS)
                && ThreadLocalRandom.current().nextFloat() < frame.getItemDropChance();
    }
}
