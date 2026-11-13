package org.cloudburstmc.server.blockentity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.cloudburstmc.server.nbt.client.NbtClient;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class CloudSignData extends CloudBlockEntityData {

    @NbtClient
    @JsonProperty("Text")
    private String text = "\n\n\n";

    @NbtClient
    @JsonProperty("TextOwner")
    private String textOwner = "";
}
