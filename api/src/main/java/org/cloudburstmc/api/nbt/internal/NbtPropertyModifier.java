package org.cloudburstmc.api.nbt.internal;

import org.cloudburstmc.api.nbt.NbtTag;
import org.cloudburstmc.api.nbt.NbtTagType;
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
 * resolved tag type before writing, and {@link NbtMap} and {@link NbtList} values are
 * written unchanged.
 */
public final class NbtPropertyModifier extends ValueSerializerModifier {

    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription.Supplier beanDesc,
                                                     List<BeanPropertyWriter> beanProperties) {
        List<BeanPropertyWriter> typed = new ArrayList<>(beanProperties.size());
        for (BeanPropertyWriter writer : beanProperties) {
            typed.add(new TypedPropertyWriter(writer,
                    NbtTypeResolver.resolve(writer.getType(), writer.getAnnotation(NbtTag.class))));
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
        /**
         * The element type, or {@code null} until it is resolved against the serializers on
         * first use.
         */
        private volatile @Nullable NbtTagType element;

        TypedPropertyWriter(BeanPropertyWriter base, NbtTypeResolver.Resolution resolution) {
            super(base);
            this.value = resolution.value();
            this.element = resolution.element() != NbtTagType.AUTO ? resolution.element() : null;
        }

        @Override
        public void serializeAsProperty(Object bean, JsonGenerator g, SerializationContext ctxt) throws Exception {
            if (!(g instanceof NbtGenerator generator)) {
                super.serializeAsProperty(bean, g, ctxt);
                return;
            }
            NbtTagType element = this.element;
            if (element == null) {
                this.element = element = NbtTypeResolver.elementType(getType(), ctxt);
            }
            generator.expect(value, element);
            try {
                super.serializeAsProperty(bean, g, ctxt);
            } finally {
                generator.clearExpectation();
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
