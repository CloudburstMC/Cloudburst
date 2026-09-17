package org.cloudburstmc.server.network;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.protocol.bedrock.data.biome.BiomeDefinitionData;
import org.cloudburstmc.protocol.bedrock.data.biome.BiomeDefinitions;
import org.cloudburstmc.protocol.bedrock.packet.BiomeDefinitionListPacket;
import org.cloudburstmc.server.Bootstrap;
import org.cloudburstmc.server.registry.RegistryUtils;
import tools.jackson.core.type.TypeReference;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Loads the vanilla biome definitions required during the login sequence.
 */
@UtilityClass
public class VanillaBiomeNetworkData {
    private static final BiomeDefinitions DEFINITIONS = loadDefinitions();

    public static BiomeDefinitionListPacket createPacket() {
        BiomeDefinitionListPacket packet = new BiomeDefinitionListPacket();
        packet.setBiomes(DEFINITIONS);
        return packet;
    }

    public static BiomeDefinitions definitions() {
        return DEFINITIONS;
    }

    private static BiomeDefinitions loadDefinitions() {
        try (InputStream input = RegistryUtils.getOrAssertResource("data/stripped_biome_definitions.json")) {
            Map<String, BiomeDefinitionData> definitions = Bootstrap.JSON_MAPPER.readValue(input, new TypeReference<>() {});
            return new BiomeDefinitions(definitions);
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
