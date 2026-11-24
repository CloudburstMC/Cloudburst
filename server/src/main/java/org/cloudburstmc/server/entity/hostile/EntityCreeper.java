package org.cloudburstmc.server.entity.hostile;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.hostile.Creeper;
import org.cloudburstmc.api.entity.misc.LightningBolt;
import org.cloudburstmc.api.event.entity.CreeperPowerEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.POWERED;

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

        this.setDefaultMaxHealth(20);
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
    protected void loadCustom(NbtMap tag) {
        if (tag.containsKey("powered")) {
            this.data.setFlag(POWERED, getBoolean(tag, "powered", this.isPowered()));
        }
        super.loadCustom(tag);
    }

    @Override
    protected void saveCustom(NbtMapBuilder tag) {
        super.saveCustom(tag);
        tag.putBoolean("powered", this.isPowered());
    }

    @Override
    protected Set<String> customKeys() {
        Set<String> keys = new HashSet<>(super.customKeys());
        keys.add("powered");
        return Set.copyOf(keys);
    }

    @Override
    public boolean isPowered() {
        return this.data.getFlag(POWERED);
    }

    @Override
    public void setPowered(boolean powered) {
        CreeperPowerEvent.PowerCause cause = powered ? CreeperPowerEvent.PowerCause.SET_ON
                : CreeperPowerEvent.PowerCause.SET_OFF;
        this.changePower(new CreeperPowerEvent(this, cause), powered);
    }

    private void changePower(CreeperPowerEvent event, boolean powered) {
        this.getServer().getEventManager().fire(event);
        if (!event.isCancelled() && !this.isClosed() && this.isPowered() != powered) {
            this.data.setFlag(POWERED, powered);
            this.markChunkDirty();
        }
    }

    @Override
    public void onStruckByLightning(LightningBolt lightningBolt) {
        super.onStruckByLightning(lightningBolt);
        this.changePower(new CreeperPowerEvent(this, lightningBolt, CreeperPowerEvent.PowerCause.LIGHTNING), true);
    }

    @Override
    protected String getDefaultName() {
        return "Creeper";
    }

    @Override
    public ItemStack[] getDrops() {
        if (this.lastDamageCause != null && this.lastDamageCause.getDamageSource().getCausingEntity() != null) {
            return new ItemStack[]{ItemStack.builder()
                    .itemType(ItemTypes.GUNPOWDER)
                    .amount(ThreadLocalRandom.current().nextInt(2) + 1)
                    .build()};
        }
        return new ItemStack[0];
    }
}
