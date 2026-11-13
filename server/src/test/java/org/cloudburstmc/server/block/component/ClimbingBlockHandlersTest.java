package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.block.component.BlockShapeContext;
import org.cloudburstmc.api.block.data.Scaffolding;
import org.cloudburstmc.api.event.block.BlockFadeEvent;
import org.cloudburstmc.api.event.block.BlockSpreadEvent;
import org.cloudburstmc.api.event.entity.EntityChangeBlockEvent;
import org.cloudburstmc.api.event.player.PlayerHarvestBlockEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.BoundingBox;
import org.cloudburstmc.api.util.CollisionContext;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.block.BlockPlacementTarget;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.cloudburstmc.server.testutil.BlockTestWorld;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class ClimbingBlockHandlersTest {

    private static final Vector3i POSITION = Vector3i.from(4, 64, 4);

    @Test
    void ladderFacesAwayFromItsSupportAndBreaksWhenThatSupportIsRemoved() {
        BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());
        Vector3i support = Direction.WEST.relative(POSITION);
        world.setState(support, BlockStates.STONE);
        BlockState ladder = LadderBlockHandlers.RESOLVE_PLACEMENT_STATE.execute(
                BlockStates.LADDER, world.block(POSITION), null, Direction.EAST, Vector3f.ZERO);
        assertNotNull(ladder);
        assertSame(Direction.EAST, ladder.ensureTrait(BlockTraits.FACING_DIRECTION));

        world.setState(POSITION, ladder);
        world.setState(support, BlockStates.AIR);
        Block block = world.block(POSITION);
        block.requireComponent(BlockComponents.ON_NEIGHBOUR_CHANGED).execute(block, world.block(support));

        assertSame(BlockStates.AIR, world.state(POSITION));
    }

    @Test
    void vinesAttachToLeafCollisionFacesWithoutMakingLeavesSturdy() {
        BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());
        for (Direction supportDirection : new Direction[]{Direction.NORTH, Direction.UP}) {
            Vector3i supportPosition = supportDirection.relative(POSITION);
            Direction clickedFace = supportDirection.getOpposite();
            world.setState(POSITION, BlockStates.AIR);
            world.setState(supportPosition, BlockStates.OAK_LEAVES);
            Block support = world.block(supportPosition);
            assertFalse(support.isFaceSturdy(clickedFace, SupportType.FULL));

            BlockPlacementContext context = new BlockPlacementContext(support, clickedFace, Vector3f.ZERO, null);
            BlockPlacementTarget target = BlockPlacementTarget.resolve(
                    BlockStates.VINE, world.block(POSITION), context);
            assertNotNull(target);
            assertEquals(POSITION, target.block().getPosition());

            BlockState placed = BlockTestRegistry.get()
                    .requireComponent(BlockTypes.VINE, BlockComponents.RESOLVE_PLACEMENT_STATE)
                    .execute(BlockStates.VINE, target.block(), null, target.placementFace(), Vector3f.ZERO);
            assertNotNull(placed);
            world.setState(POSITION, placed);
            Block vine = world.block(POSITION);
            assertTrue(vine.requireComponent(BlockComponents.CAN_SURVIVE).execute(vine));

            world.setState(supportPosition, BlockStates.AIR);
            vine.requireComponent(BlockComponents.ON_NEIGHBOUR_CHANGED).execute(vine, world.block(supportPosition));
            assertSame(BlockStates.AIR, world.state(POSITION));
        }
    }

    @Test
    void vinePlacementInheritsTheHorizontalFaceOfTheVineAbove() {
        BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());
        Vector3i above = Direction.UP.relative(POSITION);
        world.setState(above, BlockStates.VINE.withTrait(BlockTraits.VINE_DIRECTION_BITS, 4));
        world.setState(Direction.NORTH.relative(above), BlockStates.STONE);

        BlockState placed = VineBlockHandlers.RESOLVE_PLACEMENT_STATE.execute(
                BlockStates.VINE, world.block(POSITION), null, Direction.SOUTH, Vector3f.ZERO);
        assertNotNull(placed);
        assertEquals(4, placed.ensureTrait(BlockTraits.VINE_DIRECTION_BITS).intValue());

        world.setState(POSITION, placed);
        Block vine = world.block(POSITION);
        assertTrue(vine.requireComponent(BlockComponents.CAN_SURVIVE).execute(vine));
    }

    @Test
    void removingOneVineSupportKeepsTheOtherAttachment() {
        BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());
        world.setState(POSITION, BlockStates.VINE.withTrait(BlockTraits.VINE_DIRECTION_BITS, 12));
        world.setState(Direction.NORTH.relative(POSITION), BlockStates.STONE);
        Block vine = world.block(POSITION);
        vine.requireComponent(BlockComponents.ON_NEIGHBOUR_CHANGED)
                .execute(vine, world.block(Direction.EAST.relative(POSITION)));

        assertEquals(4, world.state(POSITION).ensureTrait(BlockTraits.VINE_DIRECTION_BITS).intValue());
        assertEquals(0, world.getBreakCount());
    }

    @Test
    void cancellingVineSpreadLeavesTheDestinationEmpty() {
        BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());
        world.setState(POSITION, BlockStates.VINE.withTrait(BlockTraits.VINE_DIRECTION_BITS, 4));
        Vector3i destination = Direction.DOWN.relative(POSITION);
        world.onEvent(BlockSpreadEvent.class, event -> {
            assertEquals(POSITION, event.getSource().getPosition());
            assertEquals(destination, event.getBlock().getPosition());
            event.setCancelled(true);
        });

        Block vine = world.block(POSITION);
        vine.requireComponent(BlockComponents.ON_RANDOM_TICK).execute(vine, new Random() {
            @Override
            public int nextInt(int bound) {
                return switch (bound) {
                    case 4 -> 0;
                    case 6 -> Direction.DOWN.ordinal();
                    case 16 -> 4;
                    default -> throw new AssertionError("Unexpected random bound " + bound);
                };
            }
        });

        assertEquals(1, world.getEvents(BlockSpreadEvent.class).size());
        assertSame(BlockStates.AIR, world.state(destination));
    }

    @Test
    void floatingScaffoldingHasATopPlatformButNoBottomCollision() {
        BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());
        BlockShapeContext blockContext = BlockShapeContext.at(world.block(POSITION).getLevel(), POSITION);
        BlockState scaffold = BlockStates.SCAFFOLDING.withTrait(BlockTraits.STABILITY, 1);
        Player inside = playerAtHeight(64.05f);
        Player onTop = playerAtHeight(65);

        assertTrue(ScaffoldingBlockHandlers.GET_COLLISION_SHAPE
                .execute(scaffold, blockContext, CollisionContext.of(inside)).isEmpty());
        assertFalse(ScaffoldingBlockHandlers.GET_COLLISION_SHAPE
                .execute(scaffold, blockContext, CollisionContext.of(onTop)).isEmpty());
        assertTrue(ScaffoldingBlockHandlers.GET_COLLISION_SHAPE
                .execute(scaffold, blockContext, CollisionContext.forPlacement(onTop)).isEmpty());
    }

    @Test
    void scaffoldingExtendsOnTheClickedTopOrSide() {
        BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());
        world.setState(POSITION, BlockStates.SCAFFOLDING);

        assertScaffoldTarget(world, Direction.UP, Direction.UP.relative(POSITION));
        assertScaffoldTarget(world, Direction.EAST, Direction.EAST.relative(POSITION));
    }

    @Test
    void scaffoldBottomClicksAndOccupiedDestinationsExtendUpward() {
        BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());
        world.setState(POSITION, BlockStates.SCAFFOLDING);
        Vector3i above = Direction.UP.relative(POSITION);
        Vector3i east = Direction.EAST.relative(POSITION);
        world.setState(above, BlockStates.SCAFFOLDING);
        world.setState(east, BlockStates.SCAFFOLDING);

        assertScaffoldTarget(world, Direction.DOWN, Direction.UP.relative(above));
        assertScaffoldTarget(world, Direction.EAST, Direction.UP.relative(east));
    }

    @Test
    void scaffoldCollapseCanBeCancelled() {
        BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());
        Vector3i second = Direction.EAST.relative(POSITION);
        Scaffolding data = BlockStates.SCAFFOLDING.getData(Scaffolding.class).orElseThrow();
        BlockState extension = data.withDistance(1);
        world.setState(second, extension);

        world.onEvent(BlockFadeEvent.class, event -> event.setCancelled(true));
        Block scaffold = world.block(second);
        scaffold.requireComponent(BlockComponents.ON_TICK).execute(scaffold, new Random(0));

        assertEquals(1, world.getEvents(BlockFadeEvent.class).size());
        assertSame(extension, world.state(second));
    }

    @Test
    void cancellingCaveVineHarvestKeepsTheBerriesAndDropsNothing() {
        BlockTestWorld world = new BlockTestWorld(BlockTestRegistry.get());
        BlockState berries = BlockTypes.CAVE_VINES_HEAD_WITH_BERRIES.getDefaultState();
        world.setState(POSITION, berries);
        world.onEvent(PlayerHarvestBlockEvent.class, event -> {
            assertEquals(1, event.getItemsHarvested().size());
            assertSame(ItemTypes.GLOW_BERRIES, event.getItemsHarvested().getFirst().getType());
            event.setCancelled(true);
        });

        Block vine = world.block(POSITION);
        Player player = InterfaceProxy.create(Player.class, Map.of());
        assertTrue(vine.requireComponent(BlockComponents.CAN_BE_USED).execute(vine, player));
        assertTrue(vine.requireComponent(BlockComponents.USE).execute(vine, player, Direction.NORTH, ItemStack.EMPTY));

        assertEquals(1, world.getEvents(EntityChangeBlockEvent.class).size());
        assertEquals(1, world.getEvents(PlayerHarvestBlockEvent.class).size());
        assertSame(berries, world.state(POSITION));
        assertTrue(world.getDrops().isEmpty());
    }

    private static Player playerAtHeight(float y) {
        return InterfaceProxy.create(Player.class, Map.of(
                "getMotion", Vector3f.ZERO,
                "getBoundingBox", new BoundingBox(4.2f, y, 4.2f, 4.8f, y + 1.8f, 4.8f)));
    }

    private static void assertScaffoldTarget(BlockTestWorld world, Direction face, Vector3i expected) {
        Block clicked = world.block(POSITION);
        Player player = InterfaceProxy.create(Player.class, Map.of("getHorizontalDirection", Direction.WEST));
        BlockPlacementContext context = new BlockPlacementContext(clicked, face, Vector3f.ZERO, player);
        BlockPlacementTarget target = BlockPlacementTarget.resolve(BlockStates.SCAFFOLDING,
                world.block(face.relative(POSITION)), context);
        assertNotNull(target);
        assertEquals(expected, target.block().getPosition());
    }
}
