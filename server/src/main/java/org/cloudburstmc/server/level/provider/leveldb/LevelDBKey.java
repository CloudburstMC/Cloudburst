package org.cloudburstmc.server.level.provider.leveldb;

import io.netty.buffer.ByteBuf;

public enum LevelDBKey {
    DATA_3D('+'),
    VERSION(','),
    DATA_2D('-'),
    DATA_2D_LEGACY('.'),
    SUBCHUNK_PREFIX('/'),
    LEGACY_TERRAIN('0'),
    BLOCK_ENTITIES('1'),
    ENTITIES('2'),
    PENDING_TICKS('3'),
    BLOCK_EXTRA_DATA('4'),
    BIOME_STATE('5'),
    STATE_FINALIZATION('6'),
    CONVERTER_TAG('7'),
    BORDER_BLOCKS('8'),
    HARDCODED_SPAWNERS('9'),
    PENDING_RANDOM_TICKS(':'),
    XXHASH_CHECKSUMS(';'),
    GENERATION_SEED('<'),
    GENERATED_BEFORE_CNC_BLENDING('='),
    BLENDING_BIOME_HEIGHT('>'),
    META_DATA_HASH('?'),
    BLENDING_DATA('@'),
    ACTOR_DIGEST_VERSION('A'),
    VERSION_OLD('v'),
    AABB_VOLUMES('w');

    private final byte encoded;

    LevelDBKey(char encoded) {
        this.encoded = (byte) encoded;
    }

    /**
     * Allocates and returns a new key byte array.
     */
    public byte[] getKey(int chunkX, int chunkZ) {
        return new byte[]{
                (byte) chunkX,
                (byte) (chunkX >>> 8),
                (byte) (chunkX >>> 16),
                (byte) (chunkX >>> 24),
                (byte) chunkZ,
                (byte) (chunkZ >>> 8),
                (byte) (chunkZ >>> 16),
                (byte) (chunkZ >>> 24),
                this.encoded
        };
    }

    /**
     * Allocates and returns a new key byte array.
     */
    public byte[] getKey(int chunkX, int chunkZ, int y) {
        return new byte[]{
                (byte) chunkX,
                (byte) (chunkX >>> 8),
                (byte) (chunkX >>> 16),
                (byte) (chunkX >>> 24),
                (byte) chunkZ,
                (byte) (chunkZ >>> 8),
                (byte) (chunkZ >>> 16),
                (byte) (chunkZ >>> 24),
                this.encoded,
                (byte) y
        };
    }

    /**
     * Writes the 9-byte key directly into {@code buf} at the current writer index.
     */
    public void writeTo(ByteBuf buf, int chunkX, int chunkZ) {
        buf.writeByte((byte) chunkX);
        buf.writeByte((byte) (chunkX >>> 8));
        buf.writeByte((byte) (chunkX >>> 16));
        buf.writeByte((byte) (chunkX >>> 24));
        buf.writeByte((byte) chunkZ);
        buf.writeByte((byte) (chunkZ >>> 8));
        buf.writeByte((byte) (chunkZ >>> 16));
        buf.writeByte((byte) (chunkZ >>> 24));
        buf.writeByte(this.encoded);
    }

    /**
     * Writes the 10-byte key directly into {@code buf} at the current writer index.
     */
    public void writeTo(ByteBuf buf, int chunkX, int chunkZ, int y) {
        buf.writeByte((byte) chunkX);
        buf.writeByte((byte) (chunkX >>> 8));
        buf.writeByte((byte) (chunkX >>> 16));
        buf.writeByte((byte) (chunkX >>> 24));
        buf.writeByte((byte) chunkZ);
        buf.writeByte((byte) (chunkZ >>> 8));
        buf.writeByte((byte) (chunkZ >>> 16));
        buf.writeByte((byte) (chunkZ >>> 24));
        buf.writeByte(this.encoded);
        buf.writeByte((byte) y);
    }
}
