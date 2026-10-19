package org.cloudburstmc.server.level.generator;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.protocol.bedrock.data.definitions.BlockDefinition;
import org.cloudburstmc.server.block.BlockDefinitionDeserializer;
import org.cloudburstmc.server.block.BlockStateDeserializer;
import org.cloudburstmc.server.utils.ColorDeserializer;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.awt.*;

/**
 * Typed mapping rules for generator definitions.
 */
@UtilityClass
public class GeneratorConfigurationMapper {

    public static final YAMLMapper MAPPER = YAMLMapper.builder()
            .addModule(module())
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .build();

    private static SimpleModule module() {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(BlockState.class, new BlockStateDeserializer());
        module.addDeserializer(BlockDefinition.class, new BlockDefinitionDeserializer());
        module.addDeserializer(Color.class, new ColorDeserializer());
        return module;
    }
}
