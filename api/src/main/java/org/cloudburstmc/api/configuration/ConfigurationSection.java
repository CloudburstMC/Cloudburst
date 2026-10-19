package org.cloudburstmc.api.configuration;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.ComponentDecoder;
import net.kyori.adventure.text.serializer.ComponentEncoder;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.player.OfflinePlayer;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.jetbrains.annotations.Contract;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * A mutable section addressed by dot-separated paths.
 *
 * <p>Reads include defaults unless stated otherwise. An empty path reads this section.
 * Mutating values requires a nonempty path with no empty segments. Setting a nested
 * path replaces any scalar parent with a section.</p>
 *
 * <p>Escape literal dots, brackets, and backslashes in keys with a backslash.
 * For example, {@code database\.host} addresses the literal key {@code database.host}.
 * Use {@link ConfigurationPath#of(String...)} to build paths from literal keys.
 * List-item comment paths use unescaped indices such as {@code items[0]}.</p>
 *
 * <p>Maps and lists returned by reads are independent copies. Changes are applied
 * through {@link #set(String, Object)} or a live child section. Instances require
 * external synchronization when shared between threads.</p>
 */
public interface ConfigurationSection {

    /**
     * Returns the root configuration owning this section.
     */
    Configuration getRoot();

    /**
     * Returns the parent section, or null for the root.
     */
    @Nullable
    ConfigurationSection getParent();

    /**
     * Returns this section's name, or an empty string for the root.
     */
    String getName();

    /**
     * Returns this section's path from the root, or an empty string for the root.
     */
    String getCurrentPath();

    /**
     * Returns direct keys, or all descendant paths when deep is true.
     * Keys include only explicit values, are escaped for path access, and are relative to this section.
     */
    Set<String> getKeys(boolean deep);

    /**
     * Returns a snapshot of values, optionally including descendant paths.
     * Includes only explicit values. Section values are maps.
     * Shallow keys are literal names. Deep keys are escaped paths relative to this section.
     */
    Map<String, Object> getValues(boolean deep);

    /**
     * Returns a value including defaults, or null when absent.
     */
    @Nullable
    Object get(String path);

    /**
     * Returns the fallback when the value is absent.
     * Configured defaults are ignored when an explicit fallback is supplied.
     */
    @Nullable
    @Contract("_, !null -> !null")
    Object get(String path, @Nullable Object fallback);

    /**
     * Sets a value. Null removes the explicit value and reveals any default.
     *
     * <p>Supported values are strings, booleans, numbers, sections, and lists or
     * maps containing supported values. Map keys must be strings. Mutable
     * containers are copied.</p>
     *
     * @throws IllegalArgumentException if the path or value is unsupported
     */
    void set(String path, @Nullable Object value);

    /**
     * Tests whether a value exists in this section or its defaults.
     */
    default boolean contains(String path) {
        return contains(path, false);
    }

    /**
     * Tests whether a value exists, optionally ignoring defaults.
     *
     * @param path the path relative to this section
     * @param ignoreDefaults true to check only explicitly set values
     * @return whether the path has a value
     */
    boolean contains(String path, boolean ignoreDefaults);

    /**
     * Tests whether this configuration explicitly defines a value.
     */
    default boolean isSet(String path) {
        return contains(path, true);
    }

    /**
     * Returns a live child section, or null if the path is not a section.
     */
    @Nullable
    ConfigurationSection getConfigurationSection(String path);

    /**
     * Tests whether the value, including defaults, is a section.
     */
    default boolean isConfigurationSection(String path) {
        return get(path) instanceof Map<?, ?>;
    }

    /**
     * Creates a section, replacing the existing value at that path.
     * Existing defaults remain available within the new section.
     *
     * @throws IllegalArgumentException if the path is empty or contains empty segments
     */
    ConfigurationSection createSection(String path);

    /**
     * Creates a section initialized with copies of the supplied values.
     */
    default ConfigurationSection createSection(String path, Map<String, ?> values) {
        set(path, values);
        return requireNonNull(getConfigurationSection(path));
    }

    /**
     * Returns the matching section from the defaults provider, or null when absent.
     */
    default @Nullable ConfigurationSection getDefaultSection() {
        Configuration defaults = getRoot().getDefaults();
        return defaults == null ? null : defaults.getConfigurationSection(getCurrentPath());
    }

    /**
     * Adds a default relative to this section. Null removes the default.
     */
    default void addDefault(String path, @Nullable Object value) {
        String prefix = getCurrentPath();
        getRoot().addDefault(prefix.isEmpty() ? path : prefix + "." + path, value);
    }

    /**
     * Returns a value of the requested type, including defaults, or null.
     */
    default <T> @Nullable T getObject(String path, Class<T> type) {
        Object value = get(path);
        return type.isInstance(value) ? type.cast(value) : null;
    }

    /**
     * Returns the value if it has the requested type, otherwise the fallback.
     * Configured defaults are ignored. No parsing or conversion is performed.
     */
    @Contract("_, _, !null -> !null")
    default <T> @Nullable T getObject(String path, Class<T> type, @Nullable T fallback) {
        Object value = get(path, null);
        return type.isInstance(value) ? type.cast(value) : fallback;
    }

    /**
     * Decodes a registered value, including configured defaults, or returns null for another type.
     * Malformed data identifying the requested codec is rejected.
     *
     * @throws IllegalArgumentException if the value identifies the requested codec but cannot be decoded
     */
    default <T> @Nullable T getDecoded(String path, Class<T> type) {
        return decodeValue(get(path), type, null);
    }

    /**
     * Decodes an explicit registered value, or returns the fallback for another type.
     * Configured defaults are ignored. Malformed data for the requested codec is rejected.
     *
     * @throws IllegalArgumentException if the value identifies the requested codec but cannot be decoded
     */
    @Contract("_, _, !null -> !null")
    default <T> @Nullable T getDecoded(String path, Class<T> type, @Nullable T fallback) {
        return decodeValue(get(path, null), type, fallback);
    }

    /**
     * Encodes a registered value at this path. Null removes the explicit value.
     * The value is converted to an independent mapping before storage.
     */
    default void setEncoded(String path, @Nullable Object value) {
        set(path, value == null ? null : getRoot().getCodecs().encode(value));
    }

    /**
     * Returns the saved item stack, including defaults, or null for another type.
     * Invalid encoded data is rejected.
     */
    default @Nullable ItemStack getItemStack(String path) {
        return getDecoded(path, ItemStack.class);
    }

    /**
     * Returns the explicit item stack, or the fallback for another type.
     * Configured defaults are ignored. Invalid encoded data is rejected.
     */
    @Contract("_, !null -> !null")
    default @Nullable ItemStack getItemStack(String path, @Nullable ItemStack fallback) {
        return getDecoded(path, ItemStack.class, fallback);
    }

    /**
     * Tests whether the value, including defaults, identifies an item stack.
     * This does not validate encoded data.
     */
    default boolean isItemStack(String path) {
        Object value = get(path);
        return value instanceof ItemStack || getRoot().getCodecs().isEncoded(value, ItemStack.class);
    }

    /**
     * Returns the saved location, including defaults, or null for another type.
     * Invalid encoded data is rejected.
     */
    default @Nullable Location getLocation(String path) {
        return getDecoded(path, Location.class);
    }

    /**
     * Returns the explicit location, or the fallback for another type.
     * Configured defaults are ignored. Invalid encoded data is rejected.
     */
    @Contract("_, !null -> !null")
    default @Nullable Location getLocation(String path, @Nullable Location fallback) {
        return getDecoded(path, Location.class, fallback);
    }

    /**
     * Tests whether the value, including defaults, identifies a location.
     * This does not validate encoded data.
     */
    default boolean isLocation(String path) {
        Object value = get(path);
        return value instanceof Location || getRoot().getCodecs().isEncoded(value, Location.class);
    }

    /**
     * Returns the saved floating-point vector, including defaults, or null for another type.
     * Invalid encoded data is rejected.
     */
    default @Nullable Vector3f getVector(String path) {
        return getDecoded(path, Vector3f.class);
    }

    /**
     * Returns the explicit floating-point vector, or the fallback for another type.
     * Configured defaults are ignored. Invalid encoded data is rejected.
     */
    @Contract("_, !null -> !null")
    default @Nullable Vector3f getVector(String path, @Nullable Vector3f fallback) {
        return getDecoded(path, Vector3f.class, fallback);
    }

    /**
     * Tests whether the value, including defaults, identifies a floating-point vector.
     * This does not validate encoded data.
     */
    default boolean isVector(String path) {
        Object value = get(path);
        return value instanceof Vector3f || getRoot().getCodecs().isEncoded(value, Vector3f.class);
    }

    /**
     * Returns the saved integer block vector, including defaults, or null for another type.
     * Invalid encoded data is rejected.
     */
    default @Nullable Vector3i getBlockVector(String path) {
        return getDecoded(path, Vector3i.class);
    }

    /**
     * Returns the explicit integer block vector, or the fallback for another type.
     * Configured defaults are ignored. Invalid encoded data is rejected.
     */
    @Contract("_, !null -> !null")
    default @Nullable Vector3i getBlockVector(String path, @Nullable Vector3i fallback) {
        return getDecoded(path, Vector3i.class, fallback);
    }

    /**
     * Tests whether the value, including defaults, identifies an integer block vector.
     * This does not validate encoded data.
     */
    default boolean isBlockVector(String path) {
        Object value = get(path);
        return value instanceof Vector3i || getRoot().getCodecs().isEncoded(value, Vector3i.class);
    }

    /**
     * Returns the saved block state, including defaults, or null for another type.
     * Invalid encoded data is rejected.
     */
    default @Nullable BlockState getBlockState(String path) {
        return getDecoded(path, BlockState.class);
    }

    /**
     * Returns the explicit block state, or the fallback for another type.
     * Configured defaults are ignored. Invalid encoded data is rejected.
     */
    @Contract("_, !null -> !null")
    default @Nullable BlockState getBlockState(String path, @Nullable BlockState fallback) {
        return getDecoded(path, BlockState.class, fallback);
    }

    /**
     * Tests whether the value, including defaults, identifies a block state.
     * This does not validate encoded data.
     */
    default boolean isBlockState(String path) {
        Object value = get(path);
        return value instanceof BlockState || getRoot().getCodecs().isEncoded(value, BlockState.class);
    }

    /**
     * Returns the saved color, including defaults, or null for another type.
     * Invalid encoded data is rejected.
     */
    default @Nullable Color getColor(String path) {
        return getDecoded(path, Color.class);
    }

    /**
     * Returns the explicit color, or the fallback for another type.
     * Configured defaults are ignored. Invalid encoded data is rejected.
     */
    @Contract("_, !null -> !null")
    default @Nullable Color getColor(String path, @Nullable Color fallback) {
        return getDecoded(path, Color.class, fallback);
    }

    /**
     * Tests whether the value, including defaults, identifies a color.
     * This does not validate encoded data.
     */
    default boolean isColor(String path) {
        Object value = get(path);
        return value instanceof Color || getRoot().getCodecs().isEncoded(value, Color.class);
    }

    /**
     * Returns the saved offline-player reference, including defaults, or null for another type.
     * Invalid encoded data is rejected.
     */
    default @Nullable OfflinePlayer getOfflinePlayer(String path) {
        return getDecoded(path, OfflinePlayer.class);
    }

    /**
     * Returns the explicit offline-player reference, or the fallback for another type.
     * Configured defaults are ignored. Invalid encoded data is rejected.
     */
    @Contract("_, !null -> !null")
    default @Nullable OfflinePlayer getOfflinePlayer(String path, @Nullable OfflinePlayer fallback) {
        return getDecoded(path, OfflinePlayer.class, fallback);
    }

    /**
     * Tests whether the value, including defaults, identifies an offline-player reference.
     * This does not validate encoded data.
     */
    default boolean isOfflinePlayer(String path) {
        Object value = get(path);
        return value instanceof OfflinePlayer || getRoot().getCodecs().isEncoded(value, OfflinePlayer.class);
    }

    /**
     * Decodes a string as a component with the supplied decoder, including configured defaults.
     * Returns null when no string is present.
     */
    default @Nullable Component getComponent(String path, ComponentDecoder<? super String, ? extends Component> decoder) {
        return getComponent(path, decoder, null);
    }

    /**
     * Decodes a string with the supplied decoder, including configured defaults.
     * Returns the fallback when no string is present.
     * No message format is selected automatically.
     */
    @Contract("_, _, !null -> !null")
    default @Nullable Component getComponent(String path, ComponentDecoder<? super String, ? extends Component> decoder, @Nullable Component fallback) {
        requireNonNull(decoder, "decoder");
        Object value = get(path);
        return value instanceof String text ? decoder.deserialize(text) : fallback;
    }

    /**
     * Stores a component as a string using the supplied encoder. Null removes the value.
     */
    default void setComponent(String path, ComponentEncoder<? super Component, String> encoder, @Nullable Component value) {
        requireNonNull(encoder, "encoder");
        set(path, encoder.serializeOrNull(value));
    }

    /**
     * Returns the value as a string, or null when absent.
     */
    default @Nullable String getString(String path) {
        Object value = get(path);
        return value == null ? null : value.toString();
    }

    /**
     * Returns the explicit value as a string, or the fallback when absent.
     */
    @Contract("_, !null -> !null")
    default @Nullable String getString(String path, @Nullable String fallback) {
        Object value = get(path, null);
        return value == null ? fallback : value.toString();
    }

    /**
     * Tests whether the value, including defaults, is a string.
     */
    default boolean isString(String path) {
        return get(path) instanceof String;
    }

    /**
     * Returns a boolean, or false when absent or of another type.
     */
    default boolean getBoolean(String path) {
        return get(path) instanceof Boolean value && value;
    }

    /**
     * Returns a boolean, or the fallback. Strings such as "true" are not converted.
     * Configured defaults are ignored when an explicit fallback is supplied.
     */
    default boolean getBoolean(String path, boolean fallback) {
        return get(path, null) instanceof Boolean value ? value : fallback;
    }

    /**
     * Tests whether the value, including defaults, is a boolean.
     */
    default boolean isBoolean(String path) {
        return get(path) instanceof Boolean;
    }

    /**
     * Returns a number as an int, or zero when absent or of another type.
     *
     * @see #getInt(String, int)
     */
    default int getInt(String path) {
        return get(path) instanceof Number number ? number.intValue() : 0;
    }

    /**
     * Converts a number using {@link Number#intValue()}, or returns the fallback.
     * Fractional parts are truncated and values outside the int range may overflow.
     * Strings are not parsed.
     */
    default int getInt(String path, int fallback) {
        return get(path, null) instanceof Number number ? number.intValue() : fallback;
    }

    /**
     * Tests whether the value, including defaults, is an int.
     */
    default boolean isInt(String path) {
        return get(path) instanceof Integer;
    }

    /**
     * Returns a number as a long, or zero when absent or of another type.
     *
     * @see #getLong(String, long)
     */
    default long getLong(String path) {
        return get(path) instanceof Number number ? number.longValue() : 0;
    }

    /**
     * Converts a number using {@link Number#longValue()}, or returns the fallback.
     * Fractional parts are truncated and values outside the long range may overflow.
     * Strings are not parsed.
     */
    default long getLong(String path, long fallback) {
        return get(path, null) instanceof Number number ? number.longValue() : fallback;
    }

    /**
     * Tests whether the value, including defaults, is a long.
     */
    default boolean isLong(String path) {
        return get(path) instanceof Long;
    }

    /**
     * Returns a number as a double, or zero when absent or of another type.
     *
     * @see #getDouble(String, double)
     */
    default double getDouble(String path) {
        return get(path) instanceof Number number ? number.doubleValue() : 0;
    }

    /**
     * Converts a number using {@link Number#doubleValue()}, or returns the fallback.
     * Conversion may lose precision. Strings are not parsed.
     */
    default double getDouble(String path, double fallback) {
        return get(path, null) instanceof Number number ? number.doubleValue() : fallback;
    }

    /**
     * Tests whether the value, including defaults, is a double.
     */
    default boolean isDouble(String path) {
        return get(path) instanceof Double;
    }

    /**
     * Returns a snapshot of the list, or an empty list for an incompatible value.
     */
    default List<?> getList(String path) {
        return get(path) instanceof List<?> list ? list : List.of();
    }

    /**
     * Returns the explicit list, or the supplied fallback. Defaults are ignored.
     */
    @Contract("_, !null -> !null")
    default @Nullable List<?> getList(String path, @Nullable List<?> fallback) {
        Object value = get(path, null);
        return value instanceof List<?> list ? list : fallback;
    }

    /**
     * Returns only list elements matching the requested type.
     * Elements are not converted. The returned list is unmodifiable.
     */
    default <T> List<T> getList(String path, Class<T> type) {
        return getList(path).stream().filter(type::isInstance).map(type::cast).toList();
    }

    /**
     * Tests whether the value, including defaults, is a list.
     */
    default boolean isList(String path) {
        return get(path) instanceof List<?>;
    }

    /**
     * Returns strings and primitive values converted to strings. Containers are skipped.
     */
    default List<String> getStringList(String path) {
        return getList(path).stream()
                .filter(value -> value instanceof String || value instanceof Number
                        || value instanceof Boolean || value instanceof Character)
                .map(String::valueOf)
                .toList();
    }

    /**
     * Returns boolean elements and strings equal to "true" or "false".
     * Other values are skipped.
     */
    default List<Boolean> getBooleanList(String path) {
        ArrayList<Boolean> result = new ArrayList<>();
        for (Object value : getList(path)) {
            if (value instanceof Boolean flag) {
                result.add(flag);
            } else if ("true".equals(value)) {
                result.add(true);
            } else if ("false".equals(value)) {
                result.add(false);
            }
        }
        return result;
    }

    /**
     * Returns numeric list elements converted to integer values.
     * Numeric strings are parsed. Incompatible elements are skipped.
     */
    default List<Integer> getIntegerList(String path) {
        ArrayList<Integer> result = new ArrayList<>();
        for (Object value : getList(path)) {
            if (value instanceof Number number) {
                result.add(number.intValue());
            } else if (value instanceof String text) {
                try {
                    result.add(Integer.valueOf(text));
                } catch (NumberFormatException ignored) {
                    // Incompatible elements are omitted from typed lists.
                }
            }
        }
        return result;
    }

    /**
     * Returns numeric list elements converted to long values.
     * Numeric strings are parsed. Incompatible elements are skipped.
     */
    default List<Long> getLongList(String path) {
        ArrayList<Long> result = new ArrayList<>();
        for (Object value : getList(path)) {
            if (value instanceof Number number) {
                result.add(number.longValue());
            } else if (value instanceof String text) {
                try {
                    result.add(Long.valueOf(text));
                } catch (NumberFormatException ignored) {
                    // Incompatible elements are omitted from typed lists.
                }
            }
        }
        return result;
    }

    /**
     * Returns numeric list elements converted to double values.
     * Numeric strings are parsed. Incompatible elements are skipped.
     */
    default List<Double> getDoubleList(String path) {
        ArrayList<Double> result = new ArrayList<>();
        for (Object value : getList(path)) {
            if (value instanceof Number number) {
                result.add(number.doubleValue());
            } else if (value instanceof String text) {
                try {
                    result.add(Double.valueOf(text));
                } catch (NumberFormatException ignored) {
                    // Incompatible elements are omitted from typed lists.
                }
            }
        }
        return result;
    }

    /**
     * Returns numeric list elements converted to float values.
     * Numeric strings are parsed. Incompatible elements are skipped.
     */
    default List<Float> getFloatList(String path) {
        ArrayList<Float> result = new ArrayList<>();
        for (Object value : getList(path)) {
            if (value instanceof Number number) {
                result.add(number.floatValue());
            } else if (value instanceof String text) {
                try {
                    result.add(Float.valueOf(text));
                } catch (NumberFormatException ignored) {
                    // Incompatible elements are omitted from typed lists.
                }
            }
        }
        return result;
    }

    /**
     * Returns numeric list elements converted to short values.
     * Numeric strings are parsed. Incompatible elements are skipped.
     */
    default List<Short> getShortList(String path) {
        ArrayList<Short> result = new ArrayList<>();
        for (Object value : getList(path)) {
            if (value instanceof Number number) {
                result.add(number.shortValue());
            } else if (value instanceof String text) {
                try {
                    result.add(Short.valueOf(text));
                } catch (NumberFormatException ignored) {
                    // Incompatible elements are omitted from typed lists.
                }
            }
        }
        return result;
    }

    /**
     * Returns numeric list elements converted to byte values.
     * Numeric strings are parsed. Incompatible elements are skipped.
     */
    default List<Byte> getByteList(String path) {
        ArrayList<Byte> result = new ArrayList<>();
        for (Object value : getList(path)) {
            if (value instanceof Number number) {
                result.add(number.byteValue());
            } else if (value instanceof String text) {
                try {
                    result.add(Byte.valueOf(text));
                } catch (NumberFormatException ignored) {
                    // Incompatible elements are omitted from typed lists.
                }
            }
        }
        return result;
    }

    /**
     * Returns characters from numeric values and single-character strings.
     * Other values are skipped.
     */
    default List<Character> getCharacterList(String path) {
        ArrayList<Character> result = new ArrayList<>();
        for (Object value : getList(path)) {
            if (value instanceof Number number) {
                result.add((char) number.intValue());
            } else if (value instanceof String text && text.length() == 1) {
                result.add(text.charAt(0));
            } else if (value instanceof Character character) {
                result.add(character);
            }
        }
        return result;
    }

    /**
     * Returns only map elements from the list. Each map is an independent snapshot.
     */
    default List<Map<?, ?>> getMapList(String path) {
        ArrayList<Map<?, ?>> result = new ArrayList<>();
        for (Object value : getList(path)) {
            if (value instanceof Map<?, ?> map) {
                result.add(map);
            }
        }
        return result;
    }

    /**
     * Returns block comments attached to a path. Null entries represent blank lines.
     * The returned list is unmodifiable. An empty path addresses this section's comments,
     * or the document header when called on the root.
     * List item comments use zero-based indices, for example {@code items[0]}.
     */
    List<String> getComments(String path);

    /**
     * Replaces block comments. Null entries represent blank lines.
     * Supply an empty list to clear comments. Comments on absent values are not saved.
     */
    void setComments(String path, List<String> comments);

    /**
     * Returns comments following a value on the same line.
     * The returned list is unmodifiable and is empty when no comments are set.
     */
    List<String> getInlineComments(String path);

    /**
     * Replaces comments following a value on the same line.
     * Supply an empty list to clear comments. Comments on absent values are not saved.
     */
    void setInlineComments(String path, List<String> comments);

    private <T> @Nullable T decodeValue(@Nullable Object value, Class<T> type, @Nullable T fallback) {
        if (type.isInstance(value)) {
            return type.cast(value);
        }

        ConfigurationCodecRegistry codecs = getRoot().getCodecs();
        if (!codecs.isEncoded(value, type)) {
            return fallback;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> encoded = (Map<String, Object>) value;
        return codecs.decode(encoded, type);
    }
}
