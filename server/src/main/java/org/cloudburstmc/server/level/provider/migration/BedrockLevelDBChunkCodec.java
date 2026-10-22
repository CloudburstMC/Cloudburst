package org.cloudburstmc.server.level.provider.migration;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.log4j.Log4j2;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.block.BlockStateCodec;
import org.cloudburstmc.server.level.chunk.BlockStorage;
import org.cloudburstmc.server.level.chunk.ChunkGenerationStatus;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Reads named Bedrock data without applying another format's numeric ID mappings.
 * Unregistered block names become {@code minecraft:unknown} with one warning per name.
 */
@Log4j2
public class BedrockLevelDBChunkCodec implements LevelDBChunkCodec {

    private final Set<Identifier> unknownBlocks = new HashSet<>();

    @Override
    public BlockStorage[] readSection(byte[] data, int sectionY) throws IOException {
        ByteBuf input = Unpooled.wrappedBuffer(data);
        try {
            int version = input.readUnsignedByte();
            int count;
            if (version == 1) {
                count = 1;
            } else if (version == 8 || version == 9) {
                count = input.readUnsignedByte();
                if (version == 9 && input.readByte() != sectionY) {
                    throw new IOException("Bedrock sub-chunk Y does not match its key");
                }
            } else {
                throw new IOException("Unsupported Bedrock sub-chunk encoding: " + version);
            }

            if (count > 2) {
                throw new IOException("Unsupported Bedrock block layer count: " + count);
            }

            BlockStorage[] layers = {new BlockStorage(), new BlockStorage()};
            for (int layer = 0; layer < count; layer++) {
                layers[layer] = BlockStorage.readFromStorage(input, this::readBlockState);
            }

            if (input.isReadable()) {
                throw new IOException("Unexpected trailing Bedrock sub-chunk data");
            }

            return layers;
        } catch (RuntimeException failure) {
            throw new IOException("Invalid Bedrock sub-chunk at Y " + sectionY, failure);
        } finally {
            input.release();
        }
    }

    @Override
    public BlockState readLegacyBlock(int id, int metadata) throws IOException {
        throw new IOException("Numeric Bedrock block references require a verified historical mapping: " + id + ":" + metadata);
    }

    @Override
    public BlockState readTickState(NbtMap tag) throws IOException {
        NbtMap state = tag.getCompound("blockState");
        if (!state.containsKey("name", NbtType.STRING)) {
            throw new IOException("Bedrock pending tick requires a named block state");
        }

        return this.readBlockState(state);
    }

    @Override
    public int readColumnBiome(byte[] biomes, int x, int z) {
        return biomes[z * 16 + x] & 0xff;
    }

    @Override
    public int readHeight(int encoded, int minHeight, boolean threeDimensional) {
        return threeDimensional ? encoded + minHeight : (short) encoded;
    }

    @Override
    public ChunkGenerationStatus readGeneration(byte @Nullable [] data) throws IOException {
        if (data == null) {
            return ChunkGenerationStatus.FINISHED;
        }

        if (data.length != Integer.BYTES) {
            throw new IOException("Invalid Bedrock finalization state");
        }

        int state = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).getInt();
        return switch (state) {
            case 0, 2, 3 -> ChunkGenerationStatus.FINISHED;
            case 1 -> ChunkGenerationStatus.GENERATED;
            default -> throw new IOException("Unknown Bedrock finalization state: " + state);
        };
    }

    @Override
    public NbtMap convertEntity(NbtMap tag) {
        if (tag.containsKey("DisplayTile") || tag.containsKey("TileID") || tag.containsKey("Tile")) {
            throw new IllegalArgumentException("Numeric Bedrock entity blocks require a verified historical mapping");
        }

        return convertNamedItems(tag);
    }

    @Override
    public NbtMap convertBlockEntity(NbtMap tag) {
        if (tag.containsKey("movingBlockId") || tag.getString("id").equals("FlowerPot") && tag.containsKey("item")) {
            throw new IllegalArgumentException("Numeric Bedrock block entity data requires a verified historical mapping");
        }

        return convertNamedItems(tag);
    }

    private BlockState readBlockState(NbtMap tag) {
        return BlockStateCodec.read(tag, id -> {
            if (this.unknownBlocks.add(id)) {
                log.warn("Replacing unregistered block '{}' with minecraft:unknown", id);
            }

            return BlockTypes.UNKNOWN.getDefaultState();
        });
    }

    private static NbtMap convertNamedItems(NbtMap tag) {
        NbtMapBuilder converted = tag.toBuilder();
        if (tag.containsKey("Count", NbtType.BYTE)) {
            boolean emptySlot = tag.getByte("Count") == 0 && tag.containsKey("Name", NbtType.STRING) && tag.getString("Name").isEmpty();
            if (emptySlot) {
                converted.putString("Name", "minecraft:air");
            } else if (!tag.containsKey("Name") && tag.containsKey("id", NbtType.SHORT)) {
                throw new IllegalArgumentException("Numeric Bedrock items require a verified historical mapping");
            } else if (tag.containsKey("Name") && (!tag.containsKey("Name", NbtType.STRING) || tag.getString("Name").isBlank())) {
                throw new IllegalArgumentException("Imported Bedrock item has an invalid name");
            }
        }

        for (Map.Entry<String, Object> entry : tag.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof NbtMap compound) {
                converted.putCompound(entry.getKey(), convertNamedItems(compound));
            } else if (value instanceof NbtList<?> list && list.getType() == NbtType.COMPOUND) {
                converted.putList(entry.getKey(), NbtType.COMPOUND, list.stream().map(element -> convertNamedItems((NbtMap) element)).toList());
            }
        }

        return converted.build();
    }
}
