package org.cloudburstmc.server.entity;

import co.aikar.timings.Timing;
import co.aikar.timings.Timings;
import co.aikar.timings.TimingsHistory;
import com.google.common.collect.Iterables;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.entity.*;
import org.cloudburstmc.api.entity.component.Buoyancy;
import org.cloudburstmc.api.entity.component.PickItemEntityHandler;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.damage.DamageTypeTags;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.entity.misc.DroppedItem;
import org.cloudburstmc.api.entity.misc.LightningBolt;
import org.cloudburstmc.api.entity.vehicle.Vehicle;
import org.cloudburstmc.api.event.entity.*;
import org.cloudburstmc.api.event.player.PlayerTeleportCause;
import org.cloudburstmc.api.event.vehicle.VehicleEnterEvent;
import org.cloudburstmc.api.event.vehicle.VehicleExitEvent;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.entity.data.EntityData;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.nbt.NbtMapper;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.api.level.sound.SoundTypes;
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
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataMap;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataType;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityLinkData;
import org.cloudburstmc.protocol.bedrock.packet.*;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.entity.data.BuoyancyDataSerializer;
import org.cloudburstmc.server.entity.data.SyncedEntityData;
import org.cloudburstmc.server.event.entity.CloudEntityDamageEvent;
import org.cloudburstmc.server.level.*;
import org.cloudburstmc.server.level.biome.CloudBiome;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.level.collision.CloudVoxelShapes;
import org.cloudburstmc.server.math.MathHelper;
import org.cloudburstmc.server.network.NetworkUtils;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.potion.ActivePotionEffect;
import org.cloudburstmc.server.nbt.ServerNbt;
import org.cloudburstmc.server.registry.CloudBiomeRegistry;
import org.cloudburstmc.server.registry.CloudEntityRegistry;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

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

    private static final ConcurrentHashMap<DataClassKey, Class<? extends EntityData>> EFFECTIVE_DATA_CLASSES =
            new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, java.lang.reflect.Constructor<?>> DATA_CONSTRUCTORS =
            new ConcurrentHashMap<>();

    private record DataClassKey(Class<?> runtimeClass, EntityType<?> type) {
    }

    protected final Set<CloudPlayer> hasSpawned = ConcurrentHashMap.newKeySet();

    protected final Reference2ObjectOpenHashMap<EffectType, ActivePotionEffect> effects = new Reference2ObjectOpenHashMap<>();
    protected final List<Entity> passengers = new ArrayList<>();
    @Getter
    private final long runtimeId = CloudEntityRegistry.get().newEntityId();
    @Getter
    protected final SyncedEntityData data = new SyncedEntityData();
    /**
     * The data instance holding this entity's state. The base keys the hooks
     * own (movement, fire, air, scale, name tag, effects) live in their runtime
     * holders instead.
     */
    private final EntityData entityData;
    @Getter
    private final EntityType<?> type;
    @Getter
    public CloudChunk chunk;
    @Getter
    public NbtMap tag;
    public float highestPosition;
    public boolean firstMove = true;
    @Getter
    protected Vector3f position = Vector3f.ZERO;
    protected Vector3f lastPosition = Vector3f.ZERO;
    @Getter
    protected Vector3f motion = Vector3f.ZERO;
    protected Vector3f lastMotion = Vector3f.ZERO;
    protected Vector3f stuckSpeedMultiplier = Vector3f.ZERO;
    protected float yaw;
    protected float pitch;
    protected float lastYaw;
    protected float lastPitch;
    protected float pitchDelta;
    protected float yawDelta;
    protected float entityCollisionReduction = 0; // Higher than 0.9 will result a fast collisions
    @Getter
    public boolean onGround;
    public boolean inBlock = false;
    public boolean positionChanged;
    public boolean motionChanged;
    public int deadTicks = 0;
    public float fallDistance = 0;
    public int ticksLived = 0;
    public int lastUpdate;
    public int maxFireTicks;
    public int fireTicks = 0;
    public int inPortalTicks = 0;
    private int portalCooldown;
    public boolean pendingPortalTransfer = false;
    public Vector3i portalEntryBlock = null;
    @Getter
    protected BoundingBox boundingBox;
    public boolean isCollided = false;
    public boolean isCollidedHorizontally = false;
    public boolean isCollidedVertically = false;
    public boolean verticalCollisionBelow = false;
    protected Optional<Vector3i> supportingBlockPosition = Optional.empty();
    boolean onGroundNoBlocks = false;
    public int noDamageTicks;
    public boolean justCreated = true;
    public boolean fireProof;
    protected boolean noPhysics;
    protected CloudLevel level;
    @Getter
    public boolean closed = false;
    private boolean mounting;
    private boolean dismounting;
    private int boardingCooldown;
    @Getter
    protected @Nullable Entity vehicle;
    private @Nullable Entity owner;
    @Setter
    protected EntityDamageEvent lastDamageCause = null;
    protected int age = 0;
    protected float health = 20;
    protected float absorption = 0;
    protected boolean isStatic = false;
    protected CloudServer server;
    protected Timing timing;
    protected boolean isPlayer = false;
    private int maxHealth = 20;
    private boolean freezeTickingLocked;
    private volatile boolean spawned;
    private volatile boolean initialized;
    private static final int MAX_MOVEMENT_SEGMENTS = 100;
    private final Deque<EntityMovementSegment> movementSegments = new ArrayDeque<>(MAX_MOVEMENT_SEGMENTS);
    private static final Direction[] COLLISION_ESCAPE_DIRECTIONS = {
            Direction.NORTH,
            Direction.SOUTH,
            Direction.WEST,
            Direction.EAST,
            Direction.UP
    };

    public CloudEntity(EntityType<?> type, Location location) {
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

        ServerNbt.ensureInitialized();
        this.entityData = this.newData();
    }

    /**
     * Initializes the entity after its constructor and subclass fields are complete.
     */
    public final void initialize(Location location) {
        Objects.requireNonNull(location, "location");
        if (this.initialized) {
            return;
        }

        this.initialized = true;

        this.timing = Timings.getEntityTiming(this.getType());

        this.isPlayer = this instanceof CloudPlayer;

        this.chunk = (CloudChunk) location.getLevel().getLoadedChunk(location.getPosition());
        this.level = (CloudLevel) location.getLevel();
        this.server = (CloudServer) location.getLevel().getServer();

        this.position = location.getPosition();
        this.lastPosition = this.position;
        this.yaw = location.getYaw();
        this.pitch = location.getPitch();
        this.lastYaw = this.yaw;
        this.lastPitch = this.pitch;

        this.boundingBox = new BoundingBox(0, 0, 0, 0, 0, 0);

        this.initEntity();
        this.recalculateBoundingBox();

        this.lastUpdate = this.server.getTick();

        if (this.isPlayer) {
            this.registerInLevel(location);
        }
    }

    protected void initEntity() {
        this.updateNetworkBounds();
        this.data.set(STRUCTURAL_INTEGRITY, (int) this.getHealth());

        Buoyancy buoyancy = CloudEntityRegistry.get().getComponent(this.type, EntityComponents.BUOYANCY);
        if (buoyancy != null) {
            this.data.set(IS_BUOYANT, true);
            this.data.set(BUOYANCY_DATA, BuoyancyDataSerializer.serialize(buoyancy));
        }
    }

    public final void loadAdditionalData(NbtMap tag) {
        this.tag = tag;

        ServerNbt.MAPPER.readInto(tag, this.data());
        this.normalizeData();
        this.loadBaseCustom(tag);
        this.applyData(this.data());
        this.loadCustom(tag);
    }

    public final void saveAdditionalData(NbtMapBuilder tag) {
        Class<? extends EntityData> dataClass = this.dataClass();
        Set<String> owned = ServerNbt.MAPPER.ownedKeys(dataClass);
        Set<String> custom = this.customKeys();

        if (this.tag != null && !this.tag.isEmpty()) {
            for (Map.Entry<String, Object> entry : this.tag.entrySet()) {
                String key = entry.getKey();
                if (key.equals("identifier") || owned.contains(key) || custom.contains(key)) {
                    continue;
                }
                tag.put(key, entry.getValue());
            }
        }

        EntityData data = this.data();
        this.fillData(data);
        tag.putAll(ServerNbt.MAPPER.write(data));

        if (!(this instanceof CloudPlayer)) {
            tag.putString("identifier", this.type.getId().toString());
        }

        this.saveCustom(tag);
    }

    @Override
    public Optional<NbtMap> getPluginData(String pluginId) {
        checkNotNull(pluginId, "pluginId");
        if (this.tag == null) {
            return Optional.empty();
        }
        NbtMap root = this.tag.getCompound(NbtMapper.PLUGIN_DATA_KEY, null);
        if (root == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(root.getCompound(pluginId, null));
    }

    @Override
    public void setPluginData(String pluginId, NbtMap data) {
        checkNotNull(pluginId, "pluginId");
        checkNotNull(data, "data");
        NbtMap tag = this.tag == null ? NbtMap.EMPTY : this.tag;
        NbtMapBuilder root = tag.getCompound(NbtMapper.PLUGIN_DATA_KEY, NbtMap.EMPTY).toBuilder();
        root.putCompound(pluginId, data);
        NbtMapBuilder builder = tag.toBuilder();
        builder.putCompound(NbtMapper.PLUGIN_DATA_KEY, root.build());
        this.tag = builder.build();
        this.markChunkDirty();
    }

    @Override
    public void removePluginData(String pluginId) {
        checkNotNull(pluginId, "pluginId");
        if (this.tag == null) {
            return;
        }
        if (!this.tag.getCompound(NbtMapper.PLUGIN_DATA_KEY, NbtMap.EMPTY).containsKey(pluginId)) {
            return;
        }
        NbtMapBuilder root = this.tag.getCompound(NbtMapper.PLUGIN_DATA_KEY).toBuilder();
        root.remove(pluginId);
        NbtMapBuilder builder = this.tag.toBuilder();
        if (root.isEmpty()) {
            builder.remove(NbtMapper.PLUGIN_DATA_KEY);
        } else {
            builder.putCompound(NbtMapper.PLUGIN_DATA_KEY, root.build());
        }
        this.tag = builder.build();
        this.markChunkDirty();
    }

    /**
     * Marks the chunk holding this entity dirty so plugin data changes are
     * saved with the next chunk save. Tolerates entities without a chunk,
     * such as test fixtures that never ran {@code initialize}.
     */
    private void markChunkDirty() {
        if (this.chunk != null) {
            this.chunk.markDirty();
        }
    }

    /**
     * Applies persistent data to this entity. Subclasses override this,
     * call {@code super}, then cast the data to their own type.
     *
     * @param data the read data
     * @deprecated The data instance is the runtime state now; the base state is
     * read in place and subclasses copy their own state in later tasks. Kept as
     * a no-op so unmigrated subclasses keep working.
     */
    @Deprecated
    protected void applyData(EntityData data) {
    }

    /**
     * Fills persistent data from this entity. Subclasses override this,
     * call {@code super}, then cast the data to their own type.
     *
     * @param data the data to fill
     * @deprecated The data instance is the runtime state now; the base state is
     * written from it and subclasses copy their own state in later tasks. Kept
     * as a no-op so unmigrated subclasses keep working.
     */
    @Deprecated
    protected void fillData(EntityData data) {
    }

    /**
     * Returns the data instance holding this entity's state. Subclasses
     * override this with a covariant return for their own data type.
     *
     * @return the held data instance
     */
    protected EntityData data() {
        return this.entityData;
    }

    /**
     * The narrowest data class this runtime class can apply and fill. Subclasses
     * that cast the data to their own type override this to return that type.
     *
     * @return the minimum data class of this runtime class
     */
    protected Class<? extends EntityData> minimumDataClass() {
        return EntityData.class;
    }

    /**
     * Resolves the data class for this instance: the class registered for the
     * entity type when this runtime class can apply it, otherwise this runtime
     * class's minimum data class. Both the resolution and the data constructors
     * are cached; registration is closed before instances load or save.
     *
     * @return the data class for load, save and fresh instances
     */
    protected Class<? extends EntityData> dataClass() {
        Class<?> runtimeClass = this.getClass();
        EntityType<?> type = this.type;
        Class<? extends EntityData> minimum = this.minimumDataClass();
        return EFFECTIVE_DATA_CLASSES.computeIfAbsent(new DataClassKey(runtimeClass, type), key -> {
            Class<? extends EntityData> registered = CloudEntityRegistry.get().getDataClass(key.type());
            return minimum.isAssignableFrom(registered) ? registered : minimum;
        });
    }

    /**
     * Creates a fresh data object of the type registered for this entity.
     *
     * @return a new data object
     */
    protected final EntityData newData() {
        Class<? extends EntityData> dataClass = this.dataClass();
        try {
            java.lang.reflect.Constructor<?> constructor = DATA_CONSTRUCTORS.computeIfAbsent(dataClass, key -> {
                try {
                    java.lang.reflect.Constructor<?> found = key.getDeclaredConstructor();
                    found.setAccessible(true);
                    return found;
                } catch (NoSuchMethodException e) {
                    throw new IllegalStateException("Cannot instantiate data class " + key.getName(), e);
                }
            });
            return (EntityData) constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot instantiate data class " + dataClass.getName(), e);
        }
    }

    /**
     * Re-applies the setter normalization the previous load path ran while
     * copying data onto the entity, which a plain in-place read bypasses:
     * frozen ticks are clamped and a zero result is omitted, a non-bucketable
     * never keeps a bucket value, and tags skip blanks and stop at the cap.
     */
    private void normalizeData() {
        EntityData data = this.data();

        Integer frozen = data.getTicksFrozen();
        if (frozen != null) {
            int clamped = Math.clamp(frozen, 0, this.getMaxFreezeTicks());
            data.setTicksFrozen(clamped == 0 ? null : clamped);
        }

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
     * Reads the base exception keys: the movement and physics state, the fire
     * countdown, the air supply, the scale, the name tag and the active
     * effects, with today's keys, types and presence semantics. It runs once
     * before the deprecated transition hook so subclass state derived from it
     * (a baby ghast's scale, a turtle's home) still wins. A key the tag does
     * not hold leaves the current value alone, except for the keys today's
     * load always wrote. Every read is lenient like the mapper: any number
     * binds to a numeric property, any number binds to a boolean, and any
     * numeric list binds to a position list.
     *
     * @param tag the stored tag
     */
    protected void loadBaseCustom(NbtMap tag) {
        List<Float> pos = getFloatList(tag, "Pos");
        if (pos != null && pos.size() == 3) {
            this.setPosition(Vector3f.from(pos.get(0), pos.get(1), pos.get(2)));
        }
        List<Float> rotation = getFloatList(tag, "Rotation");
        if (rotation != null && rotation.size() == 2) {
            this.setRotation(rotation.get(0), rotation.get(1));
        }
        List<Float> motion = getFloatList(tag, "Motion");
        if (motion != null && motion.size() == 3) {
            this.setMotion(Vector3f.from(motion.get(0), motion.get(1), motion.get(2)));
        }

        this.highestPosition = this.getY();

        this.setFallDistance(getFloat(tag, "FallDistance", 0f));
        // Fire loads exactly as the old applyData did: the tagged seconds ran
        // through setOnFire before the creature's armor was loaded, so no
        // Fire Protection reduction applied. Assign the countdown directly
        // instead of going through the armor-aware setter.
        short fireSeconds = getShort(tag, "Fire", (short) 0);
        int fireTicks = fireSeconds * 20;
        if (fireTicks > this.fireTicks) {
            this.fireTicks = fireTicks;
        }
        this.setAirTicks(getShort(tag, "Air", (short) 400));
        this.setOnGround(getBoolean(tag, "OnGround", false));
        if (tag.containsKey("Scale")) {
            this.setScale(getFloat(tag, "Scale", 1f));
        }

        // The DTO now holds the frozen ticks; refresh the derived freeze-strength
        // metadata the setter would have synced on a fresh read.
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

        tag.listenForString("CustomName", this::setNameTag);
        if (tag.containsKey("CustomNameVisible")) {
            this.setNameTagVisible(getBoolean(tag, "CustomNameVisible", false));
        }
        if (tag.containsKey("CustomNameAlwaysVisible")) {
            this.setNameTagAlwaysVisible(getBoolean(tag, "CustomNameAlwaysVisible", false));
        }
    }

    /**
     * Reads data that subclasses handle outside their data class. The base
     * exception keys load exactly once, in {@link #loadBaseCustom} before the
     * deprecated transition hook, so this stays a no-op for the base: every
     * subclass {@code super.loadCustom} call in the chain still resolves here.
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
        tag.putShort("Fire", (short) this.getFireTicks());
        tag.putShort("Air", this.data.require(AIR_SUPPLY));
        tag.putBoolean("OnGround", this.onGround);
        tag.putFloat("Scale", this.getScale());
        if (this.hasNameTag()) {
            tag.putString("CustomName", this.getNameTag());
            tag.putBoolean("CustomNameVisible", this.isNameTagVisible());
            tag.putBoolean("CustomNameAlwaysVisible", this.isNameTagAlwaysVisible());
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
        return Set.of("Pos", "Motion", "Rotation", "FallDistance", "Fire", "OnGround", "Air",
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
        } else if (this.owner == null || this.owner.getUniqueId() != ownerId) {
            this.owner = this.level.getEntityByRuntimeId(ownerId);
        }

        return this.owner;
    }

    @Override
    public void setOwner(@Nullable Entity entity) {
        this.owner = entity;
        this.data.set(OWNER_EID, entity == null ? -1 : entity.getUniqueId());
    }

    public String getName() {
        if (this.hasNameTag()) {
            return this.getNameTag();
        }

        String name = CloudEntityRegistry.get().getLegacyName(this.type.getId());
        return name != null ? name : this.type.getId().toString();
    }

    @Override
    public Component displayName() {
        if (this.hasNameTag()) {
            return Component.text(this.getNameTag());
        }

        if ("minecraft".equals(this.type.getId().getNamespace())) {
            return Component.translatable("entity." + this.type.getId().getName() + ".name");
        }

        return Component.text(this.type.getId().toString());
    }

    public boolean hasNameTag() {
        return !this.getNameTag().isEmpty();
    }

    public String getNameTag() {
        CharSequence value = this.data.get(NAME);
        return value != null ? value.toString() : "";
    }

    public void setNameTag(String name) {
        this.data.set(NAME, name);
    }

    public boolean isNameTagVisible() {
        return this.data.getFlag(CAN_SHOW_NAME);
    }

    public void setNameTagVisible(boolean value) {
        this.data.setFlag(CAN_SHOW_NAME, value);
    }

    public void setNameTagVisible() {
        this.setNameTagVisible(true);
    }

    public boolean isNameTagAlwaysVisible() {
        return this.data.require(NAMETAG_ALWAYS_SHOW) == 1;
    }

    public void setNameTagAlwaysVisible(boolean value) {
        this.data.set(NAMETAG_ALWAYS_SHOW, (byte) (value ? 1 : 0));
    }

    public void setNameTagAlwaysVisible() {
        this.setNameTagAlwaysVisible(true);
    }

    public String getScoreTag() {
        CharSequence value = this.data.get(SCORE);
        return value != null ? value.toString() : "";
    }

    public void setScoreTag(String score) {
        this.data.set(SCORE, score);
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
        return this.data().getTags().size() < MAX_SCOREBOARD_TAGS && this.data().getTags().add(value);
    }

    @Override
    public boolean removeScoreboardTag(String tag) {
        return this.data().getTags().remove(Objects.requireNonNull(tag, "tag"));
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
        return this.data.require(SCALE);
    }

    public void setScale(float scale) {
        this.data.set(SCALE, scale);
        this.recalculateBoundingBox();
    }

    public void recalculateBoundingBox() {
        float height = this.getHeight() * this.getScale();
        float radius = (this.getWidth() * this.getScale()) / 2;
        this.boundingBox = new BoundingBox(this.position.getX() - radius, this.position.getY(), this.position.getZ() - radius,
                this.position.getX() + radius, this.position.getY() + height, this.position.getZ() + radius);

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
        this.yaw = yaw;
        this.pitch = pitch;
        this.scheduleUpdate();
    }

    public boolean setPositionAndRotation(Vector3f pos, float yaw, float pitch) {
        if (this.setPosition(pos)) {
            this.setRotation(yaw, pitch);
            return true;
        }

        return false;
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
        return CardinalDirection.values()[GenericMath.floor((((this.yaw + 180) % 360) / 22.5))];
    }

    public Vector3f getDirectionVector() {
        double y = -Math.sin(Math.toRadians(this.getPitch()));
        double xz = Math.cos(Math.toRadians(this.getPitch()));
        double x = -xz * Math.sin(Math.toRadians(this.getYaw()));
        double z = xz * Math.cos(Math.toRadians(this.getYaw()));
        return GenericMath.normalizeSafe(Vector3f.from(x, y, z));
    }

    public Vector2f getDirectionPlane() {
        return Vector2f.from(-Math.cos(Math.toRadians(this.yaw) - Math.PI / 2), -Math.sin(Math.toRadians(this.yaw) - Math.PI / 2)).normalize();
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

    public void setImmobile() {
        this.setImmobile(true);
    }

    public boolean canClimb() {
        return this.data.getFlag(CAN_CLIMB);
    }

    public void setCanClimb() {
        this.setCanClimb(true);
    }

    public void setCanClimb(boolean value) {
        this.data.setFlag(CAN_CLIMB, value);
    }

    public boolean canClimbWalls() {
        return this.data.getFlag(WALL_CLIMBING);
    }

    public void setCanClimbWalls() {
        this.setCanClimbWalls(true);
    }

    public void setCanClimbWalls(boolean value) {
        this.data.setFlag(WALL_CLIMBING, value);
    }

    public boolean setMotion(Vector3f motion) {
        checkNotNull(motion, "motion");
        if (!Float.isFinite(motion.getX()) || !Float.isFinite(motion.getY()) || !Float.isFinite(motion.getZ())) {
            throw new IllegalArgumentException("Motion must be finite");
        }

        if (!this.justCreated) {
            EntityMotionEvent ev = new EntityMotionEvent(this, motion);
            this.server.getEventManager().fire(ev);
            if (ev.isCancelled()) {
                return false;
            }
        }

        this.motion = motion;
        this.onMotionChanged();

        if (!this.justCreated) {
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

        if (!this.justCreated) {
            this.server.getEventManager().fire(event);
        }

        if (!event.isCancelled() && !this.isClosed()) {
            this.setMotion(this.getMotion().add(event.getKnockback()));
        }
    }

    @Override
    public void setOnGround(boolean onGround) {
        EntityMovementController.setOnGroundWithMovement(this, onGround, this.isCollidedHorizontally, null);
    }

    @Override
    public Optional<Vector3i> getSupportingBlockPosition() {
        return this.supportingBlockPosition;
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

    @Override
    public void makeStuckInBlock(BlockState state, Vector3f speedMultiplier) {
        this.resetFallDistance();
        this.stuckSpeedMultiplier = speedMultiplier;
    }

    public void updateMovement() {
        float diffPosition = this.position.distanceSquared(this.lastPosition);
        double diffRotation = (this.yaw - this.lastYaw) * (this.yaw - this.lastYaw) + (this.pitch - this.lastPitch) * (this.pitch - this.lastPitch);

        float diffMotion = this.motion.distanceSquared(this.lastMotion);

        if (diffPosition > 0.0001 || diffRotation > 1.0) { //0.2 ** 2, 1.5 ** 2
            this.lastPosition = this.position;

            this.lastYaw = this.yaw;
            this.lastPitch = this.pitch;

            this.addMovement(this.position.getX(), this.position.getY() + this.getBaseOffset(), this.position.getZ(),
                    this.yaw, this.pitch, this.yaw);
        }

        if (diffMotion > 0.0025 || (diffMotion > 0.0001 && this.getMotion().lengthSquared() <= 0.0001)) { //0.05 ** 2
            this.lastMotion = this.motion;

            this.addMotion(this.motion);
        }
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

        CloudServer.broadcastPacket(this.hasSpawned, packet);
    }

    public void sendAuthoritativeDisplacement() {
        this.lastPosition = this.position;
        this.lastYaw = this.yaw;
        this.lastPitch = this.pitch;
        CloudServer.broadcastPacket(this.hasSpawned, this.createAuthoritativeDisplacementPacket());
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
        if (entity == null || this.sharesRootVehicle(entity)) {
            return false;
        }
        return !this.justCreated
                && entity.canBeCollidedWith(this);
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

    private static Entity rootVehicle(Entity entity) {
        Entity vehicle = entity.getVehicle();
        while (vehicle != null) {
            entity = vehicle;
            vehicle = entity.getVehicle();
        }
        return entity;
    }

    @Override
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

    public void onCollideWithPlayer(EntityHuman entityPlayer) {
    }

    protected void checkBlockCollision() {
        EntityInsideBlockScanner.scan(this);
    }

    protected boolean moveTowardsClosestSpace(Vector3f pos) {
        return this.moveTowardsClosestSpace(pos.getX(), (this.boundingBox.getMinY() + this.boundingBox.getMaxY()) / 2f, pos.getZ());
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

    public boolean canTriggerWalking() {
        return true;
    }

    public boolean canTriggerPressurePlate() {
        return true;
    }

    public final void scheduleUpdate() {
        if (this.initialized && !this.closed) {
            this.level.scheduleEntityUpdate(this);
        }
    }

    public boolean onUpdate(int currentTick) {
        if (this.closed) {
            return false;
        }

        if (!this.isAlive()) {
            ++this.deadTicks;
            if (this.deadTicks >= 10) {
                this.despawnFromAll();
                if (!this.isPlayer) {
                    this.close();
                }
            }

            return this.deadTicks < 10;
        }

        int tickDiff = currentTick - this.lastUpdate;
        if (tickDiff <= 0) {
            return true;
        }

        this.lastUpdate = currentTick;

        boolean hasUpdate = this.entityBaseTick(tickDiff);
        hasUpdate |= CloudEntityRegistry.get().requireComponent(this.type, EntityComponents.ON_TICK).execute(this, currentTick);

        this.updateMovement();
        this.flushEntityData();

        return hasUpdate;
    }


    public boolean entityBaseTick() {
        return this.entityBaseTick(1);
    }

    public boolean entityBaseTick(int tickDiff) {
        try (Timing ignored = Timings.entityBaseTickTimer.startTiming()) {

            this.justCreated = false;
            this.boardingCooldown = Math.max(0, this.boardingCooldown - tickDiff);

            if (!this.isAlive()) {
                this.clearActivePotionEffects(PotionEffectCause.DEATH);
                this.despawnFromAll();

                if (!this.isPlayer) {
                    this.close();
                }

                return false;
            }

            if (this.vehicle != null && (!this.vehicle.isAlive() || this.vehicle.isClosed())) {
                this.dismount(this.vehicle, false);
            }

            updatePassengers();

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

            if (this.fireTicks > 0) {
                if (this.fireProof) {
                    this.fireTicks -= 4 * tickDiff;
                    if (this.fireTicks < 0) {
                        this.fireTicks = 0;
                    }
                } else {
                    if (!this.hasPotionEffect(EffectTypes.FIRE_RESISTANCE) && ((this.fireTicks % 20) == 0 || tickDiff > 20)) {
                        this.damage(1, DamageSource.of(DamageTypes.ON_FIRE));
                    }

                    this.fireTicks -= tickDiff;
                }

                if (this.fireTicks <= 0) {
                    this.extinguish();
                } else if (!this.fireProof && (!(this instanceof CloudPlayer) || !((CloudPlayer) this).isSpectator())) {
                    this.data.setFlag(ON_FIRE, true);
                    hasUpdate = true;
                }
            }

            if (this.noDamageTicks > 0) {
                this.noDamageTicks -= tickDiff;
                if (this.noDamageTicks < 0) {
                    this.noDamageTicks = 0;
                }
            }

            if (this.isOnPortalCooldown()) {
                tickPortalCooldown();
            } else {
                int portalThreshold = getPortalTransitionTicks();
                if (this.inPortalTicks > 0 && (portalThreshold == 0 || this.inPortalTicks >= portalThreshold)) {
                    this.setPortalCooldown();
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

            this.age += tickDiff;
            this.ticksLived += tickDiff;
            TimingsHistory.activatedEntityTicks++;

            return hasUpdate;
        }
    }

    protected void onBelowLevel() {
        this.close();
    }

    @Override
    public float getHighestPosition() {
        return highestPosition;
    }

    @Override
    public void setHighestPosition(float highestPosition) {
        this.highestPosition = highestPosition;
    }

    public void setFallDistance(float fallDistance) {
        this.fallDistance = fallDistance;
        this.highestPosition = this.position.getY() + fallDistance;
    }

    public void resetFallDistance() {
        this.fallDistance = 0;
        this.highestPosition = this.getY();
    }

    protected void updateFallState(boolean onGround) {
        if (!onGround) {
            this.highestPosition = Math.max(this.highestPosition, this.getY());
            this.fallDistance = Math.max(0, this.highestPosition - this.getY());
            return;
        }

        float distance = Math.max(0, this.highestPosition - this.getY());
        this.resetFallDistance();
        if (distance > 0 && this instanceof EntityLiving && !this.level.getLiquidState(this.position.toInt()).getType().isSameFamily(LiquidTypes.WATER)) {
            this.fall(distance);
        }
    }

    public void fall(float fallDistance) {
        Block down = this.level.getBlock(this.getLandingBlockPosition());
        down.requireComponent(BlockComponents.ON_FALL_ON).execute(down, this, fallDistance);
    }

    protected Vector3i getLandingBlockPosition() {
        return this.supportingBlockPosition.orElseGet(() -> this.position.sub(0, 0.2f, 0).toInt());
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

    public void handleLavaMovement() {
        //todo
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
        return this.freezeTickingLocked;
    }

    @Override
    public void lockFreezeTicks(boolean locked) {
        this.freezeTickingLocked = locked;
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
        return this.teleport(pos, PlayerTeleportCause.PLUGIN);
    }

    public boolean teleport(Vector3f pos, PlayerTeleportCause cause) {
        return this.teleport(Location.from(pos, this.yaw, this.pitch, this.level), cause);
    }

    public boolean teleport(Location location) {
        return this.teleport(location, PlayerTeleportCause.PLUGIN);
    }

    public boolean teleport(Location location, PlayerTeleportCause cause) {
        Objects.requireNonNull(cause, "cause");
        Location from = this.getLocation();
        EntityTeleportEvent event = new EntityTeleportEvent(this, from, location);
        this.server.getEventManager().fire(event);
        if (event.isCancelled()) {
            return false;
        }

        return this.teleportWithoutEvent(event.getTo());
    }

    public boolean teleportWithoutEvent(Location location) {
        Objects.requireNonNull(location, "location");
        Location from = this.getLocation();
        if (from.getLevel() == location.getLevel() || this.switchLevel((CloudLevel) location.getLevel())) {
            this.setMotion(Vector3f.ZERO);

            if (this.setPositionAndRotation(location.getPosition(), location.getYaw(), location.getPitch())) {
                this.resetFallDistance();
                this.onGround = true;

                this.updateMovement();

                return true;
            }
        }

        return false;
    }

    protected boolean switchLevel(CloudLevel targetLevel) {
        checkNotNull(targetLevel, "targetLevel");
        if (this.closed) {
            return false;
        }

        EntityLevelChangeEvent ev = new EntityLevelChangeEvent(this, this.level, targetLevel);
        this.server.getEventManager().fire(ev);
        if (ev.isCancelled() || this.closed) {
            return false;
        }

        this.level.unregisterEntity(this);
        if (this.chunk != null) {
            this.chunk.unregisterEntity(this);
        }
        this.despawnFromAll();

        this.level = targetLevel;
        this.level.registerEntity(this);
        this.scheduleUpdate();
        this.chunk = null;

        return true;
    }

    /**
     * Returns the number of ticks this entity must wait after a portal transfer
     * before it can use another portal.
     */
    public int getPortalCooldownTicks() {
        return PORTAL_COOLDOWN_TICKS;
    }

    public final boolean isOnPortalCooldown() {
        return this.portalCooldown > 0;
    }

    public final void setPortalCooldown() {
        this.portalCooldown = this.getPortalCooldownTicks();
    }

    protected final void clearPortalCooldown() {
        this.portalCooldown = 0;
    }

    protected void tickPortalCooldown() {
        this.portalCooldown--;
    }

    /**
     * Returns the number of ticks the entity must spend inside a portal before
     * being transferred.
     */
    protected int getPortalTransitionTicks() {
        return PORTAL_TRANSFER_TICKS;
    }

    protected void onInsidePortal() {
        if (this.getVehicle() == null) {
            if (this.isOnPortalCooldown()) {
                this.setPortalCooldown();
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

        this.setPortalCooldown();
        if (!EndPortals.transfer(this)) {
            this.clearPortalCooldown();
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

        this.setPortalCooldown();
        if (!EndGateways.transfer(this, position)) {
            this.clearPortalCooldown();
        }
    }

    public Entity getPassenger() {
        return Iterables.getFirst(this.passengers, null);
    }

    @Override
    public List<Entity> getPassengers() {
        return List.copyOf(this.passengers);
    }

    public boolean isPassenger(Entity entity) {
        return this.passengers.contains(entity);
    }

    @Override
    public boolean isControlling(Entity entity) {
        return entity != null && this.getControllingPassenger() == entity;
    }

    @Override
    public @Nullable Entity getControllingPassenger() {
        return this.passengers.isEmpty() ? null : this.passengers.getFirst();
    }

    public Vector3f getSeatPosition() {
        return this.data.require(SEAT_OFFSET);
    }

    public void setSeatPosition(Vector3f pos) {
        this.data.set(SEAT_OFFSET, pos);
    }

    public Vector3f getMountedOffset(Entity passenger) {
        float yOffset = getMountedHeightOffset() + passenger.getPassengerHeightOffset();
        return Vector3f.from(0f, yOffset, 0f);
    }

    @Override
    public boolean mount(Entity vehicle) {
        checkNotNull(vehicle, "The target of the mounting entity can't be null");
        if (this.mounting || this.closed || !this.isAlive() || vehicle.isClosed() || !vehicle.isAlive() || this.level != vehicle.getLevel()
                || this.wouldCreateVehicleCycle(vehicle) || !((CloudEntity) vehicle).canAddPassenger(this)) {
            return false;
        }

        if (this.vehicle != null && !this.dismount(this.vehicle)) {
            return false;
        }

        if (vehicle instanceof Vehicle target) {
            VehicleEnterEvent event = new VehicleEnterEvent(target, this);
            this.mounting = true;

            try {
                this.server.getEventManager().fire(event);
            } finally {
                this.mounting = false;
            }

            if (event.isCancelled()) {
                return false;
            }
        }

        if (this.vehicle != null || this.closed || !this.isAlive() || vehicle.isClosed() || !vehicle.isAlive() || this.level != vehicle.getLevel()
                || this.wouldCreateVehicleCycle(vehicle) || !((CloudEntity) vehicle).canAddPassenger(this)) {
            return false;
        }

        this.vehicle = vehicle;
        this.onGround = false;
        this.resetFallDistance();

        ((CloudEntity) vehicle).onMount(this);
        this.data.setFlag(RIDING, true);
        this.flushEntityData();
        broadcastLinkPacket(vehicle, vehicle.getPassengers().indexOf(this) == 0 ? EntityLinkData.Type.RIDER : EntityLinkData.Type.PASSENGER, this instanceof Player);
        onMountComplete(vehicle);

        return true;
    }

    /**
     * Attempts gameplay boarding, respecting sneaking and the delay after dismounting.
     */
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
        checkArgument(passenger.getVehicle() == this, "passenger is not in this vehicle");
        checkArgument(!this.passengers.contains(passenger), "passenger is already mounted to this vehicle");
        this.passengers.add(passenger);

        if (passenger instanceof CloudPlayer ridingPlayer) {
            this.spawnTo(ridingPlayer);
        }

        Vector3f seatOffset = this.getMountedOffset(passenger);
        passenger.setSeatPosition(seatOffset);
        ((CloudEntity) passenger).data.set(SEAT_LOCK_RIDER_ROTATION_DEGREES, 181.0f);
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

    /**
     * Attempts dismounting with an optional preferred feet position. The vehicle validates
     * the position before using it. A canceled exit leaves the passenger mounted.
     */
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
        broadcastLinkPacket(vehicle, EntityLinkData.Type.REMOVE, riderInitiated);
        this.vehicle = null;
        this.boardingCooldown = 60;
        this.data.setFlag(RIDING, false);
        ((CloudEntity) vehicle).onDismount(this, preferredPosition);
        this.setSeatPosition(Vector3f.ZERO);
        this.data.setFlag(MOVING, true);
        this.flushEntityData();
    }

    protected void ejectPassengers() {
        for (Entity passenger : this.getPassengers()) {
            ((CloudEntity) passenger).dismount(this, false);
        }
    }

    protected void onDismount(Entity passenger, @Nullable Vector3f preferredPosition) {
        checkArgument(passenger.getVehicle() != this, "passenger is still mounted");
        checkArgument(this.passengers.remove(passenger), "passenger is not in this vehicle");

        passenger.setSeatPosition(Vector3f.ZERO);
        ((CloudEntity) passenger).data.set(SEAT_LOCK_RIDER_ROTATION_DEGREES, 0.0f);
        for (int index = 0; index < this.passengers.size(); index++) {
            ((CloudEntity) this.passengers.get(index)).broadcastLinkPacket(this,
                    index == 0 ? EntityLinkData.Type.RIDER : EntityLinkData.Type.PASSENGER, false);
        }
    }

    public void updatePassengers() {
        if (this.passengers.isEmpty()) {
            return;
        }

        for (Entity passenger : new ArrayList<>(this.passengers)) {
            if (passenger.getVehicle() != this) {
                continue;
            }

            if (!passenger.isAlive()) {
                ((CloudEntity) passenger).dismount(this, false);
                continue;
            }

            updatePassengerPosition(passenger);
        }
    }

    protected void updatePassengerPosition(Entity passenger) {
        passenger.setPosition(this.getPosition().add(this.getPassengerAttachmentPoint(passenger)));
    }

    protected void broadcastLinkPacket(Entity vehicle, EntityLinkData.Type type, boolean riderInitiated) {
        SetEntityLinkPacket packet = new SetEntityLinkPacket();
        boolean immediate = type == EntityLinkData.Type.REMOVE && (this.closed || !this.isAlive() || vehicle.isClosed() || !vehicle.isAlive());
        packet.setEntityLink(new EntityLinkData(vehicle.getUniqueId(), getUniqueId(), type, immediate, riderInitiated, 0));
        CloudServer.broadcastPacket(((CloudEntity) vehicle).getViewers(), packet);
    }

    @Override
    public float getHealth() {
        return health;
    }

    @Override
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

    @Override
    public int getMaxHealth() {
        ActivePotionEffect healthBoost = this.effects.get(EffectTypes.HEALTH_BOOST);
        long bonus = healthBoost == null ? 0 : 4L * (healthBoost.getAmplifier() + 1L);
        return (int) Math.min(Integer.MAX_VALUE, this.maxHealth + bonus);
    }

    @Override
    public void setMaxHealth(int maxHealth) {
        if (maxHealth <= 0) {
            throw new IllegalArgumentException("Maximum health must be positive");
        }

        this.maxHealth = maxHealth;

        if (this.health > this.getMaxHealth()) {
            this.setHealth(this.getMaxHealth());
        }
    }

    @Override
    public boolean isAlive() {
        return this.health > 0;
    }

    @Override
    public float getAbsorption() {
        return absorption;
    }

    @Override
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

    @Override
    public boolean damage(float amount, DamageSource source) {
        return this.damageWithResult(amount, source).applied();
    }

    /**
     * Applies one hit and retains its cancellation outcome for dependent effects.
     */
    public CloudDamageResult damageWithResult(float amount, DamageSource source) {
        CloudEntityDamageEvent event = this.createDamageEvent(source, amount);
        boolean applied = this.applyDamage(event);
        return new CloudDamageResult(applied, event.isCancelled());
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
                || (this.hasPotionEffect(EffectTypes.FIRE_RESISTANCE) && source.getDamageType().is(DamageTypeTags.IS_FIRE));
    }

    /**
     * Applies a damage event after entity-specific preprocessing.
     *
     * @param source the damage event
     * @return whether damage was applied
     */
    protected boolean applyDamage(CloudEntityDamageEvent source) {
        if (this.isDamageImmune(source.getDamageSource())) {
            return false;
        }

        getServer().getEventManager().fire(source);
        if (source.isCancelled() || source.getDamage() <= 0 || this.isClosed() || !this.isAlive()) {
            return false;
        }

        if (!this.prepareDamage(source) || source.getDamage() <= 0 || this.isClosed() || !this.isAlive()) {
            return false;
        }

        float damage = source.getFinalDamage();
        float absorbed = source.getAbsorbedDamage();
        this.onDamageAccepted(source);
        if (this.isClosed() || !this.isAlive()) {
            return false;
        }

        if (damage <= 0 && absorbed <= 0) {
            return true;
        }

        setLastDamageCause(source);
        if (absorbed > 0) {
            this.setAbsorption(Math.max(0, this.getAbsorption() - absorbed));
        }

        float remainingHealth = Math.max(0, getHealth() - damage);
        if (remainingHealth > 0) {
            this.setHealth(remainingHealth);
        } else if (!this.tryPreventDeath(source.getDamageSource()) && !this.isClosed() && this.isAlive()) {
            this.setHealth(0);
        }

        return true;
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

    protected boolean tryPreventDeath(DamageSource source) {
        return false;
    }

    @Override
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

    public boolean hasPotionEffect(EffectType type) {
        return this.effects.containsKey(checkNotNull(type, "type"));
    }

    public boolean addPotionEffect(PotionEffect effect) {
        return this.addPotionEffect(effect, null, PotionEffectCause.PLUGIN);
    }

    public boolean addPotionEffect(PotionEffect effect, @Nullable Entity source, PotionEffectCause cause) {
        checkNotNull(effect, "effect");
        checkNotNull(cause, "cause");
        checkState(this instanceof Living, "Potion effects can only be applied to living entities");

        if (effect.getType() == EffectTypes.FREEZING && !CloudEntityRegistry.get().requireComponent(this.getType(), EntityComponents.CAN_FREEZE).execute(this)) {
            return false;
        }

        ActivePotionEffect oldEffect = this.effects.get(effect.getType());
        PotionEffect oldEffectSnapshot = oldEffect == null ? null : oldEffect.snapshot();
        PotionEffectAction action = oldEffect == null ? PotionEffectAction.ADDED : PotionEffectAction.CHANGED;
        boolean override = oldEffectSnapshot == null || shouldOverrideEffect(oldEffectSnapshot, effect);

        EntityPotionEffectEvent event = new EntityPotionEffectEvent((Living) this, oldEffectSnapshot, effect, source, cause, action, override);
        this.server.getEventManager().fire(event);
        if (event.isCancelled() || action == PotionEffectAction.CHANGED && !event.isOverride()) {
            return false;
        }

        ActivePotionEffect newEffect = ActivePotionEffect.from(effect);
        newEffect.onApplied(this, oldEffect);
        this.effects.put(newEffect.getType(), newEffect);

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
        checkState(this instanceof Living, "Potion effects can only be removed from living entities");
        checkNotNull(cause, "cause");
        ActivePotionEffect effect = this.effects.get(checkNotNull(type, "type"));
        if (effect == null) {
            return false;
        }

        PotionEffect oldEffect = effect.snapshot();
        EntityPotionEffectEvent event = new EntityPotionEffectEvent((Living) this, oldEffect, null, null, cause, PotionEffectAction.REMOVED, false);
        this.server.getEventManager().fire(event);
        if (event.isCancelled()) {
            return false;
        }

        this.effects.remove(type);
        effect.onRemoved(this);
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
            player.sendPacket(NetworkUtils.effectToNetwork(effect.snapshot(), this.getRuntimeId(), MobEffectPacket.Event.ADD,
                    this.server.getTick()));
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

    public boolean onInteract(Player player, ItemStack item, Vector3f clickedPos) {
        if (CloudEntityRegistry.get().requireComponent(this.type, EntityComponents.ON_INTERACT)
                .execute(this, player, item, clickedPos)) {
            return true;
        }
        return onInteract(player, item);
    }

    public boolean onInteract(Player player, ItemStack item) {
        return false;
    }

    @Override
    public boolean spawn() {
        EntitySpawnEvent event;
        if (this instanceof DroppedItem droppedItem) {
            event = new ItemSpawnEvent(droppedItem);
        } else if (this instanceof Projectile projectile) {
            event = new ProjectileLaunchEvent(projectile);
        } else {
            event = new EntitySpawnEvent(this);
        }

        return this.spawn(event);
    }

    public boolean spawn(EntitySpawnEvent event) {
        if (this.closed || this.spawned) {
            return false;
        }

        if (event.getEntity() != this) {
            throw new IllegalArgumentException("Spawn event does not belong to this entity");
        }

        this.server.getEventManager().fire(event);
        if (event.isCancelled()) {
            this.closed = true;
            return false;
        }

        this.registerInLevel(this.getLocation());
        return true;
    }

    public void restoreFromStorage() {
        if (this.closed || this.spawned) {
            throw new IllegalStateException("Cannot restore an entity that is closed or already spawned");
        }

        this.registerInLevel(this.getLocation());
    }

    private void registerInLevel(Location location) {
        if (this.spawned) {
            return;
        }

        this.spawned = true;
        this.level.registerEntity(this);
        this.scheduleUpdate();

        this.level.getChunkFuture(location.getChunkX(), location.getChunkZ()).whenComplete((chunk, throwable) -> {
            if (throwable != null || this.closed || !this.spawned) {
                return;
            }

            this.chunk = chunk;
            chunk.registerEntity(this);
            this.spawnToAll();
        });
    }

    @Override
    public void spawnTo(Player player) {
        this.spawnTo(((CloudPlayer) player));
    }

    public void spawnTo(CloudPlayer player) {
        if (!this.spawned || this.chunk == null || this.closed) {
            return;
        }

        boolean sent = player.isChunkSent(this.chunk.getX(), this.chunk.getZ());
        boolean added = sent && this.hasSpawned.add(player);
        if (!sent || !added) {
            // chunk not yet received by client, or entity already spawned
            return;
        }

        player.sendPacket(createAddEntityPacket());

        if (this.vehicle != null) {
            this.vehicle.spawnTo(player);

            SetEntityLinkPacket packet = new SetEntityLinkPacket();
            packet.setEntityLink(new EntityLinkData(this.vehicle.getUniqueId(),
                    this.getUniqueId(), this.vehicle.getPassengers().indexOf(this) == 0
                    ? EntityLinkData.Type.RIDER : EntityLinkData.Type.PASSENGER, true, false, 0));

            player.sendPacket(packet);
        }
    }

    public void spawnToAll() {
        if (!this.spawned && !this.spawn()) {
            return;
        }

        if (this.chunk == null || this.closed) {
            return;
        }

        for (Player player : this.level.getChunkPlayers(this.chunk.getX(), this.chunk.getZ())) {
            if (player.isOnline()) {
                this.spawnTo(player);
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

        for (int i = 0; i < this.passengers.size(); i++) {
            addEntity.getEntityLinks().add(new EntityLinkData(this.getUniqueId(),
                    this.passengers.get(i).getUniqueId(), i == 0 ? EntityLinkData.Type.RIDER : EntityLinkData.Type.PASSENGER, false, false, 0));
        }

        this.addAdditionalSpawnData(addEntity);
        return addEntity;
    }

    protected void addAdditionalSpawnData(AddEntityPacket packet) {
    }

    @Override
    public void despawnFrom(Player player) {
        this.despawnFrom((CloudPlayer) player);
    }

    public void despawnFrom(CloudPlayer player) {
        if (this.hasSpawned.remove(player)) {
            RemoveEntityPacket packet = new RemoveEntityPacket();
            packet.setUniqueEntityId(this.getUniqueId());
            player.sendPacket(packet);
        }
    }

    public void despawnFromAll() {
        for (Player player : this.hasSpawned) {
            this.despawnFrom(player);
        }
    }

    public Set<CloudPlayer> getViewers() {
        return Set.copyOf(this.hasSpawned);
    }

    protected void checkChunks() {
        Vector3f pos = this.getPosition();
        if (this.chunk == null || (this.chunk.getX() != pos.getFloorX() >> 4 || this.chunk.getZ() != pos.getFloorZ() >> 4)) {
            if (this.chunk != null) {
                this.chunk.unregisterEntity(this);
            }
            this.chunk = this.level.getLoadedChunk(pos);
            if (chunk == null) {
                return; // Converter will throw NPE otherwise.
            }

            if (!this.justCreated) {
                this.updateViewers(chunk.getViewers());
            }

            if (this.chunk == null) {
                return;
            }

            this.chunk.registerEntity(this);
        }
    }

    protected final void updateViewers(Set<CloudPlayer> viewers) {
        for (CloudPlayer viewer : this.hasSpawned) {
            if (!viewers.contains(viewer)) {
                this.despawnFrom(viewer);
            }
        }

        for (CloudPlayer viewer : viewers) {
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

    public void sendData(CloudPlayer player, EntityDataType<?>... data) {
        SetEntityDataPacket packet = new SetEntityDataPacket();
        packet.setRuntimeEntityId(this.getRuntimeId());
        packet.getMetadata().putAll(this.data.snapshot(data));

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

        CloudServer.broadcastPacket(this.getViewers(), packet);
    }

    public void close() {
        if (!this.closed) {
            this.closed = true;
            if (this.vehicle != null) {
                this.dismount(this.vehicle, false);
            }

            this.ejectPassengers();

            if (this.spawned) {
                this.server.getEventManager().fire(new EntityDespawnEvent(this));
            }

            this.despawnFromAll();
            if (this.chunk != null) {
                this.chunk.unregisterEntity(this);
            }

            if (this.level != null) {
                this.level.unregisterEntity(this);
            }
        }
    }

    @Override
    public String toString() {
        return "Entity(type=" + type.getId() + ", id=" + getUniqueId() + ")";
    }

    private record LiquidContact(boolean touchingWater, Vector3f flow) {
    }
}
