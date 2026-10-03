package org.cloudburstmc.server.pack;

import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

@UtilityClass
public class CloudPackLoader {

    /**
     * Prepares an owned archive before publishing its metadata or digest.
     * Later edits to the source cannot change an active transfer.
     */
    public static CloudResourcePack load(Path source) throws IOException {
        Path archive = Files.createTempFile("cloudburst-pack-", ".zip");
        boolean transferred = false;
        try {
            if (Files.isDirectory(source, LinkOption.NOFOLLOW_LINKS)) {
                archiveDirectory(source, archive);
            } else if (Files.isRegularFile(source, LinkOption.NOFOLLOW_LINKS)) {
                Files.copy(source, archive, StandardCopyOption.REPLACE_EXISTING);
            } else {
                throw new IOException("Pack source must be a regular file or directory");
            }

            CloudPackManifest manifest;
            try (ZipFile zip = new ZipFile(archive.toFile())) {
                ZipEntry entry = zip.getEntry("manifest.json");
                if (entry == null || entry.isDirectory()) {
                    throw new IOException("Pack archive has no manifest.json");
                }
                try (InputStream input = zip.getInputStream(entry)) {
                    manifest = CloudPackManifest.load(input);
                }
            }
            validate(manifest);

            MessageDigest digest;
            try {
                digest = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException failure) {
                throw new IllegalStateException("SHA-256 is unavailable", failure);
            }

            try (InputStream input = new DigestInputStream(Files.newInputStream(archive), digest)) {
                input.transferTo(OutputStream.nullOutputStream());
            }

            CloudResourcePack pack = new CloudResourcePack(manifest, archive, Files.size(archive), digest.digest());
            transferred = true;
            return pack;
        } finally {
            if (!transferred) {
                Files.deleteIfExists(archive);
            }
        }
    }

    private static void archiveDirectory(Path source, Path archive) throws IOException {
        try (Stream<Path> paths = Files.walk(source); ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(archive))) {
            for (Path entry : paths.sorted().toList()) {
                if (Files.isSymbolicLink(entry)) {
                    throw new IOException("Pack directories must not contain symbolic links");
                }

                if (Files.isDirectory(entry, LinkOption.NOFOLLOW_LINKS)) {
                    continue;
                }

                if (!Files.isRegularFile(entry, LinkOption.NOFOLLOW_LINKS)) {
                    throw new IOException("Pack contains a non-regular file");
                }

                String name = source.relativize(entry).toString().replace('\\', '/');
                ZipEntry zipEntry = new ZipEntry(name);
                zipEntry.setTime(0);
                output.putNextEntry(zipEntry);
                Files.copy(entry, output);
                output.closeEntry();
            }
        }
    }

    private static void validate(CloudPackManifest manifest) throws IOException {
        if (manifest == null || manifest.getFormatVersion() <= 0 || manifest.getHeader() == null
                || manifest.getHeader().getName() == null || manifest.getHeader().getName().isBlank()
                || manifest.getHeader().getDescription() == null || manifest.getHeader().getUuid() == null
                || manifest.getHeader().getVersion() == null || manifest.getModules() == null
                || manifest.getModules().isEmpty() || manifest.getDependencies() == null) {
            throw new IOException("Invalid pack manifest");
        }

        for (CloudPackManifest.Module module : manifest.getModules()) {
            if (module == null || !"resources".equals(module.getType()) || module.getUuid() == null || module.getVersion() == null) {
                throw new IOException("Only resource pack modules are supported");
            }
        }

        for (CloudPackManifest.Dependency dependency : manifest.getDependencies()) {
            if (dependency == null || dependency.getUuid() == null || dependency.getVersion() == null) {
                throw new IOException("Pack dependencies must declare a UUID and version");
            }
        }
    }
}
