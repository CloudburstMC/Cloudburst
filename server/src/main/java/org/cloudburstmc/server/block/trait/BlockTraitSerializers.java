package org.cloudburstmc.server.block.trait;

import com.nukkitx.nbt.NbtMapBuilder;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import lombok.experimental.UtilityClass;
import net.daporkchop.lib.common.function.plain.TriFunction;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.serializer.DirectionHelper;
import org.cloudburstmc.server.block.trait.serializer.VineDirectionSerializer;
import org.cloudburstmc.server.block.trait.serializer.WoodTypeSerializer;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.utils.data.VineDirection;
import org.cloudburstmc.server.utils.data.WoodType;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;
import java.util.function.BiFunction;

@UtilityClass
@ParametersAreNonnullByDefault
public class BlockTraitSerializers {

    private final Reference2ObjectMap<Class<? extends Comparable<?>>, BlockTraitSerializer<?>> serializers = new Reference2ObjectOpenHashMap<>();
    private final Reference2ObjectMap<Class<? extends Comparable<?>>, TraitNameSerializer> nameSerializers = new Reference2ObjectOpenHashMap<>();

    public void init() {
        register(Direction.class, (builder, state, value) -> DirectionHelper.serialize(builder, state));
        register(VineDirection.class, new VineDirectionSerializer());

        registerName(WoodType.class, new WoodTypeSerializer());
    }

    public void register(Class<? extends Comparable<?>> clazz, BlockTraitSerializer<?> serializer) {
        Objects.requireNonNull(clazz);
        Objects.requireNonNull(serializer);
        serializers.put(clazz, serializer);
    }

    public void registerName(Class<? extends Comparable<?>> clazz, TraitNameSerializer serializer) {
        Objects.requireNonNull(clazz);
        Objects.requireNonNull(serializer);
        nameSerializers.put(clazz, serializer);
    }

    @SuppressWarnings("ConstantConditions")
    public void serialize(NbtMapBuilder builder, BlockState state, BlockTrait<?> trait) {
        serialize(builder, state, trait, state.getTrait(trait));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void serialize(NbtMapBuilder builder, BlockState state, BlockTrait<?> trait, Comparable<?> value) {
        BlockTraitSerializer serializer = serializers.get(trait.getValueClass());
        TraitNameSerializer nameSerializer = nameSerializers.get(trait.getValueClass());

        if (serializer != null) {
            value = serializer.apply(builder, state, value);
        }

        if (value instanceof Enum<?>) {
            value = ((Enum<?>) value).name().toLowerCase();
        }

        String traitName = null;

        if (nameSerializer != null) {
            traitName = nameSerializer.apply(state, trait);
        }

        if (traitName == null) {
            traitName = trait.getVanillaName();
        }

        builder.put(traitName, value);
    }

    @FunctionalInterface
    public interface BlockTraitSerializer<T extends Comparable<T>> extends TriFunction<NbtMapBuilder, BlockState, T, Comparable<?>> {

        @Override
        Comparable<?> apply(NbtMapBuilder builder, BlockState state, T t);
    }

    public interface TraitNameSerializer extends BiFunction<BlockState, BlockTrait<?>, String> {

        @Override
        String apply(BlockState state, BlockTrait<?> blockTrait);
    }
}
