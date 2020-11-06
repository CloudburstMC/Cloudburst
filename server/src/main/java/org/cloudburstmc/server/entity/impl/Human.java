package org.cloudburstmc.server.entity.impl;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.nbt.NbtType;
import com.nukkitx.protocol.bedrock.BedrockPacket;
import com.nukkitx.protocol.bedrock.data.PlayerPermission;
import com.nukkitx.protocol.bedrock.data.command.CommandPermission;
import com.nukkitx.protocol.bedrock.data.entity.EntityLinkData;
import com.nukkitx.protocol.bedrock.data.skin.AnimatedTextureType;
import com.nukkitx.protocol.bedrock.data.skin.AnimationData;
import com.nukkitx.protocol.bedrock.data.skin.ImageData;
import com.nukkitx.protocol.bedrock.data.skin.SerializedSkin;
import com.nukkitx.protocol.bedrock.packet.AddPlayerPacket;
import com.nukkitx.protocol.bedrock.packet.RemoveEntityPacket;
import com.nukkitx.protocol.bedrock.packet.SetEntityLinkPacket;
import lombok.val;
import org.cloudburstmc.server.enchantment.EnchantmentInstance;
import org.cloudburstmc.server.enchantment.EnchantmentTypes;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.event.entity.EntityDamageByEntityEvent;
import org.cloudburstmc.server.event.entity.EntityDamageEvent;
import org.cloudburstmc.server.inventory.InventoryHolder;
import org.cloudburstmc.server.inventory.PlayerEnderChestInventory;
import org.cloudburstmc.server.inventory.PlayerInventory;
import org.cloudburstmc.server.item.CloudItemStack;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ItemStacks;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.item.data.Damageable;
import org.cloudburstmc.server.level.Location;
import org.cloudburstmc.server.math.NukkitMath;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static com.nukkitx.protocol.bedrock.data.entity.EntityFlag.*;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class Human extends EntityCreature implements InventoryHolder {

    protected UUID identity;
    private final PlayerInventory inventory = new PlayerInventory(this);
    private final PlayerEnderChestInventory enderChestInventory = new PlayerEnderChestInventory(this);

    protected SerializedSkin skin;

    public Human(EntityType<Human> type, Location location) {
        super(type, location);
    }

    @Override
    public float getWidth() {
        return 0.6f;
    }

    @Override
    public float getLength() {
        return 0.6f;
    }

    @Override
    public float getHeight() {
        return 1.8f;
    }

    @Override
    public float getEyeHeight() {
        return 1.62f;
    }

    @Override
    protected float getBaseOffset() {
        return this.getEyeHeight();
    }

    public SerializedSkin getSkin() {
        return skin;
    }

    public UUID getServerId() {
        return identity;
    }

    public void setServerId(UUID uuid) {
        this.identity = uuid;
    }

    public void setSkin(SerializedSkin skin) {
        this.skin = skin;
    }

    @Override
    public PlayerInventory getInventory() {
        return inventory;
    }

    public PlayerEnderChestInventory getEnderChestInventory() {
        return enderChestInventory;
    }

    @Override
    protected void initEntity() {
        //this.data.setBoolean(EntityData.CAN_START_SLEEP, false); // TODO: what did this change to?
        this.data.setFlag(HAS_GRAVITY, true);

        super.initEntity();
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);

        if (!(this instanceof Player)) {
            tag.listenForString("NameTag", this::setNameTag);


            if (tag.containsKey("Skin") && tag.get("Skin") instanceof NbtMap) {
                NbtMap skinTag = tag.getCompound("Skin");

                SerializedSkin.Builder skin = SerializedSkin.builder();

                skinTag.listenForString("ModelId", skin::skinId);
                if (skinTag.containsKey("Data")) {
                    byte[] data = skinTag.getByteArray("Data");
                    if (skinTag.containsKey("SkinImageWidth") && skinTag.containsKey("SkinImageHeight")) {
                        int width = skinTag.getInt("SkinImageWidth");
                        int height = skinTag.getInt("SkinImageHeight");
                        skin.skinData(ImageData.of(width, height, data));
                    } else {
                        skin.skinData(ImageData.of(data));
                    }
                }

                skinTag.listenForString("CapeId", skin::capeId);
                if (skinTag.containsKey("CapeData")) {
                    byte[] data = skinTag.getByteArray("CapeData");
                    if (skinTag.containsKey("CapeImageWidth") && skinTag.containsKey("CapeImageHeight")) {
                        int width = skinTag.getInt("CapeImageWidth");
                        int height = skinTag.getInt("CapeImageHeight");
                        skin.capeData(ImageData.of(width, height, data));
                    } else {
                        skin.capeData(ImageData.of(data));
                    }
                }
                skinTag.listenForString("GeometryName", skin::geometryName);
                skinTag.listenForString("SkinResourcePatch", skin::skinResourcePatch);
                skinTag.listenForByteArray("GeometryData", bytes -> skin.geometryData(new String(bytes, UTF_8)));
                skinTag.listenForByteArray("AnimationData", bytes -> skin.animationData(new String(bytes, UTF_8)));
                skinTag.listenForBoolean("PremiumSkin", skin::premium);
                skinTag.listenForBoolean("PersonaSkin", skin::persona);
                skinTag.listenForBoolean("CapeOnClassicSkin", skin::capeOnClassic);
                if (skinTag.containsKey("AnimatedImageData")) {
                    List<NbtMap> list = skinTag.getList("AnimatedImageData", NbtType.COMPOUND);
                    List<AnimationData> animations = new ArrayList<>();
                    for (NbtMap animationTag : list) {
                        float frames = animationTag.getFloat("Frames");
                        AnimatedTextureType type = AnimatedTextureType.values()[animationTag.getInt("Type")];
                        byte[] image = animationTag.getByteArray("Image");
                        int width = animationTag.getInt("ImageWidth");
                        int height = animationTag.getInt("ImageHeight");
                        animations.add(new AnimationData(ImageData.of(width, height, image), type, frames));
                    }
                    skin.animations(animations);
                }
                this.setSkin(skin.build());
            }

            this.identity = Utils.dataToUUID(String.valueOf(this.getUniqueId()).getBytes(UTF_8), this.getSkin()
                    .getSkinData().getImage(), this.getNameTag().getBytes(UTF_8));
        }

        tag.listenForList("Inventory", NbtType.COMPOUND, items -> {
            for (NbtMap itemTag : items) {
                int slot = itemTag.getByte("Slot");
                if (slot >= 0 && slot < 9) { //hotbar
                    //Old hotbar saving stuff, useless now
                } else if (slot >= 100 && slot < 105) {
                    this.inventory.setItem(this.inventory.getSize() + slot - 100, ItemUtils.deserializeItem(itemTag));
                } else {
                    this.inventory.setItem(slot - 9, ItemUtils.deserializeItem(itemTag));
                }
            }
        });

        tag.listenForList("EnderItems", NbtType.COMPOUND, items -> {
            for (NbtMap itemTag : items) {
                this.enderChestInventory.setItem(itemTag.getByte("Slot"), ItemUtils.deserializeItem(itemTag));
            }
        });
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);

        List<NbtMap> inventoryItems = new ArrayList<>();
        int slotCount = PlayerInventory.SURVIVAL_SLOTS + 9;
        for (int slot = 9; slot < slotCount; ++slot) {
            ItemStack item = this.inventory.getItem(slot - 9);
            if (!item.isNull()) {
                inventoryItems.add(ItemUtils.serializeItem(item, slot));
            }
        }

        for (int slot = 100; slot < 105; ++slot) {
            ItemStack item = this.inventory.getItem(this.inventory.getSize() + slot - 100);
            if (!item.isNull()) {
                inventoryItems.add(ItemUtils.serializeItem(item, slot));
            }
        }

        tag.putList("Inventory", NbtType.COMPOUND, inventoryItems);

        List<NbtMap> enderItems = new ArrayList<>();
        for (int slot = 0; slot < 27; ++slot) {
            ItemStack item = this.enderChestInventory.getItem(slot);
            if (item != null && !item.isNull()) {
                enderItems.add(ItemUtils.serializeItem(item, slot));
            }
        }
        tag.putList("EnderItems", NbtType.COMPOUND, enderItems);

        if (skin != null) {
            NbtMapBuilder skinTag = NbtMap.builder()
                    .putByteArray("Data", this.getSkin().getSkinData().getImage())
                    .putInt("SkinImageWidth", this.getSkin().getSkinData().getWidth())
                    .putInt("SkinImageHeight", this.getSkin().getSkinData().getHeight())
                    .putString("ModelId", this.getSkin().getSkinId())
                    .putString("CapeId", this.getSkin().getCapeId())
                    .putByteArray("CapeData", this.getSkin().getCapeData().getImage())
                    .putInt("CapeImageWidth", this.getSkin().getCapeData().getWidth())
                    .putInt("CapeImageHeight", this.getSkin().getCapeData().getHeight())
                    .putByteArray("SkinResourcePatch", this.getSkin().getSkinResourcePatch().getBytes(UTF_8))
                    .putByteArray("GeometryData", this.getSkin().getGeometryData().getBytes(UTF_8))
                    .putByteArray("AnimationData", this.getSkin().getAnimationData().getBytes(UTF_8))
                    .putBoolean("PremiumSkin", this.getSkin().isPremium())
                    .putBoolean("PersonaSkin", this.getSkin().isPersona())
                    .putBoolean("CapeOnClassicSkin", this.getSkin().isCapeOnClassic());
            List<AnimationData> animations = this.getSkin().getAnimations();
            if (!animations.isEmpty()) {
                List<NbtMap> animationsTag = new ArrayList<>();
                for (AnimationData animation : animations) {
                    animationsTag.add(NbtMap.builder()
                            .putFloat("Frames", animation.getFrames())
                            .putInt("Type", animation.getTextureType().ordinal())
                            .putInt("ImageWidth", animation.getImage().getWidth())
                            .putInt("ImageHeight", animation.getImage().getHeight())
                            .putByteArray("Image", animation.getImage().getImage())
                            .build());
                }
                skinTag.putList("AnimationImageData", NbtType.COMPOUND, animationsTag);
            }
            tag.putCompound("Skin", skinTag.build());
        }
    }

    @Override
    public String getName() {
        return this.getNameTag();
    }

    @Override
    public void spawnTo(Player player) {
        if (this != player && !this.hasSpawned.contains(player)) {
            this.hasSpawned.add(player);

            if (!this.skin.isValid()) {
                throw new IllegalStateException(this.getClass().getSimpleName() + " must have a valid skin set");
            }

            if (this instanceof Player)
                this.server.updatePlayerListData(this.getServerId(), this.getUniqueId(), this.getName(), this.skin, ((Player) this).getXuid(), new Player[]{player});
            else
                this.server.updatePlayerListData(this.getServerId(), this.getUniqueId(), this.getName(), this.skin, new Player[]{player});

            player.sendPacket(createAddEntityPacket());

            this.inventory.sendArmorContents(player);

            if (this.vehicle != null) {
                SetEntityLinkPacket packet = new SetEntityLinkPacket();
                EntityLinkData link = new EntityLinkData(this.vehicle.getUniqueId(), this.getUniqueId(), EntityLinkData.Type.RIDER, true, false);
                packet.setEntityLink(link);

                player.sendPacket(packet);
            }

            if (!(this instanceof Player)) {
                this.server.removePlayerListData(this.getServerId(), new Player[]{player});
            }
        }
    }

    @Override
    protected BedrockPacket createAddEntityPacket() {
        AddPlayerPacket packet = new AddPlayerPacket();
        packet.setUuid(this.getServerId());
        packet.setUsername(this.getName());
        packet.setUniqueEntityId(this.getUniqueId());
        packet.setRuntimeEntityId(this.getRuntimeId());
        packet.setPosition(this.getPosition());
        packet.setMotion(this.getMotion());
        packet.setRotation(Vector3f.from(this.getPitch(), this.getYaw(), this.getYaw()));
        packet.setHand(((CloudItemStack) this.getInventory().getItemInHand()).getNetworkData());
        packet.setPlatformChatId("");
        packet.setDeviceId("");
        packet.getAdventureSettings().setCommandPermission(CommandPermission.NORMAL);
        packet.getAdventureSettings().setPlayerPermission(PlayerPermission.MEMBER);
        this.getData().putAllIn(packet.getMetadata());
        return packet;
    }

    @Override
    public void despawnFrom(Player player) {
        if (this.hasSpawned.contains(player)) {
            RemoveEntityPacket packet = new RemoveEntityPacket();
            packet.setUniqueEntityId(this.getUniqueId());
            player.sendPacket(packet);
            this.hasSpawned.remove(player);
        }
    }

    @Override
    public void close() {
        if (!this.closed) {
            if (!(this instanceof Player) || ((Player) this).loggedIn) {
                for (Player viewer : this.inventory.getViewers()) {
                    viewer.removeWindow(this.inventory);
                }
            }

            super.close();
        }
    }

    @Override
    public boolean attack(EntityDamageEvent source) {
        if (this.isClosed() || !this.isAlive()) {
            return false;
        }

        if (source.getCause() != EntityDamageEvent.DamageCause.VOID && source.getCause() != EntityDamageEvent.DamageCause.CUSTOM && source.getCause() != EntityDamageEvent.DamageCause.MAGIC) {
            int armorPoints = 0;
            int epf = 0;
            int toughness = 0;

            for (ItemStack armor : inventory.getArmorContents()) {
                armorPoints += armor.getBehavior().getArmorPoints(armor);
                epf += calculateEnchantmentProtectionFactor(armor, source);
                //toughness += armor.getToughness();
            }

            if (source.canBeReducedByArmor()) {
                source.setDamage(-source.getFinalDamage() * armorPoints * 0.04f, EntityDamageEvent.DamageModifier.ARMOR);
            }

            source.setDamage(-source.getFinalDamage() * Math.min(NukkitMath.ceilFloat(Math.min(epf, 25) * ((float) ThreadLocalRandom.current().nextInt(50, 100) / 100)), 20) * 0.04f,
                    EntityDamageEvent.DamageModifier.ARMOR_ENCHANTMENTS);

            source.setDamage(-Math.min(this.getAbsorption(), source.getFinalDamage()), EntityDamageEvent.DamageModifier.ABSORPTION);
        }

        if (super.attack(source)) {
            Entity damager = null;

            if (source instanceof EntityDamageByEntityEvent) {
                damager = ((EntityDamageByEntityEvent) source).getDamager();
            }

            for (int slot = 0; slot < 4; slot++) {
                ItemStack armor = this.inventory.getArmorItem(slot);

                if (armor.hasEnchantments()) {
                    if (damager != null) {
                        for (EnchantmentInstance enchantment : armor.getEnchantments().values()) {
                            enchantment.getBehavior().doPostAttack(enchantment, damager, this);
                        }
                    }

                    EnchantmentInstance durability = armor.getEnchantment(EnchantmentTypes.DURABILITY);
                    if (durability != null && durability.getLevel() > 0 && (100 / (durability.getLevel() + 1)) <= new Random().nextInt(100))
                        continue;
                }

                val damageable = armor.getMetadata(Damageable.class);

                if (damageable != null) {
                    if (damageable.isUnbreakable()) {
                        continue;
                    }

                    armor = armor.withData(damageable.damage());

                    if (damageable.getDurability() + 1 >= armor.getBehavior().getMaxDurability()) {
                        inventory.setArmorItem(slot, ItemStacks.AIR);
                    } else {
                        inventory.setArmorItem(slot, armor, true);
                    }
                }
            }

            return true;
        } else {
            return false;
        }
    }

    protected double calculateEnchantmentProtectionFactor(ItemStack item, EntityDamageEvent source) {
        if (!item.hasEnchantments()) {
            return 0;
        }

        double epf = 0;

        for (EnchantmentInstance ench : item.getEnchantments().values()) {
            epf += ench.getBehavior().getProtectionFactor(ench, source);
        }

        return epf;
    }

    @Override
    public void setOnFire(int seconds) {
        int level = 0;

        for (ItemStack armor : this.inventory.getArmorContents()) {
            EnchantmentInstance fireProtection = armor.getEnchantment(EnchantmentTypes.FIRE_PROTECTION);

            if (fireProtection != null && fireProtection.getLevel() > 0) {
                level = Math.max(level, fireProtection.getLevel());
            }
        }

        seconds = (int) (seconds * (1 - level * 0.15));

        super.setOnFire(seconds);
    }

    @Override
    public ItemStack[] getDrops() {
        if (this.inventory != null) {
            return this.inventory.getContents().values().toArray(new ItemStack[0]);
        }
        return new ItemStack[0];
    }

    public boolean isSneaking() {
        return this.data.getFlag(SNEAKING);
    }

    public void setSneaking(boolean value) {
        this.data.setFlag(SNEAKING, value);
    }

    public void setSneaking() {
        this.setSneaking(true);
    }

    public boolean isSwimming() {
        return this.data.getFlag(SWIMMING);
    }

    public void setSwimming(boolean value) {
        this.data.setFlag(SWIMMING, value);
    }

    public void setSwimming() {
        this.setSwimming(true);
    }

    public boolean isSprinting() {
        return this.data.getFlag(SPRINTING);
    }

    public void setSprinting(boolean value) {
        this.data.setFlag(SPRINTING, value);
    }

    public void setSprinting() {
        this.setSprinting(true);
    }

    public boolean isGliding() {
        return this.data.getFlag(GLIDING);
    }

    public void setGliding(boolean value) {
        this.data.setFlag(GLIDING, value);
    }

    public void setGliding() {
        this.setGliding(true);
    }

}
