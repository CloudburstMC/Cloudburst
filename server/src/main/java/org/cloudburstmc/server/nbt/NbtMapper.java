package org.cloudburstmc.server.nbt;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.server.nbt.annotation.NbtCodec;
import org.cloudburstmc.server.nbt.annotation.NbtTag;
import org.cloudburstmc.server.nbt.internal.NbtDeserializerModifier;
import org.cloudburstmc.server.nbt.internal.NbtGenerator;
import org.cloudburstmc.server.nbt.internal.NbtParser;
import org.cloudburstmc.server.nbt.internal.NbtSerializerModifier;
import tools.jackson.databind.*;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.introspect.BeanPropertyDefinition;
import tools.jackson.databind.introspect.ClassIntrospector;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.type.LogicalType;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maps typed objects to and from NBT compounds.
 * <p>
 * Writing infers tag types from Java property types unless overridden by {@link NbtTag}.
 * Null properties are omitted. Properties annotated with {@link NbtCodec} use that codec
 * in both directions. {@link NbtMap} and {@link NbtList} values pass through unchanged.
 */
public class NbtMapper {

    private final ObjectMapper mapper;
    private final Map<Class<?>, Set<String>> ownedKeys = new ConcurrentHashMap<>();

    private NbtMapper(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Creates a mapper.
     *
     * @param modules additional Jackson modules for property types
     * @return the configured mapper
     */
    public static NbtMapper create(JacksonModule... modules) {
        SimpleModule nbt = new SimpleModule("cloudburst-nbt");
        nbt.setSerializerModifier(new NbtSerializerModifier());
        nbt.setDeserializerModifier(new NbtDeserializerModifier());

        ObjectMapper mapper = JsonMapper.builder()
                .addModule(nbt)
                .addModules(modules)
                .changeDefaultPropertyInclusion(inclusion -> inclusion
                        .withValueInclusion(JsonInclude.Include.NON_NULL)
                        .withContentInclusion(JsonInclude.Include.NON_NULL))
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                // Numbers bind across widths and a byte binds to a boolean, but strings
                // and numbers never stand in for each other
                .withCoercionConfig(LogicalType.Textual, config -> config
                        .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail))
                .withCoercionConfig(LogicalType.Integer, config -> config
                        .setCoercion(CoercionInputShape.Float, CoercionAction.TryConvert)
                        .setCoercion(CoercionInputShape.String, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.EmptyString, CoercionAction.Fail))
                .withCoercionConfig(LogicalType.Float, config -> config
                        .setCoercion(CoercionInputShape.Integer, CoercionAction.TryConvert)
                        .setCoercion(CoercionInputShape.String, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.EmptyString, CoercionAction.Fail))
                .withCoercionConfig(LogicalType.Boolean, config -> config
                        .setCoercion(CoercionInputShape.Integer, CoercionAction.TryConvert)
                        .setCoercion(CoercionInputShape.String, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.EmptyString, CoercionAction.Fail))
                .build();
        return new NbtMapper(mapper);
    }

    /**
     * Writes an object as a compound tag.
     *
     * @param value a value that serializes as a compound
     * @return the written compound
     * @throws IllegalArgumentException if the value does not serialize as an object
     * @throws tools.jackson.core.JacksonException if a property cannot be written as its NBT type
     */
    public NbtMap write(Object value) {
        return this.write(value, null);
    }

    /**
     * Writes a compound restricted to root properties carrying the given annotation.
     * Selected properties retain their complete values.
     *
     * @param value object to write
     * @param rootPropertyMarker required annotation, or {@code null} to write all properties
     * @return the written compound
     */
    public NbtMap write(Object value, @Nullable Class<? extends Annotation> rootPropertyMarker) {
        Objects.requireNonNull(value, "value");

        try (NbtGenerator generator = new NbtGenerator(rootPropertyMarker)) {
            this.mapper.writeValue(generator, value);

            if (!(generator.result() instanceof NbtMap tag)) {
                throw new IllegalArgumentException(value.getClass().getName() + " is not written as a compound");
            }
            return tag;
        }
    }

    /**
     * Reads a compound into a new object, preserving defaults for missing or incompatible tags.
     * See {@link #readInto} for how tags are applied.
     *
     * @param tag  the compound to read
     * @param type the type to create
     * @param <T> the object type
     * @return the decoded object
     */
    public <T> T read(NbtMap tag, Class<T> type) {
        Objects.requireNonNull(tag, "tag");
        Objects.requireNonNull(type, "type");

        T target;
        try (NbtParser parser = new NbtParser(NbtMap.EMPTY)) {
            target = this.mapper.readValue(parser, type);
        }

        return this.readInto(tag, target);
    }

    /**
     * Reads a compound tag into an existing object.
     * <p>
     * Unknown keys are ignored. Missing or incompatible tags leave existing values unchanged.
     * Incompatible tags are logged. Numeric tags may be converted to other numeric types,
     * and a byte tag may be read as a boolean.
     * <p>
     * Existing property values are updated in place when their deserializer supports it.
     *
     * @param tag    the compound to read
     * @param target the object to update
     * @param <T> the object type
     * @return the updated object, normally {@code target}
     */
    public <T> T readInto(NbtMap tag, T target) {
        Objects.requireNonNull(tag, "tag");
        Objects.requireNonNull(target, "target");

        try (NbtParser parser = new NbtParser(tag)) {
            return this.mapper.readerForUpdating(target).readValue(parser);
        }
    }

    /**
     * Returns the NBT keys of the type's writable properties, including inherited properties.
     *
     * @param type the data type
     * @return the immutable set of keys
     */
    public Set<String> ownedKeys(Class<?> type) {
        Objects.requireNonNull(type, "type");
        return this.ownedKeys.computeIfAbsent(type, this::findOwnedKeys);
    }

    private Set<String> findOwnedKeys(Class<?> type) {
        SerializationConfig config = this.mapper.serializationConfig();
        ClassIntrospector introspector = config.classIntrospectorInstance();
        JavaType javaType = config.constructType(type);
        BeanDescription description = introspector.introspectForSerialization(
                javaType,
                introspector.introspectClassAnnotations(javaType)
        );

        Set<String> keys = new LinkedHashSet<>();
        for (BeanPropertyDefinition property : description.findProperties()) {
            if (property.couldSerialize()) {
                keys.add(property.getName());
            }
        }
        return Collections.unmodifiableSet(keys);
    }
}
