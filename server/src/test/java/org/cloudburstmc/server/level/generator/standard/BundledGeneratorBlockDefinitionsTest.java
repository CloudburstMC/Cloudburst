package org.cloudburstmc.server.level.generator.standard;

import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.server.level.generator.GeneratorConfigurationMapper;
import org.cloudburstmc.server.level.generator.standard.misc.filter.BlockFilter;
import org.cloudburstmc.server.level.generator.standard.misc.selector.BlockSelector;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class BundledGeneratorBlockDefinitionsTest {

    private static final CloudBlockRegistry REGISTRY = BlockTestRegistry.get();

    @Test
    void everyBundledBlockSelectionDecodesWithTheGeneratorMapper() throws Exception {
        Path root = Path.of(Objects.requireNonNull(getClass().getClassLoader().getResource("generator")).toURI());
        List<Path> definitions;
        try (Stream<Path> paths = Files.walk(root)) {
            definitions = paths.filter(path -> path.toString().endsWith(".yml")).sorted().toList();
        }

        assertFalse(definitions.isEmpty());
        int selections = 0;
        for (Path definition : definitions) {
            try (InputStream input = Files.newInputStream(definition)) {
                Object document = GeneratorConfigurationMapper.MAPPER.readValue(input, Object.class);
                selections += validateSelections(document, root.relativize(definition).toString());
            }
        }

        assertTrue(selections > 0);
    }

    @Test
    void explicitListsPreserveReplacementStatesAndSelectionWeights() {
        BlockFilter filter = GeneratorConfigurationMapper.MAPPER.convertValue(
                List.of("dirt", "grass_block"), BlockFilter.class);
        BlockSelector selector = GeneratorConfigurationMapper.MAPPER.convertValue(
                List.of("5*dirt", "4*gravel"), BlockSelector.class);

        assertTrue(filter.test(REGISTRY.getBlock(BlockTypes.DIRT)));
        assertTrue(filter.test(REGISTRY.getBlock(BlockTypes.GRASS_BLOCK)));
        assertFalse(filter.test(REGISTRY.getBlock(BlockTypes.STONE)));
        assertEquals(2, selector.size());
        assertEquals(9, selector.sizeWeighted());
        assertEquals(List.of(5, 4), selector.entries().map(BlockSelector.Entry::weight).toList());
    }

    private static int validateSelections(Object value, String path) {
        int selections = 0;
        if (value instanceof Map<?, ?> fields) {
            for (Map.Entry<?, ?> field : fields.entrySet()) {
                String name = field.getKey().toString();
                String fieldPath = path + '.' + name;
                Class<?> target = switch (name) {
                    case "block" -> BlockSelector.class;
                    case "replace", "on", "surfaceBlocks" -> BlockFilter.class;
                    default -> null;
                };

                if (target != null) {
                    assertDoesNotThrow(() -> {
                        GeneratorConfigurationMapper.MAPPER.convertValue(field.getValue(), target);
                    }, fieldPath);
                    selections++;
                } else {
                    selections += validateSelections(field.getValue(), fieldPath);
                }
            }
        } else if (value instanceof List<?> elements) {
            for (int index = 0; index < elements.size(); index++) {
                selections += validateSelections(elements.get(index), path + '[' + index + ']');
            }
        }

        return selections;
    }
}
