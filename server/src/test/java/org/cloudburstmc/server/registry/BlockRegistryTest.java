package org.cloudburstmc.server.registry;

import org.cloudburstmc.nbt.NBTInputStream;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.nbt.NbtUtils;
import org.cloudburstmc.server.block.BlockPalette;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BlockRegistryTest {

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

        CloudBlockRegistry registry = new CloudBlockRegistry(CloudItemRegistry.get());
        registry.close();

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

    private static NbtMap stripRuntimeOnlyTags(NbtMap state) {
        var builder = state.toBuilder();
        builder.remove("version");
        builder.remove("name_hash");
        builder.remove("network_id");
        builder.remove("block_id");
        return builder.build();
    }
}
