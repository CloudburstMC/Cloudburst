package org.cloudburstmc.server.block;

import org.cloudburstmc.nbt.NBTInputStream;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtUtils;
import org.cloudburstmc.protocol.bedrock.data.definitions.BlockDefinition;
import org.cloudburstmc.protocol.bedrock.data.definitions.SimpleBlockDefinition;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;

import java.io.ByteArrayInputStream;
import java.util.Base64;

public class BlockDefinitionDeserializer extends StdDeserializer<BlockDefinition> {

    public BlockDefinitionDeserializer() {
        super(BlockDefinition.class);
    }

    @Override
    public BlockDefinition deserialize(JsonParser p, DeserializationContext ctxt) {
        try {
            if (p.currentToken() != JsonToken.START_OBJECT) {
                ctxt.reportWrongTokenException(this, JsonToken.START_OBJECT, "Expected START_OBJECT for BlockDefinition");
            }

            String b64 = null;
            while (p.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = p.currentName();
                p.nextToken();
                if ("block_state_b64".equals(fieldName)) {
                    b64 = p.getString();
                } else {
                    p.skipChildren();
                }
            }

            if (b64 == null) {
                return null;
            }

            byte[] nbtBytes = Base64.getDecoder().decode(b64);
            NbtMap nbtMap;
            try (NBTInputStream reader = NbtUtils.createReaderLE(new ByteArrayInputStream(nbtBytes))) {
                nbtMap = (NbtMap) reader.readTag();
            }

            String name = nbtMap.getString("name");
            int runtimeId = nbtMap.getInt("network_id", 0);

            return new SimpleBlockDefinition(name, runtimeId, nbtMap);
        } catch (Exception e) {
            ctxt.reportBadDefinition(BlockDefinition.class, "Failed to deserialize BlockDefinition: " + e.getMessage());
            return null;
        }
    }

    @Override
    public BlockDefinition getNullValue(DeserializationContext ctxt) {
        return null;
    }
}
