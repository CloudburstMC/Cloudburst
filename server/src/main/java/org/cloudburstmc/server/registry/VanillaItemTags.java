package org.cloudburstmc.server.registry;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.item.ItemTag;
import org.cloudburstmc.api.item.ItemTagKey;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.util.Identifier;

import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.cloudburstmc.api.item.ItemTags.*;
import static org.cloudburstmc.api.item.ItemTypes.*;

@UtilityClass
public class VanillaItemTags {

    private static final Map<ItemTagKey, Set<Identifier>> DIRECT = new IdentityHashMap<>();
    private static final Map<ItemTagKey, ItemTag> RESOLVED = new IdentityHashMap<>();

    static {
        initWoodenToolMaterials();
        initStoneToolMaterials();
        initCopperToolMaterials();
        initIronToolMaterials();
        initGoldToolMaterials();
        initDiamondToolMaterials();
        initNetheriteToolMaterials();
        initArmorRepairMaterials();
    }

    public static void freeze() {
        for (ItemTagKey key : keys()) {
            RESOLVED.put(key, new ResolvedItemTag(key, ids(key)));
        }
    }

    public static ItemTag resolve(ItemTagKey key) {
        ItemTag tag = RESOLVED.get(key);
        if (tag == null) {
            throw new IllegalArgumentException("Unknown item tag: " + key);
        }
        return tag;
    }

    public static Collection<ItemTag> all() {
        return Set.copyOf(RESOLVED.values());
    }

    private static Set<ItemTagKey> keys() {
        Set<ItemTagKey> keys = Collections.newSetFromMap(new IdentityHashMap<>());
        keys.addAll(DIRECT.keySet());
        return keys;
    }

    private static void tag(ItemTagKey tag, ItemType... types) {
        Set<Identifier> values = DIRECT.computeIfAbsent(tag, ignored -> new HashSet<>());
        for (ItemType type : types) {
            values.add(type.getId());
        }
    }

    private static Set<Identifier> ids(ItemTagKey tag) {
        checkNotNull(tag, "tag");
        return Set.copyOf(DIRECT.getOrDefault(tag, Set.of()));
    }

    private static Set<Identifier> requireIds(ItemTagKey tag) {
        checkNotNull(tag, "tag");
        Set<Identifier> ids = DIRECT.get(tag);
        if (ids == null) {
            throw new IllegalArgumentException("Unknown item tag: " + tag);
        }
        return Set.copyOf(ids);
    }

    private static void initStoneToolMaterials() {
        tag(STONE_TOOL_MATERIALS,
                BLACKSTONE,
                COBBLED_DEEPSLATE,
                COBBLESTONE
        );
    }

    private static void initCopperToolMaterials() {
        tag(COPPER_TOOL_MATERIALS, COPPER_INGOT);
    }

    private static void initIronToolMaterials() {
        tag(IRON_TOOL_MATERIALS, IRON_INGOT);
    }

    private static void initGoldToolMaterials() {
        tag(GOLD_TOOL_MATERIALS, GOLD_INGOT);
    }

    private static void initDiamondToolMaterials() {
        tag(DIAMOND_TOOL_MATERIALS, DIAMOND);
    }

    private static void initNetheriteToolMaterials() {
        tag(NETHERITE_TOOL_MATERIALS, NETHERITE_INGOT);
    }

    private static void initWoodenToolMaterials() {
        tag(WOODEN_TOOL_MATERIALS,
                ACACIA_PLANKS,
                BAMBOO_PLANKS,
                BIRCH_PLANKS,
                CHERRY_PLANKS,
                CRIMSON_PLANKS,
                DARK_OAK_PLANKS,
                JUNGLE_PLANKS,
                MANGROVE_PLANKS,
                OAK_PLANKS,
                PALE_OAK_PLANKS,
                POPLAR_PLANKS,
                SPRUCE_PLANKS,
                WARPED_PLANKS
        );
    }

    private static void initArmorRepairMaterials() {
        tag(REPAIRS_LEATHER_ARMOR, LEATHER);
        tag(REPAIRS_COPPER_ARMOR, COPPER_INGOT);
        tag(REPAIRS_CHAIN_ARMOR, IRON_INGOT);
        tag(REPAIRS_IRON_ARMOR, IRON_INGOT);
        tag(REPAIRS_GOLD_ARMOR, GOLD_INGOT);
        tag(REPAIRS_DIAMOND_ARMOR, DIAMOND);
        tag(REPAIRS_NETHERITE_ARMOR, NETHERITE_INGOT);
        tag(REPAIRS_TURTLE_HELMET, TURTLE_SCUTE);
        tag(REPAIRS_WOLF_ARMOR, ARMADILLO_SCUTE);
    }

    private record ResolvedItemTag(ItemTagKey key, Set<Identifier> ids) implements ItemTag {
        @Override
        public ItemTagKey getKey() {
            return this.key;
        }

        @Override
        public boolean isTagged(ItemType type) {
            checkNotNull(type, "type");
            return this.ids.contains(type.getId());
        }

        @Override
        public Set<ItemType> getValues() {
            Set<ItemType> values = Collections.newSetFromMap(new IdentityHashMap<>());
            for (Identifier id : this.ids) {
                ItemType type = CloudItemRegistry.get().getType(id);
                values.add(type != null ? type : ItemTypes.get(id).orElseGet(() -> ItemType.of(id)));
            }
            return Set.copyOf(values);
        }
    }
}
