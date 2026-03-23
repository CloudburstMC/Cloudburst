package org.cloudburstmc.server.player;

import co.aikar.timings.Timing;
import co.aikar.timings.Timings;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.blockentity.EnderChest;
import org.cloudburstmc.api.blockentity.Sign;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.entity.Attribute;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.Interactable;
import org.cloudburstmc.api.entity.misc.DroppedItem;
import org.cloudburstmc.api.entity.misc.ExperienceOrb;
import org.cloudburstmc.api.entity.projectile.Arrow;
import org.cloudburstmc.api.entity.projectile.FishingHook;
import org.cloudburstmc.api.entity.projectile.ThrownTrident;
import org.cloudburstmc.api.event.entity.EntityDamageByBlockEvent;
import org.cloudburstmc.api.event.entity.EntityDamageByEntityEvent;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;
import org.cloudburstmc.api.event.entity.ProjectileLaunchEvent;
import org.cloudburstmc.api.event.inventory.InventoryCloseEvent;
import org.cloudburstmc.api.event.inventory.InventoryPickupArrowEvent;
import org.cloudburstmc.api.event.inventory.InventoryPickupItemEvent;
import org.cloudburstmc.api.event.player.*;
import org.cloudburstmc.api.inventory.*;
import org.cloudburstmc.api.inventory.view.*;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.ChunkLoader;
import org.cloudburstmc.api.level.Difficulty;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.api.permission.Permission;
import org.cloudburstmc.api.permission.PermissionAttachment;
import org.cloudburstmc.api.permission.PermissionAttachmentInfo;
import org.cloudburstmc.api.player.Ability;
import org.cloudburstmc.api.player.GameMode;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.player.skin.Skin;
import org.cloudburstmc.api.plugin.PluginContainer;
import org.cloudburstmc.api.util.AxisAlignedBB;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.LoginChainData;
import org.cloudburstmc.api.util.SimpleAxisAlignedBB;
import org.cloudburstmc.api.util.component.ComponentMap;
import org.cloudburstmc.math.GenericMath;
import org.cloudburstmc.math.vector.Vector2f;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.netty.channel.raknet.RakChildChannel;
import org.cloudburstmc.netty.handler.codec.raknet.common.RakSessionCodec;
import org.cloudburstmc.protocol.adventure.BedrockComponent;
import org.cloudburstmc.protocol.adventure.BedrockLegacyTextSerializer;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.data.*;
import org.cloudburstmc.protocol.bedrock.data.command.CommandPermission;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityEventType;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerId;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.data.skin.SerializedSkin;
import org.cloudburstmc.protocol.bedrock.packet.*;
import org.cloudburstmc.protocol.common.DefinitionRegistry;
import org.cloudburstmc.protocol.common.PacketSignal;
import org.cloudburstmc.protocol.common.util.OptionalBoolean;
import org.cloudburstmc.server.Achievement;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.block.BlockPalette;
import org.cloudburstmc.server.blockentity.SignBlockEntity;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.container.Container;
import org.cloudburstmc.server.container.ContainerListener;
import org.cloudburstmc.server.container.screen.*;
import org.cloudburstmc.server.container.view.CloudEnderChestView;
import org.cloudburstmc.server.container.view.CloudHotbarView;
import org.cloudburstmc.server.container.view.CloudPlayerInventory;
import org.cloudburstmc.server.container.view.CloudSlotGroupBase;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.entity.EntityHuman;
import org.cloudburstmc.server.entity.EntityLiving;
import org.cloudburstmc.server.entity.projectile.EntityArrow;
import org.cloudburstmc.server.event.server.PlayerPacketSendEvent;
import org.cloudburstmc.server.form.CustomForm;
import org.cloudburstmc.server.form.Form;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.Explosion;
import org.cloudburstmc.server.level.Sound;
import org.cloudburstmc.server.level.biome.CloudBiome;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.math.BlockRayTrace;
import org.cloudburstmc.server.network.NetworkUtils;
import org.cloudburstmc.server.network.inventory.ItemStackNetManager;
import org.cloudburstmc.server.permission.PermissibleBase;
import org.cloudburstmc.server.player.handler.PlayerPacketHandler;
import org.cloudburstmc.server.player.manager.PlayerChunkManager;
import org.cloudburstmc.server.player.manager.PlayerInventoryManager;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.registry.CloudItemRegistry;
import org.cloudburstmc.server.registry.CommandRegistry;
import org.cloudburstmc.server.registry.EntityRegistry;
import org.cloudburstmc.server.utils.ClientChainData;
import org.cloudburstmc.server.utils.DummyBossBar;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongConsumer;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes.BED_POSITION;
import static org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes.INTERACT_TEXT;
import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.USING_ITEM;

/**
 * Server-side implementation of a connected player. Extends {@link EntityHuman} and implements
 * {@link Player}, {@link CommandSender}, and {@link ContainerListener}. Handles movement, inventory,
 * chunk loading, packet processing, and all player-specific game logic.
 */
@Log4j2
public class CloudPlayer extends EntityHuman implements CommandSender, ChunkLoader, Player, ContainerListener {

    public static final float DEFAULT_SPEED = 0.1f;
    public static final float MAXIMUM_SPEED = 0.5f;

    protected final BedrockServerSession session;
    protected final PlayerData playerData = new PlayerData();
    protected final PlayerFood foodData = new PlayerFood(this, 20, 20);
    protected final Map<UUID, CloudPlayer> hiddenPlayers = new HashMap<>();
    protected final int chunksPerTick;
    protected final int spawnThreshold;

    @Getter
    private final ItemStackNetManager itemStackNetManager = new ItemStackNetManager(this);
    @Getter(AccessLevel.NONE)
    private final CloudPlayerInventory inventory = new CloudPlayerInventory(this, this.container);
    @Getter
    private final CloudHotbarView hotbar = new CloudHotbarView(this);
    @Getter
    private final CloudContainer enderChestContainer = new CloudContainer(27);
    private final PlayerPacketHandler packetHandler;
    private final Map<Container, Byte> containerToWindowId = new HashMap<>();
    private final Map<Byte, Container> windowIdToContainer = new HashMap<>();
    private final CloudEnderChestView enderChest = new CloudEnderChestView(this);
    private final PlayerInventoryManager invManager = new PlayerInventoryManager(this);
    private final Queue<BedrockPacket> inboundQueue = new ConcurrentLinkedQueue<>();
    private final AtomicReference<Locale> locale = new AtomicReference<>(null);
    private final PlayerChunkManager chunkManager = new PlayerChunkManager(this);

    public boolean spawned = false;
    public boolean loggedIn = false;
    public int packetsRecieved;
    public int pickedXPOrb = 0;
    public long creationTime = 0;
    public long lastBreak;
    public long lastSkinChange;
    public Block breakingBlock = null;
    public FishingHook fishing = null;
    public Vector3f speed = null;

    protected boolean connected = true;
    protected boolean enableClientCommand = true;
    protected boolean removeFormat = true;
    protected int inAirTicks = 0;
    protected int lastChorusFruitTeleport = 20;
    protected int lastEnderPearl = 20;
    protected int messageCounter = 2;
    protected int serverSettingsId = -1;
    protected int startAction = -1;
    protected int startAirTicks = 5;
    protected int viewDistance;
    protected float stepHeight = 0.6f;
    protected long randomClientId;
    protected Component displayName;
    protected String iusername;
    protected String username;
    protected AtomicInteger formWindowCount = new AtomicInteger(0);
    protected CloudPlayerAbilities abilities;
    //TODO: better handling server settings?
    protected CustomForm serverSettings = null;
    protected Location spawnLocation = null;
    protected RespawnConfig respawnConfig = null;
    protected Map<Integer, Form<?>> formWindows = new Int2ObjectOpenHashMap<>();
    protected Map<Long, DummyBossBar> dummyBossBars = new Long2ObjectLinkedOpenHashMap<>();
    protected Vector3f forceMovement = null;
    protected Vector3f newPosition = null;
    protected Vector3f teleportPosition = null;
    protected Vector3i sleeping = null;
    protected long clientTick = 0;

    @Getter
    private int selectedHotbarSlot = 0;
    private boolean foodEnabled = true;
    private GameMode previousGameMode = null;
    @Getter
    @Setter
    private boolean clientCacheEnabled = false;
    private boolean initialized;
    private boolean changingDimension = false;
    private byte containerIdCounter = 1;
    private int exp = 0;
    private int expLevel = 0;
    private int loaderId;
    private Entity killer = null;
    private LoginChainData loginChainData;
    private PermissibleBase perm = null;
    private String buttonText = "Button";
    private String clientSecret;

    public CloudPlayer(BedrockServerSession session, ClientChainData chainData) {
        super(EntityTypes.PLAYER, Location.from(CloudServer.getInstance().getDefaultLevel()));
        this.session = session;
        this.packetHandler = new PlayerPacketHandler(this);
        session.setPacketHandler(new Handler());
        this.server = CloudServer.getInstance();
        this.lastBreak = -1;
        this.chunksPerTick = this.server.getConfig().getChunkSending().getPerTick();
        this.spawnThreshold = this.server.getConfig().getChunkSending().getSpawnThreshold();
        this.spawnLocation = null;
        this.playerData.setGamemode(this.server.getGameMode());
        this.viewDistance = this.server.getViewDistance();
        //this.newPosition = new Vector3(0, 0, 0);
        this.boundingBox = new SimpleAxisAlignedBB(0, 0, 0, 0, 0, 0);
        this.lastSkinChange = -1;

        this.loginChainData = chainData;

        this.randomClientId = chainData.getClientId();
        this.identity = chainData.getClientUUID();
        this.username = PlainTextComponentSerializer.plainText().serialize(BedrockLegacyTextSerializer.getInstance().deserialize(chainData.getUsername()));
        this.iusername = username.toLowerCase();
        this.displayName(Component.text(this.username));
        this.setNameTag(this.username);

        this.perm = new PermissibleBase(this.server.getPermissionManager(), this);

        this.creationTime = System.currentTimeMillis();

        this.container.addContainerListener(this);
        this.enderChestContainer.addContainerListener(this);
        this.armor.getContainer().addContainerListener(this);
        this.offhand.getContainer().addContainerListener(this);
    }

    private static boolean hasSubstantiallyMoved(Vector3f oldPos, Vector3f newPos) {
        return oldPos.getFloorX() >> 4 != newPos.getFloorX() >> 4 || oldPos.getFloorZ() >> 4 != newPos.getFloorZ() >> 4;
    }

    private static int distance(int centerX, int centerZ, int x, int z) {
        int dx = centerX - x;
        int dz = centerZ - z;
        return dx * dx + dz * dz;
    }

    public static int calculateRequireExperience(int level) {
        if (level >= 30) {
            return 112 + (level - 30) * 9;
        } else if (level >= 15) {
            return 37 + (level - 15) * 5;
        } else {
            return 7 + level * 2;
        }
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

    public int getStartActionTick() {
        return startAction;
    }

    public void startAction() {
        this.startAction = this.server.getTick();
    }

    public void stopAction() {
        this.startAction = -1;
    }

    public int getLastEnderPearlThrowingTick() {
        return lastEnderPearl;
    }

    public void onThrowEnderPearl() {
        this.lastEnderPearl = this.server.getTick();
    }

    public int getLastChorusFruitTeleport() {
        return lastChorusFruitTeleport;
    }

    public void onChorusFruitTeleport() {
        this.lastChorusFruitTeleport = this.server.getTick();
    }

    public void openEnderChest(EnderChest chest) {
        Objects.requireNonNull(chest, "Ender chest can't be null");
        if (!this.canOpenInventory()) return;

        CloudEnderChestScreen screen = new CloudEnderChestScreen(this, chest.getBlock());
        this.invManager.openScreen(screen);
    }

    public Component leaveMessage() {
        return Component.translatable("multiplayer.player.left", displayName()).color(NamedTextColor.YELLOW);
    }

    public String getClientSecret() {
        return clientSecret;
    }

    /**
     * This might disappear in the future.
     * Please use getUniqueId() instead (IP + clientId + name combo, in the future it'll change to real UUID for online auth)
     *
     * @return random client id
     */
    @Deprecated
    public Long getClientId() {
        return randomClientId;
    }

    @Override
    public boolean isBanned() {
        return this.server.isBanned(this);
    }

    @Override
    public void setBanned(boolean value) {
        if (value) {
            this.server.ban(this);
            this.kick(PlayerKickEvent.Reason.NAME_BANNED, "Banned by admin");
        } else {
            this.server.unban(this);
        }
    }

    @Override
    public boolean isWhitelisted() {
        return this.server.isWhitelisted(this.getName().toLowerCase());
    }

    @Override
    public void setWhitelisted(boolean value) {
        if (value) {
            this.server.addWhitelist(this);
        } else {
            this.server.removeWhitelist(this);
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    @Override
    public OptionalLong getFirstPlayed() {
        return this.playerData.getFirstPlayed();
    }

    @Override
    public CloudPlayerAbilities getAbilities() {
        return abilities;
    }

    /**
     * Sets a boolean ability flag on this player and fires {@link PlayerAbilityChangeEvent}.
     * If the event is cancelled the ability is not changed. If a plugin redirects the value
     * via {@link PlayerAbilityChangeEvent#setNewValue}, that redirected value is applied instead.
     *
     * @param ability the ability flag to change
     * @param value   the desired new value
     */
    public void setAbility(Ability ability, boolean value) {
        boolean oldValue = this.abilities.get(ability);
        PlayerAbilityChangeEvent event = new PlayerAbilityChangeEvent(this, ability, oldValue, value);
        this.server.getEventManager().fire(event);
        if (event.isCancelled()) {
            this.abilities.update();
            return;
        }
        this.abilities.set(ability, event.getNewValue());
        this.abilities.update();
    }

    public void resetInAirTicks() {
        this.inAirTicks = 0;
    }

    @Override
    public OptionalLong getLastPlayed() {
        return this.playerData.getLastPlayed();
    }

    @Override
    public CloudServer getServer() {
        return this.server;
    }

    public boolean getRemoveFormat() {
        return removeFormat;
    }

    public void setRemoveFormat(boolean remove) {
        this.removeFormat = remove;
    }

    public void setRemoveFormat() {
        this.setRemoveFormat(true);
    }

    @Override
    public boolean hasPlayedBefore() {
        return this.playerData.getFirstPlayed().getAsLong() > 0;
    }

    public boolean canSee(CloudPlayer player) {
        return !this.hiddenPlayers.containsKey(player.getServerId());
    }

    public void hidePlayer(CloudPlayer player) {
        if (this == player) {
            return;
        }
        this.hiddenPlayers.put(player.getServerId(), player);
        player.despawnFrom(this);
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return false;
    }

    @Override
    public void spawnTo(CloudPlayer player) {
        if (this.spawned && player.spawned && this.isAlive() && player.isAlive() &&
                player.getLevel() == this.getLevel() && player.canSee(this) && !this.isSpectator()) {
            super.spawnTo(player);
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
        packet.setHand(ItemUtils.toNetwork(this.getInventory().getSelectedItem()));
        packet.setPlatformChatId("");
        packet.setDeviceId("");
        packet.setGameType(GameType.from(this.getGameMode().getVanillaId()));
        packet.setCommandPermission(this.isOp() ? CommandPermission.GAME_DIRECTORS : CommandPermission.ANY);
        packet.setPlayerPermission(this.isOp() ? PlayerPermission.OPERATOR : PlayerPermission.MEMBER);
        packet.getAbilityLayers().add(this.abilities.buildBaseLayer());
        this.getData().putAllIn(packet.getMetadata());
        return packet;
    }

    @Override
    public boolean isOnline() {
        return this.connected && this.loggedIn;
    }

    @Override
    public boolean isOp() {
        return this.server.isOp(this);
    }

    @Override
    public void setOp(boolean value) {
        if (value == this.isOp()) {
            return;
        }

        if (value) {
            this.server.addOp(this);
        } else {
            this.server.removeOp(this);
        }

        this.recalculatePermissions();
        this.abilities = buildAbilitiesForGameMode(this.getGameMode());
        this.abilities.update();
        this.sendCommandData();
    }

    @Override
    public boolean isSpawned() {
        return this.spawned;
    }

    @Override
    public boolean isPermissionSet(String name) {
        if (this.perm == null) return false;
        return this.perm.isPermissionSet(name);
    }

    @Override
    public boolean isPermissionSet(Permission permission) {
        if (this.perm == null) return false;
        return this.perm.isPermissionSet(permission);
    }

    @Override
    public boolean hasPermission(String name) {
        if (this.perm == null) return false;
        return this.perm.hasPermission(name);
    }

    @Override
    public boolean hasPermission(Permission permission) {
        if (this.perm == null) return false;
        return this.perm.hasPermission(permission);
    }

    @Override
    public PermissionAttachment addAttachment(PluginContainer plugin) {
        if (this.perm == null) throw new IllegalStateException("Player is offline");
        return this.perm.addAttachment(plugin);
    }

    @Override
    public PermissionAttachment addAttachment(PluginContainer plugin, String name) {
        if (this.perm == null) throw new IllegalStateException("Player is offline");
        return this.perm.addAttachment(plugin, name);
    }

    @Override
    public PermissionAttachment addAttachment(PluginContainer plugin, String name, boolean value) {
        if (this.perm == null) throw new IllegalStateException("Player is offline");
        return this.perm.addAttachment(plugin, name, value);
    }

    @Override
    public PermissionAttachment addAttachment(PluginContainer plugin, long ticks) {
        if (this.perm == null) throw new IllegalStateException("Player is offline");
        return this.perm.addAttachment(plugin, ticks);
    }

    @Override
    public PermissionAttachment addAttachment(PluginContainer plugin, String name, boolean value, long ticks) {
        if (this.perm == null) throw new IllegalStateException("Player is offline");
        return this.perm.addAttachment(plugin, name, value, ticks);
    }

    @Override
    public void removeAttachment(PermissionAttachment attachment) {
        if (this.perm == null) throw new IllegalStateException("Player is offline");
        this.perm.removeAttachment(attachment);
    }

    @Override
    public void recalculatePermissions() {
        PermissibleBase localPerm = this.perm;
        if (localPerm == null) return;
        localPerm.recalculatePermissions();

        this.server.getPermissionManager().unsubscribeFromPermission(CloudServer.BROADCAST_CHANNEL_USERS, this);
        this.server.getPermissionManager().unsubscribeFromPermission(CloudServer.BROADCAST_CHANNEL_ADMINISTRATIVE, this);

        if (this.hasPermission(CloudServer.BROADCAST_CHANNEL_USERS)) {
            this.server.getPermissionManager().subscribeToPermission(CloudServer.BROADCAST_CHANNEL_USERS, this);
        }

        if (this.hasPermission(CloudServer.BROADCAST_CHANNEL_ADMINISTRATIVE)) {
            this.server.getPermissionManager().subscribeToPermission(CloudServer.BROADCAST_CHANNEL_ADMINISTRATIVE, this);
        }

        if (this.isEnableClientCommand() && spawned) this.sendCommandData();
    }

    public boolean isEnableClientCommand() {
        return this.enableClientCommand;
    }

    public void setEnableClientCommand(boolean enable) {
        this.enableClientCommand = enable;
        SetCommandsEnabledPacket packet = new SetCommandsEnabledPacket();
        packet.setCommandsEnabled(enable);
        this.sendPacket(packet);
        if (enable) this.sendCommandData();
    }

    @Override
    public void resetFallDistance() {
        super.resetFallDistance();
        if (this.inAirTicks != 0) {
            this.startAirTicks = 5;
        }
        this.inAirTicks = 0;
        this.highestPosition = this.getPosition().getY();
    }

    @Override
    public Set<PermissionAttachmentInfo> getEffectivePermissions() {
        if (this.perm == null) return Set.of();
        return this.perm.getEffectivePermissions();
    }

    public void showPlayer(CloudPlayer player) {
        if (this == player) {
            return;
        }
        this.hiddenPlayers.remove(player.getServerId());
        if (player.isOnline()) {
            player.spawnTo(this);
        }
    }

    public boolean isPlayer() {
        return true;
    }

    public void sendCommandData() {
        this.sendPacket(CommandRegistry.get().createPacketFor(this));
    }

    public void removeAchievement(String achievementId) {
        this.playerData.getAchievements().remove(achievementId);
    }

    public boolean isConnected() {
        return connected;
    }

    @Override
    public Component displayName() {
        return this.displayName;
    }

    @Override
    public void displayName(Component displayName) {
        this.displayName = displayName;
        if (this.spawned) {
            this.getServer().updatePlayerListData(this.getServerId(), this.getUniqueId(), BedrockLegacyTextSerializer.getInstance().serialize(this.displayName), this.getSerializedSkin(), this.getXuid());
        }
    }

    public boolean hasAchievement(String achievementId) {
        return this.playerData.getAchievements().contains(achievementId);
    }

    @Override
    public Skin getSkin() {
        return this.loginChainData.getSkin();
    }

    @Override
    public void setSkin(Skin skin) {
        this.loginChainData.setSkin(skin);
        if (this.spawned) {
            this.getServer().updatePlayerListData(this.getServerId(), this.getUniqueId(), BedrockLegacyTextSerializer.getInstance().serialize(this.displayName()), this.getSerializedSkin(), this.getXuid());
        }
    }

    public void setSkin(SerializedSkin skin) {
        ((ClientChainData) this.loginChainData).setSkin(skin);
        super.setSkin(this.getSkin());
        if (this.spawned) {
            this.getServer().updatePlayerListData(this.getServerId(), this.getUniqueId(), BedrockLegacyTextSerializer.getInstance().serialize(this.displayName()), this.getSerializedSkin(), this.getXuid());
        }
    }

    public SerializedSkin getSerializedSkin() {
        return ((ClientChainData) this.loginChainData).getSerializedSkin();
    }

    public UUID getServerId() {
        return this.loginChainData.getClientUUID();
    }

    public SocketAddress getSocketAddress() {
        return this.session.getSocketAddress();
    }

    public boolean isSleeping() {
        return this.sleeping != null;
    }

    public int getInAirTicks() {
        return this.inAirTicks;
    }

    public Vector3f getNextPosition() {
        return this.newPosition != null ? this.newPosition : this.getPosition();
    }

    /**
     * Returns whether the player is currently using an item (right-click and hold).
     *
     * @return bool
     */
    public boolean isUsingItem() {
        return this.data.getFlag(USING_ITEM) && this.startAction > -1;
    }

    public void setUsingItem(boolean value) {
        this.startAction = value ? this.server.getTick() : -1;
        this.data.setFlag(USING_ITEM, value);
    }

    public String getButtonText() {
        return this.buttonText;
    }

    public void setButtonText(String text) {
        if (!text.equals(buttonText)) {
            this.buttonText = text;
            this.data.set(INTERACT_TEXT, this.buttonText);
        }
    }

    /**
     * Returns the player's current respawn configuration, or {@code null} if they have no personal spawn set.
     */
    @Nullable
    public RespawnConfig getRespawnConfig() {
        return this.respawnConfig;
    }

    public Location getSpawn() {
        if (this.spawnLocation != null && this.spawnLocation.getLevel() != null) {
            if (this.respawnConfig != null && !this.respawnConfig.forced()) {
                CloudLevel spawnLevel = this.respawnConfig.level();
                Vector3i pos = this.respawnConfig.pos();
                Block block = spawnLevel.getBlock(pos);
                BlockType type = block.getState().getType();
                if (type != BlockTypes.BED && type != BlockTypes.RESPAWN_ANCHOR) {
                    this.spawnLocation = null;
                    this.respawnConfig = null;
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
        PlayerSetSpawnEvent event = new PlayerSetSpawnEvent(this, cause, previous, location,
                false, false, null);
        this.server.getEventManager().fire(event);
        if (event.isCancelled()) {
            return;
        }

        Location finalLocation = event.getNewSpawn();
        if (finalLocation == null) {
            this.spawnLocation = null;
            this.respawnConfig = null;
            return;
        }

        this.spawnLocation = finalLocation;
        SetSpawnPositionPacket packet = new SetSpawnPositionPacket();
        packet.setSpawnType(SetSpawnPositionPacket.Type.PLAYER_SPAWN);
        packet.setBlockPosition(this.spawnLocation.getPosition().toInt());
        packet.setDimensionId(((CloudLevel) this.spawnLocation.getLevel()).getDimension());
        this.sendPacket(packet);
    }

    /**
     * Clears the player's personal spawn point (resets to world spawn).
     */
    public void clearSpawn() {
        if (this.spawnLocation == null) {
            return;
        }

        Location previous = this.spawnLocation;
        PlayerSetSpawnEvent event = new PlayerSetSpawnEvent(this, PlayerSetSpawnEvent.Cause.RESET, previous, null,
                false, false, null);
        this.server.getEventManager().fire(event);
        if (event.isCancelled()) {
            return;
        }

        this.spawnLocation = null;
        this.respawnConfig = null;
    }

    /**
     * Attempts to resolve the player's personal spawn point into a safe stand-up position.
     *
     * <p>The block at the stored respawn coordinate is validated:
     * <ul>
     *   <li>For {@link RespawnConfig.SpawnType#BED}: the block must be {@link BlockTypes#BED}.</li>
     *   <li>For {@link RespawnConfig.SpawnType#RESPAWN_ANCHOR}: the block must be
     *       {@link BlockTypes#RESPAWN_ANCHOR} with a charge &gt; 0.</li>
     * </ul>
     * If the block is missing or invalid the personal spawn is cleared and {@code null} is returned,
     * signaling that the caller must fall back to world spawn.</p>
     *
     * @return a safe {@link Location} to teleport the player to, or {@code null} if no valid
     * personal spawn block exists
     */
    @Nullable
    public Location findRespawnPosition() {
        if (this.respawnConfig == null) {
            return null;
        }

        CloudLevel spawnLevel = this.respawnConfig.level();
        Vector3i pos = this.respawnConfig.pos();
        Block block = spawnLevel.getBlock(pos);
        BlockType type = block.getState().getType();

        if (this.respawnConfig.spawnType() == RespawnConfig.SpawnType.BED) {
            if (type != BlockTypes.BED) {
                this.spawnLocation = null;
                this.respawnConfig = null;
                return null;
            }
        } else if (this.respawnConfig.spawnType() == RespawnConfig.SpawnType.RESPAWN_ANCHOR) {
            if (type != BlockTypes.RESPAWN_ANCHOR) {
                this.spawnLocation = null;
                this.respawnConfig = null;
                return null;
            }
            int charge = block.getState().ensureTrait(BlockTraits.RESPAWN_ANCHOR_CHARGE);
            if (charge <= 0) {
                this.spawnLocation = null;
                this.respawnConfig = null;
                return null;
            }
        }

        Location standUp = findStandUpPosition(spawnLevel, pos);
        if (standUp == null) {
            this.spawnLocation = null;
            this.respawnConfig = null;
            return null;
        }

        if (this.respawnConfig.spawnType() == RespawnConfig.SpawnType.RESPAWN_ANCHOR) {
            int currentCharge = block.getState().ensureTrait(BlockTraits.RESPAWN_ANCHOR_CHARGE);
            int newCharge = currentCharge - 1;

            BlockState newAnchorState = block.getState().withTrait(BlockTraits.RESPAWN_ANCHOR_CHARGE, newCharge);
            spawnLevel.setBlockState(pos.getX(), pos.getY(), pos.getZ(), 0, newAnchorState, false, true);

            LevelSoundEventPacket depleteSound = new LevelSoundEventPacket();
            depleteSound.setSound(SoundEvent.RESPAWN_ANCHOR_DEPLETE);
            depleteSound.setExtraData(-1);
            depleteSound.setIdentifier("");
            depleteSound.setPosition(Vector3f.from(pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f));
            depleteSound.setRelativeVolumeDisabled(false);
            depleteSound.setBabySound(false);
            this.sendPacket(depleteSound);

            if (newCharge <= 0) {
                this.spawnLocation = null;
                this.respawnConfig = null;
            }
        }

        return standUp;
    }

    /**
     * Searches candidate positions around {@code pos} for a safe 2-block-tall standing spot.
     *
     * <p>For {@link RespawnConfig.SpawnType#BED}: offsets are computed dynamically from the
     * bed's {@code FACING} direction and the player's yaw (clockwise vs counter-clockwise side).
     * The 12 candidates = 10 "surround" + 2 "above" are all tested at head-block Y.</p>
     *
     * <p>For {@link RespawnConfig.SpawnType#RESPAWN_ANCHOR}: 25 offsets comprising the 8
     * horizontal neighbors, those 8 shifted one below, those 8 shifted one above, plus
     * {@code {0,1,0}}.</p>
     *
     * <p>Both types do a first pass with {@code avoidDanger=true} (prefer non-dangerous floors)
     * then a second pass with {@code avoidDanger=false}.</p>
     *
     * @param level the level containing the block
     * @param pos   the spawn block position (bed head or anchor)
     * @return a safe {@link Location} or {@code null} if none found
     */
    @Nullable
    private Location findStandUpPosition(CloudLevel level, Vector3i pos) {
        if (this.respawnConfig != null && this.respawnConfig.spawnType() == RespawnConfig.SpawnType.BED) {
            return findBedStandUpPosition(level, pos);
        } else {
            return findAnchorStandUpPosition(level, pos);
        }
    }

    /**
     * Bed respawn stand-up position search.
     *
     * <p>All 12 offsets are 2-D XZ relative to the HEAD block and iterated at head-block Y.</p>
     */
    @Nullable
    private Location findBedStandUpPosition(CloudLevel level, Vector3i headPos) {
        Block headBlock = level.getBlock(headPos);
        Direction facing;
        try {
            facing = headBlock.getState().ensureTrait(BlockTraits.DIRECTION);
        } catch (Exception e) {
            facing = Direction.NORTH;
        }

        // sideFacing: clockwise of facing, flipped based on player yaw.
        Direction clockwise = facing.rotateClockwise();
        float yaw = this.respawnConfig != null ? this.respawnConfig.yaw() : this.getYaw();
        Direction sideFacing = clockwise.isFacing(yaw) ? clockwise.rotateCounterClockwise() : clockwise;

        // bedStandUpOffsets = bedSurroundStandUpOffsets(facing, sideFacing) + bedAboveStandUpOffsets(facing)
        // All offsets are [xOff, zOff] relative to headPos (Y stays at headPos.Y).
        int[][] offsets = bedStandUpOffsets(facing, sideFacing);

        // First pass: avoidDanger=true, then avoidDanger=false
        Location loc = findSafePositionBed(level, headPos, offsets, true);
        if (loc != null) return loc;
        return findSafePositionBed(level, headPos, offsets, false);
    }

    /**
     * Computes the 12 bed stand-up offsets from facing and side directions.
     * Returns {@code int[12][2]} where each entry is {@code [xOff, zOff]}.
     */
    private static int[][] bedStandUpOffsets(Direction f, Direction s) {
        return new int[][]{
                // surround
                {s.getStepX(), s.getStepZ()},
                {s.getStepX() - f.getStepX(), s.getStepZ() - f.getStepZ()},
                {s.getStepX() - f.getStepX() * 2, s.getStepZ() - f.getStepZ() * 2},
                {-f.getStepX() * 2, -f.getStepZ() * 2},
                {-s.getStepX() - f.getStepX() * 2, -s.getStepZ() - f.getStepZ() * 2},
                {-s.getStepX() - f.getStepX(), -s.getStepZ() - f.getStepZ()},
                {-s.getStepX(), -s.getStepZ()},
                {-s.getStepX() + f.getStepX(), -s.getStepZ() + f.getStepZ()},
                {f.getStepX(), f.getStepZ()},
                {s.getStepX() + f.getStepX(), s.getStepZ() + f.getStepZ()},
                // above
                {0, 0},
                {-f.getStepX(), -f.getStepZ()},
        };
    }

    /**
     * Iterates 2-D XZ {@code offsets} relative to {@code headPos} (all at {@code headPos.Y}) and
     * returns the first position that passes {@link #isSafeDismountLocation}.
     */
    @Nullable
    private Location findSafePositionBed(CloudLevel level, Vector3i headPos, int[][] offsets, boolean avoidDanger) {
        float yaw = this.respawnConfig != null ? this.respawnConfig.yaw() : this.getYaw();
        for (int[] off : offsets) {
            int cx = headPos.getX() + off[0];
            int cy = headPos.getY(); // Y stays at head block level
            int cz = headPos.getZ() + off[1];
            if (isSafeDismountLocation(level, Vector3i.from(cx, cy, cz), avoidDanger)) {
                return Location.from(cx + 0.5f, cy, cz + 0.5f, yaw, 0f, level);
            }
        }
        return null;
    }

    /**
     * Anchor respawn stand-up position search.
     *
     * <p>Uses 25 offsets: 8 horizontal neighbors, those 8 shifted -1Y, those 8 shifted +1Y,
     * plus {@code {0,1,0}}. Two passes: avoidDanger=true then false.</p>
     */
    @Nullable
    private Location findAnchorStandUpPosition(CloudLevel level, Vector3i pos) {
        // 8 horizontal neighbours (cardinal + diagonal)
        int[][] horizontal = {
                {0, 0, -1}, {-1, 0, 0}, {0, 0, 1}, {1, 0, 0},
                {-1, 0, -1}, {1, 0, -1}, {-1, 0, 1}, {1, 0, 1},
        };

        // Build all 25 offsets: horizontal, horizontal-1Y, horizontal+1Y, then {0,1,0}
        int[][] all = new int[25][3];
        for (int i = 0; i < 8; i++) {
            all[i] = new int[]{horizontal[i][0], 0, horizontal[i][2]}; // same Y
            all[8 + i] = new int[]{horizontal[i][0], -1, horizontal[i][2]}; // one below
            all[16 + i] = new int[]{horizontal[i][0], +1, horizontal[i][2]}; // one above
        }
        all[24] = new int[]{0, 1, 0}; // directly above anchor

        float yaw = this.respawnConfig != null ? this.respawnConfig.yaw() : this.getYaw();
        for (int[] off : all) {
            Vector3i candidate = Vector3i.from(pos.getX() + off[0], pos.getY() + off[1], pos.getZ() + off[2]);
            if (isSafeDismountLocation(level, candidate, true)) {
                return Location.from(candidate.getX() + 0.5f, candidate.getY(), candidate.getZ() + 0.5f, yaw, 0f, level);
            }
        }

        for (int[] off : all) {
            Vector3i candidate = Vector3i.from(pos.getX() + off[0], pos.getY() + off[1], pos.getZ() + off[2]);
            if (isSafeDismountLocation(level, candidate, false)) {
                return Location.from(candidate.getX() + 0.5f, candidate.getY(), candidate.getZ() + 0.5f, yaw, 0f, level);
            }
        }

        return null;
    }

    /**
     * Returns {@code true} if a player can safely stand at {@code pos}.
     *
     * <p>A position is safe when:
     * <ol>
     *   <li>The candidate block itself is passable (feet level).</li>
     *   <li>The block directly above is also passable (head level).</li>
     *   <li>The block below is solid (there is a floor to stand on).</li>
     *   <li>If {@code avoidDanger=true}: neither the candidate block nor the floor block is a
     *       "dangerous" block (fire, lava, cactus, etc.).</li>
     * </ol></p>
     *
     * @param level       the level to check in
     * @param pos         the candidate feet-level position
     * @param avoidDanger if {@code true}, reject positions with dangerous blocks
     * @return {@code true} if the position is safe to respawn at
     */
    private static boolean isSafeDismountLocation(CloudLevel level, Vector3i pos, boolean avoidDanger) {
        Block feetBlock = level.getBlock(pos);
        Block headBlock = level.getBlock(Vector3i.from(pos.getX(), pos.getY() + 1, pos.getZ()));
        Block floorBlock = level.getBlock(Vector3i.from(pos.getX(), pos.getY() - 1, pos.getZ()));

        if (!isPassable(feetBlock) || !isPassable(headBlock)) {
            return false;
        }

        if (isPassable(floorBlock)) {
            return false;
        }

        return !avoidDanger || (!isDangerous(feetBlock) && !isDangerous(floorBlock));
    }

    private static boolean isPassable(Block block) {
        return CloudBlockRegistry.REGISTRY
                .getComponent(block.getState().getType(), BlockComponents.CAN_PASS_THROUGH)
                .execute(block.getState());
    }

    /**
     * Returns {@code true} if standing in or on this block would immediately harm the player.
     */
    private static boolean isDangerous(Block block) {
        BlockType type = block.getState().getType();
        return type == BlockTypes.FIRE
                || type == BlockTypes.SOUL_FIRE
                || type == BlockTypes.LAVA
                || type == BlockTypes.FLOWING_LAVA
                || type == BlockTypes.CACTUS
                || type == BlockTypes.MAGMA
                || type == BlockTypes.WITHER_ROSE;
    }

    protected void doFirstSpawn() {
        this.spawned = true;

        this.setEnableClientCommand(true);

        this.abilities.update();

        this.sendPotionEffects(this);
        this.sendData(this);
        this.invManager.sendAllInventories();

        SetTimePacket setTimePacket = new SetTimePacket();
        setTimePacket.setTime(this.getLevel().getTime());
        this.sendPacket(setTimePacket);

        Location loc = this.findRespawnPosition();
        Set<PlayerRespawnEvent.RespawnFlag> flags = EnumSet.of(PlayerRespawnEvent.RespawnFlag.FIRST_SPAWN);
        if (loc != null) {
            // Personal spawn block was valid
            if (this.respawnConfig != null) {
                if (this.respawnConfig.spawnType() == RespawnConfig.SpawnType.BED) {
                    flags.add(PlayerRespawnEvent.RespawnFlag.BED_SPAWN);
                } else {
                    flags.add(PlayerRespawnEvent.RespawnFlag.ANCHOR_SPAWN);
                }
            }
        } else {
            CloudLevel spawnLevel = this.getLevel();
            loc = spawnLevel == this.getServer().getDefaultLevel() ? spawnLevel.getSafeSpawn() : Location.from(this.getPosition(), this.getYaw(), this.getPitch(), spawnLevel);
        }

        PlayerRespawnEvent respawnEvent = new PlayerRespawnEvent(this, loc, flags);
        this.server.getEventManager().fire(respawnEvent);
        loc = respawnEvent.getRespawnLocation();

        if (this.getHealth() <= 0) {
            RespawnPacket respawnPacket = new RespawnPacket();
            respawnPacket.setPosition(loc.getPosition());
            respawnPacket.setState(RespawnPacket.State.SERVER_SEARCHING);
            this.sendPacket(respawnPacket);
        }

        CreativeContentPacket creativePacket = CloudItemRegistry.get().getCreativeContent();
        this.sendPacket(creativePacket);

        this.sendPlayStatus(PlayStatusPacket.Status.PLAYER_SPAWN);

        this.noDamageTicks = 60;

        this.getServer().sendRecipeList(this);

        this.getChunkManager().getViewChunks().forEach((LongConsumer) chunkKey -> {
            int chunkX = CloudChunk.fromKeyX(chunkKey);
            int chunkZ = CloudChunk.fromKeyZ(chunkKey);
            for (Entity entity : this.getLevel().getLoadedChunkEntities(chunkX, chunkZ)) {
                if (this != entity && !entity.isClosed() && entity.isAlive()) {
                    entity.spawnTo(this);
                }
            }
        });

        int experience = this.getExperience();
        if (experience != 0) {
            this.sendExperience(experience);
        }

        int level = this.getExperienceLevel();
        if (level != 0) {
            this.sendExperienceLevel(this.getExperienceLevel());
        }

        this.teleport(loc, null); // Prevent PlayerTeleportEvent during player spawn

        if (!this.isSpectator()) {
            this.spawnToAll();
        }

        //todo Updater

        //Weather
        if (this.getLevel().isRaining() || this.getLevel().isThundering()) {
            this.getLevel().sendWeather(this);
        }

        //FoodLevel
        PlayerFood food = this.getFoodData();
        if (food.getLevel() != food.getMaxLevel()) {
            food.sendFoodLevel();
        }
    }

    @Override
    public int getPing() {
        if (!this.isConnected()) {
            return 0;
        }

        RakSessionCodec session = ((RakChildChannel) this.session.getPeer().getChannel())
                .rakPipeline()
                .get(RakSessionCodec.class);
        if (session == null) {
            return 0;
        }

        return (int) session.getPing();
    }

    public boolean sleepOn(Vector3i pos) {
        if (!this.isOnline()) {
            return false;
        }

        CloudLevel level = this.getLevel();
        int dim = level.getDimension();

        if (dim != CloudLevel.DIMENSION_OVERWORLD) {
            BlockState headState = level.getBlockState(pos.getX(), pos.getY(), pos.getZ());
            Direction facing = Direction.NORTH;
            try {
                facing = headState.ensureTrait(BlockTraits.DIRECTION);
            } catch (Exception ignored) {
            }

            Vector3i footPos = Vector3i.from(
                    pos.getX() - facing.getStepX(),
                    pos.getY(),
                    pos.getZ() - facing.getStepZ()
            );

            level.setBlockState(pos.getX(), pos.getY(), pos.getZ(), 0, BlockStates.AIR, false, true);
            BlockState footState = level.getBlockState(footPos.getX(), footPos.getY(), footPos.getZ());
            if (footState.getType() == headState.getType()) {
                level.setBlockState(footPos.getX(), footPos.getY(), footPos.getZ(), 0,
                        BlockStates.AIR, false, true);
            }

            Explosion explosion = new Explosion(level,
                    Vector3f.from(pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f),
                    5, this);
            explosion.explodeA();
            explosion.explodeB();
            return true;
        }

        int time = level.getTime() % Level.TIME_FULL;
        boolean canSleep = level.isThundering() || (time >= Level.TIME_NIGHT && time < Level.TIME_SUNRISE);
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

        for (Entity p : level.getNearbyEntities(this.boundingBox.grow(2, 1, 2), this)) {
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
            level.setBlockState(pos.getX(), pos.getY(), pos.getZ(), 0, occupied, false, true);
        } catch (Exception ignored) {
        }

        this.sleeping = pos.clone();
        this.teleport(Location.from(pos.toFloat().add(0.5, 0.5, 0.5), this.getYaw(), this.getPitch(), level), null);
        this.data.set(BED_POSITION, pos);

        Location bedSpawnLoc = Location.from(pos.toFloat(), this.getYaw(), 0f, level);
        setSpawnFromBed(pos, bedSpawnLoc);

        level.sleepTicks = 60;

        return true;
    }

    /**
     * Sets the player's spawn to a bed position.
     */
    private void setSpawnFromBed(Vector3i blockPos, Location location) {
        Location previous = this.spawnLocation;
        PlayerSetSpawnEvent event = new PlayerSetSpawnEvent(this, PlayerSetSpawnEvent.Cause.BED, previous, location,
                false, true, Component.translatable("tile.bed.respawnSet"));
        this.server.getEventManager().fire(event);
        if (event.isCancelled()) {
            return;
        }

        Location finalLocation = event.getNewSpawn();
        if (finalLocation == null) {
            this.spawnLocation = null;
            this.respawnConfig = null;
            return;
        }

        this.spawnLocation = finalLocation;
        this.respawnConfig = new RespawnConfig(
                (CloudLevel) finalLocation.getLevel(),
                blockPos,
                finalLocation.getYaw(),
                false,
                RespawnConfig.SpawnType.BED
        );

        SetSpawnPositionPacket packet = new SetSpawnPositionPacket();
        packet.setSpawnType(SetSpawnPositionPacket.Type.PLAYER_SPAWN);
        packet.setBlockPosition(blockPos);
        packet.setDimensionId(((CloudLevel) finalLocation.getLevel()).getDimension());
        this.sendPacket(packet);

        if (event.willNotifyPlayer() && event.getNotification() != null) {
            this.sendMessage(event.getNotification());
        }
    }

    /**
     * Called when the player right-clicks a charged respawn anchor in the Nether.
     *
     * @param blockPos position of the anchor block
     * @param location location to store as the spawn point
     * @return {@code true} if the spawn was updated (event not canceled and location changed)
     */
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
                false, true, Component.translatable("tile.respawn_anchor.respawnSet"));
        this.server.getEventManager().fire(event);
        if (event.isCancelled()) {
            return false;
        }

        Location finalLocation = event.getNewSpawn();
        if (finalLocation == null) {
            this.spawnLocation = null;
            this.respawnConfig = null;
            return false;
        }

        this.spawnLocation = finalLocation;
        this.respawnConfig = new RespawnConfig(
                (CloudLevel) finalLocation.getLevel(),
                blockPos,
                0f,
                false,
                RespawnConfig.SpawnType.RESPAWN_ANCHOR
        );

        SetSpawnPositionPacket packet = new SetSpawnPositionPacket();
        packet.setSpawnType(SetSpawnPositionPacket.Type.PLAYER_SPAWN);
        packet.setBlockPosition(blockPos);
        packet.setDimensionId(((CloudLevel) finalLocation.getLevel()).getDimension());
        this.sendPacket(packet);

        if (event.willNotifyPlayer() && event.getNotification() != null) {
            this.sendMessage(event.getNotification());
        }

        return true;
    }

    public void stopSleep() {
        if (this.sleeping != null) {
            Block bedBlock = this.getLevel().getBlock(this.sleeping);
            this.server.getEventManager().fire(new PlayerBedLeaveEvent(this, bedBlock));

            try {
                BlockState clearedState = bedBlock.getState().withTrait(BlockTraits.IS_OCCUPIED, false);
                this.getLevel().setBlockState(
                        this.sleeping.getX(), this.sleeping.getY(), this.sleeping.getZ(), 0, clearedState, false, true);
            } catch (Exception ignored) {
            }

            this.sleeping = null;
            this.data.set(BED_POSITION, Vector3i.ZERO);
            //this.data.setBoolean(CAN_START_SLEEP, false); // TODO what did this change to?


            this.getLevel().sleepTicks = 0;

            AnimatePacket pk = new AnimatePacket();
            pk.setRuntimeEntityId(this.getRuntimeId());
            pk.setAction(AnimatePacket.Action.WAKE_UP);
            this.sendPacket(pk);
        }
    }

    public boolean awardAchievement(String achievementId) {
        if (!CloudServer.getInstance().getConfig().isAchievements()) {
            return false;
        }

        Achievement achievement = Achievement.achievements.get(achievementId);

        if (achievement == null || hasAchievement(achievementId)) {
            return false;
        }

        for (String id : achievement.requires()) {
            if (!this.hasAchievement(id)) {
                return false;
            }
        }
        PlayerAchievementAwardedEvent event = new PlayerAchievementAwardedEvent(this, achievementId);
        this.server.getEventManager().fire(event);

        if (event.isCancelled()) {
            return false;
        }

        this.playerData.getAchievements().add(achievementId);
        achievement.broadcast(this);
        return true;
    }

    @Override
    public GameMode getGameMode() {
        return this.playerData.getGamemode();
    }

    @Override
    public void setGameMode(GameMode gameMode) {
        this.setGamemode(gameMode, PlayerGameModeChangeEvent.Cause.PLUGIN);
    }

    /**
     * 0 is true
     * -1 is false
     * other is identifer
     *
     * @param packet packet to send
     * @return packet successfully sent
     */
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
        try (Timing ignored = Timings.getSendDataPacketTiming(packet).startTiming()) {
            this.session.sendPacket(packet);
        }
    }

    @Override
    public ItemStack[] getDrops() {
        if (!this.isCreative()) {
            return super.getDrops();
        }

        return new ItemStack[0];
    }

    /**
     * 0 is true
     * -1 is false
     * other is identifer
     *
     * @param packet packet to send
     * @return packet successfully sent
     */
    public boolean sendPacketImmediately(BedrockPacket packet) {
        if (!this.connected) {
            return false;
        }

        try (Timing ignored = Timings.getSendDataPacketTiming(packet).startTiming()) {
            this.session.sendPacketImmediately(packet);
        }
        return true;
    }

    @Override
    public int getPortalCooldownTicks() {
        return 10;
    }

    public boolean isChangingDimension() {
        return changingDimension;
    }

    public void setChangingDimension(boolean changingDimension) {
        this.changingDimension = changingDimension;
    }

    @Override
    protected void tickPortalCooldown() {
        if (!changingDimension) {
            super.tickPortalCooldown();
        }
    }

    @Override
    protected int getPortalTransitionTicks() {
        return isCreative() ? 0 : PORTAL_TRANSFER_TICKS;
    }

    @Override
    protected void checkBlockCollision() {
        boolean portal = false;

        for (Block block : this.getBlocksAround()) {
            if (block.getState().getType() == BlockTypes.PORTAL) {
                Vector3i pos = block.getPosition();
                SimpleAxisAlignedBB portalUnitBB = new SimpleAxisAlignedBB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
                if (portalUnitBB.intersectsWith(this.getBoundingBox())) {
                    portal = true;
                    this.portalEntryBlock = pos;
                }
            }
        }

        for (Block block : this.getCollisionBlocks()) {
            block.getComponents().get(BlockComponents.ON_ENTITY_COLLIDE).execute(block, this);
        }

        if (portal && !this.isSpectator() && this.getVehicle() == null) {
            if (this.portalCooldown > 0) {
                this.portalCooldown = getPortalCooldownTicks();
            } else {
                this.inPortalTicks++;
            }
        } else {
            if (this.portalCooldown <= 0) {
                this.inPortalTicks = Math.max(0, this.inPortalTicks - 4);
                if (this.inPortalTicks == 0) {
                    this.portalEntryBlock = null;
                }
            }
        }
    }

    protected void checkNearEntities() {
        for (Entity entity : this.getLevel().getNearbyEntities(this.boundingBox.grow(1, 0.5f, 1), this)) {
            this.getLevel().scheduleEntityUpdate(entity);

            if (!entity.isAlive() || !this.isAlive()) {
                continue;
            }

            this.pickupEntity(entity, true);
        }
    }

    @Override
    public GameMode getPreviousGameMode() {
        return this.previousGameMode;
    }

    public boolean setGamemode(GameMode gamemode, PlayerGameModeChangeEvent.Cause cause) {
        if (this.getGameMode() == gamemode) {
            return false;
        }

        PlayerGameModeChangeEvent ev;
        this.server.getEventManager().fire(ev = new PlayerGameModeChangeEvent(this, gamemode, cause, null));

        if (ev.isCancelled()) {
            return false;
        }

        this.previousGameMode = this.getGameMode();
        this.playerData.setGamemode(gamemode);

        if (gamemode == GameMode.CREATIVE || gamemode == GameMode.SPECTATOR) {
            this.foodData.reset();
            this.setAir((short) 400);
        }

        this.keepMovement = this.isSpectator();

        this.abilities = buildAbilitiesForGameMode(gamemode);

        UpdatePlayerGameTypePacket gameTypePk = new UpdatePlayerGameTypePacket();
        gameTypePk.setGameType(GameType.from(gamemode.getVanillaId()));
        gameTypePk.setEntityId(this.getUniqueId());
        gameTypePk.setTick(this.clientTick);
        if (this.spawned) {
            this.sendPacket(gameTypePk);
            CloudServer.broadcastPacket(this.getViewers(), gameTypePk);
        }

        boolean collisionAfter = gamemode != GameMode.SPECTATOR;
        boolean collisionChanged = this.data.getFlag(EntityFlag.HAS_COLLISION) != collisionAfter;
        this.data.setFlag(EntityFlag.HAS_COLLISION, collisionAfter);
        if (this.spawned && collisionChanged) {
            SetEntityDataPacket entityDataPk = new SetEntityDataPacket();
            entityDataPk.setRuntimeEntityId(this.getRuntimeId());
            entityDataPk.setTick(this.clientTick);
            this.data.putFlagsIn(entityDataPk.getMetadata());
            this.sendPacket(entityDataPk);
            CloudServer.broadcastPacket(this.getViewers(), entityDataPk);
        }

        this.resetFallDistance();

        this.invManager.sendAllInventories();
        this.sendPacket(CloudItemRegistry.get().getCreativeContent());
        return true;
    }

    public boolean canOpenInventory() {
        return this.invManager.getOpenContainer() == null;
    }

    @Override
    public void closeInventory() {
        closeInventory(InventoryCloseEvent.Reason.PLUGIN);
    }

    public void closeInventory(InventoryCloseEvent.Reason reason) {
        CloudInventoryScreen screen = this.invManager.getOpenContainer();
        if (screen == null) {
            return;
        }

        this.invManager.closeScreen(reason);

        for (SlotGroup view : screen.getAllSlotGroups()) {
            if (!(view instanceof CloudSlotGroupBase)) continue;
            Container container = ((CloudSlotGroupBase) view).getContainer();
            Byte windowId = this.containerToWindowId.get(container);
            if (windowId != null) {
                unregisterContainerId(container);
                ContainerClosePacket close = new ContainerClosePacket();
                close.setId(windowId);
                close.setServerInitiated(true);
                close.setType(ContainerType.CONTAINER);
                this.sendPacket(close);
                return;
            }
        }

        ContainerClosePacket close = new ContainerClosePacket();
        close.setId((byte) ContainerId.INVENTORY);
        close.setServerInitiated(true);
        close.setType(ContainerType.INVENTORY);
        this.sendPacket(close);
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
        CloudInventoryScreen current = this.invManager.getOpenContainer();
        if (current != null) {
            return current;
        }
        return this.invManager.getDefaultScreen();
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
        if (!canOpenInventory()) return;
        if (!block.getComponents().get(BlockComponents.CAN_BE_USED).execute(block, this)) {
            throw new IllegalArgumentException("Block is not a container: " + block.getState().getType().getId());
        }
        block.getComponents().get(BlockComponents.USE).execute(block, this, Direction.DOWN, ItemStack.EMPTY);
    }

    @Override
    public void openContainer(BlockEntity blockEntity) {
        if (!canOpenInventory()) return;
        openContainer(blockEntity.getBlock());
    }

    @Override
    public VirtualChestScreen createVirtualChest(Component title) {
        return new CloudVirtualChestScreen(this, BedrockLegacyTextSerializer.getInstance().serialize(title));
    }

    @Override
    public VirtualDoubleChestScreen createVirtualDoubleChest(Component title) {
        return new CloudVirtualDoubleChestScreen(this, BedrockLegacyTextSerializer.getInstance().serialize(title));
    }

    @Override
    public VirtualHopperScreen createVirtualHopper(Component title) {
        return new CloudVirtualHopperScreen(this, BedrockLegacyTextSerializer.getInstance().serialize(title));
    }

    public void handleClientContainerClose(ContainerClosePacket packet) {
        byte windowId = packet.getId();
        unregisterContainerById(windowId);

        this.invManager.closeScreen(InventoryCloseEvent.Reason.PLAYER);

        ContainerClosePacket echo = new ContainerClosePacket();
        echo.setId(windowId);
        echo.setServerInitiated(false);
        echo.setType(packet.getType() != null ? packet.getType() : ContainerType.CONTAINER);
        this.sendPacket(echo);
    }

    @Override
    public boolean setMotion(Vector3f motion) {
        if (super.setMotion(motion)) {
            if (this.chunk != null) {
                this.addMotion(this.getMotion());  //Send to others
                SetEntityMotionPacket packet = new SetEntityMotionPacket();
                packet.setRuntimeEntityId(this.getRuntimeId());
                packet.setMotion(motion);
                this.sendPacket(packet);  //Send to self
            }

            if (this.getMotion().getY() > 0) {
                //todo: check this
                this.startAirTicks = (int) ((-(Math.log(this.getGravity() / (this.getGravity() + this.getDrag() * this.getMotion().getY()))) / this.getDrag()) * 2 + 5);
            }

            return true;
        }

        return false;
    }

    public void sendAttributes() {
        UpdateAttributesPacket pk = new UpdateAttributesPacket();
        pk.setRuntimeEntityId(this.getRuntimeId());
        List<AttributeData> attributes = pk.getAttributes();
        attributes.add(NetworkUtils.attributeToNetwork(Attribute.getAttribute(Attribute.MAX_HEALTH).setMaxValue(this.getMaxHealth()).setValue(health > 0 ? (health < getMaxHealth() ? health : getMaxHealth()) : 0)));
        attributes.add(NetworkUtils.attributeToNetwork(Attribute.getAttribute(Attribute.MAX_HUNGER).setValue(this.getFoodData().getLevel())));
        attributes.add(NetworkUtils.attributeToNetwork(Attribute.getAttribute(Attribute.MOVEMENT_SPEED).setValue(this.getMovementSpeed())));
        attributes.add(NetworkUtils.attributeToNetwork(Attribute.getAttribute(Attribute.EXPERIENCE_LEVEL).setValue(this.getExperienceLevel())));
        attributes.add(NetworkUtils.attributeToNetwork(Attribute.getAttribute(Attribute.EXPERIENCE).setValue(((float) this.getExperience()) / calculateRequireExperience(this.getExperienceLevel()))));
        this.sendPacket(pk);
    }

    @Override
    protected void checkGroundState(double movX, double movY, double movZ, double dx, double dy, double dz) {
        if (!this.onGround || movX != 0 || movY != 0 || movZ != 0) {
            boolean onGround = false;

            AxisAlignedBB bb = this.boundingBox.clone();
            bb.setMaxY(bb.getMinY() + 0.5f);
            bb.setMinY(bb.getMinY() - 1);

            AxisAlignedBB realBB = this.boundingBox.clone();
            realBB.setMaxY(realBB.getMinY() + 0.1f);
            realBB.setMinY(realBB.getMinY() - 0.2f);

            int minX = GenericMath.floor(bb.getMinX());
            int minY = GenericMath.floor(bb.getMinY());
            int minZ = GenericMath.floor(bb.getMinZ());
            int maxX = GenericMath.ceil(bb.getMaxX());
            int maxY = GenericMath.ceil(bb.getMaxY());
            int maxZ = GenericMath.ceil(bb.getMaxZ());

            for (int z = minZ; z <= maxZ; ++z) {
                for (int x = minX; x <= maxX; ++x) {
                    for (int y = minY; y <= maxY; ++y) {
                        Block block = this.getLevel().getBlock(x, y, z);
                        ComponentMap behavior = block.getComponents();

                        if (!CloudBlockRegistry.REGISTRY.getComponent(block.getState().getType(), BlockComponents.SOLID).get() && behavior.get(BlockComponents.GET_BOUNDING_BOX).execute(block.getState()).addCoord(x, y, z).intersectsWith(realBB)) {
                            onGround = true;
                            break;
                        }
                    }
                }
            }

            this.onGround = onGround;
        }

        this.isCollided = this.onGround;
    }

    public void checkInteractNearby() {
        int interactDistance = isCreative() ? 5 : 3;
        if (canInteract(this.getPosition(), interactDistance)) {
            if (getEntityPlayerLookingAt(interactDistance) != null) {
                Interactable onInteract = getEntityPlayerLookingAt(interactDistance);
                setButtonText(onInteract.getInteractButtonText());
            } else {
                setButtonText("");
            }
        } else {
            setButtonText("");
        }
    }

    protected void processMovement(int tickDiff) {
        if (!this.isAlive() || !this.spawned || this.newPosition == null || this.teleportPosition != null || this.isSleeping()) {
            return;
        }

        Vector3f newPosition = this.newPosition;
        Vector3f currentPos = this.getPosition();
        float distanceSquared = newPosition.distanceSquared(currentPos);

        boolean revert = false;
        String revertReason = null;

        float tickDiffSq = (float) tickDiff * (float) tickDiff;
        float maxSpeedThreshold = this.server.getConfig().getMovement().getMaxSpeedThreshold();

        if (this.server.getConfig().getMovement().isStrictMovement()) {
            maxSpeedThreshold *= 0.5f;
        }

        // TODO: Better way of getting max speed and when exempt
        boolean speedExempt = this.isGliding() || this.isCreative() || this.isSpectator() || (newPosition.getY() - currentPos.getY()) < -3.0f;
        if ((distanceSquared / tickDiffSq) > maxSpeedThreshold && !speedExempt) {
            log.trace("[{}] movement reverted: claimed speed {} blocks/tick exceeds threshold {}", this.getName(), String.format("%.2f", Math.sqrt(distanceSquared / tickDiffSq)), String.format("%.2f", Math.sqrt(maxSpeedThreshold)));
            revert = true;
            revertReason = "speed";
        } else {
            int targetChunkX = newPosition.getFloorX() >> 4;
            int targetChunkZ = newPosition.getFloorZ() >> 4;
            CloudChunk targetChunk = this.getLevel().getLoadedChunk(targetChunkX, targetChunkZ);
            if (targetChunk != null) {
                this.chunk = targetChunk;
            }
        }

        float tdx = newPosition.getX() - currentPos.getX();
        float tdz = newPosition.getZ() - currentPos.getZ();
        double distance = Math.sqrt(tdx * tdx + tdz * tdz);

        if (!revert && distanceSquared != 0) {
            float dx = newPosition.getX() - currentPos.getX();
            float dy = newPosition.getY() - currentPos.getY();
            float dz = newPosition.getZ() - currentPos.getZ();

            this.fastMove(dx, dy, dz);
            if (this.newPosition == null) {
                return; //maybe solve that in better way
            }

            double diffX = currentPos.getX() - newPosition.getX();
            double diffY = currentPos.getY() - newPosition.getY();
            double diffZ = currentPos.getZ() - newPosition.getZ();

            double yS = 0.5 + this.ySize;
            if (diffY > -yS && diffY < yS) {
                diffY = 0;
            }

            if (diffX != 0 || diffY != 0 || diffZ != 0) {
                this.position = newPosition;
                float radius = this.getWidth() / 2;
                this.boundingBox.setBounds(this.position.getX() - radius, this.position.getY(), this.position.getZ() - radius,
                        this.position.getX() + radius, this.position.getY() + this.getHeight(), this.position.getZ() + radius);
            }
        }

        Location from = Location.from(this.lastPosition, this.lastYaw, this.lastPitch, this.getLevel());
        Location to = this.getLocation();

        double delta = Math.pow(this.lastPosition.getX() - to.getX(), 2) + Math.pow(this.lastPosition.getY() - to.getY(), 2) + Math.pow(this.lastPosition.getZ() - to.getZ(), 2);
        double deltaAngle = Math.abs(this.lastYaw - to.getYaw()) + Math.abs(this.lastPitch - to.getPitch());

        if (!revert && (delta > 0.0001d || deltaAngle > 1d)) {
            boolean isFirst = this.firstMove;

            this.firstMove = false;
            this.lastPosition = to.getPosition();

            this.lastYaw = to.getYaw();
            this.lastPitch = to.getPitch();

            if (!isFirst) {
                List<Block> blocksAround = this.blocksAround != null ? new ArrayList<>(this.blocksAround) : new ArrayList<>();
                List<Block> collidingBlockStates = this.collisionBlockStates != null ? new ArrayList<>(this.collisionBlockStates) : new ArrayList<>();

                PlayerMoveEvent ev = new PlayerMoveEvent(this, from, to);

                this.blocksAround = null;
                this.collisionBlockStates = null;

                this.server.getEventManager().fire(ev);

                if (!(revert = ev.isCancelled())) { //Yes, this is intended
                    if (!to.equals(ev.getTo())) { //If plugins modify the destination
                        this.teleport(ev.getTo(), null);
                    } else {
                        this.addMovement(this.getX(), this.getY() + getBaseOffset(), this.getZ(), this.getYaw(), this.getPitch(), this.getYaw());
                    }
                } else {
                    revertReason = "PlayerMoveEvent cancelled";
                    this.blocksAround = blocksAround;
                    this.collisionBlockStates = collidingBlockStates;
                }
            }

            this.speed = from.getPosition().min(to.getPosition());
        } else {
            this.speed = Vector3f.ZERO;
        }

        if (!revert && (this.isFoodEnabled() || this.getServer().getDifficulty() == Difficulty.PEACEFUL)) {
            if ((this.isSurvival() || this.isAdventure())/* && !this.getRiddingOn() instanceof Entity*/) {

                //UpdateFoodExpLevel
                if (distance >= 0.05) {
                    double jump = 0;
                    double swimming = this.isInsideOfWater() ? 0.015 * distance : 0;
                    if (swimming != 0) distance = 0;
                    if (this.isSprinting()) {  //Running
                        if (this.inAirTicks == 3 && swimming == 0) {
                            jump = 0.7;
                        }
                        this.getFoodData().updateFoodExpLevel(0.06 * distance + jump + swimming);
                    } else {
                        if (this.inAirTicks == 3 && swimming == 0) {
                            jump = 0.2;
                        }
                        this.getFoodData().updateFoodExpLevel(0.01 * distance + jump + swimming);
                    }
                }
            }
        }

        if (revert) {
            this.lastPosition = from.getPosition();
            this.lastYaw = from.getYaw();
            this.lastPitch = from.getPitch();

            Vector3f correctedPos = from.getPosition();
            log.debug("[{}] movement corrected: claimed {} corrected to {} ({})", this.getName(), newPosition, correctedPos, revertReason);

            float acceptanceThreshold = this.server.getConfig().getMovement().getPositionAcceptanceThreshold();
            if (newPosition.distance(correctedPos) > acceptanceThreshold) {
                sendMovementCorrection(correctedPos, this.clientTick);
            }
            this.forceMovement = correctedPos;
        } else {
            this.forceMovement = null;
        }

        this.newPosition = null;
    }

    @Override
    public void addMovement(double x, double y, double z, double yaw, double pitch, double headYaw) {
        this.sendPosition(Vector3f.from(x, y - getBaseOffset() /*TODO: find better solution */, z), yaw, pitch, MovePlayerPacket.Mode.NORMAL, getViewers());
    }

    public void sendMovementCorrection(Vector3f authoritativePos, long tick) {
        CorrectPlayerMovePredictionPacket correction = new CorrectPlayerMovePredictionPacket();
        correction.setPredictionType(PredictionType.PLAYER);
        correction.setPosition(authoritativePos.add(0, getEyeHeight(), 0));
        correction.setDelta(Vector3f.ZERO);
        correction.setVehicleRotation(Vector2f.ZERO);
        correction.setVehicleAngularVelocity(0.0f);
        correction.setOnGround(this.isOnGround());
        correction.setTick(tick);
        this.sendPacket(correction);
    }

    @Override
    public boolean onUpdate(int currentTick) {
        this.packetsRecieved = 0;

        if (!this.loggedIn) {
            return false;
        }

        int tickDiff = currentTick - this.lastUpdate;

        if (tickDiff <= 0) {
            return true;
        }

        this.messageCounter = 2;

        this.lastUpdate = currentTick;

        try (Timing ignored = this.timing.startTiming()) {
            if (this.fishing != null && this.server.getTick() % 20 == 0) {
                if (this.getPosition().distance(fishing.getPosition()) > 33) {
                    this.stopFishing(false);
                }
            }

            if (!this.isAlive() && this.spawned) {
                ++this.deadTicks;
                if (this.deadTicks >= 10) {
                    this.despawnFromAll();
                }
                return true;
            }

            if (this.spawned) {
                Vector3f chunkCenter = this.newPosition != null ? this.newPosition : this.getPosition();
                this.processMovement(tickDiff);
                this.getChunkManager().queueNewChunks(chunkCenter);

                if (!this.isSpectator()) {
                    this.checkNearEntities();
                }

                this.entityBaseTick(tickDiff);

                if (this.getServer().getDifficulty() == Difficulty.PEACEFUL && this.getLevel().getGameRules().get(GameRules.NATURAL_REGENERATION)) {
                    if (this.getHealth() < this.getMaxHealth() && this.ticksLived % 20 == 0) {
                        this.heal(1);
                    }

                    PlayerFood foodData = this.getFoodData();

                    if (foodData.getLevel() < 20 && this.ticksLived % 10 == 0) {
                        foodData.addFoodLevel(1, 0);
                    }
                }

                if (this.isOnFire() && this.lastUpdate % 10 == 0) {
                    if (this.isCreative() && !this.isInsideOfFire()) {
                        this.extinguish();
                    } else if (this.getLevel().isRaining()) {
                        if (this.getLevel().canBlockSeeSky(this.getPosition())) {
                            this.extinguish();
                        }
                    }
                }

                if (!this.isSpectator() && this.speed != null) {
                    if (this.onGround) {
                        if (this.inAirTicks != 0) {
                            this.startAirTicks = 5;
                        }
                        this.inAirTicks = 0;
                        this.highestPosition = this.getPosition().getY();
                    } else {
                        float curY = this.getPosition().getY();
                        if (curY > highestPosition) {
                            this.highestPosition = curY;
                        }

                        if (this.isGliding()) this.resetFallDistance();

                        ++this.inAirTicks;

                    }

                    if (this.isSurvival() || this.isAdventure()) {
                        if (this.getFoodData() != null) this.getFoodData().update(tickDiff);
                    }
                }
            }

            this.checkTeleportPosition();

            if (currentTick % 10 == 0) {
                this.checkInteractNearby();
            }

            if (this.spawned && this.dummyBossBars.size() > 0 && currentTick % 100 == 0) {
                this.dummyBossBars.values().forEach(DummyBossBar::updateBossEntityPosition);
            }

            this.data.update();
        }

        return true;
    }

    /**
     * Returns the Entity the player is looking at currently
     *
     * @param maxDistance the maximum distance to check for entities
     * @return Entity|null    either NULL if no entity is found or an instance of the entity
     */
    public Interactable getEntityPlayerLookingAt(int maxDistance) {
        try (Timing ignored = Timings.playerEntityLookingAtTimer.startTiming()) {
            Interactable entity = null;

            Set<Entity> nearbyEntities = this.getLevel().getNearbyEntities(boundingBox.grow(maxDistance, maxDistance, maxDistance), this);

            // get all blocks in looking direction until the max interact distance is reached (it's possible that startblock isn't found!)

            Vector3f position = this.getPosition().add(0, getEyeHeight(), 0);
            for (Vector3i pos : BlockRayTrace.of(position, getDirectionVector(), maxDistance)) {
                Block block = this.getLevel().getLoadedBlock(pos);
                if (block == null) {
                    break;
                }

                entity = getEntityAtPosition(nearbyEntities, pos.getX(), pos.getY(), pos.getZ());

                if (entity != null) {
                    break;
                }
            }
            return entity;
        }
    }

    public boolean canInteract(Vector3f pos, double maxDistance) {
        return this.canInteract(pos, maxDistance, 6.0);
    }

    public boolean canInteract(Vector3f pos, double maxDistance, double maxDiff) {
        if (this.getPosition().distanceSquared(pos) > maxDistance * maxDistance) {
            return false;
        }

        Vector2f dV = this.getDirectionPlane();
        double dot = dV.dot(this.getPosition().toVector2(true));
        double dot1 = dV.dot(pos.toVector2(true));
        return (dot1 - dot) >= -maxDiff;
    }

    private Interactable getEntityAtPosition(Set<Entity> nearbyEntities, int x, int y, int z) {
        try (Timing ignored = Timings.playerEntityAtPositionTimer.startTiming()) {
            for (Entity nearestEntity : nearbyEntities) {
                Vector3f position = nearestEntity.getPosition();
                if (position.getFloorX() == x && position.getFloorY() == y && position.getFloorZ() == z
                        && nearestEntity instanceof Interactable
                        && ((Interactable) nearestEntity).canDoInteraction()) {
                    return (Interactable) nearestEntity;
                }
            }
            return null;
        }
    }

    public void completeLoginSequence() {
        PlayerLoginEvent ev;
        this.server.getEventManager().fire(ev = new PlayerLoginEvent(this, "Plugin reason"));
        if (ev.isCancelled()) {
            this.close(ev.kickMessage(), "login");
            return;
        }

        Vector3f pos = this.getPosition();

        StartGamePacket startGamePacket = new StartGamePacket();
        startGamePacket.setUniqueEntityId(this.getUniqueId());
        startGamePacket.setRuntimeEntityId(this.getRuntimeId());
        startGamePacket.setPlayerGameType(GameType.from(this.getGameMode().getVanillaId()));
        startGamePacket.setPlayerPosition(pos);
        startGamePacket.setRotation(Vector2f.from(this.getYaw(), this.getPitch()));
        startGamePacket.setSeed(-1L);
        startGamePacket.setDimensionId(this.getLevel().getDimension());
        startGamePacket.setTrustingPlayers(false);
        startGamePacket.setLevelGameType(GameType.from(this.getGameMode().getVanillaId()));
        startGamePacket.setDifficulty(this.server.getDifficulty().ordinal());
        startGamePacket.setDefaultSpawn(this.getSpawn().getPosition().toInt());
        startGamePacket.setAchievementsDisabled(true);
        startGamePacket.setDayCycleStopTime(this.getLevel().getTime());
        startGamePacket.setRainLevel(0);
        startGamePacket.setLightningLevel(0);
        startGamePacket.setCommandsEnabled(this.isEnableClientCommand());
        startGamePacket.setMultiplayerGame(true);
        startGamePacket.setBroadcastingToLan(true);
        NetworkUtils.gameRulesToNetwork(this.getLevel().getGameRules(), startGamePacket.getGamerules());
        startGamePacket.setLevelId(""); // This is irrelevant since we have multiple levels
        startGamePacket.setLevelName(this.getServer().getNetwork().getName()); // We might as well use the MOTD instead of the default level name
        startGamePacket.setGeneratorId(1); // 0 old, 1 infinite, 2 flat - Has no effect to my knowledge
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
        startGamePacket.getExperiments().add(new ExperimentData("data_driven_items", true));
        startGamePacket.getExperiments().add(new ExperimentData("upcoming_creator_features", true));
        startGamePacket.getExperiments().add(new ExperimentData("experimental_molang_features", true));
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
        //noinspection unchecked,rawtypes
        session.getPeer().getCodecHelper().setItemDefinitions((DefinitionRegistry) CloudItemRegistry.get());
        //noinspection unchecked,rawtypes
        session.getPeer().getCodecHelper().setBlockDefinitions((DefinitionRegistry) BlockPalette.INSTANCE);
        this.sendPacket(startGamePacket);

        ItemComponentPacket componentPacket = new ItemComponentPacket();
        componentPacket.getItems().addAll(CloudItemRegistry.get().getItemEntries());
        this.sendPacket(componentPacket);

        BiomeDefinitionListPacket biomeDefinitionListPacket = new BiomeDefinitionListPacket();
        biomeDefinitionListPacket.setBiomes(CloudBiome.BIOME_DEFINITIONS);
        this.sendPacket(biomeDefinitionListPacket);

        AvailableEntityIdentifiersPacket availableEntityIdentifiersPacket = new AvailableEntityIdentifiersPacket();
        availableEntityIdentifiersPacket.setIdentifiers(EntityRegistry.get().getEntityIdentifiersPalette());
        this.sendPacket(availableEntityIdentifiersPacket);

        this.loggedIn = true;

        this.getLevel().sendTime(this);

        this.setMovementSpeed(DEFAULT_SPEED);
        this.sendAttributes();
        this.setNameTagVisible(true);
        this.setNameTagAlwaysVisible(true);
        this.setCanClimb(true);

        log.info(this.getServer().getLanguage().translate("cloudburst.player.logIn",
                "§b" + this.username + "§r",
                "",
                this.getSocketAddress(),
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

    public void checkNetwork() {
        if (!this.isConnected()) {
            return;
        }

        try (Timing ignore = Timings.playerNetworkReceiveTimer.startTiming()) {
            BedrockPacket packet;
            while ((packet = this.inboundQueue.poll()) != null) {
                packetHandler.handlePacket(packet);
            }
        }

        if (!this.loggedIn) {
            return;
        }

        this.getChunkManager().sendQueued();

        if (this.getChunkManager().getChunksSent() >= this.spawnThreshold && !this.spawned) {
            this.doFirstSpawn();
        }
    }

    public void processLogin() {
        if (this.server.getOnlinePlayers().size() >= this.server.getMaxPlayers() && this.kick(PlayerKickEvent.Reason.SERVER_FULL, "disconnectionScreen.serverFull", false)) {
            return;
        } else if (!this.server.isWhitelisted(this)) {
            this.kick(PlayerKickEvent.Reason.NOT_WHITELISTED, "Server is white-listed");
            return;
        } else if (this.isBanned()) {
            this.kick(PlayerKickEvent.Reason.NAME_BANNED, "You are banned");
            return;
        } else if (this.server.isIPBanned(this)) {
            this.kick(PlayerKickEvent.Reason.IP_BANNED, "You are banned");
            return;
        }

        CloudPlayer oldPlayer = null;
        for (CloudPlayer p : new ArrayList<>(this.getServer().getOnlinePlayers().values())) {
            if (p != this && p.getName() != null && p.getName().equalsIgnoreCase(this.getName()) ||
                    this.getServerId().equals(p.getServerId())) {
                oldPlayer = p;
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
            File legacyDataFile = new File(server.getDataPath() + "players/" + this.username.toLowerCase() + ".dat");
            File dataFile = new File(server.getDataPath() + "players/" + this.identity.toString() + ".dat");
            if (legacyDataFile.exists() && !dataFile.exists()) {
                nbt = this.server.getOfflinePlayerData(this.username, false);

                if (!legacyDataFile.delete()) {
                    log.warn("Could not delete legacy player data for {}", this.username);
                }
            } else {
                nbt = this.server.getOfflinePlayerData(this.identity, true);
            }
        }

        if (nbt == null) {
            this.close(this.leaveMessage(), "Invalid data");
            return;
        }

        if (loginChainData.isXboxAuthed() && server.getConfig().isXboxAuth() || !server.getConfig().isXboxAuth()) {
            server.updateName(this.identity, this.username);
        }

        this.loadAdditionalData(nbt);

        if (this.server.getForceGamemode()) {
            this.setGamemode(this.server.getGameMode(), PlayerGameModeChangeEvent.Cause.DEFAULT_GAMEMODE);
        }

        this.abilities = buildAbilitiesForGameMode(this.getGameMode());

        CloudLevel level;
        if ((level = this.server.getLevelByName(this.playerData.getLevel())) == null || !isAlive()) {
            this.level = this.server.getDefaultLevel();
        } else {
            this.level = level;
        }

        if (this.server.getAutoSave()) {
            this.server.saveOfflinePlayerData(this.identity, nbt, true);
        }

        this.server.onPlayerLogin(this);

        super.init(this.getLocation());

        if (this.isSpectator()) this.keepMovement = true;

        this.forceMovement = this.teleportPosition = this.getPosition();
    }

    /**
     * Sends a chat message as this player. If the message begins with a / (forward-slash) it will be treated
     * as a command.
     *
     * @param message message to send
     * @return successful
     */
    public boolean chat(String message) {
        if (!this.spawned || !this.isAlive()) {
            return false;
        }

        if (this.removeFormat) {
            message = PlainTextComponentSerializer.plainText().serialize(
                    BedrockLegacyTextSerializer.getInstance().deserialize(message));
        }

        for (String msg : message.split("\n")) {
            if (!msg.trim().isEmpty() && msg.length() <= 255 && this.messageCounter-- > 0) {
                PlayerChatEvent chatEvent = new PlayerChatEvent(this, Component.text(msg),
                        ChatRenderer.defaultRenderer(),
                        new HashSet<>(this.getServer().getOnlinePlayers().values()));
                this.server.getEventManager().fire(chatEvent);
                if (!chatEvent.isCancelled()) {
                    for (Audience viewer : chatEvent.viewers()) {
                        Component rendered = chatEvent.renderer().render(this, this.displayName(), chatEvent.message(), viewer);
                        viewer.sendMessage(rendered);
                    }
                }
            }
        }

        return true;
    }

    public boolean kick() {
        return this.kick("");
    }

    public boolean kick(String reason, boolean isAdmin) {
        return this.kick(PlayerKickEvent.Reason.UNKNOWN, reason, isAdmin);
    }

    public boolean kick(String reason) {
        return kick(PlayerKickEvent.Reason.UNKNOWN, reason);
    }

    public boolean kick(PlayerKickEvent.Reason reason) {
        return this.kick(reason, true);
    }

    public boolean kick(PlayerKickEvent.Reason reason, String reasonString) {
        return this.kick(reason, reasonString, true);
    }

    public boolean kick(PlayerKickEvent.Reason reason, boolean isAdmin) {
        return this.kick(reason, reason.toString(), isAdmin);
    }

    public boolean kick(PlayerKickEvent.Reason reason, String reasonString, boolean isAdmin) {
        PlayerKickEvent ev;
        this.server.getEventManager().fire(ev = new PlayerKickEvent(this, reason, this.leaveMessage()));
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
        this.kick(reason, PlayerKickEvent.Reason.UNKNOWN);
    }

    @Override
    public void kick(Component reason, PlayerKickEvent.Reason cause) {
        String reasonString = PlainTextComponentSerializer.plainText().serialize(reason);
        this.kick(cause, reasonString, true);
    }

    public void handleDataPacket(BedrockPacket packet) {
        this.inboundQueue.offer(packet);
    }

    public int getChunkRadius() {
        return this.getChunkManager().getChunkRadius();
    }

    public void setChunkRadius(int chunkRadius) {
        this.getChunkManager().setChunkRadius(chunkRadius);
    }

    public String getXuid() {
        return this.getLoginChainData().isXboxAuthed() ? this.getLoginChainData().getXUID() : "";
    }

    @Override
    public void sendMessage(Component message) {
        TextPacket packet = new TextPacket();
        if (message instanceof TranslatableComponent) {
            packet.setType(TextPacket.Type.TRANSLATION);
            packet.setNeedsTranslation(true);
        } else {
            packet.setType(TextPacket.Type.SYSTEM);
            packet.setNeedsTranslation(false);
        }
        packet.setPlatformChatId("");
        packet.setSourceName("");
        packet.setXuid("");
        packet.setMessage(new BedrockComponent(message));
        this.sendPacket(packet);
    }

    public void sendChat(Component message) {
        this.sendChat("", message);
    }

    public void sendChat(String source, Component message) {
        TextPacket packet = new TextPacket();
        packet.setType(TextPacket.Type.CHAT);
        packet.setPlatformChatId("");
        packet.setSourceName(source);
        packet.setXuid("");
        packet.setMessage(new BedrockComponent(message));
        this.sendPacket(packet);
    }

    public void sendPopup(Component message) {
        TextPacket packet = new TextPacket();
        packet.setType(TextPacket.Type.POPUP);
        packet.setPlatformChatId("");
        packet.setSourceName("");
        packet.setXuid("");
        packet.setMessage(new BedrockComponent(message));
        this.sendPacket(packet);
    }

    public void sendTip(Component message) {
        TextPacket packet = new TextPacket();
        packet.setType(TextPacket.Type.TIP);
        packet.setPlatformChatId("");
        packet.setSourceName("");
        packet.setXuid("");
        packet.setMessage(new BedrockComponent(message));
        this.sendPacket(packet);
    }

    public void clearTitle() {
        SetTitlePacket packet = new SetTitlePacket();
        packet.setType(SetTitlePacket.Type.CLEAR);
        packet.setText("");
        this.sendPacket(packet);
    }

    /**
     * Resets both title animation times and subtitle for the next shown title
     */
    public void resetTitleSettings() {
        SetTitlePacket packet = new SetTitlePacket();
        packet.setType(SetTitlePacket.Type.RESET);
        packet.setText("");
        this.sendPacket(packet);
    }

    public void setSubtitle(Component subtitle) {
        SetTitlePacket packet = new SetTitlePacket();
        packet.setType(SetTitlePacket.Type.SUBTITLE);
        packet.setText(new BedrockComponent(subtitle));
        this.sendPacket(packet);
    }

    public void setTitleAnimationTimes(int fadein, int duration, int fadeout) {
        SetTitlePacket packet = new SetTitlePacket();
        packet.setType(SetTitlePacket.Type.TIMES);
        packet.setFadeInTime(fadein);
        packet.setStayTime(duration);
        packet.setFadeOutTime(fadeout);
        packet.setText("");
        this.sendPacket(packet);
    }

    private void setTitle(Component text) {
        SetTitlePacket packet = new SetTitlePacket();
        packet.setType(SetTitlePacket.Type.TITLE);
        packet.setText(new BedrockComponent(text));
        this.sendPacket(packet);
    }

    public void sendTitle(Component title) {
        this.sendTitle(title, null, 20, 20, 5);
    }

    public void sendTitle(Component title, Component subtitle) {
        this.sendTitle(title, subtitle, 20, 20, 5);
    }

    public void sendTitle(Component title, Component subtitle, int fadeIn, int stay, int fadeOut) {
        this.setTitleAnimationTimes(fadeIn, stay, fadeOut);
        if (subtitle != null) {
            this.setSubtitle(subtitle);
        }
        this.setTitle(title != null ? title : Component.text(" "));
    }

    public void sendActionBar(Component title) {
        this.sendActionBar(title, 1, 0, 1);
    }

    public void sendActionBar(Component title, int fadein, int duration, int fadeout) {
        SetTitlePacket packet = new SetTitlePacket();
        packet.setType(SetTitlePacket.Type.ACTIONBAR);
        packet.setText(new BedrockComponent(title));
        packet.setFadeInTime(fadein);
        packet.setStayTime(duration);
        packet.setFadeOutTime(fadeout);
        this.sendPacket(packet);
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

    public void save() {
        this.save(false);
    }

    public PlayerChunkManager getChunkManager() {
        return this.chunkManager;
    }

    public String getName() {
        return this.username;
    }

    @Override
    public Component name() {
        return Component.text(this.username);
    }

    public Vector3f getTeleportPosition() {
        return teleportPosition;
    }

    public void setTeleportPosition(Vector3f teleportPosition) {
        this.teleportPosition = teleportPosition;
    }

    public long getClientTick() {
        return clientTick;
    }

    public void setClientTick(long tick) {
        this.clientTick = tick;
    }

    public Vector3f getForceMovement() {
        return forceMovement;
    }

    public void setForceMovement(Vector3f forceMovement) {
        this.forceMovement = forceMovement;
    }

    public Vector3f getNewPosition() {
        return newPosition;
    }

    public void setNewPosition(Vector3f newPosition) {
        this.newPosition = newPosition;
    }

    public void close(@Nullable Component message, String reason, boolean notify) {
        if (this.connected && !this.closed) {
            if (notify && reason.length() > 0) {
                DisconnectPacket packet = new DisconnectPacket();
                packet.setKickMessage(reason);
                this.sendPacketImmediately(packet);
            }

            this.connected = false;
            PlayerQuitEvent ev = null;
            if (this.getName() != null && this.getName().length() > 0) {
                this.server.getEventManager().fire(ev = new PlayerQuitEvent(this, message, true, reason));
                if (this.loggedIn && ev.getAutoSave()) {
                    this.save();
                }
                if (this.fishing != null) {
                    this.stopFishing(false);
                }
            }

            for (CloudPlayer player : new ArrayList<>(this.server.getOnlinePlayers().values())) {
                if (!player.canSee(this)) {
                    player.showPlayer(this);
                }
            }

            this.hiddenPlayers.clear();

            this.closeInventory(InventoryCloseEvent.Reason.DISCONNECT);

            this.getChunkManager().getViewChunks().forEach((LongConsumer) chunkKey -> {
                int chunkX = CloudChunk.fromKeyX(chunkKey);
                int chunkZ = CloudChunk.fromKeyZ(chunkKey);

                for (Entity entity : this.getLevel().getLoadedChunkEntities(chunkX, chunkZ)) {
                    if (entity != this) {
                        entity.getViewers().remove(this);
                    }
                }
            });

            super.close();

            if (this.session.getPeer().isConnected()) {
                this.session.disconnect(notify ? reason : "");
            }

            if (this.loggedIn) {
                this.server.removeOnlinePlayer(this);
            }

            this.loggedIn = false;

            if (ev != null && !Objects.equals(this.username, "") && this.spawned && ev.getQuitMessage() != null) {
                this.server.broadcastMessage(ev.getQuitMessage());
            }

            this.spawned = false;
            log.info(this.getServer().getLanguage().translate("cloudburst.player.logOut",
                    "§b" + (this.getName() == null ? "" : this.getName()) + "§r",
                    this.getSocketAddress(),
                    "",
                    this.getServer().getLanguage().translate(reason)));
            this.hasSpawned.clear();
            this.spawnLocation = null;
            this.respawnConfig = null;

            if (!passengers.isEmpty()) {
                passengers.forEach(entity -> entity.dismount(this));
            }
            if (this.vehicle != null) {
                this.dismount(vehicle);
            }
        }

        if (this.perm != null) {
            this.perm.invalidate();
            this.perm = null;
        }

        this.getChunkManager().clear();

        this.chunk = null;

        this.server.removePlayer(this);
    }

    @Override
    public void setHealth(float health) {
        if (health < 1) {
            health = 0;
        }

        super.setHealth(health);
        //TODO: Remove it in future! This a hack to solve the client-side absorption bug! WFT Mojang (Half a yellow heart cannot be shown, we can test it in local gaming)
        Attribute attr = Attribute.getAttribute(Attribute.MAX_HEALTH).setMaxValue(this.getAbsorption() % 2 != 0 ? this.getMaxHealth() + 1 : this.getMaxHealth()).setValue(health > 0 ? (health < getMaxHealth() ? health : getMaxHealth()) : 0);
        if (this.spawned) {
            UpdateAttributesPacket packet = new UpdateAttributesPacket();
            packet.getAttributes().add(NetworkUtils.attributeToNetwork(attr));
            packet.setRuntimeEntityId(this.getRuntimeId());
            this.sendPacket(packet);
        }
    }

    @Override
    public void setMaxHealth(int maxHealth) {
        super.setMaxHealth(maxHealth);

        Attribute attr = Attribute.getAttribute(Attribute.MAX_HEALTH).setMaxValue(this.getAbsorption() % 2 != 0 ? this.getMaxHealth() + 1 : this.getMaxHealth()).setValue(health > 0 ? (health < getMaxHealth() ? health : getMaxHealth()) : 0);
        if (this.spawned) {
            UpdateAttributesPacket packet = new UpdateAttributesPacket();
            packet.getAttributes().add(NetworkUtils.attributeToNetwork(attr));
            packet.setRuntimeEntityId(this.getRuntimeId());
            this.sendPacket(packet);
        }
    }

    public int getExperience() {
        return this.exp;
    }

    public void setExperience(int exp) {
        setExperience(exp, this.getExperienceLevel());
    }

    public int getExperienceLevel() {
        return this.expLevel;
    }

    // TODO: something on performance, lots of exp orbs then lots of packets, could crash client

    public void addExperience(int add) {
        if (add == 0) return;
        int now = this.getExperience();
        int added = now + add;
        int level = this.getExperienceLevel();
        int most = calculateRequireExperience(level);
        while (added >= most) {
            added = added - most;
            level++;
            most = calculateRequireExperience(level);
        }
        this.setExperience(added, level);
    }

    public void setExperience(int exp, int level) {
        this.exp = exp;
        this.expLevel = level;

        this.sendExperienceLevel(level);
        this.sendExperience(exp);
    }

    public void sendExperience() {
        sendExperience(this.getExperience());
    }

    public void sendExperience(int exp) {
        if (this.spawned) {
            float percent = ((float) exp) / calculateRequireExperience(this.getExperienceLevel());
            percent = Math.max(0f, Math.min(1f, percent));
            this.setAttribute(Attribute.getAttribute(Attribute.EXPERIENCE).setValue(percent));
        }
    }

    public void sendExperienceLevel() {
        sendExperienceLevel(this.getExperienceLevel());
    }

    public void sendExperienceLevel(int level) {
        if (this.spawned) {
            this.setAttribute(Attribute.getAttribute(Attribute.EXPERIENCE_LEVEL).setValue(level));
        }
    }

    public void setAttribute(Attribute attr) {
        UpdateAttributesPacket packet = new UpdateAttributesPacket();
        packet.getAttributes().add(NetworkUtils.attributeToNetwork(attr));
        packet.setRuntimeEntityId(this.getRuntimeId());
        this.sendPacket(packet);
    }

    @Override
    public void setMovementSpeed(float speed) {
        setMovementSpeed(speed, true);
    }

    public void setMovementSpeed(float speed, boolean send) {
        super.setMovementSpeed(speed);
        if (this.spawned && send) {
            Attribute attribute = Attribute.getAttribute(Attribute.MOVEMENT_SPEED).setValue(speed);
            this.setAttribute(attribute);
            this.abilities.setWalkSpeed(speed);
            this.abilities.update();
        }
    }

    public Entity getKiller() {
        return killer;
    }

    private CloudPlayerAbilities buildAbilitiesForGameMode(GameMode mode) {
        CloudPlayerAbilities abilities = new CloudPlayerAbilities(this);
        abilities.setAll(mode.getDefaultAbilities());
        abilities.set(Ability.OPERATOR_COMMANDS, this.isOp());
        abilities.set(Ability.TELEPORT, this.isOp());
        return abilities;
    }

    @Override
    public boolean attack(EntityDamageEvent source) {
        if (!this.isAlive()) {
            return false;
        }

        if (this.isSpectator() || (this.isCreative() && source.getCause() != EntityDamageEvent.DamageCause.SUICIDE)) {
            //source.setCancelled();
            return false;
        } else if (this.abilities.get(Ability.MAY_FLY) && source.getCause() == EntityDamageEvent.DamageCause.FALL) {
            //source.setCancelled();
            return false;
        } else if (source.getCause() == EntityDamageEvent.DamageCause.FALL) {
        }
        if (this.getLevel().getBlockState(this.getPosition().add(0, -1, 0).toInt()).getType() == BlockTypes.SLIME) {
            if (!this.isSneaking()) {
                //source.setCancelled();
                this.resetFallDistance();
                return false;
            }
        }

        if (super.attack(source)) { //!source.isCancelled()
            if (this.getLastDamageCause() == source && this.spawned) {
                if (source instanceof EntityDamageByEntityEvent) {
                    Entity damager = ((EntityDamageByEntityEvent) source).getDamager();
                    if (damager instanceof CloudPlayer) {
                        ((CloudPlayer) damager).getFoodData().updateFoodExpLevel(0.3);
                    }
                }
                EntityEventPacket packet = new EntityEventPacket();
                packet.setRuntimeEntityId(this.getRuntimeId());
                packet.setType(EntityEventType.HURT);
                this.sendPacket(packet);
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Drops an item on the ground in front of the player. Returns if the item drop was successful.
     *
     * @param item to drop
     * @return bool if the item was dropped or if the item was null
     */
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

    public void sendPosition(Vector3f pos) {
        this.sendPosition(pos, this.getYaw());
    }

    public void sendPosition(Vector3f pos, double yaw) {
        this.sendPosition(pos, yaw, this.getPitch());
    }

    public void sendPosition(Vector3f pos, double yaw, double pitch) {
        this.sendPosition(pos, yaw, pitch, MovePlayerPacket.Mode.NORMAL);
    }

    public void sendPosition(Vector3f pos, double yaw, double pitch, MovePlayerPacket.Mode mode) {
        this.sendPosition(pos, yaw, pitch, mode, null);
    }

    public void sendPosition(Vector3f pos, double yaw, double pitch, MovePlayerPacket.Mode mode, Set<CloudPlayer> targets) {
        MovePlayerPacket packet = new MovePlayerPacket();
        packet.setRuntimeEntityId(this.getRuntimeId());
        packet.setPosition(pos.add(0, getEyeHeight(), 0));
        packet.setRotation(Vector3f.from(pitch, yaw, yaw));
        packet.setMode(mode);
        packet.setOnGround(this.isOnGround());
        if (mode == MovePlayerPacket.Mode.TELEPORT) {
            packet.setTeleportationCause(MovePlayerPacket.TeleportationCause.BEHAVIOR);
        }

        if (targets != null) {
            CloudServer.broadcastPacket(targets, packet);
        } else {
            this.sendPacket(packet);
        }
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        this.playerData.loadData(tag);

        String level = this.playerData.getLevel();
        this.level = this.server.getLevel(level);
        if (this.level == null) {
            this.level = this.server.getDefaultLevel();
        }

        super.loadAdditionalData(tag);

        tag.listenForInt("GameType", (id) -> this.playerData.setGamemode(GameMode.from(id)));

        int exp = tag.getInt("EXP");
        int expLevel = tag.getInt("expLevel");
        this.setExperience(exp, expLevel);

        tag.listenForInt("foodLevel", this.foodData::setLevel);
        tag.listenForFloat("FoodSaturationLevel", this.foodData::setFoodSaturationLevel);
        tag.listenForList("EnderChestInventory", NbtType.COMPOUND, items -> {
            for (NbtMap itemTag : items) {
                this.getEnderChestContainer().setItem(itemTag.getByte("Slot"), ItemUtils.deserializeItem(itemTag));
            }
        });

        String spawnLevelId = this.playerData.getSpawnLevel();
        Vector3i spawnPos = this.playerData.getSpawnLocation();
        if (spawnLevelId != null && spawnPos != null) {
            CloudLevel spawnLevel = this.server.getLevel(spawnLevelId);
            if (spawnLevel != null) {
                float spawnYaw = this.playerData.getSpawnYaw();
                this.respawnConfig = new RespawnConfig(
                        spawnLevel,
                        spawnPos,
                        spawnYaw,
                        false,
                        RespawnConfig.SpawnType.BED
                );
                this.spawnLocation = Location.from(spawnPos.toFloat(), spawnYaw, 0f, spawnLevel);
            }
        }
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);

        this.playerData.setLevel(this.level.getId());
        if (this.spawnLocation != null && this.spawnLocation.getLevel() != null) {
            this.playerData.setSpawnLevel(this.spawnLocation.getLevel().getId());
            this.playerData.setSpawnLocation(this.spawnLocation.getPosition().toInt());
            this.playerData.setSpawnYaw(this.spawnLocation.getYaw());
        }

        this.playerData.saveData(tag);

        tag.putInt("GameType", this.getGameMode().getVanillaId());

        tag.putInt("EXP", this.getExperience());
        tag.putInt("expLevel", this.getExperienceLevel());

        tag.putInt("foodLevel", this.getFoodData().getLevel());
        tag.putFloat("foodSaturationLevel", this.getFoodData().getFoodSaturationLevel());

//        this.invManager.getEnderChest().saveInventory(tag); TODO:

    }

    public void save(boolean async) {
        if (this.closed) {
            throw new IllegalStateException("Tried to save closed player");
        }

        if (!this.loggedIn || this.username.isEmpty()) {
            return; // No point in saving player data from here.
        }

        NbtMapBuilder tag = NbtMap.builder();
        this.saveAdditionalData(tag);

        this.server.saveOfflinePlayerData(this.identity, tag.build(), async);
    }

    @Override
    public void kill() {
        if (!this.spawned) {
            return;
        }

        boolean showMessages = this.getLevel().getGameRules().get(GameRules.SHOW_DEATH_MESSAGES);
        String message = "death.attack.generic";

        List<Component> params = new ArrayList<>();
        params.add(this.displayName());
        if (showMessages) {

            EntityDamageEvent cause = this.getLastDamageCause();

            switch (cause == null ? EntityDamageEvent.DamageCause.CUSTOM : cause.getCause()) {
                case ENTITY_ATTACK:
                    if (cause instanceof EntityDamageByEntityEvent) {
                        Entity e = ((EntityDamageByEntityEvent) cause).getDamager();
                        killer = e;
                        if (e instanceof CloudPlayer) {
                            message = "death.attack.player";
                            params.add(((CloudPlayer) e).displayName());
                            break;
                        } else if (e instanceof EntityLiving) {
                            message = "death.attack.mob";
                            params.add(!Objects.equals(e.getNameTag(), "") ? Component.text(e.getNameTag()) : Component.text(e.getName()));
                            break;
                        } else {
                            params.add(Component.text("Unknown"));
                        }
                    }
                    break;
                case PROJECTILE:
                    if (cause instanceof EntityDamageByEntityEvent) {
                        Entity e = ((EntityDamageByEntityEvent) cause).getDamager();
                        killer = e;
                        if (e instanceof CloudPlayer) {
                            message = "death.attack.arrow";
                            params.add(((CloudPlayer) e).displayName());
                        } else if (e instanceof EntityLiving) {
                            message = "death.attack.arrow";
                            params.add(!Objects.equals(e.getNameTag(), "") ? Component.text(e.getNameTag()) : Component.text(e.getName()));
                            break;
                        } else {
                            params.add(Component.text("Unknown"));
                        }
                    }
                    break;
                case SUICIDE:
                    message = "death.attack.generic";
                    break;
                case VOID:
                    message = "death.attack.outOfWorld";
                    break;
                case FALL:
                    if (cause != null) {
                        if (cause.getFinalDamage() > 2) {
                            message = "death.fell.accident.generic";
                            break;
                        }
                    }
                    message = "death.attack.fall";
                    break;

                case SUFFOCATION:
                    message = "death.attack.inWall";
                    break;

                case LAVA:
                    BlockState state = this.getLevel().getBlockState(this.getPosition().add(0, -1, 0).toInt());
                    if (state.getType() == BlockTypes.MAGMA) {
                        message = "death.attack.magma";
                        break;
                    }
                    message = "death.attack.lava";
                    break;

                case FIRE:
                    message = "death.attack.onFire";
                    break;

                case FIRE_TICK:
                    message = "death.attack.inFire";
                    break;

                case DROWNING:
                    message = "death.attack.drown";
                    break;

                case CONTACT:
                    if (cause instanceof EntityDamageByBlockEvent) {
                        if (((EntityDamageByBlockEvent) cause).getDamager().getState().getType() == BlockTypes.CACTUS) {
                            message = "death.attack.cactus";
                        }
                    }
                    break;

                case BLOCK_EXPLOSION:
                case ENTITY_EXPLOSION:
                    if (cause instanceof EntityDamageByEntityEvent) {
                        Entity e = ((EntityDamageByEntityEvent) cause).getDamager();
                        killer = e;
                        if (e instanceof CloudPlayer) {
                            message = "death.attack.explosion.player";
                            params.add(((CloudPlayer) e).displayName());
                        } else if (e instanceof EntityLiving) {
                            message = "death.attack.explosion.player";
                            params.add(!Objects.equals(e.getNameTag(), "") ? Component.text(e.getNameTag()) : Component.text(e.getName()));
                            break;
                        }
                    } else {
                        message = "death.attack.explosion";
                    }
                    break;

                case MAGIC:
                    message = "death.attack.magic";
                    break;

                case HUNGER:
                    message = "death.attack.starve";
                    break;

                case CUSTOM:
                    break;

                default:
                    break;

            }
        } else {
            message = "";
            params.clear();
        }

        if (this.fishing != null) {
            this.stopFishing(false);
        }

        this.health = 0;
        this.scheduleUpdate();

        PlayerDeathEvent ev = new PlayerDeathEvent(this, this.getDrops(), message.isEmpty() ? null : Component.translatable(message, params), this.getExperienceLevel());
        ev.setKeepExperience(this.getLevel().getGameRules().get(GameRules.KEEP_INVENTORY));
        ev.setKeepInventory(ev.getKeepExperience());
        this.server.getEventManager().fire(ev);

        if (!ev.getKeepInventory() && this.getLevel().getGameRules().get(GameRules.DO_ENTITY_DROPS)) {
            for (ItemStack item : ev.getDrops()) {
                this.getLevel().dropItem(this.getPosition(), item, null, true, 40);
            }

            this.getContainer().clear();
        }

        if (!ev.getKeepExperience() && this.getLevel().getGameRules().get(GameRules.DO_ENTITY_DROPS)) {
            if (this.isSurvival() || this.isAdventure()) {
                int exp = ev.getExperience() * 7;
                if (exp > 100) exp = 100;
                this.getLevel().dropExpOrb(this.getPosition(), exp);
            }
            this.setExperience(0, 0);
        }

        if (showMessages && ev.getDeathMessage() != null) {
            this.server.broadcast(ev.getDeathMessage(), CloudServer.BROADCAST_CHANNEL_USERS);
        }

        boolean hadPersonalSpawn = this.respawnConfig != null;
        Location respawnLocation = this.findRespawnPosition();
        if (respawnLocation == null) {
            if (hadPersonalSpawn && this.respawnConfig != null) {
                this.respawnConfig.level().addLevelSoundEvent(this.respawnConfig.pos(), SoundEvent.RESPAWN_ANCHOR_AMBIENT);
            }
            respawnLocation = this.getServer().getDefaultLevel().getSafeSpawn();
        }

        RespawnPacket packet = new RespawnPacket();
        packet.setPosition(respawnLocation.getPosition());
        packet.setState(RespawnPacket.State.SERVER_SEARCHING);

        //this is a dirty hack to prevent dying in a different level than the respawn point from breaking everything
        if (this.getLevel() != respawnLocation.getLevel()) {
            this.teleport(respawnLocation, null);
        }

        this.extinguish();

        this.sendPacket(packet);
    }

    protected void sendPlayStatus(PlayStatusPacket.Status status) {
        sendPlayStatus(status, false);
    }

    protected void sendPlayStatus(PlayStatusPacket.Status status, boolean immediate) {
        PlayStatusPacket packet = new PlayStatusPacket();
        packet.setStatus(status);

        if (immediate) {
            this.sendPacketImmediately(packet);
        } else {
            this.sendPacket(packet);
        }
    }

    @Override
    protected void checkChunks() {
        Vector3f pos = this.getPosition();
        if (this.chunk == null || (this.chunk.getX() != pos.getFloorX() >> 4 || this.chunk.getZ() != pos.getFloorZ() >> 4)) {
            if (this.chunk != null) {
                this.chunk.removeEntity(this);
            }
            this.chunk = this.getLevel().getChunk(pos);

            if (!this.justCreated) {

                Set<CloudPlayer> loaders = this.getChunk().getPlayerLoaders();
                for (CloudPlayer player : this.hasSpawned) {
                    if (!loaders.contains(player)) {
                        this.despawnFrom(player);
                    } else {
                        loaders.remove(player);
                    }
                }

                for (CloudPlayer player : loaders) {
                    this.spawnTo(player);
                }
            }

            if (this.chunk == null) {
                return;
            }

            this.chunk.addEntity(this);
        }
    }

    public void teleportImmediate(Location location) {
        this.teleportImmediate(location, PlayerTeleportEvent.TeleportCause.PLUGIN);
    }

    public void teleportImmediate(Location location, PlayerTeleportEvent.TeleportCause cause) {
        Location from = this.getLocation();
        if (super.teleport(location, cause)) {

            this.closeInventory(InventoryCloseEvent.Reason.TELEPORT);

            if (from.getLevel() != location.getLevel()) { //Different level, update compass position
                SetSpawnPositionPacket packet = new SetSpawnPositionPacket();
                packet.setSpawnType(SetSpawnPositionPacket.Type.WORLD_SPAWN);
                Vector3f spawn = location.getLevel().getSpawnLocation();
                packet.setBlockPosition(spawn.toInt());
                sendPacket(packet);
            }

            this.forceMovement = this.getPosition();
            this.sendPosition(this.getPosition(), this.getY(), this.getPitch(), MovePlayerPacket.Mode.RESPAWN);

            this.resetFallDistance();
            this.newPosition = null;

            //Weather
            this.getLevel().sendWeather(this);
            //Update time
            this.getLevel().sendTime(this);
        }
    }

    /**
     * Shows a new FormWindow to the player
     * You can find out FormWindow result by listening to PlayerFormRespondedEvent
     *
     * @param window to show
     * @return form id
     */
    public int showFormWindow(Form<?> window) {
        return showFormWindow(window, this.formWindowCount.getAndIncrement());
    }

    /**
     * Shows a new FormWindow to the player
     * You can find out FormWindow result by listening to PlayerFormRespondedEvent
     *
     * @param window to show
     * @param id     form id
     * @return form id
     */
    public int showFormWindow(Form<?> window, int id) {
        ModalFormRequestPacket packet = new ModalFormRequestPacket();
        packet.setFormId(id);
        try {
            packet.setFormData(new JsonMapper().writeValueAsString(window));
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
        this.formWindows.put(id, window);

        this.sendPacket(packet);
        return id;
    }

    public Form<?> removeFormWindow(int id) {
        return this.formWindows.remove(id);
    }

    public CustomForm getServerSettings() {
        return serverSettings;
    }

    public int getServerSettingsId() {
        return serverSettingsId;
    }

    /**
     * Shows a new setting page in game settings
     * You can find out settings result by listening to PlayerFormRespondedEvent
     *
     * @param window to show on settings page
     * @return form id
     */
    public int setServerSettings(CustomForm window) {
        int id = this.formWindowCount.getAndIncrement();

        this.serverSettings = window;
        this.serverSettingsId = id;
        return id;
    }

    /**
     * Creates and sends a BossBar to the player
     *
     * @param text   The BossBar message
     * @param length The BossBar percentage
     * @return bossBarId  The BossBar ID, you should store it if you want to remove or update the BossBar later
     */
    @Deprecated
    public long createBossBar(String text, int length) {
        DummyBossBar bossBar = new DummyBossBar.Builder(this).text(text).length(length).build();
        return this.createBossBar(bossBar);
    }

    /**
     * Creates and sends a BossBar to the player
     *
     * @param dummyBossBar DummyBossBar Object (Instantiate it by the Class Builder)
     * @return bossBarId  The BossBar ID, you should store it if you want to remove or update the BossBar later
     * @see DummyBossBar.Builder
     */
    public long createBossBar(DummyBossBar dummyBossBar) {
        this.dummyBossBars.put(dummyBossBar.getBossBarId(), dummyBossBar);
        dummyBossBar.create();
        return dummyBossBar.getBossBarId();
    }

    /**
     * Get a DummyBossBar object
     *
     * @param bossBarId The BossBar ID
     * @return DummyBossBar object
     * @see DummyBossBar#setText(String) Set BossBar text
     * @see DummyBossBar#setLength(float) Set BossBar length
     * @see DummyBossBar#setColor(org.cloudburstmc.server.utils.DummyBossBar.BossBarColor) Set BossBar color
     */
    public DummyBossBar getDummyBossBar(long bossBarId) {
        return this.dummyBossBars.getOrDefault(bossBarId, null);
    }

    /**
     * Get all DummyBossBar objects
     *
     * @return DummyBossBars Map
     */
    public Map<Long, DummyBossBar> getDummyBossBars() {
        return dummyBossBars;
    }

    /**
     * Updates a BossBar
     *
     * @param text      The new BossBar message
     * @param length    The new BossBar length
     * @param bossBarId The BossBar ID
     */
    @Deprecated
    public void updateBossBar(String text, int length, long bossBarId) {
        if (this.dummyBossBars.containsKey(bossBarId)) {
            DummyBossBar bossBar = this.dummyBossBars.get(bossBarId);
            bossBar.setText(text);
            bossBar.setLength(length);
        }
    }

    /**
     * Removes a BossBar
     *
     * @param bossBarId The BossBar ID
     */
    public void removeBossBar(long bossBarId) {
        if (this.dummyBossBars.containsKey(bossBarId)) {
            this.dummyBossBars.get(bossBarId).destroy();
            this.dummyBossBars.remove(bossBarId);
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

    protected boolean checkTeleportPosition() {
        if (this.teleportPosition != null) {
            int chunkX = this.teleportPosition.getFloorX() >> 4;
            int chunkZ = this.teleportPosition.getFloorZ() >> 4;

            for (int X = -1; X <= 1; ++X) {
                for (int Z = -1; Z <= 1; ++Z) {
                    long index = CloudChunk.key(chunkX + X, chunkZ + Z);
                    if (!this.getChunkManager().isChunkShellSent(index)) {
                        return false;
                    }
                }
            }

            this.spawnToAll();
            this.forceMovement = this.teleportPosition;
            this.teleportPosition = null;
            return true;
        }

        return false;
    }

    @Override
    public boolean teleport(Location location, PlayerTeleportEvent.TeleportCause cause) {
        if (!this.isOnline()) {
            return false;
        }

        Location from = this.getLocation();
        Location to = location;

        if (cause != null) {
            PlayerTeleportEvent event = new PlayerTeleportEvent(this, from, to, cause);
            this.server.getEventManager().fire(event);
            if (event.isCancelled()) return false;
            to = event.getTo();
        }

        // Suppress the EntityTeleportEvent here since PlayerTeleportEvent was already fired above.
        if (super.teleport(to, null)) {
            this.closeInventory(InventoryCloseEvent.Reason.TELEPORT);

            this.teleportPosition = this.getPosition();
            this.getChunkManager().queueNewChunks(this.teleportPosition);
            this.forceMovement = this.teleportPosition;
            this.sendPosition(this.getPosition(), this.getYaw(), this.getPitch(), MovePlayerPacket.Mode.TELEPORT);

            this.checkTeleportPosition();

            this.resetFallDistance();
            this.newPosition = null;

            //DummyBossBar
            this.getDummyBossBars().values().forEach(DummyBossBar::reshow);
            //Weather
            this.getLevel().sendWeather(this);
            //Update time
            this.getLevel().sendTime(this);
            return true;
        }

        return false;
    }

    public boolean isChunkInView(int x, int z) {
        return this.getChunkManager().isChunkInView(x, z);
    }

    public boolean isChunkSent(int x, int z) {
        return this.getChunkManager().isChunkSent(x, z);
    }

    @Override
    public void onChunkChanged(Chunk chunk) {
        this.getChunkManager().resendChunk(chunk.getX(), chunk.getZ());
    }

    @Override
    public int getLoaderId() {
        return this.loaderId;
    }

    @Override
    public boolean isLoaderActive() {
        return this.isConnected();
    }

    public boolean isFoodEnabled() {
        return !(this.isCreative() || this.isSpectator()) && this.foodEnabled;
    }

    public void setFoodEnabled(boolean foodEnabled) {
        this.foodEnabled = foodEnabled;
    }

    public PlayerFood getFoodData() {
        return this.foodData;
    }

    //todo a lot on dimension

    private void setDimension(int dimension) {
        ChangeDimensionPacket packet = new ChangeDimensionPacket();
        packet.setDimension(dimension);
        packet.setPosition(this.getPosition());
        this.sendPacketImmediately(packet);
    }

    public synchronized Locale getLocale() {
        return this.locale.get();
    }

    public synchronized void setLocale(Locale locale) {
        this.locale.set(locale);
    }

    @Override
    public void setSprinting(boolean value) {
        if (isSprinting() != value) {
            super.setSprinting(value);
            this.setMovementSpeed(value ? getMovementSpeed() * 1.3f : getMovementSpeed() / 1.3f);
        }
    }

    public void transfer(InetSocketAddress address) {
        String hostName = address.getAddress().getHostAddress();
        int port = address.getPort();
        TransferPacket pk = new TransferPacket();
        pk.setAddress(hostName);
        pk.setPort(port);
        this.sendPacket(pk);
    }

    public LoginChainData getLoginChainData() {
        return this.loginChainData;
    }

    public void setLoginChainData(LoginChainData loginChainData) {
        this.loginChainData = loginChainData;
    }

    @Override
    public void onChunkUnloaded(Chunk chunk) {
        //this.sentChunks.remove(Chunk.key(chunk.getX(), chunk.getZ()));
    }

    @Override
    public void onChunkLoaded(Chunk chunk) {

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

    public boolean isBreakingBlock() {
        return this.breakingBlock != null;
    }

    /**
     * Show a window of a XBOX account's profile
     *
     * @param xuid XUID
     */
    public void showXboxProfile(String xuid) {
        ShowProfilePacket packet = new ShowProfilePacket();
        packet.setXuid(xuid);
        this.sendPacket(packet);
    }

    /**
     * Sends a sign change. Sends "fake" sign contents
     * by sending a sign change packet. This does not actually
     * modify the sign lines and is only temporary.
     *
     * @param position the position of the sign
     * @param lines    the lines to send
     */
    public void sendSignChange(Vector3i position, String[] lines) {
        BlockEntity blockEntity = level.getBlockEntity(position);
        if (!(blockEntity instanceof Sign)) {
            return;
        }
        NbtMap tag = ((SignBlockEntity) blockEntity).getChunkTag().toBuilder().putString("Text", String.join("\n", lines)).build();
        BlockEntityDataPacket blockEntityDataPacket = new BlockEntityDataPacket();
        blockEntityDataPacket.setBlockPosition(position);
        blockEntityDataPacket.setData(tag);
        this.sendPacket(blockEntityDataPacket);
    }

    /**
     * Start fishing
     *
     * @param fishingRod fishing rod item
     */
    public void startFishing(ItemStack fishingRod) {
        Location location = Location.from(this.getPosition().add(0, this.getEyeHeight(), 0), this.getYaw(),
                this.getPitch(), this.getLevel());
        double f = 1.1;
        FishingHook fishingHook = EntityRegistry.get().newEntity(EntityTypes.FISHING_HOOK, location);
        fishingHook.setPosition(location.getPosition());
        fishingHook.setOwner(this);
        fishingHook.setMotion(Vector3f.from(-Math.sin(Math.toRadians(this.getYaw())) * Math.cos(Math.toRadians(this.getPitch())) * f * f,
                -Math.sin(Math.toRadians(this.getPitch())) * f * f, Math.cos(Math.toRadians(this.getYaw())) * Math.cos(Math.toRadians(this.getPitch())) * f * f));
        ProjectileLaunchEvent ev = new ProjectileLaunchEvent(fishingHook);
        this.getServer().getEventManager().fire(ev);
        if (ev.isCancelled()) {
            fishingHook.close();
        } else {
            fishingHook.spawnToAll();
            this.fishing = fishingHook;
            fishingHook.setRod(fishingRod);
        }
    }

    /**
     * Stop fishing
     *
     * @param click clicked or forced
     */
    public void stopFishing(boolean click) {
        if (this.fishing != null && click) {
            fishing.reelLine();
        } else if (this.fishing != null) {
            this.fishing.close();
        }

        this.fishing = null;
    }

    @Override
    public boolean switchLevel(CloudLevel level) {
        CloudLevel oldLevel = this.getLevel();
        int newDimension = level.getDimension();
        boolean dimensionChanged = newDimension != oldLevel.getDimension();

        if (dimensionChanged) {
            ChangeDimensionPacket changeDim = new ChangeDimensionPacket();
            changeDim.setDimension(newDimension);
            changeDim.setPosition(this.getPosition().add(0, this.getEyeHeight(), 0));
            changeDim.setRespawn(false);
            this.sendPacketImmediately(changeDim);
        }

        if (super.switchLevel(level)) {
            SetSpawnPositionPacket spawnPosition = new SetSpawnPositionPacket();
            spawnPosition.setSpawnType(SetSpawnPositionPacket.Type.WORLD_SPAWN);
            Vector3f spawn = level.getSpawnLocation();
            spawnPosition.setBlockPosition(spawn.toInt());
            this.sendPacket(spawnPosition);

            this.getChunkManager().prepareRegion(this.getPosition());

            SetTimePacket setTime = new SetTimePacket();
            setTime.setTime(level.getTime());
            this.sendPacket(setTime);

            GameRulesChangedPacket gameRulesChanged = new GameRulesChangedPacket();
            NetworkUtils.gameRulesToNetwork(level.getGameRules(), gameRulesChanged.getGameRules());
            this.sendPacket(gameRulesChanged);

            if (dimensionChanged) {
                PlayerActionPacket ack = new PlayerActionPacket();
                ack.setAction(PlayerActionType.DIMENSION_CHANGE_SUCCESS);
                ack.setRuntimeEntityId(this.getRuntimeId());
                ack.setBlockPosition(org.cloudburstmc.math.vector.Vector3i.ZERO);
                ack.setResultPosition(org.cloudburstmc.math.vector.Vector3i.ZERO);
                ack.setFace(0);
                this.sendPacket(ack);
            }

            return true;
        }

        return false;
    }

    public boolean pickupEntity(Entity entity, boolean near) {
        if (!this.spawned || !this.isAlive() || !this.isOnline() || this.getGameMode() == GameMode.SPECTATOR || entity.isClosed()) {
            return false;
        }

        if (near) {
            if (entity instanceof Arrow && entity.getMotion().lengthSquared() == 0) {
                ItemStack item = ItemStack.builder().itemType(ItemTypes.ARROW).build();
                if (this.isSurvival() && !this.getContainer().canAddItem(item)) {
                    return false;
                }

                InventoryPickupArrowEvent ev = new InventoryPickupArrowEvent(this.getInventory(), (EntityArrow) entity);

                int pickupMode = ((EntityArrow) entity).getPickupMode();
                if (pickupMode == EntityArrow.PICKUP_NONE || pickupMode == EntityArrow.PICKUP_CREATIVE && !this.isCreative()) {
                    ev.setCancelled();
                }

                this.server.getEventManager().fire(ev);
                if (ev.isCancelled()) {
                    return false;
                }

                TakeItemEntityPacket packet = new TakeItemEntityPacket();
                packet.setRuntimeEntityId(this.getRuntimeId());
                packet.setItemRuntimeEntityId(entity.getRuntimeId());
                CloudServer.broadcastPacket(((CloudEntity) entity).getViewers(), packet);
                this.sendPacket(packet);

                if (!this.isCreative()) {
                    this.getContainer().addItem(item);
                }
                entity.close();
                return true;
            } else if (entity instanceof ThrownTrident && entity.getMotion().lengthSquared() == 0) {
                ItemStack item = ((ThrownTrident) entity).getTrident();
                if (this.isSurvival() && !this.getContainer().canAddItem(item)) {
                    return false;
                }

                TakeItemEntityPacket packet = new TakeItemEntityPacket();
                packet.setRuntimeEntityId(this.getRuntimeId());
                packet.setItemRuntimeEntityId(entity.getRuntimeId());
                CloudServer.broadcastPacket(((CloudEntity) entity).getViewers(), packet);
                this.sendPacket(packet);

                if (!this.isCreative()) {
                    this.getContainer().addItem(item);
                }
                entity.close();
                return true;
            } else if (entity instanceof DroppedItem) {
                if (((DroppedItem) entity).getPickupDelay() <= 0) {
                    ItemStack item = ((DroppedItem) entity).getItem();

                    if (item != null) {
                        if (this.isSurvival() && !this.getContainer().canAddItem(item)) {
                            return false;
                        }

                        InventoryPickupItemEvent ev;
                        this.server.getEventManager().fire(ev = new InventoryPickupItemEvent(this.getInventory(), (DroppedItem) entity));
                        if (ev.isCancelled()) {
                            return false;
                        }

                        if (((Object) item.getType()) instanceof BlockType blockType && blockType.hasTag(BlockTags.LOG)) {
                            this.awardAchievement("mineWood");
                        } else if (item.getType() == ItemTypes.DIAMOND) {
                            this.awardAchievement("diamond");
                        }

                        TakeItemEntityPacket packet = new TakeItemEntityPacket();
                        packet.setRuntimeEntityId(this.getRuntimeId());
                        packet.setItemRuntimeEntityId(entity.getRuntimeId());
                        CloudServer.broadcastPacket(((CloudEntity) entity).getViewers(), packet);
                        this.sendPacket(packet);

                        entity.close();
                        this.getContainer().addItem(item);
                        return true;
                    }
                }
            }
        }

        int tick = this.getServer().getTick();
        if (pickedXPOrb < tick && entity instanceof ExperienceOrb experienceOrb && this.boundingBox.isVectorInside(entity.getPosition())) {
            if (experienceOrb.getPickupDelay() <= 0) {
                int exp = experienceOrb.getExperience();
                entity.kill();
                this.getLevel().addSound(this.getPosition(), Sound.RANDOM_ORB);
                pickedXPOrb = tick;

//                TODO Enchantments implementation
//                //Mending
//                ArrayList<Integer> itemsWithMending = new ArrayList<>();
//                for (int i = 0; i < 4; i++) {
//                    if (getInventory().getArmorItem(i).getEnchantment(EnchantmentTypes.MENDING) != null) {
//                        itemsWithMending.add(getInventory().getSize() + i);
//                    }
//                }
//                if (getInventory().getSelectedItem().getEnchantment(EnchantmentTypes.MENDING) != null) {
//                    itemsWithMending.add(getInventory().getHeldItemIndex());
//                }
//                if (itemsWithMending.size() > 0) {
//                    Random rand = new Random();
//                    Integer itemToRepair = itemsWithMending.get(rand.nextInt(itemsWithMending.size()));
//                    ItemStack toRepair = getInventory().getItem(itemToRepair);
//                    var behavior = toRepair.getBehavior();
//                    if (behavior.isTool(toRepair) || behavior.isArmor()) {
//                        var damage = toRepair.getMetadata(Damageable.class);
//
//                        if (damage.getDurability() > 0) {
//                            getInventory().setItem(itemToRepair, toRepair.withData(damage.repair(2)));
//                            return true;
//                        }
//                    }
//                }

                this.addExperience(exp);
                return true;
            }
        }

        return false;
    }

    @Override
    public void onInventoryRemoved(Container inventory) {
    }

    @Override
    public void onInventoryAdded(Container inventory) {
    }

    @Override
    public void onInventorySlotChange(Container inventory, int slot) {
        if (!this.spawned) {
            return;
        }

        int containerId = getContainerId(inventory);
        if (containerId == ContainerId.NONE) {
            return;
        }

        ItemStack itemStack = inventory.getItem(slot);
        InventorySlotPacket packet = new InventorySlotPacket();
        packet.setSlot(slot);
        packet.setItem(ItemUtils.toNetworkNetId(itemStack));
        packet.setContainerId(containerId);
        this.sendPacket(packet);
    }

    @Override
    public void onInventoryContentsChange(Container inventory) {
        if (!this.spawned) {
            return;
        }

        int containerId = getContainerId(inventory);
        if (containerId == ContainerId.NONE) {
            return;
        }

        List<ItemData> contents = new ArrayList<>();
        for (ItemStack item : inventory.getContents()) {
            contents.add(ItemUtils.toNetworkNetId(item));
        }

        InventoryContentPacket packet = new InventoryContentPacket();
        packet.setContents(contents);
        packet.setContainerId(containerId);
        this.sendPacket(packet);
    }

    private int getContainerId(Container inventory) {
        if (inventory == this.container) {
            return ContainerId.INVENTORY;
        } else if (inventory == this.armor.getContainer()) {
            return ContainerId.ARMOR;
        } else if (inventory == this.offhand.getContainer()) {
            return ContainerId.OFFHAND;
        }
        Byte dynamic = this.containerToWindowId.get(inventory);
        return dynamic != null ? dynamic : ContainerId.NONE;
    }

    public byte assignContainerId(Container inventory) {
        byte id = this.containerIdCounter;
        this.containerToWindowId.put(inventory, id);
        this.windowIdToContainer.put(id, inventory);
        this.containerIdCounter = (byte) (this.containerIdCounter >= 99 ? 1 : this.containerIdCounter + 1);
        return id;
    }

    private void unregisterContainerId(Container inventory) {
        Byte id = this.containerToWindowId.remove(inventory);
        if (id != null) {
            this.windowIdToContainer.remove(id);
        }
    }

    void unregisterContainerById(byte windowId) {
        Container c = this.windowIdToContainer.remove(windowId);
        if (c != null) {
            this.containerToWindowId.remove(c);
        }
    }

    public void sendHeldItemSlot() {
        this.onInventorySlotChange(this.container, this.selectedHotbarSlot);
    }

    public void setSelectedHotbarSlot(int slot) {
        this.selectedHotbarSlot = slot;

        PlayerHotbarPacket packet = new PlayerHotbarPacket();
        packet.setSelectedHotbarSlot(slot);
        packet.setContainerId(ContainerId.INVENTORY);
        packet.setSelectHotbarSlot(true);
        this.sendPacket(packet);
    }

    public void sendInventoryContents() {
        this.onInventoryContentsChange(this.container);
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

    @Override
    public boolean canTriggerPressurePlate() {
        return !this.isSpectator();
    }

    @Override
    public String toString() {
        return "Player(name=" + getName() + ")";
    }

    public enum CraftingType {
        SMALL,
        BIG,
        ANVIL,
        ENCHANT,
        BEACON
    }

    private class Handler implements BedrockPacketHandler {

        @Override
        public void onDisconnect(CharSequence reason) {
            CloudPlayer.this.close("", reason.toString());
        }

        @Override
        public PacketSignal handlePacket(BedrockPacket packet) {
            CloudPlayer.this.handleDataPacket(packet);
            return PacketSignal.HANDLED;
        }
    }
}