package org.cloudburstmc.server.block.util;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.nukkitx.math.vector.Vector3i;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.val;
import net.daporkchop.lib.common.misc.Tuple;
import net.daporkchop.lib.common.misc.string.PStrings;
import net.daporkchop.lib.common.ref.Ref;
import net.daporkchop.lib.common.ref.ThreadRef;
import net.daporkchop.lib.common.util.PorkUtil;
import org.cloudburstmc.server.block.BlockPalette;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.block.trait.BlockTrait;
import org.cloudburstmc.server.registry.BlockRegistry;
import org.cloudburstmc.server.utils.Identifier;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;
import static net.daporkchop.lib.common.util.PorkUtil.uncheckedCast;

@UtilityClass
public class BlockUtils {

    private final Ref<Matcher> SINGLE_STATE_PATTERN = ThreadRef.regex(Pattern.compile(
            "^((?:[a-z0-9_]+:)?[a-z0-9_]+)(\\{(?:[a-z_]+=[a-z0-9_]+(?:,\\s*)?)+\\})?$", Pattern.CASE_INSENSITIVE));
    private final Ref<Matcher> SINGLE_TRAIT_PATTERN = ThreadRef.regex(Pattern.compile(
            "(?<=^\\{|,|\\s)([a-z_]+)=([a-z0-9_]+),?(?=\\}$|,)", Pattern.CASE_INSENSITIVE));

    private final Ref<Matcher> WILDCARD_STATE_PATTERN = ThreadRef.regex(Pattern.compile(
            "^((?:[a-z0-9_]+:)?[a-z0-9_]+)(\\{(?:[a-z_]+=(?:[a-z0-9_]+|\\*)(?:,\\s*)?)+\\})?$", Pattern.CASE_INSENSITIVE));
    private final Ref<Matcher> WILDCARD_TRAIT_PATTERN = ThreadRef.regex(Pattern.compile(
            "(?<=^\\{|,|\\s)([a-z_]+)=([a-z0-9_]+|\\*),?(?=\\}$|,)", Pattern.CASE_INSENSITIVE));

    private final Ref<Matcher> INT_PATTERN = ThreadRef.regex(Pattern.compile(
            "\\d+", Pattern.CASE_INSENSITIVE));


    public long key(Vector3i position) {
        return key(position.getX(), position.getY(), position.getZ());
    }

    public long key(int x, int y, int z) {
        if (y < 0 || y >= 256) {
            throw new IllegalArgumentException("Y coordinate y is out of range!");
        }
        return (((long) x & (long) 0xFFFFFFF) << 36) | (((long) y & (long) 0xFF) << 28) | ((long) z & (long) 0xFFFFFFF);
    }

    public Vector3i fromKey(long key) {
        int x = (int) ((key >>> 36) & 0xFFFFFFF);
        int y = (int) ((key >> 27) & 0xFF);
        int z = (int) (key & 0xFFFFFFF);
        return Vector3i.from(x, y, z);
    }

    public <T extends Comparable<T>> BlockTrait<T> findTrait(@NonNull BlockState state, @NonNull String traitName) {
        BlockTrait<?> trait = BlockTraits.from(traitName);
        if (trait == null) {
            //fall back to vanilla name
            trait = BlockTraits.fromVanilla(traitName);
        }
        checkArgument(trait != null, "unknown trait: \"%s\"", traitName);
        checkArgument(state.getTrait(trait) != null, "block %s doesn't contain trait \"%s\"!", state.getType(), traitName);
        return uncheckedCast(trait);
    }

    public <T extends Comparable<T>> T parseTrait(@NonNull BlockTrait<T> trait, @NonNull String valueText) {
        for (T value : trait.getPossibleValues()) {
            if (valueText.equalsIgnoreCase(value.toString())) {
                return value;
            }
        }
        throw new IllegalArgumentException(PStrings.fastFormat("trait %s (vanilla: %s) doesn't contain value \"%s\"", trait.getName(), PorkUtil.fallbackIfNull(trait.getVanillaName(), trait.getName()), valueText));
    }

    public BlockState applyTrait(@NonNull BlockState state, @NonNull String traitName, @NonNull String valueText) {
        BlockTrait<?> trait = findTrait(state, traitName);
        return state.withTrait(trait, uncheckedCast(parseTrait(trait, valueText)));
    }

    private void applyDefaultValues(Identifier id, Map<String, Object> traits) {
        val vanillaTraits = BlockPalette.INSTANCE.getVanillaTraitMap();

        BlockPalette.INSTANCE.getTraits(id).forEach(name -> {
            traits.computeIfAbsent(name, k -> Iterables.get(vanillaTraits.get(name), 0));
        });
    }

    public Object convertTraitValue(String value) {
        value = value.toLowerCase();

        if (value.equals("false")) {
            return false;
        }

        if (value.equals("true")) {
            return true;
        }

        if (INT_PATTERN.get().reset(value).matches()) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ignore) {

            }
        }

        return value;
    }

    /**
     * Parses a {@link BlockState} formatted as:
     * <p>
     * {@code [namespace:]<id>[{trait=value[,trait2=value2[,...]]}]}
     * <p>
     * Examples:
     * <p>
     * - {@code minecraft:stone}
     * - {@code stone{stone_type=granite}}
     * - {@code minecraft:golden_rail{is_powered=true,simple_rail_direction=north_south}}
     *
     * @param input the {@link String} to parse
     * @return the parsed {@link BlockState}
     * @throws IllegalArgumentException if the input could not be parsed
     */
    public static BlockState parseState(@NonNull String input) {
        Matcher matcher = SINGLE_STATE_PATTERN.get().reset(input);
        checkArgument(matcher.find(), "unable to parse block state: \"%s\"", input);

        String idText = matcher.group(1);
        Identifier blockId = Identifier.fromString(idText);
        checkArgument(BlockRegistry.get().getBlock(blockId) != null, "unknown block: \"%s\"", idText);

        Map<String, Object> traits = new HashMap<>();
        String traitsTxt = matcher.group(2);
        if (traitsTxt != null) {
            matcher = SINGLE_TRAIT_PATTERN.get().reset(traitsTxt);
            while (matcher.find()) {
                traits.put(matcher.group(1), convertTraitValue(matcher.group(2)));
            }
        }

        applyDefaultValues(blockId, traits);

        BlockState state = BlockPalette.INSTANCE.getState(blockId, traits);
        checkArgument(state != null, "unknown block: \"%s\" with traits: \"%s\" (%s)", idText, traits, traitsTxt);

        return state;
    }

    /**
     * Variant of {@link #parseState(String)} which can process trait values with wildcards.
     * <p>
     * Examples:
     * <p>
     * - {@code minecraft:stone}
     * - {@code stone{stone_type=granite}}
     * - {@code minecraft:golden_rail{is_powered=*,simple_rail_direction=north_south}}
     *
     * @param input the {@link String} to parse
     * @return all {@link BlockState}s which matched the input
     * @throws IllegalArgumentException if the input could not be parsed
     */
    public static Stream<BlockState> parseStateWildcard(@NonNull String input) {
        Matcher matcher = WILDCARD_STATE_PATTERN.get().reset(input);
        checkArgument(matcher.find(), "unable to parse block state: \"%s\"", input);

        String idText = matcher.group(1);
        Identifier blockId = Identifier.fromString(idText);
        checkArgument(BlockRegistry.get().getBlock(blockId) != null, "unknown block: \"%s\"", idText);

        List<Map<String, Object>> variants = new ArrayList<>();

        String traitsTxt = matcher.group(2);
        Map<String, Object> base = new HashMap<>();
        if (traitsTxt != null) {
            List<String> wildcart = new LinkedList<>();

            matcher = WILDCARD_TRAIT_PATTERN.get().reset(traitsTxt);
            while (matcher.find()) {
                String traitName = matcher.group(1);
                String valueText = matcher.group(2);
                if ("*".equals(valueText)) {
                    wildcart.add(traitName);
                } else {
                    base.put(traitName, convertTraitValue(valueText));
                }
            }

            applyDefaultValues(blockId, base);

            if (wildcart.isEmpty()) {
                variants.add(base);
            } else {
                Lists.cartesianProduct(wildcart.stream().flatMap(name ->
                        BlockPalette.INSTANCE.getVanillaTraitMap().getOrDefault(name, Collections.emptySet())
                                .stream()
                                .map(v -> new Tuple<>(name, v))
                )
                        .collect(Collectors.toList()))
                        .forEach(entries ->
                                variants.add(entries.stream().collect(Collectors.toMap(Tuple::getA, Tuple::getB)))
                        );
            }
        } else {
            applyDefaultValues(blockId, base);
            variants.add(base);
        }

        return variants.stream().map(traits -> {
            BlockState state = BlockPalette.INSTANCE.getState(blockId, traits);
            checkArgument(state != null, "unknown block: \"%s\" with traits: \"%s\" (%s)", idText, traits, traitsTxt);

            return state;
        });
    }
}
