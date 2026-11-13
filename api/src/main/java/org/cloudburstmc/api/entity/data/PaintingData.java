package org.cloudburstmc.api.entity.data;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.cloudburstmc.api.entity.misc.PaintingVariant;

/**
 * Persistent data of a painting: hanging data plus its motive.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class PaintingData extends EntityData {

    @JsonIgnore
    private PaintingVariant variant = PaintingVariant.KEBAB;
}
