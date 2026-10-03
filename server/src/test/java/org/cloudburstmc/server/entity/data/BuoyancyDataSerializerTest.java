package org.cloudburstmc.server.entity.data;

import org.cloudburstmc.api.block.LiquidTypes;
import org.cloudburstmc.api.entity.component.Buoyancy;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BuoyancyDataSerializerTest {

    @Test
    void serializesFlotationDataWithoutExposingApiFieldNames() {
        Buoyancy buoyancy = new Buoyancy(true, 1, 0.03f, 10, 0, true, false, List.of(LiquidTypes.WATER, LiquidTypes.FLOWING_WATER));
        ObjectMapper json = new ObjectMapper();

        assertEquals(json.readTree("""
                {"apply_gravity":true,"base_buoyancy":1.0,"big_wave_probability":0.03,
                "big_wave_speed":10.0,"drag_down_on_buoyancy_removed":0.0,
                "movement_type":"waves","can_auto_step_from_liquid":false,
                "liquid_blocks":["minecraft:water","minecraft:flowing_water"]}
                """), json.readTree(BuoyancyDataSerializer.serialize(buoyancy)));
    }
}
