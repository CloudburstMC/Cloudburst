package org.cloudburstmc.codegen;

import lombok.experimental.UtilityClass;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@UtilityClass
public class DataGenSupport {
    private static final Comparator<String> NATURAL_ORDER = (left, right) -> {
        int leftIndex = 0;
        int rightIndex = 0;

        while (leftIndex < left.length() && rightIndex < right.length()) {
            char leftCharacter = left.charAt(leftIndex);
            char rightCharacter = right.charAt(rightIndex);
            if (Character.isDigit(leftCharacter) && Character.isDigit(rightCharacter)) {
                int leftNumberStart = leftIndex;
                int rightNumberStart = rightIndex;

                while (leftIndex < left.length() && Character.isDigit(left.charAt(leftIndex))) {
                    leftIndex++;
                }

                while (rightIndex < right.length() && Character.isDigit(right.charAt(rightIndex))) {
                    rightIndex++;
                }

                long leftNumber = Long.parseLong(left.substring(leftNumberStart, leftIndex));
                long rightNumber = Long.parseLong(right.substring(rightNumberStart, rightIndex));
                if (leftNumber != rightNumber) {
                    return Long.compare(leftNumber, rightNumber);
                }
            } else {
                if (leftCharacter != rightCharacter) {
                    return Character.compare(leftCharacter, rightCharacter);
                }

                leftIndex++;
                rightIndex++;
            }
        }

        return Integer.compare(left.length() - leftIndex, right.length() - rightIndex);
    };

    public static List<String> loadComponentIdentifiers(String resourceName) throws IOException {
        try (InputStream input = DataGenSupport.class.getResourceAsStream('/' + resourceName)) {
            if (input == null) {
                throw new IOException("Missing component catalog " + resourceName);
            }

            Map<String, Object> catalog = new ObjectMapper().readValue(input, new TypeReference<>() {});
            Object components = catalog.get("components");
            if (!(components instanceof List<?> values)) {
                throw new IOException("Component catalog " + resourceName + " has no components array");
            }

            return values.stream().map(Object::toString).sorted().toList();
        }
    }

    public static List<GeneratedConstant> constants(Iterable<String> identifiers) {
        List<GeneratedConstant> constants = new ArrayList<>();
        Map<String, String> identifiersByConstant = new HashMap<>();
        Set<String> seenIdentifiers = new HashSet<>();

        for (String identifier : identifiers) {
            if (!seenIdentifiers.add(identifier)) {
                throw new IllegalArgumentException("Duplicate identifier '" + identifier + "'");
            }

            String path = stripMinecraftNamespace(identifier);
            String constant = constantName(path);
            String existing = identifiersByConstant.putIfAbsent(constant, identifier);
            if (existing != null) {
                throw new IllegalArgumentException("'" + identifier + "' and '" + existing + "' both map to " + constant);
            }

            constants.add(new GeneratedConstant(constant, identifier));
        }

        constants.sort(Comparator.comparing(GeneratedConstant::name, NATURAL_ORDER));
        return constants;
    }

    public static String stripMinecraftNamespace(String identifier) {
        int colon = identifier.indexOf(':');
        if (colon < 0) {
            return identifier;
        }

        String namespace = identifier.substring(0, colon);
        if (!"minecraft".equals(namespace)) {
            throw new IllegalArgumentException("Unsupported namespace '" + namespace + "' in '" + identifier + "'");
        }

        return identifier.substring(colon + 1);
    }

    public static String constantName(String identifier) {
        String name = identifier.replaceAll("[^0-9A-Za-z]+", "_")
                .replaceAll("^_+", "")
                .replaceAll("_+$", "")
                .toUpperCase(Locale.ROOT);
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Cannot derive a Java constant from '" + identifier + "'");
        }

        return Character.isDigit(name.charAt(0)) ? '_' + name : name;
    }

    public static Path resolveProjectRoot() {
        Path directory = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (directory != null) {
            if (Files.exists(directory.resolve("settings.gradle.kts"))) {
                return directory;
            }

            directory = directory.getParent();
        }

        throw new IllegalStateException("Could not locate project root (no settings.gradle.kts found)");
    }

    public static void writeSource(Path output, String source) throws IOException {
        Files.createDirectories(output.getParent());
        Files.writeString(output, source.replace("\r\n", "\n"));
    }

    public record GeneratedConstant(String name, String identifier) {
    }
}
