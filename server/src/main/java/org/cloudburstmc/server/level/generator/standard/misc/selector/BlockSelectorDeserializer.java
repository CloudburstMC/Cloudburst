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
        BlockSelector selector = Nukkit.YAML_MAPPER.readValue(p, MultiBlockSelector.class);
        if (selector.size() == 1)   {
            return new ConstantBlock(selector.get(0));
        }
        return selector;
    }
}
