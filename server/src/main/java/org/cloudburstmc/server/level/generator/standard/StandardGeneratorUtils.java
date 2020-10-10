package org.cloudburstmc.server.level.generator.standard;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.val;
import net.daporkchop.lib.common.misc.file.PFiles;
import net.daporkchop.lib.common.misc.string.PStrings;
import net.daporkchop.lib.common.ref.Ref;
import net.daporkchop.lib.common.ref.ThreadRef;
import net.daporkchop.lib.common.util.PorkUtil;
import org.cloudburstmc.server.Bootstrap;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.block.trait.BlockTrait;
import org.cloudburstmc.server.utils.Identifier;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;
import static net.daporkchop.lib.common.util.PorkUtil.uncheckedCast;

/**
 * Various helper methods used by the Cloudburst standard generator.
 *
 * @author DaPorkchop_
 */
@UtilityClass
public class StandardGeneratorUtils {
    private static final Ref<Matcher> SINGLE_STATE_PATTERN = ThreadRef.regex(Pattern.compile(
            "^((?:[a-z0-9_]+:)?[a-z0-9_]+)(\\{(?:[a-z_]+=[a-z0-9_]+(?:,\\s*)?)+\\})?$", Pattern.CASE_INSENSITIVE));
    private static final Ref<Matcher> SINGLE_TRAIT_PATTERN = ThreadRef.regex(Pattern.compile(
            "(?<=^\\{|,|\\s)([a-z_]+)=([a-z0-9_]+),?(?=\\}$|,)", Pattern.CASE_INSENSITIVE));

    private static final Ref<Matcher> WILDCARD_STATE_PATTERN = ThreadRef.regex(Pattern.compile(
            "^((?:[a-z0-9_]+:)?[a-z0-9_]+)(\\{(?:[a-z_]+=(?:[a-z0-9_]+|\\*)(?:,\\s*)?)+\\})?$", Pattern.CASE_INSENSITIVE));
    private static final Ref<Matcher> WILDCARD_TRAIT_PATTERN = ThreadRef.regex(Pattern.compile(
            "(?<=^\\{|,|\\s)([a-z_]+)=([a-z0-9_]+|\\*),?(?=\\}$|,)", Pattern.CASE_INSENSITIVE));

    public static <T extends Comparable<T>> BlockTrait<T> findTrait(@NonNull BlockState state, @NonNull String traitName) {
        BlockTrait<?> trait = BlockTraits.from(traitName);
        if (trait == null) {
            //fall back to vanilla name
            trait = BlockTraits.fromVanilla(traitName);
        }
        checkArgument(trait != null, "unknown trait: \"%s\"", traitName);
        checkArgument(state.getTrait(trait) != null, "block %s doesn't contain trait \"%s\"!", state.getType(), traitName);
        return uncheckedCast(trait);
    }

    public static <T extends Comparable<T>> T parseTrait(@NonNull BlockTrait<T> trait, @NonNull String valueText) {
        for (T value : trait.getPossibleValues()) {
            if (valueText.equalsIgnoreCase(value.toString())) {
                return value;
            }
        }
        throw new IllegalArgumentException(PStrings.fastFormat("trait %s (vanilla: %s) doesn't contain value \"%s\"", trait.getName(), PorkUtil.fallbackIfNull(trait.getVanillaName(), trait.getName()), valueText));
    }

    public static BlockState applyTrait(@NonNull BlockState state, @NonNull String traitName, @NonNull String valueText) {
        BlockTrait<?> trait = findTrait(state, traitName);
        return state.withTrait(trait, uncheckedCast(parseTrait(trait, valueText)));
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
        BlockState state = BlockState.get(Identifier.fromString(idText));
        checkArgument(state != null, "unknown block: \"%s\"", idText);

        String traitsTxt = matcher.group(2);
        if (traitsTxt != null) {
            matcher = SINGLE_TRAIT_PATTERN.get().reset(traitsTxt);
            while (matcher.find()) {
                state = applyTrait(state, matcher.group(1), matcher.group(2));
            }
        }
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
        BlockState defaultState = BlockState.get(Identifier.fromString(idText));
        checkArgument(defaultState != null, "unknown block: \"%s\"", idText);
        Stream<BlockState> stream = Stream.of(defaultState);

        String traitsTxt = matcher.group(2);
        if (traitsTxt != null) {
            matcher = WILDCARD_TRAIT_PATTERN.get().reset(traitsTxt);
            while (matcher.find()) {
                String traitName = matcher.group(1);
                String valueText = matcher.group(2);
                if ("*".equals(valueText)) {
                    stream = stream.flatMap(state -> {
                        BlockTrait<?> trait = findTrait(state, traitName);
                        return trait.getPossibleValues().stream()
                                .map(value -> state.withTrait(trait, uncheckedCast(value)));
                    });
                } else {
                    stream = stream.map(state -> applyTrait(state, traitName, valueText));
                }
            }
        }
        return stream;
    }

    public static InputStream read(@NonNull String category, @NonNull Identifier id) throws IOException {
        String name = String.format("generator/%s/%s/%s.yml", category, id.getNamespace(), id.getName());

        File file = new File(name);
        if (PFiles.checkFileExists(file)) {
            return new BufferedInputStream(new FileInputStream(file));
        }

        InputStream in = null;
        switch (id.getNamespace()) {
            case "minecraft":
            case "cloudburst":
                in = Bootstrap.class.getClassLoader().getResourceAsStream(name);
                break;
            default:
                val plugin = CloudServer.getInstance().getPluginManager().getPlugin(id.getNamespace());
                if (plugin.isPresent()) {
                    in = plugin.get().getPlugin().getClass().getClassLoader().getResourceAsStream(name);
                }
        }
        if (in == null) {
            throw new FileNotFoundException(name);
        } else {
            return in;
        }
    }

    /**
     * Hashes a {@link String} to a 64-bit value.
     *
     * @param text the text to hash
     * @return the hashed value
     */
    public static long hash(@NonNull String text) {
        UUID uuid = UUID.nameUUIDFromBytes(text.getBytes(StandardCharsets.UTF_8));
        return uuid.getLeastSignificantBits() ^ uuid.getMostSignificantBits();
    }
}
