package org.cloudburstmc.server.potion;

import org.cloudburstmc.api.potion.*;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.network.NetworkUtils;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class PotionCatalogTest {

    private static final List<String> EFFECT_IDS = List.of(
            "absorption", "bad_omen", "blindness", "breath_of_the_nautilus", "conduit_power", "darkness",
            "fatal_poison", "fire_resistance", "haste", "health_boost", "hunger", "infested",
            "instant_damage", "instant_health", "invisibility", "jump_boost", "levitation", "mining_fatigue",
            "nausea", "night_vision", "oozing", "poison", "raid_omen", "regeneration", "resistance",
            "saturation", "slow_falling", "slowness", "speed", "strength", "trial_omen", "village_hero",
            "water_breathing", "weakness", "weaving", "wind_charged", "wither"
    );

    private static final List<String> POTION_IDS = List.of(
            "awkward", "fire_resistance", "harming", "healing", "infested", "invisibility", "leaping",
            "long_fire_resistance", "long_invisibility", "long_leaping", "long_mundane", "long_nightvision",
            "long_poison", "long_regeneration", "long_slow_falling", "long_slowness", "long_strength",
            "long_swiftness", "long_turtle_master", "long_water_breathing", "long_weakness", "mundane",
            "nightvision", "oozing", "poison", "regeneration", "slow_falling", "slowness", "strength",
            "strong_harming", "strong_healing", "strong_leaping", "strong_poison", "strong_regeneration",
            "strong_slowness", "strong_strength", "strong_swiftness", "strong_turtle_master", "swiftness",
            "thick", "turtle_master", "water", "water_breathing", "weakness", "weaving", "wind_charged",
            "wither"
    );

    @Test
    void containsEveryVanillaEffectIdentifier() {
        List<String> actual = EffectTypes.values().stream()
                .map(EffectType::getId)
                .map(Identifier::getName)
                .sorted()
                .toList();

        assertEquals(EFFECT_IDS, actual);
    }

    @Test
    void containsEveryVanillaPotionIdentifier() {
        List<String> actual = PotionTypes.values().stream()
                .map(PotionType::getId)
                .map(Identifier::getName)
                .sorted()
                .toList();

        assertEquals(POTION_IDS, actual);
    }

    @Test
    void resolvesEveryBuiltInPotionByIdentifier() {
        PotionTypes.values().forEach(type -> assertSame(type, PotionTypes.get(type.getId()).orElseThrow()));
    }

    @Test
    void roundTripsEveryNetworkPotionId() {
        for (short id = 0; id < PotionTypes.values().size(); id++) {
            assertEquals(id, NetworkUtils.potionToNetwork(NetworkUtils.potionFromNetwork(id)));
        }
    }

    @Test
    void roundTripsEveryNetworkEffectId() {
        for (byte id = 1; id <= EffectTypes.values().size(); id++) {
            assertEquals(id, NetworkUtils.effectToNetwork(NetworkUtils.effectFromNetwork(id)));
        }
    }

    @Test
    void classifiesModernEffects() {
        assertEquals(EffectCategory.HARMFUL, EffectTypes.INFESTED.getCategory());
        assertEquals(EffectCategory.NEUTRAL, EffectTypes.RAID_OMEN.getCategory());
        assertEquals(EffectCategory.BENEFICIAL, EffectTypes.BREATH_OF_THE_NAUTILUS.getCategory());
        assertEquals(EffectCategory.HARMFUL, EffectTypes.WEAVING.getCategory());
        assertEquals(EffectCategory.HARMFUL, EffectTypes.WIND_CHARGED.getCategory());
    }

    @Test
    void turtleMasterContainsBothEffects() {
        assertEquals(List.of(
                new PotionEffect(EffectTypes.SLOWNESS, 400, 3),
                new PotionEffect(EffectTypes.RESISTANCE, 400, 2)
        ), PotionTypes.TURTLE_MASTER.getEffects());
    }
}
