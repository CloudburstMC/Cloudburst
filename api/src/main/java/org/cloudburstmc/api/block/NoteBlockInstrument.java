package org.cloudburstmc.api.block;

/**
 * An instrument playable by a note block.
 */
public enum NoteBlockInstrument {
    HARP, BASEDRUM, SNARE, HAT, BASS, FLUTE, BELL, GUITAR, CHIME, XYLOPHONE, IRON_XYLOPHONE,
    COW_BELL, DIDGERIDOO, BIT, BANJO, PLING, TRUMPET, TRUMPET_EXPOSED, TRUMPET_OXIDIZED, TRUMPET_WEATHERED,
    ZOMBIE, SKELETON, CREEPER, DRAGON, WITHER_SKELETON, PIGLIN, CUSTOM_HEAD;

    /**
     * Returns whether the instrument plays a pitched note rather than a head sound.
     */
    public boolean isTunable() {
        return switch (this) {
            case ZOMBIE, SKELETON, CREEPER, DRAGON, WITHER_SKELETON, PIGLIN, CUSTOM_HEAD -> false;
            default -> true;
        };
    }
}
