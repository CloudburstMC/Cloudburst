package org.cloudburstmc.server.player;

import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.api.player.GameMode;
import org.cloudburstmc.math.vector.Vector3i;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.OptionalLong;
import java.util.Set;

public class PlayerData {
    private final Set<String> achievements = new HashSet<>();
    private long firstPlayed;
    private long lastPlayed;
    private String level;
    private String spawnLevel;
    private Vector3i spawnLocation;
    private float spawnYaw;
    private GameMode gamemode;
    private InetAddress lastAddress;

    /**
     * The values the stored tag held for the conditionally written keys. A key the
     * tag held is written back with its loaded value when the holder condition
     * fails at save time, and a condition that holds refreshes them from the
     * holder, exactly as the previous persistent data object kept and rewrote
     * its properties. A key never loaded nor saved stays {@code null} and is
     * omitted, following the save condition.
     */
    @Getter
    @Setter
    private Long loadedFirstPlayed;
    @Getter
    @Setter
    private Long loadedLastPlayed;
    @Getter
    @Setter
    private String loadedSpawnLevel;
    @Getter
    @Setter
    private Integer loadedSpawnX;
    @Getter
    @Setter
    private Integer loadedSpawnY;
    @Getter
    @Setter
    private Integer loadedSpawnZ;
    @Getter
    @Setter
    private Float loadedSpawnYaw;
    @Getter
    @Setter
    private Integer loadedPlayerGameType;

    public OptionalLong getFirstPlayed() {
        return OptionalLong.of(firstPlayed);
    }

    public void setFirstPlayed(long firstPlayed) {
        this.firstPlayed = firstPlayed;
    }

    public OptionalLong getLastPlayed() {
        return OptionalLong.of(lastPlayed);
    }

    public void setLastPlayed(long lastPlayed) {
        this.lastPlayed = lastPlayed;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getSpawnLevel() {
        return spawnLevel;
    }

    public void setSpawnLevel(String spawnLevel) {
        this.spawnLevel = spawnLevel;
    }

    public Vector3i getSpawnLocation() {
        return spawnLocation;
    }

    public void setSpawnLocation(Vector3i spawnLocation) {
        this.spawnLocation = spawnLocation;
    }

    public float getSpawnYaw() {
        return spawnYaw;
    }

    public void setSpawnYaw(float spawnYaw) {
        this.spawnYaw = spawnYaw;
    }

    public GameMode getGamemode() {
        return gamemode;
    }

    public void setGamemode(GameMode gamemode) {
        this.gamemode = gamemode;
    }

    public InetAddress getLastAddress() {
        return lastAddress;
    }

    public void setLastAddress(InetAddress lastAddress) {
        this.lastAddress = lastAddress;
    }

    void setLastAddress(String lastAddress) {
        try {
            this.lastAddress = InetAddress.getByName(lastAddress);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Invalid player address", e);
        }
    }

    public Set<String> getAchievements() {
        return achievements;
    }
}
