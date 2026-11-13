package org.cloudburstmc.server.nbt.internal;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.server.nbt.NbtTagType;
import org.cloudburstmc.server.nbt.annotation.NbtTag;
import org.jetbrains.annotations.Nullable;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.bean.BeanSerializerBase;

/**
 * Resolves property encodings from {@link NbtTag} overrides or Java declarations.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class NbtTypeResolver {

    /**
     * Resolves tag kinds for a property and its elements before serializer selection.
     *
     * @param type the declared Java type of the property
     * @param tag  the property's annotation, if any
     * @return resolved tag kinds, with {@link NbtTagType#AUTO} for deferred choices
     * @see #elementType(JavaType, SerializationContext)
     */
    public static Resolution resolve(JavaType type, @Nullable NbtTag tag) {
        NbtTagType value = tag != null && tag.value() != NbtTagType.AUTO ? tag.value() : valueType(type);
        NbtTagType element = tag != null && tag.element() != NbtTagType.AUTO ? tag.element() : elementType(type);

        return new Resolution(value, element);
    }

    /**
     * Returns the tag kind implied by the Java declaration, or {@link NbtTagType#AUTO}
     * if the serializer determines it.
     */
    private static NbtTagType valueType(JavaType type) {
        Class<?> raw = type.getRawClass();
        if (raw == boolean.class || raw == Boolean.class || raw == byte.class || raw == Byte.class) {
            return NbtTagType.BYTE;
        }

        if (raw == short.class || raw == Short.class) {
            return NbtTagType.SHORT;
        }

        if (raw == int.class || raw == Integer.class) {
            return NbtTagType.INT;
        }

        if (raw == long.class || raw == Long.class) {
            return NbtTagType.LONG;
        }

        if (raw == float.class || raw == Float.class) {
            return NbtTagType.FLOAT;
        }

        if (raw == double.class || raw == Double.class) {
            return NbtTagType.DOUBLE;
        }

        if (raw == String.class) {
            return NbtTagType.STRING;
        }

        if (raw == byte[].class) {
            return NbtTagType.BYTE_ARRAY;
        }

        if (raw == int[].class) {
            return NbtTagType.INT_ARRAY;
        }

        if (raw == long[].class) {
            return NbtTagType.LONG_ARRAY;
        }

        if (isPassthrough(raw)) {
            return NbtTagType.AUTO;
        }

        if (type.isCollectionLikeType() || type.isArrayType()) {
            return NbtTagType.LIST;
        }

        if (type.isMapLikeType()) {
            return NbtTagType.COMPOUND;
        }

        return NbtTagType.AUTO;
    }

    /**
     * Resolves the tag kind for list elements, array elements, or map values.
     */
    private static NbtTagType elementType(JavaType type) {
        JavaType content = elementDeclaration(type);
        return content == null ? NbtTagType.AUTO : valueType(content);
    }

    /**
     * Uses the serializer to resolve ambiguous element encodings. Lists of beans retain
     * the compound element kind even when empty.
     */
    public static NbtTagType elementType(JavaType type, SerializationContext ctxt) {
        JavaType content = elementDeclaration(type);
        if (content == null || content.hasRawClass(Object.class)) {
            return NbtTagType.AUTO;
        }

        NbtTagType element = valueType(content);
        if (element != NbtTagType.AUTO) {
            return element;
        }

        return ctxt.findValueSerializer(content) instanceof BeanSerializerBase
                ? NbtTagType.COMPOUND
                : NbtTagType.AUTO;
    }

    private static @Nullable JavaType elementDeclaration(JavaType type) {
        if (isPassthrough(type.getRawClass())) {
            return null;
        }

        return type.isCollectionLikeType() || type.isArrayType() || type.isMapLikeType()
                ? type.getContentType()
                : null;
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
