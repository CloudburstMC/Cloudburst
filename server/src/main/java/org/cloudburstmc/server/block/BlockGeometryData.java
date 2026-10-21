package org.cloudburstmc.server.block;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.util.VoxelShape;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.level.collision.CloudVoxelShapes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record BlockGeometryData(VoxelShape collisionShape,
        VoxelShape outlineShape,
        @Nullable BlockModelOffset collisionOffset,
        @Nullable BlockModelOffset outlineOffset
) {

    private static final float TOLERANCE = 0.00002f;

    public static BlockGeometryData read(Object value, String name) {
        Map<?, ?> geometry = object(value, name + ".geometry");
        ShapeData collision = readShape(geometry.get("collision"), true, name + ".collision");
        ShapeData outline = readShape(geometry.get("outline"), false, name + ".outline");
        if (!collision.positioned() && !outline.positioned()) {
            return new BlockGeometryData(collision.shape(), outline.shape(), null, null);
        }

        List<BlockModelOffset.Sample> samples = readSamples(geometry.get("modelPosition"), collision, outline, name);
        BlockModelOffset offset = BlockModelOffset.fromSamples(samples, name);

        return new BlockGeometryData(
                collision.shape(),
                outline.shape(),
                collision.positioned() ? offset : null,
                outline.positioned() ? offset : null
        );
    }

    private static ShapeData readShape(Object value, boolean collision, String path) {
        Map<?, ?> shape = object(value, path);
        VoxelShape geometry = collision ? boxes(shape.get("boxes"), path + ".boxes") : CloudVoxelShapes.fromBoxes(box(shape.get("box"), path + ".box"));
        return new ShapeData(geometry, positioned(shape, path));
    }

    private static List<BlockModelOffset.Sample> readSamples(Object value, ShapeData collision, ShapeData outline, String name) {
        String path = name + ".modelPosition";
        Map<?, ?> model = object(value, path);

        List<BlockModelOffset.Sample> samples = new ArrayList<>();
        List<?> observations = list(model.get("samples"), path + ".samples");
        for (int index = 0; index < observations.size(); index++) {
            String samplePath = path + ".samples[" + index + "]";
            Map<?, ?> sample = object(observations.get(index), samplePath);
            Vector3i position = blockPosition(sample.get("position"), samplePath + ".position");
            Vector3f nativeResult = vector(sample.get("nativeResult"), samplePath + ".nativeResult");
            samples.add(new BlockModelOffset.Sample(position, nativeResult));

            if (collision.positioned()) {
                VoxelShape observed = boxes(sample.get("collisionBoxes"), samplePath + ".collisionBoxes");
                verify(collision.shape(), observed, nativeResult, samplePath);
            }

            if (outline.positioned()) {
                float[] observedBox = box(sample.get("outlineBox"), samplePath + ".outlineBox");
                VoxelShape observed = CloudVoxelShapes.fromBoxes(observedBox);
                verify(outline.shape(), observed, nativeResult, samplePath);
            }
        }

        return samples;
    }

    private static boolean positioned(Map<?, ?> shape, String name) {
        if ("sampled".equals(shape.get("space"))) {
            return false;
        }

        if (!"model_base".equals(shape.get("space")) || !"native_result".equals(shape.get("translation"))) {
            throw new IllegalArgumentException("Unsupported geometry coordinate space for " + name);
        }

        return true;
    }

    private static void verify(VoxelShape base, VoxelShape observed, Vector3f translation, String name) {
        List<float[]> expected = new ArrayList<>();
        List<float[]> actual = new ArrayList<>();
        base.forEachBox((x0, y0, z0, x1, y1, z1) -> expected.add(new float[]{
                x0 + translation.getX(),
                y0 + translation.getY(),
                z0 + translation.getZ(),
                x1 + translation.getX(),
                y1 + translation.getY(),
                z1 + translation.getZ()
        }));
        observed.forEachBox((x0, y0, z0, x1, y1, z1) -> actual.add(new float[]{x0, y0, z0, x1, y1, z1}));

        if (expected.size() != actual.size()) {
            throw new IllegalArgumentException("Geometry sample box count mismatch for " + name);
        }

        for (int index = 0; index < expected.size(); index++) {
            float[] expectedBox = expected.get(index);
            float[] actualBox = actual.get(index);

            for (int coordinate = 0; coordinate < 6; coordinate++) {
                if (Math.abs(expectedBox[coordinate] - actualBox[coordinate]) > TOLERANCE) {
                    throw new IllegalArgumentException("Geometry sample does not match its base shape for " + name);
                }
            }
        }
    }

    private static VoxelShape boxes(Object value, String name) {
        List<?> boxes = list(value, name);
        float[] coordinates = new float[boxes.size() * 6];

        for (int index = 0; index < boxes.size(); index++) {
            float[] box = box(boxes.get(index), name + "[" + index + "]");
            System.arraycopy(box, 0, coordinates, index * 6, 6);
        }

        return CloudVoxelShapes.fromBoxes(coordinates);
    }

    private static float[] box(Object value, String name) {
        List<?> coordinates = list(value, name);
        if (coordinates.size() != 6) {
            throw new IllegalArgumentException("Expected six box coordinates for " + name);
        }

        float[] box = new float[6];

        for (int index = 0; index < box.length; index++) {
            box[index] = number(coordinates.get(index), name + "[" + index + "]").floatValue();
        }

        return box;
    }

    private static Vector3f vector(Object value, String name) {
        List<?> coordinates = list(value, name);
        if (coordinates.size() != 3) {
            throw new IllegalArgumentException("Expected three vector coordinates for " + name);
        }

        return Vector3f.from(
                number(coordinates.get(0), name + "[0]").floatValue(),
                number(coordinates.get(1), name + "[1]").floatValue(),
                number(coordinates.get(2), name + "[2]").floatValue()
        );
    }

    private static Vector3i blockPosition(Object value, String name) {
        List<?> coordinates = list(value, name);
        if (coordinates.size() != 3) {
            throw new IllegalArgumentException("Expected three block coordinates for " + name);
        }

        int[] position = new int[3];
        for (int axis = 0; axis < position.length; axis++) {
            String path = name + "[" + axis + "]";
            Number coordinate = number(coordinates.get(axis), path);

            try {
                position[axis] = new BigDecimal(coordinate.toString()).intValueExact();
            } catch (ArithmeticException | NumberFormatException exception) {
                throw new IllegalArgumentException("Invalid block coordinate for " + path, exception);
            }
        }

        return Vector3i.from(position[0], position[1], position[2]);
    }

    private static Number number(Object value, String name) {
        if (!(value instanceof Number number) || !Float.isFinite(number.floatValue())) {
            throw new IllegalArgumentException("Invalid geometry number for " + name);
        }

        return number;
    }

    private static Map<?, ?> object(Object value, String name) {
        if (!(value instanceof Map<?, ?> object)) {
            throw new IllegalArgumentException("Missing or invalid geometry object: " + name);
        }

        return object;
    }

    private static List<?> list(Object value, String name) {
        if (!(value instanceof List<?> list)) {
            throw new IllegalArgumentException("Missing or invalid geometry array: " + name);
        }

        return list;
    }

    private record ShapeData(VoxelShape shape, boolean positioned) {
    }
}
