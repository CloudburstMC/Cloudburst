package org.cloudburstmc.server.utils;

import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.diagnostics.CrashReports;
import org.cloudburstmc.server.diagnostics.ThreadDump;

@Log4j2
public class Watchdog extends Thread {

    private final CloudServer server;
    private final long time;
    public boolean running;
    private boolean responding = true;

    public Watchdog(CloudServer server, long time) {
        this.server = server;
        this.time = time;
        this.running = true;
        this.setName("Watchdog");
        this.setDaemon(true);
    }

    public void kill() {
        running = false;
        synchronized (this) {
            this.notifyAll();
        }
    }

    @Override
    public void run() {
        while (this.running) {
            long current = server.getNextTick();
            if (current != 0) {
                long diff = System.currentTimeMillis() - current;
                if (!responding && diff > time * 2) {
                    System.exit(1); // Kill the server if it gets stuck on shutdown
                }

                if (server.isRunning() && diff > time) {
                    if (responding) {

                        log.fatal("--------- Server stopped responding --------- (" + Math.round(diff / 1000d) + "s)");
                        log.fatal("Please report this to Cloudburst:");
                        log.fatal(" - https://github.com/CloudburstMC/Server/issues/new");
                        log.fatal(ThreadDump.capture());

                        try {
                            log.fatal("Crash report saved to {}", CrashReports.write(
                                    server.getFiles().crashReports(), this,
                                    new IllegalStateException("Server stopped responding for " + diff + "ms")));
                        } catch (Exception failure) {
                            log.error("Unable to write watchdog crash report", failure);
                        }

                        responding = false;
                        this.server.forceShutdown();
                    }
                } else {
                    responding = true;
                }
            }

            try {
                synchronized (this) {
                    this.wait(Math.max(time / 4, 1000));
                }
            } catch (InterruptedException ignore) {
            }
        }
    }
}
