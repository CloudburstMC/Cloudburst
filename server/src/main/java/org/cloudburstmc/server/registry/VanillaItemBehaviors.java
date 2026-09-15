package org.cloudburstmc.server.registry;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.enchantment.EnchantmentTarget;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.item.*;
import org.cloudburstmc.api.item.component.CanRepairWithHandler;
import org.cloudburstmc.api.registry.RegistryException;
import org.cloudburstmc.server.item.VanillaTools;
import org.cloudburstmc.server.item.component.*;
import org.cloudburstmc.server.item.serializer.BannerSerializer;
import org.cloudburstmc.server.item.serializer.FireworkRocketSerializer;
import org.cloudburstmc.server.item.serializer.FireworkStarSerializer;
import org.cloudburstmc.server.level.Sound;
import org.cloudburstmc.server.registry.component.CloudComponentMap;

import java.util.Optional;

import static org.cloudburstmc.api.item.ItemTypes.*;
import static org.cloudburstmc.server.registry.CloudItemRegistry.repairWith;

/**
 * Configures server behavior for vanilla item types.
 */
@UtilityClass
public final class VanillaItemBehaviors {

    public static void configure(CloudItemRegistry registry) {
        registry.configure(ItemTypes.AGENT_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.AGENT));
        registry.configure(ItemTypes.ALLAY_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.ALLAY));
        registry.configure(ItemTypes.ARMADILLO_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.ARMADILLO));
        registry.configure(ItemTypes.AXOLOTL_BUCKET)
                .set(ItemComponents.USE_ON, BucketItemHandlers.placeEntity(BlockStates.WATER, EntityTypes.AXOLOTL));
        registry.configure(ItemTypes.AXOLOTL_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.AXOLOTL));
        registry.configure(ItemTypes.BANNER, new BannerSerializer());
        registry.configure(ItemTypes.BAT_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.BAT));
        registry.configure(ItemTypes.BEE_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.BEE));
        registry.configure(ItemTypes.BLAZE_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.BLAZE));
        registry.configure(ItemTypes.BOGGED_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.BOGGED));
        registry.configure(ItemTypes.BONE_MEAL).set(ItemComponents.USE_ON, BoneMealItemHandlers.USE_ON);
        configureDamageableEnchantable(
                registry,
                ItemTypes.BOW,
                384,
                repairWith(),
                EnchantmentTarget.BOW,
                EnchantmentTarget.BREAKABLE,
                EnchantmentTarget.VANISHABLE);
        registry.configure(ItemTypes.BREEZE_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.BREEZE));
        configureDamageableEnchantable(
                registry, ItemTypes.BRUSH, 64, repairWith(), EnchantmentTarget.BREAKABLE, EnchantmentTarget.VANISHABLE);
        registry.configure(ItemTypes.BUCKET).set(ItemComponents.USE_ON, BucketItemHandlers.PICK_UP);
        registry.configure(ItemTypes.CAMEL_HUSK_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.CAMEL_HUSK));
        registry.configure(ItemTypes.CAMEL_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.CAMEL));
        configureDamageableEnchantable(
                registry,
                ItemTypes.CARROT_ON_A_STICK,
                25,
                repairWith(),
                EnchantmentTarget.BREAKABLE,
                EnchantmentTarget.VANISHABLE);
        registry.configure(ItemTypes.CAT_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.CAT));
        registry.configure(ItemTypes.CAVE_SPIDER_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.CAVE_SPIDER));
        configureArmorBoots(
                registry,
                ItemTypes.CHAINMAIL_BOOTS,
                15,
                repairWith(ItemTags.REPAIRS_CHAIN_ARMOR),
                Sound.ARMOR_EQUIP_CHAIN);
        configureArmorChestplate(
                registry,
                ItemTypes.CHAINMAIL_CHESTPLATE,
                15,
                repairWith(ItemTags.REPAIRS_CHAIN_ARMOR),
                Sound.ARMOR_EQUIP_CHAIN);
        configureArmorHelmet(
                registry,
                ItemTypes.CHAINMAIL_HELMET,
                15,
                repairWith(ItemTags.REPAIRS_CHAIN_ARMOR),
                Sound.ARMOR_EQUIP_CHAIN);
        configureArmorLeggings(
                registry,
                ItemTypes.CHAINMAIL_LEGGINGS,
                15,
                repairWith(ItemTags.REPAIRS_CHAIN_ARMOR),
                Sound.ARMOR_EQUIP_CHAIN);
        registry.configure(ItemTypes.CHEST_MINECART)
                .set(ItemComponents.USE_ON, MinecartItemHandlers.useOn(EntityTypes.CHEST_MINECART));
        registry.configure(ItemTypes.CHICKEN_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.CHICKEN));
        registry.configure(ItemTypes.CHORUS_FRUIT)
                .set(ItemComponents.FINISH_USE, ChorusFruitItemHandlers.FINISH_USE)
                .set(ItemComponents.USE, ChorusFruitItemHandlers.USE)
                .set(ItemComponents.USE_DURATION_TICKS, 32);
        registry.configure(ItemTypes.COD_BUCKET)
                .set(ItemComponents.USE_ON, BucketItemHandlers.placeEntity(BlockStates.WATER, EntityTypes.COD));
        registry.configure(ItemTypes.COD_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.COD));
        registry.configure(ItemTypes.COMMAND_BLOCK_MINECART)
                .set(ItemComponents.USE_ON, MinecartItemHandlers.useOn(EntityTypes.COMMAND_BLOCK_MINECART));
        configureAxe(registry, ItemTypes.COPPER_AXE, ToolMaterials.COPPER, repairWith(ItemTags.COPPER_TOOL_MATERIALS));
        configureArmorBoots(
                registry,
                ItemTypes.COPPER_BOOTS,
                11,
                repairWith(ItemTags.REPAIRS_COPPER_ARMOR),
                Sound.ARMOR_EQUIP_COPPER);
        configureArmorChestplate(
                registry,
                ItemTypes.COPPER_CHESTPLATE,
                11,
                repairWith(ItemTags.REPAIRS_COPPER_ARMOR),
                Sound.ARMOR_EQUIP_COPPER);
        registry.configure(ItemTypes.COPPER_GOLEM_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.COPPER_GOLEM));
        configureArmorHelmet(
                registry,
                ItemTypes.COPPER_HELMET,
                11,
                repairWith(ItemTags.REPAIRS_COPPER_ARMOR),
                Sound.ARMOR_EQUIP_COPPER);
        configureHoe(registry, COPPER_HOE, ToolMaterials.COPPER, repairWith(ItemTags.COPPER_TOOL_MATERIALS));
        configureArmorLeggings(
                registry,
                ItemTypes.COPPER_LEGGINGS,
                11,
                repairWith(ItemTags.REPAIRS_COPPER_ARMOR),
                Sound.ARMOR_EQUIP_COPPER);
        configurePickaxe(
                registry, ItemTypes.COPPER_PICKAXE, ToolMaterials.COPPER, repairWith(ItemTags.COPPER_TOOL_MATERIALS));
        configureShovel(
                registry, ItemTypes.COPPER_SHOVEL, ToolMaterials.COPPER, repairWith(ItemTags.COPPER_TOOL_MATERIALS));
        configureSpear(
                registry, ItemTypes.COPPER_SPEAR, ToolMaterials.COPPER, repairWith(ItemTags.COPPER_TOOL_MATERIALS));
        configureSword(
                registry, ItemTypes.COPPER_SWORD, ToolMaterials.COPPER, repairWith(ItemTags.COPPER_TOOL_MATERIALS));
        registry.configure(ItemTypes.COW_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.COW));
        registry.configure(ItemTypes.CREAKING_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.CREAKING));
        registry.configure(ItemTypes.CREEPER_BANNER_PATTERN);
        registry.configure(ItemTypes.CREEPER_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.CREEPER));
        configureDamageableEnchantable(
                registry,
                ItemTypes.CROSSBOW,
                465,
                repairWith(),
                EnchantmentTarget.CROSSBOW,
                EnchantmentTarget.BREAKABLE,
                EnchantmentTarget.VANISHABLE);
        configureAxe(
                registry, ItemTypes.DIAMOND_AXE, ToolMaterials.DIAMOND, repairWith(ItemTags.DIAMOND_TOOL_MATERIALS));
        configureArmorBoots(
                registry,
                ItemTypes.DIAMOND_BOOTS,
                33,
                repairWith(ItemTags.REPAIRS_DIAMOND_ARMOR),
                Sound.ARMOR_EQUIP_DIAMOND);
        configureArmorChestplate(
                registry,
                ItemTypes.DIAMOND_CHESTPLATE,
                33,
                repairWith(ItemTags.REPAIRS_DIAMOND_ARMOR),
                Sound.ARMOR_EQUIP_DIAMOND);
        configureArmorHelmet(
                registry,
                ItemTypes.DIAMOND_HELMET,
                33,
                repairWith(ItemTags.REPAIRS_DIAMOND_ARMOR),
                Sound.ARMOR_EQUIP_DIAMOND);
        configureHoe(
                registry, ItemTypes.DIAMOND_HOE, ToolMaterials.DIAMOND, repairWith(ItemTags.DIAMOND_TOOL_MATERIALS));
        registry.configure(ItemTypes.DIAMOND_HORSE_ARMOR);
        configureArmorLeggings(
                registry,
                ItemTypes.DIAMOND_LEGGINGS,
                33,
                repairWith(ItemTags.REPAIRS_DIAMOND_ARMOR),
                Sound.ARMOR_EQUIP_DIAMOND);
        configurePickaxe(
                registry,
                ItemTypes.DIAMOND_PICKAXE,
                ToolMaterials.DIAMOND,
                repairWith(ItemTags.DIAMOND_TOOL_MATERIALS));
        configureShovel(
                registry, ItemTypes.DIAMOND_SHOVEL, ToolMaterials.DIAMOND, repairWith(ItemTags.DIAMOND_TOOL_MATERIALS));
        configureSpear(
                registry, ItemTypes.DIAMOND_SPEAR, ToolMaterials.DIAMOND, repairWith(ItemTags.DIAMOND_TOOL_MATERIALS));
        configureSword(
                registry, ItemTypes.DIAMOND_SWORD, ToolMaterials.DIAMOND, repairWith(ItemTags.DIAMOND_TOOL_MATERIALS));
        registry.configure(ItemTypes.DOLPHIN_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.DOLPHIN));
        registry.configure(ItemTypes.DONKEY_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.DONKEY));
        registry.configure(ItemTypes.DROWNED_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.DROWNED));
        registry.configure(ItemTypes.ELDER_GUARDIAN_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.ELDER_GUARDIAN));
        configureElytra(registry, ELYTRA, repairWith(PHANTOM_MEMBRANE.getId()));
        registry.configure(ItemTypes.ENCHANTED_BOOK).set(ItemComponents.CAN_ENCHANT_WITH, (item, enchantment) -> true);
        registry.configure(ItemTypes.END_CRYSTAL).set(ItemComponents.USE_ON, EndCrystalItemHandlers.USE_ON);
        registry.configure(ItemTypes.ENDER_DRAGON_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.ENDER_DRAGON));
        registry.configure(ItemTypes.ENDER_EYE).set(ItemComponents.USE_ON, EnderEyeItemHandlers.USE_ON);
        registry.configure(ItemTypes.ENDER_PEARL).set(ItemComponents.USE, EnderPearlItemHandlers.USE);
        registry.configure(ItemTypes.ENDERMAN_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.ENDERMAN));
        registry.configure(ItemTypes.ENDERMITE_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.ENDERMITE));
        registry.configure(ItemTypes.EVOKER_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.EVOCATION_ILLAGER));
        registry.configure(ItemTypes.FIRE_CHARGE).set(ItemComponents.USE_ON, FireChargeItemHandlers.USE_ON);
        registry.configure(ItemTypes.FIREWORK_ROCKET, new FireworkRocketSerializer())
                .set(ItemComponents.USE, FireworkRocketItemHandlers.USE)
                .set(ItemComponents.USE_ON, FireworkRocketItemHandlers.USE_ON);
        registry.configure(ItemTypes.FIREWORK_STAR, new FireworkStarSerializer());
        configureDamageableEnchantable(
                        registry,
                        ItemTypes.FISHING_ROD,
                        64,
                        repairWith(),
                        EnchantmentTarget.FISHING_ROD,
                        EnchantmentTarget.BREAKABLE,
                        EnchantmentTarget.VANISHABLE)
                .set(ItemComponents.USE, FishingRodItemHandlers.USE);
        registry.configure(ItemTypes.FLINT_AND_STEEL).set(ItemComponents.USE_ON, FlintAndSteelItemHandlers.USE_ON);
        registry.configure(ItemTypes.FOX_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.FOX));
        registry.configure(ItemTypes.FROG_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.FROG));
        registry.configure(ItemTypes.GHAST_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.GHAST));
        registry.configure(ItemTypes.GLOW_SQUID_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.GLOW_SQUID));
        registry.configure(ItemTypes.GOAT_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.GOAT));
        configureAxe(registry, ItemTypes.GOLDEN_AXE, ToolMaterials.GOLD, repairWith(ItemTags.GOLD_TOOL_MATERIALS));
        configureArmorBoots(
                registry, ItemTypes.GOLDEN_BOOTS, 7, repairWith(ItemTags.REPAIRS_GOLD_ARMOR), Sound.ARMOR_EQUIP_GOLD);
        configureArmorChestplate(
                registry,
                ItemTypes.GOLDEN_CHESTPLATE,
                7,
                repairWith(ItemTags.REPAIRS_GOLD_ARMOR),
                Sound.ARMOR_EQUIP_GOLD);
        configureArmorHelmet(
                registry, ItemTypes.GOLDEN_HELMET, 7, repairWith(ItemTags.REPAIRS_GOLD_ARMOR), Sound.ARMOR_EQUIP_GOLD);
        configureHoe(registry, ItemTypes.GOLDEN_HOE, ToolMaterials.GOLD, repairWith(ItemTags.GOLD_TOOL_MATERIALS));
        configureArmorLeggings(
                registry,
                ItemTypes.GOLDEN_LEGGINGS,
                7,
                repairWith(ItemTags.REPAIRS_GOLD_ARMOR),
                Sound.ARMOR_EQUIP_GOLD);
        configurePickaxe(
                registry, ItemTypes.GOLDEN_PICKAXE, ToolMaterials.GOLD, repairWith(ItemTags.GOLD_TOOL_MATERIALS));
        configureShovel(
                registry, ItemTypes.GOLDEN_SHOVEL, ToolMaterials.GOLD, repairWith(ItemTags.GOLD_TOOL_MATERIALS));
        configureSpear(registry, ItemTypes.GOLDEN_SPEAR, ToolMaterials.GOLD, repairWith(ItemTags.GOLD_TOOL_MATERIALS));
        configureSword(registry, ItemTypes.GOLDEN_SWORD, ToolMaterials.GOLD, repairWith(ItemTags.GOLD_TOOL_MATERIALS));
        registry.configure(ItemTypes.GUARDIAN_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.GUARDIAN));
        registry.configure(ItemTypes.HAPPY_GHAST_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.HAPPY_GHAST));
        registry.configure(ItemTypes.HOGLIN_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.HOGLIN));
        registry.configure(ItemTypes.HOPPER_MINECART)
                .set(ItemComponents.USE_ON, MinecartItemHandlers.useOn(EntityTypes.HOPPER_MINECART));
        registry.configure(ItemTypes.HORSE_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.HORSE));
        registry.configure(ItemTypes.HUSK_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.HUSK));
        configureAxe(registry, IRON_AXE, ToolMaterials.IRON, repairWith(ItemTags.IRON_TOOL_MATERIALS));
        configureArmorBoots(registry, IRON_BOOTS, 15, repairWith(ItemTags.REPAIRS_IRON_ARMOR), Sound.ARMOR_EQUIP_IRON);
        configureArmorChestplate(
                registry,
                ItemTypes.IRON_CHESTPLATE,
                15,
                repairWith(ItemTags.REPAIRS_IRON_ARMOR),
                Sound.ARMOR_EQUIP_IRON);
        registry.configure(ItemTypes.IRON_GOLEM_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.IRON_GOLEM));
        configureArmorHelmet(
                registry, ItemTypes.IRON_HELMET, 15, repairWith(ItemTags.REPAIRS_IRON_ARMOR), Sound.ARMOR_EQUIP_IRON);
        configureHoe(registry, ItemTypes.IRON_HOE, ToolMaterials.IRON, repairWith(ItemTags.IRON_TOOL_MATERIALS));
        configureArmorLeggings(
                registry, ItemTypes.IRON_LEGGINGS, 15, repairWith(ItemTags.REPAIRS_IRON_ARMOR), Sound.ARMOR_EQUIP_IRON);
        configurePickaxe(registry, IRON_PICKAXE, ToolMaterials.IRON, repairWith(ItemTags.IRON_TOOL_MATERIALS));
        configureShovel(registry, ItemTypes.IRON_SHOVEL, ToolMaterials.IRON, repairWith(ItemTags.IRON_TOOL_MATERIALS));
        configureSpear(registry, ItemTypes.IRON_SPEAR, ToolMaterials.IRON, repairWith(ItemTags.IRON_TOOL_MATERIALS));
        configureSword(registry, ItemTypes.IRON_SWORD, ToolMaterials.IRON, repairWith(ItemTags.IRON_TOOL_MATERIALS));
        registry.configure(ItemTypes.LAVA_BUCKET)
                .set(ItemComponents.USE_ON, BucketItemHandlers.place(BlockStates.LAVA));
        configureArmorBoots(
                registry,
                ItemTypes.LEATHER_BOOTS,
                5,
                repairWith(ItemTags.REPAIRS_LEATHER_ARMOR),
                Sound.ARMOR_EQUIP_LEATHER);
        configureArmorChestplate(
                registry,
                ItemTypes.LEATHER_CHESTPLATE,
                5,
                repairWith(ItemTags.REPAIRS_LEATHER_ARMOR),
                Sound.ARMOR_EQUIP_LEATHER);
        configureArmorHelmet(
                registry,
                ItemTypes.LEATHER_HELMET,
                5,
                repairWith(ItemTags.REPAIRS_LEATHER_ARMOR),
                Sound.ARMOR_EQUIP_LEATHER);
        configureArmorLeggings(
                registry,
                ItemTypes.LEATHER_LEGGINGS,
                5,
                repairWith(ItemTags.REPAIRS_LEATHER_ARMOR),
                Sound.ARMOR_EQUIP_LEATHER);
        registry.configure(ItemTypes.LLAMA_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.LLAMA));
        configureDamageableEnchantableTool(
                registry,
                ItemTypes.MACE,
                VanillaTools.weapon(),
                500,
                repairWith(BREEZE_ROD.getId()),
                EnchantmentTarget.FIRE_ASPECT,
                EnchantmentTarget.MACE,
                EnchantmentTarget.WEAPON,
                EnchantmentTarget.BREAKABLE,
                EnchantmentTarget.VANISHABLE);
        registry.configure(ItemTypes.MAGMA_CUBE_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.MAGMA_CUBE));
        registry.configure(ItemTypes.MINECART)
                .set(ItemComponents.USE_ON, MinecartItemHandlers.useOn(EntityTypes.MINECART));
        registry.configure(ItemTypes.MOOSHROOM_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.MOOSHROOM));
        registry.configure(ItemTypes.MULE_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.MULE));
        registry.configure(ItemTypes.NAUTILUS_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.NAUTILUS));
        configureAxe(
                registry,
                ItemTypes.NETHERITE_AXE,
                ToolMaterials.NETHERITE,
                repairWith(ItemTags.NETHERITE_TOOL_MATERIALS));
        configureArmorBoots(
                registry,
                ItemTypes.NETHERITE_BOOTS,
                37,
                repairWith(ItemTags.REPAIRS_NETHERITE_ARMOR),
                Sound.ARMOR_EQUIP_NETHERITE);
        configureArmorChestplate(
                registry,
                ItemTypes.NETHERITE_CHESTPLATE,
                37,
                repairWith(ItemTags.REPAIRS_NETHERITE_ARMOR),
                Sound.ARMOR_EQUIP_NETHERITE);
        configureArmorHelmet(
                registry,
                ItemTypes.NETHERITE_HELMET,
                37,
                repairWith(ItemTags.REPAIRS_NETHERITE_ARMOR),
                Sound.ARMOR_EQUIP_NETHERITE);
        configureHoe(
                registry,
                ItemTypes.NETHERITE_HOE,
                ToolMaterials.NETHERITE,
                repairWith(ItemTags.NETHERITE_TOOL_MATERIALS));
        configureArmorLeggings(
                registry,
                ItemTypes.NETHERITE_LEGGINGS,
                37,
                repairWith(ItemTags.REPAIRS_NETHERITE_ARMOR),
                Sound.ARMOR_EQUIP_NETHERITE);
        configurePickaxe(
                registry,
                ItemTypes.NETHERITE_PICKAXE,
                ToolMaterials.NETHERITE,
                repairWith(ItemTags.NETHERITE_TOOL_MATERIALS));
        configureShovel(
                registry,
                ItemTypes.NETHERITE_SHOVEL,
                ToolMaterials.NETHERITE,
                repairWith(ItemTags.NETHERITE_TOOL_MATERIALS));
        configureSpear(
                registry,
                ItemTypes.NETHERITE_SPEAR,
                ToolMaterials.NETHERITE,
                repairWith(ItemTags.NETHERITE_TOOL_MATERIALS));
        configureSword(
                registry,
                ItemTypes.NETHERITE_SWORD,
                ToolMaterials.NETHERITE,
                repairWith(ItemTags.NETHERITE_TOOL_MATERIALS));
        registry.configure(ItemTypes.NPC_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.NPC));
        registry.configure(ItemTypes.OCELOT_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.OCELOT));
        registry.configure(ItemTypes.PANDA_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.PANDA));
        registry.configure(ItemTypes.PARCHED_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.PARCHED));
        registry.configure(ItemTypes.PARROT_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.PARROT));
        registry.configure(ItemTypes.PHANTOM_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.PHANTOM));
        registry.configure(ItemTypes.PIG_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.PIG));
        registry.configure(ItemTypes.PIGLIN_BRUTE_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.PIGLIN_BRUTE));
        registry.configure(ItemTypes.PIGLIN_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.PIGLIN));
        registry.configure(ItemTypes.PILLAGER_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.PILLAGER));
        registry.configure(ItemTypes.POLAR_BEAR_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.POLAR_BEAR));
        registry.configure(ItemTypes.POWDER_SNOW_BUCKET)
                .set(ItemComponents.GET_BLOCK, item -> Optional.of(BlockTypes.POWDER_SNOW.getDefaultState()))
                .set(
                        ItemComponents.USE_ON,
                        BucketItemHandlers.placePowderSnow(BlockTypes.POWDER_SNOW.getDefaultState()));
        registry.configure(ItemTypes.PUFFERFISH_BUCKET)
                .set(ItemComponents.USE_ON, BucketItemHandlers.placeEntity(BlockStates.WATER, EntityTypes.PUFFERFISH));
        registry.configure(ItemTypes.PUFFERFISH_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.PUFFERFISH));
        registry.configure(ItemTypes.RABBIT_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.RABBIT));
        registry.configure(ItemTypes.RAVAGER_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.RAVAGER));
        registry.configure(ItemTypes.SALMON_BUCKET)
                .set(ItemComponents.USE_ON, BucketItemHandlers.placeEntity(BlockStates.WATER, EntityTypes.SALMON));
        registry.configure(ItemTypes.SALMON_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.SALMON));
        configureTool(registry, SHEARS, VanillaTools.shears(), 238, repairWith());
        registry.configure(ItemTypes.SHEEP_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.SHEEP));
        registry.configure(ItemTypes.SHULKER_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.SHULKER));
        registry.configure(ItemTypes.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE);
        registry.configure(ItemTypes.SILVERFISH_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.SILVERFISH));
        registry.configure(ItemTypes.SKELETON_HORSE_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.SKELETON_HORSE));
        registry.configure(ItemTypes.SKELETON_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.SKELETON));
        registry.configure(ItemTypes.SLIME_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.SLIME));
        registry.configure(ItemTypes.SNIFFER_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.SNIFFER));
        registry.configure(ItemTypes.SNOW_GOLEM_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.SNOW_GOLEM));
        registry.configure(ItemTypes.SPIDER_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.SPIDER));
        registry.configure(ItemTypes.SQUID_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.SQUID));
        configureAxe(registry, ItemTypes.STONE_AXE, ToolMaterials.STONE, repairWith(ItemTags.STONE_TOOL_MATERIALS));
        configureHoe(registry, STONE_HOE, ToolMaterials.STONE, repairWith(ItemTags.STONE_TOOL_MATERIALS));
        configurePickaxe(registry, STONE_PICKAXE, ToolMaterials.STONE, repairWith(ItemTags.STONE_TOOL_MATERIALS));
        configureShovel(registry, STONE_SHOVEL, ToolMaterials.STONE, repairWith(ItemTags.STONE_TOOL_MATERIALS));
        configureSpear(registry, STONE_SPEAR, ToolMaterials.STONE, repairWith(ItemTags.STONE_TOOL_MATERIALS));
        configureSword(registry, STONE_SWORD, ToolMaterials.STONE, repairWith(ItemTags.STONE_TOOL_MATERIALS));
        registry.configure(ItemTypes.STRAY_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.STRAY));
        registry.configure(ItemTypes.STRIDER_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.STRIDER));
        registry.configure(ItemTypes.STRING)
                .set(ItemComponents.GET_BLOCK, item -> Optional.of(BlockTypes.TRIP_WIRE.getDefaultState()));
        registry.configure(ItemTypes.SULFUR_CUBE_BUCKET)
                .set(ItemComponents.USE_ON, BucketItemHandlers.placeEntity(EntityTypes.SULFUR_CUBE));
        registry.configure(ItemTypes.SULFUR_CUBE_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.SULFUR_CUBE));
        registry.configure(ItemTypes.SWEET_BERRIES)
                .set(
                        ItemComponents.GET_BLOCK,
                        item -> Optional.of(
                                BlockTypes.SWEET_BERRY_BUSH.getDefaultState().withTrait(BlockTraits.GROWTH, 0)));
        registry.configure(ItemTypes.TADPOLE_BUCKET)
                .set(ItemComponents.USE_ON, BucketItemHandlers.placeEntity(BlockStates.WATER, EntityTypes.TADPOLE));
        registry.configure(ItemTypes.TADPOLE_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.TADPOLE));
        registry.configure(ItemTypes.TNT_MINECART)
                .set(ItemComponents.USE_ON, MinecartItemHandlers.useOn(EntityTypes.TNT_MINECART));
        registry.configure(ItemTypes.TRADER_LLAMA_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.TRADER_LLAMA));
        configureDamageableEnchantableTool(
                registry,
                ItemTypes.TRIDENT,
                VanillaTools.weapon(),
                250,
                repairWith(),
                EnchantmentTarget.TRIDENT,
                EnchantmentTarget.WEAPON,
                EnchantmentTarget.BREAKABLE,
                EnchantmentTarget.VANISHABLE);
        registry.configure(ItemTypes.TROPICAL_FISH_BUCKET)
                .set(
                        ItemComponents.USE_ON,
                        BucketItemHandlers.placeEntity(BlockStates.WATER, EntityTypes.TROPICAL_FISH));
        registry.configure(ItemTypes.TROPICAL_FISH_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.TROPICAL_FISH));
        configureArmorHelmet(
                registry,
                ItemTypes.TURTLE_HELMET,
                25,
                repairWith(ItemTags.REPAIRS_TURTLE_HELMET),
                Sound.ARMOR_EQUIP_GENERIC);
        registry.configure(ItemTypes.TURTLE_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.TURTLE));
        registry.configure(ItemTypes.VEX_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.VEX));
        registry.configure(ItemTypes.VILLAGER_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.VILLAGER));
        registry.configure(ItemTypes.VINDICATOR_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.VINDICATOR));
        registry.configure(ItemTypes.WANDERING_TRADER_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.WANDERING_TRADER));
        registry.configure(ItemTypes.WARDEN_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.WARDEN));
        configureDamageableEnchantable(
                registry,
                ItemTypes.WARPED_FUNGUS_ON_A_STICK,
                100,
                repairWith(),
                EnchantmentTarget.BREAKABLE,
                EnchantmentTarget.VANISHABLE);
        registry.configure(ItemTypes.WATER_BUCKET)
                .set(ItemComponents.USE_ON, BucketItemHandlers.place(BlockStates.WATER));
        registry.configure(ItemTypes.WITCH_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.WITCH));
        registry.configure(ItemTypes.WITHER_SKELETON_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.WITHER_SKELETON));
        registry.configure(ItemTypes.WITHER_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.WITHER));
        registry.configure(ItemTypes.WOLF_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.WOLF));
        configureAxe(
                registry,
                ItemTypes.WOODEN_AXE,
                ToolMaterials.WOOD,
                CloudItemRegistry.repairWith(ItemTags.WOODEN_TOOL_MATERIALS));
        configureHoe(
                registry,
                ItemTypes.WOODEN_HOE,
                ToolMaterials.WOOD,
                CloudItemRegistry.repairWith(ItemTags.WOODEN_TOOL_MATERIALS));
        configurePickaxe(
                registry,
                ItemTypes.WOODEN_PICKAXE,
                ToolMaterials.WOOD,
                CloudItemRegistry.repairWith(ItemTags.WOODEN_TOOL_MATERIALS));
        configureShovel(
                registry,
                ItemTypes.WOODEN_SHOVEL,
                ToolMaterials.WOOD,
                CloudItemRegistry.repairWith(ItemTags.WOODEN_TOOL_MATERIALS));
        configureSpear(
                registry,
                ItemTypes.WOODEN_SPEAR,
                ToolMaterials.WOOD,
                CloudItemRegistry.repairWith(ItemTags.WOODEN_TOOL_MATERIALS));
        configureSword(
                registry,
                ItemTypes.WOODEN_SWORD,
                ToolMaterials.WOOD,
                CloudItemRegistry.repairWith(ItemTags.WOODEN_TOOL_MATERIALS));
        registry.configure(ItemTypes.ZOGLIN_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.ZOGLIN));
        registry.configure(ItemTypes.ZOMBIE_HORSE_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.ZOMBIE_HORSE));
        registry.configure(ItemTypes.ZOMBIE_NAUTILUS_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.ZOMBIE_NAUTILUS));
        registry.configure(ItemTypes.ZOMBIE_PIGMAN_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.ZOMBIE_PIGMAN));
        registry.configure(ItemTypes.ZOMBIE_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.ZOMBIE));
        registry.configure(ItemTypes.ZOMBIE_VILLAGER_SPAWN_EGG)
                .set(ItemComponents.USE_ON, SpawnEggItemHandlers.useOn(EntityTypes.ZOMBIE_VILLAGER));
    }

    private void configureArmorHelmet(
            CloudItemRegistry registry,
            ItemType type,
            int materialDurability,
            CanRepairWithHandler repairWith,
            Sound equipSound) {
        configureDamageableVanilla(registry, type, 11 * materialDurability, repairWith)
                .set(ItemComponents.GET_EQUIPMENT_SLOT, item -> EquipmentSlot.HEAD)
                .set(
                        ItemComponents.CAN_ENCHANT_WITH,
                        CloudItemRegistry.enchantableWith(
                                EnchantmentTarget.ARMOR,
                                EnchantmentTarget.ARMOR_HEAD,
                                EnchantmentTarget.BREAKABLE,
                                EnchantmentTarget.VANISHABLE,
                                EnchantmentTarget.WEARABLE))
                .set(ItemComponents.USE, ArmorItemHandlers.equip(EquipmentSlot.HEAD, equipSound));
    }

    private void configureArmorChestplate(
            CloudItemRegistry registry,
            ItemType type,
            int materialDurability,
            CanRepairWithHandler repairWith,
            Sound equipSound) {
        configureDamageableVanilla(registry, type, 16 * materialDurability, repairWith)
                .set(ItemComponents.GET_EQUIPMENT_SLOT, item -> EquipmentSlot.CHEST)
                .set(
                        ItemComponents.CAN_ENCHANT_WITH,
                        CloudItemRegistry.enchantableWith(
                                EnchantmentTarget.ARMOR,
                                EnchantmentTarget.ARMOR_CHEST,
                                EnchantmentTarget.BREAKABLE,
                                EnchantmentTarget.VANISHABLE,
                                EnchantmentTarget.WEARABLE))
                .set(ItemComponents.USE, ArmorItemHandlers.equip(EquipmentSlot.CHEST, equipSound));
    }

    private void configureArmorLeggings(
            CloudItemRegistry registry,
            ItemType type,
            int materialDurability,
            CanRepairWithHandler repairWith,
            Sound equipSound) {
        configureDamageableVanilla(registry, type, 15 * materialDurability, repairWith)
                .set(ItemComponents.GET_EQUIPMENT_SLOT, item -> EquipmentSlot.LEGS)
                .set(
                        ItemComponents.CAN_ENCHANT_WITH,
                        CloudItemRegistry.enchantableWith(
                                EnchantmentTarget.ARMOR,
                                EnchantmentTarget.ARMOR_LEGS,
                                EnchantmentTarget.BREAKABLE,
                                EnchantmentTarget.VANISHABLE,
                                EnchantmentTarget.WEARABLE))
                .set(ItemComponents.USE, ArmorItemHandlers.equip(EquipmentSlot.LEGS, equipSound));
    }

    private void configureElytra(CloudItemRegistry registry, ItemType type, CanRepairWithHandler repairWith) {
        configureDamageableVanilla(registry, type, 432, repairWith)
                .set(ItemComponents.GET_EQUIPMENT_SLOT, item -> EquipmentSlot.CHEST)
                .set(
                        ItemComponents.CAN_ENCHANT_WITH,
                        CloudItemRegistry.enchantableWith(
                                EnchantmentTarget.BREAKABLE, EnchantmentTarget.VANISHABLE, EnchantmentTarget.WEARABLE))
                .set(ItemComponents.USE, ArmorItemHandlers.equip(EquipmentSlot.CHEST, Sound.ARMOR_EQUIP_ELYTRA));
    }

    private void configureTool(
            CloudItemRegistry registry,
            ItemType type,
            Tool tool,
            int maxDamage,
            CanRepairWithHandler repairWith,
            EnchantmentTarget... additionalEnchantmentTargets) {
        configureDamageableVanilla(registry, type, maxDamage, repairWith)
                .set(
                        ItemComponents.CAN_ENCHANT_WITH,
                        CloudItemRegistry.toolEnchantableWith(additionalEnchantmentTargets))
                .set(ItemComponents.GET_TOOL, item -> tool)
                .set(ItemComponents.MINE_BLOCK, DefaultItemHandlers.MINE_BLOCK);
    }

    private void configureAxe(
            CloudItemRegistry registry, ItemType type, ToolMaterial material, CanRepairWithHandler repairWith) {
        configureTool(
                registry,
                type,
                VanillaTools.axe(material),
                material.getDurability(),
                repairWith,
                EnchantmentTarget.SHARP_WEAPON,
                EnchantmentTarget.WEAPON);
    }

    private void configurePickaxe(
            CloudItemRegistry registry, ItemType type, ToolMaterial material, CanRepairWithHandler repairWith) {
        configureTool(registry, type, VanillaTools.pickaxe(material), material.getDurability(), repairWith);
    }

    private void configureShovel(
            CloudItemRegistry registry, ItemType type, ToolMaterial material, CanRepairWithHandler repairWith) {
        configureTool(registry, type, VanillaTools.shovel(material.getSpeed()), material.getDurability(), repairWith);
    }

    private void configureHoe(
            CloudItemRegistry registry, ItemType type, ToolMaterial material, CanRepairWithHandler repairWith) {
        configureTool(registry, type, VanillaTools.hoe(material), material.getDurability(), repairWith);
    }

    private void configureSword(
            CloudItemRegistry registry, ItemType type, ToolMaterial material, CanRepairWithHandler repairWith) {
        configureDamageableVanilla(registry, type, material.getDurability(), repairWith)
                .set(
                        ItemComponents.CAN_ENCHANT_WITH,
                        CloudItemRegistry.enchantableWith(
                                EnchantmentTarget.FIRE_ASPECT,
                                EnchantmentTarget.MELEE_WEAPON,
                                EnchantmentTarget.SHARP_WEAPON,
                                EnchantmentTarget.WEAPON,
                                EnchantmentTarget.BREAKABLE,
                                EnchantmentTarget.VANISHABLE))
                .set(ItemComponents.GET_TOOL, item -> VanillaTools.sword())
                .set(ItemComponents.MINE_BLOCK, DefaultItemHandlers.MINE_BLOCK);
    }

    private void configureSpear(
            CloudItemRegistry registry, ItemType type, ToolMaterial material, CanRepairWithHandler repairWith) {
        configureDamageableVanilla(registry, type, material.getDurability(), repairWith)
                .set(
                        ItemComponents.CAN_ENCHANT_WITH,
                        CloudItemRegistry.enchantableWith(
                                EnchantmentTarget.FIRE_ASPECT,
                                EnchantmentTarget.MELEE_WEAPON,
                                EnchantmentTarget.SHARP_WEAPON,
                                EnchantmentTarget.SPEAR,
                                EnchantmentTarget.WEAPON,
                                EnchantmentTarget.BREAKABLE,
                                EnchantmentTarget.VANISHABLE));
    }

    private void configureArmorBoots(
            CloudItemRegistry registry,
            ItemType type,
            int materialDurability,
            CanRepairWithHandler repairWith,
            Sound equipSound) {
        configureDamageableVanilla(registry, type, 13 * materialDurability, repairWith)
                .set(ItemComponents.GET_EQUIPMENT_SLOT, item -> EquipmentSlot.FEET)
                .set(
                        ItemComponents.CAN_ENCHANT_WITH,
                        CloudItemRegistry.enchantableWith(
                                EnchantmentTarget.ARMOR,
                                EnchantmentTarget.ARMOR_FEET,
                                EnchantmentTarget.BREAKABLE,
                                EnchantmentTarget.VANISHABLE,
                                EnchantmentTarget.WEARABLE))
                .set(ItemComponents.USE, ArmorItemHandlers.equip(EquipmentSlot.FEET, equipSound));
    }

    private void configureDamageableEnchantableTool(
            CloudItemRegistry registry,
            ItemType type,
            Tool tool,
            int maxDamage,
            CanRepairWithHandler repairWith,
            EnchantmentTarget... enchantmentTargets)
            throws RegistryException {
        CloudComponentMap components =
                configureDamageableEnchantable(registry, type, maxDamage, repairWith, enchantmentTargets);
        components.set(ItemComponents.GET_TOOL, item -> tool);
        components.set(ItemComponents.MINE_BLOCK, DefaultItemHandlers.MINE_BLOCK);
    }

    private CloudComponentMap configureDamageableEnchantable(
            CloudItemRegistry registry,
            ItemType type,
            int maxDamage,
            CanRepairWithHandler repairWith,
            EnchantmentTarget... enchantmentTargets) {
        CloudComponentMap components = configureDamageableVanilla(registry, type, maxDamage, repairWith);
        components.set(ItemComponents.CAN_ENCHANT_WITH, CloudItemRegistry.enchantableWith(enchantmentTargets));
        return components;
    }

    private CloudComponentMap configureDamageableVanilla(
            CloudItemRegistry registry, ItemType type, int maxDamage, CanRepairWithHandler repairWith) {
        CloudComponentMap components = (CloudComponentMap) registry.configure(type);
        components.set(ItemComponents.DAMAGEABLE, () -> true);
        components.set(ItemComponents.GET_DAMAGE_CHANCE, DefaultItemHandlers.GET_DAMAGE_CHANCE);
        components.set(ItemComponents.GET_MAX_DAMAGE, item -> maxDamage);
        components.set(ItemComponents.GET_MAX_STACK_SIZE, item -> 1);
        components.set(ItemComponents.ON_DAMAGE, DefaultItemHandlers.ON_DAMAGE);
        components.set(ItemComponents.CAN_REPAIR_WITH, repairWith);
        return components;
    }
}
