package org.cloudburstmc.server.block;

import org.cloudburstmc.api.block.BlockComponents;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.block.component.BlockShapeContext;
import org.cloudburstmc.api.block.component.ShapeContextRequirement;
import org.cloudburstmc.api.util.BoundingBox;
import org.cloudburstmc.api.util.CollisionContext;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.cloudburstmc.server.testutil.BlockTestWorld;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BlockGeometryDataTest {

    private static final CloudBlockRegistry REGISTRY = BlockTestRegistry.get();
    private static final float TOLERANCE = 0.00002f;

    @Test
    void consumesVerifiedBaseGeometryWithoutRecenteringIt() {
        BlockGeometryData data = BlockGeometryData.read(geometry(), "test:block");
        assertEquals(0.3125f, data.collisionShape().bounds().getMinX(), TOLERANCE);
        BlockModelOffset offset = data.collisionOffset();
        assertNotNull(offset);

        BoundingBox positioned = offset.apply(data.collisionShape(), Vector3i.from(1, 64, 0)).bounds();
        assertEquals(0.4375f, positioned.getMinX(), TOLERANCE);
        assertEquals(0.30416667f, positioned.getMinZ(), TOLERANCE);
        assertEquals(0, positioned.getMinY(), TOLERANCE);
    }

    @Test
    void rejectsLegacyAndIncompleteGeometry() {
        assertThrows(IllegalArgumentException.class, () -> BlockGeometryData.read(Map.of("collisionShape", List.of()), "test:block"));

        for (String field : List.of("collision", "outline", "modelPosition")) {
            Map<String, Object> geometry = geometry();
            geometry.remove(field);
            assertThrows(IllegalArgumentException.class, () -> BlockGeometryData.read(geometry, "test:block"));
        }
    }

    @Test
    void rejectsFractionalAndOverflowingBlockCoordinatesBeforeFloatConversion() {
        for (Number coordinate : List.of(new BigDecimal("1E-50"), 0.5, 2147483648L)) {
            Map<String, Object> geometry = geometry(List.of(coordinate, 0, 0));

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> BlockGeometryData.read(geometry, "test:block"));
            assertTrue(exception.getMessage().contains("modelPosition.samples[0].position[0]"));
        }
    }

    @Test
    void readsSampledShapesWithoutRequiringModelSamples() {
        Map<String, Object> geometry = geometry();
        geometry.remove("modelPosition");
        geometry.put("collision", Map.of("space", "sampled", "boxes", List.of(baseBox())));
        geometry.put("outline", Map.of("space", "sampled", "box", baseBox()));

        BlockGeometryData data = BlockGeometryData.read(geometry, "test:block");
        assertNull(data.collisionOffset());
        assertNull(data.outlineOffset());
        assertEquals(0.3125f, data.collisionShape().bounds().getMinX(), TOLERANCE);
    }

    @Test
    void rejectsUnsupportedSpacesAndIncorrectNativeObservations() {
        Map<String, Object> unknownSpace = geometry();
        unknownSpace.put("collision", Map.of("space", "unknown", "boxes", List.of(baseBox())));
        assertThrows(IllegalArgumentException.class, () -> BlockGeometryData.read(unknownSpace, "test:block"));

        Map<String, Object> corrupted = geometry();
        corrupted.put("outline", Map.of("space", "model_base", "translation", "native_result", "box", List.of(0, 0, 0, 1, 1, 1)));
        assertThrows(IllegalArgumentException.class, () -> BlockGeometryData.read(corrupted, "test:block"));
    }

    @Test
    void allPositionedPaletteShapesHavePositionAwareHandlers() {
        for (CloudBlockDefinition definition : BlockPalette.INSTANCE.getRuntimeMap().values()) {
            BlockState state = definition.getCloudState();
            BlockType type = state.getType();
            BlockGeometryData geometry = BlockPalette.INSTANCE.getGeometry(state);
            assertNotNull(geometry, state.toString());

            if (geometry.collisionOffset() != null) {
                assertEquals(ShapeContextRequirement.POSITION, REGISTRY.requireComponent(type, BlockComponents.GET_COLLISION_SHAPE).contextRequirement(), state.toString());
            }

            if (geometry.outlineOffset() != null) {
                assertEquals(ShapeContextRequirement.POSITION, REGISTRY.requireComponent(type, BlockComponents.GET_OUTLINE_SHAPE).contextRequirement(), state.toString());
            }
        }
    }

    @Test
    void positionsBambooAndPlantOutlinesWithoutAddingPlantCollision() {
        Vector3i position = Vector3i.from(0, 64, 0);
        BlockTestWorld world = new BlockTestWorld(REGISTRY);
        BlockState bamboo = BlockTypes.BAMBOO.getDefaultState();
        world.setState(position, bamboo);

        BoundingBox base = bamboo.getCollisionShape().bounds();
        BoundingBox shifted = world.block(position).getCollisionShape(CollisionContext.empty()).bounds();

        assertEquals(base.getMinX() - 0.18333333f, shifted.getMinX(), TOLERANCE);
        assertEquals(base.getMinZ() - 0.11666667f, shifted.getMinZ(), TOLERANCE);

        BlockState grass = BlockTypes.SHORT_GRASS.getDefaultState();
        world.setState(position, grass);
        assertTrue(world.block(position).getCollisionShape(CollisionContext.empty()).isEmpty());

        BoundingBox grassBase = grass.getOutlineShape().bounds();
        BoundingBox grassOutline = world.block(position).getOutlineShape().bounds();
        assertEquals(grassBase.getMinX() - 0.18333333f, grassOutline.getMinX(), TOLERANCE);
        assertEquals(grassBase.getMinY() - 0.04f, grassOutline.getMinY(), TOLERANCE);
        assertEquals(grassBase.getMinZ() - 0.11666667f, grassOutline.getMinZ(), TOLERANCE);

        assertEquals(ShapeContextRequirement.STATE_ONLY, REGISTRY.requireComponent(BlockTypes.STONE, BlockComponents.GET_COLLISION_SHAPE).contextRequirement());
        assertSame(grass.getOutlineShape(), REGISTRY.requireComponent(BlockTypes.SHORT_GRASS, BlockComponents.GET_OUTLINE_SHAPE).execute(grass, BlockShapeContext.empty()));
    }

    private static Map<String, Object> geometry() {
        return geometry(List.of(0, 0, 0));
    }

    private static Map<String, Object> geometry(List<Number> firstPosition) {
        Map<String, Object> result = new HashMap<>();
        result.put("collision", Map.of("space", "model_base", "translation", "native_result", "boxes", List.of(baseBox())));
        result.put("outline", Map.of("space", "model_base", "translation", "native_result", "box", baseBox()));
        result.put("modelPosition", Map.of(
                "samples", List.of(
                        sample(firstPosition, List.of(-0.09166667, 0, -0.05833333), List.of(0.22083333, 0, 0.25416667, 0.59583333, 0.6875, 0.62916667)),
                        sample(List.of(1, 0, 0), List.of(1.125, 0, -0.00833333), List.of(1.4375, 0, 0.30416667, 1.8125, 0.6875, 0.67916667))
                )
        ));

        return result;
    }

    private static List<Number> baseBox() {
        return List.of(0.3125, 0, 0.3125, 0.6875, 0.6875, 0.6875);
    }

    private static Map<String, Object> sample(List<Number> position, List<Number> result, List<Number> box) {
        return Map.of("position", position, "nativeResult", result, "collisionBoxes", List.of(box), "outlineBox", box);
    }
}
