package org.cloudburstmc.server.block;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.cloudburstmc.server.block.util.BlockUtils;

import java.io.IOException;

/**
 * @author DaPorkchop_
 */
final class BlockStateDeserializer extends JsonDeserializer<BlockState> {
    @Override
    public BlockState deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        return BlockUtils.parseState(p.getText());
    }
}
