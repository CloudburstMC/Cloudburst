package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.block.component.BlockAfterBreakHandler;
import org.cloudburstmc.api.block.component.BlockExplosionHandler;
import org.cloudburstmc.api.block.component.BlockLootHandler;
import org.cloudburstmc.api.enchantment.EnchantmentTypes;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.hostile.Silverfish;
import org.cloudburstmc.api.event.entity.CreatureSpawnEvent;
import org.cloudburstmc.api.event.entity.CreatureSpawnReason;
import org.cloudburstmc.api.event.entity.EntityChangeBlockEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.registry.CloudEntityRegistry;

import java.util.List;

@UtilityClass
public class InfestedBlockHandlers {

    public static final BlockLootHandler GET_LOOT = (block, context) -> {
        if (context.enchantmentLevel(EnchantmentTypes.SILK_TOUCH) == 0) {
            return List.of();
        }

        BlockState host = hostState(block);
        return host == null ? List.of() : List.of(ItemStack.from(host));
    };

    public static final BlockAfterBreakHandler AFTER_BREAK = (block, context) -> {
        if (context.entity() instanceof Player player && player.isCreative() || context.enchantmentLevel(EnchantmentTypes.SILK_TOUCH) > 0) {
            return;
        }

        Block replacement = block.getLevel().getLoadedBlock(block.getPosition());
        if (replacement != null && replacement.getState() == BlockStates.AIR) {
            releaseSilverfish(block);
        }
    };

    public static final BlockExplosionHandler ON_EXPLOSION_HIT = (block, cause, sourceBlock) -> {
        releaseSilverfish(block);
        return isCurrent(block);
    };

    public static boolean infest(Block block, Silverfish silverfish) {
        Level level = block.getLevel();
        BlockType type = block.getComponent(BlockComponents.INFESTED_VARIANT);
        if (type == null || !level.getGameRules().get(GameRules.MOB_GRIEFING)) {
            return false;
        }

        BlockState replacement = type.getDefaultState().copyTraits(block.getState());
        return approveChange(block, silverfish, replacement)
                && level.getGameRules().get(GameRules.MOB_GRIEFING)
                && level.setBlockState(block.getPosition(), replacement
        );
    }

    public static boolean wake(Block block, Silverfish silverfish) {
        BlockState host = hostState(block);
        if (host == null) {
            return false;
        }

        Level level = block.getLevel();
        boolean griefing = level.getGameRules().get(GameRules.MOB_GRIEFING);
        if (!approveChange(block, silverfish, griefing ? BlockStates.AIR : host) || griefing != level.getGameRules().get(GameRules.MOB_GRIEFING)) {
            return false;
        }

        return griefing ? level.breakBlock(block.getPosition(), ItemStack.EMPTY, null, true) != null : level.setBlockState(block.getPosition(), host);
    }

    private static @Nullable BlockState hostState(Block block) {
        BlockType host = block.getComponent(BlockComponents.INFESTATION_HOST);
        return host == null ? null : host.getDefaultState().copyTraits(block.getState());
    }

    private static boolean approveChange(Block block, Silverfish silverfish, BlockState replacement) {
        Level level = block.getLevel();
        EntityChangeBlockEvent event = new EntityChangeBlockEvent(silverfish, block, replacement);
        level.getServer().getEventManager().fire(event);
        return !event.isCancelled() && silverfish.isAlive() && !silverfish.isClosed() && silverfish.getLevel() == level && isCurrent(block);
    }

    private static boolean isCurrent(Block block) {
        Block current = block.getLevel().getLoadedBlock(block.getPosition());
        return current != null && current.getState() == block.getState();
    }

    private static void releaseSilverfish(Block block) {
        Level level = block.getLevel();
        if (!level.getGameRules().get(GameRules.DO_TILE_DROPS)) {
            return;
        }

        Vector3i position = block.getPosition();
        Location location = Location.from(position.toFloat().add(0.5f, 0f, 0.5f), level);
        Silverfish silverfish = CloudEntityRegistry.get().newEntity(EntityTypes.SILVERFISH, location);
        ((CloudEntity) silverfish).spawn(new CreatureSpawnEvent(silverfish, CreatureSpawnReason.SILVERFISH_BLOCK));
    }
}
