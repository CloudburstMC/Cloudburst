package org.cloudburstmc.api.level.particle;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.internal.BuiltInTypeCatalog;
import org.cloudburstmc.api.util.Identifier;

import java.util.List;
import java.util.Optional;

/**
 * Built-in named emitters. Custom emitters can be referenced with {@link ParticleEmitterType#of(Identifier)}.
 *
 * <p>Generated catalog. Do not edit by hand.
 */
@UtilityClass
public class ParticleEmitterTypes {
    private static final BuiltInTypeCatalog<ParticleEmitterType> TYPES = BuiltInTypeCatalog.create(ParticleEmitterType::getId);

    public static final ParticleEmitterType ARROW_SPELL_EMITTER = type("arrow_spell_emitter");
    public static final ParticleEmitterType BALLOON_GAS_PARTICLE = type("balloon_gas_particle");
    public static final ParticleEmitterType BASIC_BUBBLE_PARTICLE = type("basic_bubble_particle");
    public static final ParticleEmitterType BASIC_BUBBLE_PARTICLE_MANUAL = type("basic_bubble_particle_manual");
    public static final ParticleEmitterType BASIC_CRIT_PARTICLE = type("basic_crit_particle");
    public static final ParticleEmitterType BASIC_FLAME_PARTICLE = type("basic_flame_particle");
    public static final ParticleEmitterType BASIC_PORTAL_PARTICLE = type("basic_portal_particle");
    public static final ParticleEmitterType BASIC_SMOKE_PARTICLE = type("basic_smoke_particle");
    public static final ParticleEmitterType BIOME_TINTED_LEAVES_PARTICLE = type("biome_tinted_leaves_particle");
    public static final ParticleEmitterType BLEACH = type("bleach");
    public static final ParticleEmitterType BLOCK_DESTRUCT = type("block_destruct");
    public static final ParticleEmitterType BLOCK_SLIDE = type("block_slide");
    public static final ParticleEmitterType BLUE_FLAME_PARTICLE = type("blue_flame_particle");
    public static final ParticleEmitterType BREAKING_ITEM_ICON = type("breaking_item_icon");
    public static final ParticleEmitterType BREAKING_ITEM_TERRAIN = type("breaking_item_terrain");
    public static final ParticleEmitterType BREEZE_GROUND_PARTICLE = type("breeze_ground_particle");
    public static final ParticleEmitterType BREEZE_WIND_EXPLOSION_EMITTER = type("breeze_wind_explosion_emitter");
    public static final ParticleEmitterType BUBBLE_COLUMN_BUBBLE = type("bubble_column_bubble");
    public static final ParticleEmitterType BUBBLE_COLUMN_DOWN_PARTICLE = type("bubble_column_down_particle");
    public static final ParticleEmitterType BUBBLE_COLUMN_UP_PARTICLE = type("bubble_column_up_particle");
    public static final ParticleEmitterType CAMERA_SHOOT_EXPLOSION = type("camera_shoot_explosion");
    public static final ParticleEmitterType CAMPFIRE_SMOKE_PARTICLE = type("campfire_smoke_particle");
    public static final ParticleEmitterType CAMPFIRE_TALL_SMOKE_PARTICLE = type("campfire_tall_smoke_particle");
    public static final ParticleEmitterType CANDLE_FLAME_PARTICLE = type("candle_flame_particle");
    public static final ParticleEmitterType CAULDRON_BUBBLE_PARTICLE = type("cauldron_bubble_particle");
    public static final ParticleEmitterType CAULDRON_EXPLOSION_EMITTER = type("cauldron_explosion_emitter");
    public static final ParticleEmitterType CAULDRON_SPELL_EMITTER = type("cauldron_spell_emitter");
    public static final ParticleEmitterType CAULDRON_SPLASH_PARTICLE = type("cauldron_splash_particle");
    public static final ParticleEmitterType CHERRY_LEAVES_PARTICLE = type("cherry_leaves_particle");
    public static final ParticleEmitterType COLORED_FLAME_PARTICLE = type("colored_flame_particle");
    public static final ParticleEmitterType CONDUIT_ABSORB_PARTICLE = type("conduit_absorb_particle");
    public static final ParticleEmitterType CONDUIT_ATTACK_EMITTER = type("conduit_attack_emitter");
    public static final ParticleEmitterType CONDUIT_PARTICLE = type("conduit_particle");
    public static final ParticleEmitterType CREAKING_CRUMBLE_BODY = type("creaking_crumble_body");
    public static final ParticleEmitterType CREAKING_CRUMBLE_HEAD = type("creaking_crumble_head");
    public static final ParticleEmitterType CREAKING_HEART_TRAIL = type("creaking_heart_trail");
    public static final ParticleEmitterType CRITICAL_HIT_EMITTER = type("critical_hit_emitter");
    public static final ParticleEmitterType CROP_GROWTH_AREA_EMITTER = type("crop_growth_area_emitter");
    public static final ParticleEmitterType CROP_GROWTH_EMITTER = type("crop_growth_emitter");
    public static final ParticleEmitterType DEATH_EXPLOSION_EMITTER = type("death_explosion_emitter");
    public static final ParticleEmitterType DOLPHIN_MOVE_PARTICLE = type("dolphin_move_particle");
    public static final ParticleEmitterType DRAGON_BREATH_FIRE = type("dragon_breath_fire");
    public static final ParticleEmitterType DRAGON_BREATH_LINGERING = type("dragon_breath_lingering");
    public static final ParticleEmitterType DRAGON_BREATH_TRAIL = type("dragon_breath_trail");
    public static final ParticleEmitterType DRAGON_DEATH_EXPLOSION_EMITTER = type("dragon_death_explosion_emitter");
    public static final ParticleEmitterType DRAGON_DESTROY_BLOCK = type("dragon_destroy_block");
    public static final ParticleEmitterType DRAGON_DYING_EXPLOSION = type("dragon_dying_explosion");
    public static final ParticleEmitterType DUST_PLUME = type("dust_plume");
    public static final ParticleEmitterType EGG_DESTROY_EMITTER = type("egg_destroy_emitter");
    public static final ParticleEmitterType ELECTRIC_SPARK_PARTICLE = type("electric_spark_particle");
    public static final ParticleEmitterType ELEPHANT_TOOTH_PASTE_VAPOR_PARTICLE = type("elephant_tooth_paste_vapor_particle");
    public static final ParticleEmitterType ENCHANTING_TABLE_PARTICLE = type("enchanting_table_particle");
    public static final ParticleEmitterType ENDROD = type("endrod");
    public static final ParticleEmitterType END_CHEST = type("end_chest");
    public static final ParticleEmitterType EVOCATION_FANG_PARTICLE = type("evocation_fang_particle");
    public static final ParticleEmitterType EVOKER_SPELL = type("evoker_spell");
    public static final ParticleEmitterType EXPLOSION_MANUAL = type("explosion_manual");
    public static final ParticleEmitterType EXPLOSION_PARTICLE = type("explosion_particle");
    public static final ParticleEmitterType EYEBLOSSOM_CLOSE = type("eyeblossom_close");
    public static final ParticleEmitterType EYEBLOSSOM_OPEN = type("eyeblossom_open");
    public static final ParticleEmitterType EYEOFENDER_DEATH_EXPLODE_PARTICLE = type("eyeofender_death_explode_particle");
    public static final ParticleEmitterType EYE_OF_ENDER_BUBBLE_PARTICLE = type("eye_of_ender_bubble_particle");
    public static final ParticleEmitterType FALLING_BORDER_DUST_PARTICLE = type("falling_border_dust_particle");
    public static final ParticleEmitterType FALLING_DUST = type("falling_dust");
    public static final ParticleEmitterType FALLING_DUST_CONCRETE_POWDER_PARTICLE = type("falling_dust_concrete_powder_particle");
    public static final ParticleEmitterType FALLING_DUST_DRAGON_EGG_PARTICLE = type("falling_dust_dragon_egg_particle");
    public static final ParticleEmitterType FALLING_DUST_GRAVEL_PARTICLE = type("falling_dust_gravel_particle");
    public static final ParticleEmitterType FALLING_DUST_RED_SAND_PARTICLE = type("falling_dust_red_sand_particle");
    public static final ParticleEmitterType FALLING_DUST_SAND_PARTICLE = type("falling_dust_sand_particle");
    public static final ParticleEmitterType FALLING_DUST_SCAFFOLDING_PARTICLE = type("falling_dust_scaffolding_particle");
    public static final ParticleEmitterType FALLING_DUST_TOP_SNOW_PARTICLE = type("falling_dust_top_snow_particle");
    public static final ParticleEmitterType FIREFLY_PARTICLE = type("firefly_particle");
    public static final ParticleEmitterType FISH_HOOK_PARTICLE = type("fish_hook_particle");
    public static final ParticleEmitterType FISH_POS_PARTICLE = type("fish_pos_particle");
    public static final ParticleEmitterType GEYSER_BASE = type("geyser_base");
    public static final ParticleEmitterType GEYSER_PLUME = type("geyser_plume");
    public static final ParticleEmitterType GEYSER_POOF = type("geyser_poof");
    public static final ParticleEmitterType GLOW_PARTICLE = type("glow_particle");
    public static final ParticleEmitterType GREEN_FLAME_PARTICLE = type("green_flame_particle");
    public static final ParticleEmitterType GUARDIAN_ATTACK_PARTICLE = type("guardian_attack_particle");
    public static final ParticleEmitterType GUARDIAN_WATER_MOVE_PARTICLE = type("guardian_water_move_particle");
    public static final ParticleEmitterType HEART_PARTICLE = type("heart_particle");
    public static final ParticleEmitterType HONEY_DRIP_PARTICLE = type("honey_drip_particle");
    public static final ParticleEmitterType HUGE_EXPLOSION_EMITTER = type("huge_explosion_emitter");
    public static final ParticleEmitterType HUGE_EXPLOSION_LAB_MISC_EMITTER = type("huge_explosion_lab_misc_emitter");
    public static final ParticleEmitterType ICE_EVAPORATION_EMITTER = type("ice_evaporation_emitter");
    public static final ParticleEmitterType INFESTED_AMBIENT = type("infested_ambient");
    public static final ParticleEmitterType INFESTED_EMITTER = type("infested_emitter");
    public static final ParticleEmitterType INK_EMITTER = type("ink_emitter");
    public static final ParticleEmitterType KNOCKBACK_ROAR_PARTICLE = type("knockback_roar_particle");
    public static final ParticleEmitterType LAB_TABLE_HEATBLOCK_DUST_PARTICLE = type("lab_table_heatblock_dust_particle");
    public static final ParticleEmitterType LAB_TABLE_MISC_MYSTICAL_PARTICLE = type("lab_table_misc_mystical_particle");
    public static final ParticleEmitterType LARGE_EXPLOSION = type("large_explosion");
    public static final ParticleEmitterType LAVA_DRIP_PARTICLE = type("lava_drip_particle");
    public static final ParticleEmitterType LAVA_PARTICLE = type("lava_particle");
    public static final ParticleEmitterType LLAMA_SPIT_SMOKE = type("llama_spit_smoke");
    public static final ParticleEmitterType MAGIC_CRITICAL_HIT_EMITTER = type("magic_critical_hit_emitter");
    public static final ParticleEmitterType MAGNESIUM_SALTS_EMITTER = type("magnesium_salts_emitter");
    public static final ParticleEmitterType MISC_FIRE_VAPOR_PARTICLE = type("misc_fire_vapor_particle");
    public static final ParticleEmitterType MOBFLAME_EMITTER = type("mobflame_emitter");
    public static final ParticleEmitterType MOBFLAME_SINGLE = type("mobflame_single");
    public static final ParticleEmitterType MOBSPELL_AMBIENT = type("mobspell_ambient");
    public static final ParticleEmitterType MOBSPELL_EMITTER = type("mobspell_emitter");
    public static final ParticleEmitterType MOBSPELL_LINGERING = type("mobspell_lingering");
    public static final ParticleEmitterType MOB_BLOCK_SPAWN_EMITTER = type("mob_block_spawn_emitter");
    public static final ParticleEmitterType MOB_PORTAL = type("mob_portal");
    public static final ParticleEmitterType MYCELIUM_DUST_PARTICLE = type("mycelium_dust_particle");
    public static final ParticleEmitterType NAUTILUS_BUBBLES_PARTICLE = type("nautilus_bubbles_particle");
    public static final ParticleEmitterType NECTAR_DRIP_PARTICLE = type("nectar_drip_particle");
    public static final ParticleEmitterType NOTE_PARTICLE = type("note_particle");
    public static final ParticleEmitterType NOXIOUS_GAS_PARTICLE = type("noxious_gas_particle");
    public static final ParticleEmitterType OBSIDIAN_GLOW_DUST_PARTICLE = type("obsidian_glow_dust_particle");
    public static final ParticleEmitterType OBSIDIAN_TEAR_PARTICLE = type("obsidian_tear_particle");
    public static final ParticleEmitterType OMINOUS_SPAWNING_PARTICLE = type("ominous_spawning_particle");
    public static final ParticleEmitterType OOZING_AMBIENT = type("oozing_ambient");
    public static final ParticleEmitterType OOZING_EMITTER = type("oozing_emitter");
    public static final ParticleEmitterType ORANGE_POPLAR_LEAVES_PARTICLE = type("orange_poplar_leaves_particle");
    public static final ParticleEmitterType PALE_OAK_LEAVES_PARTICLE = type("pale_oak_leaves_particle");
    public static final ParticleEmitterType PAUSE_MOB_GROWTH = type("pause_mob_growth");
    public static final ParticleEmitterType PHANTOM_TRAIL_PARTICLE = type("phantom_trail_particle");
    public static final ParticleEmitterType PORTAL_DIRECTIONAL = type("portal_directional");
    public static final ParticleEmitterType PORTAL_EAST_WEST = type("portal_east_west");
    public static final ParticleEmitterType PORTAL_NORTH_SOUTH = type("portal_north_south");
    public static final ParticleEmitterType PORTAL_REVERSE_PARTICLE = type("portal_reverse_particle");
    public static final ParticleEmitterType RAID_OMEN_AMBIENT = type("raid_omen_ambient");
    public static final ParticleEmitterType RAID_OMEN_EMITTER = type("raid_omen_emitter");
    public static final ParticleEmitterType RAIN_SPLASH_PARTICLE = type("rain_splash_particle");
    public static final ParticleEmitterType REDSTONE_ORE_DUST_PARTICLE = type("redstone_ore_dust_particle");
    public static final ParticleEmitterType REDSTONE_REPEATER_DUST_PARTICLE = type("redstone_repeater_dust_particle");
    public static final ParticleEmitterType REDSTONE_TORCH_DUST_PARTICLE = type("redstone_torch_dust_particle");
    public static final ParticleEmitterType REDSTONE_WIRE_DUST_PARTICLE = type("redstone_wire_dust_particle");
    public static final ParticleEmitterType RED_POPLAR_LEAVES_PARTICLE = type("red_poplar_leaves_particle");
    public static final ParticleEmitterType RESET_MOB_GROWTH = type("reset_mob_growth");
    public static final ParticleEmitterType RISING_BORDER_DUST_PARTICLE = type("rising_border_dust_particle");
    public static final ParticleEmitterType SCULK_CHARGE_PARTICLE = type("sculk_charge_particle");
    public static final ParticleEmitterType SCULK_CHARGE_POP_PARTICLE = type("sculk_charge_pop_particle");
    public static final ParticleEmitterType SCULK_SENSOR_REDSTONE_PARTICLE = type("sculk_sensor_redstone_particle");
    public static final ParticleEmitterType SCULK_SOUL_PARTICLE = type("sculk_soul_particle");
    public static final ParticleEmitterType SHRIEK_PARTICLE = type("shriek_particle");
    public static final ParticleEmitterType SHULKER_BULLET = type("shulker_bullet");
    public static final ParticleEmitterType SILVERFISH_GRIEF_EMITTER = type("silverfish_grief_emitter");
    public static final ParticleEmitterType SMALL_FLAME_PARTICLE = type("small_flame_particle");
    public static final ParticleEmitterType SMALL_SOUL_FIRE_FLAME = type("small_soul_fire_flame");
    public static final ParticleEmitterType SMASH_GROUND_PARTICLE = type("smash_ground_particle");
    public static final ParticleEmitterType SMASH_GROUND_PARTICLE_CENTER = type("smash_ground_particle_center");
    public static final ParticleEmitterType SNEEZE = type("sneeze");
    public static final ParticleEmitterType SNOWFLAKE_PARTICLE = type("snowflake_particle");
    public static final ParticleEmitterType SONIC_EXPLOSION = type("sonic_explosion");
    public static final ParticleEmitterType SOUL_PARTICLE = type("soul_particle");
    public static final ParticleEmitterType SPARKLER_EMITTER = type("sparkler_emitter");
    public static final ParticleEmitterType SPLASH_SPELL_EMITTER = type("splash_spell_emitter");
    public static final ParticleEmitterType SPONGE_ABSORB_WATER_PARTICLE = type("sponge_absorb_water_particle");
    public static final ParticleEmitterType SPORE_BLOSSOM_AMBIENT_PARTICLE = type("spore_blossom_ambient_particle");
    public static final ParticleEmitterType SPORE_BLOSSOM_SHOWER_PARTICLE = type("spore_blossom_shower_particle");
    public static final ParticleEmitterType SQUID_FLEE_PARTICLE = type("squid_flee_particle");
    public static final ParticleEmitterType SQUID_INK_BUBBLE = type("squid_ink_bubble");
    public static final ParticleEmitterType SQUID_MOVE_PARTICLE = type("squid_move_particle");
    public static final ParticleEmitterType STALACTITE_LAVA_DRIP_PARTICLE = type("stalactite_lava_drip_particle");
    public static final ParticleEmitterType STALACTITE_WATER_DRIP_PARTICLE = type("stalactite_water_drip_particle");
    public static final ParticleEmitterType STUNNED_EMITTER = type("stunned_emitter");
    public static final ParticleEmitterType SULFUR_BUBBLE_PARTICLE = type("sulfur_bubble_particle");
    public static final ParticleEmitterType SULFUR_CUBE_GOO = type("sulfur_cube_goo");
    public static final ParticleEmitterType TOTEM_MANUAL = type("totem_manual");
    public static final ParticleEmitterType TOTEM_PARTICLE = type("totem_particle");
    public static final ParticleEmitterType TRIAL_OMEN_AMBIENT = type("trial_omen_ambient");
    public static final ParticleEmitterType TRIAL_OMEN_EMITTER = type("trial_omen_emitter");
    public static final ParticleEmitterType TRIAL_OMEN_SINGLE = type("trial_omen_single");
    public static final ParticleEmitterType TRIAL_SPAWNER_DETECTION = type("trial_spawner_detection");
    public static final ParticleEmitterType TRIAL_SPAWNER_DETECTION_OMINOUS = type("trial_spawner_detection_ominous");
    public static final ParticleEmitterType UNDERWATER_TORCH_PARTICLE = type("underwater_torch_particle");
    public static final ParticleEmitterType VAULT_CONNECTION_PARTICLE = type("vault_connection_particle");
    public static final ParticleEmitterType VIBRATION_SIGNAL = type("vibration_signal");
    public static final ParticleEmitterType VILLAGER_ANGRY = type("villager_angry");
    public static final ParticleEmitterType VILLAGER_HAPPY = type("villager_happy");
    public static final ParticleEmitterType WARDEN_DIG = type("warden_dig");
    public static final ParticleEmitterType WATER_DRIP_PARTICLE = type("water_drip_particle");
    public static final ParticleEmitterType WATER_EVAPORATION_ACTOR_EMITTER = type("water_evaporation_actor_emitter");
    public static final ParticleEmitterType WATER_EVAPORATION_BUCKET_EMITTER = type("water_evaporation_bucket_emitter");
    public static final ParticleEmitterType WATER_EVAPORATION_MANUAL = type("water_evaporation_manual");
    public static final ParticleEmitterType WATER_SPLASH_PARTICLE = type("water_splash_particle");
    public static final ParticleEmitterType WATER_SPLASH_PARTICLE_MANUAL = type("water_splash_particle_manual");
    public static final ParticleEmitterType WATER_WAKE_PARTICLE = type("water_wake_particle");
    public static final ParticleEmitterType WAX_PARTICLE = type("wax_particle");
    public static final ParticleEmitterType WEAVING_AMBIENT = type("weaving_ambient");
    public static final ParticleEmitterType WEAVING_EMITTER = type("weaving_emitter");
    public static final ParticleEmitterType WHITE_SMOKE_PARTICLE = type("white_smoke_particle");
    public static final ParticleEmitterType WIND_CHARGED_AMBIENT = type("wind_charged_ambient");
    public static final ParticleEmitterType WIND_CHARGED_EMITTER = type("wind_charged_emitter");
    public static final ParticleEmitterType WIND_EXPLOSION_EMITTER = type("wind_explosion_emitter");
    public static final ParticleEmitterType WITCHSPELL_EMITTER = type("witchspell_emitter");
    public static final ParticleEmitterType WITHER_BOSS_INVULNERABLE = type("wither_boss_invulnerable");
    public static final ParticleEmitterType YELLOW_POPLAR_LEAVES_PARTICLE = type("yellow_poplar_leaves_particle");

    /**
     * Finds a built-in type by identifier.
     *
     * @param id the identifier
     * @return the matching type, if present
     */
    public static Optional<ParticleEmitterType> get(Identifier id) {
        return TYPES.get(id);
    }

    /**
     * Returns the built-in types in declaration order.
     *
     * @return an unmodifiable list of built-in types
     */
    public static List<ParticleEmitterType> values() {
        return TYPES.values();
    }

    private static ParticleEmitterType type(String id) {
        return TYPES.register(ParticleEmitterType.of(Identifier.parse(id)));
    }
}
