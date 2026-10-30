package org.cloudburstmc.api.nbt.internal;

import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.BeanDeserializerBuilder;
import tools.jackson.databind.deser.SettableBeanProperty;
import tools.jackson.databind.deser.ValueDeserializerModifier;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.type.CollectionType;
import tools.jackson.databind.type.MapType;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Adapts Jackson deserializers to {@link NbtParser}: a bean property whose tag cannot be
 * bound keeps its default instead of failing the read, and {@link NbtMap} and
 * {@link NbtList} properties take the tag unchanged.
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
        for (SettableBeanProperty property : properties) {
            builder.addOrReplaceProperty(new LenientProperty(property), true);
        }
        return builder;
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

    private static final class LenientProperty extends SettableBeanProperty.Delegating {
        @Serial
        private static final long serialVersionUID = 1L;

        LenientProperty(SettableBeanProperty delegate) {
            super(delegate);
        }

        @Override
        protected SettableBeanProperty withDelegate(SettableBeanProperty d) {
            return new LenientProperty(d);
        }

        @Override
        public void deserializeAndSet(JsonParser p, DeserializationContext ctxt, Object instance) {
            JsonToken start = p.currentToken();
            int depth = p.streamReadContext().getNestingDepth();
            try {
                delegate.deserializeAndSet(p, ctxt, instance);
            } catch (InvalidDefinitionException e) {
                throw e;
            } catch (JacksonException e) {
                skipped(p, start, depth, e);
            }
        }

        @Override
        public Object deserializeSetAndReturn(JsonParser p, DeserializationContext ctxt, Object instance) {
            JsonToken start = p.currentToken();
            int depth = p.streamReadContext().getNestingDepth();
            try {
                return delegate.deserializeSetAndReturn(p, ctxt, instance);
            } catch (InvalidDefinitionException e) {
                throw e;
            } catch (JacksonException e) {
                skipped(p, start, depth, e);
                return instance;
            }
        }

        private void skipped(JsonParser p, JsonToken start, int depth, JacksonException e) {
            log.debug("NBT key {} does not fit {}, keeping its default: {}",
                    getName(), getDeclaringClass().getName(), e.getOriginalMessage());
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
