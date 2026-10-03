package org.cloudburstmc.server.diagnostics;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.plugin.PluginContainer;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.network.ProtocolInfo;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.regex.Pattern;

@UtilityClass
public class DebugReport {
    private static final int CONFIG_BYTES = 256 * 1024;
    private static final int LOG_BYTES = 512 * 1024;
    private static final int REPORT_CHARACTERS = 2 * 1024 * 1024;
    private static final int REPORT_LINES = 20_000;
    private static final YAMLMapper YAML = YAMLMapper.builder().build();
    private static final Pattern FORMATTING = Pattern.compile("(?i)\\u00a7[0-9a-vx]|\\x1B\\[[0-?]*[ -/]*[@-~]");

    public static String capture(CloudServer server) {
        StringBuilder report = new StringBuilder("# Server\n");
        report.append("API: ").append(server.getApiVersion()).append('\n');
        report.append("Implementation: ").append(server.getImplementationVersion()).append('\n');
        report.append("Minecraft: ").append(server.getVersion()).append('\n');
        report.append("Protocol: ").append(ProtocolInfo.getDefaultProtocolVersion()).append('\n');
        report.append("TPS: ").append(server.getTicksPerSecond()).append('\n');
        report.append("Tick usage: ").append(server.getTickUsage()).append("%\n");
        report.append("Network bytes sent since statistics reset: ").append(server.getNetwork().getUpload()).append('\n');
        report.append("Network bytes received since statistics reset: ").append(server.getNetwork().getDownload()).append('\n');
        report.append("Players: ").append(server.getOnlinePlayers().size()).append('/').append(server.getMaxPlayers()).append('\n');

        for (CloudLevel level : server.getLevels()) {
            report.append("Level ").append(level.getId()).append(": chunks=").append(level.getChunks().size())
                    .append(" entities=").append(level.getEntities().size())
                    .append(" blockEntities=").append(level.getBlockEntities().size())
                    .append(" pendingLiquidTicks=").append(level.getLiquidUpdateQueue().getPendingCount())
                    .append(" tickMs=").append(level.getLastTickDuration()).append('\n');
        }

        report.append("\n# Plugins\n");
        for (PluginContainer plugin : server.getPluginManager().getAllPlugins()) {
            report.append(plugin.getDescription().getName()).append(": ")
                    .append(plugin.getDescription().getVersion()).append('\n');
        }

        Runtime runtime = Runtime.getRuntime();
        report.append("\n# Runtime\n");
        report.append("Uptime ms: ").append(ManagementFactory.getRuntimeMXBean().getUptime()).append('\n');
        report.append("Memory used: ").append(runtime.totalMemory() - runtime.freeMemory()).append('\n');
        report.append("Memory total: ").append(runtime.totalMemory()).append('\n');
        report.append("Memory max: ").append(runtime.maxMemory()).append('\n');
        report.append("Processors: ").append(runtime.availableProcessors()).append('\n');
        report.append("Platform threads: ").append(ManagementFactory.getThreadMXBean().getThreadCount()).append('\n');

        for (String property : List.of("java.vendor", "java.version", "os.arch", "os.name", "os.version")) {
            report.append(property).append(": ").append(System.getProperty(property)).append('\n');
        }

        return report.toString();
    }

    public static String collect(String snapshot, Path directory) {
        StringBuilder report = new StringBuilder(snapshot);
        appendConfiguration(report, directory.resolve("cloudburst.yml"), false);
        appendConfiguration(report, directory.resolve("server.properties"), true);
        report.append("\n# Recent server log\n");

        try {
            report.append(readLogTail(directory.resolve("logs/server.log")));
        } catch (IOException e) {
            report.append("[Log unavailable]\n");
        }

        report.append("\n# Thread dump\n").append(ThreadDump.capture());
        return limit(FORMATTING.matcher(report.toString()).replaceAll(""));
    }

    private static void appendConfiguration(StringBuilder report, Path path, boolean propertiesFile) {
        report.append("\n# ").append(path.getFileName()).append('\n');
        try (InputStream input = Files.newInputStream(path)) {
            byte[] bytes = input.readNBytes(CONFIG_BYTES + 1);
            if (bytes.length > CONFIG_BYTES) {
                report.append("[Configuration omitted: exceeds size limit]\n");
                return;
            }

            Object configuration;
            if (propertiesFile) {
                Properties properties = new Properties();
                properties.load(new ByteArrayInputStream(bytes));
                Map<String, String> values = new TreeMap<>();
                for (String name : properties.stringPropertyNames()) {
                    values.put(name, properties.getProperty(name));
                }
                configuration = values;
            } else {
                configuration = YAML.readValue(bytes, Object.class);
            }

            report.append(YAML.writeValueAsString(redact(configuration)));
        } catch (IOException | RuntimeException e) {
            report.append("[Configuration unavailable or invalid]\n");
        }
    }

    private static Object redact(Object value) {
        if (value instanceof Map<?, ?> values) {
            Map<String, Object> result = new LinkedHashMap<>();
            values.forEach((key, entry) -> {
                String name = key.toString();
                String normalized = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
                boolean sensitive = normalized.contains("password") || normalized.contains("secret")
                        || normalized.contains("token") || normalized.contains("apikey")
                        || normalized.contains("privatekey") || normalized.contains("credential");
                result.put(name, sensitive ? "[REDACTED]" : redact(entry));
            });

            return result;
        }

        if (value instanceof List<?> values) {
            List<Object> result = new ArrayList<>(values.size());
            values.forEach(entry -> result.add(redact(entry)));
            return result;
        }

        return value;
    }

    private static String readLogTail(Path path) throws IOException {
        try (SeekableByteChannel channel = Files.newByteChannel(path)) {
            long start = Math.max(0, channel.size() - LOG_BYTES);
            channel.position(start);

            ByteBuffer bytes = ByteBuffer.allocate(LOG_BYTES);
            while (bytes.hasRemaining()) {
                if (channel.read(bytes) <= 0) {
                    break;
                }
            }

            bytes.flip();

            String text = StandardCharsets.UTF_8.decode(bytes).toString();
            if (start > 0) {
                int newline = text.indexOf('\n');
                text = newline < 0 ? "" : text.substring(newline + 1);
            }

            List<String> lines = text.lines().toList();
            int first = Math.max(0, lines.size() - 5000);

            return (start > 0 || first > 0 ? "[Earlier log entries omitted]\n" : "") + String.join("\n", lines.subList(first, lines.size())) + '\n';
        }
    }

    private static String limit(String report) {
        int end = Math.min(report.length(), REPORT_CHARACTERS);
        int lines = 0;
        for (int index = 0; index < end; index++) {
            if (report.charAt(index) == '\n' && ++lines == REPORT_LINES - 1) {
                end = index;
                break;
            }
        }

        if (end == report.length()) {
            return report;
        }

        if (Character.isHighSurrogate(report.charAt(end - 1))) {
            end--;
        }

        return report.substring(0, end) + "\n[Report truncated]\n";
    }
}
