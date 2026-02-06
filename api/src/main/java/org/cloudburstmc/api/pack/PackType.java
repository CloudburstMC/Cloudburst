package org.cloudburstmc.api.pack;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.ser.std.StdSerializer;

@JsonSerialize(using = PackType.Serializer.class)
@JsonDeserialize(using = PackType.Deserializer.class)
public enum PackType {
    INVALID,
    RESOURCES,
    DATA,
    PLUGIN,
    CLIENT_DATA,
    INTERFACE,
    MANDATORY,
    WORLD_TEMPLATE;

    static class Serializer extends StdSerializer<PackType> {

        protected Serializer() {
            super(PackType.class);
        }

        @Override
        public void serialize(PackType value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
            gen.writeString(value.name().toLowerCase());
        }
    }

    static class Deserializer extends StdDeserializer<PackType> {

        protected Deserializer() {
            super(PackType.class);
        }

        @Override
        public PackType deserialize(JsonParser p, DeserializationContext ctxt) {
            try {
                return valueOf(p.getValueAsString().toUpperCase());
            } catch (Exception e) {
                return null;
            }
        }
    }
}
