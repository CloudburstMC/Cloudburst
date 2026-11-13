package org.cloudburstmc.server.blockentity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * Loot table reference and seed shared by loot-capable containers.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class CloudContainerData extends CloudBlockEntityData implements LootTableHolder {

    @JsonProperty("LootTable")
    private String lootTable;

    @JsonProperty("LootTableSeed")
    private Integer lootTableSeed;
}
