package org.cloudburstmc.api.blockentity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * Persistent data of a comparator: its output signal.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class ComparatorData extends BlockEntityData {

    @JsonProperty("OutputSignal")
    private int outputSignal;

    public ComparatorData() {
    }
}
