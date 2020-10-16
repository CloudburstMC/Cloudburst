package org.cloudburstmc.server.registry;

import com.nukkitx.blockstateupdater.BlockStateUpdaters;
import com.nukkitx.nbt.NBTInputStream;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtType;
import com.nukkitx.nbt.NbtUtils;
import org.cloudburstmc.server.block.BlockPalette;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class BlockRegistryTest {

    @DisplayName("Block Palette Test")
    @Test
    public void blockPaletteTest() throws IOException {
        Set<NbtMap> vanillaPalette;
        InputStream stream = BlockRegistryTest.class.getClassLoader().getResourceAsStream("block_palette.nbt");
        try (NBTInputStream nbtStream = NbtUtils.createGZIPReader(stream)) {
            NbtMap tag = (NbtMap) nbtStream.readTag();
            vanillaPalette = new HashSet<>(tag.getList("blocks", NbtType.COMPOUND));
        } catch (IOException e) {
            throw new AssertionError("Unable to load block palette");
        }

        int version = BlockStateUpdaters.getLatestVersion();
        int major = (version >> 24) & 0xFF;
        int minor = (version >> 16) & 0xFF;
        int patch = (version >> 8) & 0xFF;
        int build = version & 0xFF;
        System.out.printf("Latest block state version: %d.%d.%d.%d%n", major, minor, patch, build);

        BlockRegistry.get(); // init
        Collection<NbtMap> serverPalette = BlockPalette.INSTANCE.getSerializedPalette().keySet();

        List<String> invalidStates = new ArrayList<>();
        int invalid = 0;
        for (NbtMap state : serverPalette) {
            if (!vanillaPalette.remove(state)) {
                invalidStates.add(state.toString());
                invalid++;
            }
        }

        System.out.println("Found " + invalid + " invalid block states");
        Files.write(Paths.get("invalid_states.log"), invalidStates, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);

        List<String> missingStates = new ArrayList<>();
        int missing = 0;
        for (NbtMap state : vanillaPalette) {
            missing++;
            missingStates.add(state.toString());
        }

        System.out.println("Found " + missing + " missing block states");
        Files.write(Paths.get("missing_states.log"), missingStates, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);

        Assertions.assertFalse(missing > 0 | invalid > 0, "One or more block states did not match the vanilla palette");
    }
}
