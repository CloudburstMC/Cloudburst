package org.cloudburstmc.server.entity;

import com.google.gson.JsonParseException;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockComponents;
import org.cloudburstmc.api.block.LiquidState;
import org.cloudburstmc.api.block.LiquidTypes;
import org.cloudburstmc.api.entity.*;
import org.cloudburstmc.api.entity.component.Buoyancy;
import org.cloudburstmc.api.entity.component.InteractEntityHandler;
import org.cloudburstmc.api.entity.component.PickItemEntityHandler;
import org.cloudburstmc.api.entity.component.TickEntityHandler;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.damage.DamageTypeTags;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.entity.misc.DroppedItem;
import org.cloudburstmc.api.entity.misc.LightningBolt;
import org.cloudburstmc.api.entity.projectile.Projectile;
import org.cloudburstmc.api.entity.vehicle.Vehicle;
import org.cloudburstmc.api.event.entity.*;
import org.cloudburstmc.api.event.vehicle.VehicleEnterEvent;
import org.cloudburstmc.api.event.vehicle.VehicleExitEvent;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.api.level.sound.SoundTypes;
import org.cloudburstmc.api.persistence.PersistentDataContainer;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.potion.EffectType;
import org.cloudburstmc.api.potion.EffectTypes;
import org.cloudburstmc.api.potion.PotionEffect;
import org.cloudburstmc.api.util.BoundingBox;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.MovementType;
import org.cloudburstmc.api.util.data.CardinalDirection;
import org.cloudburstmc.math.GenericMath;
import org.cloudburstmc.math.vector.Vector2f;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.protocol.adventure.BedrockLegacyTextSerializer;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataMap;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityLinkData;
import org.cloudburstmc.protocol.bedrock.packet.*;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.entity.data.CloudEntityData;
import org.cloudburstmc.server.entity.metadata.BuoyancyMetadata;
import org.cloudburstmc.server.entity.metadata.SyncedEntityData;
import org.cloudburstmc.server.event.entity.CloudEntityDamageEvent;
import org.cloudburstmc.server.level.*;
import org.cloudburstmc.server.level.biome.CloudBiome;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.level.collision.CloudVoxelShapes;
import org.cloudburstmc.server.math.MathHelper;
import org.cloudburstmc.server.network.NetworkUtils;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.potion.ActivePotionEffect;
import org.cloudburstmc.server.registry.CloudBiomeRegistry;
import org.cloudburstmc.server.registry.CloudEntityRegistry;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BooleanSupplier;

import static com.google.common.base.Preconditions.*;
import static org.cloudburstmc.api.block.BlockTypes.FIRE;
import static org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes.*;
import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.*;

@Log4j2
public abstract class CloudEntity implements Entity {

    protected static final int PORTAL_TRANSFER_TICKS = 80;
    protected static final int PORTAL_COOLDOWN_TICKS = 300;
    private static final int DEFAULT_MAX_FREEZE_TICKS = 140;
    private static final int MAX_SCOREBOARD_TAGS = 1024;
    private static final int MAX_MOVEMENT_SEGMENTS = 100;
    private static final float MIN_SCALE = 0.0625f;
    private static final float MAX_SCALE = 16f;

    private static final Direction[] COLLISION_ESCAPE_DIRECTIONS = {
            Direction.NORTH,
            Direction.SOUTH,
            Direction.WEST,
            Direction.EAST,
            Direction.UP
    };

    @Getter
    private final long runtimeId = CloudEntityRegistry.get().newEntityId();
    @Getter
    private final EntityType<?> type;
    private final EntityPersistence persistence;
    private @Nullable Entity owner;
    private @Nullable Component customName;

    // Lifecycle and level membership
    protected CloudServer server;
    protected CloudLevel level;
    @Getter
    protected CloudChunk chunk;
    private volatile boolean initialized;
    private boolean spawning;
    private volatile boolean spawned;
    @Getter
    private volatile boolean closed;
    private boolean hasTicked;
    protected boolean isPlayer;
    private int lastUpdate;
    public int ticksLived;
    protected int age;
    public int deadTicks;

    // Transform and movement
    @Getter
    protected Vector3f position = Vector3f.ZERO;
    protected float yaw;
    protected float pitch;
    @Getter
    protected Vector3f motion = Vector3f.ZERO;
    protected Vector3f lastPosition = Vector3f.ZERO;
    protected float lastYaw;
    protected float lastPitch;
    private final EntityMovementTracker movementTracker = new EntityMovementTracker();
    private final Deque<EntityMovementSegment> movementSegments = new ArrayDeque<>(MAX_MOVEMENT_SEGMENTS);
    private float baseScale = 1f;
    @Getter
    protected BoundingBox boundingBox;

    protected boolean noPhysics;
    protected Vector3f stuckSpeedMultiplier = Vector3f.ZERO;
    protected float entityCollisionReduction;
    @Getter
    private boolean onGround;
    private boolean horizontalCollision;
    @Nullable Vector3i supportingBlockPosition;
    boolean onGroundNoBlocks;
    @Getter
    private float highestPosition;
    private float fallDistance;

    // Riding
    @Getter
    private @Nullable Entity vehicle;
    private List<Entity> passengers = List.of();
    private boolean mounting;
    private boolean dismounting;
    private int boardingCooldown;

    // Teleport and portals
    private boolean dispatchingTeleport;
    private @Nullable CloudLevel teleportDestinationLevel;
    public int inPortalTicks;
    private int portalCooldownTicks;
    public boolean pendingPortalTransfer;
    public Vector3i portalEntryBlock;

    // Viewer tracking and metadata
    @Getter
    protected final SyncedEntityData data = new SyncedEntityData();
    private final Set<CloudPlayer> viewers = ConcurrentHashMap.newKeySet();

    // Health and effects
    @Getter
    protected float health = 20;
    private int defaultMaxHealth = 20;
    @Getter
    protected float absorption;
    private int noDamageTicks;
    @Setter
    protected EntityDamageEvent lastDamageCause;
    public int fireTicks;
    public boolean fireProof;
    protected final Reference2ObjectOpenHashMap<EffectType, ActivePotionEffect> effects = new Reference2ObjectOpenHashMap<>();

    public CloudEntity(EntityType<?> type, Location location) {
        this(type, location, new CloudEntityData());
    }

    protected CloudEntity(EntityType<?> type, Location location, CloudEntityData state) {
        this.type = Objects.requireNonNull(type, "type");
        Objects.requireNonNull(location, "location");

        this.data.setFlag(HAS_COLLISION, true);
        this.data.set(AIR_SUPPLY, (short) 400);
        this.data.set(AIR_SUPPLY_MAX, (short) 400);
        this.data.set(NAMETAG_ALWAYS_SHOW, (byte) 0);
        this.data.set(OWNER_EID, -1L);
        this.data.set(LEASH_HOLDER, -1L);
        this.data.set(SCALE, 1f);
        this.data.set(FREEZING_EFFECT_STRENGTH, 0f);
        this.data.set(SEAT_OFFSET, Vector3f.ZERO);

        this.persistence = new EntityPersistence(Objects.requireNonNull(state, "state"), this::markChunkDirty);
    }

    /**
     * Initializes the entity after its constructor and subclass fields are complete.
     */
    public final void initialize(Location location) {
        Objects.requireNonNull(location, "location");
        if (this.initialized || this.closed) {
            return;
        }
        checkArgument(hasFiniteTransform(location), "Entity transform must be finite");

        this.initialized = true;

        this.isPlayer = this instanceof CloudPlayer;

        this.level = (CloudLevel) location.getLevel();
        this.server = this.level.getServer();
        this.chunk = this.level.getLoadedChunk(location.getPosition());

        this.position = location.getPosition();
        this.highestPosition = this.position.getY();
        this.yaw = location.getYaw();
        this.pitch = location.getPitch();
        this.resetMovementBaseline();

        this.boundingBox = new BoundingBox(0, 0, 0, 0, 0, 0);

        this.initEntity();
        this.initializeHealth();
        this.recalculateBoundingBox();

        this.lastUpdate = this.server.getTick();

        if (this.isPlayer && !this.closed) {
            this.registerInLevel();
        }
    }

    protected void initEntity() {
        this.updateNetworkBounds();
        this.data.set(STRUCTURAL_INTEGRITY, (int) this.getHealth());

        Buoyancy buoyancy = CloudEntityRegistry.get().getComponent(this.type, EntityComponents.BUOYANCY);
        if (buoyancy != null) {
            this.data.set(IS_BUOYANT, true);
            this.data.set(BUOYANCY_DATA, BuoyancyMetadata.encode(buoyancy));
        }
    }

    protected void initializeHealth() {
        int maxHealth = this.getMaxHealth();
        if (this.isAlive() && this.getHealth() != maxHealth) {
            this.setHealth(maxHealth);
        }
    }

    @Override
    public boolean spawn() {
        return switch (this) {
            case Player _ -> this.spawnInternal(null, () -> true);
            case LivingEntity living -> this.spawn(new CreatureSpawnEvent(living, CreatureSpawnReason.DEFAULT));
            case DroppedItem droppedItem -> this.spawn(new ItemSpawnEvent(droppedItem));
            case Projectile projectile -> this.spawn(new ProjectileLaunchEvent(projectile));
            default -> this.spawn(new EntitySpawnEvent(this));
        };
    }

    public boolean spawn(EntitySpawnEvent event) {
        return this.spawn(event, () -> true);
    }

    protected boolean spawn(EntitySpawnEvent event, BooleanSupplier prepareSpawn) {
        return this.spawnInternal(Objects.requireNonNull(event, "event"), prepareSpawn);
    }

    private boolean spawnInternal(@Nullable EntitySpawnEvent event, BooleanSupplier prepareSpawn) {
        if (!this.initialized || this.closed || this.spawned || this.spawning) {
            return false;
        }

        if (event != null && event.getEntity() != this) {
            throw new IllegalArgumentException("Spawn event does not belong to this entity");
        }

        this.spawning = true;
        try {
            if (event != null) {
                this.server.getEventManager().fire(event);
                if (event.isCancelled()) {
                    this.remove(EntityRemoveCause.DISCARD);
                    return false;
                }
            }

            if (this.closed || !prepareSpawn.getAsBoolean()) {
                return false;
            }

            if (this.closed) {
                return false;
            }

            this.registerInLevel();
            return true;
        } finally {
            this.spawning = false;
        }
    }

    public void restoreFromStorage() {
        if (!this.initialized || this.closed || this.spawned || this.spawning) {
            throw new IllegalStateException("Entity is not ready for restoration");
        }

        this.registerInLevel();
    }

    private void registerInLevel() {
        if (this.spawned) {
            return;
        }

        CloudLevel spawnLevel = this.level;
        Vector3f spawnPosition = this.position;
        int chunkX = spawnPosition.getFloorX() >> 4;
        int chunkZ = spawnPosition.getFloorZ() >> 4;
        CloudChunk loadedChunk = spawnLevel.getLoadedChunk(spawnPosition);
        this.chunk = loadedChunk;
        if (loadedChunk != null) {
            loadedChunk.registerEntity(this);
        }

        this.spawned = true;
        spawnLevel.registerEntity(this);
        this.scheduleUpdate();

        if (loadedChunk != null) {
            this.refreshViewers();
            return;
        }

        spawnLevel.getChunkFuture(chunkX, chunkZ).thenAccept(chunk -> {
            Vector3f position = this.position;
            if (this.closed || !this.spawned || this.level != spawnLevel || this.chunk != null
                    || (position.getFloorX() >> 4) != chunkX || (position.getFloorZ() >> 4) != chunkZ) {
                return;
            }

            this.chunk = chunk;
            chunk.registerEntity(this);
            this.refreshViewers();
        });
    }

    @Override
    public void close() {
        this.remove(EntityRemoveCause.PLUGIN);
    }

    public static void remove(Entity entity, EntityRemoveCause cause) {
        if (entity instanceof CloudEntity cloudEntity) {
            cloudEntity.remove(cause);
        } else {
            entity.close();
        }
    }

    public void remove(EntityRemoveCause cause) {
        Objects.requireNonNull(cause, "cause");
        if (this.closed) {
            return;
        }

        this.closed = true;
        if (this.vehicle != null) {
            this.dismount(this.vehicle, false);
        }
        this.ejectPassengers();

        boolean wasSpawned = this.spawned;
        try {
            if (wasSpawned && !(this instanceof Player)) {
                this.server.getEventManager().fire(new EntityRemoveEvent(this, cause));
            }
        } finally {
            this.spawned = false;
            this.despawnFromAll();
            if (this.chunk != null) {
                this.chunk.unregisterEntity(this);
                this.chunk = null;
            }

            if (wasSpawned) {
                this.level.unregisterEntity(this);
            }
        }
    }

    @Override
    public boolean isValid() {
        return this.spawned && this.isAlive();
    }

    @Override
    public boolean isAlive() {
        return !this.isClosed() && this.getHealth() > 0;
    }

    public final void loadAdditionalData(NbtMap tag) {
        this.persistence.load(tag);
        this.normalizeData();
        this.loadBaseCustom(tag);
        this.loadCustom(tag);
    }

    public final void saveAdditionalData(NbtMapBuilder tag) {
        this.persistence.writeState(tag, this.customKeys());

        if (!(this instanceof CloudPlayer)) {
            tag.putString("identifier", this.type.getId().toString());
        }

        this.saveCustom(tag);
        this.persistence.writePluginData(tag);
    }

    @Override
    public PersistentDataContainer getPersistentDataContainer() {
        return this.persistence.pluginData();
    }

    protected final void markChunkDirty() {
        if (this.chunk != null) {
            this.chunk.markDirty();
        }
    }

    protected CloudEntityData data() {
        return this.persistence.state();
    }

    private void normalizeData() {
        CloudEntityData data = this.data();

        Integer maxHealthOverride = data.getBaseMaxHealthOverride();
        if (maxHealthOverride != null && maxHealthOverride <= 0) {
            data.setBaseMaxHealthOverride(null);
        }

        Integer frozen = data.getTicksFrozen();
        if (frozen != null) {
            int clamped = Math.clamp(frozen, 0, this.getMaxFreezeTicks());
            data.setTicksFrozen(clamped == 0 ? null : clamped);
        }
        data.setFreezeTickingLocked(Boolean.TRUE.equals(data.getFreezeTickingLocked()) ? Boolean.TRUE : null);

        if (!(this instanceof Bucketable)) {
            data.setFromBucket(null);
        }

        Set<String> tags = data.getTags();
        if (tags == null) {
            data.setTags(new LinkedHashSet<>());
        } else {
            List<String> kept = new ArrayList<>(tags);
            tags.clear();
            for (String tag : kept) {
                if (tag != null && !tag.isBlank() && tags.size() < MAX_SCOREBOARD_TAGS) {
                    tags.add(tag);
                }
            }
        }
    }

    /**
     * Restores shared runtime state and derived metadata before subclass load hooks.
     * Invalid transform values are ignored.
     *
     * @param tag the stored tag
     */
    protected void loadBaseCustom(NbtMap tag) {
        List<Float> pos = getFloatList(tag, "Pos");
        if (pos != null && pos.size() == 3) {
            Vector3f position = Vector3f.from(pos.get(0), pos.get(1), pos.get(2));
            if (isFinite(position)) {
                this.setPosition(position);
            }
        }

        List<Float> rotation = getFloatList(tag, "Rotation");
        if (rotation != null && rotation.size() == 2 && isFiniteRotation(rotation.get(0), rotation.get(1))) {
            this.setRotation(rotation.get(0), rotation.get(1));
        }

        List<Float> motion = getFloatList(tag, "Motion");
        if (motion != null && motion.size() == 3) {
            Vector3f velocity = Vector3f.from(motion.get(0), motion.get(1), motion.get(2));
            if (isFinite(velocity)) {
                this.setMotion(velocity);
            }
        }

        this.highestPosition = this.getY();

        float fallDistance = getFloat(tag, "FallDistance", 0f);
        this.setFallDistance(Float.isFinite(fallDistance) ? Math.max(0, fallDistance) : 0);
        this.fireTicks = Math.max(0, getShort(tag, "Fire", (short) 0));
        this.portalCooldownTicks = Math.max(0, tag.getInt("PortalCooldown"));
        this.setAirTicks(getShort(tag, "Air", (short) 400));
        this.setOnGround(getBoolean(tag, "OnGround", false));
        float scale = getFloat(tag, "Scale", Float.NaN);
        if (isValidScale(scale)) {
            this.setScale(scale);
        }

        this.data.set(FREEZING_EFFECT_STRENGTH, this.getFreezeTicks() / (float) this.getMaxFreezeTicks());

        this.effects.clear();
        tag.listenForList("ActiveEffects", NbtType.COMPOUND, effects -> {
            for (NbtMap effect : effects) {
                this.restorePotionEffect(new PotionEffect(
                        NetworkUtils.effectFromNetwork(getByte(effect, "Id", (byte) 0)),
                        getInt(effect, "Duration", 0),
                        Byte.toUnsignedInt(getByte(effect, "Amplifier", (byte) 0)),
                        getBoolean(effect, "Ambient", false),
                        getBoolean(effect, "ShowParticles", false)));
            }
        });

        this.clampHealthToMax();

        this.loadCustomName(tag);
        if (tag.containsKey("CustomNameVisible")) {
            this.setCustomNameVisible(getBoolean(tag, "CustomNameVisible", false));
        }

        if (tag.containsKey("CustomNameAlwaysVisible")) {
            this.setCustomNameAlwaysVisible(getBoolean(tag, "CustomNameAlwaysVisible", false));
        }
    }

    private void loadCustomName(NbtMap tag) {
        switch (tag.get("CustomName")) {
            case String text -> this.customName(text.isEmpty() ? null : Component.text(text));
            case NbtMap compound when compound.containsKey("json", NbtType.STRING) -> {
                try {
                    this.customName(GsonComponentSerializer.gson().deserialize(compound.getString("json")));
                } catch (JsonParseException ignored) {
                    // Leave the current name unchanged if the saved component is malformed.
                }
            }
            case null, default -> {
            }
        }
    }

    /**
     * Restores subclass state after shared runtime state has been loaded.
     *
     * @param tag the stored tag
     */
    protected void loadCustom(NbtMap tag) {
    }

    /**
     * Reads a numeric property leniently, like the mapper: any number binds,
     * anything else keeps the default.
     */
    protected static float getFloat(NbtMap tag, String key, float defaultValue) {
        Object value = tag.get(key);
        return value instanceof Number number ? number.floatValue() : defaultValue;
    }

    /**
     * Reads a short property leniently, like the mapper: any number binds,
     * anything else keeps the default.
     */
    protected static short getShort(NbtMap tag, String key, short defaultValue) {
        Object value = tag.get(key);
        return value instanceof Number number ? number.shortValue() : defaultValue;
    }

    /**
     * Reads an int property leniently, like the mapper: any number binds,
     * anything else keeps the default.
     */
    protected static int getInt(NbtMap tag, String key, int defaultValue) {
        Object value = tag.get(key);
        return value instanceof Number number ? number.intValue() : defaultValue;
    }

    /**
     * Reads a long property leniently, like the mapper: any number binds,
     * anything else keeps the default.
     */
    protected static long getLong(NbtMap tag, String key, long defaultValue) {
        Object value = tag.get(key);
        return value instanceof Number number ? number.longValue() : defaultValue;
    }

    /**
     * Reads a byte property leniently, like the mapper: any number binds,
     * anything else keeps the default.
     */
    protected static byte getByte(NbtMap tag, String key, byte defaultValue) {
        Object value = tag.get(key);
        return value instanceof Number number ? number.byteValue() : defaultValue;
    }

    /**
     * Reads a boolean property leniently, like the mapper: any number binds,
     * with zero meaning false, and anything else keeps the default.
     */
    protected static boolean getBoolean(NbtMap tag, String key, boolean defaultValue) {
        Object value = tag.get(key);
        return value instanceof Number number ? number.intValue() != 0 : defaultValue;
    }

    /**
     * Reads a position list leniently, like the mapper: any numeric element
     * list binds, anything else (an absent key, a missing list, a non-numeric
     * element) returns {@code null} so the caller keeps the current value.
     */
    protected static @Nullable List<Float> getFloatList(NbtMap tag, String key) {
        Object value = tag.get(key);
        if (!(value instanceof List<?> list)) {
            return null;
        }

        List<Float> result = new ArrayList<>(list.size());
        for (Object element : list) {
            if (!(element instanceof Number number)) {
                return null;
            }
            result.add(number.floatValue());
        }

        return result;
    }

    /**
     * Writes data that subclasses handle outside their data class.
     *
     * @param tag tag to write data to
     */
    protected void saveCustom(NbtMapBuilder tag) {
        Vector3f position = this.getPosition();
        tag.putList("Pos", NbtType.FLOAT, List.of(position.getX(), position.getY(), position.getZ()));
        tag.putList("Rotation", NbtType.FLOAT, List.of(this.yaw, this.pitch));

        Vector3f motion = this.getMotion();
        tag.putList("Motion", NbtType.FLOAT, List.of(motion.getX(), motion.getY(), motion.getZ()));
        tag.putFloat("FallDistance", this.fallDistance);
        tag.putShort("Fire", (short) Math.min(this.getFireTicks(), Short.MAX_VALUE));

        if (this.portalCooldownTicks > 0) {
            tag.putInt("PortalCooldown", this.portalCooldownTicks);
        }

        tag.putShort("Air", this.data.require(AIR_SUPPLY));
        tag.putBoolean("OnGround", this.onGround);
        tag.putFloat("Scale", this.getScale());

        Component name = this.customName();
        if (name != null) {
            if (name instanceof TextComponent text && !text.content().isEmpty() && !text.hasStyling() && text.children().isEmpty()) {
                tag.putString("CustomName", text.content());
            } else {
                tag.putCompound("CustomName", NbtMap.builder()
                        .putString("json", GsonComponentSerializer.gson().serialize(name))
                        .build());
            }
        }

        if (name != null || this.isCustomNameVisible()) {
            tag.putBoolean("CustomNameVisible", this.isCustomNameVisible());
        }

        if (name != null || this.isCustomNameAlwaysVisible()) {
            tag.putBoolean("CustomNameAlwaysVisible", this.isCustomNameAlwaysVisible());
        }

        if (!this.effects.isEmpty()) {
            List<NbtMap> effects = new ArrayList<>();
            for (ActivePotionEffect effect : this.effects.values()) {
                PotionEffect snapshot = effect.snapshot();
                effects.add(NbtMap.builder()
                        .putByte("Id", NetworkUtils.effectToNetwork(snapshot.getType()))
                        .putByte("Amplifier", (byte) snapshot.getAmplifier())
                        .putInt("Duration", snapshot.getDuration())
                        .putBoolean("Ambient", snapshot.isAmbient())
                        .putBoolean("ShowParticles", snapshot.hasParticles())
                        .build());
            }

            tag.putList("ActiveEffects", NbtType.COMPOUND, effects);
        }
    }

    /**
     * Returns the keys a subclass reads or writes through the custom hooks.
     * They are removed from the raw tag before saving, like owned keys.
     *
     * @return the custom keys
     */
    protected Set<String> customKeys() {
        return Set.of("Pos", "Motion", "Rotation", "FallDistance", "Fire", "PortalCooldown", "OnGround", "Air",
                "Scale", "CustomName", "CustomNameVisible", "CustomNameAlwaysVisible", "ActiveEffects");
    }

    @Override
    public Optional<EntitySnapshot> createSnapshot() {
        if (this instanceof CloudPlayer || this.type == EntityTypes.PLAYER || Player.class.isAssignableFrom(this.type.getEntityClass())) {
            return Optional.empty();
        }

        return Optional.of(CloudEntitySnapshot.capture(this));
    }

    public long getUniqueId() {
        return this.runtimeId;
    }

    @Override
    public CloudServer getServer() {
        return server;
    }

    @Override
    public CloudLevel getLevel() {
        return level;
    }

    @Nullable
    @Override
    public Entity getOwner() {
        long ownerId = this.data.require(OWNER_EID);
        if (ownerId == -1) {
            this.owner = null;
        } else if (this.owner == null || this.owner.getUniqueId() != ownerId || this.owner.isClosed()) {
            this.owner = this.level == null ? null : this.level.getEntityByRuntimeId(ownerId);
            if (this.owner != null && this.owner.isClosed()) {
                this.owner = null;
            }
        }

        return this.owner;
    }

    @Override
    public void setOwner(@Nullable Entity entity) {
        this.owner = entity;
        this.data.set(OWNER_EID, entity == null ? -1 : entity.getUniqueId());
    }

    @Override
    public String getName() {
        Component name = this.customName();
        return name == null ? this.getDefaultName() : PlainTextComponentSerializer.plainText().serialize(name);
    }

    protected String getDefaultName() {
        String name = CloudEntityRegistry.get().getLegacyName(this.type.getId());
        return name != null ? name : this.type.getId().toString();
    }

    @Override
    public Component displayName() {
        Component name = this.customName();
        if (name != null) {
            return name;
        }

        if ("minecraft".equals(this.type.getId().getNamespace())) {
            return Component.translatable("entity." + this.type.getId().getName() + ".name");
        }

        return Component.text(this.type.getId().toString());
    }

    @Override
    public @Nullable Component customName() {
        return this.customName;
    }

    @Override
    public void customName(@Nullable Component name) {
        if (!Objects.equals(this.customName, name)) {
            CharSequence nameplate = name == null ? this.defaultNameplate() : BedrockLegacyTextSerializer.getInstance().serialize(name);
            this.customName = name;
            this.data.set(NAME, nameplate);
            this.markChunkDirty();
        }
    }

    protected CharSequence defaultNameplate() {
        return "";
    }

    @Override
    public boolean isCustomNameVisible() {
        return this.data.getFlag(CAN_SHOW_NAME);
    }

    @Override
    public void setCustomNameVisible(boolean value) {
        if (this.isCustomNameVisible() != value) {
            this.markChunkDirty();
        }
        this.data.setFlag(CAN_SHOW_NAME, value);
    }

    public boolean isCustomNameAlwaysVisible() {
        return this.data.require(NAMETAG_ALWAYS_SHOW) == 1;
    }

    public void setCustomNameAlwaysVisible(boolean value) {
        if (this.isCustomNameAlwaysVisible() != value) {
            this.markChunkDirty();
        }
        this.data.set(NAMETAG_ALWAYS_SHOW, (byte) (value ? 1 : 0));
    }

    @Override
    public Set<String> getScoreboardTags() {
        return Set.copyOf(this.data().getTags());
    }

    @Override
    public boolean hasScoreboardTag(String tag) {
        return this.data().getTags().contains(tag);
    }

    @Override
    public boolean addScoreboardTag(String tag) {
        String value = Objects.requireNonNull(tag, "tag");
        if (value.isBlank()) {
            throw new IllegalArgumentException("tag cannot be blank");
        }

        Set<String> tags = this.data().getTags();
        if (tags.size() >= MAX_SCOREBOARD_TAGS || !tags.add(value)) {
            return false;
        }

        this.markChunkDirty();
        return true;
    }

    @Override
    public boolean removeScoreboardTag(String tag) {
        if (!this.data().getTags().remove(Objects.requireNonNull(tag, "tag"))) {
            return false;
        }

        this.markChunkDirty();
        return true;
    }

    @Override
    public Pose getPose() {
        return Pose.STANDING;
    }

    public float getWidth() {
        return 0;
    }

    public float getLength() {
        return 0;
    }

    public float getHeight() {
        return 0;
    }

    public float getEyeHeight() {
        return this.getHeight() / 2 + 0.1f;
    }

    public float getBaseOffset() {
        return 0;
    }

    public float getScale() {
        return this.baseScale;
    }

    public void setScale(float scale) {
        checkArgument(isValidScale(scale), "Scale must be between %s and %s", MIN_SCALE, MAX_SCALE);
        this.baseScale = scale;
        this.refreshNetworkScale();
        this.recalculateBoundingBox();
    }

    protected final void refreshNetworkScale() {
        this.data.set(SCALE, this.getNetworkScale());
    }

    protected float getNetworkScale() {
        return this.getScale();
    }

    private static boolean isValidScale(float scale) {
        return scale >= MIN_SCALE && scale <= MAX_SCALE;
    }

    public void recalculateBoundingBox() {
        float height = this.getHeight() * this.getScale();
        float radius = (this.getWidth() * this.getScale()) / 2;
        this.boundingBox = new BoundingBox(
                this.position.getX() - radius, this.position.getY(), this.position.getZ() - radius,
                this.position.getX() + radius, this.position.getY() + height, this.position.getZ() + radius
        );

        this.updateNetworkBounds();
    }

    public Location getLocation() {
        return Location.from(this.position, this.yaw, this.pitch, this.level);
    }

    @Override
    public float getX() {
        return this.position.getX();
    }

    @Override
    public float getY() {
        return this.position.getY();
    }

    @Override
    public float getZ() {
        return this.position.getZ();
    }

    @Override
    public float getYaw() {
        return yaw;
    }

    @Override
    public float getPitch() {
        return pitch;
    }

    public boolean setPosition(Vector3f pos) {
        checkNotNull(pos, "position");
        checkArgument(isFinite(pos), "Position must be finite");
        if (this.closed) {
            return false;
        }

        this.position = pos;
        this.recalculateBoundingBox();
        if (this.initialized) {
            this.checkChunks();
        }

        return true;
    }

    public void setRotation(float yaw, float pitch) {
        checkArgument(isFiniteRotation(yaw, pitch), "Rotation must be finite");
        this.yaw = yaw;
        this.pitch = pitch;
        this.scheduleUpdate();
    }

    public boolean setPositionAndRotation(Vector3f pos, float yaw, float pitch) {
        checkArgument(isFiniteRotation(yaw, pitch), "Rotation must be finite");
        if (this.setPosition(pos)) {
            this.setRotation(yaw, pitch);
            return true;
        }

        return false;
    }

    private static boolean hasFiniteTransform(Location location) {
        return isFinite(location.getPosition()) && isFiniteRotation(location.getYaw(), location.getPitch());
    }

    private static boolean isFiniteRotation(float yaw, float pitch) {
        return Float.isFinite(yaw) && Float.isFinite(pitch);
    }

    private static boolean isFinite(Vector3f vector) {
        return Float.isFinite(vector.getX()) && Float.isFinite(vector.getY()) && Float.isFinite(vector.getZ());
    }

    public Direction getDirection() {
        double rotation = this.yaw % 360;
        if (rotation < 0) {
            rotation += 360.0;
        }

        if ((0 <= rotation && rotation < 45) || (315 <= rotation && rotation < 360)) {
            return Direction.SOUTH;
        } else if (45 <= rotation && rotation < 135) {
            return Direction.WEST;
        } else if (135 <= rotation && rotation < 225) {
            return Direction.NORTH;
        } else if (225 <= rotation && rotation < 315) {
            return Direction.EAST;
        } else {
            return null;
        }
    }

    public Direction getHorizontalDirection() {
        return Direction.fromYaw(this.yaw);
    }

    public CardinalDirection getCardinalDirection() {
        return CardinalDirection.values()[GenericMath.floor(this.yaw / 22.5 + 0.5) & 15];
    }

    public Vector3f getDirectionVector() {
        double y = -Math.sin(Math.toRadians(this.getPitch()));
        double xz = Math.cos(Math.toRadians(this.getPitch()));
        double x = -xz * Math.sin(Math.toRadians(this.getYaw()));
        double z = xz * Math.cos(Math.toRadians(this.getYaw()));
        return GenericMath.normalizeSafe(Vector3f.from(x, y, z));
    }

    public Vector2f getHorizontalLookVector() {
        double angle = Math.toRadians(this.yaw) - Math.PI / 2;
        return Vector2f.from(-Math.cos(angle), -Math.sin(angle)).normalize();
    }

    public float getGravity() {
        return 0;
    }

    public float getDrag() {
        return 0;
    }

    protected float getStepHeight() {
        return 0;
    }

    public boolean isImmobile() {
        return this.data.getFlag(NO_AI);
    }

    public void setImmobile(boolean value) {
        this.data.setFlag(NO_AI, value);
    }

    public void setCanClimb(boolean value) {
        this.data.setFlag(CAN_CLIMB, value);
    }

    public boolean setMotion(Vector3f motion) {
        checkNotNull(motion, "motion");
        checkArgument(isFinite(motion), "Motion must be finite");
        if (this.closed) {
            return false;
        }

        if (this.hasTicked) {
            EntityMotionEvent event = new EntityMotionEvent(this, motion);
            this.server.getEventManager().fire(event);
            if (event.isCancelled() || this.closed) {
                return false;
            }
        }

        this.motion = motion;
        this.onMotionChanged();

        if (this.hasTicked && !this.closed) {
            this.updateMovement();
        }

        return true;
    }

    /**
     * Updates motion-dependent state before accepted motion is sent to viewers.
     */
    protected void onMotionChanged() {
    }

    /**
     * Applies an attributed push after listeners can edit or cancel it.
     */
    public void applyKnockback(Vector3f knockback, KnockbackCause cause, @Nullable Entity sourceEntity) {
        EntityKnockbackEvent event = new EntityKnockbackEvent(this, cause, sourceEntity, knockback);
        if (this.isClosed()) {
            return;
        }

        if (this.hasTicked) {
            this.server.getEventManager().fire(event);
        }

        if (!event.isCancelled() && !this.isClosed()) {
            this.setMotion(this.getMotion().add(event.getKnockback()));
        }
    }

    protected final void setOnGround(boolean onGround) {
        this.updateGroundContact(onGround, null);
    }

    public final void updateCollisionContact(boolean horizontal, boolean onGround, @Nullable Vector3f movement) {
        this.horizontalCollision = horizontal;
        this.updateGroundContact(onGround, movement);
    }

    private void updateGroundContact(boolean onGround, @Nullable Vector3f movement) {
        this.onGround = onGround;
        EntityMovementController.checkSupportingBlock(this, onGround && this.level != null, movement);
    }

    protected final boolean hasHorizontalCollision() {
        return this.horizontalCollision;
    }

    @Override
    public Optional<Vector3i> getSupportingBlockPosition() {
        return Optional.ofNullable(this.supportingBlockPosition);
    }

    public void move(Vector3f movement) {
        this.move(MovementType.SELF, movement);
    }

    public void move(float dx, float dy, float dz) {
        this.move(MovementType.SELF, dx, dy, dz);
    }

    public void move(MovementType type, Vector3f movement) {
        this.move(type, movement.getX(), movement.getY(), movement.getZ());
    }

    public void move(MovementType type, float dx, float dy, float dz) {
        EntityMovementController.move(this, type, dx, dy, dz);
    }

    public void displace(Vector3f displacement) {
        checkNotNull(displacement, "displacement");
        if (displacement.equals(Vector3f.ZERO)) {
            return;
        }

        BoundingBox previousBox = this.boundingBox;
        if (this.setPosition(this.position.add(displacement))) {
            this.highestPosition += displacement.getY();
            this.recordMovement(previousBox, this.boundingBox);
            this.sendAuthoritativeDisplacement();
        }
    }

    public void recordMovement(BoundingBox previousBox, BoundingBox currentBox) {
        if (this.movementSegments.size() >= MAX_MOVEMENT_SEGMENTS) {
            EntityMovementSegment first = this.movementSegments.removeFirst();
            EntityMovementSegment second = this.movementSegments.removeFirst();
            this.movementSegments.addFirst(new EntityMovementSegment(first.fromBox(), second.toBox()));
        }

        this.movementSegments.add(new EntityMovementSegment(previousBox, currentBox));
    }

    public List<EntityMovementSegment> drainMovementSegments() {
        if (this.movementSegments.isEmpty()) {
            BoundingBox boundingBox = this.getBoundingBox();
            return List.of(new EntityMovementSegment(boundingBox, boundingBox));
        }

        List<EntityMovementSegment> movements = List.copyOf(this.movementSegments);
        this.movementSegments.clear();
        return movements;
    }

    public void makeStuckInBlock(Vector3f speedMultiplier) {
        this.resetFallDistance();
        this.stuckSpeedMultiplier = speedMultiplier;
    }

    public void updateMovement() {
        this.movementTracker.publishChanges(this);
    }

    public void addMovement(double x, double y, double z, double yaw, double pitch, double headYaw) {
        this.level.addEntityMovement(this, x, y, z, yaw, pitch, headYaw);
    }

    public void addMotion(Vector3f motion) {
        SetEntityMotionPacket packet = new SetEntityMotionPacket();
        packet.setRuntimeEntityId(this.getRuntimeId());
        packet.setMotion(motion);
        if (this.isPlayer) {
            packet.setTick(((CloudPlayer) this).getClientTick());
        }

        this.broadcastToViewers(packet);
    }

    public void sendAuthoritativeDisplacement() {
        this.publishAuthoritativeDisplacement();
    }

    private void publishAuthoritativeDisplacement() {
        this.resetMovementBaseline();
        this.broadcastToViewers(this.createAuthoritativeDisplacementPacket());
    }

    private void resetMovementBaseline() {
        this.movementTracker.resetTransform(this.position, this.yaw, this.pitch);
        this.lastPosition = this.position;
        this.lastYaw = this.yaw;
        this.lastPitch = this.pitch;
    }

    protected MoveEntityAbsolutePacket createAuthoritativeDisplacementPacket() {
        MoveEntityAbsolutePacket movement = new MoveEntityAbsolutePacket();
        movement.setRuntimeEntityId(this.getRuntimeId());
        movement.setPosition(this.getPosition().add(0, this.getBaseOffset(), 0));
        movement.setRotation(Vector3f.from(this.getPitch(), this.getYaw(), this.getYaw()));
        movement.setOnGround(this.onGround);
        movement.setTeleported(true);
        return movement;
    }

    public boolean canCollide() {
        return true;
    }

    public boolean canCollideWith(Entity entity) {
        return this.hasTicked && entity != null && !this.sharesRootVehicle(entity) && entity.canBeCollidedWith(this);
    }

    public boolean canBeCollidedWith(@Nullable Entity entity) {
        return false;
    }

    public boolean blocksBuilding() {
        return false;
    }

    protected boolean hasMovementEntityCollisions() {
        return true;
    }

    public boolean isPushable() {
        return false;
    }

    protected boolean sharesRootVehicle(Entity entity) {
        return rootVehicle(this) == rootVehicle(entity);
    }

    protected static Entity rootVehicle(Entity entity) {
        Entity vehicle = entity.getVehicle();
        while (vehicle != null) {
            entity = vehicle;
            vehicle = entity.getVehicle();
        }
        return entity;
    }

    public void onEntityCollision(Entity entity) {
        if (this.sharesRootVehicle(entity)) {
            return;
        }

        if (entity.getVehicle() != this && !entity.getPassengers().contains(this)) {
            double dx = entity.getX() - this.getX();
            double dy = entity.getZ() - this.getZ();
            double dz = Math.max(Math.abs(dx), Math.abs(dy));

            if (dz >= 0.009999999776482582D) {
                dz = MathHelper.sqrt((float) dz);
                dx /= dz;
                dy /= dz;
                double d3 = 1.0D / dz;

                if (d3 > 1.0D) {
                    d3 = 1.0D;
                }

                dx *= d3;
                dy *= d3;
                dx *= 0.05000000074505806;
                dy *= 0.05000000074505806;
                dx *= 1F + entityCollisionReduction;

                if (this.vehicle == null) {
                    this.motion = this.motion.sub(dx, 0, dy);
                }
            }
        }
    }

    protected void checkBlockCollision() {
        EntityInsideBlockScanner.scan(this);
    }

    protected boolean moveTowardsClosestSpace(Vector3f pos) {
        float centerY = (this.boundingBox.getMinY() + this.boundingBox.getMaxY()) / 2f;
        return this.moveTowardsClosestSpace(pos.getX(), centerY, pos.getZ());
    }

    protected boolean moveTowardsClosestSpace(float x, float y, float z) {
        int i = GenericMath.floor(x);
        int j = GenericMath.floor(y);
        int k = GenericMath.floor(z);
        float diffX = x - i;
        float diffY = y - j;
        float diffZ = z - k;

        Direction closestDirection = Direction.UP;
        float closestDistance = Float.MAX_VALUE;
        Vector3i blockPos = Vector3i.from(i, j, k);

        for (Direction direction : COLLISION_ESCAPE_DIRECTIONS) {
            Vector3i neighborPos = direction.relative(blockPos);
            Block neighbor = this.level.getLoadedBlock(neighborPos.getX(), neighborPos.getY(), neighborPos.getZ());
            if (neighbor != null && !this.level.isFullBlock(neighborPos, neighbor.getState())) {
                float axisDelta = switch (direction.getAxis()) {
                    case X -> diffX;
                    case Y -> diffY;
                    case Z -> diffZ;
                };

                float orientedDelta = direction.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1f - axisDelta : axisDelta;
                if (orientedDelta < closestDistance) {
                    closestDistance = orientedDelta;
                    closestDirection = direction;
                }
            }
        }

        if (closestDistance == Float.MAX_VALUE) {
            return false;
        }

        float force = (float) (ThreadLocalRandom.current().nextDouble() * 0.2d + 0.1d);
        float step = closestDirection.getAxisDirection().getStep();
        Vector3f scaledMotion = this.motion.mul(0.75f);
        this.motion = switch (closestDirection.getAxis()) {
            case X -> Vector3f.from(step * force, scaledMotion.getY(), scaledMotion.getZ());
            case Y -> Vector3f.from(scaledMotion.getX(), step * force, scaledMotion.getZ());
            case Z -> Vector3f.from(scaledMotion.getX(), scaledMotion.getY(), step * force);
        };

        return true;
    }

    public final void scheduleUpdate() {
        if (this.spawned && !this.closed) {
            this.level.scheduleEntityUpdate(this);
        }
    }

    public final boolean tick(int currentTick) {
        if (this.closed) {
            return false;
        }

        int elapsedTicks = currentTick - this.lastUpdate;
        if (elapsedTicks <= 0) {
            return true;
        }

        this.lastUpdate = currentTick;
        this.hasTicked = true;
        boolean needsNextTick = this.tickEntity(currentTick, elapsedTicks);
        if (this.closed) {
            return false;
        }

        if (this.isAlive()) {
            TickEntityHandler handler = CloudEntityRegistry.get().getComponent(this.type, EntityComponents.ON_TICK);
            if (handler != null) {
                boolean componentNeedsNextTick = handler.execute(this, currentTick);
                if (this.closed) {
                    return false;
                }
                this.updateMovement();
                needsNextTick = needsNextTick || componentNeedsNextTick || !this.isAlive();
            }
        }

        if (this.closed) {
            return false;
        }

        this.flushEntityData();
        return !this.closed && needsNextTick;
    }

    protected boolean tickEntity(int currentTick, int elapsedTicks) {
        if (!this.isAlive()) {
            ++this.deadTicks;
            if (this.deadTicks >= 10) {
                this.despawnFromAll();
                if (!this.isPlayer) {
                    this.remove(EntityRemoveCause.DEATH);
                }
            }

            return this.deadTicks < 10;
        }

        this.entityBaseTick(elapsedTicks);
        if (this.closed) {
            return false;
        }
        this.updateMovement();
        return true;
    }

    public boolean entityBaseTick() {
        return this.entityBaseTick(1);
    }

    public boolean entityBaseTick(int tickDiff) {
        this.boardingCooldown = Math.max(0, this.boardingCooldown - tickDiff);

        if (!this.isAlive()) {
            this.clearActivePotionEffects(PotionEffectCause.DEATH);
            this.despawnFromAll();

            if (!this.isPlayer) {
                this.remove(EntityRemoveCause.DEATH);
            }

            return false;
        }

        if (this.vehicle != null && (!this.vehicle.isAlive() || this.vehicle.isClosed())) {
            this.dismount(this.vehicle, false);
        }

        this.updatePassengers();

        this.tickPotionEffects(tickDiff);

        boolean hasUpdate = false;

        boolean wasBurning = this.isOnFire();
        boolean wasFreezing = this.getFreezeTicks() > 0;

        this.checkBlockCollision();
        this.applyLiquidCurrent();

        if (wasBurning && !this.isOnFire() || wasFreezing && this.getFreezeTicks() == 0) {
            float pitch = 1.6f + (this.level.getRandom().nextFloat() - this.level.getRandom().nextFloat()) * 0.4f;
            this.level.playSound(this.position, SoundTypes.RANDOM_FIZZ, 0.7f, pitch);
        }

        if (this.position.getY() < this.level.getMinHeight() - 64) {
            this.onBelowLevel();
            if (this.closed) {
                return false;
            }

            hasUpdate = true;
        }

        hasUpdate |= this.tickFire(tickDiff);
        this.noDamageTicks = Math.max(0, this.noDamageTicks - tickDiff);
        this.tickPortalTransition();

        this.age += tickDiff;
        this.ticksLived += tickDiff;

        return hasUpdate;
    }

    private boolean tickFire(int tickDiff) {
        if (this.fireTicks <= 0) {
            return false;
        }

        if (this.fireProof) {
            this.fireTicks = (int) Math.max(0, this.fireTicks - 4L * tickDiff);
        } else {
            boolean damageDue = this.fireTicks % 20 == 0 || tickDiff > 20;
            if (damageDue && !this.hasPotionEffect(EffectTypes.FIRE_RESISTANCE)) {
                this.damage(1, DamageSource.of(DamageTypes.ON_FIRE));
            }
            this.fireTicks -= tickDiff;
        }

        if (this.fireTicks <= 0) {
            this.extinguish();
            return false;
        }

        if (this.fireProof || this instanceof CloudPlayer player && player.isSpectator()) {
            return false;
        }

        this.data.setFlag(ON_FIRE, true);
        return true;
    }

    private void tickPotionEffects(int tickDiff) {
        if (!this.effects.isEmpty()) {
            for (ActivePotionEffect effect : List.copyOf(this.effects.values())) {
                for (int elapsed = 0; elapsed < tickDiff && this.isAlive() && this.effects.get(effect.getType()) == effect; elapsed++) {
                    if (effect.shouldApplyTick(this.age + elapsed)) {
                        effect.applyTick(this);
                    }

                    if (this.effects.get(effect.getType()) != effect) {
                        break;
                    }

                    effect.decreaseDuration(1);
                    if (!effect.isInfinite() && effect.getDuration() == 0) {
                        this.removePotionEffect(effect.getType(), PotionEffectCause.EXPIRATION);
                        break;
                    }
                }
            }
        }
    }

    protected void onBelowLevel() {
        this.remove(EntityRemoveCause.OUT_OF_WORLD);
    }

    @Override
    public float getFallDistance() {
        return this.fallDistance;
    }

    @Override
    public void setFallDistance(float fallDistance) {
        if (!Float.isFinite(fallDistance) || fallDistance < 0) {
            throw new IllegalArgumentException("Fall distance must be finite and non-negative");
        }

        this.fallDistance = fallDistance;
        this.highestPosition = this.position.getY() + fallDistance;
    }

    @Override
    public void resetFallDistance() {
        this.setFallDistance(0);
    }

    protected void updateFallState(boolean onGround) {
        if (!onGround) {
            this.highestPosition = Math.max(this.highestPosition, this.getY());
            this.fallDistance = Math.max(0, this.highestPosition - this.getY());
            return;
        }

        float distance = Math.max(0, this.highestPosition - this.getY());
        this.resetFallDistance();
        this.onLanding(distance);
    }

    protected void onLanding(float fallDistance) {
        if (fallDistance > 0 && this instanceof CloudLivingEntity && !this.level.getLiquidState(this.position.toInt()).getType().isSameFamily(LiquidTypes.WATER)) {
            this.fall(fallDistance);
        }
    }

    protected void fall(float fallDistance) {
        Block down = this.level.getBlock(this.getLandingBlockPosition());
        down.requireComponent(BlockComponents.ON_FALL_ON).execute(down, this, fallDistance);
    }

    protected Vector3i getLandingBlockPosition() {
        Vector3i support = this.supportingBlockPosition;
        return support != null ? support : this.position.sub(0, 0.2f, 0).toInt();
    }

    public void applyFallDamage(float fallDistance) {
        this.applyFallDamage(fallDistance, 1, DamageSource.of(DamageTypes.FALL));
    }

    public void applyFallDamage(float fallDistance, float multiplier, DamageSource source) {
        Objects.requireNonNull(source, "source");
        if (!Float.isFinite(fallDistance) || fallDistance < 0) {
            throw new IllegalArgumentException("Fall distance must be finite and non-negative");
        }

        if (!Float.isFinite(multiplier) || multiplier < 0) {
            throw new IllegalArgumentException("Fall damage multiplier must be finite and non-negative");
        }

        if (this.hasPotionEffect(EffectTypes.SLOW_FALLING)) {
            return;
        }

        if (this.isPlayer && !level.getGameRules().get(GameRules.FALL_DAMAGE)) {
            return;
        }

        ActivePotionEffect jumpBoost = this.effects.get(EffectTypes.JUMP_BOOST);
        long jumpReduction = jumpBoost == null ? 0 : jumpBoost.getAmplifier() + 1L;
        float damage = (float) Math.floor(((double) fallDistance + 1.0e-6 - 3 - jumpReduction) * multiplier);

        if (damage > 0) {
            this.damage(damage, source);
        }
    }

    public boolean isInsideOfSolid() {
        if (this.noPhysics) {
            return false;
        }

        float eyeY = this.getY() + this.getEyeHeight();
        float width = Math.max(0.1f, (this.boundingBox.getMaxX() - this.boundingBox.getMinX()) * 0.8f);
        float halfWidth = width / 2f;
        BoundingBox eyeBox = new BoundingBox(
                this.getX() - halfWidth,
                eyeY - CloudVoxelShapes.EPSILON,
                this.getZ() - halfWidth,
                this.getX() + halfWidth,
                eyeY + CloudVoxelShapes.EPSILON,
                this.getZ() + halfWidth
        );

        return this.level.collidesWithSuffocatingBlock(this, eyeBox);
    }

    public boolean isInsideOfFire() {
        return this.level.hasLoadedBlockIntersecting(this.getBoundingBox(), block -> block.getState().getType() == FIRE);
    }

    public boolean isInsideOfWater() {
        float y = this.getY() + this.getEyeHeight();
        Block block = this.level.getLoadedBlock(this.position.getFloorX(), GenericMath.floor(y), this.position.getFloorZ());
        if (block == null) {
            return false;
        }

        LiquidState state = block.getLiquid();
        if (state.getType().isSameFamily(LiquidTypes.WATER)) {
            float height = this.level.getLiquidHeight(block.getPosition());
            return y < block.getY() + height;
        } else {
            return false;
        }
    }

    protected boolean isTouchingWater() {
        return this.scanLiquidContact(false).touchingWater();
    }

    @Override
    public boolean isInWaterOrRain() {
        if (this.isTouchingWater()) {
            return true;
        }

        if (!this.level.isRaining() || this.level.getDimension() != CloudLevel.DIMENSION_OVERWORLD) {
            return false;
        }

        Vector3i feet = this.position.toInt();
        Vector3i head = this.position.add(0, this.getHeight(), 0).toInt();
        return this.isExposedToRain(feet) || this.isExposedToRain(head);
    }

    private boolean isExposedToRain(Vector3i position) {
        if (this.level.getLoadedBlock(position) == null || !this.level.canBlockSeeSky(position)) {
            return false;
        }

        CloudBiome biome = CloudBiomeRegistry.get().getBiome(this.level.getBiomeId(position.getX(), position.getY(), position.getZ()));
        return biome != null && biome.hasPrecipitation() && !biome.coldEnoughToSnow(position.getX(), position.getY(), position.getZ(), this.level.getSeaLevel());
    }

    /**
     * @return whether liquid currents may move this entity
     */
    public boolean canBeMovedByCurrents() {
        return true;
    }

    private void applyLiquidCurrent() {
        LiquidContact contact = this.scanLiquidContact(this.canBeMovedByCurrents());
        if (contact.flow().lengthSquared() > 0) {
            this.motion = this.motion.add(contact.flow().normalize().mul(0.014f));
        }

        if (contact.touchingWater() && this.fireTicks > 0) {
            this.extinguish();
        }
    }

    private LiquidContact scanLiquidContact(boolean includeFlow) {
        BoundingBox box = this.getBoundingBox().deflate(0.001f, 0.001f, 0.001f);

        int minX = GenericMath.floor(box.getMinX());
        int maxX = GenericMath.floor(box.getMaxX());
        int minY = GenericMath.floor(box.getMinY());
        int maxY = GenericMath.floor(box.getMaxY());
        int minZ = GenericMath.floor(box.getMinZ());
        int maxZ = GenericMath.floor(box.getMaxZ());

        Vector3f total = Vector3f.ZERO;
        boolean touchingWater = false;

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = this.level.getLoadedBlock(x, y, z);
                    if (block == null) {
                        continue;
                    }

                    LiquidState liquid = block.getLiquid();
                    if (liquid.isEmpty()) {
                        continue;
                    }

                    float height = this.level.getLiquidHeight(block.getPosition());
                    if (y + height <= box.getMinY()) {
                        continue;
                    }

                    if (liquid.getType().isSameFamily(LiquidTypes.WATER)) {
                        touchingWater = true;
                    }

                    if (includeFlow) {
                        total = total.add(this.level.getLiquidFlow(block.getPosition()));
                    }
                }
            }
        }

        return new LiquidContact(touchingWater, total);
    }

    public int getAirTicks() {
        return this.data.require(AIR_SUPPLY);
    }

    public void setAirTicks(int ticks) {
        this.data.set(AIR_SUPPLY, (short) ticks);
    }

    @Override
    public int getFireTicks() {
        return fireTicks;
    }

    @Override
    public void setOnFire(int seconds) {
        if (seconds < 0) {
            throw new IllegalArgumentException("Fire duration cannot be negative");
        }

        float reduction = this.getBurningTimeReduction();
        if (!Float.isFinite(reduction) || reduction < 0 || reduction > 1) {
            throw new IllegalArgumentException("Burning time reduction must be in [0, 1]");
        }

        int ticks = (int) Math.min(Integer.MAX_VALUE, Math.ceil(seconds * 20.0 * (1.0 - reduction)));
        if (ticks > this.fireTicks) {
            this.fireTicks = ticks;
        }
    }

    protected float getBurningTimeReduction() {
        return 0;
    }

    public void extinguish() {
        this.fireTicks = 0;
        this.data.setFlag(ON_FIRE, false);
    }

    public void onStruckByLightning(LightningBolt lightningBolt) {
        DamageSource source = DamageSource.of(DamageTypes.LIGHTNING_BOLT);
        if (this.damage(5, source)) {
            if (this.fireTicks < 8 * 20) {
                this.setOnFire(8);
            }
        }
    }

    @Override
    public int getFreezeTicks() {
        Integer ticksFrozen = data().getTicksFrozen();
        return ticksFrozen == null ? 0 : ticksFrozen;
    }

    @Override
    public void setFreezeTicks(int ticks) {
        int clampedTicks = Math.clamp(ticks, 0, this.getMaxFreezeTicks());
        if (this.getFreezeTicks() == clampedTicks) {
            return;
        }
        data().setTicksFrozen(clampedTicks == 0 ? null : clampedTicks);
        this.data.set(FREEZING_EFFECT_STRENGTH, this.getFreezeTicks() / (float) this.getMaxFreezeTicks());
    }

    @Override
    public int getMaxFreezeTicks() {
        return DEFAULT_MAX_FREEZE_TICKS;
    }

    @Override
    public boolean isFreezeTickingLocked() {
        return Boolean.TRUE.equals(this.data().getFreezeTickingLocked());
    }

    @Override
    public void lockFreezeTicks(boolean locked) {
        this.data().setFreezeTickingLocked(locked ? Boolean.TRUE : null);
    }

    public boolean isInvulnerable() {
        return data().isInvulnerable();
    }

    public void setInvulnerable(boolean invulnerable) {
        data().setInvulnerable(invulnerable);
    }

    public boolean isFromBucket() {
        return Boolean.TRUE.equals(data().getFromBucket());
    }

    public void setFromBucket(boolean fromBucket) {
        data().setFromBucket(this instanceof Bucketable ? fromBucket : null);
    }

    public boolean teleport(Vector3f pos) {
        return this.teleport(pos, TeleportCause.PLUGIN);
    }

    public boolean teleport(Vector3f pos, TeleportCause cause) {
        return this.teleport(Location.from(pos, this.yaw, this.pitch, this.level), cause);
    }

    public boolean teleport(Location location) {
        return this.teleport(location, TeleportCause.PLUGIN);
    }

    public final boolean teleport(Location location, TeleportCause cause) {
        Objects.requireNonNull(cause, "cause");
        Objects.requireNonNull(location, "location");
        if (this.isTeleportBlocked(location)) {
            return false;
        }

        Location destination;
        this.dispatchingTeleport = true;
        try {
            destination = this.requestTeleportDestination(this.getLocation(), location, cause);
        } finally {
            this.dispatchingTeleport = false;
        }

        return destination != null && this.teleportWithoutEvent(destination);
    }

    protected final boolean isTeleportBlocked(Location location) {
        return this.dispatchingTeleport || this.teleportDestinationLevel != null || this.isTeleportUnavailable(location);
    }

    protected boolean isTeleportUnavailable(Location location) {
        return !this.initialized || this.closed || !hasFiniteTransform(location) || !(location.getLevel() instanceof CloudLevel);
    }

    protected @Nullable Location requestTeleportDestination(Location from, Location to, TeleportCause cause) {
        EntityTeleportEvent event = new EntityTeleportEvent(this, from, to, cause);
        this.server.getEventManager().fire(event);
        return event.isCancelled() ? null : event.getTo();
    }

    public boolean teleportWithoutEvent(Location location) {
        Objects.requireNonNull(location, "location");
        if (this.isTeleportBlocked(location)) {
            return false;
        }

        CloudLevel destination = (CloudLevel) location.getLevel();
        CloudLevel previousLevel = this.level;
        boolean changesLevel = previousLevel != destination;
        this.teleportDestinationLevel = destination;
        try {
            if (changesLevel && !this.detachForLevelTransfer(previousLevel, destination)) {
                return false;
            }

            if (!changesLevel && this.vehicle != null) {
                this.dismountForTeleport();
                if (this.closed || this.vehicle != null) {
                    return false;
                }
            }

            this.level = destination;
            this.position = location.getPosition();
            this.motion = Vector3f.ZERO;
            this.movementSegments.clear();
            this.resetFallDistance();
            this.updateCollisionContact(false, false, null);

            if (changesLevel && this.spawned) {
                destination.registerEntity(this);
                this.scheduleUpdate();
            }

            this.setRotation(location.getYaw(), location.getPitch());
            this.recalculateBoundingBox();

            if (changesLevel) {
                CloudChunk destinationChunk = this.spawned ? destination.getLoadedChunk(this.position) : null;
                this.chunk = destinationChunk;
                if (destinationChunk != null) {
                    destinationChunk.registerEntity(this);
                }

                this.onLevelChanged(previousLevel, destination);
                if (!this.closed && destinationChunk != null && this.chunk == destinationChunk) {
                    this.publishChunkChange(destinationChunk);
                }
            } else {
                this.checkChunks();
            }

            if (!this.closed) {
                this.publishAuthoritativeDisplacement();
            }

            return true;
        } finally {
            this.teleportDestinationLevel = null;
        }
    }

    private boolean detachForLevelTransfer(CloudLevel previousLevel, CloudLevel targetLevel) {
        EntityLevelChangeEvent event = new EntityLevelChangeEvent(this, previousLevel, targetLevel);
        this.server.getEventManager().fire(event);
        if (event.isCancelled() || this.closed) {
            return false;
        }

        this.dismountForTeleport();
        if (this.closed || this.vehicle != null) {
            return false;
        }

        this.ejectPassengers();
        if (this.closed || !this.passengers.isEmpty()) {
            return false;
        }

        this.despawnFromAll();
        if (this.closed) {
            return false;
        }

        if (this.spawned) {
            previousLevel.unregisterEntity(this);
        }

        if (this.chunk != null) {
            this.chunk.unregisterEntity(this);
        }

        this.chunk = null;
        return true;
    }

    private void dismountForTeleport() {
        if (this.vehicle != null) {
            this.dismount(this.vehicle, false);
        }
    }

    private boolean isLeavingLevel() {
        return this.teleportDestinationLevel != null && this.teleportDestinationLevel != this.level;
    }

    protected void onLevelChanged(CloudLevel previousLevel, CloudLevel targetLevel) {
    }

    @Override
    public int getPortalCooldown() {
        return this.portalCooldownTicks;
    }

    @Override
    public void setPortalCooldown(int cooldownTicks) {
        checkArgument(cooldownTicks >= 0, "Portal cooldown cannot be negative");
        this.portalCooldownTicks = cooldownTicks;
    }

    public boolean isOnPortalCooldown() {
        return this.portalCooldownTicks > 0;
    }

    public void resetPortalCooldown() {
        this.setPortalCooldown(this.getPortalCooldownDuration());
    }

    protected int getPortalCooldownDuration() {
        return PORTAL_COOLDOWN_TICKS;
    }

    protected void tickPortalCooldown() {
        this.portalCooldownTicks--;
    }

    protected int getPortalTransitionTicks() {
        return PORTAL_TRANSFER_TICKS;
    }

    private void tickPortalTransition() {
        if (this.isOnPortalCooldown()) {
            this.tickPortalCooldown();
        } else {
            int portalThreshold = getPortalTransitionTicks();
            if (this.inPortalTicks > 0 && (portalThreshold == 0 || this.inPortalTicks >= portalThreshold)) {
                this.resetPortalCooldown();
                this.inPortalTicks = 0;

                Location newLoc = EnumLevel.moveToNether(
                        this.getX(),
                        this.getY(),
                        this.getZ(),
                        this.getYaw(),
                        this.getPitch(),
                        this.getLevel()
                );

                if (newLoc != null) {
                    NetherPortals.handlePortalTransfer(this, newLoc);
                }
            }
        }
    }

    protected void onInsidePortal() {
        if (this.getVehicle() == null) {
            if (this.isOnPortalCooldown()) {
                this.resetPortalCooldown();
            } else {
                this.inPortalTicks = PORTAL_TRANSFER_TICKS;
            }
        }
    }

    public boolean enterNetherPortal(Vector3i position) {
        EntityPortalEnterEvent event = new EntityPortalEnterEvent(this, Location.from(position, this.level), PortalType.NETHER);
        this.server.getEventManager().fire(event);
        return !event.isCancelled();
    }

    public void enterEndPortal(Vector3i position) {
        if (!this.isAlive() || this.getVehicle() != null || this.isOnPortalCooldown()) {
            return;
        }

        if (this instanceof CloudPlayer player && player.isShowingEndCredits()) {
            return;
        }

        EntityPortalEnterEvent event = new EntityPortalEnterEvent(this, Location.from(position, this.level), PortalType.END);
        this.server.getEventManager().fire(event);
        if (event.isCancelled()) {
            return;
        }

        this.resetPortalCooldown();
        if (!EndPortals.transfer(this)) {
            this.setPortalCooldown(0);
        }
    }

    public void enterEndGateway(Vector3i position) {
        if (!this.isAlive() || this.getVehicle() != null || this.isOnPortalCooldown()) {
            return;
        }

        EntityPortalEnterEvent event = new EntityPortalEnterEvent(this, Location.from(position, this.level), PortalType.END_GATEWAY);
        this.server.getEventManager().fire(event);
        if (event.isCancelled()) {
            return;
        }

        this.resetPortalCooldown();
        if (!EndGateways.transfer(this, position)) {
            this.setPortalCooldown(0);
        }
    }

    protected @Nullable Entity getFirstPassenger() {
        return this.passengers.isEmpty() ? null : this.passengers.getFirst();
    }

    @Override
    public List<Entity> getPassengers() {
        return this.passengers;
    }

    @Override
    public boolean hasPassenger(Entity passenger) {
        return this.passengers.contains(passenger);
    }

    @Override
    public boolean isControlling(Entity entity) {
        return entity != null && this.getControllingPassenger() == entity;
    }

    @Override
    public @Nullable Entity getControllingPassenger() {
        return this.getFirstPassenger();
    }

    public final void setSeatOffset(Vector3f offset) {
        this.data.set(SEAT_OFFSET, offset);
    }

    public Vector3f getMountedOffset(Entity passenger) {
        float yOffset = this.getMountedHeightOffset() + passengerHeightOffset(passenger);
        return Vector3f.from(0f, yOffset, 0f);
    }

    protected float getMountedHeightOffset() {
        return this.getHeight() * 0.75f;
    }

    protected float getPassengerHeightOffset() {
        return 0f;
    }

    protected static float passengerHeightOffset(Entity passenger) {
        return passenger instanceof CloudEntity rider ? rider.getPassengerHeightOffset() : 0f;
    }

    protected Vector3f getPassengerAttachmentPoint(Entity passenger) {
        return Vector3f.from(0f, this.getHeight(), 0f);
    }

    @Override
    public boolean mount(Entity vehicle) {
        if (this.mounting || this.dismounting || !(vehicle instanceof CloudEntity target) || this.vehicle == target || this.cannotBoard(target)) {
            return false;
        }

        Entity previous = this.vehicle;
        this.mounting = true;
        try {
            if (target instanceof Vehicle apiVehicle) {
                VehicleEnterEvent event = new VehicleEnterEvent(apiVehicle, this);
                this.server.getEventManager().fire(event);
                if (event.isCancelled()) {
                    return false;
                }
            }

            if (this.vehicle != previous || this.cannotBoard(target)) {
                return false;
            }

            if (previous != null && !this.dismount(previous)) {
                return false;
            }

            if (this.vehicle != null || this.cannotBoard(target)) {
                return false;
            }

            Entity previousFirstPassenger = target.getFirstPassenger();
            this.vehicle = target;
            target.passengers = withPassenger(target.passengers, this);
            this.setOnGround(false);
            this.resetFallDistance();
            this.data.setFlag(RIDING, true);

            target.onMount(this);
            if (this.vehicle != target) {
                return false;
            }

            this.flushEntityData();
            boolean firstPassenger = target.getFirstPassenger() == this;
            if (firstPassenger && previousFirstPassenger instanceof CloudEntity displaced && displaced.vehicle == target) {
                displaced.flushEntityData();
                displaced.broadcastLinkPacket(target, EntityLinkData.Type.PASSENGER, false);
            }

            EntityLinkData.Type link = firstPassenger ? EntityLinkData.Type.RIDER : EntityLinkData.Type.PASSENGER;
            this.broadcastLinkPacket(target, link, this instanceof Player);
            this.onMountComplete(target);
            return true;
        } finally {
            this.mounting = false;
        }
    }

    private static List<Entity> withPassenger(List<Entity> passengers, Entity passenger) {
        if (passengers.isEmpty()) {
            return List.of(passenger);
        }

        List<Entity> updated = new ArrayList<>(passengers.size() + 1);
        if (passenger instanceof Player && !(passengers.getFirst() instanceof Player)) {
            updated.add(passenger);
            updated.addAll(passengers);
        } else {
            updated.addAll(passengers);
            updated.add(passenger);
        }
        return List.copyOf(updated);
    }

    private boolean cannotBoard(CloudEntity target) {
        return !this.isAlive() || !target.isAlive() || this.level != target.level
                || this.isLeavingLevel() || target.isLeavingLevel()
                || this.wouldCreateVehicleCycle(target)
                || !target.canAddPassenger(this);
    }

    public boolean tryMount(Entity vehicle) {
        return !this.data.getFlag(SNEAKING) && this.boardingCooldown == 0 && this.mount(vehicle);
    }

    protected boolean canAddPassenger(Entity passenger) {
        return !this.passengers.contains(passenger);
    }

    private boolean wouldCreateVehicleCycle(Entity vehicle) {
        for (Entity ancestor = vehicle; ancestor != null; ancestor = ancestor.getVehicle()) {
            if (ancestor == this) {
                return true;
            }
        }

        return false;
    }

    protected void onMount(Entity passenger) {
        if (passenger instanceof CloudPlayer ridingPlayer) {
            this.spawnTo(ridingPlayer);
        }

        Vector3f seatOffset = this.getMountedOffset(passenger);
        CloudEntity rider = (CloudEntity) passenger;
        rider.setSeatOffset(seatOffset);
        rider.data.set(SEAT_LOCK_RIDER_ROTATION_DEGREES, 181.0f);
        this.updatePassengerPosition(passenger);
    }

    protected void onMountComplete(Entity vehicle) {
    }

    public boolean dismount(Entity vehicle) {
        return this.dismount(vehicle, true);
    }

    public boolean dismount(Entity vehicle, boolean cancellable) {
        return this.dismount(vehicle, cancellable, null);
    }

    public boolean dismount(Entity vehicle, boolean cancellable, @Nullable Vector3f preferredPosition) {
        if (this.vehicle != vehicle || vehicle == null) {
            return false;
        }

        if (this.dismounting) {
            if (cancellable) {
                return false;
            }

            this.detachFromVehicle(vehicle, false, null);
            return true;
        }

        this.dismounting = true;

        try {
            if (vehicle instanceof Vehicle target) {
                VehicleExitEvent event = new VehicleExitEvent(target, this, cancellable);
                this.server.getEventManager().fire(event);
                if (event.isCancelled() && this.vehicle == vehicle) {
                    return false;
                }
            }

            if (this.vehicle != vehicle) {
                return this.vehicle == null;
            }

            this.detachFromVehicle(vehicle, cancellable && this instanceof Player, preferredPosition);
            return true;
        } finally {
            this.dismounting = false;
        }
    }

    private void detachFromVehicle(Entity vehicle, boolean riderInitiated, @Nullable Vector3f preferredPosition) {
        CloudEntity target = (CloudEntity) vehicle;
        boolean wasControlling = target.isControlling(this);
        this.vehicle = null;

        List<Entity> remaining = new ArrayList<>(target.passengers);
        remaining.remove(this);
        target.passengers = List.copyOf(remaining);
        this.boardingCooldown = 60;
        this.data.setFlag(RIDING, false);
        this.setSeatOffset(Vector3f.ZERO);
        this.data.set(SEAT_LOCK_RIDER_ROTATION_DEGREES, 0.0f);

        this.broadcastLinkPacket(target, EntityLinkData.Type.REMOVE, riderInitiated);
        target.onDismount(this, preferredPosition, wasControlling);
        target.refreshPassengerLinks();
        this.data.setFlag(MOVING, true);
        this.flushEntityData();
    }

    protected void ejectPassengers() {
        for (Entity passenger : this.getPassengers()) {
            ((CloudEntity) passenger).dismount(this, false);
        }
    }

    protected void onDismount(Entity passenger, @Nullable Vector3f preferredPosition, boolean wasControlling) {
    }

    private void refreshPassengerLinks() {
        List<Entity> passengers = this.passengers;
        for (int index = 0; index < passengers.size(); index++) {
            ((CloudEntity) passengers.get(index)).broadcastLinkPacket(this,
                    index == 0 ? EntityLinkData.Type.RIDER : EntityLinkData.Type.PASSENGER, false);
        }
    }

    public void updatePassengers() {
        for (Entity passenger : this.passengers) {
            if (passenger.getVehicle() != this) {
                continue;
            }

            if (!passenger.isAlive()) {
                ((CloudEntity) passenger).dismount(this, false);
                continue;
            }

            this.updatePassengerPosition(passenger);
        }
    }

    protected void updatePassengerPosition(Entity passenger) {
        passenger.setPosition(this.getPosition().add(this.getPassengerAttachmentPoint(passenger)));
    }

    protected void broadcastLinkPacket(Entity vehicle, EntityLinkData.Type type, boolean riderInitiated) {
        SetEntityLinkPacket packet = new SetEntityLinkPacket();
        boolean immediate = type == EntityLinkData.Type.REMOVE && (this.closed || !this.isAlive() || vehicle.isClosed() || !vehicle.isAlive());
        packet.setEntityLink(new EntityLinkData(vehicle.getUniqueId(), this.getUniqueId(), type, immediate, riderInitiated, 0));
        CloudServer.broadcastPacket(((CloudEntity) vehicle).getViewers(), packet);
    }

    public void setHealth(float health) {
        if (!Float.isFinite(health) || health < 0) {
            throw new IllegalArgumentException("Health must be finite and non-negative");
        }

        if (this.health == health) {
            return;
        }

        if (health == 0) {
            if (this.isAlive()) {
                this.kill();
            }
        } else if (health <= this.getMaxHealth() || health < this.health) {
            this.health = health;
        } else {
            this.health = this.getMaxHealth();
        }

        this.data.set(STRUCTURAL_INTEGRITY, (int) this.health);
    }

    public int getMaxHealth() {
        Integer maxHealthOverride = this.persistence.state().getBaseMaxHealthOverride();
        int baseMaxHealth = maxHealthOverride != null ? maxHealthOverride : this.defaultMaxHealth;
        ActivePotionEffect healthBoost = this.effects.get(EffectTypes.HEALTH_BOOST);
        long bonus = healthBoost == null ? 0 : 4L * (healthBoost.getAmplifier() + 1L);
        return (int) Math.min(Integer.MAX_VALUE, baseMaxHealth + bonus);
    }

    public void setMaxHealth(int baseMaxHealth) {
        checkArgument(baseMaxHealth > 0, "Maximum health must be positive");
        this.persistence.state().setBaseMaxHealthOverride(baseMaxHealth);
        this.clampHealthToMax();
        this.markChunkDirty();
    }

    protected void setDefaultMaxHealth(int defaultMaxHealth) {
        checkArgument(defaultMaxHealth > 0, "Maximum health must be positive");
        this.defaultMaxHealth = defaultMaxHealth;
        this.clampHealthToMax();
    }

    private void clampHealthToMax() {
        int maxHealth = this.getMaxHealth();
        if (this.health > maxHealth) {
            this.setHealth(maxHealth);
        }
    }

    @Override
    public boolean isUndead() {
        return CloudEntityRegistry.get().requireComponent(this.getType(), EntityComponents.IS_UNDEAD).execute(this);
    }

    public void setAbsorption(float absorption) {
        if (!Float.isFinite(absorption) || absorption < 0) {
            throw new IllegalArgumentException("Absorption must be finite and non-negative");
        }

        this.absorption = absorption;
    }

    @Override
    public int getNoDamageTicks() {
        return noDamageTicks;
    }

    public void setNoDamageTicks(int noDamageTicks) {
        if (noDamageTicks < 0) {
            throw new IllegalArgumentException("Damage immunity ticks must be non-negative");
        }

        this.noDamageTicks = noDamageTicks;
    }

    @Override
    public EntityDamageEvent getLastDamageCause() {
        return lastDamageCause;
    }

    public static boolean applyDamage(Entity target, float amount, DamageSource source) {
        if (target instanceof CloudEntity cloudEntity) {
            return cloudEntity.damage(amount, source);
        }

        return target instanceof Damageable damageable && damageable.damage(amount, source);
    }

    public boolean damage(float amount, DamageSource source) {
        return this.damageWithResult(amount, source).accepted();
    }

    /**
     * Applies one hit and retains its cancellation outcome for dependent effects.
     */
    public CloudDamageResult damageWithResult(float amount, DamageSource source) {
        CloudEntityDamageEvent event = this.createDamageEvent(source, amount);
        boolean applied = this.applyDamage(event);
        if (event.isCancelled()) {
            return CloudDamageResult.CANCELLED;
        }
        return applied ? CloudDamageResult.APPLIED : CloudDamageResult.REJECTED;
    }

    /**
     * Captures the damage calculation exposed to listeners for this hit.
     *
     * @param source the damage source
     * @param amount the incoming damage
     * @return the damage event
     */
    protected CloudEntityDamageEvent createDamageEvent(DamageSource source, float amount) {
        return new CloudEntityDamageEvent(this, source, amount, this.getAbsorption());
    }

    /**
     * Applies immunity checks shared by health damage and special damage responses.
     */
    protected boolean isDamageImmune(DamageSource source) {
        return this.isClosed() || !this.isAlive()
                || ((this.isInvulnerable() || this.noDamageTicks > 0)
                && !source.getDamageType().is(DamageTypeTags.BYPASSES_INVULNERABILITY))
                || (this.hasPotionEffect(EffectTypes.FIRE_RESISTANCE)
                && source.getDamageType().is(DamageTypeTags.IS_FIRE));
    }

    /**
     * Applies a damage event after entity-specific preprocessing.
     *
     * @param event the damage event
     * @return whether damage was applied
     */
    protected boolean applyDamage(CloudEntityDamageEvent event) {
        if (this.isDamageImmune(event.getDamageSource())) {
            return false;
        }

        this.getServer().getEventManager().fire(event);
        if (this.rejectDamage(event)) {
            return false;
        }

        if (!this.prepareDamage(event) || this.rejectDamage(event)) {
            return false;
        }

        this.onDamageAccepted(event);
        if (this.isClosed() || !this.isAlive()) {
            return false;
        }

        this.commitDamage(event);
        return true;
    }

    private boolean rejectDamage(CloudEntityDamageEvent event) {
        return event.isCancelled() || event.getDamage() <= 0 || this.isClosed() || !this.isAlive();
    }

    private void commitDamage(CloudEntityDamageEvent event) {
        float absorption = this.getAbsorption();
        CloudEntityDamageEvent.DamageSplit damage = event.resolveDamage(absorption);
        if (damage.healthDamage() == 0 && damage.absorbedDamage() == 0) {
            return;
        }

        this.setLastDamageCause(event);
        if (damage.absorbedDamage() > 0) {
            this.setAbsorption(absorption - damage.absorbedDamage());
        }

        if (damage.healthDamage() == 0) {
            return;
        }

        float remainingHealth = this.getHealth() - damage.healthDamage();
        if (remainingHealth > 0) {
            this.setHealth(remainingHealth);
        } else if (!this.tryPreventDeath(event.getDamageSource()) && !this.isClosed() && this.isAlive()) {
            this.setHealth(0);
        }

        this.onHealthDamageApplied(event.getDamageSource());
    }

    /**
     * Checks entity-specific events before accepted-hit effects or health changes.
     */
    protected boolean prepareDamage(CloudEntityDamageEvent event) {
        return true;
    }

    /**
     * Applies accepted-hit effects before health changes can trigger death.
     */
    protected void onDamageAccepted(CloudEntityDamageEvent event) {
    }

    /**
     * Handles a resolved hit that dealt damage beyond absorption.
     */
    protected void onHealthDamageApplied(DamageSource source) {
    }

    protected boolean tryPreventDeath(DamageSource source) {
        return false;
    }

    public void heal(float amount, RegainReason reason) {
        EntityRegainHealthEvent source = new EntityRegainHealthEvent(this, amount, reason);
        if (this.isClosed() || !this.isAlive()) {
            return;
        }

        this.server.getEventManager().fire(source);
        if (!source.isCancelled() && !this.isClosed() && this.isAlive()) {
            this.setHealth((float) Math.min(this.getMaxHealth(), (double) this.getHealth() + source.getAmount()));
        }
    }

    public void kill() {
        this.enterDeathState();
    }

    protected final void enterDeathState() {
        this.health = 0;
        this.data.set(STRUCTURAL_INTEGRITY, 0);
        this.scheduleUpdate();

        this.ejectPassengers();
    }

    public Map<EffectType, PotionEffect> getActivePotionEffects() {
        Map<EffectType, PotionEffect> snapshots = new LinkedHashMap<>(this.effects.size());
        this.effects.forEach((type, effect) -> snapshots.put(type, effect.snapshot()));
        return Map.copyOf(snapshots);
    }

    @Nullable
    public PotionEffect getPotionEffect(EffectType type) {
        ActivePotionEffect effect = this.effects.get(checkNotNull(type, "type"));
        return effect == null ? null : effect.snapshot();
    }

    protected int getPotionAmplifier(EffectType type) {
        ActivePotionEffect effect = this.effects.get(type);
        return effect == null ? -1 : effect.getAmplifier();
    }

    public boolean hasPotionEffect(EffectType type) {
        return this.effects.containsKey(checkNotNull(type, "type"));
    }

    public boolean addPotionEffect(PotionEffect effect) {
        return this.addPotionEffect(effect, null, PotionEffectCause.PLUGIN);
    }

    public boolean addPotionEffect(PotionEffect effect, @Nullable Entity source, PotionEffectCause cause) {
        checkNotNull(effect, "effect");
        checkNotNull(cause, "cause");
        checkState(this instanceof LivingEntity, "Potion effects can only be applied to living entities");

        if (effect.getType() == EffectTypes.FREEZING && !CloudEntityRegistry.get()
                .requireComponent(this.getType(), EntityComponents.CAN_FREEZE).execute(this)) {
            return false;
        }

        ActivePotionEffect oldEffect = this.effects.get(effect.getType());
        PotionEffect oldEffectSnapshot = oldEffect == null ? null : oldEffect.snapshot();
        PotionEffectAction action = oldEffect == null ? PotionEffectAction.ADDED : PotionEffectAction.CHANGED;
        boolean override = oldEffectSnapshot == null || shouldOverrideEffect(oldEffectSnapshot, effect);

        EntityPotionEffectEvent event = new EntityPotionEffectEvent((LivingEntity) this, oldEffectSnapshot, effect, source, cause, action, override);
        this.server.getEventManager().fire(event);
        if (event.isCancelled() || action == PotionEffectAction.CHANGED && !event.isOverride()) {
            return false;
        }

        ActivePotionEffect newEffect = ActivePotionEffect.from(effect);
        newEffect.onApplied(this, oldEffect);
        this.effects.put(newEffect.getType(), newEffect);
        this.onPotionEffectChanged(newEffect.getType());

        this.recalculateEffectColor();

        if (newEffect.getType() == EffectTypes.HEALTH_BOOST) {
            this.setHealth(Math.min(this.getHealth(), this.getMaxHealth()));
        }

        return true;
    }

    private static boolean shouldOverrideEffect(PotionEffect oldEffect, PotionEffect newEffect) {
        return newEffect.getAmplifier() > oldEffect.getAmplifier()
                || (newEffect.getAmplifier() == oldEffect.getAmplifier()
                    && oldEffect.isShorterThan(newEffect))
                || oldEffect.isAmbient() && !newEffect.isAmbient()
                || oldEffect.hasParticles() != newEffect.hasParticles();
    }

    public boolean removePotionEffect(EffectType type) {
        return this.removePotionEffect(type, PotionEffectCause.PLUGIN);
    }

    public boolean removePotionEffect(EffectType type, PotionEffectCause cause) {
        checkState(this instanceof LivingEntity, "Potion effects can only be removed from living entities");
        checkNotNull(cause, "cause");
        ActivePotionEffect effect = this.effects.get(checkNotNull(type, "type"));
        if (effect == null) {
            return false;
        }

        PotionEffect oldEffect = effect.snapshot();
        EntityPotionEffectEvent event = new EntityPotionEffectEvent((LivingEntity) this, oldEffect, null, null, cause, PotionEffectAction.REMOVED, false);
        this.server.getEventManager().fire(event);
        if (event.isCancelled()) {
            return false;
        }

        this.effects.remove(type);
        effect.onRemoved(this);
        this.onPotionEffectChanged(type);
        if (type == EffectTypes.HEALTH_BOOST) {
            this.setHealth(Math.min(this.getHealth(), this.getMaxHealth()));
        }

        this.recalculateEffectColor();
        return true;
    }

    public boolean clearActivePotionEffects() {
        return this.clearActivePotionEffects(PotionEffectCause.PLUGIN);
    }

    public boolean clearActivePotionEffects(PotionEffectCause cause) {
        checkNotNull(cause, "cause");
        boolean changed = false;
        for (ActivePotionEffect effect : List.copyOf(this.effects.values())) {
            changed |= this.removePotionEffect(effect.getType(), cause);
        }
        return changed;
    }

    protected void onPotionEffectChanged(EffectType type) {
    }

    private void restorePotionEffect(PotionEffect effect) {
        ActivePotionEffect activeEffect = ActivePotionEffect.from(effect);
        activeEffect.onApplied(this, this.effects.get(effect.getType()));
        this.effects.put(effect.getType(), activeEffect);
        this.recalculateEffectColor();
    }

    protected void recalculateEffectColor() {
        int[] color = new int[3];
        int count = 0;
        for (ActivePotionEffect effect : this.effects.values()) {
            if (effect.hasParticles()) {
                Vector3i c = effect.getType().getColor();
                color[0] += c.getX() * (effect.getAmplifier() + 1);
                color[1] += c.getY() * (effect.getAmplifier() + 1);
                color[2] += c.getZ() * (effect.getAmplifier() + 1);
                count += effect.getAmplifier() + 1;
            }
        }

        if (count > 0) {
            int r = (color[0] / count) & 0xff;
            int g = (color[1] / count) & 0xff;
            int b = (color[2] / count) & 0xff;

            this.data.set(EFFECT_COLOR, (r << 16) + (g << 8) + b);
        } else {
            this.data.set(EFFECT_COLOR, 0);
        }
    }

    public void sendPotionEffects(CloudPlayer player) {
        for (ActivePotionEffect effect : this.effects.values()) {
            player.sendPacket(NetworkUtils.effectToNetwork(effect.snapshot(), this.getRuntimeId(), MobEffectPacket.Event.ADD, this.server.getTick()));
        }
    }

    @Override
    public ItemStack getPickItem(boolean includeData) {
        PickItemEntityHandler handler = CloudEntityRegistry.get().getComponent(this.type, EntityComponents.GET_PICK_ITEM);
        ItemStack item = handler == null ? ItemStack.EMPTY : Objects.requireNonNull(handler.execute(this, includeData), "picked item");

        if (!includeData || item.isEmpty() || item.has(ItemDataComponents.ENTITY_DATA)) {
            return item;
        }

        return this.createSnapshot()
                .map(snapshot -> item.toBuilder().setData(ItemDataComponents.ENTITY_DATA, snapshot).build())
                .orElse(item);
    }

    public final void handleInteraction(Player player, ItemStack item, Vector3f clickedPos) {
        InteractEntityHandler handler = CloudEntityRegistry.get().getComponent(this.type, EntityComponents.ON_INTERACT);
        if (handler == null || !handler.execute(this, player, item, clickedPos)) {
            this.interactDefault(player, item, clickedPos);
        }
    }

    protected boolean interactDefault(Player player, ItemStack item, Vector3f clickedPos) {
        return false;
    }

    protected void checkChunks() {
        if (!this.spawned || this.closed) {
            return;
        }

        Vector3f position = this.getPosition();
        CloudChunk previous = this.chunk;
        int chunkX = position.getFloorX() >> 4;
        int chunkZ = position.getFloorZ() >> 4;
        if (previous != null && previous.getX() == chunkX && previous.getZ() == chunkZ) {
            return;
        }

        CloudChunk destination = this.level.getLoadedChunk(position);
        if (previous != null) {
            previous.unregisterEntity(this);
        }

        this.chunk = destination;
        if (destination == null) {
            this.despawnFromAll();
            return;
        }

        destination.registerEntity(this);
        this.publishChunkChange(destination);
    }

    private void publishChunkChange(CloudChunk destination) {
        if (this.hasTicked) {
            this.updateViewers(destination.getViewers());
        }
        if (!this.closed && this.chunk == destination) {
            this.onChunkChanged(destination);
        }
    }

    protected void onChunkChanged(CloudChunk destination) {
    }

    public void spawnTo(CloudPlayer player) {
        if (this.canSpawnTo(player) && this.beginTracking(player)) {
            this.sendPairingData(player);
        }
    }

    private void sendPairingData(CloudPlayer player) {
        boolean added = false;
        try {
            added = this.sendTrackedAddPacket(player);
            if (!added) {
                return;
            }
        } finally {
            if (!added) {
                this.stopTracking(player);
            }
        }

        if (this.vehicle instanceof CloudEntity mount) {
            mount.spawnTo(player);
            if (!this.hasViewer(player) || this.vehicle != mount || (mount != player && !mount.hasViewer(player))) {
                return;
            }

            int seatIndex = mount.getPassengers().indexOf(this);
            if (seatIndex < 0) {
                return;
            }

            EntityLinkData.Type link = seatIndex == 0 ? EntityLinkData.Type.RIDER : EntityLinkData.Type.PASSENGER;
            SetEntityLinkPacket packet = new SetEntityLinkPacket();
            packet.setEntityLink(new EntityLinkData(mount.getUniqueId(), this.getUniqueId(), link, true, false, 0));
            player.sendPacket(packet);
        }
    }

    protected final boolean canSpawnTo(CloudPlayer player) {
        CloudChunk chunk = this.chunk;
        return this.spawned && !this.closed && chunk != null
                && player.getLevel() == this.level && player.isChunkSent(chunk.getX(), chunk.getZ());
    }

    public void refreshViewers() {
        CloudChunk currentChunk = this.chunk;
        if (!this.spawned || currentChunk == null || this.closed) {
            return;
        }

        for (Player player : this.level.getChunkPlayers(currentChunk.getX(), currentChunk.getZ())) {
            if (this.closed || this.chunk != currentChunk) {
                break;
            }

            if (player instanceof CloudPlayer viewer && viewer.isOnline()) {
                this.spawnTo(viewer);
            }
        }
    }

    protected BedrockPacket createAddEntityPacket() {
        Vector3f pos = this.getPosition();
        AddEntityPacket addEntity = new AddEntityPacket();
        addEntity.setIdentifier(this.getType().getId().toString());
        addEntity.setUniqueEntityId(this.getUniqueId());
        addEntity.setRuntimeEntityId(this.getRuntimeId());
        addEntity.setPosition(Vector3f.from(pos.getX(), pos.getY() + this.getBaseOffset(), pos.getZ()));
        addEntity.setRotation(Vector2f.from(this.pitch, this.yaw));
        addEntity.setHeadRotation(this.yaw);
        addEntity.setMotion(this.getMotion());
        addEntity.setBodyRotation(this.getYaw());
        addEntity.getMetadata().putAll(this.data.snapshot());

        List<Entity> passengers = this.passengers;
        for (int i = 0; i < passengers.size(); i++) {
            EntityLinkData.Type link = i == 0 ? EntityLinkData.Type.RIDER : EntityLinkData.Type.PASSENGER;
            addEntity.getEntityLinks().add(new EntityLinkData(
                    this.getUniqueId(), passengers.get(i).getUniqueId(), link, false, false, 0));
        }

        this.addAdditionalSpawnData(addEntity);
        return addEntity;
    }

    protected void addAdditionalSpawnData(AddEntityPacket packet) {
    }

    public void despawnFrom(CloudPlayer player) {
        if (this.stopTracking(player)) {
            this.sendRemoval(player);
        }
    }

    private void sendRemoval(CloudPlayer player) {
        RemoveEntityPacket packet = new RemoveEntityPacket();
        packet.setUniqueEntityId(this.getUniqueId());
        player.sendPacket(packet);
    }

    protected void despawnFromAll() {
        for (CloudPlayer player : this.viewers) {
            this.despawnFrom(player);
        }
    }

    public Set<CloudPlayer> getViewers() {
        return Set.copyOf(this.viewers);
    }

    protected final boolean hasViewer(CloudPlayer player) {
        return this.viewers.contains(player);
    }

    protected final boolean beginTracking(CloudPlayer player) {
        return this.viewers.add(player);
    }

    protected final boolean stopTracking(CloudPlayer player) {
        return this.viewers.remove(player);
    }

    protected final boolean sendTrackedAddPacket(CloudPlayer player) {
        if (!this.hasViewer(player) || !player.sendPacket(this.createAddEntityPacket())) {
            return false;
        }

        if (!this.hasViewer(player)) {
            this.sendRemoval(player);
            return false;
        }

        return true;
    }

    protected final void clearViewers() {
        this.viewers.clear();
    }

    protected final void broadcastToViewers(BedrockPacket packet) {
        if (!this.viewers.isEmpty()) {
            CloudServer.broadcastPacket(this.viewers, packet);
        }
    }

    protected final void updateViewers(Set<CloudPlayer> viewers) {
        CloudChunk currentChunk = this.chunk;
        for (CloudPlayer viewer : this.viewers) {
            if (this.closed || this.chunk != currentChunk) {
                return;
            }

            if (!viewers.contains(viewer)) {
                this.despawnFrom(viewer);
            }
        }

        for (CloudPlayer viewer : viewers) {
            if (this.closed || this.chunk != currentChunk) {
                return;
            }

            this.spawnTo(viewer);
        }
    }

    private void updateNetworkBounds() {
        this.data.set(WIDTH, this.getWidth());
        this.data.set(HEIGHT, this.getHeight());

        if (this.isPlayer) {
            this.data.set(COLLISION_BOX, this.getNetworkCollisionBox());
        }
    }

    protected void putNetworkBounds(EntityDataMap metadata) {
        metadata.put(WIDTH, this.getWidth());
        metadata.put(HEIGHT, this.getHeight());

        if (this.isPlayer) {
            metadata.put(COLLISION_BOX, this.getNetworkCollisionBox());
        }
    }

    private Vector3f getNetworkCollisionBox() {
        return Vector3f.from(this.getWidth(), this.getHeight(), this.getLength());
    }

    /**
     * Sends pending metadata to viewers and, for a player, to the player itself.
     */
    public void flushEntityData() {
        EntityDataMap changeSet = this.data.drainChanges();
        if (changeSet.isEmpty()) {
            return;
        }

        EntityDataMap metadata = this.withPlayerPoseMetadata(changeSet);
        this.sendDataToViewers(metadata);

        if (this.isPlayer) {
            SetEntityDataPacket packet = new SetEntityDataPacket();
            packet.setRuntimeEntityId(this.getRuntimeId());
            packet.getMetadata().putAll(metadata);
            ((CloudPlayer) this).sendPacket(packet);
        }
    }

    private EntityDataMap withPlayerPoseMetadata(EntityDataMap changeSet) {
        if (!this.isPlayer || !changeSet.containsKey(FLAGS) || this.hasNetworkBounds(changeSet)) {
            return changeSet;
        }

        EntityDataMap metadata = new EntityDataMap();
        metadata.putAll(changeSet);
        this.putNetworkBounds(metadata);
        return metadata;
    }

    private boolean hasNetworkBounds(EntityDataMap metadata) {
        return metadata.containsKey(HEIGHT) && metadata.containsKey(WIDTH) && metadata.containsKey(COLLISION_BOX);
    }

    public void sendData(CloudPlayer player) {
        SetEntityDataPacket packet = new SetEntityDataPacket();
        packet.setRuntimeEntityId(this.getRuntimeId());
        packet.getMetadata().putAll(this.data.snapshot());
        player.sendPacket(packet);
    }

    public void sendFlags(CloudPlayer player) {
        SetEntityDataPacket packet = new SetEntityDataPacket();
        packet.setRuntimeEntityId(this.getRuntimeId());
        packet.getMetadata().putAll(this.data.snapshot(FLAGS));
        if (this.isPlayer) {
            this.putNetworkBounds(packet.getMetadata());
        }

        player.sendPacket(packet);
    }

    protected void sendDataToViewers(EntityDataMap map) {
        SetEntityDataPacket packet = new SetEntityDataPacket();
        packet.setRuntimeEntityId(this.getRuntimeId());
        packet.getMetadata().putAll(map);

        this.broadcastToViewers(packet);
    }

    @Override
    public String toString() {
        return "Entity(type=" + type.getId() + ", id=" + getUniqueId() + ")";
    }

    private record LiquidContact(boolean touchingWater, Vector3f flow) {
    }
}
