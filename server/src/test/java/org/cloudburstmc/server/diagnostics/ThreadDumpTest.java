package org.cloudburstmc.server.diagnostics;

import org.junit.jupiter.api.Test;

import java.util.concurrent.FutureTask;

import static org.junit.jupiter.api.Assertions.*;

class ThreadDumpTest {

    @Test
    void capturesFramesBeyondTheThreadInfoSummary() throws Exception {
        FutureTask<String> task = new FutureTask<>(() -> captureDeepStack(20));
        Thread worker = Thread.ofPlatform().name("Deep stack diagnostic test").start(task);
        worker.join();
        String dump = task.get();
        assertTrue(dump.contains("\"Deep stack diagnostic test\""));
        assertTrue(dump.contains("Platform threads only. Virtual threads are not included."));
        long recursiveFrames = dump.lines().filter(line -> line.contains("ThreadDumpTest.captureDeepStack(")).count();
        assertEquals(21, recursiveFrames);
        assertFalse(dump.contains("\t..."));
    }

    private static String captureDeepStack(int depth) {
        return depth > 0 ? captureDeepStack(depth - 1) : ThreadDump.capture();
    }
}
