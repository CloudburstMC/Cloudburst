package org.cloudburstmc.server.entity.impl.passive;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.passive.Sheep;
import org.cloudburstmc.server.event.entity.EntityDamageByEntityEvent;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ItemTypes;
import org.cloudburstmc.server.level.Location;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.data.DyeColor;

import java.util.concurrent.ThreadLocalRandom;

import static com.nukkitx.protocol.bedrock.data.entity.EntityData.COLOR;
import static com.nukkitx.protocol.bedrock.data.entity.EntityFlag.SHEARED;
import static org.cloudburstmc.server.block.BlockTypes.WOOL;

/**
 * Author: BeYkeRYkt Nukkit Project
 */
public class EntitySheep extends Animal implements Sheep {

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
    public String getName() {
        return "Sheep";
    }

    @Override
    public void initEntity() {
        this.setMaxHealth(8);
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);

        tag.listenForByte("Color", this::setColor);
        tag.listenForBoolean("Sheared", this::setSheared);
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);

        tag.putByte("Color", (byte) this.getColor());
        tag.putBoolean("Sheared", this.isSheared());
    }

    @Override
    public boolean onInteract(Player player, ItemStack item) {
        if (item.getType() == ItemTypes.DYE) {
            this.setColor(item.getMetadata(DyeColor.class).getWoolData());
            return true;
        }

        return item.getType() == ItemTypes.SHEARS && shear();
    }

    public boolean shear() {
        if (isSheared()) {
            return false;
        }

        this.setSheared(true);
        this.data.setFlag(SHEARED, true);

        this.level.dropItem(this.getPosition(), ItemStack.get(WOOL, ThreadLocalRandom.current().nextInt(2) + 1, DyeColor.getByWoolData(getColor())));
        return true;
    }

    @Override
    public ItemStack[] getDrops() {
        if (this.lastDamageCause instanceof EntityDamageByEntityEvent) {
            return new ItemStack[]{ItemStack.get(WOOL, 1, DyeColor.getByWoolData(getColor()))};
        }
        return new ItemStack[0];
    }

    public boolean isSheared() {
        return this.data.getFlag(SHEARED);
    }

    public void setSheared(boolean sheared) {
        this.data.setFlag(SHEARED, sheared);
    }

    public int getColor() {
        return this.data.getByte(COLOR);
    }

    public void setColor(int color) {
        this.data.setByte(COLOR, color);
    }

    private int randomColor() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double rand = random.nextDouble(1, 100);

        if (rand <= 0.164) {
            return DyeColor.PINK.getWoolData();
        }

        if (rand <= 15) {
            return random.nextBoolean() ? DyeColor.BLACK.getWoolData() : random.nextBoolean() ? DyeColor.GRAY.getWoolData() : DyeColor.LIGHT_GRAY.getWoolData();
        }

        return DyeColor.WHITE.getWoolData();
    }
}
