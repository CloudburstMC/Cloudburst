package org.cloudburstmc.server.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import org.cloudburstmc.server.Bootstrap;
import org.cloudburstmc.server.level.Difficulty;

import java.nio.file.Path;

@Data
@Setter(AccessLevel.PRIVATE)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServerProperties {

    @SneakyThrows
    public static ServerProperties fromFile(Path path) {
        final ServerProperties properties = Bootstrap.JAVA_PROPS_MAPPER.readerForUpdating(new ServerProperties()).readValue(path.toFile());
        properties.path = path;
        return properties;
    }

    @JsonIgnore
    @Builder.Default
    private Path path = null;

    @Builder.Default
    private String motd = "A Cookie Powered Server";

    @Builder.Default
    private String subMotd = "https://github/Cookie-Studio/Cookie";

    @Builder.Default
    private String serverIp = "0.0.0.0";

    @Builder.Default
    private int serverPort = 19132;

    @Builder.Default
    private int viewDistance = 10;

    @Builder.Default
    private boolean whiteList = false;

    @Builder.Default
    private boolean achievements = true;

    @Builder.Default
    private boolean announcePlayerAchievements = true;

    @Builder.Default
    private int spawnProtection = 16;

    @Builder.Default
    private int maxPlayers = 20;

    @Builder.Default
    private boolean allowFlight = false;

    @Builder.Default
    private boolean spawnAnimals = true;

    @Builder.Default
    private boolean spawnMobs = true;

    @Builder.Default
    private int gamemode = 0;

    @Builder.Default
    private boolean forceGamemode = false;

    @Builder.Default
    private boolean hardcore = false;

    @Builder.Default
    private boolean pvp = true;

    @Builder.Default
    private int difficulty = 1;

    @Builder.Default
    private String defaultLevel = "world";

    @Builder.Default
    private boolean allowNether = true;

    @Builder.Default
    private boolean enableQuery = true;

    @Builder.Default
    private boolean autoSave = true;

    @Builder.Default
    private boolean forceResources = false;

    @Builder.Default
    private boolean xboxAuth = true;

    @Builder.Default
    private boolean generateStructures = true;


    public Path getPath() {
        return path;
    }

    public void setPath(Path path) { this.path = path; }


    public void modifyWhitelist(boolean b) {
        this.whiteList = b;
        this.save();
    }

    public void modifyGamemode(int id) {
        this.gamemode = id;
        this.save();
    }

    public void modifyDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty.ordinal();
        this.save();
    }

    public void modifyDefaultLevel(String name) {
        this.defaultLevel = name;
        this.save();
    }

    @SneakyThrows
    public void load() {
        Bootstrap.JAVA_PROPS_MAPPER.readerForUpdating(this).readValue(path.toFile());
    }

    @SneakyThrows
    public void save() {
        Bootstrap.JAVA_PROPS_MAPPER.writeValue(path.toFile(), this);
    }

}
