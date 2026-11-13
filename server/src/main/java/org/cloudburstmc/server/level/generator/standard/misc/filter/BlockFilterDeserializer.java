package org.cloudburstmc.server.level.generator.standard.misc.filter;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.server.level.generator.standard.misc.ConstantBlock;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

/**
 * @author DaPorkchop_
 */
final class BlockFilterDeserializer extends ValueDeserializer<BlockFilter> {
    @Override
    public BlockFilter deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        AnyOfBlockFilter filter = ctxt.readValue(p, AnyOfBlockFilter.class);
        if (filter.size() == 1) {
            BlockState state = filter.iterator().next();
            return state == BlockStates.AIR ? BlockFilter.AIR : new ConstantBlock(state);
        }
        return filter;
    }
}
