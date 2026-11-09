package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.block.component.BlockShapeContext;
import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.enchantment.EnchantmentTypes;
import org.cloudburstmc.api.event.block.BlockFadeEvent;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.biome.BiomeTypes;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.SlabSlot;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.block.BlockPlacementTarget;
import org.cloudburstmc.server.registry.CloudBiomeRegistry;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.cloudburstmc.server.testutil.BlockTestWorld;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class SnowLayerBlockHandlersTest {

    private static final Vector3i POSITION = Vector3i.from(0, 64, 0);
    private static final Vector3i BELOW = POSITION.sub(0, 1, 0);

    @Test
    void clickingTheSideOfPartialSnowAddsOneLayerToTheClickedStack() {
        BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());
        Block adjacent = world.block(Direction.EAST.relative(POSITION));

        for (int height = 0; height < 7; height++) {
            world.setState(POSITION, BlockStates.SNOW_LAYER.withTrait(BlockTraits.HEIGHT, height));
            Block clicked = world.block(POSITION);
            BlockPlacementContext context = new BlockPlacementContext(clicked, Direction.EAST, Vector3f.ZERO, null);

            BlockPlacementTarget target = BlockPlacementTarget.resolve(BlockStates.SNOW_LAYER, adjacent, context);

            assertNotNull(target, "height=" + height);
            assertSame(clicked, target.block());
            assertSame(Direction.UP, target.placementFace());

            BlockState placed = clicked.requireComponent(BlockComponents.RESOLVE_PLACEMENT_STATE)
                    .execute(BlockStates.SNOW_LAYER, target.block(), null, target.placementFace(), context.clickPosition());

            assertNotNull(placed);
            assertEquals(height + 1, placed.ensureTrait(BlockTraits.HEIGHT).intValue());
        }
    }

    @Test
    void clickingTheSideOfFullSnowSelectsTheAdjacentPosition() {
        BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());
        Block adjacent = world.block(Direction.EAST.relative(POSITION));
        BlockState fullStack = BlockStates.SNOW_LAYER.withTrait(BlockTraits.HEIGHT, 7);

        for (BlockState support : List.of(fullStack, BlockStates.SNOW)) {
            world.setState(POSITION, support);
            Block clicked = world.block(POSITION);
            BlockPlacementContext context = new BlockPlacementContext(clicked, Direction.EAST, Vector3f.ZERO, null);

            BlockPlacementTarget target = BlockPlacementTarget.resolve(BlockStates.SNOW_LAYER, adjacent, context);

            assertNotNull(target);
            assertSame(adjacent, target.block());
            assertSame(Direction.EAST, target.placementFace());
        }
    }

    @Test
    void clickingTheTopOfSnowSelectsTheExistingStack() {
        BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());
        world.setState(POSITION, BlockStates.SNOW_LAYER);
        Block clicked = world.block(POSITION);
        Block above = world.block(Direction.UP.relative(POSITION));
        BlockPlacementContext context = new BlockPlacementContext(clicked, Direction.UP, Vector3f.ZERO, null);

        BlockPlacementTarget target = BlockPlacementTarget.resolve(BlockStates.SNOW_LAYER, above, context);

        assertNotNull(target);
        assertSame(clicked, target.block());
    }

    @Test
    void snowCanStackAtTheAdjacentPositionWithoutATopClick() {
        BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());
        Vector3i adjacentPosition = Direction.EAST.relative(POSITION);
        world.setState(POSITION, BlockStates.STONE);
        world.setState(adjacentPosition, BlockStates.SNOW_LAYER);
        Block clicked = world.block(POSITION);
        Block adjacent = world.block(adjacentPosition);
        BlockPlacementContext context = new BlockPlacementContext(clicked, Direction.EAST, Vector3f.ZERO, null);

        BlockPlacementTarget target = BlockPlacementTarget.resolve(BlockStates.SNOW_LAYER, adjacent, context);

        assertNotNull(target);
        assertSame(adjacent, target.block());
    }

    @Test
    void leavesSupportSnowWithoutProvidingGeneralFaceSupport() {
        BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());
        world.setState(BELOW, BlockStates.SPRUCE_LEAVES);
        Block target = world.block(POSITION);

        assertFalse(BlockStates.SPRUCE_LEAVES.is(BlockTags.SUPPORT_OVERRIDE_SNOW_LAYER));
        assertFalse(world.block(BELOW).isFaceSturdy(Direction.UP, SupportType.FULL));
        assertTrue(BlockStates.SNOW_LAYER.isSupported(target));

        world.setState(POSITION, BlockStates.SNOW_LAYER);
        Block snow = world.block(POSITION);

        assertFalse(snow.requireComponent(BlockComponents.IS_FREE_TO_FALL).execute(snow));
    }

    @Test
    void snowRequiresTheCollisionShapeToReachTheTopFace() {
        BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());
        BlockState bottomSlab = BlockStates.OAK_SLAB.withTrait(BlockTraits.SLAB_SLOT, SlabSlot.BOTTOM);
        BlockState topSlab = bottomSlab.withTrait(BlockTraits.SLAB_SLOT, SlabSlot.TOP);
        BlockShapeContext generationContext = BlockShapeContext.empty();
        Block target = world.block(POSITION);

        world.setState(BELOW, bottomSlab);
        assertFalse(BlockStates.SNOW_LAYER.isSupported(target));
        assertFalse(SnowLayerBlockHandlers.canBeSupportedBy(BlockTestRegistry.get(), bottomSlab, generationContext));

        world.setState(BELOW, topSlab);
        assertTrue(BlockStates.SNOW_LAYER.isSupported(target));
        assertTrue(SnowLayerBlockHandlers.canBeSupportedBy(BlockTestRegistry.get(), topSlab, generationContext));
    }

    @Test
    void snowSupportOverridesTakePrecedenceOverTheTopFace() {
        BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());
        Block target = world.block(POSITION);

        for (BlockState support : List.of(BlockStates.HONEY_BLOCK, BlockStates.SOUL_SAND, BlockStates.MUD)) {
            world.setState(BELOW, support);

            assertTrue(BlockStates.SNOW_LAYER.isSupported(target), support.toString());
        }

        for (BlockState support : List.of(BlockStates.ICE, BlockStates.BARRIER)) {
            world.setState(BELOW, support);

            assertFalse(BlockStates.SNOW_LAYER.isSupported(target), support.toString());
        }
    }

    @Test
    void aFullStackSupportsAnotherLayerDuringTheGravityTick() {
        BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());
        BlockState fullStack = BlockStates.SNOW_LAYER.withTrait(BlockTraits.HEIGHT, 7);
        world.setState(BELOW.sub(0, 1, 0), BlockStates.STONE);
        world.setState(BELOW, fullStack);
        world.setState(POSITION, BlockStates.SNOW_LAYER);

        Block layer = world.block(POSITION);
        layer.requireComponent(BlockComponents.ON_TICK).execute(layer, null);

        assertSame(fullStack, world.state(BELOW));
        assertSame(BlockStates.SNOW_LAYER, world.state(POSITION));
    }

    @Test
    void aLayerIsFreeToFallWhenItsSupportIsRemoved() {
        BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());
        world.setState(POSITION, BlockStates.SNOW_LAYER);
        world.setState(BELOW, BlockStates.STONE);

        Block layer = world.block(POSITION);

        assertFalse(layer.requireComponent(BlockComponents.IS_FREE_TO_FALL).execute(layer));

        world.setState(BELOW, BlockStates.AIR);

        assertTrue(layer.requireComponent(BlockComponents.IS_FREE_TO_FALL).execute(layer));
    }

    @Test
    void fallingBlocksCanReplaceASingleLayerButNotAStack() {
        BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());
        world.setState(BELOW, BlockStates.SNOW_LAYER);

        assertTrue(FallingBlockHandlers.canReplace(world.block(BELOW)));

        for (int height = 1; height <= 7; height++) {
            world.setState(BELOW, BlockStates.SNOW_LAYER.withTrait(BlockTraits.HEIGHT, height));

            assertFalse(FallingBlockHandlers.canReplace(world.block(BELOW)), "height=" + height);
        }
    }

    @Test
    void aFallingLayerAddsToAnExistingStack() {
        BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());
        world.setState(BELOW, BlockStates.STONE);

        for (int height = 0; height < 7; height++) {
            world.setState(POSITION, BlockStates.SNOW_LAYER.withTrait(BlockTraits.HEIGHT, height));
            Block target = world.block(POSITION);

            List<BlockChange> changes = target.requireComponent(BlockComponents.RESOLVE_FALLING_PLACEMENT)
                    .execute(BlockStates.SNOW_LAYER, target);

            assertEquals(List.of(new BlockChange(target, BlockStates.SNOW_LAYER.withTrait(BlockTraits.HEIGHT, height + 1))),
                    changes);
        }
    }

    @Test
    void fallingLayersFillTheStackAndPlaceTheRemainderAbove() {
        BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());
        world.setState(BELOW, BlockStates.STONE);
        world.setState(POSITION, BlockStates.SNOW_LAYER.withTrait(BlockTraits.HEIGHT, 5));
        Block target = world.block(POSITION);
        BlockState falling = BlockStates.SNOW_LAYER.withTrait(BlockTraits.HEIGHT, 2);

        List<BlockChange> changes = target.requireComponent(BlockComponents.RESOLVE_FALLING_PLACEMENT)
                .execute(falling, target);

        assertEquals(2, changes.size());
        assertEquals(POSITION, changes.getFirst().block().getPosition());
        assertSame(BlockStates.SNOW_LAYER.withTrait(BlockTraits.HEIGHT, 7), changes.getFirst().newState());
        assertEquals(POSITION.add(0, 1, 0), changes.getLast().block().getPosition());
        assertSame(BlockStates.SNOW_LAYER, changes.getLast().newState());
    }

    @Test
    void aFallingLayerSettlesAboveAFullStack() {
        BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());
        world.setState(BELOW, BlockStates.SNOW_LAYER.withTrait(BlockTraits.HEIGHT, 7));
        Block target = world.block(POSITION);

        List<BlockChange> changes = BlockTestRegistry.get()
                .requireComponent(BlockStates.SNOW_LAYER.getType(), BlockComponents.RESOLVE_FALLING_PLACEMENT)
                .execute(BlockStates.SNOW_LAYER, target);

        assertEquals(List.of(new BlockChange(target, BlockStates.SNOW_LAYER)), changes);
    }

    @Test
    void aFallingStackDoesNotOverwriteAnObstructionAboveTheLanding() {
        BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());
        world.setState(BELOW, BlockStates.STONE);
        world.setState(POSITION, BlockStates.SNOW_LAYER.withTrait(BlockTraits.HEIGHT, 6));
        world.setState(POSITION.add(0, 1, 0), BlockStates.STONE);
        Block target = world.block(POSITION);
        BlockState falling = BlockStates.SNOW_LAYER.withTrait(BlockTraits.HEIGHT, 1);

        List<BlockChange> changes = target.requireComponent(BlockComponents.RESOLVE_FALLING_PLACEMENT)
                .execute(falling, target);

        assertTrue(changes.isEmpty());
    }

    @Test
    void ordinaryMiningKeepsTheBedrockDropCounts() {
        BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());
        BlockLootContext context = new BlockLootContext(ItemStack.from(ItemTypes.DIAMOND_SHOVEL), null, new Random(0));
        List<Integer> counts = List.of(1, 1, 1, 2, 2, 3, 3, 4);

        for (int height = 0; height < counts.size(); height++) {
            world.setState(POSITION, BlockStates.SNOW_LAYER.withTrait(BlockTraits.HEIGHT, height));
            Block block = world.block(POSITION);

            assertEquals(List.of(ItemStack.from(ItemTypes.SNOWBALL, counts.get(height))),
                    block.requireComponent(BlockComponents.GET_LOOT).execute(block, context));
        }
    }

    @Test
    void silkTouchReturnsTheLayersOrAFullBlock() {
        BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());
        ItemStack tool = ItemStack.builder(ItemTypes.DIAMOND_SHOVEL)
                .setData(ItemDataComponents.ENCHANTMENTS,
                        Map.of(EnchantmentTypes.SILK_TOUCH, new Enchantment(EnchantmentTypes.SILK_TOUCH, 1)))
                .build();
        BlockLootContext context = new BlockLootContext(tool, null, new Random(0));
        world.setState(POSITION, BlockStates.SNOW_LAYER.withTrait(BlockTraits.HEIGHT, 2));
        Block layers = world.block(POSITION);

        assertEquals(List.of(ItemStack.from(ItemTypes.SNOW_LAYER, 3)),
                layers.requireComponent(BlockComponents.GET_LOOT).execute(layers, context));

        world.setState(POSITION, BlockStates.SNOW_LAYER.withTrait(BlockTraits.HEIGHT, 7));
        Block fullStack = world.block(POSITION);

        assertEquals(List.of(ItemStack.from(ItemTypes.SNOW)),
                fullStack.requireComponent(BlockComponents.GET_LOOT).execute(fullStack, context));
    }

    @Test
    void aFailedLandingDropsOneSnowballRegardlessOfThickness() {
        BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());
        for (int height = 0; height <= 7; height++) {
            world.setState(POSITION, BlockStates.SNOW_LAYER.withTrait(BlockTraits.HEIGHT, height));
            Block block = world.block(POSITION);

            assertEquals(ItemStack.from(ItemTypes.SNOWBALL),
                    block.requireComponent(BlockComponents.GET_FALLING_BREAK_DROP).execute(block.getState()));
        }
    }

    @Test
    void blockLightMeltsOneLayerAtATimeEvenInASnowyBiome() {
        BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());
        world.setBiomeId(CloudBiomeRegistry.get().getRuntimeId(BiomeTypes.ICE_PLAINS));
        BlockState twoLayers = BlockStates.SNOW_LAYER.withTrait(BlockTraits.HEIGHT, 1);
        world.setState(POSITION, twoLayers);
        world.setBlockLight(11);

        tickSnow(world);

        assertSame(twoLayers, world.state(POSITION));

        world.setBlockLight(12);
        tickSnow(world);

        assertSame(BlockStates.SNOW_LAYER, world.state(POSITION));

        tickSnow(world);

        assertSame(BlockStates.AIR, world.state(POSITION));
    }

    @Test
    void aSnowlessBiomeCanMeltSnowInTheDarkButTheFadeCanBeCancelled() {
        BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());
        world.setBiomeId(CloudBiomeRegistry.get().getRuntimeId(BiomeTypes.PLAINS));
        BlockState twoLayers = BlockStates.SNOW_LAYER.withTrait(BlockTraits.HEIGHT, 1);
        world.setState(POSITION, twoLayers);
        world.onEvent(BlockFadeEvent.class, event -> event.setCancelled(true));

        tickSnow(world);

        assertSame(twoLayers, world.state(POSITION));
        assertSame(BlockStates.SNOW_LAYER, world.getEvents(BlockFadeEvent.class).getFirst().getNewState());
    }

    private static void tickSnow(BlockTestWorld world) {
        Block block = world.block(POSITION);
        block.requireComponent(BlockComponents.ON_RANDOM_TICK).execute(block, null);
    }
}
