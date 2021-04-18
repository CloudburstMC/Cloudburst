package org.cloudburstmc.server.level.generator.standard.registry;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.api.event.Event;
import org.cloudburstmc.server.level.generator.standard.generation.decorator.Decorator;
import org.cloudburstmc.server.level.generator.standard.misc.NextGenerationPass;
import org.cloudburstmc.server.level.generator.standard.population.*;
import org.cloudburstmc.server.level.generator.standard.population.cluster.OrePopulator;
import org.cloudburstmc.server.level.generator.standard.population.plant.DoublePlantPopulator;
import org.cloudburstmc.server.level.generator.standard.population.plant.PlantPopulator;
import org.cloudburstmc.server.level.generator.standard.population.plant.ShrubPopulator;
import org.cloudburstmc.server.level.generator.standard.population.tree.BushPopulator;
import org.cloudburstmc.server.level.generator.standard.population.tree.HugeTreePopulator;
import org.cloudburstmc.server.level.generator.standard.population.tree.TreePopulator;

/**
 * Registry for {@link Populator}.
 *
 * @author DaPorkchop_
 * @see StandardGeneratorRegistries#populator()
 */
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public final class PopulatorRegistry extends AbstractGeneratorRegistry<Populator> {
    @Override
    protected void registerDefault() {
        //register all decorators as populators as well, unless they have the @SkipRegistrationAsPopulator annotation
        StandardGeneratorRegistries.decorator().idToValues.forEach((id, clazz) -> {
            if (clazz.getAnnotation(Decorator.SkipRegistrationAsPopulator.class) == null) {
                this.register(id, clazz);
            }
        });

        this.register(BlobPopulator.ID, BlobPopulator.class);
        this.register(BushPopulator.ID, BushPopulator.class);
        this.register(CocoaPopulator.ID, CocoaPopulator.class);
        this.register(DistanceSelectionPopulator.ID, DistanceSelectionPopulator.class);
        this.register(DoublePlantPopulator.ID, DoublePlantPopulator.class);
        this.register(EndIslandPopulator.ID, EndIslandPopulator.class);
        this.register(GlowstonePopulator.ID, GlowstonePopulator.class);
        this.register(HugeTreePopulator.ID, HugeTreePopulator.class);
        this.register(LakePopulator.ID, LakePopulator.class);
        this.register(NoiseSelectionPopulator.ID, NoiseSelectionPopulator.class);
        this.register(OrePopulator.ID, OrePopulator.class);
        this.register(PlantPopulator.ID, PlantPopulator.class);
        this.register(ShrubPopulator.ID, ShrubPopulator.class);
        this.register(SpikesPopulator.ID, SpikesPopulator.class);
        this.register(SpringPopulator.ID, SpringPopulator.class);
        this.register(SubmergedOrePopulator.ID, SubmergedOrePopulator.class);
        this.register(TreePopulator.ID, TreePopulator.class);

        this.register(NextGenerationPass.ID, NextGenerationPass.class);
    }

    @Override
    protected Event constructionEvent() {
        return new ConstructionEvent(this);
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class ConstructionEvent extends Event {

        @NonNull
        private final PopulatorRegistry registry;

        public PopulatorRegistry getRegistry() {
            return this.registry;
        }
    }
}
