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
        return properties.getIp();
    }

    public int getPort() {
        return properties.getPort();
    }

    public int getViewDistance() {
        return properties.getViewDistance();
    }

    public boolean getWhitelist() {
        return properties.getWhitelist();
    }

    public void setWhitelist(boolean b) {
        properties.setWhitelist(b);
    }

    public boolean getAchievements() {
        return properties.getAchievements();
    }

    public boolean getAnnouncePlayerAchievements() {
        return properties.getAnnouncePlayerAchievements();
    }

    public int getSpawnRadius() {
        return properties.getSpawnRadius();
    }

    public int getMaxPlayers() {
        return properties.getMaxPlayers();
    }

    public boolean getAllowFlight() {
        return properties.getAllowFlight();
    }

    public boolean getSpawnAnimals() {
        return properties.getSpawnAnimals();
    }

    public boolean getSpawnMobs() {
        return properties.getSpawnMobs();
    }

    public GameMode getGamemode() {
        return properties.getGamemode();
    }

    public void setGamemode(GameMode gameMode) {
        properties.setGamemode(gameMode);
    }

    public boolean getForceGamemode() {
        return properties.getForceGamemode();
    }

    public boolean getHardcore() {
        return properties.getHardcore();
    }

    public boolean getPVP() {
        return properties.getPVP();
    }

    public Difficulty getDifficulty() {
        return properties.getDifficulty();
    }

    public void setDifficulty(Difficulty difficulty) {
        properties.setDifficulty(difficulty);
    }

    public String getDefaultLevel() {
        return properties.getDefaultLevel();
    }

    public boolean getAllowNether() {
        return properties.getAllowNether();
    }

    public boolean getEnableQuery() {
        return properties.getEnableQuery();
    }

    public boolean getAutoSave() {
        return properties.getAutoSave();
    }

    public boolean getForceResources() {
        return properties.getForceResources();
    }

    public boolean getGenerateStructures() {
        return properties.getGenerateStructures();
    }

    public boolean getXboxAuth() {
        return properties.getXboxAuth();
    }

}
