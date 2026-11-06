package org.cloudburstmc.api.nbt.internal;

import org.cloudburstmc.api.nbt.NbtClient;
import org.cloudburstmc.api.nbt.NbtCodec;
import org.cloudburstmc.api.nbt.NbtTag;
import org.cloudburstmc.api.nbt.NbtTagType;
import org.cloudburstmc.api.nbt.NbtValueCodec;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.jetbrains.annotations.Nullable;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.ser.ValueSerializerModifier;
import tools.jackson.databind.ser.std.StdSerializer;
import tools.jackson.databind.type.CollectionType;
import tools.jackson.databind.type.MapType;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapts Jackson serializers to {@link NbtGenerator}: bean properties announce their
 * resolved tag type before writing, non-client properties are left out of the client
 * view, {@link NbtCodec} properties are written by their codec, and {@link NbtMap} and
 * {@link NbtList} values are written unchanged.
 */
public final class NbtPropertyModifier extends ValueSerializerModifier {

    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription.Supplier beanDesc,
                                                     List<BeanPropertyWriter> beanProperties) {
        List<BeanPropertyWriter> typed = new ArrayList<>(beanProperties.size());
        for (BeanPropertyWriter writer : beanProperties) {
            NbtValueCodec<Object> codec = NbtCodecs.codec(writer.getAnnotation(NbtCodec.class));
            if (codec != null) {
                // The codec returns exact NBT values, so nothing is expected of them
                TypedPropertyWriter coded = new TypedPropertyWriter(writer, NbtTagType.AUTO, NbtTagType.AUTO);
                coded.assignSerializer(NbtCodecs.serializer(codec));
                typed.add(coded);
                continue;
            }
            NbtTypeResolver.Resolution resolution =
                    NbtTypeResolver.resolve(writer.getType(), writer.getAnnotation(NbtTag.class));
            typed.add(new TypedPropertyWriter(writer, resolution.value(),
                    resolution.element() != NbtTagType.AUTO ? resolution.element() : null));
        }
        return typed;
    }

    @Override
    public ValueSerializer<?> modifyMapSerializer(SerializationConfig config, MapType valueType,
                                                  BeanDescription.Supplier beanDesc, ValueSerializer<?> serializer) {
        return NbtMap.class.isAssignableFrom(valueType.getRawClass()) ? PassthroughSerializer.INSTANCE : serializer;
    }

    @Override
    public ValueSerializer<?> modifyCollectionSerializer(SerializationConfig config, CollectionType valueType,
                                                         BeanDescription.Supplier beanDesc,
                                                         ValueSerializer<?> serializer) {
        return NbtList.class.isAssignableFrom(valueType.getRawClass()) ? PassthroughSerializer.INSTANCE : serializer;
    }

    private static final class TypedPropertyWriter extends BeanPropertyWriter {
        @Serial
        private static final long serialVersionUID = 1L;

        private final NbtTagType value;
        private final boolean client;
        /**
         * The element type, or {@code null} until it is resolved against the serializers on
         * first use.
         */
        private volatile @Nullable NbtTagType element;

        /**
         * @param value   the tag type of the property
         * @param element the tag type of its elements, or {@code null} to resolve it on first use
         */
        TypedPropertyWriter(BeanPropertyWriter base, NbtTagType value, @Nullable NbtTagType element) {
            super(base);
            this.value = value;
            this.client = base.getAnnotation(NbtClient.class) != null;
            this.element = element;
        }

        @Override
        public void serializeAsProperty(Object bean, JsonGenerator g, SerializationContext ctxt) throws Exception {
            if (!(g instanceof NbtGenerator generator)) {
                super.serializeAsProperty(bean, g, ctxt);
                return;
            }
            if (generator.omits(client)) {
                return;
            }
            NbtTagType element = this.element;
            if (element == null) {
                this.element = element = NbtTypeResolver.elementType(getType(), ctxt);
            }
            generator.expect(value, element);
            if (client) {
                generator.approveName();
            }
            try {
                super.serializeAsProperty(bean, g, ctxt);
            } finally {
                generator.clearExpectation();
                generator.clearApproval();
            }
        }
    }

    private static final class PassthroughSerializer extends StdSerializer<Object> {
        static final PassthroughSerializer INSTANCE = new PassthroughSerializer();

        PassthroughSerializer() {
            super(Object.class);
        }

        @Override
        public void serialize(Object value, JsonGenerator g, SerializationContext ctxt) {
            g.writeEmbeddedObject(value);
        }
    }
}
