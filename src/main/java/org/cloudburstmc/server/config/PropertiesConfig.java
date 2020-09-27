package org.cloudburstmc.server.config;

import org.cloudburstmc.server.level.Difficulty;
import org.cloudburstmc.server.player.GameMode;

public class PropertiesConfig {

    private final ServerProperties properties;

    public PropertiesConfig(ServerProperties properties) {
        this.properties = properties;
    }

    public String getMotd() {
        return properties.getMotd();
    }

    public String getSubMotd() {
        return properties.getSubMotd();
    }

    public String getIp() {
        return properties.getServerIp();
    }

    public int getPort() {
        return properties.getServerPort();
    }

    public int getViewDistance() {
        return properties.getViewDistance();
    }

    public boolean getWhitelist() {
        return properties.isWhiteList();
    }

    public void setWhitelist(boolean b) {
        properties.modifyWhitelist(b);
    }

    public boolean getAchievements() {
        return properties.isAchievements();
    }

    public boolean getAnnouncePlayerAchievements() {
        return properties.isAnnouncePlayerAchievements();
    }

    public int getSpawnRadius() {
        return properties.getSpawnProtection();
    }

    public int getMaxPlayers() {
        return properties.getMaxPlayers();
    }

    public boolean getAllowFlight() {
        return properties.isAllowFlight();
    }

    public boolean getSpawnAnimals() {
        return properties.isSpawnAnimals();
    }

    public boolean getSpawnMobs() {
        return properties.isSpawnMobs();
    }

    public GameMode getGamemode() {
        return GameMode.from(properties.getGamemode());
    }

    public void setGamemode(GameMode gameMode) {
        properties.modifyGamemode(gameMode);
    }

    public boolean getForceGamemode() {
        return properties.isForceGamemode();
    }

    public boolean getHardcore() {
        return properties.isHardcore();
    }

    public boolean getPVP() {
        return properties.isPvp();
    }

    public Difficulty getDifficulty() {
        return Difficulty.values()[properties.getDifficulty()];
    }

    public void setDifficulty(Difficulty difficulty) {
        properties.modifyDifficulty(difficulty);
    }

    public String getDefaultLevel() {
        return properties.getDefaultLevel();
    }

    public boolean getAllowNether() {
        return properties.isAllowNether();
    }

    public boolean getEnableQuery() {
        return properties.isEnableQuery();
    }

    public boolean getAutoSave() {
        return properties.isAutoSave();
    }

    public boolean getForceResources() {
        return properties.isForceResources();
    }

    public boolean getGenerateStructures() {
        return properties.isGenerateStructures();
    }

    public boolean getXboxAuth() {
        return properties.isXboxAuth();
    }

}
