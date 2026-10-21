package org.cloudburstmc.server.item.component;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockChange;
import org.cloudburstmc.api.block.BlockComponents;
import org.cloudburstmc.api.block.FertilizationResult;
import org.cloudburstmc.api.event.block.BlockFertilizeEvent;
import org.cloudburstmc.api.event.level.StructureGrowEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.component.UseOnHandler;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.sound.SoundTypes;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.server.block.component.UnderwaterPlantBlockHandlers;
import org.cloudburstmc.server.block.util.BlockSupport;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.network.LevelEffectPacketFactory;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BoneMealItemHandlers {

    public static final UseOnHandler USE_ON = (item, entity, position, face, click) -> {
        if (!(entity instanceof CloudPlayer player)) {
            return item;
        }

        CloudLevel level = player.getLevel();
        Block block = level.getBlock(position);
        FertilizationResult result = block.requireComponent(BlockComponents.FERTILIZE).execute(block, player, ThreadLocalRandom.current());
        if (!result.succeeded() && BlockSupport.isFaceSturdy(level, position, face)) {
            Block water = level.getLoadedBlock(face.relative(position));
            if (water != null) {
                result = UnderwaterPlantBlockHandlers.spread(water, face, ThreadLocalRandom.current());
                if (result.succeeded()) {
                    block = water;
                }
            }
        }

        if (!applyFertilization(block, player, result)) {
            return item;
        }

        level.addChunkPacket(block.getPosition(), LevelEffectPacketFactory.cropGrowth(block.getPosition()));
        level.playSound(block.getPosition(), SoundTypes.ITEM_BONE_MEAL_USE);
        return player.isCreative() ? item : item.decreaseCount();
    };

    public static boolean applyFertilization(Block block, @Nullable Player player, FertilizationResult result) {
        if (!result.succeeded()) {
            return false;
        }

        if (result.changes().isEmpty() && result.drops().isEmpty()) {
            return true;
        }

        Level level = block.getLevel();
        List<BlockChange> changes = result.changes();
        boolean cancelled = false;
        if (result.treeType() != null) {
            StructureGrowEvent growth = new StructureGrowEvent(Location.from(block.getPosition(), level), result.treeType(), true, player, changes);
            level.getServer().getEventManager().fire(growth);
            changes = growth.getBlocks();
            cancelled = growth.isCancelled();
        }

        BlockFertilizeEvent event = new BlockFertilizeEvent(block, player, changes);
        event.setCancelled(cancelled);
        level.getServer().getEventManager().fire(event);
        if (event.isCancelled() || level.getBlockState(block.getPosition()) != block.getState()) {
            return false;
        }

        for (BlockChange change : event.getBlocks()) {
            Block current = level.getLoadedBlock(change.block().getPosition());
            if (change.block().getLevel() != level || current == null || current.getState() != change.block().getState()
                    || current.getSecondaryState() != change.block().getSecondaryState()) {
                return false;
            }
        }

        level.batchBlockUpdates(() -> {
            for (BlockChange change : event.getBlocks()) {
                change.block().set(change.newState());
            }
        });

        for (ItemStack drop : result.drops()) {
            level.dropItem(block.getPosition().toFloat().add(0.5f, 0.5f, 0.5f), drop);
        }

        return true;
    }
}
