package org.cloudburstmc.server.blockentity.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.inventory.ItemContainer;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.nbt.NbtTagType;
import org.cloudburstmc.server.nbt.annotation.NbtContainerSize;
import org.cloudburstmc.server.nbt.annotation.NbtTag;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class CloudChestData extends CloudContainerData {

    public static final int SIZE = 27;

    @JsonProperty("Items")
    @NbtTag(value = NbtTagType.LIST, element = NbtTagType.COMPOUND)
    @NbtContainerSize(SIZE)
    private ItemContainer items = new CloudContainer(SIZE);

    @JsonProperty("Findable")
    private boolean findable;

    @JsonIgnore
    private @Nullable Vector3i pairPosition;

    @JsonIgnore
    private boolean pairLead;
}
