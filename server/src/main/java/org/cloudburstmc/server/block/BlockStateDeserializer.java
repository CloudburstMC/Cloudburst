package org.cloudburstmc.server.block;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.server.block.util.BlockUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

public final class BlockStateDeserializer extends ValueDeserializer<BlockState> {
    @Override
    public BlockState deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        return BlockUtils.parseState(p.getString());
    }
}
