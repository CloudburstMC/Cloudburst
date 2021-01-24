package org.cloudburstmc.server.world.generator.standard.registry;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.server.event.Event;
import org.cloudburstmc.server.world.generator.standard.biome.map.BiomeMap;
import org.cloudburstmc.server.world.generator.standard.biome.map.ConstantBiomeMap;
import org.cloudburstmc.server.world.generator.standard.biome.map.complex.ComplexBiomeMap;

/**
 * Registry for {@link BiomeMap}.
 *
 * @author DaPorkchop_
 * @see StandardGeneratorRegistries#biomeMap()
 */
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public final class BiomeMapRegistry extends AbstractGeneratorRegistry<BiomeMap> {
    @Override
    protected void registerDefault() {
        this.register(ComplexBiomeMap.ID, ComplexBiomeMap.class);
        this.register(ConstantBiomeMap.ID, ConstantBiomeMap.class);
    }

    @Override
    protected Event constructionEvent() {
        return new ConstructionEvent(this);
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class ConstructionEvent extends Event {

        @NonNull
        private final BiomeMapRegistry registry;

        public BiomeMapRegistry getRegistry() {
            return this.registry;
        }
    }
}
