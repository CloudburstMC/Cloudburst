package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.passive.Sheep;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.sound.SoundTypes;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.data.DyeColor;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes.COLOR;
import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.SHEARED;

/**
 * Author: BeYkeRYkt Nukkit Project
 */
public class EntitySheep extends EntityBreedableAnimal implements Sheep {

    public static final int NETWORK_ID = 13;

    public EntitySheep(EntityType<Sheep> type, Location location) {
        super(type, location);
    }

    @Override
    public float getWidth() {
        if (this.isBaby()) {
            return 0.45f;
        }
        return 0.9f;
    }

    @Override
    public float getHeight() {
        if (isBaby()) {
            return 0.65f;
        }
        return 1.3f;
    }

    @Override
    protected String getDefaultName() {
        return "Sheep";
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setDefaultMaxHealth(8);
        this.data.set(COLOR, (byte) randomColor());
    }

    @Override
    protected void loadCustom(NbtMap tag) {
        if (tag.containsKey("Color")) {
            this.data.set(COLOR, getByte(tag, "Color", this.data.require(COLOR)));
        }
        if (tag.containsKey("Sheared")) {
            this.setSheared(getBoolean(tag, "Sheared", this.isSheared()));
        }
        super.loadCustom(tag);
    }

    @Override
    protected void saveCustom(NbtMapBuilder tag) {
        super.saveCustom(tag);
        tag.putByte("Color", (byte) this.getColor().getWoolData());
        tag.putBoolean("Sheared", this.isSheared());
    }

    @Override
    protected Set<String> customKeys() {
        Set<String> keys = new HashSet<>(super.customKeys());
        keys.add("Color");
        keys.add("Sheared");
        return Set.copyOf(keys);
    }

    @Override
    protected boolean interactDefault(Player player, ItemStack item, Vector3f clickedPos) {
        if (item.has(ItemDataComponents.COLOR)) {
            this.setColor(item.get(ItemDataComponents.COLOR));
            return true;
        }

        return PlayerShearing.tryShear(this, player, item, this::createShearingDrops, this::applyShearing)
                || super.interactDefault(player, item, clickedPos);
    }

    @Override
    public boolean readyForShearing() {
        return this.isAlive() && !this.isBaby() && !this.isSheared();
    }

    @Override
    public boolean shear() {
        if (!this.readyForShearing()) {
            return false;
        }

        this.applyShearing(this.createShearingDrops());
        return true;
    }

    private List<ItemStack> createShearingDrops() {
        return List.of(ItemStack.builder(getWoolState(this.getColor()))
                .amount(ThreadLocalRandom.current().nextInt(1, 4))
                .build());
    }

    private void applyShearing(List<ItemStack> drops) {
        this.setSheared(true);
        this.level.playSound(this.getPosition(), SoundTypes.MOB_SHEEP_SHEAR);
        for (ItemStack drop : drops) {
            this.level.dropItem(this.getPosition(), drop);
        }
    }

    @Override
    public ItemStack[] getDrops() {
        if (!this.isSheared() && this.lastDamageCause != null
                && this.lastDamageCause.getDamageSource().getCausingEntity() != null) {
            return new ItemStack[]{ItemStack.builder(getWoolState(getColor()))
                    .amount(1)
                    .build()};
        }
        return new ItemStack[0];
    }

    private static BlockState getWoolState(DyeColor color) {
        return switch (color) {
            case WHITE, NONE -> BlockStates.WHITE_WOOL;
            case ORANGE -> BlockStates.ORANGE_WOOL;
            case MAGENTA -> BlockStates.MAGENTA_WOOL;
            case LIGHT_BLUE -> BlockStates.LIGHT_BLUE_WOOL;
            case YELLOW -> BlockStates.YELLOW_WOOL;
            case LIME -> BlockStates.LIME_WOOL;
            case PINK -> BlockStates.PINK_WOOL;
            case GRAY -> BlockStates.GRAY_WOOL;
            case LIGHT_GRAY, SILVER -> BlockStates.LIGHT_GRAY_WOOL;
            case CYAN -> BlockStates.CYAN_WOOL;
            case PURPLE -> BlockStates.PURPLE_WOOL;
            case BLUE -> BlockStates.BLUE_WOOL;
            case BROWN -> BlockStates.BROWN_WOOL;
            case GREEN -> BlockStates.GREEN_WOOL;
            case RED -> BlockStates.RED_WOOL;
            case BLACK -> BlockStates.BLACK_WOOL;
        };
    }

    @Override
    public boolean isSheared() {
        return this.data.getFlag(SHEARED);
    }

    @Override
    public void setSheared(boolean sheared) {
        if (this.isSheared() != sheared) {
            this.data.setFlag(SHEARED, sheared);
            if (this.isValid()) {
                this.markChunkDirty();
            }
        }
    }

    @Override
    public DyeColor getColor() {
        return DyeColor.getByWoolData(this.data.require(COLOR));
    }

    @Override
    public void setColor(DyeColor color) {
        byte woolColor = (byte) color.getWoolData();
        Byte previousColor = this.data.get(COLOR);
        if (previousColor == null || previousColor != woolColor) {
            this.data.set(COLOR, woolColor);
            if (this.isValid()) {
                this.markChunkDirty();
            }
        }
    }

    private int randomColor() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double rand = random.nextDouble(100);

        if (rand <= 0.164) {
            return DyeColor.PINK.getWoolData();
        }

        if (rand <= 15) {
            return random.nextBoolean() ? DyeColor.BLACK.getWoolData() : random.nextBoolean() ? DyeColor.GRAY.getWoolData() : DyeColor.LIGHT_GRAY.getWoolData();
        }

        return DyeColor.WHITE.getWoolData();
    }
}
