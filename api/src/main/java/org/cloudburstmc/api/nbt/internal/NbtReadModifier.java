package org.cloudburstmc.api.nbt.internal;

import org.cloudburstmc.api.nbt.NbtCodec;
import org.cloudburstmc.api.nbt.NbtKindMismatchException;
import org.cloudburstmc.api.nbt.NbtValueCodec;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.BeanDeserializerBuilder;
import tools.jackson.databind.deser.SettableBeanProperty;
import tools.jackson.databind.deser.ValueDeserializerModifier;
import tools.jackson.databind.deser.bean.BeanDeserializerBase;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.introspect.AnnotatedMember;
import tools.jackson.databind.introspect.BeanPropertyDefinition;
import tools.jackson.databind.type.CollectionType;
import tools.jackson.databind.type.MapType;

import java.io.Serial;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Adapts Jackson deserializers to {@link NbtParser}:
 * <ul>
 * <li>a bean property whose tag cannot be bound keeps its current value instead of
 * failing the read, including when its codec throws {@link NbtKindMismatchException};</li>
 * <li>a bean property whose current value can be updated is filled in place;</li>
 * <li>an {@link NbtCodec} property is read by its codec;</li>
 * <li>{@link NbtMap} and {@link NbtList} properties take the tag unchanged.</li>
 * </ul>
 */
public final class NbtReadModifier extends ValueDeserializerModifier {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(NbtReadModifier.class);

    @Override
    public BeanDeserializerBuilder updateBuilder(DeserializationConfig config, BeanDescription.Supplier beanDesc,
                                                 BeanDeserializerBuilder builder) {
        List<SettableBeanProperty> properties = new ArrayList<>();
        for (Iterator<SettableBeanProperty> it = builder.getProperties(); it.hasNext(); ) {
            SettableBeanProperty property = it.next();
            if (!property.isCreatorProperty() && !(property instanceof LenientProperty)) {
                properties.add(property);
            }
        }
        if (properties.isEmpty()) {
            return builder;
        }
        Map<String, AnnotatedMember> accessors = accessors(config, beanDesc);
        for (SettableBeanProperty property : properties) {
            NbtValueCodec<Object> codec = NbtCodecs.codec(property.getAnnotation(NbtCodec.class));
            if (codec != null) {
                property = property.withValueDeserializer(
                        NbtCodecs.deserializer(codec, property.getType().getRawClass()));
            }
            builder.addOrReplaceProperty(new LenientProperty(property, accessors.get(property.getName())), true);
        }
        return builder;
    }

    /**
     * Finds the member that reads each property's current value, keyed by property name.
     */
    private static Map<String, AnnotatedMember> accessors(DeserializationConfig config,
                                                          BeanDescription.Supplier beanDesc) {
        Map<String, AnnotatedMember> accessors = new HashMap<>();
        for (BeanPropertyDefinition definition : beanDesc.get().findProperties()) {
            AnnotatedMember accessor = definition.getAccessor();
            if (accessor != null) {
                if (config.canOverrideAccessModifiers()) {
                    accessor.fixAccess(config.isEnabled(MapperFeature.OVERRIDE_PUBLIC_ACCESS_MODIFIERS));
                }
                accessors.put(definition.getName(), accessor);
            }
        }
        return accessors;
    }

    @Override
    public ValueDeserializer<?> modifyMapDeserializer(DeserializationConfig config, MapType type,
                                                      BeanDescription.Supplier beanDesc,
                                                      ValueDeserializer<?> deserializer) {
        return NbtMap.class.isAssignableFrom(type.getRawClass()) ? new PassthroughDeserializer(NbtMap.class)
                : deserializer;
    }

    @Override
    public ValueDeserializer<?> modifyCollectionDeserializer(DeserializationConfig config, CollectionType type,
                                                             BeanDescription.Supplier beanDesc,
                                                             ValueDeserializer<?> deserializer) {
        return NbtList.class.isAssignableFrom(type.getRawClass()) ? new PassthroughDeserializer(NbtList.class)
                : deserializer;
    }

    /**
     * Skips the rest of a value whose binding failed, leaving the parser on its last token.
     *
     * @param start the value's first token
     * @param depth the nesting depth of the read context at that token
     */
    private static void skipValue(JsonParser p, JsonToken start, int depth) {
        if (!start.isStructStart()) {
            return;
        }
        // A struct start opens a context one level deeper; its end returns to the level before
        int outside = depth - 1;
        JsonToken token = p.currentToken();
        while (token != null) {
            if (token.isStructStart()) {
                p.skipChildren();
                token = p.currentToken();
            }
            if (token.isStructEnd() && p.streamReadContext().getNestingDepth() == outside) {
                return;
            }
            token = p.nextToken();
        }
    }

    /**
     * Binds one property leniently, filling its current value in place where it can.
     */
    private static final class LenientProperty extends SettableBeanProperty.Delegating {
        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * Reads the property's current value, or {@code null} if it has no readable member.
         */
        private final @Nullable AnnotatedMember accessor;

        LenientProperty(SettableBeanProperty delegate, @Nullable AnnotatedMember accessor) {
            super(delegate);
            this.accessor = accessor;
        }

        @Override
        protected SettableBeanProperty withDelegate(SettableBeanProperty d) {
            return new LenientProperty(d, accessor);
        }

        @Override
        public void deserializeAndSet(JsonParser p, DeserializationContext ctxt, Object instance) {
            JsonToken start = p.currentToken();
            int depth = p.streamReadContext().getNestingDepth();
            try {
                Object current = updatable(ctxt, instance);
                if (current == null) {
                    delegate.deserializeAndSet(p, ctxt, instance);
                } else {
                    Object value = delegate.deserializeWith(p, ctxt, current);
                    if (value != current) {
                        delegate.set(ctxt, instance, value);
                    }
                }
            } catch (InvalidDefinitionException e) {
                throw e;
            } catch (JacksonException e) {
                if (hasNonJacksonCause(e)) {
                    throw e;
                }
                skipped(p, start, depth, e);
            }
        }

        @Override
        public Object deserializeSetAndReturn(JsonParser p, DeserializationContext ctxt, Object instance) {
            JsonToken start = p.currentToken();
            int depth = p.streamReadContext().getNestingDepth();
            try {
                Object current = updatable(ctxt, instance);
                if (current == null) {
                    return delegate.deserializeSetAndReturn(p, ctxt, instance);
                }
                Object value = delegate.deserializeWith(p, ctxt, current);
                return value != current ? delegate.setAndReturn(ctxt, instance, value) : instance;
            } catch (InvalidDefinitionException e) {
                throw e;
            } catch (JacksonException e) {
                if (hasNonJacksonCause(e)) {
                    throw e;
                }
                skipped(p, start, depth, e);
                return instance;
            }
        }

        /**
         * Returns the property's current value if its deserializer fills it in place.
         * <p>
         * Only a deserializer that declares update support and is not one of Jackson's own
         * container or bean deserializers qualifies: those would append to a list or merge
         * into a nested object, while a present key replaces the whole value of such a
         * property.
         *
         * @return the value to fill, or {@code null} to set a newly read value
         */
        private @Nullable Object updatable(DeserializationContext ctxt, Object instance) {
            if (accessor == null || delegate.hasValueTypeDeserializer() || getType().isContainerType()) {
                return null;
            }
            ValueDeserializer<Object> deserializer = delegate.getValueDeserializer();
            if (deserializer instanceof BeanDeserializerBase
                    || !Boolean.TRUE.equals(deserializer.supportsUpdate(ctxt.getConfig()))) {
                return null;
            }
            return accessor.getValue(instance);
        }

        /**
         * Whether a binding failure was caused by something other than the tag's
         * shape. A non-Jackson exception in the cause chain means element or
         * property code rejected the value, so the read must fail instead of
         * silently keeping the current value. The one exception is
         * {@link NbtKindMismatchException}, which code throws to report a shape
         * mismatch itself.
         *
         * @param e the binding failure
         * @return true when user code caused the failure
         */
        private static boolean hasNonJacksonCause(JacksonException e) {
            Throwable cause = e.getCause();
            while (cause != null) {
                if (cause instanceof NbtKindMismatchException) {
                    // A codec reporting a value of the wrong kind is a shape mismatch too
                    return false;
                }
                if (!(cause instanceof JacksonException)) {
                    return true;
                }
                cause = cause.getCause();
            }
            return false;
        }

        private void skipped(JsonParser p, JsonToken start, int depth, JacksonException e) {
            if (e.getCause() instanceof NbtKindMismatchException) {
                // Logged with the codec's exception, so its own cause is kept
                log.warn("NBT key {} does not fit {}, keeping its current value: {}",
                        getName(), getDeclaringClass().getName(), e.getOriginalMessage(), e.getCause());
            } else {
                log.warn("NBT key {} does not fit {}, keeping its current value: {}",
                        getName(), getDeclaringClass().getName(), e.getOriginalMessage());
            }
            skipValue(p, start, depth);
        }
    }

    private static final class PassthroughDeserializer extends StdDeserializer<Object> {
        PassthroughDeserializer(Class<?> type) {
            super(type);
        }

        @Override
        public Object deserialize(JsonParser p, DeserializationContext ctxt) {
            if (p instanceof NbtParser parser && p.currentToken().isStructStart()
                    && handledType().isInstance(parser.currentTag())) {
                Object tag = parser.currentTag();
                p.skipChildren();
                return tag;
            }
            return ctxt.handleUnexpectedToken(handledType(), p);
        }
    }
}
