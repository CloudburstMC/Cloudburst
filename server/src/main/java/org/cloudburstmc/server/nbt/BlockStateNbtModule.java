package org.cloudburstmc.server.nbt;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.nbt.internal.NbtParser;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.server.block.BlockStateCodec;
import org.cloudburstmc.server.block.BlockPalette;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdSerializer;

/**
 * Maps block states to their persisted named form.
 */
public class BlockStateNbtModule extends SimpleModule {

    public BlockStateNbtModule() {
        super("cloudburst-block-state");
        addSerializer(BlockState.class, new BlockStateSerializer());
        addDeserializer(BlockState.class, new BlockStateDeserializer());
    }

    private static final class BlockStateSerializer extends StdSerializer<BlockState> {

        BlockStateSerializer() {
            super(BlockState.class);
        }

        @Override
        public void serialize(BlockState value, JsonGenerator gen, SerializationContext provider) {
            gen.writeEmbeddedObject(BlockPalette.INSTANCE.getSerialized(value));
        }
    }

    private static final class BlockStateDeserializer extends StdDeserializer<BlockState> {

        BlockStateDeserializer() {
            super(BlockState.class);
        }

        @Override
        public BlockState deserialize(JsonParser p, DeserializationContext ctxt) {
            if (p instanceof NbtParser parser && parser.currentTag() instanceof NbtMap tag) {
                p.skipChildren();
                return BlockStateCodec.read(tag);
            }
            return BlockStateCodec.read(ctxt.readValue(p, NbtMap.class));
        }
    }
}
