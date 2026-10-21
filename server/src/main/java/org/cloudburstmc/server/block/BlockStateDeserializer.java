package org.cloudburstmc.server.block;

import org.cloudburstmc.api.block.BlockState;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

public class BlockStateDeserializer extends ValueDeserializer<BlockState> {

    @Override
    public BlockState deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        return BlockStateParser.parse(p.getString());
    }
}
