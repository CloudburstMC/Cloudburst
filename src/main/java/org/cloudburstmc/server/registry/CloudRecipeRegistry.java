package org.cloudburstmc.server.registry;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import io.netty.util.collection.CharObjectHashMap;
import io.netty.util.collection.CharObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import lombok.extern.log4j.Log4j2;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockIds;
import org.cloudburstmc.api.crafting.CraftingRecipe;
import org.cloudburstmc.api.crafting.MixRecipe;
import org.cloudburstmc.api.crafting.Recipe;
import org.cloudburstmc.api.crafting.RecipeType;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.registry.ItemRegistry;
import org.cloudburstmc.api.registry.RecipeRegistry;
import org.cloudburstmc.api.registry.RegistryException;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.Identifiers;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.ContainerMixData;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.CraftingData;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.PotionMixData;
import org.cloudburstmc.protocol.bedrock.packet.CraftingDataPacket;
import org.cloudburstmc.server.Bootstrap;
import org.cloudburstmc.server.crafting.*;
import org.cloudburstmc.server.inventory.ContainerRecipe;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.utils.TextFormat;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.cloudburstmc.api.block.BlockIds.LIT_BLAST_FURNACE;

@Log4j2
public class CloudRecipeRegistry implements RecipeRegistry, Registry {

    private static final String UNLABELED_PREFIX = "minecraft:crafting_recipe_";
    private static final String UNLABELED_POTION_PREFIX = "minecraft:potion_";
    private static final String UNLABELED_CONTAINER_PREFIX = "minecraft:container_";

    private static final CloudRecipeRegistry INSTANCE;

    static {
        INSTANCE = new CloudRecipeRegistry(CloudItemRegistry.get()); // forces item registry to init first
    }

    public static final Comparator<ItemStack> recipeComparator = Comparator.comparing((ItemStack i) -> i.getType().getId())
            .thenComparingInt(i -> ItemUtils.toNetwork(i).getDamage()).thenComparingInt(ItemStack::getCount);

    private final CloudItemRegistry itemRegistry;
    private final Map<Identifier, Recipe> recipeMap = new Object2ReferenceOpenHashMap<>();
    private final Int2ReferenceMap<Map<UUID, Identifier>> recipeHashMap = new Int2ReferenceOpenHashMap<>();
    private final Reference2IntMap<Identifier> netIdMap = new Reference2IntOpenHashMap<>();
    private final Int2ReferenceMap<Identifier> idNetMap = new Int2ReferenceOpenHashMap<>();
    private final AtomicInteger netIdAllocator = new AtomicInteger();

    private boolean closed;
    private CraftingDataPacket cached;

    public static CloudRecipeRegistry get() {
        return INSTANCE;
    }

    public CloudRecipeRegistry(ItemRegistry registry) {
        this.itemRegistry = (CloudItemRegistry) registry;
        try {
            loadFromFile(Thread.currentThread().getContextClassLoader().getResource("data/recipes.json").toURI());
        } catch (URISyntaxException | NullPointerException e) {
            throw new RegistryException("Unable to load recipes.json", e);
        }

    }

    @Override
    public void close() throws RegistryException {
        if (this.closed) {
            throw new RegistryException("Registry already closed!");
        }
        this.closed = true;
        log.info("Loaded {}{}{} recipies.", TextFormat.GREEN, recipeMap.size(), TextFormat.RESET);
    }

    @Override
    public void unregister(Identifier id) {
        unregister(recipeMap.get(id));
    }


    @Override
    public void unregister(Recipe recipe) {
        if (this.closed) {
            throw new RegistryException("Unable to unregister recipes after registry closes");
        }
        int outputHash = ItemUtils.getItemHash((ItemStack) recipe.getResult());
        UUID id = getInputHash(recipe);
        var hashMap = recipeHashMap.get(outputHash);
        if (hashMap.size() <= 1) {
            recipeHashMap.remove(outputHash);
        } else {
            hashMap.remove(id);
        }

        recipeMap.remove(recipe.getId());
        int netId = netIdMap.remove(recipe.getId());
        idNetMap.remove(netId);

    }

    @Override
    public void register(Recipe recipe) throws RegistryException {
        if (recipeMap.containsKey(recipe.getId())) {
            log.warn("Recipe with Identifier {} already registered! Skipping.", recipe.getId());
        }

        int outputHash;
        UUID id;
        if (recipe.getType() == RecipeType.MULTI) {
            outputHash = Objects.hash(System.identityHashCode(recipe.getId()));
            id = ((MultiRecipe) recipe).getUuid();
        } else {
            outputHash = ItemUtils.getItemHash((ItemStack) recipe.getResult());
            id = getInputHash(recipe);
        }
        register0(recipe.getId(), id, outputHash, recipe);

    }

    private void register0(Identifier id, UUID uuid, int outputHash, Recipe recipe) {
        Map<UUID, Identifier> map = recipeHashMap.computeIfAbsent(outputHash, (x) -> new HashMap<>());
        map.put(uuid, id);
        this.recipeMap.put(id, recipe);
        int netId = this.netIdAllocator.getAndIncrement();
        this.netIdMap.putIfAbsent(id, netId);
        this.idNetMap.putIfAbsent(netId, id);
    }

    public void loadFromFile(URI file) {
        log.info("Loading recipes from {}...", file);
        JsonNode json;
        int unlabeled = 0;

        try {
            json = Bootstrap.JSON_MAPPER.readTree(file.toURL());
        } catch (IOException e) {
            throw new RuntimeException("Unable to read JSON File to load recipes", e);
        }

        if (json == null) {
            log.warn("Unable to load JSON file: {}", file);
            return;
        }

        //Load Recipes
        for (JsonNode recipe : json.get("recipes")) {
            Identifier id;
            if (recipe.has("id")) {
                id = Identifier.fromString(recipe.get("id").asText());
            } else {
                id = Identifier.fromString(UNLABELED_PREFIX + (++unlabeled));
            }

            switch (recipe.get("type").asInt()) {
                case 0: // Shapeless
                case 5:
                    String craftingBlock = recipe.get("block").asText();
                    Identifier block = craftingBlock == null ? null : Identifier.fromString(craftingBlock);

                    List<ItemStack> outputs = new ArrayList<>();
                    for (Map<String, Object> item : Bootstrap.JSON_MAPPER.convertValue(recipe.get("output"), new TypeReference<List<Map<String, Object>>>() {
                    })) {
                        outputs.add(ItemUtils.fromJson(item));
                    }

                    List<ItemStack> inputs = new ArrayList<>();
                    for (Map<String, Object> item : Bootstrap.JSON_MAPPER.convertValue(recipe.get("input"), new TypeReference<List<Map<String, Object>>>() {
                    })) {
                        inputs.add(ItemUtils.fromJson(item));
                    }
                    inputs.sort(recipeComparator);

                    this.register(new ShapelessRecipe(id, recipe.get("priority").asInt(), outputs, inputs, block, RecipeType.values()[recipe.get("type").asInt()]));
                    break;
                case 1: //Shaped
                    String[] shape = Bootstrap.JSON_MAPPER.convertValue(recipe.get("shape"), new TypeReference<String[]>() {
                    });
                    craftingBlock = recipe.get("block").asText();
                    block = craftingBlock == null ? null : Identifier.fromString(craftingBlock);

                    outputs = new ArrayList<>();
                    for (Map<String, Object> item : Bootstrap.JSON_MAPPER.convertValue(recipe.get("output"), new TypeReference<List<Map<String, Object>>>() {
                    })) {
                        outputs.add(ItemUtils.fromJson(item));
                    }
                    ItemStack primary = outputs.remove(0);

                    CharObjectMap<ItemStack> ingredients = new CharObjectHashMap<>();
                    for (Map.Entry<String, Map<String, Object>> item : Bootstrap.JSON_MAPPER.convertValue(recipe.get("input"), new TypeReference<Map<String, Map<String, Object>>>() {
                    }).entrySet()) {
                        ingredients.put(item.getKey().charAt(0), ItemUtils.fromJson(item.getValue()));
                    }
                    this.register(new ShapedRecipe(id, recipe.get("priority").asInt(), primary, shape, ingredients, outputs, block));
                    break;
                case 2:
                case 3: //furnace
                    Map<String, Object> outputData = Bootstrap.JSON_MAPPER.convertValue(recipe.get("output"), new TypeReference<Map<String, Object>>() {
                    });
                    Map<String, Object> inputData = Bootstrap.JSON_MAPPER.convertValue(recipe.get("input"), new TypeReference<Map<String, Object>>() {
                    });
                    craftingBlock = recipe.get("block").asText();
                    block = craftingBlock == null ? null : Identifier.fromString(craftingBlock);

                    this.register(new FurnaceRecipe(id, ItemUtils.fromJson(outputData), ItemUtils.fromJson(inputData), block));
                    break;
                case 4: //Multi
                    UUID uuid = UUID.fromString(recipe.get("uuid").asText());
                    this.register(new MultiRecipe(id, uuid));
                    break;
                default:
                    throw new RegistryException("Unsupported Recipe type");
            }
        }

        //Load Potions
        unlabeled = 0;
        for (JsonNode recipe : json.get("potionMixes")) {
            ItemStack input = ItemUtils.deserializeItem(Identifier.fromString(recipe.get("inputId").asText()), recipe.get("inputMeta").shortValue(), 1, NbtMap.EMPTY);
            ItemStack reagent = ItemUtils.deserializeItem(Identifier.fromString(recipe.get("reagentId").asText()), recipe.get("reagentMeta").shortValue(), 1, NbtMap.EMPTY);
            ItemStack output = ItemUtils.deserializeItem(Identifier.fromString(recipe.get("outputId").asText()), recipe.get("outputMeta").shortValue(), 1, NbtMap.EMPTY);

            Identifier id = Identifier.fromString(UNLABELED_POTION_PREFIX + (++unlabeled));
            this.register(new BrewingRecipe(id, input, reagent, output));
        }

        //Load Container Mixes
        unlabeled = 0;
        for (JsonNode recipe : json.get("containerMixes")) {
            ItemStack input = ItemUtils.deserializeItem(Identifier.fromString(recipe.get("inputId").asText()), (short) 0, 1, NbtMap.EMPTY);
            ItemStack reagent = ItemUtils.deserializeItem(Identifier.fromString(recipe.get("reagentId").asText()), (short) 0, 1, NbtMap.EMPTY);
            ItemStack output = ItemUtils.deserializeItem(Identifier.fromString(recipe.get("outputId").asText()), (short) 0, 1, NbtMap.EMPTY);

            Identifier id = Identifier.fromString(UNLABELED_CONTAINER_PREFIX + (++unlabeled));
            this.register(new ContainerRecipe(id, input, reagent, output));
        }

        rebuildPacket();
        log.info("Loaded {} recipes.", recipeMap.size());
    }

    @NonNull
    public int getRecipeNetId(Identifier id) {
        return netIdMap.getOrDefault(id, 0);
    }

    @NonNull
    public int getRecipeNetId(Recipe recipe) {
        return getRecipeNetId(recipe.getId());
    }

    public Recipe getRecipeFromNetId(int netId) {
        return this.recipeMap.get(idNetMap.get(netId));
    }

    @Nullable
    @Override
    public Recipe getRecipe(Identifier identifier) {
        return this.recipeMap.get(identifier);
    }

    @Nullable
    @Override
    public Recipe getRecipe(UUID uuid) {
        for (Map<UUID, Identifier> map : this.recipeHashMap.values()) {
            for (Map.Entry<UUID, Identifier> entry : map.entrySet()) {
                if (uuid.equals(entry.getKey())) {
                    return recipeMap.get(entry.getValue());
                }
            }
        }
        return null;
    }

    @Override
    public Recipe matchRecipe(ItemStack[][] inputMap, ItemStack output, ItemStack[][] extraOutputMap, Identifier craftingBlock) {
        int key = ItemUtils.getItemHash((ItemStack) output);
        if (!recipeHashMap.containsKey(key)) {
            return null;
        }

        Map<UUID, Identifier> map = recipeHashMap.get(key);
        List<ItemStack> inputs = new ArrayList<>();
        for (ItemStack[] row : inputMap) inputs.addAll(Arrays.asList(row));

        if (craftingBlock != null) {
            if (craftingBlock == Identifiers.FURNACE || craftingBlock == Identifiers.LIT_FURNACE
                    || craftingBlock == Identifiers.BLAST_FURNACE || craftingBlock == Identifiers.LIT_BLAST_FURNACE)
                return matchFurnaceRecipe(inputs.get(0), output, craftingBlock);
            if (craftingBlock == Identifiers.BREWING_STAND)
                return matchBrewingRecipe(inputs.get(0), output);
        }

        //Shaped recipies don't have inputs sorted
        UUID id = ItemUtils.getMultiItemHash(inputs);
        if (map.containsKey(id)) {
            CraftingRecipe recipe = (CraftingRecipe) recipeMap.get(map.get(id));
            if (recipe.matchItems(inputMap.clone(), extraOutputMap.clone())) {
                if (recipe.requiresCraftingTable() && craftingBlock != recipe.getBlock()) return null;
                return recipe;
            }
        }

        //Shapeless needs inputs sorted before hash
        inputs.sort(recipeComparator);
        id = ItemUtils.getMultiItemHash(inputs);
        if (map.containsKey(id)) {
            CraftingRecipe recipe = (CraftingRecipe) recipeMap.get(map.get(id));
            if (recipe.matchItems(inputMap.clone(), extraOutputMap.clone())) {
                if (recipe.requiresCraftingTable() && craftingBlock != recipe.getBlock()) return null;
                return recipe;
            }
        }
        return null;
    }

    public FurnaceRecipe matchFurnaceRecipe(ItemStack input, ItemStack output, Identifier craftingBlock) {
        int hash = ItemUtils.getItemHash((ItemStack) output);

        if (craftingBlock == BlockIds.LIT_SMOKER) {
            craftingBlock = BlockIds.SMOKER;
        } else if (craftingBlock == LIT_BLAST_FURNACE) {
            craftingBlock = BlockIds.BLAST_FURNACE;
        } else if (craftingBlock == BlockIds.LIT_FURNACE) {
            craftingBlock = BlockIds.FURNACE;
        }

        if (!recipeHashMap.containsKey(hash)) return null;

        Map<UUID, Identifier> map = recipeHashMap.get(hash);
        UUID id = ItemUtils.getMultiItemHash(Arrays.asList(input, output));

        Recipe recipe = recipeMap.get(map.get(id));
        if (recipe != null && recipe.getBlock() == craftingBlock
                && recipe.getResult().equals(output) && ((FurnaceRecipe) recipe).getInput().equals(input))
            return (FurnaceRecipe) recipe;

        return null;
    }

    public FurnaceRecipe matchFurnaceRecipe(ItemStack input, Identifier craftingBlock) {
        if (craftingBlock == BlockIds.LIT_SMOKER) {
            craftingBlock = BlockIds.SMOKER;
        } else if (craftingBlock == LIT_BLAST_FURNACE) {
            craftingBlock = BlockIds.BLAST_FURNACE;
        } else if (craftingBlock == BlockIds.LIT_FURNACE) {
            craftingBlock = BlockIds.FURNACE;
        }

        for (Recipe recipe : this.recipeMap.values()) {
            if (recipe.getBlock() != craftingBlock)
                continue;

            if (recipe instanceof FurnaceRecipe && ((FurnaceRecipe) recipe).getInput().equals(input)) {
                return (FurnaceRecipe) recipe;
            }
        }
        return null;
    }

    public MixRecipe matchBrewingRecipe(ItemStack input, ItemStack potion) {
        int hash = ItemUtils.getItemHash((ItemStack) potion);
        if (!recipeHashMap.containsKey(hash)) return null;

        Map<UUID, Identifier> map = recipeHashMap.get(hash);
        UUID id = ItemUtils.getMultiItemHash(Arrays.asList(input, potion));

        Recipe recipe = recipeMap.get(map.get(id));
        if (recipe != null && recipe.getResult().equals(potion) && ((MixRecipe) recipe).getInput().equals(input))
            return (MixRecipe) recipe;

        return null;
    }

    @Override
    public Collection<Recipe> getRecipes() {
        return ImmutableList.copyOf(this.recipeMap.values());
    }

    public CraftingDataPacket getNetworkData() {
        if (cached != null) {
            return cached;
        }
        rebuildPacket();
        return cached;
    }

    private void rebuildPacket() {
        CraftingDataPacket packet = new CraftingDataPacket();
        packet.setCleanRecipes(true);

        for (Map<UUID, Identifier> map : recipeHashMap.values()) {
            for (Map.Entry<UUID, Identifier> entry : map.entrySet()) {
                Recipe recipe = recipeMap.get(entry.getValue());
                switch (recipe.getType()) {
                    case SHAPELESS:
                        packet.getCraftingData().add(CraftingData.fromShapeless(
                                recipe.getId().toString(),
                                ItemUtils.toDescriptors(((ShapelessRecipe) recipe).getIngredientList()),
                                ItemUtils.toNetwork(((ShapelessRecipe) recipe).getAllResults()),
                                entry.getKey(),
                                recipe.getBlock().getName(),
                                ((ShapelessRecipe) recipe).getPriority(),
                                netIdMap.getOrDefault(recipe.getId(), 0)));
                        break;
                    case SHULKER_BOX:
                        packet.getCraftingData().add(CraftingData.fromShulkerBox(
                                recipe.getId().toString(),
                                ItemUtils.toDescriptors(((ShapelessRecipe) recipe).getIngredientList()),
                                ItemUtils.toNetwork(((ShapelessRecipe) recipe).getAllResults()),
                                entry.getKey(),
                                recipe.getBlock().getName(),
                                ((ShapelessRecipe) recipe).getPriority(),
                                netIdMap.getOrDefault(recipe.getId(), 0)));
                        break;
                    case SHAPED:
                        packet.getCraftingData().add(CraftingData.fromShaped(
                                recipe.getId().toString(),
                                ((ShapedRecipe) recipe).getWidth(),
                                ((ShapedRecipe) recipe).getHeight(),
                                ItemUtils.toDescriptors(((ShapedRecipe) recipe).getIngredientList()),
                                ItemUtils.toNetwork(((ShapedRecipe) recipe).getAllResults()),
                                entry.getKey(),
                                recipe.getBlock().getName(),
                                ((ShapedRecipe) recipe).getPriority(),
                                netIdMap.getOrDefault(recipe.getId(), 0)));
                        break;
                    case FURNACE:
                        assert recipe instanceof FurnaceRecipe;
                        ItemData inputData = ItemUtils.toNetwork(((FurnaceRecipe) recipe).getInput());
                        ItemData outputData = ItemUtils.toNetwork(recipe.getResult());

                        packet.getCraftingData().add(CraftingData.fromFurnace(
                                inputData.getDefinition().getRuntimeId(),
                                outputData,
                                recipe.getBlock().getName(),
                                netIdMap.getOrDefault(recipe.getId(), 0)));
                        break;
                    case FURNACE_DATA:
                        assert recipe instanceof FurnaceRecipe;
                        inputData = ItemUtils.toNetwork(((FurnaceRecipe) recipe).getInput());
                        outputData = ItemUtils.toNetwork(recipe.getResult());

                        packet.getCraftingData().add(CraftingData.fromFurnaceData(
                                ItemUtils.toNetwork(((FurnaceRecipe) recipe).getInput()).getDefinition().getRuntimeId(),
                                inputData.getDamage(),
                                outputData,
                                recipe.getBlock().getName(),
                                netIdMap.getOrDefault(recipe.getId(), 0)));
                        break;
                    case MULTI:
                        packet.getCraftingData().add(CraftingData.fromMulti(
                                ((MultiRecipe) recipe).getUuid(),
                                netIdMap.getOrDefault(recipe.getId(), 0)));
                        break;
                    case POTION:
                        assert recipe instanceof BrewingRecipe;
                        ItemData reagentData = ItemUtils.toNetwork(((BrewingRecipe) recipe).getIngredient());
                        inputData = ItemUtils.toNetwork(((BrewingRecipe) recipe).getInput());
                        outputData = ItemUtils.toNetwork(recipe.getResult());

                        packet.getPotionMixData().add(new PotionMixData(
                                inputData.getDefinition().getRuntimeId(),
                                inputData.getDamage(),
                                reagentData.getDefinition().getRuntimeId(),
                                reagentData.getDamage(),
                                outputData.getDefinition().getRuntimeId(),
                                outputData.getDamage()));
                        break;
                    case CONTAINER:
                        assert recipe instanceof ContainerRecipe;
                        reagentData = ItemUtils.toNetwork(((ContainerRecipe) recipe).getIngredient());
                        inputData = ItemUtils.toNetwork(((ContainerRecipe) recipe).getInput());
                        outputData = ItemUtils.toNetwork(recipe.getResult());

                        packet.getContainerMixData().add(new ContainerMixData(
                                inputData.getDefinition().getRuntimeId(),
                                reagentData.getDefinition().getRuntimeId(),
                                outputData.getDefinition().getRuntimeId()));
                        break;
                    default:
                        break;
                }
            }
        }

        this.cached = packet;
    }

    private UUID getInputHash(Recipe recipe) {
        switch (recipe.getType()) {
            case SHAPED:
            case SHAPED_CHEMISTRY:
                return ItemUtils.getMultiItemHash(((ShapedRecipe) recipe).getIngredientList());
            case SHAPELESS:
            case SHAPELESS_CHEMISTRY:
            case SHULKER_BOX:
                List<ItemStack> list = ((ShapelessRecipe) recipe).getIngredientList();
                list.sort(recipeComparator);
                return ItemUtils.getMultiItemHash(list);
            case FURNACE:
            case FURNACE_DATA:
                ItemStack input = ((FurnaceRecipe) recipe).getInput();
                return ItemUtils.getMultiItemHash(Arrays.asList(input, recipe.getResult()));
            case POTION:
            case CONTAINER:
                return ItemUtils.getMultiItemHash(Arrays.asList(((MixRecipe) recipe).getInput(), ((MixRecipe) recipe).getIngredient()));
        }
        return null;
    }
}
