package org.cloudburstmc.server.persistence;

import tools.jackson.core.util.DefaultIndenter;
import tools.jackson.core.util.DefaultPrettyPrinter;
import tools.jackson.core.util.Separators;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

/**
 * Validates a complete replacement before publishing or saving a stored list.
 */
public class StoredJsonList<K, E> {

    private static final int MAX_BYTES = 1024 * 1024;
    private static final JsonMapper JSON = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .build();
    private static final ObjectWriter WRITER = JSON.writer().with(new DefaultPrettyPrinter(
            new Separators().withObjectNameValueSpacing(Separators.Spacing.AFTER)
                    .withArrayEmptySeparator("").withObjectEmptySeparator(""))
            .withArrayIndenter(new DefaultIndenter("  ", "\n"))
            .withObjectIndenter(new DefaultIndenter("  ", "\n")));

    private final Path path;
    private final Class<E[]> entryType;
    private final Function<E, K> keys;
    private volatile Map<K, E> entries = Map.of();

    public StoredJsonList(Path path, Class<E[]> entryType, Function<E, K> keys) {
        this.path = Objects.requireNonNull(path, "path");
        this.entryType = Objects.requireNonNull(entryType, "entryType");
        this.keys = Objects.requireNonNull(keys, "keys");
        reload();
    }

    public boolean contains(K key) {
        return entries.containsKey(Objects.requireNonNull(key, "key"));
    }

    public Optional<E> get(K key) {
        return Optional.ofNullable(entries.get(Objects.requireNonNull(key, "key")));
    }

    public List<E> entries() {
        return List.copyOf(entries.values());
    }

    public synchronized void add(E entry) {
        Objects.requireNonNull(entry, "entry");
        Map<K, E> replacement = new LinkedHashMap<>(entries);
        replacement.put(Objects.requireNonNull(keys.apply(entry), "entry key"), entry);
        save(replacement);
        entries = Collections.unmodifiableMap(replacement);
    }

    public synchronized void remove(K key) {
        Objects.requireNonNull(key, "key");
        if (!entries.containsKey(key)) {
            return;
        }

        Map<K, E> replacement = new LinkedHashMap<>(entries);
        replacement.remove(key);
        save(replacement);
        entries = Collections.unmodifiableMap(replacement);
    }

    public synchronized void replace(Collection<E> values) {
        Map<K, E> replacement = new LinkedHashMap<>();
        for (E entry : values) {
            Objects.requireNonNull(entry, "entry");
            K key = Objects.requireNonNull(keys.apply(entry), "entry key");
            if (replacement.putIfAbsent(key, entry) != null) {
                throw new IllegalArgumentException("Duplicate stored-list key: " + key);
            }
        }

        save(replacement);
        entries = Collections.unmodifiableMap(replacement);
    }

    public synchronized void reload() {
        if (!Files.exists(path)) {
            save(Map.of());
            entries = Map.of();
            return;
        }

        try (InputStream input = Files.newInputStream(path)) {
            byte[] data = input.readNBytes(MAX_BYTES + 1);
            if (data.length > MAX_BYTES) {
                throw new IOException("Stored list exceeds " + MAX_BYTES + " bytes");
            }

            E[] loaded = JSON.readValue(data, entryType);
            if (loaded == null) {
                throw new IOException("Stored list must be a JSON array");
            }

            Map<K, E> replacement = new LinkedHashMap<>();
            for (E entry : loaded) {
                if (entry == null || replacement.putIfAbsent(Objects.requireNonNull(keys.apply(entry), "entry key"), entry) != null) {
                    throw new IOException("Stored list contains a null entry or duplicate key");
                }
            }

            entries = Collections.unmodifiableMap(replacement);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read stored list " + path, e);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Invalid stored list " + path + ": " + e.getMessage(), e);
        }
    }

    private void save(Map<K, E> values) {
        byte[] data = WRITER.writeValueAsBytes(values.values());
        if (data.length > MAX_BYTES) {
            throw new IllegalArgumentException("Stored list exceeds " + MAX_BYTES + " bytes: " + path);
        }

        try {
            AtomicFiles.write(path, data);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to save stored list " + path, e);
        }
    }
}
