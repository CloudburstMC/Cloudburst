package org.cloudburstmc.api.entity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.api.item.ItemStack;

/**
 * Persistent data of a dropped item: everything shared plus health, pickup delay,
 * age, owner and the item itself.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class DroppedItemData extends EntityData {

    @JsonProperty("Health")
    private Short health;

    @JsonProperty("PickupDelay")
    private Short pickupDelay;

    @JsonProperty("Age")
    private Short age;

    @JsonProperty("OwnerID")
    private Long ownerId;

    @JsonProperty("Item")
    private ItemStack item;

    public DroppedItemData() {
    }
}
