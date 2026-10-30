package org.cloudburstmc.server.player;

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
