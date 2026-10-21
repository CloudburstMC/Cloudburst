package org.cloudburstmc.server.block;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import lombok.experimental.UtilityClass;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.LiquidReaction;
import org.cloudburstmc.server.registry.RegistryUtils;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@UtilityClass
public class BlockPropertyData {

    private static final TypeReference<List<Map<String, Object>>> BLOCKS = new TypeReference<>() {};
    private static final Long2ObjectOpenHashMap<StateData> BY_STATE_HASH = load();

    public static @Nullable StateData get(long stateHash) {
        return BY_STATE_HASH.get(stateHash);
    }

    private static Long2ObjectOpenHashMap<StateData> load() {
        List<Map<String, Object>> entries;
        try (InputStream stream = RegistryUtils.getOrAssertResource("data/blocks.json")) {
            entries = new ObjectMapper().readValue(stream, BLOCKS);
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }

        Long2ObjectOpenHashMap<StateData> byStateHash = new Long2ObjectOpenHashMap<>(entries.size());
        Map<String, StateData> firstStateByType = new HashMap<>();
        for (Map<String, Object> entry : entries) {
            String name = requiredString(entry, "name", "block property entry");
            long stateHash = requiredInteger(entry, "blockStateHash", name, 0, 0xFFFF_FFFFL);
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
                requiredString(entry, "translationKey", name),
                requiredBoolean(entry, "isSolid", name),
                requiredFloat(entry, "hardness", name),
                requiredFloat(entry, "explosionResistance", name),
                requiredFloat(entry, "friction", name),
                requiredFloat(entry, "thickness", name),
                requiredBoolean(entry, "requiresCorrectToolForDrops", name),
                requiredFloat(entry, "translucency", name),
                requiredString(entry, "mapColor", name),
                requiredString(entry, "tintMethod", name),
                requiredInt(entry, "lightEmission", name),
                requiredInt(entry, "lightDampening", name),
                requiredInt(entry, "burnOdds", name),
                requiredInt(entry, "flameOdds", name),
                requiredBoolean(entry, "canContainLiquidSource", name),
                readLiquidReaction(requiredString(entry, "liquidReactionOnTouch", name), name),
                BlockGeometryData.read(entry.get("geometry"), name)
        );
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

    private static LiquidReaction readLiquidReaction(String value, String blockName) {
        return switch (value) {
            case "BROKEN" -> LiquidReaction.BROKEN;
            case "POPPED" -> LiquidReaction.POPPED;
            case "BLOCKING" -> LiquidReaction.BLOCKING;
            case "NOREACTION" -> LiquidReaction.NO_REACTION;
            default -> throw new IllegalArgumentException("Unknown liquidReactionOnTouch for " + blockName + ": " + value);
        };
    }

    private static float requiredFloat(Map<String, Object> entry, String property, String blockName) {
        Object value = entry.get(property);
        if (value instanceof Number number && Float.isFinite(number.floatValue())) {
            return number.floatValue();
        }

        throw invalidProperty(property, blockName);
    }

    private static int requiredInt(Map<String, Object> entry, String property, String blockName) {
        return (int) requiredInteger(entry, property, blockName, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    private static long requiredInteger(Map<String, Object> entry, String property, String blockName, long minimum, long maximum) {
        Object value = entry.get(property);
        if (!(value instanceof Number number)) {
            throw invalidProperty(property, blockName);
        }

        try {
            long integer = new BigDecimal(number.toString()).longValueExact();
            if (integer >= minimum && integer <= maximum) {
                return integer;
            }
        } catch (ArithmeticException | NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid integer " + property + " for " + blockName, exception);
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

    public record StateData(
            String translationKey,
            boolean solid,
            float hardness,
            float explosionResistance,
            float friction,
            float thickness,
            boolean requiresCorrectToolForDrops,
            float translucency,
            String mapColor,
            String tintMethod,
            int lightEmission,
            int lightDampening,
            int burnOdds,
            int flameOdds,
            boolean canContainLiquidSource,
            LiquidReaction liquidReactionOnTouch,
            BlockGeometryData geometry
    ) {
    }
}
