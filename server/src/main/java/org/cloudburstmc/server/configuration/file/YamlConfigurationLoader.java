package org.cloudburstmc.server.configuration.file;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.configuration.*;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.player.OfflinePlayer;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.configuration.codec.*;
import org.cloudburstmc.server.persistence.AtomicFiles;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.comments.CommentLine;
import org.yaml.snakeyaml.comments.CommentType;
import org.yaml.snakeyaml.constructor.Construct;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.nodes.*;
import org.yaml.snakeyaml.representer.Representer;

import java.awt.*;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.function.Function;

/**
 * YAML encoding and file storage for configuration trees.
 */
public class YamlConfigurationLoader implements ConfigurationLoader {
    private final Map<Identifier, CodecRegistration<?>> codecs = new HashMap<>();
    private final Map<Class<?>, CodecRegistration<?>> codecsByClass = new HashMap<>();
    private final ConfigurationResolvers resolvers;

    private YamlConfigurationOptions options = new YamlConfigurationOptions();

    public YamlConfigurationLoader() {
        this(ConfigurationResolvers.unavailable());
    }

    public YamlConfigurationLoader(ConfigurationResolvers resolvers) {
        this.resolvers = Objects.requireNonNull(resolvers, "resolvers");

        registerCodec(Identifier.parse("cloudburst:item"), ItemStack.class, new ItemConfigurationCodec(this));
        registerCodec(Identifier.parse("cloudburst:block_state"), BlockState.class, new BlockStateConfigurationCodec(resolvers.blocks()));
        registerCodec(Identifier.parse("cloudburst:location"), Location.class, new LocationConfigurationCodec(resolvers.levels()));
        registerCodec(Identifier.parse("cloudburst:offline_player"), OfflinePlayer.class, new OfflinePlayerConfigurationCodec(resolvers));
        registerBuiltin("color", Color.class, value -> Map.of("argb", value.getRGB()), values -> new Color(ConfigurationNumbers.integer(values.get("argb"), "argb"), true));
        registerBuiltin("string", String.class, value -> Map.of("value", value), values -> (String) values.get("value"));
        registerBuiltin("boolean", Boolean.class, value -> Map.of("value", value), values -> (Boolean) values.get("value"));
        registerBuiltin("integer", Integer.class, value -> Map.of("value", value), values -> ConfigurationNumbers.integer(values.get("value"), "value"));
        registerBuiltin("long", Long.class, value -> Map.of("value", value), values -> ConfigurationNumbers.longValue(values.get("value"), "value"));
        registerBuiltin("float", Float.class, value -> Map.of("value", ConfigurationNumbers.floatValue(value, "value")), values -> ConfigurationNumbers.floatValue(values.get("value"), "value"));
        registerBuiltin("double", Double.class, value -> Map.of("value", ConfigurationNumbers.doubleValue(value, "value")), values -> ConfigurationNumbers.doubleValue(values.get("value"), "value"));
        registerBuiltin("identifier", Identifier.class, value -> Map.of("value", value.toString()), values -> Identifier.parse((String) values.get("value")));
        registerBuiltin("vector3f", Vector3f.class, value -> Map.of("x", ConfigurationNumbers.floatValue(value.getX(), "x"), "y", ConfigurationNumbers.floatValue(value.getY(), "y"), "z", ConfigurationNumbers.floatValue(value.getZ(), "z")),
                values -> Vector3f.from(
                        ConfigurationNumbers.floatValue(values.get("x"), "x"),
                        ConfigurationNumbers.floatValue(values.get("y"), "y"),
                        ConfigurationNumbers.floatValue(values.get("z"), "z"))
        );
        registerBuiltin("vector3i", Vector3i.class, value -> Map.of("x", value.getX(), "y", value.getY(), "z", value.getZ()),
                values -> Vector3i.from(
                        ConfigurationNumbers.integer(values.get("x"), "x"),
                        ConfigurationNumbers.integer(values.get("y"), "y"),
                        ConfigurationNumbers.integer(values.get("z"), "z"))
        );
    }

    @Override
    public <T> void registerCodec(Identifier id, Class<T> type, ConfigurationCodec<T> codec) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(codec, "codec");

        if (codecs.containsKey(id) || codecsByClass.containsKey(type)) {
            throw new IllegalArgumentException("Codec ID or type is already registered: " + id);
        }

        CodecRegistration<T> registration = new CodecRegistration<>(id, type, codec);
        codecs.put(id, registration);
        codecsByClass.put(type, registration);
    }

    @Override
    public Map<String, Object> encode(Object value) {
        CodecRegistration<?> registration = codecsByClass.get(value.getClass());
        if (registration == null) {
            List<CodecRegistration<?>> matches = codecs.values().stream()
                    .filter(candidate -> candidate.type.isInstance(value))
                    .toList();

            for (CodecRegistration<?> candidate : matches) {
                if (matches.stream().allMatch(other -> other.type.isAssignableFrom(candidate.type))) {
                    registration = candidate;
                    break;
                }
            }

            if (registration == null && !matches.isEmpty()) {
                throw new IllegalArgumentException("Ambiguous codecs for " + value.getClass().getName());
            }
        }

        if (registration == null) {
            throw new IllegalArgumentException("No codec for " + value.getClass().getName());
        }

        MemoryConfiguration copy = new MemoryConfiguration(this);
        copy.set("value", registration.encode(value));

        @SuppressWarnings("unchecked")
        Map<String, Object> encoded = (Map<String, Object>) copy.get("value");

        return encoded;
    }

    @Override
    public <T> T decode(Map<String, Object> value, Class<T> type) {
        if (!(value.get("type") instanceof String id) || !(value.get("data") instanceof Map<?, ?> data)) {
            throw new IllegalArgumentException("Encoded configuration value requires type and data");
        }

        CodecRegistration<?> registration = codecs.get(Identifier.parse(id));
        if (registration == null || registration.type != type) {
            throw new IllegalArgumentException("Unknown or incompatible configuration codec: " + id);
        }

        Map<String, Object> fields = new LinkedHashMap<>();
        data.forEach((key, field) -> {
            if (!(key instanceof String name)) {
                throw new IllegalArgumentException("Codec data keys must be strings");
            }

            fields.put(name, field);
        });

        try {
            return type.cast(Objects.requireNonNull(registration.codec.decode(fields), "Codec returned null"));
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Invalid configuration value for " + id + ": " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isEncoded(Object value, Class<?> type) {
        if (!(value instanceof Map<?, ?> map) || !(map.get("type") instanceof String id)) {
            return false;
        }

        try {
            CodecRegistration<?> registration = codecs.get(Identifier.parse(id));
            return registration != null && registration.type == type;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public ConfigurationLoader withOptions(YamlConfigurationOptions source) {
        YamlConfigurationLoader result = new YamlConfigurationLoader(resolvers);
        result.options = new YamlConfigurationOptions()
                .indent(source.indent())
                .width(source.width())
                .parseComments(source.parseComments())
                .codePointLimit(source.codePointLimit())
                .maxAliases(source.maxAliases())
                .nestingDepthLimit(source.nestingDepthLimit());

        codecs.forEach((id, registration) -> {
            if (!id.equals(Identifier.parse("cloudburst:item")) && !id.equals(Identifier.parse("cloudburst:location"))) {
                result.codecs.put(id, registration);
                result.codecsByClass.put(registration.type, registration);
            }
        });

        return result;
    }

    @Override
    public Configuration create() {
        return new MemoryConfiguration(this);
    }

    @Override
    public Configuration load(Path path) throws IOException {
        if (!Files.exists(path)) {
            return create();
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return load(reader);
        } catch (IOException e) {
            throw new IOException("Unable to load YAML configuration " + path + ": " + e.getMessage(), e);
        }
    }

    @Override
    public Configuration load(Reader reader) throws IOException {
        StringBuilder content = new StringBuilder();
        char[] buffer = new char[4096];
        int count;
        int codePoints = 0;
        boolean previousHighSurrogate = false;

        while ((count = reader.read(buffer)) != -1) {
            for (int index = 0; index < count; index++) {
                char character = buffer[index];

                if (!Character.isLowSurrogate(character) || !previousHighSurrogate) {
                    if (++codePoints > options.codePointLimit()) {
                        throw new IOException("YAML document exceeds the configured code-point limit: "
                                + options.codePointLimit());
                    }
                }

                previousHighSurrogate = Character.isHighSurrogate(character);
            }

            content.append(buffer, 0, count);
        }

        return parse(content.toString());
    }

    @Override
    public Configuration parse(String yaml) throws IOException {
        if (yaml.codePointCount(0, yaml.length()) > options.codePointLimit()) {
            throw new IOException("YAML document exceeds the configured code-point limit: " + options.codePointLimit());
        }

        MemoryConfiguration result = new MemoryConfiguration(this);
        if (yaml.isBlank()) {
            return result;
        }

        try {
            DocumentConstructor constructor = new DocumentConstructor(loaderOptions());
            Yaml codec = yaml(constructor);
            Node node = codec.compose(new StringReader(yaml));

            if (node == null) {
                return result;
            }

            if (Tag.COMMENT.equals(node.getTag())) {
                result.setComments("", lines(node.getBlockComments()));
                result.setFooter(lines(node.getEndComments()));
                return result;
            }

            Object value = constructor.read(node);
            if (value == null) {
                readComments(node, "", result);
                result.setFooter(lines(node.getEndComments()));
                return result;
            }

            if (!(value instanceof Map<?, ?> map)) {
                throw new IOException("Configuration root must be a mapping");
            }

            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!(entry.getKey() instanceof String key)) {
                    throw new IOException("Configuration keys must be strings");
                }

                result.set(ConfigurationPath.key(key), entry.getValue());
            }

            readComments(node, "", result);
            result.setFooter(lines(node.getEndComments()));

            return result;
        } catch (RuntimeException e) {
            throw new IOException("Invalid YAML configuration", e);
        }
    }

    @Override
    public String serialize(Configuration configuration) throws IOException {
        if (!(configuration instanceof MemoryConfiguration memory)) {
            throw new IllegalArgumentException("Unsupported configuration implementation");
        }

        try {
            Yaml codec = yaml();
            Node node = codec.represent(memory.explicitValues());
            writeComments(node, "", configuration);
            node.setEndComments(commentLines(configuration.getFooter(), CommentType.BLOCK));

            StringWriter writer = new StringWriter();
            codec.serialize(node, writer);

            return writer.toString();
        } catch (RuntimeException e) {
            throw new IOException("Unable to serialize configuration", e);
        }
    }

    @Override
    public void save(Configuration configuration, Path path) throws IOException {
        AtomicFiles.write(path, serialize(configuration));
    }

    private <T> void registerBuiltin(String name, Class<T> type, Function<T, Map<String, Object>> encoder, Function<Map<String, Object>, T> decoder) {
        registerCodec(Identifier.parse("cloudburst:" + name), type, new ConfigurationCodec<>() {
            @Override
            public Map<String, Object> encode(T value) {
                return encoder.apply(value);
            }

            @Override
            public T decode(Map<String, Object> values) {
                return decoder.apply(values);
            }
        });
    }

    private LoaderOptions loaderOptions() {
        LoaderOptions loader = new LoaderOptions();
        loader.setAllowDuplicateKeys(false);
        loader.setProcessComments(options.parseComments());
        loader.setCodePointLimit(options.codePointLimit());
        loader.setMaxAliasesForCollections(options.maxAliases());
        loader.setNestingDepthLimit(options.nestingDepthLimit());

        return loader;
    }

    private Yaml yaml() {
        return yaml(new DocumentConstructor(loaderOptions()));
    }

    private Yaml yaml(DocumentConstructor constructor) {
        DumperOptions dumper = new DumperOptions();
        dumper.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        dumper.setIndent(options.indent());
        dumper.setWidth(options.width());
        dumper.setProcessComments(options.parseComments());

        return new Yaml(constructor, new Representer(dumper), dumper, constructor.options);
    }

    private static void readComments(Node node, String path, Configuration configuration) {
        configuration.setComments(path, lines(node.getBlockComments()));
        configuration.setInlineComments(path, lines(node.getInLineComments()));

        if (node instanceof MappingNode mapping) {
            for (NodeTuple tuple : mapping.getValue()) {
                String key = ((ScalarNode) tuple.getKeyNode()).getValue();
                String child = path.isEmpty() ? ConfigurationPath.key(key) : path + "." + ConfigurationPath.key(key);

                readComments(tuple.getValueNode(), child, configuration);
                configuration.setComments(child, lines(tuple.getKeyNode().getBlockComments()));

                if (tuple.getValueNode() instanceof MappingNode || tuple.getValueNode() instanceof SequenceNode) {
                    configuration.setInlineComments(child, lines(tuple.getKeyNode().getInLineComments()));
                }
            }
        } else if (node instanceof SequenceNode sequence) {
            for (int index = 0; index < sequence.getValue().size(); index++) {
                readComments(sequence.getValue().get(index), path + "[" + index + "]", configuration);
            }
        }
    }

    private static void writeComments(Node node, String path, Configuration configuration) {
        node.setBlockComments(commentLines(configuration.getComments(path), CommentType.BLOCK));
        node.setInLineComments(commentLines(configuration.getInlineComments(path), CommentType.IN_LINE));

        if (node instanceof MappingNode mapping) {
            for (NodeTuple tuple : mapping.getValue()) {
                String key = ((ScalarNode) tuple.getKeyNode()).getValue();
                String child = path.isEmpty() ? ConfigurationPath.key(key) : path + "." + ConfigurationPath.key(key);

                writeComments(tuple.getValueNode(), child, configuration);
                tuple.getValueNode().setBlockComments(null);
                tuple.getKeyNode().setBlockComments(commentLines(configuration.getComments(child), CommentType.BLOCK));

                if (tuple.getValueNode() instanceof MappingNode || tuple.getValueNode() instanceof SequenceNode) {
                    tuple.getKeyNode().setInLineComments(commentLines(configuration.getInlineComments(child), CommentType.IN_LINE));
                    tuple.getValueNode().setInLineComments(null);
                }
            }
        } else if (node instanceof SequenceNode sequence) {
            for (int index = 0; index < sequence.getValue().size(); index++) {
                writeComments(sequence.getValue().get(index), path + "[" + index + "]", configuration);
            }
        }
    }

    private static List<String> lines(List<CommentLine> comments) {
        List<String> lines = new ArrayList<>();
        if (comments != null) {
            for (CommentLine comment : comments) {
                String text = comment.getValue();
                lines.add(comment.getCommentType() == CommentType.BLANK_LINE ? null : text.startsWith(" ") ? text.substring(1) : text);
            }
        }

        return lines;
    }

    private static List<CommentLine> commentLines(List<String> lines, CommentType type) {
        List<CommentLine> comments = new ArrayList<>();
        for (String line : lines) {
            comments.add(new CommentLine(null, null, line == null || line.isEmpty() ? "" : " " + line, line == null ? CommentType.BLANK_LINE : type));
        }

        return comments;
    }

    private record CodecRegistration<T>(Identifier id, Class<T> type, ConfigurationCodec<T> codec) {

        private Map<String, Object> encode(Object value) {
            return Map.of("type", id.toString(), "data", codec.encode(type.cast(value)));
        }
    }

    private static class DocumentConstructor extends SafeConstructor {
        private final LoaderOptions options;

        private DocumentConstructor(LoaderOptions options) {
            super(options);
            this.options = options;
        }

        @Override
        protected void flattenMapping(MappingNode node) {
            super.flattenMapping(node, true);
        }

        @Override
        protected Construct getConstructor(Node node) {
            node.setUseClassConstructor(false);
            return super.getConstructor(node);
        }

        private Object read(Node node) {
            return constructDocument(node);
        }
    }
}
