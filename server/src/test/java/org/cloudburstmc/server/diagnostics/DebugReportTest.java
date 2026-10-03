package org.cloudburstmc.server.diagnostics;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class DebugReportTest {

    @Test
    void removesFormattingWithoutChangingLogContent(@TempDir Path directory) throws IOException {
        Files.createDirectories(directory.resolve("logs"));
        Files.writeString(directory.resolve("logs/server.log"),
                "Loading \u00a7aworld\u00a7r\n\u001b[32m[INFO] Ready\u001b[0m\n" + "\u00a7hStone\u00a7r and \u00a7ncopper\u00a7r\nA literal section sign: \u00a7\n");
        String report = DebugReport.collect("# Server\n", directory);
        assertTrue(report.contains("Loading world\n[INFO] Ready\nStone and copper\nA literal section sign: \u00a7\n"));
        assertFalse(report.contains("\u001b"));
    }

    @Test
    void redactsStructuredConfigurationAndIncludesTheRecentLog(@TempDir Path directory) throws IOException {
        String yaml = "service:\n  password: |\n    yaml-secret\n  port: 19132\n" + "services:\n  - api-key: service-secret\n    port: 19133\n";
        Files.writeString(directory.resolve("cloudburst.yml"), yaml);
        Files.writeString(directory.resolve("server.properties"), "rcon.password=properties-\\\n  secret\nserver-port=19132\n");
        Files.createDirectories(directory.resolve("logs"));
        Files.writeString(directory.resolve("logs/server.log"), "earlier-entry\n" + "\u00a7arecent-entry\u00a7r\n".repeat(5000));
        String report = DebugReport.collect("# Server\n", directory);
        assertFalse(report.contains("yaml-secret"));
        assertFalse(report.contains("properties-secret"));
        assertFalse(report.contains("service-secret"));
        assertFalse(report.contains("earlier-entry"));
        assertTrue(report.contains("19132"));
        assertTrue(report.contains("19133"));
        assertTrue(report.contains("recent-entry"));
        assertTrue(report.contains("[REDACTED]"));
        assertFalse(report.contains("\u00a7"));
        assertEquals(yaml, Files.readString(directory.resolve("cloudburst.yml")));
    }

    @Test
    void boundsBothReportSizeAndLineCount(@TempDir Path directory) {
        assertTrue(DebugReport.collect("", directory).startsWith("\n# cloudburst.yml\n"));
        String manyLines = DebugReport.collect("line\n".repeat(25_000), directory);
        assertTrue(manyLines.lines().count() <= 20_000);
        assertTrue(manyLines.endsWith("[Report truncated]\n"));
        String large = DebugReport.collect("x".repeat(3 * 1024 * 1024), directory);
        assertTrue(large.length() < 3 * 1024 * 1024);
        assertTrue(large.endsWith("[Report truncated]\n"));
    }

    @Test
    void doesNotSplitASurrogatePairAtTheSizeLimit(@TempDir Path directory) {
        String prefix = "x".repeat(2 * 1024 * 1024 - 1);
        assertEquals(prefix + "\n[Report truncated]\n", DebugReport.collect(prefix + "\ud83d\ude00", directory));
    }
}
