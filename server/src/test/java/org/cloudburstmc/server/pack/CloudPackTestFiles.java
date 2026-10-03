package org.cloudburstmc.server.pack;

import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@UtilityClass
class CloudPackTestFiles {

    public static Path directory(Path path, UUID id, String dependencies) throws IOException {
        Files.createDirectories(path.resolve("textures/blocks"));
        Files.write(path.resolve("textures/blocks/example.bin"), new byte[]{1, 2, 3, 4, 5});
        Files.writeString(path.resolve("manifest.json"), """
                {
                  "format_version": 2,
                  "header": {
                    "name": "Example",
                    "description": "Example resources",
                    "uuid": "%s",
                    "version": [1, 0, 0]
                  },
                  "modules": [{
                    "type": "resources",
                    "uuid": "10000000-0000-0000-0000-000000000000",
                    "version": [1, 0, 0]
                  }],
                  "dependencies": [%s]
                }
                """.formatted(id, dependencies));
        return path;
    }

    public static Path archive(Path directory, Path archive) throws IOException {
        try (Stream<Path> paths = Files.walk(directory); ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(archive))) {
            for (Path entry : paths.filter(Files::isRegularFile).sorted().toList()) {
                output.putNextEntry(new ZipEntry(directory.relativize(entry).toString().replace('\\', '/')));
                Files.copy(entry, output);
                output.closeEntry();
            }
        }

        return archive;
    }

    public static String dependency(UUID id, int major) {
        return "{\"uuid\":\"" + id + "\",\"version\":[" + major + ",0,0]}";
    }
}
