package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.block.component.ComplexBlockHandler;
import org.cloudburstmc.api.block.component.LiquidChangeBlockHandler;
import org.cloudburstmc.api.block.component.TickBlockHandler;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.passive.HappyGhast;
import org.cloudburstmc.api.event.block.BlockFadeEvent;
import org.cloudburstmc.api.event.block.BlockGrowEvent;
import org.cloudburstmc.api.event.entity.CreatureSpawnEvent;
import org.cloudburstmc.api.event.entity.CreatureSpawnReason;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.sound.SoundTypes;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.registry.CloudEntityRegistry;

@UtilityClass
public class DriedGhastBlockHandlers {

    private static final int HYDRATION_DELAY = 5000;
    private static final int MAX_HYDRATION = 3;
    private static final int GHASTLING_AGE = -24000;

    public static final ComplexBlockHandler ON_ADDED = block -> {
        if (isWaterlogged(block)) {
            block.getLevel().playSound(block.getPosition(), SoundTypes.BLOCK_DRIED_GHAST_PLACE_IN_WATER);
        }
    };

    public static final LiquidChangeBlockHandler ON_LIQUID_CHANGED = (block, previousLiquid) -> {
        if (isWaterlogged(block) && !(previousLiquid.isSource() && previousLiquid.getType().isSameFamily(LiquidTypes.WATER))) {
            block.getLevel().playSound(block.getPosition(), SoundTypes.BLOCK_DRIED_GHAST_PLACE_IN_WATER);
        }
    };

    public static final TickBlockHandler ON_RANDOM_TICK = (block, random) -> {
        if ((isWaterlogged(block) || hydration(block) > 0) && !block.getLevel().isUpdateScheduled(block.getPosition())) {
            block.getLevel().scheduleUpdate(block.getPosition(), HYDRATION_DELAY);
        }
    };

    public static final TickBlockHandler ON_TICK = (block, random) -> {
        int hydration = hydration(block);
        if (isWaterlogged(block)) {
            if (hydration < MAX_HYDRATION) {
                hydrate(block, hydration + 1);
            } else {
                hatch(block);
            }
        } else if (hydration > 0) {
            block.getLevel().setBlockState(block.getPosition(), block.getState().withTrait(BlockTraits.REHYDRATION_LEVEL, hydration - 1));
        }
    };

    private static int hydration(Block block) {
        return block.getState().ensureTrait(BlockTraits.REHYDRATION_LEVEL);
    }

    private static boolean isWaterlogged(Block block) {
        return block.getLiquid().isSource() && block.getLiquid().getType().isSameFamily(LiquidTypes.WATER);
    }

    private static boolean changedOrDry(Block block) {
        Block current = block.getLevel().getBlock(block.getPosition());
        return current.getState() != block.getState() || !isWaterlogged(current);
    }

    private static void hydrate(Block block, int hydration) {
        Level level = block.getLevel();
        BlockState hydrated = block.getState().withTrait(BlockTraits.REHYDRATION_LEVEL, hydration);
        BlockGrowEvent event = new BlockGrowEvent(block, hydrated);
        level.getServer().getEventManager().fire(event);

        if (event.isCancelled() || changedOrDry(block)) {
            return;
        }

        if (level.setBlockState(block.getPosition(), event.getNewState())) {
            level.playSound(block.getPosition(), SoundTypes.BLOCK_DRIED_GHAST_STATE_CHANGE);
        }
    }

    private static void hatch(Block block) {
        Level level = block.getLevel();
        BlockFadeEvent event = new BlockFadeEvent(block, BlockStates.WATER);
        level.getServer().getEventManager().fire(event);

        if (event.isCancelled() || changedOrDry(block)) {
            return;
        }

        BlockState replacement = event.getNewState();
        if (!level.setBlockState(block.getPosition(), replacement) || replacement != BlockStates.WATER) {
            return;
        }

        float yaw = block.getState().ensureTrait(BlockTraits.CARDINAL_DIRECTION).ordinal() * 22.5f;
        Location location = Location.from(block.getPosition().toFloat().add(0.5f, 0, 0.5f), yaw, 0, level);
        HappyGhast ghastling = CloudEntityRegistry.get().newEntity(EntityTypes.HAPPY_GHAST, location);
        ghastling.setAge(GHASTLING_AGE);

        if (((CloudEntity) ghastling).spawn(new CreatureSpawnEvent(ghastling, CreatureSpawnReason.REHYDRATION))) {
            level.playSound(block.getPosition(), SoundTypes.MOB_GHASTLING_SPAWN);
        }
    }
}
