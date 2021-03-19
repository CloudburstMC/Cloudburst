package org.cloudburstmc.server.level.particle;

import com.google.common.base.Strings;
import com.nukkitx.protocol.bedrock.BedrockPacket;
import com.nukkitx.protocol.bedrock.data.PlayerPermission;
import com.nukkitx.protocol.bedrock.data.command.CommandPermission;
import com.nukkitx.protocol.bedrock.data.entity.EntityDataMap;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlags;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import com.nukkitx.protocol.bedrock.data.skin.ImageData;
import com.nukkitx.protocol.bedrock.data.skin.SerializedSkin;
import com.nukkitx.protocol.bedrock.packet.AddPlayerPacket;
import com.nukkitx.protocol.bedrock.packet.PlayerListPacket;
import com.nukkitx.protocol.bedrock.packet.RemoveEntityPacket;
import com.nukkitx.protocol.bedrock.packet.SetEntityDataPacket;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.server.level.CloudLevel;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static com.nukkitx.protocol.bedrock.data.entity.EntityData.*;

/**
 * Created on 2015/11/21 by xtypr.
 * Package cn.nukkit.level.particle in project Nukkit .
 */
public class FloatingTextParticle extends Particle {
    private static final SerializedSkin EMPTY_SKIN;
    private static final ImageData SKIN_DATA = ImageData.of(new byte[8192]);

    static {
        EMPTY_SKIN = SerializedSkin.builder()
                .skinId("FloatingText")
                .skinData(SKIN_DATA).build();
    }

    protected UUID uuid = UUID.randomUUID();
    protected final CloudLevel level;
    protected long entityId = -1;
    protected boolean invisible = false;
    protected EntityDataMap dataMap = new EntityDataMap();

    public FloatingTextParticle(Vector3f pos, String title) {
        this(pos, title, null);
    }

    public FloatingTextParticle(CloudLevel level, Vector3f pos, String title) {
        this(level, pos, title, null);
    }

    public FloatingTextParticle(Vector3f pos, String title, String text) {
        this(null, pos, title, text);
    }

    public FloatingTextParticle(CloudLevel level, Vector3f pos, String title, String text) {
        super(pos);
        this.level = level;

        EntityFlags flags = new EntityFlags();
        flags.setFlag(EntityFlag.NO_AI, true);
        dataMap.putFlags(flags)
                .putLong(LEASH_HOLDER_EID, -1)
                .putFloat(SCALE, 0.01f) //zero causes problems on debug builds?
                .putFloat(BOUNDING_BOX_HEIGHT, 0.01f)
                .putFloat(BOUNDING_BOX_WIDTH, 0.01f);
        if (!Strings.isNullOrEmpty(title)) {
            dataMap.putString(NAMETAG, title);
        }
        if (!Strings.isNullOrEmpty(text)) {
            dataMap.putString(SCORE_TAG, text);
        }
    }

    public String getText() {
        return dataMap.getString(SCORE_TAG);
    }

    public void setText(String text) {
        this.dataMap.putString(SCORE_TAG, text);
        sendMetadata();
    }

    public String getTitle() {
        return dataMap.getString(NAMETAG);
    }

    public void setTitle(String title) {
        this.dataMap.putString(NAMETAG, title);
        sendMetadata();
    }

    private void sendMetadata() {
        if (this.level != null) {
            SetEntityDataPacket packet = new SetEntityDataPacket();
            packet.setRuntimeEntityId(this.entityId);
            packet.getMetadata().putAll(this.dataMap);
            this.level.addChunkPacket(this.getPosition(), packet);
        }
    }

    public boolean isInvisible() {
        return invisible;
    }

    public void setInvisible(boolean invisible) {
        this.invisible = invisible;
    }

    public void setInvisible() {
        this.setInvisible(true);
    }

    public long getEntityId() {
        return entityId;
    }

    @Override
    public BedrockPacket[] encode() {
        ArrayList<BedrockPacket> packets = new ArrayList<>();

        if (this.entityId == -1) {
            this.entityId = 1095216660480L + ThreadLocalRandom.current().nextLong(0, 0x7fffffffL);
        } else {
            RemoveEntityPacket packet = new RemoveEntityPacket();
            packet.setUniqueEntityId(this.entityId);

            packets.add(packet);
        }

        if (!this.invisible) {
            PlayerListPacket.Entry entry = new PlayerListPacket.Entry(uuid);
            entry.setEntityId(entityId);
            entry.setName(dataMap.getString(NAMETAG));
            entry.setSkin(EMPTY_SKIN);
            entry.setXuid("");
            entry.setPlatformChatId("");
            PlayerListPacket playerAdd = new PlayerListPacket();
            playerAdd.getEntries().add(entry);
            playerAdd.setAction(PlayerListPacket.Action.ADD);
            packets.add(playerAdd);

            AddPlayerPacket packet = new AddPlayerPacket();
            packet.setUuid(uuid);
            packet.setUsername("");
            packet.setUniqueEntityId(this.entityId);
            packet.setRuntimeEntityId(this.entityId);
            packet.setPosition(this.getPosition());
            packet.setMotion(org.cloudburstmc.math.vector.Vector3f.ZERO);
            packet.setRotation(org.cloudburstmc.math.vector.Vector3f.ZERO);
            packet.getMetadata().putAll(this.dataMap);
            packet.setHand(ItemData.AIR);
            packet.setPlatformChatId("");
            packet.setDeviceId("");
            packet.getAdventureSettings().setCommandPermission(CommandPermission.NORMAL);
            packet.getAdventureSettings().setPlayerPermission(PlayerPermission.MEMBER);
            packets.add(packet);

            PlayerListPacket playerRemove = new PlayerListPacket();
            playerRemove.getEntries().add(entry);
            playerRemove.setAction(PlayerListPacket.Action.REMOVE);
            packets.add(playerRemove);
        }

        return packets.toArray(new BedrockPacket[0]);
    }
}
