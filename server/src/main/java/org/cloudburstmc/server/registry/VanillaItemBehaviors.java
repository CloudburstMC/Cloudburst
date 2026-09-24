package org.cloudburstmc.server.registry;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.enchantment.EnchantmentTarget;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.item.*;
import org.cloudburstmc.api.item.component.ArmorComponent;
import org.cloudburstmc.api.item.component.CanRepairWithHandler;
import org.cloudburstmc.api.item.component.SpawnEggComponent;
import org.cloudburstmc.api.registry.RegistryException;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.item.ArmorMaterial;
import org.cloudburstmc.server.item.VanillaArmorMaterials;
import org.cloudburstmc.server.item.VanillaTools;
import org.cloudburstmc.server.item.component.*;
import org.cloudburstmc.server.item.serializer.BannerSerializer;
import org.cloudburstmc.server.item.serializer.FireworkRocketSerializer;
import org.cloudburstmc.server.item.serializer.FireworkStarSerializer;
import org.cloudburstmc.server.item.serializer.OminousBottleItemSerializer;
import org.cloudburstmc.server.item.serializer.PotionItemSerializer;
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
        configureSpawnEgg(registry, ItemTypes.AGENT_SPAWN_EGG, EntityTypes.AGENT);
        configureSpawnEgg(registry, ItemTypes.ALLAY_SPAWN_EGG, EntityTypes.ALLAY);
        configureSpawnEgg(registry, ItemTypes.ARMADILLO_SPAWN_EGG, EntityTypes.ARMADILLO);
        registry.configure(ItemTypes.AXOLOTL_BUCKET)
                .set(ItemBehaviors.USE_ON, BucketItemHandlers.placeEntity(BlockStates.WATER, EntityTypes.AXOLOTL));
        configureSpawnEgg(registry, ItemTypes.AXOLOTL_SPAWN_EGG, EntityTypes.AXOLOTL);
        registry.configure(ItemTypes.BANNER, new BannerSerializer());
        configureSpawnEgg(registry, ItemTypes.BAT_SPAWN_EGG, EntityTypes.BAT);
        configureSpawnEgg(registry, ItemTypes.BEE_SPAWN_EGG, EntityTypes.BEE);
        configureSpawnEgg(registry, ItemTypes.BLAZE_SPAWN_EGG, EntityTypes.BLAZE);
        configureSpawnEgg(registry, ItemTypes.BOGGED_SPAWN_EGG, EntityTypes.BOGGED);
        registry.configure(ItemTypes.BONE_MEAL).set(ItemBehaviors.USE_ON, BoneMealItemHandlers.USE_ON);
        configureDamageableEnchantable(
                registry,
                ItemTypes.BOW,
                384,
                repairWith(),
                EnchantmentTarget.BOW,
                EnchantmentTarget.BREAKABLE,
                EnchantmentTarget.VANISHABLE
        )
                .set(ItemBehaviors.USE, BowItemHandlers.USE)
                .set(ItemBehaviors.RELEASE_USE, BowItemHandlers.RELEASE);
        configureSpawnEgg(registry, ItemTypes.BREEZE_SPAWN_EGG, EntityTypes.BREEZE);
        configureDamageableEnchantable(registry, ItemTypes.BRUSH, 64, repairWith(), EnchantmentTarget.BREAKABLE, EnchantmentTarget.VANISHABLE);
        registry.configure(ItemTypes.BUCKET).set(ItemBehaviors.USE_ON, BucketItemHandlers.PICK_UP);
        configureSpawnEgg(registry, ItemTypes.CAMEL_HUSK_SPAWN_EGG, EntityTypes.CAMEL_HUSK);
        configureSpawnEgg(registry, ItemTypes.CAMEL_SPAWN_EGG, EntityTypes.CAMEL);
        configureDamageableEnchantable(registry, ItemTypes.CARROT_ON_A_STICK, 25, repairWith(), EnchantmentTarget.BREAKABLE, EnchantmentTarget.VANISHABLE);
        configureSpawnEgg(registry, ItemTypes.CAT_SPAWN_EGG, EntityTypes.CAT);
        configureSpawnEgg(registry, ItemTypes.CAVE_SPIDER_SPAWN_EGG, EntityTypes.CAVE_SPIDER);
        configureArmor(registry, ItemTypes.CHAINMAIL_BOOTS, VanillaArmorMaterials.CHAINMAIL, EquipmentSlot.FEET);
        configureArmor(registry, ItemTypes.CHAINMAIL_CHESTPLATE, VanillaArmorMaterials.CHAINMAIL, EquipmentSlot.CHEST);
        configureArmor(registry, ItemTypes.CHAINMAIL_HELMET, VanillaArmorMaterials.CHAINMAIL, EquipmentSlot.HEAD);
        configureArmor(registry, ItemTypes.CHAINMAIL_LEGGINGS, VanillaArmorMaterials.CHAINMAIL, EquipmentSlot.LEGS);
        registry.configure(ItemTypes.CHEST_MINECART)
                .set(ItemBehaviors.USE_ON, MinecartItemHandlers.useOn(EntityTypes.CHEST_MINECART));
        configureSpawnEgg(registry, ItemTypes.CHICKEN_SPAWN_EGG, EntityTypes.CHICKEN);
        registry.configure(ItemTypes.CHORUS_FRUIT)
                .set(ItemBehaviors.FINISH_USE, ChorusFruitItemHandlers.FINISH_USE)
                .set(ItemBehaviors.USE, ChorusFruitItemHandlers.USE)
                .set(ItemBehaviors.USE_DURATION_TICKS, 32);
        registry.configure(ItemTypes.COD_BUCKET)
                .set(ItemBehaviors.USE_ON, BucketItemHandlers.placeEntity(BlockStates.WATER, EntityTypes.COD));
        configureSpawnEgg(registry, ItemTypes.COD_SPAWN_EGG, EntityTypes.COD);
        registry.configure(ItemTypes.COMMAND_BLOCK_MINECART)
                .set(ItemBehaviors.USE_ON, MinecartItemHandlers.useOn(EntityTypes.COMMAND_BLOCK_MINECART));
        configureAxe(registry, ItemTypes.COPPER_AXE, ToolMaterials.COPPER, 7, repairWith(ItemTags.COPPER_TOOL_MATERIALS));
        configureArmor(registry, ItemTypes.COPPER_BOOTS, VanillaArmorMaterials.COPPER, EquipmentSlot.FEET);
        configureArmor(registry, ItemTypes.COPPER_CHESTPLATE, VanillaArmorMaterials.COPPER, EquipmentSlot.CHEST);
        configureSpawnEgg(registry, ItemTypes.COPPER_GOLEM_SPAWN_EGG, EntityTypes.COPPER_GOLEM);
        configureArmor(registry, ItemTypes.COPPER_HELMET, VanillaArmorMaterials.COPPER, EquipmentSlot.HEAD);
        configureHoe(registry, COPPER_HOE, ToolMaterials.COPPER, -1, repairWith(ItemTags.COPPER_TOOL_MATERIALS));
        configureArmor(registry, ItemTypes.COPPER_LEGGINGS, VanillaArmorMaterials.COPPER, EquipmentSlot.LEGS);
        configurePickaxe(registry, ItemTypes.COPPER_PICKAXE, ToolMaterials.COPPER, repairWith(ItemTags.COPPER_TOOL_MATERIALS));
        configureShovel(registry, ItemTypes.COPPER_SHOVEL, ToolMaterials.COPPER, repairWith(ItemTags.COPPER_TOOL_MATERIALS));
        configureSpear(registry, ItemTypes.COPPER_SPEAR, ToolMaterials.COPPER, repairWith(ItemTags.COPPER_TOOL_MATERIALS),
                SoundEvent.COPPER_SPEAR_ATTACK_HIT, SoundEvent.COPPER_SPEAR_ATTACK_MISS, SoundEvent.COPPER_SPEAR_USE,
                new SpearProfile(17, 13, 0.82f, 80, 12, 165, 5.1f, 250, 4.6f));
        configureSword(registry, ItemTypes.COPPER_SWORD, ToolMaterials.COPPER, repairWith(ItemTags.COPPER_TOOL_MATERIALS));
        configureSpawnEgg(registry, ItemTypes.COW_SPAWN_EGG, EntityTypes.COW);
        configureSpawnEgg(registry, ItemTypes.CREAKING_SPAWN_EGG, EntityTypes.CREAKING);
        registry.configure(ItemTypes.CREEPER_BANNER_PATTERN);
        configureSpawnEgg(registry, ItemTypes.CREEPER_SPAWN_EGG, EntityTypes.CREEPER);
        configureDamageableEnchantable(
                registry,
                ItemTypes.CROSSBOW,
                465,
                repairWith(),
                EnchantmentTarget.CROSSBOW,
                EnchantmentTarget.BREAKABLE,
                EnchantmentTarget.VANISHABLE
        )
                .set(ItemBehaviors.USE, CrossbowItemHandlers.USE)
                .set(ItemBehaviors.USE_TICK, CrossbowItemHandlers.USE_TICK);
        configureAxe(registry, ItemTypes.DIAMOND_AXE, ToolMaterials.DIAMOND, 5, repairWith(ItemTags.DIAMOND_TOOL_MATERIALS));
        configureArmor(registry, ItemTypes.DIAMOND_BOOTS, VanillaArmorMaterials.DIAMOND, EquipmentSlot.FEET);
        configureArmor(registry, ItemTypes.DIAMOND_CHESTPLATE, VanillaArmorMaterials.DIAMOND, EquipmentSlot.CHEST);
        configureArmor(registry, ItemTypes.DIAMOND_HELMET, VanillaArmorMaterials.DIAMOND, EquipmentSlot.HEAD);
        configureHoe(registry, ItemTypes.DIAMOND_HOE, ToolMaterials.DIAMOND, -3, repairWith(ItemTags.DIAMOND_TOOL_MATERIALS));
        registry.configure(ItemTypes.DIAMOND_HORSE_ARMOR);
        configureArmor(registry, ItemTypes.DIAMOND_LEGGINGS, VanillaArmorMaterials.DIAMOND, EquipmentSlot.LEGS);
        configurePickaxe(registry, ItemTypes.DIAMOND_PICKAXE, ToolMaterials.DIAMOND, repairWith(ItemTags.DIAMOND_TOOL_MATERIALS));
        configureShovel(registry, ItemTypes.DIAMOND_SHOVEL, ToolMaterials.DIAMOND, repairWith(ItemTags.DIAMOND_TOOL_MATERIALS));
        configureSpear(registry, ItemTypes.DIAMOND_SPEAR, ToolMaterials.DIAMOND, repairWith(ItemTags.DIAMOND_TOOL_MATERIALS),
                SoundEvent.DIAMOND_SPEAR_ATTACK_HIT, SoundEvent.DIAMOND_SPEAR_ATTACK_MISS, SoundEvent.DIAMOND_SPEAR_USE,
                new SpearProfile(21, 10, 1.075f, 60, 10, 130, 5.1f, 200, 4.6f));
        configureSword(registry, ItemTypes.DIAMOND_SWORD, ToolMaterials.DIAMOND, repairWith(ItemTags.DIAMOND_TOOL_MATERIALS));
        configureSpawnEgg(registry, ItemTypes.DOLPHIN_SPAWN_EGG, EntityTypes.DOLPHIN);
        configureSpawnEgg(registry, ItemTypes.DONKEY_SPAWN_EGG, EntityTypes.DONKEY);
        configureSpawnEgg(registry, ItemTypes.DROWNED_SPAWN_EGG, EntityTypes.DROWNED);
        registry.configure(ItemTypes.EGG).set(ItemBehaviors.USE,
                ThrowableItemHandlers.throwProjectile(EntityTypes.EGG, 1.5f, 0));
        configureSpawnEgg(registry, ItemTypes.ELDER_GUARDIAN_SPAWN_EGG, EntityTypes.ELDER_GUARDIAN);
        configureElytra(registry, repairWith(PHANTOM_MEMBRANE.getId()));
        registry.configure(ItemTypes.ENCHANTED_BOOK).set(ItemBehaviors.CAN_ENCHANT_WITH, (item, enchantment) -> true);
        registry.configure(ItemTypes.END_CRYSTAL).set(ItemBehaviors.USE_ON, EndCrystalItemHandlers.USE_ON);
        configureSpawnEgg(registry, ItemTypes.ENDER_DRAGON_SPAWN_EGG, EntityTypes.ENDER_DRAGON);
        registry.configure(ItemTypes.ENDER_EYE).set(ItemBehaviors.USE_ON, EnderEyeItemHandlers.USE_ON);
        registry.configure(ItemTypes.ENDER_PEARL).set(ItemBehaviors.USE, EnderPearlItemHandlers.USE);
        configureSpawnEgg(registry, ItemTypes.ENDERMAN_SPAWN_EGG, EntityTypes.ENDERMAN);
        configureSpawnEgg(registry, ItemTypes.ENDERMITE_SPAWN_EGG, EntityTypes.ENDERMITE);
        configureSpawnEgg(registry, ItemTypes.EVOKER_SPAWN_EGG, EntityTypes.EVOCATION_ILLAGER);
        registry.configure(ItemTypes.EXPERIENCE_BOTTLE).set(ItemBehaviors.USE,
                ThrowableItemHandlers.throwProjectile(EntityTypes.XP_BOTTLE, 0.7f, -20));
        registry.configure(ItemTypes.FIRE_CHARGE).set(ItemBehaviors.USE_ON, FireChargeItemHandlers.USE_ON);
        registry.configure(ItemTypes.FIREWORK_ROCKET, new FireworkRocketSerializer())
                .set(ItemBehaviors.USE, FireworkRocketItemHandlers.USE)
                .set(ItemBehaviors.USE_ON, FireworkRocketItemHandlers.USE_ON);
        registry.configure(ItemTypes.FIREWORK_STAR, new FireworkStarSerializer());
        configureDamageableEnchantable(
                        registry,
                        ItemTypes.FISHING_ROD,
                        64,
                        repairWith(),
                        EnchantmentTarget.FISHING_ROD,
                        EnchantmentTarget.BREAKABLE,
                        EnchantmentTarget.VANISHABLE
        )
                .set(ItemBehaviors.USE, FishingRodItemHandlers.USE);
        registry.configure(ItemTypes.FLINT_AND_STEEL).set(ItemBehaviors.USE_ON, FlintAndSteelItemHandlers.USE_ON);
        configureSpawnEgg(registry, ItemTypes.FOX_SPAWN_EGG, EntityTypes.FOX);
        configureSpawnEgg(registry, ItemTypes.FROG_SPAWN_EGG, EntityTypes.FROG);
        configureSpawnEgg(registry, ItemTypes.GHAST_SPAWN_EGG, EntityTypes.GHAST);
        configureSpawnEgg(registry, ItemTypes.GLOW_SQUID_SPAWN_EGG, EntityTypes.GLOW_SQUID);
        configureSpawnEgg(registry, ItemTypes.GOAT_SPAWN_EGG, EntityTypes.GOAT);
        configureAxe(registry, ItemTypes.GOLDEN_AXE, ToolMaterials.GOLD, 6, repairWith(ItemTags.GOLD_TOOL_MATERIALS));
        configureArmor(registry, ItemTypes.GOLDEN_BOOTS, VanillaArmorMaterials.GOLD, EquipmentSlot.FEET);
        configureArmor(registry, ItemTypes.GOLDEN_CHESTPLATE, VanillaArmorMaterials.GOLD, EquipmentSlot.CHEST);
        configureArmor(registry, ItemTypes.GOLDEN_HELMET, VanillaArmorMaterials.GOLD, EquipmentSlot.HEAD);
        configureHoe(registry, ItemTypes.GOLDEN_HOE, ToolMaterials.GOLD, 0, repairWith(ItemTags.GOLD_TOOL_MATERIALS));
        configureArmor(registry, ItemTypes.GOLDEN_LEGGINGS, VanillaArmorMaterials.GOLD, EquipmentSlot.LEGS);
        configurePickaxe(registry, ItemTypes.GOLDEN_PICKAXE, ToolMaterials.GOLD, repairWith(ItemTags.GOLD_TOOL_MATERIALS));
        configureShovel(registry, ItemTypes.GOLDEN_SHOVEL, ToolMaterials.GOLD, repairWith(ItemTags.GOLD_TOOL_MATERIALS));
        configureSpear(registry, ItemTypes.GOLDEN_SPEAR, ToolMaterials.GOLD, repairWith(ItemTags.GOLD_TOOL_MATERIALS),
                SoundEvent.GOLDEN_SPEAR_ATTACK_HIT, SoundEvent.GOLDEN_SPEAR_ATTACK_MISS, SoundEvent.GOLDEN_SPEAR_USE,
                new SpearProfile(19, 14, 0.7f, 70, 13, 170, 5.1f, 275, 4.6f));
        configureSword(registry, ItemTypes.GOLDEN_SWORD, ToolMaterials.GOLD, repairWith(ItemTags.GOLD_TOOL_MATERIALS));
        configureSpawnEgg(registry, ItemTypes.GUARDIAN_SPAWN_EGG, EntityTypes.GUARDIAN);
        configureSpawnEgg(registry, ItemTypes.HAPPY_GHAST_SPAWN_EGG, EntityTypes.HAPPY_GHAST);
        configureSpawnEgg(registry, ItemTypes.HOGLIN_SPAWN_EGG, EntityTypes.HOGLIN);
        registry.configure(ItemTypes.HOPPER_MINECART)
                .set(ItemBehaviors.USE_ON, MinecartItemHandlers.useOn(EntityTypes.HOPPER_MINECART));
        configureSpawnEgg(registry, ItemTypes.HORSE_SPAWN_EGG, EntityTypes.HORSE);
        configureSpawnEgg(registry, ItemTypes.HUSK_SPAWN_EGG, EntityTypes.HUSK);
        configureAxe(registry, IRON_AXE, ToolMaterials.IRON, 6, repairWith(ItemTags.IRON_TOOL_MATERIALS));
        configureArmor(registry, IRON_BOOTS, VanillaArmorMaterials.IRON, EquipmentSlot.FEET);
        configureArmor(registry, ItemTypes.IRON_CHESTPLATE, VanillaArmorMaterials.IRON, EquipmentSlot.CHEST);
        configureSpawnEgg(registry, ItemTypes.IRON_GOLEM_SPAWN_EGG, EntityTypes.IRON_GOLEM);
        configureArmor(registry, ItemTypes.IRON_HELMET, VanillaArmorMaterials.IRON, EquipmentSlot.HEAD);
        configureHoe(registry, ItemTypes.IRON_HOE, ToolMaterials.IRON, -2, repairWith(ItemTags.IRON_TOOL_MATERIALS));
        configureArmor(registry, ItemTypes.IRON_LEGGINGS, VanillaArmorMaterials.IRON, EquipmentSlot.LEGS);
        configurePickaxe(registry, IRON_PICKAXE, ToolMaterials.IRON, repairWith(ItemTags.IRON_TOOL_MATERIALS));
        configureShovel(registry, ItemTypes.IRON_SHOVEL, ToolMaterials.IRON, repairWith(ItemTags.IRON_TOOL_MATERIALS));
        configureSpear(registry, ItemTypes.IRON_SPEAR, ToolMaterials.IRON, repairWith(ItemTags.IRON_TOOL_MATERIALS),
                SoundEvent.IRON_SPEAR_ATTACK_HIT, SoundEvent.IRON_SPEAR_ATTACK_MISS, SoundEvent.IRON_SPEAR_USE,
                new SpearProfile(19, 12, 0.95f, 50, 11, 135, 5.1f, 225, 4.6f));
        configureSword(registry, ItemTypes.IRON_SWORD, ToolMaterials.IRON, repairWith(ItemTags.IRON_TOOL_MATERIALS));
        registry.configure(ItemTypes.LAVA_BUCKET)
                .set(ItemBehaviors.USE_ON, BucketItemHandlers.place(BlockStates.LAVA));
        configureArmor(registry, ItemTypes.LEATHER_BOOTS, VanillaArmorMaterials.LEATHER, EquipmentSlot.FEET);
        configureArmor(registry, ItemTypes.LEATHER_CHESTPLATE, VanillaArmorMaterials.LEATHER, EquipmentSlot.CHEST);
        configureArmor(registry, ItemTypes.LEATHER_HELMET, VanillaArmorMaterials.LEATHER, EquipmentSlot.HEAD);
        configureArmor(registry, ItemTypes.LEATHER_LEGGINGS, VanillaArmorMaterials.LEATHER, EquipmentSlot.LEGS);
        configureSpawnEgg(registry, ItemTypes.LLAMA_SPAWN_EGG, EntityTypes.LLAMA);
        registry.configure(ItemTypes.LINGERING_POTION, new PotionItemSerializer())
                .set(ItemBehaviors.GET_MAX_STACK_SIZE, item -> 1)
                .set(ItemBehaviors.USE, PotionItemHandlers.THROW_LINGERING);
        configureDamageableEnchantableTool(
                registry,
                ItemTypes.MACE,
                VanillaTools.weapon(),
                500,
                6,
                repairWith(BREEZE_ROD.getId()),
                EnchantmentTarget.FIRE_ASPECT,
                EnchantmentTarget.MACE,
                EnchantmentTarget.WEAPON,
                EnchantmentTarget.BREAKABLE,
                EnchantmentTarget.VANISHABLE
        );
        configureSpawnEgg(registry, ItemTypes.MAGMA_CUBE_SPAWN_EGG, EntityTypes.MAGMA_CUBE);
        registry.configure(ItemTypes.MINECART)
                .set(ItemBehaviors.USE_ON, MinecartItemHandlers.useOn(EntityTypes.MINECART));
        configureSpawnEgg(registry, ItemTypes.MOOSHROOM_SPAWN_EGG, EntityTypes.MOOSHROOM);
        configureSpawnEgg(registry, ItemTypes.MULE_SPAWN_EGG, EntityTypes.MULE);
        configureSpawnEgg(registry, ItemTypes.NAUTILUS_SPAWN_EGG, EntityTypes.NAUTILUS);
        configureAxe(registry, ItemTypes.NETHERITE_AXE, ToolMaterials.NETHERITE, 5, repairWith(ItemTags.NETHERITE_TOOL_MATERIALS));
        configureArmor(registry, ItemTypes.NETHERITE_BOOTS, VanillaArmorMaterials.NETHERITE, EquipmentSlot.FEET);
        configureArmor(registry, ItemTypes.NETHERITE_CHESTPLATE, VanillaArmorMaterials.NETHERITE, EquipmentSlot.CHEST);
        configureArmor(registry, ItemTypes.NETHERITE_HELMET, VanillaArmorMaterials.NETHERITE, EquipmentSlot.HEAD);
        configureHoe(registry, ItemTypes.NETHERITE_HOE, ToolMaterials.NETHERITE, -4, repairWith(ItemTags.NETHERITE_TOOL_MATERIALS));
        configureArmor(registry, ItemTypes.NETHERITE_LEGGINGS, VanillaArmorMaterials.NETHERITE, EquipmentSlot.LEGS);
        configurePickaxe(registry, ItemTypes.NETHERITE_PICKAXE, ToolMaterials.NETHERITE, repairWith(ItemTags.NETHERITE_TOOL_MATERIALS));
        configureShovel(registry, ItemTypes.NETHERITE_SHOVEL, ToolMaterials.NETHERITE, repairWith(ItemTags.NETHERITE_TOOL_MATERIALS));
        configureSpear(registry, ItemTypes.NETHERITE_SPEAR, ToolMaterials.NETHERITE, repairWith(ItemTags.NETHERITE_TOOL_MATERIALS),
                SoundEvent.NETHERITE_SPEAR_ATTACK_HIT, SoundEvent.NETHERITE_SPEAR_ATTACK_MISS, SoundEvent.NETHERITE_SPEAR_USE,
                new SpearProfile(23, 8, 1.2f, 50, 9, 110, 5.1f, 175, 4.6f));
        configureSword(registry, ItemTypes.NETHERITE_SWORD, ToolMaterials.NETHERITE, repairWith(ItemTags.NETHERITE_TOOL_MATERIALS));
        configureSpawnEgg(registry, ItemTypes.NPC_SPAWN_EGG, EntityTypes.NPC);
        configureSpawnEgg(registry, ItemTypes.OCELOT_SPAWN_EGG, EntityTypes.OCELOT);
        registry.configure(ItemTypes.OMINOUS_BOTTLE, new OminousBottleItemSerializer())
                .set(ItemBehaviors.USE_DURATION_TICKS, 32)
                .set(ItemBehaviors.USE, OminousBottleItemHandlers.DRINK)
                .set(ItemBehaviors.FINISH_USE, OminousBottleItemHandlers.FINISH_DRINK);
        configureSpawnEgg(registry, ItemTypes.PANDA_SPAWN_EGG, EntityTypes.PANDA);
        configureSpawnEgg(registry, ItemTypes.PARCHED_SPAWN_EGG, EntityTypes.PARCHED);
        configureSpawnEgg(registry, ItemTypes.PARROT_SPAWN_EGG, EntityTypes.PARROT);
        configureSpawnEgg(registry, ItemTypes.PHANTOM_SPAWN_EGG, EntityTypes.PHANTOM);
        configureSpawnEgg(registry, ItemTypes.PIG_SPAWN_EGG, EntityTypes.PIG);
        configureSpawnEgg(registry, ItemTypes.PIGLIN_BRUTE_SPAWN_EGG, EntityTypes.PIGLIN_BRUTE);
        configureSpawnEgg(registry, ItemTypes.PIGLIN_SPAWN_EGG, EntityTypes.PIGLIN);
        configureSpawnEgg(registry, ItemTypes.PILLAGER_SPAWN_EGG, EntityTypes.PILLAGER);
        registry.configure(ItemTypes.POTION, new PotionItemSerializer())
                .set(ItemBehaviors.GET_MAX_STACK_SIZE, item -> 1)
                .set(ItemBehaviors.USE_DURATION_TICKS, 32)
                .set(ItemBehaviors.USE, PotionItemHandlers.DRINK)
                .set(ItemBehaviors.FINISH_USE, PotionItemHandlers.FINISH_DRINK);
        configureSpawnEgg(registry, ItemTypes.POLAR_BEAR_SPAWN_EGG, EntityTypes.POLAR_BEAR);
        registry.configure(ItemTypes.POWDER_SNOW_BUCKET)
                .set(ItemBehaviors.GET_BLOCK, item -> Optional.of(BlockTypes.POWDER_SNOW.getDefaultState()))
                .set(ItemBehaviors.USE_ON, BucketItemHandlers.placePowderSnow(BlockTypes.POWDER_SNOW.getDefaultState()));
        registry.configure(ItemTypes.PUFFERFISH_BUCKET)
                .set(ItemBehaviors.USE_ON, BucketItemHandlers.placeEntity(BlockStates.WATER, EntityTypes.PUFFERFISH));
        configureSpawnEgg(registry, ItemTypes.PUFFERFISH_SPAWN_EGG, EntityTypes.PUFFERFISH);
        configureSpawnEgg(registry, ItemTypes.RABBIT_SPAWN_EGG, EntityTypes.RABBIT);
        configureSpawnEgg(registry, ItemTypes.RAVAGER_SPAWN_EGG, EntityTypes.RAVAGER);
        registry.configure(ItemTypes.SALMON_BUCKET)
                .set(ItemBehaviors.USE_ON, BucketItemHandlers.placeEntity(BlockStates.WATER, EntityTypes.SALMON));
        configureSpawnEgg(registry, ItemTypes.SALMON_SPAWN_EGG, EntityTypes.SALMON);
        configureTool(registry, SHEARS, VanillaTools.shears(), 238, 1, repairWith());
        configureSpawnEgg(registry, ItemTypes.SHEEP_SPAWN_EGG, EntityTypes.SHEEP);
        configureSpawnEgg(registry, ItemTypes.SHULKER_SPAWN_EGG, EntityTypes.SHULKER);
        registry.configure(ItemTypes.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE);
        configureSpawnEgg(registry, ItemTypes.SILVERFISH_SPAWN_EGG, EntityTypes.SILVERFISH);
        configureSpawnEgg(registry, ItemTypes.SKELETON_HORSE_SPAWN_EGG, EntityTypes.SKELETON_HORSE);
        configureSpawnEgg(registry, ItemTypes.SKELETON_SPAWN_EGG, EntityTypes.SKELETON);
        configureSpawnEgg(registry, ItemTypes.SLIME_SPAWN_EGG, EntityTypes.SLIME);
        configureSpawnEgg(registry, ItemTypes.SNIFFER_SPAWN_EGG, EntityTypes.SNIFFER);
        registry.configure(ItemTypes.SNOWBALL).set(ItemBehaviors.USE,
                ThrowableItemHandlers.throwProjectile(EntityTypes.SNOWBALL, 1.5f, 0));
        configureSpawnEgg(registry, ItemTypes.SNOW_GOLEM_SPAWN_EGG, EntityTypes.SNOW_GOLEM);
        configureSpawnEgg(registry, ItemTypes.SPIDER_SPAWN_EGG, EntityTypes.SPIDER);
        registry.configure(ItemTypes.SPLASH_POTION, new PotionItemSerializer())
                .set(ItemBehaviors.GET_MAX_STACK_SIZE, item -> 1)
                .set(ItemBehaviors.USE, PotionItemHandlers.THROW_SPLASH);
        configureSpawnEgg(registry, ItemTypes.SQUID_SPAWN_EGG, EntityTypes.SQUID);
        configureAxe(registry, ItemTypes.STONE_AXE, ToolMaterials.STONE, 7, repairWith(ItemTags.STONE_TOOL_MATERIALS));
        configureHoe(registry, STONE_HOE, ToolMaterials.STONE, -1, repairWith(ItemTags.STONE_TOOL_MATERIALS));
        configurePickaxe(registry, STONE_PICKAXE, ToolMaterials.STONE, repairWith(ItemTags.STONE_TOOL_MATERIALS));
        configureShovel(registry, STONE_SHOVEL, ToolMaterials.STONE, repairWith(ItemTags.STONE_TOOL_MATERIALS));
        configureSpear(registry, STONE_SPEAR, ToolMaterials.STONE, repairWith(ItemTags.STONE_TOOL_MATERIALS),
                SoundEvent.STONE_SPEAR_ATTACK_HIT, SoundEvent.STONE_SPEAR_ATTACK_MISS, SoundEvent.STONE_SPEAR_USE,
                new SpearProfile(15, 14, 0.82f, 90, 13, 180, 5.1f, 275, 4.6f));
        configureSword(registry, STONE_SWORD, ToolMaterials.STONE, repairWith(ItemTags.STONE_TOOL_MATERIALS));
        configureSpawnEgg(registry, ItemTypes.STRAY_SPAWN_EGG, EntityTypes.STRAY);
        configureSpawnEgg(registry, ItemTypes.STRIDER_SPAWN_EGG, EntityTypes.STRIDER);
        registry.configure(ItemTypes.STRING)
                .set(ItemBehaviors.GET_BLOCK, item -> Optional.of(BlockTypes.TRIP_WIRE.getDefaultState()));
        registry.configure(ItemTypes.SULFUR_CUBE_BUCKET)
                .set(ItemBehaviors.USE_ON, BucketItemHandlers.placeEntity(EntityTypes.SULFUR_CUBE));
        configureSpawnEgg(registry, ItemTypes.SULFUR_CUBE_SPAWN_EGG, EntityTypes.SULFUR_CUBE);
        registry.configure(ItemTypes.SWEET_BERRIES)
                .set(ItemBehaviors.GET_BLOCK, item -> Optional.of(BlockTypes.SWEET_BERRY_BUSH.getDefaultState().withTrait(BlockTraits.GROWTH, 0)));
        registry.configure(ItemTypes.TADPOLE_BUCKET)
                .set(ItemBehaviors.USE_ON, BucketItemHandlers.placeEntity(BlockStates.WATER, EntityTypes.TADPOLE));
        configureSpawnEgg(registry, ItemTypes.TADPOLE_SPAWN_EGG, EntityTypes.TADPOLE);
        registry.configure(ItemTypes.TNT_MINECART)
                .set(ItemBehaviors.USE_ON, MinecartItemHandlers.useOn(EntityTypes.TNT_MINECART));
        configureSpawnEgg(registry, ItemTypes.TRADER_LLAMA_SPAWN_EGG, EntityTypes.TRADER_LLAMA);
        configureDamageableEnchantableTool(
                registry,
                ItemTypes.TRIDENT,
                VanillaTools.weapon(),
                250,
                9,
                repairWith(),
                EnchantmentTarget.TRIDENT,
                EnchantmentTarget.WEAPON,
                EnchantmentTarget.BREAKABLE,
                EnchantmentTarget.VANISHABLE
        );
        registry.configure(ItemTypes.TRIDENT)
                .set(ItemBehaviors.USE, TridentItemHandlers.USE)
                .set(ItemBehaviors.RELEASE_USE, TridentItemHandlers.RELEASE);
        registry.configure(ItemTypes.TROPICAL_FISH_BUCKET)
                .set(ItemBehaviors.USE_ON, BucketItemHandlers.placeEntity(BlockStates.WATER, EntityTypes.TROPICAL_FISH));
        configureSpawnEgg(registry, ItemTypes.TROPICAL_FISH_SPAWN_EGG, EntityTypes.TROPICAL_FISH);
        configureArmor(registry, ItemTypes.TURTLE_HELMET, VanillaArmorMaterials.TURTLE, EquipmentSlot.HEAD);
        configureSpawnEgg(registry, ItemTypes.TURTLE_SPAWN_EGG, EntityTypes.TURTLE);
        configureSpawnEgg(registry, ItemTypes.VEX_SPAWN_EGG, EntityTypes.VEX);
        configureSpawnEgg(registry, ItemTypes.VILLAGER_SPAWN_EGG, EntityTypes.VILLAGER);
        configureSpawnEgg(registry, ItemTypes.VINDICATOR_SPAWN_EGG, EntityTypes.VINDICATOR);
        configureSpawnEgg(registry, ItemTypes.WANDERING_TRADER_SPAWN_EGG, EntityTypes.WANDERING_TRADER);
        configureSpawnEgg(registry, ItemTypes.WARDEN_SPAWN_EGG, EntityTypes.WARDEN);
        configureDamageableEnchantable(
                registry,
                ItemTypes.WARPED_FUNGUS_ON_A_STICK,
                100,
                repairWith(),
                EnchantmentTarget.BREAKABLE,
                EnchantmentTarget.VANISHABLE
        );
        registry.configure(ItemTypes.WATER_BUCKET)
                .set(ItemBehaviors.USE_ON, BucketItemHandlers.place(BlockStates.WATER));
        configureSpawnEgg(registry, ItemTypes.WITCH_SPAWN_EGG, EntityTypes.WITCH);
        registry.configure(ItemTypes.WIND_CHARGE).set(ItemBehaviors.USE, WindChargeItemHandlers.USE);
        configureSpawnEgg(registry, ItemTypes.WITHER_SKELETON_SPAWN_EGG, EntityTypes.WITHER_SKELETON);
        configureSpawnEgg(registry, ItemTypes.WITHER_SPAWN_EGG, EntityTypes.WITHER);
        configureSpawnEgg(registry, ItemTypes.WOLF_SPAWN_EGG, EntityTypes.WOLF);
        configureAxe(registry, ItemTypes.WOODEN_AXE, ToolMaterials.WOOD, 6, repairWith(ItemTags.WOODEN_TOOL_MATERIALS));
        configureHoe(registry, ItemTypes.WOODEN_HOE, ToolMaterials.WOOD, 0, repairWith(ItemTags.WOODEN_TOOL_MATERIALS));
        configurePickaxe(registry, ItemTypes.WOODEN_PICKAXE, ToolMaterials.WOOD, repairWith(ItemTags.WOODEN_TOOL_MATERIALS));
        configureShovel(registry, ItemTypes.WOODEN_SHOVEL, ToolMaterials.WOOD, repairWith(ItemTags.WOODEN_TOOL_MATERIALS));
        configureSpear(registry, ItemTypes.WOODEN_SPEAR, ToolMaterials.WOOD, repairWith(ItemTags.WOODEN_TOOL_MATERIALS),
                SoundEvent.WOODEN_SPEAR_ATTACK_HIT, SoundEvent.WOODEN_SPEAR_ATTACK_MISS, SoundEvent.WOODEN_SPEAR_USE,
                new SpearProfile(13, 15, 0.7f, 100, 14, 200, 5.1f, 300, 4.6f));
        configureSword(registry, ItemTypes.WOODEN_SWORD, ToolMaterials.WOOD, repairWith(ItemTags.WOODEN_TOOL_MATERIALS));
        configureSpawnEgg(registry, ItemTypes.ZOGLIN_SPAWN_EGG, EntityTypes.ZOGLIN);
        configureSpawnEgg(registry, ItemTypes.ZOMBIE_HORSE_SPAWN_EGG, EntityTypes.ZOMBIE_HORSE);
        configureSpawnEgg(registry, ItemTypes.ZOMBIE_NAUTILUS_SPAWN_EGG, EntityTypes.ZOMBIE_NAUTILUS);
        configureSpawnEgg(registry, ItemTypes.ZOMBIE_PIGMAN_SPAWN_EGG, EntityTypes.ZOMBIE_PIGMAN);
        configureSpawnEgg(registry, ItemTypes.ZOMBIE_SPAWN_EGG, EntityTypes.ZOMBIE);
        configureSpawnEgg(registry, ItemTypes.ZOMBIE_VILLAGER_SPAWN_EGG, EntityTypes.ZOMBIE_VILLAGER);
    }

    private void configureSpawnEgg(CloudItemRegistry registry, ItemType itemType, EntityType<?> entityType) {
        registry.configure(itemType)
                .set(ItemBehaviors.SPAWN_EGG, new SpawnEggComponent(entityType))
                .set(ItemBehaviors.USE_ON, SpawnEggItemHandlers.useOn(entityType));
    }

    private void configureArmor(CloudItemRegistry registry, ItemType type, ArmorMaterial material, EquipmentSlot slot) {
        configureDamageableVanilla(registry, type, material.durability(slot), repairWith(material.repairTag()))
                .set(ItemBehaviors.ARMOR, new ArmorComponent(material.defense(slot), material.toughness(), material.knockbackResistance()))
                .set(ItemBehaviors.GET_EQUIPMENT_SLOT, item -> slot)
                .set(
                        ItemBehaviors.CAN_ENCHANT_WITH,
                        CloudItemRegistry.enchantableWith(
                                EnchantmentTarget.ARMOR,
                                enchantmentTarget(slot),
                                EnchantmentTarget.BREAKABLE,
                                EnchantmentTarget.VANISHABLE,
                                EnchantmentTarget.WEARABLE
                        )
                )
                .set(ItemBehaviors.USE, ArmorItemHandlers.equip(slot, material.equipSound()));
    }

    private EnchantmentTarget enchantmentTarget(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> EnchantmentTarget.ARMOR_HEAD;
            case CHEST -> EnchantmentTarget.ARMOR_CHEST;
            case LEGS -> EnchantmentTarget.ARMOR_LEGS;
            case FEET -> EnchantmentTarget.ARMOR_FEET;
            case MAIN_HAND, OFF_HAND -> throw new IllegalArgumentException("Not an armor slot: " + slot);
        };
    }

    private void configureElytra(CloudItemRegistry registry, CanRepairWithHandler repairWith) {
        configureDamageableVanilla(registry, ItemTypes.ELYTRA, 432, repairWith)
                .set(ItemBehaviors.GET_EQUIPMENT_SLOT, item -> EquipmentSlot.CHEST)
                .set(
                        ItemBehaviors.CAN_ENCHANT_WITH,
                        CloudItemRegistry.enchantableWith(EnchantmentTarget.BREAKABLE, EnchantmentTarget.VANISHABLE, EnchantmentTarget.WEARABLE)
                )
                .set(ItemBehaviors.USE, ArmorItemHandlers.equip(EquipmentSlot.CHEST, Sound.ARMOR_EQUIP_ELYTRA));
    }

    private void configureTool(
            CloudItemRegistry registry,
            ItemType type,
            Tool tool,
            int maxDamage,
            float attackDamage,
            CanRepairWithHandler repairWith,
            EnchantmentTarget... additionalEnchantmentTargets
    ) {
        configureDamageableVanilla(registry, type, maxDamage, repairWith)
                .set(ItemBehaviors.CAN_ENCHANT_WITH, CloudItemRegistry.toolEnchantableWith(additionalEnchantmentTargets))
                .set(ItemBehaviors.GET_ATTACK_DAMAGE, item -> attackDamage)
                .set(ItemBehaviors.GET_ATTACK_DURABILITY_DAMAGE, item -> 1)
                .set(ItemBehaviors.GET_TOOL, item -> tool)
                .set(ItemBehaviors.MINE_BLOCK, DefaultItemHandlers.MINE_BLOCK);
    }

    private void configureAxe(CloudItemRegistry registry, ItemType type, ToolMaterial material, float attackDamageBaseline, CanRepairWithHandler repairWith) {
        configureTool(
                registry,
                type,
                VanillaTools.axe(material),
                material.getDurability(),
                1 + attackDamageBaseline + material.getAttackDamageBonus(),
                repairWith,
                EnchantmentTarget.SHARP_WEAPON,
                EnchantmentTarget.WEAPON
        );
    }

    private void configurePickaxe(CloudItemRegistry registry, ItemType type, ToolMaterial material, CanRepairWithHandler repairWith) {
        configureTool(registry, type, VanillaTools.pickaxe(material), material.getDurability(), 2 + material.getAttackDamageBonus(), repairWith);
    }

    private void configureShovel(CloudItemRegistry registry, ItemType type, ToolMaterial material, CanRepairWithHandler repairWith) {
        configureTool(registry, type, VanillaTools.shovel(material.getSpeed()), material.getDurability(), 2.5f + material.getAttackDamageBonus(), repairWith);
    }

    private void configureHoe(CloudItemRegistry registry, ItemType type, ToolMaterial material, float attackDamageBaseline, CanRepairWithHandler repairWith) {
        configureTool(
                registry,
                type,
                VanillaTools.hoe(material),
                material.getDurability(),
                1 + attackDamageBaseline + material.getAttackDamageBonus(),
                repairWith
        );
    }

    private void configureSword(CloudItemRegistry registry, ItemType type, ToolMaterial material, CanRepairWithHandler repairWith) {
        configureDamageableVanilla(registry, type, material.getDurability(), repairWith)
                .set(
                        ItemBehaviors.CAN_ENCHANT_WITH,
                        CloudItemRegistry.enchantableWith(
                                EnchantmentTarget.FIRE_ASPECT,
                                EnchantmentTarget.MELEE_WEAPON,
                                EnchantmentTarget.SHARP_WEAPON,
                                EnchantmentTarget.WEAPON,
                                EnchantmentTarget.BREAKABLE,
                                EnchantmentTarget.VANISHABLE
                        )
                )
                .set(ItemBehaviors.GET_ATTACK_DAMAGE, item -> 4 + material.getAttackDamageBonus())
                .set(ItemBehaviors.GET_ATTACK_DURABILITY_DAMAGE, item -> 1)
                .set(ItemBehaviors.GET_TOOL, item -> VanillaTools.sword())
                .set(ItemBehaviors.MINE_BLOCK, DefaultItemHandlers.MINE_BLOCK);
    }

    private void configureSpear(CloudItemRegistry registry, ItemType type, ToolMaterial material,
                                CanRepairWithHandler repairWith, SoundEvent hitSound, SoundEvent missSound,
                                SoundEvent useSound, SpearProfile spear) {
        configureDamageableVanilla(registry, type, material.getDurability(), repairWith)
                .set(
                        ItemBehaviors.CAN_ENCHANT_WITH,
                        CloudItemRegistry.enchantableWith(
                                EnchantmentTarget.FIRE_ASPECT,
                                EnchantmentTarget.MELEE_WEAPON,
                                EnchantmentTarget.SHARP_WEAPON,
                                EnchantmentTarget.SPEAR,
                                EnchantmentTarget.WEAPON,
                                EnchantmentTarget.BREAKABLE,
                                EnchantmentTarget.VANISHABLE
                        )
                )
                .set(ItemBehaviors.ATTACK_DAMAGE_TYPE, DamageTypes.SPEAR)
                .set(ItemBehaviors.GET_ATTACK_DAMAGE, item -> 1 + material.getAttackDamageBonus())
                .set(ItemBehaviors.GET_ATTACK_DURABILITY_DAMAGE, item -> 1)
                .set(ItemBehaviors.STAB, SpearItemHandlers.stab(spear, hitSound, missSound))
                .set(ItemBehaviors.USE, SpearItemHandlers.use(useSound))
                .set(ItemBehaviors.USE_TICK, SpearItemHandlers.kinetic(spear, hitSound))
                .set(ItemBehaviors.RELEASE_USE, SpearItemHandlers.RELEASE);
    }

    private void configureDamageableEnchantableTool(
            CloudItemRegistry registry,
            ItemType type,
            Tool tool,
            int maxDamage,
            float attackDamage,
            CanRepairWithHandler repairWith,
            EnchantmentTarget... enchantmentTargets
    ) throws RegistryException {
        CloudComponentMap components =
                configureDamageableEnchantable(registry, type, maxDamage, repairWith, enchantmentTargets);
        components.set(ItemBehaviors.GET_ATTACK_DAMAGE, item -> attackDamage);
        components.set(ItemBehaviors.GET_ATTACK_DURABILITY_DAMAGE, item -> 1);
        components.set(ItemBehaviors.GET_TOOL, item -> tool);
        components.set(ItemBehaviors.MINE_BLOCK, DefaultItemHandlers.MINE_BLOCK);
    }

    private CloudComponentMap configureDamageableEnchantable(
            CloudItemRegistry registry,
            ItemType type,
            int maxDamage,
            CanRepairWithHandler repairWith,
            EnchantmentTarget... enchantmentTargets
    ) {
        CloudComponentMap components = configureDamageableVanilla(registry, type, maxDamage, repairWith);
        components.set(ItemBehaviors.CAN_ENCHANT_WITH, CloudItemRegistry.enchantableWith(enchantmentTargets));
        return components;
    }

    private CloudComponentMap configureDamageableVanilla(CloudItemRegistry registry, ItemType type, int maxDamage, CanRepairWithHandler repairWith) {
        CloudComponentMap components = (CloudComponentMap) registry.configure(type);
        components.set(ItemBehaviors.DAMAGEABLE, () -> true);
        components.set(ItemBehaviors.GET_DAMAGE_CHANCE, DefaultItemHandlers.GET_DAMAGE_CHANCE);
        components.set(ItemBehaviors.GET_MAX_DAMAGE, item -> maxDamage);
        components.set(ItemBehaviors.GET_MAX_STACK_SIZE, item -> 1);
        components.set(ItemBehaviors.ON_DAMAGE, DefaultItemHandlers.ON_DAMAGE);
        components.set(ItemBehaviors.CAN_REPAIR_WITH, repairWith);
        return components;
    }
}
