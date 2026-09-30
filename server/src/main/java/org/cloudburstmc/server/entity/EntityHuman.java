package org.cloudburstmc.server.entity;

import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.api.enchantment.EnchantmentTypes;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.Human;
import org.cloudburstmc.api.entity.Pose;
import org.cloudburstmc.api.event.entity.EntityPoseChangeEvent;
import org.cloudburstmc.api.item.ItemBehaviors;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.player.Ability;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.player.skin.Skin;
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
import java.util.Map;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.*;

/**
 * Abstract base class for human-shaped entities such as players.
 * Adds skin, game type, permissions, and player inventory support.
 */
public class EntityHuman extends EntityCreature implements Human {
    private static final float STANDING_EYE_HEIGHT = 1.62f;

    private Pose pose = Pose.STANDING;
    @Getter
    private boolean sneaking;
    @Getter
    private boolean swimming;
    @Getter
    private boolean gliding;
    @Getter
    private boolean crawling;

    protected final CloudContainer container = new CloudContainer(36);
    protected UUID identity;
    @Getter
    @Setter
    protected Skin skin;

    public EntityHuman(EntityType<Human> type, Location location) {
        super(type, location);
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
                skinTag.listenForString("PlayFabId", skin::playFabId);
                skinTag.listenForString("SkinResourcePatch", skin::skinResourcePatch);
                skinTag.listenForByteArray("GeometryData", bytes -> skin.geometryData(new String(bytes, UTF_8)));
                skinTag.listenForString("GeometryDataEngineVersion", skin::geometryDataEngineVersion);
                skinTag.listenForByteArray("SkinAnimationData", bytes -> skin.animationData(new String(bytes, UTF_8)));
                skinTag.listenForBoolean("PremiumSkin", skin::premium);
                skinTag.listenForBoolean("PersonaSkin", skin::persona);
                skinTag.listenForBoolean("CapeOnClassicSkin", skin::capeOnClassic);
                skinTag.listenForBoolean("TrustedSkin", skin::trusted);
                skinTag.listenForBoolean("OverrideSkin", skin::overridingPlayerAppearance);
                skinTag.listenForString("ProfileHash", skin::profileHash);
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
                    .putString("PlayFabId", nbtSkin.getPlayFabId())
                    .putString("CapeId", nbtSkin.getCapeId())
                    .putByteArray("CapeData", nbtSkin.getCapeData().getImage())
                    .putInt("CapeImageWidth", nbtSkin.getCapeData().getWidth())
                    .putInt("CapeImageHeight", nbtSkin.getCapeData().getHeight())
                    .putByteArray("SkinResourcePatch", nbtSkin.getSkinResourcePatch().getBytes(UTF_8))
                    .putByteArray("GeometryData", nbtSkin.getGeometryData().getBytes(UTF_8))
                    .putString("GeometryDataEngineVersion", nbtSkin.getGeometryDataEngineVersion())
                    .putByteArray("SkinAnimationData", nbtSkin.getAnimationData().getBytes(UTF_8))
                    .putBoolean("PremiumSkin", nbtSkin.isPremium())
                    .putBoolean("PersonaSkin", nbtSkin.isPersona())
                    .putBoolean("CapeOnClassicSkin", nbtSkin.isCapeOnClassic())
                    .putBoolean("TrustedSkin", nbtSkin.isTrusted())
                    .putBoolean("OverrideSkin", nbtSkin.isOverridingPlayerAppearance())
                    .putString("ProfileHash", nbtSkin.getProfileHash());
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

    public UUID getServerId() {
        return identity;
    }

    public void setServerId(UUID uuid) {
        this.identity = uuid;
    }

    @Override
    public float getWidth() {
        return HumanPoses.dimensions(this.pose).width();
    }

    @Override
    public float getLength() {
        return this.getWidth();
    }

    @Override
    public float getHeight() {
        return HumanPoses.dimensions(this.pose).height();
    }

    @Override
    public float getEyeHeight() {
        return HumanPoses.dimensions(this.pose).eyeHeight();
    }

    @Override
    public float getBaseOffset() {
        return STANDING_EYE_HEIGHT;
    }

    @Override
    public Pose getPose() {
        return this.pose;
    }

    public void setSneaking(boolean value) {
        this.sneaking = value;
        this.recalculateBoundingBox();
    }

    public void setSneaking() {
        this.setSneaking(true);
    }

    public void setSwimming(boolean value) {
        this.swimming = value;
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

    public void setGliding(boolean value) {
        this.gliding = value;
        this.recalculateBoundingBox();
    }

    public void setGliding() {
        this.setGliding(true);
    }

    public void setCrawling(boolean value) {
        this.crawling = value;
        this.recalculateBoundingBox();
    }
    @Override
    public void recalculateBoundingBox() {
        this.refreshPose(true);
    }

    private void refreshPose(boolean updateBounds) {
        Pose previousPose = this.pose;
        Pose desiredPose = this.getDesiredPose();
        boolean unrestricted = desiredPose == Pose.SLEEPING || this.level == null || this.vehicle != null
                || this instanceof CloudPlayer player && player.isSpectator();

        Pose resolvedPose = HumanPoses.resolve(previousPose, desiredPose, unrestricted, this::canFitPose);
        if (!updateBounds && resolvedPose == previousPose) {
            return;
        }

        this.pose = resolvedPose;
        this.data.setFlag(SNEAKING, this.pose == Pose.CROUCHING);
        this.data.setFlag(SWIMMING, this.pose == Pose.SWIMMING);
        this.data.setFlag(CRAWLING, this.pose == Pose.CRAWLING);
        this.data.setFlag(GLIDING, this.pose == Pose.FALL_FLYING);
        super.recalculateBoundingBox();

        if (previousPose != this.pose) {
            this.server.getEventManager().fire(new EntityPoseChangeEvent(this, previousPose, this.pose));
        }
    }

    private Pose getDesiredPose() {
        if (this instanceof CloudPlayer player && player.isSleeping()) {
            return Pose.SLEEPING;
        }

        if (this.swimming) {
            return Pose.SWIMMING;
        }

        if (this.crawling) {
            return Pose.CRAWLING;
        }

        if (this.gliding) {
            return Pose.FALL_FLYING;
        }

        if (this.data.getFlag(DAMAGE_NEARBY_MOBS)) {
            return Pose.SPIN_ATTACK;
        }

        if (!this.sneaking) {
            return Pose.STANDING;
        }

        return this instanceof CloudPlayer player && player.getAbilities().get(Ability.FLYING) ? Pose.STANDING : Pose.CROUCHING;
    }

    private boolean canFitPose(Pose pose) {
        return !this.level.hasCollision(this, HumanPoses.dimensions(pose).boundingBox(this.position, this.scale).deflate(1.0E-5f, 1.0E-5f, 1.0E-5f));
    }

    @Override
    public boolean entityBaseTick(int tickDiff) {
        this.refreshPose(false);
        return super.entityBaseTick(tickDiff);
    }


    @Override
    protected void hurtHelmet(float damage) {
        ItemStack helmet = this.getArmor().getHelmet();
        ItemStack damagedHelmet = this.damageArmorItem(helmet, damage);
        if (!damagedHelmet.equals(helmet) && this.getArmor().getHelmet().equals(helmet)) {
            this.getArmor().setHelmet(damagedHelmet);
        }
    }

    @Override
    protected void hurtArmor(float damage) {
        for (int slot = 0; slot < this.armor.size(); slot++) {
            ItemStack item = this.armor.getItem(slot);
            if (!item.isEmpty() && this.server.getItemRegistry().getComponent(item.getType(), ItemBehaviors.ARMOR) != null) {
                ItemStack damagedItem = this.damageArmorItem(item, damage);
                if (!damagedItem.equals(item) && this.armor.getItem(slot).equals(item)) {
                    this.armor.setItem(slot, damagedItem);
                }
            }
        }
    }

    private ItemStack damageArmorItem(ItemStack item, float damage) {
        if (item.isEmpty() || damage <= 0 || !this.server.getItemRegistry().requireComponent(item.getType(), ItemBehaviors.DAMAGEABLE).get()) {
            return item;
        }

        int durabilityDamage = Math.max((int) (damage / 4), 1);
        return this.server.getItemRegistry().requireComponent(item.getType(), ItemBehaviors.ON_DAMAGE).execute(item, durabilityDamage, this);
    }

    @Override
    public ItemStack[] getDrops() {
        List<ItemStack> drops = new ArrayList<>(this.container.size() + this.armor.size() + this.offhand.size());
        addDrops(drops, this.container.getContents());
        addDrops(drops, this.armor.getContents());
        addDrops(drops, this.offhand.getContents());
        return drops.toArray(ItemStack[]::new);
    }

    private static void addDrops(List<ItemStack> drops, ItemStack[] contents) {
        for (ItemStack item : contents) {
            if (!item.isEmpty()
                    && item.get(ItemDataComponents.KEEP_ON_DEATH) != Boolean.TRUE
                    && !item.getOrDefault(ItemDataComponents.ENCHANTMENTS, Map.of()).containsKey(EnchantmentTypes.VANISHING)) {
                drops.add(item);
            }
        }
    }

    @Override
    public void spawnTo(CloudPlayer player) {
        if (this == player || this.hasSpawned.contains(player) || this.chunk == null || !player.isChunkSent(this.chunk.getX(),
                this.chunk.getZ())) {
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
            this.getServer().updatePlayerListData(this.getServerId(),
                    this.getUniqueId(),
                    BedrockLegacyTextSerializer.getInstance().serialize(cloudPlayer.displayName()),
                    serializedSkin,
                    cloudPlayer.getXuid(),
                    new CloudPlayer[]{player}
            );
            playerSkin = serializedSkin;
        } else {
            this.getServer().updatePlayerListData(this.getServerId(),
                    this.getUniqueId(),
                    this.getName(),
                    SkinUtils.toSerialized(currentSkin),
                    new CloudPlayer[]{player}
            );
        }

        player.sendPacket(this.createAddEntityPacket());
        if (playerSkin != null) {
            player.sendPacket(this.createPlayerSkinPacket(playerSkin));
        }

//            this.getContainer().sendArmorContents(player); TODO: Fix this

        if (this.vehicle != null) {
            SetEntityLinkPacket packet = new SetEntityLinkPacket();
            EntityLinkData link = new EntityLinkData(this.vehicle.getUniqueId(),
                    this.getUniqueId(),
                    EntityLinkData.Type.RIDER,
                    true,
                    false,
                    0
            );
            packet.setEntityLink(link);

            player.sendPacket(packet);
        }

        if (!(this instanceof CloudPlayer)) {
            this.server.removePlayerListData(this.getServerId(), new CloudPlayer[]{player});
        }
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
        packet.getMetadata().putAll(this.getData().snapshot());
        return packet;
    }

    private PlayerSkinPacket createPlayerSkinPacket(SerializedSkin skin) {
        PlayerSkinPacket packet = new PlayerSkinPacket();
        packet.setUuid(this.getServerId());
        packet.setSkin(skin);
        packet.setNewSkinName(skin.getSkinId());
        packet.setOldSkinName("");
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

}
