package org.cloudburstmc.api.entity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.api.item.ItemStack;

/**
 * Persistent data of an arrow: abstract-arrow data plus its item
 * and potion ground ticks. The item is set when the entity initializes;
 * a data instance without one writes no item, as before.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class ArrowData extends AbstractArrowData {

    @JsonProperty("Item")
    private ItemStack item;

    @JsonProperty("PotionGroundTicks")
    private int potionGroundTicks;

    public ArrowData() {
    }
}
