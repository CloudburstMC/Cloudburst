package org.cloudburstmc.server.container.screen;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.inventory.ScreenTypes;
import org.cloudburstmc.api.inventory.VirtualDoubleChestScreen;
import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.api.inventory.view.StorageView;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType;
import org.cloudburstmc.protocol.bedrock.packet.BlockEntityDataPacket;
import org.cloudburstmc.protocol.bedrock.packet.ContainerOpenPacket;
import org.cloudburstmc.protocol.bedrock.packet.UpdateBlockPacket;
import org.cloudburstmc.server.block.CloudBlock;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.container.mapping.SimpleContainerMapping;
import org.cloudburstmc.server.container.view.CloudVirtualStorageSection;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudBlockRegistry;

/**
 * A {@link VirtualDoubleChestScreen} implementation that presents a 54-slot double-chest GUI to a
 * player without any backing block entities in the world.
 */
public class CloudVirtualDoubleChestScreen extends CloudContainerScreen implements VirtualDoubleChestScreen {

    private static final int OPEN_DELAY_TICKS = 3;
    private static final int CLOSE_RESTORE_DELAY_TICKS = 2;
    private static final int DOUBLE_CHEST_SIZE = 54;

    private final CloudContainer backingContainer;
    private final CloudVirtualStorageSection storageSection;

    private String title;
    private Vector3i fakePosA;
    private Vector3i fakePosB;
    private BlockState[] originalStatesA;
    private BlockState[] originalStatesB;

    public CloudVirtualDoubleChestScreen(CloudPlayer player, String title) {
        super(ScreenTypes.VIRTUAL_DOUBLE_CHEST, player);
        this.title = title;
        this.backingContainer = new CloudContainer(DOUBLE_CHEST_SIZE);
        this.storageSection = new CloudVirtualStorageSection(SlotGroupTypes.VIRTUAL_DOUBLE_CHEST, backingContainer);
    }

    @Override
    public boolean isOpen() {
        return fakePosA != null;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public StorageView getStorage() {
        return getSlotsOrThrow(SlotGroupTypes.VIRTUAL_DOUBLE_CHEST);
    }

    @Override
    protected void setupMappings() {
        super.setupMappings();
        this.addMapping(new SimpleContainerMapping(ContainerSlotType.LEVEL_ENTITY, storageSection));
    }

    @Override
    public void open() {
        CloudLevel level = player.getLevel();

        Vector3f pos = player.getPosition();
        int fakeY = Math.min((int) pos.getY() + 2, level.getMaxHeight() - 1);
        fakePosA = Vector3i.from((int) pos.getX(), fakeY, (int) pos.getZ());
        fakePosB = Vector3i.from((int) pos.getX() + 1, fakeY, (int) pos.getZ());

        originalStatesA = new BlockState[]{
                level.getBlockState(fakePosA.getX(), fakePosA.getY(), fakePosA.getZ(), 0),
                level.getBlockState(fakePosA.getX(), fakePosA.getY(), fakePosA.getZ(), 1)
        };
        originalStatesB = new BlockState[]{
                level.getBlockState(fakePosB.getX(), fakePosB.getY(), fakePosB.getZ(), 0),
                level.getBlockState(fakePosB.getX(), fakePosB.getY(), fakePosB.getZ(), 1)
        };

        BlockState chestState = CloudBlockRegistry.REGISTRY.getBlock(BlockTypes.CHEST);
        BlockState airState = CloudBlockRegistry.REGISTRY.getBlock(BlockTypes.AIR);

        CloudBlock fakeBlockA = new CloudBlock(level, fakePosA, new BlockState[]{chestState, airState});
        CloudBlock fakeBlockB = new CloudBlock(level, fakePosB, new BlockState[]{chestState, airState});
        level.sendBlocks(
                new Player[]{player},
                new Block[]{fakeBlockA, fakeBlockB},
                UpdateBlockPacket.FLAG_ALL_PRIORITY
        );

        String customName = title != null ? title : "Chest";

        player.sendPacket(buildChestNbtPacket(fakePosA, customName, null));
        player.sendPacket(buildChestNbtPacket(fakePosB, customName, null));

        player.sendPacket(buildChestNbtPacket(fakePosA, customName, fakePosB));
        player.sendPacket(buildChestNbtPacket(fakePosB, customName, fakePosA));

        final Vector3i anchorPos = fakePosA;
        final byte windowId = player.assignContainerId(backingContainer);

        player.getServer().getGlobalScheduler().runDelayed(null, t -> {
            if (!player.isConnected()) {
                return;
            }
            ContainerOpenPacket openPkt = new ContainerOpenPacket();
            openPkt.setId(windowId);
            openPkt.setType(ContainerType.CONTAINER);
            openPkt.setBlockPosition(anchorPos);
            player.sendPacket(openPkt);
            player.getInventoryManager().sendAllInventories();
        }, OPEN_DELAY_TICKS);
    }

    @Override
    public void close() {
        if (fakePosA == null) {
            return;
        }

        final Vector3i posA = fakePosA;
        final Vector3i posB = fakePosB;
        final BlockState[] statesA = originalStatesA;
        final BlockState[] statesB = originalStatesB;
        fakePosA = null;
        fakePosB = null;
        originalStatesA = null;
        originalStatesB = null;

        player.getServer().getGlobalScheduler().runDelayed(null, t -> {
            if (!player.isConnected()) return;
            CloudLevel level = player.getLevel();
            level.sendBlocks(
                    new Player[]{player},
                    new Block[]{new CloudBlock(level, posA, statesA)},
                    UpdateBlockPacket.FLAG_ALL_PRIORITY
            );
        }, CLOSE_RESTORE_DELAY_TICKS);

        player.getServer().getGlobalScheduler().runDelayed(null, t -> {
            if (!player.isConnected()) return;
            CloudLevel level = player.getLevel();
            level.sendBlocks(
                    new Player[]{player},
                    new Block[]{new CloudBlock(level, posB, statesB)},
                    UpdateBlockPacket.FLAG_ALL_PRIORITY
            );
        }, CLOSE_RESTORE_DELAY_TICKS + 1);
    }

    private BlockEntityDataPacket buildChestNbtPacket(Vector3i pos, String customName, Vector3i pairPos) {
        NbtMapBuilder nbt = NbtMap.builder()
                .putString("id", "Chest")
                .putInt("x", pos.getX())
                .putInt("y", pos.getY())
                .putInt("z", pos.getZ())
                .putString("CustomName", customName);

        if (pairPos != null) {
            nbt.putInt("pairx", pairPos.getX());
            nbt.putInt("pairz", pairPos.getZ());
        }

        BlockEntityDataPacket pkt = new BlockEntityDataPacket();
        pkt.setBlockPosition(pos);
        pkt.setData(nbt.build());
        return pkt;
    }
}
