package org.cloudburstmc.api.entity;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.entity.hostile.*;
import org.cloudburstmc.api.entity.misc.*;
import org.cloudburstmc.api.entity.passive.*;
import org.cloudburstmc.api.entity.projectile.*;
import org.cloudburstmc.api.entity.vehicle.*;
import org.cloudburstmc.api.internal.BuiltInTypeCatalog;
import org.cloudburstmc.api.util.Identifier;

import java.util.List;
import java.util.Optional;

@UtilityClass
public class EntityTypes {
    private static final BuiltInTypeCatalog<EntityType<?>> TYPES = BuiltInTypeCatalog.create(EntityType::getId);

    public static final EntityType<Agent> AGENT = type("agent", Agent.class);
    public static final EntityType<Allay> ALLAY = type("allay", Allay.class);
    public static final EntityType<AreaEffectCloud> AREA_EFFECT_CLOUD = type("area_effect_cloud", AreaEffectCloud.class);
    public static final EntityType<Armadillo> ARMADILLO = type("armadillo", Armadillo.class);
    public static final EntityType<ArmorStand> ARMOR_STAND = type("armor_stand", ArmorStand.class);
    public static final EntityType<Arrow> ARROW = type("arrow", Arrow.class);
    public static final EntityType<Axolotl> AXOLOTL = type("axolotl", Axolotl.class);
    public static final EntityType<Balloon> BALLOON = type("balloon", Balloon.class);
    public static final EntityType<Bat> BAT = type("bat", Bat.class);
    public static final EntityType<Bee> BEE = type("bee", Bee.class);
    public static final EntityType<Blaze> BLAZE = type("blaze", Blaze.class);
    public static final EntityType<Boat> BOAT = type("boat", Boat.class);
    public static final EntityType<Bogged> BOGGED = type("bogged", Bogged.class);
    public static final EntityType<Breeze> BREEZE = type("breeze", Breeze.class);
    public static final EntityType<BreezeWindChargeProjectile> BREEZE_WIND_CHARGE_PROJECTILE = type("breeze_wind_charge_projectile", BreezeWindChargeProjectile.class);
    public static final EntityType<Camel> CAMEL = type("camel", Camel.class);
    public static final EntityType<CamelHusk> CAMEL_HUSK = type("camel_husk", CamelHusk.class);
    public static final EntityType<Cat> CAT = type("cat", Cat.class);
    public static final EntityType<CaveSpider> CAVE_SPIDER = type("cave_spider", CaveSpider.class);
    public static final EntityType<ChestBoat> CHEST_BOAT = type("chest_boat", ChestBoat.class);
    public static final EntityType<ChestMinecart> CHEST_MINECART = type("chest_minecart", ChestMinecart.class);
    public static final EntityType<Chicken> CHICKEN = type("chicken", Chicken.class);
    public static final EntityType<Cod> COD = type("cod", Cod.class);
    public static final EntityType<CommandBlockMinecart> COMMAND_BLOCK_MINECART = type("command_block_minecart", CommandBlockMinecart.class);
    public static final EntityType<CopperGolem> COPPER_GOLEM = type("copper_golem", CopperGolem.class);
    public static final EntityType<Cow> COW = type("cow", Cow.class);
    public static final EntityType<Creaking> CREAKING = type("creaking", Creaking.class);
    public static final EntityType<Creeper> CREEPER = type("creeper", Creeper.class);
    public static final EntityType<DeprecatedVillager> DEPRECATED_VILLAGER = type("villager", DeprecatedVillager.class);
    public static final EntityType<DeprecatedZombieVillager> DEPRECATED_ZOMBIE_VILLAGER = type("zombie_villager", DeprecatedZombieVillager.class);
    public static final EntityType<Dolphin> DOLPHIN = type("dolphin", Dolphin.class);
    public static final EntityType<Donkey> DONKEY = type("donkey", Donkey.class);
    public static final EntityType<DragonFireball> DRAGON_FIREBALL = type("dragon_fireball", DragonFireball.class);
    public static final EntityType<DroppedItem> ITEM = type("item", DroppedItem.class);
    public static final EntityType<Drowned> DROWNED = type("drowned", Drowned.class);
    public static final EntityType<Egg> EGG = type("egg", Egg.class);
    public static final EntityType<ElderGuardian> ELDER_GUARDIAN = type("elder_guardian", ElderGuardian.class);
    public static final EntityType<ElderGuardianGhost> ELDER_GUARDIAN_GHOST = type("elder_guardian_ghost", ElderGuardianGhost.class);
    public static final EntityType<EnderCrystal> ENDER_CRYSTAL = type("ender_crystal", EnderCrystal.class);
    public static final EntityType<EnderDragon> ENDER_DRAGON = type("ender_dragon", EnderDragon.class);
    public static final EntityType<EnderPearl> ENDER_PEARL = type("ender_pearl", EnderPearl.class);
    public static final EntityType<Enderman> ENDERMAN = type("enderman", Enderman.class);
    public static final EntityType<Endermite> ENDERMITE = type("endermite", Endermite.class);
    public static final EntityType<EvocationFang> EVOCATION_FANG = type("evocation_fang", EvocationFang.class);
    public static final EntityType<EvocationIllager> EVOCATION_ILLAGER = type("evocation_illager", EvocationIllager.class);
    public static final EntityType<ExperienceOrb> XP_ORB = type("xp_orb", ExperienceOrb.class);
    public static final EntityType<EyeOfEnderSignal> EYE_OF_ENDER_SIGNAL = type("eye_of_ender_signal", EyeOfEnderSignal.class);
    public static final EntityType<FallingBlock> FALLING_BLOCK = type("falling_block", FallingBlock.class);
    public static final EntityType<Fireball> FIREBALL = type("fireball", Fireball.class);
    public static final EntityType<FireworksRocket> FIREWORKS_ROCKET = type("fireworks_rocket", FireworksRocket.class);
    public static final EntityType<FishingHook> FISHING_HOOK = type("fishing_hook", FishingHook.class);
    public static final EntityType<Fox> FOX = type("fox", Fox.class);
    public static final EntityType<Frog> FROG = type("frog", Frog.class);
    public static final EntityType<Ghast> GHAST = type("ghast", Ghast.class);
    public static final EntityType<GlowSquid> GLOW_SQUID = type("glow_squid", GlowSquid.class);
    public static final EntityType<Goat> GOAT = type("goat", Goat.class);
    public static final EntityType<Guardian> GUARDIAN = type("guardian", Guardian.class);
    public static final EntityType<HappyGhast> HAPPY_GHAST = type("happy_ghast", HappyGhast.class);
    public static final EntityType<Hoglin> HOGLIN = type("hoglin", Hoglin.class);
    public static final EntityType<HopperMinecart> HOPPER_MINECART = type("hopper_minecart", HopperMinecart.class);
    public static final EntityType<Horse> HORSE = type("horse", Horse.class);
    public static final EntityType<Human> PLAYER = type("player", Human.class);
    public static final EntityType<Husk> HUSK = type("husk", Husk.class);
    public static final EntityType<IceBomb> ICE_BOMB = type("ice_bomb", IceBomb.class);
    public static final EntityType<IronGolem> IRON_GOLEM = type("iron_golem", IronGolem.class);
    public static final EntityType<LeashKnot> LEASH_KNOT = type("leash_knot", LeashKnot.class);
    public static final EntityType<LightningBolt> LIGHTNING_BOLT = type("lightning_bolt", LightningBolt.class);
    public static final EntityType<LingeringPotion> LINGERING_POTION = type("lingering_potion", LingeringPotion.class);
    public static final EntityType<Llama> LLAMA = type("llama", Llama.class);
    public static final EntityType<LlamaSpit> LLAMA_SPIT = type("llama_spit", LlamaSpit.class);
    public static final EntityType<MagmaCube> MAGMA_CUBE = type("magma_cube", MagmaCube.class);
    public static final EntityType<Minecart> MINECART = type("minecart", Minecart.class);
    public static final EntityType<Mooshroom> MOOSHROOM = type("mooshroom", Mooshroom.class);
    public static final EntityType<Mule> MULE = type("mule", Mule.class);
    public static final EntityType<Nautilus> NAUTILUS = type("nautilus", Nautilus.class);
    public static final EntityType<Npc> NPC = type("npc", Npc.class);
    public static final EntityType<Ocelot> OCELOT = type("ocelot", Ocelot.class);
    public static final EntityType<OminousItemSpawner> OMINOUS_ITEM_SPAWNER = type("ominous_item_spawner", OminousItemSpawner.class);
    public static final EntityType<Painting> PAINTING = type("painting", Painting.class);
    public static final EntityType<Panda> PANDA = type("panda", Panda.class);
    public static final EntityType<Parched> PARCHED = type("parched", Parched.class);
    public static final EntityType<Parrot> PARROT = type("parrot", Parrot.class);
    public static final EntityType<Phantom> PHANTOM = type("phantom", Phantom.class);
    public static final EntityType<Pig> PIG = type("pig", Pig.class);
    public static final EntityType<Piglin> PIGLIN = type("piglin", Piglin.class);
    public static final EntityType<PiglinBrute> PIGLIN_BRUTE = type("piglin_brute", PiglinBrute.class);
    public static final EntityType<Pillager> PILLAGER = type("pillager", Pillager.class);
    public static final EntityType<PolarBear> POLAR_BEAR = type("polar_bear", PolarBear.class);
    public static final EntityType<PrimedTnt> TNT = type("tnt", PrimedTnt.class);
    public static final EntityType<Pufferfish> PUFFERFISH = type("pufferfish", Pufferfish.class);
    public static final EntityType<Rabbit> RABBIT = type("rabbit", Rabbit.class);
    public static final EntityType<Ravager> RAVAGER = type("ravager", Ravager.class);
    public static final EntityType<Salmon> SALMON = type("salmon", Salmon.class);
    public static final EntityType<Sheep> SHEEP = type("sheep", Sheep.class);
    public static final EntityType<Shulker> SHULKER = type("shulker", Shulker.class);
    public static final EntityType<ShulkerBullet> SHULKER_BULLET = type("shulker_bullet", ShulkerBullet.class);
    public static final EntityType<Silverfish> SILVERFISH = type("silverfish", Silverfish.class);
    public static final EntityType<Skeleton> SKELETON = type("skeleton", Skeleton.class);
    public static final EntityType<SkeletonHorse> SKELETON_HORSE = type("skeleton_horse", SkeletonHorse.class);
    public static final EntityType<Slime> SLIME = type("slime", Slime.class);
    public static final EntityType<SmallFireball> SMALL_FIREBALL = type("small_fireball", SmallFireball.class);
    public static final EntityType<Sniffer> SNIFFER = type("sniffer", Sniffer.class);
    public static final EntityType<SnowGolem> SNOW_GOLEM = type("snow_golem", SnowGolem.class);
    public static final EntityType<Snowball> SNOWBALL = type("snowball", Snowball.class);
    public static final EntityType<Spider> SPIDER = type("spider", Spider.class);
    public static final EntityType<SplashPotion> SPLASH_POTION = type("splash_potion", SplashPotion.class);
    public static final EntityType<Squid> SQUID = type("squid", Squid.class);
    public static final EntityType<Stray> STRAY = type("stray", Stray.class);
    public static final EntityType<Strider> STRIDER = type("strider", Strider.class);
    public static final EntityType<SulfurCube> SULFUR_CUBE = type("sulfur_cube", SulfurCube.class);
    public static final EntityType<Tadpole> TADPOLE = type("tadpole", Tadpole.class);
    public static final EntityType<ThrownTrident> THROWN_TRIDENT = type("thrown_trident", ThrownTrident.class);
    public static final EntityType<TntMinecart> TNT_MINECART = type("tnt_minecart", TntMinecart.class);
    public static final EntityType<TraderLlama> TRADER_LLAMA = type("trader_llama", TraderLlama.class);
    public static final EntityType<TripodCamera> TRIPOD_CAMERA = type("tripod_camera", TripodCamera.class);
    public static final EntityType<TropicalFish> TROPICAL_FISH = type("tropicalfish", TropicalFish.class);
    public static final EntityType<Turtle> TURTLE = type("turtle", Turtle.class);
    public static final EntityType<Vex> VEX = type("vex", Vex.class);
    public static final EntityType<Villager> VILLAGER = type("villager_v2", Villager.class);
    public static final EntityType<Vindicator> VINDICATOR = type("vindicator", Vindicator.class);
    public static final EntityType<WanderingTrader> WANDERING_TRADER = type("wandering_trader", WanderingTrader.class);
    public static final EntityType<Warden> WARDEN = type("warden", Warden.class);
    public static final EntityType<WindChargeProjectile> WIND_CHARGE_PROJECTILE = type("wind_charge_projectile", WindChargeProjectile.class);
    public static final EntityType<Witch> WITCH = type("witch", Witch.class);
    public static final EntityType<Wither> WITHER = type("wither", Wither.class);
    public static final EntityType<WitherSkeleton> WITHER_SKELETON = type("wither_skeleton", WitherSkeleton.class);
    public static final EntityType<WitherSkull> WITHER_SKULL = type("wither_skull", WitherSkull.class);
    public static final EntityType<WitherSkull> WITHER_SKULL_DANGEROUS = type("wither_skull_dangerous", WitherSkull.class);
    public static final EntityType<Wolf> WOLF = type("wolf", Wolf.class);
    public static final EntityType<XpBottle> XP_BOTTLE = type("xp_bottle", XpBottle.class);
    public static final EntityType<Zoglin> ZOGLIN = type("zoglin", Zoglin.class);
    public static final EntityType<Zombie> ZOMBIE = type("zombie", Zombie.class);
    public static final EntityType<ZombieHorse> ZOMBIE_HORSE = type("zombie_horse", ZombieHorse.class);
    public static final EntityType<ZombieNautilus> ZOMBIE_NAUTILUS = type("zombie_nautilus", ZombieNautilus.class);
    public static final EntityType<ZombiePigman> ZOMBIE_PIGMAN = type("zombie_pigman", ZombiePigman.class);
    public static final EntityType<ZombieVillager> ZOMBIE_VILLAGER = type("zombie_villager_v2", ZombieVillager.class);

    /**
     * Finds a built-in entity type by identifier.
     *
     * @param id the entity type identifier
     * @return matching built-in entity type, if present
     */
    public static Optional<EntityType<?>> get(Identifier id) {
        return TYPES.get(id);
    }

    /**
     * Returns all built-in entity types in declaration order.
     *
     * @return built-in entity types
     */
    public static List<EntityType<?>> values() {
        return TYPES.values();
    }

    private static <T extends Entity> EntityType<T> type(String id, Class<T> apiType) {
        return TYPES.register(EntityType.from(id, apiType));
    }
}
