package org.cloudburstmc.server.block.util;

import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Log4j2
@UtilityClass
public class BlockStateHash {

    private static final int FNV1_32_INIT = 0x811c9dc5;
    private static final int FNV1_PRIME_32 = 0x01000193;

    /**
     * Returns the block state hash for the given block name and states compound.
     *
     * @param name   full block identifier
     * @param states the states compound from the palette entry
     */
    public static int compute(String name, NbtMap states) {
        if ("minecraft:unknown".equals(name)) {
            return -2;
        }

        NbtMap tag = NbtMap.builder()
                .putString("name", name)
                .putCompound("states", states)
                .build();
        return fnv1a32(tag);
    }

    /**
     * Hashes the NBT tag and returns the FNV-1a 32-bit result.
     */
    public static int fnv1a32(NbtMap tag) {
        byte[] bytes;
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream();
             var writer = NbtUtils.createWriterLE(stream)) {
            writer.writeTag(tag);
            bytes = stream.toByteArray();
        } catch (IOException e) {
            log.error("Failed to serialize NBT for block state hash", e);
            throw new RuntimeException(e);
        }
        return fnv1a32(bytes);
    }

    /**
     * FNV-1a 32-bit hash over raw bytes.
     */
    public static int fnv1a32(byte[] data) {
        int hash = FNV1_32_INIT;
        for (byte b : data) {
            hash ^= (b & 0xff);
            hash *= FNV1_PRIME_32;
        }
        return hash;
    }
}
