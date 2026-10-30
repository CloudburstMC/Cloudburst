package org.cloudburstmc.api.entity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * Persistent data of a tameable animal: everything shared plus owner and sitting state.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class TameableData extends CreatureData {

    @JsonProperty("OwnerID")
    private Long ownerId;

    @JsonProperty("Sitting")
    private Boolean sitting;

    public TameableData() {
    }
}
