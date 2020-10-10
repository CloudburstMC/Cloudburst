package org.cloudburstmc.server.item.behavior;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.nbt.*;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.api.registry.RegistryException;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockStates;
import org.cloudburstmc.server.block.behavior.BlockBehavior;
import org.cloudburstmc.server.block.util.BlockStateMetaMappings;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.inventory.Fuel;
import org.cloudburstmc.server.item.enchantment.Enchantment;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.ItemRegistry;
import org.cloudburstmc.server.utils.Config;
import org.cloudburstmc.server.utils.Identifier;
import org.cloudburstmc.server.utils.Utils;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.cloudburstmc.server.block.BlockIds.AIR;
import static org.cloudburstmc.server.item.behavior.ItemIds.BUCKET;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
@Log4j2
public abstract class Item implements Cloneable {

    private static final Pattern ITEM_STRING_PATTERN = Pattern.compile("^((?:[A-Za-z_0-9]+:)?[A-Za-z_0-9]+):?([0-9]+)?$");

    private final Identifier id;
    private int meta;
    private int count;
    private final List<String> lore = new ArrayList<>();
    private final Short2ObjectMap<Enchantment> enchantments = new Short2ObjectOpenHashMap<>();
    private int damage;
    private String customName;
    private NbtMap tag = NbtMap.EMPTY;
    private final Set<Identifier> canPlaceOn = new HashSet<>();
    private final Set<Identifier> canDestroy = new HashSet<>();

    public Item(Identifier id) {
        this.id = id;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void initCreativeItems() {
        clearCreativeItems();

        Config config = new Config(Config.JSON);
        config.load(CloudServer.class.getClassLoader().getResourceAsStream("data/creative_items.json"));
        List<Map> list = config.getMapList("items");

        for (Map map : list) {
            try {
                addCreativeItem(fromJson(map));
            } catch (RegistryException e) {
                // ignore
            } catch (Exception e) {
                log.error("Error whilst adding creative item", e);
            }
        }
    }

    public boolean canBeActivated() {
        return false;
    }

    private static final ArrayList<Item> creative = new ArrayList<>();

    public static void clearCreativeItems() {
        Item.creative.clear();
    }

    public static ArrayList<Item> getCreativeItems() {
        return new ArrayList<>(Item.creative);
    }

    public static void addCreativeItem(Item item) {
        Item.creative.add(item.clone());
    }

    public static void removeCreativeItem(Item item) {
        int index = getCreativeItemIndex(item);
        if (index != -1) {
            Item.creative.remove(index);
        }
    }

    public static boolean isCreativeItem(Item item) {
        for (Item aCreative : Item.creative) {
            if (item.equals(aCreative, !item.isTool())) {
                return true;
            }
        }
        return false;
    }

    public static Item getCreativeItem(int index) {
        return (index >= 0 && index < Item.creative.size()) ? Item.creative.get(index) : null;
    }

    public static int getCreativeItemIndex(Item item) {
        for (int i = 0; i < Item.creative.size(); i++) {
            if (item.equals(Item.creative.get(i), !item.isTool())) {
                return i;
            }
        }
        return -1;
    }

    public static Item get(BlockState blockState) {
        return get(blockState, 1);
    }

    public static Item get(BlockState blockState, int count) {
        return get(blockState.getType(), BlockStateMetaMappings.getMetaFromState(blockState), count);
    }

    public static Item get(Identifier id) {
        return get(id, 0);
    }

    public static Item get(Identifier id, int meta) {
        return get(id, meta, 1);
    }

    public static Item get(Identifier id, int meta, int count) {
        return get(id, meta, count, NbtMap.EMPTY);
    }

    public static Item get(Identifier id, int meta, int count, NbtMap tag) {
        Item item = CloudServer.getInstance().getItemRegistry().getItem(id, meta);
        item.setMeta(meta);
        item.setCount(count);
        item.loadAdditionalData(tag);

        return item;
    }

    @Deprecated
    public static Item get(int id) {
        return get(id, 0);
    }

    @Deprecated
    public static Item get(int id, int meta) {
        return get(id, meta, 1);
    }

    @Deprecated
    public static Item get(int id, int meta, int count) {
        return get(id, meta, count, NbtMap.EMPTY);
    }

    @Deprecated
    public static Item get(int id, int meta, int count, NbtMap tags) {
        ItemRegistry registry = CloudServer.getInstance().getItemRegistry();
        Identifier identifier = registry.fromLegacy(id);
        return get(identifier, meta, count, tags);
    }

    public static Item fromString(String str) {
        Matcher matcher = ITEM_STRING_PATTERN.matcher(str);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid item string");
        }

        Identifier id;
        int meta = 0;

        String stringId = matcher.group(1);
        Pattern integerPattern = Pattern.compile("^[1-9]\\d*$");
        if (integerPattern.matcher(stringId).matches()) {
            id = ItemRegistry.get().fromLegacy(Integer.parseInt(stringId));
        } else {
            id = Identifier.fromString(stringId);
        }

        String metaString = matcher.group(2);
        if (metaString != null) meta = Integer.parseInt(metaString) & 0xFFFF;

        return get(id, meta);
    }

    public boolean hasMeta() {
        return this.meta < Short.MAX_VALUE;
    }

    public static Item fromJson(Map<String, Object> data) {
        String nbt = (String) data.get("nbt_b64");

        byte[] nbtBytes = null;
        if (nbt != null) {
            nbtBytes = Base64.getDecoder().decode(nbt);
        } else if ((nbt = nbt = (String) data.getOrDefault("nbt_hex", null)) != null) { // Support old format for backwards compat
            nbtBytes = Utils.parseHexBinary(nbt);
        }

        NbtMap tag;
        if (nbtBytes != null) {
            try (NBTInputStream stream = NbtUtils.createReaderLE(new ByteArrayInputStream(Base64.getDecoder().decode(nbt)))) {
                tag = (NbtMap) stream.readTag();
            } catch (IOException e) {
                throw new IllegalStateException("Unable to decode tag", e);
            }
        } else {
            tag = NbtMap.EMPTY;
        }

        return get(Utils.toInt(data.get("id")), Utils.toInt(data.getOrDefault("damage", 0)), Utils.toInt(data.getOrDefault("count", 1)), tag);
    }

    public static Item[] fromStringMultiple(String str) {
        String[] b = str.split(",");
        Item[] items = new Item[b.length - 1];
        for (int i = 0; i < b.length; i++) {
            items[i] = fromString(b[i]);
        }
        return items;
    }

    public static ItemData[] toNetwork(List<Item> items) {
        return toNetwork(items.toArray(new Item[0]));
    }

    public static ItemData[] toNetwork(Item[] items) {
        ItemData[] itemData = new ItemData[items.length];
        for (int i = 0; i < items.length; i++) {
            itemData[i] = items[i].toNetwork();
        }
        return itemData;
    }

    public static Item fromNetwork(ItemData itemData) {
        Identifier identifier = ItemRegistry.get().getIdentifier(itemData.getId());
        int meta = itemData.getDamage();
        int count = itemData.getCount();
        String[] canBreak = itemData.getCanBreak();
        String[] canPlace = itemData.getCanPlace();
        NbtMap tag = itemData.getTag();
        if (tag == null) {
            tag = NbtMap.EMPTY;
        }

        if (canBreak.length > 0 || canPlace.length > 0) {
            NbtMapBuilder tagBuilder = tag.toBuilder();

            if (canBreak.length > 0) {
                List<String> listTag = new ArrayList<>(Arrays.asList(canBreak));
                tagBuilder.putList("CanDestroy", NbtType.STRING, listTag);
            }

            if (canPlace.length > 0) {
                List<String> listTag = new ArrayList<>(Arrays.asList(canPlace));
                tagBuilder.putList("CanPlaceOn", NbtType.STRING, listTag);
            }
            tag = tagBuilder.build();
        }

        return Item.get(identifier, meta, count, tag);
    }

    public void loadAdditionalData(NbtMap tag) {
        this.tag = tag;

        tag.listenForCompound("display", displayTag -> {
            displayTag.listenForString("Name", this::setCustomName);
            displayTag.listenForList("Lore", NbtType.STRING, this::setLore);
        });

        tag.listenForList("ench", NbtType.COMPOUND, tags -> {
            for (NbtMap entry : tags) {
                short id = entry.getShort("id");
                int level = entry.getShort("lvl");
                this.enchantments.put(id, Enchantment.getEnchantment(id).setLevel(level, false));
            }
        });
    }

    public void saveAdditionalData(NbtMapBuilder tag) {
        tag.putAll(this.getTag());


        NbtMapBuilder displayTag = NbtMap.builder();
        if (this.customName != null) {
            displayTag.putString("Name", this.customName);
        }

        if (!this.lore.isEmpty()) {
            displayTag.putList("Lore", NbtType.STRING, this.lore);
        }

        NbtMap display = displayTag.build();
        if (!display.isEmpty()) {
            tag.putCompound("display", display);
        }

        if (!this.canDestroy.isEmpty()) {
            List<String> listTag = new ArrayList<>();
            for (Identifier blockName : this.canDestroy) {
                listTag.add(blockName.toString());
            }
            tag.putList("CanDestroy", NbtType.STRING, listTag);
        }

        if (!this.canPlaceOn.isEmpty()) {
            List<String> listTag = new ArrayList<>();
            for (Identifier blockName : this.canPlaceOn) {
                listTag.add(blockName.toString());
            }
            tag.putList("CanPlaceOn", NbtType.STRING, listTag);
        }

        if (!this.enchantments.isEmpty()) {
            List<NbtMap> enchantmentTags = new ArrayList<>();
            for (Enchantment enchantment : this.enchantments.values()) {
                enchantmentTags.add(NbtMap.builder()
                        .putShort("id", (short) enchantment.getId())
                        .putShort("lvl", (short) enchantment.getLevel())
                        .build());
            }
            tag.putList("ench", NbtType.COMPOUND, enchantmentTags);
        }
    }

    public NbtMap createTag() {
        NbtMapBuilder tag = NbtMap.builder();
        this.saveAdditionalData(tag);
        return tag.build();
    }

    public boolean canPlaceOn(Identifier identifier) {
        return this.canPlaceOn.contains(identifier);
    }

    public boolean canDestroy(Identifier identifier) {
        return this.canDestroy.contains(identifier);
    }

    public boolean hasNbtMap() {
        return !this.tag.isEmpty();
    }

    public NbtMap getTag() {
        return tag;
    }

    public Enchantment getEnchantment(int id) {
        return getEnchantment((short) (id & 0xffff));
    }

    public void setTag(NbtMap tag) {
        this.tag = tag == null ? NbtMap.EMPTY : tag;
    }

    public Item addTag(NbtMap tag) {
        this.tag = this.tag.toBuilder().putCompound("", tag).build();
        return this;
    }

    public boolean hasEnchantments() {
        return !this.enchantments.isEmpty();
    }

    public Enchantment getEnchantment(short id) {
        return this.enchantments.get(id);
    }

    public void addEnchantment(Enchantment... enchantments) {
        for (Enchantment enchantment : enchantments) {
            this.enchantments.putIfAbsent((short) enchantment.getId(), enchantment);
        }
    }

    public Enchantment[] getEnchantments() {
        if (!this.hasEnchantments()) {
            return new Enchantment[0];
        }

        return this.enchantments.values().toArray(new Enchantment[0]);
    }

    public boolean hasCustomName() {
        return this.customName != null;
    }

    @Nullable
    public String getCustomName() {
        return this.customName;
    }

    public Item setCustomName(String name) {
        this.customName = name;
        return this;
    }

    public Item clearCustomName() {
        this.customName = null;
        return this;
    }

    public void decrementCount() {
        this.decrementCount(1);
    }

    public void decrementCount(int amount) {
        this.count -= amount;
    }

    public void incrementCount() {
        this.incrementCount(1);
    }

    public void incrementCount(int amount) {
        this.count += amount;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public boolean isNull() {
        return this.count <= 0 || this.id == AIR;
    }

    final public String getName() {
        if (this.hasCustomName())
            return this.getCustomName();
        return CloudServer.getInstance().getLanguage().translate("item." + getId().getName() + ".name");
    }

    public final boolean canBePlaced() {
        BlockState state = this.getBlock();
        BlockBehavior behavior = state.getBehavior();
        return behavior.canBePlaced();
    }

    public BlockState getBlock() {
        return BlockStates.AIR;
    }

    public Identifier getId() {
        return id;
    }

    public String[] getLore() {
        return this.lore.toArray(new String[0]);
    }

    public Item setLore(Collection<String> lines) {
        this.lore.clear();
        this.lore.addAll(lines);
        return this;
    }

    public Item setLore(String... lines) {
        this.lore.clear();
        Collections.addAll(this.lore, lines);
        return this;
    }

    public int getMaxStackSize() {
        return 64;
    }

    final public Short getFuelTime() {
        if (!Fuel.duration.containsKey(id)) {
            return null;
        }
        if (this.id != BUCKET || this.meta == 10) {
            return Fuel.duration.get(this.id);
        }
        return null;
    }

    public boolean useOn(Entity entity) {
        return false;
    }

    public boolean useOn(Block block) {
        return false;
    }

    public boolean isTool() {
        return false;
    }

    public int getMaxDurability() {
        return -1;
    }

    public int getTier() {
        return 0;
    }

    public boolean isPickaxe() {
        return false;
    }

    public boolean isAxe() {
        return false;
    }

    public boolean isSword() {
        return false;
    }

    public boolean isShovel() {
        return false;
    }

    public boolean isHoe() {
        return false;
    }

    public boolean isShears() {
        return false;
    }

    public boolean isArmor() {
        return false;
    }

    public boolean isHelmet() {
        return false;
    }

    public boolean isChestplate() {
        return false;
    }

    public boolean isLeggings() {
        return false;
    }

    public boolean isBoots() {
        return false;
    }

    public int getEnchantAbility() {
        return 0;
    }

    public int getAttackDamage() {
        return 1;
    }

    public int getArmorPoints() {
        return 0;
    }

    public int getToughness() {
        return 0;
    }

    public boolean isUnbreakable() {
        return false;
    }

    public boolean onUse(Player player, int ticksUsed) {
        return false;
    }

    public boolean onRelease(Player player, int ticksUsed) {
        return false;
    }

    public int getMeta() {
        return meta == 0xffff ? 0 : meta;
    }

    public int getDestroySpeed(BlockState blockState, Player player) {
        return 1;
    }

    public boolean onActivate(Level level, Player player, Block block, Block target, Direction face, Vector3f clickPos) {
        return false;
    }

    /**
     * Called when a player uses the item on air, for example throwing a projectile.
     * Returns whether the item was changed, for example count decrease or durability change.
     *
     * @param player          player
     * @param directionVector direction
     * @return item changed
     */
    public boolean onClickAir(Player player, Vector3f directionVector) {
        return false;
    }

    @Override
    public final boolean equals(Object item) {
        return item instanceof Item && this.equals((Item) item, true);
    }

    public final boolean equals(Item item, boolean checkDamage) {
        return equals(item, checkDamage, true);
    }

    public void setMeta(int meta) {
        meta &= 0xffff;
        if (meta != this.meta) {
            this.meta = meta;
            this.onMetaChange(this.meta);
        }
    }

    /**
     * Returns whether the specified item stack has the same ID, damage, NBT and count as this item stack.
     *
     * @param other item
     * @return equal
     */
    public final boolean equalsExact(Item other) {
        return this.equals(other, true, true) && this.count == other.count;
    }

    @Deprecated
    public final boolean deepEquals(Item item) {
        return equals(item, true);
    }

    @Deprecated
    public final boolean deepEquals(Item item, boolean checkDamage) {
        return equals(item, checkDamage, true);
    }

    @Deprecated
    public final boolean deepEquals(Item item, boolean checkDamage, boolean checkCompound) {
        return equals(item, checkDamage, checkCompound);
    }

    protected void onMetaChange(int newMeta) {
    }

    @Override
    final public String toString() {
        return "Item(id=" + this.id +
                ", meta=" + this.meta +
                ", count=" + this.count +
                ", customName=" + this.customName +
                ", enchantments=" + this.enchantments +
                ")";
    }

    public final boolean equals(Item that, boolean checkDamage, boolean checkCompound) {
        if (this.id == that.id && (!checkDamage || this.meta == that.meta)) {
            if (!checkCompound) {
                return true;
            }
            NbtMapBuilder thisTag = NbtMap.builder();
            NbtMapBuilder thatTag = NbtMap.builder();
            this.saveAdditionalData(thisTag);
            that.saveAdditionalData(thatTag);
            return thisTag.build().equals(thatTag.build());
        }
        return false;
    }

    @Override
    public Item clone() {
        try {
            return (Item) super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    public int getNetworkId() {
        return ItemRegistry.get().getRuntimeId(this.id);
    }

    public ItemData toNetwork() {
        int id = ItemRegistry.get().getRuntimeId(this.id);

        NbtMapBuilder tagBuilder = NbtMap.builder();
        this.saveAdditionalData(tagBuilder);
        NbtMap tag = tagBuilder.build();
        if (tag.isEmpty()) tag = null;

        String[] canPlace = this.canPlaceOn.stream().map(Identifier::toString).toArray(String[]::new);
        String[] canBreak = this.canDestroy.stream().map(Identifier::toString).toArray(String[]::new);

        return ItemData.of(id, (short) this.meta, this.count, tag, canPlace, canBreak);
    }
}
