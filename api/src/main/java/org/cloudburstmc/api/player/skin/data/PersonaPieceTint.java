package org.cloudburstmc.api.player.skin.data;

import com.google.common.collect.ImmutableList;
import lombok.Data;

import java.util.List;

@Data
public class PersonaPieceTint {
    private final String pieceType;
    private final ImmutableList<String> colors;

    public PersonaPieceTint(String pieceType, List<String> colors) {
        this.pieceType = pieceType;
        this.colors = ImmutableList.copyOf(colors);
    }
}
