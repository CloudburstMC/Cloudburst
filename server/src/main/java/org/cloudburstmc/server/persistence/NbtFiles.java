package org.cloudburstmc.server.persistence;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.nbt.NBTInputStream;
import org.cloudburstmc.nbt.NBTOutputStream;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class NbtFiles {

    public static NbtMap readCompressed(Path path) throws IOException {
        try (InputStream file = Files.newInputStream(path);
             NBTInputStream input = NbtUtils.createGZIPReader(file)) {
            if (!(input.readTag() instanceof NbtMap data)) {
                throw new IOException("Stored NBT must contain a compound: " + path);
            }
            return data;
        }
    }

    public static byte[] encodeCompressed(NbtMap data) throws IOException {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            try (NBTOutputStream output = NbtUtils.createGZIPWriter(bytes)) {
                output.writeTag(data);
            }
            return bytes.toByteArray();
        }
    }
}
