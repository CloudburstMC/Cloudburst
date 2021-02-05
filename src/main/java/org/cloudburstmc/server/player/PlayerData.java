package org.cloudburstmc.server.player;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.api.player.GameMode;
import org.cloudburstmc.math.vector.Vector3i;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.OptionalLong;
import java.util.Set;

public class PlayerData {
    private static final String TAG_FIRST_PLAYED = "firstPlayed";
    private static final String TAG_LAST_PLAYED = "lastPlayed";
    private static final String TAG_LAST_ADDRESS = "lastIP";
    private static final String TAG_LEVEL = "Level";
    private static final String TAG_SPAWN_LEVEL = "SpawnLevel";
    private static final String TAG_SPAWN_X = "SpawnX";
    private static final String TAG_SPAWN_Y = "SpawnY";
    private static final String TAG_SPAWN_Z = "SpawnZ";
    private static final String TAG_ACHIEVEMENTS = "Achievements";
    private static final String TAG_GAME_TYPE = "playerGameType";
    private final Set<String> achievements = new HashSet<>();
    private long firstPlayed;
    private long lastPlayed;
    private String level;
    private String spawnLevel;
    private Vector3i spawnLocation;
    private GameMode gamemode;
    private InetAddress lastAddress;

    public void loadData(NbtMap tag) {
        tag.listenForLong(TAG_FIRST_PLAYED, this::setFirstPlayed);
        tag.listenForLong(TAG_LAST_PLAYED, this::setLastPlayed);
        tag.listenForString(TAG_LEVEL, this::setLevel);
        tag.listenForString(TAG_SPAWN_LEVEL, this::setSpawnLevel);

        if (tag.containsKey(TAG_SPAWN_X) && tag.containsKey(TAG_SPAWN_Y) && tag.containsKey(TAG_SPAWN_Z)) {
            this.setSpawnLocation(Vector3i.from(
                    tag.getInt(TAG_SPAWN_X),
                    tag.getInt(TAG_SPAWN_Y),
                    tag.getInt(TAG_SPAWN_Z)
            ));
        }
        tag.listenForInt(TAG_GAME_TYPE, gm -> setGamemode(GameMode.from(gm)));
        tag.listenForString(TAG_LAST_ADDRESS, this::setLastAddress);
        /*tag.listenForCompound(TAG_ACHIEVEMENTS, achievementsTag -> {
            achievementsTag.getValue().forEach((achievement, tag1) -> {
                if (tag1 instanceof ByteTag && ((ByteTag) tag1).getAsBoolean()) {
                    this.achievements.add(achievement);
                }
            });
        });*/
    }

    public void saveData(NbtMapBuilder tag) {
        if (this.firstPlayed > 0 && this.lastPlayed > 0) {
            tag.putLong(TAG_FIRST_PLAYED, this.firstPlayed);
            tag.putLong(TAG_LAST_PLAYED, this.lastPlayed);
        }
        if (this.level != null) {
            tag.putString(TAG_LEVEL, this.level);
        }
        if (this.spawnLevel != null && spawnLocation != null) {
            tag.putString(TAG_SPAWN_LEVEL, this.spawnLevel)
                    .putInt(TAG_SPAWN_X, this.spawnLocation.getX())
                    .putInt(TAG_SPAWN_Y, this.spawnLocation.getY())
                    .putInt(TAG_SPAWN_Z, this.spawnLocation.getZ());
        }

    }

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

    private void setLastAddress(String lastAddress) {
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
