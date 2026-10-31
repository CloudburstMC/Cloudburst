package org.cloudburstmc.api.blockentity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.api.nbt.NbtClient;

/**
 * Persistent data of a sign: the joined text lines and the text owner.
 * A cleared sign saves its empty lines joined with newlines, like today's save code.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class SignData extends BlockEntityData {

    @NbtClient
    @JsonProperty("Text")
    private String text = "\n\n\n";

    @NbtClient
    @JsonProperty("TextOwner")
    private String textOwner = "";

    public SignData() {
    }
}
