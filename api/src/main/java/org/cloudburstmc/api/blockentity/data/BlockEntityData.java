package org.cloudburstmc.api.blockentity.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

/**
 * Persistent data shared by every block entity.
 */
@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, isGetterVisibility = NONE, setterVisibility = NONE)
@Getter
@Setter
public class BlockEntityData {

    @JsonProperty("isMovable")
    private boolean movable = true;

    @JsonProperty("CustomName")
    private String customName;

    public BlockEntityData() {
    }
}
