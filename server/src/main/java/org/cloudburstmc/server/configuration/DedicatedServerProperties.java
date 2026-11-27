package org.cloudburstmc.server.configuration;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.*;
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
    private String serverIp = "";

    @Builder.Default
    @SettingConstraint(min = 1, max = 65535)
    private int serverPort = 19132;

    @Builder.Default
    private boolean onlineMode = true;

    @Builder.Default
    @SettingConstraint(min = 1)
    private int maxPlayers = 20;

    @Builder.Default
    @SettingConstraint(min = 1)
    private int viewDistance = 10;

    @Builder.Default
    @SettingConstraint(min = 1)
    private int simulationDistance = 10;

    @Builder.Default
    private boolean whitelist = false;

    @Builder.Default
    private boolean enforceWhitelist = false;

    @Builder.Default
    @SettingConstraint(nonBlank = true)
    private String levelName = "world";

    @Builder.Default
    @SettingConstraint(nonBlank = true)
    private String gamemode = "survival";

    @Builder.Default
    private boolean forceGamemode = false;

    @Builder.Default
    @SettingConstraint(nonBlank = true)
    private String difficulty = "easy";

    @Builder.Default
    private boolean hardcore = false;

    @Builder.Default
    @SettingConstraint(min = 0)
    private int spawnProtection = 16;

    @Builder.Default
    @SettingConstraint(min = 0, max = 4)
    private int opPermissionLevel = 4;

    @Builder.Default
    private boolean allowFlight = false;

    @Builder.Default
    private boolean requireResourcePack = false;

    @Builder.Default
    @SettingConstraint(min = 0)
    private int playerIdleTimeout = 0;

    @Builder.Default
    @SettingConstraint(min = 0, max = 65535)
    private int networkCompressionThreshold = 1;

    @Builder.Default
    @SettingConstraint(min = 0)
    private int rateLimit = 0;

    @Builder.Default
    private boolean logIps = true;

    @Builder.Default
    private long maxTickTime = 60000;

    public void modifyMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public void modifySpawnProtection(int spawnProtection) {
        this.spawnProtection = spawnProtection;
    }

    public void modifyPlayerIdleTimeout(int playerIdleTimeout) {
        this.playerIdleTimeout = playerIdleTimeout;
    }

    public void modifyMotd(String motd) {
        this.motd = motd;
    }

    public void modifySubMotd(String subMotd) {
        this.subMotd = subMotd;
    }

    public void modifyWhitelist(boolean whitelist) {
        this.whitelist = whitelist;
    }

    public void modifyEnforceWhitelist(boolean enforceWhitelist) {
        this.enforceWhitelist = enforceWhitelist;
    }

    public void modifyGamemode(String gamemode) {
        this.gamemode = gamemode;
    }

    public void modifyDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }
}
