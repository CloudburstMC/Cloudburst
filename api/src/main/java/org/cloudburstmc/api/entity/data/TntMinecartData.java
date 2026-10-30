package org.cloudburstmc.api.entity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * Persistent data of a TNT minecart: abstract-minecart data plus its fuse.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class TntMinecartData extends AbstractMinecartData {

    @JsonProperty("Fuse")
    private Integer fuse;

    public TntMinecartData() {
    }
}
