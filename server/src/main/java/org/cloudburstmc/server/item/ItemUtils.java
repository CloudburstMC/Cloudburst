package org.cloudburstmc.server.item;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.item.ItemKeys;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemStackBuilder;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.nbt.*;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.data.definitions.SimpleItemDefinition;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.data.inventory.descriptor.ComplexAliasDescriptor;
import org.cloudburstmc.protocol.bedrock.data.inventory.descriptor.DefaultDescriptor;
import org.cloudburstmc.protocol.bedrock.data.inventory.descriptor.ItemDescriptorWithCount;
import org.cloudburstmc.protocol.bedrock.data.inventory.descriptor.ItemTagDescriptor;
import org.cloudburstmc.server.block.BlockPalette;
import org.cloudburstmc.server.block.CloudBlockDefinition;
import org.cloudburstmc.server.block.util.BlockStateMetaMappings;
import org.cloudburstmc.server.network.inventory.NetworkItemStack;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.registry.CloudItemRegistry;
import org.cloudburstmc.server.utils.Utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ExecutionException;

@UtilityClass
@Log4j2
public class ItemUtils {

    private static final CloudItemRegistry registry = CloudItemRegistry.get();
    private static final Cache<ItemStack, NbtMap> ITEM_CACHE = CacheBuilder.newBuilder().weakKeys().softValues().build();

    private static final Map<String, NbtMap> RECIPE_BLOCK_STATES;

    static {
        NbtMap north = NbtMap.builder().putString("minecraft:cardinal_direction", "north").build();
        NbtMap facingUp = NbtMap.builder().putInt("facing_direction", 1).build();
        NbtMap facingSouth = NbtMap.builder()
                .putInt("facing_direction", 3)
                .putByte("triggered_bit", (byte) 0)
                .build();

        RECIPE_BLOCK_STATES = Map.ofEntries(
                Map.entry("minecraft:chest", north),
                Map.entry("minecraft:copper_chest", north),
                Map.entry("minecraft:ender_chest", north),
                Map.entry("minecraft:trapped_chest", north),
                Map.entry("minecraft:stonecutter_block", north),
                Map.entry("minecraft:waxed_copper_chest", north),
                Map.entry("minecraft:waxed_exposed_copper_chest", north),
                Map.entry("minecraft:waxed_oxidized_copper_chest", north),
                Map.entry("minecraft:waxed_weathered_copper_chest", north),
                Map.entry("minecraft:piston", facingUp),
                Map.entry("minecraft:sticky_piston", facingUp),
                Map.entry("minecraft:dispenser", facingSouth),
                Map.entry("minecraft:dropper", facingSouth)
        );
    }

    public static NbtMap serializeItem(ItemStack item) {
        if (item == null || item.isEmpty()) {
            return NbtMap.builder()
                    .putString("Name", "minecraft:air")
                    .putByte("Count", (byte) 0)
                    .putShort("Damage", (short) 0)
                    .build();
        }

        NbtMapBuilder nbtTag = NbtMap.builder();

        nbtTag.putString("Name", item.getType().getId().toString())
                .putByte("Count", (byte) item.getCount())
                .putShort("Damage", (short) 0);

        if (item.isBlock()) {
            NbtMapBuilder blockTag = NbtMap.builder();
            BlockState blockState = item.get(ItemKeys.BLOCK_STATE);

//            log.info(item.getType() + " - " + blockState + " - " + BlockPalette.INSTANCE.getIdentifier(blockState));
            blockTag.putString("Name", BlockPalette.INSTANCE.getIdentifier(blockState).toString());
            blockTag.putShort("Damage", (short) 0);

            nbtTag.put("Block", blockTag.build());
        }

        if (item.get(ItemKeys.CAN_DESTROY) != null) {
            List<String> blocks = item.get(ItemKeys.CAN_DESTROY)
                    .stream()
                    .map(BlockPalette.INSTANCE::getTypeIdentifiers)
                    .flatMap(Collection::stream)
                    .map(Identifier::toString)
                    .toList();

            nbtTag.putList("CanDestroy", NbtType.STRING, blocks);
        }

        if (item.get(ItemKeys.CAN_PLACE_ON) != null) {
            List<String> blocks = item.get(ItemKeys.CAN_PLACE_ON).stream().map(blockType -> blockType.getId().toString()).toList();
            nbtTag.putList("CanPlaceOn", NbtType.STRING, blocks);
        }

        NbtMap tag = ItemUtils.getSerializedTag(item);
        if (!tag.isEmpty()) {
            nbtTag.put("tag", tag);
        }

        return nbtTag.build();
    }

    private static NbtMap getSerializedTag(ItemStack item) {
        try {
            return ITEM_CACHE.get(item, () -> {
                NbtMapBuilder tagBuilder = NbtMap.builder();
                registry.getSerializer(item.getType()).serialize(item, tagBuilder);
                return tagBuilder.build();
            });
        } catch (ExecutionException e) {
            throw new IllegalStateException("Invalid state while serializing item " + item, e);
        }
    }

    public static NbtMap serializeItem(ItemStack item, int slot) {
        NbtMap map = serializeItem(item);

        if (slot >= 0) {
            return map.toBuilder().putByte("Slot", (byte) slot).build();
        }

        return map;
    }

    public static ItemStack deserializeItem(NbtMap tag) {
        if (!tag.containsKey("Name", NbtType.STRING) || !tag.containsKey("Count", NbtType.BYTE)) {
            return ItemStack.EMPTY;
        }

        ItemStack base = ItemUtils.deserializeItem(
                Identifier.parse(tag.getString("Name")),
                tag.getShort("Damage", (short) 0),
                tag.getByte("Count"),
                tag.getCompound("tag", NbtMap.EMPTY));

        if (base.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStackBuilder builder = base.toBuilder();

        if (tag.containsKey("CanPlaceOn", NbtType.LIST)) {
            List<BlockType> list = tag.getList("CanPlaceOn", NbtType.STRING, Collections.emptyList()).stream().map(Identifier::parse).map(BlockType::of).toList();
            builder.data(ItemKeys.CAN_PLACE_ON, list);
        }

        if (tag.containsKey("CanDestroy", NbtType.LIST)) {
            List<BlockType> list = tag.getList("CanDestroy", NbtType.STRING, Collections.emptyList()).stream().map(Identifier::parse).map(BlockType::of).toList();
            builder.data(ItemKeys.CAN_DESTROY, list);
        }

        return builder.build();
    }

    public static ItemStack deserializeItem(Identifier id, short damage, int amount, NbtMap tag) {
        ItemStackBuilder builder = ItemStack.builder();
        if (amount > 0) {
            ItemType type = registry.get(id).orElse(ItemTypes.UNKNOWN);
            builder.itemType(type);
            builder.amount(amount);

            if (BlockPalette.INSTANCE.getType(id) != null) {
                BlockState blockState = BlockStateMetaMappings.getStateFromMeta(id, damage);

                if (blockState != null) {
                    builder.data(ItemKeys.BLOCK_STATE, blockState);
                }

                if (damage != 0) {
                    builder.data(ItemKeys.DAMAGE, (int) damage);
                }
            }

            registry.getSerializer(type).deserialize(id, damage, builder, tag);
        } else {
            return ItemStack.EMPTY;
        }
        return builder.build();
    }

    public static ItemData toNetwork(ItemStack item) {
        if (item == null || item.isEmpty()) {
            return ItemData.AIR;
        }
        return ItemUtils.toNetworkBuilder(item)
                .netId(0)
                .usingNetId(false)
                .build();
    }

    public static List<ItemData> toNetwork(Collection<ItemStack> item) {
        List<ItemData> data = new ArrayList<>();

        for (ItemStack itemStack : item) {
            data.add(toNetwork(itemStack));
        }

        return data;
    }

    public static List<ItemDescriptorWithCount> toDescriptors(Collection<ItemStack> item) {
        List<ItemDescriptorWithCount> data = new ArrayList<>();

        for (ItemStack itemStack : item) {
            data.add(toDescriptor(itemStack));
        }

        return data;
    }

    public static ItemDescriptorWithCount toDescriptor(ItemStack item) {
        return ItemDescriptorWithCount.fromItem(toNetwork(item));
    }

    public static ItemData toNetworkRecipe(ItemStack item) {
        if (item == null || item.isEmpty()) {
            return ItemData.AIR;
        }

        return toNetworkRecipeBuilder(item)
                .netId(0)
                .usingNetId(false)
                .build();
    }

    public static List<ItemData> toNetworkRecipe(Collection<ItemStack> items) {
        List<ItemData> data = new ArrayList<>();
        for (ItemStack item : items) {
            data.add(toNetworkRecipe(item));
        }
        return data;
    }

    public static List<ItemDescriptorWithCount> toDescriptorsRecipe(Collection<ItemStack> items) {
        List<ItemDescriptorWithCount> data = new ArrayList<>();
        for (ItemStack item : items) {
            data.add(ItemDescriptorWithCount.fromItem(toNetworkRecipe(item)));
        }
        return data;
    }

    public static List<ItemDescriptorWithCount> toRecipeDescriptors(List<ItemDescriptorWithCount> descriptors) {
        List<ItemDescriptorWithCount> result = new ArrayList<>(descriptors.size());
        for (ItemDescriptorWithCount desc : descriptors) {
            if (desc.getDescriptor() instanceof DefaultDescriptor dd && !(dd.getItemId() instanceof SimpleItemDefinition)) {
                ItemDefinition raw = dd.getItemId();
                ItemDefinition plain = new SimpleItemDefinition(raw.getIdentifier(), raw.getRuntimeId(), false);
                result.add(new ItemDescriptorWithCount(new DefaultDescriptor(plain, dd.getAuxValue()), desc.getCount()));
            } else {
                result.add(desc);
            }
        }
        return result;
    }

    private static ItemData.Builder toNetworkRecipeBuilder(ItemStack item) {
        Identifier identifier = item.getType().getId();
        int damage = item.getDamage();
        ItemDefinition rawDefinition = registry.getDefinition(identifier, damage);

        ItemDefinition recipeDefinition = rawDefinition != null
                ? new SimpleItemDefinition(rawDefinition.getIdentifier(), rawDefinition.getRuntimeId(), false)
                : null;

        String[] canPlace = new String[0];
        if (item.get(ItemKeys.CAN_PLACE_ON) != null) {
            canPlace = item.get(ItemKeys.CAN_PLACE_ON).stream().map(BlockType::getId).map(Identifier::toString).toArray(String[]::new);
        }

        String[] canBreak = new String[0];
        if (item.get(ItemKeys.CAN_DESTROY) != null) {
            canBreak = item.get(ItemKeys.CAN_DESTROY).stream().map(BlockType::getId).map(Identifier::toString).toArray(String[]::new);
        }

        CloudBlockDefinition blockDefinition = null;
        if (isRecipeBlockItem(rawDefinition, identifier)) {
            NbtMap specificStates = RECIPE_BLOCK_STATES.get(identifier.toString());
            if (specificStates != null) {
                blockDefinition = BlockPalette.INSTANCE.getDefinitionByStates(identifier, specificStates);
            }
            if (blockDefinition == null) {
                blockDefinition = BlockPalette.INSTANCE.getFirstDefinition(identifier);
            }
        }

        NbtMap tag = ItemUtils.getSerializedTag(item);
        return ItemData.builder()
                .definition(recipeDefinition)
                .damage(damage)
                .count(item.getCount())
                .tag(tag.isEmpty() ? null : tag)
                .canPlace(canPlace)
                .canBreak(canBreak)
                .blockDefinition(blockDefinition);
    }

    private static boolean isRecipeBlockItem(ItemDefinition definition, Identifier identifier) {
        if (definition == null) {
            return false;
        }

        if (definition.isComponentBased()) {
            return false;
        }

        int legacyId = definition.getRuntimeId();
        if (legacyId > 255) {
            return false;
        }

        String name = identifier.getName();
        return !name.endsWith("_door") && !name.endsWith("_hanging_sign");
    }

    public static ItemData toNetworkNetId(ItemStack item) {
        if (item == null || item.isEmpty()) {
            return ItemData.AIR;
        }
        int netId = NetworkItemStack.getNetId(item);

        return ItemUtils.toNetworkBuilder(item)
                .netId(netId)
                .usingNetId(true)
                .build();
    }

    private static ItemData.Builder toNetworkBuilder(ItemStack item) {
        Identifier identifier = item.getType().getId();
        int damage = item.getDamage();
        ItemDefinition definition = registry.getDefinition(identifier, damage);

        String[] canPlace = new String[0];
        if (item.get(ItemKeys.CAN_PLACE_ON) != null) {
            canPlace = item.get(ItemKeys.CAN_PLACE_ON).stream().map(BlockType::getId).map(Identifier::toString).toArray(String[]::new);
        }
        String[] canBreak = new String[0];
        if (item.get(ItemKeys.CAN_DESTROY) != null) {
            canBreak = item.get(ItemKeys.CAN_DESTROY).stream().map(BlockType::getId).map(Identifier::toString).toArray(String[]::new);
        }

        CloudBlockDefinition blockDefinition = null;
        try {
            blockDefinition = item.getBlockState().map(CloudBlockRegistry.REGISTRY::getDefinition).orElse(null);
        } catch (IllegalArgumentException e) {
            // Block state exists but doesn't have a definition in the vanilla palette
            // This can happen when creative_items.json has block states that were merged
            // but don't exist as exact matches in block_palette.nbt
        }
        NbtMap tag = ItemUtils.getSerializedTag(item);

        return ItemData.builder()
                .definition(definition)
                .damage(damage)
                .count(item.getCount())
                .tag(tag.isEmpty() ? null : tag)
                .canPlace(canPlace)
                .canBreak(canBreak)
                .blockDefinition(blockDefinition);
    }

    public static ItemStack fromNetwork(ItemData data) {
        if (data.isUsingNetId()) {
            int netId = data.getNetId();

            ItemStack cached = NetworkItemStack.getItemStack(netId);

            if (cached == null) {
                log.trace("Trying to find cached ItemStack for netId {} but it doesn't exist", netId);
            } else {
                return cached;
            }
        }

        Identifier id = Identifier.parse(data.getDefinition().getIdentifier());
        NbtMap tag = data.getTag();
        if (tag == null) {
            tag = NbtMap.EMPTY;
        }

        parseBreakPlaceData(data, tag);

        return ItemUtils.deserializeItem(id, (short) data.getDamage(), data.getCount(), tag);
    }

    private static void parseBreakPlaceData(ItemData data, NbtMap tag) {
        String[] canBreak = data.getCanBreak();
        String[] canPlace = data.getCanPlace();

        if (canBreak.length > 0 || canPlace.length > 0) {
            NbtMapBuilder nbt = tag.toBuilder();

            if (canBreak.length > 0) {
                List<String> listTag = new ArrayList<>(Arrays.asList(canBreak));
                nbt.putList("CanDestroy", NbtType.STRING, listTag);
            }

            if (canPlace.length > 0) {
                List<String> listTag = new ArrayList<>(Arrays.asList(canPlace));
                nbt.putList("CanPlaceOn", NbtType.STRING, listTag);
            }

            tag.putAll(nbt);
        }
    }

    public static ItemStack fromJson(Map<String, Object> data) {
        String type = (String) data.get("type");
        if ("item_tag".equals(type) || "complex_alias".equals(type)) {
            return ItemStack.EMPTY;
        }

        String nbt = (String) data.get("nbt_b64");
        NbtMap tag;

        if (nbt != null) {
            byte[] nbtBytes = Base64.getDecoder().decode(nbt);
            try (NBTInputStream stream = NbtUtils.createReaderLE(new ByteArrayInputStream(nbtBytes))) {
                tag = (NbtMap) stream.readTag();
            } catch (IOException e) {
                throw new IllegalStateException("Unable to decode tag", e);
            }
        } else {
            tag = NbtMap.EMPTY;
        }

        Identifier id;
        if (data.containsKey("id")) {
            id = Identifier.parse(data.get("id").toString());
        } else if (data.containsKey("legacyId")) {
            id = registry.fromLegacy(Utils.toInt(data.get("legacyId")), Utils.toInt(data.getOrDefault("damage", 0)));
        } else if (data.containsKey("itemId")) {
            int auxValue = Utils.toInt(data.getOrDefault("auxValue", 0));
            id = registry.fromLegacy(Utils.toInt(data.get("itemId")), auxValue == 32767 ? 0 : auxValue);
        } else {
            id = null;
        }

        if (id == null) {
            throw new IllegalStateException("Unable to decode item JSON: " + data);
        }

        int damage = Utils.toInt(data.getOrDefault("damage", data.getOrDefault("auxValue", 0)));
        return deserializeItem(id, (short) damage, Utils.toInt(data.getOrDefault("count", 1)), tag);
    }

    public static ItemDescriptorWithCount descriptorFromJson(Map<String, Object> data) {
        String type = (String) data.get("type");
        int count = Utils.toInt(data.getOrDefault("count", 1));

        if ("item_tag".equals(type)) {
            String itemTag = (String) data.get("itemTag");
            return new ItemDescriptorWithCount(new ItemTagDescriptor(itemTag), count);
        }

        if ("complex_alias".equals(type)) {
            String name = (String) data.getOrDefault("complexAliasName", "");
            return new ItemDescriptorWithCount(new ComplexAliasDescriptor(name), count);
        }

        if (data.containsKey("id")) {
            Identifier id = Identifier.parse(data.get("id").toString());
            int auxValue = Utils.toInt(data.getOrDefault("auxValue", 0));
            ItemDefinition definition = registry.getDefinition(id);
            if (definition == null) {
                log.warn("Unknown item '{}' in recipe descriptor", id);
                return ItemDescriptorWithCount.EMPTY;
            }
            return new ItemDescriptorWithCount(new DefaultDescriptor(definition, auxValue), count);
        }

        int auxValue = Utils.toInt(data.getOrDefault("auxValue", 0));
        int itemId = Utils.toInt(data.get("itemId"));

        Identifier id = registry.fromLegacy(itemId, auxValue == 32767 ? 0 : auxValue);
        if (id == null) {
            log.warn("Unknown legacy item ID {} in recipe descriptor", itemId);
            return ItemDescriptorWithCount.EMPTY;
        }

        ItemDefinition definition = registry.getDefinition(id);
        return new ItemDescriptorWithCount(new DefaultDescriptor(definition, auxValue), count);
    }

    public static int getItemHash(ItemStack item) {
        return Objects.hash(System.identityHashCode(item.getType().getId()), item.getCount());
    }

    public static UUID getMultiItemHash(List<ItemStack> items) {
        ByteBuffer buffer = ByteBuffer.allocate(items.size() * 8);
        for (ItemStack item : items) {
            if (item != null && !item.isEmpty())
                buffer.putInt(getItemHash(item));
        }
        return UUID.nameUUIDFromBytes(buffer.array());
    }
}
