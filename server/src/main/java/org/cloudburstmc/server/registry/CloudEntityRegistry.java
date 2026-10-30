package org.cloudburstmc.server.registry;

import com.google.common.base.CaseFormat;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.api.block.LiquidTypes;
import org.cloudburstmc.api.entity.*;
import org.cloudburstmc.api.entity.data.AbstractArrowData;
import org.cloudburstmc.api.entity.data.AbstractMinecartData;
import org.cloudburstmc.api.entity.data.AreaEffectCloudData;
import org.cloudburstmc.api.entity.data.ArrowData;
import org.cloudburstmc.api.entity.data.BoatData;
import org.cloudburstmc.api.entity.data.ChestBoatData;
import org.cloudburstmc.api.entity.data.CreeperData;
import org.cloudburstmc.api.entity.data.CreatureData;
import org.cloudburstmc.api.entity.data.DroppedItemData;
import org.cloudburstmc.api.entity.data.EnderCrystalData;
import org.cloudburstmc.api.entity.data.EnderDragonData;
import org.cloudburstmc.api.entity.data.ExperienceOrbData;
import org.cloudburstmc.api.entity.data.FallingBlockData;
import org.cloudburstmc.api.entity.data.FireworksRocketData;
import org.cloudburstmc.api.entity.data.HappyGhastData;
import org.cloudburstmc.api.entity.data.PaintingData;
import org.cloudburstmc.api.entity.data.PrimedTntData;
import org.cloudburstmc.api.entity.data.ProjectileData;
import org.cloudburstmc.api.entity.data.SheepData;
import org.cloudburstmc.api.entity.data.SnowGolemData;
import org.cloudburstmc.api.entity.data.SplashPotionData;
import org.cloudburstmc.api.entity.data.ThrownTridentData;
import org.cloudburstmc.api.entity.data.TntMinecartData;
import org.cloudburstmc.api.entity.data.TurtleData;
import org.cloudburstmc.api.entity.data.VillagerData;
import org.cloudburstmc.api.entity.component.Buoyancy;
import org.cloudburstmc.api.entity.component.InteractEntityHandler;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.item.ItemBehaviors;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.item.component.SpawnEggComponent;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.registry.RegistryException;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.component.ComponentMap;
import org.cloudburstmc.nbt.NBTInputStream;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.nbt.NbtUtils;
import org.cloudburstmc.server.Bootstrap;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.entity.EntityHuman;
import org.cloudburstmc.server.entity.UnknownEntity;
import org.cloudburstmc.server.entity.component.BucketableEntityHandlers;
import org.cloudburstmc.server.entity.component.PickItemEntityHandlers;
import org.cloudburstmc.server.entity.component.SnowGolemEntityHandlers;
import org.cloudburstmc.server.entity.hostile.*;
import org.cloudburstmc.server.entity.misc.*;
import org.cloudburstmc.server.entity.passive.*;
import org.cloudburstmc.server.entity.projectile.*;
import org.cloudburstmc.server.entity.vehicle.*;
import org.cloudburstmc.server.registry.component.CloudComponentMap;
import tools.jackson.core.type.TypeReference;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.cloudburstmc.api.entity.EntityTypes.*;

@Log4j2
public class CloudEntityRegistry extends CloudComponentRegistry<EntityType<?>> implements org.cloudburstmc.api.registry.EntityRegistry {
    private static final CloudEntityRegistry INSTANCE;

    private static final BiMap<String, Identifier> LEGACY_NAMES;
    private static final Map<String, Identifier> CANONICAL_NAMES;
    private static final List<NbtMap> VANILLA_ENTITIES;

    private static final EntityData<UnknownEntity> UNKNOWN_ENTITY_DATA =
            new EntityData<>(false, new RegistryProvider<>(
                    new EntityFactoryData<>(UnknownEntity::new,
                            org.cloudburstmc.api.entity.data.EntityData.class), null, 0));

    static {
        try (InputStream stream = RegistryUtils.getOrAssertResource("legacy/entity_names.json")) {
            Map<String, String> legacyNames = Bootstrap.JSON_MAPPER.readValue(stream, new TypeReference<Map<String, String>>() {
            });

            ImmutableBiMap.Builder<String, Identifier> mapBuilder = ImmutableBiMap.builder();

            legacyNames.forEach((name, identifier) -> mapBuilder.put(name, Identifier.parse(identifier)));
            LEGACY_NAMES = mapBuilder.build();
        } catch (IOException e) {
            throw new AssertionError("Unable to load legacy entity names", e);
        }

        try (InputStream stream = RegistryUtils.getOrAssertResource("data/entity_identifiers.dat");
             NBTInputStream nbtInputStream = NbtUtils.createNetworkReader(stream)) {
            NbtMap tag = (NbtMap) nbtInputStream.readTag();
            VANILLA_ENTITIES = tag.getList("idlist", NbtType.COMPOUND);
        } catch (IOException e) {
            throw new AssertionError("Unable to close resource stream", e);
        }

        Map<String, Identifier> canonicalNames = new HashMap<>();
        for (EntityType<?> type : EntityTypes.values()) {
            Identifier id = type.getId();
            String name = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, id.getName());
            Identifier previous = canonicalNames.putIfAbsent(name, id);
            if (previous != null && !previous.equals(id)) {
                throw new IllegalStateException("Ambiguous canonical entity name: " + name);
            }
        }

        CANONICAL_NAMES = Map.copyOf(canonicalNames);
        INSTANCE = new CloudEntityRegistry();
    }

    private final BiMap<Identifier, EntityType<?>> identifierTypeMap = HashBiMap.create();
    private final AtomicLong entityIdAllocator = new AtomicLong();
    private final Map<EntityType<?>, EntityData<?>> dataMap = new IdentityHashMap<>();
    private final Int2ObjectMap<EntityType<?>> runtimeTypeMap = new Int2ObjectOpenHashMap<>();
    private final Object2IntMap<EntityType<?>> typeToRuntimeMap = new Object2IntLinkedOpenHashMap<>();
    private final int customEntityStart;
    private int runtimeTypeAllocator;
    private volatile boolean closed;
    @Getter
    private NbtMap entityIdentifiersPalette;

    private CloudEntityRegistry() {
        this.registerVanillaEntityComponents();
        this.registerVanillaEntities();
        this.registerBuoyancyComponents();
        this.registerPickItemComponents();
        this.registerProjectileDamageComponents();
        this.registerBucketableEntities();
        this.registerPowderSnowProperties();
        this.registerSnowGolemComponents();
        customEntityStart = runtimeTypeAllocator;
    }

    public static CloudEntityRegistry get() {
        return INSTANCE;
    }

    @Override
    public ComponentMap getComponents(EntityType<?> type) {
        return super.getComponents(type);
    }

    @Override
    public synchronized <T extends Entity> void register(EntityType<T> type, EntityFactory<T> factory,
                                                         int priority, boolean hasSpawnEgg) {
        this.register(type, factory, priority, hasSpawnEgg, org.cloudburstmc.api.entity.data.EntityData.class);
    }

    @Override
    public synchronized <T extends Entity> void register(EntityType<T> type, EntityFactory<T> factory,
                                                         int priority, boolean hasSpawnEgg,
                                                         Class<? extends org.cloudburstmc.api.entity.data.EntityData> dataClass) {
        this.registerInternal(null, type, factory, this.runtimeTypeAllocator++, priority, hasSpawnEgg, dataClass);
    }

    public synchronized <T extends Entity> void register(Object plugin, EntityType<T> type, EntityFactory<T> factory,
                                                         int priority, boolean hasSpawnEgg) {
        this.register(plugin, type, factory, priority, hasSpawnEgg,
                org.cloudburstmc.api.entity.data.EntityData.class);
    }

    public synchronized <T extends Entity> void register(Object plugin, EntityType<T> type, EntityFactory<T> factory,
                                                         int priority, boolean hasSpawnEgg,
                                                         Class<? extends org.cloudburstmc.api.entity.data.EntityData> dataClass) {
        this.registerInternal(plugin, type, factory, this.runtimeTypeAllocator++, priority, hasSpawnEgg, dataClass);
    }

    @Override
    public Class<? extends org.cloudburstmc.api.entity.data.EntityData> getDataClass(EntityType<?> type) {
        checkNotNull(type, "type");
        EntityData<?> data = this.dataMap.get(type);
        if (data == null) {
            return org.cloudburstmc.api.entity.data.EntityData.class;
        }
        return data.serviceProvider().getProvider().getValue().dataClass();
    }

    private <T extends Entity> void registerVanilla(EntityType<T> type, EntityFactory<T> factory, int legacyId,
                                                      Class<? extends org.cloudburstmc.api.entity.data.EntityData> dataClass) {
        this.registerInternal(null, type, factory, legacyId, 1000, false, dataClass);
    }

    private synchronized <T extends Entity> void registerInternal(Object plugin, EntityType<T> type, EntityFactory<T> factory,
                                                                  int runtimeType, int priority, boolean hasSpawnEgg,
                                                                  Class<? extends org.cloudburstmc.api.entity.data.EntityData> dataClass)
            throws RegistryException {
        checkClosed();
        checkNotNull(type, "type");
        checkNotNull(factory, "factory");
        EntityType<?> existingType = this.identifierTypeMap.get(type.getId());

        if (existingType == null) { // new entity
            if (runtimeType >= this.runtimeTypeAllocator) {
                this.runtimeTypeAllocator = runtimeType + 1;
            }

            this.runtimeTypeMap.put(runtimeType, type);
            this.typeToRuntimeMap.put(type, runtimeType);
            this.identifierTypeMap.put(type.getId(), type);

            EntityData<T> entityData = new EntityData<>(hasSpawnEgg,
                    new RegistryProvider<>(new EntityFactoryData<>(factory, dataClass), plugin, priority));
            this.dataMap.put(type, entityData);

            CloudComponentMap map = new CloudComponentMap(this);
            putComponents(type, map);
        } else if (existingType == type) { // existing - add plugin's factory if one does not exist
            RegistryProvider<EntityFactoryData<T>> provider =
                    new RegistryProvider<>(new EntityFactoryData<>(factory, dataClass), plugin, priority);
            getServiceProvider(type).add(provider);
        } else { // invalid - registering EntityType with used identifier.
            throw new RegistryException(type.getId() + " is already registered");
        }
    }

    public int getRuntimeType(EntityType<?> type) {
        return typeToRuntimeMap.getOrDefault(type, -1);
    }

    public EntityType<?> getEntityType(int runtimeId) {
        return runtimeTypeMap.get(runtimeId);
    }

    @Override
    public Optional<EntityType<?>> get(Identifier identifier) {
        checkNotNull(identifier, "identifier");
        return Optional.ofNullable(this.identifierTypeMap.get(identifier));
    }

    public EntityType<?> getOrCreateUnknownType(Identifier identifier) {
        checkState(this.closed, "Cannot get entity type during registration");
        return this.identifierTypeMap.computeIfAbsent(identifier, id -> {
            log.warn("Creating unknown entity type for {}", id);
            return EntityType.from(id, UnknownEntity.class);
        });
    }

    /**
     * Creates new entity of given type
     *
     * @param type     entity type
     * @param location location to spawn entity
     * @param <T>      entity class type
     * @return new entity
     */
    @Override
    public <T extends Entity> T create(EntityType<T> type, Location location) {
        checkState(closed, "Cannot create entity till registry is closed");
        checkNotNull(type, "type");
        checkNotNull(location, "location");
        EntityFactory<T> factory = getServiceProvider(type).getProvider().getValue().factory();
        return this.create(factory, type, location);
    }

    public <T extends Entity> T newEntity(EntityType<T> type, Location location) {
        return this.create(type, location);
    }

    /**
     * Creates new entity of given type from specific plugin factory
     *
     * @param type     entity type
     * @param location location to spawn entity
     * @param <T>      entity class type
     * @return new entity
     */
    public <T extends Entity> T newEntity(EntityType<T> type, Object plugin, Location location) {
        checkState(closed, "Cannot create entity till registry is closed");
        checkNotNull(type, "type");
        checkNotNull(plugin, "plugin");
        checkNotNull(location, "location");

        RegistryProvider<EntityFactoryData<T>> provider = getServiceProvider(type).getProvider(plugin);
        if (provider == null) {
            throw new RegistryException("Plugin has no registered provider for " + type.getId());
        }

        return this.create(provider.getValue().factory(), type, location);
    }

    private <T extends Entity> T create(EntityFactory<T> factory, EntityType<T> type, Location location) {
        T entity = factory.create(type, location);
        if (!(entity instanceof CloudEntity cloudEntity)) {
            throw new RegistryException("Entity factory must create a CloudEntity for " + type.getId());
        }

        cloudEntity.initialize(location);
        return entity;
    }

    /**
     * Allocate new entity ID
     *
     * @return entity ID
     */
    public long newEntityId() {
        return this.entityIdAllocator.incrementAndGet();
    }

    /**
     * Resolves unnamespaced saved entity names, preferring historical aliases.
     * Otherwise matches the UpperCamelCase form of a built-in entity's identifier name.
     * Both lookups are case-sensitive and do not include plugin registrations.
     * Returns {@code null} for unrecognized names.
     */
    public Identifier getIdentifier(String legacyName) {
        Identifier historical = LEGACY_NAMES.get(legacyName);
        if (historical != null) {
            return historical;
        }

        return CANONICAL_NAMES.get(legacyName);
    }

    /**
     * Returns a historical save name, or {@code null} when no historical alias exists.
     */
    public String getLegacyName(Identifier identifier) {
        return LEGACY_NAMES.inverse().get(identifier);
    }

    @Override
    public ImmutableSet<EntityType<?>> values() {
        return ImmutableSet.copyOf(this.identifierTypeMap.values());
    }

    @Override
    public CloudComponentMap configure(EntityType<?> type) throws RegistryException {
        this.checkClosed();
        checkNotNull(type, "type");
        if (!this.dataMap.containsKey(type)) {
            throw new RegistryException(type.getId() + " is not a registered entity");
        }
        return this.getComponentMap(type);
    }

    @SuppressWarnings("unchecked")
    private <T extends Entity> RegistryServiceProvider<EntityFactoryData<T>> getServiceProvider(EntityType<T> type) {
        EntityData<T> entityData = (EntityData<T>) this.dataMap.get(type);
        if (entityData == null) {
            if (type.getEntityClass() != UnknownEntity.class) {
                throw new RegistryException(type.getId() + " is not a registered entity");
            }
            entityData = (EntityData<T>) UNKNOWN_ENTITY_DATA;
        }
        return entityData.serviceProvider();
    }

    @Override
    public synchronized void close() throws RegistryException {
        checkClosed();

        // Bake registry providers
        this.dataMap.values().forEach(entityData -> entityData.serviceProvider().bake());
        this.freezeComponentMaps();

        // generate cache

        List<NbtMap> entityIdentifiers = new ArrayList<>(VANILLA_ENTITIES);

        for (int id = customEntityStart; id < runtimeTypeAllocator; id++) {
            EntityType<?> type = this.runtimeTypeMap.get(id);
            EntityData<?> data = this.dataMap.get(type);

            entityIdentifiers.add(NbtMap.builder()
                    .putBoolean("summonable", true)
                    .putBoolean("hasSpawnEgg", data.hasSpawnEgg())
                    .putBoolean("experimental", true) // If there are experimental features, we may as well enable them
                    .putString("id", type.getId().toString())
                    .putString("bid", "") // ???
                    .putInt("rid", id)
                    .build()
            );
        }

        this.entityIdentifiersPalette = NbtMap.builder()
                .putList("idlist", NbtType.COMPOUND, entityIdentifiers)
                .build();
        this.closed = true;
    }

    private void checkClosed() {
        checkState(!closed, "Registration is already closed");
    }

    private void registerVanillaEntityComponents() {
        this.registerComponent(EntityComponents.BUOYANCY);
        this.registerComponent(EntityComponents.GET_ATTACK_DAMAGE, entity -> 2f);
        this.registerComponent(EntityComponents.GET_PROJECTILE_DAMAGE_TYPE, entity -> DamageTypes.MOB_PROJECTILE);
        this.registerComponent(EntityComponents.GET_PICK_ITEM, (entity, includeData) -> ItemStack.EMPTY);
        this.registerComponent(EntityComponents.ON_INTERACT, (entity, player, item, clickedPos) -> false);
        this.registerComponent(EntityComponents.ON_TICK, (entity, currentTick) -> true);
        this.registerComponent(EntityComponents.CAN_BE_NAMED, entity -> true);
        this.registerComponent(EntityComponents.CAN_FREEZE, entity -> true);
        this.registerComponent(EntityComponents.CAN_WALK_ON_POWDER_SNOW, entity -> false);
        this.registerComponent(EntityComponents.GET_FREEZING_DAMAGE_MULTIPLIER, entity -> 1f);
    }

    private void registerBuoyancyComponents() {
        Buoyancy boat = new Buoyancy(true, 1, 0.03f, 10, 0, true, false, List.of(LiquidTypes.WATER, LiquidTypes.FLOWING_WATER));
        this.configure(BOAT).set(EntityComponents.BUOYANCY, boat);
        this.configure(CHEST_BOAT).set(EntityComponents.BUOYANCY, boat);
    }

    private void registerPickItemComponents() {
        CloudItemRegistry items = CloudItemRegistry.get();
        for (ItemType item : items.values()) {
            SpawnEggComponent egg = items.getComponent(item, ItemBehaviors.SPAWN_EGG);
            if (egg != null && this.dataMap.containsKey(egg.entityType())) {
                this.registerPickItem(egg.entityType(), item);
            }
        }

        this.registerPickItem(ARMOR_STAND, ItemTypes.ARMOR_STAND);
        this.registerPickItem(PAINTING, ItemTypes.PAINTING);
        this.registerPickItem(ENDER_CRYSTAL, ItemTypes.END_CRYSTAL);
        this.registerPickItem(MINECART, ItemTypes.MINECART);
        this.registerPickItem(CHEST_MINECART, ItemTypes.CHEST_MINECART);
        this.registerPickItem(HOPPER_MINECART, ItemTypes.HOPPER_MINECART);
        this.registerPickItem(TNT_MINECART, ItemTypes.TNT_MINECART);
        this.registerPickItem(COMMAND_BLOCK_MINECART, ItemTypes.COMMAND_BLOCK_MINECART);
        this.configure(BOAT).set(EntityComponents.GET_PICK_ITEM, PickItemEntityHandlers.BOAT);
        this.configure(CHEST_BOAT).set(EntityComponents.GET_PICK_ITEM, PickItemEntityHandlers.BOAT);
    }

    private void registerPickItem(EntityType<?> type, ItemType item) {
        this.configure(type).set(EntityComponents.GET_PICK_ITEM, (entity, includeData) -> ItemStack.from(item));
    }

    private void registerVanillaEntities() {
        registerVanilla(CHICKEN, EntityChicken::new, 10, CreatureData.class);
        registerVanilla(COW, EntityCow::new, 11, CreatureData.class);
        registerVanilla(PIG, EntityPig::new, 12, CreatureData.class);
        registerVanilla(SHEEP, EntitySheep::new, 13, SheepData.class);
        registerVanilla(WOLF, EntityWolf::new, 14, CreatureData.class);
        registerVanilla(DEPRECATED_VILLAGER, EntityDeprecatedVillager::new, 15, CreatureData.class);
        registerVanilla(MOOSHROOM, EntityMooshroom::new, 16, CreatureData.class);
        registerVanilla(SQUID, EntitySquid::new, 17, CreatureData.class);
        registerVanilla(RABBIT, EntityRabbit::new, 18, CreatureData.class);
        registerVanilla(BAT, EntityBat::new, 19, CreatureData.class);
        registerVanilla(IRON_GOLEM, EntityIronGolem::new, 20, org.cloudburstmc.api.entity.data.EntityData.class);
        registerVanilla(SNOW_GOLEM, EntitySnowGolem::new, 21, SnowGolemData.class);
        registerVanilla(OCELOT, EntityOcelot::new, 22, CreatureData.class);
        registerVanilla(HORSE, EntityHorse::new, 23, CreatureData.class);
        registerVanilla(DONKEY, EntityDonkey::new, 24, CreatureData.class);
        registerVanilla(MULE, EntityMule::new, 25, CreatureData.class);
        registerVanilla(SKELETON_HORSE, EntitySkeletonHorse::new, 26, CreatureData.class);
        registerVanilla(ZOMBIE_HORSE, EntityZombieHorse::new, 27, CreatureData.class);
        registerVanilla(POLAR_BEAR, EntityPolarBear::new, 28, CreatureData.class);
        registerVanilla(LLAMA, EntityLlama::new, 29, CreatureData.class);
        registerVanilla(PARROT, EntityParrot::new, 30, CreatureData.class);
        registerVanilla(DOLPHIN, EntityDolphin::new, 31, CreatureData.class);
        registerVanilla(ZOMBIE, EntityZombie::new, 32, CreatureData.class);
        registerVanilla(CREEPER, EntityCreeper::new, 33, CreeperData.class);
        registerVanilla(SKELETON, EntitySkeleton::new, 34, CreatureData.class);
        registerVanilla(SPIDER, EntitySpider::new, 35, CreatureData.class);
        registerVanilla(ZOMBIE_PIGMAN, EntityZombiePigman::new, 36, CreatureData.class);
        registerVanilla(SLIME, EntitySlime::new, 37, CreatureData.class);
        registerVanilla(ENDERMAN, EntityEnderman::new, 38, CreatureData.class);
        registerVanilla(SILVERFISH, EntitySilverfish::new, 39, CreatureData.class);
        registerVanilla(CAVE_SPIDER, EntityCaveSpider::new, 40, CreatureData.class);
        registerVanilla(GHAST, EntityGhast::new, 41, CreatureData.class);
        registerVanilla(MAGMA_CUBE, EntityMagmaCube::new, 42, CreatureData.class);
        registerVanilla(BLAZE, EntityBlaze::new, 43, CreatureData.class);
        registerVanilla(DEPRECATED_ZOMBIE_VILLAGER, EntityDeprecatedZombieVillager::new, 44, CreatureData.class);
        registerVanilla(WITCH, EntityWitch::new, 45, CreatureData.class);
        registerVanilla(STRAY, EntityStray::new, 46, CreatureData.class);
        registerVanilla(HUSK, EntityHusk::new, 47, CreatureData.class);
        registerVanilla(WITHER_SKELETON, EntityWitherSkeleton::new, 48, CreatureData.class);
        registerVanilla(GUARDIAN, EntityGuardian::new, 49, CreatureData.class);
        registerVanilla(ELDER_GUARDIAN, EntityElderGuardian::new, 50, CreatureData.class);
        registerVanilla(NPC, EntityNpc::new, 51, org.cloudburstmc.api.entity.data.EntityData.class);
        registerVanilla(WITHER, EntityWither::new, 52, CreatureData.class);
        registerVanilla(ENDER_DRAGON, EntityEnderDragon::new, 53, EnderDragonData.class);
        registerVanilla(SHULKER, EntityShulker::new, 54, CreatureData.class);
        registerVanilla(ENDERMITE, EntityEndermite::new, 55, CreatureData.class);
        registerVanilla(AGENT, EntityAgent::new, 56, CreatureData.class);
        registerVanilla(VINDICATOR, EntityVindicator::new, 57, CreatureData.class);
        registerVanilla(PHANTOM, EntityPhantom::new, 58, CreatureData.class);
        registerVanilla(RAVAGER, EntityRavager::new, 59, CreatureData.class);
        registerVanilla(ARMOR_STAND, EntityArmorStand::new, 61, org.cloudburstmc.api.entity.data.EntityData.class);
        registerVanilla(TRIPOD_CAMERA, EntityTripodCamera::new, 62, org.cloudburstmc.api.entity.data.EntityData.class);
        registerVanilla(ITEM, EntityItem::new, 64, DroppedItemData.class);
        registerVanilla(TNT, EntityPrimedTnt::new, 65, PrimedTntData.class);
        registerVanilla(FALLING_BLOCK, EntityFallingBlock::new, 66, FallingBlockData.class);
        registerVanilla(XP_BOTTLE, EntityXpBottle::new, 68, ProjectileData.class);
        registerVanilla(XP_ORB, EntityXpOrb::new, 69, ExperienceOrbData.class);
        registerVanilla(EYE_OF_ENDER_SIGNAL, EntityEyeOfEnderSignal::new, 70, ProjectileData.class);
        registerVanilla(ENDER_CRYSTAL, EntityEnderCrystal::new, 71, EnderCrystalData.class);
        registerVanilla(FIREWORKS_ROCKET, EntityFireworksRocket::new, 72, FireworksRocketData.class);
        registerVanilla(THROWN_TRIDENT, EntityThrownTrident::new, 73, ThrownTridentData.class);
        registerVanilla(TURTLE, EntityTurtle::new, 74, TurtleData.class);
        registerVanilla(CAT, EntityCat::new, 75, CreatureData.class);
        registerVanilla(SHULKER_BULLET, EntityShulkerBullet::new, 76, ProjectileData.class);
        registerVanilla(FISHING_HOOK, EntityFishingHook::new, 77, ProjectileData.class);
        registerVanilla(DRAGON_FIREBALL, EntityDragonFireball::new, 79, ProjectileData.class);
        registerVanilla(ARROW, EntityArrow::new, 80, ArrowData.class);
        registerVanilla(SNOWBALL, EntitySnowball::new, 81, ProjectileData.class);
        registerVanilla(EGG, EntityEgg::new, 82, ProjectileData.class);
        registerVanilla(PAINTING, EntityPainting::new, 83, PaintingData.class);
        registerVanilla(MINECART, EntityMinecart::new, 84, AbstractMinecartData.class);
        registerVanilla(FIREBALL, EntityFireball::new, 85, ProjectileData.class);
        registerVanilla(SPLASH_POTION, EntitySplashPotion::new, 86, SplashPotionData.class);
        registerVanilla(ENDER_PEARL, EntityEnderPearl::new, 87, ProjectileData.class);
        registerVanilla(LEASH_KNOT, EntityLeashKnot::new, 88, org.cloudburstmc.api.entity.data.EntityData.class);
        registerVanilla(WITHER_SKULL, EntityWitherSkull::new, 89, ProjectileData.class);
        registerVanilla(BOAT, EntityBoat::new, 90, BoatData.class);
        registerVanilla(WITHER_SKULL_DANGEROUS, EntityWitherSkullDangerous::new, 91, ProjectileData.class);
        registerVanilla(LIGHTNING_BOLT, EntityLightningBolt::new, 93, org.cloudburstmc.api.entity.data.EntityData.class);
        registerVanilla(SMALL_FIREBALL, EntitySmallFireball::new, 94, ProjectileData.class);
        registerVanilla(AREA_EFFECT_CLOUD, EntityAreaEffectCloud::new, 95, AreaEffectCloudData.class);
        registerVanilla(HOPPER_MINECART, EntityHopperMinecart::new, 96, AbstractMinecartData.class);
        registerVanilla(TNT_MINECART, EntityTntMinecart::new, 97, TntMinecartData.class);
        registerVanilla(CHEST_MINECART, EntityChestMinecart::new, 98, AbstractMinecartData.class);
        registerVanilla(COMMAND_BLOCK_MINECART, EntityCommandBlockMinecart::new, 100, AbstractMinecartData.class);
        registerVanilla(LINGERING_POTION, EntityLingeringPotion::new, 101, SplashPotionData.class);
        registerVanilla(LLAMA_SPIT, EntityLlamaSpit::new, 102, ProjectileData.class);
        registerVanilla(EVOCATION_FANG, EntityEvocationFang::new, 103, org.cloudburstmc.api.entity.data.EntityData.class);
        registerVanilla(EVOCATION_ILLAGER, EntityEvocationIllager::new, 104, CreatureData.class);
        registerVanilla(VEX, EntityVex::new, 105, CreatureData.class);
        registerVanilla(ICE_BOMB, EntityIceBomb::new, 106, org.cloudburstmc.api.entity.data.EntityData.class);
        registerVanilla(BALLOON, EntityBalloon::new, 107, org.cloudburstmc.api.entity.data.EntityData.class);
        registerVanilla(PUFFERFISH, EntityPufferfish::new, 108, CreatureData.class);
        registerVanilla(SALMON, EntitySalmon::new, 109, CreatureData.class);
        registerVanilla(DROWNED, EntityDrowned::new, 110, CreatureData.class);
        registerVanilla(TROPICAL_FISH, EntityTropicalFish::new, 111, CreatureData.class);
        registerVanilla(COD, EntityCod::new, 112, CreatureData.class);
        registerVanilla(PANDA, EntityPanda::new, 113, CreatureData.class);
        registerVanilla(PILLAGER, EntityPillager::new, 114, CreatureData.class);
        registerVanilla(VILLAGER, EntityVillager::new, 115, VillagerData.class);
        registerVanilla(ZOMBIE_VILLAGER, EntityZombieVillager::new, 116, CreatureData.class);
        registerVanilla(WANDERING_TRADER, EntityWanderingTrader::new, 118, CreatureData.class);
        registerVanilla(ELDER_GUARDIAN_GHOST, EntityElderGuardianGhost::new, 120, CreatureData.class);
        registerVanilla(FOX, EntityFox::new, 121, CreatureData.class);
        registerVanilla(BEE, EntityBee::new, 122, CreatureData.class);
        registerVanilla(PIGLIN, EntityPiglin::new, 123, CreatureData.class);
        registerVanilla(HOGLIN, EntityHoglin::new, 124, CreatureData.class);
        registerVanilla(STRIDER, EntityStrider::new, 125, CreatureData.class);
        registerVanilla(ZOGLIN, EntityZoglin::new, 126, CreatureData.class);
        registerVanilla(PIGLIN_BRUTE, EntityPiglinBrute::new, 127, CreatureData.class);
        registerVanilla(GOAT, EntityGoat::new, 128, CreatureData.class);
        registerVanilla(GLOW_SQUID, EntityGlowSquid::new, 129, CreatureData.class);
        registerVanilla(AXOLOTL, EntityAxolotl::new, 130, CreatureData.class);
        registerVanilla(WARDEN, EntityWarden::new, 131, CreatureData.class);
        registerVanilla(FROG, EntityFrog::new, 132, CreatureData.class);
        registerVanilla(TADPOLE, EntityTadpole::new, 133, CreatureData.class);
        registerVanilla(ALLAY, EntityAllay::new, 134, CreatureData.class);
        registerVanilla(CAMEL, EntityCamel::new, 138, CreatureData.class);
        registerVanilla(SNIFFER, EntitySniffer::new, 139, CreatureData.class);
        registerVanilla(BREEZE, EntityBreeze::new, 140, CreatureData.class);
        registerVanilla(BREEZE_WIND_CHARGE_PROJECTILE, EntityBreezeWindChargeProjectile::new, 141, ProjectileData.class);
        registerVanilla(ARMADILLO, EntityArmadillo::new, 142, CreatureData.class);
        registerVanilla(WIND_CHARGE_PROJECTILE, EntityWindChargeProjectile::new, 143, ProjectileData.class);
        registerVanilla(BOGGED, EntityBogged::new, 144, CreatureData.class);
        registerVanilla(OMINOUS_ITEM_SPAWNER, EntityOminousItemSpawner::new, 145, org.cloudburstmc.api.entity.data.EntityData.class);
        registerVanilla(CREAKING, EntityCreaking::new, 146, CreatureData.class);
        registerVanilla(HAPPY_GHAST, EntityHappyGhast::new, 147, HappyGhastData.class);
        registerVanilla(COPPER_GOLEM, EntityCopperGolem::new, 148, CreatureData.class);
        registerVanilla(NAUTILUS, EntityNautilus::new, 149, CreatureData.class);
        registerVanilla(ZOMBIE_NAUTILUS, EntityZombieNautilus::new, 150, CreatureData.class);
        registerVanilla(PARCHED, EntityParched::new, 151, CreatureData.class);
        registerVanilla(CAMEL_HUSK, EntityCamelHusk::new, 152, CreatureData.class);
        registerVanilla(SULFUR_CUBE, EntitySulfurCube::new, 153, CreatureData.class);
        registerVanilla(TRADER_LLAMA, EntityTraderLlama::new, 157, CreatureData.class);
        registerVanilla(CHEST_BOAT, EntityChestBoat::new, 218, ChestBoatData.class);
        registerVanilla(PLAYER, EntityHuman::new, 257, CreatureData.class);
    }

    private void registerProjectileDamageComponents() {
        getComponentMap(ARROW).set(EntityComponents.GET_PROJECTILE_DAMAGE_TYPE, entity -> DamageTypes.ARROW);
        getComponentMap(BREEZE_WIND_CHARGE_PROJECTILE).set(EntityComponents.GET_PROJECTILE_DAMAGE_TYPE, entity -> DamageTypes.WIND_CHARGE);
        getComponentMap(DRAGON_FIREBALL).set(EntityComponents.GET_PROJECTILE_DAMAGE_TYPE, entity -> DamageTypes.DRAGON_BREATH);
        getComponentMap(EGG).set(EntityComponents.GET_PROJECTILE_DAMAGE_TYPE, entity -> DamageTypes.THROWN);
        getComponentMap(FIREBALL).set(EntityComponents.GET_PROJECTILE_DAMAGE_TYPE, entity -> entity.getOwner() == null ? DamageTypes.UNATTRIBUTED_FIREBALL : DamageTypes.FIREBALL);
        getComponentMap(LINGERING_POTION).set(EntityComponents.GET_PROJECTILE_DAMAGE_TYPE, entity -> DamageTypes.INDIRECT_MAGIC);
        getComponentMap(LLAMA_SPIT).set(EntityComponents.GET_PROJECTILE_DAMAGE_TYPE, entity -> DamageTypes.SPIT);
        getComponentMap(SMALL_FIREBALL).set(EntityComponents.GET_PROJECTILE_DAMAGE_TYPE, entity -> entity.getOwner() == null ? DamageTypes.UNATTRIBUTED_FIREBALL : DamageTypes.FIREBALL);
        getComponentMap(SNOWBALL).set(EntityComponents.GET_PROJECTILE_DAMAGE_TYPE, entity -> DamageTypes.THROWN);
        getComponentMap(SPLASH_POTION).set(EntityComponents.GET_PROJECTILE_DAMAGE_TYPE, entity -> DamageTypes.INDIRECT_MAGIC);
        getComponentMap(THROWN_TRIDENT).set(EntityComponents.GET_PROJECTILE_DAMAGE_TYPE, entity -> DamageTypes.TRIDENT);
        getComponentMap(WIND_CHARGE_PROJECTILE).set(EntityComponents.GET_PROJECTILE_DAMAGE_TYPE, entity -> DamageTypes.WIND_CHARGE);
        getComponentMap(WITHER_SKULL).set(EntityComponents.GET_PROJECTILE_DAMAGE_TYPE, entity -> DamageTypes.WITHER_SKULL);
        getComponentMap(WITHER_SKULL_DANGEROUS).set(EntityComponents.GET_PROJECTILE_DAMAGE_TYPE, entity -> DamageTypes.WITHER_SKULL);
    }

    private void registerBucketableEntities() {
        InteractEntityHandler waterBucket = BucketableEntityHandlers.capture(ItemTypes.WATER_BUCKET);
        getComponentMap(AXOLOTL).set(EntityComponents.ON_INTERACT, waterBucket);
        getComponentMap(COD).set(EntityComponents.ON_INTERACT, waterBucket);
        getComponentMap(PUFFERFISH).set(EntityComponents.ON_INTERACT, waterBucket);
        getComponentMap(SALMON).set(EntityComponents.ON_INTERACT, waterBucket);
        getComponentMap(TADPOLE).set(EntityComponents.ON_INTERACT, waterBucket);
        getComponentMap(TROPICAL_FISH).set(EntityComponents.ON_INTERACT, waterBucket);
        getComponentMap(SULFUR_CUBE).set(EntityComponents.ON_INTERACT,
                BucketableEntityHandlers.capture(ItemTypes.BUCKET));
    }

    private void registerPowderSnowProperties() {
        for (EntityType<?> type : List.of(RABBIT, ENDERMITE, SILVERFISH, FOX)) {
            getComponentMap(type).set(EntityComponents.CAN_WALK_ON_POWDER_SNOW, entity -> true);
        }

        for (EntityType<?> type : List.of(STRAY, POLAR_BEAR, SNOW_GOLEM, WITHER)) {
            getComponentMap(type).set(EntityComponents.CAN_FREEZE, entity -> false);
        }

        for (EntityType<?> type : List.of(STRIDER, BLAZE, MAGMA_CUBE)) {
            getComponentMap(type).set(EntityComponents.GET_FREEZING_DAMAGE_MULTIPLIER, entity -> 5f);
        }
    }

    private void registerSnowGolemComponents() {
        getComponentMap(SNOW_GOLEM).set(EntityComponents.ON_TICK, SnowGolemEntityHandlers.ON_TICK);
    }

    private record EntityFactoryData<T extends Entity>(EntityFactory<T> factory,
                                                       Class<? extends org.cloudburstmc.api.entity.data.EntityData> dataClass) {
    }

    private record EntityData<T extends Entity>(boolean hasSpawnEgg, RegistryServiceProvider<EntityFactoryData<T>> serviceProvider) {
        private EntityData(boolean hasSpawnEgg, RegistryProvider<EntityFactoryData<T>> provider) {
            this(hasSpawnEgg, new RegistryServiceProvider<>(provider));
        }
    }
}
