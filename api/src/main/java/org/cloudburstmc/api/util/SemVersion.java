package org.cloudburstmc.api.util;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.ser.std.StdSerializer;

import java.util.Arrays;

@JsonSerialize(using = SemVersion.Serializer.class)
@JsonDeserialize(using = SemVersion.Deserializer.class)
public final class SemVersion {
    private final int[] version;

    public SemVersion(int... version) {
        this.version = Arrays.copyOf(version, 3);
    }

    public int getMajor() {
        return this.version[0];
    }

    public int getMinor() {
        return this.version[1];
    }

    public int getPatch() {
        return this.version[2];
    }

    @Override
    public String toString() {
        return String.format("%d.%d.%d", version[0], version[1], version[2]);
    }

    static class Serializer extends StdSerializer<SemVersion> {

        protected Serializer() {
            super(SemVersion.class);
        }

        @Override
        public void serialize(SemVersion value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
            gen.writeArray(value.version, 0, 3);
        }
    }

    static class Deserializer extends StdDeserializer<SemVersion> {

        protected Deserializer() {
            super(SemVersion.class);
        }

        @Override
        public SemVersion deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
            int[] version = p.readValueAs(int[].class);
            return new SemVersion(version[0], version[1], version[2]);
        }
    }
}
