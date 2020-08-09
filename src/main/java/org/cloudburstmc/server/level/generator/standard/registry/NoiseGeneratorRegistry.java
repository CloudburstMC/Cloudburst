package org.cloudburstmc.server.level.generator.standard.registry;

import lombok.*;
import org.cloudburstmc.server.event.Event;
import org.cloudburstmc.server.event.HandlerList;
import org.cloudburstmc.server.level.generator.standard.generation.noise.*;

/**
 * Registry for {@link NoiseGenerator}.
 *
 * @author DaPorkchop_
 * @see StandardGeneratorRegistries#noiseGenerator()
 */
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public final class NoiseGeneratorRegistry extends AbstractGeneratorRegistry<NoiseGenerator> {
    @Override
    protected void registerDefault() {
        this.register(OpenSimplexDefaultNoiseGenerator.ID, OpenSimplexDefaultNoiseGenerator.class);
        this.register(PerlinDefaultNoiseGenerator.ID, PerlinDefaultNoiseGenerator.class);
        this.register(PorkianDefaultNoiseGenerator.ID, PorkianDefaultNoiseGenerator.class);
        this.register(SimplexDefaultNoiseGenerator.ID, SimplexDefaultNoiseGenerator.class);
    }

    @Override
    protected Event constructionEvent() {
        return new ConstructionEvent(this);
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class ConstructionEvent extends Event {
        @Getter
        private static HandlerList handlers = new HandlerList();

        @NonNull
        private final NoiseGeneratorRegistry registry;

        public NoiseGeneratorRegistry getRegistry() {
            return this.registry;
        }
    }
}
