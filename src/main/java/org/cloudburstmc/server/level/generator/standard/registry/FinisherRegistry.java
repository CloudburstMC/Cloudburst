package org.cloudburstmc.server.level.generator.standard.registry;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.api.event.Event;
import org.cloudburstmc.server.level.generator.standard.finish.Finisher;
import org.cloudburstmc.server.level.generator.standard.finish.IceSnowFinisher;
import org.cloudburstmc.server.level.generator.standard.generation.decorator.Decorator;
import org.cloudburstmc.server.level.generator.standard.misc.NextGenerationPass;
import org.cloudburstmc.server.level.generator.standard.population.Populator;

/**
 * Registry for {@link Populator}.
 *
 * @author DaPorkchop_
 * @see StandardGeneratorRegistries#populator()
 */
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public final class FinisherRegistry extends AbstractGeneratorRegistry<Finisher> {
    @Override
    protected void registerDefault() {
        //register all decorators as populators as well, unless they have the @SkipRegistrationAsPopulator annotation
        StandardGeneratorRegistries.populator().idToValues.forEach((id, clazz) -> {
            if (clazz.getAnnotation(Populator.SkipRegistrationAsFinisher.class) == null || clazz.getAnnotation(Decorator.SkipRegistrationAsPopulator.class) == null) {
                this.register(id, clazz);
            }
        });

        this.register(IceSnowFinisher.ID, IceSnowFinisher.class);

        this.register(NextGenerationPass.ID, NextGenerationPass.class);
    }

    @Override
    protected Event constructionEvent() {
        return new ConstructionEvent(this);
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class ConstructionEvent extends Event {

        @NonNull
        private final FinisherRegistry registry;

        public FinisherRegistry getRegistry() {
            return this.registry;
        }
    }
}
