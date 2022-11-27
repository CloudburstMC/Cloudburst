package org.cloudburstmc.server.registry;

import com.nukkitx.blockstateupdater.BlockStateUpdaters;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.nbt.NBTInputStream;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.nbt.NbtUtils;
import org.cloudburstmc.server.block.BlockPalette;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class BlockRegistryTest {

    @DisplayName("Block Palette Test")
    @Test
    public void blockPaletteTest() throws IOException {
        LinkedList<NbtMap> vanillaPalette;
        InputStream stream = BlockRegistryTest.class.getClassLoader().getResourceAsStream("data/block_palette.nbt");
        try (NBTInputStream nbtStream = NbtUtils.createGZIPReader(stream)) {
            NbtMap tag = (NbtMap) nbtStream.readTag();
            vanillaPalette = new LinkedList<>(tag.getList("blocks", NbtType.COMPOUND));
        } catch (IOException e) {
            throw new AssertionError("Unable to load block palette");
        }

        int version = BlockStateUpdaters.getLatestVersion();
        int paletteVersion = vanillaPalette.get(0).getInt("version");

        Assertions.assertTrue(version == paletteVersion, "Palette version missmatch");

        int major = (version >> 24) & 0xFF;
        int minor = (version >> 16) & 0xFF;
        int patch = (version >> 8) & 0xFF;
        int build = version & 0xFF;
        System.out.printf("Latest block state version: %d.%d.%d.%d%n", major, minor, patch, build);

        CloudBlockRegistry.REGISTRY.close(); // init

        Map<NbtMap, BlockState> serverPalette = BlockPalette.INSTANCE.getSerializedPalette();
        Map<Integer, BlockState> runtimeIdMap = BlockPalette.INSTANCE.getRuntimeMap();

       // Assertions.assertTrue(runtimeIdMap.size() == vanillaPalette.size(), "Palettes are not the same size");

        Path logPath = Paths.get("./logs");

        List<String> invalidStates = new ArrayList<>();
        int invalid = 0;
        for (NbtMap nbt : serverPalette.keySet()) {
            if (!vanillaPalette.remove(nbt)) {
                invalidStates.add(nbt.toString());
                invalid++;
            }
        }

        System.out.println("Found " + invalid + " invalid block states");
        if (invalid > 0) {
            Files.write(logPath.resolve("invalid_states.log"), invalidStates, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
        }

        List<String> missingStates = new ArrayList<>();
        int missing = 0;
        for (NbtMap state : vanillaPalette) {
            missing++;
            missingStates.add(state.toString());
        }

        System.out.println("Found " + missing + " missing block states");

        if (missing > 0) {
            Files.write(logPath.resolve("missing_states.log"), missingStates, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
        }

        Assertions.assertFalse(missing > 0 | invalid > 0, "One or more block states did not match the vanilla palette");
    }
}
