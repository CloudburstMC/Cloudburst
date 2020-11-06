package org.cloudburstmc.server;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.common.base.Preconditions;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Log4J2LoggerFactory;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import joptsimple.util.PathConverter;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.cloudburstmc.server.utils.ServerKiller;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;

/*
 *    _____ _                 _ _                    _
 *   / ____| |               | | |                  | |
 *  | |    | | ___  _   _  __| | |__  _   _ _ __ ___| |_
 *  | |    | |/ _ \| | | |/ _` | '_ \| | | | '__/ __| __|
 *  | |____| | (_) | |_| | (_| | |_) | |_| | |  \__ \ |_
 *   \_____|_|\___/ \__,_|\__,_|_.__/ \__,_|_|  |___/\__|
 */

/**
 * The entry point of Cloudburst CloudServer.
 */
@Log4j2
public class Bootstrap {
    public final static Properties GIT_INFO = getGitInfo();
    public final static String VERSION = getVersion();
    public final static String API_VERSION = "2.0.0";

    public final static Path PATH = Paths.get(System.getProperty("user.dir"));
    public static final JsonMapper JSON_MAPPER = new JsonMapper();
    public static final YAMLMapper YAML_MAPPER = new YAMLMapper();
    public static final YAMLMapper KEBAB_CASE_YAML_MAPPER = (YAMLMapper) new YAMLMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategy.KEBAB_CASE);
    public static final JavaPropsMapper JAVA_PROPS_MAPPER = (JavaPropsMapper) new JavaPropsMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategy.KEBAB_CASE);
    public static final long START_TIME = System.currentTimeMillis();
    public static boolean ANSI = true;
    public static boolean TITLE = false;
    public static boolean shortTitle = requiresShortTitle();
    public static int DEBUG = 1;

    public static void main(String[] args) {
        Locale.setDefault(Locale.ENGLISH);
        System.setProperty("log4j.skipJansi", "false");

        YAML_MAPPER.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

        // Force Mapped ByteBuffers for LevelDB till fixed.
        System.setProperty("leveldb.mmap", "true");

        System.getProperties().putIfAbsent("io.netty.allocator.type", "unpooled");

        // Netty logger for debug info
        InternalLoggerFactory.setDefaultFactory(Log4J2LoggerFactory.INSTANCE);
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED);

        // Define args
        OptionParser parser = new OptionParser();
        parser.allowsUnrecognizedOptions();

        OptionSpec<Void> helpSpec = parser.accepts("help", "Shows this page").forHelp();
        OptionSpec<Void> ansiSpec = parser.accepts("disable-ansi", "Disables console coloring");
        OptionSpec<Void> titleSpec = parser.accepts("enable-title", "Enables title at the top of the window");
        OptionSpec<String> verbositySpec = parser.acceptsAll(Arrays.asList("v", "verbosity"), "Set verbosity of logging").withRequiredArg().ofType(String.class);
        OptionSpec<String> languageSpec = parser.accepts("language", "Set a predefined language").withOptionalArg().ofType(String.class);
        OptionSpec<Path> dataPathSpec = parser.accepts("data-path", "path of main server data e.g. plexus.yml")
                .withRequiredArg()
                .withValuesConvertedBy(new PathConverter())
                .defaultsTo(PATH);
        OptionSpec<Path> pluginPathSpec = parser.accepts("plugin-path", "path to your plugins directory")
                .withRequiredArg()
                .withValuesConvertedBy(new PathConverter());
        OptionSpec<Path> levelPathSpec = parser.accepts("level-path", "path to your plugins directory")
                .withRequiredArg()
                .withValuesConvertedBy(new PathConverter());


        // Parse arguments
        OptionSet options = parser.parse(args);

        if (options.has(helpSpec)) {
            try {
                // Display help page
                parser.printHelpOn(System.out);
            } catch (IOException ignored) {
            }
            return;
        }

        Path dataPath = options.valueOf(dataPathSpec);

        Path pluginPath;
        if (options.has(pluginPathSpec)) {
            pluginPath = options.valueOf(pluginPathSpec);
        } else {
            pluginPath = dataPath.resolve("plugins");
        }

        Path levelPath;
        if (options.has(levelPathSpec)) {
            levelPath = options.valueOf(levelPathSpec);
        } else {
            levelPath = dataPath.resolve("worlds");
        }

        ANSI = !options.has(ansiSpec);
        TITLE = options.has(titleSpec);

        String verbosity = options.valueOf(verbositySpec);
        if (verbosity != null) {
            try {
                Level level = Level.valueOf(verbosity);
                setLogLevel(level);
            } catch (Exception e) {
                // ignore
            }
        }

        String language = options.valueOf(languageSpec);

        CloudServer server = new CloudServer(dataPath, pluginPath, levelPath, language);

        try {
            if (TITLE) {
                System.out.print((char) 0x1b + "]0;Cloudburst is starting up..." + (char) 0x07);
            }
            server.boot();
        } catch (Throwable t) {
            log.fatal("Cloudburst crashed", t);
        }

        if (TITLE) {
            System.out.print((char) 0x1b + "]0;Stopping CloudServer..." + (char) 0x07);
        }
        log.info("Stopping other threads");

        for (Thread thread : java.lang.Thread.getAllStackTraces().keySet()) {
            if (!(thread instanceof InterruptibleThread)) {
                continue;
            }
            log.debug("Stopping {} thread", thread.getClass().getSimpleName());
            if (thread.isAlive()) {
                thread.interrupt();
            }
        }

        ServerKiller killer = new ServerKiller(8);
        killer.start();

        if (TITLE) {
            System.out.print((char) 0x1b + "]0;Server Stopped" + (char) 0x07);
        }

        // Make sure all log messages have made it to their destination before ending the process,
        // especially since we use async logging.
        LogManager.shutdown();
    }

    /**
     * Checks if the shorter version of the window title should be used.
     *
     * The longer window title also contains the upload and download
     * speeds, in KB/s, for the network.
     *
     * @return true if the os is windows 8 or windows server 2020
     */
    private static boolean requiresShortTitle() {
        // Shorter title for windows 8/2012
        String osName = System.getProperty("os.name").toLowerCase();
        return osName.contains("windows") && (osName.contains("windows 8") || osName.contains("2012"));
    }

    /**
     * Returns the git information from this build of Cloudburst.
     *
     * The information is created by the "git-commit-id-plugin" maven
     * plugin and is saved in the resources directory.
     *
     * @return The git information as a {@link Properties} object
     */
    private static Properties getGitInfo() {
        InputStream gitFileStream = Bootstrap.class.getClassLoader().getResourceAsStream("git.properties");
        if (gitFileStream == null) {
            return null;
        }
        Properties properties = new Properties();
        try {
            properties.load(gitFileStream);
        } catch (IOException e) {
            return null;
        }
        return properties;
    }

    /**
     * Returns the git commit that this build of Cloudburst was built on.
     *
     * @return The git commit hash, if found, prefixed with "git-", else "git-null"
     */
    private static String getVersion() {
        StringBuilder version = new StringBuilder();
        version.append("git-");
        String commitId;
        if (GIT_INFO == null || (commitId = GIT_INFO.getProperty("git.commit.id.abbrev")) == null) {
            return version.append("null").toString();
        }
        return version.append(commitId).toString();
    }

    public static Level getLogLevel() {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration log4jConfig = ctx.getConfiguration();
        LoggerConfig loggerConfig = log4jConfig.getLoggerConfig(org.apache.logging.log4j.LogManager.ROOT_LOGGER_NAME);
        return loggerConfig.getLevel();
    }

    public static void setLogLevel(Level level) {
        Preconditions.checkNotNull(level, "level");
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration log4jConfig = ctx.getConfiguration();
        LoggerConfig loggerConfig = log4jConfig.getLoggerConfig(org.apache.logging.log4j.LogManager.ROOT_LOGGER_NAME);
        loggerConfig.setLevel(level);
        ctx.updateLoggers();
    }
}
