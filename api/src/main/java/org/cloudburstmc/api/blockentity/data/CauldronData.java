package org.cloudburstmc.api.blockentity.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.api.blockentity.CauldronContents;

/**
 * Persistent data of a cauldron: its potion or dye contents. The network potion
 * and form ids, the custom color, the item list and the stale splash flag travel
 * outside this class, written by the load and save hooks.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class CauldronData extends BlockEntityData {

    @JsonIgnore
    private CauldronContents contents = CauldronContents.PLAIN;

    public CauldronData() {
    }
}
