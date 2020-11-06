package org.cloudburstmc.server.item.behavior;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.cloudburstmc.server.item.ArmorPartType;
import org.cloudburstmc.server.item.ArmorPartTypes;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.TierType;
import org.cloudburstmc.server.player.Player;

import static org.cloudburstmc.server.item.TierTypes.*;

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
        equipSounds.put(NETHERITE, SoundEvent.ARMOR_EQUIP_DIAMOND); //TODO: sound
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
    public boolean onClickAir(ItemStack item, Vector3f directionVector, Player player) {
        boolean equip = false;
        if (this.isHelmet() && player.getInventory().getHelmet().isNull()) {
            if (player.getInventory().setHelmet(item)) {
                equip = true;
            }
        } else if (this.isChestplate() && player.getInventory().getChestplate().isNull()) {
            if (player.getInventory().setChestplate(item)) {
                equip = true;
            }
        } else if (this.isLeggings() && player.getInventory().getLeggings().isNull()) {
            if (player.getInventory().setLeggings(item)) {
                equip = true;
            }
        } else if (this.isBoots() && player.getInventory().getBoots().isNull()) {
            if (player.getInventory().setBoots(item)) {
                equip = true;
            }
        }
        if (equip) {
            player.getInventory().decrementCount(player.getInventory().getHeldItemIndex());
            val tier = getTier(item);
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
