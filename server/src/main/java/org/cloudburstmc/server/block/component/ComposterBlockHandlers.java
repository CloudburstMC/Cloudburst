package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.component.TickBlockHandler;
import org.cloudburstmc.api.block.component.UseBlockHandler;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.event.block.CompostItemEvent;
import org.cloudburstmc.api.event.entity.EntityChangeBlockEvent;
import org.cloudburstmc.api.event.entity.EntityCompostItemEvent;
import org.cloudburstmc.api.event.inventory.InventoryMoveItemEvent;
import org.cloudburstmc.api.inventory.view.ComposterView;
import org.cloudburstmc.api.inventory.view.SlotGroup;
import org.cloudburstmc.api.inventory.view.SlotGroupType;
import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.api.item.ItemBehaviors;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudItemRegistry;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

@UtilityClass
public class ComposterBlockHandlers {

    private static final int FULL = 7;
    private static final int READY = 8;
    private static final int MATURATION_TICKS = 20;

    public static final UseBlockHandler USE = (block, player, _, item) -> {
        if (!(player instanceof CloudPlayer cloudPlayer)) {
            return false;
        }

        int fill = level(block);
        if (fill == READY) {
            empty(block);
            world(block).dropBlockContents(block.getPosition().add(0, 1, 0), boneMeal());
            return true;
        }

        Integer chance = compostingChance(item);
        if (chance == null) {
            return false;
        }

        if (fill == FULL) {
            return true;
        }

        BlockState before = block.getState();
        EntityCompostItemEvent event = new EntityCompostItemEvent(cloudPlayer, block, item.withCount(1), roll(block, chance));
        cloudPlayer.getServer().getEventManager().fire(event);
        BlockState after = filledState(before, event.getLevelsToRaise());
        if (!event.isCancelled() && after != before) {
            EntityChangeBlockEvent change = new EntityChangeBlockEvent(cloudPlayer, block, after);
            cloudPlayer.getServer().getEventManager().fire(change);
            if (change.isCancelled()) {
                return true;
            }
        }

        if (event.isCancelled() || cloudPlayer.getLevel() != block.getLevel() || isStale(block, before) || !item.equals(cloudPlayer.getInventory().getSelectedItem())) {
            return true;
        }

        if (!cloudPlayer.isCreative()) {
            cloudPlayer.getInventory().setSelectedItem(item.decreaseCount());
        }

        applyFill(block, before, after);

        return true;
    };

    public static final TickBlockHandler ON_TICK = (block, _) -> {
        if (level(block) == FULL) {
            block.set(block.getState().withTrait(BlockTraits.COMPOSTER_FILL_LEVEL, READY), false, true);
            world(block).addLevelSoundEvent(block.getPosition(), SoundEvent.COMPOSTER_READY);
        }
    };

    public static boolean insertFromAbove(Block block, SlotGroup source, CloudContainer inventory) {
        if (level(block) >= FULL) {
            return false;
        }

        BlockState before = block.getState();
        ComposterView input = new TransferSlot(block, ItemStack.EMPTY);
        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack original = inventory.getItem(slot);
            if (compostingChance(original) == null) {
                continue;
            }

            InventoryMoveItemEvent transfer = InventoryMoveItemEvent.transfer(source, input, source, original.withCount(1));
            world(block).getServer().getEventManager().fire(transfer);
            if (transfer.isCancelled() || isClosed(source) || isStale(block, before) || inventory.getItem(slot) != original) {
                return false;
            }

            ItemStack item = transfer.getItem();
            Integer chance = compostingChance(item);
            if (chance == null || item.getCount() != 1) {
                return false;
            }

            CompostItemEvent event = new CompostItemEvent(block, item, roll(block, chance));
            world(block).getServer().getEventManager().fire(event);
            if (isClosed(source) || isStale(block, before) || inventory.getItem(slot) != original) {
                return false;
            }

            inventory.setItem(slot, original.decreaseCount());
            applyFill(block, before, filledState(before, event.getLevelsToRaise()));

            return true;
        }

        return false;
    }

    public static boolean extractFromBelow(Block block, SlotGroup target, CloudContainer inventory) {
        ItemStack result = boneMeal();
        if (level(block) != READY || !inventory.canAddItem(result)) {
            return false;
        }

        BlockState before = block.getState();
        InventoryMoveItemEvent transfer = InventoryMoveItemEvent.transfer(new TransferSlot(block, result), target, target, result);
        world(block).getServer().getEventManager().fire(transfer);
        ItemStack item = transfer.getItem();
        if (transfer.isCancelled() || isClosed(target) || isStale(block, before) || item.isEmpty() || item.getCount() != 1 || !inventory.canAddItem(item)) {
            return false;
        }

        empty(block);
        for (ItemStack remaining : inventory.addItem(item)) {
            world(block).dropBlockContents(block.getPosition(), remaining);
        }

        return true;
    }

    private static ItemStack boneMeal() {
        return ItemStack.from(ItemTypes.BONE_MEAL);
    }

    private static @Nullable Integer compostingChance(ItemStack item) {
        return item.isEmpty() ? null : CloudItemRegistry.get().getComponent(item.getType(), ItemBehaviors.COMPOSTING_CHANCE);
    }

    private static int roll(Block block, int chance) {
        return level(block) == 0 || world(block).getRandom().nextInt(100) < chance ? 1 : 0;
    }

    private static BlockState filledState(BlockState state, int layers) {
        int fill = state.ensureTrait(BlockTraits.COMPOSTER_FILL_LEVEL);
        return layers == 0 ? state : state.withTrait(BlockTraits.COMPOSTER_FILL_LEVEL, fill + Math.min(layers, FULL - fill));
    }

    private static void applyFill(Block block, BlockState before, BlockState after) {
        CloudLevel level = world(block);
        boolean raised = after != before;
        if (raised) {
            block.set(after, false, true);
            if (after.ensureTrait(BlockTraits.COMPOSTER_FILL_LEVEL) == FULL) {
                level.scheduleUpdate(level.getBlock(block.getPosition()), MATURATION_TICKS);
            }
        }

        level.addLevelEvent(block.getPosition().toFloat().add(0.5f, 0.5f, 0.5f), LevelEvent.PARTICLE_CROP_GROWTH, 0);
        level.addLevelSoundEvent(block.getPosition(), raised ? SoundEvent.COMPOSTER_FILL_LAYER : SoundEvent.COMPOSTER_FILL);
    }

    private static void empty(Block block) {
        block.set(block.getState().withTrait(BlockTraits.COMPOSTER_FILL_LEVEL, 0), false, true);
        world(block).addLevelSoundEvent(block.getPosition(), SoundEvent.COMPOSTER_EMPTY);
    }

    private static int level(Block block) {
        return block.getState().ensureTrait(BlockTraits.COMPOSTER_FILL_LEVEL);
    }

    private static CloudLevel world(Block block) {
        return (CloudLevel) block.getLevel();
    }

    private static boolean isStale(Block block, BlockState expected) {
        Block current = world(block).getLoadedBlock(block.getPosition());
        return current == null || current.getState() != expected;
    }

    private static boolean isClosed(SlotGroup group) {
        return group instanceof BlockEntity entity && entity.isClosed();
    }

    private record TransferSlot(Block block, ItemStack item) implements ComposterView {
        @Override
        public Block getBlock() {
            return this.block;
        }

        @Override
        public SlotGroupType<ComposterView> getSlotGroupType() {
            return SlotGroupTypes.COMPOSTER;
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public ItemStack getItem(int slot) {
            Objects.checkIndex(slot, this.size());
            return this.item;
        }

        @Override
        public void setItem(int slot, ItemStack item) {
            throw new UnsupportedOperationException("Composter transfer views are read-only");
        }
    }
}
