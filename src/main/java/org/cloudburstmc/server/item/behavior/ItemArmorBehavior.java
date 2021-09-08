package org.cloudburstmc.server.item.behavior;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.api.item.ArmorPartType;
import org.cloudburstmc.api.item.ArmorPartTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.TierType;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import static org.cloudburstmc.api.item.TierTypes.*;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
@RequiredArgsConstructor
abstract public class ItemArmorBehavior extends CloudItemBehavior {

    private static final Reference2ObjectMap<TierType, SoundEvent> equipSounds = new Reference2ObjectOpenHashMap<>();

    private final TierType tierType;
    private final ArmorPartType armorPartType;

    static {
        equipSounds.put(CHAINMAIL, SoundEvent.ARMOR_EQUIP_CHAIN);
        equipSounds.put(DIAMOND, SoundEvent.ARMOR_EQUIP_DIAMOND);
        equipSounds.put(GOLD, SoundEvent.ARMOR_EQUIP_GOLD);
        equipSounds.put(IRON, SoundEvent.ARMOR_EQUIP_IRON);
        equipSounds.put(LEATHER, SoundEvent.ARMOR_EQUIP_LEATHER);
        equipSounds.put(NETHERITE, SoundEvent.EQUIP_NETHERITE);
    }

    @Override
    public boolean isArmor() {
        return true;
    }

    @Override
    public boolean isHelmet() {
        return this.armorPartType == ArmorPartTypes.HELMET;
    }

    @Override
    public boolean isChestplate() {
        return this.armorPartType == ArmorPartTypes.CHESTPLATE;
    }

    @Override
    public boolean isLeggings() {
        return this.armorPartType == ArmorPartTypes.LEGGINGS;
    }

    @Override
    public boolean isBoots() {
        return this.armorPartType == ArmorPartTypes.BOOTS;
    }

    @Override
    public boolean onClickAir(ItemStack item, Vector3f directionVector, Player p) {
        CloudPlayer player = (CloudPlayer) p;
        boolean equip = false;
        ItemStack oldSlotItem = CloudItemRegistry.get().AIR;

        if (this.isHelmet()) {
            oldSlotItem = player.getInventory().getHelmet();
            if (player.getInventory().setHelmet(item)) {
                equip = true;
            }
        } else if (this.isChestplate()) {
            oldSlotItem = player.getInventory().getChestplate();
            if (player.getInventory().setChestplate(item)) {
                equip = true;
            }
        } else if (this.isLeggings()) {
            oldSlotItem = player.getInventory().getLeggings();
            if (player.getInventory().setLeggings(item)) {
                equip = true;
            }
        } else if (this.isBoots()) {
            oldSlotItem = player.getInventory().getBoots();
            if (player.getInventory().setBoots(item)) {
                equip = true;
            }
        }
        if (equip) {
            player.getInventory().setItem(player.getInventory().getHeldItemIndex(), oldSlotItem);
            var tier = getTier(item);
            if (tier != null) {
                player.getLevel().addLevelSoundEvent(player.getPosition(), equipSounds.getOrDefault(tier, SoundEvent.ARMOR_EQUIP_GENERIC));
            }
        }

        return item.getAmount() == 0;
    }

    @Override
    public int getEnchantAbility(ItemStack item) {
        return item.getBehavior().getTier(item).getArmorEnchantAbility();
    }
}
