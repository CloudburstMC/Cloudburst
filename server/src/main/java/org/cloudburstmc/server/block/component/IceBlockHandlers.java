package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockLootContext;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.component.BlockAfterBreakHandler;
import org.cloudburstmc.api.block.component.TickBlockHandler;
import org.cloudburstmc.api.enchantment.EnchantmentTypes;
import org.cloudburstmc.api.event.block.BlockFadeEvent;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.server.block.util.BlockSupport;
import org.cloudburstmc.server.level.CloudLevel;

@UtilityClass
public class IceBlockHandlers {

    public static final BlockAfterBreakHandler AFTER_BREAK = (block, context) ->
            afterBreak(block, context, ((CloudLevel) block.getLevel()).getDimension() == CloudLevel.DIMENSION_NETHER);

    public static final TickBlockHandler ON_RANDOM_TICK = (block, random) -> {
        CloudLevel level = (CloudLevel) block.getLevel();
        randomTick(block, level.getBlockLightAt(block.getX(), block.getY(), block.getZ()), level.getDimension() == CloudLevel.DIMENSION_NETHER);
    };

    public static void afterBreak(Block block, BlockLootContext context, boolean waterEvaporates) {
        if (waterEvaporates || context.entity() instanceof Player player && player.isCreative()
                || context.enchantmentLevel(EnchantmentTypes.SILK_TOUCH) > 0
                || block.getLevel().getBlockState(block.getPosition()) != BlockStates.AIR) {
            return;
        }

        Block below = block.getLevel().getLoadedBlock(block.getPosition().add(0, -1, 0));
        if (below != null && (BlockSupport.blocksMotion(below.getState()) || below.getState().getType().isLiquid())) {
            block.getLevel().setBlockState(block.getPosition(), BlockStates.WATER);
        }
    }

    public static void randomTick(Block block, int blockLight, boolean waterEvaporates) {
        if (blockLight > 11 - block.getState().getLightDampening()) {
            melt(block, waterEvaporates);
        }
    }

    public static boolean melt(Block block, boolean waterEvaporates) {
        return fade(block, waterEvaporates ? BlockStates.AIR : BlockStates.WATER);
    }

    public static boolean fade(Block block, BlockState replacement) {
        BlockFadeEvent event = new BlockFadeEvent(block, replacement);
        block.getLevel().getServer().getEventManager().fire(event);
        return !event.isCancelled() && block.getLevel().getBlockState(block.getPosition()) == block.getState()
                && block.getLevel().setBlockState(block.getPosition(), event.getNewState(), false, true);
    }
}
