package org.cloudburstmc.server.registry;

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
import org.cloudburstmc.api.crafting.MixRecipe;
import org.cloudburstmc.api.crafting.Recipe;
import org.cloudburstmc.api.crafting.RecipeType;
import org.cloudburstmc.api.crafting.RecipeUnlockContext;
import org.cloudburstmc.api.item.*;
import org.cloudburstmc.api.registry.ItemRegistry;
import org.cloudburstmc.api.registry.RecipeRegistry;
import org.cloudburstmc.api.registry.RegistryException;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.Identifiers;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.ContainerMixData;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.PotionMixData;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.RecipeUnlockingRequirement;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.recipe.*;
import org.cloudburstmc.protocol.bedrock.data.inventory.descriptor.ItemDescriptorWithCount;
import org.cloudburstmc.protocol.bedrock.data.inventory.descriptor.ItemTagDescriptor;
import org.cloudburstmc.protocol.bedrock.packet.CraftingDataPacket;
import org.cloudburstmc.protocol.bedrock.packet.TrimDataPacket;
import org.cloudburstmc.protocol.bedrock.packet.UnlockedRecipesPacket;
import org.cloudburstmc.server.Bootstrap;
import org.cloudburstmc.server.crafting.*;
import org.cloudburstmc.server.item.ItemUtils;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.cloudburstmc.api.block.BlockIds.LIT_BLAST_FURNACE;

@Log4j2
public class CloudRecipeRegistry implements RecipeRegistry {

    public static final Comparator<ItemStack> recipeComparator = Comparator
            .comparing((ItemStack i) -> i.isEmpty() || i.getType() == null ? "" : i.getType().getId().toString())
            .thenComparingInt(i -> i.isEmpty() || i.getType() == null ? 0 : ItemUtils.toNetwork(i).getDamage())
            .thenComparingInt(ItemStack::getCount);

    private static final String UNLABELED_PREFIX = "minecraft:crafting_recipe_";
    private static final String UNLABELED_POTION_PREFIX = "minecraft:potion_";
    private static final String UNLABELED_CONTAINER_PREFIX = "minecraft:container_";

    private static final int RECIPE_TYPE_SHAPELESS = 0;
    private static final int RECIPE_TYPE_SHAPED = 1;
    private static final int RECIPE_TYPE_COOKING = 3;
    private static final int RECIPE_TYPE_COMPLEX = 4;
    private static final int RECIPE_TYPE_SHULKER_BOX = 5;
    private static final int RECIPE_TYPE_SMITHING_TRANSFORM = 8;
    private static final int RECIPE_TYPE_SMITHING_TRIM = 9;
    private static final CloudRecipeRegistry INSTANCE;

    static {
        INSTANCE = new CloudRecipeRegistry(CloudItemRegistry.get()); // forces item registry to init first
    }

    private final CloudItemRegistry itemRegistry;
    private final Map<Identifier, Recipe> recipeMap = new Object2ReferenceOpenHashMap<>();
    private final Int2ReferenceMap<Map<UUID, Identifier>> recipeHashMap = new Int2ReferenceOpenHashMap<>();
    private final Reference2IntMap<Identifier> netIdMap = new Reference2IntOpenHashMap<>();
    private final Int2ReferenceMap<Identifier> idNetMap = new Int2ReferenceOpenHashMap<>();
    private final AtomicInteger netIdAllocator = new AtomicInteger();
    private final List<TrimPattern> trimPatterns = new ArrayList<>();
    private final List<TrimMaterial> trimMaterials = new ArrayList<>();
    private final List<SmithingUpgrade> smithingUpgrades = new ArrayList<>();

    private boolean closed;
    private CraftingDataPacket cached;
    private TrimDataPacket trimData;
    private UnlockedRecipesPacket unlockedRecipes;

    public CloudRecipeRegistry(ItemRegistry registry) {
        this.itemRegistry = (CloudItemRegistry) registry;
        try {
            loadFromFile(Thread.currentThread().getContextClassLoader().getResource("data/recipes.json").toURI());
        } catch (URISyntaxException | NullPointerException e) {
            throw new RegistryException("Unable to load data/recipes.json", e);
        }
        registerVanillaTrimData();
        registerVanillaSmithingTransformRecipes();
    }

    public static CloudRecipeRegistry get() {
        return INSTANCE;
    }

    @Override
    public void close() throws RegistryException {
        if (this.closed) {
            throw new RegistryException("Registry already closed!");
        }
        this.closed = true;
        rebuildPacket();
        log.info("Loaded §a{}§r recipes.", recipeMap.size());
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

        int outputHash = ItemUtils.getItemHash(recipe.getResult());
        UUID id = getInputHash(recipe);
        if (id == null) {
            recipeMap.remove(recipe.getId());
            int netId = netIdMap.remove(recipe.getId());
            idNetMap.remove(netId);
            return;
        }

        Map<UUID, Identifier> hashMap = recipeHashMap.get(outputHash);
        if (hashMap != null) {
            if (hashMap.size() <= 1) {
                recipeHashMap.remove(outputHash);
            } else {
                hashMap.remove(id);
            }
        }

        recipeMap.remove(recipe.getId());
        int netId = netIdMap.remove(recipe.getId());
        idNetMap.remove(netId);
    }

    @Override
    public void register(Recipe recipe) throws RegistryException {
        if (recipeMap.containsKey(recipe.getId())) {
            log.debug("Recipe with Identifier §e{}§r already registered! Skipping.", recipe.getId());
            return;
        }

        int outputHash;
        UUID id = switch (recipe.getType()) {
            case COMPLEX -> {
                outputHash = Objects.hash(System.identityHashCode(recipe.getId()));
                yield ((CloudComplexRecipe) recipe).uuid();
            }
            case SMITHING_TRANSFORM, SMITHING_TRIM -> {
                outputHash = Objects.hash(System.identityHashCode(recipe.getId()));
                yield UUID.nameUUIDFromBytes(recipe.getId().toString().getBytes());
            }
            default -> {
                outputHash = ItemUtils.getItemHash(recipe.getResult());
                yield getInputHash(recipe);
            }
        };
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
        JsonNode json;
        int unlabeled = 0;

        try {
            json = Bootstrap.JSON_MAPPER.readTree(file.toURL().openStream());
        } catch (IOException e) {
            throw new RuntimeException("Unable to read JSON File to load recipes", e);
        }

        if (json == null) {
            log.warn("Unable to load JSON file: {}", file);
            return;
        }

        // Load Recipes
        for (JsonNode recipe : json.get("recipes")) {
            Identifier id;
            if (recipe.has("id")) {
                id = Identifier.parse(recipe.get("id").asString());
            } else {
                id = Identifier.parse(UNLABELED_PREFIX + (++unlabeled));
            }

            Identifier block = null;
            if (recipe.has("block")) {
                String craftingBlock = recipe.get("block").asString();
                if (craftingBlock != null && !craftingBlock.equals("deprecated")) {
                    block = Identifier.parse(craftingBlock);
                }
            }

            switch (recipe.get("type").asInt()) {
                case RECIPE_TYPE_SHAPELESS:
                case RECIPE_TYPE_SHULKER_BOX:
                    List<ItemStack> outputs = new ArrayList<>();
                    for (Map<String, Object> item : Bootstrap.JSON_MAPPER.convertValue(recipe.get("output"), new TypeReference<List<Map<String, Object>>>() {
                    })) {
                        outputs.add(ItemUtils.fromJson(item));
                    }

                    List<ItemStack> inputs = new ArrayList<>();
                    List<ItemDescriptorWithCount> inputDescriptors = new ArrayList<>();
                    for (Map<String, Object> item : Bootstrap.JSON_MAPPER.convertValue(recipe.get("input"), new TypeReference<List<Map<String, Object>>>() {
                    })) {
                        inputDescriptors.add(ItemUtils.descriptorFromJson(item));
                        inputs.add(ItemUtils.fromJson(item));
                    }
                    inputs.removeIf(ItemStack::isEmpty);

                    if (recipe.get("type").asInt() == RECIPE_TYPE_SHAPELESS && block != null && block.getName().equals("stonecutter")) {
                        ItemStack scInput = inputs.isEmpty() ? ItemStack.EMPTY : inputs.getFirst();
                        ItemDescriptorWithCount scDescriptor = inputDescriptors.isEmpty() ? ItemDescriptorWithCount.EMPTY : inputDescriptors.getFirst();
                        ItemStack scResult = outputs.isEmpty() ? ItemStack.EMPTY : outputs.getFirst();
                        this.register(new CloudStonecuttingRecipe(id, recipe.get("priority").asInt(), scInput, scDescriptor, scResult));
                    } else if (recipe.get("type").asInt() == RECIPE_TYPE_SHULKER_BOX) {
                        inputs.sort(recipeComparator);
                        this.register(new CloudShulkerBoxRecipe(id, recipe.get("priority").asInt(), outputs, inputs, inputDescriptors, block));
                    } else {
                        inputs.sort(recipeComparator);
                        this.register(new CloudShapelessRecipe(id, recipe.get("priority").asInt(), outputs, inputs, inputDescriptors, block, RecipeType.SHAPELESS));
                    }
                    break;
                case RECIPE_TYPE_SHAPED:
                    String[] shape = Bootstrap.JSON_MAPPER.convertValue(recipe.get("shape"), new TypeReference<>() {
                    });

                    outputs = new ArrayList<>();
                    for (Map<String, Object> item : Bootstrap.JSON_MAPPER.convertValue(recipe.get("output"), new TypeReference<List<Map<String, Object>>>() {
                    })) {
                        outputs.add(ItemUtils.fromJson(item));
                    }
                    ItemStack primary = outputs.remove(0);

                    CharObjectMap<ItemStack> ingredients = new CharObjectHashMap<>();
                    CharObjectMap<ItemDescriptorWithCount> ingredientDescs = new CharObjectHashMap<>();
                    for (Map.Entry<String, Map<String, Object>> item : Bootstrap.JSON_MAPPER.convertValue(recipe.get("input"), new TypeReference<Map<String, Map<String, Object>>>() {
                    }).entrySet()) {
                        char key = item.getKey().charAt(0);
                        ingredients.put(key, ItemUtils.fromJson(item.getValue()));
                        ingredientDescs.put(key, ItemUtils.descriptorFromJson(item.getValue()));
                    }
                    this.register(new CloudShapedRecipe(id, recipe.get("priority").asInt(), primary, shape, ingredients, ingredientDescs, outputs, block));
                    break;
                case RECIPE_TYPE_COOKING:
                    Map<String, Object> outputData = Bootstrap.JSON_MAPPER.convertValue(recipe.get("output"), new TypeReference<>() {
                    });
                    Map<String, Object> inputData = Bootstrap.JSON_MAPPER.convertValue(recipe.get("input"), new TypeReference<>() {
                    });

                    this.register(new CloudFurnaceRecipe(id, ItemUtils.fromJson(outputData), ItemUtils.fromJson(inputData), ItemUtils.descriptorFromJson(inputData), block, recipe.has("priority") ? recipe.get("priority").asInt() : 0));
                    break;
                case RECIPE_TYPE_COMPLEX:
                    UUID uuid = UUID.fromString(recipe.get("uuid").asString());
                    this.register(new CloudComplexRecipe(id, uuid));
                    break;
                case RECIPE_TYPE_SMITHING_TRANSFORM:
                    break;
                case RECIPE_TYPE_SMITHING_TRIM:
                    ItemDescriptorWithCount trimBase = new ItemDescriptorWithCount(new ItemTagDescriptor("minecraft:trimmable_armors"), 1);
                    ItemDescriptorWithCount trimAddition = new ItemDescriptorWithCount(new ItemTagDescriptor("minecraft:trim_materials"), 1);
                    ItemDescriptorWithCount trimTemplate = new ItemDescriptorWithCount(new ItemTagDescriptor("minecraft:trim_templates"), 1);
                    this.register(new CloudSmithingTrimRecipe(
                            Identifier.parse("minecraft:smithing_armor_trim"),
                            trimTemplate, trimBase, trimAddition,
                            Identifier.parse("smithing_table")));
                    break;
                default:
                    log.warn("Unsupported recipe type {} for recipe {}", recipe.get("type").asInt(), id);
                    break;
            }
        }

        // Load Potions
        unlabeled = 0;
        for (JsonNode recipe : json.get("potionMixes")) {
            ItemStack input = ItemUtils.deserializeItem(Identifier.parse(recipe.get("inputId").asString()), recipe.get("inputMeta").shortValue(), 1, NbtMap.EMPTY);
            ItemStack reagent = ItemUtils.deserializeItem(Identifier.parse(recipe.get("reagentId").asString()), recipe.get("reagentMeta").shortValue(), 1, NbtMap.EMPTY);
            ItemStack output = ItemUtils.deserializeItem(Identifier.parse(recipe.get("outputId").asString()), recipe.get("outputMeta").shortValue(), 1, NbtMap.EMPTY);

            Identifier id = Identifier.parse(UNLABELED_POTION_PREFIX + (++unlabeled));
            this.register(new CloudBrewingRecipe(id, input, reagent, output));
        }

        // Load Container Mixes
        unlabeled = 0;
        for (JsonNode recipe : json.get("containerMixes")) {
            ItemStack input = ItemUtils.deserializeItem(Identifier.parse(recipe.get("inputId").asString()), (short) 0, 1, NbtMap.EMPTY);
            ItemStack reagent = ItemUtils.deserializeItem(Identifier.parse(recipe.get("reagentId").asString()), (short) 0, 1, NbtMap.EMPTY);
            ItemStack output = ItemUtils.deserializeItem(Identifier.parse(recipe.get("outputId").asString()), (short) 0, 1, NbtMap.EMPTY);

            Identifier id = Identifier.parse(UNLABELED_CONTAINER_PREFIX + (++unlabeled));
            this.register(new CloudContainerRecipe(id, input, reagent, output));
        }
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
        int key = ItemUtils.getItemHash(output);
        if (!recipeHashMap.containsKey(key)) {
            return null;
        }

        Map<UUID, Identifier> map = recipeHashMap.get(key);
        List<ItemStack> inputs = new ArrayList<>();
        for (ItemStack[] row : inputMap) {
            inputs.addAll(Arrays.asList(row));
        }

        if (craftingBlock != null) {
            if (craftingBlock == Identifiers.FURNACE || craftingBlock == Identifiers.LIT_FURNACE || craftingBlock == Identifiers.BLAST_FURNACE || craftingBlock == Identifiers.LIT_BLAST_FURNACE) {
                return matchFurnaceRecipe(inputs.getFirst(), output, craftingBlock);
            }
            if (craftingBlock == Identifiers.BREWING_STAND) {
                return matchBrewingRecipe(inputs.getFirst(), output);
            }
        }

        // Shaped recipes don't have inputs sorted
        UUID id = ItemUtils.getMultiItemHash(inputs);
        if (map.containsKey(id)) {
            Recipe candidate = recipeMap.get(map.get(id));
            if (candidate instanceof CloudShapedRecipe shaped && shaped.matchItems(inputMap.clone(), extraOutputMap.clone())) {
                if (shaped.requiresCraftingTable() && craftingBlock != shaped.getBlock()) return null;
                return shaped;
            }
        }

        // Shapeless recipes need inputs sorted before hashing
        inputs.sort(recipeComparator);
        id = ItemUtils.getMultiItemHash(inputs);
        if (map.containsKey(id)) {
            Recipe candidate = recipeMap.get(map.get(id));
            if (candidate instanceof CloudShapelessRecipe shapeless && shapeless.matchItems(inputMap.clone(), extraOutputMap.clone())) {
                if (shapeless.requiresCraftingTable() && craftingBlock != shapeless.getBlock()) return null;
                return shapeless;
            }
        }
        return null;
    }

    public CloudFurnaceRecipe matchFurnaceRecipe(ItemStack input, ItemStack output, Identifier craftingBlock) {
        int hash = ItemUtils.getItemHash(output);

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
                && recipe.getResult().equals(output) && ((CloudFurnaceRecipe) recipe).getInputItem().equals(input))
            return (CloudFurnaceRecipe) recipe;

        return null;
    }

    public CloudFurnaceRecipe matchFurnaceRecipe(ItemStack input, Identifier craftingBlock) {
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

            if (recipe instanceof CloudFurnaceRecipe furnace && furnace.getInputItem().equals(input)) {
                return furnace;
            }
        }
        return null;
    }

    @Override
    public Recipe matchRecipe(ItemStack input, Identifier craftingBlock) {
        if (craftingBlock != null && craftingBlock.getName().equals("stonecutter")) {
            return matchStonecuttingRecipe(input);
        }
        return matchFurnaceRecipe(input, craftingBlock);
    }

    public CloudStonecuttingRecipe matchStonecuttingRecipe(ItemStack input) {
        for (Recipe recipe : this.recipeMap.values()) {
            if (recipe instanceof CloudStonecuttingRecipe sc && sc.getInputItem().equals(input)) {
                return sc;
            }
        }
        return null;
    }

    /**
     * Finds a brewing or container mix recipe whose base bottle matches {@code basePotion} and
     * whose top-slot ingredient matches {@code ingredient}. Returns {@code null} if no match exists.
     *
     * @param ingredient the item placed in the ingredient slot (top of the stand)
     * @param basePotion the potion bottle being transformed (one of the three bottle slots)
     */
    @Override
    public MixRecipe matchBrewingRecipe(ItemStack ingredient, ItemStack basePotion) {
        for (Recipe recipe : recipeMap.values()) {
            if (!(recipe instanceof MixRecipe mix)) continue;
            if (mix.getInput().equals(basePotion) && mix.getIngredient().equals(ingredient)) {
                return mix;
            }
        }
        return null;
    }

    @Override
    public Collection<Recipe> getRecipes() {
        return ImmutableList.copyOf(this.recipeMap.values());
    }

    @Override
    public Collection<Recipe> getRecipes(RecipeType type) {
        ImmutableList.Builder<Recipe> builder = ImmutableList.builder();
        for (Recipe recipe : this.recipeMap.values()) {
            if (recipe.getType() == type) {
                builder.add(recipe);
            }
        }
        return builder.build();
    }

    @Override
    public Collection<Recipe> getRecipesByStation(Identifier craftingBlock) {
        ImmutableList.Builder<Recipe> builder = ImmutableList.builder();
        for (Recipe recipe : this.recipeMap.values()) {
            if (craftingBlock.equals(recipe.getBlock())) {
                builder.add(recipe);
            }
        }
        return builder.build();
    }

    @Override
    public Collection<Recipe> getRecipesForInput(ItemStack input, Identifier craftingBlock) {
        ImmutableList.Builder<Recipe> builder = ImmutableList.builder();
        for (Recipe recipe : this.recipeMap.values()) {
            if (!craftingBlock.equals(recipe.getBlock())) {
                continue;
            }
            if (recipe instanceof CloudFurnaceRecipe furnace && furnace.getInputItem().getType().getId().equals(input.getType().getId())) {
                builder.add(recipe);
            } else if (recipe instanceof CloudStonecuttingRecipe sc && sc.getInputItem().getType().getId().equals(input.getType().getId())) {
                builder.add(recipe);
            } else if (recipe instanceof CloudShapelessRecipe shapeless) {
                for (ItemStack ingredient : shapeless.getIngredientList()) {
                    if (ingredient.getType().getId().equals(input.getType().getId())) {
                        builder.add(recipe);
                        break;
                    }
                }
            } else if (recipe instanceof CloudShapedRecipe shaped) {
                for (ItemStack ingredient : shaped.getIngredientList()) {
                    if (ingredient.getType().getId().equals(input.getType().getId())) {
                        builder.add(recipe);
                        break;
                    }
                }
            }
        }
        return builder.build();
    }

    public CraftingDataPacket getNetworkData() {
        if (cached != null) {
            return cached;
        }
        rebuildPacket();
        return cached;
    }

    public TrimDataPacket getTrimData() {
        if (trimData != null) {
            return trimData;
        }
        rebuildPacket();
        return trimData;
    }

    public UnlockedRecipesPacket getUnlockedRecipesPacket() {
        if (unlockedRecipes != null) {
            return unlockedRecipes;
        }
        rebuildPacket();
        return unlockedRecipes;
    }

    @Override
    public void registerTrimPattern(TrimPattern pattern) {
        trimPatterns.add(pattern);
        trimData = null;
    }

    @Override
    public void registerTrimMaterial(TrimMaterial material) {
        trimMaterials.add(material);
        trimData = null;
    }

    @Override
    public Collection<TrimPattern> getTrimPatterns() {
        return ImmutableList.copyOf(trimPatterns);
    }

    @Override
    public Collection<TrimMaterial> getTrimMaterials() {
        return ImmutableList.copyOf(trimMaterials);
    }

    @Override
    public void registerSmithingUpgrade(SmithingUpgrade upgrade) {
        smithingUpgrades.add(upgrade);
    }

    @Override
    public Collection<SmithingUpgrade> getSmithingUpgrades() {
        return ImmutableList.copyOf(smithingUpgrades);
    }

    @Override
    public Collection<Recipe> getRecipesFor(ItemStack result) {
        return recipeMap.values().stream()
                .filter(r -> r.getResult().isSimilarMetadata(result))
                .collect(ImmutableList.toImmutableList());
    }

    private void registerVanillaTrimData() {
        trimPatterns.add(TrimPatterns.BOLT);
        trimPatterns.add(TrimPatterns.COAST);
        trimPatterns.add(TrimPatterns.DUNE);
        trimPatterns.add(TrimPatterns.EYE);
        trimPatterns.add(TrimPatterns.FLOW);
        trimPatterns.add(TrimPatterns.HOST);
        trimPatterns.add(TrimPatterns.RAISER);
        trimPatterns.add(TrimPatterns.RIB);
        trimPatterns.add(TrimPatterns.SENTRY);
        trimPatterns.add(TrimPatterns.SHAPER);
        trimPatterns.add(TrimPatterns.SILENCE);
        trimPatterns.add(TrimPatterns.SNOUT);
        trimPatterns.add(TrimPatterns.SPIRE);
        trimPatterns.add(TrimPatterns.TIDE);
        trimPatterns.add(TrimPatterns.VEX);
        trimPatterns.add(TrimPatterns.WARD);
        trimPatterns.add(TrimPatterns.WAYFINDER);
        trimPatterns.add(TrimPatterns.WILD);

        trimMaterials.add(TrimMaterials.AMETHYST);
        trimMaterials.add(TrimMaterials.COPPER);
        trimMaterials.add(TrimMaterials.DIAMOND);
        trimMaterials.add(TrimMaterials.EMERALD);
        trimMaterials.add(TrimMaterials.GOLD);
        trimMaterials.add(TrimMaterials.IRON);
        trimMaterials.add(TrimMaterials.LAPIS);
        trimMaterials.add(TrimMaterials.NETHERITE);
        trimMaterials.add(TrimMaterials.QUARTZ);
        trimMaterials.add(TrimMaterials.REDSTONE);
        trimMaterials.add(TrimMaterials.RESIN);
    }

    private void registerVanillaSmithingTransformRecipes() {
        Identifier smithingTable = Identifier.parse("smithing_table");
        ItemDescriptorWithCount templateDesc = ItemUtils.descriptorFromJson(Map.of("id", ItemIds.NETHERITE_UPGRADE_SMITHING_TEMPLATE.toString(), "count", 1));
        ItemDescriptorWithCount additionDesc = ItemUtils.descriptorFromJson(Map.of("id", ItemIds.NETHERITE_INGOT.toString(), "count", 1));

        List<SmithingUpgrade> upgrades = List.of(
                SmithingUpgrades.AXE, SmithingUpgrades.BOOTS, SmithingUpgrades.CHESTPLATE,
                SmithingUpgrades.HELMET, SmithingUpgrades.HOE, SmithingUpgrades.HORSE_ARMOR,
                SmithingUpgrades.LEGGINGS, SmithingUpgrades.NAUTILUS_ARMOR, SmithingUpgrades.PICKAXE,
                SmithingUpgrades.SHOVEL, SmithingUpgrades.SPEAR, SmithingUpgrades.SWORD
        );

        for (SmithingUpgrade upgrade : upgrades) {
            registerSmithingUpgrade(upgrade);
            ItemDescriptorWithCount baseDesc = ItemUtils.descriptorFromJson(Map.of("id", upgrade.base().toString(), "count", 1));
            ItemStack resultItem = ItemUtils.deserializeItem(upgrade.result(), (short) 0, 1, NbtMap.EMPTY);
            this.register(new CloudSmithingTransformRecipe(upgrade.id(), templateDesc, baseDesc, additionDesc, resultItem, smithingTable));
        }
    }

    private void rebuildPacket() {
        CraftingDataPacket packet = new CraftingDataPacket();
        packet.setCleanRecipes(true);

        for (Map<UUID, Identifier> map : recipeHashMap.values()) {
            for (Map.Entry<UUID, Identifier> entry : map.entrySet()) {
                Recipe recipe = recipeMap.get(entry.getValue());
                String blockTag = recipe.getBlock() != null ? recipe.getBlock().getName() : "";

                switch (recipe.getType()) {
                    case SHAPELESS: {
                        CloudShapelessRecipe shapeless = (CloudShapelessRecipe) recipe;
                        List<ItemDescriptorWithCount> descriptors = shapeless.getInputDescriptors();
                        if (descriptors == null) {
                            descriptors = ItemUtils.toDescriptors(shapeless.getIngredientList());
                        }
                        packet.getCraftingData().add(ShapelessRecipeData.shapeless(
                                recipe.getId().toString(),
                                descriptors,
                                ItemUtils.toNetwork(shapeless.getAllResults()),
                                entry.getKey(),
                                blockTag,
                                shapeless.getPriority(),
                                netIdMap.getOrDefault(recipe.getId(), 0),
                                toProtocolRequirement(shapeless.getUnlockContext())));
                        break;
                    }
                    case STONECUTTING: {
                        CloudStonecuttingRecipe sc = (CloudStonecuttingRecipe) recipe;
                        packet.getCraftingData().add(ShapelessRecipeData.shapeless(
                                recipe.getId().toString(),
                                sc.getInputDescriptors(),
                                ItemUtils.toNetwork(List.of(sc.getResult())),
                                entry.getKey(),
                                blockTag,
                                sc.getPriority(),
                                netIdMap.getOrDefault(recipe.getId(), 0),
                                RecipeUnlockingRequirement.INVALID));
                        break;
                    }
                    case SHULKER_BOX: {
                        CloudShapelessRecipe shulker = (CloudShapelessRecipe) recipe;
                        List<ItemDescriptorWithCount> descriptors = shulker.getInputDescriptors();
                        if (descriptors == null) {
                            descriptors = ItemUtils.toDescriptors(shulker.getIngredientList());
                        }
                        packet.getCraftingData().add(ShapelessRecipeData.shulkerBox(
                                recipe.getId().toString(),
                                descriptors,
                                ItemUtils.toNetwork(shulker.getAllResults()),
                                entry.getKey(),
                                blockTag,
                                shulker.getPriority(),
                                netIdMap.getOrDefault(recipe.getId(), 0)));
                        break;
                    }
                    case SHAPED: {
                        CloudShapedRecipe shaped = (CloudShapedRecipe) recipe;
                        List<ItemDescriptorWithCount> descriptors = shaped.getInputDescriptorList();
                        if (descriptors == null) {
                            descriptors = ItemUtils.toDescriptors(shaped.getIngredientList());
                        }
                        packet.getCraftingData().add(ShapedRecipeData.shaped(
                                recipe.getId().toString(),
                                shaped.getWidth(),
                                shaped.getHeight(),
                                descriptors,
                                ItemUtils.toNetwork(shaped.getAllResults()),
                                entry.getKey(),
                                blockTag,
                                shaped.getPriority(),
                                netIdMap.getOrDefault(recipe.getId(), 0),
                                shaped.isAssumeSymmetry(),
                                toProtocolRequirement(shaped.getUnlockContext())));
                        break;
                    }
                    case COOKING:
                        packet.getCraftingData().add(((CloudFurnaceRecipe) recipe).toNetwork());
                        break;
                    case COMPLEX:
                        packet.getCraftingData().add(MultiRecipeData.of(((CloudComplexRecipe) recipe).uuid(), netIdMap.getOrDefault(recipe.getId(), 0)));
                        break;
                    case SMITHING_TRANSFORM: {
                        CloudSmithingTransformRecipe transform = (CloudSmithingTransformRecipe) recipe;
                        packet.getCraftingData().add(SmithingTransformRecipeData.of(
                                recipe.getId().toString(),
                                transform.getTemplateDescriptor(),
                                transform.getBaseDescriptor(),
                                transform.getAdditionDescriptor(),
                                ItemUtils.toNetwork(recipe.getResult()),
                                blockTag,
                                netIdMap.getOrDefault(recipe.getId(), 0)));
                        break;
                    }
                    case SMITHING_TRIM: {
                        CloudSmithingTrimRecipe trim = (CloudSmithingTrimRecipe) recipe;
                        packet.getCraftingData().add(SmithingTrimRecipeData.of(
                                recipe.getId().toString(),
                                trim.getBaseDescriptor(),
                                trim.getAdditionDescriptor(),
                                trim.getTemplateDescriptor(),
                                blockTag,
                                netIdMap.getOrDefault(recipe.getId(), 0)));
                        break;
                    }
                    case POTION: {
                        CloudBrewingRecipe brewing = (CloudBrewingRecipe) recipe;
                        ItemData reagentData = ItemUtils.toNetwork(brewing.getIngredient());
                        ItemData inputData = ItemUtils.toNetwork(brewing.getInput());
                        ItemData outputData = ItemUtils.toNetwork(recipe.getResult());

                        packet.getPotionMixData().add(new PotionMixData(
                                inputData.getDefinition().getRuntimeId(),
                                inputData.getDamage(),
                                reagentData.getDefinition().getRuntimeId(),
                                reagentData.getDamage(),
                                outputData.getDefinition().getRuntimeId(),
                                outputData.getDamage()));
                        break;
                    }
                    case POTION_CONTAINER: {
                        CloudContainerRecipe container = (CloudContainerRecipe) recipe;
                        ItemData reagentData = ItemUtils.toNetwork(container.getIngredient());
                        ItemData inputData = ItemUtils.toNetwork(container.getInput());
                        ItemData outputData = ItemUtils.toNetwork(recipe.getResult());

                        packet.getContainerMixData().add(new ContainerMixData(
                                inputData.getDefinition().getRuntimeId(),
                                reagentData.getDefinition().getRuntimeId(),
                                outputData.getDefinition().getRuntimeId()));
                        break;
                    }
                    default:
                        break;
                }
            }
        }

        this.cached = packet;
        this.trimData = buildTrimDataPacket();
        this.unlockedRecipes = buildUnlockedRecipesPacket();
    }

    private TrimDataPacket buildTrimDataPacket() {
        TrimDataPacket packet = new TrimDataPacket();
        for (TrimPattern p : trimPatterns) {
            packet.getPatterns().add(new org.cloudburstmc.protocol.bedrock.data.TrimPattern(p.itemId().toString(), p.patternId()));
        }

        for (TrimMaterial m : trimMaterials) {
            packet.getMaterials().add(new org.cloudburstmc.protocol.bedrock.data.TrimMaterial(m.materialId(), m.color().legacyCode(), m.itemId().toString()));
        }
        return packet;
    }

    private UnlockedRecipesPacket buildUnlockedRecipesPacket() {
        UnlockedRecipesPacket packet = new UnlockedRecipesPacket();
        packet.setAction(UnlockedRecipesPacket.ActionType.INITIALLY_UNLOCKED);
        for (Recipe recipe : recipeMap.values()) {
            switch (recipe.getType()) {
                case POTION:
                case POTION_CONTAINER:
                    break;
                default:
                    packet.getUnlockedRecipes().add(recipe.getId().toString());
                    break;
            }
        }
        return packet;
    }

    private UUID getInputHash(Recipe recipe) {
        switch (recipe.getType()) {
            case SHAPED:
            case SHAPED_CHEMISTRY:
                return ItemUtils.getMultiItemHash(((CloudShapedRecipe) recipe).getIngredientList());
            case SHAPELESS:
            case SHAPELESS_CHEMISTRY:
            case SHULKER_BOX:
                List<ItemStack> list = new ArrayList<>(((CloudShapelessRecipe) recipe).getIngredientList());
                list.sort(recipeComparator);
                return ItemUtils.getMultiItemHash(list);
            case COOKING:
                ItemStack input = ((CloudFurnaceRecipe) recipe).getInputItem();
                return ItemUtils.getMultiItemHash(Arrays.asList(input, recipe.getResult()));
            case STONECUTTING:
                ItemStack scInput = ((CloudStonecuttingRecipe) recipe).getInputItem();
                return ItemUtils.getMultiItemHash(Arrays.asList(scInput, recipe.getResult()));
            case POTION:
            case POTION_CONTAINER:
                return ItemUtils.getMultiItemHash(Arrays.asList(((MixRecipe) recipe).getInput(), ((MixRecipe) recipe).getIngredient()));
        }
        return null;
    }

    private static RecipeUnlockingRequirement toProtocolRequirement(RecipeUnlockContext context) {
        RecipeUnlockingRequirement.UnlockingContext protocolContext = switch (context) {
            case NONE -> RecipeUnlockingRequirement.UnlockingContext.NONE;
            case ALWAYS_UNLOCKED -> RecipeUnlockingRequirement.UnlockingContext.ALWAYS_UNLOCKED;
            case PLAYER_IN_WATER -> RecipeUnlockingRequirement.UnlockingContext.PLAYER_IN_WATER;
            case PLAYER_HAS_MANY_ITEMS -> RecipeUnlockingRequirement.UnlockingContext.PLAYER_HAS_MANY_ITEMS;
        };
        return new RecipeUnlockingRequirement(protocolContext);
    }
}