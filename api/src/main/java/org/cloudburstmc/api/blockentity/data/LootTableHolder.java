package org.cloudburstmc.api.blockentity.data;

/**
 * Persistent loot-table reference held by loot containers.
 */
public interface LootTableHolder {

    String getLootTable();

    void setLootTable(String lootTable);

    Integer getLootTableSeed();

    void setLootTableSeed(Integer lootTableSeed);
}
