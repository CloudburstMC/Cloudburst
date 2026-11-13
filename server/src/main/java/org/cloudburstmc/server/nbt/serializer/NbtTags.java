package org.cloudburstmc.server.nbt.serializer;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.server.nbt.internal.NbtParser;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;

import java.util.List;

/**
 * Helpers shared by the server NBT serializers.
 */
@UtilityClass
public class NbtTags {

    public static String describe(Object tag) {
        return switch (tag) {
            case List<?> list -> "a list of " + list.size() + " values";
            case NbtMap ignored -> "a compound";
            case Number ignored -> "a number";
            case String value -> "\"" + value + "\"";
            case null -> "nothing";
            default -> "an NBT " + tag.getClass().getSimpleName();
        };
    }

    public static NbtMap readTag(JsonParser p, DeserializationContext ctxt) {
        if (p instanceof NbtParser parser && parser.currentTag() instanceof NbtMap tag) {
            p.skipChildren();
            return tag;
        }
        return ctxt.readValue(p, NbtMap.class);
    }
}
