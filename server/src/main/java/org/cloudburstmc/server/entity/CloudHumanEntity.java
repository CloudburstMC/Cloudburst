package org.cloudburstmc.server.entity;

import lombok.Getter;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.enchantment.EnchantmentTypes;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.HumanEntity;
import org.cloudburstmc.api.entity.Pose;
import org.cloudburstmc.api.event.entity.EntityPoseChangeEvent;
import org.cloudburstmc.api.event.entity.EntityRemoveCause;
import org.cloudburstmc.api.inventory.ItemContainer;
import org.cloudburstmc.api.item.EquipmentSlot;
import org.cloudburstmc.api.item.ItemBehaviors;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.component.DeathProtectionComponent;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.player.Ability;
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
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerId;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.data.skin.SerializedSkin;
import org.cloudburstmc.protocol.bedrock.packet.*;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.container.Container;
import org.cloudburstmc.server.container.ContainerListener;
import org.cloudburstmc.server.container.view.CloudEntityStorageView;
import org.cloudburstmc.server.entity.data.CloudEquipmentData;
import org.cloudburstmc.server.entity.data.CloudHumanData;
import org.cloudburstmc.server.item.ItemStackNbtCodec;
import org.cloudburstmc.server.item.ItemStackPacketCodec;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.player.CloudPlayerAbilities;
import org.cloudburstmc.server.registry.CloudItemRegistry;
import org.cloudburstmc.server.utils.SkinUtils;

import java.util.*;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.*;

/**
 * A standalone human or the shared entity implementation for a connected player.
 * Session state belongs to the player.
 */
public class CloudHumanEntity extends CloudEquippedLivingEntity implements HumanEntity {

    private static final int INVENTORY_SIZE = 36;
    private static final int HOTBAR_SIZE = 9;
    private static final String INVENTORY_KEY = "Inventory";
    private static final String SELECTED_SLOT_KEY = "SelectedInventorySlot";

    protected final CloudContainer container = new CloudContainer(INVENTORY_SIZE);
    private final @Nullable StandaloneState standalone;
    private int selectedHotbarSlot;

    private Pose pose = Pose.STANDING;
    @Getter
    private boolean sneaking;
    @Getter
    private boolean swimming;
    private boolean glideRequested;
    @Getter
    private boolean crawling;

    public CloudHumanEntity(EntityType<HumanEntity> type, Location location) {
        super(type, location, new CloudHumanData());
        this.standalone = new StandaloneState();
        this.container.addContainerListener(this.standalone);
        this.armor.getContainer().addContainerListener(this.standalone);
        this.offhand.getContainer().addContainerListener(this.standalone);
    }

    protected CloudHumanEntity(EntityType<HumanEntity> type, Location location, CloudEquipmentData equipment) {
        super(type, location, equipment);
        this.standalone = null;
    }

    @Override
    protected void initEntity() {
        this.data.setFlag(HAS_GRAVITY, true);
        super.initEntity();
    }

    @Override
    public void remove(EntityRemoveCause cause) {
        if (this.isClosed()) {
            return;
        }

        try {
            super.remove(cause);
        } finally {
            if (this.standalone != null) {
                this.container.removeContainerListener(this.standalone);
                this.armor.getContainer().removeContainerListener(this.standalone);
                this.offhand.getContainer().removeContainerListener(this.standalone);
            }
        }
    }

    @Override
    protected void loadCustom(NbtMap tag) {
        super.loadCustom(tag);
        int selectedSlot = tag.getInt(SELECTED_SLOT_KEY, 0);
        this.selectedHotbarSlot = isHotbarSlot(selectedSlot) ? selectedSlot : 0;
        if (this.standalone == null) {
            return;
        }

        ItemStack[] contents = new ItemStack[INVENTORY_SIZE];
        for (NbtMap itemTag : tag.getList(INVENTORY_KEY, NbtType.COMPOUND)) {
            int slot = itemTag.getByte("Slot", (byte) -1);
            if (slot >= 0 && slot < contents.length) {
                contents[slot] = ItemStackNbtCodec.deserializeItem(itemTag);
            }
        }

        this.container.setContents(contents);
    }

    @Override
    protected void saveCustom(NbtMapBuilder tag) {
        super.saveCustom(tag);
        if (this.selectedHotbarSlot != 0) {
            tag.putInt(SELECTED_SLOT_KEY, this.selectedHotbarSlot);
        }

        if (this.standalone != null) {
            List<NbtMap> items = this.container.toNbt();
            if (!items.isEmpty()) {
                tag.putList(INVENTORY_KEY, NbtType.COMPOUND, items);
            }
        }
    }

    @Override
    protected Set<String> customKeys() {
        Set<String> keys = new HashSet<>(super.customKeys());
        keys.add(SELECTED_SLOT_KEY);
        if (this.standalone != null) {
            keys.add(INVENTORY_KEY);
        }
        return keys;
    }

    @Override
    protected String getDefaultName() {
        return "";
    }

    @Override
    public UUID getServerId() {
        return this.requireStandalone().serverId;
    }

    @Override
    public ItemContainer getInventory() {
        return this.requireStandalone().inventory;
    }

    @Override
    public int getSelectedHotbarSlot() {
        return this.selectedHotbarSlot;
    }

    @Override
    public void setSelectedHotbarSlot(int slot) {
        if (!isHotbarSlot(slot)) {
            throw new IllegalArgumentException("Hotbar slot must be between 0 and 8");
        }

        if (slot != this.selectedHotbarSlot) {
            this.selectedHotbarSlot = slot;
            this.onSelectedHotbarSlotChanged();
        }
    }

    protected void onSelectedHotbarSlotChanged() {
        if (this.standalone != null) {
            this.markChunkDirty();
            this.standalone.broadcastEquipmentChange(this.container);
        }
    }

    private static boolean isHotbarSlot(int slot) {
        return slot >= 0 && slot < HOTBAR_SIZE;
    }

    private StandaloneState requireStandalone() {
        return Objects.requireNonNull(this.standalone, "Session-backed humans supply their own state");
    }

    @Override
    public boolean entityBaseTick(int tickDiff) {
        this.refreshPose(false);
        return super.entityBaseTick(tickDiff);
    }

    @Override
    public Pose getPose() {
        return this.pose;
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
        return HumanPoses.dimensions(Pose.STANDING).eyeHeight();
    }

    @Override
    public void setSneaking(boolean sneaking) {
        this.sneaking = sneaking;
        this.recalculateBoundingBox();
    }

    @Override
    public void setSwimming(boolean swimming) {
        this.swimming = swimming;
        this.recalculateBoundingBox();
    }

    @Override
    public boolean isSprinting() {
        return this.data.getFlag(SPRINTING);
    }

    @Override
    public void setSprinting(boolean sprinting) {
        this.data.setFlag(SPRINTING, sprinting);
    }

    @Override
    public boolean isGliding() {
        return this.glideRequested && this.pose == Pose.FALL_FLYING;
    }

    protected boolean hasGlideRequest() {
        return this.glideRequested;
    }

    @Override
    public void setGliding(boolean gliding) {
        this.glideRequested = gliding;
        this.recalculateBoundingBox();
    }

    @Override
    public void setCrawling(boolean crawling) {
        this.crawling = crawling;
        this.recalculateBoundingBox();
    }

    @Override
    public void recalculateBoundingBox() {
        this.refreshPose(true);
    }

    private void refreshPose(boolean forceBoundsUpdate) {
        Pose previousPose = this.pose;
        Pose desiredPose = this.getDesiredPose();
        boolean unrestricted = desiredPose == Pose.SLEEPING
                || this.level == null
                || this.getVehicle() != null
                || this instanceof CloudPlayer player && player.isSpectator();

        Pose resolvedPose = HumanPoses.resolve(previousPose, desiredPose, unrestricted, this::canFitPose);
        if (!forceBoundsUpdate && resolvedPose == previousPose) {
            return;
        }

        this.pose = resolvedPose;
        this.data.setFlag(SNEAKING, this.pose == Pose.CROUCHING);
        this.data.setFlag(SWIMMING, this.pose == Pose.SWIMMING);
        this.data.setFlag(CRAWLING, this.pose == Pose.CRAWLING);
        this.data.setFlag(GLIDING, this.isGliding());
        super.recalculateBoundingBox();

        if (previousPose != this.pose && this.server != null) {
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

        if (this.glideRequested) {
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
        return !this.level.hasCollision(this, HumanPoses.dimensions(pose)
                .boundingBox(this.position, this.getScale())
                .deflate(1.0E-5f, 1.0E-5f, 1.0E-5f));
    }

    @Override
    protected @Nullable DeathProtectionUse findDeathProtection() {
        ItemContainer inventory = this.getInventory();
        int slot = this.getSelectedHotbarSlot();
        ItemStack mainHand = inventory.getItem(slot);
        DeathProtectionComponent protection = mainHand.isEmpty() ? null : CloudItemRegistry.get().getComponent(mainHand.getType(), ItemBehaviors.DEATH_PROTECTION);
        if (protection == null) {
            return super.findDeathProtection();
        }

        return new DeathProtectionUse(EquipmentSlot.MAIN_HAND, mainHand, protection, replacement -> {
            if (inventory.getItem(slot).equals(mainHand)) {
                inventory.setItem(slot, replacement);
            }
        });
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
            if (item.isEmpty() || this.server.getItemRegistry().getComponent(item.getType(), ItemBehaviors.ARMOR) == null) {
                continue;
            }

            ItemStack damagedItem = this.damageArmorItem(item, damage);
            if (!damagedItem.equals(item) && this.armor.getItem(slot).equals(item)) {
                this.armor.setItem(slot, damagedItem);
            }
        }
    }

    private ItemStack damageArmorItem(ItemStack item, float damage) {
        if (item.isEmpty() || damage <= 0 || !this.server.getItemRegistry().requireComponent(item.getType(), ItemBehaviors.DAMAGEABLE).get()) {
            return item;
        }

        int durabilityDamage = Math.max((int) (damage / 4), 1);
        return this.server.getItemRegistry()
                .requireComponent(item.getType(), ItemBehaviors.ON_DAMAGE)
                .execute(item, durabilityDamage, this);
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
            if (!item.isEmpty() && item.get(ItemDataComponents.KEEP_ON_DEATH) != Boolean.TRUE
                    && !item.getOrDefault(ItemDataComponents.ENCHANTMENTS, Map.of())
                            .containsKey(EnchantmentTypes.VANISHING)) {
                drops.add(item);
            }
        }
    }

    @Override
    public @Nullable Skin getSkin() {
        return this.data() instanceof CloudHumanData state ? state.getSkin() : null;
    }

    @Override
    public void setSkin(Skin skin) {
        if (!(this.data() instanceof CloudHumanData state)) {
            throw new IllegalStateException("Session-backed humans own their skin separately");
        }

        Objects.requireNonNull(skin, "skin");
        if (!skin.isValid()) {
            throw new IllegalArgumentException("Invalid human skin");
        }

        SerializedSkin serialized = this.getViewers().isEmpty() ? null : SkinUtils.toSerialized(skin);
        state.setSkin(skin);
        this.markChunkDirty();
        if (serialized != null) {
            this.broadcastToViewers(this.createPlayerSkinPacket(serialized));
        }

        // A human that previously had no valid skin may now be visible.
        this.refreshViewers();
    }

    @Override
    public void spawnTo(CloudPlayer player) {
        if (this == player || !this.canSpawnTo(player) || this.hasViewer(player)) {
            return;
        }

        Skin currentSkin = this.getSkin();
        if (currentSkin == null || !currentSkin.isValid()) {
            return;
        }

        CloudPlayer sessionPlayer = this instanceof CloudPlayer connected ? connected : null;
        SerializedSkin skin = sessionPlayer == null ? SkinUtils.toSerialized(currentSkin) : sessionPlayer.getSerializedSkin();
        if (this.beginTracking(player)) {
            this.sendHumanPairingData(player, sessionPlayer, skin);
        }
    }

    private void sendHumanPairingData(CloudPlayer viewer, @Nullable CloudPlayer sessionPlayer, SerializedSkin skin) {
        boolean temporaryListEntry = false;
        boolean added = false;
        try {
            if (sessionPlayer == null) {
                temporaryListEntry = viewer.sendPacket(this.createTemporaryListEntry(skin));
                if (!temporaryListEntry) {
                    return;
                }
            } else {
                this.getServer().updatePlayerListData(this.getServerId(), this.getUniqueId(),
                        BedrockLegacyTextSerializer.getInstance().serialize(sessionPlayer.displayName()),
                        skin, sessionPlayer.getXuid(), new CloudPlayer[]{viewer}
                );
            }

            added = this.sendTrackedAddPacket(viewer);
            if (!added) {
                return;
            }

            if (sessionPlayer == null) {
                viewer.sendPacket(this.createArmorPacket());
                viewer.sendPacket(this.createOffhandPacket());
            } else {
                viewer.sendPacket(this.createPlayerSkinPacket(skin));
            }

            this.sendVehicleLink(viewer);
        } finally {
            try {
                if (temporaryListEntry) {
                    this.server.removePlayerListData(this.getServerId(), new CloudPlayer[]{viewer});
                }
            } finally {
                if (!added) {
                    this.stopTracking(viewer);
                }
            }
        }
    }

    private PlayerListPacket createTemporaryListEntry(SerializedSkin skin) {
        PlayerListPacket.Entry entry = new PlayerListPacket.Entry(this.getServerId());
        entry.setAction(PlayerListPacket.Action.ADD);
        entry.setEntityId(this.getUniqueId());
        entry.setName(this.getName());
        entry.setSkin(skin);
        entry.setXuid("");
        entry.setPlatformChatId("");
        entry.setColor(skin.getColor());

        PlayerListPacket packet = new PlayerListPacket();
        packet.getEntries().add(entry);
        return packet;
    }

    private void sendVehicleLink(CloudPlayer player) {
        if (!this.hasViewer(player) || !(this.getVehicle() instanceof CloudEntity vehicle)) {
            return;
        }

        vehicle.spawnTo(player);
        int seat = vehicle.getPassengers().indexOf(this);
        if (seat < 0 || !this.hasViewer(player) || (!vehicle.hasViewer(player) && vehicle != player)) {
            return;
        }

        SetEntityLinkPacket packet = new SetEntityLinkPacket();
        packet.setEntityLink(new EntityLinkData(vehicle.getUniqueId(), this.getUniqueId(),
                seat == 0 ? EntityLinkData.Type.RIDER : EntityLinkData.Type.PASSENGER, true, false, 0));
        player.sendPacket(packet);
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
        packet.setHand(ItemStackPacketCodec.toNetwork(this.getMainHandItem()));
        packet.setCommandPermission(CommandPermission.ANY);
        packet.setPlayerPermission(PlayerPermission.MEMBER);
        packet.setDeviceId("");
        packet.setGameType(GameType.SURVIVAL);
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

    private MobArmorEquipmentPacket createArmorPacket() {
        MobArmorEquipmentPacket packet = new MobArmorEquipmentPacket();
        packet.setRuntimeEntityId(this.getRuntimeId());
        packet.setHelmet(ItemStackPacketCodec.toNetwork(this.armor.getHelmet()));
        packet.setChestplate(ItemStackPacketCodec.toNetwork(this.armor.getChestplate()));
        packet.setLeggings(ItemStackPacketCodec.toNetwork(this.armor.getLeggings()));
        packet.setBoots(ItemStackPacketCodec.toNetwork(this.armor.getBoots()));
        packet.setBody(ItemData.AIR);
        return packet;
    }

    private MobEquipmentPacket createOffhandPacket() {
        return this.createEquipmentPacket(this.offhand.getOffhandItem(), ContainerId.OFFHAND, 0, -1);
    }

    private MobEquipmentPacket createMainHandPacket() {
        int slot = this.getSelectedHotbarSlot();
        return this.createEquipmentPacket(this.getMainHandItem(), ContainerId.INVENTORY, slot, slot);
    }

    private MobEquipmentPacket createEquipmentPacket(ItemStack item, int containerId, int slot, int hotbarSlot) {
        MobEquipmentPacket packet = new MobEquipmentPacket();
        packet.setRuntimeEntityId(this.getRuntimeId());
        packet.setItem(ItemStackPacketCodec.toNetwork(item));
        packet.setContainerId(containerId);
        packet.setInventorySlot(slot);
        packet.setHotbarSlot(hotbarSlot);
        return packet;
    }

    private final class StandaloneState implements ContainerListener {
        private final UUID serverId = UUID.randomUUID();
        private final ItemContainer inventory = new CloudEntityStorageView(CloudHumanEntity.this.container);

        @Override
        public void onInventorySlotChange(Container inventory, int slot) {
            CloudHumanEntity.this.markChunkDirty();
            if (inventory != CloudHumanEntity.this.container || slot == CloudHumanEntity.this.selectedHotbarSlot) {
                this.broadcastEquipmentChange(inventory);
            }
        }

        @Override
        public void onInventoryContentsChange(Container inventory) {
            CloudHumanEntity.this.markChunkDirty();
            this.broadcastEquipmentChange(inventory);
        }

        private void broadcastEquipmentChange(Container inventory) {
            CloudHumanEntity human = CloudHumanEntity.this;
            if (human.isClosed() || human.getViewers().isEmpty()) {
                return;
            }

            BedrockPacket packet;
            if (inventory == human.armor.getContainer()) {
                packet = human.createArmorPacket();
            } else if (inventory == human.offhand.getContainer()) {
                packet = human.createOffhandPacket();
            } else {
                packet = human.createMainHandPacket();
            }

            human.broadcastToViewers(packet);
        }

        @Override
        public void onInventoryAdded(Container inventory) {
        }

        @Override
        public void onInventoryRemoved(Container inventory) {
        }

        @Override
        public void onInventoryDataChange(Container inventory, int property, int value) {
        }
    }
}
