package org.cloudburstmc.server.entity;

import co.aikar.timings.Timing;
import co.aikar.timings.Timings;
import co.aikar.timings.TimingsHistory;
import com.google.common.collect.Iterables;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import lombok.extern.log4j.Log4j2;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.entity.*;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.damage.DamageTypeTags;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.entity.misc.LightningBolt;
import org.cloudburstmc.api.entity.vehicle.Vehicle;
import org.cloudburstmc.api.event.Event;
import org.cloudburstmc.api.event.entity.*;
import org.cloudburstmc.api.event.player.PlayerInteractEvent;
import org.cloudburstmc.api.event.player.PlayerTeleportEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.api.player.GameMode;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.potion.Effect;
import org.cloudburstmc.api.potion.EffectType;
import org.cloudburstmc.api.potion.EffectTypes;
import org.cloudburstmc.api.util.BoundingBox;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.MovementType;
import org.cloudburstmc.api.util.data.CardinalDirection;
import org.cloudburstmc.api.util.data.MountType;
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
import org.cloudburstmc.server.entity.data.SyncedEntityData;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.EnumLevel;
import org.cloudburstmc.server.level.NetherPortals;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.level.collision.CloudVoxelShapes;
import org.cloudburstmc.server.math.MathHelper;
import org.cloudburstmc.server.network.NetworkUtils;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.potion.CloudEffect;
import org.cloudburstmc.server.registry.CloudEntityRegistry;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.cloudburstmc.api.block.BlockTypes.FARMLAND;
import static org.cloudburstmc.api.block.BlockTypes.FIRE;
import static org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes.*;
import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.*;

@Log4j2
public abstract class CloudEntity implements Entity {

    protected static final int PORTAL_TRANSFER_TICKS = 80;
    protected static final int PORTAL_COOLDOWN_TICKS = 300;
    private static final int DEFAULT_MAX_FREEZE_TICKS = 140;
    private static final int MAX_SCOREBOARD_TAGS = 1024;

    protected final Set<CloudPlayer> hasSpawned = ConcurrentHashMap.newKeySet();

    protected final Reference2ObjectOpenHashMap<EffectType, Effect> effects = new Reference2ObjectOpenHashMap<>();
    protected final List<Entity> passengers = new ArrayList<>();
    private final long runtimeId = CloudEntityRegistry.get().newEntityId();
    protected final SyncedEntityData data = new SyncedEntityData(this::onDataChange);
    private final EntityType<?> type;
    public CloudChunk chunk;
    public NbtMap tag;
    public float highestPosition;
    public boolean firstMove = true;
    protected Vector3f position = Vector3f.ZERO;
    protected Vector3f lastPosition = Vector3f.ZERO;
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
    public int portalCooldown = 0;
    public boolean pendingPortalTransfer = false;
    public Vector3i portalEntryBlock = null;
    public float scale = 1;
    protected BoundingBox boundingBox;
    public boolean isCollided = false;
    public boolean isCollidedHorizontally = false;
    public boolean isCollidedVertically = false;
    public boolean verticalCollisionBelow = false;
    protected Optional<Vector3i> supportingBlockPosition = Optional.empty();
    boolean onGroundNoBlocks = false;
    public int noDamageTicks;
    public boolean justCreated;
    public boolean fireProof;
    public boolean invulnerable;
    protected boolean noPhysics;
    protected CloudLevel level;
    public boolean closed = false;
    protected Entity vehicle;
    protected EntityDamageEvent lastDamageCause = null;
    private final Set<String> tags = new LinkedHashSet<>();
    protected int age = 0;
    protected float health = 20;
    protected float absorption = 0;
    protected float ySize = 0;
    protected boolean isStatic = false;
    protected CloudServer server;
    protected Timing timing;
    protected boolean isPlayer = false;
    private int maxHealth = 20;
    private int freezeTicks;
    private boolean freezeTickingLocked;
    private boolean fromBucket;
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
        this.type = type;
        if (this instanceof CloudPlayer) {
            return;
        }

        this.init(location);
    }

    public float getHeight() {
        return 0;
    }

    public float getEyeHeight() {
        return this.getHeight() / 2 + 0.1f;
    }

    public float getWidth() {
        return 0;
    }

    public float getLength() {
        return 0;
    }

    protected float getStepHeight() {
        return 0;
    }

    public boolean canCollide() {
        return true;
    }

    public float getGravity() {
        return 0;
    }

    public float getDrag() {
        return 0;
    }

    public float getBaseOffset() {
        return 0;
    }

    /**
     * Returns the number of ticks this entity must wait after a portal transfer
     * before it can use another portal.
     */
    public int getPortalCooldownTicks() {
        return PORTAL_COOLDOWN_TICKS;
    }

    /**
     * Returns the number of ticks the entity must spend inside a portal before
     * being transferred.
     */
    protected int getPortalTransitionTicks() {
        return PORTAL_TRANSFER_TICKS;
    }

    protected void tickPortalCooldown() {
        this.portalCooldown--;
    }

    protected void onInsidePortal() {
        if (this.getVehicle() == null) {
            if (this.portalCooldown > 0) {
                this.portalCooldown = getPortalCooldownTicks();
            } else {
                this.inPortalTicks = PORTAL_TRANSFER_TICKS;
            }
        }
    }

    protected void initEntity() {
        this.data.setFlag(HAS_COLLISION, true);
        this.data.set(AIR_SUPPLY, (short) 400);
        this.data.set(AIR_SUPPLY_MAX, (short) 400);
        this.data.set(LEASH_HOLDER, -1L);
        this.data.set(SCALE, 1f);
        this.data.set(FREEZING_EFFECT_STRENGTH, 0f);
        this.updateNetworkBounds();
        this.data.set(STRUCTURAL_INTEGRITY, (int) this.getHealth());

        this.scheduleUpdate();
    }

    public EntityType<?> getType() {
        return type;
    }

    @Override
    public CloudLevel getLevel() {
        return level;
    }

    public CloudChunk getChunk() {
        return chunk;
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
    public float getPitch() {
        return pitch;
    }

    @Override
    public float getYaw() {
        return yaw;
    }

    // @Override
    public void loadAdditionalData(NbtMap tag) {
        this.tag = tag;

        tag.listenForList("Pos", NbtType.FLOAT, list -> {
            this.setPosition(Vector3f.from(list.get(0), list.get(1), list.get(2)));
        });
        tag.listenForList("Rotation", NbtType.FLOAT, list -> {
            this.setRotation(list.get(0), list.get(1));
        });
        tag.listenForList("Motion", NbtType.FLOAT, list -> {
            this.setMotion(Vector3f.from(list.get(0), list.get(1), list.get(2)));
        });

//        this.highestPosition = this.y + this.namedTag.getFloat("FallDistance");
        tag.listenForFloat("FallDistance", this::setFallDistance);

        tag.listenForShort("Fire", this::setOnFire);

        tag.listenForShort("Air", this::setAir);

        tag.listenForBoolean("OnGround", this::setOnGround);

        tag.listenForBoolean("Invulnerable", this::setInvulnerable);
        tag.listenForInt("TicksFrozen", this::setFreezeTicks);

        if (this instanceof Bucketable) {
            tag.listenForBoolean("FromBucket", this::setFromBucket);
        }

        tag.listenForFloat("scale", this::setScale);

        if (tag.containsKey("ActiveEffects")) {
            List<NbtMap> effects = tag.getList("ActiveEffects", NbtType.COMPOUND);
            for (NbtMap e : effects) {
                this.addEffect(CloudEffect.fromNBT(e));
            }
        }

        tag.listenForString("CustomName", this::setNameTag);
        tag.listenForBoolean("CustomNameVisible", this::setNameTagVisible);
        tag.listenForBoolean("CustomNameAlwaysVisible", this::setNameTagAlwaysVisible);
        this.tags.clear();
        if (tag.containsKey("Tags")) {
            for (String entityTag : tag.getList("Tags", NbtType.STRING)) {
                if (!entityTag.isBlank() && this.tags.size() < MAX_SCOREBOARD_TAGS) {
                    this.tags.add(entityTag);
                }
            }
        }
    }

    // @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        if (this.tag != null && !this.tag.isEmpty()) {
            tag.putAll(this.tag);
        }

        if (this.hasNameTag()) {
            tag.putString("CustomName", this.getNameTag());
            tag.putBoolean("CustomNameVisible", this.isNameTagVisible());
            tag.putBoolean("CustomNameAlwaysVisible", this.isNameTagAlwaysVisible());
        }

        tag.putList("Tags", NbtType.STRING, List.copyOf(this.tags));

        if (!(this instanceof CloudPlayer)) {
            tag.putString("identifier", this.type.getId().toString());
        }

        tag.putList("Pos", NbtType.FLOAT, Arrays.asList(
                this.position.getX(),
                this.position.getY(),
                this.position.getZ())
        );

        tag.putList("Motion", NbtType.FLOAT, Arrays.asList(
                this.motion.getX(),
                this.motion.getY(),
                this.motion.getZ())
        );

        tag.putList("Rotation", NbtType.FLOAT, Arrays.asList(
                this.yaw,
                this.pitch)
        );

        tag.putFloat("FallDistance", this.fallDistance);
        tag.putShort("Fire", (short) this.fireTicks);
        tag.putShort("Air", this.data.get(AIR_SUPPLY));
        tag.putBoolean("OnGround", this.onGround);
        tag.putBoolean("Invulnerable", this.invulnerable);

        if (this.freezeTicks > 0) {
            tag.putInt("TicksFrozen", this.freezeTicks);
        }

        if (this instanceof Bucketable) {
            tag.putBoolean("FromBucket", this.fromBucket);
        }

        tag.putFloat("Scale", this.scale);

        if (!this.effects.isEmpty()) {
            List<NbtMap> list = new ArrayList<>();
            for (Effect effect : this.effects.values()) {
                list.add(((CloudEffect) effect).createTag());
            }

            tag.putList("ActiveEffects", NbtType.COMPOUND, list);
        }
    }

    public SyncedEntityData getData() {
        return this.data;
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

    @Override
    public Set<String> getScoreboardTags() {
        return Set.copyOf(this.tags);
    }

    @Override
    public boolean hasScoreboardTag(String tag) {
        return this.tags.contains(tag);
    }

    @Override
    public boolean addScoreboardTag(String tag) {
        String value = Objects.requireNonNull(tag, "tag");
        if (value.isBlank()) {
            throw new IllegalArgumentException("tag cannot be blank");
        }
        return this.tags.size() < MAX_SCOREBOARD_TAGS && this.tags.add(value);
    }

    @Override
    public boolean removeScoreboardTag(String tag) {
        return this.tags.remove(Objects.requireNonNull(tag, "tag"));
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
        return this.data.get(NAMETAG_ALWAYS_SHOW) == 1;
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

    public float getScale() {
        return this.scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
        this.data.set(SCALE, this.scale);
        this.recalculateBoundingBox();
    }

    public short getAir() {
        return this.data.get(AIR_SUPPLY);
    }

    public void setAir(short air) {
        this.data.set(AIR_SUPPLY, air);
    }

    public boolean isInvulnerable() {
        return invulnerable;
    }

    public void setInvulnerable(boolean invulnerable) {
        this.invulnerable = invulnerable;
    }

    public List<Entity> getPassengers() {
        return passengers;
    }

    public Entity getPassenger() {
        return Iterables.getFirst(this.passengers, null);
    }

    public boolean isPassenger(Entity entity) {
        return this.passengers.contains(entity);
    }

    public boolean isControlling(Entity entity) {
        return this.passengers.indexOf(entity) == 0;
    }

    public boolean hasControllingPassenger() {
        return !this.passengers.isEmpty() && isControlling(this.passengers.get(0));
    }

    public Entity getVehicle() {
        return vehicle;
    }

    @Override
    public Map<EffectType, Effect> getEffects() {
        return effects;
    }

    @Override
    public void removeAllEffects() {
        for (Effect effect : this.effects.values()) {
            this.removeEffect(effect.getType());
        }
    }

    @Deprecated
    @Override
    public void removeEffect(int effectId) {
        removeEffect(NetworkUtils.effectFromLegacy((byte) effectId));
    }

    @Override
    public void removeEffect(EffectType type) {
        if (this.effects.containsKey(type)) {
            Effect effect = this.effects.remove(type);
            effect.remove(this);

            this.recalculateEffectColor();
        }
    }

    @Deprecated
    @Override
    public Effect getEffect(int effectId) {
        EffectType type = NetworkUtils.effectFromLegacy((byte) effectId);
        return this.effects.getOrDefault(type, null);
    }

    @Nullable
    @Override
    public CloudEffect getEffect(EffectType type) {
        return (CloudEffect) this.effects.getOrDefault(type, null);
    }

    @Deprecated
    @Override
    public boolean hasEffect(int effectId) {
        return this.hasEffect(NetworkUtils.effectFromLegacy((byte) effectId));
    }

    @Override
    public boolean hasEffect(EffectType type) {
        return this.effects.containsKey(type);
    }

    @Override
    public void addEffect(Effect effect) {
        if (effect == null) {
            return; //here add null means add nothing
        }

        effect.add(this);

        this.effects.put(effect.getType(), effect);

        this.recalculateEffectColor();

        if (effect.getType() == EffectTypes.HEALTH_BOOST) {
            this.setHealth(this.getHealth() + 4 * (effect.getAmplifier() + 1));
        }

    }

    public void recalculateBoundingBox() {
        float height = this.getHeight() * this.scale;
        float radius = (this.getWidth() * this.scale) / 2;
        this.boundingBox = new BoundingBox(this.position.getX() - radius, this.position.getY(), this.position.getZ() - radius,
                this.position.getX() + radius, this.position.getY() + height, this.position.getZ() + radius);

        this.updateNetworkBounds();
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

    protected void recalculateEffectColor() {
        int[] color = new int[3];
        int count = 0;
        for (Effect effect : this.effects.values()) {
            if (effect.isVisible()) {
                int[] c = effect.getColor();
                color[0] += c[0] * (effect.getAmplifier() + 1);
                color[1] += c[1] * (effect.getAmplifier() + 1);
                color[2] += c[2] * (effect.getAmplifier() + 1);
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

    protected final void init(Location location) {
        if (location == null) {
            throw new IllegalArgumentException("Invalid garbage Location given to Entity");
        }

        if (this.initialized) {
            // We've already initialized this entity
            return;
        }
        this.initialized = true;

        this.timing = Timings.getEntityTiming(this.getType());

        this.isPlayer = this instanceof CloudPlayer;

        this.justCreated = true;

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

    //@Override
    public NbtMap getTag() {
        return tag;
    }

    public String getName() {
        if (this.hasNameTag()) {
            return this.getNameTag();
        } else {
            // FIXME: 04/01/2020 Use language files
            return CloudEntityRegistry.get().getLegacyName(this.type.getId());
        }
    }

    @Override
    public boolean spawn() {
        if (this.closed || this.spawned) {
            return false;
        }

        EntitySpawnEvent event;
        if (this instanceof org.cloudburstmc.api.entity.misc.DroppedItem droppedItem) {
            event = new ItemSpawnEvent(droppedItem);
        } else if (this instanceof Projectile projectile) {
            event = new ProjectileLaunchEvent(projectile);
        } else {
            event = new EntitySpawnEvent(this);
        }

        this.server.getEventManager().fire(event);
        if (event.isCancelled()) {
            this.closed = true;
            return false;
        }

        this.registerInLevel(this.getLocation());
        return true;
    }

    private void registerInLevel(Location location) {
        if (this.spawned) {
            return;
        }

        this.spawned = true;
        this.level.addEntity(this);
        this.scheduleUpdate();

        this.level.getChunkFuture(location.getChunkX(), location.getChunkZ()).whenComplete((chunk, throwable) -> {
            if (throwable != null || this.closed || !this.spawned) {
                return;
            }

            this.chunk = chunk;
            chunk.addEntity(this);
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
        boolean added = sent && this.getViewers().add(player);
        if (!sent || !added) {
            // chunk not yet received by client, or entity already spawned
            return;
        }

        player.sendPacket(createAddEntityPacket());

        if (this.vehicle != null) {
            this.vehicle.spawnTo(player);

            SetEntityLinkPacket packet = new SetEntityLinkPacket();
            packet.setEntityLink(new EntityLinkData(this.vehicle.getUniqueId(),
                    this.getUniqueId(), EntityLinkData.Type.RIDER, true, false, 0));

            player.sendPacket(packet);
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
        this.data.putAllIn(addEntity.getMetadata());

        for (int i = 0; i < this.passengers.size(); i++) {
            addEntity.getEntityLinks().add(new EntityLinkData(this.getUniqueId(),
                    this.passengers.get(i).getUniqueId(), i == 0 ? EntityLinkData.Type.RIDER : EntityLinkData.Type.PASSENGER, false, false, 0));
        }
        return addEntity;
    }

    public Set<CloudPlayer> getViewers() {
        return hasSpawned;
    }

    public void sendPotionEffects(CloudPlayer player) {
        for (Effect effect : this.effects.values()) {
            MobEffectPacket pk = new MobEffectPacket();
            pk.setRuntimeEntityId(this.getRuntimeId());
            pk.setEffectId(NetworkUtils.effectToNetwork(effect.getType()));
            pk.setAmplifier(effect.getAmplifier());
            pk.setParticles(effect.isVisible());
            pk.setDuration(effect.getDuration());
            pk.setEvent(MobEffectPacket.Event.ADD);

            player.sendPacket(pk);
        }
    }

    private void onDataChange(EntityDataMap changeSet) {
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
        this.data.putAllIn(packet.getMetadata());
        player.sendPacket(packet);
    }

    private void sendDataToViewers(EntityDataMap map) {
        SetEntityDataPacket packet = new SetEntityDataPacket();
        packet.setRuntimeEntityId(this.getRuntimeId());
        packet.getMetadata().putAll(map);

        CloudServer.broadcastPacket(this.getViewers(), packet);
    }

    public void sendData(CloudPlayer player, EntityDataType<?>... data) {
        SetEntityDataPacket packet = new SetEntityDataPacket();
        packet.setRuntimeEntityId(this.getRuntimeId());
        for (EntityDataType<?> entityData : data) {
            packet.getMetadata().put(entityData, this.data.get(entityData));
        }

        player.sendPacket(packet);
    }

    public void sendFlags(CloudPlayer player) {
        SetEntityDataPacket packet = new SetEntityDataPacket();
        packet.setRuntimeEntityId(this.getRuntimeId());
        this.data.putFlagsIn(packet.getMetadata());
        if (this.isPlayer) {
            this.putNetworkBounds(packet.getMetadata());
        }

        player.sendPacket(packet);
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

    public boolean attack(EntityDamageEvent source) {
        if (hasEffect(EffectTypes.FIRE_RESISTANCE) && source.getDamageType().is(DamageTypeTags.IS_FIRE)) {
            return false;
        }

        getServer().getEventManager().fire(source);
        if (source.isCancelled()) {
            return false;
        }
        setLastDamageCause(source);
        float absorbed = Math.min(this.getAbsorption(), source.getDamage());
        this.setAbsorption(this.getAbsorption() - absorbed);
        setHealth(getHealth() - (source.getDamage() - absorbed));
        return true;
    }

    public boolean attack(float damage) {
        return this.attack(new EntityDamageEvent(this, DamageTypes.CUSTOM, damage));
    }

    public void heal(EntityRegainHealthEvent source) {
        this.server.getEventManager().fire(source);
        if (source.isCancelled()) {
            return;
        }
        this.setHealth(this.getHealth() + source.getAmount());
    }

    public void heal(float amount) {
        this.heal(new EntityRegainHealthEvent(this, amount, EntityRegainHealthEvent.CAUSE_REGEN));
    }

    public float getHealth() {
        return health;
    }

    public void setHealth(float health) {
        if (this.health == health) {
            return;
        }

        if (health < 1) {
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

    public boolean isAlive() {
        return this.health > 0;
    }

    public boolean isClosed() {
        return closed;
    }

    public EntityDamageEvent getLastDamageCause() {
        return lastDamageCause;
    }

    public void setLastDamageCause(EntityDamageEvent type) {
        this.lastDamageCause = type;
    }

    public int getMaxHealth() {
        return maxHealth + (this.hasEffect(EffectTypes.HEALTH_BOOST) ? 4 * (this.getEffect(EffectTypes.HEALTH_BOOST).getAmplifier() + 1) : 0);
    }

    public void setMaxHealth(int maxHealth) {
        this.maxHealth = maxHealth;
    }

    @Override
    public int getFreezeTicks() {
        return this.freezeTicks;
    }

    @Override
    public void setFreezeTicks(int ticks) {
        int clampedTicks = Math.clamp(ticks, 0, this.getMaxFreezeTicks());
        if (this.freezeTicks == clampedTicks) {
            return;
        }
        this.freezeTicks = clampedTicks;
        this.data.set(FREEZING_EFFECT_STRENGTH, this.freezeTicks / (float) this.getMaxFreezeTicks());
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

    public boolean canCollideWith(Entity entity) {
        return !this.justCreated
                && entity != null
                && entity.canBeCollidedWith(this)
                && !this.isPassengerOfSameVehicle(entity);
    }

    public boolean canBeCollidedWith(@Nullable Entity entity) {
        return false;
    }

    public boolean isPushable() {
        return false;
    }

    protected boolean isPassengerOfSameVehicle(Entity entity) {
        return this.getVehicle() != null && this.getVehicle() == entity.getVehicle();
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

    public boolean entityBaseTick() {
        return this.entityBaseTick(1);
    }

    public boolean entityBaseTick(int tickDiff) {
        try (Timing ignored = Timings.entityBaseTickTimer.startTiming()) {

            this.justCreated = false;

            if (!this.isAlive()) {
                this.removeAllEffects();
                this.despawnFromAll();
                if (!this.isPlayer) {
                    this.close();
                }
                return false;
            }
            if (vehicle != null && !vehicle.isAlive() && vehicle instanceof Rideable) {
                this.mount(vehicle);
            }

            updatePassengers();

            if (!this.effects.isEmpty()) {
                for (Effect effect : this.effects.values()) {
                    if (effect.canTick()) {
                        effect.applyEffect(this);
                    }
                    effect.setDuration(effect.getDuration() - tickDiff);

                    if (effect.getDuration() <= 0) {
                        this.removeEffect(effect.getType());
                    }
                }
            }

            boolean hasUpdate = false;

            this.checkBlockCollision();
            this.applyLiquidCurrent();

            if (this.position.getY() <= -16 && this.isAlive()) {
                if (this instanceof CloudPlayer player) {
                    if (player.getGameMode() != GameMode.CREATIVE)
                        this.attack(new EntityDamageEvent(this, DamageTypes.VOID, 10));
                } else {
                    this.attack(new EntityDamageEvent(this, DamageTypes.VOID, 10));
                    hasUpdate = true;
                }
            }

            if (this.fireTicks > 0) {
                if (this.fireProof) {
                    this.fireTicks -= 4 * tickDiff;
                    if (this.fireTicks < 0) {
                        this.fireTicks = 0;
                    }
                } else {
                    if (!this.hasEffect(EffectTypes.FIRE_RESISTANCE) && ((this.fireTicks % 20) == 0 || tickDiff > 20)) {
                        this.attack(new EntityDamageEvent(this, DamageTypes.FIRE_TICK, 1));
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

            if (this.portalCooldown > 0) {
                tickPortalCooldown();
            } else {
                int portalThreshold = getPortalTransitionTicks();
                if (this.inPortalTicks > 0 && (portalThreshold == 0 || this.inPortalTicks >= portalThreshold)) {
                    EntityPortalEnterEvent ev = new EntityPortalEnterEvent(this, EntityPortalEnterEvent.PortalType.NETHER);
                    getServer().getEventManager().fire(ev);

                    if (!ev.isCancelled()) {
                        this.portalCooldown = getPortalCooldownTicks();
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

            this.age += tickDiff;
            this.ticksLived += tickDiff;
            TimingsHistory.activatedEntityTicks++;

            return hasUpdate;
        }
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

    public void addMotion(Vector3f motion) {
        SetEntityMotionPacket packet = new SetEntityMotionPacket();
        packet.setRuntimeEntityId(this.getRuntimeId());
        packet.setMotion(motion);
        if (this.isPlayer) {
            packet.setTick(((CloudPlayer) this).getClientTick());
        }

        CloudServer.broadcastPacket(this.hasSpawned, packet);
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

    public Direction getHorizontalDirection() {
        return Direction.fromYaw(this.yaw);
    }

    public CardinalDirection getCardinalDirection() {
        return CardinalDirection.values()[GenericMath.floor((((this.yaw + 180) % 360) / 22.5))];
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
        hasUpdate |= CloudEntityRegistry.get().requireComponent(this.type, EntityComponents.ON_TICK)
                .execute(this, currentTick);

        this.updateMovement();

        this.data.update();

        return hasUpdate;
    }

    /**
     * Mount or dismounts an Entity from a/into vehicle
     *
     * @param vehicle The target Entity
     * @return {@code true} if the mounting successful
     */
    @Override
    public boolean mount(Entity vehicle, MountType mode) {
        checkNotNull(vehicle, "The target of the mounting entity can't be null");

        if (this.vehicle != null && !this.vehicle.dismount(this)) {
            return false;
        }

        // Entity entering a vehicle
        EntityVehicleEnterEvent ev = new EntityVehicleEnterEvent(this, (Vehicle) vehicle);
        server.getEventManager().fire(ev);
        if (ev.isCancelled()) {
            return false;
        }

        // Add variables to entity
        this.vehicle = vehicle;

        vehicle.onMount(this); // Flags have to be set before
//        this.data.setFlag(RIDING, true);
        this.data.update(); // force any data that needs to be sent
        broadcastLinkPacket(vehicle, EntityLinkData.Type.byId(mode.ordinal()));
        onMountComplete(vehicle);

        return true;
    }

    protected void onMountComplete(Entity vehicle) {
    }

    public boolean dismount(Entity vehicle) {
        if (this.vehicle == null) {
            // Not in a vehicle
            return false;
        }

        // Run the events
        EntityVehicleExitEvent event = new EntityVehicleExitEvent(this, (Vehicle) vehicle);
        server.getEventManager().fire(event);
        if (event.isCancelled()) {
            return false;
        }

        broadcastLinkPacket(vehicle, EntityLinkData.Type.REMOVE);

        // Refurbish the entity
        this.vehicle = null;
//        vehicle.setFlag(RIDING, false);
        vehicle.onDismount(this);

        this.setSeatPosition(Vector3f.ZERO);
        this.data.setFlag(MOVING, true);

        return true;
    }

    @Override
    public void onMount(Entity passenger) {
        checkArgument(passenger.getVehicle() == this, "passenger is not in this vehicle");
        checkArgument(this.passengers.add(passenger), "passenger is already mounted to this vehicle");

        if (passenger instanceof CloudPlayer ridingPlayer) {
            this.hasSpawned.add(ridingPlayer);
        }

        Vector3f seatOffset = this.getMountedOffset(passenger);
        passenger.setSeatPosition(seatOffset);
        ((CloudEntity) passenger).data.set(SEAT_LOCK_RIDER_ROTATION_DEGREES, 181.0f);
        this.updatePassengerPosition(passenger);
    }

    @Override
    public void onDismount(Entity passenger) {
        checkArgument(passenger.getVehicle() != this, "passenger is still mounted");
        checkArgument(this.passengers.remove(passenger), "passenger is not in this vehicle");

        if (passenger instanceof CloudPlayer ridingPlayer) {
            this.hasSpawned.remove(ridingPlayer);
        }

        passenger.setSeatPosition(Vector3f.ZERO);
        ((CloudEntity) passenger).data.set(SEAT_LOCK_RIDER_ROTATION_DEGREES, 0.0f);
    }

    protected void broadcastLinkPacket(Entity vehicle, EntityLinkData.Type type) {
        SetEntityLinkPacket packet = new SetEntityLinkPacket();
        boolean riderInitiated = type == EntityLinkData.Type.RIDER || type == EntityLinkData.Type.PASSENGER;
        packet.setEntityLink(new EntityLinkData(vehicle.getUniqueId(), getUniqueId(), type, false, riderInitiated, 0));
        CloudServer.broadcastPacket(((CloudEntity) vehicle).getViewers(), packet);
    }

    public void updatePassengers() {
        if (this.passengers.isEmpty()) {
            return;
        }

        for (Entity passenger : new ArrayList<>(this.passengers)) {
            if (!passenger.isAlive()) {
                dismount(passenger);
                continue;
            }

            updatePassengerPosition(passenger);
        }
    }

    protected void updatePassengerPosition(Entity passenger) {
        passenger.setPosition(this.getPosition().add(this.getPassengerAttachmentPoint(passenger)));
    }

    public Vector3f getSeatPosition() {
        return this.data.get(SEAT_OFFSET);
    }

    public void setSeatPosition(Vector3f pos) {
        this.data.set(SEAT_OFFSET, pos);
    }

    public Vector3f getMountedOffset(Entity passenger) {
        float yOffset = getMountedHeightOffset() + passenger.getPassengerHeightOffset();
        return Vector3f.from(0f, yOffset, 0f);
    }

    public final void scheduleUpdate() {
        this.level.scheduleEntityUpdate(this);
    }

    @Override
    public int getNoDamageTicks() {
        return noDamageTicks;
    }

    public void setNoDamageTicks(int noDamageTicks) {
        this.noDamageTicks = noDamageTicks;
    }

    @Override
    public int getFireTicks() {
        return fireTicks;
    }

    @Override
    public void setOnFire(int seconds) {
        int ticks = seconds * 20;
        if (ticks > this.fireTicks) {
            this.fireTicks = ticks;
        }
    }

    public float getAbsorption() {
        return absorption;
    }

    public void setAbsorption(float absorption) {
        if (absorption != this.absorption) {
            this.absorption = absorption;
            if (this instanceof CloudPlayer)
                ((CloudPlayer) this).setAttribute(Attribute.getAttribute(Attribute.ABSORPTION).setValue(absorption));
        }
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

    public void extinguish() {
        this.fireTicks = 0;
        this.data.setFlag(ON_FIRE, false);
    }

    public boolean canTriggerWalking() {
        return true;
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
        this.highestPosition = 0;
    }

    protected void updateFallState(boolean onGround) {
        if (onGround) {
            fallDistance = this.highestPosition - this.getY();

            if (fallDistance > 0) {
                // check if we fell into at least 1 block of water
                if (this instanceof EntityLiving) {
                    var liquid = this.level.getBlock(this.position.toInt()).getLiquid().getType();

                    if (!liquid.isSameFamily(LiquidTypes.WATER)) {
                        this.fall(fallDistance);
                    }
                }
                this.resetFallDistance();
            }
        }
    }

    public BoundingBox getBoundingBox() {
        return this.boundingBox;
    }

    public void fall(float fallDistance) {
        Block down = this.level.getBlock(Direction.DOWN.getUnitVector().add(this.getPosition().toInt()));
        down.requireComponent(BlockComponents.ON_FALL_ON).execute(down, this, fallDistance);

        if (fallDistance > 0.75 && down.getState().getType() == FARMLAND) {
            Event ev;

            if (this instanceof CloudPlayer) {
                ev = new PlayerInteractEvent((Player) this, null, down, null, PlayerInteractEvent.Action.PHYSICAL);
            } else {
                ev = new EntityInteractEvent(this, down);
            }

            this.server.getEventManager().fire(ev);
            if (ev.isCancelled()) {
                return;
            }
            this.level.setBlockState(down.getPosition(), BlockStates.DIRT, false, true);
        }
    }

    public void applyFallDamage(float fallDistance) {
        if (this.hasEffect(EffectTypes.SLOW_FALLING)) {
            return;
        }

        if (this.isPlayer && !level.getGameRules().get(GameRules.FALL_DAMAGE)) {
            return;
        }

        float damage = (float) Math.floor(fallDistance - 3 - (this.hasEffect(EffectTypes.JUMP_BOOST) ? this.getEffect(EffectTypes.JUMP_BOOST).getAmplifier() + 1 : 0));

        if (damage > 0) {
            this.attack(new EntityDamageEvent(this, DamageTypes.FALL, damage));
        }
    }

    public void handleLavaMovement() {
        //todo
    }

    public void onCollideWithPlayer(EntityHuman entityPlayer) {

    }

    @Override
    public void onEntityCollision(Entity entity) {
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

    public void onStruckByLightning(LightningBolt lightningBolt) {
        DamageSource source = DamageSource.builder(DamageTypes.LIGHTNING)
                .directEntity(lightningBolt).causingEntity(lightningBolt).location(lightningBolt.getLocation()).build();
        if (this.attack(new EntityDamageEvent(this, source, 5))) {
            if (this.fireTicks < 8 * 20) {
                this.setOnFire(8);
            }
        }
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

    public boolean isFromBucket() {
        return this.fromBucket;
    }

    public void setFromBucket(boolean fromBucket) {
        this.fromBucket = fromBucket;
    }

    protected boolean switchLevel(CloudLevel targetLevel) {
        checkNotNull(targetLevel, "targetLevel");
        if (this.closed) {
            return false;
        }

        EntityLevelChangeEvent ev = new EntityLevelChangeEvent(this, this.level, targetLevel);
        this.server.getEventManager().fire(ev);
        if (ev.isCancelled()) {
            return false;
        }

        this.level.removeEntity(this);
        if (this.chunk != null) {
            this.chunk.removeEntity(this);
        }
        this.despawnFromAll();

        this.level = targetLevel;
        this.level.addEntity(this);
        this.chunk = null;

        return true;
    }

    public Vector3f getPosition() {
        return this.position;
    }

    public Location getLocation() {
        return Location.from(this.position, this.yaw, this.pitch, this.level);
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

    public boolean move(Vector3f d) {
        return this.move(MovementType.SELF, d);
    }

    public boolean move(float dx, float dy, float dz) {
        return this.move(MovementType.SELF, dx, dy, dz);
    }

    public boolean move(MovementType type, Vector3f movement) {
        return this.move(type, movement.getX(), movement.getY(), movement.getZ());
    }

    public boolean move(MovementType type, float dx, float dy, float dz) {
        return EntityMovementController.move(this, type, dx, dy, dz);
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

    protected void setOnGroundWithMovement(boolean onGround, boolean horizontalCollision, @Nullable Vector3f movement) {
        EntityMovementController.setOnGroundWithMovement(this, onGround, horizontalCollision, movement);
    }

    /**
     * Returns whether this entity can be moved by currents in liquids.
     *
     * @return boolean
     */
    public boolean canBeMovedByCurrents() {
        return true;
    }

    private void applyLiquidCurrent() {
        boolean movesWithCurrent = this.canBeMovedByCurrents();
        BoundingBox box = this.getBoundingBox().deflate(0.001f, 0.001f, 0.001f);

        int minX = GenericMath.floor(box.getMinX());
        int maxX = GenericMath.floor(box.getMaxX());
        int minY = GenericMath.floor(box.getMinY());
        int maxY = GenericMath.floor(box.getMaxY());
        int minZ = GenericMath.floor(box.getMinZ());
        int maxZ = GenericMath.floor(box.getMaxZ());

        Vector3f total = Vector3f.ZERO;
        int count = 0;
        boolean touchingWater = false;

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = this.level.getBlock(x, y, z);
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

                    if (movesWithCurrent) {
                        total = total.add(this.level.getLiquidFlow(block.getPosition()));
                        count++;
                    }
                }
            }
        }

        if (count > 0 && total.lengthSquared() > 0) {
            this.motion = this.motion.add(total.normalize().mul(0.014f));
        }

        if (touchingWater && this.fireTicks > 0) {
            this.extinguish();
        }
    }

    protected void checkBlockCollision() {
        EntityInsideBlockScanner.scan(this);
    }

    public boolean setPositionAndRotation(Vector3f pos, float yaw, float pitch) {
        if (this.setPosition(pos)) {
            this.setRotation(yaw, pitch);
            return true;
        }

        return false;
    }

    public void setRotation(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
        this.scheduleUpdate();
    }

    public boolean canTriggerPressurePlate() {
        return true;
    }

    protected void checkChunks() {
        Vector3f pos = this.getPosition();
        if (this.chunk == null || (this.chunk.getX() != pos.getFloorX() >> 4 || this.chunk.getZ() != pos.getFloorZ() >> 4)) {
            if (this.chunk != null) {
                this.chunk.removeEntity(this);
            }
            this.chunk = this.level.getLoadedChunk(pos);
            if (chunk == null) {
                return; // Converter will throw NPE otherwise.
            }

            if (!this.justCreated) {
                Set<CloudPlayer> viewers = chunk.getViewers();
                for (Player player : this.hasSpawned) {
                    if (!viewers.contains(player)) {
                        this.despawnFrom(player);
                    } else {
                        viewers.remove(player);
                    }
                }

                for (Player player : viewers) {
                    this.spawnTo(player);
                }
            }

            if (this.chunk == null) {
                return;
            }

            this.chunk.addEntity(this);
        }
    }

    public boolean setPosition(Vector3f pos) {
        checkNotNull(pos, "position");
        if (this.closed) {
            return false;
        }

        this.position = pos;

        this.recalculateBoundingBox();

        this.checkChunks();

        return true;
    }

    public Vector3f getMotion() {
        return this.motion;
    }

    public boolean setMotion(Vector3f motion) {
        if (!this.justCreated) {
            EntityMotionEvent ev = new EntityMotionEvent(this, motion);
            this.server.getEventManager().fire(ev);
            if (ev.isCancelled()) {
                return false;
            }
        }

        this.motion = motion;

        if (!this.justCreated) {
            this.updateMovement();
        }

        return true;
    }

    @Override
    public void makeStuckInBlock(BlockState state, Vector3f speedMultiplier) {
        this.resetFallDistance();
        this.stuckSpeedMultiplier = speedMultiplier;
    }

    public boolean isOnGround() {
        return onGround;
    }

    @Override
    public void setOnGround(boolean onGround) {
        this.setOnGroundWithMovement(onGround, this.isCollidedHorizontally, null);
    }

    @Override
    public Optional<Vector3i> getSupportingBlockPosition() {
        return this.supportingBlockPosition;
    }

    public void kill() {
        this.health = 0;
        this.scheduleUpdate();

        for (Entity passenger : new ArrayList<>(this.passengers)) {
            passenger.dismount(this);
        }
    }

    public boolean teleport(Vector3f pos) {
        return this.teleport(pos, PlayerTeleportEvent.TeleportCause.PLUGIN);
    }

    public boolean teleport(Vector3f pos, PlayerTeleportEvent.TeleportCause cause) {
        return this.teleport(Location.from(pos, this.yaw, this.pitch, this.level), cause);
    }

    public boolean teleport(Location location) {
        return this.teleport(location, PlayerTeleportEvent.TeleportCause.PLUGIN);
    }

    public boolean teleport(Location location, PlayerTeleportEvent.TeleportCause cause) {
        float yaw = location.getYaw();
        float pitch = location.getPitch();

        Location from = this.getLocation();
        Location to = location;
        if (cause != null) {
            EntityTeleportEvent ev = new EntityTeleportEvent(this, from, to);
            this.server.getEventManager().fire(ev);
            if (ev.isCancelled()) {
                return false;
            }
            to = ev.getTo();
        }

        if (from.getLevel() != to.getLevel() && !this.switchLevel((CloudLevel) to.getLevel())) {
            return false;
        }

        this.ySize = 0;

        this.setMotion(Vector3f.ZERO);

        if (this.setPositionAndRotation(to.getPosition(), yaw, pitch)) {
            this.resetFallDistance();
            this.onGround = true;

            this.updateMovement();

            return true;
        }

        return false;
    }

    public long getUniqueId() {
        return this.runtimeId;
    }

    public long getRuntimeId() {
        return this.runtimeId;
    }

    public void respawnToAll() {
        for (Player player : this.hasSpawned) {
            this.spawnTo(player);
        }
        this.hasSpawned.clear();
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

    public void despawnFromAll() {
        for (Player player : this.hasSpawned) {
            this.despawnFrom(player);
        }
    }

    public void close() {
        if (!this.closed) {
            this.closed = true;
            if (this.spawned) {
                this.server.getEventManager().fire(new EntityDespawnEvent(this));
            }

            this.despawnFromAll();
            if (this.chunk != null) {
                this.chunk.removeEntity(this);
            }

            if (this.level != null) {
                this.level.removeEntity(this);
            }
        }
    }

    @Nullable
    @Override
    public Entity getOwner() {
        if (this.data.contains(OWNER_EID)) {
            return this.level.getEntityByRuntimeId(this.data.get(OWNER_EID));
        }
        return null;
    }

    @Override
    public void setOwner(@Nullable Entity entity) {
        this.data.set(OWNER_EID, entity == null ? -1 : entity.getUniqueId());
    }

    @Override
    public CloudServer getServer() {
        return server;
    }

    @Override
    public String toString() {
        return "Entity(type=" + type.getId() + ", id=" + getUniqueId() + ")";
    }
}
