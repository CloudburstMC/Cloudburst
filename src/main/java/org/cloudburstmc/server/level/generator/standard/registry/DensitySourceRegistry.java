package org.cloudburstmc.server.level.generator.standard.registry;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.lib.noise.NoiseSource;
import org.cloudburstmc.server.event.Event;
import org.cloudburstmc.server.level.generator.standard.generation.density.DensitySource;
import org.cloudburstmc.server.level.generator.standard.generation.density.EndDensitySource;
import org.cloudburstmc.server.level.generator.standard.generation.density.NetherDensitySource;
import org.cloudburstmc.server.level.generator.standard.generation.density.VanillaDensitySource;

/**
 * Registry for {@link NoiseSource}.
 *
 * @author DaPorkchop_
 * @see StandardGeneratorRegistries#noiseGenerator()
 */
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public final class DensitySourceRegistry extends AbstractGeneratorRegistry<DensitySource> {
    @Override
    protected void registerDefault() {
        this.register(EndDensitySource.ID, EndDensitySource.class);
        this.register(NetherDensitySource.ID, NetherDensitySource.class);
        this.register(VanillaDensitySource.ID, VanillaDensitySource.class);
    }

    @Override
    protected Event constructionEvent() {
        return new ConstructionEvent(this);
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class ConstructionEvent extends Event {

        @NonNull
        private final DensitySourceRegistry registry;

        public DensitySourceRegistry getRegistry() {
            return this.registry;
        }
    }
}
