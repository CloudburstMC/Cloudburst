package org.cloudburstmc.server.command.defaults;

import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.api.plugin.PluginContainer;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.CommandSender;
import org.cloudburstmc.server.command.data.CommandData;
import org.cloudburstmc.server.network.ProtocolInfo;
import org.cloudburstmc.server.registry.CommandRegistry;
import org.cloudburstmc.server.scheduler.AsyncTask;
import org.cloudburstmc.server.utils.HastebinUtility;
import org.cloudburstmc.server.utils.Utils;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;

@Log4j2
public class DebugPasteCommand extends Command {

    public DebugPasteCommand() {
        super("debugpaste", CommandData.builder("debugpaste")
                .setDescription("commands.debug.description")
                .setPermissions("cloudburst.command.debug.perform")
                .build());
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!this.testPermission(sender)) {
            return true;
        }
        CloudServer server = CloudServer.getInstance();
        server.getScheduler().scheduleAsyncTask(null, new AsyncTask() {
            @Override
            public void onRun() {
                try {
                    CommandRegistry.get().dispatch(sender, "status");
                    Path dataPath = server.getDataPath();
                    String cloudburstYML = HastebinUtility.upload(dataPath.resolve("cloudburst.yml").toFile());
                    String serverProperties = HastebinUtility.upload(dataPath.resolve("server.properties").toFile());
                    String latestLog = HastebinUtility.upload(dataPath.resolve("logs/server.log").toFile());
                    String threadDump = HastebinUtility.upload(Utils.getAllThreadDumps());

                    StringBuilder b = new StringBuilder();
                    b.append("# Files\n");
                    b.append("links.cloudburst_yml: ").append(cloudburstYML).append('\n');
                    b.append("links.server_properties: ").append(serverProperties).append('\n');
                    b.append("links.server_log: ").append(latestLog).append('\n');
                    b.append("links.thread_dump: ").append(threadDump).append('\n');
                    b.append("\n# Server Information\n");

                    b.append("version.api: ").append(server.getApiVersion()).append('\n');
                    b.append("version.cloudburst: ").append(server.getImplementationVersion()).append('\n');
                    b.append("version.minecraft: ").append(server.getVersion()).append('\n');
                    b.append("version.protocol: ").append(ProtocolInfo.getDefaultProtocolVersion()).append('\n');
                    b.append("plugins:");
                    for (PluginContainer plugin : server.getPluginManager().getAllPlugins()) {
                        String name = plugin.getDescription().getName();
                        b.append("\n  ")
                                .append(name)
                                .append(":\n    ")
                                .append("version: '")
                                .append(plugin.getDescription().getVersion())
                                .append('\'');
                    }
                    b.append("\n\n# Java Details\n");
                    Runtime runtime = Runtime.getRuntime();
                    b.append("memory.free: ").append(runtime.freeMemory()).append('\n');
                    b.append("memory.max: ").append(runtime.maxMemory()).append('\n');
                    b.append("cpu.runtime: ").append(ManagementFactory.getRuntimeMXBean().getUptime()).append('\n');
                    b.append("cpu.processors: ").append(runtime.availableProcessors()).append('\n');
                    b.append("java.specification.version: '").append(System.getProperty("java.specification.version")).append("'\n");
                    b.append("java.vendor: '").append(System.getProperty("java.vendor")).append("'\n");
                    b.append("java.version: '").append(System.getProperty("java.version")).append("'\n");
                    b.append("os.arch: '").append(System.getProperty("os.arch")).append("'\n");
                    b.append("os.name: '").append(System.getProperty("os.name")).append("'\n");
                    b.append("os.version: '").append(System.getProperty("os.version")).append("'\n\n");
                    b.append("\n# Create a ticket: https://github.com/CloudburstMC/Server/issues/new");
                    String link = HastebinUtility.upload(b.toString());
                    sender.sendMessage(link);
                } catch (IOException e) {
                    log.error("Error creating debug paste", e);
                }
            }
        });
        return true;
    }
}
