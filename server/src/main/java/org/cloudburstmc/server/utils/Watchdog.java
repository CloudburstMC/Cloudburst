package org.cloudburstmc.server.utils;

import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.diagnostics.CrashReports;
import org.cloudburstmc.server.diagnostics.ThreadDump;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Monitors tick heartbeats without reading or mutating world state from the watchdog thread.
 */
@Log4j2
public class Watchdog extends Thread {

    private final CloudServer server;
    private final long timeoutNanos;
    private final CountDownLatch stopped = new CountDownLatch(1);

    public Watchdog(CloudServer server, long timeoutMillis) {
        if (timeoutMillis <= 0) {
            throw new IllegalArgumentException("Watchdog timeout must be positive");
        }
        this.server = Objects.requireNonNull(server, "server");
        this.timeoutNanos = TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        this.setName("Watchdog");
        this.setDaemon(true);
    }

    public void kill() {
        this.stopped.countDown();
    }

    @Override
    public void run() {
        boolean shutdownRequested = false;
        long shutdownAt = 0;
        long pollInterval = Math.min(this.timeoutNanos / 4, TimeUnit.SECONDS.toNanos(1));

        while (this.stopped.getCount() != 0) {
            long unresponsive = this.server.getUnresponsiveNanos();
            if (!shutdownRequested && unresponsive >= this.timeoutNanos) {
                long milliseconds = TimeUnit.NANOSECONDS.toMillis(unresponsive);
                log.fatal("Server has not responded for {} ms. Requesting shutdown.", milliseconds);
                log.fatal(ThreadDump.capture());

                try {
                    log.fatal("Crash report saved to {}", CrashReports.write(
                            this.server.getFiles().crashReports(), this.server.getPrimaryThread(),
                            new IllegalStateException("Server stopped responding for " + milliseconds + " ms")));
                } catch (Exception failure) {
                    log.error("Unable to write watchdog crash report", failure);
                }

                shutdownRequested = true;
                shutdownAt = System.nanoTime();
                this.server.shutdown();
            }

            if (shutdownRequested && System.nanoTime() - shutdownAt >= this.timeoutNanos) {
                log.fatal("Server did not complete watchdog shutdown. Terminating the process.");
                Runtime.getRuntime().halt(1);
            }

            try {
                if (this.stopped.await(pollInterval, TimeUnit.NANOSECONDS)) {
                    return;
                }
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
