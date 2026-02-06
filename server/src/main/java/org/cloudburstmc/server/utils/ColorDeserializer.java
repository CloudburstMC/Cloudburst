package org.cloudburstmc.server.utils;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

import java.awt.*;

public final class ColorDeserializer extends ValueDeserializer<Color> {

    @Override
    public Color deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        int r = 0, g = 0, b = 0, a = 255;
        while (p.nextToken() != JsonToken.END_OBJECT) {
            String name = p.currentName();
            p.nextToken();
            switch (name) {
                case "r" -> r = p.getIntValue();
                case "g" -> g = p.getIntValue();
                case "b" -> b = p.getIntValue();
                case "a" -> a = p.getIntValue();
            }
        }
        return new Color(r, g, b, a);
    }
}
