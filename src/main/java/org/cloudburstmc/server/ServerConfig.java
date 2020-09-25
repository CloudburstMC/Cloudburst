package org.cloudburstmc.server;

import org.cloudburstmc.server.level.Difficulty;
import org.cloudburstmc.server.player.GameMode;

import java.nio.file.Path;
import java.util.Properties;

/**
 * the universal public facing facade for the server's config
 * so you dont have to care about where the config actually lies
 */
public class ServerConfig {

    private final ServerProperties properties;

    private final CloudburstYaml cloudburstYaml;

    public ServerConfig(ServerProperties properties, CloudburstYaml cloudburstYaml) {
        this.properties = properties;
        this.cloudburstYaml = cloudburstYaml;
    }

    // forwarding server.properties

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

    public boolean getBugReport() {
        return properties.getBugReport();
    }

    public boolean getXboxAuth() {
        return properties.getXboxAuth();
    }

    // forwarding cloudburst.yml //

    // escape hatch //

    public ServerProperties getServerProperties() {
        return properties;
    }

    public CloudburstYaml getCloudburstYaml() { return cloudburstYaml; }

}
