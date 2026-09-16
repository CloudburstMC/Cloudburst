package org.cloudburstmc.server.network;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.nbt.*;
import org.cloudburstmc.protocol.bedrock.packet.JigsawStructureDataPacket;
import org.cloudburstmc.server.registry.RegistryUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Loads the vanilla jigsaw structure registries required during the login sequence.
 */
@UtilityClass
public class VanillaJigsawStructureNetworkData {

    private static final List<String> REGISTRY_NAMES = List.of(
            "processors",
            "template_pools",
            "jigsaws",
            "structure_sets"
    );

    private static final NbtMap REGISTRIES = loadRegistries();

    public static JigsawStructureDataPacket createPacket() {
        JigsawStructureDataPacket packet = new JigsawStructureDataPacket();
        packet.setJigsawStructureDataTag(REGISTRIES);
        return packet;
    }

    private static NbtMap loadRegistries() {
        Object root;
        try (InputStream stream = RegistryUtils.getOrAssertResource("data/jigsaw_structure_data.nbt");
             NBTInputStream input = NbtUtils.createGZIPReader(stream)) {
            root = input.readTag();
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }

        if (!(root instanceof NbtMap registries)) {
            throw new IllegalArgumentException("Jigsaw structure data root is not a compound");
        }

        for (String registryName : REGISTRY_NAMES) {
            Object value = registries.get(registryName);
            if (!(value instanceof NbtList<?> entries) || entries.getType() != NbtType.COMPOUND) {
                throw new IllegalArgumentException("Jigsaw structure registry " + registryName + " is not a compound list");
            }
        }

        return registries;
    }
}
