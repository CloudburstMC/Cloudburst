package org.cloudburstmc.server.entity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.cloudburstmc.api.item.ItemStack;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ArrowData extends AbstractArrowData {

    @JsonProperty("Item")
    private ItemStack item;

    @JsonProperty("PotionGroundTicks")
    private int potionGroundTicks;
}
