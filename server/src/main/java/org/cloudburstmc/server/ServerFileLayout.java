package org.cloudburstmc.server;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Owns the locations of server settings, persistence, and diagnostics.
 */
public record ServerFileLayout(Path directory, Path levelsDirectory) {

    public ServerFileLayout {
        Objects.requireNonNull(directory, "directory");
        Objects.requireNonNull(levelsDirectory, "levelsDirectory");
    }

    public Path properties() {
        return directory.resolve("server.properties");
    }

    public Path global() {
        return directory.resolve("config/cloudburst-global.yml");
    }

    public Path levelDefaults() {
        return directory.resolve("config/cloudburst-level-defaults.yml");
    }

    public Path level(String name) {
        return levelDirectory(name).resolve("cloudburst-level.yml");
    }

    public Path playerData(String primaryLevel) {
        return levelDirectory(primaryLevel).resolve("playerdata");
    }

    public Path savedData(String primaryLevel) {
        return levelDirectory(primaryLevel).resolve("data");
    }

    public Path userCache() {
        return directory.resolve("usercache.json");
    }

    public Path operators() {
        return directory.resolve("ops.json");
    }

    public Path whitelist() {
        return directory.resolve("whitelist.json");
    }

    public Path playerBans() {
        return directory.resolve("banned-players.json");
    }

    public Path ipBans() {
        return directory.resolve("banned-ips.json");
    }

    public Path latestLog() {
        return directory.resolve("logs/latest.log");
    }

    public Path resourcePacks() {
        return directory.resolve("resource_packs");
    }

    public Path crashReports() {
        return directory.resolve("crash-reports");
    }

    private Path levelDirectory(String name) {
        Objects.requireNonNull(name, "name");
        Path id = Path.of(name);
        Path root = levelsDirectory.toAbsolutePath().normalize();
        Path folder = root.resolve(name).normalize();

        if (name.isBlank() || id.isAbsolute() || id.getNameCount() != 1 || !root.equals(folder.getParent())) {
            throw new IllegalArgumentException("Level ID must be a single directory name: " + name);
        }

        return folder;
    }

}
