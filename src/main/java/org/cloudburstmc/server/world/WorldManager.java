package org.cloudburstmc.server.world;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.server.Server;
import org.cloudburstmc.server.event.level.WorldLoadEvent;
import org.cloudburstmc.server.event.level.WorldUnloadEvent;
import org.cloudburstmc.server.math.NukkitMath;
import org.cloudburstmc.server.utils.Utils;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.Closeable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Log4j2
@Singleton
public class WorldManager implements Closeable {
    private final ExecutorService chunkExecutor = Executors.newWorkStealingPool();
    private final Server server;
    private final Set<World> worlds = new HashSet<>();
    private final Map<String, World> worldIds = new HashMap<>();
    private volatile World defaultWorld;

    @Inject
    public WorldManager(Server server) {
        this.server = server;
    }

    public synchronized void register(World world) {
        Preconditions.checkNotNull(world, "world");
        Preconditions.checkArgument(world.getServer() == this.server, "World did not come from this server");
        Preconditions.checkArgument(!worlds.contains(world), "world already registered");

        WorldLoadEvent event = new WorldLoadEvent(world);
        this.server.getEventManager().fire(event);

        this.worlds.add(world);
        this.worldIds.put(world.getId(), world);
    }

    public boolean deregister(World world) {
        return deregister(world, false);
    }

    public synchronized boolean deregister(World world, boolean force) {
        Preconditions.checkNotNull(world, "world");
        Preconditions.checkArgument(worlds.contains(world), "world not registered");

        WorldUnloadEvent event = new WorldUnloadEvent(world);
        this.server.getEventManager().fire(event);
        if (event.isCancelled() && !force) {
            return false;
        } else {
            this.worldIds.remove(world.getId());
            return worlds.remove(world);
        }
    }

    @Nullable
    public synchronized World getWorld(String id) {
        return this.worldIds.get(id);
    }

    @Nullable
    public synchronized World getWorldByName(String name) {
        for (World world : this.worlds) {
            if (world.getName().equals(name)) {
                return world;
            }
        }
        return null;
    }

    public World getDefaultWorld() {
        return defaultWorld;
    }

    public synchronized void setDefaultWorld(World world) {
        Preconditions.checkNotNull(world, "world");
        Preconditions.checkArgument(worlds.contains(world), "world not registered");

        this.defaultWorld = world;
    }

    public synchronized Set<World> getWorlds() {
        return ImmutableSet.copyOf(worlds);
    }

    public synchronized void save() {
        this.worlds.forEach(World::save);
    }

    @Override
    public synchronized void close() {
        for (World world : this.worlds) {
            world.close();
        }
    }

    public void tick(int currentTick) {
        for (World world : this.worlds) {
            try {
                long levelTime = System.currentTimeMillis();
                world.doTick(currentTick);
                int tickMs = (int) (System.currentTimeMillis() - levelTime);
                world.tickRateTime = tickMs;

                if (server.isAutoTickRate()) {
                    if (tickMs < 50 && world.getTickRate() > server.getBaseTickRate()) {
                        int r;
                        world.setTickRate(r = world.getTickRate() - 1);
                        if (r > server.getBaseTickRate()) {
                            world.tickRateCounter = world.getTickRate();
                        }
                        log.debug("Raising world \"" + world.getName() + "\" tick rate to " + world.getTickRate() + " ticks");
                    } else if (tickMs >= 50) {
                        if (world.getTickRate() == server.getBaseTickRate()) {
                            world.setTickRate(Math.max(server.getBaseTickRate() + 1, Math.min(server.getAutoTickRateLimit(), tickMs / 50)));
                            log.debug("World \"" + world.getName() + "\" took " + NukkitMath.round(tickMs, 2) + "ms, setting tick rate to " + world.getTickRate() + " ticks");
                        } else if ((tickMs / world.getTickRate()) >= 50 && world.getTickRate() < server.getAutoTickRateLimit()) {
                            world.setTickRate(world.getTickRate() + 1);
                            log.debug("World \"" + world.getName() + "\" took " + NukkitMath.round(tickMs, 2) + "ms, setting tick rate to " + world.getTickRate() + " ticks");
                        }
                        world.tickRateCounter = world.getTickRate();
                    }
                }

                if (currentTick % 100 == 0) {
                    world.doChunkGarbageCollection();
                }
            } catch (Exception e) {
                log.error(server.getLanguage().translate("cloudburst.world.tickError", world.getId(), Utils.getExceptionMessage(e)));
            }
        }
    }

    public ExecutorService getChunkExecutor() {
        return chunkExecutor;
    }
}
