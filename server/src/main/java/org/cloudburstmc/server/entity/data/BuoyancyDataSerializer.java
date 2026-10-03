package org.cloudburstmc.server.entity.data;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.entity.component.Buoyancy;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@UtilityClass
public class BuoyancyDataSerializer {

    private static final ObjectMapper JSON = new ObjectMapper();

    public static String serialize(Buoyancy buoyancy) {
        return JSON.writeValueAsString(Map.of(
                "apply_gravity", buoyancy.applyGravity(),
                "base_buoyancy", buoyancy.baseBuoyancy(),
                "big_wave_probability", buoyancy.largeWaveProbability(),
                "big_wave_speed", buoyancy.largeWaveSpeed(),
                "drag_down_on_buoyancy_removed", buoyancy.dragOnRemoval(),
                "movement_type", buoyancy.simulateWaves() ? "waves" : "none",
                "can_auto_step_from_liquid", buoyancy.canStepFromLiquid(),
                "liquid_blocks", buoyancy.liquids().stream().map(liquid -> liquid.getId().toString()).toList()
        ));
    }
}
