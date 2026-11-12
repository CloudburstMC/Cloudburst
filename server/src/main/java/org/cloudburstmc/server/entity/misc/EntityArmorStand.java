package org.cloudburstmc.server.entity.misc;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.damage.DamageType;
import org.cloudburstmc.api.entity.damage.DamageTypeTags;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.entity.misc.ArmorStand;
import org.cloudburstmc.api.event.entity.EntityDeathEvent;
import org.cloudburstmc.api.event.player.PlayerArmorStandManipulateEvent;
import org.cloudburstmc.api.item.*;
import org.cloudburstmc.api.item.component.DeathProtectionComponent;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.api.level.sound.SoundType;
import org.cloudburstmc.api.level.sound.SoundTypes;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerId;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.packet.LevelEventPacket;
import org.cloudburstmc.protocol.bedrock.packet.LevelSoundEventPacket;
import org.cloudburstmc.protocol.bedrock.packet.MobArmorEquipmentPacket;
import org.cloudburstmc.protocol.bedrock.packet.MobEquipmentPacket;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.block.util.RedstoneSignals;
import org.cloudburstmc.server.entity.CloudDamageResult;
import org.cloudburstmc.server.entity.EntityLiving;
import org.cloudburstmc.server.event.entity.CloudEntityDamageEvent;
import org.cloudburstmc.server.item.ItemStackNbtCodec;
import org.cloudburstmc.server.item.ItemStackPacketCodec;
import org.cloudburstmc.server.network.LevelEffectPacketFactory;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import java.util.*;

public class EntityArmorStand extends EntityLiving implements ArmorStand {

    private static final int POSE_COUNT = 13;
    private static final int BREAK_WINDOW = 5;

    private static final Map<SoundType, SoundEvent> EQUIP_EVENTS = Map.of(
            SoundTypes.ARMOR_EQUIP_CHAIN, SoundEvent.ARMOR_EQUIP_CHAIN,
            SoundTypes.ARMOR_EQUIP_COPPER, SoundEvent.EQUIP_COPPER,
            SoundTypes.ARMOR_EQUIP_DIAMOND, SoundEvent.ARMOR_EQUIP_DIAMOND,
            SoundTypes.ARMOR_EQUIP_ELYTRA, SoundEvent.ARMOR_EQUIP_ELYTRA,
            SoundTypes.ARMOR_EQUIP_GENERIC, SoundEvent.ARMOR_EQUIP_GENERIC,
            SoundTypes.ARMOR_EQUIP_GOLD, SoundEvent.ARMOR_EQUIP_GOLD,
            SoundTypes.ARMOR_EQUIP_IRON, SoundEvent.ARMOR_EQUIP_IRON,
            SoundTypes.ARMOR_EQUIP_LEATHER, SoundEvent.ARMOR_EQUIP_LEATHER,
            SoundTypes.ARMOR_EQUIP_NETHERITE, SoundEvent.EQUIP_NETHERITE
    );

    private static final Map<EquipmentSlot, String> EQUIPMENT_KEYS = Map.of(
            EquipmentSlot.HEAD, "Head", EquipmentSlot.CHEST, "Chest",
            EquipmentSlot.LEGS, "Legs", EquipmentSlot.FEET, "Feet",
            EquipmentSlot.MAIN_HAND, "MainHand", EquipmentSlot.OFF_HAND, "OffHand"
    );

    private static final Set<ItemType> HEADWEAR = Set.of(
            ItemTypes.CARVED_PUMPKIN, ItemTypes.SKELETON_SKULL, ItemTypes.WITHER_SKELETON_SKULL,
            ItemTypes.ZOMBIE_HEAD, ItemTypes.CREEPER_HEAD, ItemTypes.DRAGON_HEAD,
            ItemTypes.PLAYER_HEAD, ItemTypes.PIGLIN_HEAD
    );

    private final EnumMap<EquipmentSlot, ItemStack> equipment = new EnumMap<>(EquipmentSlot.class);
    private int poseIndex;
    private int previousSignal = -1;
    private int lastHitTick = -BREAK_WINDOW - 1;

    public EntityArmorStand(EntityType<?> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        this.setMaxHealth(6);
        super.initEntity();
        this.setPoseIndex(0);
    }

    @Override
    public float getWidth() {
        return 0.5f;
    }

    @Override
    public float getLength() {
        return this.getWidth();
    }

    @Override
    public float getHeight() {
        return 1.975f;
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);
        tag.listenForInt("PoseIndex", pose -> this.setPoseIndex(Math.clamp(pose, 0, POSE_COUNT - 1)));
        this.equipment.clear();
        tag.listenForCompound("Equipment", stored -> EQUIPMENT_KEYS.forEach((slot, key) ->
                stored.listenForCompound(key, item -> this.storeEquipment(slot, ItemStackNbtCodec.deserializeItem(item)))));
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);
        tag.putInt("PoseIndex", this.poseIndex);
        NbtMapBuilder stored = NbtMap.builder();
        this.equipment.forEach((slot, item) -> stored.putCompound(EQUIPMENT_KEYS.get(slot), ItemStackNbtCodec.serializeItem(item)));
        tag.putCompound("Equipment", stored.build());
    }

    @Override
    public ItemStack getEquipment(EquipmentSlot slot) {
        return this.equipment.getOrDefault(Objects.requireNonNull(slot, "slot"), ItemStack.EMPTY);
    }

    @Override
    public void setEquipment(EquipmentSlot slot, ItemStack item) {
        Objects.requireNonNull(slot, "slot");
        Objects.requireNonNull(item, "item");
        ItemStack previous = this.getEquipment(slot);
        this.storeEquipment(slot, item);
        if (!previous.equals(this.getEquipment(slot))) {
            if (slot.isArmor()) {
                CloudServer.broadcastPacket(this.getViewers(), this.armorPacket());
            } else {
                CloudServer.broadcastPacket(this.getViewers(), this.handPacket(slot));
            }
        }
    }

    private void storeEquipment(EquipmentSlot slot, ItemStack item) {
        if (item.isEmpty()) {
            this.equipment.remove(slot);
        } else {
            this.equipment.put(slot, item);
        }
    }

    @Override
    public int getPoseIndex() {
        return this.poseIndex;
    }

    @Override
    public void setPoseIndex(int index) {
        if (index < 0 || index >= POSE_COUNT) {
            throw new IllegalArgumentException("Armor stand pose index must be between 0 and " + (POSE_COUNT - 1));
        }

        this.poseIndex = index;
        this.data.set(EntityDataTypes.ARMOR_STAND_POSE_INDEX, index);
    }

    @Override
    public boolean onInteract(Player player, ItemStack item, Vector3f clickedPos) {
        if (!this.isAlive() || player.isSpectator() || !player.getInventory().getSelectedItem().equals(item)) {
            return false;
        }

        if (super.onInteract(player, item, clickedPos)) {
            return true;
        }

        if (item.getType() == ItemTypes.NAME_TAG) {
            String name = item.get(ItemDataComponents.CUSTOM_NAME);
            if (name == null || name.isEmpty()) {
                return false;
            }

            this.setNameTag(name);
            if (!player.isCreative()) {
                player.getInventory().setSelectedItem(item.decreaseCount());
            }

            return true;
        }

        if (player.isSneaking()) {
            this.setPoseIndex((this.poseIndex + 1) % POSE_COUNT);
            return true;
        }

        EquipmentSlot slot = item.isEmpty() ? this.clickedSlot(clickedPos.getY() - this.getY()) : equipmentSlot(item);
        if (slot == null) {
            return false;
        }

        ItemStack equipped = this.getEquipment(slot);
        if (!item.isEmpty() && !equipped.isEmpty() && isSameEquipment(item, equipped)) {
            return true;
        }

        if ((item.isEmpty() && equipped.isEmpty()) || (item.getCount() > 1 && !equipped.isEmpty())) {
            return false;
        }

        PlayerArmorStandManipulateEvent event = new PlayerArmorStandManipulateEvent(player, this, item, equipped, slot);
        this.getServer().getEventManager().fire(event);
        if (event.isCancelled() || this.isClosed() || !this.isAlive() || player.getLevel() != this.getLevel()
                || !player.getInventory().getSelectedItem().equals(item) || !this.getEquipment(slot).equals(equipped)) {
            return false;
        }

        this.setEquipment(slot, item.isEmpty() ? ItemStack.EMPTY : item.withCount(1));
        if (!player.isCreative() || item.isEmpty() || !equipped.isEmpty()) {
            player.getInventory().setSelectedItem(item.getCount() <= 1 ? equipped : item.decreaseCount());
        }
        this.playEquipmentInteractionSounds(slot, item);
        return true;
    }

    private static boolean isSameEquipment(ItemStack first, ItemStack second) {
        return first.isSimilar(second)
                && normalizeEquipmentDefaults(first).isStackableWith(normalizeEquipmentDefaults(second));
    }

    private static ItemStack normalizeEquipmentDefaults(ItemStack item) {
        if (item.getDamage() == 0) {
            item = item.resetDamage();
        }

        if (Integer.valueOf(0).equals(item.get(ItemDataComponents.REPAIR_COST))) {
            item = item.toBuilder().removeData(ItemDataComponents.REPAIR_COST).build();
        }

        return item;
    }

    private @Nullable EquipmentSlot clickedSlot(float height) {
        if (!Float.isFinite(height)) {
            return null;
        }

        if (height >= 1.6f && !this.getEquipment(EquipmentSlot.HEAD).isEmpty()) {
            return EquipmentSlot.HEAD;
        }

        if (height >= 0.9f && height < 1.6f && !this.getEquipment(EquipmentSlot.CHEST).isEmpty()) {
            return EquipmentSlot.CHEST;
        }

        if (height >= 0.1f && height < 0.55f && !this.getEquipment(EquipmentSlot.FEET).isEmpty()) {
            return EquipmentSlot.FEET;
        }

        if (height >= 0.4f && height < 1.2f && !this.getEquipment(EquipmentSlot.LEGS).isEmpty()) {
            return EquipmentSlot.LEGS;
        }

        return height >= 0.9f && height < 1.6f ? EquipmentSlot.MAIN_HAND : null;
    }

    private static EquipmentSlot equipmentSlot(ItemStack item) {
        if (HEADWEAR.contains(item.getType())) {
            return EquipmentSlot.HEAD;
        }

        EquipmentSlot slot = CloudItemRegistry.get().requireComponent(item.getType(), ItemBehaviors.GET_EQUIPMENT_SLOT).execute(item);
        return slot != null && slot.isArmor() ? slot : EquipmentSlot.MAIN_HAND;
    }

    @Override
    public boolean entityBaseTick(int tickDiff) {
        boolean updated = super.entityBaseTick(tickDiff);
        if (!this.isAlive() || this.isClosed()) {
            return updated;
        }

        if (this.getServer().getTick() - this.lastHitTick > BREAK_WINDOW && this.data.getFlag(EntityFlag.SHAKING)) {
            this.data.setFlag(EntityFlag.SHAKING, false);
        }

        Block block = this.getLevel().getLoadedBlock(this.getPosition().toInt());
        if (block != null) {
            int signal = RedstoneSignals.getNeighborSignal(block);
            if (signal != this.previousSignal) {
                if (signal > 0 || this.previousSignal > 0) {
                    this.setPoseIndex(Math.clamp(signal - 1, 0, POSE_COUNT - 1));
                }

                this.previousSignal = signal;
            }
        }

        if (this.getVehicle() == null) {
            this.motion = this.motion.sub(0, this.getGravity(), 0);
            this.move(this.motion);
            float drag = 1 - this.getDrag();
            float friction = this.isOnGround() ? this.getLevel().getBlockState(this.getLandingBlockPosition()).getFriction() * drag : drag;
            this.motion = this.motion.mul(friction, drag, friction);
        }

        return true;
    }

    @Override
    public void fall(float fallDistance) {
        this.playSound(LevelEvent.SOUND_ARMOR_STAND_LAND);
        super.fall(fallDistance);
    }

    @Override
    public CloudDamageResult damageWithResult(float amount, DamageSource source) {
        DamageType type = source.getDamageType();
        if (type == DamageTypes.CACTUS || type == DamageTypes.SWEET_BERRY_BUSH
                || type == DamageTypes.DROWN || type == DamageTypes.MOB_ATTACK
                || type == DamageTypes.MOB_ATTACK_NO_AGGRO || type.is(DamageTypeTags.IS_FALL)) {
            return CloudDamageResult.REJECTED;
        }

        if (type.is(DamageTypeTags.IS_PLAYER_ATTACK) && source.getCausingEntity() instanceof Player player) {
            if (player.isSpectator() || player.isAdventure()) {
                return CloudDamageResult.REJECTED;
            }

            return this.hitStand(this.createDamageEvent(source, amount), player.isCreative());
        }

        if (type.is(DamageTypeTags.ALWAYS_KILLS_ARMOR_STANDS) || type.is(DamageTypeTags.IS_EXPLOSION)) {
            return this.hitStand(this.createDamageEvent(source, amount), true);
        }

        return super.damageWithResult(amount, source);
    }

    private CloudDamageResult hitStand(CloudEntityDamageEvent event, boolean instantBreak) {
        if (this.isDamageImmune(event.getDamageSource())) {
            return CloudDamageResult.REJECTED;
        }

        int tick = this.getServer().getTick();
        boolean breaks = instantBreak || tick - this.lastHitTick <= BREAK_WINDOW;

        this.getServer().getEventManager().fire(event);
        if (event.isCancelled()) {
            return CloudDamageResult.CANCELLED;
        }

        if (this.isClosed() || !this.isAlive() || event.getDamage() <= 0) {
            return CloudDamageResult.REJECTED;
        }

        if (breaks) {
            this.setLastDamageCause(event);
            this.kill();
            return !this.isAlive() || this.isClosed() ? CloudDamageResult.HANDLED : CloudDamageResult.CANCELLED;
        }

        this.lastHitTick = tick;
        this.data.setFlag(EntityFlag.SHAKING, true);
        this.playSound(LevelEvent.SOUND_ARMOR_STAND_HIT);
        return CloudDamageResult.HANDLED;
    }

    @Override
    protected float getKnockbackResistance() {
        return 1;
    }

    @Override
    protected @Nullable DeathProtectionUse findDeathProtection() {
        for (EquipmentSlot slot : List.of(EquipmentSlot.MAIN_HAND, EquipmentSlot.OFF_HAND)) {
            ItemStack item = this.getEquipment(slot);
            DeathProtectionComponent protection = item.isEmpty() ? null : CloudItemRegistry.get().getComponent(item.getType(), ItemBehaviors.DEATH_PROTECTION);
            if (protection != null) {
                return new DeathProtectionUse(slot, item, protection, replacement -> this.setEquipment(slot, replacement));
            }
        }
        return null;
    }

    @Override
    public ItemStack[] getDrops() {
        DamageSource source = this.getDeathDamageSource();
        DamageType type = source.getDamageType();
        if (type.is(DamageTypeTags.IS_PLAYER_ATTACK) && source.getCausingEntity() instanceof Player player && player.isCreative()) {
            return new ItemStack[0];
        }

        List<ItemStack> drops = new ArrayList<>(this.equipment.values());
        if (!type.is(DamageTypeTags.IS_EXPLOSION) && (type.is(DamageTypeTags.IS_PLAYER_ATTACK) || type.is(DamageTypeTags.ALWAYS_KILLS_ARMOR_STANDS))) {
            ItemStack stand = ItemStack.from(ItemTypes.ARMOR_STAND);
            if (this.hasNameTag()) {
                stand = stand.toBuilder().setData(ItemDataComponents.CUSTOM_NAME, this.getNameTag()).build();
            }
            drops.add(stand);
        }

        return drops.toArray(ItemStack[]::new);
    }

    @Override
    protected void processDeath(EntityDeathEvent event) {
        this.playSound(LevelEvent.SOUND_ARMOR_STAND_BREAK);
        LevelEventPacket particles = new LevelEventPacket();
        particles.setType(LevelEvent.PARTICLE_DESTROY_ARMOR_STAND);
        particles.setPosition(this.getPosition());
        this.getLevel().addChunkPacket(this.getPosition(), particles);

        if (this.getLevel().getGameRules().get(GameRules.DO_ENTITY_DROPS)) {
            for (ItemStack item : event.getDrops()) {
                this.getLevel().dropItem(this.getPosition(), item);
            }

            if (event.getDroppedExperience() > 0) {
                this.dropDeathExperience(event.getDroppedExperience());
            }
        }

        this.equipment.clear();
        this.close();
    }

    public void playPlacementSound() {
        this.playSound(LevelEvent.SOUND_ARMOR_STAND_PLACE);
    }

    private void playEquipmentInteractionSounds(EquipmentSlot slot, ItemStack item) {
        if (slot.isArmor() && !item.isEmpty()) {
            SoundType sound = CloudItemRegistry.get().getComponent(item.getType(), ItemBehaviors.EQUIP_SOUND);
            if (sound != null) {
                float height = switch (slot) {
                    case HEAD -> this.getHeight() * 0.85f;
                    case CHEST, LEGS -> this.getHeight() * 0.5f;
                    default -> 0;
                };

                Vector3f position = this.getPosition().add(0, height, 0);
                SoundEvent event = EQUIP_EVENTS.get(sound);
                if (event != null) {
                    this.sendSoundEvent(position, event, this.getType().getId().toString(), this.getUniqueId());
                } else {
                    this.getLevel().playSound(position, sound);
                }
            }
        }

        this.sendSoundEvent(this.getPosition().add(0.5f, 0.5f, 0.5f), SoundEvent.MOB_ARMOR_STAND_PLACE, "", -1);
    }

    private void sendSoundEvent(Vector3f position, SoundEvent sound, String identifier, long entityUniqueId) {
        LevelSoundEventPacket packet = new LevelSoundEventPacket();
        packet.setSound(sound);
        packet.setPosition(position);
        packet.setExtraData(-1);
        packet.setIdentifier(identifier);
        packet.setEntityUniqueId(entityUniqueId);
        this.getLevel().addChunkPacket(this.getPosition(), packet);
    }

    private void playSound(LevelEvent sound) {
        Vector3f position = this.getPosition();
        this.getLevel().addChunkPacket(position, LevelEffectPacketFactory.event(position, sound, 0));
    }

    @Override
    public void spawnTo(CloudPlayer player) {
        boolean alreadySpawned = this.getViewers().contains(player);
        super.spawnTo(player);
        if (!alreadySpawned && this.getViewers().contains(player)) {
            player.sendPacket(this.armorPacket());
            player.sendPacket(this.handPacket(EquipmentSlot.MAIN_HAND));
            player.sendPacket(this.handPacket(EquipmentSlot.OFF_HAND));
        }
    }

    private MobArmorEquipmentPacket armorPacket() {
        MobArmorEquipmentPacket packet = new MobArmorEquipmentPacket();
        packet.setRuntimeEntityId(this.getRuntimeId());
        packet.setHelmet(ItemStackPacketCodec.toNetwork(this.getEquipment(EquipmentSlot.HEAD)));
        packet.setChestplate(ItemStackPacketCodec.toNetwork(this.getEquipment(EquipmentSlot.CHEST)));
        packet.setLeggings(ItemStackPacketCodec.toNetwork(this.getEquipment(EquipmentSlot.LEGS)));
        packet.setBoots(ItemStackPacketCodec.toNetwork(this.getEquipment(EquipmentSlot.FEET)));
        packet.setBody(ItemData.AIR);
        return packet;
    }

    private MobEquipmentPacket handPacket(EquipmentSlot slot) {
        boolean offhand = slot == EquipmentSlot.OFF_HAND;
        MobEquipmentPacket packet = new MobEquipmentPacket();
        packet.setRuntimeEntityId(this.getRuntimeId());
        packet.setItem(ItemStackPacketCodec.toNetwork(this.getEquipment(slot)));
        packet.setContainerId(offhand ? ContainerId.OFFHAND : ContainerId.INVENTORY);
        packet.setInventorySlot(0);
        packet.setHotbarSlot(offhand ? -1 : 0);
        return packet;
    }
}
