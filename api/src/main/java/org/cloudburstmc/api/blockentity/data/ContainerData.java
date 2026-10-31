package org.cloudburstmc.api.blockentity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * Persistent data shared by every loot-capable item container: an optional loot-table
 * reference. Each concrete container DTO declares its own items property with the slot
 * count of its block-entity type.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public abstract class ContainerData extends BlockEntityData implements LootTableHolder {

    @JsonProperty("LootTable")
    private String lootTable;

    @JsonProperty("LootTableSeed")
    private Integer lootTableSeed;

    protected ContainerData() {
    }
}
