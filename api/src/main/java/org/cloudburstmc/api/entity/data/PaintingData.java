package org.cloudburstmc.api.entity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * Persistent data of a painting: hanging data plus its motive.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class PaintingData extends HangingData {

    @JsonProperty("Motive")
    private String motive;

    public PaintingData() {
    }
}
