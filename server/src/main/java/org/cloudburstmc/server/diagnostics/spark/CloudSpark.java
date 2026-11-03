package org.cloudburstmc.server.diagnostics.spark;

import me.lucko.spark.common.SparkBuildInfo;
import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.SparkPlugin;
import me.lucko.spark.common.command.Command;
import me.lucko.spark.common.monitor.ping.PlayerPingProvider;
import me.lucko.spark.common.platform.PlatformInfo;
import me.lucko.spark.common.platform.world.WorldInfoProvider;
import me.lucko.spark.common.sampler.ThreadDumper;
import me.lucko.spark.common.sampler.source.ClassSourceLookup;
import me.lucko.spark.common.sampler.source.SourceMetadata;
import me.lucko.spark.common.tick.TickHook;
import me.lucko.spark.common.tick.TickReporter;
import me.lucko.spark.common.util.classfinder.ClassFinder;
import net.kyori.adventure.text.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.permission.Permission;
import org.cloudburstmc.api.permission.PermissionDefault;
import org.cloudburstmc.api.plugin.PluginContainer;
import org.cloudburstmc.api.plugin.PluginDescription;
import org.cloudburstmc.server.CloudServer;

import java.nio.file.Path;
import java.security.CodeSource;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.stream.Stream;

/**
 * Hosts the bundled profiler and keeps its background work separate from level access.
 * Tick hooks and queued synchronous tasks are driven by the server thread.
 */
public class CloudSpark implements SparkPlugin, AutoCloseable {

    private static final Logger LOGGER = LogManager.getLogger("spark");

    private final CloudServer server;
    private final Object lifecycleLock = new Object();
    private final SparkTickMonitor ticks;
    private final SparkMainThreadExecutor synchronousTasks;
    private final SparkWorldInfoProvider worldInfo;
    private final Map<CommandSender, CloudSparkSender> commandSenders = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("Cloudburst Spark #", 0).factory());
    private final ThreadDumper threadDumper;
    private final SparkPlatform platform;
    private final List<String> permissions;

    private volatile List<CloudSparkSender> senders = List.of();
    private volatile Map<String, Integer> pings = Map.of();
    private volatile List<PluginContainer> plugins = List.of();
    private volatile boolean closed;
    private volatile boolean enabled;
    private boolean disposed;
    private int activeTasks;
    private int snapshotTicks;

    /**
     * Creates a profiler host without enabling monitoring.
     * Construction and enabling must run on the server thread.
     */
    public CloudSpark(CloudServer server) {
        this.server = Objects.requireNonNull(server, "server");
        this.ticks = new SparkTickMonitor(server.getPrimaryThread());
        this.synchronousTasks = new SparkMainThreadExecutor(server.getPrimaryThread());
        this.worldInfo = new SparkWorldInfoProvider(new CloudSparkWorldInfoProvider(server), this.synchronousTasks);
        this.threadDumper = new ThreadDumper.Specific(server.getPrimaryThread());
        this.platform = new SparkPlatform(this);
        this.permissions = this.platform.getCommandManager().getAllSparkPermissions().stream().sorted().toList();
    }

    /**
     * Returns Spark's root and subcommand permissions, available before monitoring starts.
     */
    public List<String> permissions() {
        return this.permissions;
    }

    /**
     * Returns Spark's registered commands for client syntax generation.
     */
    public List<Command> commands() {
        return List.copyOf(this.platform.getCommandManager().getCommands());
    }

    /**
     * Registers operator-only defaults while preserving Spark's individual permission names.
     */
    public void registerPermissions() {
        this.server.getPermissionManager().registerAll(this.permissions.stream()
                .map(name -> new Permission(name, "Allows using the spark profiler", PermissionDefault.OPERATORS))
                .toList());
    }

    /**
     * Enables monitoring when the server is ready to process ticks, unless disabled in configuration.
     */
    public void enable() {
        if (this.closed || this.enabled || !this.server.getConfig().getSpark().isEnabled()) {
            return;
        }

        this.refreshSnapshots();
        this.enabled = true;

        try {
            this.platform.enable();
        } catch (RuntimeException | Error failure) {
            try {
                this.close();
            } catch (RuntimeException cleanup) {
                failure.addSuppressed(cleanup);
            }
            throw failure;
        }
    }

    /**
     * Submits a command without blocking the server thread on profiling or uploads.
     */
    public void executeCommand(CommandSender sender, String[] arguments) {
        if (!this.enabled || this.closed) {
            sender.sendMessage(Component.text("The spark profiler is disabled."));
            return;
        }

        this.platform.executeCommand(this.sender(sender), arguments);
    }

    /**
     * Returns permission-filtered replacement tokens for the current command argument.
     */
    public List<String> complete(CommandSender sender, String[] arguments) {
        if (!this.enabled || this.closed) {
            return List.of();
        }

        return this.platform.tabCompleteCommand(this.sender(sender), arguments);
    }

    /**
     * Processes queued profiler tasks and delivers the tick-start callbacks on the server thread.
     */
    public void tickStart() {
        if (!this.enabled || this.closed) {
            return;
        }

        if (this.snapshotTicks++ % 20 == 0) {
            this.refreshSnapshots();
        }

        this.synchronousTasks.drain();
        runSafely(this.ticks::tickStart);
    }

    /**
     * Records active tick duration in milliseconds, excluding the tick pacing sleep.
     */
    public void tickEnd(double durationMillis) {
        if (this.enabled && !this.closed) {
            runSafely(() -> this.ticks.tickEnd(durationMillis));
        }
    }

    /**
     * Stops accepting work and interrupts workers. Profiler cleanup follows the last running task.
     * Queued synchronous work runs during server-thread shutdown and is discarded during
     * shutdown from another thread. Repeated calls have no effect.
     */
    @Override
    public void close() {
        boolean disposeNow;
        synchronized (this.lifecycleLock) {
            if (this.closed) {
                return;
            }

            this.closed = true;
            disposeNow = this.activeTasks == 0;
        }

        this.worldInfo.close();

        if (this.server.isPrimaryThread()) {
            this.synchronousTasks.close();
        } else {
            this.synchronousTasks.shutdownNow();
        }

        this.executor.shutdownNow();
        if (disposeNow) {
            this.disposePlatform();
        }

        try {
            if (!this.executor.awaitTermination(5, TimeUnit.SECONDS)) {
                LOGGER.warn("Spark tasks are still stopping after the shutdown timeout");
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public String getVersion() {
        return SparkBuildInfo.VERSION;
    }

    @Override
    public Path getPluginDirectory() {
        return this.server.getFiles().directory().resolve("spark");
    }

    @Override
    public String getCommandName() {
        return "spark";
    }

    @Override
    public Stream<CloudSparkSender> getCommandSenders() {
        return this.senders.stream();
    }

    @Override
    public void executeAsync(Runnable task) {
        if (this.closed) {
            throw new RejectedExecutionException("Spark is stopping");
        }

        this.executor.execute(() -> {
            synchronized (this.lifecycleLock) {
                if (this.closed) {
                    return;
                }
                this.activeTasks++;
            }

            try {
                runSafely(task);
            } finally {
                boolean dispose;
                synchronized (this.lifecycleLock) {
                    dispose = --this.activeTasks == 0 && this.closed;
                }
                if (dispose) {
                    this.disposePlatform();
                }
            }
        });
    }

    /**
     * Runs work immediately on the server thread or queues it for server-thread processing.
     *
     * @throws RejectedExecutionException if synchronous work is no longer accepted
     */
    @Override
    public void executeSync(Runnable task) {
        Objects.requireNonNull(task, "task");
        this.synchronousTasks.execute(() -> runSafely(task));
    }

    @Override
    public ThreadDumper getDefaultThreadDumper() {
        return this.threadDumper;
    }

    @Override
    public TickHook createTickHook() {
        return this.ticks;
    }

    @Override
    public TickReporter createTickReporter() {
        return this.ticks;
    }

    @Override
    public ClassSourceLookup createClassSourceLookup() {
        return type -> {
            CodeSource source = type.getProtectionDomain().getCodeSource();
            if (source == null || source.getLocation() == null || !source.getLocation().getProtocol().equals("file")) {
                return null;
            }

            Path location = Path.of(source.getLocation().toURI()).toAbsolutePath().normalize();
            return this.plugins.stream().map(PluginContainer::getDescription)
                    .filter(plugin -> plugin.getPath()
                            .map(path -> path.toAbsolutePath().normalize().equals(location)).orElse(false))
                    .map(PluginDescription::getId)
                    .findFirst().orElse(null);
        };
    }

    @Override
    public ClassFinder createClassFinder() {
        ClassFinder fallback = SparkPlugin.super.createClassFinder();
        return name -> {
            Class<?> found = fallback.findClass(name);
            if (found != null) {
                return found;
            }

            for (PluginContainer plugin : this.plugins) {
                try {
                    return Class.forName(name, false, plugin.getPlugin().getClass().getClassLoader());
                } catch (ClassNotFoundException ignored) {
                }
            }

            return null;
        };
    }

    @Override
    public Collection<SourceMetadata> getKnownSources() {
        return SourceMetadata.gather(this.plugins,
                plugin -> plugin.getDescription().getId(),
                plugin -> plugin.getDescription().getVersion(),
                plugin -> String.join(", ", plugin.getDescription().getAuthors()),
                plugin -> plugin.getDescription().getDescription().orElse(null));
    }

    @Override
    public PlayerPingProvider createPlayerPingProvider() {
        return () -> this.pings;
    }

    @Override
    public WorldInfoProvider createWorldInfoProvider() {
        return this.worldInfo;
    }

    @Override
    public PlatformInfo getPlatformInfo() {
        return new PlatformInfo() {
            @Override
            public Type getType() {
                return Type.SERVER;
            }

            @Override
            public String getName() {
                return "Cloudburst";
            }

            @Override
            public String getBrand() {
                return CloudSpark.this.server.getName();
            }

            @Override
            public String getVersion() {
                return CloudSpark.this.server.getImplementationVersion();
            }

            @Override
            public String getMinecraftVersion() {
                return CloudSpark.this.server.getVersion();
            }
        };
    }

    @Override
    public void log(Level level, String message) {
        LOGGER.log(logLevel(level), message);
    }

    @Override
    public void log(Level level, String message, Throwable failure) {
        LOGGER.log(logLevel(level), message, failure);
    }

    private void disposePlatform() {
        synchronized (this.lifecycleLock) {
            if (this.disposed) {
                return;
            }
            this.disposed = true;
        }

        try {
            if (this.enabled) {
                this.platform.disable();
            } else {
                this.platform.getCommandManager().close();
            }
        } catch (RuntimeException failure) {
            LOGGER.error("Unable to stop spark", failure);
        } finally {
            this.enabled = false;
            this.ticks.close();
            this.senders = List.of();
            this.commandSenders.clear();
            this.pings = Map.of();
            this.plugins = List.of();
        }
    }

    private void refreshSnapshots() {
        this.worldInfo.refresh();
        this.plugins = List.copyOf(this.server.getPluginManager().getAllPlugins());
        List<CommandSender> connected = Stream.concat(
                this.server.getOnlinePlayers().values().stream(),
                Stream.of(this.server.getConsoleSender())
        ).toList();
        this.senders = connected.stream().map(this::sender).toList();
        this.commandSenders.keySet().retainAll(connected);

        Map<String, Integer> snapshot = new LinkedHashMap<>();
        this.server.getOnlinePlayers().values().forEach(player -> snapshot.put(player.getName(), player.getPing()));
        this.pings = Map.copyOf(snapshot);
    }

    private CloudSparkSender sender(CommandSender sender) {
        return this.commandSenders.computeIfAbsent(sender, delegate -> new CloudSparkSender(this, delegate));
    }

    private static void runSafely(Runnable task) {
        try {
            task.run();
        } catch (Throwable failure) {
            LOGGER.error("Unable to execute spark task", failure);
        }
    }

    private static org.apache.logging.log4j.Level logLevel(Level level) {
        if (level.intValue() >= Level.SEVERE.intValue()) {
            return org.apache.logging.log4j.Level.ERROR;
        }

        if (level.intValue() >= Level.WARNING.intValue()) {
            return org.apache.logging.log4j.Level.WARN;
        }

        if (level.intValue() >= Level.INFO.intValue()) {
            return org.apache.logging.log4j.Level.INFO;
        }

        return org.apache.logging.log4j.Level.DEBUG;
    }
}
