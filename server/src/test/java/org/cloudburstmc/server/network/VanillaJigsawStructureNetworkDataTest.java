package org.cloudburstmc.server.network;

import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.protocol.bedrock.packet.JigsawStructureDataPacket;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class VanillaJigsawStructureNetworkDataTest {

    private static final Set<String> REQUIRED_REGISTRY_NAMES = Set.of(
            "processors",
            "template_pools",
            "jigsaws",
            "structure_sets"
    );

    @Test
    void createsPopulatedVanillaJigsawStructureRegistries() {
        JigsawStructureDataPacket packet = VanillaJigsawStructureNetworkData.createPacket();
        NbtMap data = packet.getJigsawStructureDataTag();

        assertTrue(data.keySet().containsAll(REQUIRED_REGISTRY_NAMES));
        REQUIRED_REGISTRY_NAMES.forEach(name -> {
            NbtList<?> entries = assertInstanceOf(NbtList.class, data.get(name));
            assertEquals(NbtType.COMPOUND, entries.getType());
            assertFalse(entries.isEmpty(), () -> name + " must not be empty");
        });
    }
}
