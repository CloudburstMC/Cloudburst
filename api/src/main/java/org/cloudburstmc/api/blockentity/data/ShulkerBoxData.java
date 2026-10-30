package org.cloudburstmc.api.blockentity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.api.nbt.NbtClient;

/**
 * Persistent data of a shulker box: its container plus the facing the lid opens towards.
 * The facing is sent to the client so it can render the open lid.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class ShulkerBoxData extends ContainerData {

    @NbtClient
    @JsonProperty("facing")
    private byte facing;

    public ShulkerBoxData() {
    }
}
