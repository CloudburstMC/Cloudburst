package org.cloudburstmc.server.diagnostics;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CrashReportsTest {

    @Test
    void writesAUniqueLocalReportWithoutAServerInstance(@TempDir Path directory) throws Exception {
        Path reports = directory.resolve("crash-reports");
        Path file = CrashReports.write(reports, Thread.currentThread(), new IllegalStateException("Example failure"));
        String report = Files.readString(file);

        assertEquals(reports, file.getParent());
        assertTrue(file.getFileName().toString().endsWith("-server.txt"));
        assertTrue(report.contains("Example failure"));
        assertTrue(report.contains("# Runtime"));
        assertTrue(report.contains("# Thread dump"));
    }
}
