package org.cloudburstmc.codegen;

import com.palantir.javapoet.*;
import lombok.experimental.UtilityClass;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public class ItemDataGen {

    private static final String API_ITEM_PACKAGE = "org.cloudburstmc.api.item";
    private static final String API_UTIL_PACKAGE = "org.cloudburstmc.api.util";

    private static final Pattern IDENTIFIER_CONSTANT = Pattern.compile(
            "public\\s+static\\s+final\\s+Identifier\\s+(\\w+)\\s*=\\s*Identifier\\.parse\\(\"([^\"]+)\"\\)");
    private static final Pattern BLOCK_TYPE_CONSTANT = Pattern.compile(
            "public\\s+static\\s+final\\s+BlockType\\s+\\w+\\s*=\\s*BlockType\\.of\\(BlockIds\\.(\\w+)");

    private static final Comparator<String> NATURAL_ORDER = (left, right) -> {
        int leftIdx = 0;
        int rightIdx = 0;

        while (leftIdx < left.length() && rightIdx < right.length()) {
            char leftChar = left.charAt(leftIdx), rightChar = right.charAt(rightIdx);
            if (Character.isDigit(leftChar) && Character.isDigit(rightChar)) {
                int leftNumStart = leftIdx, rightNumStart = rightIdx;

                while (leftIdx < left.length() && Character.isDigit(left.charAt(leftIdx))) {
                    leftIdx++;
                }

                while (rightIdx < right.length() && Character.isDigit(right.charAt(rightIdx))) {
                    rightIdx++;
                }

                long leftNum = Long.parseLong(left.substring(leftNumStart, leftIdx));
                long rightNum = Long.parseLong(right.substring(rightNumStart, rightIdx));

                if (leftNum != rightNum) {
                    return Long.compare(leftNum, rightNum);
                }
            } else {
                if (leftChar != rightChar) {
                    return Character.compare(leftChar, rightChar);
                }

                leftIdx++;
                rightIdx++;
            }
        }

        return Integer.compare(left.length() - leftIdx, right.length() - rightIdx);
    };

    static void main() throws IOException {
        Path projectRoot = resolveProjectRoot();
        Path outputRoot = projectRoot.resolve("api/src/main/java");

        GenerationResult result = generate(projectRoot);
        Files.createDirectories(outputRoot);
        writeCompact(result.itemIds(), outputRoot, "ItemIds.java");
        writeCompact(result.itemTypes(), outputRoot, "ItemTypes.java");
    }

    private static GenerationResult generate(Path projectRoot) throws IOException {
        Path dataDir = projectRoot.resolve("server/src/main/resources/data");
        Path blockIdsJava = projectRoot.resolve("api/src/main/java/org/cloudburstmc/api/block/BlockIds.java");
        Path blockTypesJava = projectRoot.resolve("api/src/main/java/org/cloudburstmc/api/block/BlockTypes.java");

        RuntimeItems runtimeItems = loadRuntimeItems(dataDir.resolve("runtime_item_states.json"));
        Set<String> blockItemIdentifiers = parseBlockItemIdentifiers(blockIdsJava, blockTypesJava);
        Set<String> mappedAliasIdentifiers = loadMappedAliasIdentifiers(dataDir.resolve("item_mappings.json"));

        List<ItemConstant> allIdConstants = buildItemConstants(runtimeItems.identifiers());
        int skippedMappedAliases = (int) allIdConstants.stream()
                .filter(item -> mappedAliasIdentifiers.contains(item.identifier()))
                .count();
        List<ItemConstant> idConstants = new ArrayList<>(allIdConstants.stream()
                .filter(item -> !mappedAliasIdentifiers.contains(item.identifier()))
                .toList());
        idConstants.sort(Comparator.comparing(ItemConstant::constantName, NATURAL_ORDER));

        List<String> typeConstants = new ArrayList<>();
        int skippedBlockBackedItems = 0;
        for (ItemConstant item : idConstants) {
            if (blockItemIdentifiers.contains(item.identifier()) && !runtimeItems.itemAliases().contains(item.identifier())) {
                skippedBlockBackedItems++;
                continue;
            }
            typeConstants.add(item.constantName());
        }
        typeConstants.sort(NATURAL_ORDER);

        return new GenerationResult(
                generateItemIds(idConstants),
                generateItemTypes(typeConstants),
                typeConstants.size(),
                skippedBlockBackedItems,
                skippedMappedAliases);
    }

    private static JavaFile generateItemIds(List<ItemConstant> constants) {
        ClassName utilityClass = ClassName.get("lombok.experimental", "UtilityClass");
        ClassName identifier = ClassName.get(API_UTIL_PACKAGE, "Identifier");

        TypeSpec.Builder type = TypeSpec.classBuilder("ItemIds")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class)
                        .addMember("value", "$S", "RedundantModifiersUtilityClassLombok")
                        .build())
                .addAnnotation(utilityClass)
                .addJavadoc("Generated by {@code ItemDataGen}, do not edit by hand.\n");

        for (ItemConstant constant : constants) {
            type.addField(FieldSpec.builder(identifier,
                            constant.constantName(),
                            Modifier.PUBLIC,
                            Modifier.STATIC,
                            Modifier.FINAL)
                    .initializer("$T.parse($S)", identifier, constant.identifier())
                    .build());
        }

        type.addField(FieldSpec.builder(identifier, "UNKNOWN", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("$T.parse($S)", identifier, "unknown")
                .build());

        return JavaFile.builder(API_ITEM_PACKAGE, type.build())
                .skipJavaLangImports(true)
                .indent("    ")
                .build();
    }

    private static JavaFile generateItemTypes(List<String> constants) {
        ClassName utilityClass = ClassName.get("lombok.experimental", "UtilityClass");
        ClassName identifiers = ClassName.get(API_UTIL_PACKAGE, "Identifiers");
        ClassName itemIds = ClassName.get(API_ITEM_PACKAGE, "ItemIds");
        ClassName itemType = ClassName.get(API_ITEM_PACKAGE, "ItemType");

        TypeSpec.Builder type = TypeSpec.classBuilder("ItemTypes")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(utilityClass)
                .addJavadoc("Generated by {@code ItemDataGen}, do not edit by hand.\n");

        for (String constant : constants) {
            type.addField(FieldSpec.builder(itemType, constant, Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    .initializer("$T.of($T.$L)", itemType, itemIds, constant)
                    .build());
        }

        type.addField(FieldSpec.builder(itemType, "UNKNOWN", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("$T.of($T.UNKNOWN)", itemType, identifiers)
                .build());

        return JavaFile.builder(API_ITEM_PACKAGE, type.build())
                .skipJavaLangImports(true)
                .indent("    ")
                .build();
    }

    private static void writeCompact(JavaFile javaFile, Path outputRoot, String fileName) throws IOException {
        Path output = outputRoot.resolve(API_ITEM_PACKAGE.replace('.', '/')).resolve(fileName);
        Files.createDirectories(output.getParent());
        Files.writeString(output, compactGeneratedSource(javaFile.toString()));
    }

    private static String compactGeneratedSource(String source) {
        return source.replace("\r\n", "\n")
                .replaceAll("\n\n(    public static final )", "\n$1")
                .replaceAll("\n(    public static final \\w+ UNKNOWN )", "\n\n$1");
    }

    private static RuntimeItems loadRuntimeItems(Path runtimeItemStates) throws IOException {
        List<Map<String, Object>> data = new ObjectMapper().readValue(runtimeItemStates.toFile(), new TypeReference<>() {});
        List<String> identifiers = new ArrayList<>();
        Set<String> aliases = new HashSet<>();
        Set<String> seen = new HashSet<>();

        for (Map<String, Object> entry : data) {
            String identifier = stripNamespace(entry.get("name").toString());
            if ("unknown".equals(identifier) || seen.contains(identifier)) {
                continue;
            }

            if (identifier.startsWith("item.")) {
                aliases.add(identifier.substring("item.".length()));
                continue;
            }

            seen.add(identifier);
            identifiers.add(identifier);
        }

        return new RuntimeItems(identifiers, aliases);
    }

    private static Set<String> loadMappedAliasIdentifiers(Path itemMappings) throws IOException {
        Map<String, Object> data = new ObjectMapper().readValue(itemMappings.toFile(), new TypeReference<>() {});
        Set<String> aliases = new HashSet<>();
        Object simple = data.get("simple");
        if (simple instanceof Map<?, ?> simpleMap) {
            for (Object identifier : simpleMap.keySet()) {
                aliases.add(stripNamespace(identifier.toString()));
            }
        }

        Object complex = data.get("complex");
        if (complex instanceof Map<?, ?> complexMap) {
            for (Map.Entry<?, ?> entry : complexMap.entrySet()) {
                String identifier = stripNamespace(entry.getKey().toString());
                if (entry.getValue() instanceof Map<?, ?> metaMap) {
                    Object metaZero = metaMap.get("0");
                    if (metaZero != null && !identifier.equals(stripNamespace(metaZero.toString()))) {
                        aliases.add(identifier);
                    }
                }
            }
        }

        return aliases;
    }

    private static Set<String> parseBlockItemIdentifiers(Path blockIdsJava, Path blockTypesJava) throws IOException {
        Map<String, String> blockIdByConstant = new HashMap<>();
        for (Map.Entry<String, String> entry : parseIdentifierConstants(blockIdsJava).entrySet()) {
            blockIdByConstant.put(entry.getValue(), entry.getKey());
        }

        Set<String> blockItems = new LinkedHashSet<>();
        Matcher matcher = BLOCK_TYPE_CONSTANT.matcher(Files.readString(blockTypesJava));
        while (matcher.find()) {
            String identifier = blockIdByConstant.get(matcher.group(1));
            if (identifier != null) {
                blockItems.add(identifier);
            }
        }

        return blockItems;
    }

    private static Map<String, String> parseIdentifierConstants(Path source) throws IOException {
        Map<String, String> identifiers = new LinkedHashMap<>();
        Matcher matcher = IDENTIFIER_CONSTANT.matcher(Files.readString(source));
        while (matcher.find()) {
            identifiers.put(matcher.group(2), matcher.group(1));
        }

        return identifiers;
    }

    private static List<ItemConstant> buildItemConstants(List<String> identifiers) {
        List<ItemConstant> constants = new ArrayList<>();
        Map<String, String> identifiersByConstant = new HashMap<>();

        for (String identifier : identifiers) {
            String constant = constantName(identifier);
            String existing = identifiersByConstant.putIfAbsent(constant, identifier);
            if (existing != null && !existing.equals(identifier)) {
                throw new IllegalStateException("'" + identifier + "' and '" + existing + "' both map to ItemIds." + constant);
            }
            constants.add(new ItemConstant(constant, identifier));
        }

        return constants;
    }

    private static String stripNamespace(String identifier) {
        int colon = identifier.indexOf(':');
        if (colon < 0) {
            return identifier;
        }

        String namespace = identifier.substring(0, colon);
        if (!"minecraft".equals(namespace)) {
            throw new IllegalArgumentException("Unsupported item namespace '" + namespace + "' in '" + identifier + "'");
        }

        return identifier.substring(colon + 1);
    }

    private static String constantName(String identifier) {
        String name = identifier.replaceAll("[^0-9A-Za-z]+", "_")
                .replaceAll("^_+", "")
                .replaceAll("_+$", "")
                .toUpperCase(Locale.ROOT);
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Cannot derive a Java constant from '" + identifier + "'");
        }

        if (Character.isDigit(name.charAt(0))) {
            name = "_" + name;
        }

        return name;
    }

    private static Path resolveProjectRoot() {
        Path dir = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (dir != null) {
            if (Files.exists(dir.resolve("settings.gradle.kts"))) {
                return dir;
            }

            dir = dir.getParent();
        }
        throw new IllegalStateException("Could not locate project root (no settings.gradle.kts found)");
    }

    private record RuntimeItems(List<String> identifiers, Set<String> itemAliases) {
    }

    private record ItemConstant(String constantName, String identifier) {
    }

    private record GenerationResult(JavaFile itemIds, JavaFile itemTypes, int itemTypeCount, int skippedBlockBackedItems, int skippedMappedAliases) {
    }
}
