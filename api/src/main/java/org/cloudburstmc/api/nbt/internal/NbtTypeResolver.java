package org.cloudburstmc.api.nbt.internal;

import org.cloudburstmc.api.nbt.NbtTag;
import org.cloudburstmc.api.nbt.NbtTagType;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.jetbrains.annotations.Nullable;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.bean.BeanSerializerBase;

/**
 * Resolves the NBT type of a property from its {@link NbtTag} annotation, falling back to
 * its Java type.
 */
public final class NbtTypeResolver {

    private NbtTypeResolver() {
    }

    /**
     * Resolves the tag type of a property and of its elements from the annotation and the
     * Java type alone.
     *
     * @param type the declared Java type of the property
     * @param tag  the property's annotation, if any
     * @return the resolved types; {@link NbtTagType#AUTO} where the Java type alone does not decide
     * @see #elementType(JavaType, SerializationContext)
     */
    public static Resolution resolve(JavaType type, @Nullable NbtTag tag) {
        NbtTagType value = tag != null && tag.value() != NbtTagType.AUTO ? tag.value() : valueType(type);
        NbtTagType element = tag != null && tag.element() != NbtTagType.AUTO ? tag.element() : elementType(type);
        return new Resolution(value, element);
    }

    /**
     * Maps a Java type to the tag type it is written as. Types without a fixed mapping,
     * such as nested objects, enums and values with custom serializers, resolve to
     * {@link NbtTagType#AUTO} so the written value decides. A nested data object is then
     * written as a compound.
     */
    public static NbtTagType valueType(JavaType type) {
        Class<?> raw = type.getRawClass();
        if (raw == boolean.class || raw == Boolean.class || raw == byte.class || raw == Byte.class) {
            return NbtTagType.BYTE;
        } else if (raw == short.class || raw == Short.class) {
            return NbtTagType.SHORT;
        } else if (raw == int.class || raw == Integer.class) {
            return NbtTagType.INT;
        } else if (raw == long.class || raw == Long.class) {
            return NbtTagType.LONG;
        } else if (raw == float.class || raw == Float.class) {
            return NbtTagType.FLOAT;
        } else if (raw == double.class || raw == Double.class) {
            return NbtTagType.DOUBLE;
        } else if (raw == String.class) {
            return NbtTagType.STRING;
        } else if (raw == byte[].class) {
            return NbtTagType.BYTE_ARRAY;
        } else if (raw == int[].class) {
            return NbtTagType.INT_ARRAY;
        } else if (raw == long[].class) {
            return NbtTagType.LONG_ARRAY;
        } else if (isPassthrough(raw)) {
            return NbtTagType.AUTO;
        } else if (type.isCollectionLikeType() || type.isArrayType()) {
            return NbtTagType.LIST;
        } else if (type.isMapLikeType()) {
            return NbtTagType.COMPOUND;
        }
        return NbtTagType.AUTO;
    }

    /**
     * Maps the element type of a list or array, or the value type of a map, to its tag type.
     */
    public static NbtTagType elementType(JavaType type) {
        if (isPassthrough(type.getRawClass()) || type.getContentType() == null) {
            return NbtTagType.AUTO;
        }
        if (type.isCollectionLikeType() || type.isArrayType() || type.isMapLikeType()) {
            return valueType(type.getContentType());
        }
        return NbtTagType.AUTO;
    }

    /**
     * Resolves the element type of a list, array or map once serializers are known. Where
     * the Java type gives no fixed mapping, elements that serialize as plain data objects
     * resolve to {@link NbtTagType#COMPOUND}, so an empty list of them keeps that type.
     */
    public static NbtTagType elementType(JavaType type, SerializationContext ctxt) {
        NbtTagType element = elementType(type);
        if (element != NbtTagType.AUTO || isPassthrough(type.getRawClass())
                || !(type.isCollectionLikeType() || type.isArrayType() || type.isMapLikeType())) {
            return element;
        }
        JavaType content = type.getContentType();
        if (content == null || content.hasRawClass(Object.class)) {
            return NbtTagType.AUTO;
        }
        return ctxt.findValueSerializer(content) instanceof BeanSerializerBase
                ? NbtTagType.COMPOUND : NbtTagType.AUTO;
    }

    private static boolean isPassthrough(Class<?> raw) {
        return NbtMap.class.isAssignableFrom(raw) || NbtList.class.isAssignableFrom(raw);
    }

    /**
     * The resolved tag types of a property.
     *
     * @param value   the type of the property itself
     * @param element the type of its list elements or map values
     */
    public record Resolution(NbtTagType value, NbtTagType element) {
    }
}
