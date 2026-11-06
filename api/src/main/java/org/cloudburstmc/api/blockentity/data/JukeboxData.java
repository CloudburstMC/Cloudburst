package org.cloudburstmc.api.blockentity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.cloudburstmc.api.item.ItemStack;

/**
 * Persistent data of a jukebox: the record it holds, if any.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class JukeboxData extends BlockEntityData {

    @JsonProperty("RecordItem")
    private ItemStack recordItem;
}
