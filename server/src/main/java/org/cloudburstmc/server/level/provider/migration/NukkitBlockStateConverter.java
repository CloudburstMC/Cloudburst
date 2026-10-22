package org.cloudburstmc.server.level.provider.migration;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.blockstateupdater.BlockStateUpdaters;
import org.cloudburstmc.nbt.*;
import org.cloudburstmc.server.Bootstrap;
import org.cloudburstmc.server.block.BlockStateCodec;
import org.cloudburstmc.server.registry.RegistryUtils;
import tools.jackson.core.type.TypeReference;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Converts Nukkit numeric block references and saved class names at the world import boundary.
 * Unknown references fail conversion rather than losing blocks or their metadata.
 */
public class NukkitBlockStateConverter {

    private final Map<Long, NbtMap> states;
    private final Map<String, Integer> saveIds;
    private final Map<Long, BlockState> resolved = new ConcurrentHashMap<>();

    public NukkitBlockStateConverter() throws IOException {
        try (InputStream input = RegistryUtils.getOrAssertResource("migration/nukkit/block_save_ids.json")) {
            this.saveIds = Map.copyOf(Bootstrap.JSON_MAPPER.readValue(input, new TypeReference<Map<String, Integer>>() {}));
        }

        try (NBTInputStream input = NbtUtils.createGZIPReader(RegistryUtils.getOrAssertResource("migration/nukkit/block_states.nbt"))) {
            Object root = input.readTag();
            if (!(root instanceof NbtList<?> palette) || palette.getType() != NbtType.COMPOUND) {
                throw new IOException("Invalid Nukkit migration palette");
            }

            Map<Long, NbtMap> entries = new HashMap<>();
            for (boolean overload : new boolean[]{false, true}) {
                for (Object entry : palette) {
                    NbtMap tag = (NbtMap) entry;
                    if (tag.getBoolean("stateOverload") != overload) {
                        continue;
                    }

                    long key = key(tag.getInt("id"), tag.getShort("data"));
                    NbtMapBuilder state = tag.toBuilder();
                    state.remove("id");
                    state.remove("data");
                    state.remove("runtimeId");
                    state.remove("stateOverload");
                    entries.put(key, state.build());
                }
            }

            this.states = Map.copyOf(entries);
        }
    }

    public int readSaveId(String name) {
        Integer id = this.saveIds.get(name);
        if (id == null) {
            throw new IllegalArgumentException("Unmapped Nukkit block save ID: " + name);
        }
        return id;
    }

    public BlockState read(int id, int metadata) {
        long key = key(id, metadata);
        return this.resolved.computeIfAbsent(key, ignored -> {
            NbtMap state = this.states.get(key);
            if (state == null) {
                throw new IllegalArgumentException("Unmapped Nukkit block " + id + ":" + metadata);
            }

            return BlockStateCodec.read(state);
        });
    }

    public BlockState readNamed(NbtMap tag) {
        NbtMap state = tag.containsKey("val") ? BlockStateUpdaters.updateBlockState(tag, tag.getInt("version")) : tag;
        return BlockStateCodec.read(state);
    }

    public NbtMap convertEntity(NbtMap tag) {
        NbtMapBuilder converted = tag.toBuilder();
        convertVector(tag, converted, "Pos");
        convertVector(tag, converted, "Motion");

        if (tag.containsKey("DisplayTile")) {
            int display = tag.getInt("DisplayTile");
            int metadata = tag.containsKey("DisplayData") ? tag.getInt("DisplayData") : display >>> 16;
            converted.putCompound("DisplayBlock", BlockStateCodec.write(read(display & 0xffff, metadata)));
            converted.remove("DisplayTile");
            converted.remove("DisplayData");
        }

        if (tag.containsKey("TileID") || tag.containsKey("Tile")) {
            int id = tag.containsKey("TileID") ? tag.getInt("TileID") : tag.getInt("Tile");
            converted.putCompound("BlockState", BlockStateCodec.write(read(id, tag.getByte("Data") & 0xff)));
            converted.remove("TileID");
            converted.remove("Tile");
            converted.remove("Data");
        }

        return converted.build();
    }

    public NbtMap convertBlockEntity(NbtMap tag) {
        NbtMapBuilder converted = tag.toBuilder();
        if (tag.getString("id").equals("FlowerPot") && tag.containsKey("item")) {
            int metadata = tag.containsKey("mData") ? tag.getInt("mData") : tag.getInt("data");
            converted.putCompound("PlantBlock", BlockStateCodec.write(read(tag.getShort("item") & 0xffff, metadata)));
            converted.remove("item");
            converted.remove("mData");
            converted.remove("data");
        }

        if (tag.containsKey("movingBlockId")) {
            converted.putCompound("movingBlock", BlockStateCodec.write(read(tag.getByte("movingBlockId") & 0xff, tag.getByte("movingBlockData") & 0xff)));
            converted.remove("movingBlockId");
            converted.remove("movingBlockData");
        }

        if (tag.containsKey("movingEntity", NbtType.COMPOUND)) {
            converted.putCompound("movingEntity", convertBlockEntity(tag.getCompound("movingEntity")));
        }

        return converted.build();
    }

    private static void convertVector(NbtMap source, NbtMapBuilder target, String name) {
        if (!source.containsKey(name)) {
            return;
        }

        if (!(source.get(name) instanceof List<?> values) || values.size() != 3) {
            throw new IllegalArgumentException("Invalid Nukkit entity vector: " + name);
        }

        List<Float> coordinates = new ArrayList<>(3);
        for (Object value : values) {
            if (!(value instanceof Number number) || !Float.isFinite(number.floatValue())) {
                throw new IllegalArgumentException("Invalid Nukkit entity coordinate: " + name);
            }

            coordinates.add(number.floatValue());
        }

        target.putList(name, NbtType.FLOAT, coordinates);
    }

    private static long key(int id, int metadata) {
        if (id < 0 || id > 0xffff || metadata < 0 || metadata >= 8192) {
            throw new IllegalArgumentException("Invalid Nukkit block reference " + id + ":" + metadata);
        }

        return (long) id << 32 | metadata;
    }
}
