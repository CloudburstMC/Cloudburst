package org.cloudburstmc.server.block.component;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.block.component.*;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.api.blockentity.FlowerPot;
import org.cloudburstmc.api.event.player.PlayerFlowerPotManipulateEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.server.registry.CloudBlockEntityRegistry;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FlowerPotBlockHandlers {

    public static final SurviveBlockHandler CAN_SURVIVE = block -> {
        Block below = block.getSide(Direction.DOWN);
        return !below.getState().is(BlockTags.TRAPDOOR) && below.isFaceSturdy(Direction.UP, SupportType.CENTER);
    };

    public static final ComplexBlockHandler ON_ADDED = block -> {
        if (block.getLevel().getBlockEntity(block.getPosition()) == null) {
            CloudBlockEntityRegistry.get().newEntity(BlockEntityTypes.FLOWER_POT, block);
        }
    };

    public static final UseBlockHandler USE = (block, player, face, held) -> {
        BlockEntity current = block.getLevel().getBlockEntity(block.getPosition());
        FlowerPot pot = current instanceof FlowerPot flowerPot ? flowerPot : null;
        BlockState contents = pot == null ? BlockStates.AIR : pot.getPlant();
        boolean placing = contents == BlockStates.AIR;
        BlockState next = BlockStates.AIR;
        ItemStack plantItem;

        if (placing) {
            BlockState heldBlock = held.getBlockState().orElse(BlockStates.AIR);
            if (!heldBlock.is(BlockTags.POTTABLE_PLANTS) || (current != null && pot == null)) {
                return false;
            }
            next = heldBlock.getType().getDefaultState();
            plantItem = held.withCount(1);
        } else {
            if (!held.isEmpty()) {
                return false;
            }
            plantItem = ItemStack.from(contents);
        }

        PlayerFlowerPotManipulateEvent event = new PlayerFlowerPotManipulateEvent(player, block, plantItem, placing);
        player.getServer().getEventManager().fire(event);
        if (event.isCancelled() || !player.getInventory().getSelectedItem().equals(held)
                || block.getLevel().getBlockState(block.getPosition()).getType() != BlockTypes.FLOWER_POT
                || block.getLevel().getBlockEntity(block.getPosition()) != current
                || (pot != null && (pot.isClosed() || pot.getPlant() != contents))) {
            return true;
        }

        if (pot == null) {
            pot = CloudBlockEntityRegistry.get().newEntity(BlockEntityTypes.FLOWER_POT, block);
        }
        pot.setPlant(next);

        if (placing) {
            if (!player.isCreative()) {
                player.getInventory().setSelectedItem(held.decreaseCount());
            }
        } else {
            for (ItemStack remaining : player.getInventory().addItem(plantItem)) {
                block.getLevel().dropItem(player.getPosition(), remaining);
            }
        }

        return true;
    };

    public static final BlockLootHandler GET_LOOT = (block, context) -> {
        ItemStack pot = ItemStack.from(ItemTypes.FLOWER_POT);
        ItemStack plant = getPlantItem(block);
        return plant.isEmpty() ? List.of(pot) : List.of(pot, plant);
    };

    public static final PickBlockHandler GET_PICK_BLOCK = block -> {
        ItemStack plant = getPlantItem(block);
        return plant.isEmpty() ? ItemStack.from(ItemTypes.FLOWER_POT) : plant;
    };

    private static ItemStack getPlantItem(Block block) {
        if (block.getLevel().getBlockEntity(block.getPosition()) instanceof FlowerPot pot && pot.getPlant() != BlockStates.AIR) {
            return ItemStack.from(pot.getPlant());
        }
        return ItemStack.EMPTY;
    }
}
