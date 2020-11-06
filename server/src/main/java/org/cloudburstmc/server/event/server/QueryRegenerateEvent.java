package org.cloudburstmc.server.event.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.cloudburstmc.api.plugin.PluginContainer;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.player.Player;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class QueryRegenerateEvent extends ServerEvent {
    //alot todo

    private static final Pattern PLUGIN_NAME_PATTERN = Pattern.compile("[;: ]");
    private static final String GAME_ID = "MINECRAFTPE";

    private int timeout;
    private String serverName;
    private boolean listPlugins;
    private PluginContainer[] plugins;
    private Player[] players;

    private final String gameType;
    private final String version;
    private final String server_engine;
    private String map;
    private int numPlayers;
    private int maxPlayers;
    private final String whitelist;
    private final int port;
    private final String ip;

    private Map<String, String> extraData = new HashMap<>();

    public QueryRegenerateEvent(CloudServer server) {
        this(server, 5);
    }

    public QueryRegenerateEvent(CloudServer server, int timeout) {
        this.timeout = timeout;
        this.serverName = server.getMotd();
        this.listPlugins = server.getConfig().getSettings().isQueryPlugins();
        this.plugins = server.getPluginManager().getAllPlugins().toArray(new PluginContainer[0]);
        this.players = server.getOnlinePlayers().values().toArray(new Player[0]);
        this.gameType = server.getGamemode().isSurvival() ? "SMP" : "CMP";
        this.version = server.getVersion();
        this.server_engine = server.getName() + " " + server.getImplementationVersion();
        this.map = server.getDefaultLevel() == null ? "unknown" : server.getDefaultLevel().getName();
        this.numPlayers = this.players.length;
        this.maxPlayers = server.getMaxPlayers();
        this.whitelist = server.hasWhitelist() ? "on" : "off";
        this.port = server.getPort();
        this.ip = server.getIp();
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public boolean canListPlugins() {
        return this.listPlugins;
    }

    public void setListPlugins(boolean listPlugins) {
        this.listPlugins = listPlugins;
    }

    public PluginContainer[] getPlugins() {
        return plugins;
    }

    public void setPlugins(PluginContainer[] plugins) {
        this.plugins = plugins;
    }

    public Player[] getPlayerList() {
        return players;
    }

    public void setPlayerList(Player[] players) {
        this.players = players;
    }

    public int getPlayerCount() {
        return this.numPlayers;
    }

    public void setPlayerCount(int count) {
        this.numPlayers = count;
    }

    public int getMaxPlayerCount() {
        return this.maxPlayers;
    }

    public void setMaxPlayerCount(int count) {
        this.maxPlayers = count;
    }

    public String getWorld() {
        return map;
    }

    public void setWorld(String world) {
        this.map = world;
    }

    public Map<String, String> getExtraData() {
        return extraData;
    }

    public void setExtraData(Map<String, String> extraData) {
        this.extraData = extraData;
    }

    public byte[] getLongQuery() {
        ByteArrayOutputStream query = new ByteArrayOutputStream();
        try {

            String plist = this.server_engine;

            if (this.plugins.length > 0 && this.listPlugins) {
                StringJoiner joiner = new StringJoiner(";");
                Matcher matcher = PLUGIN_NAME_PATTERN.matcher("");
                for (PluginContainer p : this.plugins) {
                    String name = matcher.reset(p.getDescription().getName()).replaceAll(" ");
                    String version = matcher.reset(p.getDescription().getVersion()).replaceAll(" ");
                    joiner.add(name + " " + version);
                }
                plist += ":" + joiner.toString();
            }

            query.write("splitnum".getBytes());
            query.write((byte) 0x00);
            query.write((byte) 128);
            query.write((byte) 0x00);

            LinkedHashMap<String, String> KVdata = new LinkedHashMap<>();
            KVdata.put("hostname", this.serverName);
            KVdata.put("gametype", this.gameType);
            KVdata.put("game_id", GAME_ID);
            KVdata.put("version", this.version);
            KVdata.put("server_engine", this.server_engine);
            KVdata.put("plugins", plist);
            KVdata.put("map", this.map);
            KVdata.put("numplayers", String.valueOf(this.numPlayers));
            KVdata.put("maxplayers", String.valueOf(this.maxPlayers));
            KVdata.put("whitelist", this.whitelist);
            KVdata.put("hostip", this.ip);
            KVdata.put("hostport", String.valueOf(this.port));

            for (Map.Entry<String, String> entry : KVdata.entrySet()) {
                query.write(entry.getKey().getBytes(StandardCharsets.UTF_8));
                query.write((byte) 0x00);
                query.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                query.write((byte) 0x00);
            }

            query.write(new byte[]{0x00, 0x01});
            query.write("player_".getBytes());
            query.write(new byte[]{0x00, 0x00});

            for (Player player : this.players) {
                query.write(player.getName().getBytes(StandardCharsets.UTF_8));
                query.write((byte) 0x00);
            }

            query.write((byte) 0x00);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return query.toByteArray();
    }

    private static void writeNullTerminatedString(ByteBuf buffer, String string) {
        buffer.writeBytes(string.getBytes(StandardCharsets.UTF_8));
        buffer.writeByte(0);
    }

    public byte[] getShortQuery() {
        ByteBuf buffer = ByteBufAllocator.DEFAULT.heapBuffer();
        try {
            writeNullTerminatedString(buffer, this.serverName);
            writeNullTerminatedString(buffer, this.gameType);
            writeNullTerminatedString(buffer, this.map);
            writeNullTerminatedString(buffer, Integer.toString(this.numPlayers));
            writeNullTerminatedString(buffer, Integer.toString(this.maxPlayers));
            buffer.writeShortLE(this.port);
            writeNullTerminatedString(buffer, this.ip);

            byte[] bytes = new byte[buffer.readableBytes()];
            buffer.readBytes(bytes);
            return bytes;
        } finally {
            buffer.release();
        }
    }
}
