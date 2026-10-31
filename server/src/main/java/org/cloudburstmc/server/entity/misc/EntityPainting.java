package org.cloudburstmc.server.entity.misc;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.data.EntityData;
import org.cloudburstmc.api.entity.data.PaintingData;
import org.cloudburstmc.api.entity.misc.Painting;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.protocol.bedrock.packet.AddPaintingPacket;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.server.entity.HangingEntity;
import org.cloudburstmc.server.event.entity.CloudEntityDamageEvent;
import org.cloudburstmc.server.player.CloudPlayer;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class EntityPainting extends HangingEntity implements Painting {

    public final static Motive[] motives = Motive.values();

    public EntityPainting(EntityType<Painting> type, Location location) {
        super(type, location);
    }

    public static Motive getMotive(String name) {
        return Motive.from(name, Motive.KEBAB);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
    }

    @Override
    protected PaintingData data() {
        return (PaintingData) super.data();
    }

    @Override
    protected Class<? extends EntityData> minimumDataClass() {
        return PaintingData.class;
    }

    @Override
    public BedrockPacket createAddEntityPacket() {
        AddPaintingPacket addPainting = new AddPaintingPacket();
        addPainting.setUniqueEntityId(this.getUniqueId());
        addPainting.setRuntimeEntityId(this.getRuntimeId());
        addPainting.setPosition(this.getPosition());
        addPainting.setDirection(this.getDirection().getHorizontalIndex());
        addPainting.setMotive(this.getMotive().title);
        return addPainting;
    }

    @Override
    protected boolean applyDamage(CloudEntityDamageEvent source) {
        if (super.applyDamage(source)) {
            Entity damager = source.getDamageSource().getCausingEntity();
            if (damager instanceof CloudPlayer player) {
                if (player.isSurvival() && this.level.getGameRules().get(GameRules.DO_ENTITY_DROPS)) {
                    this.level.dropItem(this.getPosition(), ItemStack.builder().itemType(ItemTypes.PAINTING).build());
                }
            }
            this.close();
            return true;
        } else {
            return false;
        }
    }

    public Motive getMotive() {
        return this.data().getMotive();
    }

    public void setMotive(Motive motive) {
        this.data().setMotive(motive);
    }
}
