package org.cloudburstmc.server.entity;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.Human;
import org.cloudburstmc.api.entity.damage.DamageTypeTags;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;
import org.cloudburstmc.api.inventory.view.ArmorView;
import org.cloudburstmc.api.item.ItemComponents;
import org.cloudburstmc.api.item.ItemKeys;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.player.skin.Skin;
import org.cloudburstmc.math.GenericMath;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.protocol.adventure.BedrockLegacyTextSerializer;
import org.cloudburstmc.protocol.bedrock.data.GameType;
import org.cloudburstmc.protocol.bedrock.data.PlayerPermission;
import org.cloudburstmc.protocol.bedrock.data.command.CommandPermission;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityLinkData;
import org.cloudburstmc.protocol.bedrock.data.skin.AnimatedTextureType;
import org.cloudburstmc.protocol.bedrock.data.skin.AnimationData;
import org.cloudburstmc.protocol.bedrock.data.skin.ImageData;
import org.cloudburstmc.protocol.bedrock.data.skin.SerializedSkin;
import org.cloudburstmc.protocol.bedrock.packet.*;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.player.CloudPlayerAbilities;
import org.cloudburstmc.server.utils.SkinUtils;
import org.cloudburstmc.server.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.*;

/**
 * Abstract base class for human-shaped entities such as players.
 * Adds skin, game type, permissions, and player inventory support.
 */
public class EntityHuman extends EntityCreature implements Human {
    private static final float STANDING_HEIGHT = 1.8f;
    private static final float SNEAKING_HEIGHT = 1.5f;
    private static final float CRAWLING_HEIGHT = 0.625f;
    private static final float SWIMMING_OR_GLIDING_HEIGHT = 0.6f;
    private static final float WIDTH = 0.6f;
    private static final float STANDING_EYE_HEIGHT = 1.62f;

    protected final CloudContainer container = new CloudContainer(36);
    protected UUID identity;
    protected Skin skin;

    public EntityHuman(EntityType<Human> type, Location location) {
        super(type, location);
    }

    @Override
    public float getWidth() {
        return WIDTH;
    }

    @Override
    public float getLength() {
        return WIDTH;
    }

    @Override
    public float getHeight() {
        if (this.isSwimming() || this.isGliding()) {
            return SWIMMING_OR_GLIDING_HEIGHT;
        } else if (this.isCrawling()) {
            return CRAWLING_HEIGHT;
        } else if (this.isSneaking()) {
            return SNEAKING_HEIGHT;
        }

        return STANDING_HEIGHT;
    }

    @Override
    public float getEyeHeight() {
        if (this.isSwimming() || this.isGliding() || this.isCrawling()) {
            return this.getHeight() * 0.67f;
        } else if (this.isSneaking()) {
            return SNEAKING_HEIGHT * 0.85f;
        }

        return STANDING_EYE_HEIGHT;
    }

    @Override
    public float getBaseOffset() {
        return STANDING_EYE_HEIGHT;
    }

    public Skin getSkin() {
        return skin;
    }

    public void setSkin(Skin skin) {
        this.skin = skin;
    }

    public UUID getServerId() {
        return identity;
    }

    public void setServerId(UUID uuid) {
        this.identity = uuid;
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

        tag.listenForList("Inventory", NbtType.COMPOUND, items -> {
            for (NbtMap itemTag : items) {
                this.container.setItem(itemTag.getByte("Slot"), ItemUtils.deserializeItem(itemTag));
            }
        });

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
                skinTag.listenForByteArray("SkinAnimationData", bytes -> skin.animationData(new String(bytes, UTF_8)));
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

        Skin currentSkin = this.getSkin();
        if (currentSkin != null) {
            SerializedSkin nbtSkin = SkinUtils.toSerialized(currentSkin);
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
                    .putByteArray("SkinAnimationData", nbtSkin.getAnimationData().getBytes(UTF_8))
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
                skinTag.putList("AnimatedImageData", NbtType.COMPOUND, animationsTag);
            }
            tag.putCompound("Skin", skinTag.build());
        }
    }

    @Override
    public String getName() {
        return this.getNameTag();
    }

    @Override
    public void spawnTo(CloudPlayer player) {
        if (this == player || this.hasSpawned.contains(player) || this.chunk == null || !player.isChunkSent(this.chunk.getX(), this.chunk.getZ())) {
            return;
        }

        Skin currentSkin = this.getSkin();
        if (currentSkin == null || !currentSkin.isValid()) {
            throw new IllegalStateException(this.getClass().getSimpleName() + " must have a valid skin set");
        }

        this.hasSpawned.add(player);

        SerializedSkin playerSkin = null;
        if (this instanceof CloudPlayer cloudPlayer) {
            SerializedSkin serializedSkin = cloudPlayer.getSerializedSkin();
            this.getServer().updatePlayerListData(this.getServerId(), this.getUniqueId(), BedrockLegacyTextSerializer.getInstance().serialize(cloudPlayer.displayName()), serializedSkin, cloudPlayer.getXuid(), new CloudPlayer[]{player});
            playerSkin = serializedSkin;
        } else {
            this.getServer().updatePlayerListData(this.getServerId(), this.getUniqueId(), this.getName(), SkinUtils.toSerialized(currentSkin), new CloudPlayer[]{player});
        }

        player.sendPacket(this.createAddEntityPacket());
        if (playerSkin != null) {
            player.sendPacket(this.createPlayerSkinPacket(playerSkin));
        }

//            this.getContainer().sendArmorContents(player); TODO: Fix this

        if (this.vehicle != null) {
            SetEntityLinkPacket packet = new SetEntityLinkPacket();
            EntityLinkData link = new EntityLinkData(this.vehicle.getUniqueId(), this.getUniqueId(), EntityLinkData.Type.RIDER, true, false, 0);
            packet.setEntityLink(link);

            player.sendPacket(packet);
        }

        if (!(this instanceof CloudPlayer)) {
            this.server.removePlayerListData(this.getServerId(), new CloudPlayer[]{player});
        }
    }

    private PlayerSkinPacket createPlayerSkinPacket(SerializedSkin skin) {
        PlayerSkinPacket packet = new PlayerSkinPacket();
        packet.setUuid(this.getServerId());
        packet.setSkin(skin);
        packet.setNewSkinName(skin.getSkinId());
        packet.setOldSkinName("");
        packet.setTrustedSkin(true);
        return packet;
    }

    @Override
    protected BedrockPacket createAddEntityPacket() {
        AddPlayerPacket packet = new AddPlayerPacket();
        packet.setUuid(this.getServerId());
        packet.setUsername(this.getName());
        packet.setUniqueEntityId(this.getUniqueId());
        packet.setRuntimeEntityId(this.getRuntimeId());
        packet.setPlatformChatId("");
        packet.setPosition(this.getPosition());
        packet.setMotion(this.getMotion());
        packet.setRotation(Vector3f.from(this.getPitch(), this.getYaw(), this.getYaw()));
        packet.setHand(ItemUtils.toNetwork(ItemStack.EMPTY));
//        packet.setHand(ItemUtils.toNetwork(this.getInventory().getSelectedItem())); TODO: Fix this
        packet.setCommandPermission(CommandPermission.ANY);
        packet.setPlayerPermission(PlayerPermission.MEMBER);
        packet.setDeviceId("");
        packet.setGameType(GameType.SURVIVAL); // TODO
        packet.getAbilityLayers().add(CloudPlayerAbilities.defaultBaseLayer());
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
//            this.getContainer().close();
            super.close();
        }
    }

    @Override
    public boolean attack(EntityDamageEvent source) {
        if (this.isClosed() || !this.isAlive()) {
            return false;
        }

        if (!source.getDamageType().is(DamageTypeTags.BYPASSES_ARMOR)) {
            int armorPoints = 0;
            int epf = 0;
            int toughness = 0;

            ArmorView armorView = getArmor();
            for (int armorSlot = 0; armorSlot < armorView.size(); armorSlot++) {
                ItemStack armor = armorView.getItem(armorSlot);
//                TODO: Needs implementation
//                armorPoints += armor.getBlockState().getBehavior().getArmorPoints(armor);
                epf += calculateEnchantmentProtectionFactor(armor, source);
                //toughness += armor.getToughness();
            }

            float damage = source.getDamage() * (1 - armorPoints * 0.04f);
            float enchantmentReduction = Math.min(GenericMath.ceil(Math.min(epf, 25)
                    * ((float) ThreadLocalRandom.current().nextInt(50, 100) / 100)), 20) * 0.04f;
            source.setDamage(Math.max(0, damage * (1 - enchantmentReduction)));
        }

        if (super.attack(source)) {
            Entity damager = source.getDamageSource().getCausingEntity();

            for (int slot = 0; slot < 4; slot++) {
                ItemStack armor = this.getArmor().getItem(slot);
                //TODO: Enchantments implementation
//                List<Enchantment> enchantments = armor.get(ItemKeys.ENCHANTMENTS);
//
//                if (enchantments != null) {
//                    if (damager != null) {
//                        for (EnchantmentInstance enchantment : armor.getEnchantments().values()) {
//                            enchantment.getBehavior().doPostAttack(enchantment, damager, this);
//                        }
//                    }
//
//                    EnchantmentInstance durability = armor.getEnchantment(EnchantmentTypes.UNBREAKING);
//                    if (durability != null && durability.getLevel() > 0 && (100 / (durability.getLevel() + 1)) <= new Random().nextInt(100))
//                        continue;
//                }

                var damage = armor.get(ItemKeys.DAMAGE);
                Boolean unbreakable = armor.get(ItemKeys.UNBREAKABLE);

                if (damage != null) {
                    if (unbreakable != null && unbreakable) {
                        continue;
                    }

                    armor = armor.toBuilder()
                            .data(ItemKeys.DAMAGE, damage - 1)
                            .build();

                    int maxDurability = this.server.getItemRegistry().requireComponent(armor.getType(), ItemComponents.GET_MAX_DAMAGE).execute(armor);
                    if (damage + 1 >= maxDurability) {
                        getArmor().setItem(slot, ItemStack.EMPTY);
                    } else {
                        getArmor().setItem(slot, armor);
                    }
                }
            }

            return true;
        } else {
            return false;
        }
    }

    //TODO: Enchantments implementation
    protected double calculateEnchantmentProtectionFactor(ItemStack item, EntityDamageEvent source) {
//        List<Enchantment> enchantments = item.get(ItemKeys.ENCHANTMENTS);
//        if (enchantments == null) {
//            return 0;
//        }

        double epf = 0;

//        for (EnchantmentInstance ench : enchantments) {
//            epf += ench.getBehavior().getProtectionFactor(ench, source);
//        }

        return epf;
    }

    @Override
    public void setOnFire(int seconds) {
        int level = 0;

        //TODO: Enchantments implementation
//        for (ItemStack armor : this.getInventory().getArmorContents()) {
//            EnchantmentInstance fireProtection = armor.getEnchantment(EnchantmentTypes.FIRE_PROTECTION);
//
//            if (fireProtection != null && fireProtection.getLevel() > 0) {
//                level = Math.max(level, fireProtection.getLevel());
//            }
//        }

        seconds = (int) (seconds * (1 - level * 0.15));

        super.setOnFire(seconds);
    }

    @Override
    public ItemStack[] getDrops() {
//        if (this.getContainer() != null) {
//            return this.getContainer().getContents();
//        }
        return new ItemStack[0];
    }

    public boolean isSneaking() {
        return this.data.getFlag(SNEAKING);
    }

    public void setSneaking(boolean value) {
        this.data.setFlag(SNEAKING, value);
        this.recalculateBoundingBox();
    }

    public void setSneaking() {
        this.setSneaking(true);
    }

    public boolean isSwimming() {
        return this.data.getFlag(SWIMMING);
    }

    public void setSwimming(boolean value) {
        this.data.setFlag(SWIMMING, value);
        this.recalculateBoundingBox();
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
        this.recalculateBoundingBox();
    }

    public void setGliding() {
        this.setGliding(true);
    }

    public boolean isCrawling() {
        return this.data.getFlag(CRAWLING);
    }

    public void setCrawling(boolean value) {
        this.data.setFlag(CRAWLING, value);
        this.recalculateBoundingBox();
    }
}
