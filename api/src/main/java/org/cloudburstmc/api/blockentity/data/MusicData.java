package org.cloudburstmc.api.blockentity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * Persistent data of a music (note) block: its note and powered state.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class MusicData extends BlockEntityData {

    @JsonProperty("note")
    private byte note;

    @JsonProperty("powered")
    private boolean powered;

    public MusicData() {
    }
}
