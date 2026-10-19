package org.cloudburstmc.server.configuration;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.cloudburstmc.api.level.Difficulty;
import org.cloudburstmc.server.configuration.validation.SettingConstraint;

@Getter
@EqualsAndHashCode
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class DedicatedServerProperties {

    @Builder.Default
    private String motd = "A Cloudburst Powered Server";

    @Builder.Default
    private String subMotd = "https://cloudburstmc.org";

    @Builder.Default
    private String serverIp = "0.0.0.0";

    @Builder.Default
    @SettingConstraint(min = 1, max = 65535)
    private int serverPort = 19132;

    @Builder.Default
    @SettingConstraint(min = 1)
    private int viewDistance = 10;

    @Builder.Default
    private boolean whiteList = false;

    @Builder.Default
    private boolean achievements = true;

    @Builder.Default
    private boolean announcePlayerAchievements = true;

    @Builder.Default
    @SettingConstraint(min = 0)
    private int spawnProtection = 16;

    @Builder.Default
    @SettingConstraint(min = 1)
    private int maxPlayers = 20;

    @Builder.Default
    private boolean allowFlight = false;

    @Builder.Default
    private boolean spawnAnimals = true;

    @Builder.Default
    private boolean spawnMobs = true;

    @Builder.Default
    @SettingConstraint(min = 0, max = 3)
    private int gamemode = 0;

    @Builder.Default
    private boolean forceGamemode = false;

    @Builder.Default
    private boolean hardcore = false;

    @Builder.Default
    private boolean pvp = true;

    @Builder.Default
    @SettingConstraint(min = 0, max = 3)
    private int difficulty = 1;

    @Builder.Default
    @SettingConstraint(nonBlank = true)
    private String defaultLevel = "world";

    @Builder.Default
    private boolean allowNether = true;

    @Builder.Default
    private boolean allowEnd = true;

    @Builder.Default
    private boolean autoSave = true;

    @Builder.Default
    private boolean forceResources = false;

    @Builder.Default
    private boolean xboxAuth = true;

    @Builder.Default
    private boolean generateStructures = true;

    public void modifyMotd(String motd) {
        this.motd = motd;
    }

    public void modifySubMotd(String subMotd) {
        this.subMotd = subMotd;
    }

    public void modifyWhitelist(boolean b) {
        this.whiteList = b;
    }

    public void modifyGamemode(int id) {
        this.gamemode = id;
    }

    public void modifyDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty.getId();
    }
}
