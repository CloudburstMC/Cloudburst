package org.cloudburstmc.api.nbt.internal;

import org.cloudburstmc.api.nbt.NbtCodec;
import org.cloudburstmc.api.nbt.NbtKindMismatchException;
import org.cloudburstmc.api.nbt.NbtValueCodec;
import org.jetbrains.annotations.Nullable;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.ser.std.StdSerializer;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Applies {@link NbtCodec} codecs: creates and caches one instance per codec class and
 * adapts it to a Jackson serializer and deserializer.
 */
public final class NbtCodecs {

    private static final Map<Class<?>, NbtValueCodec<?>> CODECS = new ConcurrentHashMap<>();

    private NbtCodecs() {
    }

    /**
     * Returns the shared instance of a property's codec.
     *
     * @param annotation the property's annotation, if any
     * @return the codec, or {@code null} if the property has none
     * @throws IllegalArgumentException if the codec cannot be created with its no-arg constructor
     */
    @SuppressWarnings("unchecked")
    public static @Nullable NbtValueCodec<Object> codec(@Nullable NbtCodec annotation) {
        if (annotation == null) {
            return null;
        }
        return (NbtValueCodec<Object>) CODECS.computeIfAbsent(annotation.value(), NbtCodecs::create);
    }

    private static NbtValueCodec<?> create(Class<?> type) {
        try {
            Constructor<?> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return (NbtValueCodec<?>) constructor.newInstance();
        } catch (ReflectiveOperationException | RuntimeException e) {
            throw new IllegalArgumentException("Cannot create NBT codec " + type.getName()
                    + "; it needs an accessible no-arg constructor", e);
        }
    }

    /**
     * Returns a serializer that writes the codec's NBT value unchanged.
     */
    public static ValueSerializer<Object> serializer(NbtValueCodec<Object> codec) {
        return new CodecSerializer(codec);
    }

    /**
     * Returns a deserializer that hands the stored NBT value to the codec.
     *
     * @param type the property's type
     */
    public static ValueDeserializer<Object> deserializer(NbtValueCodec<Object> codec, Class<?> type) {
        return new CodecDeserializer(codec, type);
    }

    private static final class CodecSerializer extends StdSerializer<Object> {
        private final NbtValueCodec<Object> codec;

        CodecSerializer(NbtValueCodec<Object> codec) {
            super(Object.class);
            this.codec = codec;
        }

        @Override
        public void serialize(Object value, JsonGenerator g, SerializationContext ctxt) {
            Object nbt = codec.toNbt(value);
            if (nbt == null) {
                g.writeNull();
            } else {
                // The generator writes NBT values as they are and rejects anything else
                g.writeEmbeddedObject(nbt);
            }
        }
    }

    private static final class CodecDeserializer extends StdDeserializer<Object> {
        private final NbtValueCodec<Object> codec;

        CodecDeserializer(NbtValueCodec<Object> codec, Class<?> type) {
            super(type);
            this.codec = codec;
        }

        @Override
        public Object deserialize(JsonParser p, DeserializationContext ctxt) {
            if (!(p instanceof NbtParser parser)) {
                return ctxt.handleUnexpectedToken(handledType(), p);
            }
            Object tag = parser.currentTag();
            if (p.currentToken().isStructStart()) {
                // The codec takes the whole value, so the parser moves past it
                p.skipChildren();
            }
            if (tag == null) {
                return ctxt.handleUnexpectedToken(handledType(), p);
            }
            try {
                return codec.fromNbt(tag);
            } catch (NbtKindMismatchException e) {
                // A binding mismatch: the property keeps its current value. Every other
                // exception propagates and fails the read.
                MismatchedInputException mismatch = MismatchedInputException.from(p, handledType(),
                        String.format("NBT %s does not fit codec %s: %s", tag.getClass().getSimpleName(),
                                codec.getClass().getName(), e.getMessage()));
                mismatch.initCause(e);
                throw mismatch;
            }
        }
    }
}
