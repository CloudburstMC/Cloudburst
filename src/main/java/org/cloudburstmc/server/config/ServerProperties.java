package org.cloudburstmc.server.config;

import org.cloudburstmc.server.level.Difficulty;
import org.cloudburstmc.server.player.GameMode;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * represents the server.properties file on disk
 */
public class ServerProperties {

    private final Path path;

    private final Properties properties;

    public ServerProperties(Path path) {
        this.path = path;
        properties = new Properties();
        //laod default value
        this.properties.setProperty("motd", "A Cloudburst Powered Server");
        this.properties.setProperty("sub-motd", "https://cloudburstmc.org");
        this.properties.setProperty("server-ip", "0.0.0.0");
        this.properties.setProperty("server-port", "19132");
        this.properties.setProperty("view-distance", "10");
        this.properties.setProperty("white-list", "false");
        this.properties.setProperty("achievements", "true");
        this.properties.setProperty("announce-player-achievements", "true");
        this.properties.setProperty("spawn-protection", "16");
        this.properties.setProperty("max-players", "20");
        this.properties.setProperty("allow-flight", "false");
        this.properties.setProperty("spawn-animals", "true");
        this.properties.setProperty("spawn-mobs", "true");
        this.properties.setProperty("gamemode", "0");
        this.properties.setProperty("force-gamemode", "false");
        this.properties.setProperty("hardcore", "false");
        this.properties.setProperty("pvp", "true");
        this.properties.setProperty("difficulty", "1");
        this.properties.setProperty("default-level", "world");
        this.properties.setProperty("allow-nether", "true");
        this.properties.setProperty("enable-query", "true");
        this.properties.setProperty("auto-save", "true");
        this.properties.setProperty("force-resources", "false");
        this.properties.setProperty("xbox-auth", "true");
    }

    // specialized getters //

    public String getMotd() {
        return this.getProperty("motd", "A Cloudburst Powered Server");
    }

    public String getSubMotd() {
        return this.getProperty("sub-motd", "https://cloudburstmc.org");
    }

    public String getIp() {
        return this.getProperty("server-ip", "0.0.0.0");
    }

    public int getPort() {
        return this.getPropertyInt("server-port", 19132);
    }

    public int getViewDistance() {
        return this.getPropertyInt("view-distance", 10);
    }

    public boolean getWhitelist() {
        return this.getPropertyBoolean("white-list", false);
    }

    public void setWhitelist(boolean b) {
        setPropertyBoolean("white-list", b);
    }

    public boolean getAchievements() {
        return this.getPropertyBoolean("achievements", true);
    }

    public boolean getAnnouncePlayerAchievements() {
        return this.getPropertyBoolean("announce-player-achievements", true);
    }

    public int getSpawnRadius() {
        return this.getPropertyInt("spawn-protection", 16);
    }

    public int getMaxPlayers() {
        return this.getPropertyInt("max-players", 20);
    }

    public boolean getAllowFlight() {
        return this.getPropertyBoolean("allow-flight", false);
    }

    public boolean getSpawnAnimals() {
        return this.getPropertyBoolean("spawn-animals", true);
    }

    public boolean getSpawnMobs() {
        return this.getPropertyBoolean("spawn-mobs", true);
    }

    public GameMode getGamemode() {
        try {
            return GameMode.from(this.getPropertyInt("gamemode", 0));
        } catch (NumberFormatException exception) {
            return GameMode.from(this.getProperty("gamemode"));
        }
    }

    public void setGamemode(GameMode gameMode) {
        setPropertyInt("gamemode", gameMode.getVanillaId());
    }

    public boolean getForceGamemode() {
        return this.getPropertyBoolean("force-gamemode", false);
    }

    public boolean getHardcore() {
        return this.getPropertyBoolean("hardcore", false);
    }

    public boolean getPVP() {
        return this.getPropertyBoolean("pvp", true);
    }

    public Difficulty getDifficulty() {
        return Difficulty.values()[this.getPropertyInt("difficulty", 1) & 0x03];
    }

    public void setDifficulty(Difficulty difficulty) {
        setPropertyInt("difficulty", difficulty.ordinal());
    }

    public String getDefaultLevel() { return this.getProperty("default-level"); }

    public void setDefaultLevel(String name) {
        setProperty("default-level", name);
    }

    public boolean getAllowNether() {
        return this.getPropertyBoolean("allow-nether", true);
    }

    public boolean getEnableQuery() {
        return this.getPropertyBoolean("enable-query", true);
    }

    public boolean getAutoSave() {
        return this.getPropertyBoolean("auto-save", true);
    }

    public boolean getForceResources() {
        return this.getPropertyBoolean("force-resources", false);
    }

    public boolean getGenerateStructures() {
        return this.getPropertyBoolean("generate-structures", true);
    }

    public boolean getXboxAuth() {
        return this.getPropertyBoolean("xbox-auth", true);
    }

    // generic getters //

    public Properties getRawProperties() {
        return properties;
    }

    public Path getPath() {
        return path;
    }

    public Properties getProperties() {
        return properties;
    }

    private String getProperty(String property) {
        return this.getProperty(property, null);
    }

    private String getProperty(String property, String defaultValue) {
        return properties.getProperty(property, defaultValue);
    }

    private void setProperty(String property, String value) {
        properties.setProperty(property, value);
        this.save();
    }

    private int getPropertyInt(String property) {
        return this.getPropertyInt(property, 0);
    }

    private int getPropertyInt(String property, int defaultValue) {
        String value = properties.getProperty(property, Integer.toString(0));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private void setPropertyInt(String property, int value) {
        properties.setProperty(property, Integer.toString(value));
        this.save();
    }

    private boolean getPropertyBoolean(String variable) {
        return this.getPropertyBoolean(variable, false);
    }

    private boolean getPropertyBoolean(String property, boolean defaultValue) {
        if (!properties.containsKey(property)) {
            return defaultValue;
        }
        String value = properties.getProperty(property);

        switch (value) {
            case "on":
            case "true":
            case "1":
            case "yes":
                return true;
            default:
                return false;
        }
    }

    private void setPropertyBoolean(String property, boolean value) {
        properties.setProperty(property, Boolean.toString(value));
    }

    // IO functions //

    public void load() {
        try (InputStream stream = Files.newInputStream(path)) {
            properties.load(stream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void save() {
        try (OutputStream stream = Files.newOutputStream(path)) {
            properties.store(stream, "");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
