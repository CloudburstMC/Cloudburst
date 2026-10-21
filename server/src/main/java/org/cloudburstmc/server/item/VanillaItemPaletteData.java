package org.cloudburstmc.server.item;

import org.cloudburstmc.api.registry.RegistryException;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.Identifiers;
import org.cloudburstmc.nbt.NBTInputStream;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtUtils;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemVersion;
import org.cloudburstmc.server.Bootstrap;
import org.cloudburstmc.server.registry.RegistryUtils;
import tools.jackson.core.type.TypeReference;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable definitions and identifier mappings loaded from bundled item data.
 */
public record VanillaItemPaletteData(
        Map<Identifier, CloudItemDefinition> definitions,
        Map<Identifier, Identifier> aliases,
        Map<Identifier, Map<Integer, Identifier>> variants,
        Map<Integer, Identifier> legacyIds
) {

    public VanillaItemPaletteData {
        definitions = Collections.unmodifiableMap(new LinkedHashMap<>(definitions));
        aliases = Map.copyOf(aliases);
        Map<Identifier, Map<Integer, Identifier>> copiedVariants = new HashMap<>();
        variants.forEach((id, values) -> copiedVariants.put(id, Map.copyOf(values)));
        variants = Map.copyOf(copiedVariants);
        legacyIds = Map.copyOf(legacyIds);
    }

    public static VanillaItemPaletteData load() {
        Map<Integer, Identifier> legacyIds = readLegacyIds("data/legacy_item_ids.json");
        readLegacyIds("data/legacy_block_ids.json").forEach(legacyIds::putIfAbsent);
        Map<Identifier, CloudItemDefinition> definitions = readDefinitions();
        definitions.values().forEach(definition -> legacyIds.putIfAbsent(definition.getRuntimeId(), definition.getCloudIdentifier()));

        Mappings mappings = readJson("data/item_mappings.json", new TypeReference<>() {});
        Map<Identifier, Identifier> aliases = new HashMap<>();
        Objects.requireNonNull(mappings.simple(), "simple item mappings").forEach((source, target) ->
                aliases.put(Identifier.parse(source), requireTarget(definitions, target)));

        Map<Identifier, Map<Integer, Identifier>> variants = new HashMap<>();
        Objects.requireNonNull(mappings.complex(), "complex item mappings").forEach((source, values) -> {
            Map<Integer, Identifier> entries = new HashMap<>();
            values.forEach((meta, target) -> entries.put(meta, requireTarget(definitions, target)));
            variants.put(Identifier.parse(source), entries);
        });

        return new VanillaItemPaletteData(definitions, aliases, variants, legacyIds);
    }

    private static Map<Identifier, CloudItemDefinition> readDefinitions() {
        NbtMap componentData;
        try (InputStream input = RegistryUtils.getOrAssertResource("data/item_components.nbt");
             NBTInputStream reader = NbtUtils.createGZIPReader(input)) {
            if (!(reader.readTag() instanceof NbtMap compound)) {
                throw new RegistryException("Item component data must contain an NBT compound");
            }
            componentData = compound;
        } catch (IOException failure) {
            throw new RegistryException("Unable to load item components", failure);
        }

        List<RuntimeEntry> entries = readJson("data/runtime_item_states.json", new TypeReference<>() {});
        Map<Identifier, CloudItemDefinition> definitions = new LinkedHashMap<>();
        Set<Integer> runtimeIds = new HashSet<>();

        for (RuntimeEntry entry : entries) {
            Identifier id = Identifier.parse(Objects.requireNonNull(entry.name(), "item name"));
            int runtimeId = Objects.requireNonNull(entry.id(), "item runtime ID");
            if (runtimeId < Short.MIN_VALUE || runtimeId > Short.MAX_VALUE || runtimeId == 0) {
                throw new RegistryException("Invalid item runtime ID " + runtimeId + " for " + id);
            }

            if (definitions.containsKey(id) || !runtimeIds.add(runtimeId)) {
                throw new RegistryException("Duplicate item identifier or runtime ID: " + id + " (" + runtimeId + ")");
            }

            Object rawComponents = componentData.get(id.toString());
            if (rawComponents != null && !(rawComponents instanceof NbtMap)) {
                throw new RegistryException("Item component data must be a compound on " + id);
            }

            NbtMap components = rawComponents == null ? NbtMap.EMPTY : (NbtMap) rawComponents;
            VanillaItemDefinitionDataValidator.validate(id, components);
            int versionId = Objects.requireNonNull(entry.version(), "item version");
            if (versionId < 0 || versionId >= ItemVersion.values().length) {
                throw new RegistryException("Unknown item version on " + id + ": " + entry.version());
            }

            ItemVersion version = ItemVersion.from(versionId);
            definitions.put(id, new CloudItemDefinition(id, runtimeId, Objects.requireNonNull(entry.componentBased(), "componentBased"), version, components));
        }

        if (!definitions.containsKey(Identifiers.AIR)) {
            throw new RegistryException("Vanilla item definitions are missing minecraft:air");
        }

        return definitions;
    }

    private static Map<Integer, Identifier> readLegacyIds(String resource) {
        Map<String, Integer> entries = readJson(resource, new TypeReference<>() {});
        Map<Integer, Identifier> ids = new HashMap<>();
        entries.forEach((name, numericId) -> {
            Identifier id = Identifier.parse(name);
            if (ids.putIfAbsent(Objects.requireNonNull(numericId, "legacy item ID"), id) != null) {
                throw new RegistryException("Duplicate numeric ID " + numericId + " in " + resource);
            }
        });
        return ids;
    }

    private static Identifier requireTarget(Map<Identifier, CloudItemDefinition> definitions, String name) {
        Identifier id = Identifier.parse(name);
        if (!definitions.containsKey(id)) {
            throw new RegistryException("Item mapping references an unknown definition: " + id);
        }
        return id;
    }

    private static <T> T readJson(String resource, TypeReference<T> type) {
        try (InputStream input = RegistryUtils.getOrAssertResource(resource)) {
            return Objects.requireNonNull(Bootstrap.JSON_MAPPER.readValue(input, type), resource);
        } catch (IOException | RuntimeException failure) {
            throw new RegistryException("Unable to load " + resource, failure);
        }
    }

    private record RuntimeEntry(String name, Integer id, Integer version, Boolean componentBased) {}

    private record Mappings(Map<String, String> simple, Map<String, Map<Integer, String>> complex) {}
}
