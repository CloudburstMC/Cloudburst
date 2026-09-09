package org.cloudburstmc.server.player.handler;

import io.netty.buffer.Unpooled;
import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.pack.Pack;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.data.DisconnectFailReason;
import org.cloudburstmc.protocol.bedrock.data.ResourcePackType;
import org.cloudburstmc.protocol.bedrock.packet.*;
import org.cloudburstmc.protocol.common.PacketSignal;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.math.MathHelper;
import org.cloudburstmc.server.player.PlayerLoginContext;

public class ResourcePackPacketHandler implements BedrockPacketHandler {
    private static final int RESOURCE_PACK_CHUNK_SIZE = 8 * 1024; // 8KB
    private final BedrockServerSession session;
    private final CloudServer server;
    private final PlayerLoginContext loginContext;

    public ResourcePackPacketHandler(BedrockServerSession session, CloudServer server, PlayerLoginContext loginContext) {
        this.session = session;
        this.server = server;
        this.loginContext = loginContext;
    }

    @Override
    public PacketSignal handle(ResourcePackClientResponsePacket packet) {
        return switch (packet.getStatus()) {
            case REFUSED -> {
                this.disconnect(DisconnectFailReason.NO_REASON, "disconnectionScreen.noReason");
                yield PacketSignal.HANDLED;
            }
            case SEND_PACKS -> {
                for (String entry : packet.getPackIds()) {
                    Pack pack = this.server.getPackManager().getPackByIdVersion(entry);
                    if (pack == null) {
                        this.disconnect(DisconnectFailReason.RESOURCE_PACK_PROBLEM, "disconnectionScreen.resourcePack");
                        yield PacketSignal.HANDLED;
                    }

                    ResourcePackDataInfoPacket dataInfoPacket = new ResourcePackDataInfoPacket();
                    dataInfoPacket.setPackId(pack.getId());
                    dataInfoPacket.setPackVersion(pack.getVersion().toString());
                    dataInfoPacket.setMaxChunkSize(RESOURCE_PACK_CHUNK_SIZE);
                    dataInfoPacket.setChunkCount(MathHelper.ceil(pack.getSize() / (float) RESOURCE_PACK_CHUNK_SIZE));
                    dataInfoPacket.setCompressedPackSize(pack.getSize());
                    dataInfoPacket.setHash(pack.getHash());
                    dataInfoPacket.setType(ResourcePackType.values()[pack.getType().ordinal()]);
                    this.session.sendPacket(dataInfoPacket);
                }
                yield PacketSignal.HANDLED;
            }
            case HAVE_ALL_PACKS -> {
                this.session.sendPacket(this.server.getPackManager().getPackStack());
                yield PacketSignal.HANDLED;
            }
            case COMPLETED -> {
                this.server.getGlobalScheduler().run(null, task -> this.loginContext.completeResourcePacks());
                yield PacketSignal.HANDLED;
            }
            default -> PacketSignal.HANDLED;
        };
    }

    @Override
    public PacketSignal handle(ResourcePackChunkRequestPacket packet) {
        Pack resourcePack = this.server.getPackManager().getPackByIdVersion(packet.getPackId() + "_" + packet.getPackVersion());
        if (resourcePack == null) {
            this.disconnect(DisconnectFailReason.RESOURCE_PACK_PROBLEM, "disconnectionScreen.resourcePack");
            return PacketSignal.HANDLED;
        }

        ResourcePackChunkDataPacket dataPacket = new ResourcePackChunkDataPacket();
        dataPacket.setPackId(packet.getPackId());
        dataPacket.setPackVersion(packet.getPackVersion());
        dataPacket.setChunkIndex(packet.getChunkIndex());
        dataPacket.setData(Unpooled.wrappedBuffer(resourcePack.getChunk(RESOURCE_PACK_CHUNK_SIZE * packet.getChunkIndex(), RESOURCE_PACK_CHUNK_SIZE)));
        dataPacket.setProgress((long) RESOURCE_PACK_CHUNK_SIZE * packet.getChunkIndex());
        this.session.sendPacket(dataPacket);
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(ClientCacheStatusPacket packet) {
        this.loginContext.setClientCacheEnabled(packet.isSupported());
        return PacketSignal.HANDLED;
    }

    private void disconnect(DisconnectFailReason reason, String translationKey) {
        this.loginContext.disconnect(reason, Component.text(this.server.getLanguage().translate(translationKey)));
    }
}
