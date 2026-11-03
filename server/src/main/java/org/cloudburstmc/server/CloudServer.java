package org.cloudburstmc.server;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.spotify.futures.CompletableFutures;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.Server;
import org.cloudburstmc.api.ServerException;
import org.cloudburstmc.api.ban.BanList;
import org.cloudburstmc.api.boss.BossBar;
import org.cloudburstmc.api.boss.BossBarColor;
import org.cloudburstmc.api.boss.BossBarStyle;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.command.Commands;
import org.cloudburstmc.api.crafting.Recipe;
import org.cloudburstmc.api.entity.Attribute;
import org.cloudburstmc.api.event.server.*;
import org.cloudburstmc.api.level.Difficulty;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.permission.PermissionManager;
import org.cloudburstmc.api.player.GameMode;
import org.cloudburstmc.api.player.OfflinePlayer;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.registry.*;
import org.cloudburstmc.api.scheduler.AsyncScheduler;
import org.cloudburstmc.api.scheduler.GlobalScheduler;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.PlayerDataSerializer;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.adventure.BedrockLegacyTextSerializer;
import org.cloudburstmc.protocol.bedrock.data.skin.SerializedSkin;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayerListPacket;
import org.cloudburstmc.server.boss.CloudStandaloneBossBar;
import org.cloudburstmc.server.command.CloudConsoleCommandSender;
import org.cloudburstmc.server.configuration.ConfigurationTemplates;
import org.cloudburstmc.server.configuration.LevelDefinition;
import org.cloudburstmc.server.configuration.ServerConfigurations;
import org.cloudburstmc.server.configuration.type.LevelDimension;
import org.cloudburstmc.server.console.CloudConsole;
import org.cloudburstmc.server.crafting.CraftingManager;
import org.cloudburstmc.server.diagnostics.ServerExceptionHandler;
import org.cloudburstmc.server.diagnostics.ServerTickStatistics;
import org.cloudburstmc.server.diagnostics.spark.CloudSpark;
import org.cloudburstmc.server.event.CloudEventManager;
import org.cloudburstmc.server.inject.CloudburstModule;
import org.cloudburstmc.server.inject.CloudburstPrivateModule;
import org.cloudburstmc.server.level.*;
import org.cloudburstmc.server.level.storage.StorageIds;
import org.cloudburstmc.server.locale.LocaleManager;
import org.cloudburstmc.server.map.CloudMapManager;
import org.cloudburstmc.server.metrics.CloudMetrics;
import org.cloudburstmc.server.network.CloudNetwork;
import org.cloudburstmc.server.network.ProtocolInfo;
import org.cloudburstmc.server.pack.CloudPackManager;
import org.cloudburstmc.server.permission.DefaultPermissions;
import org.cloudburstmc.server.player.*;
import org.cloudburstmc.server.plugin.CloudPluginManager;
import org.cloudburstmc.server.plugin.loader.JavaPluginLoader;
import org.cloudburstmc.server.registry.*;
import org.cloudburstmc.server.scheduler.CloudAsyncScheduler;
import org.cloudburstmc.server.scheduler.CloudGlobalScheduler;
import org.cloudburstmc.server.utils.Watchdog;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Log4j2
public class CloudServer implements Server {

    @Getter
    private static CloudServer instance;

    private final Path filePath;
    private final Path dataPath;
    private final Path pluginPath;
    private final ServerFileLayout files;
    private final Thread currentThread;

    @Getter
    private final Injector injector;

    private final AtomicBoolean isRunning = new AtomicBoolean(true);
    private final AtomicBoolean shutdownStarted = new AtomicBoolean();
    private Watchdog watchdog;

    private ServerConfigurations configuration;
    private UUID serverID;
    private int maxPlayers;
    private Boolean getAllowFlight;
    private Difficulty difficulty;
    private GameMode defaultGamemode;

    private final CloudConsole console;
    private final Thread consoleThread;
    private final CloudConsoleCommandSender consoleSender;

    private final CloudPluginManager pluginManager;
    private final CloudEventManager eventManager;
    private final PermissionManager permissionManager;
    private final CraftingManager craftingManager;
    private final CloudPackManager packManager;

    private CloudGlobalScheduler globalScheduler;
    private AsyncScheduler asyncScheduler;

    private CloudNetwork network;
    private final Set<String> ignoredPackets = new HashSet<>();

    private final LevelManager levelManager;
    private CloudMapManager mapManager;
    private final CloudLevelDefaults levelDefaults = new CloudLevelDefaults();
    private volatile Identifier defaultStorageId;

    private final Map<SocketAddress, CloudPlayer> players = new HashMap<>();
    private final Map<UUID, CloudPlayer> playerList = new HashMap<>();

    private UserProfileCache profileCache;
    private PlayerDataSerializer playerDataSerializer;
    private final CloudPlayerDataStore playerDataStore = new CloudPlayerDataStore(this);

    private UserBanList playerBans;
    private IpBanList ipBans;
    private ServerOpList operators;
    private UserWhiteList whitelist;

    private int tickCounter;
    private final ServerTickLoop tickLoop = new ServerTickLoop();
    private final ServerTickStatistics tickStatistics = new ServerTickStatistics();

    private boolean autoSave = true;
    private int autoSaveTicker;

    private boolean forceLanguage;
    private String predefinedLanguage;
    private final LocaleManager localeManager = LocaleManager.from(
            "locale/cloudburst/languages.json",
            "locale/cloudburst/texts",
            "locale/vanilla"
    );

    private CloudSpark spark;

    private final CloudGameRuleRegistry gameRuleRegistry = CloudGameRuleRegistry.get();
    private final GeneratorRegistry generatorRegistry = GeneratorRegistry.get();
    private final StorageRegistry storageRegistry = StorageRegistry.get();
    private final BiomeRegistry biomeRegistry = CloudBiomeRegistry.get();

    private final CloudItemRegistry itemRegistry = CloudItemRegistry.get();
    private final CloudBlockRegistry blockRegistry = new CloudBlockRegistry(itemRegistry);
    private final CloudBlockEntityRegistry blockEntityRegistry = CloudBlockEntityRegistry.get();

    private final CloudDamageTypeRegistry damageTypeRegistry = CloudDamageTypeRegistry.get();
    private final CloudEffectRegistry effectRegistry = CloudEffectRegistry.get();
    private final CloudEnchantmentRegistry enchantmentRegistry = CloudEnchantmentRegistry.get();
    private final CloudRecipeRegistry recipeRegistry = CloudRecipeRegistry.get();
    private final CloudEntityRegistry entityRegistry = CloudEntityRegistry.get();
    private final CloudParticleRegistry particleRegistry = CloudParticleRegistry.get();

    private final CloudCommandRegistry commandRegistry = new CloudCommandRegistry(this.blockRegistry);

    public CloudServer(final Path dataPath, final Path pluginPath, final Path levelPath, final String predefinedLanguage) {
        Preconditions.checkState(instance == null, "Already initialized!");
        instance = this;
        currentThread = Thread.currentThread(); // Saves the current thread instance as a reference, used in Server#isPrimaryThread()

        this.injector = Guice.createInjector(Stage.PRODUCTION, new CloudburstPrivateModule(this), new CloudburstModule(this, levelPath));

        this.filePath = Bootstrap.PATH;
        this.dataPath = dataPath;
        this.files = new ServerFileLayout(dataPath, levelPath);
        this.pluginPath = pluginPath;
        this.predefinedLanguage = predefinedLanguage;

        this.pluginManager = injector.getInstance(CloudPluginManager.class);
        this.eventManager = injector.getInstance(CloudEventManager.class);
        this.permissionManager = injector.getInstance(PermissionManager.class);
        this.levelManager = injector.getInstance(LevelManager.class);
        this.craftingManager = injector.getInstance(CraftingManager.class);
        this.packManager = injector.getInstance(CloudPackManager.class);
        this.globalScheduler = injector.getInstance(CloudGlobalScheduler.class);
        this.asyncScheduler = injector.getInstance(CloudAsyncScheduler.class);

/*        this.playerMetadata = injector.getInstance(PlayerMetadataStore.class);
        this.levelMetadata = injector.getInstance(LevelMetadataStore.class);
        this.entityMetadata = injector.getInstance(EntityMetadataStore.class);*/

        this.consoleSender = injector.getInstance(CloudConsoleCommandSender.class);

        this.console = new CloudConsole(this);
        this.consoleThread = Thread.ofPlatform().name("Cloudburst Console").daemon().unstarted(this.console::start);
    }

    public static void broadcastPackets(Player[] players, BedrockPacket[] packets) {
        CloudServer.getInstance().batchPackets(players, packets);
    }

    public static void broadcastPacket(Player[] players, BedrockPacket packet) {
        CloudServer.getInstance().batchPackets(players, new BedrockPacket[]{packet});
    }

    public static void broadcastPacket(Set<CloudPlayer> players, BedrockPacket packet) {
        broadcastPacket(players.toArray(new CloudPlayer[0]), packet);
    }

    public void broadcastMessage(String message) {
        this.broadcastMessage(Component.text(message));
    }

    public void broadcastMessage(Component message) {
        this.broadcast(message, BROADCAST_CHANNEL_USERS);
    }

    public void broadcastMessage(String message, CommandSender[] recipients) {
        this.broadcastMessage(Component.text(message), recipients);
    }

    public void broadcastMessage(Component message, CommandSender[] recipients) {
        for (CommandSender recipient : recipients) {
            recipient.sendMessage(message);
        }
    }

    public void broadcastMessage(String message, Collection<? extends CommandSender> recipients) {
        this.broadcastMessage(Component.text(message), recipients);
    }

    public void broadcastMessage(Component message, Collection<? extends CommandSender> recipients) {
        for (CommandSender recipient : recipients) {
            recipient.sendMessage(message);
        }
    }

    public void broadcast(String message, String permission) {
        this.broadcast(Component.text(message), permission);
    }

    public void broadcast(Component message, String permission) {
        Set<CommandSender> recipients = new HashSet<>();
        if (this.consoleSender.hasPermission(permission)) {
            recipients.add(this.consoleSender);
        }

        for (CloudPlayer player : this.players.values()) {
            if (player.hasPermission(permission)) {
                recipients.add(player);
            }
        }

        for (CommandSender recipient : recipients) {
            recipient.sendMessage(message);
        }
    }

    public void boot() throws IOException {
        this.consoleThread.start();
        Path configPath = this.files.global();

        if (Files.notExists(configPath)) {
            log.info("Welcome! Please choose a language first!");

            for (Locale locale : localeManager.getAvailableLocales()) {
                log.info("{}: {}", locale.toString(), localeManager.getConsoleDisplayName(locale));
            }

            String locale;
            do {
                if (this.predefinedLanguage != null) {
                    locale = this.predefinedLanguage;
                    this.predefinedLanguage = null;
                } else {
                    locale = this.console.readStartupInput();
                }
            } while (!localeManager.setLocale(locale));

            ConfigurationTemplates.ensure(configPath, "cloudburst-global.yml", locale, true);
        }

        this.console.enableCommands();

        log.info("Loading {} ...", configPath);
        this.configuration = ServerConfigurations.load(this.files);
        this.levelManager.initializeExecutors(this.configuration.getChunkSystem());
        this.playerDataSerializer = new DefaultPlayerDataSerializer(this.getPlayerDataPath());
        this.profileCache = new UserProfileCache(this.files.userCache(),
                this.getConfig().getPlayer().getUserCacheSize(), this.getConfig().getPlayer().isSaveUserCacheOnStopOnly());
        this.mapManager = new CloudMapManager(this, this.files.savedData(this.configuration.getDefaultLevel()));

        ignoredPackets.addAll(getConfig().getDebug().getIgnoredPackets());

        Bootstrap.configureLogLevel(this.getConfig().getDebug().getLevel());
        log.debug("DataPath Directory: {}", this.dataPath);

        this.forceLanguage = getConfig().getSettings().isForceLanguage();
        this.localeManager.setLocaleOrFallback(getConfig().getSettings().getLanguage());
        Locale locale = this.getLanguage().getLocale();
        log.info(this.getLanguage().translate("cloudburst.language.selected", "§b" + locale.getDisplayCountry(locale) + "§r", locale));
        log.info(this.getLanguage().translate("cloudburst.server.start", "§b" + this.getVersion() + "§r"));

        this.operators = new ServerOpList(this.files.operators());
        this.whitelist = new UserWhiteList(this.files.whitelist());
        this.playerBans = new UserBanList(this.files.playerBans(), uuid -> this.profileCache.find(uuid).map(UserProfileCache.CachedProfile::name).orElse(null));
        this.ipBans = new IpBanList(this.files.ipBans());

        this.maxPlayers = this.configuration.getMaxPlayers();
        this.setAutoSave(this.configuration.isAutoSave());

        if (this.configuration.isHardcore() && this.getDifficulty() != Difficulty.HARD) {
            this.configuration.setDifficulty(Difficulty.HARD);
        }

        if (this.getConfig().getDebug().isUncaughtExceptionReports()) {
            Thread.setDefaultUncaughtExceptionHandler(new ServerExceptionHandler(this.files.crashReports()));
        }

        log.info(this.getLanguage().translate("cloudburst.server.info", this.getName(), "§e" + this.getImplementationVersion() + "§r", "", this.getApiVersion()));
        log.info(this.getLanguage().translate("cloudburst.server.license", this.getName()));

        // Initialize metrics
        new CloudMetrics(this);

        DefaultPermissions.registerCorePermissions(this.permissionManager);

        this.spark = new CloudSpark(this);
        this.spark.registerPermissions();
        this.commandRegistry.registerBuiltIns(this.spark, this.tickStatistics);

        this.pluginManager.registerLoader(JavaPluginLoader.class, JavaPluginLoader.builder().build());

        this.loadPlugins();

        this.eventManager.fire(new ServerInitializationEvent());
        this.eventManager.fire(new CommandRegistrationEvent(this.commandRegistry));

        // load packs before registry closes to register new blocks and after plugins to register block factories.
        this.loadPacks();

        // Close registries
        try {
            this.blockEntityRegistry.close();
            this.blockRegistry.close();
            this.damageTypeRegistry.close();
            this.effectRegistry.close();
            this.enchantmentRegistry.close();
            this.itemRegistry.close();
            this.recipeRegistry.close();
            this.entityRegistry.close();
            this.particleRegistry.close();
            this.biomeRegistry.close();
            this.gameRuleRegistry.close();
            this.generatorRegistry.close();
            this.storageRegistry.close();
            this.packManager.close();
            this.commandRegistry.close();
        } catch (RegistryException e) {
            throw new IllegalStateException("Unable to close registries", e);
        }

        this.registerVanillaComponents();
        this.eventManager.fire(new RegistriesClosedEvent(this.packManager));

        Identifier defaultStorageId = Identifier.parse(getConfig().getLevelStorage().getDefaultFormat());
        if (storageRegistry.isRegistered(defaultStorageId)) {
            this.defaultStorageId = defaultStorageId;
        } else {
            log.warn("Unknown default storage type. Reverting to 'minecraft:leveldb' instead");
            this.defaultStorageId = StorageIds.LEVELDB;
        }

        this.loadLevels();

        if (this.getDefaultLevel() == null) {
            log.fatal(this.getLanguage().translate("cloudburst.level.defaultError"));
            this.forceShutdown();

            return;
        }

        EnumLevel.initLevels();

        //TODO: event

        log.info(this.getLanguage().translate("cloudburst.server.networkStart", "§b" + (this.getIp().equals("") ? "*" : this.getIp()) + "§r", "§b" + this.getPort() + "§r"));
        this.serverID = UUID.randomUUID();

        this.network = new CloudNetwork(this);

        try {
            this.network.bind();
        } catch (Exception e) {
            log.fatal("Unable to start NetherNet on {}:{}", getIp(), getPort(), e);
            this.forceShutdown();
            throw new IllegalStateException("Unable to start the network listener", e);
        }

        try {
            if (Bootstrap.DEBUG < 2) {
                this.watchdog = new Watchdog(this, 60000);
                this.watchdog.start();
            }

            this.eventManager.fire(new ServerLoadEvent(ServerLoadType.STARTUP));
            this.start();
        } finally {
            this.forceShutdown();
        }
    }

    public void batchPackets(Player[] players, BedrockPacket[] packets) {
        this.batchPackets(players, packets, false);
    }

    public void batchPackets(Player[] players, BedrockPacket[] packets, boolean forceSync) {
        if (players == null || packets == null || players.length == 0 || packets.length == 0) {
            return;
        }

        for (Player player : players) {
            if (player.isConnected()) {
                for (BedrockPacket packet : packets) {
                    ((CloudPlayer) player).sendPacket(packet);
                }
            }
        }
    }

    public boolean dispatchCommand(CommandSender sender, String commandLine) throws ServerException {
        // First we need to check if this command is on the main thread or not, if not, warn the user
        if (!this.isPrimaryThread()) {
            log.warn("Command Dispatched Async: " + commandLine);
            log.warn("Please notify author of plugin causing this execution to fix this bug!", new Throwable());
            // TODO: We should sync the command to the main thread too!
        }
        if (sender == null) {
            throw new ServerException("CommandSender is not valid");
        }

        if (this.commandRegistry.dispatch(sender, commandLine)) {
            return true;
        }

        sender.sendMessage(Component.translatable("commands.generic.unknown", Component.text(commandLine)).color(NamedTextColor.RED));

        return false;
    }

    public CloudConsoleCommandSender getConsoleSender() {
        return consoleSender;
    }

    public void shutdown() {
        this.isRunning.set(false);
        this.tickLoop.wakeUp();
    }

    public void forceShutdown() {
        if (!this.isPrimaryThread()) {
            throw new IllegalStateException("Shutdown cleanup must run on the server thread");
        }

        if (!this.shutdownStarted.compareAndSet(false, true)) {
            return;
        }

        this.shutdown();
        shutdownStep("update terminal title", () -> CloudConsole.setWindowTitle("Stopping Cloudburst..."));
        if (this.network != null) {
            shutdownStep("stop accepting connections", this.network::stopAcceptingConnections);
        }

        shutdownStep("disconnect players", this::disconnectPlayers);
        shutdownStep("notify shutdown listeners", () -> this.eventManager.fire(new ServerShutdownEvent()));

        this.stopServices();
        this.saveAndCloseWorlds();
        this.closeServerResources();
    }

    private void disconnectPlayers() {
        for (CloudPlayer player : List.copyOf(this.players.values())) {
            shutdownStep("disconnect player " + player.getName(), () -> player.close(player.leaveMessage(), this.getConfig().getSettings().getShutdownMessage()));
        }
    }

    private void stopServices() {
        if (this.spark != null) {
            shutdownStep("stop spark", this.spark::close);
        }

        shutdownStep("deregister plugin loader", () -> this.pluginManager.deregisterLoader(JavaPluginLoader.class));
        shutdownStep("cancel global tasks", this.globalScheduler::cancelAllTasks);
        shutdownStep("cancel asynchronous tasks", this.asyncScheduler::cancelAllTasks);
    }

    private void saveAndCloseWorlds() {
        if (this.mapManager != null) {
            shutdownStep("save maps", this.mapManager::save);
        }
        shutdownStep("close worlds", this.levelManager::close);
    }

    private void closeServerResources() {
        shutdownStep("stop console", this.consoleThread::interrupt);
        shutdownStep("finish saving player data", this.playerDataStore::close);

        if (this.profileCache != null) {
            shutdownStep("save user profile cache", this.profileCache::close);
        }

        if (this.network != null) {
            shutdownStep("close network", this.network::close);
        }

        shutdownStep("release resource packs", this.packManager::shutdown);
        if (this.watchdog != null) {
            shutdownStep("stop watchdog", this.watchdog::kill);
        }
    }

    private static void shutdownStep(String action, Runnable task) {
        log.debug("Shutdown: {}", action);
        try {
            task.run();
        } catch (Throwable failure) {
            log.error("Unable to {} during shutdown", action, failure);
        }
    }

    private void start() {
        this.network.refreshStatus();
        this.network.startAcceptingConnections();

        String gameMode = this.getLanguage().translate(this.getGameMode().translationKey());
        log.info(this.getLanguage().translate("cloudburst.server.defaultGameMode", "§b" + gameMode + "§r"));

        double startupSeconds = (System.currentTimeMillis() - Bootstrap.START_TIME) / 1000d;
        log.info(this.getLanguage().translate("cloudburst.server.startFinished", String.format(Locale.ROOT, "%.2f", startupSeconds)));

        this.runTicks();
    }

    private void runTicks() {
        this.spark.enable();
        this.tickStatistics.start();
        try {
            this.tickLoop.run(this.isRunning::get, this::tick);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } finally {
            this.shutdown();
        }
    }

    private void tick(long nextTickDeadlineNanos) {
        long tickStartNanos = System.nanoTime();
        try {
            this.spark.tickStart();
            int tickNumber = this.tickCounter + 1;
            this.eventManager.fire(new ServerTickStartEvent(tickNumber));
            this.tickCounter = tickNumber;

            this.tickServer();

            long endNanos = System.nanoTime();
            double durationMillis = (endNanos - tickStartNanos) / 1_000_000d;
            this.eventManager.fire(new ServerTickEndEvent(tickNumber, durationMillis,
                    nextTickDeadlineNanos - endNanos));
        } finally {
            long durationNanos = System.nanoTime() - tickStartNanos;
            this.tickStatistics.recordTick(durationNanos);
            this.spark.tickEnd(durationNanos / 1_000_000d);
        }
    }

    private void tickServer() {
        this.globalScheduler.tick();
        if (!this.isRunning()) {
            return;
        }

        for (CloudPlayer player : List.copyOf(this.players.values())) {
            player.processNetworkInput();
        }

        if (!this.isRunning()) {
            return;
        }

        for (CloudPlayer player : List.copyOf(this.players.values())) {
            player.onUpdate(this.tickCounter);
        }

        this.levelManager.tick(this.tickCounter);
        this.mapManager.tick();

        for (CloudPlayer player : List.copyOf(this.players.values())) {
            player.flushNetworkUpdates();
        }

        if (this.tickCounter % 20 == 0) {
            this.network.refreshStatus();
        }

        int autoSaveInterval = this.configuration.getTicksPer().getAutosave();
        if (this.autoSave && autoSaveInterval > 0 && ++this.autoSaveTicker >= autoSaveInterval) {
            this.autoSaveTicker = 0;
            this.doAutoSave();
        }

        this.console.updateStatus();
    }

    public void doAutoSave() {
        if (!this.getAutoSave()) {
            return;
        }

        for (CloudPlayer player : List.copyOf(this.players.values())) {
            if (player.isOnline()) {
                player.save(true);
            } else if (!player.isConnected()) {
                this.removePlayer(player);
            }
        }

        this.levelManager.save();
        this.mapManager.save();
    }

    public long getUnresponsiveNanos() {
        return this.tickLoop.unresponsiveNanos();
    }

    public void onPlayerCompleteLoginSequence(Player player) {
        this.sendFullPlayerListData((CloudPlayer) player);
    }

    public void addPlayer(SocketAddress socketAddress, Player player) {
        this.players.put(socketAddress, (CloudPlayer) player);
    }

    public void addOnlinePlayer(Player player) {
        this.playerList.put(player.getServerId(), (CloudPlayer) player);
        this.updatePlayerListData(player.getServerId(), player.getUniqueId(),
                BedrockLegacyTextSerializer.getInstance().serialize(player.displayName()),
                ((CloudPlayer) player).getSerializedSkin(), player.getXuid(), this.playerList.values());
    }

    public void removeOnlinePlayer(Player player) {
        if (this.playerList.containsKey(player.getServerId())) {
            this.playerList.remove(player.getServerId());

            PlayerListPacket packet = new PlayerListPacket();
            PlayerListPacket.Entry entry = new PlayerListPacket.Entry(player.getServerId());
            entry.setAction(PlayerListPacket.Action.REMOVE);
            packet.getEntries().add(entry);

            CloudServer.broadcastPacket(this.playerList.values().toArray(new CloudPlayer[0]), packet);
        }
    }

    public void updatePlayerListData(UUID uuid, long entityId, String name, SerializedSkin skin) {
        this.updatePlayerListData(uuid, entityId, name, skin, "", this.playerList.values());
    }

    public void updatePlayerListData(UUID uuid, long entityId, String name, SerializedSkin skin, String xboxUserId) {
        this.updatePlayerListData(uuid, entityId, name, skin, xboxUserId, this.playerList.values());
    }

    public void updatePlayerListData(UUID uuid, long entityId, String name, SerializedSkin skin, CloudPlayer[] players) {
        this.updatePlayerListData(uuid, entityId, name, skin, "", players);
    }

    public void updatePlayerListData(UUID uuid, long entityId, String name, SerializedSkin skin, String xboxUserId, CloudPlayer[] players) {
        PlayerListPacket packet = new PlayerListPacket();
        PlayerListPacket.Entry entry = new PlayerListPacket.Entry(uuid);
        entry.setAction(PlayerListPacket.Action.ADD);
        entry.setEntityId(entityId);
        entry.setName(name);
        entry.setSkin(skin);
        entry.setXuid(xboxUserId);
        entry.setPlatformChatId("");
        entry.setColor(skin.getColor());
        packet.getEntries().add(entry);
        CloudServer.broadcastPacket(players, packet);
    }

    public void updatePlayerListData(UUID uuid, long entityId, String name, SerializedSkin skin, String xboxUserId, Collection<CloudPlayer> players) {
        this.updatePlayerListData(uuid, entityId, name, skin, xboxUserId,
                players.stream()
                        .filter(p -> !p.getServerId().equals(uuid))
                        .toArray(CloudPlayer[]::new));
    }

    public void removePlayerListData(UUID uuid) {
        this.removePlayerListData(uuid, this.playerList.values());
    }

    public void removePlayerListData(UUID uuid, CloudPlayer[] players) {
        PlayerListPacket packet = new PlayerListPacket();
        PlayerListPacket.Entry entry = new PlayerListPacket.Entry(uuid);
        entry.setAction(PlayerListPacket.Action.REMOVE);
        packet.getEntries().add(entry);
        CloudServer.broadcastPacket(players, packet);
    }

    public void removePlayerListData(UUID uuid, Collection<CloudPlayer> players) {
        this.removePlayerListData(uuid, players.toArray(new CloudPlayer[0]));
    }

    public void sendFullPlayerListData(CloudPlayer player) {
        PlayerListPacket packet = new PlayerListPacket();
        packet.getEntries().addAll(this.playerList.values().stream()
                .map(p -> {
                    PlayerListPacket.Entry entry = new PlayerListPacket.Entry(p.getServerId());
                    entry.setAction(PlayerListPacket.Action.ADD);
                    entry.setEntityId(p.getUniqueId());
                    entry.setName(BedrockLegacyTextSerializer.getInstance().serialize(p.displayName()));
                    entry.setSkin(p.getSerializedSkin());
                    entry.setXuid(p.getXuid());
                    entry.setPlatformChatId("");
                    entry.setColor(p.getSerializedSkin().getColor());
                    return entry;
                }).collect(Collectors.toList()));

        player.sendPacket(packet);
    }

    public void sendRecipeList(Player player) {
        this.craftingManager.sendRecipesTo((CloudPlayer) player);
    }

    public String getName() {
        return "Cloudburst";
    }

    public boolean isRunning() {
        return isRunning.get();
    }

    public String getImplementationVersion() {
        return Bootstrap.VERSION;
    }

    public String getVersion() {
        return ProtocolInfo.getDefaultMinecraftVersion();
    }

    public String getApiVersion() {
        return Bootstrap.API_VERSION;
    }

    public Path getFilePath() {
        return filePath;
    }

    public Path getDataPath() {
        return dataPath;
    }

    public Path getPluginPath() {
        return pluginPath;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public int getPort() {
        return this.configuration.getServerPort();
    }

    public int getViewDistance() {
        return this.configuration.getViewDistance();
    }

    public String getIp() {
        return this.configuration.getServerIp();
    }

    public UUID getServerUniqueId() {
        return this.serverID;
    }

    public boolean getAutoSave() {
        return this.autoSave;
    }

    public void setAutoSave(boolean autoSave) {
        this.autoSave = autoSave;
        for (CloudLevel level : this.levelManager.getLevels()) {
            level.setAutoSave(this.autoSave);
        }
    }

    public boolean getGenerateStructures() {
        return this.configuration.isGenerateStructures();
    }

    public GameMode getGameMode() {
        return this.configuration.getGamemode();
    }

    public boolean getForceGamemode() {
        return this.configuration.isForceGamemode();
    }

    public Difficulty getDifficulty() {
        if (this.difficulty == null) {
            this.difficulty = this.configuration.getDifficulty();
        }
        return this.difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.getConfig().setDifficulty(difficulty);
        this.difficulty = difficulty;
        this.levelDefaults.setDifficulty(difficulty);
        for (CloudLevel level : this.levelManager.getLevels()) {
            level.setDifficulty(difficulty);
        }
    }

    @Override
    public boolean hasWhitelist() {
        return this.configuration.isWhiteList();
    }

    public int getSpawnRadius() {
        return this.configuration.getSpawnProtection();
    }

    public boolean getAllowFlight() {
        if (getAllowFlight == null) {
            getAllowFlight = this.configuration.isAllowFlight();
        }

        return getAllowFlight;
    }

    public boolean isHardcore() {
        return this.configuration.isHardcore();
    }

    public GameMode getDefaultGamemode() {
        if (this.defaultGamemode == null) {
            this.defaultGamemode = this.getGameMode();
        }
        return this.defaultGamemode;
    }

    @Override
    public Component motd() {
        return Component.text(this.configuration.getMotd());
    }

    @Override
    public void motd(Component motd) {
        this.configuration.setMotd(BedrockLegacyTextSerializer.getInstance().serialize(motd));
    }

    @Override
    public Component subMotd() {
        return Component.text(this.configuration.getSubMotd());
    }

    @Override
    public void subMotd(Component subMotd) {
        this.configuration.setSubMotd(BedrockLegacyTextSerializer.getInstance().serialize(subMotd));
    }

/*    public EntityMetadataStore getEntityMetadata() {
        return entityMetadata;
    }

    public PlayerMetadataStore getPlayerMetadata() {
        return playerMetadata;
    }

    public LevelMetadataStore getLevelMetadata() {
        return levelMetadata;
    }*/

    public boolean getForceResources() {
        return this.configuration.isForceResources();
    }

    public CloudEventManager getEventManager() {
        return eventManager;
    }

    public CloudPluginManager getPluginManager() {
        return this.pluginManager;
    }

    public PermissionManager getPermissionManager() {
        return permissionManager;
    }

    public CraftingManager getCraftingManager() {
        return craftingManager;
    }

    public CloudPackManager getPackManager() {
        return packManager;
    }

    @Override
    public GlobalScheduler getGlobalScheduler() {
        return globalScheduler;
    }

    @Override
    public AsyncScheduler getAsyncScheduler() {
        return asyncScheduler;
    }

    public int getTick() {
        return tickCounter;
    }

    public ServerTickStatistics getTickStatistics() {
        return this.tickStatistics;
    }

    @Override
    public Commands commands() {
        return this.commandRegistry;
    }

    @Override
    public CloudMapManager maps() {
        return this.mapManager;
    }

    public CloudCommandRegistry getCommandRegistry() {
        return this.commandRegistry;
    }

    public Map<UUID, CloudPlayer> getOnlinePlayers() {
        return ImmutableMap.copyOf(playerList);
    }

    public void addRecipe(Recipe recipe) {
        this.recipeRegistry.register(recipe);
    }

    public Optional<Player> getPlayer(UUID uuid) {
        Preconditions.checkNotNull(uuid, "uuid");
        return Optional.ofNullable(playerList.get(uuid));
    }

    public Optional<UUID> lookupName(String name) {
        return this.profileCache.find(name);
    }

    public void updateName(UUID uuid, String name) {
        this.profileCache.add(uuid, name);
    }

    public ServerFileLayout getFiles() {
        return this.files;
    }

    public Path getPlayerDataPath() {
        return this.files.playerData(this.configuration.getDefaultLevel());
    }

    @Override
    public OfflinePlayer getOfflinePlayer(UUID uuid) {
        Preconditions.checkNotNull(uuid, "uuid");
        String name = this.profileCache.find(uuid).map(UserProfileCache.CachedProfile::name)
                .or(() -> this.operators.get(uuid).map(ServerOpListEntry::name))
                .or(() -> this.whitelist.get(uuid).map(UserWhiteListEntry::name))
                .or(() -> this.playerBans.findName(uuid)).orElse(null);
        return new CloudOfflinePlayer(this, uuid, name);
    }

    @Override
    public OfflinePlayer getOfflinePlayer(String name) {
        Preconditions.checkNotNull(name, "name");
        CloudPlayer player = this.getPlayerExact(name);
        if (player != null) {
            return new CloudOfflinePlayer(this, player.getServerId(), player.getName());
        }

        return this.lookupName(name)
                .or(() -> this.findListedIdentity(name))
                .<OfflinePlayer>map(uuid -> new CloudOfflinePlayer(this, uuid, name))
                .orElseGet(() -> new CloudOfflinePlayer(this, name));
    }

    private Optional<UUID> findListedIdentity(String name) {
        Set<UUID> matches = new HashSet<>(this.playerBans.findIdentities(name));
        for (ServerOpListEntry entry : this.operators.entries()) {
            if (entry.name() != null && entry.name().equalsIgnoreCase(name)) {
                matches.add(entry.uuid());
            }
        }

        for (UserWhiteListEntry entry : this.whitelist.entries()) {
            if (entry.name() != null && entry.name().equalsIgnoreCase(name)) {
                matches.add(entry.uuid());
            }
        }

        return matches.size() == 1 ? Optional.of(matches.iterator().next()) : Optional.empty();
    }

    public NbtMap getOfflinePlayerData(UUID uuid) {
        return this.getOfflinePlayerData(uuid, false);
    }

    public NbtMap getOfflinePlayerData(UUID uuid, boolean create) {
        Preconditions.checkNotNull(uuid, "uuid");
        return this.playerDataStore.read(uuid, create);
    }

    public NbtMap getOfflinePlayerData(String name) {
        return this.getOfflinePlayerData(name, false);
    }

    public NbtMap getOfflinePlayerData(String name, boolean create) {
        Preconditions.checkNotNull(name, "name");
        return this.playerDataStore.read(name, create);
    }

    public void saveOfflinePlayerData(UUID uuid, NbtMap tag) {
        this.saveOfflinePlayerData(uuid, tag, false);
    }

    public void saveOfflinePlayerData(String name, NbtMap tag) {
        this.saveOfflinePlayerData(name, tag, false);
    }

    public void saveOfflinePlayerData(UUID uuid, NbtMap tag, boolean async) {
        Preconditions.checkNotNull(uuid, "uuid");
        Preconditions.checkNotNull(tag, "tag");
        this.playerDataStore.save(uuid, tag, async);
    }

    public void saveOfflinePlayerData(String name, NbtMap tag, boolean async) {
        Preconditions.checkNotNull(name, "name");
        Preconditions.checkNotNull(tag, "tag");
        this.playerDataStore.save(name, tag, async);
    }

    public Player getPlayer(String name) {
        Player found = null;
        name = name.toLowerCase(Locale.ROOT);
        int delta = Integer.MAX_VALUE;
        for (Player player : this.getOnlinePlayers().values()) {
            if (player.getName().toLowerCase(Locale.ROOT).startsWith(name)) {
                int curDelta = player.getName().length() - name.length();
                if (curDelta < delta) {
                    found = player;
                    delta = curDelta;
                }
                if (curDelta == 0) {
                    break;
                }
            }
        }

        return found;
    }

    public CloudPlayer getPlayerExact(String name) {
        name = name.toLowerCase(Locale.ROOT);
        for (Player player : this.getOnlinePlayers().values()) {
            if (player.getName().toLowerCase(Locale.ROOT).equals(name)) {
                return (CloudPlayer) player;
            }
        }

        return null;
    }

    public Player[] matchPlayer(String partialName) {
        partialName = partialName.toLowerCase(Locale.ROOT);
        List<Player> matchedPlayer = new ArrayList<>();
        for (Player player : this.getOnlinePlayers().values()) {
            if (player.getName().toLowerCase(Locale.ROOT).equals(partialName)) {
                return new Player[]{player};
            } else if (player.getName().toLowerCase(Locale.ROOT).contains(partialName)) {
                matchedPlayer.add(player);
            }
        }

        return matchedPlayer.toArray(new Player[0]);
    }

    public void removePlayer(Player player) {
        CloudPlayer toRemove = this.players.remove(((CloudPlayer) player).getSocketAddress());
        if (toRemove != null) {
            return;
        }

        for (SocketAddress socketAddress : new ArrayList<>(this.players.keySet())) {
            CloudPlayer p = this.players.get(socketAddress);
            if (player == p) {
                this.players.remove(socketAddress);
                break;
            }
        }
    }

    @Override
    public Set<CloudLevel> getLevels() {
        return this.levelManager.getLevels();
    }

    public CloudLevel getDefaultLevel() {
        return this.levelManager.getDefaultLevel();
    }

    public void setDefaultLevel(CloudLevel level) {
        this.levelManager.setDefaultLevel(level);
    }

    public boolean isLevelLoaded(String name) {
        return this.getLevelByName(name) != null;
    }

    public CloudLevel getLevel(String id) {
        return this.levelManager.getLevel(id);
    }

    public CloudLevel getLevelByName(String name) {
        return this.levelManager.getLevelByName(name);
    }

    @Override
    public boolean unloadLevel(Level level) {
        if (!(level instanceof CloudLevel cloudLevel) || cloudLevel.getServer() != this) {
            throw new IllegalArgumentException("Level is not owned by this server");
        }

        return this.levelManager.unload(cloudLevel);
    }

    @Override
    public CloudLevelBuilder levelBuilder(String id) {
        return new CloudLevelBuilder(this, id);
    }

    public LocaleManager getLanguage() {
        return localeManager;
    }

    public boolean isLanguageForced() {
        return forceLanguage;
    }

    public CloudNetwork getNetwork() {
        return network;
    }

    public ServerConfigurations getConfig() {
        return this.configuration;
    }

    @Override
    public void reloadConfiguration() {
        if (!this.isPrimaryThread()) {
            throw new IllegalStateException("Server configuration must be reloaded on the server thread");
        }

        this.configuration.reload();

        for (CloudPlayer player : this.getOnlinePlayers().values()) {
            player.getChunkManager().queueNewChunks();
        }
    }

    @Override
    public BanList<UUID> getPlayerBanList() {
        return this.playerBans;
    }

    @Override
    public BanList<InetAddress> getIpBanList() {
        return this.ipBans;
    }

    public boolean isOperator(UUID uuid) {
        return this.operators.contains(uuid);
    }

    public void setOperator(UUID uuid, @Nullable String name, boolean value) {
        if (value == this.operators.contains(uuid)) {
            return;
        }

        if (value) {
            this.operators.add(new ServerOpListEntry(uuid, name, false));
        } else {
            this.operators.remove(uuid);
        }

        this.getPlayer(uuid).ifPresent(player -> ((CloudPlayer) player).refreshOperatorStatus());
    }

    public boolean isWhitelisted(UUID uuid) {
        return this.whitelist.contains(uuid);
    }

    public boolean mayJoin(UUID uuid) {
        return !this.hasWhitelist() || this.isOperator(uuid) || this.isWhitelisted(uuid);
    }

    public boolean canBypassPlayerLimit(UUID uuid) {
        return this.operators.canBypassPlayerLimit(uuid);
    }

    public void setWhitelisted(UUID uuid, @Nullable String name, boolean value) {
        if (value) {
            this.whitelist.add(new UserWhiteListEntry(uuid, name));
        } else {
            this.whitelist.remove(uuid);
        }
    }

    @Override
    public void setWhitelist(boolean enabled) {
        this.configuration.setWhitelist(enabled);
    }

    @Override
    public Set<OfflinePlayer> getWhitelistedPlayers() {
        Set<OfflinePlayer> players = new LinkedHashSet<>();
        for (UserWhiteListEntry entry : this.whitelist.entries()) {
            players.add(new CloudOfflinePlayer(this, entry.uuid(), entry.name()));
        }

        return Collections.unmodifiableSet(players);
    }

    @Override
    public Set<OfflinePlayer> getOperators() {
        Set<OfflinePlayer> players = new LinkedHashSet<>();
        for (ServerOpListEntry entry : this.operators.entries()) {
            players.add(new CloudOfflinePlayer(this, entry.uuid(), entry.name()));
        }

        return Collections.unmodifiableSet(players);
    }

    @Override
    public void reloadWhitelist() {
        this.whitelist.reload();
    }

    public boolean shouldSavePlayerData() {
        return this.getConfig().getPlayer().isSavePlayerData();
    }

    public int getPlayerSkinChangeCooldown() {
        return this.getConfig().getPlayer().getSkinChangeCooldown();
    }

    /**
     * Checks the current thread against the expected primary thread for the
     * server.
     * <p>
     * <b>Note:</b> this method should not be used to indicate the current
     * synchronized state of the runtime. A current thread matching the main
     * thread indicates that it is synchronized, but a mismatch does not
     * preclude the same assumption.
     *
     * @return true if the current thread matches the expected primary thread,
     * false otherwise
     */
    public final boolean isPrimaryThread() {
        return (Thread.currentThread() == currentThread);
    }

    public Thread getPrimaryThread() {
        return currentThread;
    }

    private void loadPlugins() {
        log.info("Loading Plugins...");
        try {
            Files.createDirectories(this.pluginPath);
            this.pluginManager.loadPlugins(this.pluginPath);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to load plugins from " + this.pluginPath, e);
        }
        log.info("Loaded §a{}§r plugins.", pluginManager.getAllPlugins().size());
    }

    private void loadPacks() {
        Path resourcePath = this.files.resourcePacks();
        if (Files.notExists(resourcePath)) {
            try {
                Files.createDirectory(resourcePath);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to create resource_packs directory");
            }
        }
        this.packManager.loadPacks(resourcePath);
    }

    private void registerVanillaComponents() {
        Attribute.init();
        this.levelDefaults.setDifficulty(this.getDifficulty());
        this.levelDefaults.getGameRules().loadFrom(this.gameRuleRegistry.getDefaultRules());
    }

    private void loadLevels() throws IOException {
        Path levelPath = this.levelManager.getLevelsPath();
        if (Files.notExists(levelPath)) {
            Files.createDirectories(levelPath);
        } else if (!Files.isDirectory(levelPath)) {
            throw new IllegalStateException("Level location " + levelPath + " is not a directory.");
        }

        Map<String, LevelDefinition> levelDefinitions = getConfig().getLevels();
        if (levelDefinitions.isEmpty()) {
            throw new IllegalStateException("No levels configured. Add a level to config/cloudburst-global.yml.");
        }

        String defaultName = this.configuration.getDefaultLevel();

        List<CloudLevelLoadRequest> requests = new ArrayList<>(levelDefinitions.size());
        for (String name : levelDefinitions.keySet()) {
            final LevelDefinition config = levelDefinitions.get(name);
            if (config.getDimension() == null) {
                throw new IllegalStateException("Level '" + name + "' does not declare a dimension in config/cloudburst-global.yml");
            }

            if (this.isDimensionDisabled(config.getDimension())) {
                log.info("Skipping world '{}' because its dimension is disabled", name);
                continue;
            }

            //fallback to level name if no seed is set
            Object seedObj = config.getSeed() == null ? name : config.getSeed();
            long seed;
            if (seedObj instanceof Number) {
                seed = ((Number) seedObj).longValue();
            } else if (seedObj instanceof String) {
                //this internally generates an MD5 hash of the seed string
                UUID uuid = UUID.nameUUIDFromBytes(((String) seedObj).getBytes(StandardCharsets.UTF_8));
                seed = uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits();
            } else {
                throw new IllegalStateException("Seed for world \"" + name + "\" is invalid: " + (seedObj == null ? "null" : seedObj.getClass().getCanonicalName()));
            }

            Identifier generator = Identifier.parse(config.getGenerator());
            String options = config.getOptions();

            requests.add(this.levelBuilder(name)
                    .seed(seed)
                    .dimension(switch (config.getDimension()) {
                        case OVERWORLD -> CloudLevel.DIMENSION_OVERWORLD;
                        case NETHER -> CloudLevel.DIMENSION_NETHER;
                        case THE_END -> CloudLevel.DIMENSION_THE_END;
                    })
                    .generator(generator == null ? this.generatorRegistry.getFallback() : generator)
                    .generatorOptions(options)
                    .importProfile(config.getImportProfile())
                    .build());
        }

        // Split any combined Bedrock import before a destination level opens its database.
        this.levelManager.prepareConfiguredLevels(requests, defaultName);
        List<CompletableFuture<CloudLevel>> levelFutures = new ArrayList<>(requests.size());
        for (CloudLevelLoadRequest request : requests) {
            levelFutures.add(this.levelManager.load(request));
        }

        // Wait for levels to load.
        CompletableFutures.allAsList(levelFutures).join();

        //set default level
        if (this.getDefaultLevel() == null) {
            CloudLevel defaultLevel = this.levelManager.getLevel(defaultName);
            if (defaultLevel == null) {
                throw new IllegalArgumentException("default-level refers to unknown level: \"" + defaultName + '"');
            }
            this.levelManager.setDefaultLevel(defaultLevel);
        }
    }

    private boolean isDimensionDisabled(LevelDimension dimension) {
        return switch (dimension) {
            case OVERWORLD -> false;
            case NETHER -> !this.configuration.isAllowNether();
            case THE_END -> !this.configuration.isAllowEnd();
        };
    }

    @Override
    public boolean isNetherAllowed() {
        return this.configuration.isAllowNether();
    }

    @Override
    public boolean isEndAllowed() {
        return this.configuration.isAllowEnd();
    }

    public PlayerDataSerializer getPlayerDataSerializer() {
        return playerDataSerializer;
    }

    public void setPlayerDataSerializer(PlayerDataSerializer playerDataSerializer) {
        this.playerDataSerializer = Preconditions.checkNotNull(playerDataSerializer, "playerDataSerializer");
    }

    public LevelManager getLevelManager() {
        return levelManager;
    }

    public CloudLevelDefaults getLevelDefaults() {
        return this.levelDefaults;
    }

    public Identifier getDefaultStorageId() {
        return defaultStorageId;
    }

    public StorageRegistry getStorageRegistry() {
        return storageRegistry;
    }

    @Override
    public GameRuleRegistry getGameRuleRegistry() {
        return gameRuleRegistry;
    }

    @Override
    public BossBar createBossBar(Component title, BossBarColor color, BossBarStyle style) {
        return new CloudStandaloneBossBar(title, color, style);
    }

    @Override
    public BlockEntityRegistry getBlockEntityRegistry() {
        return blockEntityRegistry;
    }

    @Override
    public CloudBlockRegistry getBlockRegistry() {
        return blockRegistry;
    }

    @Override
    public BiomeRegistry getBiomeRegistry() {
        return biomeRegistry;
    }

    @Override
    public DamageTypeRegistry getDamageTypeRegistry() {
        return damageTypeRegistry;
    }

    @Override
    public EffectRegistry getEffectRegistry() {
        return effectRegistry;
    }

    @Override
    public EnchantmentRegistry getEnchantmentRegistry() {
        return enchantmentRegistry;
    }

    @Override
    public EntityRegistry getEntityRegistry() {
        return entityRegistry;
    }

    @Override
    public ItemRegistry getItemRegistry() {
        return itemRegistry;
    }

    @Override
    public ParticleRegistry getParticleRegistry() {
        return particleRegistry;
    }

    @Override
    public RecipeRegistry getRecipeRegistry() {
        return recipeRegistry;
    }

    @Override
    public ResourcePackRegistry getResourcePackRegistry() {
        return packManager;
    }

    public GeneratorRegistry getGeneratorRegistry() {
        return this.generatorRegistry;
    }

    public boolean isIgnoredPacket(Class<? extends BedrockPacket> clazz) {
        return this.ignoredPackets.contains(clazz.getSimpleName());
    }

}
