package org.cloudburstmc.server.network;

import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.data.BlockPropertyData;
import org.cloudburstmc.protocol.bedrock.packet.StartGamePacket;
import org.cloudburstmc.protocol.bedrock.packet.VoxelShapesPacket;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VanillaBlockNetworkDataTest {

    @Test
    void loadsVanillaVoxelShapes() {
        VoxelShapesPacket packet = VanillaBlockNetworkData.createVoxelShapesPacket();

        assertEquals(287, packet.getShapes().size());
        assertEquals(220, packet.getNameMap().size());
        assertEquals(0, packet.getCustomShapeCount());
        assertEquals(0, packet.getNameMap().get("minecraft:empty"));
        assertTrue(packet.getNameMap().values().stream().allMatch(index -> index >= 0 && index < packet.getShapes().size()));
    }

    @Test
    void loadsVanillaDataDrivenBlockProperties() {
        StartGamePacket packet = new StartGamePacket();
        VanillaBlockNetworkData.addBlockProperties(packet);

        assertEquals(98, packet.getBlockProperties().size());

        BlockPropertyData stairs = packet.getBlockProperties().stream()
                .filter(property -> property.getName().equals("minecraft:light_gray_concrete_stairs"))
                .findFirst()
                .orElseThrow();
        NbtMap components = stairs.getProperties().getCompound("components");
        NbtMap mining = components.getCompound("minecraft:destructible_by_mining");

        assertInstanceOf(Float.class, mining.get("value"));
        assertInstanceOf(Byte.class, stairs.getProperties().getCompound("menu_category").get("is_hidden_in_commands"));
        assertInstanceOf(Integer.class, stairs.getProperties().get("molangVersion"));
    }
}
