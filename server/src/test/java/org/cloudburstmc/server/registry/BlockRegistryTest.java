package org.cloudburstmc.server.registry;

import org.cloudburstmc.api.block.BlockComponents;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.data.ComponentType;
import org.cloudburstmc.api.util.component.ComponentMap;
import org.cloudburstmc.nbt.*;
import org.cloudburstmc.server.block.BlockPalette;
import org.cloudburstmc.server.block.component.AnvilPlaceHandler;
import org.cloudburstmc.server.block.component.ContainerBlockHandlers;
import org.cloudburstmc.server.block.component.DefaultBlockHandlers;
import org.cloudburstmc.server.block.component.SlabPlaceHandler;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BlockRegistryTest {
    private static final CloudBlockRegistry REGISTRY = createRegistry();

    @Test
    void serializedPaletteMatchesVanillaPalette() throws IOException {
        LinkedList<NbtMap> vanillaPalette;
        InputStream stream = Objects.requireNonNull(
                BlockRegistryTest.class.getClassLoader().getResourceAsStream("data/block_palette.nbt"),
                "Missing vanilla block palette"
        );

        try (NBTInputStream nbtStream = NbtUtils.createGZIPReader(stream)) {
            NbtMap tag = (NbtMap) nbtStream.readTag();
            vanillaPalette = tag.getList("blocks", NbtType.COMPOUND).stream()
                    .map(BlockRegistryTest::stripRuntimeOnlyTags)
                    .collect(LinkedList::new, LinkedList::add, LinkedList::addAll);
        }

        Set<NbtMap> serializedStates = BlockPalette.INSTANCE.getSerializedPalette().keySet();
        List<NbtMap> missingStates = vanillaPalette.stream()
                .filter(state -> !serializedStates.contains(state))
                .toList();

        assertAll(
                () -> assertEquals(vanillaPalette.size(), BlockPalette.INSTANCE.getRuntimeMap().size(),
                        "Every vanilla state must have one runtime definition"),
                () -> assertTrue(missingStates.isEmpty(),
                        () -> missingStates.size() + " vanilla states are absent from the serialized palette: "
                                + missingStates.stream().limit(5).toList())
        );
    }

    @Test
    void configuresSpecializedVanillaBlockBehaviors() {
        assertAll(
                () -> assertPlaceHandler(BlockTypes.ANVIL, AnvilPlaceHandler.class),
                () -> assertPlaceHandler(BlockTypes.BAMBOO_MOSAIC_SLAB, SlabPlaceHandler.class),
                () -> assertSame(
                        DefaultBlockHandlers.CAN_BE_USED,
                        component(BlockTypes.ENCHANTING_TABLE, BlockComponents.CAN_BE_USED)),
                () -> assertSame(
                        ContainerBlockHandlers.ENCHANTING_TABLE,
                        component(BlockTypes.ENCHANTING_TABLE, BlockComponents.USE)),
                () -> assertSame(
                        DefaultBlockHandlers.CAN_BE_USED,
                        component(BlockTypes.ENDER_CHEST, BlockComponents.CAN_BE_USED)),
                () -> assertSame(
                        ContainerBlockHandlers.ENDER_CHEST,
                        component(BlockTypes.ENDER_CHEST, BlockComponents.USE)),
                () -> assertPlaceHandler(BlockTypes.GRANITE_SLAB, SlabPlaceHandler.class),
                () -> assertPlaceHandler(BlockTypes.MOSSY_STONE_BRICK_SLAB, SlabPlaceHandler.class)
        );
    }

    private static void assertPlaceHandler(BlockType blockType, Class<?> expectedType) {
        assertInstanceOf(expectedType, component(blockType, BlockComponents.ON_PLACE));
    }

    private static Object component(BlockType blockType, ComponentType<?> componentType) {
        ComponentMap components = Objects.requireNonNull(
                REGISTRY.getComponents(blockType),
                () -> blockType.getId() + " has no component map");
        return Objects.requireNonNull(
                components.get(componentType),
                () -> blockType.getId() + " has no " + componentType.getId() + " component");
    }

    private static CloudBlockRegistry createRegistry() {
        CloudBlockRegistry registry = new CloudBlockRegistry(CloudItemRegistry.get());
        registry.close();
        return registry;
    }

    private static NbtMap stripRuntimeOnlyTags(NbtMap state) {
        NbtMapBuilder builder = state.toBuilder();
        builder.remove("version");
        builder.remove("name_hash");
        builder.remove("network_id");
        builder.remove("block_id");
        return builder.build();
    }
}
