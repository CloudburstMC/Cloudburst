package org.cloudburstmc.api.nbt;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.cloudburstmc.api.nbt.internal.NbtGenerator;
import org.cloudburstmc.api.nbt.internal.NbtParser;
import org.cloudburstmc.api.nbt.internal.NbtPropertyModifier;
import org.cloudburstmc.api.nbt.internal.NbtReadModifier;
import org.cloudburstmc.nbt.NbtMap;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.introspect.BeanPropertyDefinition;
import tools.jackson.databind.introspect.ClassIntrospector;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.type.LogicalType;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Converts typed data objects to NBT and back.
 * <p>
 * Writing is exact: each property is written as its resolved NBT type, the
 * {@link NbtTag} annotation if present, else the type that follows from the property's
 * Java type. {@code null} properties are not written.
 * <p>
 * Reading is lenient: any numeric tag binds to any numeric property, and a byte binds to
 * a {@code boolean}. A property whose tag has another kind, for example a string for an
 * {@code int}, keeps its default and the mismatch is logged at debug level. Absent and
 * unknown keys are ignored.
 * <p>
 * {@link NbtMap} and {@link org.cloudburstmc.nbt.NbtList} values pass through unchanged
 * in both directions.
 */
public final class NbtMapper {

    /**
     * The key of the compound that holds plugin data, keyed by plugin id.
     */
    public static final String PLUGIN_DATA_KEY = "CloudburstPluginData";

    private final ObjectMapper mapper;
    private final Map<Class<?>, Set<String>> ownedKeys = new ConcurrentHashMap<>();

    private NbtMapper(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Creates a mapper.
     *
     * @param modules extra Jackson modules, for example serializers for types the data objects hold
     */
    public static NbtMapper create(JacksonModule... modules) {
        SimpleModule nbt = new SimpleModule("cloudburst-nbt");
        nbt.setSerializerModifier(new NbtPropertyModifier());
        nbt.setDeserializerModifier(new NbtReadModifier());
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
     * @param value the object to write; it must serialize as an object
     * @return the written compound
     * @throws IllegalArgumentException if the value does not serialize as an object
     * @throws tools.jackson.core.JacksonException if a property cannot be written as its NBT type
     */
    public NbtMap write(Object value) {
        return write(value, false);
    }

    /**
     * Writes the client view of an object: only its {@link NbtClient} properties. The
     * values of those properties are written whole.
     *
     * @param value the object to write; it must serialize as an object
     * @return the written compound
     * @throws IllegalArgumentException if the value does not serialize as an object
     * @throws tools.jackson.core.JacksonException if a property cannot be written as its NBT type
     */
    public NbtMap writeClient(Object value) {
        return write(value, true);
    }

    private NbtMap write(Object value, boolean clientView) {
        Objects.requireNonNull(value, "value");
        NbtGenerator generator = new NbtGenerator(clientView);
        mapper.writeValue(generator, value);
        if (!(generator.result() instanceof NbtMap tag)) {
            throw new IllegalArgumentException(value.getClass().getName() + " is not written as a compound");
        }
        return tag;
    }

    /**
     * Reads a compound tag into a new object of the given type.
     * <p>
     * Properties whose key is absent, or whose tag does not fit them, keep the value the
     * type's no-arg constructor gives them.
     *
     * @param tag  the compound to read
     * @param type the type to create
     * @return the read object
     */
    public <T> T read(NbtMap tag, Class<T> type) {
        Objects.requireNonNull(tag, "tag");
        Objects.requireNonNull(type, "type");
        try (NbtParser parser = new NbtParser(tag)) {
            return mapper.readValue(parser, type);
        }
    }

    /**
     * Returns the NBT keys of every property a type declares, inherited ones included.
     *
     * @param type the data type
     * @return the keys; the set cannot be modified
     */
    public Set<String> ownedKeys(Class<?> type) {
        Objects.requireNonNull(type, "type");
        return ownedKeys.computeIfAbsent(type, this::findOwnedKeys);
    }

    private Set<String> findOwnedKeys(Class<?> type) {
        SerializationConfig config = mapper.serializationConfig();
        ClassIntrospector introspector = config.classIntrospectorInstance();
        JavaType javaType = config.constructType(type);
        BeanDescription description = introspector.introspectForSerialization(javaType,
                introspector.introspectClassAnnotations(javaType));
        Set<String> keys = new LinkedHashSet<>();
        for (BeanPropertyDefinition property : description.findProperties()) {
            if (property.couldSerialize()) {
                keys.add(property.getName());
            }
        }
        return Collections.unmodifiableSet(keys);
    }
}
