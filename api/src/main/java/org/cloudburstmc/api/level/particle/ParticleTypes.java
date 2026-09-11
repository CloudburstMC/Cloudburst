package org.cloudburstmc.api.level.particle;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.util.Identifier;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Particle types understood by the vanilla client.
 */
@UtilityClass
public class ParticleTypes {
    public static final ParticleType BALLOON_GAS = type("balloon_gas");
    public static final ParticleType BLEACH = type("bleach");
    public static final ParticleType BLOCK_FORCE_FIELD = type("block_force_field");
    public static final ParticleType BLUE_FLAME = type("blue_flame");
    public static final ParticleType BREEZE_WIND_EXPLOSION = type("breeze_wind_explosion");
    public static final ParticleType BRUSH_DUST = type("brush_dust");
    public static final ParticleType BUBBLE = type("bubble");
    public static final ParticleType BUBBLE_COLUMN_DOWN = type("bubble_column_down");
    public static final ParticleType BUBBLE_COLUMN_UP = type("bubble_column_up");
    public static final ParticleType BUBBLE_MANUAL = type("bubble_manual");
    public static final ParticleType CAMPFIRE_SMOKE = type("campfire_smoke");
    public static final ParticleType CAMPFIRE_SMOKE_TALL = type("campfire_smoke_tall");
    public static final ParticleType CANDLE_FLAME = type("candle_flame");
    public static final ParticleType CARROT_BOOST = type("carrot_boost");
    public static final ParticleType CHERRY_LEAVES = type("cherry_leaves");
    public static final ParticleType COLORED_FLAME = type("colored_flame");
    public static final ParticleType CONDUIT = type("conduit");
    public static final ParticleType CREAKING_CRUMBLE = type("creaking_crumble");
    public static final ParticleType CRIT = type("crit");
    public static final ParticleType DRAGON_BREATH = type("dragon_breath");
    public static final ParticleType DRAGON_BREATH_FIRE = type("dragon_breath_fire");
    public static final ParticleType DRAGON_BREATH_TRAIL = type("dragon_breath_trail");
    public static final ParticleType DRAGON_DESTROY_BLOCK = type("dragon_destroy_block");
    public static final ParticleType DRIP_HONEY = type("drip_honey");
    public static final ParticleType DRIP_LAVA = type("drip_lava");
    public static final ParticleType DRIP_WATER = type("drip_water");
    public static final ParticleType DUST_PLUME = type("dust_plume");
    public static final ParticleType ELECTRIC_SPARK = type("electric_spark");
    public static final ParticleType ENCHANTING_TABLE = type("enchanting_table");
    public static final ParticleType END_ROD = type("end_rod");
    public static final ParticleType EVAPORATION = type("evaporation");
    public static final ParticleType EXPLODE = type("explode");
    public static final ParticleType EYEBLOSSOM_CLOSE = type("eyeblossom_close");
    public static final ParticleType EYEBLOSSOM_OPEN = type("eyeblossom_open");
    public static final ParticleType FALLING_BORDER_DUST = type("falling_border_dust");
    public static final ParticleType FALLING_DUST = type("falling_dust");
    public static final ParticleType FIREWORKS = type("fireworks");
    public static final ParticleType FIREWORKS_OVERLAY = type("fireworks_overlay");
    public static final ParticleType FIREWORKS_STARTER = type("fireworks_starter");
    public static final ParticleType FLAME = type("flame");
    public static final ParticleType FOOD = type("food");
    public static final ParticleType GREEN_FLAME = type("green_flame");
    public static final ParticleType HEART = type("heart");
    public static final ParticleType HUGE_EXPLOSION = type("huge_explosion");
    public static final ParticleType ICON_CRACK = type("icon_crack");
    public static final ParticleType INK = type("ink");
    public static final ParticleType LARGE_EXPLODE = type("large_explode");
    public static final ParticleType LARGE_SMOKE = type("large_smoke");
    public static final ParticleType LAVA = type("lava");
    public static final ParticleType MOB_APPEARANCE = type("mob_appearance");
    public static final ParticleType MOB_FLAME = type("mob_flame");
    public static final ParticleType MOB_PORTAL = type("mob_portal");
    public static final ParticleType MOB_SPELL = type("mob_spell");
    public static final ParticleType MOB_SPELL_AMBIENT = type("mob_spell_ambient");
    public static final ParticleType MOB_SPELL_INSTANTANEOUS = type("mob_spell_instantaneous");
    public static final ParticleType MYCELIUM_DUST = type("mycelium_dust");
    public static final ParticleType NOTE = type("note");
    public static final ParticleType OBSIDIAN_TEAR = type("obsidian_tear");
    public static final ParticleType OMINOUS_ITEM_SPAWNER = type("ominous_item_spawner");
    public static final ParticleType ORANGE_POPLAR_LEAVES = type("orange_poplar_leaves");
    public static final ParticleType PALE_OAK_LEAVES = type("pale_oak_leaves");
    public static final ParticleType PAUSE_MOB_GROWTH = type("pause_mob_growth");
    public static final ParticleType PORTAL = type("portal");
    public static final ParticleType PORTAL_REVERSE = type("portal_reverse");
    public static final ParticleType RAIN_SPLASH = type("rain_splash");
    public static final ParticleType RED_DUST = type("red_dust");
    public static final ParticleType RED_POPLAR_LEAVES = type("red_poplar_leaves");
    public static final ParticleType RESET_MOB_GROWTH = type("reset_mob_growth");
    public static final ParticleType RISING_BORDER_DUST = type("rising_border_dust");
    public static final ParticleType SCULK_SENSOR_REDSTONE = type("sculk_sensor_redstone");
    public static final ParticleType SCULK_SOUL = type("sculk_soul");
    public static final ParticleType SHRIEK = type("shriek");
    public static final ParticleType SHULKER_BULLET = type("shulker_bullet");
    public static final ParticleType SLIME = type("slime");
    public static final ParticleType SMOKE = type("smoke");
    public static final ParticleType SNEEZE = type("sneeze");
    public static final ParticleType SNOWBALL_POOF = type("snowball_poof");
    public static final ParticleType SNOWFLAKE = type("snowflake");
    public static final ParticleType SONIC_EXPLOSION = type("sonic_explosion");
    public static final ParticleType SOUL = type("soul");
    public static final ParticleType SPARKLER = type("sparkler");
    public static final ParticleType SPIT = type("spit");
    public static final ParticleType SPORE_BLOSSOM_AMBIENT = type("spore_blossom_ambient");
    public static final ParticleType SPORE_BLOSSOM_SHOWER = type("spore_blossom_shower");
    public static final ParticleType STALACTITE_DRIP_LAVA = type("stalactite_drip_lava");
    public static final ParticleType STALACTITE_DRIP_WATER = type("stalactite_drip_water");
    public static final ParticleType SULFUR_CUBE = type("sulfur_cube");
    public static final ParticleType TERRAIN = type("terrain");
    public static final ParticleType TOTEM = type("totem");
    public static final ParticleType TOWN_AURA = type("town_aura");
    public static final ParticleType TRACKER_EMITTER = type("tracker_emitter");
    public static final ParticleType UNDEFINED = type("undefined");
    public static final ParticleType VAULT_CONNECTION = type("vault_connection");
    public static final ParticleType VIBRATION_SIGNAL = type("vibration_signal");
    public static final ParticleType VILLAGER_ANGRY = type("villager_angry");
    public static final ParticleType VILLAGER_HAPPY = type("villager_happy");
    public static final ParticleType WATER_SPLASH = type("water_splash");
    public static final ParticleType WATER_SPLASH_MANUAL = type("water_splash_manual");
    public static final ParticleType WATER_WAKE = type("water_wake");
    public static final ParticleType WAX = type("wax");
    public static final ParticleType WHITE_SMOKE = type("white_smoke");
    public static final ParticleType WIND_EXPLOSION = type("wind_explosion");
    public static final ParticleType WITCH_SPELL = type("witch_spell");
    public static final ParticleType WOLF_ARMOR_BREAK = type("wolf_armor_break");
    public static final ParticleType YELLOW_POPLAR_LEAVES = type("yellow_poplar_leaves");

    /**
     * Finds a particle type by identifier.
     *
     * @param id the particle identifier
     * @return the matching particle type, if registered
     */
    public static Optional<ParticleType> get(Identifier id) {
        Objects.requireNonNull(id, "id");
        return Optional.ofNullable(Lookup.VALUES.get(id));
    }

    /**
     * Returns all known particle types.
     *
     * @return known particle types
     */
    public static Collection<ParticleType> values() {
        return Lookup.VALUES.values();
    }

    private static ParticleType type(String id) {
        return ParticleType.of(Identifier.parse(id));
    }

    private static final class Lookup {
        private static final Map<Identifier, ParticleType> VALUES = create();

        private static Map<Identifier, ParticleType> create() {
            Map<Identifier, ParticleType> values = new LinkedHashMap<>();
            for (Field field : ParticleTypes.class.getFields()) {
                if (field.getType() != ParticleType.class) {
                    continue;
                }
                try {
                    ParticleType type = (ParticleType) field.get(null);
                    values.put(type.id(), type);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException("Unable to read particle type field " + field.getName(), e);
                }
            }
            return Collections.unmodifiableMap(values);
        }
    }
}
