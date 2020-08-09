package org.cloudburstmc.server.item;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.Identifier;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
abstract public class ItemArmor extends Item implements ItemDurable {

    public static final int TIER_LEATHER = 1;
    public static final int TIER_IRON = 2;
    public static final int TIER_CHAIN = 3;
    public static final int TIER_GOLD = 4;
    public static final int TIER_DIAMOND = 5;
    public static final int TIER_OTHER = 6;

    private boolean unbreakable;

    public ItemArmor(Identifier id) {
        super(id);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean isArmor() {
        return true;
    }

    @Override
    public boolean onClickAir(Player player, Vector3f directionVector) {
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
            player.getInventory().clear(player.getInventory().getHeldItemIndex());
            switch (this.getTier()) {
                case TIER_CHAIN:
                    player.getLevel().addLevelSoundEvent(player.getPosition(), SoundEvent.ARMOR_EQUIP_CHAIN);
                    break;
                case TIER_DIAMOND:
                    player.getLevel().addLevelSoundEvent(player.getPosition(), SoundEvent.ARMOR_EQUIP_DIAMOND);
                    break;
                case TIER_GOLD:
                    player.getLevel().addLevelSoundEvent(player.getPosition(), SoundEvent.ARMOR_EQUIP_GOLD);
                    break;
                case TIER_IRON:
                    player.getLevel().addLevelSoundEvent(player.getPosition(), SoundEvent.ARMOR_EQUIP_IRON);
                    break;
                case TIER_LEATHER:
                    player.getLevel().addLevelSoundEvent(player.getPosition(), SoundEvent.ARMOR_EQUIP_LEATHER);
                    break;
                case TIER_OTHER:
                default:
                    player.getLevel().addLevelSoundEvent(player.getPosition(), SoundEvent.ARMOR_EQUIP_GENERIC);
            }
        }

        return this.getCount() == 0;
    }

    @Override
    public int getEnchantAbility() {
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

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);

        tag.listenForBoolean("Unbreakable", v -> this.unbreakable = v);
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);

        tag.putBoolean("Unbreakable", this.unbreakable);
    }

    @Override
    public boolean isUnbreakable() {
        return unbreakable;
    }

    public void setUnbreakable(boolean unbreakable) {
        this.unbreakable = unbreakable;
    }
}
