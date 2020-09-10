package org.cloudburstmc.server.item.behavior;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.TierType;
import org.cloudburstmc.server.player.Player;

import static org.cloudburstmc.server.item.TierTypes.*;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
abstract public class ItemArmorBehavior extends CloudItemBehavior {

    private static final Reference2ObjectMap<TierType, SoundEvent> equipSounds = new Reference2ObjectOpenHashMap<>();

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
    public boolean onClickAir(ItemStack item, Vector3f directionVector, Player player) {
        boolean equip = false;
        if (this.isHelmet() && player.getInventory().getHelmet().isNull()) {
            if (player.getInventory().setHelmet(this)) {
                equip = true;
            }
        } else if (this.isChestplate() && player.getInventory().getChestplate().isNull()) {
            if (player.getInventory().setChestplate(this)) {
                equip = true;
            }
        } else if (this.isLeggings() && player.getInventory().getLeggings().isNull()) {
            if (player.getInventory().setLeggings(this)) {
                equip = true;
            }
        } else if (this.isBoots() && player.getInventory().getBoots().isNull()) {
            if (player.getInventory().setBoots(this)) {
                equip = true;
            }
        }
        if (equip) {
            player.getInventory().decrementCount(player.getInventory().getHeldItemIndex());
            getTier(item).ifPresent((tier) ->
                    player.getLevel().addLevelSoundEvent(player.getPosition(), equipSounds.getOrDefault(tier, SoundEvent.ARMOR_EQUIP_GENERIC))
            );
        }

        return item.getAmount() == 0;
    }

    @Override
    public int getEnchantAbility(ItemStack item) {
        switch (this.getTier()) {
            case TIER_CHAIN:
                return 12;
            case TIER_LEATHER:
                return 15;
            case TIER_DIAMOND:
                return 10;
            case TIER_GOLD:
                return 25;
            case TIER_IRON:
                return 9;
        }

        return 0;
    }
}
