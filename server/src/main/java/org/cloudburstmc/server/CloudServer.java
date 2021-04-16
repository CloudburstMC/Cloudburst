package org.cloudburstmc.server;

import co.aikar.timings.Timing;
import co.aikar.timings.Timings;
import com.dosse.upnp.UPnP;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Guice;
import com.google.inject.Stage;
import com.nukkitx.nbt.*;
import com.nukkitx.protocol.bedrock.BedrockPacket;
import com.nukkitx.protocol.bedrock.data.skin.SerializedSkin;
import com.nukkitx.protocol.bedrock.packet.PlayerListPacket;
import com.spotify.futures.CompletableFutures;
import io.netty.buffer.ByteBuf;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import net.daporkchop.ldbjni.LevelDB;
import org.cloudburstmc.api.Server;
import org.cloudburstmc.api.ServerException;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.entity.Attribute;
import org.cloudburstmc.api.event.server.*;
import org.cloudburstmc.api.inventory.Recipe;
import org.cloudburstmc.api.level.Difficulty;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.locale.TextContainer;
import org.cloudburstmc.api.permission.Permissible;
import org.cloudburstmc.api.player.GameMode;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.registry.ItemRegistry;
import org.cloudburstmc.api.registry.RecipeRegistry;
import org.cloudburstmc.api.registry.RegistryException;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.PlayerDataSerializer;
import org.cloudburstmc.server.command.ConsoleCommandSender;
import org.cloudburstmc.server.config.CloudburstYaml;
import org.cloudburstmc.server.config.ServerConfig;
import org.cloudburstmc.server.config.ServerProperties;
import org.cloudburstmc.server.console.NukkitConsole;
import org.cloudburstmc.server.crafting.CraftingManager;
import org.cloudburstmc.server.event.CloudEventManager;
import org.cloudburstmc.server.inject.CloudburstModule;
import org.cloudburstmc.server.inject.CloudburstPrivateModule;
import org.cloudburstmc.server.level.*;
import org.cloudburstmc.server.level.storage.StorageIds;
import org.cloudburstmc.server.locale.LocaleManager;
import org.cloudburstmc.server.locale.TranslationContainer;
import org.cloudburstmc.server.math.NukkitMath;
import org.cloudburstmc.server.network.BedrockInterface;
import org.cloudburstmc.server.network.Network;
import org.cloudburstmc.server.network.ProtocolInfo;
import org.cloudburstmc.server.network.SourceInterface;
import org.cloudburstmc.server.network.query.QueryHandler;
import org.cloudburstmc.server.pack.PackManager;
import org.cloudburstmc.server.permission.BanEntry;
import org.cloudburstmc.server.permission.BanList;
import org.cloudburstmc.server.permission.CloudPermissionManager;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.player.OfflinePlayer;
import org.cloudburstmc.server.plugin.CloudPluginManager;
import org.cloudburstmc.server.plugin.loader.JavaPluginLoader;
import org.cloudburstmc.server.registry.*;
import org.cloudburstmc.server.scheduler.ServerScheduler;
import org.cloudburstmc.server.scheduler.Task;
import org.cloudburstmc.server.utils.*;
import org.cloudburstmc.server.utils.bugreport.ExceptionHandler;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author MagicDroidX
 * @author Box
 */
@Log4j2
public class CloudServer implements Server {

    private static CloudServer instance = null;

    private BanList banByName;

    private BanList banByIP;

    private Config operators;

    private Config whitelist;

    private final AtomicBoolean isRunning = new AtomicBoolean(true);

    private boolean hasStopped = false;

    private final CloudPluginManager pluginManager;

    private final CloudEventManager eventManager;

    private final CloudPermissionManager permissionManager;

    private final int profilingTickrate = 20;

    private ServerScheduler scheduler;

    private int tickCounter;

    private long nextTick;

    private final float[] tickAverage = {20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20};

    private final float[] useAverage = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    private float maxTick = 20;

    private float maxUse = 0;

    private int sendUsageTicker = 0;

    private final boolean dispatchSignals = false;

    private final NukkitConsole console;
    private final ConsoleThread consoleThread;

    private final CraftingManager craftingManager;

    private final PackManager packManager;

    private final ConsoleCommandSender consoleSender;

    private int maxPlayers;

    private boolean autoSave = true;

/*    private final EntityMetadataStore entityMetadata;

    private final PlayerMetadataStore playerMetadata;

    private final LevelMetadataStore levelMetadata;*/

    private Network network;

    private boolean networkCompressionAsync = true;
    public int networkCompressionLevel = 7;

    private boolean upnpEnabled = false;
    private boolean autoTickRate = true;
    private int autoTickRateLimit = 20;
    private boolean alwaysTickPlayers = false;
    private int baseTickRate = 1;
    private Boolean getAllowFlight = null;
    private Difficulty difficulty = null;
    private GameMode defaultGamemode = null;

    private int autoSaveTicker = 0;
    private int autoSaveTicks = 6000;

    private boolean forceLanguage = false;

    private UUID serverID;

    private final LevelManager levelManager;

    private final Path filePath;
    private final Path dataPath;
    private final Path pluginPath;

    private final Set<UUID> uniquePlayers = new HashSet<>();

    private QueryHandler queryHandler;

    private QueryRegenerateEvent queryRegenerateEvent;
    private CloudburstYaml cloudburstYaml;

    private final LocaleManager localeManager = LocaleManager.from("locale/cloudburst/languages.json",
            "locale/cloudburst/texts", "locale/vanilla");
    private final CloudGameRuleRegistry gameRuleRegistry = CloudGameRuleRegistry.get();
    private final GeneratorRegistry generatorRegistry = GeneratorRegistry.get();
    private final StorageRegistry storageRegistry = StorageRegistry.get();
    private final EnchantmentRegistry enchantmentRegistry = EnchantmentRegistry.get();
    private final CloudBlockRegistry blockRegistry = CloudBlockRegistry.get();
    private final BlockEntityRegistry blockEntityRegistry = BlockEntityRegistry.get();
    private final CloudItemRegistry itemRegistry = CloudItemRegistry.get();
    private final CloudRecipeRegistry recipeRegistry = CloudRecipeRegistry.get();
    private final EntityRegistry entityRegistry = EntityRegistry.get();
    private final BiomeRegistry biomeRegistry = BiomeRegistry.get();
    private final CommandRegistry commandRegistry = CommandRegistry.get();

    private final Map<InetSocketAddress, CloudPlayer> players = new HashMap<>();

    private final Map<UUID, CloudPlayer> playerList = new HashMap<>();
    private final LevelData defaultLevelData = new LevelData();
    private String predefinedLanguage;

    private boolean allowNether;

    private final Thread currentThread;

    private Watchdog watchdog;

    private DB nameLookup;

    private PlayerDataSerializer playerDataSerializer = new DefaultPlayerDataSerializer(this);
    private ServerProperties serverProperties;

    private volatile Identifier defaultStorageId;

    private final Set<String> ignoredPackets = new HashSet<>();

    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}.dat$", Pattern.CASE_INSENSITIVE);

    public CloudServer(final Path dataPath, final Path pluginPath, final Path levelPath, final String predefinedLanguage) {
        Preconditions.checkState(instance == null, "Already initialized!");
        instance = this;
        currentThread = Thread.currentThread(); // Saves the current thread instance as a reference, used in Server#isPrimaryThread()

        val injector = Guice.createInjector(Stage.PRODUCTION, new CloudburstPrivateModule(this), new CloudburstModule(this, dataPath, pluginPath, levelPath));

        this.filePath = Bootstrap.PATH;
        this.dataPath = dataPath;
        this.pluginPath = pluginPath;
        this.predefinedLanguage = predefinedLanguage;

        this.pluginManager = injector.getInstance(CloudPluginManager.class);
        this.eventManager = injector.getInstance(CloudEventManager.class);
        this.permissionManager = injector.getInstance(CloudPermissionManager.class);
        this.levelManager = injector.getInstance(LevelManager.class);
        this.craftingManager = injector.getInstance(CraftingManager.class);
        this.packManager = injector.getInstance(PackManager.class);
        this.scheduler = injector.getInstance(ServerScheduler.class);

/*        this.playerMetadata = injector.getInstance(PlayerMetadataStore.class);
        this.levelMetadata = injector.getInstance(LevelMetadataStore.class);
        this.entityMetadata = injector.getInstance(EntityMetadataStore.class);*/

        this.consoleSender = injector.getInstance(ConsoleCommandSender.class);

        this.console = new NukkitConsole(this);
        this.consoleThread = new ConsoleThread();
    }

    public static void broadcastPackets(CloudPlayer[] players, BedrockPacket[] packets) {
        CloudServer.getInstance().batchPackets(players, packets);
    }

    public static void broadcastPacket(CloudPlayer[] players, BedrockPacket packet) {
        CloudServer.getInstance().batchPackets(players, new BedrockPacket[]{packet});
    }

    public int broadcastMessage(String message) {
        return this.broadcast(message, BROADCAST_CHANNEL_USERS);
    }

    public int broadcastMessage(TextContainer message) {
        return this.broadcast(message, BROADCAST_CHANNEL_USERS);
    }

    public int broadcastMessage(String message, CommandSender[] recipients) {
        for (CommandSender recipient : recipients) {
            recipient.sendMessage(message);
        }

        return recipients.length;
    }

    public int broadcastMessage(String message, Collection<? extends CommandSender> recipients) {
        for (CommandSender recipient : recipients) {
            recipient.sendMessage(message);
        }

        return recipients.size();
    }

    public int broadcastMessage(TextContainer message, Collection<? extends CommandSender> recipients) {
        for (CommandSender recipient : recipients) {
            recipient.sendMessage(message);
        }

        return recipients.size();
    }

    public int broadcast(String message, String permissions) {
        Set<CommandSender> recipients = new HashSet<>();

        for (String permission : permissions.split(";")) {
            for (Permissible permissible : this.permissionManager.getPermissionSubscriptions(permission)) {
                if (permissible instanceof CommandSender && permissible.hasPermission(permission)) {
                    recipients.add((CommandSender) permissible);
                }
            }
        }

        for (CommandSender recipient : recipients) {
            recipient.sendMessage(message);
        }

        return recipients.size();
    }

    public int broadcast(TextContainer message, String permissions) {
        Set<CommandSender> recipients = new HashSet<>();

        for (String permission : permissions.split(";")) {
            for (Permissible permissible : this.permissionManager.getPermissionSubscriptions(permission)) {
                if (permissible instanceof CommandSender && permissible.hasPermission(permission)) {
                    recipients.add((CommandSender) permissible);
                }
            }
        }

        for (CommandSender recipient : recipients) {
            recipient.sendMessage(message);
        }

        return recipients.size();
    }

    public static void broadcastPacket(Set<CloudPlayer> players, BedrockPacket packet) {
        broadcastPacket(players.toArray(new CloudPlayer[0]), packet);
    }

    public void boot() throws IOException {
        // Create directories
        Path playerPath = dataPath.resolve("players");

        if (Files.notExists(playerPath)) {
            Files.createDirectory(playerPath);
        } else if (!Files.isDirectory(playerPath)) {
            throw new RuntimeException("Players path " + playerPath + " is not a directory.");
        }

        this.consoleThread.start();
        Path configPath = dataPath.resolve("cloudburst.yml");

        if (Files.notExists(configPath)) {
            log.info(TextFormat.GREEN + "Welcome! Please choose a language first!");

            for (Locale locale : localeManager.getAvailableLocales()) {
                log.info("{}: {}", locale.toString(), locale.getDisplayName(locale));
            }

            String locale;
            do {
                if (this.predefinedLanguage != null) {
                    locale = this.predefinedLanguage;
                    this.predefinedLanguage = null;
                } else {
                    locale = this.console.readLine();
                }
            } while (!localeManager.setLocale(locale));

            // Generate config with specified locale
            LocaleManager configLocaleManager = LocaleManager.from("locale/cloudburst/languages.json",
                    "locale/cloudburst/configs");
            configLocaleManager.setLocale(locale);

            File configFile = configPath.toFile();
            InputStream stream = Bootstrap.class.getClassLoader().getResourceAsStream("cloudburst.yml");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                 BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(configFile)))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.isEmpty()) {
                        line = configLocaleManager.translate(line);
                    }
                    writer.write(line + '\n');
                }
            }
        }

        this.console.setExecutingCommands(true);

        log.info("Loading {} ...", TextFormat.GREEN + "cloudburst.yml" + TextFormat.WHITE);
        this.cloudburstYaml = CloudburstYaml.fromFile(configPath);

        ignoredPackets.addAll(getConfig().getDebug().getIgnoredPackets());

        Bootstrap.DEBUG = Math.max(getConfig().getDebug().getLevel(), 1);

        int logLevel = (Bootstrap.DEBUG + 3) * 100;
        for (org.apache.logging.log4j.Level level : org.apache.logging.log4j.Level.values()) {
            if (level.intLevel() == logLevel) {
                if (level.intLevel() > Bootstrap.getLogLevel().intLevel()) {
                    Bootstrap.setLogLevel(level);
                }
                break;
            }
        }
        log.debug("DataPath Directory: {}", this.dataPath);

        log.info("Loading {} ...", TextFormat.GREEN + "server.properties" + TextFormat.WHITE);
        Path serverPropPath = this.dataPath.resolve("server.properties");
        if (!Files.exists(serverPropPath)) {
            serverProperties = new ServerProperties();
            serverProperties.setPath(serverPropPath);
            serverProperties.save();
        } else {
            serverProperties = ServerProperties.fromFile(serverPropPath);
        }

        // Allow Nether? (determines if we create a nether world if one doesn't exist on startup)
        this.allowNether = this.serverProperties.isAllowNether();

        this.forceLanguage = getConfig().getSettings().isForceLanguage();
        this.localeManager.setLocaleOrFallback(getConfig().getSettings().getLanguage());
        Locale locale = this.getLanguage().getLocale();
        log.info(this.getLanguage().translate("cloudburst.language.selected", locale.getDisplayCountry(locale), locale));
        log.info(this.getLanguage().translate("cloudburst.server.start", TextFormat.AQUA + this.getVersion() + TextFormat.RESET));

        Object poolSize = getConfig().getSettings().getAsyncWorkers();
        if (!(poolSize instanceof Integer)) {
            try {
                poolSize = Integer.valueOf((String) poolSize);
            } catch (Exception e) {
                poolSize = -1;
            }
        }
        int parallelism = (int) poolSize;
        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", String.valueOf(parallelism));
        System.setProperty("java.util.concurrent.ForkJoinPool.common.exceptionHandler", "org.cloudburstmc.server.scheduler.ServerScheduler.ExceptionHandler");
        log.debug("Async pool parallelism: {}", parallelism == -1 ? "auto" : parallelism);

//        this.networkZlibProvider = this.getConfig("network.zlib-provider", 2);
//        Zlib.setProvider(this.networkZlibProvider);

        this.networkCompressionLevel = getConfig().getNetwork().getCompressionLevel();
        this.networkCompressionAsync = getConfig().getNetwork().isAsyncCompression();

        this.autoTickRate = getConfig().getLevelSettings().isAutoTickRate();
        this.autoTickRateLimit = getConfig().getLevelSettings().getAutoTickRateLimit();
        this.alwaysTickPlayers = getConfig().getLevelSettings().isAlwaysTickPlayers();
        this.baseTickRate = getConfig().getLevelSettings().getBaseTickRate();

        this.operators = new Config(this.dataPath.resolve("ops.txt").toFile(), Config.ENUM);
        this.whitelist = new Config(this.dataPath.resolve("white-list.txt").toFile(), Config.ENUM);
        this.banByName = new BanList(this.dataPath.resolve("banned-players.json").toString());
        this.banByName.load();
        this.banByIP = new BanList(this.dataPath.resolve("banned-ips.json").toString());
        this.banByIP.load();

        this.maxPlayers = this.serverProperties.getMaxPlayers();
        this.setAutoSave(this.serverProperties.isAutoSave());

        if (this.serverProperties.isHardcore() && this.getDifficulty() != Difficulty.HARD) {
            this.serverProperties.modifyDifficulty(Difficulty.HARD);
        }

        if (this.getConfig().getDebug().isBugReport()) {
            ExceptionHandler.registerExceptionHandler();
        }

        log.info(this.getLanguage().translate("cloudburst.server.info", this.getName(), TextFormat.YELLOW + this.getImplementationVersion() + TextFormat.WHITE, TextFormat.AQUA + "" + TextFormat.WHITE, this.getApiVersion()));
        log.info(this.getLanguage().translate("cloudburst.server.license", this.getName()));

        // Convert legacy data before plugins get the chance to mess with it.
        try {
            nameLookup = LevelDB.PROVIDER.open(playerPath.toFile(), new Options()
                    .createIfMissing(true)
                    .compressionType(CompressionType.ZLIB_RAW));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.commandRegistry.registerVanilla();

        this.convertLegacyPlayerData();

        this.permissionManager.subscribeToPermission(CloudServer.BROADCAST_CHANNEL_ADMINISTRATIVE, this.consoleSender);

        this.pluginManager.registerLoader(JavaPluginLoader.class, JavaPluginLoader.builder().build());

        this.queryRegenerateEvent = new QueryRegenerateEvent(this, 5);

        this.loadPlugins();

        this.eventManager.fire(ServerInitializationEvent.INSTANCE);

        // load packs before registry closes to register new blocks and after plugins to register block factories.
        this.loadPacks();

        // Close registries
        try {
            this.blockEntityRegistry.close();
            this.blockRegistry.close();
            this.enchantmentRegistry.close();
            this.itemRegistry.close();
            this.recipeRegistry.close();
            this.entityRegistry.close();
            this.biomeRegistry.close();
            this.gameRuleRegistry.close();
            this.generatorRegistry.close();
            this.storageRegistry.close();
            this.packManager.closeRegistration();
            this.commandRegistry.close();
        } catch (RegistryException e) {
            throw new IllegalStateException("Unable to close registries", e);
        } finally {
            this.eventManager.fire(new RegistriesClosedEvent(this.packManager));
        }

        this.registerVanillaComponents();

        Identifier defaultStorageId = Identifier.fromString(getConfig().getLevelSettings().getDefaultFormat());
        if (storageRegistry.isRegistered(defaultStorageId)) {
            this.defaultStorageId = defaultStorageId;
        } else {
            log.warn("Unknown default storage type. Reverting to 'minecraft:leveldb' instead");
            this.defaultStorageId = StorageIds.LEVELDB;
        }

        this.loadLevels();

        this.serverProperties.save();

        if (this.getDefaultLevel() == null) {
            log.fatal(this.getLanguage().translate("cloudburst.level.defaultError"));
            this.forceShutdown();

            return;
        }

        EnumLevel.initLevels();

        if (this.getConfig().getTicksPer().getAutosave() > 0) {
            this.autoSaveTicks = this.getConfig().getTicksPer().getAutosave();
        }

        //TODO: event

        log.info(this.getLanguage().translate("cloudburst.server.networkStart", this.getIp().equals("") ? "*" : this.getIp(), this.getPort()));
        this.serverID = UUID.randomUUID();

        this.network = new Network(this);
        this.network.setName(this.getMotd());
        this.network.setSubName(this.getSubMotd());

        try {
            this.network.registerInterface(new BedrockInterface(this));
        } catch (Exception e) {
            log.fatal("**** FAILED TO BIND TO " + getIp() + ":" + getPort() + "!");
            log.fatal("Perhaps a server is already running on that port?");
            this.forceShutdown();
        }

        if (Bootstrap.DEBUG < 2) {
            this.watchdog = new Watchdog(this, 60000);
            this.watchdog.start();
        }

        if (this.getConfig().getSettings().isUpnp()) {
            if (UPnP.isUPnPAvailable()) {
                log.debug(this.getLanguage().translate("cloudburst.server.upnp.enabled"));
                if (UPnP.openPortUDP(getPort(), "Cloudburst")) {
                    this.upnpEnabled = true; // Saved to disable the port-forwarding on shutdown
                    log.info(this.getLanguage().translate("cloudburst.server.upnp.success", getPort()));
                } else {
                    this.upnpEnabled = false;
                    log.warn("cloudburst.server.upnp.fail");
                }
            } else {
                this.upnpEnabled = false;
                log.warn(this.getLanguage().translate("cloudburst.server.upnp.unavailable"));
            }
        } else {
            this.upnpEnabled = false;
            log.debug(this.getLanguage().translate("cloudburst.server.upnp.disabled"));
        }

        this.eventManager.fire(ServerStartEvent.INSTANCE);

        this.start();
    }

    public void batchPackets(Player[] players, BedrockPacket[] packets) {
        this.batchPackets(players, packets, false);
    }

    public void batchPackets(Player[] players, BedrockPacket[] packets, boolean forceSync) {
        if (players == null || packets == null || players.length == 0 || packets.length == 0) {
            return;
        }

        try (Timing ignored = Timings.playerNetworkSendTimer.startTiming()) {
            for (Player p : players) {
                if (p.isConnected()) {
                    for (BedrockPacket packet : packets) {
                        ((CloudPlayer) p).sendPacket(packet);
                    }
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

        sender.sendMessage(new TranslationContainer(TextFormat.RED + "%commands.generic.unknown", commandLine));

        return false;
    }

    //todo: use ticker to check console
    public ConsoleCommandSender getConsoleSender() {
        return consoleSender;
    }

    public void shutdown() {
        isRunning.compareAndSet(true, false);
    }

    public void forceShutdown() {
        if (this.hasStopped) {
            return;
        }

        try {
            isRunning.compareAndSet(true, false);

            this.hasStopped = true;

            for (CloudPlayer player : new ArrayList<>(this.players.values())) {
                player.close(player.getLeaveMessage(), this.getConfig().getSettings().getShutdownMessage());
            }

            this.eventManager.fire(ServerShutdownEvent.INSTANCE);

            this.pluginManager.deregisterLoader(JavaPluginLoader.class);

            log.debug("Stopping all tasks");
            this.scheduler.cancelAllTasks();
            this.scheduler.mainThreadHeartbeat(Integer.MAX_VALUE);

            log.debug("Unloading all levels");
            this.levelManager.close();

            log.debug("Closing console");
            this.consoleThread.interrupt();

            if (this.upnpEnabled) {
                log.debug("Closing UPnP port");
                if (UPnP.closePortUDP(this.getPort())) {
                    log.info(this.getLanguage().translate("cloudburst.server.upnp.closed"));
                }
            }

            log.debug("Stopping network interfaces");
            for (SourceInterface interfaz : this.network.getInterfaces()) {
                interfaz.shutdown();
                this.network.unregisterInterface(interfaz);
            }

            if (nameLookup != null) {
                nameLookup.close();
            }

            log.debug("Disabling timings");
            Timings.stopServer();
            if (this.watchdog != null) {
                this.watchdog.kill();
            }
            //todo other things
        } catch (Exception e) {
            log.fatal("Exception happened while shutting down", e);
        }
    }

    public void start() {
        if (this.serverProperties.isEnableQuery()) {
            this.queryHandler = new QueryHandler();
        }

        for (BanEntry entry : this.getIPBans().getEntires().values()) {
            try {
                this.network.blockAddress(InetAddress.getByName(entry.getName()));
            } catch (UnknownHostException e) {
                // ignore
            }
        }

        //todo send usage setting
        this.tickCounter = 0;

        log.info(this.getLanguage().translate("cloudburst.server.defaultGameMode", this.getGamemode().getTranslation()));

        log.info(this.getLanguage().translate("cloudburst.server.startFinished", (System.currentTimeMillis() - Bootstrap.START_TIME) / 1000d));

        this.tickProcessor();
        this.forceShutdown();
    }

    public void handlePacket(InetSocketAddress address, ByteBuf payload) {
        try {
            if (!payload.isReadable(3)) {
                return;
            }
            byte[] prefix = new byte[2];
            payload.readBytes(prefix);

            if (!Arrays.equals(prefix, new byte[]{(byte) 0xfe, (byte) 0xfd})) {
                return;
            }
            if (this.queryHandler != null) {
                this.queryHandler.handle(address, payload);
            }
        } catch (Exception e) {
            log.error("Error whilst handling packet", e);

            this.network.blockAddress(address.getAddress());
        }
    }

    private int lastLevelGC;

    public void tickProcessor() {
        this.nextTick = System.currentTimeMillis();
        try {
            while (this.isRunning.get()) {
                try {
                    this.tick();

                    long next = this.nextTick;
                    long current = System.currentTimeMillis();

                    if (next - 0.1 > current) {
                        long allocated = next - current - 1;

                        if (allocated > 0) {
                            Thread.sleep(allocated, 900000);
                        }
                    }
                } catch (RuntimeException e) {
                    log.error("Error whilst ticking server", e);
                }
            }
        } catch (Throwable e) {
            log.fatal("Exception happened while ticking server", e);
            log.fatal(Utils.getAllThreadDumps());
        }
    }

    public void onPlayerCompleteLoginSequence(Player player) {
        this.sendFullPlayerListData((CloudPlayer) player);
    }

    public void onPlayerLogin(Player player) {
        if (this.sendUsageTicker > 0) {
            this.uniquePlayers.add(player.getServerId());
        }
    }

    public void addPlayer(InetSocketAddress socketAddress, Player player) {
        this.players.put(socketAddress, (CloudPlayer) player);
    }

    public void addOnlinePlayer(Player player) {
        this.playerList.put(player.getServerId(), (CloudPlayer) player);
        this.updatePlayerListData(player.getServerId(), player.getUniqueId(), player.getDisplayName(), ((CloudPlayer) player).getSerializedSkin(), player.getXuid());
    }

    public void removeOnlinePlayer(Player player) {
        if (this.playerList.containsKey(player.getServerId())) {
            this.playerList.remove(player.getServerId());

            PlayerListPacket packet = new PlayerListPacket();
            packet.setAction(PlayerListPacket.Action.REMOVE);
            packet.getEntries().add(new PlayerListPacket.Entry(player.getServerId()));

            CloudServer.broadcastPacket((Set<CloudPlayer>) this.playerList.values(), packet);
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
        packet.setAction(PlayerListPacket.Action.ADD);
        PlayerListPacket.Entry entry = new PlayerListPacket.Entry(uuid);
        entry.setEntityId(entityId);
        entry.setName(name);
        entry.setSkin(skin);
        entry.setXuid(xboxUserId);
        entry.setPlatformChatId("");
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
        packet.setAction(PlayerListPacket.Action.REMOVE);
        packet.getEntries().add(new PlayerListPacket.Entry(uuid));
        CloudServer.broadcastPacket(players, packet);
    }

    public void removePlayerListData(UUID uuid, Collection<CloudPlayer> players) {
        this.removePlayerListData(uuid, players.toArray(new CloudPlayer[0]));
    }

    public void sendFullPlayerListData(CloudPlayer player) {
        PlayerListPacket packet = new PlayerListPacket();
        packet.setAction(PlayerListPacket.Action.ADD);
        packet.getEntries().addAll(this.playerList.values().stream()
                .map(p -> {
                    PlayerListPacket.Entry entry = new PlayerListPacket.Entry(p.getServerId());
                    entry.setEntityId(p.getUniqueId());
                    entry.setName(p.getDisplayName());
                    entry.setSkin(p.getSerializedSkin());
                    entry.setXuid(p.getXuid());
                    entry.setPlatformChatId("");
                    return entry;
                }).collect(Collectors.toList()));

        player.sendPacket(packet);
    }

    public void sendRecipeList(Player player) {
        this.craftingManager.sendRecipesTo((CloudPlayer) player);
    }

    private void checkTickUpdates(int currentTick, long tickTime) {
        for (Player p : new ArrayList<>(this.players.values())) {
            /*if (!p.loggedIn && (tickTime - p.creationTime) >= 10000 && p.kick(PlayerKickEvent.Reason.LOGIN_TIMEOUT, "Login timeout")) {
                continue;
            }

            client freezes when applying resource packs
            todo: fix*/
            p.onUpdate(currentTick);
        }
    }

    public void doAutoSave() {
        if (this.getAutoSave()) {
            try (Timing ignored = Timings.levelSaveTimer.startTiming()) {
                for (Player player : new ArrayList<>(this.players.values())) {
                    if (player.isOnline()) {
                        player.save(true);
                    } else if (!player.isConnected()) {
                        this.removePlayer(player);
                    }
                }

                this.levelManager.save();
            }
        }
    }

    private boolean tick() {
        long tickTime = System.currentTimeMillis();

        // TODO
        long time = tickTime - this.nextTick;
        if (time < -25) {
            try {
                Thread.sleep(Math.max(5, -time - 25));
            } catch (InterruptedException e) {
                log.error("Server interrupted whilst sleeping", e);
            }
        }

        long tickTimeNano = System.nanoTime();
        if ((tickTime - this.nextTick) < -25) {
            return false;
        }

        try (Timing ignored = Timings.fullServerTickTimer.startTiming()) {

            ++this.tickCounter;

            try (Timing ignored2 = Timings.connectionTimer.startTiming()) {
                this.network.processInterfaces();
            }

            try (Timing ignored2 = Timings.schedulerTimer.startTiming()) {
                this.scheduler.mainThreadHeartbeat(this.tickCounter);
            }

            this.checkTickUpdates(this.tickCounter, tickTime);

            this.levelManager.tick(this.tickCounter);

            for (Player player : new ArrayList<>(this.players.values())) {
                ((CloudPlayer) player).checkNetwork();
            }

            if ((this.tickCounter & 0b1111) == 0) {
                this.titleTick();
                this.network.resetStatistics();
                this.maxTick = 20;
                this.maxUse = 0;

                if ((this.tickCounter & 0b111111111) == 0) {
                    try {
                        this.eventManager.fire(this.queryRegenerateEvent = new QueryRegenerateEvent(this, 5));
                        if (this.queryHandler != null) {
                            this.queryHandler.regenerateInfo();
                        }
                    } catch (Exception e) {
                        log.error(e);
                    }
                }

                this.getNetwork().updateName();
            }

            if (this.autoSave && ++this.autoSaveTicker >= this.autoSaveTicks) {
                this.autoSaveTicker = 0;
                this.doAutoSave();
            }

            if (this.sendUsageTicker > 0 && --this.sendUsageTicker == 0) {
                this.sendUsageTicker = 6000;
                //todo sendUsage
            }
        }
        //long now = System.currentTimeMillis();
        long nowNano = System.nanoTime();
        //float tick = Math.min(20, 1000 / Math.max(1, now - tickTime));
        //float use = Math.min(1, (now - tickTime) / 50);

        float tick = (float) Math.min(20, 1000000000 / Math.max(1000000, ((double) nowNano - tickTimeNano)));
        float use = (float) Math.min(1, ((double) (nowNano - tickTimeNano)) / 50000000);

        if (this.maxTick > tick) {
            this.maxTick = tick;
        }

        if (this.maxUse < use) {
            this.maxUse = use;
        }

        System.arraycopy(this.tickAverage, 1, this.tickAverage, 0, this.tickAverage.length - 1);
        this.tickAverage[this.tickAverage.length - 1] = tick;

        System.arraycopy(this.useAverage, 1, this.useAverage, 0, this.useAverage.length - 1);
        this.useAverage[this.useAverage.length - 1] = use;

        if ((this.nextTick - tickTime) < -1000) {
            this.nextTick = tickTime;
        } else {
            this.nextTick += 50;
        }

        return true;
    }

    public long getNextTick() {
        return nextTick;
    }

    // TODO: Fix title tick
    public void titleTick() {
        if (!Bootstrap.ANSI || !Bootstrap.TITLE) {
            return;
        }

        Runtime runtime = Runtime.getRuntime();
        double used = NukkitMath.round((double) (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024, 2);
        double max = NukkitMath.round(((double) runtime.maxMemory()) / 1024 / 1024, 2);
        String usage = Math.round(used / max * 100) + "%";
        String title = (char) 0x1b + "]0;" + this.getName() + " "
                + this.getImplementationVersion()
                + " | Online " + this.players.size() + "/" + this.getMaxPlayers()
                + " | Memory " + usage;
        if (!Bootstrap.shortTitle) {
            title += " | U " + NukkitMath.round((this.network.getUpload() / 1024 * 1000), 2)
                    + " D " + NukkitMath.round((this.network.getDownload() / 1024 * 1000), 2) + " kB/s";
        }
        title += " | TPS " + this.getTicksPerSecond()
                + " | Load " + this.getTickUsage() + "%" + (char) 0x07;

        System.out.print(title);
    }

    public QueryRegenerateEvent getQueryInformation() {
        return this.queryRegenerateEvent;
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
        return this.serverProperties.getServerPort();
    }

    public int getViewDistance() {
        return this.serverProperties.getViewDistance();
    }

    public String getIp() {
        return this.serverProperties.getServerIp();
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
        return this.serverProperties.isGenerateStructures();
    }

    public GameMode getGamemode() {
        return GameMode.from(this.serverProperties.getGamemode());
    }

    public boolean getForceGamemode() {
        return this.serverProperties.isForceGamemode();
    }

    public Difficulty getDifficulty() {
        if (this.difficulty == null) {
            this.difficulty = Difficulty.values()[this.serverProperties.getDifficulty()];
        }
        return this.difficulty;
    }

    public boolean hasWhitelist() {
        return this.serverProperties.isWhiteList();
    }

    public int getSpawnRadius() {
        return this.serverProperties.getSpawnProtection();
    }

    public boolean getAllowFlight() {
        if (getAllowFlight == null) {
            getAllowFlight = this.serverProperties.isAllowFlight();
        }
        return getAllowFlight;
    }

    public boolean isHardcore() {
        return this.serverProperties.isHardcore();
    }

    public GameMode getDefaultGamemode() {
        if (this.defaultGamemode == null) {
            this.defaultGamemode = this.getGamemode();
        }
        return this.defaultGamemode;
    }

    public String getMotd() {
        return this.serverProperties.getMotd();
    }

    public String getSubMotd() {
        return this.serverProperties.getSubMotd();
    }

    public boolean getForceResources() {
        return this.serverProperties.isForceResources();
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

    public CloudEventManager getEventManager() {
        return eventManager;
    }

    public CloudPluginManager getPluginManager() {
        return this.pluginManager;
    }

    public CloudPermissionManager getPermissionManager() {
        return permissionManager;
    }

    public CraftingManager getCraftingManager() {
        return craftingManager;
    }

    public PackManager getPackManager() {
        return packManager;
    }

    public ServerScheduler getScheduler() {
        return scheduler;
    }

    public int getTick() {
        return tickCounter;
    }

    public float getTicksPerSecond() {
        return ((float) Math.round(this.maxTick * 100)) / 100;
    }

    public float getTicksPerSecondAverage() {
        float sum = 0;
        int count = this.tickAverage.length;
        for (float aTickAverage : this.tickAverage) {
            sum += aTickAverage;
        }
        return (float) NukkitMath.round(sum / count, 2);
    }

    public float getTickUsage() {
        return (float) NukkitMath.round(this.maxUse * 100, 2);
    }

    public float getTickUsageAverage() {
        float sum = 0;
        int count = this.useAverage.length;
        for (float aUseAverage : this.useAverage) {
            sum += aUseAverage;
        }
        return ((float) Math.round(sum / count * 100)) / 100;
    }

    public CommandRegistry getCommandRegistry() {
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
        byte[] nameBytes = name.toLowerCase().getBytes(StandardCharsets.UTF_8);
        byte[] uuidBytes = nameLookup.get(nameBytes);
        if (uuidBytes == null) {
            return Optional.empty();
        }

        if (uuidBytes.length != 16) {
            log.warn("Invalid uuid in name lookup database detected! Removing");
            nameLookup.delete(nameBytes);
            return Optional.empty();
        }

        ByteBuffer buffer = ByteBuffer.wrap(uuidBytes);
        return Optional.of(new UUID(buffer.getLong(), buffer.getLong()));
    }

    public void updateName(UUID uuid, String name) {
        byte[] nameBytes = name.toLowerCase().getBytes(StandardCharsets.UTF_8);

        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());

        nameLookup.put(nameBytes, buffer.array());
    }

    public Player getOfflinePlayer(UUID uuid) {
        Preconditions.checkNotNull(uuid, "uuid");
        Optional<Player> onlinePlayer = getPlayer(uuid);
        //noinspection OptionalIsPresent
        if (onlinePlayer.isPresent()) {
            return onlinePlayer.get();
        }

        return new OfflinePlayer(this, uuid);
    }

    public NbtMap getOfflinePlayerData(UUID uuid) {
        return getOfflinePlayerData(uuid, false);
    }

    public NbtMap getOfflinePlayerData(UUID uuid, boolean create) {
        return getOfflinePlayerDataInternal(uuid.toString(), true, create);
    }

    @Deprecated
    public NbtMap getOfflinePlayerData(String name) {
        return getOfflinePlayerData(name, false);
    }

    @Deprecated
    public NbtMap getOfflinePlayerData(String name, boolean create) {
        Optional<UUID> uuid = lookupName(name);
        return getOfflinePlayerDataInternal(uuid.map(UUID::toString).orElse(name), true, create);
    }

    private NbtMap getOfflinePlayerDataInternal(String name, boolean runEvent, boolean create) {
        Preconditions.checkNotNull(name, "name");

        PlayerDataSerializeEvent event = new PlayerDataSerializeEvent(name, playerDataSerializer);
        if (runEvent) {
            eventManager.fire(event);
        }

        Optional<InputStream> dataStream = Optional.empty();
        try {
            dataStream = event.getSerializer().read(name, event.getUuid().orElse(null));
            if (dataStream.isPresent()) {
                try (NBTInputStream stream = NbtUtils.createGZIPReader(dataStream.get())) {
                    return (NbtMap) stream.readTag();
                }
            }
        } catch (IOException e) {
            log.warn(this.getLanguage().translate("cloudburst.data.playerCorrupted", name));
            log.throwing(e);
        } finally {
            if (dataStream.isPresent()) {
                try {
                    dataStream.get().close();
                } catch (IOException e) {
                    log.throwing(e);
                }
            }
        }
        NbtMap nbt = null;
        if (create) {
            log.info(this.getLanguage().translate("cloudburst.data.playerNotFound", name));
            Location spawn = this.getDefaultLevel().getSafeSpawn();
            nbt = NbtMap.builder()
                    .putLong("firstPlayed", System.currentTimeMillis() / 1000)
                    .putLong("lastPlayed", System.currentTimeMillis() / 1000)
                    .putList("Pos", NbtType.FLOAT, Arrays.asList(
                            spawn.getPosition().getX(),
                            spawn.getPosition().getY(),
                            spawn.getPosition().getZ()
                    ))
                    .putString("Level", this.getDefaultLevel().getName())
                    .putInt("playerGameType", this.getGamemode().getVanillaId())
                    .putList("Rotation", NbtType.FLOAT, Arrays.asList(
                            spawn.getYaw(),
                            spawn.getPitch()
                    ))
                    .build();

            this.saveOfflinePlayerData(name, nbt, true, runEvent);
        }
        return nbt;
    }

    public void saveOfflinePlayerData(UUID uuid, NbtMap tag) {
        this.saveOfflinePlayerData(uuid, tag, false);
    }

    public void saveOfflinePlayerData(String name, NbtMap tag) {
        this.saveOfflinePlayerData(name, tag, false);
    }

    public void saveOfflinePlayerData(UUID uuid, NbtMap tag, boolean async) {
        this.saveOfflinePlayerData(uuid.toString(), tag, async);
    }

    public void saveOfflinePlayerData(String name, NbtMap tag, boolean async) {
        Optional<UUID> uuid = lookupName(name);
        saveOfflinePlayerData(uuid.map(UUID::toString).orElse(name), tag, async, true);
    }

    private void saveOfflinePlayerData(String name, NbtMap tag, boolean async, boolean runEvent) {
        String nameLower = name.toLowerCase();
        if (this.shouldSavePlayerData()) {
            PlayerDataSerializeEvent event = new PlayerDataSerializeEvent(nameLower, playerDataSerializer);
            if (runEvent) {
                eventManager.fire(event);
            }

            this.getScheduler().scheduleTask(new Task() {
                boolean hasRun = false;

                @Override
                public void onRun(int currentTick) {
                    this.onCancel();
                }

                //doing it like this ensures that the playerdata will be saved in a server shutdown
                @Override
                public void onCancel() {
                    if (!this.hasRun) {
                        this.hasRun = true;
                        saveOfflinePlayerDataInternal(event.getSerializer(), tag, nameLower, event.getUuid().orElse(null));
                    }
                }
            }, async);
        }
    }

    private void saveOfflinePlayerDataInternal(PlayerDataSerializer serializer, NbtMap tag, String name, UUID uuid) {
        try (OutputStream dataStream = serializer.write(name, uuid);
             NBTOutputStream stream = NbtUtils.createGZIPWriter(dataStream)) {
            stream.writeTag(tag);
        } catch (Exception e) {
            log.error(this.getLanguage().translate("cloudburst.data.saveError", name, e));
        }
    }

    private void convertLegacyPlayerData() {
        File dataDirectory = this.dataPath.resolve("players").toFile();

        File[] files = dataDirectory.listFiles(file -> {
            String name = file.getName();
            Matcher matcher = UUID_PATTERN.matcher(name);
            return !matcher.matches() && name.endsWith(".dat");
        });

        if (files == null) {
            return;
        }

        for (File legacyData : files) {
            String name = legacyData.getName();
            // Remove file extension
            name = name.substring(0, name.length() - 4);

            log.debug("Attempting legacy player data conversion for {}", name);

            NbtMap tag = this.getOfflinePlayerDataInternal(name, false, false);

            if (tag == null || !tag.containsKey("UUIDLeast") || !tag.containsKey("UUIDMost")) {
                // No UUID so we cannot convert. Wait until player logs in.
                continue;
            }

            UUID uuid = new UUID(tag.getLong("UUIDMost"), tag.getLong("UUIDLeast"));
            if (!tag.containsKey("NameTag")) {
                tag = tag.toBuilder().putString("NameTag", name).build();
            }

            if (new File(getDataPath() + "players/" + uuid.toString() + ".dat").exists()) {
                // We don't want to overwrite existing data.
                continue;
            }

            this.saveOfflinePlayerData(uuid.toString(), tag, false, false);

            // Add name to lookup table
            this.updateName(uuid, name);

            // Delete legacy data
            if (!legacyData.delete()) {
                log.warn("Unable to delete legacy data for {}", name);
            }
        }
    }

    public Player getPlayer(String name) {
        Player found = null;
        name = name.toLowerCase();
        int delta = Integer.MAX_VALUE;
        for (Player player : this.getOnlinePlayers().values()) {
            if (player.getName().toLowerCase().startsWith(name)) {
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
        name = name.toLowerCase();
        for (Player player : this.getOnlinePlayers().values()) {
            if (player.getName().toLowerCase().equals(name)) {
                return (CloudPlayer) player;
            }
        }

        return null;
    }

    public Player[] matchPlayer(String partialName) {
        partialName = partialName.toLowerCase();
        List<Player> matchedPlayer = new ArrayList<>();
        for (Player player : this.getOnlinePlayers().values()) {
            if (player.getName().toLowerCase().equals(partialName)) {
                return new Player[]{player};
            } else if (player.getName().toLowerCase().contains(partialName)) {
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

        for (InetSocketAddress socketAddress : new ArrayList<>(this.players.keySet())) {
            CloudPlayer p = this.players.get(socketAddress);
            if (player == p) {
                this.players.remove(socketAddress);
                break;
            }
        }
    }

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

    public boolean unloadLevel(CloudLevel level) {
        return this.unloadLevel(level, false);
    }

    public boolean unloadLevel(CloudLevel level, boolean forceUnload) {
        if (level == this.getDefaultLevel() && !forceUnload) {
            throw new IllegalStateException("The default level cannot be unloaded while running, please switch levels.");
        }

        return level.unload(forceUnload);

    }

    public LevelBuilder loadLevel() {
        return new LevelBuilder(this);
    }

    public LocaleManager getLanguage() {
        return localeManager;
    }

    public boolean isLanguageForced() {
        return forceLanguage;
    }

    public Network getNetwork() {
        return network;
    }

    public ServerConfig getConfig() {
        return new ServerConfig(serverProperties, cloudburstYaml);
    }

    public BanList getNameBans() {
        return this.banByName;
    }

    public BanList getIPBans() {
        return this.banByIP;
    }

    @Override
    public boolean isBanned(Player player) {
        return this.banByName.isBanned(player.getName().toLowerCase());
    }

    @Override
    public boolean isIPBanned(Player player) {
        return this.banByIP.isBanned(player.getName().toLowerCase());
    }

    @Override
    public void setBanned(Player who, boolean banned, boolean byIP) {
        if (banned) {
            if (byIP)
                this.banByIP.addBan(((CloudPlayer) who).getAddress());
            else
                this.banByName.addBan(who.getName().toLowerCase());
        } else {
            this.banByName.remove(who.getName());
            this.banByIP.remove(((CloudPlayer) who).getAddress());
        }
    }

    @Override
    public void addOp(Player who) {
        this.addOp(who.getName());
    }

    public void addOp(String name) {
        this.operators.set(name.toLowerCase(), true);
        CloudPlayer player = this.getPlayerExact(name);
        if (player != null) {
            player.recalculatePermissions();
        }
        this.operators.save(true);
    }

    @Override
    public void removeOp(Player who) {
        this.removeOp(who.getName());
    }

    public void removeOp(String name) {
        this.operators.remove(name.toLowerCase());
        CloudPlayer player = this.getPlayerExact(name);
        if (player != null) {
            player.recalculatePermissions();
        }
        this.operators.save();
    }

    @Override
    public void addWhitelist(Player player) {
        this.addWhitelist(player.getName());
    }

    public void addWhitelist(String name) {
        this.whitelist.set(name.toLowerCase(), true);
        this.whitelist.save(true);
    }

    @Override
    public void removeWhitelist(Player player) {
        this.removeWhitelist(player.getName());
    }

    public void removeWhitelist(String name) {
        this.whitelist.remove(name.toLowerCase());
        this.whitelist.save(true);
    }

    @Override
    public boolean isWhitelisted(Player player) {
        return this.isWhitelisted(player.getName());
    }

    public boolean isWhitelisted(String name) {
        return !this.hasWhitelist() || this.operators.exists(name, true) || this.whitelist.exists(name, true);
    }

    @Override
    public boolean isOp(Player player) {
        return this.isOp(player.getName());
    }

    public boolean isOp(String name) {
        return this.operators.exists(name, true);
    }

    public Config getWhitelist() {
        return whitelist;
    }

    public Config getOps() {
        return operators;
    }

    public void reloadWhitelist() {
        this.whitelist.reload();
    }

    public Map<String, List<String>> getCommandAliases() {
        return getConfig().getCommandAliases();
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
            Path pluginPath = dataPath.resolve("plugins");
            if (Files.notExists(pluginPath)) {
                Files.createDirectory(pluginPath);
            } else {
                if (!Files.isDirectory(pluginPath)) {
                    log.info("Plugin location {} is not a directory, continuing without loading plugins.", pluginPath);
                    return;
                }
            }
            pluginManager.loadPlugins(pluginPath);
        } catch (Exception e) {
            log.error("Can't load plugins", e);
        }
        log.info("Loaded {} plugins.", pluginManager.getAllPlugins().size());
    }

    private void loadPacks() {
        Path resourcePath = dataPath.resolve("resource_packs");
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
        this.defaultLevelData.getGameRules().putAll(this.gameRuleRegistry.getDefaultRules());
    }

    private void loadLevels() throws IOException {
        Path levelPath = dataPath.resolve("worlds");
        if (Files.notExists(levelPath)) {
            Files.createDirectory(levelPath);
        } else if (!Files.isDirectory(levelPath)) {
            throw new RuntimeException("Worlds location " + levelPath + " is not a directory.");
        }

        Map<String, ServerConfig.World> worldConfigs = getConfig().getWorlds();
        if (worldConfigs.isEmpty()) {
            throw new IllegalStateException("No worlds configured! Add a world to cloudburst.yml and try again!");
        }
        List<CompletableFuture<CloudLevel>> levelFutures = new ArrayList<>(worldConfigs.size());

        for (String name : worldConfigs.keySet()) {
            final ServerConfig.World config = worldConfigs.get(name);
            //fallback to level name if no seed is set
            Object seedObj = config.getSeed();
            long seed;
            if (seedObj instanceof Number) {
                seed = ((Number) seedObj).longValue();
            } else if (seedObj instanceof String) {
                if (seedObj == name) {
                    log.warn("World \"{}\" does not have a seed! Using a the name as the seed", name);
                }

                //this internally generates an MD5 hash of the seed string
                UUID uuid = UUID.nameUUIDFromBytes(((String) seedObj).getBytes(StandardCharsets.UTF_8));
                seed = uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits();
            } else {
                throw new IllegalStateException("Seed for world \"" + name + "\" is invalid: " + (seedObj == null ? "null" : seedObj.getClass().getCanonicalName()));
            }

            Identifier generator = Identifier.fromString(config.getGenerator());
            String options = config.getOptions();

            levelFutures.add(this.loadLevel().id(name)
                    .seed(seed)
                    .generator(generator == null ? this.generatorRegistry.getFallback() : generator)
                    .generatorOptions(options)
                    .load());
        }

        // Wait for levels to load.
        CompletableFutures.allAsList(levelFutures).join();

        //set default level
        if (this.getDefaultLevel() == null) {
            String defaultName = this.serverProperties.getDefaultLevel();
            if (defaultName == null || defaultName.trim().isEmpty()) {
                this.serverProperties.modifyDefaultLevel(worldConfigs.keySet().iterator().next());
                log.warn("default-level is unset or empty, falling back to \"" + defaultName + '"');
            }

            CloudLevel defaultLevel = this.levelManager.getLevel(defaultName);
            if (defaultLevel == null) {
                throw new IllegalArgumentException("default-level refers to unknown level: \"" + defaultName + '"');
            }
            this.levelManager.setDefaultLevel(defaultLevel);
        }
    }

    public boolean isNetherAllowed() {
        return this.allowNether;
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

    public LevelData getDefaultLevelData() {
        return defaultLevelData;
    }

    public Identifier getDefaultStorageId() {
        return defaultStorageId;
    }

    public StorageRegistry getStorageRegistry() {
        return storageRegistry;
    }

    public CloudGameRuleRegistry getGameRuleRegistry() {
        return gameRuleRegistry;
    }

    public CloudBlockRegistry getBlockRegistry() {
        return blockRegistry;
    }

    public ItemRegistry getItemRegistry() {
        return itemRegistry;
    }

    public RecipeRegistry getRecipeRegistry() {
        return recipeRegistry;
    }

    public GeneratorRegistry getGeneratorRegistry() {
        return this.generatorRegistry;
    }

    public int getBaseTickRate() {
        return baseTickRate;
    }

    public int getAutoTickRateLimit() {
        return autoTickRateLimit;
    }

    public boolean isAutoTickRate() {
        return autoTickRate;
    }

    public static CloudServer getInstance() {
        return instance;
    }

    public boolean isIgnoredPacket(Class<? extends BedrockPacket> clazz) {
        return this.ignoredPackets.contains(clazz.getSimpleName());
    }

    private class ConsoleThread extends Thread implements InterruptibleThread {

        private ConsoleThread() {
            super("Console Thread");
        }

        @Override
        public void run() {
            console.start();
        }
    }
}
