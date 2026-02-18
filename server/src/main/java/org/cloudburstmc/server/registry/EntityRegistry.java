package org.cloudburstmc.server.registry;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityFactory;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.registry.Registry;
import org.cloudburstmc.api.registry.RegistryException;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.nbt.NBTInputStream;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.nbt.NbtUtils;
import org.cloudburstmc.server.Bootstrap;
import org.cloudburstmc.server.entity.EntityHuman;
import org.cloudburstmc.server.entity.UnknownEntity;
import org.cloudburstmc.server.entity.hostile.*;
import org.cloudburstmc.server.entity.misc.*;
import org.cloudburstmc.server.entity.passive.*;
import org.cloudburstmc.server.entity.projectile.*;
import org.cloudburstmc.server.entity.vehicle.*;
import tools.jackson.core.type.TypeReference;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.cloudburstmc.api.entity.EntityTypes.*;

@Log4j2
public class EntityRegistry implements Registry {
    private static final EntityRegistry INSTANCE;

    private static final BiMap<String, Identifier> LEGACY_NAMES;
    private static final List<NbtMap> VANILLA_ENTITIES;

    private static final EntityData<UnknownEntity> UNKNOWN_ENTITY_DATA =
            new EntityData<>(false, new RegistryProvider<>(UnknownEntity::new, null, 0));

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

        INSTANCE = new EntityRegistry();
    }

    private final BiMap<Identifier, EntityType<?>> identifierTypeMap = HashBiMap.create();
    private final AtomicLong entityIdAllocator = new AtomicLong();
    private final Map<EntityType<?>, EntityData<?>> dataMap = new IdentityHashMap<>();
    private final Int2ObjectMap<EntityType<?>> runtimeTypeMap = new Int2ObjectOpenHashMap<>();
    private final Object2IntMap<EntityType<?>> typeToRuntimeMap = new Object2IntLinkedOpenHashMap<>();
    private final int customEntityStart;
    private int runtimeTypeAllocator;
    private volatile boolean closed;
    private NbtMap entityIdentifiersPalette;

    private EntityRegistry() {
        this.registerVanillaEntities();
        customEntityStart = runtimeTypeAllocator;
    }

    public static EntityRegistry get() {
        return INSTANCE;
    }

    public synchronized <T extends Entity> void register(Object plugin, EntityType<T> type, EntityFactory<T> factory,
                                                         int priority, boolean hasSpawnEgg) {
        this.registerInternal(plugin, type, factory, this.runtimeTypeAllocator++, priority, hasSpawnEgg);
    }

    private <T extends Entity> void registerVanilla(EntityType<T> type, EntityFactory<T> factory, int legacyId) {
        this.registerInternal(null, type, factory, legacyId, 1000, false); // Vanilla NBT decides
    }

    private synchronized <T extends Entity> void registerInternal(Object plugin, EntityType<T> type, EntityFactory<T> factory,
                                                                  int runtimeType, int priority, boolean hasSpawnEgg)
            throws RegistryException {
        checkClosed();
        checkNotNull(type, "type");
        checkNotNull(factory, "factory");
        EntityType<?> existingType = this.identifierTypeMap.get(type.getIdentifier());

        if (existingType == null) { // new entity
            if (runtimeType >= this.runtimeTypeAllocator) {
                this.runtimeTypeAllocator = runtimeType + 1;
            }

            this.runtimeTypeMap.put(runtimeType, type);
            this.typeToRuntimeMap.put(type, runtimeType);
            this.identifierTypeMap.put(type.getIdentifier(), type);

            EntityData<T> entityData = new EntityData<>(hasSpawnEgg, new RegistryProvider<>(factory, plugin, priority));
            this.dataMap.put(type, entityData);
        } else if (existingType == type) { // existing - add plugin's factory if one does not exist
            RegistryProvider<EntityFactory<T>> provider = new RegistryProvider<>(factory, plugin, priority);
            //noinspection unchecked
            ((EntityData<T>) this.dataMap.get(type)).serviceProvider.add(provider);
        } else { // invalid - registering EntityType with used identifier.
            throw new RegistryException(type.getIdentifier() + " is already registered");
        }
    }

    public int getRuntimeType(EntityType<?> type) {
        return typeToRuntimeMap.getOrDefault(type, -1);
    }

    public EntityType<?> getEntityType(int runtimeId) {
        return runtimeTypeMap.get(runtimeId);
    }

    public EntityType<?> getEntityType(Identifier identifier) {
        Preconditions.checkArgument(this.closed, "Cannot get entity type during registration");
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
    public <T extends Entity> T newEntity(EntityType<T> type, Location location) {
        checkState(closed, "Cannot create entity till registry is closed");
        checkNotNull(type, "type");
        checkNotNull(location, "location");
        EntityFactory<T> factory = getServiceProvider(type).getProvider().getValue();
        return factory.create(type, location);
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
        RegistryProvider<EntityFactory<T>> provider = getServiceProvider(type).getProvider(plugin);
        if (provider == null) {
            throw new RegistryException("Plugin has no registered provider for " + type.getIdentifier());
        }
        return provider.getValue().create(type, location);
    }

    /**
     * Allocate new entity ID
     *
     * @return entity ID
     */
    public long newEntityId() {
        return this.entityIdAllocator.incrementAndGet();
    }

    public Identifier getIdentifier(String legacyName) {
        return LEGACY_NAMES.get(legacyName);
    }

    public String getLegacyName(Identifier identifier) {
        return LEGACY_NAMES.inverse().get(identifier);
    }

    public NbtMap getEntityIdentifiersPalette() {
        return entityIdentifiersPalette;
    }

    public ImmutableSet<EntityType<?>> getEntityTypes() {
        return ImmutableSet.copyOf(this.identifierTypeMap.values());
    }

    @SuppressWarnings("unchecked")
    private <T extends Entity> RegistryServiceProvider<EntityFactory<T>> getServiceProvider(EntityType<T> type) {
        EntityData<T> entityData = (EntityData<T>) this.dataMap.get(type);
        if (entityData == null) {
            if (type.getEntityClass() != UnknownEntity.class) {
                throw new RegistryException(type.getIdentifier() + " is not a registered entity");
            }
            entityData = (EntityData<T>) UNKNOWN_ENTITY_DATA;
        }
        return entityData.serviceProvider;
    }

    @Override
    public synchronized void close() throws RegistryException {
        checkClosed();

        // Bake registry providers
        this.dataMap.values().forEach(entityData -> entityData.serviceProvider.bake());

        // generate cache

        List<NbtMap> entityIdentifiers = new ArrayList<>(VANILLA_ENTITIES);

        for (int id = customEntityStart; id < runtimeTypeAllocator; id++) {
            EntityType<?> type = this.runtimeTypeMap.get(id);
            EntityData<?> data = this.dataMap.get(type);

            entityIdentifiers.add(NbtMap.builder()
                    .putBoolean("summonable", true) // TODO: 07/01/2020 This affects the summon command auto completion
                    .putBoolean("hasSpawnEgg", data.hasSpawnEgg)
                    .putBoolean("experimental", true) // If there are experimental features, we may as well enable them
                    .putString("id", type.getIdentifier().toString())
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

    private void registerVanillaEntities() {
        registerVanilla(CHICKEN, EntityChicken::new, 10);
        registerVanilla(COW, EntityCow::new, 11);
        registerVanilla(PIG, EntityPig::new, 12);
        registerVanilla(SHEEP, EntitySheep::new, 13);
        registerVanilla(WOLF, EntityWolf::new, 14);
        registerVanilla(DEPRECATED_VILLAGER, EntityDeprecatedVillager::new, 15);
        registerVanilla(MOOSHROOM, EntityMooshroom::new, 16);
        registerVanilla(SQUID, EntitySquid::new, 17);
        registerVanilla(RABBIT, EntityRabbit::new, 18);
        registerVanilla(BAT, EntityBat::new, 19);
        registerVanilla(IRON_GOLEM, EntityIronGolem::new, 20);
        registerVanilla(SNOW_GOLEM, EntitySnowGolem::new, 21);
        registerVanilla(OCELOT, EntityOcelot::new, 22);
        registerVanilla(HORSE, EntityHorse::new, 23);
        registerVanilla(DONKEY, EntityDonkey::new, 24);
        registerVanilla(MULE, EntityMule::new, 25);
        registerVanilla(SKELETON_HORSE, EntitySkeletonHorse::new, 26);
        registerVanilla(ZOMBIE_HORSE, EntityZombieHorse::new, 27);
        registerVanilla(POLAR_BEAR, EntityPolarBear::new, 28);
        registerVanilla(LLAMA, EntityLlama::new, 29);
        registerVanilla(PARROT, EntityParrot::new, 30);
        registerVanilla(DOLPHIN, EntityDolphin::new, 31);
        registerVanilla(ZOMBIE, EntityZombie::new, 32);
        registerVanilla(CREEPER, EntityCreeper::new, 33);
        registerVanilla(SKELETON, EntitySkeleton::new, 34);
        registerVanilla(SPIDER, EntitySpider::new, 35);
        registerVanilla(ZOMBIE_PIGMAN, EntityZombiePigman::new, 36);
        registerVanilla(SLIME, EntitySlime::new, 37);
        registerVanilla(ENDERMAN, EntityEnderman::new, 38);
        registerVanilla(SILVERFISH, EntitySilverfish::new, 39);
        registerVanilla(CAVE_SPIDER, EntityCaveSpider::new, 40);
        registerVanilla(GHAST, EntityGhast::new, 41);
        registerVanilla(MAGMA_CUBE, EntityMagmaCube::new, 42);
        registerVanilla(BLAZE, EntityBlaze::new, 43);
        registerVanilla(DEPRECATED_ZOMBIE_VILLAGER, EntityDeprecatedZombieVillager::new, 44);
        registerVanilla(WITCH, EntityWitch::new, 45);
        registerVanilla(STRAY, EntityStray::new, 46);
        registerVanilla(HUSK, EntityHusk::new, 47);
        registerVanilla(WITHER_SKELETON, EntityWitherSkeleton::new, 48);
        registerVanilla(GUARDIAN, EntityGuardian::new, 49);
        registerVanilla(ELDER_GUARDIAN, EntityElderGuardian::new, 50);
        registerVanilla(NPC, EntityNpc::new, 51);
        registerVanilla(WITHER, EntityWither::new, 52);
        registerVanilla(ENDER_DRAGON, EntityEnderDragon::new, 53);
        registerVanilla(SHULKER, EntityShulker::new, 54);
        registerVanilla(ENDERMITE, EntityEndermite::new, 55);
        registerVanilla(AGENT, EntityAgent::new, 56);
        registerVanilla(VINDICATOR, EntityVindicator::new, 57);
        registerVanilla(PHANTOM, EntityPhantom::new, 58);
        registerVanilla(RAVAGER, EntityRavager::new, 59);
        registerVanilla(ARMOR_STAND, EntityArmorStand::new, 61);
        registerVanilla(TRIPOD_CAMERA, EntityTripodCamera::new, 62);
        registerVanilla(ITEM, EntityItem::new, 64);
        registerVanilla(TNT, EntityTnt::new, 65);
        registerVanilla(FALLING_BLOCK, EntityFallingBlock::new, 66);
        registerVanilla(XP_BOTTLE, EntityXpBottle::new, 68);
        registerVanilla(XP_ORB, EntityXpOrb::new, 69);
        registerVanilla(EYE_OF_ENDER_SIGNAL, EntityEyeOfEnderSignal::new, 70);
        registerVanilla(ENDER_CRYSTAL, EntityEnderCrystal::new, 71);
        registerVanilla(FIREWORKS_ROCKET, EntityFireworksRocket::new, 72);
        registerVanilla(THROWN_TRIDENT, EntityThrownTrident::new, 73);
        registerVanilla(TURTLE, EntityTurtle::new, 74);
        registerVanilla(CAT, EntityCat::new, 75);
        registerVanilla(SHULKER_BULLET, EntityShulkerBullet::new, 76);
        registerVanilla(FISHING_HOOK, EntityFishingHook::new, 77);
        registerVanilla(DRAGON_FIREBALL, EntityDragonFireball::new, 79);
        registerVanilla(ARROW, EntityArrow::new, 80);
        registerVanilla(SNOWBALL, EntitySnowball::new, 81);
        registerVanilla(EGG, EntityEgg::new, 82);
        registerVanilla(PAINTING, EntityPainting::new, 83);
        registerVanilla(MINECART, EntityMinecart::new, 84);
        registerVanilla(FIREBALL, EntityFireball::new, 85);
        registerVanilla(SPLASH_POTION, EntitySplashPotion::new, 86);
        registerVanilla(ENDER_PEARL, EntityEnderPearl::new, 87);
        registerVanilla(LEASH_KNOT, EntityLeashKnot::new, 88);
        registerVanilla(WITHER_SKULL, EntityWitherSkull::new, 89);
        registerVanilla(BOAT, EntityBoat::new, 90);
        registerVanilla(WITHER_SKULL_DANGEROUS, EntityWitherSkullDangerous::new, 91);
        registerVanilla(LIGHTNING_BOLT, EntityLightningBolt::new, 93);
        registerVanilla(SMALL_FIREBALL, EntitySmallFireball::new, 94);
        registerVanilla(AREA_EFFECT_CLOUD, EntityAreaEffectCloud::new, 95);
        registerVanilla(HOPPER_MINECART, EntityHopperMinecart::new, 96);
        registerVanilla(TNT_MINECART, EntityTntMinecart::new, 97);
        registerVanilla(CHEST_MINECART, EntityChestMinecart::new, 98);
        registerVanilla(COMMAND_BLOCK_MINECART, EntityCommandBlockMinecart::new, 100);
        registerVanilla(LINGERING_POTION, EntityLingeringPotion::new, 101);
        registerVanilla(LLAMA_SPIT, EntityLlamaSpit::new, 102);
        registerVanilla(EVOCATION_FANG, EntityEvocationFang::new, 103);
        registerVanilla(EVOCATION_ILLAGER, EntityEvocationIllager::new, 104);
        registerVanilla(VEX, EntityVex::new, 105);
        registerVanilla(ICE_BOMB, EntityIceBomb::new, 106);
        registerVanilla(BALLOON, EntityBalloon::new, 107);
        registerVanilla(PUFFERFISH, EntityPufferfish::new, 108);
        registerVanilla(SALMON, EntitySalmon::new, 109);
        registerVanilla(DROWNED, EntityDrowned::new, 110);
        registerVanilla(TROPICAL_FISH, EntityTropicalFish::new, 111);
        registerVanilla(COD, EntityCod::new, 112);
        registerVanilla(PANDA, EntityPanda::new, 113);
        registerVanilla(PILLAGER, EntityPillager::new, 114);
        registerVanilla(VILLAGER, EntityVillager::new, 115);
        registerVanilla(ZOMBIE_VILLAGER, EntityZombieVillager::new, 116);
        registerVanilla(WANDERING_TRADER, EntityWanderingTrader::new, 118);
        registerVanilla(ELDER_GUARDIAN_GHOST, EntityElderGuardianGhost::new, 120);
        registerVanilla(FOX, EntityFox::new, 121);
        registerVanilla(BEE, EntityBee::new, 122);
        registerVanilla(PIGLIN, EntityPiglin::new, 123);
        registerVanilla(HOGLIN, EntityHoglin::new, 124);
        registerVanilla(STRIDER, EntityStrider::new, 125);
        registerVanilla(ZOGLIN, EntityZoglin::new, 126);
        registerVanilla(PIGLIN_BRUTE, EntityPiglinBrute::new, 127);
        registerVanilla(GOAT, EntityGoat::new, 128);
        registerVanilla(GLOW_SQUID, EntityGlowSquid::new, 129);
        registerVanilla(AXOLOTL, EntityAxolotl::new, 130);
        registerVanilla(WARDEN, EntityWarden::new, 131);
        registerVanilla(FROG, EntityFrog::new, 132);
        registerVanilla(TADPOLE, EntityTadpole::new, 133);
        registerVanilla(ALLAY, EntityAllay::new, 134);
        registerVanilla(CAMEL, EntityCamel::new, 138);
        registerVanilla(SNIFFER, EntitySniffer::new, 139);
        registerVanilla(BREEZE, EntityBreeze::new, 140);
        registerVanilla(BREEZE_WIND_CHARGE_PROJECTILE, EntityBreezeWindChargeProjectile::new, 141);
        registerVanilla(ARMADILLO, EntityArmadillo::new, 142);
        registerVanilla(WIND_CHARGE_PROJECTILE, EntityWindChargeProjectile::new, 143);
        registerVanilla(BOGGED, EntityBogged::new, 144);
        registerVanilla(OMINOUS_ITEM_SPAWNER, EntityOminousItemSpawner::new, 145);
        registerVanilla(CREAKING, EntityCreaking::new, 146);
        registerVanilla(HAPPY_GHAST, EntityHappyGhast::new, 147);
        registerVanilla(COPPER_GOLEM, EntityCopperGolem::new, 148);
        registerVanilla(NAUTILUS, EntityNautilus::new, 149);
        registerVanilla(ZOMBIE_NAUTILUS, EntityZombieNautilus::new, 150);
        registerVanilla(PARCHED, EntityParched::new, 151);
        registerVanilla(CAMEL_HUSK, EntityCamelHusk::new, 152);
        registerVanilla(TRADER_LLAMA, EntityTraderLlama::new, 157);
        registerVanilla(CHEST_BOAT, EntityChestBoat::new, 218);
        registerVanilla(PLAYER, EntityHuman::new, 257);
    }

    private static class EntityData<T extends Entity> {
        private final boolean hasSpawnEgg;
        private final RegistryServiceProvider<EntityFactory<T>> serviceProvider;

        private EntityData(boolean hasSpawnEgg, RegistryProvider<EntityFactory<T>> provider) {
            this.hasSpawnEgg = hasSpawnEgg;
            this.serviceProvider = new RegistryServiceProvider<>(provider);
        }
    }
}