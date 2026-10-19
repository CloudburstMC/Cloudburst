package org.cloudburstmc.server.diagnostics;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.server.persistence.AtomicFiles;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Writes local failure reports without requiring an initialized or responsive server.
 */
@UtilityClass
public class CrashReports {
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss")
            .withZone(ZoneOffset.UTC);

    public static Path write(Path directory, Thread thread, Throwable failure) throws IOException {
        Instant now = Instant.now();
        StringWriter trace = new StringWriter();
        failure.printStackTrace(new PrintWriter(trace));
        StringBuilder report = new StringBuilder("# Crash report\n");
        report.append("Time: ").append(now).append('\n');
        report.append("Thread: ").append(thread.getName()).append('\n');
        report.append("\n# Failure\n").append(trace);
        RuntimeDiagnostics.append(report);
        report.append("\n# Thread dump\n").append(ThreadDump.capture());

        Path file = directory.resolve("crash-" + FILE_TIME.format(now) + "-" + UUID.randomUUID() + "-server.txt");
        AtomicFiles.write(file, report.toString());
        return file;
    }
}
