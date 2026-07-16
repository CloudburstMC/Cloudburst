package org.cloudburstmc.server.block;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.LiquidReaction;
import org.cloudburstmc.server.registry.RegistryUtils;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@UtilityClass
class BlockPropertyData {

    private static final TypeReference<List<Map<String, Object>>> BLOCK_PROPERTIES = new TypeReference<>() {};
    private static final Long2ObjectOpenHashMap<StateData> BY_STATE_HASH = load();

    static StateData get(long stateHash) {
        return BY_STATE_HASH.get(stateHash);
    }

    private static Long2ObjectOpenHashMap<StateData> load() {
        List<Map<String, Object>> entries;
        try (InputStream stream = RegistryUtils.getOrAssertResource("data/block_properties.json")) {
            entries = new ObjectMapper().readValue(stream, BLOCK_PROPERTIES);
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }

        Long2ObjectOpenHashMap<StateData> byStateHash = new Long2ObjectOpenHashMap<>(entries.size());
        Map<String, StateData> firstStateByType = new HashMap<>();
        for (Map<String, Object> entry : entries) {
            String name = requiredString(entry, "name", "block property entry");
            long stateHash = requiredNumber(entry, "blockStateHash", name).longValue();
            StateData stateData = readStateData(entry, name);

            StateData previous = byStateHash.putIfAbsent(stateHash, stateData);
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate block state hash " + Long.toUnsignedString(stateHash) + " for " + name);
            }

            StateData firstState = firstStateByType.putIfAbsent(name, stateData);
            if (firstState != null) {
                validateTypeProperties(name, firstState, stateData);
            }
        }

        return byStateHash;
    }

    private static StateData readStateData(Map<String, Object> entry, String name) {
        return new StateData(
                readCollisionBoxes(entry.get("collisionShape"), name),
                readOutlineBox(entry.get("outlineShape"), name),
                requiredNumber(entry, "hardness", name).floatValue(),
                requiredNumber(entry, "explosionResistance", name).floatValue(),
                requiredNumber(entry, "friction", name).floatValue(),
                requiredNumber(entry, "translucency", name).floatValue(),
                requiredNumber(entry, "thickness", name).floatValue(),
                requiredNumber(entry, "burnOdds", name).intValue(),
                requiredNumber(entry, "flameOdds", name).intValue(),
                requiredNumber(entry, "lightDampening", name).intValue(),
                requiredNumber(entry, "lightEmission", name).intValue(),
                requiredBoolean(entry, "isSolid", name),
                requiredBoolean(entry, "requiresCorrectToolForDrops", name),
                requiredString(entry, "mapColor", name),
                requiredBoolean(entry, "canContainLiquidSource", name),
                readLiquidReaction(requiredString(entry, "liquidReactionOnTouch", name), name)
        );
    }

    private static float[] readCollisionBoxes(Object value, String blockName) {
        if (!(value instanceof List<?> boxes)) {
            throw invalidProperty("collisionShape", blockName);
        }

        float[] coordinates = new float[boxes.size() * 6];
        int offset = 0;
        for (Object box : boxes) {
            float[] parsed = readBox(box, "collisionShape", blockName);
            System.arraycopy(parsed, 0, coordinates, offset, parsed.length);
            offset += parsed.length;
        }

        return coordinates;
    }

    private static float[] readOutlineBox(Object value, String blockName) {
        return value == null ? null : readBox(value, "outlineShape", blockName);
    }

    private static float[] readBox(Object value, String property, String blockName) {
        if (!(value instanceof List<?> coordinates) || coordinates.size() != 6) {
            throw invalidProperty(property, blockName);
        }

        float[] box = new float[6];
        for (int index = 0; index < box.length; index++) {
            Object coordinate = coordinates.get(index);
            if (!(coordinate instanceof Number number)) {
                throw invalidProperty(property, blockName);
            }

            box[index] = number.floatValue();
        }

        return box;
    }

    private static LiquidReaction readLiquidReaction(String value, String blockName) {
        return switch (value) {
            case "BROKEN" -> LiquidReaction.BROKEN;
            case "POPPED" -> LiquidReaction.POPPED;
            case "BLOCKING" -> LiquidReaction.BLOCKING;
            case "NO_REACTION", "NOREACTION" -> LiquidReaction.NO_REACTION;
            default -> throw new IllegalArgumentException("Unknown liquidReactionOnTouch for " + blockName + ": " + value);
        };
    }

    private static Number requiredNumber(Map<String, Object> entry, String property, String blockName) {
        Object value = entry.get(property);
        if (value instanceof Number number) {
            return number;
        }

        throw invalidProperty(property, blockName);
    }

    private static boolean requiredBoolean(Map<String, Object> entry, String property, String blockName) {
        Object value = entry.get(property);
        if (value instanceof Boolean bool) {
            return bool;
        }

        throw invalidProperty(property, blockName);
    }

    private static String requiredString(Map<String, Object> entry, String property, String context) {
        Object value = entry.get(property);
        if (value instanceof String string && !string.isEmpty()) {
            return string;
        }

        throw invalidProperty(property, context);
    }

    private static IllegalArgumentException invalidProperty(String property, String blockName) {
        return new IllegalArgumentException("Missing or invalid " + property + " for " + blockName);
    }

    private static void validateTypeProperties(String name, StateData first, StateData state) {
        if (Float.compare(first.hardness(), state.hardness()) != 0
                || Float.compare(first.explosionResistance(), state.explosionResistance()) != 0
                || Float.compare(first.friction(), state.friction()) != 0
                || Float.compare(first.translucency(), state.translucency()) != 0
                || Float.compare(first.thickness(), state.thickness()) != 0
                || first.burnOdds() != state.burnOdds()
                || first.flameOdds() != state.flameOdds()
                || first.solid() != state.solid()
                || first.requiresCorrectToolForDrops() != state.requiresCorrectToolForDrops()) {
            throw new IllegalArgumentException("Type-level block properties vary by state for " + name);
        }
    }

    record StateData(
            float[] collisionBoxes,
            float[] outlineShape,
            float hardness,
            float explosionResistance,
            float friction,
            float translucency,
            float thickness,
            int burnOdds,
            int flameOdds,
            int lightDampening,
            int lightEmission,
            boolean solid,
            boolean requiresCorrectToolForDrops,
            String mapColor,
            boolean canContainLiquidSource,
            LiquidReaction liquidReactionOnTouch
    ) {
    }
}
