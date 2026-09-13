package org.cloudburstmc.server.item.food;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.event.player.PlayerTeleportCause;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.BoundingBox;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.block.util.BlockSupport;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.particle.PortalParticle;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.ThreadLocalRandom;

public final class FoodChorusFruit extends FoodNormal {

    private static final int TELEPORT_ATTEMPTS = 16;
    private static final double TELEPORT_DIAMETER = 16;

    public FoodChorusFruit() {
        super(4, 2.4F);
        setMetadata(ItemTypes.CHORUS_FRUIT.getId());
    }

    @Override
    public boolean onEatenBy(@NonNull Player player) {
        super.onEatenBy(player);

        if (player.getVehicle() != null && !player.dismount(player.getVehicle())) {
            return true;
        }

        CloudLevel level = (CloudLevel) player.getLevel();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int attempt = 0; attempt < TELEPORT_ATTEMPTS; attempt++) {
            double x = player.getPosition().getX() + (random.nextDouble() - 0.5) * TELEPORT_DIAMETER;
            double y = Math.clamp(player.getPosition().getY() + (random.nextDouble() - 0.5) * TELEPORT_DIAMETER, level.getMinHeight(), level.getMaxHeight() - 1);
            double z = player.getPosition().getZ() + (random.nextDouble() - 0.5) * TELEPORT_DIAMETER;
            Vector3f destination = findDestination(player, level, x, y, z);
            if (destination == null) {
                continue;
            }

            Vector3f source = player.getPosition();
            if (!player.teleport(destination, PlayerTeleportCause.CHORUS_FRUIT)) {
                continue;
            }

            player.resetFallDistance();
            addTeleportParticles(level, source, player.getPosition());
            level.addLevelSoundEvent(player.getPosition(), SoundEvent.TELEPORT);
            break;
        }

        return true;
    }

    private static Vector3f findDestination(Player player, CloudLevel level, double x, double y, double z) {
        Vector3i blockPosition = Vector3i.from((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
        if (!level.isChunkLoaded(blockPosition)) {
            return null;
        }

        while (blockPosition.getY() > level.getMinHeight()) {
            BlockState below = level.getBlockState(blockPosition.sub(0, 1, 0));
            if (BlockSupport.blocksMotion(below)) {
                break;
            }

            y--;
            blockPosition = blockPosition.sub(0, 1, 0);
        }

        if (blockPosition.getY() <= level.getMinHeight()) {
            return null;
        }

        Vector3f destination = Vector3f.from(x, y, z);
        Vector3f movement = destination.sub(player.getPosition());
        BoundingBox destinationBounds = player.getBoundingBox().move(movement);
        if (level.hasCollision(player, destinationBounds, false) || containsLiquid(level, destinationBounds)) {
            return null;
        }

        return destination;
    }

    private static boolean containsLiquid(CloudLevel level, BoundingBox bounds) {
        int minX = (int) Math.floor(bounds.getMinX());
        int minY = (int) Math.floor(bounds.getMinY());
        int minZ = (int) Math.floor(bounds.getMinZ());

        int maxX = (int) Math.floor(Math.nextDown(bounds.getMaxX()));
        int maxY = (int) Math.floor(Math.nextDown(bounds.getMaxY()));
        int maxZ = (int) Math.floor(Math.nextDown(bounds.getMaxZ()));

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (!level.getLiquidState(Vector3i.from(x, y, z)).isEmpty()) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static void addTeleportParticles(CloudLevel level, Vector3f source, Vector3f destination) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 32; i++) {
            float progress = random.nextFloat();
            Vector3f position = source.add(destination.sub(source).mul(progress)).add(
                    random.nextGaussian() * 0.2,
                    random.nextDouble() * 2,
                    random.nextGaussian() * 0.2
            );
            level.addParticle(new PortalParticle(position));
        }
    }
}
