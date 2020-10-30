package org.cloudburstmc.server.level.generator.standard.misc.selector;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import net.daporkchop.lib.common.ref.Ref;
import net.daporkchop.lib.common.ref.ThreadRef;
import net.daporkchop.lib.common.util.PValidation;
import org.cloudburstmc.server.Bootstrap;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.util.BlockUtils;
import org.cloudburstmc.server.level.generator.standard.misc.ConstantBlock;

import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * @author DaPorkchop_
 */
final class BlockSelectorDeserializer extends JsonDeserializer<BlockSelector> {
    @Override
    public BlockSelector deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        BlockSelector.Entry[] entries = Arrays.stream(Bootstrap.YAML_MAPPER.readValue(p, String[].class))
                .flatMap(value -> Arrays.stream(value.split(",")))
                .map(TempEntry::new)
                .flatMap(TempEntry::flatten)
                .toArray(BlockSelector.Entry[]::new);
        if (entries.length == 1) {
            return new ConstantBlock(entries[0].state());
        }
        return new MultiBlockSelector(entries);
    }

    @JsonDeserialize
    private static final class TempEntry {
        private static final Ref<Matcher> ENTRY_MATCHER_CACHE = ThreadRef.regex(Pattern.compile("^(?:(\\d+)\\*)?(.+)$"));

        private final BlockState[] states;
        private final int weight;

        @JsonCreator
        public TempEntry(String value) {
            Matcher matcher = ENTRY_MATCHER_CACHE.get().reset(value);

            Preconditions.checkArgument(matcher.find(), "invalid input: \"%s\"", value);

            this.states = BlockUtils.parseStateWildcard(matcher.group(2)).toArray(BlockState[]::new);
            this.weight = matcher.group(1) == null ? 1 : PValidation.positive(Integer.parseUnsignedInt(matcher.group(1)));
        }

        public Stream<BlockSelector.Entry> flatten() {
            return Arrays.stream(this.states).map(state -> new MultiBlockSelector.SelectionEntry(state, this.weight));
        }
    }
}
