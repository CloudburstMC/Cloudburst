package org.cloudburstmc.server.configuration.file;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.configuration.Configuration;
import org.cloudburstmc.api.configuration.ConfigurationCodecRegistry;
import org.cloudburstmc.api.configuration.ConfigurationPath;
import org.cloudburstmc.api.configuration.ConfigurationSection;

import java.util.*;

/**
 * In-memory configuration tree.
 */
public class MemoryConfiguration implements Configuration {
    private final ConfigurationCodecRegistry codecs;
    private final Map<String, Section> sections = new HashMap<>();
    private final Map<String, List<String>> comments = new HashMap<>();
    private final Map<String, List<String>> inlineComments = new HashMap<>();

    private final Map<String, Object> values = new LinkedHashMap<>();
    private Configuration defaults;
    private List<String> footer = List.of();

    public MemoryConfiguration(ConfigurationCodecRegistry codecs) {
        this.codecs = Objects.requireNonNull(codecs, "codecs");
    }

    @Override
    public ConfigurationCodecRegistry getCodecs() {
        return codecs;
    }

    @Override
    public Configuration getRoot() {
        return this;
    }

    @Override
    public ConfigurationSection getParent() {
        return null;
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public String getCurrentPath() {
        return "";
    }

    @Override
    public Set<String> getKeys(boolean deep) {
        if (deep) {
            return getValues(true).keySet();
        }

        Set<String> keys = new LinkedHashSet<>();
        values.keySet().forEach(key -> keys.add(ConfigurationPath.key(key)));

        return keys;
    }

    @Override
    public Map<String, Object> getValues(boolean deep) {
        Map<String, Object> merged = copyMap(values);
        if (!deep) {
            return merged;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        flatten("", merged, result);

        return result;
    }

    @Override
    public @Nullable Object get(String path) {
        Object value = lookup(values, path);
        Object fallback = defaults == null ? null : defaults.get(path);
        return copy(value == null ? fallback : value);
    }

    @Override
    public Object get(String path, Object fallback) {
        Object value = lookup(values, path);
        return value == null ? fallback : copy(value);
    }

    @Override
    public void set(String path, @Nullable Object value) {
        put(values, path, value);
        sections.keySet().removeIf(key -> key.equals(path) || key.startsWith(path + "."));
    }

    @Override
    public boolean contains(String path, boolean ignoreDefaults) {
        return lookup(values, path) != null || !ignoreDefaults && defaults != null && defaults.contains(path);
    }

    @Override
    public @Nullable ConfigurationSection getConfigurationSection(String path) {
        if (path.isEmpty()) {
            return this;
        }

        int separator = ConfigurationPaths.firstSeparator(path);
        if (separator >= 0) {
            ConfigurationSection parent = getConfigurationSection(path.substring(0, separator));
            return parent == null ? null : parent.getConfigurationSection(path.substring(separator + 1));
        }

        Object value = get(path);
        if (!(value instanceof Map<?, ?>)) {
            return null;
        }

        if (!(lookup(values, path) instanceof Map<?, ?>)) {
            put(values, path, Map.of());
        }

        Section section = sections.get(path);
        if (section == null) {
            section = new Section(path);
            sections.put(path, section);
        }

        return section;
    }

    @Override
    public ConfigurationSection createSection(String path) {
        set(path, Map.of());
        return getConfigurationSection(path);
    }

    @Override
    public void setDefaults(Configuration source) {
        for (Configuration current = source; current != null; current = current.getDefaults()) {
            if (current == this) {
                throw new IllegalArgumentException("Configuration defaults cannot be cyclic");
            }
        }

        defaults = Objects.requireNonNull(source, "defaults");
    }

    @Override
    public Configuration getDefaults() {
        return defaults;
    }

    @Override
    public void addDefault(String path, Object value) {
        if (defaults == null) {
            defaults = new MemoryConfiguration(codecs);
        }

        defaults.set(path, value);
    }

    @Override
    public void copyDefaults() {
        if (defaults == null) {
            return;
        }

        Map<String, Object> inherited = new LinkedHashMap<>();
        Map<String, List<String>> inheritedComments = new HashMap<>();
        Map<String, List<String>> inheritedInlineComments = new HashMap<>();
        List<Configuration> providers = new ArrayList<>();

        for (Configuration provider = defaults; provider != null; provider = provider.getDefaults()) {
            providers.add(provider);
        }

        for (Configuration provider : providers.reversed()) {
            merge(inherited, provider.getValues(false));
            for (String path : provider.getKeys(true)) {
                inheritedComments.put(path, provider.getComments(path));
                inheritedInlineComments.put(path, provider.getInlineComments(path));
            }
        }

        Map<String, Object> paths = new LinkedHashMap<>();
        flatten("", inherited, paths);

        for (String path : paths.keySet()) {
            if (!contains(path, true)) {
                setComments(path, inheritedComments.getOrDefault(path, List.of()));
                setInlineComments(path, inheritedInlineComments.getOrDefault(path, List.of()));
            }
        }

        mergeMissing(values, inherited);
    }

    @Override
    public List<String> getFooter() {
        return footer;
    }

    @Override
    public void setFooter(List<String> comments) {
        footer = Collections.unmodifiableList(new ArrayList<>(comments));
    }

    @Override
    public List<String> getComments(String path) {
        return comments.getOrDefault(path, List.of());
    }

    @Override
    public void setComments(String path, List<String> lines) {
        comments.put(path, Collections.unmodifiableList(new ArrayList<>(lines)));
    }

    @Override
    public List<String> getInlineComments(String path) {
        return inlineComments.getOrDefault(path, List.of());
    }

    @Override
    public void setInlineComments(String path, List<String> lines) {
        inlineComments.put(path, Collections.unmodifiableList(new ArrayList<>(lines)));
    }

    public Map<String, Object> explicitValues() {
        return copyMap(values);
    }

    private static Object lookup(Map<String, Object> source, String path) {
        Objects.requireNonNull(path, "path");

        if (path.isEmpty()) {
            return source;
        }

        Object current = source;
        for (String key : ConfigurationPaths.keys(path).toArray(String[]::new)) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }

            current = map.get(key);
        }

        return current;
    }

    @SuppressWarnings("unchecked")
    private static void put(Map<String, Object> source, String path, Object value) {
        Objects.requireNonNull(path, "path");

        if (path.isEmpty()) {
            throw new IllegalArgumentException("Cannot replace the root section");
        }

        String[] keys = ConfigurationPaths.keys(path).toArray(String[]::new);
        if (Arrays.stream(keys).anyMatch(String::isEmpty)) {
            throw new IllegalArgumentException("Empty path segment: " + path);
        }

        Object normalized = copy(value);
        Map<String, Object> parent = source;

        for (int i = 0; i < keys.length - 1; i++) {
            Object child = parent.get(keys[i]);
            if (!(child instanceof Map<?, ?>)) {
                if (normalized == null) {
                    return;
                }

                child = new LinkedHashMap<String, Object>();
                parent.put(keys[i], child);
            }

            parent = (Map<String, Object>) child;
        }

        if (normalized == null) {
            parent.remove(keys[keys.length - 1]);
        } else {
            parent.put(keys[keys.length - 1], normalized);
        }
    }

    private static Map<String, Object> copyMap(Map<?, ?> source) {
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) copy(source);
        return result;
    }

    private static Object copy(Object value) {
        return copy(value, new IdentityHashMap<>());
    }

    private static Object copy(Object value, IdentityHashMap<Object, Boolean> active) {
        if (value instanceof ConfigurationSection section) {
            value = section.getValues(false);
        }

        if (value instanceof Map<?, ?> || value instanceof List<?>) {
            if (active.put(value, true) != null) {
                throw new IllegalArgumentException("Cyclic configuration values are not supported");
            }

            try {
                if (value instanceof Map<?, ?> map) {
                    Map<String, Object> result = new LinkedHashMap<>();
                    for (Map.Entry<?, ?> entry : map.entrySet()) {
                        if (!(entry.getKey() instanceof String key)) {
                            throw new IllegalArgumentException("Configuration keys must be strings");
                        }

                        result.put(key, copy(entry.getValue(), active));
                    }

                    return result;
                }

                List<Object> result = new ArrayList<>();
                for (Object element : (List<?>) value) {
                    result.add(copy(element, active));
                }

                return result;
            } finally {
                active.remove(value);
            }
        }

        if (value == null || value instanceof String || value instanceof Boolean || value instanceof Number) {
            return value;
        }

        throw new IllegalArgumentException("Unsupported configuration value: " + value.getClass().getName());
    }

    @SuppressWarnings("unchecked")
    private static void merge(Map<String, Object> target, Map<String, Object> source) {
        source.forEach((key, value) -> {
            if (value instanceof Map<?, ?> child && target.get(key) instanceof Map<?, ?> existing) {
                merge((Map<String, Object>) existing, (Map<String, Object>) child);
            } else {
                target.put(key, copy(value));
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static void mergeMissing(Map<String, Object> target, Map<String, Object> defaults) {
        defaults.forEach((key, value) -> {
            Object existing = target.get(key);

            if (existing == null) {
                target.put(key, copy(value));
            } else if (existing instanceof Map<?, ?> existingMap && value instanceof Map<?, ?> defaultMap) {
                mergeMissing((Map<String, Object>) existingMap, (Map<String, Object>) defaultMap);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static void flatten(String prefix, Map<String, Object> source, Map<String, Object> target) {
        source.forEach((key, value) -> {
            String path = prefix + ConfigurationPath.key(key);
            target.put(path, copy(value));

            if (value instanceof Map<?, ?> map) {
                flatten(path + ".", (Map<String, Object>) map, target);
            }
        });
    }

    private class Section implements ConfigurationSection {
        private final String path;
        private final Map<String, Object> values;
        private final ConfigurationSection parent;
        private final Map<String, ConfigurationSection> children = new HashMap<>();

        @SuppressWarnings("unchecked")
        private Section(String path) {
            this(path, (Map<String, Object>) lookup(MemoryConfiguration.this.values, path), MemoryConfiguration.this);
        }

        private Section(String path, Map<String, Object> data, ConfigurationSection parent) {
            this.path = path;
            this.parent = parent;
            this.values = data;
        }

        @Override
        public Configuration getRoot() {
            return MemoryConfiguration.this;
        }

        @Override
        public ConfigurationSection getParent() {
            return parent;
        }

        @Override
        public String getName() {
            return ConfigurationPaths.keys(path).getLast();
        }

        @Override
        public String getCurrentPath() {
            return path;
        }

        @Override
        public Object get(String child) {
            Object explicit = lookup(values, child);
            return explicit != null ? copy(explicit) : defaults == null ? null : defaults.get(resolve(child));
        }

        @Override
        public Object get(String child, Object fallback) {
            Object value = lookup(values, child);
            return value == null ? fallback : copy(value);
        }

        @Override
        public void set(String child, Object value) {
            put(values, child, value);
            children.keySet().removeIf(key -> key.equals(child) || key.startsWith(child + "."));
        }

        @Override
        public boolean contains(String child, boolean ignoreDefaults) {
            return lookup(values, child) != null || !ignoreDefaults && defaults != null && defaults.contains(resolve(child));
        }

        @Override
        @SuppressWarnings("unchecked")
        public ConfigurationSection getConfigurationSection(String child) {
            if (child.isEmpty()) {
                return this;
            }

            int separator = ConfigurationPaths.firstSeparator(child);
            if (separator >= 0) {
                ConfigurationSection parent = getConfigurationSection(child.substring(0, separator));
                return parent == null ? null : parent.getConfigurationSection(child.substring(separator + 1));
            }

            Object data = lookup(values, child);
            if (!(data instanceof Map<?, ?>)) {
                if (!(get(child) instanceof Map<?, ?>)) {
                    return null;
                }

                put(values, child, Map.of());
                data = lookup(values, child);
            }

            Section existing = (Section) children.get(child);
            if (existing == null || existing.values != data) {
                existing = new Section(resolve(child), (Map<String, Object>) data, this);
                children.put(child, existing);
            }

            return existing;
        }

        @Override
        public ConfigurationSection createSection(String child) {
            set(child, Map.of());
            return getConfigurationSection(child);
        }

        @Override
        public Map<String, Object> getValues(boolean deep) {
            Map<String, Object> result = copyMap(values);
            if (!deep) {
                return result;
            }

            Map<String, Object> flattened = new LinkedHashMap<>();
            flatten("", result, flattened);

            return flattened;
        }

        @Override
        public Set<String> getKeys(boolean deep) {
            if (deep) {
                return getValues(true).keySet();
            }

            Set<String> keys = new LinkedHashSet<>();
            values.keySet().forEach(key -> keys.add(ConfigurationPath.key(key)));

            return keys;
        }

        @Override
        public List<String> getComments(String child) {
            return MemoryConfiguration.this.getComments(resolve(child));
        }

        @Override
        public void setComments(String child, List<String> lines) {
            MemoryConfiguration.this.setComments(resolve(child), lines);
        }

        @Override
        public List<String> getInlineComments(String child) {
            return MemoryConfiguration.this.getInlineComments(resolve(child));
        }

        @Override
        public void setInlineComments(String child, List<String> lines) {
            MemoryConfiguration.this.setInlineComments(resolve(child), lines);
        }

        private String resolve(String child) {
            return child.isEmpty() ? path : path.isEmpty() ? child : path + "." + child;
        }
    }
}
