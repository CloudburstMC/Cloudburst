package org.cloudburstmc.server.player.handler;

import io.netty.buffer.Unpooled;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.api.pack.Pack;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.data.ResourcePackType;
import org.cloudburstmc.protocol.bedrock.packet.*;
import org.cloudburstmc.protocol.common.PacketSignal;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.math.MathHelper;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.player.PlayerLoginData;

@Log4j2
public class ResourcePackPacketHandler implements BedrockPacketHandler {
    private static final int RESOURCE_PACK_CHUNK_SIZE = 8 * 1024; // 8KB
    private final BedrockServerSession session;
    private final CloudServer server;
    private final PlayerLoginData loginData;

    public ResourcePackPacketHandler(BedrockServerSession session, CloudServer server, PlayerLoginData loginData) {
        this.session = session;
        this.server = server;
        this.loginData = loginData;
    }

    @Override
    public PacketSignal handle(ResourcePackClientResponsePacket packet) {
        return switch (packet.getStatus()) {
            case REFUSED -> {
                session.disconnect("disconnectionScreen.noReason");
                yield PacketSignal.HANDLED;
            }
            case SEND_PACKS -> {
                for (String entry : packet.getPackIds()) {
                    Pack pack = this.server.getPackManager().getPackByIdVersion(entry);
                    if (pack == null) {
                        session.disconnect("disconnectionScreen.resourcePack");
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
                    session.sendPacket(dataInfoPacket);
                }
                yield PacketSignal.HANDLED;
            }
            case HAVE_ALL_PACKS -> {
                session.sendPacket(this.server.getPackManager().getPackStack());
                yield PacketSignal.HANDLED;
            }
            case COMPLETED -> {
                if (loginData.getPreLoginEventTask().isFinished()) {
                    try {
                        CloudPlayer player = loginData.initializePlayer();
//                        for (Consumer<Player> task : loginData.getLoginTasks()) {
//                            task.accept(player);
//                        }
                    } catch (Exception e) {
                        log.debug("Exception in Player initialization: {}", e.getMessage());
                        e.printStackTrace();
                    }

                } else {
                    loginData.setShouldLogin(true);
                }
                yield PacketSignal.HANDLED;
            }
            default -> PacketSignal.HANDLED;
        };
    }

    @Override
    public PacketSignal handle(ResourcePackChunkRequestPacket packet) {
        Pack resourcePack = this.server.getPackManager().getPackByIdVersion(packet.getPackId() + "_" + packet.getPackVersion());
        if (resourcePack == null) {
            session.disconnect("disconnectionScreen.resourcePack");
            return PacketSignal.HANDLED;
        }

        ResourcePackChunkDataPacket dataPacket = new ResourcePackChunkDataPacket();
        dataPacket.setPackId(packet.getPackId());
        dataPacket.setPackVersion(packet.getPackVersion());
        dataPacket.setChunkIndex(packet.getChunkIndex());
        dataPacket.setData(Unpooled.wrappedBuffer(resourcePack.getChunk(RESOURCE_PACK_CHUNK_SIZE * packet.getChunkIndex(), RESOURCE_PACK_CHUNK_SIZE)));
        dataPacket.setProgress((long) RESOURCE_PACK_CHUNK_SIZE * packet.getChunkIndex());
        session.sendPacket(dataPacket);
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(ClientCacheStatusPacket packet) {
        loginData.setClientCacheEnabled(packet.isSupported());
        return PacketSignal.HANDLED;
    }
}
