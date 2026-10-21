package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.block.component.BlockShapeContext;
import org.cloudburstmc.api.block.component.ShapeContextRequirement;
import org.cloudburstmc.api.util.BoundingBox;
import org.cloudburstmc.api.util.CollisionContext;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.VoxelShape;
import org.cloudburstmc.api.util.data.DripstoneThickness;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.cloudburstmc.server.testutil.BlockTestWorld;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PointedDripstoneShapeTest {

    private static final CloudBlockRegistry REGISTRY = BlockTestRegistry.get();
    private static final float TOLERANCE = 0.00002f;

    @Test
    void centeredQueriesKeepEveryThicknessAndTipOrientation() {
        for (DripstoneThickness thickness : DripstoneThickness.values()) {
            for (boolean hanging : List.of(false, true)) {
                BlockState state = state(thickness, hanging);
                VoxelShape shape = REGISTRY.requireComponent(BlockTypes.POINTED_DRIPSTONE, BlockComponents.GET_COLLISION_SHAPE)
                        .execute(state, BlockShapeContext.empty(), CollisionContext.empty());
                BoundingBox bounds = shape.bounds();
                float width = switch (thickness) {
                    case TIP, MERGE -> 6 / 16f;
                    case FRUSTUM -> 8 / 16f;
                    case MIDDLE -> 10 / 16f;
                    case BASE -> 12 / 16f;
                };

                assertEquals(0.5f - width / 2, bounds.getMinX(), TOLERANCE);
                assertEquals(0.5f + width / 2, bounds.getMaxX(), TOLERANCE);
                assertEquals(bounds.getMinX(), bounds.getMinZ(), TOLERANCE);
                assertEquals(bounds.getMaxX(), bounds.getMaxZ(), TOLERANCE);
                assertEquals(thickness == DripstoneThickness.TIP && hanging ? 5 / 16f : 0, bounds.getMinY(), TOLERANCE);
                assertEquals(thickness == DripstoneThickness.TIP && !hanging ? 11 / 16f : 1, bounds.getMaxY(), TOLERANCE);
            }
        }
    }

    @Test
    void positionsMatchBedrockModelOffsetsIncludingNegativeCoordinates() {
        assertOffset(Vector3i.from(0, 64, 0), -0.09166667f, -0.05833333f);
        assertOffset(Vector3i.from(1, 64, 0), 0.125f, -0.00833333f);
        assertOffset(Vector3i.from(-1, 64, 0), -0.04166667f, 0.125f);
    }

    @Test
    void verticalColumnsShareTheirOffsetAndAllShapeQueriesAgree() {
        BlockTestWorld world = new BlockTestWorld(REGISTRY);
        Vector3i bottom = Vector3i.from(-71, 64, 142);
        Vector3i top = bottom.add(0, 20, 0);
        BlockState state = state(DripstoneThickness.MIDDLE, false);
        world.setState(bottom, state);
        world.setState(top, state);

        Block block = world.block(bottom);
        VoxelShape collision = block.getCollisionShape(CollisionContext.empty());
        assertBoundsEqual(collision, world.block(top).getCollisionShape(CollisionContext.empty()));
        assertBoundsEqual(collision, block.getOutlineShape());
        assertBoundsEqual(collision, REGISTRY.requireComponent(BlockTypes.POINTED_DRIPSTONE, BlockComponents.GET_BLOCK_SUPPORT_SHAPE)
                .execute(state, BlockShapeContext.at(block.getLevel(), bottom)));

        assertEquals(ShapeContextRequirement.POSITION,
                REGISTRY.requireComponent(BlockTypes.POINTED_DRIPSTONE, BlockComponents.GET_COLLISION_SHAPE).contextRequirement());
        assertEquals(ShapeContextRequirement.POSITION,
                REGISTRY.requireComponent(BlockTypes.POINTED_DRIPSTONE, BlockComponents.GET_OUTLINE_SHAPE).contextRequirement());
        assertEquals(ShapeContextRequirement.POSITION,
                REGISTRY.requireComponent(BlockTypes.POINTED_DRIPSTONE, BlockComponents.GET_BLOCK_SUPPORT_SHAPE).contextRequirement());
    }

    @Test
    void walkingBesideTheActualModelDoesNotCollideWithTheDumpPositionsOffset() {
        BlockTestWorld world = new BlockTestWorld(REGISTRY);
        Vector3i position = Vector3i.from(1, 64, 0);
        world.setState(position, state(DripstoneThickness.TIP, false));
        VoxelShape collision = world.block(position).getCollisionShape(CollisionContext.empty());
        BoundingBox player = new BoundingBox(0.75f, 64, 0.35f, 1.35f, 65.8f, 0.65f);

        assertFalse(collision.overlaps(player, 1, 64, 0));
        assertEquals(0.05f, collision.collide(Direction.Axis.X, player, 0.05f, 1, 64, 0), TOLERANCE);
        assertEquals(0.0875f, collision.collide(Direction.Axis.X, player, 0.2f, 1, 64, 0), TOLERANCE);
    }

    private static void assertOffset(Vector3i position, float x, float z) {
        BlockTestWorld world = new BlockTestWorld(REGISTRY);
        world.setState(position, state(DripstoneThickness.TIP, false));
        BoundingBox bounds = world.block(position).getCollisionShape(CollisionContext.empty()).bounds();

        assertEquals(5 / 16f + x, bounds.getMinX(), TOLERANCE);
        assertEquals(11 / 16f + x, bounds.getMaxX(), TOLERANCE);
        assertEquals(5 / 16f + z, bounds.getMinZ(), TOLERANCE);
        assertEquals(11 / 16f + z, bounds.getMaxZ(), TOLERANCE);
    }

    private static void assertBoundsEqual(VoxelShape expected, VoxelShape actual) {
        BoundingBox left = expected.bounds();
        BoundingBox right = actual.bounds();
        assertEquals(left.getMinX(), right.getMinX(), TOLERANCE);
        assertEquals(left.getMinY(), right.getMinY(), TOLERANCE);
        assertEquals(left.getMinZ(), right.getMinZ(), TOLERANCE);
        assertEquals(left.getMaxX(), right.getMaxX(), TOLERANCE);
        assertEquals(left.getMaxY(), right.getMaxY(), TOLERANCE);
        assertEquals(left.getMaxZ(), right.getMaxZ(), TOLERANCE);
    }

    private static BlockState state(DripstoneThickness thickness, boolean hanging) {
        return BlockTypes.POINTED_DRIPSTONE.getDefaultState().withTrait(BlockTraits.DRIPSTONE_THICKNESS, thickness)
                .withTrait(BlockTraits.IS_HANGING, hanging);
    }
}
