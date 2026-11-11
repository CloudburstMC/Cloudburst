package org.cloudburstmc.server.item;

import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.nbt.NBTInputStream;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtUtils;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.data.definitions.SimpleItemDefinition;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.data.inventory.descriptor.ComplexAliasDescriptor;
import org.cloudburstmc.protocol.bedrock.data.inventory.descriptor.DefaultDescriptor;
import org.cloudburstmc.protocol.bedrock.data.inventory.descriptor.ItemDescriptorWithCount;
import org.cloudburstmc.protocol.bedrock.data.inventory.descriptor.ItemTagDescriptor;
import org.cloudburstmc.server.block.BlockPalette;
import org.cloudburstmc.server.block.CloudBlockDefinition;
import org.cloudburstmc.server.registry.CloudItemRegistry;
import org.cloudburstmc.server.utils.Utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;

/**
 * Reads vanilla recipe items and encodes recipe packet definitions.
 */
@UtilityClass
@Log4j2
public class RecipeItemCodec {

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
            id = CloudItemRegistry.get().fromLegacy(Utils.toInt(data.get("legacyId")), Utils.toInt(data.getOrDefault("damage", 0)));
        } else if (data.containsKey("itemId")) {
            int auxValue = Utils.toInt(data.getOrDefault("auxValue", 0));
            id = CloudItemRegistry.get().fromLegacy(Utils.toInt(data.get("itemId")), auxValue == 32767 ? 0 : auxValue);
        } else {
            id = null;
        }

        if (id == null) {
            throw new IllegalStateException("Unable to decode item JSON: " + data);
        }

        int damage = Utils.toInt(data.getOrDefault("damage", data.getOrDefault("auxValue", 0)));
        return ItemStackNbtCodec.deserializeItem(id, (short) damage, Utils.toInt(data.getOrDefault("count", 1)), tag);
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
            ItemDefinition definition = CloudItemRegistry.get().getDefinition(id);
            if (definition == null) {
                log.warn("Unknown item '{}' in recipe descriptor", id);
                return ItemDescriptorWithCount.EMPTY;
            }
            return new ItemDescriptorWithCount(new DefaultDescriptor(definition, auxValue), count);
        }

        int auxValue = Utils.toInt(data.getOrDefault("auxValue", 0));
        int itemId = Utils.toInt(data.get("itemId"));

        Identifier id = CloudItemRegistry.get().fromLegacy(itemId, auxValue == 32767 ? 0 : auxValue);
        if (id == null) {
            log.warn("Unknown legacy item ID {} in recipe descriptor", itemId);
            return ItemDescriptorWithCount.EMPTY;
        }

        ItemDefinition definition = CloudItemRegistry.get().getDefinition(id);
        return new ItemDescriptorWithCount(new DefaultDescriptor(definition, auxValue), count);
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
        int damage = CloudItemRegistry.get().getSerializer(item.getType()).getAuxValue(item);
        ItemDefinition rawDefinition = CloudItemRegistry.get().getDefinition(identifier, damage);

        ItemDefinition recipeDefinition = new SimpleItemDefinition(
                rawDefinition.getIdentifier(), rawDefinition.getRuntimeId(), false);

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

        return ItemStackPacketCodec.encode(item, recipeDefinition, damage, blockDefinition);
    }

    private static boolean isRecipeBlockItem(ItemDefinition definition, Identifier identifier) {
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

    public static int getItemHash(ItemStack item) {
        return Objects.hash(System.identityHashCode(item.getType().getId()), item.getCount());
    }
}
