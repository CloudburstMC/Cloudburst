package org.cloudburstmc.server.diagnostics;

import lombok.experimental.UtilityClass;

import java.lang.management.ManagementFactory;
import java.util.List;

/**
 * Runtime details that can be collected without reading mutable level state.
 */
@UtilityClass
public class RuntimeDiagnostics {

    public static void append(StringBuilder report) {
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
    }
}
