package org.cloudburstmc.server.entity;

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
import org.cloudburstmc.api.enchantment.EnchantmentInstance;
import org.cloudburstmc.api.enchantment.EnchantmentTypes;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.Human;
import org.cloudburstmc.api.event.entity.EntityDamageByEntityEvent;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.data.Damageable;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.player.skin.Skin;
import org.cloudburstmc.server.item.CloudItemStack;
import org.cloudburstmc.server.math.NukkitMath;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudItemRegistry;
import org.cloudburstmc.server.utils.SkinUtils;
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
public class EntityHuman extends EntityCreature implements Human {

    protected UUID identity;


    protected Skin skin;

    public EntityHuman(EntityType<Human> type, Location location) {
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

    public Skin getSkin() {
        return skin;
    }

    public UUID getServerId() {
        return identity;
    }

    public void setServerId(UUID uuid) {
        this.identity = uuid;
    }

    public void setSkin(Skin skin) {
        this.skin = skin;
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

        if (!(this instanceof CloudPlayer)) {
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
                this.setSkin(SkinUtils.fromSerialized(skin.build()));
            }

            this.identity = Utils.dataToUUID(String.valueOf(this.getUniqueId()).getBytes(UTF_8), this.getSkin()
                    .getSkinData().getImage(), this.getNameTag().getBytes(UTF_8));
        }
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);

        if (this.skin != null) {
            SerializedSkin nbtSkin = SkinUtils.fromSkin(skin);
            NbtMapBuilder skinTag = NbtMap.builder()
                    .putByteArray("Data", nbtSkin.getSkinData().getImage())
                    .putInt("SkinImageWidth", nbtSkin.getSkinData().getWidth())
                    .putInt("SkinImageHeight", nbtSkin.getSkinData().getHeight())
                    .putString("ModelId", nbtSkin.getSkinId())
                    .putString("CapeId", nbtSkin.getCapeId())
                    .putByteArray("CapeData", nbtSkin.getCapeData().getImage())
                    .putInt("CapeImageWidth", nbtSkin.getCapeData().getWidth())
                    .putInt("CapeImageHeight", nbtSkin.getCapeData().getHeight())
                    .putByteArray("SkinResourcePatch", nbtSkin.getSkinResourcePatch().getBytes(UTF_8))
                    .putByteArray("GeometryData", nbtSkin.getGeometryData().getBytes(UTF_8))
                    .putByteArray("AnimationData", nbtSkin.getAnimationData().getBytes(UTF_8))
                    .putBoolean("PremiumSkin", nbtSkin.isPremium())
                    .putBoolean("PersonaSkin", nbtSkin.isPersona())
                    .putBoolean("CapeOnClassicSkin", nbtSkin.isCapeOnClassic());
            List<AnimationData> animations = nbtSkin.getAnimations();
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
    public void spawnTo(Player p) {
        CloudPlayer player = (CloudPlayer) p;
        if (this != player && !this.hasSpawned.contains(player)) {
            this.hasSpawned.add(player);

            if (!this.skin.isValid()) {
                throw new IllegalStateException(this.getClass().getSimpleName() + " must have a valid skin set");
            }

            if (this instanceof CloudPlayer)
                this.getServer().updatePlayerListData(this.getServerId(), this.getUniqueId(), this.getName(), ((CloudPlayer) this).getSerializedSkin(), ((CloudPlayer) this).getXuid(), new CloudPlayer[]{player});
            else
                this.getServer().updatePlayerListData(this.getServerId(), this.getUniqueId(), this.getName(), SkinUtils.fromSkin(this.skin), new CloudPlayer[]{player});

            player.sendPacket(createAddEntityPacket());

            this.getInventory().sendArmorContents(player);

            if (this.vehicle != null) {
                SetEntityLinkPacket packet = new SetEntityLinkPacket();
                EntityLinkData link = new EntityLinkData(this.vehicle.getUniqueId(), this.getUniqueId(), EntityLinkData.Type.RIDER, true, false);
                packet.setEntityLink(link);

                player.sendPacket(packet);
            }

            if (!(this instanceof CloudPlayer)) {
                this.server.removePlayerListData(this.getServerId(), new CloudPlayer[]{player});
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
            ((CloudPlayer) player).sendPacket(packet);
            this.hasSpawned.remove(player);
        }
    }

    @Override
    public void close() {
        if (!this.closed) {
            if (!(this instanceof CloudPlayer) || ((CloudPlayer) this).loggedIn) {
                for (CloudPlayer viewer : this.getInventory().getViewers()) {
                    viewer.removeWindow(this.getInventory());
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

            for (ItemStack armor : getInventory().getArmorContents()) {
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
                ItemStack armor = this.getInventory().getArmorItem(slot);

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
                        getInventory().setArmorItem(slot, CloudItemRegistry.AIR);
                    } else {
                        getInventory().setArmorItem(slot, armor, true);
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

        for (ItemStack armor : this.getInventory().getArmorContents()) {
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
        if (this.getInventory() != null) {
            return this.getInventory().getContents().values().toArray(new ItemStack[0]);
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
