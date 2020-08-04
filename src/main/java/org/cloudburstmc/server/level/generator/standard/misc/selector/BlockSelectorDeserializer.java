package org.cloudburstmc.server.level.generator.standard.misc.selector;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.lib.common.ref.Ref;
import net.daporkchop.lib.common.ref.ThreadRef;
import net.daporkchop.lib.common.util.PValidation;
import org.cloudburstmc.server.Nukkit;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.level.generator.standard.StandardGeneratorUtils;
import org.cloudburstmc.server.level.generator.standard.misc.ConstantBlock;

import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * @author DaPorkchop_
 */
final class BlockSelectorDeserializer extends JsonDeserializer<BlockSelector> {
    @Override
    public BlockSelector deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        return Nukkit.YAML_MAPPER.readValue(p, TempBlockSelector.class).toActualSelector();
    }

    @AllArgsConstructor(onConstructor_ = {@JsonCreator})
    @JsonDeserialize
    private static final class TempBlockSelector {
        @NonNull
        private final SelectionEntry[] entries;

        @JsonCreator
        public TempBlockSelector(String value) {
            this.entries = Arrays.stream(value.split(","))
                    .map(SelectionEntry::new)
                    .toArray(SelectionEntry[]::new);
        }

        public BlockSelector toActualSelector() {
            Preconditions.checkState(this.entries.length > 0, "block selector must have at least 1 entry");
            if (this.entries.length == 1) {
                return new ConstantBlock(this.entries[0].state);
            } else {
                return new MultiBlockSelector(Arrays.stream(this.entries)
                        .flatMap(entry -> entry.weight == 1 ? Stream.of(entry) : IntStream.range(0, entry.weight).mapToObj(i -> entry))
                        .map(SelectionEntry::getState)
                        .toArray(BlockState[]::new));
            }
        }
    }

    @JsonDeserialize
    @Getter
    private static final class SelectionEntry {
        private static final Ref<Matcher> ENTRY_MATCHER_CACHE = ThreadRef.regex(Pattern.compile("^(?:(\\d+)\\*)(.+)$"));

        private final BlockState state;
        private final int weight;

        @JsonCreator
        public SelectionEntry(
                @JsonProperty(value = "block", required = true) ConstantBlock block,
                @JsonProperty(value = "weight", required = true) int weight) {
            this.state = block.state();
            this.weight = PValidation.ensurePositive(weight);
        }

        public SelectionEntry(ConstantBlock block) {
            this.state = block.state();
            this.weight = 1;
        }

        @JsonCreator
        public SelectionEntry(String value) {
            Matcher matcher = ENTRY_MATCHER_CACHE.get().reset(value);

            this.state = StandardGeneratorUtils.parseState(matcher.group(2));
            this.weight = matcher.group(1) == null ? 1 : PValidation.ensurePositive(Integer.parseUnsignedInt(matcher.group(1)));
        }
    }
}
