package org.cloudburstmc.server.player;

import io.netty.channel.ChannelPipeline;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.chat.ChatType;
import net.kyori.adventure.chat.SignedMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.TitlePart;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.ban.BanEntry;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.entity.*;
import org.cloudburstmc.api.entity.damage.*;
import org.cloudburstmc.api.entity.misc.DroppedItem;
import org.cloudburstmc.api.entity.projectile.AbstractArrow;
import org.cloudburstmc.api.entity.projectile.Arrow;
import org.cloudburstmc.api.entity.projectile.FishingHook;
import org.cloudburstmc.api.entity.projectile.ThrownTrident;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;
import org.cloudburstmc.api.event.entity.EntityRemoveCause;
import org.cloudburstmc.api.event.entity.PotionEffectCause;
import org.cloudburstmc.api.event.entity.TeleportCause;
import org.cloudburstmc.api.event.inventory.InventoryCloseEvent;
import org.cloudburstmc.api.event.inventory.InventoryPickupArrowEvent;
import org.cloudburstmc.api.event.inventory.InventoryPickupItemEvent;
import org.cloudburstmc.api.event.player.*;
import org.cloudburstmc.api.inventory.*;
import org.cloudburstmc.api.inventory.view.*;
import org.cloudburstmc.api.item.*;
import org.cloudburstmc.api.item.component.*;
import org.cloudburstmc.api.level.*;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.api.level.sound.SoundPlayback;
import org.cloudburstmc.api.level.sound.SoundType;
import org.cloudburstmc.api.permission.EffectivePermission;
import org.cloudburstmc.api.permission.PermissionAttachment;
import org.cloudburstmc.api.player.*;
import org.cloudburstmc.api.player.Ability;
import org.cloudburstmc.api.player.skin.Skin;
import org.cloudburstmc.api.plugin.PluginContainer;
import org.cloudburstmc.api.potion.EffectType;
import org.cloudburstmc.api.potion.EffectTypes;
import org.cloudburstmc.api.potion.PotionEffect;
import org.cloudburstmc.api.util.*;
import org.cloudburstmc.math.GenericMath;
import org.cloudburstmc.math.vector.Vector2f;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.netty.channel.nethernet.NetherNetChannel;
import org.cloudburstmc.protocol.adventure.BedrockComponent;
import org.cloudburstmc.protocol.adventure.BedrockLegacyTextSerializer;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.data.*;
import org.cloudburstmc.protocol.bedrock.data.command.CommandPermission;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityEventType;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerId;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.data.skin.SerializedSkin;
import org.cloudburstmc.protocol.bedrock.packet.*;
import org.cloudburstmc.protocol.common.util.OptionalBoolean;
import org.cloudburstmc.server.Achievement;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.block.BlockPalette;
import org.cloudburstmc.server.block.component.BedBlockHandlers;
import org.cloudburstmc.server.block.component.RespawnAnchorBlockHandlers;
import org.cloudburstmc.server.boss.CloudBossBar;
import org.cloudburstmc.server.command.network.CommandNetworkCompiler;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.container.Container;
import org.cloudburstmc.server.container.ContainerListener;
import org.cloudburstmc.server.container.screen.CloudInventoryScreen;
import org.cloudburstmc.server.container.screen.CloudVirtualChestScreen;
import org.cloudburstmc.server.container.screen.CloudVirtualDoubleChestScreen;
import org.cloudburstmc.server.container.screen.CloudVirtualHopperScreen;
import org.cloudburstmc.server.container.view.CloudEnderChestView;
import org.cloudburstmc.server.container.view.CloudHotbarView;
import org.cloudburstmc.server.container.view.CloudPlayerInventory;
import org.cloudburstmc.server.entity.CloudDamageResult;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.entity.CloudHumanEntity;
import org.cloudburstmc.server.entity.CloudLivingEntity;
import org.cloudburstmc.server.entity.data.CloudEquipmentData;
import org.cloudburstmc.server.entity.misc.EntityExperienceOrb;
import org.cloudburstmc.server.entity.projectile.EntityFishingHook;
import org.cloudburstmc.server.entity.vehicle.EntityVehicle;
import org.cloudburstmc.server.event.entity.CloudEntityDamageEvent;
import org.cloudburstmc.server.event.server.PlayerPacketSendEvent;
import org.cloudburstmc.server.item.ItemStackPacketCodec;
import org.cloudburstmc.server.item.component.ArmorItemHandlers;
import org.cloudburstmc.server.item.component.ConsumableItemHandlers;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.EndPortals;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.network.*;
import org.cloudburstmc.server.network.inventory.InventoryRequestQueue;
import org.cloudburstmc.server.network.nethernet.CloudLoginTimeoutHandler;
import org.cloudburstmc.server.network.nethernet.CloudNetherNetPeer;
import org.cloudburstmc.server.permission.CloudPermissible;
import org.cloudburstmc.server.player.handler.PlayerPacketHandler;
import org.cloudburstmc.server.player.manager.PlayerChunkManager;
import org.cloudburstmc.server.player.manager.PlayerInventoryManager;
import org.cloudburstmc.server.registry.CloudEnchantmentRegistry;
import org.cloudburstmc.server.registry.CloudEntityRegistry;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes.*;
import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.DAMAGE_NEARBY_MOBS;
import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.USING_ITEM;

@Log4j2
public class CloudPlayer extends CloudHumanEntity implements Player, ContainerListener {

    private static final float DEFAULT_MOVEMENT_SPEED = 0.1f;
    private static final int BEDROCK_FIREWORK_GLIDE_BOOST_DURATION = 1_000_000;
    private static final float TELEPORT_ACK_DISTANCE_TOLERANCE = 1.0f;
    private static final float TELEPORT_ACK_DISTANCE_TOLERANCE_SQUARED = TELEPORT_ACK_DISTANCE_TOLERANCE * TELEPORT_ACK_DISTANCE_TOLERANCE;

    private static final int SURVIVAL_INTERACTION_DISTANCE = 7;
    private static final int CREATIVE_INTERACTION_DISTANCE = 13;
    private static final float INTERACTION_BEHIND_TOLERANCE = 6.0f;
    private static final int COMPLETED_USE_GRACE_TICKS = 9;

    protected final BedrockServerSession session;
    private final AuthenticatedPlayerData connectionData;
    private final PlayerPacketHandler packetHandler;
    private final GameplayPacketQueue inboundQueue;

    @Getter
    protected boolean connected = true;
    public boolean loggedIn;
    public boolean spawned;
    public long creationTime;

    @Getter
    private boolean initialized;

    @Getter
    @Setter
    private boolean clientCacheEnabled;

    protected String username;
    protected Component displayName;
    public long lastSkinChange;
    private final AtomicReference<Locale> locale = new AtomicReference<>();

    @Getter
    @Setter
    protected boolean removeFormat = true;
    protected int messageCounter = 2;

    private CloudPermissible perm;
    protected CloudPlayerAbilities abilities;
    private GameMode previousGameMode;
    private boolean clientCommandsEnabledStateSent;
    private boolean clientCommandDataSent;
    private boolean lastSentOperatorAbilities;

    @Getter
    private final PlayerChunkManager chunkManager = new PlayerChunkManager(this);
    private final Map<UUID, Set<PluginContainer>> hiddenPlayers = new HashMap<>();
    private final Set<CloudBossBar> bossBars = new LinkedHashSet<>();

    @Getter
    private final InventoryRequestQueue inventoryRequestQueue = new InventoryRequestQueue(this);
    private final CloudPlayerInventory inventory = new CloudPlayerInventory(this, this.container);

    @Getter
    private final CloudHotbarView hotbar = new CloudHotbarView(this);

    @Getter
    private final CloudContainer enderChestContainer = new CloudContainer(27);
    private final CloudEnderChestView enderChest = new CloudEnderChestView(this);
    private final PlayerInventoryManager invManager = new PlayerInventoryManager(this);

    private final LongOpenHashSet attachedFireworkRockets = new LongOpenHashSet();
    private float baseMovementSpeed = DEFAULT_MOVEMENT_SPEED;
    private boolean firstMove = true;
    private boolean wasUnderwater;

    @Setter
    private boolean scaffoldDescentInput;

    @Setter
    protected Vector3f newPosition;

    @Getter
    @Setter
    protected long clientTick;

    @Getter
    protected Vector3f teleportPosition;
    private boolean teleportAcknowledged;
    private boolean pendingTeleportEntityViewRefresh;

    @Setter
    private boolean changingDimension;
    private boolean showingCredits;
    private boolean seenCredits;

    protected Location spawnLocation;
    protected RespawnConfig respawnConfig;
    private @Nullable Location respawnTarget;
    protected Vector3i sleeping;
    private long sleepStartedAt;

    private final PlayerSaveData saveData = new PlayerSaveData();
    private final Set<String> achievements = new HashSet<>();

    @Getter
    protected final CloudPlayerFood foodData = new CloudPlayerFood(this);

    private final PlayerExperience experience = new PlayerExperience();
    private int lastExperiencePickupTick;

    @Getter
    private Entity killer;

    private @Nullable EntityFishingHook fishingHook;

    @Getter
    private String buttonText = "Button";

    private final Map<ItemType, Integer> itemCooldowns = new HashMap<>();
    private final ItemBlockingController itemBlocking = new ItemBlockingController(this);
    protected int startAction = -1;
    private @Nullable ItemStack activeUseStack;
    private int activeUseSlot = -1;
    private int itemUseCompleteTick;
    private int lastCompletedItemUseTick = Integer.MIN_VALUE;
    private int spinAttackEndTick = -1;

    public CloudPlayer(BedrockServerSession session, AuthenticatedPlayerData connectionData) {
        super(EntityTypes.PLAYER, Location.from(CloudServer.getInstance().getDefaultLevel()), new CloudEquipmentData());
        this.session = session;
        this.packetHandler = new PlayerPacketHandler(this);
        this.server = CloudServer.getInstance();
        this.saveData.setGameMode(this.server.getGameMode());
        this.boundingBox = new BoundingBox(0, 0, 0, 0, 0, 0);
        this.lastSkinChange = -1;

        this.connectionData = connectionData;
        this.locale.set(connectionData.getLocale());

        this.username = PlainTextComponentSerializer.plainText().serialize(BedrockLegacyTextSerializer.getInstance().deserialize(connectionData.getName()));
        this.displayName(Component.text(this.username));
        this.data.set(NAME, this.defaultNameplate());

        this.perm = new CloudPermissible(this.server.getPermissionManager(), this, this::onPermissionsChanged);

        this.creationTime = System.currentTimeMillis();

        this.container.addContainerListener(this);
        this.enderChestContainer.addContainerListener(this);
        this.armor.getContainer().addContainerListener(this);
        this.offhand.getContainer().addContainerListener(this);

        this.inboundQueue = ((CloudNetherNetPeer) session.getPeer()).startGameplay(session, new SessionHandler());
    }

    public void loadLoginData() {
        CloudPlayer oldPlayer = null;
        for (CloudPlayer player : new ArrayList<>(this.getServer().getOnlinePlayers().values())) {
            if (player != this && (player.getName() != null && player.getName().equalsIgnoreCase(this.getName()) || this.getServerId().equals(player.getServerId()))) {
                oldPlayer = player;
                break;
            }
        }

        NbtMap nbt;
        if (oldPlayer != null) {
            NbtMapBuilder tag = NbtMap.builder();
            oldPlayer.saveAdditionalData(tag);
            nbt = tag.build();
            oldPlayer.close("", "disconnectionScreen.loggedinOtherLocation");
        } else {
            nbt = this.server.getOfflinePlayerData(this.getServerId(), true);
        }

        if (nbt == null) {
            this.close(this.leaveMessage(), "Invalid data");
            return;
        }

        if (this.connectionData.isAuthenticated() || !this.server.getConfig().isXboxAuth()) {
            server.updateName(this.getServerId(), this.username);
        }

        this.loadAdditionalData(nbt);

        if (this.server.getForceGamemode()) {
            this.setGameMode(this.server.getGameMode(), PlayerGameModeChangeEvent.Cause.DEFAULT_GAMEMODE);
        }

        this.abilities = buildAbilitiesForGameMode(this.getGameMode());

        if (!this.isAlive()) {
            this.level = this.server.getDefaultLevel();
        }

        if (this.server.getAutoSave()) {
            this.server.saveOfflinePlayerData(this.getServerId(), nbt, true);
        }
    }

    public void initializeForLogin() {
        super.initialize(this.getLocation());
        this.noPhysics = this.isSpectator();
        this.teleportPosition = this.getPosition();
    }

    public void completeLoginSequence() {
        Vector3f pos = this.getPosition();

        StartGamePacket startGamePacket = new StartGamePacket();
        startGamePacket.setUniqueEntityId(this.getUniqueId());
        startGamePacket.setRuntimeEntityId(this.getRuntimeId());
        startGamePacket.setPlayerGameType(GameModeNetworkMapping.forStartGame(this.getGameMode()));
        startGamePacket.setPlayerPosition(pos);
        startGamePacket.setRotation(Vector2f.from(this.getYaw(), this.getPitch()));
        startGamePacket.setSeed(-1L);
        startGamePacket.setDimensionId(this.getLevel().getDimension());
        startGamePacket.setTrustingPlayers(false);
        startGamePacket.setLevelGameType(GameModeNetworkMapping.forStartGame(this.server.getGameMode()));
        startGamePacket.setDifficulty(this.server.getDifficulty().getId());
        startGamePacket.setDefaultSpawn(this.getLevel().getSpawnLocation().toInt());
        startGamePacket.setAchievementsDisabled(true);
        startGamePacket.setDayCycleStopTime(LevelTimePacketFactory.networkTime(this.getLevel().getFullTime()));
        startGamePacket.setCurrentTick(this.getLevel().getGameTime());
        startGamePacket.setRainLevel(0);
        startGamePacket.setLightningLevel(0);
        startGamePacket.setCommandsEnabled(true);
        startGamePacket.setMultiplayerGame(true);
        startGamePacket.setBroadcastingToLan(true);
        LevelTimePacketFactory.writeGameRules(this.getLevel(), startGamePacket.getGamerules());
        startGamePacket.setLevelId("");
        startGamePacket.setLevelName(this.getLevel().getName());
        startGamePacket.setGeneratorId(1);
        startGamePacket.setXblBroadcastMode(GamePublishSetting.PUBLIC);
        startGamePacket.setPlatformBroadcastMode(GamePublishSetting.PUBLIC);
        startGamePacket.setDefaultPlayerPermission(this.isOp() ? PlayerPermission.OPERATOR : PlayerPermission.MEMBER);
        startGamePacket.setServerChunkTickRange(4);
        startGamePacket.setBehaviorPackLocked(false);
        startGamePacket.setResourcePackLocked(false);
        startGamePacket.setFromLockedWorldTemplate(false);
        startGamePacket.setUsingMsaGamertagsOnly(false);
        startGamePacket.setFromWorldTemplate(false);
        startGamePacket.setWorldTemplateOptionLocked(false);
        startGamePacket.setVanillaVersion("*");
        startGamePacket.getExperiments().addAll(VanillaExperiments.getEnabled());
        startGamePacket.setPremiumWorldTemplateId("00000000-0000-0000-0000-000000000000");
        startGamePacket.setMultiplayerCorrelationId("");
        startGamePacket.setInventoriesServerAuthoritative(true);
        startGamePacket.setRewindHistorySize(this.server.getConfig().getMovement().getRewindHistorySize());
        startGamePacket.setServerAuthoritativeBlockBreaking(true);
        startGamePacket.setServerEngine("");
        startGamePacket.setPlayerPropertyData(NbtMap.EMPTY);
        startGamePacket.setWorldTemplateId(UUID.randomUUID());
        startGamePacket.setChatRestrictionLevel(ChatRestrictionLevel.NONE);
        startGamePacket.setSpawnBiomeType(SpawnBiomeType.DEFAULT);
        startGamePacket.setCustomBiomeName("");
        startGamePacket.setEducationProductionId("");
        startGamePacket.setForceExperimentalGameplay(OptionalBoolean.empty());
        startGamePacket.setServerId("");
        startGamePacket.setWorldId("");
        startGamePacket.setScenarioId("");
        startGamePacket.setOwnerId("");
        session.getPeer().getCodecHelper().setItemDefinitions(CloudItemRegistry.get().getPalette());
        session.getPeer().getCodecHelper().setBlockDefinitions(BlockPalette.INSTANCE);
        VanillaBlockNetworkData.addBlockProperties(startGamePacket);
        this.sendPacket(VanillaJigsawStructureNetworkData.createPacket());
        this.sendPacket(VanillaBlockNetworkData.createVoxelShapesPacket());
        this.sendPacket(startGamePacket);

        ItemComponentPacket componentPacket = new ItemComponentPacket();
        componentPacket.getItems().addAll(CloudItemRegistry.get().getPalette().getDefinitions());
        this.sendPacket(componentPacket);

        this.sendPacket(VanillaBiomeNetworkData.createPacket());

        AvailableEntityIdentifiersPacket availableEntityIdentifiersPacket = new AvailableEntityIdentifiersPacket();
        availableEntityIdentifiersPacket.setIdentifiers(CloudEntityRegistry.get().getEntityIdentifiersPalette());
        this.sendPacket(availableEntityIdentifiersPacket);

        if (this.isSpectator()) {
            SetPlayerGameTypePacket gameTypePacket = new SetPlayerGameTypePacket();
            gameTypePacket.setGamemode(GameModeNetworkMapping.playerTypeId(this.getGameMode()));
            this.sendPacket(gameTypePacket);
        }

        this.loggedIn = true;

        this.getLevel().sendTime(this);

        this.sendAttributes();
        this.setCustomNameVisible(true);
        this.setCustomNameAlwaysVisible(true);
        this.setCanClimb(true);

        log.info(this.getServer().getLanguage().translate("cloudburst.player.logIn",
                "§b" + this.username + "§r",
                this.getLoggableAddress(),
                this.getUniqueId(),
                this.getLevel().getName(),
                GenericMath.round(pos.getX(), 4),
                GenericMath.round(pos.getY(), 4),
                GenericMath.round(pos.getZ(), 4)
        ));

        if (this.hasPermission("cloudburst.textcolor")) {
            this.setRemoveFormat(false);
        }

        this.server.addOnlinePlayer(this);
        this.server.onPlayerCompleteLoginSequence(this);
    }

    @SuppressWarnings("resource")
    public void completeClientInitialization() {
        if (this.initialized || !this.spawned || !this.isConnected()) {
            return;
        }

        this.initialized = true;
        this.session.getPeer().getChannel().eventLoop().execute(() -> {
            ChannelPipeline pipeline = this.session.getPeer().getChannel().pipeline();
            if (pipeline.get(CloudLoginTimeoutHandler.class) != null) {
                pipeline.remove(CloudLoginTimeoutHandler.class);
            }
        });

        PlayerJoinEvent event = new PlayerJoinEvent(this, Component.translatable("multiplayer.player.joined", this.displayName()).color(NamedTextColor.YELLOW));
        this.server.getEventManager().fire(event);
        if (!this.isConnected()) {
            return;
        }

        this.server.getNetwork().reportExternalJoin(this.session.getPeer().getChannel());
        if (event.getJoinMessage() != null) {
            this.server.broadcast(event.getJoinMessage());
        }

        this.syncClientAuthorityState(true);
    }

    private void completeInitialSpawn() {
        this.spawned = true;

        this.sendPotionEffects(this);
        this.sendData(this);
        this.invManager.sendAllInventories();
        this.onInventoryContentsChange(this.armor.getContainer());
        this.sendHotbarSelection();

        this.getLevel().sendTime(this);

        Location loc = Location.from(this.getPosition(), this.getYaw(), this.getPitch(), this.getLevel());
        PlayerRespawnEvent respawnEvent = new PlayerRespawnEvent(this, loc, PlayerRespawnReason.INITIAL_SPAWN, EnumSet.noneOf(PlayerRespawnFlag.class));
        this.server.getEventManager().fire(respawnEvent);
        loc = respawnEvent.getRespawnLocation();

        if (this.getHealth() <= 0) {
            RespawnPacket respawnPacket = new RespawnPacket();
            respawnPacket.setRuntimeEntityId(0);
            respawnPacket.setPosition(loc.getPosition());
            respawnPacket.setState(RespawnPacket.State.SERVER_SEARCHING);
            this.sendPacket(respawnPacket);
        }

        CreativeContentPacket creativePacket = CloudItemRegistry.get().getCreativeContent();
        this.sendPacket(creativePacket);

        this.getServer().sendRecipeList(this);
        this.syncClientAuthorityState(false);
        this.sendPlayerSpawnStatus();

        this.setNoDamageTicks(60);

        this.getChunkManager().pairReadyEntities();

        if (this.experience.points() != 0 || this.getExperienceLevel() != 0) {
            this.sendExperienceAttributes();
        }

        this.teleportWithoutEvent(loc);

        if (!this.isSpectator()) {
            this.refreshViewers();
        }

        if (this.getLevel().isRaining() || this.getLevel().isThundering()) {
            this.getLevel().sendWeather(this);
        }

        CloudPlayerFood food = this.getFoodData();
        food.sendAttributes();
    }

    private void sendPlayerSpawnStatus() {
        PlayStatusPacket packet = new PlayStatusPacket();
        packet.setStatus(PlayStatusPacket.Status.PLAYER_SPAWN);
        this.sendPacket(packet);
    }

    @Override
    public boolean isOnline() {
        return this.connected && this.loggedIn;
    }

    @Override
    public boolean isSpawned() {
        return this.spawned;
    }

    @Override
    public int getPing() {
        if (!this.isConnected()) {
            return 0;
        }

        long ping = ((NetherNetChannel) this.session.getPeer().getChannel()).getPing();
        return Math.clamp(ping, 0, Integer.MAX_VALUE);
    }

    public SocketAddress getSocketAddress() {
        return this.session.getSocketAddress();
    }

    @Override
    public @Nullable InetSocketAddress getAddress() {
        return this.getSocketAddress() instanceof InetSocketAddress address ? address : null;
    }

    private String getLoggableAddress() {
        return NetworkUtils.loggableAddress(this.getSocketAddress(), this.server.getConfig().getPlayer().isLogPlayerAddresses());
    }

    @Override
    public PlayerClientInfo getClientInfo() {
        return this.connectionData;
    }

    public String getXuid() {
        return this.connectionData.isAuthenticated() ? this.connectionData.getXuid() : "";
    }

    @Override
    public CloudServer getServer() {
        return this.server;
    }

    public String getName() {
        return this.username;
    }

    @Override
    public Component name() {
        return Component.text(this.username);
    }

    @Override
    protected CharSequence defaultNameplate() {
        return this.username;
    }

    @Override
    public Component displayName() {
        return this.displayName;
    }

    @Override
    public void displayName(Component displayName) {
        this.displayName = displayName;
        if (this.spawned) {
            this.updatePlayerListEntry(displayName);
        }
    }

    public UUID getServerId() {
        return this.connectionData.getUniqueId();
    }

    @Override
    public PlayerProfile getProfile() {
        return this.connectionData;
    }

    @Override
    public Skin getSkin() {
        return this.connectionData.getSkin();
    }

    @Override
    public void setSkin(Skin skin) {
        this.connectionData.setSkin(skin);
        if (this.spawned) {
            this.updatePlayerListEntry(this.displayName());
        }
    }

    public void setSkin(SerializedSkin skin) {
        this.connectionData.setSkin(skin);
        if (this.spawned) {
            this.updatePlayerListEntry(this.displayName());
        }
    }

    public SerializedSkin getSerializedSkin() {
        return this.connectionData.getSerializedSkin();
    }

    private void updatePlayerListEntry(Component displayName) {
        this.getServer().updatePlayerListData(
                this.getServerId(), this.getUniqueId(),
                BedrockLegacyTextSerializer.getInstance().serialize(displayName),
                this.getSerializedSkin(), this.getXuid()
        );
    }

    public synchronized Locale getLocale() {
        return this.locale.get();
    }

    public synchronized void setLocale(Locale locale) {
        this.locale.set(locale);
    }

    @Override
    public boolean isBanned() {
        return this.server.getPlayerBanList().isBanned(this.getServerId());
    }

    @Override
    public BanEntry<UUID> ban(@Nullable String reason, @Nullable Instant expiration, @Nullable String source, boolean kick) {
        BanEntry<UUID> entry = this.server.getPlayerBanList().addBan(this.getServerId(), reason, expiration, source);
        if (kick && !entry.isExpired()) {
            this.kick(KickReason.PLAYER_BANNED, entry.reason());
        }

        return entry;
    }

    @Override
    public boolean isWhitelisted() {
        return this.server.isWhitelisted(this.getServerId());
    }

    @Override
    public void setWhitelisted(boolean value) {
        this.server.setWhitelisted(this.getServerId(), this.getName(), value);
    }

    @Override
    public OptionalLong getFirstPlayed() {
        return this.saveData.firstPlayed();
    }

    @Override
    public OptionalLong getLastPlayed() {
        return this.saveData.lastPlayed();
    }

    @Override
    public boolean hasPlayedBefore() {
        return this.getFirstPlayed().orElse(0) > 0;
    }

    @Override
    public GameMode getGameMode() {
        return Objects.requireNonNull(this.saveData.gameMode(), "Game mode has not been initialized");
    }

    @Override
    public GameMode getPreviousGameMode() {
        return this.previousGameMode;
    }

    @Override
    public void setGameMode(GameMode gameMode) {
        this.setGameMode(gameMode, PlayerGameModeChangeEvent.Cause.PLUGIN);
    }

    public boolean setGameMode(GameMode gameMode, PlayerGameModeChangeEvent.Cause cause) {
        if (this.getGameMode() == gameMode) {
            return false;
        }

        PlayerGameModeChangeEvent event = new PlayerGameModeChangeEvent(this, gameMode, cause, null);
        this.server.getEventManager().fire(event);

        if (event.isCancelled()) {
            return false;
        }

        this.previousGameMode = this.getGameMode();
        this.saveData.setGameMode(gameMode);

        if (gameMode == GameMode.CREATIVE || gameMode == GameMode.SPECTATOR) {
            this.foodData.reset();
            this.setAirTicks(400);
        }

        this.noPhysics = this.isSpectator();
        this.abilities = this.buildAbilitiesForGameMode(gameMode);

        if (this.spawned) {
            SetPlayerGameTypePacket localGameType = new SetPlayerGameTypePacket();
            localGameType.setGamemode(GameModeNetworkMapping.playerTypeId(this.getGameMode()));
            this.sendPacket(localGameType);

            UpdatePlayerGameTypePacket gameType = new UpdatePlayerGameTypePacket();
            gameType.setGameType(GameModeNetworkMapping.forPlayer(this.getGameMode()));
            gameType.setEntityId(this.getUniqueId());
            gameType.setTick(this.clientTick);
            CloudServer.broadcastPacket(this.getViewers(), gameType);

            this.syncClientAuthorityState(false);
        }

        boolean collisionAfter = gameMode != GameMode.SPECTATOR;
        boolean collisionChanged = this.data.getFlag(EntityFlag.HAS_COLLISION) != collisionAfter;
        this.data.setFlag(EntityFlag.HAS_COLLISION, collisionAfter);
        if (this.spawned && collisionChanged) {
            SetEntityDataPacket entityDataPk = new SetEntityDataPacket();
            entityDataPk.setRuntimeEntityId(this.getRuntimeId());
            entityDataPk.setTick(this.clientTick);
            entityDataPk.getMetadata().putAll(this.data.snapshot(FLAGS));
            this.putNetworkBounds(entityDataPk.getMetadata());
            this.sendPacket(entityDataPk);
            CloudServer.broadcastPacket(this.getViewers(), entityDataPk);
        }

        this.resetFallDistance();

        this.invManager.sendAllInventories();
        this.sendPacket(CloudItemRegistry.get().getCreativeContent());
        return true;
    }

    private CloudPlayerAbilities buildAbilitiesForGameMode(GameMode mode) {
        CloudPlayerAbilities abilities = new CloudPlayerAbilities(this);
        abilities.setAll(mode.getDefaultAbilities());
        abilities.set(Ability.OPERATOR_COMMANDS, this.isOp());
        abilities.set(Ability.TELEPORT, this.isOp());
        abilities.setWalkSpeed(this.getMovementSpeed());
        return abilities;
    }

    @Override
    public CloudPlayerAbilities getAbilities() {
        return abilities;
    }

    @Override
    public boolean isOp() {
        return this.server.isOperator(this.getServerId());
    }

    @Override
    public void setOp(boolean value) {
        this.server.setOperator(this.getServerId(), this.getName(), value);
    }

    @Override
    public boolean isPermissionSet(String name) {
        if (this.perm == null) return false;
        return this.perm.isPermissionSet(name);
    }

    @Override
    public boolean hasPermission(String name) {
        if (this.perm == null) return false;
        return this.perm.hasPermission(name);
    }

    @Override
    public Set<EffectivePermission> getEffectivePermissions() {
        if (this.perm == null) return Set.of();
        return this.perm.getEffectivePermissions();
    }

    @Override
    public PermissionAttachment addAttachment(PluginContainer plugin) {
        if (this.perm == null) throw new IllegalStateException("Player is offline");
        return this.perm.addAttachment(plugin);
    }

    @Override
    public PermissionAttachment addAttachment(PluginContainer plugin, Map<String, Boolean> permissions) {
        if (this.perm == null) {
            throw new IllegalStateException("Player is offline");
        }

        return this.perm.addAttachment(plugin, permissions);
    }

    @Override
    public PermissionAttachment addTemporaryAttachment(
            PluginContainer plugin, Map<String, Boolean> permissions, long ticks) {
        if (this.perm == null) {
            throw new IllegalStateException("Player is offline");
        }

        return this.perm.addTemporaryAttachment(plugin, permissions, ticks);
    }

    private void onPermissionsChanged() {
        if (this.spawned) {
            this.syncClientAuthorityState(true);
        }
    }

    public void refreshOperatorStatus() {
        this.abilities = buildAbilitiesForGameMode(this.getGameMode());
        CloudPermissible permissions = this.perm;
        if (permissions != null) {
            permissions.refresh();
        }
    }

    private void syncClientAuthorityState(boolean forceCommandData) {
        boolean commandStateChanged = this.syncClientCommandsEnabled();
        this.sendAdventureSettings();
        this.abilities.update();
        this.syncClientCommandData(commandStateChanged, forceCommandData);
    }

    private boolean syncClientCommandsEnabled() {
        boolean operatorAbilities = this.isOp();
        boolean enabledStateChanged = !this.clientCommandsEnabledStateSent
                || this.lastSentOperatorAbilities != operatorAbilities;

        if (enabledStateChanged) {
            this.sendClientCommandsEnabled();
            this.clientCommandsEnabledStateSent = true;
            this.lastSentOperatorAbilities = operatorAbilities;
        }

        return enabledStateChanged;
    }

    private void syncClientCommandData(boolean commandStateChanged, boolean forceCommandData) {
        if (this.spawned && this.initialized && (forceCommandData || commandStateChanged || !this.clientCommandDataSent)) {
            this.sendCommandData();
            this.clientCommandDataSent = true;
        }
    }

    private void sendClientCommandsEnabled() {
        SetCommandsEnabledPacket packet = new SetCommandsEnabledPacket();
        packet.setCommandsEnabled(true);
        this.sendPacket(packet);
    }

    private void sendAdventureSettings() {
        UpdateAdventureSettingsPacket packet = new UpdateAdventureSettingsPacket();
        packet.setNoPvM(false);
        packet.setNoMvP(false);
        packet.setImmutableWorld(this.isAdventure() || this.isSpectator());
        packet.setShowNameTags(false);
        packet.setAutoJump(true);
        this.sendPacket(packet);
    }

    public void sendCommandData() {
        this.sendPacket(CommandNetworkCompiler.compile(this.server.getCommandRegistry(), this));
    }

    public void processNetworkInput() {
        if (!this.isConnected()) {
            return;
        }

        this.inboundQueue.drain(this.packetHandler::handlePacket);
    }

    public void flushNetworkUpdates() {
        if (!this.isConnected() || !this.loggedIn) {
            return;
        }

        this.getChunkManager().sendQueued();

        int spawnThreshold = this.getLevel().getConfiguration().getChunkSending().getSpawnThreshold();
        if (!this.spawned && this.getChunkManager().getChunksSent() >= spawnThreshold) {
            this.completeInitialSpawn();
        }
    }

    public boolean sendPacket(BedrockPacket packet) {
        if (!this.connected) {
            return false;
        }

        PlayerPacketSendEvent event = new PlayerPacketSendEvent(this, packet);
        this.server.getEventManager().fire(event);
        if (event.isCancelled()) {
            return false;
        }

        if (log.isTraceEnabled() && !this.getServer().isIgnoredPacket(packet.getClass())) {
            log.trace("Outbound {}: {}", this.getName(), packet);
        }

        sendPacketInternal(packet);
        return true;
    }

    public void sendPacketInternal(BedrockPacket packet) {
        this.session.sendPacket(packet);
    }

    private void sendPacketImmediately(BedrockPacket packet) {
        if (!this.connected) {
            return;
        }

        this.session.sendPacketImmediately(packet);
    }

    public void transfer(InetSocketAddress address) {
        String hostName = address.getAddress().getHostAddress();
        int port = address.getPort();
        TransferPacket pk = new TransferPacket();
        pk.setAddress(hostName);
        pk.setPort(port);
        this.sendPacket(pk);
    }

    public boolean kick() {
        return this.kick("");
    }

    public boolean kick(String reason, boolean isAdmin) {
        return this.kick(KickReason.UNKNOWN, reason, isAdmin);
    }

    public boolean kick(String reason) {
        return kick(KickReason.UNKNOWN, reason);
    }

    public boolean kick(KickReason reason) {
        return this.kick(reason, true);
    }

    public boolean kick(KickReason reason, String reasonString) {
        return this.kick(reason, reasonString, true);
    }

    public boolean kick(KickReason reason, boolean isAdmin) {
        return this.kick(reason, reason.toString(), isAdmin);
    }

    public boolean kick(KickReason reason, String reasonString, boolean isAdmin) {
        PlayerKickEvent ev;
        this.server.getEventManager().fire(ev = new PlayerKickEvent(this, reason, reasonString, this.leaveMessage()));
        if (!ev.isCancelled()) {
            String message;
            if (isAdmin) {
                if (!this.isBanned()) {
                    message = "Kicked by admin." + (!reasonString.isEmpty() ? " Reason: " + reasonString : "");
                } else {
                    message = reasonString;
                }
            } else {
                if (reasonString.isEmpty()) {
                    message = "disconnectionScreen.noReason";
                } else {
                    message = reasonString;
                }
            }

            this.close(ev.getQuitMessage(), message);

            return true;
        }

        return false;
    }

    @Override
    public void kick(Component reason) {
        this.kick(reason, KickReason.UNKNOWN);
    }

    @Override
    public void kick(Component reason, KickReason cause) {
        String reasonString = PlainTextComponentSerializer.plainText().serialize(reason);
        this.kick(cause, reasonString, true);
    }

    @Override
    public void close() {
        this.close("");
    }

    public void close(String message) {
        this.close(message, "generic");
    }

    public void close(String message, String reason) {
        this.close(message, reason, true);
    }

    public void close(String message, String reason, boolean notify) {
        this.close(message.isEmpty() ? null : Component.text(message), reason, notify);
    }

    public void close(Component message) {
        this.close(message, "generic");
    }

    public void close(Component message, String reason) {
        this.close(message, reason, true);
    }

    public void close(@Nullable Component message, String reason, boolean notify) {
        if (this.connected && !this.isClosed()) {
            List.copyOf(this.bossBars).forEach(bossBar -> bossBar.removePlayer(this));
            this.connected = false;
            this.inboundQueue.close();
            PlayerQuitEvent ev = null;

            if (this.getName() != null && !this.getName().isEmpty()) {
                if (this.initialized) {
                    this.server.getEventManager().fire(ev = new PlayerQuitEvent(this, message, true, reason));
                }

                if (this.loggedIn && (ev == null || ev.getAutoSave())) {
                    this.save();
                }

                if (this.fishingHook != null) {
                    this.stopFishing();
                }
            }

            for (CloudPlayer player : new ArrayList<>(this.server.getOnlinePlayers().values())) {
                player.hiddenPlayers.remove(this.getServerId());
            }

            this.hiddenPlayers.clear();

            this.closeInventory(InventoryCloseEvent.Reason.DISCONNECT);

            this.getChunkManager().unpairReadyEntities();

            super.remove(EntityRemoveCause.PLAYER_QUIT);

            if (this.session.getPeer().isConnected()) {
                this.session.disconnect(notify ? reason : "");
            }

            if (this.loggedIn) {
                this.server.removeOnlinePlayer(this);
            }

            this.loggedIn = false;

            if (ev != null && !Objects.equals(this.username, "") && this.initialized && ev.getQuitMessage() != null) {
                this.server.broadcast(ev.getQuitMessage());
            }

            this.spawned = false;
            this.initialized = false;
            log.info(this.getServer().getLanguage().translate("cloudburst.player.logOut",
                    "§b" + (this.getName() == null ? "" : this.getName()) + "§r",
                    this.getLoggableAddress(),
                    this.getServer().getLanguage().translate(reason))
            );
            this.clearViewers();
            this.spawnLocation = null;
            this.respawnConfig = null;

        }

        if (this.perm != null) {
            this.perm.invalidate();
            this.perm = null;
        }

        this.getChunkManager().clear();

        this.server.removePlayer(this);
    }

    @Override
    public void remove(EntityRemoveCause cause) {
        Objects.requireNonNull(cause, "cause");
        this.close();
    }

    public Component leaveMessage() {
        return Component.translatable("multiplayer.player.left", displayName()).color(NamedTextColor.YELLOW);
    }

    @Nullable
    public RespawnConfig getRespawnConfig() {
        return this.respawnConfig;
    }

    public Location getSpawn() {
        if (this.spawnLocation != null && this.spawnLocation.getLevel() != null) {
            if (this.respawnConfig != null && this.respawnConfig.spawnType() != RespawnConfig.SpawnType.FORCED) {
                CloudLevel spawnLevel = this.respawnConfig.level();
                Vector3i pos = this.respawnConfig.pos();
                Block block = spawnLevel.getBlock(pos);
                BlockType type = block.getState().getType();
                if (!type.is(BlockTags.BEDS) && type != BlockTypes.RESPAWN_ANCHOR) {
                    this.clearPersonalSpawn();
                    return this.getServer().getDefaultLevel().getSafeSpawn();
                }
            }
            return this.spawnLocation;
        } else {
            return this.getServer().getDefaultLevel().getSafeSpawn();
        }
    }

    @Override
    public void setSpawn(Location location) {
        setSpawn(location, PlayerSetSpawnEvent.Cause.PLUGIN);
    }

    public void setSpawn(Location location, PlayerSetSpawnEvent.Cause cause) {
        checkNotNull(location, "location");
        Location previous = this.spawnLocation;
        PlayerSetSpawnEvent event = new PlayerSetSpawnEvent(this, cause, previous, location, true, false, null);
        this.server.getEventManager().fire(event);
        if (event.isCancelled()) {
            return;
        }

        Location finalLocation = event.getNewSpawn();
        if (finalLocation == null) {
            this.clearPersonalSpawn();
            return;
        }

        CloudLevel spawnLevel = (CloudLevel) finalLocation.getLevel();
        Vector3i spawnPosition = finalLocation.getPosition().toInt();
        this.respawnConfig = new RespawnConfig(spawnLevel, spawnPosition, finalLocation.getYaw(), RespawnConfig.SpawnType.FORCED);
        this.spawnLocation = finalLocation;
        this.sendPersonalSpawnPosition(spawnLevel, spawnPosition);
    }

    private void sendPersonalSpawnPosition(CloudLevel level, Vector3i position) {
        SetSpawnPositionPacket packet = new SetSpawnPositionPacket();
        packet.setSpawnType(SetSpawnPositionPacket.Type.PLAYER_SPAWN);
        packet.setBlockPosition(position);
        packet.setDimensionId(level.getDimension());
        this.sendPacket(packet);
    }

    public void clearSpawn() {
        if (this.spawnLocation == null) {
            this.clearPersonalSpawn();
            return;
        }

        Location previous = this.spawnLocation;
        PlayerSetSpawnEvent event = new PlayerSetSpawnEvent(this, PlayerSetSpawnEvent.Cause.RESET, previous, null, false, false, null);
        this.server.getEventManager().fire(event);
        if (event.isCancelled()) {
            return;
        }

        this.clearPersonalSpawn();
    }

    private void clearPersonalSpawn() {
        this.spawnLocation = null;
        this.respawnConfig = null;
        this.saveData.clearRespawn();
    }

    @Nullable
    public Location findRespawnPosition() {
        return this.findRespawnPosition(true);
    }

    @Nullable
    public Location findRespawnPosition(boolean consumeSpawnBlock) {
        if (this.respawnConfig == null) {
            return null;
        }

        if (this.respawnConfig.spawnType() == RespawnConfig.SpawnType.FORCED) {
            return this.spawnLocation;
        }

        CloudLevel spawnLevel = this.respawnConfig.level();
        Vector3i pos = this.respawnConfig.pos();
        float respawnYaw = this.respawnConfig.yaw();
        Block block = spawnLevel.getBlock(pos);
        BlockType type = block.getState().getType();

        if (this.respawnConfig.spawnType() == RespawnConfig.SpawnType.BED) {
            if (!type.is(BlockTags.BEDS)) {
                this.clearPersonalSpawn();
                return null;
            }
        } else if (this.respawnConfig.spawnType() == RespawnConfig.SpawnType.RESPAWN_ANCHOR) {
            if (type != BlockTypes.RESPAWN_ANCHOR) {
                this.clearPersonalSpawn();
                return null;
            }

            int charge = block.getState().ensureTrait(BlockTraits.RESPAWN_ANCHOR_CHARGE);
            if (charge <= 0) {
                this.clearPersonalSpawn();
                return null;
            }
        }

        Vector3f standUpPosition = this.respawnConfig.spawnType() == RespawnConfig.SpawnType.BED
                ? BedBlockHandlers.findStandUpPosition(spawnLevel, pos,
                BedBlockHandlers.getFacing(block.getState()), respawnYaw)
                : RespawnAnchorBlockHandlers.findStandUpPosition(spawnLevel, pos);
        if (standUpPosition == null) {
            this.clearPersonalSpawn();
            return null;
        }

        if (consumeSpawnBlock && this.respawnConfig.spawnType() == RespawnConfig.SpawnType.RESPAWN_ANCHOR) {
            int currentCharge = block.getState().ensureTrait(BlockTraits.RESPAWN_ANCHOR_CHARGE);
            int newCharge = currentCharge - 1;

            BlockState newAnchorState = block.getState().withTrait(BlockTraits.RESPAWN_ANCHOR_CHARGE, newCharge);
            spawnLevel.setBlockState(pos.getX(), pos.getY(), pos.getZ(), newAnchorState, false, true);

            LevelSoundEventPacket depleteSound = new LevelSoundEventPacket();
            depleteSound.setSound(SoundEvent.RESPAWN_ANCHOR_DEPLETE);
            depleteSound.setExtraData(-1);
            depleteSound.setIdentifier("");
            depleteSound.setPosition(Vector3f.from(pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f));
            depleteSound.setRelativeVolumeDisabled(false);
            depleteSound.setBabySound(false);
            this.sendPacket(depleteSound);

            if (newCharge <= 0) {
                this.clearPersonalSpawn();
            }
        }

        return Location.from(standUpPosition, respawnYaw, 0, spawnLevel);
    }

    private void setSpawnFromBed(Vector3i blockPos, Location location) {
        Location previous = this.spawnLocation;
        PlayerSetSpawnEvent event = new PlayerSetSpawnEvent(this, PlayerSetSpawnEvent.Cause.BED, previous, location, false, true, Component.translatable("tile.bed.respawnSet"));
        this.server.getEventManager().fire(event);
        if (event.isCancelled()) {
            return;
        }

        Location finalLocation = event.getNewSpawn();
        if (finalLocation == null) {
            this.clearPersonalSpawn();
            return;
        }

        this.spawnLocation = finalLocation;
        CloudLevel spawnLevel = (CloudLevel) finalLocation.getLevel();
        this.respawnConfig = new RespawnConfig(spawnLevel, blockPos, finalLocation.getYaw(), RespawnConfig.SpawnType.BED);
        this.sendPersonalSpawnPosition(spawnLevel, blockPos);

        if (event.willNotifyPlayer() && event.getNotification() != null) {
            this.sendMessage(event.getNotification());
        }
    }

    public boolean setSpawnFromAnchor(Vector3i blockPos, Location location) {
        if (this.respawnConfig != null
                && this.respawnConfig.spawnType() == RespawnConfig.SpawnType.RESPAWN_ANCHOR
                && this.respawnConfig.pos().equals(blockPos)
                && this.respawnConfig.level() == location.getLevel()) {
            return false;
        }

        Location previous = this.spawnLocation;
        PlayerSetSpawnEvent event = new PlayerSetSpawnEvent(
                this, PlayerSetSpawnEvent.Cause.RESPAWN_ANCHOR, previous, location,
                false, true, Component.translatable("tile.respawn_anchor.respawnSet")
        );
        this.server.getEventManager().fire(event);
        if (event.isCancelled()) {
            return false;
        }

        Location finalLocation = event.getNewSpawn();
        if (finalLocation == null) {
            this.clearPersonalSpawn();
            return false;
        }

        this.spawnLocation = finalLocation;
        CloudLevel spawnLevel = (CloudLevel) finalLocation.getLevel();
        this.respawnConfig = new RespawnConfig(spawnLevel, blockPos, 0f, RespawnConfig.SpawnType.RESPAWN_ANCHOR);
        this.sendPersonalSpawnPosition(spawnLevel, blockPos);

        if (event.willNotifyPlayer() && event.getNotification() != null) {
            this.sendMessage(event.getNotification());
        }

        return true;
    }

    public boolean sleepOn(Vector3i pos) {
        if (!this.isOnline()) {
            return false;
        }

        CloudLevel level = this.getLevel();
        int dim = level.getDimension();

        if (dim != CloudLevel.DIMENSION_OVERWORLD) {
            BlockState headState = level.getBlockState(pos.getX(), pos.getY(), pos.getZ());
            Direction facing = BedBlockHandlers.getFacing(headState);

            Vector3i footPos = Vector3i.from(
                    pos.getX() - facing.getStepX(),
                    pos.getY(),
                    pos.getZ() - facing.getStepZ()
            );

            Block sourceBlock = level.getBlock(pos);
            level.setBlockState(pos.getX(), pos.getY(), pos.getZ(), BlockStates.AIR, false, true);
            BlockState footState = level.getBlockState(footPos.getX(), footPos.getY(), footPos.getZ());
            if (footState.getType() == headState.getType()) {
                level.setBlockState(footPos.getX(), footPos.getY(), footPos.getZ(), BlockStates.AIR, false, true);
            }

            level.explode(Vector3f.from(pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f),
                    new ExplosionSettings(5, ExplosionBlockInteraction.DESTROY_WITH_DECAY, true, null, sourceBlock));
            return true;
        }

        boolean canSleep = level.isNightTime();
        if (!canSleep) {
            sendMessage(Component.translatable("tile.bed.noSleep"));
            return true;
        }

        Block bedBlock = level.getBlock(pos);
        try {
            if (bedBlock.getState().ensureTrait(BlockTraits.IS_OCCUPIED)) {
                sendMessage(Component.translatable("tile.bed.occupied"));
                return true;
            }
        } catch (Exception ignored) {
        }

        for (Entity p : level.getNearbyEntities(this, this.boundingBox.inflate(2, 1, 2))) {
            if (p instanceof CloudPlayer) {
                if (((CloudPlayer) p).sleeping != null && pos.distance(((CloudPlayer) p).sleeping) <= 0.1) {
                    return false;
                }
            }
        }

        PlayerBedEnterEvent ev;
        this.server.getEventManager().fire(ev = new PlayerBedEnterEvent(this, level.getBlock(pos)));
        if (ev.isCancelled()) {
            return false;
        }

        try {
            BlockState occupied = bedBlock.getState().withTrait(BlockTraits.IS_OCCUPIED, true);
            level.setBlockState(pos.getX(), pos.getY(), pos.getZ(), occupied, false, true);
        } catch (Exception ignored) {
        }

        this.sleeping = pos.clone();
        this.sleepStartedAt = level.getGameTime();
        this.teleportWithoutEvent(Location.from(pos.toFloat().add(0.5, 0.5, 0.5), this.getYaw(), this.getPitch(), level));
        this.data.set(BED_POSITION, pos);

        Location bedSpawnLoc = Location.from(pos.toFloat(), this.getYaw(), 0f, level);
        setSpawnFromBed(pos, bedSpawnLoc);

        return true;
    }

    public void stopSleep() {
        if (this.sleeping != null) {
            Block bedBlock = this.getLevel().getBlock(this.sleeping);
            this.server.getEventManager().fire(new PlayerBedLeaveEvent(this, bedBlock));

            try {
                BlockState clearedState = bedBlock.getState().withTrait(BlockTraits.IS_OCCUPIED, false);
                this.getLevel().setBlockState(
                        this.sleeping.getX(), this.sleeping.getY(), this.sleeping.getZ(),
                        BlockLayer.PRIMARY, clearedState, false, true
                );
            } catch (Exception ignored) {
            }

            this.sleeping = null;
            this.recalculateBoundingBox();
            this.data.set(BED_POSITION, Vector3i.ZERO);

            AnimatePacket pk = new AnimatePacket();
            pk.setRuntimeEntityId(this.getRuntimeId());
            pk.setAction(AnimatePacket.Action.WAKE_UP);
            this.sendPacket(pk);
        }
    }

    public boolean isSleeping() {
        return this.sleeping != null;
    }

    public boolean isSleepingLongEnough() {
        return this.isSleeping() && this.getLevel().getGameTime() - this.sleepStartedAt >= 100;
    }

    public boolean beginEndCredits() {
        if (this.showingCredits) {
            return true;
        }

        if (this.seenCredits) {
            return false;
        }

        this.seenCredits = true;
        this.showingCredits = true;
        ShowCreditsPacket packet = new ShowCreditsPacket();
        packet.setRuntimeEntityId(this.getRuntimeId());
        packet.setStatus(ShowCreditsPacket.Status.START_CREDITS);
        this.sendPacket(packet);
        return true;
    }

    public boolean isShowingEndCredits() {
        return this.showingCredits;
    }

    public void finishEndCredits() {
        if (!this.showingCredits) {
            return;
        }

        this.showingCredits = false;
        if (!EndPortals.returnPlayer(this)) {
            this.setPortalCooldown(0);
        }
    }

    @Override
    protected int getPortalCooldownDuration() {
        return 10;
    }

    @Override
    protected int getPortalTransitionTicks() {
        return isCreative() ? 0 : PORTAL_TRANSFER_TICKS;
    }

    @Override
    protected void tickPortalCooldown() {
        if (!changingDimension) {
            super.tickPortalCooldown();
        }
    }

    @Override
    protected void onInsidePortal() {
        if (this.isSpectator() || this.getVehicle() != null) {
            return;
        }

        if (this.isOnPortalCooldown()) {
            this.resetPortalCooldown();
        } else {
            this.inPortalTicks++;
        }
    }

    @Override
    protected void onLevelChanged(CloudLevel previousLevel, CloudLevel level) {
        int newDimension = level.getDimension();
        boolean dimensionChanged = newDimension != previousLevel.getDimension();

        if (dimensionChanged) {
            this.changingDimension = true;
            ChangeDimensionPacket changeDim = new ChangeDimensionPacket();
            changeDim.setDimension(newDimension);
            Vector3f targetPosition = this.respawnTarget == null ? this.getPosition().add(0, this.getBaseOffset(), 0) : this.respawnTarget.getPosition();
            changeDim.setPosition(targetPosition);
            changeDim.setRespawn(this.respawnTarget != null);
            this.sendPacketImmediately(changeDim);
        }

        SetSpawnPositionPacket spawnPosition = new SetSpawnPositionPacket();
        spawnPosition.setSpawnType(SetSpawnPositionPacket.Type.WORLD_SPAWN);
        Vector3f spawn = level.getSpawnLocation();
        spawnPosition.setBlockPosition(spawn.toInt());
        this.sendPacket(spawnPosition);

        this.getChunkManager().prepareRegion(this.getPosition());

        this.sendPacket(LevelTimePacketFactory.gameRules(level));
        level.sendTime(this);

        if (dimensionChanged) {
            PlayerActionPacket ack = new PlayerActionPacket();
            ack.setAction(PlayerActionType.DIMENSION_CHANGE_SUCCESS);
            ack.setRuntimeEntityId(this.getRuntimeId());
            ack.setBlockPosition(Vector3i.ZERO);
            ack.setResultPosition(Vector3i.ZERO);
            ack.setFace(0);
            this.sendPacket(ack);
        }
    }

    @Override
    protected void onChunkChanged(CloudChunk destination) {
        this.getChunkManager().pairReadyEntitiesIn(destination);
    }

    public void setChunkRadius(int chunkRadius) {
        this.getChunkManager().setChunkRadius(chunkRadius);
    }

    public void recordSubChunkServed(int chunkX, int chunkZ, int sectionsServed) {
        this.getChunkManager().recordSubChunkServed(chunkX, chunkZ, sectionsServed);
    }

    public boolean isChunkInView(int x, int z) {
        return this.getChunkManager().isChunkInView(x, z);
    }

    public boolean isChunkSent(int x, int z) {
        return this.getChunkManager().isChunkSent(x, z);
    }

    @Override
    protected boolean tickEntity(int currentTick, int elapsedTicks) {
        if (!this.loggedIn) {
            return false;
        }

        this.messageCounter = 2;

        if (!this.isAlive() && this.spawned) {
            ++this.deadTicks;
            if (this.deadTicks >= 10) {
                this.despawnFromAll();
            }
            return true;
        }

        if (this.spawned) {
            this.getInventoryManager().closeUnavailableScreen();
            this.reconcileGlidingState();
            if (this.spinAttackEndTick >= 0 && currentTick >= this.spinAttackEndTick) {
                this.stopSpinAttack();
            }
            this.getChunkManager().queueNewChunks(this.getPosition());

            if (!this.isSpectator()) {
                this.pickUpNearbyEntities();
            }

            this.entityBaseTick(elapsedTicks);
            this.tickTurtleHelmet();
            this.finishItemUse(currentTick);
            this.updateBlockingState();
            this.updateUnderwaterSound();
            this.updateScaffoldingFlags();

            if (this.getServer().getDifficulty() == Difficulty.PEACEFUL
                    && this.getLevel().getGameRules().get(GameRules.NATURAL_REGENERATION)) {
                if (this.getHealth() < this.getMaxHealth() && this.ticksLived % 20 == 0) {
                    this.heal(1, RegainReason.REGEN);
                }

                CloudPlayerFood foodData = this.getFoodData();
                if (foodData.getSaturation() < CloudPlayerFood.MAX_LEVEL && this.ticksLived % 20 == 0) {
                    foodData.setSaturation(foodData.getSaturation() + 1.0f);
                }

                if (foodData.getLevel() < 20 && this.ticksLived % 10 == 0) {
                    foodData.setLevel(foodData.getLevel() + 1);
                }
            }

            if (this.isOnFire() && currentTick % 10 == 0) {
                if (this.isCreative() && !this.isInsideOfFire()) {
                    this.extinguish();
                } else if (this.getLevel().isRaining()) {
                    if (this.getLevel().canBlockSeeSky(this.getPosition())) {
                        this.extinguish();
                    }
                }
            }

            if (!this.isSpectator()) {
                if (!this.isOnGround() && this.isGliding()) {
                    this.resetFallDistance();
                }

                if (this.isSurvival() || this.isAdventure()) {
                    this.foodData.update(elapsedTicks);
                }
            }
        }

        this.finishTeleportWhenReady();

        if (currentTick % 10 == 0) {
            this.updateInteractionPrompt();
        }

        if (this.spawned && currentTick % 100 == 0) {
            this.bossBars.forEach(bossBar -> bossBar.updatePosition(this));
        }

        this.flushEntityData();

        return true;
    }

    public void applyInputMovement() {
        if (!this.isAlive() || !this.spawned || this.getVehicle() != null || this.newPosition == null
                || this.teleportPosition != null || this.isSleeping()) {
            return;
        }

        Vector3f newPosition = this.newPosition;
        Vector3f currentPos = this.getPosition();
        float distanceSquared = newPosition.distanceSquared(currentPos);

        boolean revert = false;
        String revertReason = null;
        Vector3f authoritativePosition = null;
        Vector3f collisionResolvedPosition = null;

        float maxSpeedThreshold = this.server.getConfig().getMovement().getMaxSpeedThreshold();

        if (this.server.getConfig().getMovement().isStrictMovement()) {
            maxSpeedThreshold *= 0.5f;
        }

        boolean speedExempt = this.isCreative() || this.isSpectator() || newPosition.getY() - currentPos.getY() < -3.0f || (this.isGliding() && this.hasValidGlidingState());
        if (distanceSquared > maxSpeedThreshold && !speedExempt) {
            log.trace("[{}] movement reverted: claimed speed {} blocks/tick exceeds threshold {}", this.getName(),
                    String.format("%.2f", Math.sqrt(distanceSquared)),
                    String.format("%.2f", Math.sqrt(maxSpeedThreshold))
            );
            revert = true;
            revertReason = "speed";
            authoritativePosition = currentPos;
        }

        if (!revert && distanceSquared != 0) {
            float dx = newPosition.getX() - currentPos.getX();
            float dy = newPosition.getY() - currentPos.getY();
            float dz = newPosition.getZ() - currentPos.getZ();

            this.move(MovementType.PLAYER, dx, dy, dz);
            Vector3f resolvedPosition = this.getPosition();
            if (!resolvedPosition.equals(newPosition)) {
                collisionResolvedPosition = resolvedPosition;
            }

            if (this.newPosition == null) {
                return;
            }
        }

        Location from = Location.from(this.lastPosition, this.lastYaw, this.lastPitch, this.getLevel());
        Location to = this.getLocation();

        boolean firstMovement = this.firstMove;
        boolean redirectedMovement = false;

        double delta = Math.pow(this.lastPosition.getX() - to.getX(), 2)
                + Math.pow(this.lastPosition.getY() - to.getY(), 2)
                + Math.pow(this.lastPosition.getZ() - to.getZ(), 2);
        double deltaAngle = Math.abs(this.lastYaw - to.getYaw()) + Math.abs(this.lastPitch - to.getPitch());

        if (!revert && (delta > 0.0001d || deltaAngle > 1d)) {
            this.firstMove = false;
            this.lastPosition = to.getPosition();

            this.lastYaw = to.getYaw();
            this.lastPitch = to.getPitch();

            if (!firstMovement) {
                PlayerMoveEvent ev = new PlayerMoveEvent(this, from, to);
                this.server.getEventManager().fire(ev);

                if (!(revert = ev.isCancelled())) {
                    if (!to.equals(ev.getTo())) {
                        redirectedMovement = true;
                        this.teleportWithoutEvent(ev.getTo());
                    } else {
                        this.addMovement(this.getX(), this.getY() + getBaseOffset(), this.getZ(), this.getYaw(), this.getPitch(), this.getYaw());
                    }
                } else {
                    revertReason = "PlayerMoveEvent cancelled";
                }
            }
        }

        if (!revert && !firstMovement && !redirectedMovement && this.getVehicle() == null) {
            this.addMovementExhaustion(currentPos, this.getPosition());
        }

        if (revert) {
            this.motion = Vector3f.ZERO;
            this.lastPosition = from.getPosition();
            this.lastYaw = from.getYaw();
            this.lastPitch = from.getPitch();

            Vector3f correctedPos = authoritativePosition == null ? from.getPosition() : authoritativePosition;
            this.position = correctedPos;
            float radius = this.getWidth() / 2;
            this.boundingBox = new BoundingBox(
                    correctedPos.getX() - radius, correctedPos.getY(), correctedPos.getZ() - radius,
                    correctedPos.getX() + radius, correctedPos.getY() + this.getHeight(), correctedPos.getZ() + radius
            );
            this.lastPosition = correctedPos;

            log.debug("[{}] movement corrected: claimed {} corrected to {} ({})", this.getName(), newPosition, correctedPos, revertReason);
            sendMovementCorrection(correctedPos, this.clientTick);
        } else if (this.getPosition().equals(collisionResolvedPosition)) {
            float acceptanceThreshold = this.server.getConfig().getMovement().getPositionAcceptanceThreshold();
            if (newPosition.distance(collisionResolvedPosition) > acceptanceThreshold) {
                log.debug("[{}] movement corrected: claimed {} corrected to {} (collision)",
                        this.getName(), newPosition, collisionResolvedPosition);
                sendMovementCorrection(collisionResolvedPosition, this.clientTick);
            }
        }

        if (!revert) {
            this.updateEnchantmentLocationEffects();
        }

        this.newPosition = null;
    }

    public Vector3f getNextPosition() {
        return this.newPosition != null ? this.newPosition : this.getPosition();
    }

    public void acceptInputMotion(Vector3f motion) {
        this.motion = motion;
    }

    @Override
    public void addMovement(double x, double y, double z, double yaw, double pitch, double headYaw) {
        MovePlayerPacket packet = new MovePlayerPacket();
        packet.setRuntimeEntityId(this.getRuntimeId());
        packet.setPosition(Vector3f.from(x, y, z));
        packet.setRotation(Vector3f.from(pitch, yaw, headYaw));
        packet.setOnGround(this.isNetworkOnGround());
        packet.setMode(MovePlayerPacket.Mode.NORMAL);
        packet.setTick(this.clientTick);

        CloudServer.broadcastPacket(this.getViewers(), packet);
    }

    @Override
    public boolean setMotion(Vector3f motion) {
        if (super.setMotion(motion)) {
            if (this.getChunk() != null) {
                this.addMotion(this.getMotion());
                SetEntityMotionPacket packet = new SetEntityMotionPacket();
                packet.setRuntimeEntityId(this.getRuntimeId());
                packet.setMotion(motion);
                packet.setTick(this.server.getTick());
                this.sendPacket(packet);
            }

            return true;
        }

        return false;
    }

    public void setPredictedMotion(Vector3f motion) {
        super.setMotion(motion);
    }

    private void addMovementExhaustion(Vector3f from, Vector3f to) {
        float dx = to.getX() - from.getX();
        float dy = to.getY() - from.getY();
        float dz = to.getZ() - from.getZ();

        float horizontalDistance = (float) Math.sqrt(dx * dx + dz * dz);
        boolean swimming = this.isSwimming();

        if (swimming || this.isInsideOfWater()) {
            float distance = Math.round((float) Math.sqrt(dx * dx + dy * dy + dz * dz) * 100.0f) / 100.0f;
            if (distance > 0) {
                this.addExhaustion(distance * 0.01f, swimming ? ExhaustionReason.SWIM : ExhaustionReason.WALK_UNDERWATER);
            }
        } else if (this.isTouchingWater()) {
            float distance = Math.round(horizontalDistance * 100.0f) / 100.0f;
            if (distance > 0) {
                this.addExhaustion(distance * 0.01f, ExhaustionReason.WALK_ON_WATER);
            }
        } else if (this.isOnGround()) {
            float distance = Math.round(horizontalDistance * 100.0f) / 100.0f;
            if (distance > 0) {
                if (this.isSprinting()) {
                    this.addExhaustion(distance * 0.1f, ExhaustionReason.SPRINT);
                } else {
                    this.addExhaustion(0, this.isSneaking() ? ExhaustionReason.CROUCH : ExhaustionReason.WALK);
                }
            }
        }
    }

    @Override
    public float getBaseMovementSpeed() {
        return this.baseMovementSpeed;
    }

    @Override
    public void setBaseMovementSpeed(float speed) {
        if (!Float.isFinite(speed) || speed < 0f) {
            throw new IllegalArgumentException("Base movement speed must be finite and non-negative");
        }
        this.baseMovementSpeed = speed;
        this.syncMovementSpeed();
    }

    @Override
    public float getMovementSpeed() {
        return effectiveMovementSpeed(this.getBaseMovementSpeed(), this.isSprinting(),
                this.getPotionAmplifier(EffectTypes.SPEED), this.getPotionAmplifier(EffectTypes.SLOWNESS));
    }

    private static float effectiveMovementSpeed(float baseSpeed, boolean sprinting, int speedAmplifier, int slownessAmplifier) {
        double speed = baseSpeed * (sprinting ? 1.3d : 1d);
        speed *= 1d + 0.2d * (speedAmplifier + 1d);
        speed *= Math.max(0d, 1d - 0.15d * (slownessAmplifier + 1d));
        return (float) Math.min(speed, Float.MAX_VALUE);
    }

    private void syncMovementSpeed() {
        if (this.abilities == null) {
            return;
        }

        float speed = this.getMovementSpeed();
        this.abilities.setWalkSpeed(speed);
        if (this.spawned) {
            this.sendAttributeUpdate(NetworkEntityAttribute.MOVEMENT_SPEED.encode(speed));
            this.abilities.update();
        }
    }

    @Override
    protected void onPotionEffectChanged(EffectType type) {
        if (type == EffectTypes.SPEED || type == EffectTypes.SLOWNESS) {
            this.syncMovementSpeed();
        }
    }

    @Override
    public void setSprinting(boolean sprinting) {
        if (this.isSprinting() != sprinting) {
            super.setSprinting(sprinting);
            this.syncMovementSpeed();
        }
    }

    @Override
    protected void onMountComplete(Entity vehicle) {
        this.newPosition = null;
    }

    public void sendMovementCorrection(Vector3f authoritativePos, long tick) {
        CorrectPlayerMovePredictionPacket correction = new CorrectPlayerMovePredictionPacket();
        correction.setPredictionType(PredictionType.PLAYER);
        correction.setPosition(authoritativePos.add(0, getBaseOffset(), 0));
        correction.setDelta(this.getMotion());
        correction.setVehicleRotation(Vector2f.ZERO);
        correction.setVehicleAngularVelocity(0.0f);
        correction.setOnGround(this.isNetworkOnGround());
        correction.setTick(tick);
        this.sendPacket(correction);
    }

    @Override
    public void sendAuthoritativeDisplacement() {
        super.sendAuthoritativeDisplacement();
        this.sendMovementCorrection(this.getPosition(), this.clientTick);
    }

    public void sendPosition(Vector3f pos, double yaw, double pitch, MovePlayerPacket.Mode mode) {
        MovePlayerPacket packet = new MovePlayerPacket();
        packet.setRuntimeEntityId(this.getRuntimeId());
        packet.setPosition(pos.add(0, getBaseOffset(), 0));
        packet.setRotation(Vector3f.from(pitch, yaw, yaw));
        packet.setOnGround(this.isNetworkOnGround());
        packet.setMode(mode);
        packet.setTick(this.clientTick);
        if (mode == MovePlayerPacket.Mode.TELEPORT) {
            packet.setTeleportationCause(MovePlayerPacket.TeleportationCause.BEHAVIOR);
        }

        this.sendPacket(packet);
    }

    private boolean isNetworkOnGround() {
        return this.isOnGround() && !this.isGliding();
    }

    public boolean isDescendingScaffolding() {
        return this.isSneaking() || this.scaffoldDescentInput;
    }

    private void updateScaffoldingFlags() {
        Vector3i feet = this.getPosition().toInt();
        boolean canClimb = this.getVehicle() == null && !this.isSpectator() && !this.getAbilities().get(Ability.FLYING);
        boolean inside = canClimb && this.level.getBlockState(feet).getType() == BlockTypes.SCAFFOLDING;
        boolean above = canClimb && !inside && this.isOnGround()
                && this.level.getBlockState(feet.sub(0, 1, 0)).getType() == BlockTypes.SCAFFOLDING;

        this.data.setFlag(EntityFlag.IN_SCAFFOLDING, inside);
        this.data.setFlag(EntityFlag.IN_ASCENDABLE_BLOCK, inside);
        this.data.setFlag(EntityFlag.OVER_DESCENDABLE_BLOCK, inside || above);
        this.data.setFlag(EntityFlag.OVER_SCAFFOLDING, (inside || above) && this.isDescendingScaffolding());
    }

    private void tickTurtleHelmet() {
        PotionEffect effect = ArmorItemHandlers.getTurtleHelmetEffect(this, this.isInsideOfWater());
        if (effect != null) {
            this.addPotionEffect(effect, null, PotionEffectCause.TURTLE_HELMET);
        }
    }

    private void updateUnderwaterSound() {
        boolean underwater = this.isInsideOfWater();
        if (this.wasUnderwater == underwater) {
            return;
        }
        this.wasUnderwater = underwater;

        LevelSoundEventPacket packet = new LevelSoundEventPacket();
        packet.setSound(underwater ? SoundEvent.AMBIENT_UNDERWATER_ENTER : SoundEvent.AMBIENT_UNDERWATER_EXIT);
        packet.setPosition(this.getPosition());
        packet.setExtraData(-1);
        packet.setIdentifier(EntityTypes.PLAYER.getId().toString());
        packet.setEntityUniqueId(this.getUniqueId());
        this.sendPacket(packet);
    }

    public void tryStartGliding() {
        if (this.hasGlideRequest()) {
            return;
        }

        if (!this.hasValidGlidingState()) {
            this.sendFlags(this);
            return;
        }

        if (this.getAbilities().get(Ability.FLYING)) {
            this.getAbilities().set(Ability.FLYING, false);
            this.getAbilities().update();
        }

        this.sendArmorEquipmentToViewers();
        this.setGlidingWithEvent(true);
    }

    public void stopGliding() {
        if (!this.hasGlideRequest()) {
            this.sendFlags(this);
            return;
        }

        this.setGlidingWithEvent(false);
    }

    private void reconcileGlidingState() {
        if (this.hasGlideRequest() && !this.hasValidGlidingState()) {
            this.setGlidingWithEvent(false);
        }
    }

    private void setGlidingWithEvent(boolean gliding) {
        PlayerToggleGlideEvent glideEvent = new PlayerToggleGlideEvent(this, gliding);
        this.server.getEventManager().fire(glideEvent);
        if (glideEvent.isCancelled()) {
            this.sendFlags(this);
            return;
        }

        this.setGliding(gliding);
    }

    private boolean hasValidGlidingState() {
        if (this.isOnGround() || this.getVehicle() != null
                || this.isInsideOfWater() || this.hasPotionEffect(EffectTypes.LEVITATION)) {
            return false;
        }

        return this.hasUsableElytraEquipped();
    }

    private boolean hasUsableElytraEquipped() {
        ItemStack chestplate = this.getArmor().getChestplate();
        if (chestplate.getType() != ItemTypes.ELYTRA) {
            return false;
        }

        IntItemHandler maxDamageHandler = CloudItemRegistry.get().requireComponent(chestplate.getType(), ItemBehaviors.GET_MAX_DAMAGE);
        if (maxDamageHandler == null) {
            return true;
        }

        int maxDamage = maxDamageHandler.execute(chestplate);
        return maxDamage <= 0 || chestplate.getDamage() < maxDamage - 1;
    }

    public void beginFireworkGlideBoost(long fireworkRuntimeId) {
        if (this.attachedFireworkRockets.add(fireworkRuntimeId)) {
            this.sendGlideBoost(BEDROCK_FIREWORK_GLIDE_BOOST_DURATION);
        }
    }

    public void endFireworkGlideBoost(long fireworkRuntimeId) {
        if (this.attachedFireworkRockets.remove(fireworkRuntimeId) && this.attachedFireworkRockets.isEmpty()) {
            this.sendGlideBoost(0);
        }
    }

    private void sendGlideBoost(int duration) {
        MovementEffectPacket packet = new MovementEffectPacket();
        packet.setEntityRuntimeId(this.getRuntimeId());
        packet.setEffectType(MovementEffectType.GLIDE_BOOST);
        packet.setDuration(duration);
        packet.setTick(this.clientTick);
        this.sendPacket(packet);
    }

    @Override
    protected boolean isTeleportUnavailable(Location location) {
        return !this.isOnline() || super.isTeleportUnavailable(location);
    }

    @Override
    protected @Nullable Location requestTeleportDestination(Location from, Location to, TeleportCause cause) {
        PlayerTeleportEvent event = new PlayerTeleportEvent(this, from, to, cause);
        this.server.getEventManager().fire(event);
        return event.isCancelled() ? null : event.getTo();
    }

    @Override
    public boolean teleportWithoutEvent(Location location) {
        Objects.requireNonNull(location, "location");
        if (this.isTeleportBlocked(location)) {
            return false;
        }

        boolean hasPendingTeleport = this.teleportPosition != null;
        if (!hasPendingTeleport) {
            this.getChunkManager().unpairReadyEntities();
        }

        if (super.teleportWithoutEvent(location)) {
            if (!this.isOnline()) {
                return true;
            }
            this.closeInventory(InventoryCloseEvent.Reason.TELEPORT);

            this.teleportPosition = this.getPosition();
            this.getChunkManager().queueNewChunks(this.teleportPosition);
            this.teleportAcknowledged = false;
            this.pendingTeleportEntityViewRefresh = true;
            this.sendPosition(this.getPosition(), this.getYaw(), this.getPitch(), MovePlayerPacket.Mode.TELEPORT);

            this.finishTeleportWhenReady();

            this.resetFallDistance();
            this.newPosition = null;

            this.getLevel().sendWeather(this);
            this.getLevel().sendTime(this);
            return true;
        }

        if (!hasPendingTeleport) {
            this.getChunkManager().pairReadyEntities();
        }

        return false;
    }

    public boolean teleportForRespawn(Location location) {
        Objects.requireNonNull(location, "location");
        if (!this.isOnline()) {
            return false;
        }

        this.getChunkManager().unpairReadyEntities();
        this.respawnTarget = location;
        boolean teleported;
        try {
            teleported = super.teleportWithoutEvent(location);
        } finally {
            this.respawnTarget = null;
        }

        if (!teleported) {
            this.getChunkManager().pairReadyEntities();
            return false;
        }

        this.teleportPosition = null;
        this.teleportAcknowledged = false;
        this.pendingTeleportEntityViewRefresh = false;
        this.newPosition = null;

        this.getChunkManager().queueNewChunks(this.getPosition());
        this.getChunkManager().resendReadyEntities();
        this.resetFallDistance();
        this.getLevel().sendWeather(this);
        this.getLevel().sendTime(this);

        return true;
    }

    public boolean acknowledgeTeleport(Vector3f clientPosition) {
        if (this.teleportPosition == null) {
            return true;
        }

        if (clientPosition.distanceSquared(this.teleportPosition) > TELEPORT_ACK_DISTANCE_TOLERANCE_SQUARED) {
            return false;
        }

        this.teleportAcknowledged = true;
        this.finishTeleportWhenReady();
        return this.teleportPosition == null;
    }

    private void finishTeleportWhenReady() {
        if (this.teleportPosition != null) {
            if (this.pendingTeleportEntityViewRefresh && !this.teleportAcknowledged) {
                return;
            }

            long teleportChunk = CloudChunk.key(this.teleportPosition.getFloorX() >> 4, this.teleportPosition.getFloorZ() >> 4);
            if (!this.getChunkManager().isChunkSent(teleportChunk)) {
                return;
            }

            this.refreshViewers();
            this.refreshEntityViewAfterTeleport();
            this.teleportPosition = null;
            this.teleportAcknowledged = false;
        }
    }

    private void refreshEntityViewAfterTeleport() {
        if (!this.pendingTeleportEntityViewRefresh) {
            return;
        }

        this.pendingTeleportEntityViewRefresh = false;
        this.getChunkManager().resendReadyEntities();
    }

    @Override
    public boolean canSee(Player target) {
        return !this.hiddenPlayers.containsKey(target.getServerId());
    }

    @Override
    public void hidePlayer(PluginContainer plugin, Player target) {
        Objects.requireNonNull(plugin, "plugin");
        CloudPlayer player = this.requireVisibilityTarget(target);
        if (player == this) {
            return;
        }

        Set<PluginContainer> plugins = this.hiddenPlayers.computeIfAbsent(player.getServerId(), _ -> new HashSet<>());
        if (plugins.add(plugin) && plugins.size() == 1) {
            player.despawnFrom(this);
            this.server.removePlayerListData(player.getServerId(), new CloudPlayer[]{this});
        }
    }

    @Override
    public void showPlayer(PluginContainer plugin, Player target) {
        Objects.requireNonNull(plugin, "plugin");
        CloudPlayer player = this.requireVisibilityTarget(target);
        UUID targetId = player.getServerId();
        Set<PluginContainer> plugins = this.hiddenPlayers.get(targetId);
        if (plugins == null || !plugins.remove(plugin) || !plugins.isEmpty()) {
            return;
        }

        this.hiddenPlayers.remove(targetId);
        if (player.isOnline()) {
            player.spawnTo(this);
            if (!player.hasViewer(this)) {
                this.server.updatePlayerListData(targetId, player.getUniqueId(),
                        BedrockLegacyTextSerializer.getInstance().serialize(player.displayName()),
                        player.getSerializedSkin(), player.getXuid(), new CloudPlayer[]{this}
                );
            }
        }
    }

    private CloudPlayer requireVisibilityTarget(Player target) {
        if (!(target instanceof CloudPlayer player) || player.getServer() != this.server) {
            throw new IllegalArgumentException("Target must be a player from the same server");
        }
        return player;
    }

    @Override
    public void spawnTo(CloudPlayer player) {
        if (this.spawned && player.spawned && this.isAlive() && player.isAlive() &&
                player.getLevel() == this.getLevel() && player.canSee(this) && !this.isSpectator() &&
                this.canSpawnTo(player)) {
            if (this == player || this.hasViewer(player)) {
                return;
            }

            super.spawnTo(player);
            if (!this.hasViewer(player)) {
                return;
            }

            player.sendPacket(buildArmorEquipmentPacket());
            player.sendPacket(this.createEquipmentPacket(this.getOffhand().getOffhandItem(), ContainerId.OFFHAND, 0));
        }
    }

    @Override
    protected BedrockPacket createAddEntityPacket() {
        AddPlayerPacket packet = new AddPlayerPacket();
        packet.setUuid(this.getServerId());
        packet.setUsername(this.getName());
        packet.setUniqueEntityId(this.getUniqueId());
        packet.setRuntimeEntityId(this.getRuntimeId());
        packet.setPosition(this.getPosition());
        packet.setMotion(this.getMotion());
        packet.setRotation(Vector3f.from(this.getPitch(), this.getYaw(), this.getYaw()));
        packet.setHand(ItemStackPacketCodec.toNetwork(this.getInventory().getSelectedItem()));
        packet.setPlatformChatId("");
        packet.setDeviceId("");
        packet.setGameType(GameModeNetworkMapping.forPlayer(this.getGameMode()));
        packet.setCommandPermission(CommandPermission.ANY);
        packet.setPlayerPermission(PlayerPermission.MEMBER);
        packet.getAbilityLayers().add(this.abilities.buildBaseLayer());
        packet.getMetadata().putAll(this.getData().snapshot());
        return packet;
    }

    public void trackBossBar(CloudBossBar bossBar) {
        this.bossBars.add(bossBar);
    }

    public void untrackBossBar(CloudBossBar bossBar) {
        this.bossBars.remove(bossBar);
    }

    public void refreshBossBars() {
        if (!this.changingDimension) {
            this.bossBars.forEach(bossBar -> bossBar.refresh(this));
        }
    }

    @Override
    public PlayerInventoryView getInventory() {
        return this.inventory;
    }

    public CloudContainer getContainer() {
        return this.inventory.getContainer();
    }

    public PlayerInventoryManager getInventoryManager() {
        return invManager;
    }

    @Override
    public EnderChestView getEnderChest() {
        return this.enderChest;
    }

    @Override
    public ArmorView getArmor() {
        return this.armor;
    }

    @Override
    public OffhandView getOffhand() {
        return this.offhand;
    }

    public boolean hasOpenContainer() {
        return this.invManager.getOpenContainer() != null;
    }

    @Override
    public HudScreen getHudScreen() {
        return this.invManager.getDefaultScreen();
    }

    @Override
    public InventoryScreen getOpenInventory() {
        return this.invManager.getOpenContainer();
    }

    @Override
    public InventoryScreen getCurrentScreen() {
        return this.invManager.getScreen();
    }

    @Override
    public PlayerInventoryScreen getInventoryScreen() {
        CloudInventoryScreen current = this.invManager.getOpenContainer();
        return (current instanceof PlayerInventoryScreen pis) ? pis : null;
    }

    @Override
    public void openInventory(InventoryScreen view) {
        if (!(view instanceof CloudInventoryScreen)) {
            throw new IllegalArgumentException("View must be a CloudInventoryScreen, got: " + view.getClass().getName());
        }
        this.invManager.openScreen((CloudInventoryScreen) view);
    }

    @Override
    public void openContainer(Block block) {
        if (hasOpenContainer()) return;
        if (!block.requireComponent(BlockComponents.CAN_BE_USED).execute(block, this)) {
            throw new IllegalArgumentException("Block is not a container: " + block.getState().getType().getId());
        }
        block.requireComponent(BlockComponents.USE).execute(block, this, Direction.DOWN, ItemStack.EMPTY);
    }

    @Override
    public void openContainer(BlockEntity blockEntity) {
        if (hasOpenContainer()) return;
        openContainer(blockEntity.getBlock());
    }

    @Override
    public void closeInventory() {
        closeInventory(InventoryCloseEvent.Reason.PLUGIN);
    }

    public void closeInventory(InventoryCloseEvent.Reason reason) {
        this.invManager.closeScreen(reason);
    }

    @Override
    public VirtualChestScreen createVirtualChest(Component title) {
        return new CloudVirtualChestScreen(this, title);
    }

    @Override
    public VirtualDoubleChestScreen createVirtualDoubleChest(Component title) {
        return new CloudVirtualDoubleChestScreen(this, title);
    }

    @Override
    public VirtualHopperScreen createVirtualHopper(Component title) {
        return new CloudVirtualHopperScreen(this, title);
    }

    public void handleClientContainerClose(ContainerClosePacket packet) {
        this.invManager.handleWindowClose(packet);
    }

    @Override
    public void onInventoryAdded(Container inventory) {
    }

    @Override
    public void onInventoryRemoved(Container inventory) {
    }

    @Override
    public void onInventorySlotChange(Container inventory, int slot) {
        if (!this.spawned) {
            return;
        }

        if (this.inventoryRequestQueue.deferInventorySlotUpdate(inventory, slot)) {
            return;
        }

        if (this.invManager.getScreen().sendUIContainerSlot(inventory, slot)) {
            return;
        }

        if (inventory == this.offhand.getContainer()) {
            this.onInventoryContentsChange(inventory);
            return;
        }

        int containerId = getContainerId(inventory);
        if (containerId == ContainerId.NONE) {
            return;
        }

        ItemStack itemStack = inventory.getItem(slot);
        InventorySlotPacket packet = new InventorySlotPacket();
        packet.setSlot(slot);
        packet.setItem(ItemStackPacketCodec.toNetworkNetId(itemStack));
        packet.setContainerId(containerId);
        this.sendPacket(packet);

        if (inventory == this.armor.getContainer()) {
            this.sendArmorEquipmentToViewers();
        } else if (inventory == this.container && slot == this.getSelectedHotbarSlot()) {
            this.sendHandEquipmentToViewers();
        }
    }

    @Override
    public void onInventoryContentsChange(Container inventory) {
        if (!this.spawned) {
            return;
        }

        if (this.inventoryRequestQueue.deferInventoryContentsUpdate(inventory)) {
            return;
        }

        if (this.invManager.getScreen().sendUIContainerContents(inventory)) {
            return;
        }

        int containerId = getContainerId(inventory);
        if (containerId == ContainerId.NONE) {
            return;
        }

        List<ItemData> contents = new ArrayList<>();
        for (ItemStack item : inventory.getContents()) {
            contents.add(ItemStackPacketCodec.toNetworkNetId(item));
        }

        InventoryContentPacket packet = new InventoryContentPacket();
        packet.setContents(contents);
        packet.setContainerId(containerId);
        this.sendPacket(packet);

        if (inventory == this.armor.getContainer()) {
            this.sendArmorEquipmentToViewers();
        } else if (inventory == this.offhand.getContainer()) {
            this.sendOffhandEquipmentToViewers();
        } else if (inventory == this.container) {
            this.sendHandEquipmentToViewers();
        }
    }

    @Override
    public void onInventoryDataChange(Container inventory, int property, int value) {
        int containerId = getContainerId(inventory);
        if (containerId == ContainerId.NONE) {
            return;
        }
        ContainerSetDataPacket packet = new ContainerSetDataPacket();
        packet.setWindowId((byte) containerId);
        packet.setProperty(property);
        packet.setValue(value);
        this.sendPacket(packet);
    }

    private int getContainerId(Container inventory) {
        if (inventory == this.container) {
            return ContainerId.INVENTORY;
        }

        if (inventory == this.armor.getContainer()) {
            return ContainerId.ARMOR;
        }

        if (inventory == this.offhand.getContainer()) {
            return ContainerId.OFFHAND;
        }

        return this.invManager.getWindowId(inventory);
    }

    public void sendInventoryContents() {
        this.onInventoryContentsChange(this.container);
    }

    @Override
    public void setSelectedHotbarSlot(int slot) {
        super.setSelectedHotbarSlot(slot);
        this.sendHotbarSelection();
    }

    public void acknowledgeHotbarSlot(int slot) {
        super.setSelectedHotbarSlot(slot);
    }

    @Override
    protected void onSelectedHotbarSlotChanged() {
        this.setUsingItem(false);
        if (this.spawned) {
            this.sendHandEquipmentToViewers();
        }
    }

    public void sendHotbarSelection() {
        PlayerHotbarPacket packet = new PlayerHotbarPacket();
        packet.setSelectedHotbarSlot(this.getSelectedHotbarSlot());
        packet.setContainerId(ContainerId.INVENTORY);
        packet.setSelectHotbarSlot(true);
        this.sendPacket(packet);
    }

    public void sendHeldItemSlot() {
        this.onInventorySlotChange(this.container, this.getSelectedHotbarSlot());
    }

    private MobArmorEquipmentPacket buildArmorEquipmentPacket() {
        MobArmorEquipmentPacket packet = new MobArmorEquipmentPacket();
        packet.setRuntimeEntityId(this.getRuntimeId());
        packet.setHelmet(ItemStackPacketCodec.toNetwork(this.armor.getHelmet()));
        packet.setChestplate(ItemStackPacketCodec.toNetwork(this.armor.getChestplate()));
        packet.setLeggings(ItemStackPacketCodec.toNetwork(this.armor.getLeggings()));
        packet.setBoots(ItemStackPacketCodec.toNetwork(this.armor.getBoots()));
        packet.setBody(ItemData.AIR);
        return packet;
    }

    private MobEquipmentPacket createEquipmentPacket(ItemStack item, int containerId, int slot) {
        MobEquipmentPacket packet = new MobEquipmentPacket();
        packet.setRuntimeEntityId(this.getRuntimeId());
        packet.setItem(ItemStackPacketCodec.toNetwork(item));
        packet.setContainerId(containerId);
        packet.setInventorySlot(slot);
        packet.setHotbarSlot(containerId == ContainerId.OFFHAND ? -1 : slot);
        return packet;
    }

    private void sendArmorEquipmentToViewers() {
        MobArmorEquipmentPacket armorPacket = buildArmorEquipmentPacket();
        for (CloudPlayer viewer : this.getViewers()) {
            viewer.sendPacket(armorPacket);
        }
    }

    private void sendHandEquipmentToViewers() {
        CloudServer.broadcastPacket(this.getViewers(), this.createEquipmentPacket(this.getInventory().getSelectedItem(), ContainerId.INVENTORY, this.getSelectedHotbarSlot()));
    }

    private void sendOffhandEquipmentToViewers() {
        CloudServer.broadcastPacket(this.getViewers(), this.createEquipmentPacket(this.getOffhand().getOffhandItem(), ContainerId.OFFHAND, 0));
    }

    @Override
    public int getItemCooldown(ItemType itemType) {
        Objects.requireNonNull(itemType, "itemType");
        int remaining = this.itemCooldowns.getOrDefault(itemType, 0) - this.server.getTick();
        if (remaining <= 0) {
            this.itemCooldowns.remove(itemType);
            return 0;
        }
        return remaining;
    }

    @Override
    public void setItemCooldown(ItemType itemType, int ticks) {
        Objects.requireNonNull(itemType, "itemType");
        if (ticks < 0) {
            throw new IllegalArgumentException("ticks must not be negative");
        }

        if (ticks == 0) {
            this.itemCooldowns.remove(itemType);
        } else {
            this.itemCooldowns.put(itemType, this.server.getTick() + ticks);
        }

        PlayerStartItemCooldownPacket packet = new PlayerStartItemCooldownPacket();
        packet.setItemCategory(itemType == ItemTypes.SHIELD || itemType == ItemTypes.GOAT_HORN ? itemType.getId().getName() : itemType.getId().toString());
        packet.setCooldownDuration(ticks);
        this.sendPacket(packet);
    }

    public boolean isUsingItem() {
        return this.data.getFlag(USING_ITEM) && this.startAction > -1;
    }

    public void setUsingItem(boolean value) {
        this.startAction = value ? this.server.getTick() : -1;
        this.data.setFlag(USING_ITEM, value);
        if (!value) {
            this.activeUseStack = null;
            this.activeUseSlot = -1;
            this.itemUseCompleteTick = -1;
        }
        this.updateBlockingState();
    }

    public void startUsingItem(ItemStack item, int durationTicks) {
        Objects.requireNonNull(item, "item");
        if (item.isEmpty()) {
            throw new IllegalArgumentException("item must not be empty");
        }

        if (durationTicks <= 0) {
            throw new IllegalArgumentException("durationTicks must be positive");
        }

        this.activeUseStack = item;
        this.activeUseSlot = this.getSelectedHotbarSlot();
        this.itemUseCompleteTick = this.server.getTick() + durationTicks;
        this.setUsingItem(true);
    }

    public void startUsingItem(ItemStack item) {
        Objects.requireNonNull(item, "item");
        if (item.isEmpty()) {
            throw new IllegalArgumentException("item must not be empty");
        }

        this.activeUseStack = item;
        this.activeUseSlot = this.getSelectedHotbarSlot();
        this.itemUseCompleteTick = -1;
        this.setUsingItem(true);
    }

    private void finishItemUse(int currentTick) {
        if (this.activeUseStack == null) {
            return;
        }

        if (!this.activeUseMatchesCurrentSlot()) {
            this.setUsingItem(false);
            return;
        }

        ItemStack activeItem = this.getInventory().getItem(this.activeUseSlot);
        UseTickHandler tickHandler = CloudItemRegistry.get().getComponent(activeItem.getType(), ItemBehaviors.USE_TICK);
        if (tickHandler != null) {
            int slot = this.activeUseSlot;
            UseTickResult result = Objects.requireNonNull(
                    tickHandler.execute(activeItem, this, Math.max(0, currentTick - this.startAction)),
                    "use tick result"
            );

            ItemStack updatedItem = result.item();
            if (result.stopUsing()) {
                this.setUsingItem(false);
                this.lastCompletedItemUseTick = currentTick;
                this.flushEntityData();
            }

            if (updatedItem != activeItem) {
                this.getInventory().setItem(slot, updatedItem);
                if (!result.stopUsing()) {
                    this.activeUseStack = updatedItem;
                }
            }

            if (result.stopUsing()) {
                return;
            }
        }

        if (this.itemUseCompleteTick < 0 || currentTick < this.itemUseCompleteTick) {
            return;
        }

        int slot = this.activeUseSlot;
        ItemStack item = this.getInventory().getItem(slot);
        this.setUsingItem(false);
        this.flushEntityData();
        FinishUseHandler handler = CloudItemRegistry.get().requireComponent(item.getType(), ItemBehaviors.FINISH_USE);
        ItemStack result = CloudItemRegistry.get().getComponent(item.getType(), ItemBehaviors.CONSUMABLE) != null
                ? ConsumableItemHandlers.finishConsumption(item, this) : handler.execute(item, this);
        this.getInventory().setItem(slot, result);
    }

    public void releaseUsingItem() {
        int slot = this.activeUseSlot;
        int ticksUsed = this.isUsingItem() ? Math.max(0, this.server.getTick() - this.startAction) : 0;
        ItemStack activeStack = this.activeUseStack;
        boolean valid = this.activeUseMatchesCurrentSlot();
        this.setUsingItem(false);
        if (activeStack == null || !valid) {
            return;
        }

        ItemStack item = this.getInventory().getItem(slot);
        ReleaseUseHandler handler =
                CloudItemRegistry.get().getComponent(activeStack.getType(), ItemBehaviors.RELEASE_USE);
        if (handler != null) {
            this.getInventory().setItem(slot, handler.execute(item, this, ticksUsed));
        }
    }

    private boolean activeUseMatchesCurrentSlot() {
        return this.activeUseStack != null && this.activeUseSlot >= 0
                && this.activeUseSlot == this.getSelectedHotbarSlot()
                && this.getInventory().getItem(this.activeUseSlot).hasSameDataComponents(this.activeUseStack);
    }

    public boolean consumeRecentCompletedItemUse() {
        int completedTick = this.lastCompletedItemUseTick;
        this.lastCompletedItemUseTick = Integer.MIN_VALUE;
        int elapsed = this.server.getTick() - completedTick;
        return completedTick != Integer.MIN_VALUE && elapsed >= 0 && elapsed < COMPLETED_USE_GRACE_TICKS;
    }

    @Override
    public ItemStack getBlockingItem() {
        return this.itemBlocking.getBlockingItem();
    }

    public void updateBlockingState() {
        this.itemBlocking.update();
    }

    public void interruptBlocking() {
        this.itemBlocking.interrupt();
    }

    public void startSpinAttack(int durationTicks) {
        if (durationTicks <= 0) {
            throw new IllegalArgumentException("durationTicks must be positive");
        }
        this.spinAttackEndTick = this.server.getTick() + durationTicks;
        this.data.setFlag(DAMAGE_NEARBY_MOBS, true);
        this.recalculateBoundingBox();
    }

    private void stopSpinAttack() {
        this.spinAttackEndTick = -1;
        this.data.setFlag(DAMAGE_NEARBY_MOBS, false);
        this.recalculateBoundingBox();
    }

    public boolean isBreakingBlock() {
        return this.packetHandler.isBreakingBlock();
    }

    public boolean canInteractWithBlock(Vector3i position) {
        return this.isWithinInteractionReach(position.toFloat().add(0.5f, 0.5f, 0.5f));
    }

    public boolean isEntityOutOfReach(Entity entity) {
        return !this.isWithinInteractionReach(entity.getPosition());
    }

    private boolean isWithinInteractionReach(Vector3f position) {
        int maxDistance = this.isCreative() ? CREATIVE_INTERACTION_DISTANCE : SURVIVAL_INTERACTION_DISTANCE;
        if (this.getPosition().distanceSquared(position) > (double) maxDistance * maxDistance) {
            return false;
        }

        Vector2f direction = this.getHorizontalLookVector();
        float dx = position.getX() - this.getX();
        float dz = position.getZ() - this.getZ();
        return direction.getX() * dx + direction.getY() * dz >= -INTERACTION_BEHIND_TOLERANCE;
    }

    private void updateInteractionPrompt() {
        int interactDistance = isCreative() ? 5 : 3;
        this.setInteractionPrompt(this.findInteractionPrompt(interactDistance));
    }

    private String findInteractionPrompt(int maxDistance) {
        Vector3f start = this.getPosition().add(0, this.getEyeHeight(), 0);
        RayTraceContext trace = new RayTraceContext(start, start.add(this.getDirectionVector().mul(maxDistance)),
                BlockShapeMode.OUTLINE, FluidCollisionMode.NONE, CollisionContext.of(this));
        HitResult hit = this.getLevel().rayTrace(trace, 0, entity -> entity instanceof EntityVehicle vehicle && !vehicle.getInteractionPrompt().isEmpty());
        return hit instanceof EntityHitResult entityHit && entityHit.entity() instanceof EntityVehicle vehicle ? vehicle.getInteractionPrompt() : "";
    }

    private void setInteractionPrompt(String text) {
        if (!text.equals(buttonText)) {
            this.buttonText = text;
            this.data.set(INTERACT_TEXT, this.buttonText);
        }
    }

    public int useFishingRod(ItemStack fishingRod) {
        if (this.fishingHook != null) {
            return this.fishingHook.retrieve(fishingRod);
        }

        Vector3f castPosition = fishingHookCastPosition();
        Location location = Location.from(castPosition, this.getYaw(), this.getPitch(), this.getLevel());

        EntityFishingHook fishingHook = (EntityFishingHook) CloudEntityRegistry.get().newEntity(EntityTypes.FISHING_HOOK, location);
        fishingHook.setPosition(location.getPosition());
        fishingHook.setOwner(this);
        fishingHook.setMotion(fishingHookCastMotion());
        fishingHook.configure(fishingRod);

        PlayerFishEvent fishEvent = new PlayerFishEvent(this, fishingHook, null, PlayerFishState.CAST);
        this.getServer().getEventManager().fire(fishEvent);
        if (fishEvent.isCancelled()) {
            fishingHook.remove(EntityRemoveCause.DISCARD);
            return 0;
        }

        if (fishingHook.getOwner() != this || !fishingHook.spawn() || fishingHook.getOwner() != this) {
            fishingHook.remove(EntityRemoveCause.DISCARD);
            return 0;
        }

        this.fishingHook = fishingHook;
        this.getLevel().addLevelSoundEvent(this.getPosition(), SoundEvent.THROW, -1, Identifier.parse("minecraft:player"), false, false);
        return 0;
    }

    @Override
    public @Nullable FishingHook getFishingHook() {
        return this.fishingHook;
    }

    public void stopFishing() {
        if (this.fishingHook != null) {
            this.fishingHook.remove(EntityRemoveCause.DESPAWN);
        }

        this.fishingHook = null;
    }

    public void clearFishingHook(EntityFishingHook hook) {
        if (this.fishingHook == hook) {
            this.fishingHook = null;
        }
    }

    private Vector3f fishingHookCastPosition() {
        float yaw = (float) Math.toRadians(-this.getYaw()) - (float) Math.PI;
        float yawSin = (float) Math.sin(yaw);
        float yawCos = (float) Math.cos(yaw);
        return Vector3f.from(this.getX() - yawSin * 0.3f, this.getY() + this.getEyeHeight(), this.getZ() - yawCos * 0.3f);
    }

    private Vector3f fishingHookCastMotion() {
        float yaw = (float) Math.toRadians(-this.getYaw()) - (float) Math.PI;
        float pitch = (float) Math.toRadians(-this.getPitch());

        float yawSin = (float) Math.sin(yaw);
        float yawCos = (float) Math.cos(yaw);

        float pitchSin = (float) Math.sin(pitch);
        float pitchCos = -(float) Math.cos(pitch);

        Vector3f direction = Vector3f.from(-yawSin, Math.clamp(-(pitchSin / pitchCos), -5, 5), -yawCos);
        float normalizedScale = 0.6f / direction.length();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        return Vector3f.from(
                direction.getX() * (normalizedScale + sampleFishingCastNoise(random)),
                direction.getY() * (normalizedScale + sampleFishingCastNoise(random)),
                direction.getZ() * (normalizedScale + sampleFishingCastNoise(random))
        );
    }

    private static float sampleFishingCastNoise(ThreadLocalRandom random) {
        return 0.5f + 0.0103365f * (random.nextFloat() - random.nextFloat());
    }

    public boolean dropItem(ItemStack item) {
        if (!this.spawned || !this.isAlive()) {
            return false;
        }

        if (item.isEmpty()) {
            return true;
        }

        Vector3f motion = this.getDirectionVector().mul(0.4);

        this.getLevel().dropItem(this.getPosition().add(0, 1.3, 0), item, motion, 40);

        this.setUsingItem(false);
        return true;
    }

    private void pickUpNearbyEntities() {
        for (Entity entity : this.getLevel().getNearbyEntities(this, this.boundingBox.inflate(1, 0.5f, 1))) {
            this.getLevel().scheduleEntityUpdate(entity);

            if (!entity.isAlive() || !this.isAlive()) {
                continue;
            }

            this.pickupEntity(entity, true);
        }
    }

    public boolean pickupEntity(Entity entity, boolean near) {
        if (!this.spawned || !this.isAlive() || !this.isOnline() || this.getGameMode() == GameMode.SPECTATOR || entity.isClosed()) {
            return false;
        }

        if (near && entity instanceof CloudEntity trackedEntity) {
            if (entity instanceof AbstractArrow arrow && !this.canPickUpProjectile(arrow)) {
                return false;
            }

            switch (entity) {
                case Arrow arrow when arrow.getMotion().lengthSquared() == 0 -> {
                    return this.pickUpArrow(trackedEntity, arrow);
                }
                case ThrownTrident trident when trident.getMotion().lengthSquared() == 0 -> {
                    Entity owner = trident.getOwner();
                    return (owner == null || owner == this) && this.completeProjectilePickup(trackedEntity, trident.getTrident());
                }
                case DroppedItem droppedItem when droppedItem.getPickupDelay() == 0 -> {
                    return this.pickUpDroppedItem(trackedEntity, droppedItem);
                }
                default -> {
                }
            }
        }

        int tick = this.getServer().getTick();
        if (this.lastExperiencePickupTick < tick && entity instanceof EntityExperienceOrb experienceOrb && this.boundingBox.intersects(entity.getBoundingBox())) {
            if (experienceOrb.getPickupDelay() <= 0) {
                int exp = experienceOrb.getExperience();
                Vector3f pickupPosition = experienceOrb.getPosition();
                experienceOrb.consumeOne();
                LevelEventPacket sound = new LevelEventPacket();
                sound.setType(LevelEvent.SOUND_EXPERIENCE_ORB_PICKUP);
                sound.setPosition(pickupPosition);
                sound.setData(0);
                this.sendPacket(sound);
                this.lastExperiencePickupTick = tick;

                this.giveExperiencePoints(PlayerExperience.repairEquipment(this, exp));
                return true;
            }
        }

        return false;
    }

    private boolean pickUpArrow(CloudEntity entity, Arrow arrow) {
        if (!this.isCreative() && !this.getContainer().canAddItem(arrow.getItemStack())) {
            return false;
        }

        InventoryPickupArrowEvent event = new InventoryPickupArrowEvent(this.getInventory(), arrow);
        this.server.getEventManager().fire(event);
        return !event.isCancelled() && this.canPickUpProjectile(arrow) && this.completeProjectilePickup(entity, arrow.getItemStack());
    }

    private boolean pickUpDroppedItem(CloudEntity entity, DroppedItem droppedItem) {
        ItemStack item = droppedItem.getItem();
        if (item == null || item.isEmpty() || !this.getContainer().canAddItem(item)) {
            return false;
        }

        InventoryPickupItemEvent event = new InventoryPickupItemEvent(this.getInventory(), droppedItem);
        this.server.getEventManager().fire(event);
        if (event.isCancelled()) {
            return false;
        }

        if (item.getBlockState().filter(state -> state.is(BlockTags.LOG)).isPresent()) {
            this.awardAchievement("mineWood");
        } else if (item.getType() == ItemTypes.DIAMOND) {
            this.awardAchievement("diamond");
        }

        ItemStack[] remaining = this.getContainer().addItem(item);
        if (remaining.length != 0) {
            throw new IllegalStateException("Inventory accepted an item pickup but failed to insert it");
        }

        this.finishEntityPickup(entity);
        return true;
    }

    private boolean canPickUpProjectile(AbstractArrow projectile) {
        return switch (projectile.getPickupStatus()) {
            case DISALLOWED -> false;
            case ALLOWED -> true;
            case CREATIVE_ONLY -> this.isCreative();
        };
    }

    private boolean completeProjectilePickup(CloudEntity projectile, ItemStack item) {
        if (projectile.isClosed()) {
            return false;
        }

        if (!this.isCreative()) {
            CloudContainer inventory = this.getContainer();
            if (!inventory.canAddItem(item)) {
                return false;
            }

            CloudContainer.applySlotWrites(() -> {
                if (inventory.addItem(item).length != 0) {
                    throw new IllegalStateException("Inventory accepted a projectile pickup but failed to insert it");
                }
            });
        }

        this.finishEntityPickup(projectile);
        return true;
    }

    private void finishEntityPickup(CloudEntity entity) {
        TakeItemEntityPacket packet = new TakeItemEntityPacket();
        packet.setRuntimeEntityId(this.getRuntimeId());
        packet.setItemRuntimeEntityId(entity.getRuntimeId());
        CloudServer.broadcastPacket(entity.getViewers(), packet);
        this.sendPacket(packet);
        entity.remove(EntityRemoveCause.PICKUP);
    }

    public boolean hasAchievement(String achievementId) {
        return this.achievements.contains(achievementId);
    }

    private void awardAchievement(String achievementId) {
        if (!CloudServer.getInstance().getConfig().isAchievements()) {
            return;
        }

        Achievement achievement = Achievement.achievements.get(achievementId);
        if (achievement == null || hasAchievement(achievementId)) {
            return;
        }

        for (String id : achievement.requires()) {
            if (!this.hasAchievement(id)) {
                return;
            }
        }

        PlayerAchievementAwardedEvent event = new PlayerAchievementAwardedEvent(this, achievementId);
        this.server.getEventManager().fire(event);

        if (event.isCancelled()) {
            return;
        }

        this.achievements.add(achievementId);
        achievement.broadcast(this);
    }

    @Override
    public boolean attack(Entity target) {
        return this.attackWithBonusDamage(target, 0);
    }

    public boolean attackWithBonusDamage(Entity target, float bonusDamage) {
        checkNotNull(target, "target");
        if (!Float.isFinite(bonusDamage) || bonusDamage < 0) {
            throw new IllegalArgumentException("bonusDamage must be finite and nonnegative");
        }

        this.interruptBlocking();

        int attackSlot = this.getSelectedHotbarSlot();
        ItemStack heldItem = this.getInventory().getItem(attackSlot);
        float baseDamage = 1;
        if (!heldItem.isEmpty()) {
            FloatItemHandler attackDamage = CloudItemRegistry.get().requireComponent(heldItem.getType(), ItemBehaviors.GET_ATTACK_DAMAGE);
            baseDamage = attackDamage.execute(heldItem);
        }

        baseDamage = Math.max(0, (baseDamage + bonusDamage) * this.getAttackDamageMultiplier());
        float damage = CloudEnchantmentRegistry.get().modifyDamage(heldItem, target, baseDamage);
        boolean criticalHit = this.isCriticalAttack(target);
        if (criticalHit) {
            damage += baseDamage * 0.5f;
        }

        DamageType damageType = CloudItemRegistry.get().requireComponent(heldItem.getType(), ItemBehaviors.ATTACK_DAMAGE_TYPE);
        DamageSource source = DamageSource.builder(damageType)
                .directEntity(this)
                .causingEntity(this)
                .weaponItem(heldItem)
                .build();
        boolean piercingWeapon = !heldItem.isEmpty() && CloudItemRegistry.get().getComponent(heldItem.getType(), ItemBehaviors.STAB) != null;

        CloudDamageResult result = target instanceof CloudEntity cloudTarget ? cloudTarget.damageWithResult(damage, source)
                : CloudEntity.applyDamage(target, damage, source) ? CloudDamageResult.APPLIED : CloudDamageResult.REJECTED;
        if (result == CloudDamageResult.APPLIED) {
            CloudEnchantmentRegistry.get().applyPostAttackEffects(heldItem, this, target);
            this.applyAttackKnockback(target, heldItem, damageType);

            if (criticalHit && !piercingWeapon) {
                this.broadcastCriticalHit(target);
            } else if (!piercingWeapon) {
                this.getLevel().addLevelSoundEvent(target.getPosition(), SoundEvent.ATTACK_STRONG, -1, EntityTypes.PLAYER, false, false);
            }

            this.addExhaustion(0.1f, ExhaustionReason.ATTACK);
            this.damageHeldItemAfterAttack(attackSlot, heldItem);
        } else if (result == CloudDamageResult.REJECTED && !piercingWeapon) {
            this.getLevel().addLevelSoundEvent(target.getPosition(), SoundEvent.ATTACK_NODAMAGE, -1, EntityTypes.PLAYER, false, false);
        }

        if (!piercingWeapon) {
            AnimatePacket swingPacket = new AnimatePacket();
            swingPacket.setAction(AnimatePacket.Action.SWING_ARM);
            swingPacket.setRuntimeEntityId(this.getRuntimeId());
            CloudServer.broadcastPacket(this.getViewers(), swingPacket);
        }

        return result.accepted();
    }

    private float getAttackDamageMultiplier() {
        float multiplier = 1;
        PotionEffect strength = this.getPotionEffect(EffectTypes.STRENGTH);
        if (strength != null) {
            multiplier += 0.3f * (strength.getAmplifier() + 1);
        }

        PotionEffect weakness = this.getPotionEffect(EffectTypes.WEAKNESS);
        if (weakness != null) {
            multiplier -= 0.2f * (weakness.getAmplifier() + 1);
        }

        return multiplier;
    }

    private boolean isCriticalAttack(Entity target) {
        return target instanceof LivingEntity
                && this.getFallDistance() > 0
                && !this.isOnGround()
                && !this.isClimbing()
                && !this.isInsideOfWater()
                && !this.hasPotionEffect(EffectTypes.BLINDNESS)
                && !this.isImmobile()
                && this.getVehicle() == null
                && !this.isSprinting();
    }

    private void applyAttackKnockback(Entity target, ItemStack heldItem, DamageType damageType) {
        if (damageType.is(DamageTypeTags.NO_KNOCKBACK) || !(target instanceof CloudLivingEntity livingTarget)) {
            return;
        }

        boolean sprintingAttack = this.isSprinting();
        float extraKnockback = CloudEnchantmentRegistry.get().modifyKnockback(heldItem, target, sprintingAttack ? 0.5f : 0);
        if (extraKnockback <= 0) {
            return;
        }

        Vector2f direction = target.getPosition().sub(this.getPosition()).toVector2(true);
        livingTarget.knockBack(extraKnockback, direction.getX(), direction.getY(), KnockbackCause.ENTITY_ATTACK, this);
        Vector3f motion = this.getMotion();
        this.setMotion(Vector3f.from(motion.getX() * 0.6f, motion.getY(), motion.getZ() * 0.6f));
        this.setSprinting(false);
    }

    private void broadcastCriticalHit(Entity target) {
        if (target instanceof CloudLivingEntity livingTarget) {
            livingTarget.broadcastCriticalHit();
            return;
        }

        if (target instanceof CloudEntity cloudTarget) {
            AnimatePacket animate = new AnimatePacket();
            animate.setAction(AnimatePacket.Action.CRITICAL_HIT);
            animate.setRuntimeEntityId(cloudTarget.getRuntimeId());
            this.getLevel().addChunkPacket(target.getPosition(), animate);
        }

        this.getLevel().addLevelSoundEvent(target.getPosition(), SoundEvent.ATTACK_CRITICAL);
    }

    private void damageHeldItemAfterAttack(int slot, ItemStack heldItem) {
        if (heldItem.isEmpty() || !this.getInventory().getItem(slot).equals(heldItem)) {
            return;
        }

        int durabilityDamage = CloudItemRegistry.get()
                .requireComponent(heldItem.getType(), ItemBehaviors.GET_ATTACK_DURABILITY_DAMAGE)
                .execute(heldItem);
        if (durabilityDamage <= 0) {
            return;
        }

        DamageItemHandler damageItem = CloudItemRegistry.get().requireComponent(heldItem.getType(), ItemBehaviors.ON_DAMAGE);
        ItemStack damagedItem = damageItem.execute(heldItem, durabilityDamage, this);
        if (!damagedItem.equals(heldItem) && this.getInventory().getItem(slot).equals(heldItem)) {
            boolean activeUseOfHeldItem = this.getSelectedHotbarSlot() == slot && this.activeUseMatchesCurrentSlot();
            this.getInventory().setItem(slot, damagedItem);
            if (activeUseOfHeldItem) {
                if (damagedItem.isEmpty()) {
                    this.setUsingItem(false);
                } else {
                    this.activeUseStack = damagedItem;
                }
            }
        }
    }

    @Override
    protected void onDamageBlocked(EntityDamageEvent event) {
        super.onDamageBlocked(event);
        this.itemBlocking.onDamageBlocked(event);
    }

    @Override
    protected boolean applyDamage(CloudEntityDamageEvent event) {
        if (!this.isAlive()) {
            return false;
        }

        if ((this.isCreative() || this.isSpectator()) && !event.getDamageType().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }

        if (this.abilities.get(Ability.MAY_FLY) && event.getDamageType() == DamageTypes.FALL) {
            return false;
        }

        if (event.getDamageType() == DamageTypes.FALL && !this.isSneaking() && this.getLevel().getBlockState(this.getPosition().add(0, -1, 0).toInt()).getType() == BlockTypes.SLIME) {
            this.resetFallDistance();
            return false;
        }

        if (event.getDamageSource().scalesWithDifficulty()) {
            event.setDamage(this.scaleDamageForDifficulty(event.getDamage()));
            if (event.getDamage() == 0) {
                return false;
            }
        }

        return super.applyDamage(event);
    }

    @Override
    protected void onHealthDamageApplied(DamageSource source) {
        this.addExhaustion(source.getFoodExhaustion(), ExhaustionReason.DAMAGED);
    }

    @Override
    protected void broadcastHurtEvent(DamageEffect effect) {
        super.broadcastHurtEvent(effect);
        if (this.spawned) {
            this.sendPacket(this.createHurtEventPacket(effect));
        }
    }

    private float scaleDamageForDifficulty(float damage) {
        return switch (this.getLevel().getDifficulty()) {
            case PEACEFUL -> 0;
            case EASY -> Math.min(damage / 2 + 1, damage);
            case NORMAL -> damage;
            case HARD -> (float) Math.min((double) damage * 1.5, Float.MAX_VALUE);
        };
    }

    @Override
    public void kill() {
        if (!this.spawned || !this.isAlive()) {
            return;
        }

        boolean showMessages = this.getLevel().getGameRules().get(GameRules.SHOW_DEATH_MESSAGES);
        DeathMessageResolver.Resolution death = DeathMessageResolver.resolve(this, this.getLastDamageCause());
        int droppedExperience = (int) Math.min((long) this.getExperienceLevel() * 7, 100);
        PlayerDeathEvent event = new PlayerDeathEvent(
                this, this.getDeathDamageSource(), Arrays.asList(this.getDrops()),
                showMessages ? death.message() : null, droppedExperience
        );
        event.setKeepExperience(this.getLevel().getGameRules().get(GameRules.KEEP_INVENTORY) || this.isSpectator());
        event.setKeepInventory(event.getKeepExperience());
        if (this.shouldAbortDeath(event)) {
            return;
        }

        this.stopSpinAttack();

        this.killer = death.killer();
        if (this.fishingHook != null) {
            this.stopFishing();
        }

        EntityEventPacket deathPacket = new EntityEventPacket();
        deathPacket.setRuntimeEntityId(this.getRuntimeId());
        deathPacket.setType(EntityEventType.DEATH);
        this.sendPacket(deathPacket);
        this.broadcastToViewers(deathPacket);

        this.enterDeathState();
        this.updateBlockingState();
        this.sendHealthAttribute();

        if (!event.getKeepInventory()) {
            for (ItemStack item : event.getDrops()) {
                this.getLevel().dropItem(this.getPosition(), item, null, true, 40);
            }

            clearItemsLostOnDeath(this.getInventory());
            clearItemsLostOnDeath(this.getArmor());
            clearItemsLostOnDeath(this.getOffhand());
        }

        if (!event.getKeepExperience()) {
            int exp = event.getDroppedExperience();
            if (exp > 0) {
                this.getLevel().dropExpOrb(this.getPosition(), exp);
            }

            this.resetExperience();
        }

        if (showMessages && event.getDeathMessage() != null) {
            this.server.broadcast(event.getDeathMessage());
        }

        boolean hadPersonalSpawn = this.respawnConfig != null;
        Location respawnLocation = this.findRespawnPosition(false);
        if (respawnLocation == null) {
            if (hadPersonalSpawn && this.respawnConfig != null) {
                this.respawnConfig.level().addLevelSoundEvent(this.respawnConfig.pos(), SoundEvent.RESPAWN_ANCHOR_AMBIENT);
            }
            respawnLocation = this.getServer().getDefaultLevel().getSafeSpawn();
        }

        RespawnPacket packet = new RespawnPacket();
        packet.setRuntimeEntityId(0);
        packet.setPosition(respawnLocation.getPosition());
        packet.setState(RespawnPacket.State.SERVER_SEARCHING);

        this.extinguish();

        this.sendPacket(packet);
    }

    private static void clearItemsLostOnDeath(SlotGroup slots) {
        for (int slot = 0; slot < slots.size(); slot++) {
            if (slots.getItem(slot).get(ItemDataComponents.KEEP_ON_DEATH) != Boolean.TRUE) {
                slots.setItem(slot, ItemStack.EMPTY);
            }
        }
    }

    public void sendAttributes() {
        UpdateAttributesPacket pk = new UpdateAttributesPacket();
        pk.setRuntimeEntityId(this.getRuntimeId());
        List<AttributeData> attributes = pk.getAttributes();
        attributes.add(this.createHealthAttribute());
        attributes.add(NetworkEntityAttribute.ABSORPTION.encode(this.getAbsorption()));
        attributes.add(NetworkEntityAttribute.HUNGER.encode(this.getFoodData().getLevel()));
        attributes.add(NetworkEntityAttribute.SATURATION.encode(this.foodData.getSaturation()));
        attributes.add(NetworkEntityAttribute.EXHAUSTION.encode(this.foodData.getExhaustion()));
        attributes.add(NetworkEntityAttribute.MOVEMENT_SPEED.encode(this.getMovementSpeed()));
        attributes.add(NetworkEntityAttribute.EXPERIENCE_LEVEL.encode(this.getExperienceLevel()));
        attributes.add(NetworkEntityAttribute.EXPERIENCE.encode(this.experience.progress()));
        this.sendPacket(pk);
    }

    void sendAttributeUpdate(AttributeData attribute) {
        UpdateAttributesPacket packet = new UpdateAttributesPacket();
        packet.getAttributes().add(attribute);
        packet.setRuntimeEntityId(this.getRuntimeId());
        this.sendPacket(packet);
    }

    @Override
    public void setHealth(float health) {
        super.setHealth(health);
        this.sendHealthAttribute();
    }

    @Override
    public void setMaxHealth(int baseMaxHealth) {
        super.setMaxHealth(baseMaxHealth);
        this.sendHealthAttribute();
    }

    @Override
    public void setAbsorption(float absorption) {
        super.setAbsorption(absorption);
        if (this.spawned) {
            this.sendAttributeUpdate(NetworkEntityAttribute.ABSORPTION.encode(this.getAbsorption()));
        }
    }

    private void sendHealthAttribute() {
        if (this.spawned) {
            UpdateAttributesPacket packet = new UpdateAttributesPacket();
            packet.getAttributes().add(this.createHealthAttribute());
            packet.setRuntimeEntityId(this.getRuntimeId());
            this.sendPacket(packet);
        }
    }

    private AttributeData createHealthAttribute() {
        return NetworkEntityAttribute.encodeHealth(this.getHealth(), this.getMaxHealth());
    }

    public boolean isFoodEnabled() {
        return !this.isCreative() && !this.isSpectator();
    }

    @Override
    public int getFoodLevel() {
        return this.foodData.getLevel();
    }

    @Override
    public void setFoodLevel(int level) {
        this.foodData.setLevel(level);
    }

    @Override
    public float getSaturation() {
        return this.foodData.getSaturation();
    }

    @Override
    public void setSaturation(float saturation) {
        this.foodData.setSaturation(saturation);
    }

    @Override
    public float getExhaustion() {
        return this.foodData.getExhaustion();
    }

    @Override
    public void addExhaustion(float amount, ExhaustionReason reason) {
        this.foodData.addExhaustion(amount, reason);
    }

    public void sendFoodAttributes() {
        if (!this.spawned) {
            return;
        }

        UpdateAttributesPacket packet = new UpdateAttributesPacket();
        packet.setRuntimeEntityId(this.getRuntimeId());
        packet.getAttributes().add(NetworkEntityAttribute.HUNGER.encode(this.foodData.getLevel()));
        packet.getAttributes().add(NetworkEntityAttribute.SATURATION.encode(this.foodData.getSaturation()));
        packet.getAttributes().add(NetworkEntityAttribute.EXHAUSTION.encode(this.foodData.getExhaustion()));
        this.sendPacket(packet);
    }

    @Override
    public int getExperienceLevel() {
        return this.experience.level();
    }

    public int getEnchantmentSeed() {
        return this.experience.enchantmentSeed();
    }

    public void giveExperiencePoints(int amount) {
        if (amount != 0) {
            this.experience.givePoints(amount);
            this.sendExperienceAttributes();
        }
    }

    public void setExperienceLevel(int level) {
        this.experience.setLevel(level);
        this.sendExperienceAttributes();
    }

    public void resetExperience() {
        this.experience.setProgress(0, 0);
        this.sendExperienceAttributes();
    }

    public void completeEnchantment(int levelsSpent) {
        this.experience.completeEnchantment(levelsSpent);
        if (levelsSpent > 0) {
            this.sendExperienceAttributes();
        }
    }

    private void sendExperienceAttributes() {
        if (!this.spawned) {
            return;
        }

        UpdateAttributesPacket packet = new UpdateAttributesPacket();
        packet.setRuntimeEntityId(this.getRuntimeId());
        packet.getAttributes().add(NetworkEntityAttribute.EXPERIENCE_LEVEL.encode(this.experience.level()));
        packet.getAttributes().add(NetworkEntityAttribute.EXPERIENCE.encode(this.experience.progress()));
        this.sendPacket(packet);
    }

    public boolean chat(String message) {
        if (!this.spawned || !this.isAlive()) {
            return false;
        }

        if (this.removeFormat) {
            message = PlainTextComponentSerializer.plainText().serialize(BedrockLegacyTextSerializer.getInstance().deserialize(message));
        }

        for (String msg : message.split("\n")) {
            if (!msg.trim().isEmpty() && msg.length() <= 255 && this.messageCounter-- > 0) {
                PlayerChatEvent chatEvent = new PlayerChatEvent(this, Component.text(msg),
                        ChatRenderer.defaultRenderer(),
                        new HashSet<>(this.getServer().getOnlinePlayers().values())
                );
                this.server.getEventManager().fire(chatEvent);
                if (!chatEvent.isCancelled()) {
                    for (Audience viewer : chatEvent.viewers()) {
                        Component rendered = chatEvent.renderer().render(
                                this, this.displayName(), chatEvent.message(), viewer);
                        viewer.sendMessage(rendered);
                    }
                }
            }
        }

        return true;
    }

    @Override
    public void sendMessage(@NonNull Component message) {
        this.sendPacket(TextPacketFactory.system(message, getLocale()));
    }

    @Override
    public void sendMessage(@NonNull Component message, ChatType.Bound boundChatType) {
        String source = PlainTextComponentSerializer.plainText().serialize(boundChatType.name());
        this.sendPacket(TextPacketFactory.chat(source, message, this.getLocale()));
    }

    @Override
    public void sendMessage(SignedMessage signedMessage, ChatType.@NonNull Bound boundChatType) {
        Component message = Objects.requireNonNullElseGet(signedMessage.unsignedContent(), () -> Component.text(signedMessage.message()));
        if (signedMessage.isSystem()) {
            this.sendMessage(message);
            return;
        }
        this.sendMessage(message, boundChatType);
    }

    @Override
    public void sendPopup(Component message) {
        this.sendPacket(TextPacketFactory.popup(message, getLocale()));
    }

    @Override
    public void sendTip(Component message) {
        this.sendPacket(TextPacketFactory.tip(message, getLocale()));
    }

    @Override
    public void clearTitle() {
        SetTitlePacket packet = new SetTitlePacket();
        packet.setType(SetTitlePacket.Type.CLEAR);
        packet.setText("");
        this.sendPacket(packet);
    }

    @Override
    public void resetTitle() {
        SetTitlePacket packet = new SetTitlePacket();
        packet.setType(SetTitlePacket.Type.RESET);
        packet.setText("");
        this.sendPacket(packet);
    }

    @Override
    public void sendSubtitle(Component subtitle) {
        SetTitlePacket packet = new SetTitlePacket();
        packet.setType(SetTitlePacket.Type.SUBTITLE);
        packet.setText(new BedrockComponent(subtitle));
        this.sendPacket(packet);
    }

    @Override
    public void setTitleTimes(int fadeIn, int duration, int fadeOut) {
        SetTitlePacket packet = new SetTitlePacket();
        packet.setType(SetTitlePacket.Type.TIMES);
        packet.setFadeInTime(fadeIn);
        packet.setStayTime(duration);
        packet.setFadeOutTime(fadeOut);
        packet.setText("");
        this.sendPacket(packet);
    }

    private void setTitle(Component text) {
        SetTitlePacket packet = new SetTitlePacket();
        packet.setType(SetTitlePacket.Type.TITLE);
        packet.setText(new BedrockComponent(text));
        this.sendPacket(packet);
    }

    @Override
    public void sendTitle(Component title) {
        this.sendTitle(title, Component.empty(), 20, 20, 5);
    }

    @Override
    public void sendTitle(Component title, Component subtitle) {
        this.sendTitle(title, subtitle, 20, 20, 5);
    }

    @Override
    public void sendTitle(Component title, Component subtitle, int fadeIn, int stay, int fadeOut) {
        this.setTitleTimes(fadeIn, stay, fadeOut);
        this.sendSubtitle(subtitle != null ? subtitle : Component.empty());
        this.setTitle(title != null ? title : Component.text(" "));
    }

    @Override
    public void showTitle(Title title) {
        Title.Times times = title.times();
        if (times != null) {
            this.setTitleTimes(ticks(times.fadeIn()), ticks(times.stay()), ticks(times.fadeOut()));
        }
        this.sendSubtitle(title.subtitle());
        this.setTitle(title.title());
    }

    @Override
    public <T> void sendTitlePart(@NonNull TitlePart<T> part, @NonNull T value) {
        Objects.requireNonNull(part, "part");
        Objects.requireNonNull(value, "value");

        if (part == TitlePart.TITLE) {
            this.setTitle((Component) value);
        } else if (part == TitlePart.SUBTITLE) {
            this.sendSubtitle((Component) value);
        } else if (part == TitlePart.TIMES) {
            Title.Times times = (Title.Times) value;
            this.setTitleTimes(ticks(times.fadeIn()), ticks(times.stay()), ticks(times.fadeOut()));
        } else {
            throw new IllegalArgumentException("Unknown title part: " + part);
        }
    }

    @Override
    public void sendActionBar(@NonNull Component title) {
        this.sendActionBar(title, 1, 0, 1);
    }

    @Override
    public void sendActionBar(Component title, int fadeIn, int duration, int fadeout) {
        SetTitlePacket packet = new SetTitlePacket();
        packet.setType(SetTitlePacket.Type.ACTIONBAR);
        packet.setText(new BedrockComponent(title));
        packet.setFadeInTime(fadeIn);
        packet.setStayTime(duration);
        packet.setFadeOutTime(fadeout);
        this.sendPacket(packet);
    }

    private int ticks(Duration duration) {
        return Math.toIntExact(duration.toMillis() / 50L);
    }

    @Override
    public void playSound(Vector3f position, SoundPlayback playback) {
        this.sendPacket(SoundPacketFactory.play(playback, position));
    }

    @Override
    public void stopSound(SoundType sound) {
        this.sendPacket(SoundPacketFactory.stop(sound));
    }

    @Override
    public void stopAllSounds() {
        this.sendPacket(SoundPacketFactory.stopAll());
    }

    public void save() {
        this.save(false);
    }

    public void save(boolean async) {
        if (this.isClosed()) {
            throw new IllegalStateException("Tried to save closed player");
        }

        if (!this.loggedIn || this.username.isEmpty()) {
            return;
        }

        NbtMapBuilder tag = NbtMap.builder();
        this.saveAdditionalData(tag);

        this.server.saveOfflinePlayerData(this.getServerId(), tag.build(), async);
    }

    @Override
    protected void loadBaseCustom(NbtMap tag) {
        if (this.server != null) {
            CloudLevel savedLevel = tag.get("Level") instanceof String levelId ? this.server.getLevel(levelId) : null;
            this.level = savedLevel != null ? savedLevel : this.server.getDefaultLevel();
        }
        super.loadBaseCustom(tag);
    }

    @Override
    protected void loadCustom(NbtMap tag) {
        PlayerSaveData.loadInventories(tag, this.container, this.enderChestContainer);
        PlayerSaveData.Progress progress = PlayerSaveData.Progress.read(tag, this.experience.enchantmentSeed(), DEFAULT_MOVEMENT_SPEED);
        this.experience.load(progress.experience(), progress.experienceLevel(), progress.enchantmentSeed());
        this.seenCredits = progress.seenCredits();
        this.baseMovementSpeed = progress.baseMovementSpeed();
        this.sendExperienceAttributes();

        PlayerSaveData.Food food = this.saveData.load(tag);
        this.foodData.load(food.level(), food.saturation(), food.exhaustion(), food.tickTimer());

        super.loadCustom(tag);
        this.syncMovementSpeed();
        this.restoreSavedRespawn();
    }

    private void restoreSavedRespawn() {
        this.spawnLocation = null;
        this.respawnConfig = null;
        PlayerSaveData.Respawn savedRespawn = this.saveData.respawn();
        String spawnLevelId = savedRespawn.levelId();
        Vector3i spawnPosition = savedRespawn.position();
        if (spawnLevelId == null || spawnPosition == null) {
            return;
        }

        CloudLevel spawnLevel = this.server.getLevel(spawnLevelId);
        if (spawnLevel == null) {
            return;
        }

        RespawnConfig.SpawnType spawnType = savedRespawn.type();
        if (spawnType == null) {
            spawnType = inferSpawnType(spawnLevel.getBlock(spawnPosition).getState().getType());
        }

        if (spawnType == null) {
            this.clearPersonalSpawn();
            return;
        }

        float spawnYaw = savedRespawn.yaw();
        this.respawnConfig = new RespawnConfig(spawnLevel, spawnPosition, spawnYaw, spawnType);
        this.spawnLocation = Location.from(spawnPosition.toFloat(), spawnYaw, 0f, spawnLevel);
    }

    private static RespawnConfig.@Nullable SpawnType inferSpawnType(BlockType blockType) {
        if (blockType == BlockTypes.RESPAWN_ANCHOR) {
            return RespawnConfig.SpawnType.RESPAWN_ANCHOR;
        }
        return blockType.is(BlockTags.BEDS) ? RespawnConfig.SpawnType.BED : null;
    }

    @Override
    protected void saveCustom(NbtMapBuilder tag) {
        super.saveCustom(tag);
        PlayerSaveData.saveInventories(tag, this.container, this.enderChestContainer);
        new PlayerSaveData.Progress(
                this.experience.points(), this.experience.level(), this.experience.enchantmentSeed(),
                this.seenCredits, this.baseMovementSpeed).write(tag, DEFAULT_MOVEMENT_SPEED
        );

        this.saveData.setLevelId(this.level.getId());
        if (this.spawnLocation != null && this.spawnLocation.getLevel() != null) {
            Vector3i spawnPos = this.spawnLocation.getPosition().toInt();
            RespawnConfig.SpawnType spawnType = this.respawnConfig != null
                    && this.respawnConfig.level() == this.spawnLocation.getLevel()
                    && this.respawnConfig.pos().equals(spawnPos) ? this.respawnConfig.spawnType() : null;
            this.saveData.setRespawn(this.spawnLocation.getLevel().getId(), spawnPos, this.spawnLocation.getYaw(), spawnType);
        }

        this.saveData.save(tag,
                new PlayerSaveData.Food(this.foodData.getLevel(), this.foodData.getSaturation(), this.foodData.getExhaustion(), this.foodData.getTickTimer()),
                this.getSkin()
        );
    }

    @Override
    protected Set<String> customKeys() {
        Set<String> keys = new HashSet<>(super.customKeys());
        keys.addAll(PlayerSaveData.ownedKeys());
        return Set.copyOf(keys);
    }

    @Override
    public int hashCode() {
        return Long.hashCode(this.getRuntimeId());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof CloudPlayer other)) return false;
        return this.getRuntimeId() == other.getRuntimeId();
    }

    @Override
    public String toString() {
        return "Player(name=" + getName() + ")";
    }

    private class SessionHandler implements BedrockPacketHandler {

        @Override
        public void onDisconnect(CharSequence reason) {
            String disconnectReason = reason.toString();
            if (CloudPlayer.this.server.isPrimaryThread()) {
                CloudPlayer.this.close("", disconnectReason);
            } else {
                CloudPlayer.this.server.getGlobalScheduler().execute(null, () -> CloudPlayer.this.close("", disconnectReason));
            }
        }
    }
}
