package org.cloudburstmc.server.network;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.nbt.NBTInputStream;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtUtils;
import org.cloudburstmc.protocol.bedrock.data.BlockPropertyData;
import org.cloudburstmc.protocol.bedrock.data.SerializableVoxelShape;
import org.cloudburstmc.protocol.bedrock.packet.StartGamePacket;
import org.cloudburstmc.protocol.bedrock.packet.VoxelShapesPacket;
import org.cloudburstmc.server.registry.RegistryUtils;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Loads the vanilla block definitions and voxel shapes required during the login sequence.
 */
@UtilityClass
public class VanillaBlockNetworkData {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final List<BlockPropertyData> BLOCK_PROPERTIES = loadBlockProperties();
    private static final VoxelShapeData VOXEL_SHAPES = loadVoxelShapes();

    public static VoxelShapesPacket createVoxelShapesPacket() {
        VoxelShapesPacket packet = new VoxelShapesPacket();
        packet.setShapes(VOXEL_SHAPES.shapes());
        packet.setNameMap(VOXEL_SHAPES.names());
        packet.setCustomShapeCount(0);
        return packet;
    }

    public static void addBlockProperties(StartGamePacket packet) {
        packet.getBlockProperties().addAll(BLOCK_PROPERTIES);
    }

    private static List<BlockPropertyData> loadBlockProperties() {
        NbtMap properties;
        try (InputStream stream = RegistryUtils.getOrAssertResource("data/data_driven_blocks.nbt");
             NBTInputStream input = NbtUtils.createGZIPReader(stream)) {
            properties = (NbtMap) input.readTag();
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }

        List<BlockPropertyData> entries = new ArrayList<>(properties.size());
        properties.forEach((name, value) -> {
            if (name.isBlank()) {
                throw new IllegalArgumentException("Block property entry has no name");
            }

            if (!(value instanceof NbtMap data)) {
                throw new IllegalArgumentException("Block property entry " + name + " is not a compound");
            }

            entries.add(new BlockPropertyData(name, data));
        });

        return List.copyOf(entries);
    }

    private static VoxelShapeData loadVoxelShapes() {
        RawVoxelShapeData data;
        try (InputStream stream = RegistryUtils.getOrAssertResource("data/voxel_shapes.json")) {
            data = MAPPER.readValue(stream, RawVoxelShapeData.class);
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }

        if (data.names() == null || data.shapes() == null) {
            throw new IllegalArgumentException("Voxel shape data must contain names and shapes");
        }

        List<SerializableVoxelShape> shapes = new ArrayList<>(data.shapes().size());
        for (int index = 0; index < data.shapes().size(); index++) {
            shapes.add(toVoxelShape(data.shapes().get(index), index));
        }

        LinkedHashMap<String, Integer> names = new LinkedHashMap<>(data.names().size());
        data.names().forEach((name, shapeIndex) -> {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Voxel shape name must not be blank");
            }

            if (shapeIndex == null || shapeIndex < 0 || shapeIndex >= shapes.size()) {
                throw new IllegalArgumentException("Voxel shape " + name + " references invalid shape " + shapeIndex);
            }

            names.put(name, shapeIndex);
        });

        return new VoxelShapeData(Collections.unmodifiableMap(names), List.copyOf(shapes));
    }

    private static SerializableVoxelShape toVoxelShape(RawVoxelShape shape, int index) {
        if (shape == null || shape.cells() == null) {
            throw new IllegalArgumentException("Voxel shape " + index + " has no cells");
        }

        RawVoxelCells cells = shape.cells();
        validateCellSize(cells.xSize(), "x", index);
        validateCellSize(cells.ySize(), "y", index);
        validateCellSize(cells.zSize(), "z", index);
        validateCoordinates(shape.xCoordinates(), cells.xSize(), "x", index);
        validateCoordinates(shape.yCoordinates(), cells.ySize(), "y", index);
        validateCoordinates(shape.zCoordinates(), cells.zSize(), "z", index);

        return new SerializableVoxelShape(
                serializeCells(cells, index),
                List.copyOf(shape.xCoordinates()),
                List.copyOf(shape.yCoordinates()),
                List.copyOf(shape.zCoordinates())
        );
    }

    private static SerializableVoxelShape.SerializableCells serializeCells(RawVoxelCells cells, int shapeIndex) {
        if (cells.storage() == null) {
            throw new IllegalArgumentException("Voxel shape " + shapeIndex + " has no cell storage");
        }

        int expectedStorageSize = (cells.xSize() * cells.ySize() * cells.zSize() + 7) / 8;
        if (cells.storage().size() != expectedStorageSize) {
            throw new IllegalArgumentException("Voxel shape " + shapeIndex + " has invalid cell storage size");
        }

        List<Short> storage = new ArrayList<>(cells.storage().size());
        for (Integer value : cells.storage()) {
            if (value == null || value < 0 || value > 255) {
                throw new IllegalArgumentException("Voxel shape " + shapeIndex + " has an invalid cell value");
            }

            storage.add(value.shortValue());
        }

        return new SerializableVoxelShape.SerializableCells(
                (short) cells.xSize(),
                (short) cells.ySize(),
                (short) cells.zSize(),
                List.copyOf(storage)
        );
    }

    private static void validateCellSize(int size, String axis, int shapeIndex) {
        if (size < 0 || size > 255) {
            throw new IllegalArgumentException("Voxel shape " + shapeIndex + " has invalid " + axis + " cell size");
        }
    }

    private static void validateCoordinates(List<Float> coordinates, int cellSize, String axis, int shapeIndex) {
        if (coordinates == null || coordinates.size() != cellSize + 1) {
            throw new IllegalArgumentException("Voxel shape " + shapeIndex + " has invalid " + axis + " coordinates");
        }
    }

    private record RawVoxelShapeData(Map<String, Integer> names, List<RawVoxelShape> shapes) {
    }

    private record RawVoxelShape(RawVoxelCells cells, List<Float> xCoordinates, List<Float> yCoordinates, List<Float> zCoordinates) {
    }

    private record RawVoxelCells(int xSize, int ySize, int zSize, List<Integer> storage) {
    }

    private record VoxelShapeData(Map<String, Integer> names, List<SerializableVoxelShape> shapes) {
    }
}
