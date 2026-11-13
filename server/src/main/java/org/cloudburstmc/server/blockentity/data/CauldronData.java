package org.cloudburstmc.server.blockentity.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.cloudburstmc.api.blockentity.CauldronContents;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class CauldronData extends BlockEntityData {

    @JsonIgnore
    private CauldronContents contents = CauldronContents.PLAIN;
}
