package org.cloudburstmc.server.level.generator.standard.registry;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.api.registry.RegistryException;
import org.cloudburstmc.server.event.Event;
import org.cloudburstmc.server.level.generator.standard.generation.decorator.*;
import org.cloudburstmc.server.level.generator.standard.misc.NextGenerationPass;

/**
 * Registry for {@link Decorator}.
 *
 * @author DaPorkchop_
 * @see StandardGeneratorRegistries#decorator()
 */
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public final class DecoratorRegistry extends AbstractGeneratorRegistry<Decorator> {
    @Override
    protected void registerDefault() {
        this.register(BedrockDecorator.ID, BedrockDecorator.class);
        this.register(DeepSurfaceDecorator.ID, DeepSurfaceDecorator.class);
        this.register(DistanceSelectionDecorator.ID, DistanceSelectionDecorator.class);
        this.register(GroundCoverDecorator.ID, GroundCoverDecorator.class);
        this.register(HeightSelectionDecorator.ID, HeightSelectionDecorator.class);
        this.register(MesaSurfaceDecorator.ID, MesaSurfaceDecorator.class);
        this.register(NoiseSelectionDecorator.ID, NoiseSelectionDecorator.class);
        this.register(ReplaceTopDecorator.ID, ReplaceTopDecorator.class);
        this.register(ScatteredCoverDecorator.ID, ScatteredCoverDecorator.class);
        this.register(SurfaceDecorator.ID, SurfaceDecorator.class);

        this.register(NextGenerationPass.ID, NextGenerationPass.class);
    }

    @Override
    public void close() throws RegistryException {
        super.close();
    }

    @Override
    protected Event constructionEvent() {
        return new ConstructionEvent(this);
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class ConstructionEvent extends Event {

        @NonNull
        private final DecoratorRegistry registry;

        public DecoratorRegistry getRegistry() {
            return this.registry;
        }
    }
}
