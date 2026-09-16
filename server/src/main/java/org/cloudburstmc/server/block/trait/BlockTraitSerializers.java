package org.cloudburstmc.server.block.trait;

import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.block.trait.BlockTrait;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.Direction.Axis;
import org.cloudburstmc.api.util.data.RailDirection;
import org.cloudburstmc.api.util.data.SeaGrassType;
import org.cloudburstmc.api.util.data.SlabSlot;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.server.block.trait.serializer.*;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.Objects;

@UtilityClass
@ParametersAreNonnullByDefault
public class BlockTraitSerializers {

    private final Reference2ObjectMap<Class<? extends Comparable<?>>, TraitSerializer<?>> serializers = new Reference2ObjectOpenHashMap<>();
    private final Reference2ObjectMap<BlockTrait<?>, TraitSerializer<?>> traitSerializers = new Reference2ObjectOpenHashMap<>();

    public void init() {
        register(Direction.class, new DirectionSerializer());
        register(SeaGrassType.class, new SeagrassSerializer());
        register(RailDirection.class, new EnumOrdinalSerializer<RailDirection>());

        register(Axis.class, new AxisSerializer());
        register(SlabSlot.class, new SlabSlotSerializer());
        register(BlockTraits.CARDINAL_DIRECTION, new CardinalDirectionSerializer());
        register(BlockTraits.SIGN_DIRECTION, new SignDirectionSerializer());
        register(BlockTraits.TORCH_DIRECTION, new TorchDirectionSerializer());
        register(BlockTraits.IS_POWERED, new PoweredSerializer());
        register(BlockTraits.BLOCK_FACE, NoopTraitSerializer.instance());
    }

    public <T extends Comparable<T>> void serialize(NbtMapBuilder builder, BlockType type, Map<BlockTrait<?>, Comparable<?>> traits, BlockTrait<T> trait) {
        T value = trait.getValueClass().cast(traits.get(trait));
        TraitSerializer<T> serializer = getSerializerFor(trait);
        String traitName = serializer == null ? trait.getVanillaName() : serializer.getName(type, traits, trait);
        Comparable<?> serializedValue = serializer == null ? value : serializer.serialize(type, traits, trait, value);

        if (serializedValue instanceof Enum<?> enumValue) {
            serializedValue = enumValue.name().toLowerCase();
        }

        builder.put(traitName, serializedValue);
    }

    private <T extends Comparable<T>> void register(Class<T> clazz, TraitSerializer<T> serializer) {
        Objects.requireNonNull(clazz);
        Objects.requireNonNull(serializer);
        serializers.put(clazz, serializer);
    }

    private <T extends Comparable<T>> void register(BlockTrait<T> trait, TraitSerializer<T> serializer) {
        Objects.requireNonNull(trait);
        Objects.requireNonNull(serializer);
        traitSerializers.put(trait, serializer);
    }

    @SuppressWarnings("unchecked")
    private <T extends Comparable<T>> TraitSerializer<T> getSerializerFor(BlockTrait<T> trait) {
        TraitSerializer<?> serializer = traitSerializers.get(trait);

        if (serializer == null) {
            serializer = serializers.get(trait.getValueClass());
        }

        return (TraitSerializer<T>) serializer;
    }

    public interface TraitSerializer<T extends Comparable<T>> {

        default String getName(BlockType type, Map<BlockTrait<?>, Comparable<?>> traits, BlockTrait<T> blockTrait) {
            return blockTrait.getVanillaName();
        }

        default Comparable<?> serialize(BlockType type, Map<BlockTrait<?>, Comparable<?>> traits, BlockTrait<T> trait, T value) {
            return value;
        }
    }
}
