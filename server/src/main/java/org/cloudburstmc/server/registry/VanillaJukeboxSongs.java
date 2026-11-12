package org.cloudburstmc.server.registry;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.item.ItemBehaviors;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.item.data.JukeboxSong;
import org.cloudburstmc.api.level.sound.SoundType;

import static org.cloudburstmc.api.item.ItemTypes.*;
import static org.cloudburstmc.api.level.sound.SoundTypes.*;

@UtilityClass
public class VanillaJukeboxSongs {

    public static void configure(CloudItemRegistry registry) {
        register(registry, MUSIC_DISC_13, RECORD_13, 178, 1);
        register(registry, MUSIC_DISC_CAT, RECORD_CAT, 185, 2);
        register(registry, MUSIC_DISC_BLOCKS, RECORD_BLOCKS, 345, 3);
        register(registry, MUSIC_DISC_CHIRP, RECORD_CHIRP, 185, 4);
        register(registry, MUSIC_DISC_FAR, RECORD_FAR, 174, 5);
        register(registry, MUSIC_DISC_MALL, RECORD_MALL, 197, 6);
        register(registry, MUSIC_DISC_MELLOHI, RECORD_MELLOHI, 96, 7);
        register(registry, MUSIC_DISC_STAL, RECORD_STAL, 150, 8);
        register(registry, MUSIC_DISC_STRAD, RECORD_STRAD, 188, 9);
        register(registry, MUSIC_DISC_WARD, RECORD_WARD, 251, 10);
        register(registry, MUSIC_DISC_11, RECORD_11, 71, 11);
        register(registry, MUSIC_DISC_WAIT, RECORD_WAIT, 238, 12);
        register(registry, MUSIC_DISC_PIGSTEP, RECORD_PIGSTEP, 149, 13);
        register(registry, MUSIC_DISC_OTHERSIDE, RECORD_OTHERSIDE, 195, 14);
        register(registry, MUSIC_DISC_5, RECORD_5, 178, 15);
        register(registry, MUSIC_DISC_RELIC, RECORD_RELIC, 218, 14);
        register(registry, MUSIC_DISC_PRECIPICE, RECORD_PRECIPICE, 299, 13);
        register(registry, MUSIC_DISC_CREATOR, RECORD_CREATOR, 176, 12);
        register(registry, MUSIC_DISC_CREATOR_MUSIC_BOX, RECORD_CREATOR_MUSIC_BOX, 73, 11);
        register(registry, MUSIC_DISC_TEARS, RECORD_TEARS, 175, 10);
        register(registry, MUSIC_DISC_LAVA_CHICKEN, RECORD_LAVA_CHICKEN, 134, 9);
        register(registry, MUSIC_DISC_BOUNCE, RECORD_BOUNCE, 234, 8);
    }

    private static void register(CloudItemRegistry registry, ItemType item, SoundType sound, int seconds, int signal) {
        registry.configure(item).set(ItemBehaviors.JUKEBOX_PLAYABLE, new JukeboxSong(sound, seconds * 20, signal));
    }
}
