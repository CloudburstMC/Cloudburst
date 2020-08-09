package org.cloudburstmc.server.level.generator.standard.misc.filter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.cloudburstmc.server.Nukkit;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockStates;
import org.cloudburstmc.server.level.generator.standard.misc.ConstantBlock;

import java.io.IOException;

/**
 * @author DaPorkchop_
 */
final class BlockFilterDeserializer extends JsonDeserializer<BlockFilter> {
    @Override
    public BlockFilter deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        AnyOfBlockFilter filter = Nukkit.YAML_MAPPER.readValue(p, AnyOfBlockFilter.class);
        if (filter.size() == 1) {
            BlockState state = filter.iterator().next();
            return state == BlockStates.AIR ? BlockFilter.AIR : new ConstantBlock(state);
        }
        return filter;
    }
}
