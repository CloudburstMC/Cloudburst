package org.cloudburstmc.server;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.protocol.bedrock.data.definitions.BlockDefinition;
import org.cloudburstmc.server.block.BlockDefinitionDeserializer;
import org.cloudburstmc.server.block.BlockStateDeserializer;
import org.cloudburstmc.server.utils.ColorDeserializer;
import tools.jackson.core.Version;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

import java.awt.*;

/**
 * Shared JSON mapper with server value deserializers.
 */
@UtilityClass
public class ServerJson {

    public static final JsonMapper MAPPER = JsonMapper.builder()
            .addModule(createModule())
            .build();

    private static SimpleModule createModule() {
        SimpleModule module = new SimpleModule("Cloudburst", new Version(0, 0, 1, null, null, null));
        module.addDeserializer(BlockState.class, new BlockStateDeserializer());
        module.addDeserializer(BlockDefinition.class, new BlockDefinitionDeserializer());
        module.addDeserializer(Color.class, new ColorDeserializer());
        return module;
    }
}
