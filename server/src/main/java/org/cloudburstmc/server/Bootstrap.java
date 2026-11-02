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
import org.cloudburstmc.server.console.CloudConsole;
import org.cloudburstmc.server.diagnostics.CrashReports;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

/**
 * Configures the process, launches the server and reports startup failures.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Bootstrap {

    public static final String VERSION = readVersion();
    public static final String API_VERSION = "2.0.0";

    public static final Path PATH = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
    public static final long START_TIME = System.currentTimeMillis();

    public static int DEBUG = 1;
    private static @Nullable Level commandLineLogLevel;

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
        OptionSpec<Void> disableAnsi = parser.accepts("disable-ansi", "Disables console coloring");
        OptionSpec<Void> enableTitle = parser.accepts("enable-title", "Enables terminal title updates");
        OptionSpec<String> verbosity = parser.acceptsAll(List.of("v", "verbosity"), "Sets the log level")
                .withRequiredArg();
        OptionSpec<String> language = parser.accepts("language", "Sets the initial server language")
                .withRequiredArg();
        OptionSpec<Path> dataPath = parser.accepts("data-path", "Main server data directory")
                .withRequiredArg().withValuesConvertedBy(new PathConverter()).defaultsTo(PATH);
        OptionSpec<Path> pluginPath = parser.accepts("plugin-path", "Plugin directory")
                .withRequiredArg().withValuesConvertedBy(new PathConverter());
        OptionSpec<Path> levelPath = parser.accepts("level-path", "World directory")
                .withRequiredArg().withValuesConvertedBy(new PathConverter());

        OptionSet options = parser.parse(args);
        if (options.has(help)) {
            parser.printHelpOn(System.out);
            return Optional.empty();
        }

        if (!options.nonOptionArguments().isEmpty()) {
            throw new IllegalArgumentException("Unexpected arguments: " + options.nonOptionArguments());
        }

        Path data = options.valueOf(dataPath).toAbsolutePath().normalize();
        Path plugins = options.has(pluginPath) ? options.valueOf(pluginPath) : data.resolve("plugins");
        Path levels = options.has(levelPath) ? options.valueOf(levelPath) : data.resolve("worlds");
        Level logLevel = parseLogLevel(options.valueOf(verbosity));

        return Optional.of(new LaunchOptions(data, plugins.toAbsolutePath().normalize(),
                levels.toAbsolutePath().normalize(), options.valueOf(language), logLevel,
                options.has(disableAnsi), options.has(enableTitle)));
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
        titleDisabled = options.disableAnsi() || !options.enableTitle();
        commandLineLogLevel = options.logLevel();
        System.setProperty("cloudburst.disable-ansi", Boolean.toString(options.disableAnsi()));
        if (options.disableAnsi()) {
            System.setProperty("terminal.ansi", "false");
            System.setProperty("cloudburst.console-pattern", "%d{HH:mm:ss} [%-5level] %minecraftFormatting{%msg}{strip}%n");
        }

        System.setProperty("cloudburst.log-directory", options.dataPath().resolve("logs").toString());
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        context.reconfigure();
        configureLogLevel(DEBUG);

        InternalLoggerFactory.setDefaultFactory(Log4J2LoggerFactory.INSTANCE);
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED);
    }

    public static void configureLogLevel(int debugLevel) {
        DEBUG = Math.max(debugLevel, 1);
        Level configured = DEBUG >= 3 ? Level.TRACE : DEBUG == 2 ? Level.DEBUG : Level.INFO;
        setLogLevel(commandLineLogLevel != null ? commandLineLogLevel : configured);
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
            server = new CloudServer(options.dataPath(), options.pluginPath(), options.levelPath(), options.language());
            server.boot();
            return 0;
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

    private static String readVersion() {
        Properties properties = new Properties();
        try (InputStream stream = Bootstrap.class.getClassLoader().getResourceAsStream("git.properties")) {
            if (stream != null) {
                properties.load(stream);
            }
        } catch (IOException ignored) {
            return "git-null";
        }
        return "git-" + properties.getProperty("git.commit.id.abbrev", "null");
    }

    private record LaunchOptions(Path dataPath, Path pluginPath, Path levelPath, @Nullable String language, @Nullable Level logLevel, boolean disableAnsi, boolean enableTitle) {
    }
}
