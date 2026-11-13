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
import org.cloudburstmc.api.entity.component.Buoyancy;
import org.cloudburstmc.api.entity.component.InteractEntityHandler;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.item.ItemBehaviors;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.item.component.SpawnEggComponent;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.registry.EntityRegistry;
import org.cloudburstmc.api.registry.RegistryException;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.component.ComponentMap;
import org.cloudburstmc.nbt.NBTInputStream;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.nbt.NbtUtils;
import org.cloudburstmc.server.ServerJson;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.entity.EntityHuman;
import org.cloudburstmc.server.entity.UnknownEntity;
import org.cloudburstmc.server.entity.component.BucketableEntityHandlers;
import org.cloudburstmc.server.entity.component.PickItemEntityHandlers;
import org.cloudburstmc.server.entity.component.SnowGolemEntityHandlers;
import org.cloudburstmc.server.entity.data.*;
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
public class CloudEntityRegistry extends CloudComponentRegistry<EntityType<?>> implements EntityRegistry {
    private static final CloudEntityRegistry INSTANCE;

    private static final BiMap<String, Identifier> LEGACY_NAMES;
    private static final Map<String, Identifier> CANONICAL_NAMES;
    private static final List<NbtMap> VANILLA_ENTITIES;

    private static final EntityRegistration<UnknownEntity> UNKNOWN_ENTITY_DATA =
            new EntityRegistration<>(false, new RegistryProvider<>(
                    new EntityFactoryData<>(UnknownEntity::new, CloudEntityData.class), null, 0));

    static {
        try (InputStream stream = RegistryUtils.getOrAssertResource("legacy/entity_names.json")) {
            Map<String, String> legacyNames = ServerJson.MAPPER.readValue(stream, new TypeReference<Map<String, String>>() {
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
    private final Map<EntityType<?>, EntityRegistration<?>> dataMap = new IdentityHashMap<>();
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
        this.registerInternal(null, type, factory, this.runtimeTypeAllocator++, priority, hasSpawnEgg, CloudEntityData.class);
    }

    public synchronized <T extends Entity> void register(Object plugin, EntityType<T> type, EntityFactory<T> factory,
                                                         int priority, boolean hasSpawnEgg) {
        this.register(plugin, type, factory, priority, hasSpawnEgg,
                CloudEntityData.class);
    }

    public synchronized <T extends Entity> void register(Object plugin, EntityType<T> type, EntityFactory<T> factory,
                                                         int priority, boolean hasSpawnEgg,
                                                         Class<? extends CloudEntityData> dataClass) {
        this.registerInternal(plugin, type, factory, this.runtimeTypeAllocator++, priority, hasSpawnEgg, dataClass);
    }

    public Class<? extends CloudEntityData> getDataClass(EntityType<?> type) {
        checkNotNull(type, "type");
        EntityRegistration<?> data = this.dataMap.get(type);
        if (data == null) {
            return CloudEntityData.class;
        }
        return data.serviceProvider().getProvider().getValue().dataClass();
    }

    private <T extends Entity> void registerVanilla(EntityType<T> type, EntityFactory<T> factory, int legacyId,
                                                      Class<? extends CloudEntityData> dataClass) {
        this.registerInternal(null, type, factory, legacyId, 1000, false, dataClass);
    }

    private synchronized <T extends Entity> void registerInternal(Object plugin, EntityType<T> type, EntityFactory<T> factory,
                                                                  int runtimeType, int priority, boolean hasSpawnEgg,
                                                                  Class<? extends CloudEntityData> dataClass)
            throws RegistryException {
        checkClosed();
        checkNotNull(type, "type");
        checkNotNull(factory, "factory");
        checkNotNull(dataClass, "dataClass");
        EntityType<?> existingType = this.identifierTypeMap.get(type.getId());

        if (existingType == null) { // new entity
            if (runtimeType >= this.runtimeTypeAllocator) {
                this.runtimeTypeAllocator = runtimeType + 1;
            }

            this.runtimeTypeMap.put(runtimeType, type);
            this.typeToRuntimeMap.put(type, runtimeType);
            this.identifierTypeMap.put(type.getId(), type);

            EntityRegistration<T> entityData = new EntityRegistration<>(hasSpawnEgg,
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
        EntityRegistration<T> entityData = (EntityRegistration<T>) this.dataMap.get(type);
        if (entityData == null) {
            if (type.getEntityClass() != UnknownEntity.class) {
                throw new RegistryException(type.getId() + " is not a registered entity");
            }
            entityData = (EntityRegistration<T>) UNKNOWN_ENTITY_DATA;
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
            EntityRegistration<?> data = this.dataMap.get(type);

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
        registerVanilla(CHICKEN, EntityChicken::new, 10, CloudCreatureData.class);
        registerVanilla(COW, EntityCow::new, 11, CloudCreatureData.class);
        registerVanilla(PIG, EntityPig::new, 12, CloudCreatureData.class);
        registerVanilla(SHEEP, EntitySheep::new, 13, CloudCreatureData.class);
        registerVanilla(WOLF, EntityWolf::new, 14, CloudCreatureData.class);
        registerVanilla(DEPRECATED_VILLAGER, EntityDeprecatedVillager::new, 15, CloudCreatureData.class);
        registerVanilla(MOOSHROOM, EntityMooshroom::new, 16, CloudCreatureData.class);
        registerVanilla(SQUID, EntitySquid::new, 17, CloudCreatureData.class);
        registerVanilla(RABBIT, EntityRabbit::new, 18, CloudCreatureData.class);
        registerVanilla(BAT, EntityBat::new, 19, CloudCreatureData.class);
        registerVanilla(IRON_GOLEM, EntityIronGolem::new, 20, CloudEntityData.class);
        registerVanilla(SNOW_GOLEM, EntitySnowGolem::new, 21, CloudCreatureData.class);
        registerVanilla(OCELOT, EntityOcelot::new, 22, CloudCreatureData.class);
        registerVanilla(HORSE, EntityHorse::new, 23, CloudCreatureData.class);
        registerVanilla(DONKEY, EntityDonkey::new, 24, CloudCreatureData.class);
        registerVanilla(MULE, EntityMule::new, 25, CloudCreatureData.class);
        registerVanilla(SKELETON_HORSE, EntitySkeletonHorse::new, 26, CloudCreatureData.class);
        registerVanilla(ZOMBIE_HORSE, EntityZombieHorse::new, 27, CloudCreatureData.class);
        registerVanilla(POLAR_BEAR, EntityPolarBear::new, 28, CloudCreatureData.class);
        registerVanilla(LLAMA, EntityLlama::new, 29, CloudCreatureData.class);
        registerVanilla(PARROT, EntityParrot::new, 30, CloudCreatureData.class);
        registerVanilla(DOLPHIN, EntityDolphin::new, 31, CloudCreatureData.class);
        registerVanilla(ZOMBIE, EntityZombie::new, 32, CloudCreatureData.class);
        registerVanilla(CREEPER, EntityCreeper::new, 33, CloudCreatureData.class);
        registerVanilla(SKELETON, EntitySkeleton::new, 34, CloudCreatureData.class);
        registerVanilla(SPIDER, EntitySpider::new, 35, CloudCreatureData.class);
        registerVanilla(ZOMBIE_PIGMAN, EntityZombiePigman::new, 36, CloudCreatureData.class);
        registerVanilla(SLIME, EntitySlime::new, 37, CloudCreatureData.class);
        registerVanilla(ENDERMAN, EntityEnderman::new, 38, CloudCreatureData.class);
        registerVanilla(SILVERFISH, EntitySilverfish::new, 39, CloudCreatureData.class);
        registerVanilla(CAVE_SPIDER, EntityCaveSpider::new, 40, CloudCreatureData.class);
        registerVanilla(GHAST, EntityGhast::new, 41, CloudCreatureData.class);
        registerVanilla(MAGMA_CUBE, EntityMagmaCube::new, 42, CloudCreatureData.class);
        registerVanilla(BLAZE, EntityBlaze::new, 43, CloudCreatureData.class);
        registerVanilla(DEPRECATED_ZOMBIE_VILLAGER, EntityDeprecatedZombieVillager::new, 44, CloudCreatureData.class);
        registerVanilla(WITCH, EntityWitch::new, 45, CloudCreatureData.class);
        registerVanilla(STRAY, EntityStray::new, 46, CloudCreatureData.class);
        registerVanilla(HUSK, EntityHusk::new, 47, CloudCreatureData.class);
        registerVanilla(WITHER_SKELETON, EntityWitherSkeleton::new, 48, CloudCreatureData.class);
        registerVanilla(GUARDIAN, EntityGuardian::new, 49, CloudCreatureData.class);
        registerVanilla(ELDER_GUARDIAN, EntityElderGuardian::new, 50, CloudCreatureData.class);
        registerVanilla(NPC, EntityNpc::new, 51, CloudEntityData.class);
        registerVanilla(WITHER, EntityWither::new, 52, CloudCreatureData.class);
        registerVanilla(ENDER_DRAGON, EntityEnderDragon::new, 53, CloudEnderDragonData.class);
        registerVanilla(SHULKER, EntityShulker::new, 54, CloudCreatureData.class);
        registerVanilla(ENDERMITE, EntityEndermite::new, 55, CloudCreatureData.class);
        registerVanilla(AGENT, EntityAgent::new, 56, CloudCreatureData.class);
        registerVanilla(VINDICATOR, EntityVindicator::new, 57, CloudCreatureData.class);
        registerVanilla(PHANTOM, EntityPhantom::new, 58, CloudCreatureData.class);
        registerVanilla(RAVAGER, EntityRavager::new, 59, CloudCreatureData.class);
        registerVanilla(ARMOR_STAND, EntityArmorStand::new, 61, CloudLivingData.class);
        registerVanilla(TRIPOD_CAMERA, EntityTripodCamera::new, 62, CloudEntityData.class);
        registerVanilla(ITEM, EntityItem::new, 64, CloudDroppedItemData.class);
        registerVanilla(TNT, EntityPrimedTnt::new, 65, CloudPrimedTntData.class);
        registerVanilla(FALLING_BLOCK, EntityFallingBlock::new, 66, CloudFallingBlockData.class);
        registerVanilla(XP_BOTTLE, EntityXpBottle::new, 68, CloudProjectileData.class);
        registerVanilla(XP_ORB, EntityXpOrb::new, 69, CloudExperienceOrbData.class);
        registerVanilla(EYE_OF_ENDER_SIGNAL, EntityEyeOfEnderSignal::new, 70, CloudProjectileData.class);
        registerVanilla(ENDER_CRYSTAL, EntityEnderCrystal::new, 71, CloudEnderCrystalData.class);
        registerVanilla(FIREWORKS_ROCKET, EntityFireworksRocket::new, 72, CloudFireworksRocketData.class);
        registerVanilla(THROWN_TRIDENT, EntityThrownTrident::new, 73, CloudThrownTridentData.class);
        registerVanilla(TURTLE, EntityTurtle::new, 74, CloudTurtleData.class);
        registerVanilla(CAT, EntityCat::new, 75, CloudCreatureData.class);
        registerVanilla(SHULKER_BULLET, EntityShulkerBullet::new, 76, CloudProjectileData.class);
        registerVanilla(FISHING_HOOK, EntityFishingHook::new, 77, CloudProjectileData.class);
        registerVanilla(DRAGON_FIREBALL, EntityDragonFireball::new, 79, CloudProjectileData.class);
        registerVanilla(ARROW, EntityArrow::new, 80, CloudArrowData.class);
        registerVanilla(SNOWBALL, EntitySnowball::new, 81, CloudProjectileData.class);
        registerVanilla(EGG, EntityEgg::new, 82, CloudProjectileData.class);
        registerVanilla(PAINTING, EntityPainting::new, 83, CloudPaintingData.class);
        registerVanilla(MINECART, EntityMinecart::new, 84, CloudAbstractMinecartData.class);
        registerVanilla(FIREBALL, EntityFireball::new, 85, CloudProjectileData.class);
        registerVanilla(SPLASH_POTION, EntitySplashPotion::new, 86, CloudSplashPotionData.class);
        registerVanilla(ENDER_PEARL, EntityEnderPearl::new, 87, CloudProjectileData.class);
        registerVanilla(LEASH_KNOT, EntityLeashKnot::new, 88, CloudEntityData.class);
        registerVanilla(WITHER_SKULL, EntityWitherSkull::new, 89, CloudProjectileData.class);
        registerVanilla(BOAT, EntityBoat::new, 90, CloudBoatData.class);
        registerVanilla(WITHER_SKULL_DANGEROUS, EntityWitherSkullDangerous::new, 91, CloudProjectileData.class);
        registerVanilla(LIGHTNING_BOLT, EntityLightningBolt::new, 93, CloudEntityData.class);
        registerVanilla(SMALL_FIREBALL, EntitySmallFireball::new, 94, CloudProjectileData.class);
        registerVanilla(AREA_EFFECT_CLOUD, EntityAreaEffectCloud::new, 95, CloudAreaEffectCloudData.class);
        registerVanilla(HOPPER_MINECART, EntityHopperMinecart::new, 96, CloudAbstractMinecartData.class);
        registerVanilla(TNT_MINECART, EntityTntMinecart::new, 97, CloudTntMinecartData.class);
        registerVanilla(CHEST_MINECART, EntityChestMinecart::new, 98, CloudAbstractMinecartData.class);
        registerVanilla(COMMAND_BLOCK_MINECART, EntityCommandBlockMinecart::new, 100, CloudAbstractMinecartData.class);
        registerVanilla(LINGERING_POTION, EntityLingeringPotion::new, 101, CloudSplashPotionData.class);
        registerVanilla(LLAMA_SPIT, EntityLlamaSpit::new, 102, CloudProjectileData.class);
        registerVanilla(EVOCATION_FANG, EntityEvocationFang::new, 103, CloudEntityData.class);
        registerVanilla(EVOCATION_ILLAGER, EntityEvocationIllager::new, 104, CloudCreatureData.class);
        registerVanilla(VEX, EntityVex::new, 105, CloudCreatureData.class);
        registerVanilla(ICE_BOMB, EntityIceBomb::new, 106, CloudEntityData.class);
        registerVanilla(BALLOON, EntityBalloon::new, 107, CloudEntityData.class);
        registerVanilla(PUFFERFISH, EntityPufferfish::new, 108, CloudCreatureData.class);
        registerVanilla(SALMON, EntitySalmon::new, 109, CloudCreatureData.class);
        registerVanilla(DROWNED, EntityDrowned::new, 110, CloudCreatureData.class);
        registerVanilla(TROPICAL_FISH, EntityTropicalFish::new, 111, CloudCreatureData.class);
        registerVanilla(COD, EntityCod::new, 112, CloudCreatureData.class);
        registerVanilla(PANDA, EntityPanda::new, 113, CloudCreatureData.class);
        registerVanilla(PILLAGER, EntityPillager::new, 114, CloudCreatureData.class);
        registerVanilla(VILLAGER, EntityVillager::new, 115, CloudCreatureData.class);
        registerVanilla(ZOMBIE_VILLAGER, EntityZombieVillager::new, 116, CloudCreatureData.class);
        registerVanilla(WANDERING_TRADER, EntityWanderingTrader::new, 118, CloudCreatureData.class);
        registerVanilla(ELDER_GUARDIAN_GHOST, EntityElderGuardianGhost::new, 120, CloudCreatureData.class);
        registerVanilla(FOX, EntityFox::new, 121, CloudCreatureData.class);
        registerVanilla(BEE, EntityBee::new, 122, CloudCreatureData.class);
        registerVanilla(PIGLIN, EntityPiglin::new, 123, CloudCreatureData.class);
        registerVanilla(HOGLIN, EntityHoglin::new, 124, CloudCreatureData.class);
        registerVanilla(STRIDER, EntityStrider::new, 125, CloudCreatureData.class);
        registerVanilla(ZOGLIN, EntityZoglin::new, 126, CloudCreatureData.class);
        registerVanilla(PIGLIN_BRUTE, EntityPiglinBrute::new, 127, CloudCreatureData.class);
        registerVanilla(GOAT, EntityGoat::new, 128, CloudCreatureData.class);
        registerVanilla(GLOW_SQUID, EntityGlowSquid::new, 129, CloudCreatureData.class);
        registerVanilla(AXOLOTL, EntityAxolotl::new, 130, CloudCreatureData.class);
        registerVanilla(WARDEN, EntityWarden::new, 131, CloudCreatureData.class);
        registerVanilla(FROG, EntityFrog::new, 132, CloudCreatureData.class);
        registerVanilla(TADPOLE, EntityTadpole::new, 133, CloudCreatureData.class);
        registerVanilla(ALLAY, EntityAllay::new, 134, CloudCreatureData.class);
        registerVanilla(CAMEL, EntityCamel::new, 138, CloudCreatureData.class);
        registerVanilla(SNIFFER, EntitySniffer::new, 139, CloudCreatureData.class);
        registerVanilla(BREEZE, EntityBreeze::new, 140, CloudCreatureData.class);
        registerVanilla(BREEZE_WIND_CHARGE_PROJECTILE, EntityBreezeWindChargeProjectile::new, 141, CloudProjectileData.class);
        registerVanilla(ARMADILLO, EntityArmadillo::new, 142, CloudCreatureData.class);
        registerVanilla(WIND_CHARGE_PROJECTILE, EntityWindChargeProjectile::new, 143, CloudProjectileData.class);
        registerVanilla(BOGGED, EntityBogged::new, 144, CloudCreatureData.class);
        registerVanilla(OMINOUS_ITEM_SPAWNER, EntityOminousItemSpawner::new, 145, CloudEntityData.class);
        registerVanilla(CREAKING, EntityCreaking::new, 146, CloudCreatureData.class);
        registerVanilla(HAPPY_GHAST, EntityHappyGhast::new, 147, CloudHappyGhastData.class);
        registerVanilla(COPPER_GOLEM, EntityCopperGolem::new, 148, CloudCreatureData.class);
        registerVanilla(NAUTILUS, EntityNautilus::new, 149, CloudCreatureData.class);
        registerVanilla(ZOMBIE_NAUTILUS, EntityZombieNautilus::new, 150, CloudCreatureData.class);
        registerVanilla(PARCHED, EntityParched::new, 151, CloudCreatureData.class);
        registerVanilla(CAMEL_HUSK, EntityCamelHusk::new, 152, CloudCreatureData.class);
        registerVanilla(SULFUR_CUBE, EntitySulfurCube::new, 153, CloudCreatureData.class);
        registerVanilla(TRADER_LLAMA, EntityTraderLlama::new, 157, CloudCreatureData.class);
        registerVanilla(CHEST_BOAT, EntityChestBoat::new, 218, CloudChestBoatData.class);
        registerVanilla(PLAYER, EntityHuman::new, 257, CloudHumanData.class);
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
                                                       Class<? extends CloudEntityData> dataClass) {
    }

    private record EntityRegistration<T extends Entity>(boolean hasSpawnEgg, RegistryServiceProvider<EntityFactoryData<T>> serviceProvider) {
        private EntityRegistration(boolean hasSpawnEgg, RegistryProvider<EntityFactoryData<T>> provider) {
            this(hasSpawnEgg, new RegistryServiceProvider<>(provider));
        }
    }
}
