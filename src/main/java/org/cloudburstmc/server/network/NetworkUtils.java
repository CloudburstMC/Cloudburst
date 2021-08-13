package org.cloudburstmc.server.network;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.protocol.bedrock.data.AttributeData;
import com.nukkitx.protocol.bedrock.data.GameRuleData;
import com.nukkitx.protocol.bedrock.data.inventory.ContainerType;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import com.nukkitx.protocol.bedrock.data.inventory.StackRequestSlotInfoData;
import com.nukkitx.protocol.bedrock.packet.ItemStackResponsePacket;
import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.entity.Attribute;
import org.cloudburstmc.api.inventory.InventoryType;
import org.cloudburstmc.api.item.data.Damageable;
import org.cloudburstmc.api.level.gamerule.GameRuleMap;
import org.cloudburstmc.api.potion.EffectType;
import org.cloudburstmc.api.potion.EffectTypes;
import org.cloudburstmc.api.potion.PotionType;
import org.cloudburstmc.api.potion.PotionTypes;
import org.cloudburstmc.server.inventory.BaseInventory;
import org.cloudburstmc.server.item.CloudItemStack;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

@UtilityClass
public class NetworkUtils {

    private BiMap<PotionType, Short> potionTypeMap = HashBiMap.create();
    private BiMap<EffectType, Byte> effectTypeMap = HashBiMap.create();
    private Map<InventoryType, ContainerType> inventoryTypeMap = new IdentityHashMap<>();

    static {
        potionTypeMap.put(PotionTypes.WATER, (short) 0);
        potionTypeMap.put(PotionTypes.MUNDANE, (short) 1);
        potionTypeMap.put(PotionTypes.LONG_MUNDANE, (short) 2);
        potionTypeMap.put(PotionTypes.THICK, (short) 3);
        potionTypeMap.put(PotionTypes.AWKWARD, (short) 4);
        potionTypeMap.put(PotionTypes.NIGHT_VISION, (short) 5);
        potionTypeMap.put(PotionTypes.LONG_NIGHT_VISION, (short) 6);
        potionTypeMap.put(PotionTypes.INVISIBILITY, (short) 7);
        potionTypeMap.put(PotionTypes.LONG_INVISIBILITY, (short) 8);
        potionTypeMap.put(PotionTypes.LEAPING, (short) 9);
        potionTypeMap.put(PotionTypes.LONG_LEAPING, (short) 10);
        potionTypeMap.put(PotionTypes.STRONG_LEAPING, (short) 11);
        potionTypeMap.put(PotionTypes.FIRE_RESISTANCE, (short) 12);
        potionTypeMap.put(PotionTypes.LONG_FIRE_RESISTANCE, (short) 13);
        potionTypeMap.put(PotionTypes.SWIFTNESS, (short) 14);
        potionTypeMap.put(PotionTypes.LONG_SWIFTNESS, (short) 15);
        potionTypeMap.put(PotionTypes.STRONG_SWIFTNESS, (short) 16);
        potionTypeMap.put(PotionTypes.SLOWNESS, (short) 17);
        potionTypeMap.put(PotionTypes.LONG_SLOWNESS, (short) 18);
        potionTypeMap.put(PotionTypes.WATER_BREATHING, (short) 19);
        potionTypeMap.put(PotionTypes.LONG_WATER_BREATHING, (short) 20);
        potionTypeMap.put(PotionTypes.HEALING, (short) 21);
        potionTypeMap.put(PotionTypes.STRONG_HEALING, (short) 22);
        potionTypeMap.put(PotionTypes.HARMING, (short) 23);
        potionTypeMap.put(PotionTypes.STRONG_HARMING, (short) 24);
        potionTypeMap.put(PotionTypes.POISON, (short) 25);
        potionTypeMap.put(PotionTypes.LONG_POISON, (short) 26);
        potionTypeMap.put(PotionTypes.STRONG_POISON, (short) 27);
        potionTypeMap.put(PotionTypes.REGENERATION, (short) 28);
        potionTypeMap.put(PotionTypes.LONG_REGENERATION, (short) 29);
        potionTypeMap.put(PotionTypes.STRONG_REGENERATION, (short) 30);
        potionTypeMap.put(PotionTypes.STRENGTH, (short) 31);
        potionTypeMap.put(PotionTypes.LONG_STRENGTH, (short) 32);
        potionTypeMap.put(PotionTypes.STRONG_STRENGTH, (short) 33);
        potionTypeMap.put(PotionTypes.WEAKNESS, (short) 34);
        potionTypeMap.put(PotionTypes.LONG_WEAKNESS, (short) 35);
        potionTypeMap.put(PotionTypes.WITHER, (short) 36);
        potionTypeMap.put(PotionTypes.TURTLE_MASTER, (short) 37);
        potionTypeMap.put(PotionTypes.LONG_TURTLE_MASTER, (short) 38);
        potionTypeMap.put(PotionTypes.STRONG_TURTLE_MASTER, (short) 39);
        potionTypeMap.put(PotionTypes.SLOW_FALLING, (short) 40);
        potionTypeMap.put(PotionTypes.LONG_SLOW_FALLING, (short) 41);

        effectTypeMap.put(EffectTypes.SPEED, (byte) 1);
        effectTypeMap.put(EffectTypes.SLOWNESS, (byte) 2);
        effectTypeMap.put(EffectTypes.HASTE, (byte) 3);
        effectTypeMap.put(EffectTypes.MINING_FATIGUE, (byte) 4);
        effectTypeMap.put(EffectTypes.STRENGTH, (byte) 5);
        effectTypeMap.put(EffectTypes.INSTANT_HEALTH, (byte) 6);
        effectTypeMap.put(EffectTypes.INSTANT_DAMAGE, (byte) 7);
        effectTypeMap.put(EffectTypes.JUMP_BOOST, (byte) 8);
        effectTypeMap.put(EffectTypes.NAUSEA, (byte) 9);
        effectTypeMap.put(EffectTypes.REGENERATION, (byte) 10);
        effectTypeMap.put(EffectTypes.RESISTANCE, (byte) 11);
        effectTypeMap.put(EffectTypes.FIRE_RESISTANCE, (byte) 12);
        effectTypeMap.put(EffectTypes.WATER_BREATHING, (byte) 13);
        effectTypeMap.put(EffectTypes.INVISIBILITY, (byte) 14);
        effectTypeMap.put(EffectTypes.BLINDNESS, (byte) 15);
        effectTypeMap.put(EffectTypes.NIGHT_VISION, (byte) 16);
        effectTypeMap.put(EffectTypes.HUNGER, (byte) 17);
        effectTypeMap.put(EffectTypes.WEAKNESS, (byte) 18);
        effectTypeMap.put(EffectTypes.POISON, (byte) 19);
        effectTypeMap.put(EffectTypes.WITHER, (byte) 20);
        effectTypeMap.put(EffectTypes.HEALTH_BOOST, (byte) 21);
        effectTypeMap.put(EffectTypes.ABSORPTION, (byte) 22);
        effectTypeMap.put(EffectTypes.SATURATION, (byte) 23);
        effectTypeMap.put(EffectTypes.LEVITATION, (byte) 24);
        effectTypeMap.put(EffectTypes.FATAL_POISON, (byte) 25);
        effectTypeMap.put(EffectTypes.CONDUIT_POWER, (byte) 26);
        effectTypeMap.put(EffectTypes.SLOW_FALLING, (byte) 27);
        effectTypeMap.put(EffectTypes.BAD_OMEN, (byte) 28);
        effectTypeMap.put(EffectTypes.VILLAGE_HERO, (byte) 29);

        inventoryTypeMap.put(InventoryType.CHEST, ContainerType.CONTAINER);
        inventoryTypeMap.put(InventoryType.ENDER_CHEST, ContainerType.CONTAINER);
        inventoryTypeMap.put(InventoryType.DOUBLE_CHEST, ContainerType.CONTAINER);
        inventoryTypeMap.put(InventoryType.PLAYER, ContainerType.INVENTORY);
        inventoryTypeMap.put(InventoryType.FURNACE, ContainerType.FURNACE);
        inventoryTypeMap.put(InventoryType.CRAFTING, ContainerType.WORKBENCH);
        inventoryTypeMap.put(InventoryType.WORKBENCH, ContainerType.WORKBENCH);
        inventoryTypeMap.put(InventoryType.BREWING_STAND, ContainerType.BREWING_STAND);
        inventoryTypeMap.put(InventoryType.ANVIL, ContainerType.ANVIL);
        inventoryTypeMap.put(InventoryType.ENCHANT_TABLE, ContainerType.ENCHANTMENT);
        inventoryTypeMap.put(InventoryType.DISPENSER, ContainerType.DISPENSER);
        inventoryTypeMap.put(InventoryType.DROPPER, ContainerType.DROPPER);
        inventoryTypeMap.put(InventoryType.HOPPER, ContainerType.HOPPER);
        inventoryTypeMap.put(InventoryType.UI, ContainerType.INVENTORY); // Might not be needed anymore?
        inventoryTypeMap.put(InventoryType.SHULKER_BOX, ContainerType.CONTAINER);
        inventoryTypeMap.put(InventoryType.BEACON, ContainerType.BEACON);
        inventoryTypeMap.put(InventoryType.BLAST_FURNACE, ContainerType.BLAST_FURNACE);
        inventoryTypeMap.put(InventoryType.SMOKER, ContainerType.SMOKER);
        inventoryTypeMap.put(InventoryType.BARREL, ContainerType.CONTAINER);

    }

    public static AttributeData attributeToNetwork(Attribute attr) {
        return new AttributeData(attr.getName(), attr.getMinValue(), attr.getMaxValue(), attr.getValue(), attr.getDefaultValue());
    }

    public static void gameRulesToNetwork(GameRuleMap gameRules, List<GameRuleData<?>> networkRules) {
        gameRules.forEach((rule, o) -> {
            networkRules.add(new GameRuleData<>(rule.getName(), o));
        });
    }

    public static short potionToNetwork(PotionType type) {
        return potionTypeMap.get(type);
    }

    public static PotionType potionFromLegacy(short potionId) {
        return potionTypeMap.inverse().get(potionId);
    }

    public static byte effectToNetwork(EffectType type) {
        return effectTypeMap.get(type);
    }

    public static EffectType effectFromLegacy(byte effectId) {
        return effectTypeMap.inverse().get(effectId);
    }

    public static ContainerType inventoryToNetwork(InventoryType type) {
        return inventoryTypeMap.get(type);
    }

    public static ItemStackResponsePacket.ItemEntry itemStackToNetwork(StackRequestSlotInfoData data, BaseInventory inv) {
        int durablility = 0;
        CloudItemStack item = inv.getItem(data.getSlot());
        if (item.hasMetadata(Damageable.class)) {
            durablility = item.getMetadata(Damageable.class).getDurability();
        }

        if (item.getStackNetworkId() == -1) {
            item.getNetworkData(); // Will regen and assign stack ID
        }

        return new ItemStackResponsePacket.ItemEntry(data.getSlot(),
                data.getSlot(),
                (byte) item.getAmount(),
                item.getStackNetworkId(),
                item.getName() == null ? "" : item.getName(),
                durablility);

    }

    public static CloudItemStack itemStackFromNetwork(ItemData data) {
        int runtimeId = data.getId();
        return ItemUtils.deserializeItem(CloudItemRegistry.get().getIdentifier(runtimeId), (short) data.getDamage(), data.getCount(), data.getTag() == null ? NbtMap.EMPTY : data.getTag());
    }
}
