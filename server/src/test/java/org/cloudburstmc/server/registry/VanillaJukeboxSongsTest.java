package org.cloudburstmc.server.registry;

import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.item.data.JukeboxSong;
import org.cloudburstmc.api.level.sound.SoundTypes;
import org.cloudburstmc.server.blockentity.JukeboxBlockEntity;
import org.cloudburstmc.server.testutil.BlockTestRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VanillaJukeboxSongsTest {

    @Test
    void registeredDiscsHaveSongsWithoutLegacyItemData() {
        BlockTestRegistry.ensureInitialized();
        int discs = 0;
        for (ItemType type : ItemTypes.values()) {
            if (type.getId().getName().startsWith("music_disc_")) {
                assertNotNull(JukeboxBlockEntity.songFor(ItemStack.from(type)), type.getId().toString());
                discs++;
            }
        }

        assertEquals(22, discs);
        assertEquals(new JukeboxSong(SoundTypes.RECORD_5, 178 * 20, 15),
                JukeboxBlockEntity.songFor(ItemStack.from(ItemTypes.MUSIC_DISC_5)));
        assertNull(JukeboxBlockEntity.songFor(ItemStack.from(ItemTypes.DISC_FRAGMENT_5)));
    }
}
