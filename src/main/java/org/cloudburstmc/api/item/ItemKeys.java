package org.cloudburstmc.api.item;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.data.DataKey;
import org.cloudburstmc.api.data.ListDataKey;
import org.cloudburstmc.api.data.MapDataKey;
import org.cloudburstmc.api.data.SimpleDataKey;
import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.enchantment.EnchantmentType;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.item.data.Bucket;
import org.cloudburstmc.api.item.data.Record;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.data.DyeColor;
import org.cloudburstmc.api.util.data.FireworkData;
import org.cloudburstmc.api.util.data.TreeSpecies;

public final class ItemKeys {

    public static final SimpleDataKey<BlockState> BLOCK_STATE = DataKey.simple(Identifier.fromString("block_state"), BlockState.class);

    public static final SimpleDataKey<String> CUSTOM_NAME = DataKey.simple(Identifier.fromString("custom_name"), String.class);

    public static final ListDataKey<String> CUSTOM_LORE = DataKey.list(Identifier.fromString("custom_lore"), String.class);

    public static final SimpleDataKey<Integer> DAMAGE = DataKey.simple(Identifier.fromString("item_damage"), Integer.class);

    public static final SimpleDataKey<Boolean> UNBREAKABLE = DataKey.simple(Identifier.fromString("unbreakable"), Boolean.class);

    public static final SimpleDataKey<DyeColor> COLOR = DataKey.simple(Identifier.fromString("dye_color"), DyeColor.class);

    public static final MapDataKey<EnchantmentType, Enchantment> ENCHANTMENTS = DataKey.map(Identifier.fromString("enchantments"), EnchantmentType.class, Enchantment.class);

    public static final SimpleDataKey<FireworkData> FIREWORK_DATA = DataKey.simple(Identifier.fromString("firework_data"), FireworkData.class);

    public static final SimpleDataKey<Bucket> BUCKET_DATA = DataKey.simple(Identifier.fromString("bucket_data"), Bucket.class);

    public static final ListDataKey<BlockType> CAN_DESTROY = DataKey.list(Identifier.fromString("can_destroy"), BlockType.class);

    public static final ListDataKey<BlockType> CAN_PLACE_ON = DataKey.list(Identifier.fromString("can_place_on"), BlockType.class);

    public static final SimpleDataKey<Long> SHIELD_BLOCKING_TICKS = DataKey.simple(Identifier.fromString("shield_blocking_ticks"), Long.class);

    public static final SimpleDataKey<Record> RECORD_TYPE = DataKey.simple(Identifier.fromString("record_type"), Record.class);

    public static final SimpleDataKey<EntityType<?>> SPAWN_EGG_TYPE = DataKey.simple(Identifier.fromString("spawn_egg_type"), EntityType.class);

    public static final SimpleDataKey<TreeSpecies> TREE_SPECIES = DataKey.simple(Identifier.fromString("tree_species"), TreeSpecies.class);
}
