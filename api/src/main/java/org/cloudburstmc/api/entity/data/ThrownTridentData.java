package org.cloudburstmc.api.entity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.api.item.ItemStack;

/**
 * Persistent data of a thrown trident: abstract-arrow data plus its trident
 * item and whether it already dealt damage.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class ThrownTridentData extends AbstractArrowData {

    @JsonProperty("Trident")
    private ItemStack trident;

    @JsonProperty("DealtDamage")
    private Boolean dealtDamage;

    public ThrownTridentData() {
    }
}
