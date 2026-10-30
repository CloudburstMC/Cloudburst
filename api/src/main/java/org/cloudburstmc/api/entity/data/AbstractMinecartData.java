package org.cloudburstmc.api.entity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.api.block.BlockState;

/**
 * Persistent data of an abstract minecart: everything shared plus its display
 * block offset and optional custom display block.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class AbstractMinecartData extends EntityData {

    @JsonProperty("DisplayOffset")
    private Integer displayOffset;

    @JsonProperty("CustomDisplayTile")
    private Boolean customDisplayTile;

    @JsonProperty("DisplayBlock")
    private BlockState displayBlock;

    public AbstractMinecartData() {
    }
}
