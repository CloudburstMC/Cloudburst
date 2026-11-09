package org.cloudburstmc.server.level.provider.anvil;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.BitSet;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

/**
 * Reads Anvil regions without modifying their files.
 * Chunk coordinates are local to the region and range from 0 to 31.
 */
public class RegionFile implements Closeable {

    private static final int COMPRESSION_GZIP = 1;
    private static final int COMPRESSION_ZLIB = 2;
    private static final int SECTOR_BYTES = 4096;
    private static final int ENTRY_COUNT = 1024;
    private static final int HEADER_BYTES = SECTOR_BYTES * 2;
    private static final int MAX_CHUNK_BYTES = 16 * 1024 * 1024;

    private final int[] offsets = new int[ENTRY_COUNT];
    private final FileChannel channel;

    /**
     * Opens a region for reading and validates its location table.
     * Chunk payloads are validated when read, not when the file is opened.
     *
     * @param path region file to open
     * @throws IOException if the file cannot be opened, has invalid sector boundaries
     *                     or assigns overlapping sectors to chunks
     */
    public RegionFile(Path path) throws IOException {
        channel = FileChannel.open(path, StandardOpenOption.READ);
        try {
            long size = channel.size();
            if (size < HEADER_BYTES || size % SECTOR_BYTES != 0) {
                throw new IOException("Invalid region file size: " + path);
            }

            ByteBuffer header = ByteBuffer.allocate(HEADER_BYTES);
            readFully(header, 0);
            header.flip();
            BitSet occupied = new BitSet();
            occupied.set(0, 2);

            for (int i = 0; i < ENTRY_COUNT; i++) {
                int offset = header.getInt();
                offsets[i] = offset;
                if (offset == 0) {
                    continue;
                }

                int sector = offset >>> 8;
                int count = offset & 0xff;
                int overlap = occupied.nextSetBit(sector);

                if (sector < 2 || count == 0 || (long) sector + count > size / SECTOR_BYTES || overlap >= 0 && overlap < sector + count) {
                    throw new IOException("Invalid or overlapping chunk sectors in " + path);
                }

                occupied.set(sector, sector + count);
            }
        } catch (IOException | RuntimeException failure) {
            try {
                channel.close();
            } catch (IOException closeFailure) {
                failure.addSuppressed(closeFailure);
            }

            throw failure;
        }
    }

    /**
     * Returns the decompressed chunk payload. The caller must release the returned buffer.
     * Supports GZIP and zlib compression and limits decompressed payloads to 16 MiB.
     *
     * @param x local chunk x from 0 to 31
     * @param z local chunk z from 0 to 31
     * @return a read-only buffer independent of the file's lifetime
     * @throws IllegalArgumentException if either coordinate is outside the region
     * @throws IOException if the chunk is absent, its payload cannot be read or decompressed,
     *                     or its compression or size is unsupported
     */
    public ByteBuf readChunk(int x, int z) throws IOException {
        int offset = offsets[index(x, z)];
        if (offset == 0) {
            throw new IOException("No stored chunk at " + x + "," + z);
        }

        long position = (long) (offset >>> 8) * SECTOR_BYTES;
        ByteBuffer header = ByteBuffer.allocate(5);
        readFully(header, position);
        header.flip();

        int length = header.getInt();
        int compression = Byte.toUnsignedInt(header.get());

        if (length <= 1 || length > (offset & 0xff) * SECTOR_BYTES - Integer.BYTES) {
            throw new IOException("Invalid stored chunk length: " + length);
        }

        ByteBuffer compressed = ByteBuffer.allocate(length - 1);
        readFully(compressed, position + 5);
        try (InputStream bytes = new ByteArrayInputStream(compressed.array());
             InputStream decoded = switch (compression) {
                 case COMPRESSION_GZIP -> new GZIPInputStream(bytes);
                 case COMPRESSION_ZLIB -> new InflaterInputStream(bytes);
                 default -> throw new IOException("Unsupported region compression: " + compression);
             }) {
            byte[] data = decoded.readNBytes(MAX_CHUNK_BYTES + 1);

            if (data.length > MAX_CHUNK_BYTES) {
                throw new IOException("Imported chunk exceeds " + MAX_CHUNK_BYTES + " bytes");
            }

            return Unpooled.wrappedBuffer(data).asReadOnly();
        }
    }

    /**
     * Tests for an entry in the location table without reading or validating its payload.
     *
     * @param x local chunk x from 0 to 31
     * @param z local chunk z from 0 to 31
     * @return whether the chunk has an allocated location
     * @throws IllegalArgumentException if either coordinate is outside the region
     */
    public boolean hasChunk(int x, int z) {
        return offsets[index(x, z)] != 0;
    }

    private static int index(int x, int z) {
        if (x < 0 || x >= 32 || z < 0 || z >= 32) {
            throw new IllegalArgumentException("Region chunk coordinates must be between 0 and 31");
        }

        return x + z * 32;
    }

    private void readFully(ByteBuffer buffer, long position) throws IOException {
        while (buffer.hasRemaining()) {
            int count = channel.read(buffer, position);
            if (count < 0) {
                throw new EOFException("Truncated region file");
            }

            position += count;
        }
    }

    /**
     * Closes the file channel. Previously returned buffers remain the caller's responsibility.
     */
    @Override
    public void close() throws IOException {
        channel.close();
    }
}
