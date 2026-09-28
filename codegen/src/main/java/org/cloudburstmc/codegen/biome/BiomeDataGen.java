package org.cloudburstmc.codegen.biome;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.codegen.support.DataGenSupport;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.cloudburstmc.codegen.support.DataGenSupport.*;

@UtilityClass
public class BiomeDataGen {
    private static final Path BIOME_TYPES_PATH = Path.of("org", "cloudburstmc", "api", "level", "biome", "BiomeTypes.java");
    private static final Path BEHAVIOR_COMPONENTS_PATH = Path.of("org", "cloudburstmc", "api", "level", "biome", "BiomeBehaviorComponentTypes.java");
    private static final Path APPEARANCE_COMPONENTS_PATH = Path.of("org", "cloudburstmc", "api", "level", "biome", "BiomeAppearanceComponentTypes.java");

    public static void generate() throws IOException {
        Path projectRoot = resolveProjectRoot();
        Path outputRoot = projectRoot.resolve("api/src/main/java");

        writeSource(outputRoot.resolve(BIOME_TYPES_PATH), generateBiomeTypes(projectRoot));
        writeSource(outputRoot.resolve(BEHAVIOR_COMPONENTS_PATH), generateComponentTypes(
                "BiomeBehaviorComponentTypes", "BiomeBehaviorComponentType",
                "biome/biome_behavior_components.json", "biome behavior",
                "Component identifiers for vanilla biome behavior and world-generation definitions."));
        writeSource(outputRoot.resolve(APPEARANCE_COMPONENTS_PATH), generateComponentTypes(
                "BiomeAppearanceComponentTypes", "BiomeAppearanceComponentType",
                "biome/biome_appearance_components.json", "biome appearance",
                "Component identifiers for resource-pack biome presentation, including colors and ambient sounds."));
    }

    private static String generateBiomeTypes(Path projectRoot) {
        Path definitionsPath = projectRoot.resolve("server/src/main/resources/data/stripped_biome_definitions.json");
        Map<String, Object> definitions = new ObjectMapper().readValue(definitionsPath.toFile(), new TypeReference<>() {});
        List<DataGenSupport.GeneratedConstant> biomeConstants = constants(definitions.keySet());

        StringBuilder source = new StringBuilder();
        source.append("""
                package org.cloudburstmc.api.level.biome;
                
                import lombok.experimental.UtilityClass;
                import org.cloudburstmc.api.internal.BuiltInTypeCatalog;
                import org.cloudburstmc.api.util.Identifier;
                
                import java.util.List;
                import java.util.Optional;
                
                /**
                 * Built-in biome types for biome lookup and assignment.
                 *
                 * <p>Generated catalog. Do not edit by hand.
                 */
                @UtilityClass
                public class BiomeTypes {
                    private static final BuiltInTypeCatalog<BiomeType> TYPES = BuiltInTypeCatalog.create(BiomeType::getId);
                
                """);

        for (DataGenSupport.GeneratedConstant constant : biomeConstants) {
            source.append("    public static final BiomeType ")
                    .append(constant.name())
                    .append(" = type(\"")
                    .append(stripMinecraftNamespace(constant.identifier()))
                    .append("\");\n");
        }

        source.append("""
                
                    /**
                     * Returns a built-in biome type.
                     *
                     * @param id biome identifier
                     * @return matching biome type, if present
                     */
                    public static Optional<BiomeType> get(Identifier id) {
                        return TYPES.get(id);
                    }
                
                    /**
                     * Returns all built-in biome types.
                     *
                     * @return biome types in declaration order
                     */
                    public static List<BiomeType> values() {
                        return TYPES.values();
                    }
                
                    private static BiomeType type(String id) {
                        return TYPES.register(BiomeType.of(Identifier.parse(id)));
                    }
                }
                """);
        return source.toString();
    }

    private static String generateComponentTypes(String catalogClass, String typeClass, String resourceName,
                                                 String description, String catalogDescription) throws IOException {
        List<DataGenSupport.GeneratedConstant> componentConstants = constants(loadIdentifiers(resourceName, "components"));

        StringBuilder source = new StringBuilder();
        source.append("""
                package org.cloudburstmc.api.level.biome;
                
                import lombok.experimental.UtilityClass;
                import org.cloudburstmc.api.internal.BuiltInTypeCatalog;
                import org.cloudburstmc.api.util.Identifier;
                
                import java.util.List;
                import java.util.Optional;
                
                /**
                 * %s
                 *
                 * <p>Generated catalog. Do not edit by hand.
                 */
                @UtilityClass
                public class %s {
                    private static final BuiltInTypeCatalog<%s> TYPES = BuiltInTypeCatalog.create(%s::getId);
                
                """.formatted(catalogDescription, catalogClass, typeClass, typeClass));

        for (DataGenSupport.GeneratedConstant constant : componentConstants) {
            source.append("    public static final ")
                    .append(typeClass).append(' ').append(constant.name())
                    .append(" = type(\"")
                    .append(stripMinecraftNamespace(constant.identifier()))
                    .append("\");\n");
        }

        source.append("""
                
                    /**
                     * Returns a built-in %s component type.
                     *
                     * @param id component identifier
                     * @return matching component type, if present
                     */
                    public static Optional<%s> get(Identifier id) {
                        return TYPES.get(id);
                    }
                
                    /**
                     * Returns all built-in %s component types.
                     *
                     * @return component types in declaration order
                     */
                    public static List<%s> values() {
                        return TYPES.values();
                    }
                
                    private static %s type(String id) {
                        return TYPES.register(%s.of(Identifier.parse(id)));
                    }
                }
                """.formatted(description, typeClass, description, typeClass, typeClass, typeClass));
        return source.toString();
    }
}
