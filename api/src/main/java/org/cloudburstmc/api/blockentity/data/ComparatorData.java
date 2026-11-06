package org.cloudburstmc.api.blockentity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Persistent data of a comparator: its output signal.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ComparatorData extends BlockEntityData {

    @JsonProperty("OutputSignal")
    private int outputSignal;
}
