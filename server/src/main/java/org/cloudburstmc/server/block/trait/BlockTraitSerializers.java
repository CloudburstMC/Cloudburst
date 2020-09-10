package org.cloudburstmc.server.block.trait;

import com.nukkitx.nbt.NbtMapBuilder;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import lombok.experimental.UtilityClass;
import lombok.val;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.block.BlockType;
import org.cloudburstmc.server.block.trait.serializer.*;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.math.Direction.Axis;
import org.cloudburstmc.server.utils.data.*;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.Objects;

@UtilityClass
@ParametersAreNonnullByDefault
public class BlockTraitSerializers {

    private final Reference2ObjectMap<Class<? extends Comparable<?>>, TraitSerializer<?>> serializers = new Reference2ObjectOpenHashMap<>();
    private final Reference2ObjectMap<BlockTrait<?>, TraitSerializer<?>> traitSerializers = new Reference2ObjectOpenHashMap<>();

    public void register(Class<? extends Comparable<?>> clazz, TraitSerializer<?> serializer) {
        Objects.requireNonNull(clazz);
        Objects.requireNonNull(serializer);
        serializers.put(clazz, serializer);
    }

    public void register(BlockTrait<?> trait, TraitSerializer<?> serializer) {
        Objects.requireNonNull(trait);
        Objects.requireNonNull(serializer);
        traitSerializers.put(trait, serializer);
    }

    @SuppressWarnings("ConstantConditions")
    public void serialize(NbtMapBuilder builder, BlockType type, Map<BlockTrait<?>, Comparable<?>> traits, BlockTrait<?> trait) {
        serialize(builder, type, traits, trait, traits.get(trait));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void serialize(NbtMapBuilder builder, BlockType type, Map<BlockTrait<?>, Comparable<?>> traits, BlockTrait<?> trait, Comparable<?> value) {
        TraitSerializer serializer = getSerializerFor(trait);

        String traitName = null;
        if (serializer != null) {
            val v = serializer.serialize(builder, type, traits, value);
            if (v != null) {
                value = v;
            }

            traitName = serializer.getName(type, traits, trait);
        }

        if (value instanceof Enum<?>) {
            value = ((Enum<?>) value).name().toLowerCase();
        }

        if (traitName == null) {
            traitName = trait.getVanillaName();
        }

        builder.put(traitName, value);
    }

    @SuppressWarnings("rawtypes")
    public TraitSerializer getSerializerFor(BlockTrait<?> trait) {
        TraitSerializer serializer = traitSerializers.get(trait);
        ;

        if (serializer == null) {
            serializer = serializers.get(trait.getValueClass());
        }

        return serializer;
    }

    public void init() {
        register(Direction.class, new DirectionSerializer());
        register(TreeSpecies.class, new TreeSpeciesSerializer());
        register(StoneSlabType.class, new StoneSlabSerializer());
        register(SeaGrassType.class, new SeagrassSerializer());
        register(CardinalDirection.class, new EnumOrdinalSerializer<CardinalDirection>());
        register(RailDirection.class, new EnumOrdinalSerializer<RailDirection>());
        register(DyeColor.class, new DyeColorSerializer());
        register(SandStoneType.class, new SandstoneTypeSerializer());
        register(FluidType.class, new FluidTypeSerializer());
        register(Axis.class, new AxisSerializer());
        register(BlockTraits.TORCH_DIRECTION, new TorchDirectionSerializer());
        register(BlockTraits.IS_POWERED, new PoweredSerializer());
        register(BlockTraits.IS_TRIGGERED, new TriggeredSerializer());
    }

    public interface TraitSerializer<T extends Comparable<T>> {

        default String getName(BlockType type, Map<BlockTrait<?>, Comparable<?>> traits, BlockTrait<?> blockTrait) {
            return blockTrait.getVanillaName();
        }

        default Comparable<?> serialize(NbtMapBuilder builder, BlockType type, Map<BlockTrait<?>, Comparable<?>> traits, T t) {
            return null;
        }
    }
}
