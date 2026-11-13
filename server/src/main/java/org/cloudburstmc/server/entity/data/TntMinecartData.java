package org.cloudburstmc.server.entity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Persistent data of a TNT minecart: abstract-minecart data plus its fuse.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class TntMinecartData extends AbstractMinecartData {

    @JsonProperty("Fuse")
    private int fuse = -1;
}
