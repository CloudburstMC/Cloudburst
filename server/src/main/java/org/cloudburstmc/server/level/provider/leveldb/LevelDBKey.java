package org.cloudburstmc.server.level.provider.leveldb;

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

    public byte[] getKey(int chunkX, int chunkZ) {
        return new byte[]{
                (byte) (chunkX & 0xff),
                (byte) ((chunkX >>> 8) & 0xff),
                (byte) ((chunkX >>> 16) & 0xff),
                (byte) ((chunkX >>> 24) & 0xff),
                (byte) (chunkZ & 0xff),
                (byte) ((chunkZ >>> 8) & 0xff),
                (byte) ((chunkZ >>> 16) & 0xff),
                (byte) ((chunkZ >>> 24) & 0xff),
                this.encoded
        };
    }

    public byte[] getKey(int chunkX, int chunkZ, int y) {
        return new byte[]{
                (byte) (chunkX & 0xff),
                (byte) ((chunkX >>> 8) & 0xff),
                (byte) ((chunkX >>> 16) & 0xff),
                (byte) ((chunkX >>> 24) & 0xff),
                (byte) (chunkZ & 0xff),
                (byte) ((chunkZ >>> 8) & 0xff),
                (byte) ((chunkZ >>> 16) & 0xff),
                (byte) ((chunkZ >>> 24) & 0xff),
                this.encoded,
                (byte) y
        };
    }
}
