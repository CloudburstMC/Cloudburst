package org.cloudburstmc.server.registry;

import com.nukkitx.nbt.NBTInputStream;
import com.nukkitx.nbt.NbtList;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

public class BlockRegistryTest {

    @DisplayName("Block Palette Test")
    @Test
    public void blockPaletteTest() {
        Set<NbtMap> vanillaPalette;
        InputStream stream = BlockRegistryTest.class.getClassLoader().getResourceAsStream("runtime_block_states.dat");
        try (NBTInputStream nbtStream = NbtUtils.createNetworkReader(stream)) {
            NbtList<NbtMap> tag = (NbtList<NbtMap>) nbtStream.readTag();
            vanillaPalette = new HashSet<>(tag);
        } catch (IOException e) {
            throw new AssertionError("Unable to load block palette");
        }

        NbtList<NbtMap> serverPalette = BlockRegistry.get().getPaletteTag(); // init

        boolean failed = false;
        for (NbtMap state : serverPalette) {
            if (!vanillaPalette.remove(state)) {
                System.out.println("State does not exist in vanilla palette:\n" + state.toString());
                failed = true;
            }
        }

        for (NbtMap state : vanillaPalette) {
            System.out.println("State does not exist in Cloudburst:\n" + state.toString());
            failed = true;
        }

        Assertions.assertFalse(failed, "One or more block states did not match the vanilla palette");
    }
}
