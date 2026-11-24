package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.block.component.BlockLootHandler;
import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.enchantment.EnchantmentTypes;
import org.cloudburstmc.api.entity.hostile.Silverfish;
import org.cloudburstmc.api.event.entity.EntityChangeBlockEvent;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.registry.BlockRegistry;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.item.ToolUtils;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.cloudburstmc.server.testutil.BlockTestWorld;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.cloudburstmc.api.block.BlockTypes.*;
import static org.junit.jupiter.api.Assertions.*;

class InfestedBlockHandlersTest {

    private static final BlockRegistry REGISTRY = BlockTestRegistry.get();
    private static final Vector3i POSITION = Vector3i.from(0, 64, 0);

    @Test
    void netheritePickaxeUsesItsMiningSpeedOnInfestedBlocks() {
        ItemStack pickaxe = ItemStack.from(ItemTypes.NETHERITE_PICKAXE);
        List<BlockType> infestedBlocks = List.of(INFESTED_CHISELED_STONE_BRICKS, INFESTED_COBBLESTONE,
                INFESTED_CRACKED_STONE_BRICKS, INFESTED_DEEPSLATE, INFESTED_MOSSY_STONE_BRICKS,
                INFESTED_STONE, INFESTED_STONE_BRICKS);

        for (BlockType type : infestedBlocks) {
            assertEquals(9f, ToolUtils.getMiningSpeed(pickaxe, type.getDefaultState()), type.getId().toString());
        }

        Player player = InterfaceProxy.create(Player.class, Map.of("isOnGround", true));
        assertEquals(0.2D, ToolUtils.getDestroyProgress(player, pickaxe, BlockStates.INFESTED_DEEPSLATE), 1e-6);
    }

    @Test
    void onlySilkTouchDropsTheMatchingHostBlock() {
        Map<BlockType, BlockType> hosts = Map.of(
                INFESTED_STONE, STONE,
                INFESTED_COBBLESTONE, COBBLESTONE,
                INFESTED_STONE_BRICKS, STONE_BRICKS,
                INFESTED_MOSSY_STONE_BRICKS, MOSSY_STONE_BRICKS,
                INFESTED_CRACKED_STONE_BRICKS, CRACKED_STONE_BRICKS,
                INFESTED_CHISELED_STONE_BRICKS, CHISELED_STONE_BRICKS,
                INFESTED_DEEPSLATE, DEEPSLATE
        );
        ItemStack tool = ItemStack.from(ItemTypes.DIAMOND_PICKAXE).toBuilder()
                .setData(ItemDataComponents.ENCHANTMENTS,
                        Map.of(EnchantmentTypes.SILK_TOUCH, new Enchantment(EnchantmentTypes.SILK_TOUCH, 1)))
                .build();
        BlockLootContext ordinary = BlockLootContext.empty(new Random(0));
        BlockLootContext silk = new BlockLootContext(tool, null, new Random(0));
        BlockTestWorld world = new BlockTestWorld(REGISTRY);

        hosts.forEach((infested, host) -> {
            world.setState(POSITION, infested.getDefaultState());
            BlockLootHandler loot = REGISTRY.requireComponent(infested, BlockComponents.GET_LOOT);

            assertSame(infested, REGISTRY.requireComponent(host, BlockComponents.INFESTED_VARIANT));
            assertSame(host, REGISTRY.requireComponent(infested, BlockComponents.INFESTATION_HOST));
            assertTrue(loot.execute(world.block(POSITION), ordinary).isEmpty());
            assertSame(host.asItem().orElseThrow(), loot.execute(world.block(POSITION), silk).getFirst().getType());
        });
    }

    @Test
    void disabledTileDropsPreventReleaseFromBreaksAndExplosions() {
        BlockTestWorld world = new BlockTestWorld(REGISTRY);
        world.setState(POSITION, BlockStates.INFESTED_STONE);
        Block block = world.block(POSITION);
        block.getLevel().getGameRules().set(GameRules.DO_TILE_DROPS, false);

        assertTrue(REGISTRY.requireComponent(INFESTED_STONE, BlockComponents.ON_EXPLOSION_HIT)
                .execute(block, null, null));

        world.setState(POSITION, BlockStates.AIR);
        assertDoesNotThrow(() -> REGISTRY.requireComponent(INFESTED_STONE, BlockComponents.AFTER_BREAK)
                .execute(block, BlockLootContext.empty(new Random(0))));
    }

    @Test
    void infestationAndNonGriefingWakePreserveDeepslateAxis() {
        BlockTestWorld world = new BlockTestWorld(REGISTRY);
        BlockState host = DEEPSLATE.getDefaultState().withTrait(BlockTraits.AXIS, Direction.Axis.X);
        world.setState(POSITION, host);
        Level level = world.block(POSITION).getLevel();
        Silverfish silverfish = silverfish(level);
        level.getGameRules().set(GameRules.MOB_GRIEFING, true);

        assertTrue(InfestedBlockHandlers.infest(world.block(POSITION), silverfish));
        BlockState infested = world.state(POSITION);
        assertSame(INFESTED_DEEPSLATE, infested.getType());
        assertEquals(Direction.Axis.X, infested.ensureTrait(BlockTraits.AXIS));

        level.getGameRules().set(GameRules.MOB_GRIEFING, false);
        assertTrue(InfestedBlockHandlers.wake(world.block(POSITION), silverfish));
        assertSame(host, world.state(POSITION));
        assertSame(host, world.getEvents(EntityChangeBlockEvent.class).getLast().getBlockState());
        assertEquals(0, world.getBreakCount());
    }

    @Test
    void mergeDoesNotReplaceAHostChangedDuringTheEvent() {
        BlockTestWorld world = new BlockTestWorld(REGISTRY);
        world.setState(POSITION, BlockStates.STONE);
        Level level = world.block(POSITION).getLevel();
        Silverfish silverfish = silverfish(level);
        level.getGameRules().set(GameRules.MOB_GRIEFING, true);
        world.onEvent(EntityChangeBlockEvent.class, _ -> world.setState(POSITION, BlockStates.COBBLESTONE));

        assertFalse(InfestedBlockHandlers.infest(world.block(POSITION), silverfish));
        assertSame(BlockStates.COBBLESTONE, world.state(POSITION));
        EntityChangeBlockEvent event = world.getEvents(EntityChangeBlockEvent.class).getFirst();
        assertSame(silverfish, event.getEntity());
        assertSame(BlockStates.INFESTED_STONE, event.getBlockState());
    }

    @Test
    void griefingWakeCanBeCancelledBeforeBreaking() {
        BlockTestWorld world = new BlockTestWorld(REGISTRY);
        world.setState(POSITION, BlockStates.INFESTED_STONE);
        Level level = world.block(POSITION).getLevel();
        Silverfish silverfish = silverfish(level);
        level.getGameRules().set(GameRules.MOB_GRIEFING, true);
        world.onEvent(EntityChangeBlockEvent.class, event -> event.setCancelled(true));

        assertFalse(InfestedBlockHandlers.wake(world.block(POSITION), silverfish));
        assertSame(BlockStates.INFESTED_STONE, world.state(POSITION));
        assertSame(BlockStates.AIR, world.getEvents(EntityChangeBlockEvent.class).getFirst().getBlockState());
        assertEquals(0, world.getBreakCount());

        world.onEvent(EntityChangeBlockEvent.class, event -> event.setCancelled(false));
        assertTrue(InfestedBlockHandlers.wake(world.block(POSITION), silverfish));
        assertEquals(1, world.getBreakCount());
    }

    private static Silverfish silverfish(Level level) {
        return InterfaceProxy.create(Silverfish.class, Map.of("isAlive", true, "getLevel", level));
    }
}
