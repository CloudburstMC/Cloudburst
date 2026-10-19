package org.cloudburstmc.server.configuration;

import lombok.experimental.UtilityClass;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.dataformat.javaprop.JavaPropsMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;


/**
 * Typed mapping rules for server properties and settings.
 */
@UtilityClass
public class ServerConfigurationMappers {

    public static final JavaPropsMapper PROPERTIES = JavaPropsMapper.builder()
            .propertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE)
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .build();

    public static final YAMLMapper SETTINGS = YAMLMapper.builder()
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
            .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
            .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

}
