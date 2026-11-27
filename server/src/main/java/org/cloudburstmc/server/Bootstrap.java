package org.cloudburstmc.server;

import io.netty.util.ResourceLeakDetector;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Log4J2LoggerFactory;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import joptsimple.util.PathConverter;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.server.configuration.validation.ConfigurationValidationException;
import org.cloudburstmc.server.console.CloudConsole;
import org.cloudburstmc.server.diagnostics.CrashReports;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

/**
 * Configures the process, launches the server and reports startup failures.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Bootstrap {

    @Getter
    private static boolean titleDisabled = true;

    static void main(String[] args) {
        configureRuntime();

        LaunchOptions options;
        try {
            Optional<LaunchOptions> parsed = parseArguments(args);
            if (parsed.isEmpty()) {
                return;
            }
            options = parsed.get();
        } catch (OptionException | IllegalArgumentException | IOException failure) {
            System.err.println("Unable to start Cloudburst: " + failure.getMessage());
            System.err.println("Use --help to view launch options.");
            System.exit(1);
            return;
        }

        int exitCode = 1;
        try {
            configureLogging(options);
            exitCode = runServer(options);
        } catch (Throwable failure) {
            System.err.println("Unable to launch Cloudburst");
            failure.printStackTrace(System.err);
        } finally {
            LogManager.shutdown();
        }

        System.exit(exitCode);
    }

    private static void configureRuntime() {
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err), true, StandardCharsets.UTF_8));
        Locale.setDefault(Locale.ENGLISH);

        System.setProperty("org.jline.terminal.disableDeprecatedProviderWarning", "true");
        System.setProperty("guice_bytecode_gen_option", "DISABLED");
        System.setProperty("leveldb.mmap", "true");
        System.getProperties().putIfAbsent("io.netty.allocator.type", "unpooled");
    }

    private static Optional<LaunchOptions> parseArguments(String[] args) throws IOException {
        OptionParser parser = new OptionParser();
        OptionSpec<Void> help = parser.accepts("help", "Shows this page").forHelp();
        OptionSpec<Void> noAnsi = parser.accepts("no-ansi", "Disables console coloring");
        OptionSpec<Void> title = parser.accepts("title", "Updates the terminal title");
        OptionSpec<String> logLevel = parser.accepts("log-level", "Sets the log level, such as info, debug or trace")
                .withRequiredArg();
        OptionSpec<String> language = parser.accepts("language", "Sets the initial server language")
                .withRequiredArg();
        OptionSpec<Path> dataDir = parser.accepts("data-dir", "Server data directory")
                .withRequiredArg().withValuesConvertedBy(new PathConverter()).defaultsTo(Path.of(""));
        OptionSpec<Path> pluginPath = parser.accepts("plugins", "Plugin directory")
                .withRequiredArg().withValuesConvertedBy(new PathConverter());
        OptionSpec<Path> levelPath = parser.accepts("world-container", "World container directory")
                .withRequiredArg().withValuesConvertedBy(new PathConverter());
        List<OptionSpec<String>> propertyOverrides = List.of(
                parser.accepts("server-ip", "Overrides server-ip in server.properties").withRequiredArg(),
                parser.accepts("server-port", "Overrides server-port in server.properties").withRequiredArg(),
                parser.accepts("online-mode", "Overrides online-mode in server.properties").withRequiredArg(),
                parser.accepts("max-players", "Overrides max-players in server.properties").withRequiredArg(),
                parser.accepts("level-name", "Overrides level-name in server.properties").withRequiredArg()
        );

        OptionSet options = parser.parse(args);
        if (options.has(help)) {
            parser.printHelpOn(System.out);
            return Optional.empty();
        }

        if (!options.nonOptionArguments().isEmpty()) {
            throw new IllegalArgumentException("Unexpected arguments: " + options.nonOptionArguments());
        }

        Path data = options.valueOf(dataDir).toAbsolutePath().normalize();
        Path plugins = options.has(pluginPath) ? options.valueOf(pluginPath) : data.resolve("plugins");
        Path levels = options.has(levelPath) ? options.valueOf(levelPath) : data.resolve("worlds");
        Map<String, String> properties = new LinkedHashMap<>();
        for (OptionSpec<String> override : propertyOverrides) {
            if (options.has(override)) {
                properties.put(override.options().getFirst(), options.valueOf(override));
            }
        }

        return Optional.of(new LaunchOptions(data, plugins.toAbsolutePath().normalize(),
                levels.toAbsolutePath().normalize(), Map.copyOf(properties), options.valueOf(language),
                parseLogLevel(options.valueOf(logLevel)), options.has(noAnsi), options.has(title))
        );
    }

    private static @Nullable Level parseLogLevel(@Nullable String value) {
        if (value == null) {
            return null;
        }

        Level level = Level.getLevel(value.toUpperCase(Locale.ROOT));
        if (level == null) {
            throw new IllegalArgumentException("Unknown log level: " + value);
        }

        return level;
    }

    private static void configureLogging(LaunchOptions options) {
        titleDisabled = options.noAnsi() || !options.title();
        System.setProperty("cloudburst.disable-ansi", Boolean.toString(options.noAnsi()));
        if (options.noAnsi()) {
            System.setProperty("terminal.ansi", "false");
            System.setProperty("cloudburst.console-pattern", "%d{HH:mm:ss} [%-5level] %minecraftFormatting{%msg}{strip}%n");
        }

        System.setProperty("cloudburst.log-directory", options.dataPath().resolve("logs").toString());
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        context.reconfigure();
        if (options.logLevel() != null) {
            setLogLevel(options.logLevel());
        }

        InternalLoggerFactory.setDefaultFactory(Log4J2LoggerFactory.INSTANCE);
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED);
    }

    private static void setLogLevel(Level level) {
        Objects.requireNonNull(level, "level");
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        context.getConfiguration().getRootLogger().setLevel(level);
        context.updateLoggers();
    }

    private static int runServer(LaunchOptions options) {
        Logger log = LogManager.getLogger(Bootstrap.class);
        ServerFileLayout files = new ServerFileLayout(options.dataPath(), options.levelPath());
        CloudServer server = null;
        try {
            CloudConsole.setWindowTitle("Cloudburst is starting up...");
            server = new CloudServer(options.dataPath(), options.pluginPath(), options.levelPath(), options.propertyOverrides(), options.language());
            server.boot();
            return 0;
        } catch (ConfigurationValidationException invalid) {
            log.fatal(invalid.getMessage());
            return 1;
        } catch (Throwable failure) {
            reportCrash(log, files, failure);
            return 1;
        } finally {
            if (server != null) {
                server.forceShutdown();
            }
            CloudConsole.setWindowTitle("Cloudburst stopped");
        }
    }

    private static void reportCrash(Logger log, ServerFileLayout files, Throwable failure) {
        log.fatal("Cloudburst crashed", failure);
        try {
            Path report = CrashReports.write(files.crashReports(), Thread.currentThread(), failure);
            log.fatal("Crash report saved to {}", report);
        } catch (Exception reportFailure) {
            log.error("Unable to write crash report", reportFailure);
        }
    }

    private record LaunchOptions(Path dataPath, Path pluginPath, Path levelPath, Map<String, String> propertyOverrides, @Nullable String language, @Nullable Level logLevel, boolean noAnsi, boolean title) {
    }
}
