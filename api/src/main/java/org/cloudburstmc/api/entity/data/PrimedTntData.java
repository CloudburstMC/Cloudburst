package org.cloudburstmc.api.entity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * Persistent data of a primed TNT: everything shared plus its fuse.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class PrimedTntData extends EntityData {

    @JsonProperty("Fuse")
    private Integer fuse;

    public PrimedTntData() {
    }
}
