package org.cloudburstmc.server.diagnostics;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class TestLoggingConfigurationTest {

    @Test
    void testsUseConsoleAppendersInsteadOfServerLogFiles() {
        try (LoggerContext context = new LoggerContext("Cloudburst Test Logging")) {
            context.start();
            assertFalse(context.getConfiguration().getAppenders().isEmpty());
            context.getConfiguration().getAppenders().values().forEach(appender -> assertInstanceOf(ConsoleAppender.class, appender));
        }
    }
}
