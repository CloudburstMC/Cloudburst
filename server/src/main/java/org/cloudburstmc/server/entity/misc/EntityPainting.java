package org.cloudburstmc.server.entity.misc;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.damage.DamageTypeTags;
import org.cloudburstmc.api.entity.misc.Painting;
import org.cloudburstmc.api.entity.misc.PaintingVariant;
import org.cloudburstmc.api.event.entity.EntitySpawnEvent;
import org.cloudburstmc.api.event.entity.HangingBreakCause;
import org.cloudburstmc.api.event.entity.HangingBreakEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.BoundingBox;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.protocol.bedrock.packet.AddPaintingPacket;
import org.cloudburstmc.server.entity.CloudDamageResult;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.entity.data.CloudEntityData;
import org.cloudburstmc.server.entity.data.CloudPaintingData;
import org.cloudburstmc.server.event.entity.CloudEntityDamageEvent;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.network.LevelEffectPacketFactory;

import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public class EntityPainting extends CloudEntity implements Painting {

    private static final PaintingVariant[] VARIANTS = PaintingVariant.values();
    private static final int SUPPORT_CHECK_INTERVAL = 20;
    private static final float HALF_THICKNESS = 1f / 32;

    private int supportCheckTicks;
    private boolean breaking;

    public EntityPainting(EntityType<Painting> type, Location location) {
        super(type, location);
    }

    @Override
    protected Class<? extends CloudEntityData> minimumDataClass() {
        return CloudPaintingData.class;
    }

    @Override
    protected CloudPaintingData data() {
        return (CloudPaintingData) super.data();
    }

    @Override
    protected void initEntity() {
        this.setMaxHealth(1);
        super.initEntity();
    }

    @Override
    protected void loadCustom(NbtMap tag) {
        super.loadCustom(tag);
        tag.listenForByte("Direction", facing -> this.setRotation(facing * 90f, 0));
        tag.listenForString("Motive", name -> this.setVariant(readVariant(name)));
    }

    @Override
    protected void saveCustom(NbtMapBuilder tag) {
        super.saveCustom(tag);
        tag.putByte("Direction", (byte) this.getFacing().getHorizontalIndex());
        tag.putString("Motive", networkTitle(this.data().getVariant()));
        tag.putInt("TileX", this.anchor().getX());
        tag.putInt("TileY", this.anchor().getY());
        tag.putInt("TileZ", this.anchor().getZ());
    }

    @Override
    public PaintingVariant getVariant() {
        return this.data().getVariant();
    }

    @Override
    public void setVariant(PaintingVariant variant) {
        this.data().setVariant(Objects.requireNonNull(variant, "variant"));
        this.recalculateBoundingBox();
        this.refreshAppearance();
    }

    @Override
    public Direction getFacing() {
        return Direction.fromYaw(this.getYaw());
    }

    @Override
    public Direction getDirection() {
        return this.getFacing();
    }

    @Override
    public void setRotation(float yaw, float pitch) {
        super.setRotation(Direction.fromYaw(yaw).getHorizontalIndex() * 90f, 0);
        this.recalculateBoundingBox();
        this.refreshAppearance();
    }

    private void refreshAppearance() {
        if (!this.getViewers().isEmpty()) {
            this.despawnFromAll();
            this.spawnToAll();
        }
    }

    @Override
    public void recalculateBoundingBox() {
        this.boundingBox = bounds(this.anchor(), this.getFacing(), this.data().getVariant() == null ? PaintingVariant.KEBAB : this.data().getVariant());
    }

    @Override
    public boolean canCollide() {
        return false;
    }

    @Override
    public boolean canTriggerPressurePlate() {
        return false;
    }

    @Override
    public boolean entityBaseTick(int tickDiff) {
        this.supportCheckTicks += tickDiff;
        if (this.supportCheckTicks >= SUPPORT_CHECK_INTERVAL) {
            this.supportCheckTicks %= SUPPORT_CHECK_INTERVAL;
            Fit fit = this.checkFit(this.data().getVariant());
            if (fit == Fit.UNSUPPORTED) {
                this.breakPainting(HangingBreakCause.PHYSICS, null);
            } else if (fit == Fit.OBSTRUCTED) {
                this.breakPainting(HangingBreakCause.OBSTRUCTION, null);
            }
        }
        return !this.isClosed();
    }

    public boolean selectLargestVariant() {
        PaintingVariant selected = null;
        int largestArea = 0;
        int ties = 0;
        Fit[][] fitBySize = new Fit[5][5];
        for (PaintingVariant candidate : VARIANTS) {
            int width = candidate.getWidth();
            int height = candidate.getHeight();
            int area = width * height;
            if (area < largestArea || !isPlaceable(candidate)) {
                continue;
            }

            Fit fit = fitBySize[width][height];
            if (fit == null) {
                fit = this.checkFit(candidate);
                fitBySize[width][height] = fit;
            }

            if (fit != Fit.VALID) {
                continue;
            }

            if (area > largestArea) {
                largestArea = area;
                ties = 0;
            }

            if (ThreadLocalRandom.current().nextInt(++ties) == 0) {
                selected = candidate;
            }
        }

        if (selected == null) {
            return false;
        }

        this.setVariant(selected);
        return true;
    }

    public boolean fitsWall() {
        return this.checkFit(this.data().getVariant()) == Fit.VALID;
    }

    public boolean spawnOnWall() {
        CloudLevel level = this.getLevel();
        return this.spawn(new EntitySpawnEvent(this), () -> this.getLevel() == level && this.fitsWall());
    }

    private Fit checkFit(PaintingVariant variant) {
        Vector3i anchor = this.anchor();
        Direction facing = this.getFacing();
        Direction left = facing.rotateCounterClockwise();
        Direction behind = facing.getOpposite();
        Vector3i start = anchor.sub(left.getStepX() * ((variant.getWidth() - 1) / 2),
                (variant.getHeight() - 1) / 2, left.getStepZ() * ((variant.getWidth() - 1) / 2));
        BoundingBox bounds = bounds(anchor, facing, variant);

        for (int x = 0; x < variant.getWidth(); x++) {
            for (int y = 0; y < variant.getHeight(); y++) {
                Vector3i cell = start.add(left.getStepX() * x, y, left.getStepZ() * x);
                if (this.getLevel().isOutsideBuildHeight(cell.getY())) {
                    return Fit.OBSTRUCTED;
                }

                Block front = this.getLevel().getLoadedBlock(cell);
                Block back = this.getLevel().getLoadedBlock(behind.relative(cell));
                if (front == null || back == null) {
                    return Fit.UNLOADED;
                }

                if (isFrame(front.getState().getType()) || this.getLevel().hasBlockCollision(this, front.getState(), cell, bounds)) {
                    return Fit.OBSTRUCTED;
                }

                if (!back.getState().isSolid() && !isRedstoneSupport(back.getState().getType())) {
                    return Fit.UNSUPPORTED;
                }
            }
        }

        return this.getLevel().getNearbyEntities(bounds,
                entity -> entity != this && entity instanceof Painting && !entity.isClosed()).isEmpty()
                ? Fit.VALID : Fit.OBSTRUCTED;
    }

    @Override
    public CloudDamageResult damageWithResult(float amount, DamageSource source) {
        if (amount <= 0 || this.isDamageImmune(source)) {
            return CloudDamageResult.REJECTED;
        }

        CloudEntityDamageEvent damage = this.createDamageEvent(source, amount);
        this.getServer().getEventManager().fire(damage);
        if (damage.isCancelled()) {
            return CloudDamageResult.CANCELLED;
        }

        if (damage.getDamage() <= 0 || this.isClosed() || !this.isAlive()) {
            return CloudDamageResult.REJECTED;
        }

        Entity remover = source.getCausingEntity() == null ? source.getDirectEntity() : source.getCausingEntity();
        HangingBreakCause cause = source.getDamageType().is(DamageTypeTags.IS_EXPLOSION)
                ? HangingBreakCause.EXPLOSION
                : remover == null ? HangingBreakCause.DEFAULT : HangingBreakCause.ENTITY;
        return this.breakPainting(cause, remover) ? CloudDamageResult.HANDLED : CloudDamageResult.CANCELLED;
    }

    private boolean breakPainting(HangingBreakCause cause, @Nullable Entity remover) {
        if (this.breaking || this.isClosed()) {
            return false;
        }

        this.breaking = true;
        try {
            HangingBreakEvent event = new HangingBreakEvent(this, cause, remover);
            this.getServer().getEventManager().fire(event);
            if (event.isCancelled() || this.isClosed()) {
                return false;
            }

            Vector3f center = center(this.boundingBox);
            this.getLevel().addLevelEvent(center, LevelEvent.SOUND_ITEMFRAME_BREAK, 0);
            this.getLevel().addChunkPacket(center, LevelEffectPacketFactory.blockDestruction(center, BlockStates.OAK_PLANKS, false));
            if (this.getLevel().getGameRules().get(GameRules.DO_ENTITY_DROPS) && (cause == HangingBreakCause.EXPLOSION || !(remover instanceof Player player && player.isCreative()))) {
                this.getLevel().dropItem(center, ItemStack.from(ItemTypes.PAINTING));
            }
            this.enterDeathState();
            this.close();
            return true;
        } finally {
            this.breaking = false;
        }
    }

    @Override
    public AddPaintingPacket createAddEntityPacket() {
        AddPaintingPacket packet = new AddPaintingPacket();
        packet.setUniqueEntityId(this.getUniqueId());
        packet.setRuntimeEntityId(this.getRuntimeId());
        packet.setPosition(center(this.boundingBox));
        packet.setDirection(this.getFacing().getHorizontalIndex());
        packet.setMotive(networkTitle(this.data().getVariant()));
        return packet;
    }

    private Vector3i anchor() {
        return Vector3i.from(this.position.getFloorX(), this.position.getFloorY(), this.position.getFloorZ());
    }

    private static BoundingBox bounds(Vector3i anchor, Direction facing, PaintingVariant variant) {
        Direction left = facing.rotateCounterClockwise();
        float x = anchor.getX() + 0.5f - facing.getStepX() * (0.5f - HALF_THICKNESS);
        float y = anchor.getY() + 0.5f + (variant.getHeight() % 2 == 0 ? 0.5f : 0);
        float z = anchor.getZ() + 0.5f - facing.getStepZ() * (0.5f - HALF_THICKNESS);
        if (variant.getWidth() % 2 == 0) {
            x += left.getStepX() * 0.5f;
            z += left.getStepZ() * 0.5f;
        }
        float halfWidth = variant.getWidth() / 2f;
        float halfHeight = variant.getHeight() / 2f;
        float halfX = facing.getStepX() == 0 ? halfWidth : HALF_THICKNESS;
        float halfZ = facing.getStepZ() == 0 ? halfWidth : HALF_THICKNESS;
        return new BoundingBox(x - halfX, y - halfHeight, z - halfZ, x + halfX, y + halfHeight, z + halfZ);
    }

    private static Vector3f center(BoundingBox box) {
        return Vector3f.from((box.getMinX() + box.getMaxX()) / 2,
                (box.getMinY() + box.getMaxY()) / 2, (box.getMinZ() + box.getMaxZ()) / 2);
    }

    private static boolean isFrame(BlockType type) {
        return type == BlockTypes.FRAME || type == BlockTypes.GLOW_FRAME;
    }

    private static boolean isRedstoneSupport(BlockType type) {
        return type == BlockTypes.POWERED_REPEATER || type == BlockTypes.UNPOWERED_REPEATER
                || type == BlockTypes.POWERED_COMPARATOR || type == BlockTypes.UNPOWERED_COMPARATOR;
    }

    private static boolean isPlaceable(PaintingVariant variant) {
        return switch (variant) {
            case EARTH, WIND, WATER, FIRE -> false;
            default -> true;
        };
    }

    private static PaintingVariant readVariant(String name) {
        if (name.equalsIgnoreCase("Flaming Skull")) {
            return PaintingVariant.BURNING_SKULL;
        }
        for (PaintingVariant variant : VARIANTS) {
            if (networkTitle(variant).equalsIgnoreCase(name) || variant.name().equalsIgnoreCase(name)) {
                return variant;
            }
        }
        return PaintingVariant.KEBAB;
    }

    private static String networkTitle(PaintingVariant variant) {
        return switch (variant) {
            case SKULL_AND_ROSES -> "SkullAndRoses";
            case DONKEY_KONG -> "DonkeyKong";
            case BURNING_SKULL -> "BurningSkull";
            case KEBAB, AZTEC, ALBAN, AZTEC2, BOMB, PLANT, WASTELAND, WANDERER, GRAHAM,
                    POOL, COURBET, SUNSET, SEA, CREEBET, MATCH, BUST, STAGE, VOID, WITHER,
                    FIGHTERS, SKELETON, POINTER, PIGSCENE, EARTH, WIND, WATER, FIRE -> {
                String name = variant.name();
                yield name.charAt(0) + name.substring(1).toLowerCase(Locale.ROOT);
            }
            default -> variant.name().toLowerCase(Locale.ROOT);
        };
    }

    private enum Fit {
        VALID, UNLOADED, UNSUPPORTED, OBSTRUCTED
    }
}
