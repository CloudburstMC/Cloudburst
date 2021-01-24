package org.cloudburstmc.server.world;

import com.nukkitx.math.vector.Vector3f;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.server.Server;

@Log4j2
public enum EnumWorld {
    OVERWORLD,
    NETHER,
    //THE_END
    ;

    World world;

    public World getWorld() {
        return world;
    }

    public static void initWorlds() {
        OVERWORLD.world = Server.getInstance().getDefaultWorld();

        World netherWorld = Server.getInstance().getWorldByName("nether");
        // attempt to load the nether world if it is allowed in server properties
        if (netherWorld != null && Server.getInstance().isNetherAllowed()) {
            NETHER.world = netherWorld;
        } else {
            // Nether is not found or disabled
            log.warn("No world called \"nether\" found or nether is disabled in server properties! Nether functionality will be disabled.");
        }
    }

    public static World getOtherNetherPair(World current) {
        if (current == OVERWORLD.world) {
            return NETHER.world;
        } else if (current == NETHER.world) {
            return OVERWORLD.world;
        } else {
            throw new IllegalArgumentException("Neither overworld nor nether given!");
        }
    }

    public static Location moveToNether(Location current) {
        if (NETHER.world == null) {
            return null;
        } else {
            int x, y, z;
            World world;
            if (current.getWorld() == OVERWORLD.world) {
                x = mRound(current.getFloorX() >> 3, 128);
                y = mRound(current.getFloorY(), 32);
                z = mRound(current.getFloorZ() >> 3, 128);
                world = NETHER.world;
            } else if (current.getWorld() == NETHER.world) {
                x = mRound(current.getFloorX() << 3, 1024);
                y = mRound(current.getFloorY(), 32);
                z = mRound(current.getFloorZ() << 3, 1024);
                world = OVERWORLD.world;
            } else {
                throw new IllegalArgumentException("Neither overworld nor nether given!");
            }
            return Location.from(Vector3f.from(x, y, z), current.getYaw(), current.getPitch(), world);
        }
    }

    private static final int mRound(int value, int factor) {
        return Math.round((float) value / factor) * factor;
    }
}
