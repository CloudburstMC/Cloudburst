package org.cloudburstmc.server.configuration;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.configuration.Configuration;
import org.cloudburstmc.api.configuration.YamlConfigurationOptions;
import org.cloudburstmc.api.item.ItemDataComponentType;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.configuration.codec.BlockStateConfigurationCodec;
import org.cloudburstmc.server.configuration.codec.ConfigurationResolvers;
import org.cloudburstmc.server.configuration.file.YamlConfigurationLoader;
import org.cloudburstmc.server.registry.CloudItemRegistry;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigurationCodecTest {

    private static final ItemDataComponentType<Integer> COUNTER = ItemDataComponentType.value(
            Identifier.parse("configuration_test:counter"), Integer.class);

    @Test
    void blockStateRoundTripResolvesTheTypeAndRestoresTraits() {
        BlockState original = BlockTypes.OAK_LOG.getDefaultState().withTrait(BlockTraits.AXIS, Direction.Axis.X);
        Map<Identifier, BlockState> blocks = Map.of(BlockTypes.OAK_LOG.getId(), BlockTypes.OAK_LOG.getDefaultState());
        BlockStateConfigurationCodec codec = new BlockStateConfigurationCodec(blocks::get);

        assertEquals(original, codec.decode(codec.encode(original)));
    }

    @Test
    void blockStateDecodingRejectsUnknownBlocks() {
        BlockStateConfigurationCodec codec = new BlockStateConfigurationCodec(id -> null);
        assertThrows(IllegalArgumentException.class, () -> codec.decode(Map.of("block", "test:missing", "traits", Map.of())));
    }

    @Test
    void blockStateDecodingRejectsInvalidEnumAndBooleanValues() {
        Map<Identifier, BlockState> blocks = Map.of(
                BlockTypes.OAK_LOG.getId(), BlockTypes.OAK_LOG.getDefaultState(),
                BlockTypes.OAK_DOOR.getId(), BlockTypes.OAK_DOOR.getDefaultState());
        BlockStateConfigurationCodec codec = new BlockStateConfigurationCodec(blocks::get);

        assertThrows(IllegalArgumentException.class, () -> codec.decode(Map.of(
                "block", BlockTypes.OAK_LOG.getId().toString(),
                "traits", Map.of(BlockTraits.AXIS.getName().toString(), "invalid"))));
        assertThrows(IllegalArgumentException.class, () -> codec.decode(Map.of(
                "block", BlockTypes.OAK_DOOR.getId().toString(),
                "traits", Map.of(BlockTraits.IS_OPEN.getName().toString(), "invalid"))));
    }

    @Test
    void numericCodecsRejectLossyOrNonfiniteInput() {
        YamlConfigurationLoader loader = new YamlConfigurationLoader();

        assertThrows(IllegalArgumentException.class, () -> loader.decode(encoded("integer", Map.of("value", 1.5)), Integer.class));
        assertThrows(IllegalArgumentException.class, () -> loader.decode(encoded("integer", Map.of("value", Long.MAX_VALUE)), Integer.class));
        assertThrows(IllegalArgumentException.class, () -> loader.decode(encoded("long", Map.of("value", new BigInteger("9223372036854775808"))), Long.class));
        assertThrows(IllegalArgumentException.class, () -> loader.decode(encoded("float", Map.of("value", Double.MAX_VALUE)), Float.class));
        assertThrows(IllegalArgumentException.class, () -> loader.decode(encoded("double", Map.of("value", Double.NaN)), Double.class));
        assertThrows(IllegalArgumentException.class, () -> loader.decode(encoded("vector3i", Map.of("x", 0, "y", 1.5, "z", 0)), Vector3i.class));
        assertThrows(IllegalArgumentException.class, () -> loader.encode(Float.POSITIVE_INFINITY));

        assertEquals(Integer.MAX_VALUE, loader.decode(encoded("integer", Map.of("value", Integer.MAX_VALUE)), Integer.class));
        assertEquals(Long.MAX_VALUE, loader.decode(encoded("long", Map.of("value", Long.MAX_VALUE)), Long.class));
        assertEquals(Float.floatToIntBits(-0.0f), Float.floatToIntBits(loader.decode(loader.encode(-0.0f), Float.class)));
    }

    @Test
    void itemRoundTripPreservesCountAndExplicitComponents() throws IOException {
        assertEquals(ItemTypes.STICK, CloudItemRegistry.get().get(ItemTypes.STICK.getId()).orElseThrow());
        YamlConfigurationLoader loader = new YamlConfigurationLoader();
        ItemStack original = ItemStack.builder().itemType(ItemTypes.STICK).amount(200)
                .setData(ItemDataComponents.CUSTOM_LORE, List.of("First line", "Second line"))
                .setData(COUNTER, 17)
                .build();
        Configuration configuration = loader.create();
        configuration.set("reward", loader.encode(original));
        Configuration reloaded = loader.parse(loader.serialize(configuration));

        assertEquals(original, loader.decode(requireNonNull(reloaded.getConfigurationSection("reward")).getValues(false), ItemStack.class));
    }

    @Test
    void emptyItemRoundTrips() {
        YamlConfigurationLoader loader = new YamlConfigurationLoader();
        assertEquals(ItemStack.EMPTY, loader.decode(loader.encode(ItemStack.EMPTY), ItemStack.class));
    }

    @Test
    void locationRoundTripResolvesTheLoadedLevel() {
        Level level = InterfaceProxy.create(Level.class, Map.of("getId", "example"));
        YamlConfigurationLoader loader = new YamlConfigurationLoader(new ConfigurationResolvers(id -> id.equals("example") ? level : null, id -> null, id -> null, name -> null));
        Location original = Location.from(1.25f, 72, -3.5f, 90, -30, level);

        assertEquals(original, loader.decode(loader.encode(original), Location.class));
        assertEquals(original, loader.withOptions(new YamlConfigurationOptions()).decode(loader.encode(original), Location.class));
    }

    @Test
    void unavailableLevelsAndNonfiniteCoordinatesAreRejected() {
        Level level = InterfaceProxy.create(Level.class, Map.of("getId", "unavailable"));
        YamlConfigurationLoader loader = new YamlConfigurationLoader();

        assertThrows(IllegalArgumentException.class, () -> loader.decode(loader.encode(Location.from(level)), Location.class));
        assertThrows(IllegalArgumentException.class, () -> loader.encode(Location.from(Float.NaN, 0, 0, level)));
    }

    private static Map<String, Object> encoded(String type, Map<String, Object> data) {
        return Map.of("type", "cloudburst:" + type, "data", data);
    }
}
