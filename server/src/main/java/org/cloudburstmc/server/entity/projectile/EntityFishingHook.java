package org.cloudburstmc.server.entity.projectile;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.enchantment.EnchantmentType;
import org.cloudburstmc.api.enchantment.EnchantmentTypes;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.misc.DroppedItem;
import org.cloudburstmc.api.entity.projectile.FishingHook;
import org.cloudburstmc.api.entity.projectile.FishingHookState;
import org.cloudburstmc.api.event.entity.FishingHookStateChangeEvent;
import org.cloudburstmc.api.event.entity.ProjectileHitEvent;
import org.cloudburstmc.api.event.player.PlayerFishEvent;
import org.cloudburstmc.api.event.player.PlayerFishState;
import org.cloudburstmc.api.item.ItemKeys;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.MovingObjectPosition;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityEventType;
import org.cloudburstmc.protocol.bedrock.packet.EntityEventPacket;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.entity.misc.EntityDroppedItem;
import org.cloudburstmc.server.item.loot.FishingLoot;
import org.cloudburstmc.server.level.particle.BubbleParticle;
import org.cloudburstmc.server.level.particle.WaterParticle;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudEntityRegistry;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class EntityFishingHook extends EntityProjectile implements FishingHook {
    private static final int MIN_WAIT_TIME = 100;
    private static final int MAX_WAIT_TIME = 600;
    private static final int MAX_GROUND_TIME = 1200;

    private FishingHookState fishingState = FishingHookState.FLYING;
    private @Nullable Entity hookedEntity;
    private int luck;
    private int lureSpeed;
    private int groundTime;
    private int outOfWaterTime;
    private int timeUntilLured;
    private int timeUntilHooked;
    private int biteTime;
    private float fishAngle;
    private boolean biting;
    private boolean openWater = true;

    public EntityFishingHook(EntityType<FishingHook> type, Location location) {
        super(type, location);
        this.closeOnCollide = false;
    }

    public void configure(ItemStack rod) {
        Map<EnchantmentType, Enchantment> enchantments = rod.get(ItemKeys.ENCHANTMENTS);
        Enchantment luckEnchantment = enchantments.get(EnchantmentTypes.LUCK_OF_THE_SEA);
        Enchantment lureEnchantment = enchantments.get(EnchantmentTypes.LURE);
        this.luck = luckEnchantment == null ? 0 : Math.max(0, luckEnchantment.level());
        this.lureSpeed = lureEnchantment == null ? 0 : Math.max(0, lureEnchantment.level() * 100);
    }

    @Override
    public FishingHookState getFishingState() {
        return this.fishingState;
    }

    @Override
    public @Nullable Entity getHookedEntity() {
        return this.hookedEntity;
    }

    @Override
    public void setHookedEntity(@Nullable Entity entity) {
        if (entity != null && entity.getLevel() != this.getLevel()) {
            throw new IllegalArgumentException("Hooked entity must be in the same level");
        }

        this.hookedEntity = entity;
        setFishingState(entity == null ? FishingHookState.FLYING : FishingHookState.HOOKED_IN_ENTITY);

        if (entity != null) {
            this.motion = Vector3f.ZERO;
        }
    }

    @Override
    public boolean pullHookedEntity() {
        CloudPlayer owner = playerOwner();
        if (owner == null || this.hookedEntity == null) {
            return false;
        }

        pull(this.hookedEntity, owner);
        return true;
    }

    @Override
    public boolean isBiting() {
        return this.biting;
    }

    @Override
    public boolean isOpenWaterFishing() {
        return this.openWater;
    }

    @Override
    public float getWidth() {
        return 0.25f;
    }

    @Override
    public float getLength() {
        return 0.25f;
    }

    @Override
    public float getHeight() {
        return 0.25f;
    }

    @Override
    public float getGravity() {
        return isInWaterBlock() ? 0 : 0.03f;
    }

    @Override
    public float getDrag() {
        return 0.08f;
    }

    @Override
    public boolean onUpdate(int currentTick) {
        CloudPlayer owner = playerOwner();
        if (owner == null || shouldStopFishing(owner)) {
            this.close();
            return false;
        }

        if (this.onGround) {
            if (++this.groundTime >= MAX_GROUND_TIME) {
                this.close();
                return false;
            }
        } else {
            this.groundTime = 0;
        }

        if (this.fishingState == FishingHookState.HOOKED_IN_ENTITY) {
            if (this.hookedEntity == null || !this.hookedEntity.isAlive() || this.hookedEntity.getLevel() != this.getLevel()) {
                setHookedEntity(null);
            } else {
                this.setPosition(this.hookedEntity.getPosition().add(0, this.hookedEntity.getHeight() * 0.8f, 0));
                this.motion = Vector3f.ZERO;
                return true;
            }
        }

        boolean bobbing = this.fishingState == FishingHookState.BOBBING;
        if (bobbing) {
            tickBobbing(isInWaterBlock(), owner);
        }

        boolean updated = super.onUpdate(currentTick);
        if (this.closed) {
            return false;
        }

        if (this.fishingState == FishingHookState.FLYING && isInWaterBlock()) {
            setFishingState(FishingHookState.BOBBING);
            this.motion = this.motion.mul(0.3f, 0.2f, 0.3f);
        }

        if (bobbing) {
            updated = true;
        }

        this.motion = this.motion.mul(1 - this.getDrag());
        this.updateMovement();
        return updated;
    }

    private void tickBobbing(boolean inWater, CloudPlayer owner) {
        if (!inWater) {
            this.outOfWaterTime = Math.min(10, this.outOfWaterTime + 1);
            return;
        }

        this.outOfWaterTime = Math.max(0, this.outOfWaterTime - 1);
        Vector3i position = this.getPosition().toInt();
        float surface = position.getY() + this.getLevel().getLiquidHeight(position);
        float force = this.getY() + this.motion.getY() - surface;
        if (Math.abs(force) < 0.01f) {
            force += Math.copySign(0.1f, force == 0 ? 1 : force);
        }

        this.motion = Vector3f.from(this.motion.getX() * 0.9f,
                this.motion.getY() - force * ThreadLocalRandom.current().nextFloat() * 0.2f,
                this.motion.getZ() * 0.9f);
        if (this.biting) {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            this.motion = this.motion.add(0, -0.1f * random.nextFloat() * random.nextFloat(), 0);
        }

        if (this.biteTime <= 0 && this.timeUntilHooked <= 0) {
            this.openWater = true;
        } else {
            this.openWater &= this.outOfWaterTime < 10 && calculateOpenWater(position);
        }

        catchFish(owner, position);
    }

    private void catchFish(CloudPlayer owner, Vector3i position) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int speed = 1;
        Vector3i above = Direction.UP.relative(position);
        if (random.nextFloat() < 0.25f && this.getLevel().isRaining() && this.getLevel().canBlockSeeSky(above)) {
            speed++;
        }

        if (random.nextFloat() < 0.5f && !this.getLevel().canBlockSeeSky(above)) {
            speed--;
        }

        if (this.biteTime > 0) {
            this.biteTime--;
            if (this.biteTime <= 0) {
                this.biting = false;
                this.timeUntilLured = 0;
                this.timeUntilHooked = 0;
                fire(owner, PlayerFishState.FAILED_ATTEMPT, null);
            }
            return;
        }

        if (this.timeUntilHooked > 0) {
            this.timeUntilHooked -= speed;
            if (this.timeUntilHooked <= 0) {
                if (fire(owner, PlayerFishState.BITE, null).isCancelled()) {
                    return;
                }
                startBite(random);
            } else {
                spawnApproachParticle(random);
            }
            return;
        }

        if (this.timeUntilLured > 0) {
            this.timeUntilLured -= speed;
            if (this.timeUntilLured <= 0) {
                this.fishAngle = random.nextFloat(360);
                this.timeUntilHooked = random.nextInt(20, 81);
                if (fire(owner, PlayerFishState.LURED, null).isCancelled()) {
                    this.timeUntilHooked = 0;
                }
            } else if (random.nextFloat() < teaseChance()) {
                spawnTeaseParticle(random);
            }
            return;
        }

        this.timeUntilLured = Math.max(1, random.nextInt(MIN_WAIT_TIME, MAX_WAIT_TIME + 1) - this.lureSpeed);
    }

    private void startBite(ThreadLocalRandom random) {
        this.biteTime = random.nextInt(20, 41);
        this.biting = true;
        this.motion = Vector3f.from(this.motion.getX(), -0.4f * random.nextFloat(0.6f, 1), this.motion.getZ());

        sendHookEvent(EntityEventType.FISH_HOOK_TIME);
        sendHookEvent(EntityEventType.FISH_HOOK_BUBBLE);
        sendHookEvent(EntityEventType.FISH_HOOK_TEASE);

        for (int i = 0; i < 5; i++) {
            this.getLevel().addParticle(new BubbleParticle(Vector3f.from(
                    this.getX() + random.nextFloat(-0.25f, 0.25f),
                    this.getY() + 0.5f,
                    this.getZ() + random.nextFloat(-0.25f, 0.25f))));
        }
    }

    private float teaseChance() {
        if (this.timeUntilLured < 20) {
            return 0.15f + (20 - this.timeUntilLured) * 0.05f;
        }

        if (this.timeUntilLured < 40) {
            return 0.15f + (40 - this.timeUntilLured) * 0.02f;
        }

        if (this.timeUntilLured < 60) {
            return 0.15f + (60 - this.timeUntilLured) * 0.01f;
        }

        return 0.15f;
    }

    private void spawnApproachParticle(ThreadLocalRandom random) {
        this.fishAngle += (float) ((random.nextDouble() - random.nextDouble()) * 9.188);
        float angle = (float) Math.toRadians(this.fishAngle);
        float distance = this.timeUntilHooked * 0.1f;

        Vector3f fish = Vector3f.from(this.getX() + Math.sin(angle) * distance,
                (float) Math.floor(this.getY()) + 1, this.getZ() + Math.cos(angle) * distance);
        if (!hasWaterBelow(fish)) {
            return;
        }

        if (random.nextFloat() < 0.15f) {
            this.getLevel().addParticle(new BubbleParticle(fish.sub(0, 0.1f, 0)));
        }

        this.getLevel().addParticle(new WaterParticle(fish));
    }

    private void spawnTeaseParticle(ThreadLocalRandom random) {
        float angle = random.nextFloat((float) (Math.PI * 2));
        float distance = random.nextFloat(2.5f, 6f);
        Vector3f fish = Vector3f.from(this.getX() + Math.sin(angle) * distance,
                (float) Math.floor(this.getY()) + 1, this.getZ() + Math.cos(angle) * distance);
        if (hasWaterBelow(fish)) {
            this.getLevel().addParticle(new WaterParticle(fish));
        }
    }

    private boolean hasWaterBelow(Vector3f position) {
        BlockType type = this.getLevel().getBlock(position.toInt().sub(0, 1, 0)).getState().getType();
        return type == BlockTypes.WATER || type == BlockTypes.FLOWING_WATER;
    }

    private boolean calculateOpenWater(Vector3i origin) {
        FishingOpenWaterType previous = FishingOpenWaterType.INVALID;
        for (int y = -1; y <= 2; y++) {
            FishingOpenWaterType layer = openWaterLayer(origin.add(-2, y, -2), origin.add(2, y, 2));
            if (layer == FishingOpenWaterType.INVALID
                    || layer == FishingOpenWaterType.ABOVE_WATER && previous == FishingOpenWaterType.INVALID
                    || layer == FishingOpenWaterType.INSIDE_WATER && previous == FishingOpenWaterType.ABOVE_WATER) {
                return false;
            }

            previous = layer;
        }

        return true;
    }

    private FishingOpenWaterType openWaterLayer(Vector3i from, Vector3i to) {
        FishingOpenWaterType result = null;
        for (int x = from.getX(); x <= to.getX(); x++) {
            for (int y = from.getY(); y <= to.getY(); y++) {
                for (int z = from.getZ(); z <= to.getZ(); z++) {
                    Block block = this.getLevel().getBlock(x, y, z);
                    FishingOpenWaterType type = openWaterType(block);

                    if (result != null && result != type) {
                        return FishingOpenWaterType.INVALID;
                    }

                    result = type;
                }
            }
        }

        return result == null ? FishingOpenWaterType.INVALID : result;
    }

    private static FishingOpenWaterType openWaterType(Block block) {
        if (block.getState() == BlockStates.AIR || block.getState().getType() == BlockTypes.WATERLILY) {
            return FishingOpenWaterType.ABOVE_WATER;
        }

        LiquidState liquid = block.getLiquid();
        return isWater(liquid) && liquid.isSource() && block.getState().getCollisionShape().isEmpty()
                ? FishingOpenWaterType.INSIDE_WATER : FishingOpenWaterType.INVALID;
    }

    private boolean isInWaterBlock() {
        Vector3i position = this.getPosition().toInt();
        LiquidState liquid = this.getLevel().getLiquidState(position);
        return isWater(liquid);
    }

    private static boolean isWater(LiquidState liquid) {
        return liquid.getType().isSameFamily(LiquidTypes.WATER);
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return super.canCollideWith(entity) || entity instanceof DroppedItem && !this.onGround;
    }

    @Override
    public void onCollideWithEntity(Entity entity) {
        this.server.getEventManager().fire(new ProjectileHitEvent(this, MovingObjectPosition.fromEntity(entity)));
        setHookedEntity(entity);
    }

    @Override
    public int retrieve(ItemStack rod) {
        CloudPlayer owner = playerOwner();
        if (rod.getType() != ItemTypes.FISHING_ROD) {
            throw new IllegalArgumentException("rod must be a fishing rod");
        }

        if (owner == null) {
            throw new IllegalStateException("Fishing hook has no player owner");
        }

        if (shouldStopFishing(owner)) {
            this.close();
            return 0;
        }

        int damage = 0;
        if (this.hookedEntity != null) {
            PlayerFishEvent event = fire(owner, PlayerFishState.CAUGHT_ENTITY, this.hookedEntity);
            if (event.isCancelled()) {
                return 0;
            }

            pull(this.hookedEntity, owner);
            damage = this.hookedEntity instanceof DroppedItem ? 3 : 5;
        } else if (this.biteTime > 0) {
            ItemStack item = FishingLoot.select(this.luck, this.openWater);
            DroppedItem dropped = createCaughtItem(item, owner);
            PlayerFishEvent event = new PlayerFishEvent(owner, this, dropped, PlayerFishState.CAUGHT_ITEM);
            event.setExperience(ThreadLocalRandom.current().nextInt(1, 7));
            this.server.getEventManager().fire(event);
            if (event.isCancelled()) {
                dropped.close();
                return 0;
            }

            dropped.spawnToAll();
            if (event.getExperience() > 0) {
                this.getLevel().spawnExperienceOrb(owner.getPosition().add(0, 0.5f, 0.5f),
                        event.getExperience(), null, 0);
            }

            damage = 1;
        } else {
            PlayerFishState state = this.onGround ? PlayerFishState.IN_GROUND : PlayerFishState.REEL_IN;
            if (fire(owner, state, null).isCancelled()) {
                return 0;
            }
        }

        if (this.onGround) {
            damage = 2;
        }

        this.close();
        return damage;
    }

    private DroppedItem createCaughtItem(ItemStack item, CloudPlayer owner) {
        Vector3f delta = owner.getPosition().sub(this.getPosition());
        Vector3f motion = delta.mul(0.1f).add(0, (float) Math.sqrt(Math.sqrt(delta.lengthSquared())) * 0.08f, 0);
        DroppedItem dropped = CloudEntityRegistry.get().newEntity(EntityTypes.ITEM, this.getLocation());
        dropped.setItem(item);
        dropped.setMotion(motion);
        ((EntityDroppedItem) dropped).setFromFishing(true);
        return dropped;
    }

    private static void pull(Entity entity, CloudPlayer owner) {
        entity.setMotion(entity.getMotion().add(owner.getPosition().sub(entity.getPosition()).mul(0.1f)));
    }

    private PlayerFishEvent fire(CloudPlayer owner, PlayerFishState state, @Nullable Entity caught) {
        PlayerFishEvent event = new PlayerFishEvent(owner, this, caught, state);
        this.server.getEventManager().fire(event);
        return event;
    }

    private void setFishingState(FishingHookState state) {
        if (this.fishingState == state) {
            return;
        }

        FishingHookState previous = this.fishingState;
        this.fishingState = state;
        this.server.getEventManager().fire(new FishingHookStateChangeEvent(this, previous, state));
    }

    private void sendHookEvent(EntityEventType type) {
        EntityEventPacket packet = new EntityEventPacket();
        packet.setRuntimeEntityId(this.getRuntimeId());
        packet.setType(type);
        CloudServer.broadcastPacket(this.getViewers(), packet);
    }

    private boolean shouldStopFishing(CloudPlayer owner) {
        ItemStack mainHand = owner.getInventory().getSelectedItem();
        ItemStack offhand = owner.getOffhand().getOffhandItem();
        return !owner.isAlive() || owner.getLevel() != this.getLevel()
                || (mainHand.getType() != ItemTypes.FISHING_ROD
                && offhand.getType() != ItemTypes.FISHING_ROD)
                || owner.getPosition().distanceSquared(this.getPosition()) > 1024;
    }

    private @Nullable CloudPlayer playerOwner() {
        return this.getOwner() instanceof CloudPlayer player ? player : null;
    }

    @Override
    public void close() {
        CloudPlayer owner = playerOwner();
        if (owner != null) {
            owner.clearFishingHook(this);
        }

        super.close();
    }

    @Override
    public boolean isCritical() {
        return false;
    }

    @Override
    public void setCritical(boolean critical) {
    }
}
