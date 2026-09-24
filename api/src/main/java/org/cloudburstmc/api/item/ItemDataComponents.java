package org.cloudburstmc.api.item;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.enchantment.EnchantmentType;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.item.data.*;
import org.cloudburstmc.api.item.data.Record;
import org.cloudburstmc.api.potion.PotionType;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.data.DyeColor;
import org.cloudburstmc.api.util.data.FireworkData;
import org.cloudburstmc.api.util.data.TreeSpecies;

import java.util.List;
import java.util.Map;

/**
 * Standard item stack data component types.
 */
@UtilityClass
public class ItemDataComponents {

    /**
     * Block state represented by a block item.
     */
    public static final ItemDataComponentType<BlockState> BLOCK_STATE = value("block_state", BlockState.class);

    /**
     * Custom display name.
     */
    public static final ItemDataComponentType<String> CUSTOM_NAME = value("custom_name", String.class);

    /**
     * Custom lore lines.
     */
    public static final ItemDataComponentType<List<String>> CUSTOM_LORE = list("custom_lore", String.class);

    /**
     * Durability damage already sustained by the item.
     */
    public static final ItemDataComponentType<Integer> DAMAGE = value("damage", Integer.class);

    /**
     * Additional anvil cost accumulated by prior operations.
     */
    public static final ItemDataComponentType<Integer> REPAIR_COST = value("repair_cost", Integer.class);

    /**
     * Inventory lock mode.
     */
    public static final ItemDataComponentType<ItemLockMode> ITEM_LOCK = value("item_lock", ItemLockMode.class);

    /**
     * Whether the item remains in its owner's inventory after death.
     */
    public static final ItemDataComponentType<Boolean> KEEP_ON_DEATH = booleanValue("keep_on_death");

    /**
     * Whether durability damage is disabled for the item.
     */
    public static final ItemDataComponentType<Boolean> UNBREAKABLE = booleanValue("unbreakable");

    /**
     * Dye color applied to the item.
     */
    public static final ItemDataComponentType<DyeColor> COLOR = value("dye_color", DyeColor.class);

    /**
     * Enchantments indexed by type.
     */
    public static final ItemDataComponentType<Map<EnchantmentType, Enchantment>> ENCHANTMENTS = ItemDataComponentType.registerMap(Identifier.parse("enchantments"), EnchantmentType.class, Enchantment.class);

    /**
     * Potion contained in a drinkable or thrown potion item.
     */
    public static final ItemDataComponentType<PotionType> POTION_TYPE = value("potion_type", PotionType.class);

    /**
     * Zero-based Bad Omen level stored by an ominous bottle.
     */
    public static final ItemDataComponentType<Integer> OMINOUS_BOTTLE_AMPLIFIER = value("ominous_bottle_amplifier", Integer.class);

    /**
     * Projectile stored in a charged crossbow.
     */
    public static final ItemDataComponentType<ItemStack> CHARGED_PROJECTILE = value("charged_projectile", ItemStack.class);

    /**
     * Firework rocket or star data.
     */
    public static final ItemDataComponentType<FireworkData> FIREWORK_DATA = value("firework_data", FireworkData.class);

    /**
     * Fluid stored by a bucket item.
     */
    public static final ItemDataComponentType<Bucket> BUCKET_DATA = value("bucket_data", Bucket.class);

    /**
     * Entity state stored by an entity bucket.
     */
    public static final ItemDataComponentType<BucketEntityData> BUCKET_ENTITY_DATA = value("bucket_entity_data", BucketEntityData.class);

    /**
     * Blocks the item may destroy in adventure mode.
     */
    public static final ItemDataComponentType<List<BlockType>> CAN_DESTROY = list("can_destroy", BlockType.class);

    /**
     * Blocks against which the item may be placed in adventure mode.
     */
    public static final ItemDataComponentType<List<BlockType>> CAN_PLACE_ON = list("can_place_on", BlockType.class);

    /**
     * Tick at which a shield began blocking.
     */
    public static final ItemDataComponentType<Long> SHIELD_BLOCKING_TICKS = value("shield_blocking_ticks", Long.class);

    /**
     * Music record represented by the item.
     */
    public static final ItemDataComponentType<Record> RECORD_TYPE = value("record_type", Record.class);

    /**
     * Entity type represented by a spawn egg.
     */
    public static final ItemDataComponentType<EntityType<?>> SPAWN_EGG_TYPE = value("spawn_egg_type", EntityType.class);

    /**
     * Tree species encoded by a legacy item variant.
     */
    public static final ItemDataComponentType<TreeSpecies> TREE_SPECIES = value("tree_species", TreeSpecies.class);

    /**
     * Written-book contents.
     */
    public static final ItemDataComponentType<WrittenBook> BOOK_DATA = value("book_data", WrittenBook.class);

    /**
     * Map identifier and display data.
     */
    public static final ItemDataComponentType<MapItem> MAP_DATA = value("map_data", MapItem.class);

    /**
     * Banner base color and patterns.
     */
    public static final ItemDataComponentType<BannerData> BANNER_DATA = value("banner_data", BannerData.class);

    private static <T> ItemDataComponentType<T> value(String id, Class<? super T> valueType) {
        return ItemDataComponentType.builtInValue(Identifier.parse(id), valueType);
    }

    private static ItemDataComponentType<Boolean> booleanValue(String id) {
        return ItemDataComponentType.builtInBooleanValue(Identifier.parse(id));
    }

    private static <E> ItemDataComponentType<List<E>> list(String id, Class<E> elementType) {
        return ItemDataComponentType.builtInList(Identifier.parse(id), elementType);
    }
}
