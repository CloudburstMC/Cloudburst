package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.enchantment.EnchantmentTypes;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.event.block.BlockFadeEvent;
import org.cloudburstmc.api.event.block.BlockGrowEvent;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.particle.ParticleTypes;
import org.cloudburstmc.api.level.sound.SoundTypes;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.CrackedState;
import org.cloudburstmc.api.util.data.TurtleEggCount;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.cloudburstmc.server.testutil.BlockTestWorld;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.*;

class TurtleEggBlockHandlersTest {

    private static final CloudBlockRegistry REGISTRY = BlockTestRegistry.get();
    private static final Vector3i POSITION = Vector3i.from(0, 64, 0);
    private static final BlockState EGG = BlockTypes.TURTLE_EGG.getDefaultState()
            .withTrait(BlockTraits.TURTLE_EGG_COUNT, TurtleEggCount.ONE_EGG)
            .withTrait(BlockTraits.CRACKED_STATE, CrackedState.NO_CRACKS);
    private static final RandomGenerator HATCH_RANDOM = () -> 0L;

    @Test
    void newEggsOnSandEmitParticlesEvenWhenWaterlogged() {
        BlockTestWorld world = world();
        world.setLiquid(POSITION, LiquidState.of(BlockStates.WATER));

        REGISTRY.requireComponent(BlockTypes.TURTLE_EGG, BlockComponents.ON_ADDED).execute(world.block(POSITION));

        assertEquals(15, world.getParticles().size());
        assertTrue(world.getParticles().stream().allMatch(type -> type == ParticleTypes.VILLAGER_HAPPY));
        assertTrue(world.getSounds().isEmpty());

        world.setState(POSITION.add(0, -1, 0), BlockStates.STONE);
        TurtleEggBlockHandlers.ON_ADDED.execute(world.block(POSITION));
        assertEquals(15, world.getParticles().size());
        assertTrue(world.getSounds().isEmpty());
    }

    @Test
    void addingEachEggOnSandPlaysOneBoneMealSound() {
        BlockTestWorld world = world();
        Player player = InterfaceProxy.create(Player.class, Map.of("getLevel", world.block(POSITION).getLevel()));
        world.setState(POSITION, BlockStates.AIR);

        assertTrue(TurtleEggBlockHandlers.ON_PLACE.execute(EGG, player, POSITION, Direction.UP, Vector3f.ZERO));
        assertTrue(world.getSounds().isEmpty());

        for (TurtleEggCount count : List.of(TurtleEggCount.TWO_EGG, TurtleEggCount.THREE_EGG, TurtleEggCount.FOUR_EGG)) {
            BlockState stacked = EGG.withTrait(BlockTraits.TURTLE_EGG_COUNT, count);
            assertTrue(REGISTRY.requireComponent(BlockTypes.TURTLE_EGG, BlockComponents.ON_PLACE)
                    .execute(stacked, player, POSITION, Direction.UP, Vector3f.ZERO));
            assertEquals(count.ordinal(), world.getSounds().size());
        }

        assertTrue(world.getSounds().stream().allMatch(sound -> sound == SoundTypes.ITEM_BONE_MEAL_USE));

        world.setState(POSITION.add(0, -1, 0), BlockStates.STONE);
        world.setState(POSITION, EGG);
        assertTrue(TurtleEggBlockHandlers.ON_PLACE.execute(EGG.withTrait(BlockTraits.TURTLE_EGG_COUNT, TurtleEggCount.TWO_EGG),
                player, POSITION, Direction.UP, Vector3f.ZERO));
        assertEquals(3, world.getSounds().size());
    }

    @Test
    void addingEggsOrCrackingAnExistingClusterDoesNotReplayPlacementParticles() {
        BlockTestWorld world = world();
        Player player = InterfaceProxy.create(Player.class, Map.of("isSneaking", false));
        BlockState stacked = TurtleEggBlockHandlers.RESOLVE_PLACEMENT_STATE.execute(EGG, world.block(POSITION), player, Direction.UP, Vector3f.ZERO);
        assertNotNull(stacked);
        world.setState(POSITION, stacked);

        tick(world);

        assertTrue(world.getParticles().isEmpty());
        assertEquals(List.of(SoundTypes.BLOCK_TURTLE_EGG_CRACK), world.getSounds());
    }

    @Test
    void newClustersRequireSupportRegardlessOfSneakingOrClickedFace() {
        BlockTestWorld world = world();
        world.setState(POSITION, EGG.withTrait(BlockTraits.TURTLE_EGG_COUNT, TurtleEggCount.FOUR_EGG)
                .withTrait(BlockTraits.CRACKED_STATE, CrackedState.MAX_CRACKED));
        for (boolean sneaking : List.of(false, true)) {
            Player player = InterfaceProxy.create(Player.class, Map.of("isSneaking", sneaking));

            for (Vector3i position : List.of(POSITION.add(0, 1, 0), POSITION.add(1, 0, 0))) {
                for (Direction face : List.of(Direction.UP, Direction.EAST)) {
                    assertNull(TurtleEggBlockHandlers.RESOLVE_PLACEMENT_STATE.execute(EGG, world.block(position), player, face, Vector3f.ZERO));

                    Vector3i support = position.add(0, -1, 0);
                    BlockState previous = world.state(support);
                    world.setState(support, BlockStates.STONE);
                    BlockState placed = TurtleEggBlockHandlers.RESOLVE_PLACEMENT_STATE.execute(EGG, world.block(position), player, face, Vector3f.ZERO);

                    assertNotNull(placed);
                    assertEquals(TurtleEggCount.ONE_EGG, placed.ensureTrait(BlockTraits.TURTLE_EGG_COUNT));
                    assertEquals(CrackedState.NO_CRACKS, placed.ensureTrait(BlockTraits.CRACKED_STATE));
                    world.setState(support, previous);
                }
            }
        }
    }

    @Test
    void stacksToFourAndPreservesCracksButDoesNotStackWhileSneaking() {
        BlockTestWorld world = world();
        world.setState(POSITION, EGG.withTrait(BlockTraits.CRACKED_STATE, CrackedState.CRACKED));
        Player standing = InterfaceProxy.create(Player.class, Map.of("isSneaking", false));
        Player sneaking = InterfaceProxy.create(Player.class, Map.of("isSneaking", true));

        for (TurtleEggCount count : List.of(TurtleEggCount.TWO_EGG, TurtleEggCount.THREE_EGG, TurtleEggCount.FOUR_EGG)) {
            Block block = world.block(POSITION);
            assertTrue(TurtleEggBlockHandlers.CAN_BE_REPLACED.execute(block, EGG, standing, Direction.UP, Vector3f.ZERO));
            assertFalse(TurtleEggBlockHandlers.CAN_BE_REPLACED.execute(block, EGG, sneaking, Direction.UP, Vector3f.ZERO));

            BlockState placed = TurtleEggBlockHandlers.RESOLVE_PLACEMENT_STATE.execute(EGG, block, standing, Direction.UP, Vector3f.ZERO);
            assertNotNull(placed);
            assertEquals(count, placed.ensureTrait(BlockTraits.TURTLE_EGG_COUNT));
            assertEquals(CrackedState.CRACKED, placed.ensureTrait(BlockTraits.CRACKED_STATE));
            world.setState(POSITION, placed);
        }

        assertFalse(TurtleEggBlockHandlers.CAN_BE_REPLACED.execute(world.block(POSITION), EGG, standing, Direction.UP, Vector3f.ZERO));
        assertNull(TurtleEggBlockHandlers.RESOLVE_PLACEMENT_STATE.execute(EGG, world.block(POSITION), standing, Direction.UP, Vector3f.ZERO));
    }

    @Test
    void survivalMiningRemovesOneEggWhileCreativeRemovesTheCluster() {
        BlockTestWorld world = world();
        world.setState(POSITION, EGG.withTrait(BlockTraits.TURTLE_EGG_COUNT, TurtleEggCount.FOUR_EGG)
                .withTrait(BlockTraits.CRACKED_STATE, CrackedState.CRACKED));
        Player survival = InterfaceProxy.create(Player.class, Map.of("isCreative", false));

        for (TurtleEggCount remaining : List.of(TurtleEggCount.THREE_EGG, TurtleEggCount.TWO_EGG, TurtleEggCount.ONE_EGG)) {
            TurtleEggBlockHandlers.ON_DESTROY.execute(world.block(POSITION), survival);
            assertEquals(remaining, world.state(POSITION).ensureTrait(BlockTraits.TURTLE_EGG_COUNT));
            assertEquals(CrackedState.CRACKED, world.state(POSITION).ensureTrait(BlockTraits.CRACKED_STATE));
        }

        TurtleEggBlockHandlers.ON_DESTROY.execute(world.block(POSITION), survival);
        assertSame(BlockStates.AIR, world.state(POSITION));

        world.setState(POSITION, EGG.withTrait(BlockTraits.TURTLE_EGG_COUNT, TurtleEggCount.FOUR_EGG));
        Player creative = InterfaceProxy.create(Player.class, Map.of("isCreative", true));
        TurtleEggBlockHandlers.ON_DESTROY.execute(world.block(POSITION), creative);
        assertSame(BlockStates.AIR, world.state(POSITION));
        assertEquals(5, world.getSounds().size());
        assertTrue(world.getSounds().stream().allMatch(sound -> sound == SoundTypes.BLOCK_TURTLE_EGG_BREAK));
    }

    @Test
    void cracksOnlyOnSandAndStopsWhenTheSupportIsUnloaded() {
        assertTrue(REGISTRY.requireComponent(BlockTypes.TURTLE_EGG, BlockComponents.CAN_RANDOM_TICK));

        for (BlockType support : List.of(BlockTypes.SAND, BlockTypes.RED_SAND, BlockTypes.SUSPICIOUS_SAND)) {
            BlockTestWorld world = world();
            world.setState(POSITION.add(0, -1, 0), support.getDefaultState());

            tick(world);
            assertEquals(CrackedState.CRACKED, world.state(POSITION).ensureTrait(BlockTraits.CRACKED_STATE));

            tick(world);
            assertEquals(CrackedState.MAX_CRACKED, world.state(POSITION).ensureTrait(BlockTraits.CRACKED_STATE));
        }

        BlockTestWorld world = world();
        world.setState(POSITION.add(0, -1, 0), BlockStates.STONE);
        tick(world);
        assertSame(EGG, world.state(POSITION));

        world.setState(POSITION.add(0, -1, 0), BlockTypes.SAND.getDefaultState());
        world.unload(POSITION.add(0, -1, 0));
        tick(world);
        assertSame(EGG, world.state(POSITION));
    }

    @Test
    void growthCancellationAndListenerChangesAreRespected() {
        BlockTestWorld world = world();
        world.onEvent(BlockGrowEvent.class, event -> event.setCancelled(true));
        tick(world);
        assertSame(EGG, world.state(POSITION));

        world.onEvent(BlockGrowEvent.class, event -> world.setState(POSITION, BlockStates.STONE));
        tick(world);
        assertSame(BlockStates.STONE, world.state(POSITION));
    }

    @Test
    void cancelledOrReplacedHatchingDoesNotSpawnTurtlesOrBreakParticles() {
        BlockTestWorld world = world();
        BlockState cracked = EGG.withTrait(BlockTraits.CRACKED_STATE, CrackedState.MAX_CRACKED);
        world.setState(POSITION, cracked);
        world.onEvent(BlockFadeEvent.class, event -> event.setCancelled(true));

        tick(world);
        assertSame(cracked, world.state(POSITION));
        assertEquals(1, world.getEvents(BlockFadeEvent.class).size());
        assertTrue(world.getBlockBreakEffects().isEmpty());

        world.onEvent(BlockFadeEvent.class, event -> event.setNewState(BlockStates.STONE));
        tick(world);
        assertSame(BlockStates.STONE, world.state(POSITION));
        assertTrue(world.getBlockBreakEffects().isEmpty());
    }

    @Test
    void silkTouchDropsOnlyTheEggThatIsMined() {
        BlockTestWorld world = world();
        world.setState(POSITION, EGG.withTrait(BlockTraits.TURTLE_EGG_COUNT, TurtleEggCount.FOUR_EGG));
        BlockLootContext context = new BlockLootContext(ItemStack.EMPTY, null, HATCH_RANDOM);

        assertTrue(REGISTRY.requireComponent(BlockTypes.TURTLE_EGG, BlockComponents.GET_LOOT)
                .execute(world.block(POSITION), context).isEmpty());

        ItemStack tool = ItemStack.builder()
                .itemType(ItemTypes.DIAMOND_PICKAXE)
                .setData(ItemDataComponents.ENCHANTMENTS,
                        Map.of(EnchantmentTypes.SILK_TOUCH, new Enchantment(EnchantmentTypes.SILK_TOUCH, 1)))
                .build();
        BlockLootContext silk = new BlockLootContext(tool, null, HATCH_RANDOM);
        List<ItemStack> drops = REGISTRY.requireComponent(BlockTypes.TURTLE_EGG, BlockComponents.GET_LOOT)
                .execute(world.block(POSITION), silk);

        assertEquals(1, drops.size());
        assertSame(ItemTypes.TURTLE_EGG, drops.getFirst().getType());
        assertEquals(1, drops.getFirst().getCount());
    }

    @Test
    void sneakingAndIneligibleEntitiesCannotTrampleEggs() {
        BlockTestWorld world = world();
        Player sneaking = InterfaceProxy.create(Player.class, Map.of("isSneaking", true));
        TurtleEggBlockHandlers.ON_STEP_ON.execute(world.block(POSITION), sneaking);

        for (EntityType<?> type : List.of(EntityTypes.TURTLE, EntityTypes.BAT, EntityTypes.ARROW)) {
            Entity entity = InterfaceProxy.create(Entity.class, Map.of("getType", type));
            TurtleEggBlockHandlers.ON_STEP_ON.execute(world.block(POSITION), entity);
        }

        assertSame(EGG, world.state(POSITION));
        assertTrue(world.getBlockBreakEffects().isEmpty());
    }

    private static void tick(BlockTestWorld world) {
        REGISTRY.requireComponent(BlockTypes.TURTLE_EGG, BlockComponents.ON_RANDOM_TICK).execute(world.block(POSITION), HATCH_RANDOM);
    }

    private static BlockTestWorld world() {
        BlockTestWorld world = new BlockTestWorld(REGISTRY);
        world.setState(POSITION, EGG);
        world.setState(POSITION.add(0, -1, 0), BlockTypes.SAND.getDefaultState());
        return world;
    }
}
