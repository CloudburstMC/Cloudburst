package org.cloudburstmc.codegen.support;

import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.nio.file.Path;

@UtilityClass
public class IdentifierCatalogGenerator {
    public static void generate(String packageName, String typeName, String catalogName,
                                String description, Iterable<String> identifiers) throws IOException {
        StringBuilder source = new StringBuilder("""
                package %s;
                
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
                
                """.formatted(packageName, description, catalogName, typeName, typeName));
        for (DataGenSupport.GeneratedConstant constant : DataGenSupport.constants(identifiers)) {
            source.append("    public static final ").append(typeName).append(' ').append(constant.name())
                    .append(" = type(\"").append(DataGenSupport.stripMinecraftNamespace(constant.identifier()))
                    .append("\");\n");
        }
        source.append("""
                
                    /**
                     * Finds a built-in type by identifier.
                     *
                     * @param id the identifier
                     * @return the matching type, if present
                     */
                    public static Optional<%s> get(Identifier id) {
                        return TYPES.get(id);
                    }
                
                    /**
                     * Returns the built-in types in declaration order.
                     *
                     * @return an unmodifiable list of built-in types
                     */
                    public static List<%s> values() {
                        return TYPES.values();
                    }
                
                    private static %s type(String id) {
                        return TYPES.register(%s.of(Identifier.parse(id)));
                    }
                }
                """.formatted(typeName, typeName, typeName, typeName));
        Path output = DataGenSupport.resolveProjectRoot().resolve("api/src/main/java")
                .resolve(packageName.replace('.', '/')).resolve(catalogName + ".java");
        DataGenSupport.writeSource(output, source.toString());
    }
}
