package org.cloudburstmc.api.nbt;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.cloudburstmc.api.nbt.internal.NbtGenerator;
import org.cloudburstmc.api.nbt.internal.NbtPropertyModifier;
import org.cloudburstmc.nbt.NbtMap;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

import java.util.Objects;

/**
 * Converts typed data objects to NBT.
 * <p>
 * Each property is written as its resolved NBT type: the {@link NbtTag} annotation if
 * present, else the type that follows from the property's Java type. {@code null}
 * properties are not written. {@link NbtMap} and {@link org.cloudburstmc.nbt.NbtList}
 * values are written unchanged.
 */
public final class NbtMapper {

    /**
     * The key of the compound that holds plugin data, keyed by plugin id.
     */
    public static final String PLUGIN_DATA_KEY = "CloudburstPluginData";

    private final ObjectMapper mapper;

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
        ObjectMapper mapper = JsonMapper.builder()
                .addModule(nbt)
                .addModules(modules)
                .changeDefaultPropertyInclusion(inclusion -> inclusion
                        .withValueInclusion(JsonInclude.Include.NON_NULL)
                        .withContentInclusion(JsonInclude.Include.NON_NULL))
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
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
        Objects.requireNonNull(value, "value");
        NbtGenerator generator = new NbtGenerator();
        mapper.writeValue(generator, value);
        if (!(generator.result() instanceof NbtMap tag)) {
            throw new IllegalArgumentException(value.getClass().getName() + " is not written as a compound");
        }
        return tag;
    }
}
