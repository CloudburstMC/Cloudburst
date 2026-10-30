package org.cloudburstmc.server.nbt;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.nbt.NbtShortBlockState;
import org.cloudburstmc.api.nbt.internal.NbtParser;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.server.block.BlockPalette;
import org.cloudburstmc.server.block.BlockStateCodec;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdSerializer;

/**
 * Maps block states to their persisted named form. Properties annotated with
 * {@link NbtShortBlockState} write the bare {@code {name, states}} compound instead;
 * every shape reads back through the same codec.
 */
public class BlockStateNbtModule extends SimpleModule {

    public BlockStateNbtModule() {
        super("cloudburst-block-state");
        addSerializer(BlockState.class, new BlockStateSerializer());
        addDeserializer(BlockState.class, new BlockStateDeserializer());
    }

    private static final class BlockStateSerializer extends StdSerializer<BlockState> {

        private final boolean shortForm;

        BlockStateSerializer() {
            this(false);
        }

        private BlockStateSerializer(boolean shortForm) {
            super(BlockState.class);
            this.shortForm = shortForm;
        }

        @Override
        public ValueSerializer<?> createContextual(SerializationContext ctxt, BeanProperty property) {
            boolean shortForm = property != null && property.getAnnotation(NbtShortBlockState.class) != null;
            return new BlockStateSerializer(shortForm);
        }

        @Override
        public void serialize(BlockState value, JsonGenerator gen, SerializationContext provider) {
            gen.writeEmbeddedObject(shortForm
                    ? BlockPalette.INSTANCE.getDefinition(value).getState()
                    : BlockPalette.INSTANCE.getSerialized(value));
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
