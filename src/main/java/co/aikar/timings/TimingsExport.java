/*
 * This file is licensed under the MIT License (MIT).
 *
 * Copyright (c) 2014 Daniel Ennis <http://aikar.co>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package co.aikar.timings;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.server.Bootstrap;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.command.ConsoleCommandSender;
import org.cloudburstmc.server.locale.TranslationContainer;
import org.cloudburstmc.server.timings.JsonUtil;
import org.cloudburstmc.server.utils.TextFormat;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.GZIPOutputStream;

import static co.aikar.timings.TimingsManager.HISTORY;

@Log4j2
public class TimingsExport extends Thread {
    private final CommandSender sender;
    private final ObjectNode out;
    private final TimingsHistory[] history;

    private TimingsExport(CommandSender sender, ObjectNode out, TimingsHistory[] history) {
        super("Timings paste thread");
        this.sender = sender;
        this.out = out;
        this.history = history;
    }

    /**
     * Builds a JSON timings report and sends it to Aikar's viewer
     *
     * @param sender Sender that issued the command
     */
    public static void reportTimings(CommandSender sender) {
        ObjectNode out = Bootstrap.JSON_MAPPER.createObjectNode();
        out.put("version", CloudServer.getInstance().getVersion());
        out.put("maxplayers", CloudServer.getInstance().getMaxPlayers());
        out.put("start", TimingsManager.timingStart / 1000);
        out.put("end", System.currentTimeMillis() / 1000);
        out.put("sampletime", (System.currentTimeMillis() - TimingsManager.timingStart) / 1000);

        if (!Timings.isPrivacy()) {
            out.put("server", CloudServer.getInstance().getName());
            out.put("motd", CloudServer.getInstance().getMotd());
            out.put("online-mode", CloudServer.getInstance().getConfig().isXboxAuth());
            out.put("icon", ""); //"data:image/png;base64,"
        }

        final Runtime runtime = Runtime.getRuntime();
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();

        ObjectNode system = Bootstrap.JSON_MAPPER.createObjectNode();
        system.put("timingcost", getCost());
        system.put("name", System.getProperty("os.name"));
        system.put("version", System.getProperty("os.version"));
        system.put("jvmversion", System.getProperty("java.version"));
        system.put("arch", System.getProperty("os.arch"));
        system.put("maxmem", runtime.maxMemory());
        system.put("cpu", runtime.availableProcessors());
        system.put("runtime", ManagementFactory.getRuntimeMXBean().getUptime());
        system.put("flags", String.join(" ", runtimeBean.getInputArguments()));
        system.set("gc", JsonUtil.mapToObject(ManagementFactory.getGarbageCollectorMXBeans(), (input) ->
                new JsonUtil.JSONPair(input.getName(), JsonUtil.toArray(input.getCollectionCount(), input.getCollectionTime()))));
        out.set("system", system);

        TimingsHistory[] history = HISTORY.toArray(new TimingsHistory[HISTORY.size() + 1]);
        history[HISTORY.size()] = new TimingsHistory(); //Current snapshot

        ObjectNode timings = Bootstrap.JSON_MAPPER.createObjectNode();
        for (TimingIdentifier.TimingGroup group : TimingIdentifier.GROUP_MAP.values()) {
            for (Timing id : group.timings) {
                if (!id.timed && !id.isSpecial()) {
                    continue;
                }

                timings.set(String.valueOf(id.id), JsonUtil.toArray(group.id, id.name));
            }
        }

        ObjectNode idmap = Bootstrap.JSON_MAPPER.createObjectNode();
        idmap.set("groups", JsonUtil.mapToObject(TimingIdentifier.GROUP_MAP.values(), (group) ->
                new JsonUtil.JSONPair(group.id, group.name)));
        idmap.set("handlers", timings);
        idmap.set("worlds", JsonUtil.mapToObject(TimingsHistory.levelMap.entrySet(), (entry) ->
                new JsonUtil.JSONPair(entry.getValue(), entry.getKey())));
        idmap.set("tileentity", JsonUtil.mapToObject(TimingsHistory.blockEntityMap.entrySet(), (entry) ->
                new JsonUtil.JSONPair(entry.getKey().toString(), entry.getValue())));
        idmap.set("entity", JsonUtil.mapToObject(TimingsHistory.entityMap.entrySet(), (entry) ->
                new JsonUtil.JSONPair(entry.getKey().toString(), entry.getValue())));
        out.set("idmap", idmap);

        //Information about loaded plugins
        out.set("plugins", JsonUtil.mapToObject(CloudServer.getInstance().getPluginManager().getAllPlugins(), (plugin) -> {
            ObjectNode jsonPlugin = Bootstrap.JSON_MAPPER.createObjectNode();
            jsonPlugin.put("version", plugin.getDescription().getVersion());

            plugin.getDescription().getDescription().ifPresent((desc) -> jsonPlugin.put("description", desc));
            plugin.getDescription().getUrl().ifPresent((url) -> jsonPlugin.put("website", url));
            jsonPlugin.putPOJO("authors", String.join(", ", plugin.getDescription().getAuthors()));
            return new JsonUtil.JSONPair(plugin.getDescription().getName(), jsonPlugin);
        }));

        //Information on the users Config
        ObjectNode config = Bootstrap.JSON_MAPPER.createObjectNode();
        if (!Timings.getIgnoredConfigSections().contains("all")) {
            final ObjectNode rootNode = CloudServer.getInstance().getConfig().getCloudburstYaml().getRootNode();
            Timings.getIgnoredConfigSections().forEach(rootNode::remove);
            config.set("cloudburst", rootNode);
        } else {
            config.set("cloudburst", null);
        }
        out.set("config", config);

        new TimingsExport(sender, out, history).start();
    }

    private static long getCost() {
        int passes = 200;
        Timing SAMPLER1 = TimingsManager.getTiming(null, "Timings sampler 1", null);
        Timing SAMPLER2 = TimingsManager.getTiming(null, "Timings sampler 2", null);
        Timing SAMPLER3 = TimingsManager.getTiming(null, "Timings sampler 3", null);
        Timing SAMPLER4 = TimingsManager.getTiming(null, "Timings sampler 4", null);
        Timing SAMPLER5 = TimingsManager.getTiming(null, "Timings sampler 5", null);
        Timing SAMPLER6 = TimingsManager.getTiming(null, "Timings sampler 6", null);

        long start = System.nanoTime();
        for (int i = 0; i < passes; i++) {
            SAMPLER1.startTiming();
            SAMPLER2.startTiming();
            SAMPLER3.startTiming();
            SAMPLER4.startTiming();
            SAMPLER5.startTiming();
            SAMPLER6.startTiming();
            SAMPLER6.stopTiming();
            SAMPLER5.stopTiming();
            SAMPLER4.stopTiming();
            SAMPLER3.stopTiming();
            SAMPLER2.stopTiming();
            SAMPLER1.stopTiming();
        }

        long timingsCost = (System.nanoTime() - start) / passes / 6;

        SAMPLER1.reset(true);
        SAMPLER2.reset(true);
        SAMPLER3.reset(true);
        SAMPLER4.reset(true);
        SAMPLER5.reset(true);
        SAMPLER6.reset(true);

        return timingsCost;
    }

    @Override
    public void run() {
        this.sender.sendMessage(new TranslationContainer("cloudburst.command.timings.uploadStart"));
        this.out.set("data", JsonUtil.mapToArray(this.history, TimingsHistory::export));

        String response = null;
        try {
            HttpURLConnection con = (HttpURLConnection) new URL("http://timings.aikar.co/post").openConnection();
            con.setDoOutput(true);
            con.setRequestProperty("User-Agent", "Cloudburst/" + CloudServer.getInstance().getName() + "/" + InetAddress.getLocalHost().getHostName());
            con.setRequestMethod("POST");
            con.setInstanceFollowRedirects(false);

            try (GZIPOutputStream outputStream = new GZIPOutputStream(con.getOutputStream())) {
                outputStream.write(Bootstrap.JSON_MAPPER.writeValueAsBytes(this.out));
            }

            response = getResponse(con);

            if (con.getResponseCode() != 302) {
                this.sender.sendMessage(new TranslationContainer("cloudburst.command.timings.uploadError", String.valueOf(con.getResponseCode()), con.getResponseMessage()));
                if (response != null) {
                    log.warn(response);
                }
                return;
            }

            String location = con.getHeaderField("Location");
            this.sender.sendMessage(new TranslationContainer("cloudburst.command.timings.timingsLocation", location));
            if (!(this.sender instanceof ConsoleCommandSender)) {
                log.info(CloudServer.getInstance().getLanguage().translate("cloudburst.command.timings.timingsLocation", location));
            }

            if (response != null && !response.isEmpty()) {
                log.info(CloudServer.getInstance().getLanguage().translate("cloudburst.command.timings.timingsResponse", response));
            }

            File timingFolder = new File(CloudServer.getInstance().getDataPath() + File.separator + "timings");
            timingFolder.mkdirs();
            String fileName = timingFolder + File.separator + new SimpleDateFormat("'timings-'yyyy-MM-dd-hh-mm'.txt'").format(new Date());

            FileWriter writer = new FileWriter(fileName);
            writer.write(CloudServer.getInstance().getLanguage().translate("cloudburst.command.timings.timingsLocation", location) + "\n\n");
            writer.write(Bootstrap.JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(this.out));
            writer.close();

            log.info(CloudServer.getInstance().getLanguage().translate("cloudburst.command.timings.timingsWrite", fileName));
        } catch (IOException exception) {
            this.sender.sendMessage(TextFormat.RED + "" + new TranslationContainer("cloudburst.command.timings.reportError"));
            if (response != null) {
                log.warn(response);
            }
            log.throwing(Level.ERROR, exception);
        }
    }

    private String getResponse(HttpURLConnection con) throws IOException {
        try (InputStream is = con.getInputStream()) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();

            byte[] b = new byte[1024];
            int bytesRead;
            while ((bytesRead = is.read(b)) != -1) {
                bos.write(b, 0, bytesRead);
            }
            return bos.toString();

        } catch (IOException exception) {
            this.sender.sendMessage(TextFormat.RED + "" + new TranslationContainer("cloudburst.command.timings.reportError"));
            log.warn(con.getResponseMessage(), exception);
            return null;
        }
    }
}
