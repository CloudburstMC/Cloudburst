package org.cloudburstmc.server.entity.impl.hostile;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.hostile.Creeper;
import org.cloudburstmc.server.entity.misc.LightningBolt;
import org.cloudburstmc.server.event.entity.CreeperPowerEvent;
import org.cloudburstmc.server.event.entity.EntityDamageByEntityEvent;
import org.cloudburstmc.server.item.ItemIds;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.level.Location;

import java.util.concurrent.ThreadLocalRandom;

import static com.nukkitx.protocol.bedrock.data.entity.EntityFlag.POWERED;

/**
 * @author Box.
 */
public class EntityCreeper extends EntityHostile implements Creeper {

    public EntityCreeper(EntityType<Creeper> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();

        this.setMaxHealth(20);
    }

    @Override
    public float getWidth() {
        return 0.6f;
    }

    @Override
    public float getHeight() {
        return 1.7f;
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);

        tag.listenForBoolean("powered", this::setPowered);
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);

        tag.putBoolean("powered", this.isPowered());
    }

    public boolean isPowered() {
        return this.data.getFlag(POWERED);
    }

    public void setPowered(LightningBolt lightningBolt) {
        CreeperPowerEvent ev = new CreeperPowerEvent(this, lightningBolt, CreeperPowerEvent.PowerCause.LIGHTNING);
        this.getServer().getEventManager().fire(ev);

        if (!ev.isCancelled()) {
            this.data.setFlag(POWERED, true);
        }
    }

    public void setPowered(boolean powered) {
        CreeperPowerEvent ev = new CreeperPowerEvent(this, powered ? CreeperPowerEvent.PowerCause.SET_ON : CreeperPowerEvent.PowerCause.SET_OFF);
        this.getServer().getEventManager().fire(ev);

        if (!ev.isCancelled()) {
            this.data.setFlag(POWERED, powered);
        }
    }

    @Override
    public void onStruckByLightning(LightningBolt lightningBolt) {
        this.setPowered(true);
    }

    @Override
    public String getName() {
        return "Creeper";
    }

    @Override
    public ItemStack[] getDrops() {
        if (this.lastDamageCause instanceof EntityDamageByEntityEvent) {
            return new ItemStack[]{ItemStack.get(ItemIds.GUNPOWDER, ThreadLocalRandom.current().nextInt(2) + 1)};
        }
        return new ItemStack[0];
    }
}
