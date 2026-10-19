package org.cloudburstmc.server.diagnostics;

import lombok.extern.log4j.Log4j2;

import java.nio.file.Path;
import java.util.Objects;

@Log4j2
public class ServerExceptionHandler implements Thread.UncaughtExceptionHandler {

    private final Path reports;

    public ServerExceptionHandler(Path reports) {
        this.reports = Objects.requireNonNull(reports, "reports");
    }

    public static void dispatch(Thread thread, Throwable failure) {
        Thread.UncaughtExceptionHandler handler = Thread.getDefaultUncaughtExceptionHandler();
        if (handler != null) {
            handler.uncaughtException(thread, failure);
        } else {
            log.error("Uncaught exception in thread {}", thread.getName(), failure);
        }
    }

    @Override
    public void uncaughtException(Thread thread, Throwable failure) {
        log.error("Uncaught exception in thread {}", thread.getName(), failure);
        try {
            log.error("Crash report saved to {}", CrashReports.write(reports, thread, failure));
        } catch (Exception reportFailure) {
            log.error("Unable to write crash report in {}", reports, reportFailure);
        }
    }
}
